using SelfCustodyHealth.Domain;

namespace SelfCustodyHealth.Tests;

public sealed class ReminderSchedulerTests
{
	[Fact]
	public void GetNextOccurrence_DailyReminder_ReturnsNextDayWhenTodayPassed()
	{
		var reminder = new Reminder
		{
			Id = Guid.NewGuid(),
			Title = "Take medication",
			StartsOn = new DateOnly(2026, 5, 10),
			TimeOfDay = new TimeOnly(8, 0),
			Recurrence = ReminderRecurrence.Daily
		};

		var now = new DateTimeOffset(2026, 5, 16, 9, 0, 0, TimeSpan.Zero);
		var next = ReminderScheduler.GetNextOccurrence(reminder, now);

		Assert.Equal(new DateTimeOffset(2026, 5, 17, 8, 0, 0, TimeSpan.Zero), next);
	}

	[Fact]
	public void GetNextOccurrence_EndedReminder_ReturnsNull()
	{
		var reminder = new Reminder
		{
			Id = Guid.NewGuid(),
			Title = "Past reminder",
			StartsOn = new DateOnly(2026, 5, 10),
			TimeOfDay = new TimeOnly(8, 0),
			Recurrence = ReminderRecurrence.Daily,
			EndsOn = new DateOnly(2026, 5, 15)
		};

		var now = new DateTimeOffset(2026, 5, 16, 9, 0, 0, TimeSpan.Zero);

		Assert.Null(ReminderScheduler.GetNextOccurrence(reminder, now));
	}
}
