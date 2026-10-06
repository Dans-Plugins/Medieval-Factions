/*
 * trace-client 0.6.1 -- https://github.com/Stephenson-Software/trace-client-java
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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * <p>Every event carries the program's own version as the tag
 * {@code version} -- the third argument to {@link #builder}, required, so a
 * {@code command} event can be tied to a release as well as a
 * {@code startup} one. An event's own {@code version} tag wins over it.
 *
 * <p>Every event also carries a random per-installation ID as the tag
 * {@code install}, so the trace server can count distinct servers rather
 * than raw events. On a Spigot server it is the {@code server-id:} line of
 * {@code plugins/trace/config.yml}, generated with
 * {@link UUID#randomUUID()} and appended to that file the first time an
 * enabled client finds none -- the same idea as bStats' {@code serverUuid}.
 * Anything else may pass a file to keep it in with
 * {@link Builder#installIdFile(File)}, or the ID itself with
 * {@link Builder#installId(String)}. When more than one is given, the
 * explicit {@code installId} wins, then {@code installIdFile}, then the
 * server-wide {@code server-id:}; with none of them, no {@code install} tag
 * is sent. The ID is random: it names no person, account or address. A
 * disabled client never generates, reads or writes one. An event's own
 * {@code install} tag wins over it.
 *
 * <p>A disabled client is a no-op that costs nothing. Programs that run on
 * other people's machines should expose their own switch in their
 * configuration and say on startup whether reporting is on.
 *
 * <pre>{@code
 * TraceClient trace = TraceClient.builder("https://trace.example.org", "MyPlugin",
 *                 getDescription().getVersion())
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
    public static final String VERSION = "0.6.1";

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

    /**
     * Explains the {@code server-id:} line. Part of a freshly created file,
     * and written above the line when it is appended to an existing one.
     */
    static final String SERVER_ID_COMMENT =
            "#\n"
            + "# server-id: a random ID made on first run and sent as the tag \"install\", so\n"
            + "# trace can count servers, not events. It identifies no person and no IP\n"
            + "# address. Delete the server-id line to get a new one.\n";

    /** Exactly what a missing server-wide switch file is created with. */
    static final String SERVER_WIDE_CONFIG_CONTENT =
            "# Server-wide switch for usage reporting by plugins that report to trace\n"
            + "# (https://danielstephenson.dev/usage-reporting).\n"
            + "# Set enabled to false and every such plugin on this server stops reporting,\n"
            + "# regardless of its own usage-reporting.enabled setting. Plugins never turn\n"
            + "# this back on.\n"
            + "enabled: true\n"
            + "#\n"
            + "# Tags added to every event such plugins on this server report. A plugin's\n"
            + "# own tag of the same name wins. On a test or CI server, uncomment the two\n"
            + "# lines below so its events are left out of real-installation figures.\n"
            + "# tags:\n"
            + "#   ci: \"true\"\n"
            + SERVER_ID_COMMENT;

    /** The tag every event carries the installation's ID as. */
    static final String INSTALL_TAG = "install";

    /** What {@link #installIdFromFile(File)} accepts as an ID on a line of its file. */
    private static final Pattern INSTALL_ID_LINE = Pattern.compile("[A-Za-z0-9_.\\-]{1,255}");

    private static final Pattern ENABLED_LINE = Pattern.compile("^\\s*enabled\\s*:\\s*(\\S+)");
    private static final Pattern TAGS_LINE = Pattern.compile("^tags\\s*:\\s*(#.*)?$");
    private static final Pattern SERVER_ID_LINE = Pattern.compile("^server-id\\s*:(.*)$");

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
    private final String version;
    private final Logger logger;
    private final String disabledReason; // null when enabled
    private final Map<String, String> serverWideTags; // never null; read once, at build()
    private final String installId; // null when disabled or when there is none
    private final ThreadPoolExecutor executor; // null when disabled

    private TraceClient(Builder builder) {
        this.endpoint = builder.baseUrl.replaceAll("/+$", "") + "/api/metrics";
        this.key = builder.key;
        this.application = builder.application;
        this.version = builder.version;
        this.logger = builder.logger;
        ServerWideConfig serverWide = builder.pluginsDirectory == null || environmentDisables()
                ? ServerWideConfig.NONE
                : readServerWideConfig(builder.pluginsDirectory);
        this.disabledReason = disabledReason(builder, serverWide);
        this.serverWideTags = serverWide.tags;
        // After the opt-outs, never before: a disabled client neither makes
        // up an ID nor writes one to disk.
        this.installId = disabledReason == null ? resolveInstallId(builder, serverWide) : null;
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
     * at {@code version}, reporting to the trace server at {@code baseUrl}.
     * The version is sent as the tag {@code version} on every event; a blank
     * one, or one longer than {@value #MAX_TAG_LENGTH} characters, is an
     * {@link IllegalArgumentException}.
     */
    public static Builder builder(String baseUrl, String application, String version) {
        return new Builder(baseUrl, application, version);
    }

    /** A client that reports nothing. Useful as a default before configuration is read. */
    public static TraceClient disabled() {
        return new Builder("http://disabled.invalid", "disabled", "disabled").enabled(false).build();
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

    /**
     * The random per-installation ID every event carries as the tag
     * {@code install}, or {@code null} when the client is disabled or has
     * none (no {@link Builder#installId(String)}, no
     * {@link Builder#installIdFile(File)} and no
     * {@link Builder#serverWideConfig(File)}).
     */
    public String installId() {
        return installId;
    }

    /**
     * The installation's ID: the builder's, else the one kept in the
     * builder's install ID file, else the server-wide file's
     * {@code server-id:}, else a new random one appended to that file. If a
     * file could not be read or written, the new ID lives in memory for this
     * process only. Never throws.
     */
    private String resolveInstallId(Builder builder, ServerWideConfig serverWide) {
        if (builder.installId != null) {
            return builder.installId;
        }
        if (builder.installIdFile != null) {
            return loadOrCreateInstallId(builder.installIdFile, logger);
        }
        if (builder.pluginsDirectory == null) {
            return null;
        }
        if (serverWide.serverId != null) {
            return serverWide.serverId;
        }
        String fresh = UUID.randomUUID().toString();
        if (!serverWide.read) {
            // A file that could not be read is not appended to: one that is
            // there but unreadable would otherwise gain a line every start.
            log("using an in-memory server-id for this process: server-wide config was not readable");
            return fresh;
        }
        File location = new File(builder.pluginsDirectory, SERVER_WIDE_CONFIG_PATH);
        try {
            Path file = location.toPath();
            byte[] existing = Files.readAllBytes(file);
            StringBuilder block = new StringBuilder();
            if (existing.length > 0 && existing[existing.length - 1] != '\n') {
                block.append('\n');
            }
            if (!serverWide.created) {
                block.append(SERVER_ID_COMMENT); // a fresh file already has it
            }
            block.append("server-id: ").append(fresh).append('\n');
            Files.write(file, block.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            // Read back, so two clients racing on one file agree on the first line.
            String written = parseServerWideConfig(Files.readAllLines(file, StandardCharsets.UTF_8)).serverId;
            return written != null ? written : fresh;
        } catch (IOException | RuntimeException failure) {
            log("could not write server-id to server-wide config " + location
                    + ", using an in-memory one for this process: " + failure);
            return fresh;
        }
    }

    /**
     * The installation's ID kept in {@code file}, which the program chooses
     * -- there is no default location. The first line that is an ID
     * ({@code [A-Za-z0-9_.-]}, at most {@value #MAX_TAG_LENGTH} characters,
     * surrounding whitespace ignored) is returned. If the file does not exist
     * or holds no such line, a new random {@link UUID#randomUUID()} is written
     * to it (parent directories created) and returned. Delete the file to get
     * a new one.
     *
     * <p>Never throws: if the file exists but cannot be read, or cannot be
     * written, a fresh random ID is returned for this process only, and an
     * unreadable file is left as it is.
     *
     * <p><b>Called directly, this writes whatever the opt-outs say.</b> Pass
     * the file to {@link Builder#installIdFile(File)} instead to keep the
     * guarantee that a disabled client writes nothing.
     */
    public static String installIdFromFile(File file) {
        return loadOrCreateInstallId(file, null);
    }

    private static String loadOrCreateInstallId(File file, Logger logger) {
        String fresh = UUID.randomUUID().toString();
        if (file == null || file.getPath().trim().isEmpty()) {
            return fresh;
        }
        Path path;
        try {
            // Inside the try: toPath() throws InvalidPathException for a path
            // the file system cannot represent.
            path = file.toPath();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String candidate = line.trim();
                if (INSTALL_ID_LINE.matcher(candidate).matches()) {
                    return candidate;
                }
            }
        } catch (NoSuchFileException missing) {
            path = file.toPath(); // it converted, or there would be no NoSuchFileException
        } catch (IOException | RuntimeException failure) {
            // There but unreadable (a directory, no permission, not UTF-8):
            // never overwrite it.
            log(logger, "could not read install ID file " + file + ", using an in-memory one: " + failure);
            return fresh;
        }
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, (fresh + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException failure) {
            log(logger, "could not write install ID file " + file + ", using an in-memory one: " + failure);
        }
        return fresh;
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

    /** What {@code plugins/trace/config.yml} says: the switch, the server-wide tags and the server-id. */
    static final class ServerWideConfig {
        /** No file was read: none was asked for, or it could not be read. */
        static final ServerWideConfig NONE = new ServerWideConfig(false, Collections.<String, String>emptyMap(), null);

        final boolean disables;
        final Map<String, String> tags;
        final String serverId; // null when the file has no usable server-id: line
        final boolean read; // the file was read successfully
        final boolean created; // ... and build() had just created it

        ServerWideConfig(boolean disables, Map<String, String> tags, String serverId) {
            this(disables, tags, serverId, false, false);
        }

        private ServerWideConfig(boolean disables, Map<String, String> tags, String serverId, boolean read, boolean created) {
            this.disables = disables;
            this.tags = tags;
            this.serverId = serverId;
            this.read = read;
            this.created = created;
        }

        ServerWideConfig readFromDisk(boolean created) {
            return new ServerWideConfig(disables, tags, serverId, true, created);
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
        File location = new File(pluginsDirectory, SERVER_WIDE_CONFIG_PATH);
        try {
            // Inside the try: toPath() throws InvalidPathException for a path
            // the file system cannot represent, and build() never throws.
            Path file = location.toPath();
            boolean created = false;
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.write(file, SERVER_WIDE_CONFIG_CONTENT.getBytes(StandardCharsets.UTF_8));
                // just written: enabled: true, and the tags example commented out
                created = true;
            }
            return parseServerWideConfig(Files.readAllLines(file, StandardCharsets.UTF_8)).readFromDisk(created);
        } catch (IOException | RuntimeException failure) {
            log("could not read server-wide config " + location + ": " + failure);
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
     * and at most {@link #MAX_TAGS} are kept. The first {@code server-id:}
     * line at column 0, outside a {@code tags:} block, whose value is a valid
     * tag value made of {@code [A-Za-z0-9_.-]} is the installation's ID; any
     * other is ignored. Nothing here throws.
     */
    static ServerWideConfig parseServerWideConfig(List<String> lines) {
        Boolean disables = null;
        String serverId = null;
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
            if (serverId == null) {
                Matcher matcher = SERVER_ID_LINE.matcher(line);
                if (matcher.matches()) {
                    String value = scalar(matcher.group(1).trim());
                    if (value != null && value.length() <= MAX_TAG_LENGTH && TAG_KEY.matcher(value).matches()) {
                        serverId = value;
                    }
                    continue;
                }
            }
            if (disables == null) {
                Matcher matcher = ENABLED_LINE.matcher(line);
                if (matcher.find()) {
                    disables = isOff(matcher.group(1));
                }
            }
        }
        return new ServerWideConfig(disables != null && disables,
                tags.isEmpty() ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(tags),
                serverId);
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

    /**
     * The event's own tags plus {@code version}, unless the event already
     * carries one. A copy; the caller's map is never modified.
     */
    static Map<String, String> withVersion(Map<String, String> tags, String version) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (tags != null) {
            for (Map.Entry<String, String> tag : new LinkedHashMap<>(tags).entrySet()) {
                if (tag.getKey() != null && tag.getValue() != null) {
                    merged.put(tag.getKey(), tag.getValue());
                }
            }
        }
        if (!merged.containsKey("version")) {
            merged.put("version", version);
        }
        return merged;
    }

    /**
     * The tags plus {@code install}, unless they already carry one, there is
     * no ID, or adding it would pass {@link #MAX_TAGS}. A copy when it adds.
     */
    static Map<String, String> withInstall(Map<String, String> tags, String installId) {
        if (installId == null || tags.containsKey(INSTALL_TAG) || tags.size() >= MAX_TAGS) {
            return tags;
        }
        Map<String, String> merged = new LinkedHashMap<>(tags);
        merged.put(INSTALL_TAG, installId);
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
        final String body = json(application, name, value,
                withServerWideTags(withInstall(withVersion(tags, version), installId), serverWideTags));
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
        log(logger, message);
    }

    private static void log(Logger logger, String message) {
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
        private final String version;
        private String key;
        private boolean enabled = true;
        private File pluginsDirectory;
        private String installId;
        private File installIdFile;
        private Logger logger;

        private Builder(String baseUrl, String application, String version) {
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            if (application == null || application.trim().isEmpty()) {
                throw new IllegalArgumentException("application is required");
            }
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalArgumentException("version is required");
            }
            if (version.trim().length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("version is longer than " + MAX_TAG_LENGTH + " characters");
            }
            this.baseUrl = baseUrl.trim();
            this.application = application.trim();
            this.version = version.trim();
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
         * {@code enabled: false} in it. Its optional {@code tags:} block is
         * added to every event this client reports, below the event's own
         * tags. Its {@code server-id:} line is sent as the tag
         * {@code install}; when an enabled client finds none, it appends one
         * -- a random UUID under a comment saying what it is -- and otherwise
         * never changes the file. If that write fails, the ID is kept in
         * memory for this process only:
         *
         * <pre>
         * enabled: true
         * tags:
         *   ci: "true"
         * server-id: 0f8b6c1e-...
         * </pre>
         *
         * <p>Both are read once, here. Optional; programs that are not
         * plugins leave it unset.
         */
        public Builder serverWideConfig(File pluginsDirectory) {
            this.pluginsDirectory = pluginsDirectory;
            return this;
        }

        /**
         * The installation's ID, for programs that are not Spigot plugins:
         * sent as the tag {@code install} on every event, so the trace
         * server can count installations. It should be random -- e.g. a
         * {@link UUID#randomUUID()} the program stores in its own
         * configuration -- and never derived from a person, account or
         * address. Takes precedence over {@link #installIdFile(File)} and the
         * server-wide {@code server-id:}. Trimmed; {@code null} or blank means
         * none. Without it, {@link #installIdFile(File)} or
         * {@link #serverWideConfig(File)}, no {@code install} tag is sent.
         *
         * @throws IllegalArgumentException when longer than {@value TraceClient#MAX_TAG_LENGTH} characters
         */
        public Builder installId(String installId) {
            String trimmed = installId == null ? null : installId.trim();
            if (trimmed != null && trimmed.length() > MAX_TAG_LENGTH) {
                throw new IllegalArgumentException("installId is longer than " + MAX_TAG_LENGTH + " characters");
            }
            this.installId = trimmed == null || trimmed.isEmpty() ? null : trimmed;
            return this;
        }

        /**
         * A file to keep the installation's ID in, for programs that are not
         * Spigot plugins -- e.g. {@code <user data dir>/<program>/trace-install-id}.
         * {@link #build()} resolves it with {@link TraceClient#installIdFromFile(File)}
         * <b>only when the client is enabled</b>, after every opt-out: the first
         * run writes a new random UUID there (creating parent directories) and
         * later runs reuse it. A file that cannot be read or written yields an
         * in-memory ID for this process; an unreadable one is never
         * overwritten; {@code build()} still never throws.
         *
         * <p>Precedence: an explicit {@link #installId(String)} wins over this,
         * and this wins over the server-wide {@code server-id:} of
         * {@link #serverWideConfig(File)} (whose file then gains no
         * {@code server-id:} line). {@code null} means none.
         */
        public Builder installIdFile(File installIdFile) {
            this.installIdFile = installIdFile;
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
