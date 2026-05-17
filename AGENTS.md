# Self-Custody Health Instructions

This repository uses one production project: `src/SelfCustodyHealth/SelfCustodyHealth.csproj`.

## Rules

- Use modern .NET, C#, and .NET MAUI APIs first.
- Keep all production code inside the single MAUI project.
- Keep tests under `test`.
- Use Windows CRLF line endings.
- Do not introduce a backend, web/WASM app, cloud AI, ads, or tracking.
- Do not store sensitive health data in plaintext files.
- Keep platform-specific code isolated under clear service boundaries.
- Do not make medical, diagnostic, emergency, regulatory, or clinical certification claims.

## Architecture

- `Domain` contains pure health models and logic.
- `Crypto` contains authenticated encryption services and key contracts.
- `Storage` contains encrypted local vault persistence and future backup/local AI contracts.
- `Security` contains app lock and device unlock services.
- `Features` contains MAUI pages.
- `Shared` contains reusable UI components.

## Testing

Use xUnit v3 on Microsoft Testing Platform. Add meaningful tests for domain logic, encryption, storage, and app-lock behavior.
