package jkolev.result

import kotlin.test.*

class ResultExtensionsTest {

    @Test
    fun `asSuccess creates Success`() {
        val result = 42.asSuccess()
        assertTrue(result is Result.Success)
        assertEquals(42, result.value)
    }

    @Test
    fun `asFailure creates Failure`() {
        val result = "error".asFailure()
        assertTrue(result is Result.Failure)
        assertEquals("error", result.error)
    }

    @Test
    fun `zip combines two Success results`() {
        val result1: Result<Int, String> = Result.Success(10)
        val result2: Result<String, String> = Result.Success("hello")
        val combined = result1.zip(result2)

        assertTrue(combined is Result.Success)
        assertEquals(10 to "hello", combined.value)
    }

    @Test
    fun `zip fails fast on first Failure`() {
        val result1: Result<Int, String> = Result.Failure("error1")
        val result2: Result<String, String> = Result.Success("hello")
        val combined = result1.zip(result2)

        assertTrue(combined is Result.Failure)
        assertEquals("error1", combined.error)
    }

    @Test
    fun `zip fails on second Failure`() {
        val result1: Result<Int, String> = Result.Success(10)
        val result2: Result<String, String> = Result.Failure("error2")
        val combined = result1.zip(result2)

        assertTrue(combined is Result.Failure)
        assertEquals("error2", combined.error)
    }

    @Test
    fun `zipWith combines and transforms two Success results`() {
        val result1: Result<Int, String> = Result.Success(10)
        val result2: Result<Int, String> = Result.Success(5)
        val combined = result1.zipWith(result2) { a, b -> a + b }

        assertTrue(combined is Result.Success)
        assertEquals(15, combined.value)
    }

    @Test
    fun `zipWith fails fast on first Failure`() {
        val result1: Result<Int, String> = Result.Failure("error1")
        val result2: Result<Int, String> = Result.Success(5)
        val combined = result1.zipWith(result2) { a, b -> a + b }

        assertTrue(combined is Result.Failure)
        assertEquals("error1", combined.error)
    }

    @Test
    fun `sequence returns Success of all values when all succeed`() {
        val results = listOf(
            Result.Success(1),
            Result.Success(2),
            Result.Success(3)
        )
        val sequenced = results.sequence()

        assertTrue(sequenced is Result.Success)
        assertEquals(listOf(1, 2, 3), sequenced.value)
    }

    @Test
    fun `sequence returns first Failure`() {
        val results: List<Result<Int, String>> = listOf(
            Result.Success(1),
            Result.Failure("error1"),
            Result.Failure("error2")
        )
        val sequenced = results.sequence()

        assertTrue(sequenced is Result.Failure)
        assertEquals("error1", sequenced.error)
    }

    @Test
    fun `sequence handles empty list`() {
        val results: List<Result<Int, String>> = emptyList()
        val sequenced = results.sequence()

        assertTrue(sequenced is Result.Success)
        assertEquals(emptyList(), sequenced.value)
    }

    @Test
    fun `traverseResult maps and sequences successfully`() {
        val numbers = listOf(1, 2, 3)
        val result = numbers.traverseResult { n -> Result.Success(n * 2) }

        assertTrue(result is Result.Success)
        assertEquals(listOf(2, 4, 6), result.value)
    }

    @Test
    fun `traverseResult short-circuits on first Failure`() {
        val numbers = listOf(1, 2, 3)
        val result: Result<List<Int>, String> = numbers.traverseResult { n ->
            if (n == 2) Result.Failure("error at $n")
            else Result.Success(n * 2)
        }

        assertTrue(result is Result.Failure)
        assertEquals("error at 2", result.error)
    }

    @Test
    fun `traverseResult handles empty list`() {
        val numbers = emptyList<Int>()
        val result = numbers.traverseResult { n -> Result.Success(n * 2) }

        assertTrue(result is Result.Success)
        assertEquals(emptyList(), result.value)
    }
}