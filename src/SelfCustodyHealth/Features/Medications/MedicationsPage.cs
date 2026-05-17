using SelfCustodyHealth.Shared;
using SelfCustodyHealth.Shared.Ui;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth.Features.Medications;

public sealed class MedicationsPage(HealthDataService dataService) : ContentPage
{
	protected override async void OnAppearing()
	{
		base.OnAppearing();
		var snapshot = await dataService.GetSnapshotAsync();
		var stack = Ui.PageStack(
			Ui.PageTitle("Medications"),
			Ui.Body("Track treatments and reminders. This app does not tell you what to take."));

		foreach (var medication in snapshot.Medications.OrderByDescending(m => m.IsActive).ThenBy(m => m.Name))
		{
			stack.Children.Add(Ui.Card(new VerticalStackLayout
			{
				Spacing = 8,
				Children =
				{
					new HorizontalStackLayout
					{
						Spacing = 10,
						Children =
						{
							Ui.SectionTitle(medication.Name),
							Ui.Badge(medication.IsActive ? "Active" : "Inactive", medication.IsActive ? Color.FromArgb("#2E7D55") : Color.FromArgb("#6B7A80"))
						}
					},
					Ui.Body(medication.Dose),
					Ui.Muted(medication.Instructions),
					Ui.Muted($"{HealthText.RecurrenceName(medication.Schedule.Recurrence)} at {medication.Schedule.TimeOfDay:h\\:mm}")
				}
			}));
		}

		Content = Ui.Scroll(stack);
	}
}
