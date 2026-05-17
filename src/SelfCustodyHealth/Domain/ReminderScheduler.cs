namespace SelfCustodyHealth.Domain;

public static class ReminderScheduler
{
	public static DateTimeOffset? GetNextOccurrence(Reminder reminder, DateTimeOffset now)
	{
		if (!reminder.IsEnabled)
		{
			return null;
		}

		var first = ToDateTimeOffset(reminder.StartsOn, reminder.TimeOfDay, now.Offset);
		var end = reminder.EndsOn is { } endsOn
			? ToDateTimeOffset(endsOn, TimeOnly.MaxValue, now.Offset)
			: (DateTimeOffset?)null;

		var next = reminder.Recurrence switch
		{
			ReminderRecurrence.None => first > now ? first : null,
			ReminderRecurrence.Daily => NextDaily(first, now),
			ReminderRecurrence.Weekly => NextWeekly(first, now, reminder.DaysOfWeek),
			ReminderRecurrence.Monthly => NextMonthly(first, now),
			_ => null
		};

		return next is not null && (end is null || next <= end) ? next : null;
	}

	private static DateTimeOffset ToDateTimeOffset(DateOnly date, TimeOnly time, TimeSpan offset) =>
		new(date.ToDateTime(time), offset);

	private static DateTimeOffset NextDaily(DateTimeOffset first, DateTimeOffset now)
	{
		if (first > now)
		{
			return first;
		}

		var days = Math.Floor((now - first).TotalDays) + 1;
		return first.AddDays(days);
	}

	private static DateTimeOffset? NextWeekly(
		DateTimeOffset first,
		DateTimeOffset now,
		IReadOnlyList<DayOfWeek> daysOfWeek)
	{
		var activeDays = daysOfWeek.Count is 0
			? [first.DayOfWeek]
			: daysOfWeek.Distinct().ToArray();

		for (var dayOffset = 0; dayOffset <= 14; dayOffset++)
		{
			var candidateDate = DateOnly.FromDateTime(now.Date).AddDays(dayOffset);
			if (!activeDays.Contains(candidateDate.DayOfWeek))
			{
				continue;
			}

			var candidate = ToDateTimeOffset(candidateDate, TimeOnly.FromDateTime(first.DateTime), now.Offset);
			if (candidate >= first && candidate > now)
			{
				return candidate;
			}
		}

		return null;
	}

	private static DateTimeOffset NextMonthly(DateTimeOffset first, DateTimeOffset now)
	{
		if (first > now)
		{
			return first;
		}

		var months = ((now.Year - first.Year) * 12) + now.Month - first.Month;
		var candidate = AddMonthsClamped(first, months);
		return candidate > now ? candidate : AddMonthsClamped(first, months + 1);
	}

	private static DateTimeOffset AddMonthsClamped(DateTimeOffset value, int months)
	{
		var target = value.AddMonths(months);
		return new DateTimeOffset(
			target.Year,
			target.Month,
			Math.Min(value.Day, DateTime.DaysInMonth(target.Year, target.Month)),
			value.Hour,
			value.Minute,
			value.Second,
			value.Offset);
	}
}
