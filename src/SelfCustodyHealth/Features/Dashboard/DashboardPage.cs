using SelfCustodyHealth.Domain;
using SelfCustodyHealth.Security;
using SelfCustodyHealth.Shared;
using SelfCustodyHealth.Shared.Ui;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth.Features.Dashboard;

public sealed class DashboardPage(
	HealthDataService dataService,
	IAppLockService appLockService) : ContentPage
{
	protected override async void OnAppearing()
	{
		base.OnAppearing();
		await RenderAsync();
	}

	private async Task RenderAsync()
	{
		var snapshot = await dataService.GetSnapshotAsync();
		var nextReminder = snapshot.Reminders
			.Select(reminder => new
			{
				Reminder = reminder,
				Next = ReminderScheduler.GetNextOccurrence(reminder, DateTimeOffset.Now)
			})
			.Where(item => item.Next is not null)
			.OrderBy(item => item.Next)
			.FirstOrDefault();

		Content = Ui.Scroll(Ui.PageStack(
			Ui.PageTitle($"Good day, {snapshot.Profile.DisplayName}"),
			Ui.Body("Your health data stays on this device. This app does not provide medical advice."),
			CreatePrivacyCard(),
			CreateMetrics(snapshot),
			CreateSummaryCard(snapshot),
			CreateNextCard("Next appointment", snapshot.Appointments.OrderBy(a => a.StartsAt).FirstOrDefault()?.Title ?? "No upcoming appointments", snapshot.Appointments.OrderBy(a => a.StartsAt).FirstOrDefault()?.StartsAt.ToString("MMM d, h:mm tt") ?? "Add one when ready"),
			CreateNextCard("Next reminder", nextReminder?.Reminder.Title ?? "No active reminders", nextReminder?.Next?.ToString("MMM d, h:mm tt") ?? "Reminders stay local"),
			CreateAiCard()));
	}

	private Border CreatePrivacyCard()
	{
		var status = appLockService.IsEnabled ? "App lock enabled" : "App lock available";
		var badge = Ui.Badge(status, appLockService.IsEnabled ? Color.FromArgb("#2E7D55") : Color.FromArgb("#3267A8"));

		var header = new Grid
		{
			ColumnDefinitions =
			{
				new ColumnDefinition(GridLength.Star),
				new ColumnDefinition(GridLength.Auto)
			},
			Children =
			{
				Ui.SectionTitle("Encrypted local vault"),
				badge
			}
		};
		Grid.SetColumn(badge, 1);

		return Ui.Card(new VerticalStackLayout
		{
			Spacing = 12,
			Children =
			{
				header,
				Ui.Body("Health records are stored locally. Backup and sync are user-controlled and will store only encrypted data when added."),
				Ui.Muted("Biometric unlock is an app access gate; vault encryption is managed separately.")
			}
		});
	}

	private static Grid CreateMetrics(HealthVaultSnapshot snapshot)
	{
		var grid = new Grid
		{
			ColumnDefinitions =
			{
				new ColumnDefinition(GridLength.Star),
				new ColumnDefinition(GridLength.Star)
			},
			RowDefinitions =
			{
				new RowDefinition(GridLength.Auto),
				new RowDefinition(GridLength.Auto)
			},
			ColumnSpacing = 12,
			RowSpacing = 12
		};

		AddMetric(grid, "Documents", snapshot.Documents.Count.ToString(), 0, 0);
		AddMetric(grid, "Medications", snapshot.Medications.Count(m => m.IsActive).ToString(), 1, 0);
		AddMetric(grid, "Appointments", snapshot.Appointments.Count(a => a.StartsAt >= DateTimeOffset.Now).ToString(), 0, 1);
		AddMetric(grid, "Reminders", snapshot.Reminders.Count(r => r.IsEnabled).ToString(), 1, 1);

		return grid;
	}

	private static Border CreateSummaryCard(HealthVaultSnapshot snapshot) =>
		Ui.Card(new VerticalStackLayout
		{
			Spacing = 10,
			Children =
			{
				Ui.SectionTitle("Quick health summary"),
				Ui.Body($"Blood type: {snapshot.Summary.BloodType}"),
				Ui.Body($"Allergies: {string.Join(", ", snapshot.Summary.Allergies)}"),
				Ui.Muted($"Last updated {snapshot.Summary.LastUpdatedAt:MMM d, yyyy}")
			}
		});

	private static Border CreateNextCard(string title, string value, string detail) =>
		Ui.Card(new VerticalStackLayout
		{
			Spacing = 6,
			Children =
			{
				Ui.SectionTitle(title),
				Ui.Body(value),
				Ui.Muted(detail)
			}
		});

	private static Border CreateAiCard() =>
		Ui.Card(new VerticalStackLayout
		{
			Spacing = 8,
			Children =
			{
				new HorizontalStackLayout
				{
					Spacing = 10,
					Children =
					{
						Ui.SectionTitle("Local AI assistant"),
						Ui.Badge("Pending", Color.FromArgb("#A86A22"))
					}
				},
				Ui.Body("AI features will run locally when available and will assist with organization only."),
				Ui.Muted("No cloud AI is enabled.")
			}
		});

	private static void AddMetric(Grid grid, string label, string value, int column, int row)
	{
		var card = Ui.Card(new VerticalStackLayout
		{
			Spacing = 4,
			Children =
			{
				Ui.Metric(value),
				Ui.Muted(label)
			}
		});

		Grid.SetColumn(card, column);
		Grid.SetRow(card, row);
		grid.Children.Add(card);
	}
}
