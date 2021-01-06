package com.virtlink.gocolonize

import mu.KotlinLogging
import java.util.*

/**
 * Manages properties.
 */
object PropertiesManager {

    private val log = KotlinLogging.logger {}
    private val properties = Properties()

    /** The application version. */
    val version: String get() = properties.getProperty("version")!!
    /** The application revision. */
    val revision: String get() = properties.getProperty("revision")!!
    /** The application full revision. */
    val fullRevision: String get() = properties.getProperty("full-revision")!!
    /** The application build time. */
    val buildTime: String get() = properties.getProperty("build-time")!!

    init {
        load()
    }

    /**
     * Loads the properties.
     */
    private fun load() {
        log.trace("Loading properties...")
        PropertiesManager::class.java.getResourceAsStream("/version.properties").use {
            properties.load(it)
        }
        log.debug { "Loaded properties:\n  " + properties.entries.joinToString("\n  ") { (k, v) -> "$k: $v" } }
    }

}