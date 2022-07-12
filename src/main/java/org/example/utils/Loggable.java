package org.example.utils;

import io.github.ludovicianul.prettylogger.PrettyLogger;
import io.github.ludovicianul.prettylogger.PrettyLoggerFactory;
import io.github.ludovicianul.prettylogger.config.PrettyLoggerProperties;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

public class Loggable {

    final PrettyLogger log;

    protected HashMap<String, Instant> asyncSafeTimers;
    Instant start;
    Instant end;
    public Loggable(Class<?> parent) {
        log = PrettyLoggerFactory.getLogger(parent);
        this.inject();
        this.asyncSafeTimers = new HashMap<>();
    }

    public void start() {
        this.start = Instant.now();
    }

    public void startAsync(String name) {
        if (this.asyncSafeTimers.get(name) != null) this.asyncSafeTimers.remove(name);

        this.asyncSafeTimers.putIfAbsent(name, Instant.now());
    }

    public long milliElapsedAsync(String name) {
        final Instant asyncEnd = Instant.now();
        final Instant asyncStart = this.asyncSafeTimers.get(name);

        if (asyncStart == null) {
            this.log.warn("Async start timer for name %s was, if this error occurs consistently please contact a developer"
                    .formatted(name));
            return 0L;
        }

        return Duration.between(asyncStart, asyncEnd).toMillis();
    }

    public long milliElapsed() {
        this.end = Instant.now();

        return Duration.between(this.start, this.end).toMillis();
    }

    public String complete(String name) {
        final String milliElapsed = this.prettyAsyncMilliElapsed(name);
        this.asyncSafeTimers.remove(name);
        return milliElapsed;
    }

    public String prettyAsyncMilliElapsed(String name) {
        long milliElapsed = this.milliElapsedAsync(name);
        int seconds = 0;
        long leftOver;
        seconds += milliElapsed / 1000;
        leftOver = milliElapsed % 1000;

        return "%s.%ss".formatted(seconds, String.format("%03d", leftOver));
    }

    public String prettyMilliElapsed() {
        long milliElapsed = this.milliElapsed();
        int seconds = 0;
        long leftOver;

        seconds += milliElapsed / 1000;
        leftOver = milliElapsed % 1000;

        return "%s.%ss".formatted(seconds, String.format("%03d", leftOver));
    }

    protected void inject() {

        Object cc = PrettyLoggerProperties.INSTANCE;
        try {
            Field prefixFormat = PrettyLoggerProperties.INSTANCE
                    .getClass()
                    .getDeclaredField("prefixFormat");
            prefixFormat.setAccessible(true);
            prefixFormat.set(cc, "%1$-29s");

        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public PrettyLogger logger() {
        return this.log;
    }

}

