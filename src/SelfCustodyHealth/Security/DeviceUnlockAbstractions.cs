namespace SelfCustodyHealth.Security;

public enum DeviceUnlockAvailability
{
	Available,
	Unavailable,
	NotEnrolled,
	DisabledByPolicy,
	LockedOut
}

public sealed record DeviceUnlockResult(
	bool Succeeded,
	DeviceUnlockAvailability Availability,
	string Message)
{
	public static DeviceUnlockResult Success(string message) =>
		new(true, DeviceUnlockAvailability.Available, message);

	public static DeviceUnlockResult Failure(DeviceUnlockAvailability availability, string message) =>
		new(false, availability, message);
}

public interface IDeviceUnlockService
{
	Task<DeviceUnlockAvailability> GetAvailabilityAsync(CancellationToken cancellationToken);

	Task<DeviceUnlockResult> RequestUnlockAsync(string reason, CancellationToken cancellationToken);

	void CancelPendingUnlock();
}
