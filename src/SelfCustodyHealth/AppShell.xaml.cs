using Microsoft.Extensions.DependencyInjection;
using SelfCustodyHealth.Features.Appointments;
using SelfCustodyHealth.Features.Dashboard;
using SelfCustodyHealth.Features.HealthSummary;
using SelfCustodyHealth.Features.Lock;
using SelfCustodyHealth.Features.Medications;
using SelfCustodyHealth.Features.Settings;
using SelfCustodyHealth.Features.Vault;
using SelfCustodyHealth.Security;

namespace SelfCustodyHealth;

public partial class AppShell : Shell
{
	private readonly IServiceProvider _services;
	private readonly AppLockCoordinator _appLockCoordinator;
	private readonly SemaphoreSlim _lockPresentation = new(1, 1);

	public AppShell(IServiceProvider services)
	{
		_services = services;
		_appLockCoordinator = services.GetRequiredService<AppLockCoordinator>();
		InitializeComponent();
		Items.Add(CreateMainTabs());
	}

	public Task ShowLockIfRequiredAsync()
	{
		var decision = _appLockCoordinator.RequestPresentation();
		return ApplyLockPresentationAsync(decision);
	}

	public async Task ApplyLockPresentationAsync(AppLockPresentationDecision decision)
	{
		if (decision is { ShouldShowLock: false, ShouldStartAutomaticUnlock: false })
		{
			return;
		}

		LockPage? lockPageToUnlock = null;

		await _lockPresentation.WaitAsync();
		try
		{
			if (decision.ShouldShowLock)
			{
				var lockPage = _services.GetRequiredService<LockPage>();
				lockPage.Unlocked += (_, _) => _appLockCoordinator.NotifyLockDismissed();

				try
				{
					await Navigation.PushModalAsync(lockPage);
					lockPageToUnlock = decision.ShouldStartAutomaticUnlock ? lockPage : null;
				}
				catch
				{
					_appLockCoordinator.NotifyLockPresentationFailed();
					throw;
				}
			}
			else if (decision.ShouldStartAutomaticUnlock)
			{
				lockPageToUnlock = Navigation.ModalStack.OfType<LockPage>().LastOrDefault();
			}
		}
		finally
		{
			_lockPresentation.Release();
		}

		if (lockPageToUnlock is not null)
		{
			lockPageToUnlock.Dispatcher.Dispatch(() =>
			{
				_ = lockPageToUnlock.StartAutomaticUnlockAsync();
			});
		}
	}

	private TabBar CreateMainTabs()
	{
		var tabs = new TabBar();
		tabs.Items.Add(CreateContent<DashboardPage>("Dashboard", "dashboard"));
		tabs.Items.Add(CreateContent<VaultPage>("Vault", "vault"));
		tabs.Items.Add(CreateContent<HealthSummaryPage>("Summary", "health-summary"));
		tabs.Items.Add(CreateContent<MedicationsPage>("Meds", "medications"));
		tabs.Items.Add(CreateContent<AppointmentsPage>("Visits", "appointments"));
		tabs.Items.Add(CreateContent<SettingsPage>("Settings", "settings"));

		return tabs;
	}

	private ShellContent CreateContent<TPage>(string title, string route)
		where TPage : Page
	{
		return new ShellContent
		{
			Title = title,
			Route = route,
			ContentTemplate = new DataTemplate(() => _services.GetRequiredService<TPage>())
		};
	}
}
