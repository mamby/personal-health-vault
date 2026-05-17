using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Shared;

internal static class HealthText
{
	public static string CategoryName(DocumentCategory category) =>
		category switch
		{
			DocumentCategory.All => "All",
			DocumentCategory.LabResults => "Lab Results",
			DocumentCategory.Prescriptions => "Prescriptions",
			DocumentCategory.Reports => "Reports",
			DocumentCategory.Vaccinations => "Vaccinations",
			DocumentCategory.Other => "Other",
			_ => "Other"
		};

	public static string RecurrenceName(ReminderRecurrence recurrence) =>
		recurrence switch
		{
			ReminderRecurrence.None => "Once",
			ReminderRecurrence.Daily => "Daily",
			ReminderRecurrence.Weekly => "Weekly",
			ReminderRecurrence.Monthly => "Monthly",
			_ => "Scheduled"
		};

	public static string FormatDate(DateOnly date) =>
		date.ToString("MMM d, yyyy");

	public static string FormatDateTime(DateTimeOffset dateTime) =>
		dateTime.ToString("MMM d, yyyy h:mm tt");
}
