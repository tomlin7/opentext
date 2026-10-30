package com.tom7.opentext.data

import android.content.Context

/** Small preference store for the last local folder and remote connection. */
class WorkspaceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("opentext", Context.MODE_PRIVATE)

    var lastLocalTree: String?
        get() = prefs.getString("local_tree", null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString("local_tree", value).apply()
        }

    var lastLocalLabel: String?
        get() = prefs.getString("local_label", null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString("local_label", value).apply()
        }

    var remoteUrl: String?
        get() = prefs.getString("remote_url", null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString("remote_url", value).apply()
        }

    var remotePassword: String
        get() = prefs.getString("remote_password", "").orEmpty()
        set(value) {
            prefs.edit().putString("remote_password", value).apply()
        }

    var remoteDirectory: String?
        get() = prefs.getString("remote_directory", null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString("remote_directory", value).apply()
        }
}
