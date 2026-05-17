using Microsoft.Extensions.DependencyInjection;

namespace SelfCustodyHealth;

public partial class App : Application
{
	private readonly IServiceProvider _services;

	public App(IServiceProvider services)
	{
		_services = services;
		InitializeComponent();
	}

	protected override Window CreateWindow(IActivationState? activationState)
	{
		var shell = new AppShell(_services);
		var window = new Window(shell);
		var appLockCoordinator = _services.GetRequiredService<Security.AppLockCoordinator>();

		window.Activated += async (_, _) =>
		{
			var decision = appLockCoordinator.Activated();
			await shell.ApplyLockPresentationAsync(decision);
		};
		window.Resumed += (_, _) => appLockCoordinator.Resumed();
		window.Stopped += (_, _) => appLockCoordinator.Stopped();
		window.Destroying += (_, _) => appLockCoordinator.Destroying();

		return window;
	}
}
