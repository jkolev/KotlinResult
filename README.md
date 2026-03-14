# Kotlin Result

A lightweight, dual-typed `Result<V, E>` implementation for Kotlin that fixes the limitations of the standard library's `Result` type.

## Why This Library?

Kotlin's standard library `Result<T>` has several limitations:

- ❌ Error type is hardcoded to `Throwable`
- ❌ Limited monadic API (no `mapError`, `flatMapError`)
- ❌ `runCatching` swallows `CancellationException` (breaks structured concurrency)
- ❌ Not truly exhaustive (can use `getOrThrow()` without handling failures)

This library provides:

- ✅ Generic error type `E` - use sealed classes, enums, or any type
- ✅ Full monadic API: `map`, `mapError`, `flatMap`, `flatMapError`
- ✅ Coroutine-safe: explicit `CancellationException` re-throwing
- ✅ Exhaustive: sealed class forces handling both branches
- ✅ Railway-oriented programming support

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("jkolev.result:kotlin-result:1.0.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'jkolev.result:kotlin-result:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>jkolev.result</groupId>
    <artifactId>kotlin-result</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```kotlin
import jkolev.result.*

// Define your domain errors
sealed interface UserError {
    data object NotFound : UserError
    data object Unauthorized : UserError
    data class ValidationFailed(val field: String, val reason: String) : UserError
}

// Functions that return Result
fun findUser(id: Int): Result<User, UserError> =
    if (id > 0) User(id, "user@example.com", 30).asSuccess()
    else UserError.NotFound.asFailure()

fun validateAge(user: User): Result<User, UserError> =
    if (user.age >= 18) user.asSuccess()
    else UserError.ValidationFailed("age", "must be 18 or older").asFailure()

// Railway-oriented programming
fun processUser(id: Int): Result<String, UserError> =
    findUser(id)
        .flatMap(::validateAge)
        .map { user -> "Welcome, ${user.email}!" }

// Handle results
processUser(1)
    .onSuccess { println(it) }
    .onFailure { error ->
        when (error) {
            UserError.NotFound -> println("404: user not found")
            UserError.Unauthorized -> println("403: access denied")
            is UserError.ValidationFailed -> println("400: ${error.field} — ${error.reason}")
        }
    }
```

## Core API

### Construction

```kotlin
// Direct construction
val success: Result<Int, String> = Result.Success(42)
val failure: Result<Int, String> = Result.Failure("error")

// Companion methods
val success = Result.success(42)
val failure = Result.failure("error")

// Extension methods
val success = 42.asSuccess()
val failure = "error".asFailure()

// Catching exceptions (coroutine-safe)
val result: Result<Int, Exception> = Result.catch { "42".toInt() }
```

### Transformation

```kotlin
result.map { it * 2 }              // Transform success value
result.mapError { it.uppercase() } // Transform error value
result.flatMap { doSomething(it) } // Chain operations
result.flatMapError { recover() }  // Recovery path
```

### Unwrapping

```kotlin
result.getOrNull()              // V? (null on failure)
result.errorOrNull()            // E? (null on success)
result.getOrElse(42)            // V (default value)
result.getOrElse { 0 }          // V (computed default)
result.getOrThrow()             // V or throws RuntimeException
result.getOrThrow { MyEx(it) }  // V or throws custom exception
```

### Side Effects

```kotlin
result
    .onSuccess { println("Success: $it") }
    .onFailure { println("Error: $it") }
```

### Folding

```kotlin
val message = result.fold(
    onSuccess = { "Got: $it" },
    onFailure = { "Failed: $it" }
)
```

## Advanced Features

### Combining Results

```kotlin
// Combine two results into a Pair
val combined: Result<Pair<Int, String>, Error> =
    result1.zip(result2)

// Combine and transform
val combined: Result<String, Error> =
    result1.zipWith(result2) { a, b -> "$a-$b" }
```

### Collection Operations

```kotlin
// Convert List<Result> to Result<List>
val results: List<Result<Int, Error>> = listOf(...)
val sequenced: Result<List<Int>, Error> = results.sequence()

// Map and sequence in one operation
val numbers = listOf(1, 2, 3)
val result: Result<List<String>, Error> =
    numbers.traverseResult { processNumber(it) }
```

## Railway-Oriented Programming

Chain operations that can fail, short-circuiting on the first error:

```kotlin
fun processOrder(orderId: String): Result<Receipt, OrderError> =
    validateOrderId(orderId)
        .flatMap { fetchOrder(it) }
        .flatMap { checkInventory(it) }
        .flatMap { processPayment(it) }
        .flatMap { shipOrder(it) }
        .map { generateReceipt(it) }
```

## Coroutine Safety

Unlike Kotlin's `runCatching`, our `Result.catch` properly handles coroutine cancellation:

```kotlin
// Safe for use in coroutines
suspend fun fetchData(): Result<Data, Exception> = Result.catch {
    // If the coroutine is cancelled, CancellationException is re-thrown
    // It's NOT captured as a Result.Failure
    api.fetch()
}
```

## Testing

The library includes comprehensive tests (53 tests covering all functionality). See `ResultTest.kt` and `ResultExtensionsTest.kt` for examples.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Credits

Inspired by functional programming patterns and Railway-Oriented Programming by Scott Wlaschin.