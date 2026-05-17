using SelfCustodyHealth.Crypto;
using SelfCustodyHealth.Domain;
using SelfCustodyHealth.Storage;

namespace SelfCustodyHealth.Tests;

public sealed class EncryptedVaultStoreTests
{
	[Fact]
	public async Task SaveThenLoad_RoundTripsEncryptedVault()
	{
		var directory = CreateTempDirectory();
		var vaultPath = Path.Combine(directory, "vault.sch");
		var store = CreateStore(vaultPath);
		var snapshot = DemoHealthVaultFactory.Create(new DateTimeOffset(2026, 5, 16, 12, 0, 0, TimeSpan.Zero));
		var cancellationToken = TestContext.Current.CancellationToken;

		await store.SaveAsync(snapshot, cancellationToken);
		var raw = await File.ReadAllTextAsync(vaultPath, cancellationToken);
		var loaded = await store.LoadAsync(cancellationToken);

		Assert.NotNull(loaded);
		Assert.Equal(snapshot.Documents.Count, loaded.Documents.Count);
		Assert.DoesNotContain("Annual blood panel", raw, StringComparison.OrdinalIgnoreCase);
		Directory.Delete(directory, recursive: true);
	}

	[Fact]
	public async Task Delete_RemovesVaultAndKey()
	{
		var directory = CreateTempDirectory();
		var vaultPath = Path.Combine(directory, "vault.sch");
		var keyProvider = new InMemoryVaultKeyProvider();
		var store = new EncryptedVaultStore(vaultPath, keyProvider, new AesGcmEncryptionService());
		var cancellationToken = TestContext.Current.CancellationToken;

		await store.SaveAsync(DemoHealthVaultFactory.Create(DateTimeOffset.UtcNow), cancellationToken);
		await store.DeleteAsync(cancellationToken);

		Assert.False(File.Exists(vaultPath));
		Assert.Null(await store.LoadAsync(cancellationToken));
		Directory.Delete(directory, recursive: true);
	}

	private static EncryptedVaultStore CreateStore(string vaultPath) =>
		new(vaultPath, new InMemoryVaultKeyProvider(), new AesGcmEncryptionService());

	private static string CreateTempDirectory()
	{
		var path = Path.Combine(Path.GetTempPath(), $"sch-{Guid.NewGuid():N}");
		Directory.CreateDirectory(path);
		return path;
	}
}
