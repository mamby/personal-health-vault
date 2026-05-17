using Microsoft.Maui.Controls.Shapes;

namespace SelfCustodyHealth.Shared.Ui;

internal static class Ui
{
	public static Label PageTitle(string text) =>
		StyledLabel(text, "PageTitleLabel");

	public static Label SectionTitle(string text) =>
		StyledLabel(text, "SectionTitleLabel");

	public static Label Body(string text) =>
		StyledLabel(text, "BodyTextLabel");

	public static Label Muted(string text) =>
		StyledLabel(text, "MutedTextLabel");

	public static Label Metric(string text) =>
		StyledLabel(text, "MetricValueLabel");

	public static Border Card(View content)
	{
		var border = new Border
		{
			Content = content
		};
		ApplyStyle(border, "HealthCardBorder");
		return border;
	}

	public static Border Badge(string text, Color? background = null)
	{
		var label = new Label
		{
			Text = text,
			FontFamily = "OpenSansSemibold",
			FontSize = 12,
			TextColor = Colors.White
		};

		var border = new Border
		{
			Content = label,
			BackgroundColor = background ?? Color.FromArgb("#0E7C73")
		};
		ApplyStyle(border, "StatusBadgeBorder");
		return border;
	}

	public static Border Row(View content)
	{
		var border = new Border
		{
			Content = content
		};
		ApplyStyle(border, "DocumentRowBorder");
		return border;
	}

	public static Button SecondaryButton(string text) =>
		new()
		{
			Text = text,
			Style = GetStyle("SecondaryButton")
		};

	public static VerticalStackLayout PageStack(params View[] children)
	{
		var stack = new VerticalStackLayout
		{
			Padding = new Thickness(20, 18, 20, 28),
			Spacing = 16
		};

		foreach (var child in children)
		{
			stack.Children.Add(child);
		}

		return stack;
	}

	public static ScrollView Scroll(View content) =>
		new()
		{
			Content = content,
			VerticalScrollBarVisibility = ScrollBarVisibility.Never
		};

	public static Border SoftContainer(View content)
	{
		return new Border
		{
			Content = content,
			Padding = 14,
			StrokeThickness = 0,
			StrokeShape = new RoundRectangle { CornerRadius = new CornerRadius(16) },
			BackgroundColor = Color.FromArgb("#1A0E7C73")
		};
	}

	private static Label StyledLabel(string text, string styleKey) =>
		new()
		{
			Text = text,
			Style = GetStyle(styleKey)
		};

	private static void ApplyStyle(VisualElement element, string styleKey) =>
		element.Style = GetStyle(styleKey);

	private static Style GetStyle(string styleKey) =>
		(Style)Application.Current!.Resources[styleKey];
}
