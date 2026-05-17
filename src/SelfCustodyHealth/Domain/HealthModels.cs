namespace SelfCustodyHealth.Domain;

public enum DocumentCategory
{
	All,
	LabResults,
	Prescriptions,
	Reports,
	Vaccinations,
	Other
}

public enum ReminderRecurrence
{
	None,
	Daily,
	Weekly,
	Monthly
}

public enum VaultItemKind
{
	Document,
	Medication,
	Appointment,
	Vaccination,
	Reminder
}

public sealed record EmergencyContact
{
	public required Guid Id { get; init; }
	public required string Name { get; init; }
	public required string Relationship { get; init; }
	public required string PhoneNumber { get; init; }
	public string? Notes { get; init; }
}

public sealed record HealthProfile
{
	public required Guid Id { get; init; }
	public required string DisplayName { get; init; }
	public string? BloodType { get; init; }
	public IReadOnlyList<string> Allergies { get; init; } = [];
	public IReadOnlyList<string> ChronicConditions { get; init; } = [];
	public IReadOnlyList<string> Surgeries { get; init; } = [];
	public IReadOnlyList<EmergencyContact> EmergencyContacts { get; init; } = [];
	public required DateTimeOffset LastUpdatedAt { get; init; }
}

public sealed record MedicalDocument
{
	public required Guid Id { get; init; }
	public required string Title { get; init; }
	public required DocumentCategory Category { get; init; }
	public required DateOnly DocumentDate { get; init; }
	public required string Source { get; init; }
	public string? Notes { get; init; }
	public IReadOnlyList<string> Tags { get; init; } = [];
	public bool IsDemo { get; init; }
}

public sealed record MedicationSchedule
{
	public required ReminderRecurrence Recurrence { get; init; }
	public required TimeOnly TimeOfDay { get; init; }
	public IReadOnlyList<DayOfWeek> DaysOfWeek { get; init; } = [];
	public DateOnly? StartsOn { get; init; }
	public DateOnly? EndsOn { get; init; }
}

public sealed record Medication
{
	public required Guid Id { get; init; }
	public required string Name { get; init; }
	public required string Dose { get; init; }
	public required string Instructions { get; init; }
	public required MedicationSchedule Schedule { get; init; }
	public bool IsActive { get; init; }
	public bool RemindersEnabled { get; init; }
}

public sealed record Appointment
{
	public required Guid Id { get; init; }
	public required string Title { get; init; }
	public required string Clinician { get; init; }
	public required string Location { get; init; }
	public required DateTimeOffset StartsAt { get; init; }
	public IReadOnlyList<Guid> RelatedDocumentIds { get; init; } = [];
	public string? Notes { get; init; }
}

public sealed record Vaccination
{
	public required Guid Id { get; init; }
	public required string Name { get; init; }
	public required DateOnly DateAdministered { get; init; }
	public string? Provider { get; init; }
	public string? LotNumber { get; init; }
	public DateOnly? NextDueOn { get; init; }
}

public sealed record HealthSummary
{
	public required string BloodType { get; init; }
	public IReadOnlyList<string> Allergies { get; init; } = [];
	public IReadOnlyList<string> ChronicConditions { get; init; } = [];
	public IReadOnlyList<string> Surgeries { get; init; } = [];
	public IReadOnlyList<EmergencyContact> EmergencyContacts { get; init; } = [];
	public required DateTimeOffset LastUpdatedAt { get; init; }
}

public sealed record Reminder
{
	public required Guid Id { get; init; }
	public required string Title { get; init; }
	public required DateOnly StartsOn { get; init; }
	public required TimeOnly TimeOfDay { get; init; }
	public required ReminderRecurrence Recurrence { get; init; }
	public IReadOnlyList<DayOfWeek> DaysOfWeek { get; init; } = [];
	public DateOnly? EndsOn { get; init; }
	public bool IsEnabled { get; init; } = true;
}

public sealed record VaultItem
{
	public required Guid Id { get; init; }
	public required VaultItemKind Kind { get; init; }
	public required string Title { get; init; }
	public required DateTimeOffset UpdatedAt { get; init; }
}

public sealed record HealthVaultSnapshot
{
	public int Version { get; init; } = 1;
	public required HealthProfile Profile { get; init; }
	public required HealthSummary Summary { get; init; }
	public IReadOnlyList<MedicalDocument> Documents { get; init; } = [];
	public IReadOnlyList<Medication> Medications { get; init; } = [];
	public IReadOnlyList<Appointment> Appointments { get; init; } = [];
	public IReadOnlyList<Vaccination> Vaccinations { get; init; } = [];
	public IReadOnlyList<Reminder> Reminders { get; init; } = [];
	public IReadOnlyList<VaultItem> VaultItems { get; init; } = [];
	public required DateTimeOffset UpdatedAt { get; init; }
}
