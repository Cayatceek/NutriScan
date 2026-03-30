package com.nutriscan.app.util

import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

const val BRANDED_LOADING_DURATION_MILLIS = 2000L

sealed interface BrandedLoadingOutcome<out T> {
    data class Success<T>(val value: T) : BrandedLoadingOutcome<T>
    data class Error(val throwable: Throwable) : BrandedLoadingOutcome<Nothing>
    data object Timeout : BrandedLoadingOutcome<Nothing>
}

suspend fun <T> runBrandedLoadingGate(
    timeoutMillis: Long = BRANDED_LOADING_DURATION_MILLIS,
    block: suspend () -> T
): BrandedLoadingOutcome<T> {
    val startedAt = SystemClock.elapsedRealtime()

    return try {
        val value = withTimeoutOrNull(timeoutMillis) {
            block()
        }
        waitForRemainingDuration(startedAt, timeoutMillis)
        if (value != null) {
            BrandedLoadingOutcome.Success(value)
        } else {
            BrandedLoadingOutcome.Timeout
        }
    } catch (throwable: Throwable) {
        waitForRemainingDuration(startedAt, timeoutMillis)
        BrandedLoadingOutcome.Error(throwable)
    }
}

private suspend fun waitForRemainingDuration(
    startedAt: Long,
    timeoutMillis: Long
) {
    val elapsed = SystemClock.elapsedRealtime() - startedAt
    val remaining = timeoutMillis - elapsed
    if (remaining > 0) {
        delay(remaining)
    }
}
