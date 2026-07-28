# Security Policy

## Supported Versions

Only the latest release of **Visor PDF** on the `main` branch is actively supported with security updates.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0.0 | :x:                |

## Reporting a Vulnerability

We take the security of our application seriously. If you discover a security vulnerability, please follow these steps:

1. **Do not create a public issue.**
2. Send a report detailing the vulnerability, impact, and steps to reproduce.
3. We will acknowledge receipt of your vulnerability report within 48 hours and provide a timeline for resolution.

## Security Best Practices Enforced

- **Zero Invasiveness**: This application strictly adheres to Android Scoped Storage using `ContentResolver` and Storage Access Framework (`ACTION_OPEN_DOCUMENT`). It does not request `MANAGE_EXTERNAL_STORAGE`.
- **Credential Protection**: Private signing keys (`*.jks`, `*.keystore`) and local configuration files (`local.properties`) are strictly ignored via `.gitignore` and must never be committed to version control.
- **Dependency Integrity**: Dependencies are monitored for security vulnerabilities via GitHub Dependabot.
