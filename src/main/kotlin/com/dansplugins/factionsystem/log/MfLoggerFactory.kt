package com.dansplugins.factionsystem.log

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.LogManager

class MfLoggerFactory : ILoggerFactory {

    private val loggerMap = ConcurrentHashMap<String, Logger>()

    init {
        LogManager.getLogManager()
    }

    override fun getLogger(name: String): Logger {
        val bukkitLogger = loggerMap[name]
        return if (bukkitLogger != null) {
            bukkitLogger
        } else {
            val newInstance = MfLoggerAdapter(name)
            loggerMap.putIfAbsent(name, newInstance) ?: newInstance
        }
    }
}
