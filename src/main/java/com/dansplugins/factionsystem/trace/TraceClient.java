/*
 * trace-client 0.3.0 -- https://github.com/Stephenson-Software/trace-client-java
 *
 * One call to report that a program was used. Copy this file into a project as
 * is, or depend on the artifact; either way there is nothing else to add.
 *
 * MIT licensed. Keep this header when vendoring so the file can be found again.
 */
package com.dansplugins.factionsystem.trace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * <p>Reporting is opt-out, and the person running the program always has the
 * last word. {@link Builder#build()} checks, in this order, and the first
 * match is what {@link #disabledReason()} reports:
 *
 * <ol>
 *   <li>the environment: {@code TRACE_USAGE_REPORTING=off} (or {@code false},
 *       {@code 0}, {@code no}) or {@code DO_NOT_TRACK=1} (or {@code true},
 *       {@code yes}), case-insensitive -- reason {@code environment};</li>
 *   <li>the server-wide switch, when {@link Builder#serverWideConfig(File)}
 *       was given: {@code enabled: false} in {@code plugins/trace/config.yml}
 *       -- reason {@code server-wide config: plugins/trace/config.yml};</li>
 *   <li>the program's own setting, {@link Builder#enabled(boolean)
 *       enabled(false)} -- reason {@code config.yml};</li>
 *   <li>no key -- reason {@code no key}.</li>
 * </ol>
 *
 * <p>The same server-wide file can also carry a {@code tags:} block, merged
 * into every event every plugin on the server reports -- {@code ci: "true"}
 * on a test server keeps its events out of real-installation figures. An
 * event's own tag wins over a server-wide one of the same name. See
 * {@link Builder#serverWideConfig(File)}.
 *
 * <p>A disabled client is a no-op that costs nothing. Programs that run on
 * other people's machines should expose their own switch in their
 * configuration and say on startup whether reporting is on.
 *
 * <pre>{@code
 * TraceClient trace = TraceClient.builder("https://trace.example.org", "MyPlugin")
 *         .key(config.getString("usage-reporting.key"))
 *         .enabled(config.getBoolean("usage-reporting.enabled", true))
 *         .serverWideConfig(getDataFolder().getParentFile()) // plugins/
 *         .logger(getLogger())
 *         .build();
 *
 * if (trace.isEnabled()) {
 *     getLogger().info("Usage reporting is on: ...");
 * } else {
 *     getLogger().info("Usage reporting is off (" + trace.disabledReason() + ").");
 * }
 *
 * trace.report("startup");
 * trace.report("command", 1.0, Collections.singletonMap("name", "home"));
 *
 * // on shutdown
 * trace.close();
 * }</pre>
 */
public final class TraceClient {

    /** This client's version, as sent in the User-Agent. */
    public static final String VERSION = "0.3.0";

    /** How many reports may wait to be sent before new ones are dropped. */
    public static final int QUEUE_CAPACITY = 256;

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    /** Reason reported when an environment variable turned reporting off. */
    public static final String REASON_ENVIRONMENT = "environment";
    /** Reason reported when {@code plugins/trace/config.yml} turned reporting off. */
    public static final String REASON_SERVER_WIDE = "server-wide config: plugins/trace/config.yml";
    /** Reason reported when the program's own setting turned reporting off. */
    public static final String REASON_CONFIG = "config.yml";
    /** Reason reported when no key was given. */
    public static final String REASON_NO_KEY = "no key";

    /** Environment variable that turns reporting off: {@code off}, {@code false}, {@code 0}, {@code no}. */
    public static final String ENV_USAGE_REPORTING = "TRACE_USAGE_REPORTING";
    /** Environment variable that turns reporting off: {@code 1}, {@code true}, {@code yes}. See https://consoledonottrack.com. */
    public static final String ENV_DO_NOT_TRACK = "DO_NOT_TRACK";

    /** The server-wide switch, relative to the plugins directory. */
    static final String SERVER_WIDE_CONFIG_PATH = "trace" + File.separator + "config.yml";

    /** Exactly what a missing server-wide switch file is created with. */
    static final String SERVER_WIDE_CONFIG_CONTENT =
            "# Server-wide switch for usage reporting by plugins that report to trace\n"
            + "# (https://github.com/Stephenson-Software/trace#usage-reporting).\n"
            + "# Set enabled to false and every such plugin on this server stops reporting,\n"
            + "# regardless of its own usage-reporting.enabled setting. Plugins never turn\n"
            + "# this back on.\n"
            + "enabled: true\n"
            + "#\n"
            + "# Tags added to every event such plugins on this server report. A plugin's\n"
            + "# own tag of the same name wins. On a test or CI server, uncomment the two\n"
            + "# lines below so its events are left out of real-installation figures.\n"
            + "# tags:\n"
            + "#   ci: \"true\"\n";

    private static final Pattern ENABLED_LINE = Pattern.compile("^\\s*enabled\\s*:\\s*(\\S+)");
    private static final Pattern TAGS_LINE = Pattern.compile("^tags\\s*:\\s*(#.*)?$");

    // What the trace server accepts in a report's tags (MetricDto): at most
    // MAX_TAGS pairs, keys not blank, keys and values at most MAX_TAG_LENGTH
    // characters. Server-wide tags are held to that and to a stricter key
    // alphabet, so a typo in the file can never turn every report into a 400.
    static final int MAX_TAGS = 32;
    static final int MAX_TAG_LENGTH = 255;
    private static final Pattern TAG_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.\\-]*");

    // Where environment variables come from. A seam rather than System.getenv
    // directly, so tests can point it at a map; nothing else should touch it.
    static Function<String, String> environment = System::getenv;

    private final String endpoint;
    private final String key;
    private final String application;
    private final Logger logger;
    private final String disabledReason; // null when enabled
    private final Map<String, String> serverWideTags; // never null; read once, at build()
    private final ThreadPoolExecutor executor; // null when disabled

    private TraceClient(Builder builder) {
        this.endpoint = builder.baseUrl.replaceAll("/+$", "") + "/api/metrics";
        this.key = builder.key;
        this.application = builder.application;
        this.logger = builder.logger;
        ServerWideConfig serverWide = builder.pluginsDirectory == null || environmentDisables()
                ? ServerWideConfig.NONE
                : readServerWideConfig(builder.pluginsDirectory);
        this.disabledReason = disabledReason(builder, serverWide);
        this.serverWideTags = serverWide.tags;
        if (disabledReason == null) {
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

    /**
     * Why {@link #report} sends nothing: {@code null} when enabled, otherwise
     * one of {@link #REASON_ENVIRONMENT}, {@link #REASON_SERVER_WIDE},
     * {@link #REASON_CONFIG} or {@link #REASON_NO_KEY}, verbatim, so a program
     * can print {@code "Usage reporting is off (" + reason + ")."}.
     */
    public String disabledReason() {
        return disabledReason;
    }

    private static String disabledReason(Builder builder, ServerWideConfig serverWide) {
        if (environmentDisables()) {
            return REASON_ENVIRONMENT;
        }
        if (serverWide.disables) {
            return REASON_SERVER_WIDE;
        }
        if (!builder.enabled) {
            return REASON_CONFIG;
        }
        if (builder.key == null || builder.key.trim().isEmpty()) {
            return REASON_NO_KEY;
        }
        return null;
    }

    private static boolean environmentDisables() {
        return isOff(environment.apply(ENV_USAGE_REPORTING)) || isYes(environment.apply(ENV_DO_NOT_TRACK));
    }

    private static boolean isOff(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return v.equals("off") || v.equals("false") || v.equals("0") || v.equals("no");
    }

    private static boolean isYes(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("yes");
    }

    /** What {@code plugins/trace/config.yml} says: the switch and the server-wide tags. */
    static final class ServerWideConfig {
        static final ServerWideConfig NONE = new ServerWideConfig(false, Collections.<String, String>emptyMap());

        final boolean disables;
        final Map<String, String> tags;

        ServerWideConfig(boolean disables, Map<String, String> tags) {
            this.disables = disables;
            this.tags = tags;
        }
    }

    /**
     * Ensures {@code <pluginsDirectory>/trace/config.yml} exists and reads its
     * {@code enabled:} line and {@code tags:} block. No YAML library: the file
     * is ours, shallow, and a line scan is enough. Anything going wrong on
     * disk is logged at FINE and counts as enabled with no tags -- a
     * read-only plugins directory must not silently switch reporting off,
     * nor stop the host program.
     */
    private ServerWideConfig readServerWideConfig(File pluginsDirectory) {
        Path file = new File(pluginsDirectory, SERVER_WIDE_CONFIG_PATH).toPath();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.write(file, SERVER_WIDE_CONFIG_CONTENT.getBytes(StandardCharsets.UTF_8));
                // just written: enabled: true, and the tags example commented out
            }
            return parseServerWideConfig(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException failure) {
            log("could not read server-wide config " + file + ": " + failure);
            return ServerWideConfig.NONE;
        }
    }

    /**
     * Reads the switch and the tags from the lines of the server-wide file.
     * The first {@code enabled:} line outside a {@code tags:} block is the
     * switch. A {@code tags:} line at column 0 opens a block of indented
     * {@code key: value} lines, which ends at the next non-blank line that is
     * not indented; blank and {@code #} lines inside it are skipped, as are
     * lines indented differently from its first entry. Values may be bare,
     * double- or single-quoted. Entries the trace server would reject -- and
     * anything this reader does not understand -- are dropped, one by one,
     * and at most {@link #MAX_TAGS} are kept; nothing here throws.
     */
    static ServerWideConfig parseServerWideConfig(List<String> lines) {
        Boolean disables = null;
        Map<String, String> tags = new LinkedHashMap<>();
        boolean inTags = false;
        int entryIndent = -1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int indent = 0;
            while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
                indent++;
            }
            if (inTags) {
                if (indent > 0) {
                    if (entryIndent < 0) {
                        entryIndent = indent;
                    }
                    if (indent == entryIndent) {
                        addServerWideTag(tags, trimmed);
                    }
                    continue;
                }
                inTags = false;
            }
            if (TAGS_LINE.matcher(line).matches()) {
                inTags = true;
                entryIndent = -1;
                continue;
            }
            if (disables == null) {
                Matcher matcher = ENABLED_LINE.matcher(line);
                if (matcher.find()) {
                    disables = isOff(matcher.group(1));
                }
            }
        }
        return new ServerWideConfig(disables != null && disables,
                tags.isEmpty() ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(tags));
    }

    private static void addServerWideTag(Map<String, String> tags, String entry) {
        if (tags.size() >= MAX_TAGS) {
            return;
        }
        int colon = entry.indexOf(':');
        if (colon <= 0) {
            return;
        }
        String key = unquote(entry.substring(0, colon).trim());
        String rest = entry.substring(colon + 1);
        if (key == null || !rest.isEmpty() && rest.charAt(0) != ' ' && rest.charAt(0) != '\t') {
            return; // "a:b" is a string in YAML, not a pair
        }
        String value = scalar(rest.trim());
        if (value == null
                || key.length() > MAX_TAG_LENGTH || !TAG_KEY.matcher(key).matches()
                || value.length() > MAX_TAG_LENGTH) {
            return;
        }
        if (!tags.containsKey(key)) {
            tags.put(key, value);
        }
    }

    /** A key, quoted or not; null when the quoting is broken. */
    private static String unquote(String key) {
        if (key.startsWith("\"") || key.startsWith("'")) {
            return key.length() >= 2 && key.charAt(key.length() - 1) == key.charAt(0)
                    ? key.substring(1, key.length() - 1)
                    : null;
        }
        return key;
    }

    /**
     * A YAML scalar value, with any trailing comment removed; null for an
     * empty (YAML null) value, a broken quote, or anything that is not a
     * plain one-line scalar.
     */
    private static String scalar(String text) {
        if (text.isEmpty() || text.startsWith("#")) {
            return null;
        }
        char first = text.charAt(0);
        if (first == '"' || first == '\'') {
            StringBuilder out = new StringBuilder();
            int i = 1;
            for (; i < text.length(); i++) {
                char c = text.charAt(i);
                if (first == '"' && c == '\\' && i + 1 < text.length()) {
                    char next = text.charAt(++i);
                    switch (next) {
                        case 'n': out.append('\n'); break;
                        case 't': out.append('\t'); break;
                        case 'r': out.append('\r'); break;
                        default: out.append(next); // \" \\ \/ and anything else, literally
                    }
                } else if (c == first) {
                    if (first == '\'' && i + 1 < text.length() && text.charAt(i + 1) == '\'') {
                        out.append('\'');
                        i++;
                    } else {
                        break;
                    }
                } else {
                    out.append(c);
                }
            }
            if (i >= text.length()) {
                return null; // never closed
            }
            String after = text.substring(i + 1).trim();
            return after.isEmpty() || after.startsWith("#") ? out.toString() : null;
        }
        if ("[{|>&*!%@`".indexOf(first) >= 0) {
            return null; // flow collections, block scalars, anchors, tags: not ours
        }
        int comment = -1;
        for (int i = 1; i < text.length(); i++) {
            if (text.charAt(i) == '#' && (text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '\t')) {
                comment = i;
                break;
            }
        }
        String value = (comment < 0 ? text : text.substring(0, comment)).trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * The event's tags with the server-wide ones added: an event's own tag
     * wins on a key conflict, and server-wide tags stop being added once
     * {@link #MAX_TAGS} is reached, so the merge never makes a report the
     * server would reject. A tag with a null key or value counts as absent,
     * as it does in {@link #json}.
     */
    static Map<String, String> withServerWideTags(Map<String, String> tags, Map<String, String> serverWide) {
        if (serverWide.isEmpty()) {
            return tags;
        }
        Map<String, String> merged = new LinkedHashMap<>();
        if (tags != null) {
            for (Map.Entry<String, String> tag : new LinkedHashMap<>(tags).entrySet()) {
                if (tag.getKey() != null && tag.getValue() != null) {
                    merged.put(tag.getKey(), tag.getValue());
                }
            }
        }
        for (Map.Entry<String, String> tag : serverWide.entrySet()) {
            if (merged.size() >= MAX_TAGS) {
                break;
            }
            if (!merged.containsKey(tag.getKey())) {
                merged.put(tag.getKey(), tag.getValue());
            }
        }
        return merged;
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
        final String body = json(application, name, value, withServerWideTags(tags, serverWideTags));
        executor.execute(() -> send(body));
    }

    /**
     * Stops the sending thread, giving reports already queued up to
     * {@value #READ_TIMEOUT_MS} ms in total to be sent first. A program that
     * reports and then exits within milliseconds -- a CLI -- would otherwise
     * lose its one event to the race between queueing it and the thread
     * picking it up. The bound still holds: an unreachable server delays exit
     * by at most the timeout, never a hang; whatever has not been sent by
     * then is dropped. Safe to call more than once, and on a disabled client.
     */
    public void close() {
        if (executor == null) {
            return;
        }
        executor.shutdown(); // no new work; queued reports still run
        try {
            if (!executor.awaitTermination(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
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
            connection.setRequestProperty("User-Agent", "trace-client/" + VERSION + " (" + application + ")");
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
        private File pluginsDirectory;
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

        /**
         * The server-wide opt-out shared by every plugin on a Spigot server.
         * Given the plugins directory ({@code getDataFolder().getParentFile()}
         * in a Bukkit plugin), {@link #build()} makes sure
         * {@code plugins/trace/config.yml} exists -- creating it with
         * {@code enabled: true} if it is missing -- and honours
         * {@code enabled: false} in it. The file is never rewritten once it
         * exists. Its optional {@code tags:} block is added to every event
         * this client reports, below the event's own tags:
         *
         * <pre>
         * enabled: true
         * tags:
         *   ci: "true"
         * </pre>
         *
         * <p>Both are read once, here. Optional; programs that are not
         * plugins leave it unset.
         */
        public Builder serverWideConfig(File pluginsDirectory) {
            this.pluginsDirectory = pluginsDirectory;
            return this;
        }

        /** Where dropped reports are mentioned, at {@link Level#FINE}. Optional. */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Builds the client. The environment variables
         * {@value TraceClient#ENV_USAGE_REPORTING} and
         * {@value TraceClient#ENV_DO_NOT_TRACK} are always consulted first,
         * then the server-wide config if one was given, then
         * {@link #enabled(boolean)}, then the key. Never throws.
         */
        public TraceClient build() {
            return new TraceClient(this);
        }
    }
}
