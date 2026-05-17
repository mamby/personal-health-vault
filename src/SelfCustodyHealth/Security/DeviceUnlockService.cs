#if ANDROID
using AndroidX.Biometric;
using AndroidX.Core.Content;
using AndroidX.Fragment.App;
using Java.Lang;
using static AndroidX.Biometric.BiometricManager;
using static AndroidX.Biometric.BiometricPrompt;
#elif IOS || MACCATALYST
using Foundation;
using LocalAuthentication;
#elif WINDOWS
using Microsoft.Maui.Platform;
using Microsoft.UI.Windowing;
using Windows.Security.Credentials.UI;
#endif

namespace SelfCustodyHealth.Security;

public sealed class DeviceUnlockService : IDeviceUnlockService
{
	private readonly object _unlockGate = new();
	private CancellationTokenSource? _pendingUnlockCancellation;

	public Task<DeviceUnlockAvailability> GetAvailabilityAsync(CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();

#if ANDROID
		var activity = Platform.CurrentActivity;
		if (activity is null)
		{
			return Task.FromResult(DeviceUnlockAvailability.Unavailable);
		}

		var status = BiometricManager.From(activity)
			.CanAuthenticate(Authenticators.BiometricStrong | Authenticators.DeviceCredential);

		return Task.FromResult(MapAndroidAvailability(status));
#elif IOS || MACCATALYST
		using var context = new LAContext();
		var canEvaluate = context.CanEvaluatePolicy(LAPolicy.DeviceOwnerAuthentication, out var error);
		return Task.FromResult(canEvaluate ? DeviceUnlockAvailability.Available : MapAppleAvailability(error));
#elif WINDOWS
		return GetWindowsAvailabilityAsync(cancellationToken);
#else
		return Task.FromResult(DeviceUnlockAvailability.Unavailable);
#endif
	}

	public async Task<DeviceUnlockResult> RequestUnlockAsync(string reason, CancellationToken cancellationToken)
	{
		using var unlockCancellation = CreatePendingUnlockCancellation(cancellationToken);

		try
		{
#if ANDROID
			return await RequestAndroidUnlockAsync(reason, unlockCancellation.Token).ConfigureAwait(false);
#elif IOS || MACCATALYST
			return await RequestAppleUnlockAsync(reason, unlockCancellation.Token).ConfigureAwait(false);
#elif WINDOWS
			return await RequestWindowsUnlockAsync(reason, unlockCancellation.Token).ConfigureAwait(false);
#else
			return DeviceUnlockResult.Failure(
				DeviceUnlockAvailability.Unavailable,
				"Device unlock is not available on this platform.");
#endif
		}
		catch (OperationCanceledException) when (unlockCancellation.IsCancellationRequested)
		{
			return DeviceUnlockResult.Failure(
				DeviceUnlockAvailability.Available,
				"Device unlock was canceled.");
		}
		finally
		{
			ClearPendingUnlockCancellation(unlockCancellation);
		}
	}

	public void CancelPendingUnlock()
	{
		CancellationTokenSource? cancellation;
		lock (_unlockGate)
		{
			cancellation = _pendingUnlockCancellation;
		}

		cancellation?.Cancel();
	}

	private CancellationTokenSource CreatePendingUnlockCancellation(CancellationToken cancellationToken)
	{
		var cancellation = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
		CancellationTokenSource? previousCancellation;

		lock (_unlockGate)
		{
			previousCancellation = _pendingUnlockCancellation;
			_pendingUnlockCancellation = cancellation;
		}

		previousCancellation?.Cancel();
		return cancellation;
	}

	private void ClearPendingUnlockCancellation(CancellationTokenSource cancellation)
	{
		lock (_unlockGate)
		{
			if (ReferenceEquals(_pendingUnlockCancellation, cancellation))
			{
				_pendingUnlockCancellation = null;
			}
		}
	}

#if ANDROID
	private static async Task<DeviceUnlockResult> RequestAndroidUnlockAsync(
		string reason,
		CancellationToken cancellationToken)
	{
		var activity = Platform.CurrentActivity as FragmentActivity;
		if (activity is null)
		{
			return DeviceUnlockResult.Failure(
				DeviceUnlockAvailability.Unavailable,
				"Device unlock is unavailable because the current Android activity is not ready.");
		}

		var availability = MapAndroidAvailability(BiometricManager.From(activity)
			.CanAuthenticate(Authenticators.BiometricStrong | Authenticators.DeviceCredential));

		if (availability is not DeviceUnlockAvailability.Available)
		{
			return DeviceUnlockResult.Failure(availability, ToAvailabilityMessage(availability));
		}

		var completion = new TaskCompletionSource<DeviceUnlockResult>(
			TaskCreationOptions.RunContinuationsAsynchronously);
		var callback = new AndroidAuthenticationCallback(completion);
		var promptInfo = new PromptInfo.Builder()
			.SetTitle("Unlock Self-Custody Health")
			.SetDescription(reason)
			.SetConfirmationRequired(true)
			.SetAllowedAuthenticators(Authenticators.BiometricStrong | Authenticators.DeviceCredential)
			.Build();

		var executor = ContextCompat.GetMainExecutor(activity);
		if (executor is null)
		{
			return DeviceUnlockResult.Failure(
				DeviceUnlockAvailability.Unavailable,
				"Device unlock is unavailable because Android could not provide a main executor.");
		}

		var prompt = new BiometricPrompt(activity, executor, callback);
		using var registration = cancellationToken.Register(() =>
		{
			MainThread.BeginInvokeOnMainThread(prompt.CancelAuthentication);
			completion.TrySetCanceled(cancellationToken);
		});

		cancellationToken.ThrowIfCancellationRequested();
		await MainThread.InvokeOnMainThreadAsync(() => prompt.Authenticate(promptInfo));

		return await completion.Task.ConfigureAwait(false);
	}

