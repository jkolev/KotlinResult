# Coroutine Support Guide

This library is **fully coroutine-safe** and provides specialized functions for working with suspend functions.

## Key Principle: Never Capture `CancellationException`

The most important rule when using Result with coroutines:

**WRONG - Breaks structured concurrency:**
```kotlin
suspend fun fetchData(): Result<Data, Exception> = try {
    api.fetch().asSuccess()
} catch (e: Exception) {
    e.asFailure()  // Captures CancellationException!
}
```

**CORRECT - Use built-in helpers:**
```kotlin
suspend fun fetchData(): Result<Data, Exception> = Result.catchSuspend {
    api.fetch()
}
```

## API Reference

### `catchSuspend` - For Exception Error Types

Wraps a suspend block, automatically handling `CancellationException`:

```kotlin
suspend fun <V> catchSuspend(block: suspend () -> V): Result<V, Exception>
```

**Example:**
```kotlin
suspend fun fetchUser(id: Int): Result<User, Exception> = Result.catchSuspend {
    delay(100)  // Coroutine operations are safe
    userApi.getUser(id)
}
```

**What it does:**
- Returns `Result.Success` on success
- Returns `Result.Failure` for exceptions
- Re-throws `CancellationException` (preserves structured concurrency)
- Re-throws `Error` types (OutOfMemoryError, etc.)

### `catchingSuspend` - For Custom Error Types

Map exceptions to your domain error type:

```kotlin
suspend fun <V, E> catchingSuspend(
    mapError: (Exception) -> E,
    block: suspend () -> V
): Result<V, E>
```

**Example:**
```kotlin
sealed interface DomainError {
    data class NetworkError(val message: String?) : DomainError
    data class NotFound(val id: String) : DomainError
}

suspend fun fetchUser(id: Int): Result<User, DomainError> =
    Result.catchingSuspend({ DomainError.NetworkError(it.message) }) {
        if (id <= 0) throw IllegalArgumentException("Invalid ID")
        userApi.getUser(id)
    }
```

### `catching` / `catch` - Non-Suspend Versions

For blocking code (same CancellationException handling):

```kotlin
// Standard exception type
fun <V> catch(block: () -> V): Result<V, Exception>

// Custom error type
fun <V, E> catching(mapError: (Exception) -> E, block: () -> V): Result<V, E>
```

## Common Patterns

### 1. Simple API Call

```kotlin
suspend fun getUser(id: Int): Result<User, Exception> = Result.catchSuspend {
    httpClient.get("/users/$id")
}
```

### 2. Custom Error Types

```kotlin
sealed interface ApiError {
    data class NetworkError(val cause: String?) : ApiError
    data class Unauthorized(val message: String) : ApiError
    data class NotFound(val resource: String) : ApiError
}

suspend fun fetchUser(id: Int): Result<User, ApiError> =
    Result.catchingSuspend({ ApiError.NetworkError(it.message) }) {
        val response = httpClient.get("/users/$id")
        when (response.status) {
            401 -> throw IllegalStateException("Unauthorized")
            404 -> throw NoSuchElementException("User not found")
            else -> response.body<User>()
        }
    }
```

### 3. Chaining Suspend Operations

```kotlin
suspend fun processUser(id: Int): Result<String, ApiError> {
    val userResult = fetchUser(id)
    val profileResult = when (userResult) {
        is Result.Success -> fetchProfile(userResult.value.id)
        is Result.Failure -> return userResult
    }
    return profileResult.map { "Processed: ${it.name}" }
}
```

### 4. Parallel Operations with `coroutineScope`

```kotlin
suspend fun getUserDashboard(id: Int): Result<Dashboard, ApiError> = coroutineScope {
    val userDeferred = async { fetchUser(id) }
    val postsDeferred = async { fetchPosts(id) }
    val friendsDeferred = async { fetchFriends(id) }

    val user = userDeferred.await()
    val posts = postsDeferred.await()
    val friends = friendsDeferred.await()

    // Combine results
    user.zipWith(posts) { u, p -> u to p }
        .zipWith(friends) { (u, p), f -> Dashboard(u, p, f) }
}
```

### 5. Timeout with Context

```kotlin
suspend fun fetchWithTimeout(id: Int): Result<User, ApiError> =
    withTimeout(5000) {
        Result.catchingSuspend({ ApiError.NetworkError(it.message) }) {
            delay(100)
            userApi.getUser(id)
        }
    }
```

### 6. Retry Logic

```kotlin
suspend fun fetchWithRetry(
    id: Int,
    maxRetries: Int = 3
): Result<User, ApiError> {
    repeat(maxRetries) { attempt ->
        val result = fetchUser(id)
        if (result is Result.Success) return result
        if (attempt < maxRetries - 1) delay(1000 * (attempt + 1))
    }
    return Result.failure(ApiError.NetworkError("Max retries exceeded"))
}
```

## Testing with Coroutines

Use `kotlinx-coroutines-test` for testing:

```kotlin
@Test
fun `test async operation`() = runTest {
    val result = Result.catchSuspend {
        delay(100)
        "test value"
    }

    assertTrue(result is Result.Success)
    assertEquals("test value", result.value)
}
```

## Migration from `runCatching`

### Before (stdlib - Unsafe with coroutines):
```kotlin
suspend fun fetchData(): Result<Data> = runCatching {
    api.fetch()  // Can capture CancellationException!
}
```

### After (this library - Safe):
```kotlin
suspend fun fetchData(): Result<Data, Exception> = Result.catchSuspend {
    api.fetch()  // CancellationException is properly propagated
}
```

## Why This Matters

When using Kotlin coroutines, `CancellationException` is used for **structured concurrency**. If you accidentally catch it:

- Coroutine cancellation doesn't propagate
- `withTimeout` won't work correctly
- Parent job cancellation won't stop children
- Resource cleanup may not happen

This library **guarantees** that `CancellationException` is always re-thrown, making it safe to use in any coroutine context.

## Performance Notes

- `catchSuspend` and `catchingSuspend` are `inline` functions - **no runtime overhead**
- No additional allocations beyond the Result wrapper
- Same performance as hand-written try-catch blocks

## Dependencies

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
```

## See Also

- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Cancellation and Timeouts](https://kotlinlang.org/docs/cancellation-and-timeouts.html)
- [Railway-Oriented Programming](https://fsharpforfunandprofit.com/rop/)