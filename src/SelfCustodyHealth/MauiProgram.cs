using CommunityToolkit.Maui;
using Microsoft.Extensions.Logging;
using SelfCustodyHealth.Crypto;
using SelfCustodyHealth.Features.Appointments;
using SelfCustodyHealth.Features.Dashboard;
using SelfCustodyHealth.Features.HealthSummary;
using SelfCustodyHealth.Features.Lock;
using SelfCustodyHealth.Features.Medications;
using SelfCustodyHealth.Features.Settings;
using SelfCustodyHealth.Features.Vault;
using SelfCustodyHealth.Security;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth;

public static class MauiProgram
{
	public static MauiApp CreateMauiApp()
	{
		var builder = MauiApp.CreateBuilder();
		builder
			.UseMauiApp<App>()
			.UseMauiCommunityToolkit()
			.ConfigureFonts(fonts =>
			{
				fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
				fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
			});

		builder.Services.AddSingleton<IEncryptionService, AesGcmEncryptionService>();
		builder.Services.AddSingleton<IVaultKeyProvider, SecureStorageVaultKeyProvider>();
		builder.Services.AddSingleton<IVaultStore>(services =>
		{
			var vaultPath = Path.Combine(
				FileSystem.AppDataDirectory,
				"vault",
				"self-custody-health.vault");

			return new EncryptedVaultStore(
				vaultPath,
				services.GetRequiredService<IVaultKeyProvider>(),
				services.GetRequiredService<IEncryptionService>());
		});
		builder.Services.AddSingleton<HealthDataService>();
		builder.Services.AddSingleton<IDocumentStore, DocumentSearchService>();
		builder.Services.AddSingleton<IBackupService, PendingBackupService>();
		builder.Services.AddSingleton<ILocalDocumentClassifier, PendingLocalDocumentClassifier>();
		builder.Services.AddSingleton<ILocalOcrService, PendingLocalOcrService>();
		builder.Services.AddSingleton<ILocalSummaryService, PendingLocalSummaryService>();
		builder.Services.AddSingleton<IDeviceUnlockService, DeviceUnlockService>();
		builder.Services.AddSingleton<IAppLockSettingsStore, PreferencesAppLockSettingsStore>();
		builder.Services.AddSingleton<IAppLockService, AppLockService>();
		builder.Services.AddSingleton<AppLockCoordinator>();
		builder.Services.AddTransient<DashboardPage>();
		builder.Services.AddTransient<VaultPage>();
		builder.Services.AddTransient<HealthSummaryPage>();
		builder.Services.AddTransient<MedicationsPage>();
		builder.Services.AddTransient<AppointmentsPage>();
		builder.Services.AddTransient<SettingsPage>();
		builder.Services.AddTransient<LockPage>();

#if DEBUG
		builder.Logging.AddDebug();
#endif

		return builder.Build();
	}
}
