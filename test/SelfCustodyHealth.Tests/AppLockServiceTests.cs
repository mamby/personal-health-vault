using SelfCustodyHealth.Security;

namespace SelfCustodyHealth.Tests;

public sealed class AppLockServiceTests
{
	[Fact]
	public async Task EnableAsync_WithSuccessfulUnlock_EnablesWithoutLocking()
	{
		var settings = new InMemoryAppLockSettingsStore();
		var service = new AppLockService(new FakeDeviceUnlockService(true), settings);

		var result = await service.EnableAsync(TestContext.Current.CancellationToken);

		Assert.True(result.Succeeded);
		Assert.True(service.IsEnabled);
		Assert.False(service.IsLocked);
	}

	[Fact]
	public async Task MarkResumed_AfterBackground_LocksWhenEnabled()
	{
		var settings = new InMemoryAppLockSettingsStore(true);
		var service = new AppLockService(new FakeDeviceUnlockService(true), settings);

		await service.UnlockAsync(TestContext.Current.CancellationToken);
		service.MarkBackgrounded();
		service.MarkResumed();

		Assert.True(service.IsLockRequired);
	}

	private sealed class FakeDeviceUnlockService(bool shouldSucceed) : IDeviceUnlockService
	{
		public Task<DeviceUnlockAvailability> GetAvailabilityAsync(CancellationToken cancellationToken) =>
			Task.FromResult(DeviceUnlockAvailability.Available);

		public Task<DeviceUnlockResult> RequestUnlockAsync(string reason, CancellationToken cancellationToken) =>
			Task.FromResult(shouldSucceed
				? DeviceUnlockResult.Success("Unlocked.")
				: DeviceUnlockResult.Failure(DeviceUnlockAvailability.Available, "Denied."));

		public void CancelPendingUnlock()
		{
		}
	}
}
