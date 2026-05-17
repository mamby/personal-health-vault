namespace SelfCustodyHealth.Security;

public sealed class PreferencesAppLockSettingsStore : IAppLockSettingsStore
{
	private const string AppLockEnabledKey = "self_custody_health_app_lock_enabled";

	public bool IsEnabled
	{
		get => Preferences.Default.Get(AppLockEnabledKey, false);
		set => Preferences.Default.Set(AppLockEnabledKey, value);
	}
}
