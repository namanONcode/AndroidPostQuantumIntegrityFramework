# Contributing to Android Post-Quantum Integrity Framework

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing.

## 🚀 Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 8.5+
- Android SDK (for testing example app)

### Setup

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/AndroidPostQuantumIntegrityFramework.git
   cd AndroidPostQuantumIntegrityFramework
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Run tests:
   ```bash
   ./gradlew test
   ```

## 📝 Code Style

We use [Spotless](https://github.com/diffplug/spotless) with Google Java Format for code formatting.

### Check formatting:
```bash
./gradlew spotlessCheck
```

### Apply formatting:
```bash
./gradlew spotlessApply
```

## 🔄 Pull Request Process

1. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and ensure:
   - All tests pass: `./gradlew test`
   - Code is formatted: `./gradlew spotlessApply`
   - Plugin validates: `./gradlew validatePlugins`

3. Commit your changes with a descriptive message:
   ```bash
   git commit -m "Add: description of your changes"
   ```

4. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

5. Open a Pull Request against the `master` branch

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests with Example App
```bash
cd example-android-app
./gradlew assembleDebug
```

## 📋 Commit Message Convention

- `Add:` for new features
- `Fix:` for bug fixes
- `Update:` for updates to existing features
- `Remove:` for removed features
- `Docs:` for documentation changes
- `Refactor:` for code refactoring
- `Test:` for test additions/modifications

## 🐛 Reporting Issues

When reporting issues, please include:

1. Description of the issue
2. Steps to reproduce
3. Expected behavior
4. Actual behavior
5. Environment details (OS, JDK version, Gradle version, AGP version)

## 📄 License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

## 💬 Questions?

Feel free to open an issue for any questions or discussions.

