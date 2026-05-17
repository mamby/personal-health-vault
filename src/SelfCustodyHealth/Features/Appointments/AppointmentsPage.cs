using SelfCustodyHealth.Domain;
using SelfCustodyHealth.Shared;
using SelfCustodyHealth.Shared.Ui;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth.Features.Appointments;

public sealed class AppointmentsPage(HealthDataService dataService) : ContentPage
{
	protected override async void OnAppearing()
	{
		base.OnAppearing();
		var snapshot = await dataService.GetSnapshotAsync();
		var now = DateTimeOffset.Now;
		var upcoming = snapshot.Appointments.Where(appointment => appointment.StartsAt >= now).OrderBy(appointment => appointment.StartsAt).ToArray();
		var past = snapshot.Appointments.Where(appointment => appointment.StartsAt < now).OrderByDescending(appointment => appointment.StartsAt).ToArray();

		var stack = Ui.PageStack(
			Ui.PageTitle("Appointments"),
			Ui.Body("Upcoming and past visits stay in your local vault."),
			Ui.SectionTitle("Upcoming"));

		AddAppointments(stack, upcoming, "No upcoming appointments.");
		stack.Children.Add(Ui.SectionTitle("Past"));
		AddAppointments(stack, past, "No past appointments.");

		Content = Ui.Scroll(stack);
	}

	private static void AddAppointments(VerticalStackLayout stack, IReadOnlyList<Appointment> appointments, string emptyText)
	{
		if (appointments.Count is 0)
		{
			stack.Children.Add(Ui.Card(Ui.Muted(emptyText)));
			return;
		}

		foreach (var appointment in appointments)
		{
			stack.Children.Add(Ui.Card(new VerticalStackLayout
			{
				Spacing = 8,
				Children =
				{
					Ui.SectionTitle(appointment.Title),
					Ui.Body(appointment.Clinician),
					Ui.Muted(HealthText.FormatDateTime(appointment.StartsAt)),
					Ui.Muted(appointment.Location),
					Ui.Muted(appointment.RelatedDocumentIds.Count is 0 ? "No related documents" : $"{appointment.RelatedDocumentIds.Count} related document(s)")
				}
			}));
		}
	}
}
