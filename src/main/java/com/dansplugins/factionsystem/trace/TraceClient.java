/*
 * trace-client 0.1.0 -- https://github.com/Stephenson-Software/trace-client-java
 *
 * One call to report that a program was used. Copy this file into a project as
 * is, or depend on the artifact; either way there is nothing else to add.
 *
 * MIT licensed. Keep this header when vendoring so the file can be found again.
 *
 * Vendored into MedievalFactions unmodified apart from the package line.
 */
package com.dansplugins.factionsystem.trace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reports usage events to a trace server, and never gets in the way of the
 * program doing the reporting.
 *
 * <p>Three properties hold for every call to {@link #report}:
 *
 * <ul>
 *   <li><b>It returns immediately.</b> The HTTP call happens on a single
 *       daemon thread owned by this client. A Spigot plugin can report from
 *       the server thread without a tick ever waiting on the network.</li>
 *   <li><b>It never throws.</b> A server that is down, slow, or rejecting the
 *       key is a dropped report, not an exception in the host program. Failures
 *       are logged at {@link Level#FINE} if a logger was given, and otherwise
 *       not at all.</li>
 *   <li><b>It is bounded.</b> At most {@value #QUEUE_CAPACITY} reports wait to
 *       be sent; beyond that, new reports are dropped rather than accumulated.
 *       A trace server that is unreachable for a week costs a few kilobytes,
 *       not the host's heap.</li>
 * </ul>
 *
 * <p>Reporting is opt-out: a client built with {@link Builder#enabled(boolean)
 * enabled(false)}, or with no key, is a no-op that costs nothing. Programs that
 * run on other people's machines should expose that switch in their
 * configuration.
 *
 * <pre>{@code
 * TraceClient trace = TraceClient.builder("https://trace.example.org", "MyPlugin")
 *         .key(config.getString("usage-reporting.key"))
 *         .enabled(config.getBoolean("usage-reporting.enabled", true))
 *         .logger(getLogger())
 *         .build();
 *
 * trace.report("startup");
 * trace.report("command", 1.0, Collections.singletonMap("name", "home"));
 *
 * // on shutdown
 * trace.close();
 * }</pre>
 */
public final class TraceClient {

    /** How many reports may wait to be sent before new ones are dropped. */
    public static final int QUEUE_CAPACITY = 256;

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private final String endpoint;
    private final String key;
    private final String application;
    private final Logger logger;
    private final ThreadPoolExecutor executor; // null when disabled

    private TraceClient(Builder builder) {
        this.endpoint = builder.baseUrl.replaceAll("/+$", "") + "/api/metrics";
        this.key = builder.key;
        this.application = builder.application;
        this.logger = builder.logger;
        boolean enabled = builder.enabled && builder.key != null && !builder.key.trim().isEmpty();
        if (enabled) {
            this.executor = new ThreadPoolExecutor(
                    1, 1, 30, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY),
                    runnable -> {
                        Thread thread = new Thread(runnable, "trace-client/" + application);
                        thread.setDaemon(true);
                        return thread;
                    },
                    new ThreadPoolExecutor.DiscardPolicy());
            this.executor.allowCoreThreadTimeOut(true);
        } else {
            this.executor = null;
        }
    }

    /**
     * Starts describing a client for the program named {@code application},
     * reporting to the trace server at {@code baseUrl}.
     */
    public static Builder builder(String baseUrl, String application) {
        return new Builder(baseUrl, application);
    }

    /** A client that reports nothing. Useful as a default before configuration is read. */
    public static TraceClient disabled() {
        return new Builder("http://disabled.invalid", "disabled").enabled(false).build();
    }

    /** Whether {@link #report} will actually send anything. */
    public boolean isEnabled() {
        return executor != null;
    }

    /** Reports that {@code name} happened. */
    public void report(String name) {
        report(name, null, null);
    }

    /**
     * Reports that {@code name} happened, with an optional numeric value and
     * optional string tags. Returns immediately; see the class comment.
     */
    public void report(String name, Double value, Map<String, String> tags) {
        if (executor == null || name == null || name.trim().isEmpty()) {
            return;
        }
        final String body = json(application, name, value, tags);
        executor.execute(() -> send(body));
    }

    /**
     * Stops the sending thread. Reports already queued are dropped; one in
     * flight is given a moment to finish. Safe to call more than once, and on
     * a disabled client.
     */
    public void close() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            executor.awaitTermination(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void send(String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("User-Agent", "trace-client/0.1.0 (" + application + ")");
            connection.setDoOutput(true);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }
            int status = connection.getResponseCode();
            drain(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (status != 201) {
                log("trace server answered " + status + " for " + body);
            }
        } catch (IOException | RuntimeException failure) {
            // RuntimeException too: a misconfigured URL surfaces as one, and a
            // usage report must never be the reason a host program logs a
            // stack trace, let alone stops.
            log("could not deliver " + body + ": " + failure);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void drain(InputStream stream) throws IOException {
        // Reading the body to its end lets HttpURLConnection reuse the
        // connection; the content itself is not interesting.
        if (stream == null) {
            return;
        }
        try (InputStream in = stream) {
            byte[] buffer = new byte[512];
            while (in.read(buffer) != -1) { /* discard */ }
        }
    }

    private void log(String message) {
        if (logger != null) {
            logger.log(Level.FINE, "[trace] " + message);
        }
    }

    // JSON is written by hand so this file has no dependencies. The shape is
    // fixed and small -- three scalars and a flat string map -- which is all
    // a usage report needs.
    static String json(String application, String name, Double value, Map<String, String> tags) {
        StringBuilder out = new StringBuilder(128);
        out.append("{\"application\":").append(quote(application));
        out.append(",\"name\":").append(quote(name));
        if (value != null && !value.isNaN() && !value.isInfinite()) {
            out.append(",\"value\":").append(value);
        }
        Map<String, String> safeTags = tags == null ? Collections.<String, String>emptyMap() : new LinkedHashMap<>(tags);
        if (!safeTags.isEmpty()) {
            out.append(",\"tags\":{");
            boolean first = true;
            for (Map.Entry<String, String> tag : safeTags.entrySet()) {
                if (tag.getKey() == null || tag.getValue() == null) {
                    continue;
                }
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(quote(tag.getKey())).append(':').append(quote(tag.getValue()));
            }
            out.append('}');
        }
        return out.append('}').toString();
    }

    static String quote(String text) {
        StringBuilder out = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }

    /** Describes a {@link TraceClient}; see {@link TraceClient#builder}. */
    public static final class Builder {
        private final String baseUrl;
        private final String application;
        private String key;
        private boolean enabled = true;
        private Logger logger;

        private Builder(String baseUrl, String application) {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            if (application == null || application.trim().isEmpty()) {
                throw new IllegalArgumentException("application is required");
            }
            this.baseUrl = baseUrl.trim();
            this.application = application.trim();
        }

        /** The program's write key. Without one the client is a no-op. */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /** The opt-out. {@code false} yields a client that reports nothing. */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /** Where dropped reports are mentioned, at {@link Level#FINE}. Optional. */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public TraceClient build() {
            return new TraceClient(this);
        }
    }
}
