package com.dansplugins.factionsystem.log

import org.bukkit.Bukkit
import org.slf4j.Marker
import org.slf4j.event.EventConstants
import org.slf4j.event.LoggingEvent
import org.slf4j.helpers.MarkerIgnoringBase
import org.slf4j.helpers.MessageFormatter
import org.slf4j.spi.LocationAwareLogger
import java.time.Instant
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.Level as JulLevel

class MfLoggerAdapter(name: String) : MarkerIgnoringBase(), LocationAwareLogger {

    init {
        this.name = name
    }

    private val logger: Logger
        get() = Bukkit.getPluginManager().getPlugin("MedievalFactions")?.logger
            ?: Bukkit.getLogger()

    /**
     * Is this logger instance enabled for the FINEST level?
     *
     * @return True if this Logger is enabled for level FINEST, false otherwise.
     */
    override fun isTraceEnabled(): Boolean {
        return logger.isLoggable(JulLevel.FINEST)
    }

    /**
     * Log a message object at level FINEST.
     *
     * @param msg
     * - the message object to be logged
     */
    override fun trace(msg: String) {
        if (logger.isLoggable(JulLevel.FINEST)) {
            log(SELF, JulLevel.FINEST, msg, null)
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and
     * argument.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for level FINEST.
     *
     *
     * @param format
     * the format string
     * @param arg
     * the argument
     */
    override fun trace(format: String?, arg: Any?) {
        if (logger.isLoggable(JulLevel.FINEST)) {
            val ft = MessageFormatter.format(format, arg)
            log(SELF, JulLevel.FINEST, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINEST level.
     *
     *
     * @param format
     * the format string
     * @param arg1
     * the first argument
     * @param arg2
     * the second argument
     */
    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (logger.isLoggable(JulLevel.FINEST)) {
            val ft = MessageFormatter.format(format, arg1, arg2)
            log(SELF, JulLevel.FINEST, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINEST level.
     *
     *
     * @param format
     * the format string
     * @param argArray
     * an array of arguments
     */
    override fun trace(format: String?, vararg argArray: Any?) {
        if (logger.isLoggable(JulLevel.FINEST)) {
            val ft = MessageFormatter.arrayFormat(format, argArray)
            log(SELF, JulLevel.FINEST, ft.message, ft.throwable)
        }
    }

    /**
     * Log an exception (throwable) at level FINEST with an accompanying message.
     *
     * @param msg
     * the message accompanying the exception
     * @param t
     * the exception (throwable) to log
     */
    override fun trace(msg: String, t: Throwable?) {
        if (logger.isLoggable(JulLevel.FINEST)) {
            log(SELF, JulLevel.FINEST, msg, t)
        }
    }

    /**
     * Is this logger instance enabled for the FINE level?
     *
     * @return True if this Logger is enabled for level FINE, false otherwise.
     */
    override fun isDebugEnabled(): Boolean {
        return logger.isLoggable(JulLevel.FINE)
    }

    /**
     * Log a message object at level FINE.
     *
     * @param msg
     * - the message object to be logged
     */
    override fun debug(msg: String) {
        if (logger.isLoggable(JulLevel.FINE)) {
            log(SELF, JulLevel.FINE, msg, null)
        }
    }

    /**
     * Log a message at level FINE according to the specified format and argument.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for level FINE.
     *
     *
     * @param format
     * the format string
     * @param arg
     * the argument
     */
    override fun debug(format: String?, arg: Any?) {
        if (logger.isLoggable(JulLevel.FINE)) {
            val ft = MessageFormatter.format(format, arg)
            log(SELF, JulLevel.FINE, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at level FINE according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINE level.
     *
     *
     * @param format
     * the format string
     * @param arg1
     * the first argument
     * @param arg2
     * the second argument
     */
    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (logger.isLoggable(JulLevel.FINE)) {
            val ft = MessageFormatter.format(format, arg1, arg2)
            log(SELF, JulLevel.FINE, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at level FINE according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINE level.
     *
     *
     * @param format
     * the format string
     * @param argArray
     * an array of arguments
     */
    override fun debug(format: String?, vararg argArray: Any?) {
        if (logger.isLoggable(JulLevel.FINE)) {
            val ft = MessageFormatter.arrayFormat(format, argArray)
            log(SELF, JulLevel.FINE, ft.message, ft.throwable)
        }
    }

    /**
     * Log an exception (throwable) at level FINE with an accompanying message.
     *
     * @param msg
     * the message accompanying the exception
     * @param t
     * the exception (throwable) to log
     */
    override fun debug(msg: String, t: Throwable?) {
        if (logger.isLoggable(JulLevel.FINE)) {
            log(SELF, JulLevel.FINE, msg, t)
        }
    }

    /**
     * Is this logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     */
    override fun isInfoEnabled(): Boolean {
        return logger.isLoggable(JulLevel.INFO)
    }

    /**
     * Log a message object at the INFO level.
     *
     * @param msg
     * - the message object to be logged
     */
    override fun info(msg: String) {
        if (logger.isLoggable(JulLevel.INFO)) {
            log(SELF, JulLevel.INFO, msg, null)
        }
    }

    /**
     * Log a message at level INFO according to the specified format and argument.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     *
     *
     * @param format
     * the format string
     * @param arg
     * the argument
     */
    override fun info(format: String?, arg: Any?) {
        if (logger.isLoggable(JulLevel.INFO)) {
            val ft = MessageFormatter.format(format, arg)
            log(SELF, JulLevel.INFO, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     *
     *
     * @param format
     * the format string
     * @param arg1
     * the first argument
     * @param arg2
     * the second argument
     */
    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (logger.isLoggable(JulLevel.INFO)) {
            val ft = MessageFormatter.format(format, arg1, arg2)
            log(SELF, JulLevel.INFO, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at level INFO according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     *
     *
     * @param format
     * the format string
     * @param argArray
     * an array of arguments
     */
    override fun info(format: String?, vararg argArray: Any?) {
        if (logger.isLoggable(JulLevel.INFO)) {
            val ft = MessageFormatter.arrayFormat(format, argArray)
            log(SELF, JulLevel.INFO, ft.message, ft.throwable)
        }
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying
     * message.
     *
     * @param msg
     * the message accompanying the exception
     * @param t
     * the exception (throwable) to log
     */
    override fun info(msg: String, t: Throwable?) {
        if (logger.isLoggable(JulLevel.INFO)) {
            log(SELF, JulLevel.INFO, msg, t)
        }
    }

    /**
     * Is this logger instance enabled for the WARNING level?
     *
     * @return True if this Logger is enabled for the WARNING level, false
     * otherwise.
     */
    override fun isWarnEnabled(): Boolean {
        return logger.isLoggable(JulLevel.WARNING)
    }

    /**
     * Log a message object at the WARNING level.
     *
     * @param msg
     * - the message object to be logged
     */
    override fun warn(msg: String) {
        if (logger.isLoggable(JulLevel.WARNING)) {
            log(SELF, JulLevel.WARNING, msg, null)
        }
    }

    /**
     * Log a message at the WARNING level according to the specified format and
     * argument.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARNING level.
     *
     *
     * @param format
     * the format string
     * @param arg
     * the argument
     */
    override fun warn(format: String?, arg: Any?) {
        if (logger.isLoggable(JulLevel.WARNING)) {
            val ft = MessageFormatter.format(format, arg)
            log(SELF, JulLevel.WARNING, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at the WARNING level according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARNING level.
     *
     *
     * @param format
     * the format string
     * @param arg1
     * the first argument
     * @param arg2
     * the second argument
     */
    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (logger.isLoggable(JulLevel.WARNING)) {
            val ft = MessageFormatter.format(format, arg1, arg2)
            log(SELF, JulLevel.WARNING, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at level WARNING according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARNING level.
     *
     *
     * @param format
     * the format string
     * @param argArray
     * an array of arguments
     */
    override fun warn(format: String?, vararg argArray: Any?) {
        if (logger.isLoggable(JulLevel.WARNING)) {
            val ft = MessageFormatter.arrayFormat(format, argArray)
            log(SELF, JulLevel.WARNING, ft.message, ft.throwable)
        }
    }

    /**
     * Log an exception (throwable) at the WARNING level with an accompanying
     * message.
     *
     * @param msg
     * the message accompanying the exception
     * @param t
     * the exception (throwable) to log
     */
    override fun warn(msg: String, t: Throwable?) {
        if (logger.isLoggable(JulLevel.WARNING)) {
            log(SELF, JulLevel.WARNING, msg, t)
        }
    }

    /**
     * Is this logger instance enabled for level SEVERE?
     *
     * @return True if this Logger is enabled for level SEVERE, false otherwise.
     */
    override fun isErrorEnabled(): Boolean {
        return logger.isLoggable(JulLevel.SEVERE)
    }

    /**
     * Log a message object at the SEVERE level.
     *
     * @param msg
     * - the message object to be logged
     */
    override fun error(msg: String) {
        if (logger.isLoggable(JulLevel.SEVERE)) {
            log(SELF, JulLevel.SEVERE, msg, null)
        }
    }

    /**
     * Log a message at the SEVERE level according to the specified format and
     * argument.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the SEVERE level.
     *
     *
     * @param format
     * the format string
     * @param arg
     * the argument
     */
    override fun error(format: String?, arg: Any?) {
        if (logger.isLoggable(JulLevel.SEVERE)) {
            val ft = MessageFormatter.format(format, arg)
            log(SELF, JulLevel.SEVERE, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at the SEVERE level according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the SEVERE level.
     *
     *
     * @param format
     * the format string
     * @param arg1
     * the first argument
     * @param arg2
     * the second argument
     */
    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (logger.isLoggable(JulLevel.SEVERE)) {
            val ft = MessageFormatter.format(format, arg1, arg2)
            log(SELF, JulLevel.SEVERE, ft.message, ft.throwable)
        }
    }

    /**
     * Log a message at level SEVERE according to the specified format and
     * arguments.
     *
     *
     *
     * This form avoids superfluous object creation when the logger is disabled
     * for the SEVERE level.
     *
     *
     * @param format
     * the format string
     * @param arguments
     * an array of arguments
     */
    override fun error(format: String?, vararg arguments: Any?) {
        if (logger.isLoggable(JulLevel.SEVERE)) {
            val ft = MessageFormatter.arrayFormat(format, arguments)
            log(SELF, JulLevel.SEVERE, ft.message, ft.throwable)
        }
    }

    /**
     * Log an exception (throwable) at the SEVERE level with an accompanying
     * message.
     *
     * @param msg
     * the message accompanying the exception
     * @param t
     * the exception (throwable) to log
     */
    override fun error(msg: String, t: Throwable?) {
        if (logger.isLoggable(JulLevel.SEVERE)) {
            log(SELF, JulLevel.SEVERE, msg, t)
        }
    }

    /**
     * Log the message at the specified level with the specified throwable if any.
     * This method creates a LogRecord and fills in caller date before calling
     * this instance's JDK14 logger.
     *
     * See bug report #13 for more details.
     *
     * @param level
     * @param msg
     * @param t
     */
    private fun log(callerFQCN: String, level: JulLevel, msg: String, t: Throwable?) {
        // millis and thread are filled by the constructor
        val record = LogRecord(level, msg)
        record.loggerName = getName()
        record.thrown = t
        // Note: parameters in record are not set because SLF4J only
        // supports a single formatting style
        fillCallerData(callerFQCN, record)
        logger.log(record)
    }

    var SELF: String = MfLoggerAdapter::class.java.name
    var SUPER = MarkerIgnoringBase::class.java.name

    /**
     * Fill in caller data if possible.
     *
     * @param record
     * The record to update
     */
    private fun fillCallerData(callerFQCN: String, record: LogRecord) {
        val steArray = Throwable().stackTrace
        var selfIndex = -1
        for (i in steArray.indices) {
            val className = steArray[i].className
            if (className.equals(callerFQCN) || className.equals(SUPER)) {
                selfIndex = i
                break
            }
        }
        var found = -1
        for (i in selfIndex + 1 until steArray.size) {
            val className = steArray[i].className
            if (!(className.equals(callerFQCN) || className.equals(SUPER))) {
                found = i
                break
            }
        }
        if (found != -1) {
            val ste = steArray[found]
            // setting the class name has the side effect of setting
            // the needToInferCaller variable to false.
            record.sourceClassName = ste.className
            record.sourceMethodName = ste.methodName
        }
    }

    override fun log(
        marker: Marker?,
        callerFQCN: String,
        level: Int,
        message: String,
        argArray: Array<Any?>?,
        t: Throwable?
    ) {
        val julLevel: JulLevel = slf4jLevelIntToJULLevel(level)
        // the logger.isLoggable check avoids the unconditional
        // construction of location data for disabled log
        // statements. As of 2008-07-31, callers of this method
        // do not perform this check. See also
        // http://jira.qos.ch/browse/SLF4J-81
        if (logger.isLoggable(julLevel)) {
            log(callerFQCN, julLevel, message, t)
        }
    }

    private fun slf4jLevelIntToJULLevel(slf4jLevelInt: Int): JulLevel {
        val julLevel = when (slf4jLevelInt) {
            LocationAwareLogger.TRACE_INT -> JulLevel.FINEST
            LocationAwareLogger.DEBUG_INT -> JulLevel.FINE
            LocationAwareLogger.INFO_INT -> JulLevel.INFO
            LocationAwareLogger.WARN_INT -> JulLevel.WARNING
            LocationAwareLogger.ERROR_INT -> JulLevel.SEVERE
            else -> throw IllegalStateException("Level number $slf4jLevelInt is not recognized.")
        }
        return julLevel
    }

    /**
     * @since 1.7.15
     */
    fun log(event: LoggingEvent) {
        val julLevel: JulLevel = slf4jLevelIntToJULLevel(event.level.toInt())
        if (logger.isLoggable(julLevel)) {
            val record = eventToRecord(event, julLevel)
            logger.log(record)
        }
    }

    private fun eventToRecord(event: LoggingEvent, julLevel: JulLevel): LogRecord {
        val format = event.message
        val arguments = event.argumentArray
        val ft = MessageFormatter.arrayFormat(format, arguments)
        require(!(ft.throwable != null && event.throwable != null)) { "both last element in argument array and last argument are of type Throwable" }
        var t = event.throwable
        if (ft.throwable != null) {
            t = ft.throwable
            throw IllegalStateException("fix above code")
        }
        val record = LogRecord(julLevel, ft.message)
        record.loggerName = event.loggerName
        record.instant = Instant.ofEpochMilli(event.timeStamp)
        record.sourceClassName = EventConstants.NA_SUBST
        record.sourceMethodName = EventConstants.NA_SUBST
        record.thrown = t
        return record
    }
}