	private static DeviceUnlockAvailability MapAndroidAvailability(int status) =>
		status switch
		{
			BiometricSuccess => DeviceUnlockAvailability.Available,
			BiometricErrorNoneEnrolled => DeviceUnlockAvailability.NotEnrolled,
			BiometricErrorSecurityUpdateRequired => DeviceUnlockAvailability.DisabledByPolicy,
			BiometricErrorHwUnavailable => DeviceUnlockAvailability.Unavailable,
			BiometricErrorNoHardware => DeviceUnlockAvailability.Unavailable,
			BiometricErrorUnsupported => DeviceUnlockAvailability.Unavailable,
			_ => DeviceUnlockAvailability.Unavailable
		};

	private sealed class AndroidAuthenticationCallback(
		TaskCompletionSource<DeviceUnlockResult> completion) : AuthenticationCallback
	{
		public override void OnAuthenticationSucceeded(AuthenticationResult result)
		{
			completion.TrySetResult(DeviceUnlockResult.Success("Unlocked."));
		}

		public override void OnAuthenticationFailed()
		{
		}

		public override void OnAuthenticationError(int errorCode, ICharSequence errString)
		{
			var availability = errorCode switch
			{
				ErrorLockout or ErrorLockoutPermanent => DeviceUnlockAvailability.LockedOut,
				ErrorNoBiometrics => DeviceUnlockAvailability.NotEnrolled,
				_ => DeviceUnlockAvailability.Available
			};

			var message = errorCode switch
			{
				ErrorUserCanceled or ErrorCanceled or ErrorNegativeButton => "Device unlock was canceled.",
				ErrorLockout or ErrorLockoutPermanent => "Device unlock is temporarily locked. Use your device passcode or try again later.",
				_ => errString.ToString()
			};

			completion.TrySetResult(DeviceUnlockResult.Failure(availability, message));
		}
	}
#endif

#if IOS || MACCATALYST
	private static async Task<DeviceUnlockResult> RequestAppleUnlockAsync(
		string reason,
		CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();

		using var context = new LAContext();
		if (!context.CanEvaluatePolicy(LAPolicy.DeviceOwnerAuthentication, out var availabilityError))
		{
			var availability = MapAppleAvailability(availabilityError);
			return DeviceUnlockResult.Failure(availability, ToAvailabilityMessage(availability));
		}

		try
		{
			var (success, error) = await context.EvaluatePolicyAsync(
				LAPolicy.DeviceOwnerAuthentication,
				reason).ConfigureAwait(false);

			return success
				? DeviceUnlockResult.Success("Unlocked.")
				: DeviceUnlockResult.Failure(MapAppleAvailability(error), MapAppleError(error));
		}
		catch (OperationCanceledException)
		{
			throw;
		}
		catch
		{
			return DeviceUnlockResult.Failure(
				DeviceUnlockAvailability.Unavailable,
				"Device unlock failed unexpectedly.");
		}
	}

	private static DeviceUnlockAvailability MapAppleAvailability(NSError? error)
	{
		if (error is null)
		{
			return DeviceUnlockAvailability.Unavailable;
		}

		return (long)error.Code switch
		{
			-7 => DeviceUnlockAvailability.NotEnrolled,
			-8 => DeviceUnlockAvailability.LockedOut,
			-6 => DeviceUnlockAvailability.Unavailable,
			_ => DeviceUnlockAvailability.Unavailable
		};
	}

