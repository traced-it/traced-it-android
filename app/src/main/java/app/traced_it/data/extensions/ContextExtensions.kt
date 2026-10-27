package app.traced_it.data.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.TimeZone
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "ContextExtensions"

fun Context.openInputStreamOrNull(uri: Uri): InputStream? {
    val inputStream = try {
        contentResolver.openInputStream(uri)
    } catch (_: FileNotFoundException) {
        Log.e(TAG, "Input stream URI $uri could not be opened")
        return null
    }
    if (inputStream == null) {
        Log.e(TAG, "Input stream URI $uri recently crashed")
        return null
    }
    return inputStream
}

fun Context.openOutputStreamOrNull(uri: Uri): OutputStream? {
    val outputStream = try {
        contentResolver.openOutputStream(uri)
    } catch (_: FileNotFoundException) {
        Log.e(TAG, "Output stream URI $uri could not be opened")
        return null
    }
    if (outputStream == null) {
        Log.e(TAG, "Output stream URI $uri recently crashed")
        return null
    }
    return outputStream
}

/**
 * A flow that emits the current system time zone by listening to [Intent.ACTION_TIMEZONE_CHANGED].
 */
fun Context.systemTimeZoneFlow(): Flow<TimeZone> = callbackFlow {
    val appContext = applicationContext
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(TimeZone.currentSystemDefault())
        }
    }
    ContextCompat.registerReceiver(
        appContext, receiver,
        IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    trySend(TimeZone.currentSystemDefault())
    awaitClose { appContext.unregisterReceiver(receiver) }
}.distinctUntilChanged()
