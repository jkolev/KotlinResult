package jkolev.result

import kotlin.coroutines.cancellation.CancellationException

/**
 * A lightweight, dual-typed Result<V, E> that fixes the problems
 * with Kotlin's stdlib Result:
 *  - Error type is a generic E, not hardcoded to Throwable
 *  - Full monadic API: map, mapError, flatMap, flatMapError
 *  - Coroutine-safe: no runCatching that swallows CancellationException
 *  - Exhaustive: sealed class forces handling of both branches
 */
sealed class Result<out V, out E> {

    data class Success<out V>(val value: V) : Result<V, Nothing>()
    data class Failure<out E>(val error: E) : Result<Nothing, E>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): V? = (this as? Success)?.value
    fun errorOrNull(): E? = (this as? Failure)?.error

    fun getOrElse(default: @UnsafeVariance V): V =
        if (this is Success) value else default

    fun getOrElse(transform: (E) -> @UnsafeVariance V): V = when (this) {
        is Success -> value
        is Failure -> transform(error)
    }

    /** Throws if the result is a Failure. Only use at app boundaries. */
    fun getOrThrow(transform: (E) -> Throwable = { RuntimeException(it.toString()) }): V =
        when (this) {
            is Success -> value
            is Failure -> throw transform(error)
        }

    /** Transform the success value, leaving failures untouched. */
    fun <T> map(transform: (V) -> T): Result<T, E> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    /** Transform the error value, leaving successes untouched. */
    fun <F> mapError(transform: (E) -> F): Result<V, F> = when (this) {
        is Success -> this
        is Failure -> Failure(transform(error))
    }

    /** Chain a computation that itself returns a Result (Railway Oriented Programming). */
    fun <T> flatMap(transform: (V) -> Result<T, @UnsafeVariance E>): Result<T, E> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    /** Chain a recovery path on failure. */
    fun <F> flatMapError(transform: (E) -> Result<@UnsafeVariance V, F>): Result<V, F> = when (this) {
        is Success -> this
        is Failure -> transform(error)
    }

    fun onSuccess(action: (V) -> Unit): Result<V, E> {
        if (this is Success) action(value)
        return this
    }

    fun onFailure(action: (E) -> Unit): Result<V, E> {
        if (this is Failure) action(error)
        return this
    }

    /** Collapse both branches into a single value. */
    fun <T> fold(
        onSuccess: (V) -> T,
        onFailure: (E) -> T,
    ): T = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }

    companion object {
        fun <V> success(value: V): Result<V, Nothing> = Success(value)
        fun <E> failure(error: E): Result<Nothing, E> = Failure(error)

        /**
         * Wraps a throwing block. Unlike runCatching, it only catches Exception
         * (not Error), and it re-throws CancellationException to stay
         * coroutine-safe and structured-concurrency-friendly.
         */
        inline fun <V> catch(block: () -> V): Result<V, Exception> =
            try {
                success(block())
            } catch (e: Exception) {
                // Never swallow coroutine cancellation
                if (e is CancellationException) throw e
                failure(e)
            }

        /**
         * Suspend-aware version of catch. Wraps a suspend block and captures exceptions
         * while preserving structured concurrency by re-throwing CancellationException.
         *
         * Example:
         * ```
         * suspend fun fetchUser(id: Int): Result<User, Exception> = Result.catchSuspend {
         *     delay(100)
         *     api.getUser(id)
         * }
         * ```
         */
        suspend inline fun <V> catchSuspend(
            crossinline block: suspend () -> V
        ): Result<V, Exception> = try {
            success(block())
        } catch (e: Exception) {
            // Never swallow coroutine cancellation
            if (e is CancellationException) throw e
            failure(e)
        }

        /**
         * Wraps a throwing block with a custom error type mapper.
         * Re-throws CancellationException to stay coroutine-safe.
         *
         * Example:
         * ```
         * Result.catching({ DomainError.NetworkError(it.message) }) {
         *     api.fetch()
         * }
         * ```
         */
        inline fun <V, E> catching(
            mapError: (Exception) -> E,
            block: () -> V
        ): Result<V, E> = try {
            success(block())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            failure(mapError(e))
        }

        /**
         * Suspend-aware version of catching with custom error mapper.
         *
         * Example:
         * ```
         * suspend fun fetchUser(id: Int): Result<User, DomainError> =
         *     Result.catchingSuspend({ DomainError.NetworkError(it.message) }) {
         *         api.getUser(id)
         *     }
         * ```
         */
        suspend inline fun <V, E> catchingSuspend(
            crossinline mapError: (Exception) -> E,
            crossinline block: suspend () -> V
        ): Result<V, E> = try {
            success(block())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            failure(mapError(e))
        }
    }
}

//region Extension methods

fun <V> V.asSuccess(): Result<V, Nothing> = Result.Success(this)
fun <E> E.asFailure(): Result<Nothing, E> = Result.Failure(this)

/** Combines two Results. Fails fast on the first Failure. */
fun <A, B, E> Result<A, E>.zip(
    other: Result<B, E>,
): Result<Pair<A, B>, E> = flatMap { a -> other.map { b -> a to b } }

/** Combines two Results into a new value. Fails fast on the first Failure. */
fun <A, B, C, E> Result<A, E>.zipWith(
    other: Result<B, E>,
    transform: (A, B) -> C,
): Result<C, E> = flatMap { a -> other.map { b -> transform(a, b) } }

/** Returns the first Failure, or a Success of all values if none failed. */
fun <V, E> List<Result<V, E>>.sequence(): Result<List<V>, E> {
    val values = mutableListOf<V>()
    for (result in this) {
        when (result) {
            is Result.Success -> values += result.value
            is Result.Failure -> return result
        }
    }
    return values.asSuccess()
}

/** Maps each element and short-circuits on the first Failure. */
fun <T, V, E> List<T>.traverseResult(transform: (T) -> Result<V, E>): Result<List<V>, E> =
    map(transform).sequence()

// endregion