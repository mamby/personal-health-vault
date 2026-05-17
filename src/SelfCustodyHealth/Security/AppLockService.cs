namespace SelfCustodyHealth.Security;

public interface IAppLockSettingsStore
{
	bool IsEnabled { get; set; }
}

public interface IAppLockService
{
	bool IsEnabled { get; }

	bool IsLocked { get; }

	bool IsLockRequired { get; }

	bool IsUnlockInProgress { get; }

	Task<DeviceUnlockResult> EnableAsync(CancellationToken cancellationToken);

	void Disable();

	void LockNow();

	void CancelPendingUnlock();

	void MarkBackgrounded();

	void MarkResumed();

	Task<DeviceUnlockResult> UnlockAsync(CancellationToken cancellationToken);
}

public sealed class AppLockService(
	IDeviceUnlockService deviceUnlockService,
	IAppLockSettingsStore settingsStore) : IAppLockService
{
	private bool _isLocked = settingsStore.IsEnabled;
	private bool _wasBackgrounded;
	private int _unlockRequestCount;

	public bool IsEnabled => settingsStore.IsEnabled;

	public bool IsLocked => IsEnabled && _isLocked;

	public bool IsLockRequired => IsEnabled && _isLocked;

	public bool IsUnlockInProgress => _unlockRequestCount > 0;

	public async Task<DeviceUnlockResult> EnableAsync(CancellationToken cancellationToken)
	{
		var result = await RequestDeviceUnlockAsync(
			"Unlock Self-Custody Health to enable app lock.",
			cancellationToken).ConfigureAwait(false);

		if (!result.Succeeded)
		{
			return result;
		}

		settingsStore.IsEnabled = true;
		_isLocked = false;
		return result;
	}

	public void Disable()
	{
		settingsStore.IsEnabled = false;
		_isLocked = false;
	}

	public void LockNow()
	{
		if (IsEnabled)
		{
			_isLocked = true;
		}
	}

	public void CancelPendingUnlock() => deviceUnlockService.CancelPendingUnlock();

	public void MarkBackgrounded() => _wasBackgrounded = true;

	public void MarkResumed()
	{
		if (IsEnabled && _wasBackgrounded)
		{
			_isLocked = true;
		}

		_wasBackgrounded = false;
	}

	public async Task<DeviceUnlockResult> UnlockAsync(CancellationToken cancellationToken)
	{
		if (!IsEnabled)
		{
			_isLocked = false;
			return DeviceUnlockResult.Success("App lock is disabled.");
		}

		var result = await RequestDeviceUnlockAsync(
			"Unlock your local health vault.",
			cancellationToken).ConfigureAwait(false);

		if (result.Succeeded)
		{
			_isLocked = false;
		}

		return result;
	}

	private async Task<DeviceUnlockResult> RequestDeviceUnlockAsync(
		string reason,
		CancellationToken cancellationToken)
	{
		Interlocked.Increment(ref _unlockRequestCount);
		try
		{
			return await deviceUnlockService.RequestUnlockAsync(reason, cancellationToken).ConfigureAwait(false);
		}
		finally
		{
			Interlocked.Decrement(ref _unlockRequestCount);
		}
	}
}

public sealed class InMemoryAppLockSettingsStore(bool isEnabled = false) : IAppLockSettingsStore
{
	public bool IsEnabled { get; set; } = isEnabled;
}
