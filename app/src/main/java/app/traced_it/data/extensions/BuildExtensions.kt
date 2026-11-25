package app.traced_it.data.extensions

import android.os.Build

fun hasClipboardEditor(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
