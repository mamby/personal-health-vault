namespace SelfCustodyHealth.Security;

public readonly record struct AppLockPresentationDecision(
	bool ShouldShowLock,
	bool ShouldStartAutomaticUnlock)
{
	public static AppLockPresentationDecision None { get; } = new(false, false);
}

public sealed class AppLockCoordinator(IAppLockService appLockService)
{
	private bool _isLockVisible;
	private bool _autoUnlockOnNextActivation = true;
	private int _foregroundCycle = 1;
	private int _lastAutomaticUnlockCycle;

	public AppLockPresentationDecision Activated()
	{
		var shouldAutoUnlock = _autoUnlockOnNextActivation
			&& _lastAutomaticUnlockCycle != _foregroundCycle;

		_autoUnlockOnNextActivation = false;
		return RequestPresentationCore(shouldAutoUnlock);
	}

	public void Resumed()
	{
		appLockService.MarkResumed();
		_foregroundCycle++;
		_autoUnlockOnNextActivation = appLockService.IsLockRequired;
	}

	public void Stopped()
	{
		if (appLockService.IsUnlockInProgress)
		{
			return;
		}

		appLockService.CancelPendingUnlock();
		appLockService.MarkBackgrounded();
	}

	public void Destroying() => appLockService.CancelPendingUnlock();

	public AppLockPresentationDecision RequestPresentation(bool shouldAutoUnlock = false) =>
		RequestPresentationCore(
			shouldAutoUnlock && _lastAutomaticUnlockCycle != _foregroundCycle);

	public void NotifyLockDismissed() => _isLockVisible = false;

	public void NotifyLockPresentationFailed() => _isLockVisible = false;

	private AppLockPresentationDecision RequestPresentationCore(bool shouldAutoUnlock)
	{
		if (_isLockVisible)
		{
			return MarkAutomaticUnlockCycle(new(false, shouldAutoUnlock));
		}

		if (!appLockService.IsLockRequired)
		{
			return AppLockPresentationDecision.None;
		}

		_isLockVisible = true;
		return MarkAutomaticUnlockCycle(new(true, shouldAutoUnlock));
	}

	private AppLockPresentationDecision MarkAutomaticUnlockCycle(AppLockPresentationDecision decision)
	{
		if (decision.ShouldStartAutomaticUnlock)
		{
			_lastAutomaticUnlockCycle = _foregroundCycle;
		}

		return decision;
	}
}
