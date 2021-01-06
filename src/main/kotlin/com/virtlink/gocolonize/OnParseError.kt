package com.virtlink.gocolonize

/**
 * Specifies what to do when a parse error is encountered.
 */
enum class OnParseError {
    /** Log the error as an error and abort. */
    Fatal,
    /** Log the error as a warning. */
    Warn,
    /** Ignore the error and don't log it. */
    Ignore,
}