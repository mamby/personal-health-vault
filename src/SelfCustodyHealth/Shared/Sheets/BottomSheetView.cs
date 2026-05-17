using Microsoft.Maui.Controls.Shapes;

namespace SelfCustodyHealth.Shared.Sheets;

public sealed class BottomSheetView : ContentView
{
	private const double DefaultMaximumHeightRatio = 0.74d;
	private const uint EntranceAnimationDuration = 280u;
	private const uint ExitAnimationDuration = 220u;

	private readonly BoxView _scrim;
	private readonly Border _sheet;
	private readonly ScrollView _scrollView;
	private readonly SemaphoreSlim _transitionLock = new(1, 1);

	public BottomSheetView()
	{
		IsVisible = false;
		InputTransparent = false;

		_scrim = new BoxView
		{
			Color = Colors.Black,
			Opacity = 0
		};
		_scrim.GestureRecognizers.Add(new TapGestureRecognizer
		{
			Command = new Command(async () => await HideAsync())
		});

		var handle = new Border
		{
			WidthRequest = 44,
			HeightRequest = 4,
			StrokeThickness = 0,
			StrokeShape = new RoundRectangle { CornerRadius = new CornerRadius(2) },
			Opacity = 0.55,
			HorizontalOptions = LayoutOptions.Center,
			BackgroundColor = Color.FromArgb("#B9C7C5")
		};

		_scrollView = new ScrollView
		{
			VerticalScrollBarVisibility = ScrollBarVisibility.Never
		};

		_sheet = new Border
		{
			Padding = new Thickness(20, 10, 20, 22),
			StrokeThickness = 0,
			StrokeShape = new RoundRectangle
			{
				CornerRadius = new CornerRadius(28, 28, 0, 0)
			},
			VerticalOptions = LayoutOptions.End,
			Content = new Grid
			{
				RowDefinitions =
				{
					new RowDefinition(GridLength.Auto),
					new RowDefinition(GridLength.Star)
				},
				Children =
				{
					new Grid
					{
						HeightRequest = 34,
						Children = { handle }
					},
					_scrollView
				}
			}
		};
		_sheet.SetAppThemeColor(Border.BackgroundColorProperty, Color.FromArgb("#FFFFFF"), Color.FromArgb("#090D10"));
		Grid.SetRow(_scrollView, 1);

		Content = new Grid
		{
			Children =
			{
				_scrim,
				_sheet
			}
		};
	}

	public double MaximumHeightRatio { get; set; } = DefaultMaximumHeightRatio;

	public View? SheetContent
	{
		get => _scrollView.Content;
		set => _scrollView.Content = value;
	}

	public bool IsOpen => IsVisible;

	public async Task ShowAsync(View content)
	{
		SheetContent = content;
		await ShowAsync();
	}

	public async Task ShowAsync()
	{
		await _transitionLock.WaitAsync();
		try
		{
			if (IsVisible)
			{
				return;
			}

			ConfigureLayout();
			IsVisible = true;
			_scrim.Opacity = 0;
			_sheet.TranslationY = GetHiddenTranslation();

			await Task.WhenAll(
				_scrim.FadeToAsync(0.42, EntranceAnimationDuration, Easing.CubicOut),
				_sheet.TranslateToAsync(0, 0, EntranceAnimationDuration, Easing.CubicOut));
		}
		finally
		{
			_transitionLock.Release();
		}
	}

	public async Task HideAsync()
	{
		await _transitionLock.WaitAsync();
		try
		{
			if (!IsVisible)
			{
				return;
			}

			await Task.WhenAll(
				_scrim.FadeToAsync(0, ExitAnimationDuration, Easing.CubicInOut),
				_sheet.TranslateToAsync(0, GetHiddenTranslation(), ExitAnimationDuration, Easing.CubicInOut));

			IsVisible = false;
			SheetContent = null;
		}
		finally
		{
			_transitionLock.Release();
		}
	}

	protected override void OnSizeAllocated(double width, double height)
	{
		base.OnSizeAllocated(width, height);
		ConfigureLayout();
	}

	private void ConfigureLayout()
	{
		var height = Height > 0 ? Height : DeviceDisplay.Current.MainDisplayInfo.Height / DeviceDisplay.Current.MainDisplayInfo.Density;
		_sheet.HeightRequest = Math.Max(320, height * MaximumHeightRatio);
	}

	private double GetHiddenTranslation() =>
		_sheet.HeightRequest > 0
			? _sheet.HeightRequest
			: DeviceDisplay.Current.MainDisplayInfo.Height / DeviceDisplay.Current.MainDisplayInfo.Density;
}
