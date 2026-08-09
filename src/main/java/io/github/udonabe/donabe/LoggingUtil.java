package io.github.udonabe.donabe;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class LoggingUtil {
    private LoggingUtil() {}
    public static void configure(boolean verbose) {
        if (!verbose) return;
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        setLevel(root, "stderr", Level.DEBUG);
        setLevel(root, "file", Level.TRACE);
    }

    private static void setLevel(Logger logger, String appenderName, Level level) {
        Appender<ILoggingEvent> appender = logger.getAppender(appenderName);
        List<Filter<ILoggingEvent>> filters = appender.getCopyOfAttachedFiltersList();
        var thresholds = filters.stream()
                .filter(t -> t instanceof ThresholdFilter)
                .map(t -> (ThresholdFilter) t)
                .toList();
        for (ThresholdFilter thresholdFilter : thresholds) {
            thresholdFilter.setLevel(level.levelStr);
        }
    }
}
