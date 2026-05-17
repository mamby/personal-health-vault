namespace SelfCustodyHealth.Domain;

public static class DemoHealthVaultFactory
{
	public static HealthVaultSnapshot Create(DateTimeOffset now)
	{
		var contact = new EmergencyContact
		{
			Id = Guid.Parse("0f28c8c2-8150-4a02-93c1-39a0a24b4f7d"),
			Name = "Alex Morgan",
			Relationship = "Partner",
			PhoneNumber = "+1 555 010 0142",
			Notes = "Primary emergency contact"
		};

		var summary = new HealthSummary
		{
			BloodType = "O+",
			Allergies = ["Penicillin"],
			ChronicConditions = ["Mild asthma"],
			Surgeries = ["Appendectomy, 2018"],
			EmergencyContacts = [contact],
			LastUpdatedAt = now.AddDays(-2)
		};

		var documents = new[]
		{
			new MedicalDocument
			{
				Id = Guid.Parse("45e4dcf3-4899-4e7f-a455-85d5b3e878cb"),
				Title = "Annual blood panel",
				Category = DocumentCategory.LabResults,
				DocumentDate = DateOnly.FromDateTime(now.AddDays(-18).Date),
				Source = "Northside Lab",
				Notes = "Demo document. Replace with your own encrypted local records.",
				Tags = ["blood", "annual"],
				IsDemo = true
			},
			new MedicalDocument
			{
				Id = Guid.Parse("db2e8730-6fa9-4775-a978-c2f0fbd5190e"),
				Title = "Asthma inhaler prescription",
				Category = DocumentCategory.Prescriptions,
				DocumentDate = DateOnly.FromDateTime(now.AddDays(-32).Date),
				Source = "Primary care",
				Notes = "Demo prescription record.",
				Tags = ["asthma", "prescription"],
				IsDemo = true
			},
			new MedicalDocument
			{
				Id = Guid.Parse("08ebee47-cd87-4f2d-aee2-75774ad4ab74"),
				Title = "Flu vaccination receipt",
				Category = DocumentCategory.Vaccinations,
				DocumentDate = DateOnly.FromDateTime(now.AddMonths(-4).Date),
				Source = "Community pharmacy",
				Notes = "Demo vaccination record.",
				Tags = ["flu", "vaccine"],
				IsDemo = true
			}
		};

		var schedule = new MedicationSchedule
		{
			Recurrence = ReminderRecurrence.Daily,
			TimeOfDay = new TimeOnly(8, 0),
			StartsOn = DateOnly.FromDateTime(now.AddDays(-20).Date)
		};

		var medications = new[]
		{
			new Medication
			{
				Id = Guid.Parse("6be3c297-fd55-4ec7-8c66-3ea11d668302"),
				Name = "Maintenance inhaler",
				Dose = "1 puff",
				Instructions = "Demo treatment record. Confirm actual medication details with your clinician.",
				Schedule = schedule,
				IsActive = true,
				RemindersEnabled = true
			}
		};

		var appointments = new[]
		{
			new Appointment
			{
				Id = Guid.Parse("f887f353-b8d2-497e-99ac-9084dfe12920"),
				Title = "Primary care check-in",
				Clinician = "Dr. Rivera",
				Location = "Downtown Clinic",
				StartsAt = now.AddDays(12).Date.AddHours(10),
				RelatedDocumentIds = [documents[0].Id],
				Notes = "Bring recent lab result."
			}
		};

		var vaccinations = new[]
		{
			new Vaccination
			{
				Id = Guid.Parse("2707e3f2-71a6-4fd8-a68c-63f0e1d5fb66"),
				Name = "Influenza",
				DateAdministered = DateOnly.FromDateTime(now.AddMonths(-4).Date),
				Provider = "Community pharmacy"
			}
		};

		var reminders = new[]
		{
			new Reminder
			{
				Id = Guid.Parse("5efe669e-64c0-4191-a996-c7ac3519c20e"),
				Title = "Take maintenance inhaler",
				StartsOn = DateOnly.FromDateTime(now.AddDays(-20).Date),
				TimeOfDay = new TimeOnly(8, 0),
				Recurrence = ReminderRecurrence.Daily
			}
		};

		var profile = new HealthProfile
		{
			Id = Guid.Parse("54b4f6a2-8d3b-47e7-bf8f-dc388f6d857f"),
			DisplayName = "Demo vault",
			BloodType = summary.BloodType,
			Allergies = summary.Allergies,
			ChronicConditions = summary.ChronicConditions,
			Surgeries = summary.Surgeries,
			EmergencyContacts = summary.EmergencyContacts,
			LastUpdatedAt = summary.LastUpdatedAt
		};

		return new HealthVaultSnapshot
		{
			Profile = profile,
			Summary = summary,
			Documents = documents,
			Medications = medications,
			Appointments = appointments,
			Vaccinations = vaccinations,
			Reminders = reminders,
			VaultItems = CreateVaultItems(documents, medications, appointments, vaccinations, reminders, now),
			UpdatedAt = now
		};
	}

	private static IReadOnlyList<VaultItem> CreateVaultItems(
		IReadOnlyList<MedicalDocument> documents,
		IReadOnlyList<Medication> medications,
		IReadOnlyList<Appointment> appointments,
		IReadOnlyList<Vaccination> vaccinations,
		IReadOnlyList<Reminder> reminders,
		DateTimeOffset now) =>
		[
			.. documents.Select(document => new VaultItem
			{
				Id = document.Id,
				Kind = VaultItemKind.Document,
				Title = document.Title,
				UpdatedAt = now.AddDays(-1)
			}),
			.. medications.Select(medication => new VaultItem
			{
				Id = medication.Id,
				Kind = VaultItemKind.Medication,
				Title = medication.Name,
				UpdatedAt = now.AddDays(-2)
			}),
			.. appointments.Select(appointment => new VaultItem
			{
				Id = appointment.Id,
				Kind = VaultItemKind.Appointment,
				Title = appointment.Title,
				UpdatedAt = appointment.StartsAt
			}),
			.. vaccinations.Select(vaccination => new VaultItem
			{
				Id = vaccination.Id,
				Kind = VaultItemKind.Vaccination,
				Title = vaccination.Name,
				UpdatedAt = now.AddMonths(-4)
			}),
			.. reminders.Select(reminder => new VaultItem
			{
				Id = reminder.Id,
				Kind = VaultItemKind.Reminder,
				Title = reminder.Title,
				UpdatedAt = now.AddDays(-1)
			})
		];
}
