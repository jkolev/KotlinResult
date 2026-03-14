package jkolev.result

import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class ResultTest {

    @Test
    fun `Success construction creates success result`() {
        val result = Result.Success(42)
        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun `Failure construction creates failure result`() {
        val result = Result.Failure("error")
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `companion success creates Success`() {
        val result = Result.success(42)
        assertTrue(result is Result.Success)
        assertEquals(42, result.value)
    }

    @Test
    fun `companion failure creates Failure`() {
        val result = Result.failure("error")
        assertTrue(result is Result.Failure)
        assertEquals("error", result.error)
    }

    @Test
    fun `getOrNull returns value on Success`() {
        val result: Result<Int, String> = Result.success(42)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `getOrNull returns null on Failure`() {
        val result: Result<Int, String> = Result.failure("error")
        assertNull(result.getOrNull())
    }

    @Test
    fun `errorOrNull returns error on Failure`() {
        val result: Result<Int, String> = Result.failure("error")
        assertEquals("error", result.errorOrNull())
    }

    @Test
    fun `errorOrNull returns null on Success`() {
        val result: Result<Int, String> = Result.success(42)
        assertNull(result.errorOrNull())
    }

    @Test
    fun `getOrElse with value returns value on Success`() {
        val result: Result<Int, String> = Result.success(42)
        assertEquals(42, result.getOrElse(0))
    }

    @Test
    fun `getOrElse with value returns default on Failure`() {
        val result: Result<Int, String> = Result.failure("error")
        assertEquals(0, result.getOrElse(0))
    }

    @Test
    fun `getOrElse with transform returns value on Success`() {
        val result: Result<Int, String> = Result.success(42)
        assertEquals(42, result.getOrElse { 0 })
    }

    @Test
    fun `getOrElse with transform returns transformed error on Failure`() {
        val result: Result<Int, String> = Result.failure("error")
        assertEquals(-1, result.getOrElse { -1 })
    }

    @Test
    fun `getOrThrow returns value on Success`() {
        val result: Result<Int, String> = Result.success(42)
        assertEquals(42, result.getOrThrow())
    }

    @Test
    fun `getOrThrow throws on Failure with default transform`() {
        val result: Result<Int, String> = Result.failure("error")
        val exception = assertFailsWith<RuntimeException> {
            result.getOrThrow()
        }
        assertEquals("error", exception.message)
    }

    @Test
    fun `getOrThrow throws on Failure with custom transform`() {
        val result: Result<Int, String> = Result.failure("custom error")
        val exception = assertFailsWith<IllegalStateException> {
            result.getOrThrow { IllegalStateException(it) }
        }
        assertEquals("custom error", exception.message)
    }

    @Test
    fun `map transforms Success value`() {
        val result: Result<Int, String> = Result.success(42)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Success)
        assertEquals(84, mapped.value)
    }

    @Test
    fun `map does not transform Failure`() {
        val result: Result<Int, String> = Result.failure("error")
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Failure)
        assertEquals("error", mapped.error)
    }

    @Test
    fun `mapError transforms Failure error`() {
        val result: Result<Int, String> = Result.failure("error")
        val mapped = result.mapError { it.uppercase() }
        assertTrue(mapped is Result.Failure)
        assertEquals("ERROR", mapped.error)
    }

    @Test
    fun `mapError does not transform Success`() {
        val result: Result<Int, String> = Result.success(42)
        val mapped = result.mapError { it.uppercase() }
        assertTrue(mapped is Result.Success)
        assertEquals(42, mapped.value)
    }

    @Test
    fun `flatMap chains Success results`() {
        val result: Result<Int, String> = Result.success(42)
        val chained = result.flatMap { Result.success(it * 2) }
        assertTrue(chained is Result.Success)
        assertEquals(84, chained.value)
    }

    @Test
    fun `flatMap can convert Success to Failure`() {
        val result: Result<Int, String> = Result.success(42)
        val chained = result.flatMap { Result.failure("error") }
        assertTrue(chained is Result.Failure)
        assertEquals("error", chained.error)
    }

    @Test
    fun `flatMap does not transform Failure`() {
        val result: Result<Int, String> = Result.failure("error")
        val chained = result.flatMap { Result.success(it * 2) }
        assertTrue(chained is Result.Failure)
        assertEquals("error", chained.error)
    }

    @Test
    fun `flatMapError chains Failure results`() {
        val result: Result<Int, String> = Result.failure("error")
        val chained = result.flatMapError { Result.failure(it.uppercase()) }
        assertTrue(chained is Result.Failure)
        assertEquals("ERROR", chained.error)
    }

    @Test
    fun `flatMapError can recover from Failure to Success`() {
        val result: Result<Int, String> = Result.failure("error")
        val chained = result.flatMapError { Result.success(0) }
        assertTrue(chained is Result.Success)
        assertEquals(0, chained.value)
    }

    @Test
    fun `flatMapError does not transform Success`() {
        val result: Result<Int, String> = Result.success(42)
        val chained = result.flatMapError { Result.success(0) }
        assertTrue(chained is Result.Success)
        assertEquals(42, chained.value)
    }

    @Test
    fun `onSuccess executes action on Success and returns self`() {
        var executed = false
        val result: Result<Int, String> = Result.success(42)
        val returned = result.onSuccess { executed = true }
        assertTrue(executed)
        assertSame(result, returned)
    }

    @Test
    fun `onSuccess does not execute action on Failure`() {
        var executed = false
        val result: Result<Int, String> = Result.failure("error")
        val returned = result.onSuccess { executed = true }
        assertFalse(executed)
        assertSame(result, returned)
    }

    @Test
    fun `onFailure executes action on Failure and returns self`() {
        var executed = false
        val result: Result<Int, String> = Result.failure("error")
        val returned = result.onFailure { executed = true }
        assertTrue(executed)
        assertSame(result, returned)
    }

    @Test
    fun `onFailure does not execute action on Success`() {
        var executed = false
        val result: Result<Int, String> = Result.success(42)
        val returned = result.onFailure { executed = true }
        assertFalse(executed)
        assertSame(result, returned)
    }

    @Test
    fun `onSuccess and onFailure can be chained`() {
        var successExecuted = false
        var failureExecuted = false
        Result.success(42)
            .onSuccess { successExecuted = true }
            .onFailure { failureExecuted = true }
        assertTrue(successExecuted)
        assertFalse(failureExecuted)
    }

    @Test
    fun `fold executes onSuccess for Success`() {
        val result: Result<Int, String> = Result.success(42)
        val folded = result.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: $it" }
        )
        assertEquals("success: 42", folded)
    }

    @Test
    fun `fold executes onFailure for Failure`() {
        val result: Result<Int, String> = Result.failure("error")
        val folded = result.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: $it" }
        )
        assertEquals("failure: error", folded)
    }

    @Test
    fun `catch returns Success when block succeeds`() {
        val result = Result.catch { 42 }
        assertTrue(result is Result.Success)
        assertEquals(42, result.value)
    }

    @Test
    fun `catch returns Failure when block throws Exception`() {
        val result = Result.catch { throw IllegalArgumentException("test") }
        assertTrue(result is Result.Failure)
        assertTrue(result.error is IllegalArgumentException)
        assertEquals("test", result.error.message)
    }

    @Test
    fun `catch rethrows CancellationException`() {
        assertFailsWith<CancellationException> {
            Result.catch { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `catch does not catch Error`() {
        assertFailsWith<OutOfMemoryError> {
            Result.catch { throw OutOfMemoryError("test") }
        }
    }

    @Test
    fun `railway oriented programming chain`() {
        fun validate(value: Int): Result<Int, String> =
            if (value > 5) Result.Success(value) else Result.Failure("too small")

        val initial: Result<Int, String> = Result.Success(5)
        val result = initial
            .map { it * 2 }
            .flatMap(::validate)
            .map { it + 10 }
            .onSuccess { println("Success: $it") }

        assertTrue(result is Result.Success)
        assertEquals(20, result.value)
    }

    @Test
    fun `railway oriented programming with failure`() {
        fun validate(value: Int): Result<Int, String> =
            if (value > 5) Result.Success(value) else Result.Failure("too small")

        val initial: Result<Int, String> = Result.Success(2)
        val result = initial
            .map { it * 2 }
            .flatMap(::validate)
            .map { it + 10 }

        assertTrue(result is Result.Failure)
        assertEquals("too small", result.error)
    }

    @Test
    fun `recovery from failure using flatMapError`() {
        fun recover(error: String): Result<Int, Nothing> = Result.Success(0)

        val initial: Result<Int, String> = Result.Failure("error")
        val result = initial
            .flatMapError(::recover)
            .map { it + 42 }

        assertTrue(result is Result.Success)
        assertEquals(42, result.value)
    }

    @Test
    fun `type changes through transformations`() {
        val initial: Result<String, Int> = Result.Success("hello")
        val result: Result<Int, String> = initial
            .map { it.length }
            .mapError { it.toString() }

        assertTrue(result is Result.Success)
        assertEquals(5, result.value)
    }
}