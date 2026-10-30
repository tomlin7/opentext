package com.tom7.opentext.data

import kotlinx.serialization.json.Json

/** Lenient JSON for server payloads; unknown fields are ignored. */
val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = false
}
