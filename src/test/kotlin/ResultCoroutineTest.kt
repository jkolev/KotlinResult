package jkolev.result

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ResultCoroutineTest {

    @Test
    fun `catchSuspend returns Success when suspend block succeeds`() = runTest {
        val result = Result.catchSuspend {
            delay(10)
            42
        }
        assertTrue(result is Result.Success)
        assertEquals(42, result.value)
    }

    @Test
    fun `catchSuspend returns Failure when suspend block throws Exception`() = runTest {
        val result = Result.catchSuspend {
            delay(10)
            throw IllegalArgumentException("test error")
        }
        assertTrue(result is Result.Failure)
        assertTrue(result.error is IllegalArgumentException)
        assertEquals("test error", result.error.message)
    }

    @Test
    fun `catchSuspend rethrows CancellationException`() = runTest {
        assertFailsWith<CancellationException> {
            Result.catchSuspend {
                delay(10)
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun `catchSuspend does not catch Error`() = runTest {
        assertFailsWith<OutOfMemoryError> {
            Result.catchSuspend {
                delay(10)
                throw OutOfMemoryError("test")
            }
        }
    }

    // ─── catching ─────────────────────────────────────────────────────────────

    sealed interface DomainError {
        data class NetworkError(val message: String?) : DomainError
    }

    @Test
    fun `catching maps exception to custom error type`() {
        val result = Result.catching(
            mapError = { DomainError.NetworkError(it.message) }
        ) {
            throw IllegalStateException("network failed")
        }

        assertTrue(result is Result.Failure)
        assertEquals("network failed", result.error.message)
    }

    @Test
    fun `catching returns Success when block succeeds`() {
        val result = Result.catching(
            mapError = { DomainError.NetworkError(it.message) }
        ) {
            "success"
        }

        assertTrue(result is Result.Success)
        assertEquals("success", result.value)
    }

    @Test
    fun `catching rethrows CancellationException`() {
        assertFailsWith<CancellationException> {
            Result.catching(
                mapError = { DomainError.NetworkError(it.message) }
            ) {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun `catchingSuspend maps exception to custom error type`() = runTest {
        val result = Result.catchingSuspend(
            mapError = { DomainError.NetworkError(it.message) }
        ) {
            delay(10)
            throw IllegalStateException("network failed")
        }

        assertTrue(result is Result.Failure)
        assertEquals("network failed", result.error.message)
    }

    @Test
    fun `catchingSuspend returns Success when suspend block succeeds`() = runTest {
        val result = Result.catchingSuspend(
            mapError = { DomainError.NetworkError(it.message) }
        ) {
            delay(10)
            "success"
        }

        assertTrue(result is Result.Success)
        assertEquals("success", result.value)
    }

    @Test
    fun `catchingSuspend rethrows CancellationException`() = runTest {
        assertFailsWith<CancellationException> {
            Result.catchingSuspend(
                mapError = { DomainError.NetworkError(it.message) }
            ) {
                delay(10)
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun `catchingSuspend does not catch Error`() = runTest {
        assertFailsWith<OutOfMemoryError> {
            Result.catchingSuspend(
                mapError = { DomainError.NetworkError(it.message) }
            ) {
                delay(10)
                throw OutOfMemoryError("test")
            }
        }
    }

    @Test
    fun `suspend function returning Result can be chained`() = runTest {
        suspend fun fetchUser(id: Int): Result<String, Exception> = Result.catchSuspend {
            delay(10)
            if (id > 0) "User$id" else throw IllegalArgumentException("Invalid ID")
        }

        suspend fun validateUser(user: String): Result<String, Exception> = Result.catchSuspend {
            delay(10)
            user.ifEmpty { throw IllegalStateException("Empty user") }
        }

        // Chain suspend functions manually
        val validatedResult = when (val userResult = fetchUser(1)) {
            is Result.Success -> validateUser(userResult.value)
            is Result.Failure -> userResult
        }
        val finalResult = validatedResult.map { it.uppercase() }

        assertTrue(finalResult is Result.Success)
        assertEquals("USER1", finalResult.value)
    }

    @Test
    fun `custom error types with suspend functions`() = runTest {
        suspend fun fetchData(id: Int): Result<String, DomainError> =
            Result.catchingSuspend({ DomainError.NetworkError(it.message) }) {
                delay(10)
                if (id > 0) "Data$id"
                else throw IllegalArgumentException("Invalid ID")
            }

        val successResult = fetchData(1)
        assertTrue(successResult is Result.Success)
        assertEquals("Data1", successResult.value)

        val failureResult = fetchData(-1)
        assertTrue(failureResult is Result.Failure)
        assertTrue(failureResult.error is DomainError.NetworkError)
    }
}