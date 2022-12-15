package com.dansplugins.factionsystem.log

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class MfSlf4jServiceProvider : SLF4JServiceProvider {

    companion object {
        const val REQUESTED_API_VERSION = "2.0.0-alpha7"
    }

    private lateinit var loggerFactory: ILoggerFactory
    private lateinit var markerFactory: IMarkerFactory
    private lateinit var mdcAdapter: MDCAdapter

    override fun getLoggerFactory() = loggerFactory
    override fun getMarkerFactory() = markerFactory
    override fun getMDCAdapter() = mdcAdapter
    override fun getRequesteApiVersion() = REQUESTED_API_VERSION

    override fun initialize() {
        loggerFactory = MfLoggerFactory()
        markerFactory = BasicMarkerFactory()
        mdcAdapter = BasicMDCAdapter()
    }
}
