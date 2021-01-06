package com.virtlink.gocolonize

import java.lang.RuntimeException

/**
 * A fatal parse error exception.
 */
class FatalParseErrorException @JvmOverloads constructor(msg: String? = null, cause: Throwable? = null)
    : RuntimeException(msg ?: "A fatal parse error occurred.", cause) {
}