using SelfCustodyHealth.Security;

namespace SelfCustodyHealth.Tests;

public sealed class AppLockCoordinatorTests
{
	[Fact]
	public void Activated_OnLockedLaunch_ShowsLockAndStartsAutomaticUnlock()
	{
		var coordinator = new AppLockCoordinator(new FakeAppLockService());

		var decision = coordinator.Activated();

		Assert.True(decision.ShouldShowLock);
		Assert.True(decision.ShouldStartAutomaticUnlock);
	}

	[Fact]
	public void Activated_AfterAutomaticPromptCanceled_DoesNotPromptAgainInSameForegroundCycle()
	{
		var coordinator = new AppLockCoordinator(new FakeAppLockService());

		coordinator.Activated();
		var decision = coordinator.Activated();

		Assert.False(decision.ShouldShowLock);
		Assert.False(decision.ShouldStartAutomaticUnlock);
	}

	[Fact]
	public void Activated_AfterStoppedAndResumed_StartsAutomaticUnlockAgainForVisibleLock()
	{
		var appLock = new FakeAppLockService();
		var coordinator = new AppLockCoordinator(appLock);

		coordinator.Activated();
		coordinator.Stopped();
		coordinator.Resumed();
		var decision = coordinator.Activated();

		Assert.False(decision.ShouldShowLock);
		Assert.True(decision.ShouldStartAutomaticUnlock);
		Assert.Equal(1, appLock.CancelPendingUnlockCalls);
		Assert.Equal(1, appLock.MarkBackgroundedCalls);
		Assert.Equal(1, appLock.MarkResumedCalls);
	}

	[Fact]
	public void Stopped_WhenUnlockIsInProgress_DoesNotCancelOrMarkBackgrounded()
	{
		var appLock = new FakeAppLockService
		{
			IsUnlockInProgress = true
		};
		var coordinator = new AppLockCoordinator(appLock);

		coordinator.Stopped();

		Assert.Equal(0, appLock.CancelPendingUnlockCalls);
		Assert.Equal(0, appLock.MarkBackgroundedCalls);
	}

	[Fact]
	public void PresentationFailure_AllowsLockToBeShownAgain()
	{
		var coordinator = new AppLockCoordinator(new FakeAppLockService());

		coordinator.Activated();
		coordinator.NotifyLockPresentationFailed();
		var decision = coordinator.RequestPresentation();

		Assert.True(decision.ShouldShowLock);
		Assert.False(decision.ShouldStartAutomaticUnlock);
	}

	[Fact]
	public void Activated_WhenLockIsNotRequired_DoesNothing()
	{
		var appLock = new FakeAppLockService
		{
			IsLockRequired = false
		};
		var coordinator = new AppLockCoordinator(appLock);

		var decision = coordinator.Activated();

		Assert.False(decision.ShouldShowLock);
		Assert.False(decision.ShouldStartAutomaticUnlock);
	}

	private sealed class FakeAppLockService : IAppLockService
	{
		private bool _wasBackgrounded;

		public bool IsEnabled { get; set; } = true;

		public bool IsLocked => IsLockRequired;

		public bool IsLockRequired { get; set; } = true;

		public bool IsUnlockInProgress { get; set; }

		public int CancelPendingUnlockCalls { get; private set; }

		public int MarkBackgroundedCalls { get; private set; }

		public int MarkResumedCalls { get; private set; }

		public Task<DeviceUnlockResult> EnableAsync(CancellationToken cancellationToken) =>
			Task.FromResult(DeviceUnlockResult.Success("Unlocked."));

		public void Disable()
		{
			IsEnabled = false;
			IsLockRequired = false;
		}

		public void LockNow() => IsLockRequired = true;

		public void CancelPendingUnlock() => CancelPendingUnlockCalls++;

		public void MarkBackgrounded()
		{
			MarkBackgroundedCalls++;
			_wasBackgrounded = true;
		}

		public void MarkResumed()
		{
			MarkResumedCalls++;
			if (IsEnabled && _wasBackgrounded)
			{
				IsLockRequired = true;
			}

			_wasBackgrounded = false;
		}

		public Task<DeviceUnlockResult> UnlockAsync(CancellationToken cancellationToken)
		{
			IsLockRequired = false;
			return Task.FromResult(DeviceUnlockResult.Success("Unlocked."));
		}
	}
}
