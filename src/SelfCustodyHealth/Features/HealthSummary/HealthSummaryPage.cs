using SelfCustodyHealth.Shared;
using SelfCustodyHealth.Shared.Ui;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth.Features.HealthSummary;

public sealed class HealthSummaryPage(HealthDataService dataService) : ContentPage
{
	protected override async void OnAppearing()
	{
		base.OnAppearing();
		var snapshot = await dataService.GetSnapshotAsync();
		var summary = snapshot.Summary;

		var contacts = new VerticalStackLayout
		{
			Spacing = 10,
			Children = { Ui.SectionTitle("Emergency contacts") }
		};

		foreach (var contact in summary.EmergencyContacts)
		{
			contacts.Children.Add(Ui.Body($"{contact.Name} / {contact.Relationship} / {contact.PhoneNumber}"));
		}

		Content = Ui.Scroll(Ui.PageStack(
			Ui.PageTitle("Health Summary"),
			Ui.Body("A concise local summary for your own records. Verify every detail yourself."),
			Ui.Card(CreateList("Blood type", [summary.BloodType])),
			Ui.Card(CreateList("Allergies", summary.Allergies)),
			Ui.Card(CreateList("Chronic conditions", summary.ChronicConditions)),
			Ui.Card(CreateList("Surgeries", summary.Surgeries)),
			Ui.Card(contacts),
			Ui.Muted($"Last updated {summary.LastUpdatedAt:MMM d, yyyy}")));
	}

	private static VerticalStackLayout CreateList(string title, IReadOnlyList<string> items)
	{
		var stack = new VerticalStackLayout
		{
			Spacing = 8,
			Children = { Ui.SectionTitle(title) }
		};

		foreach (var item in items.DefaultIfEmpty("Not set"))
		{
			stack.Children.Add(Ui.Body(item));
		}

		return stack;
	}
}