	private static string MapAppleError(NSError? error) =>
		error is null
			? "Device unlock failed."
			: (long)error.Code switch
			{
				-2 or -4 or -9 => "Device unlock was canceled.",
				-7 => "No biometric or device credential is enrolled.",
				-8 => "Device unlock is temporarily locked. Use your device passcode or try again later.",
				_ => "Device unlock failed."
			};
#endif

#if WINDOWS
	private static async Task<DeviceUnlockAvailability> GetWindowsAvailabilityAsync(
		CancellationToken cancellationToken)
	{
		cancellationToken.ThrowIfCancellationRequested();

		var availability = await UserConsentVerifier.CheckAvailabilityAsync();
		return availability switch
		{
			UserConsentVerifierAvailability.Available => DeviceUnlockAvailability.Available,
			UserConsentVerifierAvailability.NotConfiguredForUser => DeviceUnlockAvailability.NotEnrolled,
			UserConsentVerifierAvailability.DisabledByPolicy => DeviceUnlockAvailability.DisabledByPolicy,
			_ => DeviceUnlockAvailability.Unavailable
		};
	}

	private static async Task<DeviceUnlockResult> RequestWindowsUnlockAsync(
		string reason,
		CancellationToken cancellationToken)
	{
		var availability = await GetWindowsAvailabilityAsync(cancellationToken).ConfigureAwait(false);
		if (availability is not DeviceUnlockAvailability.Available)
		{
			return DeviceUnlockResult.Failure(availability, ToAvailabilityMessage(availability));
		}

		var ownerWindow = await MainThread.InvokeOnMainThreadAsync(GetWindowsOwnerWindow);
		if (ownerWindow is not null)
		{
			await MainThread.InvokeOnMainThreadAsync(() => RestoreAndActivateWindowsOwner(ownerWindow));
		}

		var ownerWindowHandle = ownerWindow is null
			? IntPtr.Zero
			: WinRT.Interop.WindowNative.GetWindowHandle(ownerWindow);
		if (ownerWindowHandle == IntPtr.Zero)
		{
			return DeviceUnlockResult.Failure(
				DeviceUnlockAvailability.Unavailable,
				"Windows Hello is unavailable because the app window is not ready.");
		}

		var operation = UserConsentVerifierInterop.RequestVerificationForWindowAsync(ownerWindowHandle, reason);
		using var registration = cancellationToken.Register(operation.Cancel);
		UserConsentVerificationResult result;
		try
		{
			result = await operation.AsTask(cancellationToken).ConfigureAwait(false);
		}
		finally
		{
			if (ownerWindow is not null)
			{
				await MainThread.InvokeOnMainThreadAsync(() => RestoreAndActivateWindowsOwner(ownerWindow));
			}
		}

		return result switch
		{
			UserConsentVerificationResult.Verified => DeviceUnlockResult.Success("Unlocked."),
			UserConsentVerificationResult.NotConfiguredForUser => DeviceUnlockResult.Failure(DeviceUnlockAvailability.NotEnrolled, "No Windows Hello method is configured for this user."),
			UserConsentVerificationResult.DisabledByPolicy => DeviceUnlockResult.Failure(DeviceUnlockAvailability.DisabledByPolicy, "Windows Hello is disabled by policy."),
			UserConsentVerificationResult.RetriesExhausted => DeviceUnlockResult.Failure(DeviceUnlockAvailability.LockedOut, "Windows Hello retries were exhausted."),
			UserConsentVerificationResult.Canceled => DeviceUnlockResult.Failure(DeviceUnlockAvailability.Available, "Device unlock was canceled."),
			UserConsentVerificationResult.DeviceBusy => DeviceUnlockResult.Failure(DeviceUnlockAvailability.Unavailable, "Windows Hello is busy. Try again."),
			_ => DeviceUnlockResult.Failure(DeviceUnlockAvailability.Unavailable, "Windows Hello is unavailable.")
		};
	}

	private static MauiWinUIWindow? GetWindowsOwnerWindow()
	{
		var window = Application.Current?.Windows
			.FirstOrDefault(candidate => candidate.Handler?.PlatformView is MauiWinUIWindow);

		return window?.Handler?.PlatformView as MauiWinUIWindow;
	}

	private static void RestoreAndActivateWindowsOwner(MauiWinUIWindow nativeWindow)
	{
		if (nativeWindow.AppWindow.Presenter is OverlappedPresenter
			{
				State: OverlappedPresenterState.Minimized
			} presenter)
		{
			presenter.Restore(true);
		}

		nativeWindow.Activate();
	}
#endif

	private static string ToAvailabilityMessage(DeviceUnlockAvailability availability) =>
		availability switch
		{
			DeviceUnlockAvailability.Available => "Device unlock is available.",
			DeviceUnlockAvailability.NotEnrolled => "No biometric or device credential is enrolled.",
			DeviceUnlockAvailability.DisabledByPolicy => "Device unlock is disabled by policy.",
			DeviceUnlockAvailability.LockedOut => "Device unlock is temporarily locked.",
			_ => "Device unlock is unavailable on this device."
		};
}
