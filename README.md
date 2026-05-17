# Self-Custody Health

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![.NET](https://img.shields.io/badge/.NET-11-512BD4)](https://dotnet.microsoft.com/)
[![C#](https://img.shields.io/badge/C%23-preview-239120.svg)](https://learn.microsoft.com/dotnet/csharp/)
[![MAUI](https://img.shields.io/badge/MAUI-native-512BD4.svg)](https://learn.microsoft.com/dotnet/maui/)

A local-first personal health records app for iOS, Android, Windows, and macOS.

## Purpose

Self-Custody Health helps people organize their health documents, treatments, appointments, reminders, vaccinations, emergency contacts, and health summary.

It is not a diagnostic app. It does not provide medical advice, medical decisions, prescriptions, triage, emergency recommendations, or clinical certification claims.

## Privacy Principles

- Your health data stays on this device by default.
- Saved vault data is encrypted locally.
- Backup and sync are user-controlled future capabilities.
- Backup data, when added, must be encrypted before leaving the device.
- No backend health-data storage is included.
- No cloud AI is included.
- No ads or tracking are included by default.

## Supported Platforms

- iOS
- Android
- Windows
- macOS through Mac Catalyst

## Tech Stack

- .NET 11 preview
- C# preview
- .NET MAUI
- xUnit v3 on Microsoft Testing Platform
- Authenticated encryption with `AesGcm`
- Platform biometric/device unlock APIs

## Getting Started

```bash
git clone https://github.com/mamby/self-custody-health.git
cd self-custody-health
dotnet restore SelfCustodyHealth.sln
dotnet build src/SelfCustodyHealth/SelfCustodyHealth.csproj -f net11.0-windows10.0.19041.0
dotnet test test/SelfCustodyHealth.Tests/SelfCustodyHealth.Tests.csproj
```

## Current Limitations

- Document import is scaffolded but not connected to real files yet.
- Demo records are shown before a user-created encrypted vault exists.
- Backup, export, OCR, classification, and local summaries are placeholders.
- Biometric/device unlock is an app access gate; it is not a claim that encryption keys are hardware-bound.
