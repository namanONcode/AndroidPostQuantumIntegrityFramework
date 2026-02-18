# Security Policy

## Supported Versions

The following versions of Android Post-Quantum Integrity Framework are currently supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please follow responsible disclosure practices.

### How to Report

**Please DO NOT report security vulnerabilities through public GitHub issues.**

Instead, report them via one of the following methods:

1. **GitHub Security Advisories** (Preferred)
   - Go to the [Security tab](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/security/advisories)
   - Click "Report a vulnerability"
   - Fill out the form with details

2. **Email**
   - Send details to the repository owner via GitHub profile contact

### What to Include

Please include the following information in your report:

- Type of vulnerability (e.g., cryptographic weakness, injection, etc.)
- Full path of the affected source file(s)
- Location of the affected code (line numbers)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if available)
- Potential impact of the vulnerability

### Response Timeline

| Action | Timeline |
| ------ | -------- |
| Initial acknowledgment | Within 48 hours |
| Status update | Within 7 days |
| Vulnerability assessment | Within 14 days |
| Fix release (if confirmed) | Within 30 days |

### What to Expect

- **Accepted vulnerabilities**: We will work with you to understand and resolve the issue. You will be credited in the security advisory (unless you prefer to remain anonymous).

- **Declined reports**: If we determine the report is not a security vulnerability, we will provide an explanation.

### Safe Harbor

We consider security research conducted in accordance with this policy to be:

- Authorized under the Computer Fraud and Abuse Act (CFAA)
- Exempt from DMCA restrictions
- Lawful and helpful to the security community

We will not pursue legal action against researchers who follow this policy.

## Security Best Practices

When using this plugin, follow these recommendations:

### Build Environment
- Use trusted CI/CD environments
- Verify plugin checksums when possible
- Keep Gradle and dependencies updated

### Key Management (ML-KEM)
- Store ML-KEM private keys securely (HSM/Keystore)
- Rotate keys periodically
- Never commit keys to version control

### Server Verification
- Use HTTPS for all verification endpoints
- Validate server certificates
- Implement rate limiting on verification endpoints

## Security-Related Dependencies

This project uses the following security-critical dependencies:

| Dependency | Purpose | Security Notes |
| ---------- | ------- | -------------- |
| Bouncy Castle | ML-KEM/Kyber crypto | Keep updated for CVE fixes |
| Gson | JSON parsing | Input validation applied |

## Changelog

Security fixes will be documented in release notes with CVE identifiers when applicable.

---

Thank you for helping keep Android Post-Quantum Integrity Framework secure!

