# Contributing to Kotlin Result

Thank you for your interest in contributing! This document provides guidelines for contributing to this project.

## How to Contribute

### Reporting Issues

- Check if the issue already exists in the [issue tracker](../../issues)
- Provide a clear description of the problem
- Include code samples that reproduce the issue
- Specify your Kotlin version and environment

### Suggesting Features

- Open an issue with the `enhancement` label
- Clearly describe the use case and benefits
- Consider backward compatibility

### Submitting Pull Requests

1. **Fork the repository** and create your branch from `main`

2. **Make your changes**
   - Write clear, readable code following Kotlin conventions
   - Add tests for new functionality
   - Ensure all tests pass: `./gradlew test`
   - Update documentation if needed

3. **Commit your changes**
   - Use clear, descriptive commit messages
   - Reference related issues (e.g., "Fixes #123")

4. **Submit a pull request**
   - Provide a clear description of the changes
   - Link to related issues
   - Wait for code review

## Development Setup

### Prerequisites

- JDK 21 or higher
- Kotlin 2.3.0+

### Building the Project

```bash
# Clone the repository
git clone https://github.com/yourusername/kotlin-result.git
cd kotlin-result

# Run tests
./gradlew test

# Build the project
./gradlew build
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests ResultTest

# Run with coverage
./gradlew test jacocoTestReport
```

## Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions focused and concise
- Add comments for complex logic

## Testing Guidelines

- Write tests for all new functionality
- Aim for high test coverage
- Test both success and failure paths
- Test edge cases
- Use descriptive test names (backtick syntax for readability)

Example:
```kotlin
@Test
fun `map transforms Success value`() {
    val result: Result<Int, String> = Result.success(42)
    val mapped = result.map { it * 2 }
    assertTrue(mapped is Result.Success)
    assertEquals(84, mapped.value)
}
```

## Documentation

- Update README.md for user-facing changes
- Add KDoc comments for public API
- Include usage examples for new features

## Questions?

Feel free to open an issue for any questions or reach out to the maintainers.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.