using SelfCustodyHealth.Domain;
using SelfCustodyHealth.Shared;
using SelfCustodyHealth.Shared.Sheets;
using SelfCustodyHealth.Shared.Ui;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth.Features.Vault;

public sealed class VaultPage(IDocumentStore documentStore, HealthDataService dataService) : ContentPage
{
	private readonly BottomSheetView _detailsSheet = new();
	private readonly VerticalStackLayout _documentList = new() { Spacing = 10 };
	private readonly SearchBar _searchBar = new() { Placeholder = "Search local documents" };
	private DocumentCategory _selectedCategory = DocumentCategory.All;

	protected override async void OnAppearing()
	{
		base.OnAppearing();
		await LoadDocumentsAsync();
	}

	public void Build()
	{
		_searchBar.TextChanged += async (_, _) => await LoadDocumentsAsync();

		var content = Ui.PageStack(
			Ui.PageTitle("Vault"),
			Ui.Body("Encrypted local vault. Add document storage is scaffolded; demo records are clearly marked."),
			_searchBar,
			CreateCategoryBar(),
			_documentList,
			Ui.Card(new VerticalStackLayout
			{
				Spacing = 8,
				Children =
				{
					Ui.SectionTitle("Add document"),
					Ui.Body("Document import will encrypt records before saving them locally."),
					Ui.Muted("No sensitive health data is stored in plaintext files.")
				}
			}));

		Content = new Grid
		{
			Children =
			{
				Ui.Scroll(content),
				_detailsSheet
			}
		};
	}

	private ScrollView CreateCategoryBar()
	{
		var row = new HorizontalStackLayout { Spacing = 8 };
		foreach (var category in Enum.GetValues<DocumentCategory>())
		{
			var button = Ui.SecondaryButton(HealthText.CategoryName(category));
			button.Clicked += async (_, _) =>
			{
				_selectedCategory = category;
				await LoadDocumentsAsync();
			};
			row.Children.Add(button);
		}

		return new ScrollView
		{
			Orientation = ScrollOrientation.Horizontal,
			HorizontalScrollBarVisibility = ScrollBarVisibility.Never,
			Content = row
		};
	}

	private async Task LoadDocumentsAsync()
	{
		if (Content is null)
		{
			Build();
		}

		var documents = await documentStore.SearchAsync(_selectedCategory, _searchBar.Text, CancellationToken.None);
		_documentList.Children.Clear();

		var snapshot = await dataService.GetSnapshotAsync();
		if (dataService.IsDemoData)
		{
			_documentList.Children.Add(Ui.SoftContainer(Ui.Muted("Demo data only. Your encrypted vault will be created when you save real records.")));
		}

		foreach (var document in documents)
		{
			_documentList.Children.Add(CreateDocumentRow(document));
		}

		if (documents.Count is 0)
		{
			_documentList.Children.Add(Ui.Card(Ui.Body("No documents match this filter.")));
		}
	}

	private Border CreateDocumentRow(MedicalDocument document)
	{
		var badge = Ui.Badge(document.IsDemo ? "Demo" : "Local", document.IsDemo ? Color.FromArgb("#A86A22") : Color.FromArgb("#2E7D55"));
		var grid = new Grid
		{
			ColumnDefinitions =
			{
				new ColumnDefinition(GridLength.Star),
				new ColumnDefinition(GridLength.Auto)
			},
			ColumnSpacing = 12,
			Children =
			{
				new VerticalStackLayout
				{
					Spacing = 4,
					Children =
					{
						Ui.SectionTitle(document.Title),
						Ui.Muted($"{HealthText.CategoryName(document.Category)} / {HealthText.FormatDate(document.DocumentDate)}"),
						Ui.Muted(document.Source)
					}
				},
				badge
			}
		};

		Grid.SetColumn(badge, 1);
		var row = Ui.Row(grid);

		row.GestureRecognizers.Add(new TapGestureRecognizer
		{
			Command = new Command(async () => await ShowDocumentAsync(document))
		});

		return row;
	}

	private async Task ShowDocumentAsync(MedicalDocument document)
	{
		var content = new VerticalStackLayout
		{
			Spacing = 14,
			Children =
			{
				Ui.Muted(HealthText.CategoryName(document.Category).ToUpperInvariant()),
				Ui.PageTitle(document.Title),
				Ui.Body(document.Notes ?? "No notes."),
				Ui.Muted($"Source: {document.Source}"),
				Ui.Muted($"Date: {HealthText.FormatDate(document.DocumentDate)}"),
				Ui.SoftContainer(Ui.Muted("This app organizes records and does not provide medical advice."))
			}
		};

		await _detailsSheet.ShowAsync(content);
	}
}
