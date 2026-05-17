using SelfCustodyHealth.Security;
using SelfCustodyHealth.Shared.Ui;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth.Features.Settings;

public sealed class SettingsPage(
	IAppLockService appLockService,
	IDeviceUnlockService deviceUnlockService,
	IBackupService backupService,
	HealthDataService dataService) : ContentPage
{
	protected override async void OnAppearing()
	{
		base.OnAppearing();
		await RenderAsync();
	}

	private async Task RenderAsync()
	{
		var availability = await deviceUnlockService.GetAvailabilityAsync(CancellationToken.None);
		var backupConfigured = await backupService.IsConfiguredAsync(CancellationToken.None);

		var lockButton = new Button
		{
			Text = appLockService.IsEnabled ? "Disable app lock" : "Enable app lock"
		};
		lockButton.Clicked += async (_, _) => await ToggleAppLockAsync();

		var lockNowButton = Ui.SecondaryButton("Lock now");
		lockNowButton.IsEnabled = appLockService.IsEnabled;
		lockNowButton.Clicked += async (_, _) =>
		{
			appLockService.LockNow();
			if (Shell.Current is AppShell shell)
			{
				await shell.ShowLockIfRequiredAsync();
			}
		};

		var deleteButton = Ui.SecondaryButton("Delete local vault");
		deleteButton.TextColor = Color.FromArgb("#B64646");
		deleteButton.Clicked += async (_, _) => await DeleteLocalVaultAsync();

		Content = Ui.Scroll(Ui.PageStack(
			Ui.PageTitle("Settings"),
			Ui.Body("Privacy controls for a local-first vault."),
			Ui.Card(new VerticalStackLayout
			{
				Spacing = 10,
				Children =
				{
					Ui.SectionTitle("Privacy"),
					Ui.Body("Your health data stays on this device."),
					Ui.Muted("No ads. No tracking by default. No backend health-data storage.")
				}
			}),
			Ui.Card(new VerticalStackLayout
			{
				Spacing = 10,
				Children =
				{
					Ui.SectionTitle("Encryption status"),
					Ui.Body(dataService.IsDemoData ? "Demo data only" : "Encrypted local vault"),
					Ui.Muted("Authenticated encryption is used for saved local vault data.")
				}
			}),
			Ui.Card(new VerticalStackLayout
			{
				Spacing = 10,
				Children =
				{
					Ui.SectionTitle("App lock"),
					Ui.Body(appLockService.IsEnabled ? "Biometric/device unlock is enabled." : "Biometric/device unlock is off."),
					Ui.Muted($"Device unlock availability: {availability}"),
					lockButton,
					lockNowButton
				}
			}),
			Ui.Card(new VerticalStackLayout
			{
				Spacing = 10,
				Children =
				{
					Ui.SectionTitle("Backup"),
					Ui.Body(backupConfigured ? "User-controlled backup configured." : "User-controlled backup pending."),
					Ui.Muted("Future backup and sync must store only encrypted data.")
				}
			}),
			Ui.Card(new VerticalStackLayout
			{
				Spacing = 10,
				Children =
				{
					Ui.SectionTitle("Data"),
					Ui.Body("Export data is planned. Delete removes the encrypted local vault and vault key."),
					deleteButton
				}
			}),
			Ui.Muted("Self-Custody Health is not a diagnostic app and does not provide medical advice.")));
	}

	private async Task ToggleAppLockAsync()
	{
		if (appLockService.IsEnabled)
		{
			appLockService.Disable();
			await RenderAsync();
			return;
		}

		var result = await appLockService.EnableAsync(CancellationToken.None);
		if (!result.Succeeded)
		{
			await DisplayAlertAsync("App lock", result.Message, "OK");
		}

		await RenderAsync();
	}

	private async Task DeleteLocalVaultAsync()
	{
		var confirmed = await DisplayAlertAsync(
			"Delete local vault",
			"This deletes the encrypted local vault on this device. Demo data will be shown afterward.",
			"Delete",
			"Cancel");

		if (!confirmed)
		{
			return;
		}

		await dataService.DeleteLocalVaultAsync();
		await RenderAsync();
	}
}
