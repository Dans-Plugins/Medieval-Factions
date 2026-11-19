package com.dansplugins.factionsystem.trace;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Drives the client against a real HTTP server on a loopback port -- the
 * JDK's own, so the test has no more dependencies than the client does.
 */
class TraceClientTest {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    private HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();
    private volatile int replyStatus = 201;
    private volatile CountDownLatch arrived = new CountDownLatch(1);

    // What the client sees as its environment. Empty unless a test says
    // otherwise, so a DO_NOT_TRACK on the machine running the suite cannot
    // fail the tests that expect an enabled client.
    private final Map<String, String> environment = new HashMap<>();
    private Function<String, String> realEnvironment;

    private static final class Received {
        final String method;
        final String path;
        final String authorization;
        final String contentType;
        final String userAgent;
        final String body;

        Received(String method, String path, String authorization, String contentType, String userAgent, String body) {
            this.method = method;
            this.path = path;
            this.authorization = authorization;
            this.contentType = contentType;
            this.userAgent = userAgent;
            this.body = body;
        }
    }

    @BeforeEach
    void isolateEnvironment() {
        realEnvironment = TraceClient.environment;
        TraceClient.environment = environment::get;
    }

    @AfterEach
    void restoreEnvironment() {
        TraceClient.environment = realEnvironment;
    }

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = readAll(exchange.getRequestBody());
            received.add(new Received(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("User-Agent"),
                    new String(body, StandardCharsets.UTF_8)));
            exchange.sendResponseHeaders(replyStatus, -1);
            exchange.close();
            arrived.countDown();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void report_postsTheEventToTheMetricsEndpointWithTheKey() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl() + "/", "MyPlugin", "1.2.3").key("k-123").build();

        // Act
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the report should reach the server");
        Received request = received.get(0);
        assertEquals("POST", request.method);
        assertEquals("/api/metrics", request.path, "a trailing slash on the base URL must not double up");
        assertEquals("Bearer k-123", request.authorization);
        assertTrue(request.contentType.startsWith("application/json"), request.contentType);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\"}}", request.body);
        client.close();
    }

    @Test
    void report_carriesValueAndTagsWhenGiven() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("command", "home");
        tags.put("world", "the \"end\"");

        // Act
        client.report("command", 2.5, tags);

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        assertEquals(
                "{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.5,"
                        + "\"tags\":{\"command\":\"home\",\"world\":\"the \\\"end\\\"\",\"version\":\"1.2.3\"}}",
                received.get(0).body);
        client.close();
    }

    @Test
    void report_returnsBeforeTheServerAnswers() throws Exception {
        // Arrange
        // A server that never replies. If report() waited on the network the
        // caller -- a game server's main thread, in the case that matters --
        // would wait with it.
        CountDownLatch release = new CountDownLatch(1);
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyPlugin", "1.2.3")
                .key("k").build();

        // Act
        long before = System.nanoTime();
        client.report("startup");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);

        // Assert
        assertTrue(elapsedMs < 1_000, "report() took " + elapsedMs + " ms; it must not wait on the network");
        release.countDown();
        client.close();
        slow.stop(0);
    }

    @Test
    void report_doesNotThrowWhenNothingIsListening() throws Exception {
        // Arrange
        // Pick a port by binding and releasing it, so nothing answers there.
        HttpServer probe = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int deadPort = probe.getAddress().getPort();
        probe.stop(0);
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.dead");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + deadPort, "MyPlugin", "1.2.3")
                .key("k").logger(logger).build();

        // Act
        assertDoesNotThrow(() -> client.report("startup"));
        client.close(); // waits for the in-flight attempt to fail

        // Assert
        assertTrue(log.await(5, TimeUnit.SECONDS), "the failure should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("could not deliver"), log.records.get(0).getMessage());
    }

    @Test
    void report_doesNotThrowWhenTheServerRejectsTheKey() throws Exception {
        // Arrange
        replyStatus = 401;
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.rejected");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("revoked").logger(logger).build();

        // Act
        assertDoesNotThrow(() -> client.report("startup"));

        // Assert
        assertTrue(log.await(5, TimeUnit.SECONDS));
        assertTrue(log.records.get(0).getMessage().contains("answered 401"), log.records.get(0).getMessage());
        client.close();
    }

    @Test
    void report_aTagsMapChangedMidReadIsDroppedWithoutThrowing() throws Exception {
        // Arrange
        // What a HashMap does when another thread changes it during the
        // copy, without needing to win that race.
        Map<String, String> beingChanged = new AbstractMap<String, String>() {
            @Override
            public Set<Map.Entry<String, String>> entrySet() {
                throw new ConcurrentModificationException();
            }
        };
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.unreadableTags");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").logger(logger).build();

        // Act
        assertDoesNotThrow(() -> client.report("command", 1.0, beingChanged));
        client.report("after", null, null);

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the client should keep working");
        assertEquals(1, client.coalescing(), "only the good report opened a window");
        client.close();
        assertEquals(1, received.size(), "the unreadable report is dropped");
        assertTrue(received.get(0).body.contains("\"name\":\"after\""), received.get(0).body);
        assertTrue(log.records.isEmpty(), "nothing is logged on the caller's thread: " + log.records);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void report_aNonStringTagSmuggledInByARawMapIsDroppedWithoutThrowing() throws Exception {
        // Arrange
        // A Bukkit getValues(false) is a Map<String, Object>; one unchecked
        // cast puts an Integer where json() needs a String.
        Map raw = new HashMap();
        raw.put("count", 5);
        Map<String, String> tags = raw;
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();

        // Act
        assertDoesNotThrow(() -> client.report("command", null, tags));
        client.report("after", null, null);

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the client should keep working");
        client.close();
        assertEquals(1, received.size(), "the unreadable report is dropped");
        assertTrue(received.get(0).body.contains("\"name\":\"after\""), received.get(0).body);
    }

    @Test
    void disabledClient_sendsNothing() throws Exception {
        // Arrange
        TraceClient byFlag = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").enabled(false).build();
        TraceClient byMissingKey = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").build();
        TraceClient byBlankKey = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("  ").build();
        TraceClient explicit = TraceClient.disabled();

        // Act
        for (TraceClient client : new TraceClient[] {byFlag, byMissingKey, byBlankKey, explicit}) {
            assertFalse(client.isEnabled());
            client.report("startup");
            client.close();
        }

        // Assert
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should have been sent");
        assertTrue(received.isEmpty());
    }

    @Test
    void report_ignoresABlankName() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();

        // Act
        client.report(null);
        client.report("   ");
        client.close();

        // Assert
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS));
        assertTrue(received.isEmpty());
    }

    @Test
    void builder_rejectsAMissingBaseUrlOrApplication() {
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder(null, "MyPlugin", "1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder(" ", "MyPlugin", "1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder("http://x", null, "1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder("http://x", "", "1.2.3"));
    }

    @Test
    void builder_rejectsAMissingOrOverlongVersion() {
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder("http://x", "MyPlugin", null));
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder("http://x", "MyPlugin", "  "));
        StringBuilder overlong = new StringBuilder();
        for (int i = 0; i <= TraceClient.MAX_TAG_LENGTH; i++) {
            overlong.append('9');
        }
        assertThrows(IllegalArgumentException.class, () -> TraceClient.builder("http://x", "MyPlugin", overlong.toString()));
    }

    @Test
    void builder_namesWhatIsMissingInItsMessage() {
        // Act
        IllegalArgumentException noBaseUrl = assertThrows(IllegalArgumentException.class,
                () -> TraceClient.builder("  ", "MyPlugin", "1.2.3"));
        IllegalArgumentException noApplication = assertThrows(IllegalArgumentException.class,
                () -> TraceClient.builder("http://x", null, "1.2.3"));
        IllegalArgumentException noVersion = assertThrows(IllegalArgumentException.class,
                () -> TraceClient.builder("http://x", "MyPlugin", ""));
        IllegalArgumentException allMissing = assertThrows(IllegalArgumentException.class,
                () -> TraceClient.builder(null, null, null));

        // Assert
        assertEquals("baseUrl is required", noBaseUrl.getMessage());
        assertEquals("application is required", noApplication.getMessage());
        assertEquals("version is required", noVersion.getMessage());
        assertEquals("baseUrl is required", allMissing.getMessage(), "checked in argument order");
    }

    @Test
    void builder_rejectsAnOverlongVersionAndAcceptsOneAtTheLimit() throws Exception {
        // Arrange
        StringBuilder max = new StringBuilder();
        for (int i = 0; i < TraceClient.MAX_TAG_LENGTH; i++) {
            max.append('9');
        }

        // Act
        IllegalArgumentException tooLong = assertThrows(IllegalArgumentException.class,
                () -> TraceClient.builder(baseUrl(), "MyPlugin", max + "9"));
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "  " + max + "  ").key("k").build();
        client.report("startup");

        // Assert
        assertEquals("version is longer than " + TraceClient.MAX_TAG_LENGTH + " characters", tooLong.getMessage());
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "a version at the limit is accepted");
        assertTrue(received.get(0).body.contains("\"version\":\"" + max + "\""),
                "surrounding whitespace does not count and is not sent: " + received.get(0).body);
        client.close();
    }

    @Test
    void report_tagsACommandWithTheProgramVersionTrimmed() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", " 2.0.0-SNAPSHOT ").key("k").build();

        // Act
        client.report("command", null, Collections.singletonMap("name", "home"));

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\","
                + "\"tags\":{\"name\":\"home\",\"version\":\"2.0.0-SNAPSHOT\"}}", received.get(0).body);
        client.close();
    }

    @Test
    void report_anEventsOwnVersionTagWinsOverTheProgramVersion() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("version", "9.9.9");

        // Act
        client.report("startup", null, tags);

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"9.9.9\"}}",
                received.get(0).body);
        assertEquals(Collections.singletonMap("version", "9.9.9"), tags, "the caller's map is not modified");
        client.close();
    }

    @Test
    void withVersion_neverModifiesTheCallersMap() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "home");

        Map<String, String> merged = TraceClient.withVersion(tags, "1.2.3");

        assertEquals(Collections.singletonMap("name", "home"), tags);
        assertEquals("1.2.3", merged.get("version"));
        assertEquals("1.2.3", TraceClient.withVersion(null, "1.2.3").get("version"));
    }

    @Test
    void withVersion_dropsNullKeysAndValuesAndKeepsTheEventsOwnVersion() {
        // Arrange
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "home");
        tags.put("nullValue", null);
        tags.put(null, "nullKey");
        tags.put("version", "own");

        // Act
        Map<String, String> merged = TraceClient.withVersion(tags, "1.2.3");

        // Assert
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("name", "home");
        expected.put("version", "own");
        assertEquals(expected, merged);
        assertEquals(4, tags.size(), "the caller's map is not modified");
    }

    @Test
    void withVersion_addsTheVersionEvenToAnEventAlreadyAtMaxTags() {
        // Characterizes current behaviour, which issue #13 questions: unlike
        // install and the server-wide tags, version is not held to MAX_TAGS,
        // so an event with MAX_TAGS tags of its own goes out with one more.
        // Arrange
        Map<String, String> tags = new LinkedHashMap<>();
        for (int i = 0; i < TraceClient.MAX_TAGS; i++) {
            tags.put("e" + i, "v");
        }

        // Act
        Map<String, String> merged = TraceClient.withVersion(tags, "1.2.3");

        // Assert
        assertEquals(TraceClient.MAX_TAGS + 1, merged.size());
        assertEquals("1.2.3", merged.get("version"));
    }

    @Test
    void withInstall_addsTheIdOnlyWhenThereIsOneAndRoomAndNoneAlready() {
        // Arrange
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("version", "1.2.3");
        Map<String, String> own = Collections.singletonMap("install", "the-events-own");
        Map<String, String> full = new LinkedHashMap<>();
        for (int i = 0; i < TraceClient.MAX_TAGS; i++) {
            full.put("e" + i, "v");
        }

        // Act
        Map<String, String> added = TraceClient.withInstall(tags, "abc");

        // Assert
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("version", "1.2.3");
        expected.put("install", "abc");
        assertEquals(expected, added);
        assertEquals(Collections.singletonMap("version", "1.2.3"), tags, "the caller's map is not modified");
        assertSame(tags, TraceClient.withInstall(tags, null), "no ID, nothing added");
        assertSame(own, TraceClient.withInstall(own, "abc"), "the event's own install tag wins");
        assertSame(full, TraceClient.withInstall(full, "abc"), "never pushed past MAX_TAGS");
    }

    @Test
    void serverWideConfig_anyEnabledLineThatIsNotOnDisables() {
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled: true", "enabled: false")).disables,
                "a later off wins over an earlier on");
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled: false", "enabled: true")).disables,
                "and an earlier off wins over a later on");
        assertFalse(TraceClient.parseServerWideConfig(Arrays.asList("enabled: true", "enabled: yes")).disables);
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("# enabled: true", "enabled: false")).disables,
                "a commented-out line is not a switch");
    }

    @Test
    void serverWideConfig_aValuelessEnabledLineDisables() {
        // Fails closed (issue #27): an enabled: line with no value, or only a
        // comment, does not clearly mean "on", so it turns reporting off.
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled:")).disables);
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled:   ")).disables);
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled: # set below", "enabled: true")).disables);
        assertFalse(TraceClient.parseServerWideConfig(Arrays.asList("enabled: true # on")).disables,
                "a trailing comment after a value is not part of it");
    }

    @Test
    void json_escapesControlCharactersAndSkipsNullTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("ok", "line\nbreak\ttab\\slash");
        tags.put("nullValue", null);
        tags.put(null, "nullKey");

        String json = TraceClient.json("App", "n", Double.NaN, tags);

        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"tags\":{\"ok\":\"line\\nbreak\\ttab\\\\slash\"}}", json,
                "NaN is not JSON and is dropped; null keys or values are skipped");
        assertEquals("\"\\u0001\"", TraceClient.quote("\u0001"));
    }

    @Test
    void json_dropsInfiniteValuesAndKeepsFiniteOnes() {
        // Arrange + Act
        String positive = TraceClient.json("App", "n", Double.POSITIVE_INFINITY, null);
        String negative = TraceClient.json("App", "n", Double.NEGATIVE_INFINITY, null);
        String finite = TraceClient.json("App", "n", -0.5, null);

        // Assert
        assertEquals("{\"application\":\"App\",\"name\":\"n\"}", positive, "Infinity is not JSON and is dropped");
        assertEquals("{\"application\":\"App\",\"name\":\"n\"}", negative, "-Infinity is not JSON and is dropped");
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":-0.5}", finite);
    }

    @Test
    void json_writesValuesAsDoubleToStringDoesWhichIsStillValidJson() {
        // A value goes out as Double.toString: always with a fraction, and in
        // exponent form below 1e-3 and from 1e7 up. JSON allows "1.0E20" and
        // "1.0E-5" (an upper-case E and a signed exponent), so the server
        // must parse both forms.
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":1.0}", TraceClient.json("App", "n", 1.0, null));
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":1234567.0}", TraceClient.json("App", "n", 1234567.0, null));
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":1.0E7}", TraceClient.json("App", "n", 1e7, null));
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":1.0E20}", TraceClient.json("App", "n", 1e20, null));
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":0.001}", TraceClient.json("App", "n", 0.001, null));
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":1.0E-5}", TraceClient.json("App", "n", 1e-5, null));
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":-0.0}", TraceClient.json("App", "n", -0.0, null));
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"value\":1.7976931348623157E308}",
                TraceClient.json("App", "n", Double.MAX_VALUE, null));
    }

    @Test
    void json_omitsTagsWhenNullOrEmpty() {
        // Arrange + Act
        String nullTags = TraceClient.json("App", "n", null, null);
        String emptyTags = TraceClient.json("App", "n", null, Collections.<String, String>emptyMap());

        // Assert
        assertEquals("{\"application\":\"App\",\"name\":\"n\"}", nullTags);
        assertEquals("{\"application\":\"App\",\"name\":\"n\"}", emptyTags);
    }

    @Test
    void json_writesAnEmptyTagsObjectWhenEveryTagIsSkipped() {
        // Characterizes current behaviour: a map that is non-empty but holds
        // only null keys or values still produces a "tags" key, with nothing
        // in it. Valid JSON either way.
        // Arrange
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("nullValue", null);
        tags.put(null, "nullKey");

        // Act
        String json = TraceClient.json("App", "n", null, tags);

        // Assert
        assertEquals("{\"application\":\"App\",\"name\":\"n\",\"tags\":{}}", json);
    }

    @Test
    void quote_escapesCarriageReturnAndEveryOtherControlCharacter() {
        assertEquals("\"a\\rb\"", TraceClient.quote("a\rb"));
        assertEquals("\"\\u0000\"", TraceClient.quote("\u0000"));
        assertEquals("\"\\u0008\"", TraceClient.quote("\b"));
        assertEquals("\"\\u000c\"", TraceClient.quote("\f"));
        assertEquals("\"\\u001f\"", TraceClient.quote("\u001f"));
        assertEquals("\"\"", TraceClient.quote(""));
    }

    @Test
    void quote_passesPrintableAndNonAsciiCharactersThrough() {
        // Only what JSON requires is escaped; the body is sent as UTF-8.
        assertEquals("\" /é€\u007f\"", TraceClient.quote(" /é€\u007f"));
    }

    @Test
    void queue_isBoundedAndDropsRatherThanGrows() throws Exception {
        // Arrange
        // Hold the sending thread on the first report so everything behind it
        // queues, overfill the queue, then let the server drain and count.
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger delivered = new AtomicInteger();
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            delivered.incrementAndGet();
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.flood");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyPlugin", "1.2.3")
                .key("k").logger(logger).build();
        int flood = TraceClient.QUEUE_CAPACITY * 3;

        // Act
        // startup, because it is never coalesced: every one of these goes
        // to the queue, so the queue is all that bounds them.
        for (int i = 0; i < flood; i++) {
            client.report("startup");
        }
        release.countDown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        int seen = -1;
        while (System.nanoTime() < deadline) {
            Thread.sleep(200);
            int now = delivered.get();
            if (now == seen) {
                break; // nothing arrived in the last 200 ms: the queue is drained
            }
            seen = now;
        }

        // Assert
        assertTrue(delivered.get() >= 1, "the first report was in flight and must land");
        assertTrue(delivered.get() <= TraceClient.QUEUE_CAPACITY + 1,
                "delivered " + delivered.get() + " of " + flood + "; at most the in-flight one plus a full queue may survive");
        assertTrue(delivered.get() < flood, "an unbounded queue would have delivered all " + flood);
        client.close();
        // The README says drops are silent. The server only answers 201, so
        // the one other line the send path can write is "could not deliver",
        // for a loopback connection that failed -- one stray line was seen
        // once on Java 8 CI. Allow a few of those, far fewer than there were
        // drops: logging each drop would be hundreds of lines.
        List<String> messages = log.records.stream().map(LogRecord::getMessage).collect(Collectors.toList());
        int dropped = flood - (TraceClient.QUEUE_CAPACITY + 1);
        assertTrue(messages.stream().allMatch(m -> m.startsWith("[trace] could not deliver ")),
                "only a failed send may be logged, but logged: " + messages);
        assertTrue(messages.size() < dropped / 10,
                "a full queue drops without logging, but logged " + messages.size() + " lines: " + messages);
        slow.stop(0);
    }

    @Test
    void close_sendsWhatWasJustQueuedBeforeStopping() throws Exception {
        // A CLI reports once and exits at once. Without draining, the event
        // races the sender thread and is lost a good fraction of the time; 30
        // back-to-back report()+close() pairs make that fraction visible.
        for (int i = 0; i < 30; i++) {
            TraceClient client = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").build();
            client.report("startup", null, Collections.singletonMap("run", String.valueOf(i)));
            client.close();
        }
        assertEquals(30, received.size(), "every report()+close() pair must deliver");
    }

    @Test
    void close_stillReturnsWithinTheTimeoutWhenTheServerHangs() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            try { release.await(15, TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyCli", "1.2.3").key("k").build();
        client.report("startup");

        long before = System.nanoTime();
        client.close();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);

        assertTrue(elapsedMs < 7_000, "close() took " + elapsedMs + " ms; draining must be bounded by the timeout");
        release.countDown();
        slow.stop(0);
    }

    @Test
    void close_whenInterruptedReturnsAtOnceAndKeepsTheInterruptFlag() throws Exception {
        // Arrange
        // A server holding a request, so close() would otherwise wait out
        // the whole drain timeout. A host shutting down on an interrupt must
        // not wait that long, and must still see that it was interrupted.
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            inFlight.countDown();
            try { release.await(15, TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyCli", "1.2.3").key("k").build();
        client.report("startup");
        assertTrue(inFlight.await(5, TimeUnit.SECONDS), "the report should reach the server");

        // Act
        Thread.currentThread().interrupt();
        long before = System.nanoTime();
        client.close();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
        boolean stillInterrupted = Thread.interrupted(); // clears it, so later tests run uninterrupted

        // Assert
        assertTrue(stillInterrupted, "close() must restore the interrupt it swallowed");
        assertTrue(elapsedMs < 2_000, "close() took " + elapsedMs + " ms; an interrupt must cut the drain short");
        release.countDown();
        slow.stop(0);
    }

    @Test
    void report_sendsFromOneDaemonThreadNamedForTheApplication() throws Exception {
        // Arrange
        // A server holding the first request, so the sender thread is alive
        // and busy while the rest queue behind it.
        CountDownLatch inFlight = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            inFlight.countDown();
            try { release.await(15, TimeUnit.SECONDS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "ThreadProbe", "1.2.3")
                .key("k").build();

        // Act
        for (int i = 0; i < 5; i++) {
            client.report("startup");
        }
        assertTrue(inFlight.await(5, TimeUnit.SECONDS), "the first report should reach the server");
        List<Thread> senders = Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals("trace-client/ThreadProbe"))
                .collect(Collectors.toList());

        // Assert
        assertEquals(1, senders.size(), "one sender thread, however many reports wait");
        assertTrue(senders.get(0).isDaemon(), "a daemon thread never keeps the host's JVM from exiting");
        release.countDown();
        client.close();
        slow.stop(0);
    }

    @Test
    void close_isSafeToCallTwice() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        client.report("startup");

        // Act
        client.close();

        // Assert
        assertDoesNotThrow(client::close);
        assertEquals(1, received.size(), "the report queued before the first close() is delivered once");
    }

    @Test
    void report_afterCloseIsDroppedWithoutThrowing() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        client.close();

        // Act
        assertDoesNotThrow(() -> client.report("late"));

        // Assert
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should be sent after close()");
        assertTrue(received.isEmpty());
    }

    @Test
    void report_sendsAUserAgentNamingTheClientAndTheApplication() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();

        // Act
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        String userAgent = received.get(0).userAgent;
        assertTrue(userAgent != null && userAgent.matches("trace-client/\\d+\\.\\d+\\.\\d+ \\(MyPlugin\\)"),
                "unexpected User-Agent: " + userAgent);
        client.close();
    }

    /** What reached a server, as bytes, with the two headers that say how to read them. */
    private static final class RawRequest {
        final byte[] body;
        final String contentType;
        final String contentLength;

        RawRequest(byte[] body, String contentType, String contentLength) {
            this.body = body;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }
    }

    /** Reports {@code name} with {@code tags} to a server of its own and returns the one request it received. */
    private static RawRequest reportAndCaptureRawRequest(String name, Map<String, String> tags) throws Exception {
        List<RawRequest> raw = new CopyOnWriteArrayList<>();
        CountDownLatch got = new CountDownLatch(1);
        HttpServer capturing = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        capturing.createContext("/", exchange -> {
            raw.add(new RawRequest(readAll(exchange.getRequestBody()),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("Content-Length")));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
            got.countDown();
        });
        capturing.start();
        try {
            TraceClient client = TraceClient.builder("http://127.0.0.1:" + capturing.getAddress().getPort(), "MyPlugin", "1.2.3")
                    .key("k").build();
            client.report(name, null, tags);
            assertTrue(got.await(5, TimeUnit.SECONDS), "the report should reach the server");
            client.close();
            return raw.get(0);
        } finally {
            capturing.stop(0);
        }
    }

    @Test
    void report_sendsNonAsciiTextAsUtf8WithALengthCountedInBytes() throws Exception {
        // Arrange
        // A two-byte, a three-byte and a four-byte (surrogate pair) character.
        // The body is streamed with a fixed length: counted in chars rather
        // than bytes, every such report would fail to send.
        Map<String, String> tags = Collections.singletonMap("place", "€ 🏠");
        String expected = "{\"application\":\"MyPlugin\",\"name\":\"démarrage\","
                + "\"tags\":{\"place\":\"€ 🏠\",\"version\":\"1.2.3\"}}";

        // Act
        RawRequest request = reportAndCaptureRawRequest("démarrage", tags);

        // Assert
        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), request.body, "the body is the JSON in UTF-8");
        assertTrue(request.body.length > expected.length(), "the multi-byte characters must have been sent as such");
        assertEquals(String.valueOf(request.body.length), request.contentLength, "the length is counted in bytes");
        assertEquals("application/json; charset=utf-8", request.contentType);
    }

    @Test
    void report_aLoneSurrogateIsSentAsAQuestionMarkRatherThanDroppingTheReport() throws Exception {
        // Arrange
        // Half of a surrogate pair is not text UTF-8 can encode. quote()
        // passes it through, and String.getBytes(UTF_8) then writes its
        // replacement byte -- so the report still goes out, with '?' there.
        Map<String, String> tags = Collections.singletonMap("broken", "a\uD83Cb");

        // Act
        RawRequest request = reportAndCaptureRawRequest("command", tags);

        // Assert
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\","
                        + "\"tags\":{\"broken\":\"a?b\",\"version\":\"1.2.3\"}}",
                new String(request.body, StandardCharsets.UTF_8));
        assertEquals(String.valueOf(request.body.length), request.contentLength);
    }

    @Test
    void report_logsASuccessStatusOtherThan201() throws Exception {
        // Arrange
        // The server answers 201 Created; any other status, even a 2xx, is
        // worth a FINE line because it means the endpoint is not what the
        // client expects.
        replyStatus = 200;
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.status200");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").logger(logger).build();

        // Act
        assertDoesNotThrow(() -> client.report("startup"));

        // Assert
        assertTrue(log.await(5, TimeUnit.SECONDS));
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().startsWith("[trace] trace server answered 200"),
                log.records.get(0).getMessage());
        client.close();
    }

    @Test
    void report_aBaseUrlThatIsNotHttpIsLoggedFineAndNeverThrows() throws Exception {
        // Arrange
        // build() takes the base URL as a string and never parses it, so a
        // typo only shows up in send(): "no protocol" is an IOException, and a
        // file: URL opens a connection that is not an HttpURLConnection -- a
        // ClassCastException, which is why send() catches RuntimeException too.
        String[][] cases = {
                {"not a url", "java.net.MalformedURLException"},
                {"file:///trace-client-test", "java.lang.ClassCastException"},
        };
        for (String[] misconfigured : cases) {
            RecordingHandler log = new RecordingHandler();
            Logger logger = Logger.getLogger("TraceClientTest.misconfigured." + misconfigured[1]);
            logger.setLevel(Level.ALL);
            logger.addHandler(log);
            TraceClient client = assertDoesNotThrow(() ->
                    TraceClient.builder(misconfigured[0], "MyPlugin", "1.2.3").key("k").logger(logger).build());
            assertTrue(client.isEnabled(), "a bad URL is not an opt-out");

            // Act
            assertDoesNotThrow(() -> client.report("startup"));
            client.close();

            // Assert
            assertTrue(log.await(5, TimeUnit.SECONDS), misconfigured[0] + " should be mentioned at FINE");
            LogRecord record = log.records.get(0);
            assertEquals(Level.FINE, record.getLevel());
            assertTrue(record.getMessage().startsWith("[trace] could not deliver {\"application\":\"MyPlugin\""),
                    record.getMessage());
            assertTrue(record.getMessage().contains(misconfigured[1]), record.getMessage());
        }
    }

    @Test
    void report_drainsAResponseBodyAndKeepsSendingAfterAnError() throws Exception {
        // Arrange
        // Unlike the shared server, this one answers with a body: an error page
        // first, then a 201 with content. send() reads either to its end and
        // the next report still goes out.
        List<String> bodies = new CopyOnWriteArrayList<>();
        CountDownLatch both = new CountDownLatch(2);
        HttpServer chatty = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        chatty.createContext("/", exchange -> {
            bodies.add(new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8));
            byte[] reply = new byte[4096]; // more than drain()'s 512-byte buffer
            Arrays.fill(reply, (byte) 'x');
            exchange.sendResponseHeaders(bodies.size() == 1 ? 500 : 201, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
            both.countDown();
        });
        chatty.start();
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.chatty");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + chatty.getAddress().getPort(), "MyPlugin", "1.2.3")
                .key("k").logger(logger).build();

        // Act
        client.report("first");
        client.report("second");
        client.close();

        // Assert
        assertTrue(both.await(5, TimeUnit.SECONDS), "both reports should reach the server");
        assertEquals(2, bodies.size());
        assertTrue(bodies.get(1).contains("\"name\":\"second\""), bodies.get(1));
        assertEquals(1, log.records.size(), "only the 500 is worth a line: " + log.records);
        assertEquals("[trace] trace server answered 500 for "
                        + "{\"application\":\"MyPlugin\",\"name\":\"first\",\"tags\":{\"version\":\"1.2.3\"}}",
                log.records.get(0).getMessage());
        chatty.stop(0);
    }

    @Test
    void report_keepsAPathInTheBaseUrlInFrontOfApiMetrics() throws Exception {
        // Arrange
        // A trace server behind a reverse proxy under a sub-path: only the
        // trailing slashes go, the path itself stays.
        TraceClient client = TraceClient.builder(baseUrl() + "/trace/", "MyPlugin", "1.2.3").key("k").build();

        // Act
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        assertEquals("/trace/api/metrics", received.get(0).path);
        client.close();
    }

    @Test
    void report_aRedirectIsNotFollowedAndIsLoggedFine() throws Exception {
        // Arrange
        // Characterizes current behaviour: the body is streamed with a fixed
        // length, so HttpURLConnection cannot replay it to a redirect target.
        // It throws HttpRetryException instead, so a base URL that redirects
        // (http to https, say) drops every report, and the only trace of it
        // is a FINE "could not deliver" line.
        List<String> paths = new CopyOnWriteArrayList<>();
        HttpServer redirecting = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        redirecting.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            readAll(exchange.getRequestBody());
            exchange.getResponseHeaders().add("Location", "/moved/api/metrics");
            exchange.sendResponseHeaders(301, -1);
            exchange.close();
        });
        redirecting.start();
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.redirect");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + redirecting.getAddress().getPort(), "MyPlugin", "1.2.3")
                .key("k").logger(logger).build();

        // Act
        assertDoesNotThrow(() -> client.report("startup"));

        // Assert
        assertTrue(log.await(5, TimeUnit.SECONDS), "the redirect should be mentioned at FINE");
        client.close();
        assertEquals(Collections.singletonList("/api/metrics"), paths, "the redirect target is never asked");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        String message = log.records.get(0).getMessage();
        assertTrue(message.startsWith("[trace] could not deliver {\"application\":\"MyPlugin\",\"name\":\"startup\""), message);
        assertTrue(message.contains("java.net.HttpRetryException"), message);
        redirecting.stop(0);
    }

    @Test
    void builder_trimsTheBaseUrlAndApplicationAndDropsEveryTrailingSlash() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder("  " + baseUrl() + "///  ", "  MyPlugin  ", "1.2.3").key("k").build();

        // Act
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        assertEquals("/api/metrics", received.get(0).path);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\"}}", received.get(0).body);
        client.close();
    }

    @Test
    void disabledClient_saysWhy() {
        assertEquals(TraceClient.REASON_CONFIG, TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").enabled(false).build().disabledReason());
        assertEquals(TraceClient.REASON_NO_KEY, TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").build().disabledReason());
        assertEquals(TraceClient.REASON_NO_KEY, TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("  ").build().disabledReason());
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build().disabledReason(), "an enabled client has no reason");
    }

    @Test
    void serverWideConfig_isCreatedWithTheExactContentWhenMissing(@TempDir Path plugins) throws Exception {
        // Arrange
        File pluginsDirectory = plugins.toFile();
        Path file = plugins.resolve("trace").resolve("config.yml");
        assertFalse(Files.exists(file));

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(pluginsDirectory).build();

        // Assert
        assertTrue(Files.exists(file), "plugins/trace/config.yml should have been created");
        String expected = "# Server-wide switch for usage reporting by plugins that report to trace\n"
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
                + "#\n"
                + "# server-id: a random ID made on first run and sent as the tag \"install\", so\n"
                + "# trace can count servers, not events. It identifies no person and no IP\n"
                + "# address. Delete the server-id line to get a new one.\n";
        String actual = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue(actual.startsWith(expected), actual);
        String idLine = actual.substring(expected.length());
        assertTrue(idLine.matches("server-id: " + UUID_PATTERN + "\n"), "the template is followed by one server-id line: " + idLine);
        assertEquals(idLine.substring("server-id: ".length()).trim(), client.installId());
        assertTrue(client.isEnabled(), "a freshly created switch file means enabled");
        assertNull(client.disabledReason());
        client.close();
    }

    @Test
    void serverWideConfig_enabledFalseDisablesWithTheServerWideReason(@TempDir Path plugins) throws Exception {
        // Arrange
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        String operatorsFile = "# my notes\n  enabled :   False   # turned off by the operator\nother: true\n";
        Files.write(file, operatorsFile.getBytes(StandardCharsets.UTF_8));

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").enabled(true)
                .serverWideConfig(plugins.toFile()).build();
        client.report("startup");
        client.close();

        // Assert
        assertFalse(client.isEnabled());
        assertEquals("server-wide config: plugins/trace/config.yml", client.disabledReason());
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should have been sent");
        assertEquals(operatorsFile, new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
                "an existing switch file is never rewritten");
    }

    @Test
    void serverWideConfig_onlyClearSpellingsOfOnLeaveReportingOn(@TempDir Path plugins) throws Exception {
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        for (String off : new String[] {"false", "no", "0", "off", "OFF", "No", "\"false\"", "'off'",
                "flase", "disabled", "anything-else"}) {
            Files.write(file, ("enabled: " + off + "\n").getBytes(StandardCharsets.UTF_8));
            assertEquals(TraceClient.REASON_SERVER_WIDE,
                    TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build().disabledReason(),
                    "enabled: " + off + " should disable");
        }
        for (String on : new String[] {"true", "yes", "1", "on", "ON", "\"true\""}) {
            Files.write(file, ("enabled: " + on + "\n").getBytes(StandardCharsets.UTF_8));
            assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build().disabledReason(),
                    "enabled: " + on + " should not disable");
        }
        Files.write(file, "# nothing here\n".getBytes(StandardCharsets.UTF_8));
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build().disabledReason(),
                "a file without an enabled: line means enabled");
    }

    @Test
    void serverWideConfig_theSwitchIgnoresATrailingCommentAndLeadingIndent() {
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled: false # off for now")).disables);
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("  enabled: false")).disables,
                "indented, but outside a tags: block, it is still the switch");
        assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled :\tFALSE")).disables);
    }

    @Test
    void serverWideConfig_aQuotedOffDisables() {
        // Issue #15: the value used to be compared with its quotes on.
        for (String quoted : new String[] {"\"false\"", "'false'", "\"off\"", "'no'"}) {
            assertTrue(TraceClient.parseServerWideConfig(Arrays.asList("enabled: " + quoted)).disables,
                    "enabled: " + quoted);
        }
        assertFalse(TraceClient.parseServerWideConfig(Arrays.asList("enabled: \"true\"")).disables);
    }

    @Test
    void environment_thatCannotBeReadOptsOut() {
        java.util.function.Function<String, String> saved = TraceClient.environment;
        TraceClient.environment = name -> { throw new SecurityException("getenv." + name); };
        try {
            TraceClient client = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build());
            assertEquals(TraceClient.REASON_ENVIRONMENT, client.disabledReason());
            assertDoesNotThrow(() -> TraceClient.disabled());
        } finally {
            TraceClient.environment = saved;
        }
    }

    @Test
    void allowsReporting_failsClosed() {
        assertTrue(TraceClient.allowsReporting(true));
        assertTrue(TraceClient.allowsReporting("true"));
        assertTrue(TraceClient.allowsReporting(" ON "));
        assertTrue(TraceClient.allowsReporting("yes"));
        assertTrue(TraceClient.allowsReporting("1"));
        assertFalse(TraceClient.allowsReporting(false));
        assertFalse(TraceClient.allowsReporting("false"));
        assertFalse(TraceClient.allowsReporting("off"));
        assertFalse(TraceClient.allowsReporting("flase"));
        assertFalse(TraceClient.allowsReporting(""));
        assertFalse(TraceClient.allowsReporting(1));
        assertFalse(TraceClient.allowsReporting(null));
    }

    @Test
    void serverWideConfig_aFileThatIsNotUtf8IsStillObeyed(@TempDir Path plugins) throws Exception {
        // Issue #40: one byte of another encoding used to fail the whole read,
        // so the enabled: false below it was never seen.
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        byte[] latin1 = "# café\nenabled: false\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file, latin1);

        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(plugins.toFile()).build();

        assertEquals(TraceClient.REASON_SERVER_WIDE, client.disabledReason());
        assertArrayEquals(latin1, Files.readAllBytes(file), "the file is left as it is");
        client.close();
    }

    @Test
    void serverWideConfig_aFileThatIsThereButUnreadableDisables(@TempDir Path plugins) throws Exception {
        // A directory where the file should be cannot be read, and may be
        // hiding an opt-out: reporting stays off.
        Files.createDirectories(plugins.resolve("trace").resolve("config.yml"));

        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(plugins.toFile()).build();

        assertEquals(TraceClient.REASON_SERVER_WIDE, client.disabledReason());
    }

    @Test
    void serverWideConfig_aByteOrderMarkHidesAFirstLineEnabledFalseYet(@TempDir Path plugins) throws Exception {
        // Characterizes current behaviour, which issue #40 questions: the
        // mark is not whitespace to ENABLED_LINE, so the line does not match.
        // Arrange
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        Files.write(file, "﻿enabled: false\n".getBytes(StandardCharsets.UTF_8));

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(plugins.toFile()).build();

        // Assert
        assertTrue(client.isEnabled(), "the switch was not recognised");
        client.close();
    }

    @Test
    void environment_disablesAndWinsOverTheServerWideFile(@TempDir Path plugins) throws Exception {
        // Arrange
        // The file says on; the environment says off. The environment wins,
        // and is the reason given.
        TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build().close();
        assertTrue(Files.readAllLines(plugins.resolve("trace").resolve("config.yml"), StandardCharsets.UTF_8).contains("enabled: true"));

        for (String off : new String[] {"off", "OFF", "false", "0", "no", " No "}) {
            environment.clear();
            environment.put("TRACE_USAGE_REPORTING", off);
            TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
            assertFalse(client.isEnabled(), "TRACE_USAGE_REPORTING=" + off + " should disable");
            assertEquals("environment", client.disabledReason());
            client.report("startup");
            client.close();
        }
        for (String yes : new String[] {"1", "true", "TRUE", "yes"}) {
            environment.clear();
            environment.put("DO_NOT_TRACK", yes);
            TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
            assertFalse(client.isEnabled(), "DO_NOT_TRACK=" + yes + " should disable");
            assertEquals("environment", client.disabledReason());
            client.report("startup");
            client.close();
        }
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should have been sent");

        // Values that are not an opt-out leave the client alone.
        environment.clear();
        environment.put("TRACE_USAGE_REPORTING", "on");
        environment.put("DO_NOT_TRACK", "0");
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build().disabledReason());
    }

    @Test
    void environment_eitherVariableAloneTurnsReportingOffWhateverTheOtherSays() {
        // Arrange: one variable opts out, the other says something that is
        // not an opt-out. Neither can cancel the other.
        String[][] pairs = {
                {"on", "1"},
                {"true", " Yes "},
                {"off", "0"},
                {"No", "false"},
        };

        for (String[] pair : pairs) {
            environment.clear();
            environment.put("TRACE_USAGE_REPORTING", pair[0]);
            environment.put("DO_NOT_TRACK", pair[1]);

            // Act
            TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();

            // Assert
            assertEquals(TraceClient.REASON_ENVIRONMENT, client.disabledReason(),
                    "TRACE_USAGE_REPORTING=" + pair[0] + ", DO_NOT_TRACK=" + pair[1]);
            assertFalse(client.isEnabled());
        }
    }

    @Test
    void environment_onlyBlankOrAClearOnLeavesReportingOn() {
        // Fails closed: once a variable is set to anything but blank, only a
        // value that clearly means "keep reporting" leaves it on.
        for (String value : new String[] {"", "   ", "on", "true", "yes", "1", "TRUE"}) {
            environment.clear();
            environment.put("TRACE_USAGE_REPORTING", value);
            TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
            assertNull(client.disabledReason(), "TRACE_USAGE_REPORTING=\"" + value + "\"");
            client.close();
        }
        for (String value : new String[] {"disabled", "offline", "nope", "2"}) {
            environment.clear();
            environment.put("TRACE_USAGE_REPORTING", value);
            assertEquals(TraceClient.REASON_ENVIRONMENT,
                    TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build().disabledReason(),
                    "TRACE_USAGE_REPORTING=\"" + value + "\"");
        }
        for (String value : new String[] {"", "   ", "0", "false", "no", "off"}) {
            environment.clear();
            environment.put("DO_NOT_TRACK", value);
            TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
            assertNull(client.disabledReason(), "DO_NOT_TRACK=\"" + value + "\"");
            client.close();
        }
        for (String value : new String[] {"2", "y", "please"}) {
            environment.clear();
            environment.put("DO_NOT_TRACK", value);
            assertEquals(TraceClient.REASON_ENVIRONMENT,
                    TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build().disabledReason(),
                    "DO_NOT_TRACK=\"" + value + "\"");
        }
    }

    @Test
    void disabled_isOffForTheConfigReasonUnlessTheEnvironmentSaysSoFirst() {
        // Act
        TraceClient byDefault = TraceClient.disabled();

        // Assert
        assertFalse(byDefault.isEnabled());
        assertEquals(TraceClient.REASON_CONFIG, byDefault.disabledReason());
        assertNull(byDefault.installId(), "a disabled client has no install ID");

        // Arrange: the environment is checked before the client's own switch,
        // so it is the reason even here.
        environment.put("DO_NOT_TRACK", "1");

        // Act
        TraceClient byEnvironment = TraceClient.disabled();

        // Assert
        assertFalse(byEnvironment.isEnabled());
        assertEquals(TraceClient.REASON_ENVIRONMENT, byEnvironment.disabledReason());
        byDefault.close();
        byEnvironment.close();
    }

    @Test
    void disabledReason_followsThePrecedenceEnvironmentThenServerWideThenConfigThenKey(@TempDir Path plugins) throws Exception {
        // Arrange: everything says off at once.
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        Files.write(file, "enabled: false\n".getBytes(StandardCharsets.UTF_8));
        environment.put("DO_NOT_TRACK", "1");
        File pluginsDirectory = plugins.toFile();

        // Act + Assert: peel the reasons off one at a time, in order.
        assertEquals("environment",
                TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").enabled(false).serverWideConfig(pluginsDirectory).build().disabledReason());
        environment.clear();
        assertEquals("server-wide config: plugins/trace/config.yml",
                TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").enabled(false).serverWideConfig(pluginsDirectory).build().disabledReason());
        Files.write(file, "enabled: true\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("config.yml",
                TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").enabled(false).serverWideConfig(pluginsDirectory).build().disabledReason());
        assertEquals("no key",
                TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").enabled(true).serverWideConfig(pluginsDirectory).build().disabledReason());
        TraceClient enabled = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").enabled(true).serverWideConfig(pluginsDirectory).build();
        assertNull(enabled.disabledReason());
        assertTrue(enabled.isEnabled());
        enabled.close();
    }

    @Test
    void serverWideTags_areMergedIntoEveryEvent(@TempDir Path plugins) throws Exception {
        // Arrange
        writeServerWideConfig(plugins, "enabled: true\ntags:\n  ci: \"true\"\n");
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("version", "1.2.3");

        // Act
        String body = reportedBody(plugins, "startup", tags);

        // Assert
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"test-server\",\"ci\":\"true\"}}", body);
    }

    @Test
    void serverWideTags_areAddedToAnEventWithNoTagsOfItsOwn(@TempDir Path plugins) throws Exception {
        writeServerWideConfig(plugins, "tags:\n  ci: \"true\"\n");

        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"test-server\",\"ci\":\"true\"}}",
                reportedBody(plugins, "startup", null));
    }

    @Test
    void serverWideTags_neverOverwriteTheEventsOwnTag(@TempDir Path plugins) throws Exception {
        // Arrange
        writeServerWideConfig(plugins, "tags:\n  version: \"9.9.9\"\n  name: overwritten\n  ci: true\n");
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("version", "1.2.3");
        tags.put("name", "home");

        // Act
        String body = reportedBody(plugins, "command", tags);

        // Assert
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"tags\":"
                + "{\"version\":\"1.2.3\",\"name\":\"home\",\"install\":\"test-server\",\"ci\":\"true\"}}", body);
    }

    @Test
    void serverWideTags_acceptDoubleQuotedSingleQuotedAndBareValues() {
        Map<String, String> tags = tagsOf("tags:\n"
                + "  a: \"double \\\"quoted\\\" # not a comment\"\n"
                + "  b: 'single ''quoted'''\n"
                + "  c: bare value   # a comment\n"
                + "  d: true\n"
                + "  e: \"\"\n"
                + "  'f': \"quoted key\"\n"
                + "  g: \"x\"  # comment after a quote\n");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "double \"quoted\" # not a comment");
        expected.put("b", "single 'quoted'");
        expected.put("c", "bare value");
        expected.put("d", "true");
        expected.put("e", "");
        expected.put("f", "quoted key");
        expected.put("g", "x");
        assertEquals(expected, tags);
    }

    @Test
    void serverWideTags_skipBlankAndCommentLinesInsideTheBlock() {
        Map<String, String> tags = tagsOf("tags:   # server-wide\n"
                + "\n"
                + "  # the CI marker\n"
                + "# a comment at column 0 does not end the block either\n"
                + "  ci: \"true\"\n"
                + "   \n"
                + "  env: staging\n");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("ci", "true");
        expected.put("env", "staging");
        assertEquals(expected, tags);
    }

    @Test
    void serverWideTags_blockEndsAtTheNextUnindentedLineOrEndOfFile() {
        // Ends at the next key.
        TraceClient.ServerWideConfig config = TraceClient.parseServerWideConfig(Arrays.asList(
                "tags:", "  ci: \"true\"", "enabled: false", "  stray: x"));
        assertEquals(Collections.singletonMap("ci", "true"), config.tags);
        assertTrue(config.disables, "the enabled: line after the block is still the switch");

        // Ends at end of file, with no trailing newline.
        assertEquals(Collections.singletonMap("ci", "true"), tagsOf("enabled: true\ntags:\n  ci: \"true\""));

        // An enabled: entry inside the block is a tag, not the switch.
        config = TraceClient.parseServerWideConfig(Arrays.asList("tags:", "  enabled: \"false\""));
        assertFalse(config.disables);
        assertEquals(Collections.singletonMap("enabled", "false"), config.tags);

        // Deeper-indented lines are not entries of this block.
        assertEquals(Collections.singletonMap("ci", "true"), tagsOf("tags:\n  ci: \"true\"\n    nested: x\n"));

        // "tags:" must be at column 0, and an empty block is no tags.
        assertTrue(tagsOf("other:\n  tags:\n    ci: \"true\"\n").isEmpty());
        assertTrue(tagsOf("tags:\nenabled: true\n").isEmpty());
    }

    @Test
    void serverWideTags_aSecondTagsBlockAddsToTheFirstAndTheFirstEntryOfAKeyWins() {
        // Arrange
        // Two blocks, as an operator might leave after pasting an example
        // below their own; the second is indented differently from the first.
        String content = "tags:\n"
                + "  ci: \"true\"\n"
                + "  env: staging\n"
                + "enabled: true\n"
                + "tags:\n"
                + "    env: prod\n"
                + "    region: eu\n";

        // Act
        TraceClient.ServerWideConfig config = TraceClient.parseServerWideConfig(Arrays.asList(content.split("\n", -1)));

        // Assert
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("ci", "true");
        expected.put("env", "staging");
        expected.put("region", "eu");
        assertEquals(expected, config.tags, "each block takes its own indent; a key already read is not replaced");
        assertFalse(config.disables);
    }

    @Test
    void serverWideTags_theTagsLineIsCaseSensitiveAndAllowsSpaceBeforeTheColon() {
        // Arrange, Act and Assert
        assertEquals(Collections.singletonMap("ci", "true"), tagsOf("tags :\n  ci: \"true\"\n"));
        assertTrue(tagsOf("Tags:\n  ci: \"true\"\n").isEmpty(), "Tags: is not tags:");
        assertTrue(tagsOf("tags: ci\n  env: prod\n").isEmpty(), "a value on the tags: line is not a block");

        // Under a line that does not open a block, an indented enabled: is
        // outside any tags: block, so it is the switch.
        TraceClient.ServerWideConfig config = TraceClient.parseServerWideConfig(Arrays.asList("Tags:", "  enabled: false"));
        assertTrue(config.disables);
        assertTrue(config.tags.isEmpty());
    }

    @Test
    void serverWideTags_dropEntriesTheServerWouldRejectOrThatAreNotUnderstood() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < TraceClient.MAX_TAG_LENGTH + 1; i++) {
            longText.append('x');
        }
        String exactlyMax = longText.substring(1);
        Map<String, String> tags = tagsOf("tags:\n"
                + "  ci: \"true\"\n"
                + "  " + longText + ": key-too-long\n"
                + "  long: \"" + longText + "\"\n"
                + "  max: " + exactlyMax + "\n"
                + "  \"has space\": x\n"
                + "  \"\": blank-key\n"
                + "  -dash-first: x\n"
                + "  empty:\n"
                + "  comment-only:   # nothing\n"
                + "  a:b\n"
                + "  no colon at all\n"
                + "  broken: \"never closed\n"
                + "  trailing: \"x\" junk\n"
                + "  list: [1, 2]\n"
                + "  map: {a: b}\n"
                + "  block: |\n"
                + "  ok.key_1-2: fine\n"
                + "  ci: \"duplicate, first wins\"\n");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("ci", "true");
        expected.put("max", exactlyMax);
        expected.put("ok.key_1-2", "fine");
        assertEquals(expected, tags);
    }

    @Test
    void serverWideTags_doubleQuotedValuesUnescapeNewlineTabCarriageReturnAndBackslash() {
        Map<String, String> tags = tagsOf("tags:\n"
                + "  a: \"line\\nbreak\\ttab\\rreturn\"\n"
                + "  b: \"back\\\\slash\\/solidus\\qother\"\n"
                + "  c: 'single \\n stays'\n");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "line\nbreak\ttab\rreturn");
        expected.put("b", "back\\slash/solidusqother");
        expected.put("c", "single \\n stays");
        assertEquals(expected, tags);
    }

    @Test
    void serverWideTags_dropKeysAndValuesWhoseQuoteIsNeverClosed() {
        Map<String, String> tags = tagsOf("tags:\n"
                + "  \"open-double: x\n"
                + "  'open-single: x\n"
                + "  \"mismatched': x\n"
                + "  \": lone-quote\n"
                + "  value: \"ends in a backslash\\\n"
                + "  ok: fine\n");
        assertEquals(Collections.singletonMap("ok", "fine"), tags);
    }

    @Test
    void serverWideTags_aSingleQuotedValueTakesACommentButNothingElseAfterIt() {
        Map<String, String> tags = tagsOf("tags:\n"
                + "  a: 'x'  # comment after a quote\n"
                + "  b: 'x' junk\n");
        assertEquals(Collections.singletonMap("a", "x"), tags);
    }

    @Test
    void serverWideTags_dropValuesThatOpenAnAnchorAliasTagOrFoldedScalar() {
        Map<String, String> tags = tagsOf("tags:\n"
                + "  anchor: &a x\n"
                + "  alias: *a\n"
                + "  tag: !!str x\n"
                + "  directive: %x\n"
                + "  at: @x\n"
                + "  backtick: `x\n"
                + "  folded: >\n"
                + "  ok: fine\n");
        assertEquals(Collections.singletonMap("ok", "fine"), tags);
    }

    @Test
    void serverWideTags_aHashIsACommentOnlyAfterWhitespace() {
        Map<String, String> tags = tagsOf("tags:\n"
                + "  a: issue#13\n"
                + "  b: x\t# tab before the hash\n"
                + "\tc: tab-separated\n"
                + "  d:\tafter-a-tab\n");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "issue#13");
        expected.put("b", "x");
        expected.put("d", "after-a-tab");
        assertEquals(expected, tags, "c is indented differently from the block's first entry");
    }

    @Test
    void serverWideTags_areCappedSoTheEventStaysWithinTheServersTagLimit(@TempDir Path plugins) throws Exception {
        // Arrange: 40 server-wide tags, 29 event tags.
        StringBuilder file = new StringBuilder("tags:\n");
        for (int i = 0; i < 40; i++) {
            file.append("  s").append(i).append(": v\n");
        }
        assertEquals(TraceClient.MAX_TAGS, tagsOf(file.toString()).size(), "at most MAX_TAGS are read");
        writeServerWideConfig(plugins, file.toString());
        Map<String, String> tags = new LinkedHashMap<>();
        for (int i = 0; i < 29; i++) {
            tags.put("e" + i, "v");
        }

        // Act
        String body = reportedBody(plugins, "startup", tags);

        // Assert
        int pairs = body.split("\":\"v\"", -1).length - 1;
        assertEquals(TraceClient.MAX_TAGS - 2, pairs, "29 event tags + version + install + 1 server-wide: " + body);
        assertTrue(body.contains("\"e28\":\"v\"") && body.contains("\"version\":\"1.2.3\"")
                && body.contains("\"install\":\"test-server\"")
                && body.contains("\"s0\":\"v\"") && !body.contains("\"s1\""), body);
    }

    @Test
    void serverWideTags_doNotResurrectADisabledClient(@TempDir Path plugins) throws Exception {
        writeServerWideConfig(plugins, "enabled: false\ntags:\n  ci: \"true\"\n");

        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        client.report("startup");
        client.close();

        assertFalse(client.isEnabled());
        assertEquals(TraceClient.REASON_SERVER_WIDE, client.disabledReason());
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should have been sent");
    }

    @Test
    void serverWideTags_areNoneWithoutAServerWideConfigOrAFileThatHasNone(@TempDir Path plugins) throws Exception {
        // No serverWideConfig(...) at all.
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        client.report("startup");
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        client.close();
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\"}}", received.get(0).body);

        // A file with only the switch in it.
        arrived = new CountDownLatch(1);
        writeServerWideConfig(plugins, "enabled: true\n");
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"test-server\"}}", reportedBody(plugins, "startup", null));
    }

    @Test
    void serverWideTags_aFreshlyCreatedFileHasNoActiveTags(@TempDir Path plugins) throws Exception {
        // Created by build() because it was missing ...
        String body = reportedBody(plugins, "startup", null);

        // ... and nothing in it is live: the example is commented out.
        // (The install tag is the server-id build() appended, not a tags: entry.)
        assertTrue(Files.exists(plugins.resolve("trace").resolve("config.yml")));
        assertTrue(body.matches("\\{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":"
                + "\\{\"version\":\"1\\.2\\.3\",\"install\":\"" + UUID_PATTERN + "\"}}"), body);
        TraceClient.ServerWideConfig config = TraceClient.parseServerWideConfig(
                Arrays.asList(TraceClient.SERVER_WIDE_CONFIG_CONTENT.split("\n")));
        assertTrue(config.tags.isEmpty());
        assertFalse(config.disables);
        assertNull(config.serverId, "the template's server-id explanation is a comment, not a value");
    }

    @Test
    void serverWideTags_malformedFileNeverThrowsAndReportingStillWorks(@TempDir Path plugins) throws Exception {
        // Arrange: garbage of every kind.
        String[] contents = {
                "tags",
                "tags:\n  :\n  ::::\n  \"\n  '\n  \\\n\t\tci:\t\"true\n",
                "tags: {ci: true}\n",
                "tags:\n- ci\n- \"true\"\n",
                "\u0000\u0001tags:\n  \u0000: \u0001\n",
        };
        for (String content : contents) {
            received.clear();
            arrived = new CountDownLatch(1);
            writeServerWideConfig(plugins, content);
            String body = assertDoesNotThrow(() -> reportedBody(plugins, "startup", null), content);
            assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"test-server\"}}", body, content);
        }

        // Bytes that are not UTF-8 at all.
        received.clear();
        arrived = new CountDownLatch(1);
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.write(file, new byte[] {'t', 'a', 'g', 's', ':', '\n', ' ', ' ', 'c', 'i', ':', ' ', (byte) 0xC3, (byte) 0x28, '\n'});
        byte[] notUtf8 = Files.readAllBytes(file);
        String body = reportedBody(plugins, "startup", null);
        assertTrue(body.matches("\\{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":"
                        + "\\{\"version\":\"1\\.2\\.3\",\"install\":\"" + UUID_PATTERN + "\",\"ci\":\"\u00c3\\(\"}}"),
                "a file that is not UTF-8 is read as ISO-8859-1, with an in-memory install ID: " + body);
        assertArrayEquals(notUtf8, Files.readAllBytes(file), "a file that cannot be read is never appended to");
    }

    @Test
    void withServerWideTags_leavesTheEventAloneWhenThereAreNone() {
        Map<String, String> tags = Collections.singletonMap("name", "home");
        assertSame(tags, TraceClient.withServerWideTags(tags, Collections.<String, String>emptyMap()));
        assertNull(TraceClient.withServerWideTags(null, Collections.<String, String>emptyMap()));
    }

    @Test
    void withServerWideTags_dropsNullTagsLetsTheEventWinAndNeverModifiesTheCallersMap() {
        // Arrange
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("name", "home");
        tags.put("nullValue", null);
        tags.put(null, "nullKey");
        tags.put("ci", "the-events-own");
        Map<String, String> serverWide = new LinkedHashMap<>();
        serverWide.put("ci", "true");
        serverWide.put("env", "staging");

        // Act
        Map<String, String> merged = TraceClient.withServerWideTags(tags, serverWide);

        // Assert
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("name", "home");
        expected.put("ci", "the-events-own");
        expected.put("env", "staging");
        assertEquals(expected, merged);
        assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(merged.keySet()), "event tags first, then server-wide");
        assertEquals(4, tags.size(), "the caller's map is not modified");
        assertEquals(serverWide, TraceClient.withServerWideTags(null, serverWide), "an event with no tags gets the server-wide ones");
    }

    @Test
    void withServerWideTags_addsNoneToAnEventAlreadyAtOrOverMaxTags() {
        // Arrange
        Map<String, String> atMax = new LinkedHashMap<>();
        for (int i = 0; i < TraceClient.MAX_TAGS; i++) {
            atMax.put("e" + i, "v");
        }
        Map<String, String> overMax = new LinkedHashMap<>(atMax);
        overMax.put("version", "1.2.3"); // what withVersion does today, see issue #13
        Map<String, String> oneShort = new LinkedHashMap<>(atMax);
        oneShort.remove("e0");
        Map<String, String> serverWide = new LinkedHashMap<>();
        serverWide.put("ci", "true");
        serverWide.put("env", "staging");

        // Act + Assert
        assertEquals(atMax, TraceClient.withServerWideTags(atMax, serverWide));
        assertEquals(overMax, TraceClient.withServerWideTags(overMax, serverWide), "the cap adds nothing, and drops nothing either");
        Map<String, String> filled = TraceClient.withServerWideTags(oneShort, serverWide);
        assertEquals(TraceClient.MAX_TAGS, filled.size());
        assertEquals("true", filled.get("ci"));
        assertFalse(filled.containsKey("env"), "only the first server-wide tag fits");
    }

    @Test
    void serverWideConfig_ioFailureIsLoggedFineAndTreatedAsEnabled(@TempDir Path scratch) throws Exception {
        // Arrange
        // A "plugins directory" that is a regular file: trace/config.yml can
        // be neither created nor read under it. (Permission bits are no use
        // here -- the suite may run as root.)
        Path notADirectory = scratch.resolve("plugins");
        Files.write(notADirectory, "not a directory".getBytes(StandardCharsets.UTF_8));
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.serverWideIo");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);

        // Act
        TraceClient client = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(notADirectory.toFile()).logger(logger).build());

        // Assert
        assertTrue(client.isEnabled(), "a switch file that cannot be handled must not turn reporting off");
        assertNull(client.disabledReason());
        assertTrue(log.await(1, TimeUnit.SECONDS), "the failure should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("server-wide config"), log.records.get(0).getMessage());
        client.report("startup");
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "and it still reports");
        client.close();
    }

    @Test
    void serverWideConfig_aPathThatCannotBeConvertedIsLoggedFineAndTreatedAsEnabled() throws Exception {
        // Arrange
        // A NUL in the name makes File.toPath() throw InvalidPathException --
        // a RuntimeException that build() must not let out.
        File unconvertible = new File("plugins\u0000");
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.serverWideInvalidPath");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);

        // Act
        TraceClient client = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(unconvertible).logger(logger).build());

        // Assert
        assertTrue(client.isEnabled(), "a switch file that cannot be located must not turn reporting off");
        assertTrue(log.await(1, TimeUnit.SECONDS), "the failure should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("server-wide config"), log.records.get(0).getMessage());
        client.close();
    }

    @Test
    void installId_isGeneratedPersistedOnceAndReusedAcrossBuilds(@TempDir Path plugins) throws Exception {
        // Arrange
        Path file = plugins.resolve("trace").resolve("config.yml");

        // Act: two starts of the same server, two plugins each time.
        TraceClient first = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        String afterFirst = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        TraceClient second = TraceClient.builder(baseUrl(), "OtherPlugin", "4.5.6").key("k").serverWideConfig(plugins.toFile()).build();
        TraceClient third = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        first.report("startup");
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        first.close();
        second.close();
        third.close();

        // Assert
        String id = first.installId();
        assertNotNull(id);
        assertTrue(id.matches(UUID_PATTERN), id);
        assertEquals(id, second.installId(), "every plugin on the server shares the one ID");
        assertEquals(id, third.installId(), "and it survives a restart");
        assertEquals(afterFirst, new String(Files.readAllBytes(file), StandardCharsets.UTF_8), "only the first build writes");
        assertEquals(1, afterFirst.split("\nserver-id: ", -1).length - 1, afterFirst);
        assertTrue(afterFirst.endsWith("\nserver-id: " + id + "\n"), afterFirst);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"" + id + "\"}}",
                received.get(0).body);
    }

    @Test
    void installId_isAppendedWithoutDisturbingTheOperatorsFile(@TempDir Path plugins) throws Exception {
        // Arrange: hand-edited, comments everywhere, no trailing newline.
        String operatorsFile = "# my notes -- keep me\nenabled: true   # on, for now\ntags:\n  ci: \"true\"  # test box\n# the end";
        Path file = writeServerWideConfigExactly(plugins, operatorsFile);

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        client.close();

        // Assert
        String after = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertEquals(operatorsFile + "\n" + TraceClient.SERVER_ID_COMMENT + "server-id: " + client.installId() + "\n", after,
                "the operator's text is kept byte for byte; one commented block is appended");
        TraceClient.ServerWideConfig reread = TraceClient.parseServerWideConfig(Arrays.asList(after.split("\n", -1)));
        assertEquals(Collections.singletonMap("ci", "true"), reread.tags, "the appended line does not join the tags block");
        assertEquals(client.installId(), reread.serverId);
    }

    @Test
    void installId_anExistingServerIdIsUsedAndAnUnusableOneIsIgnored() {
        assertEquals("abc-123", TraceClient.parseServerWideConfig(Arrays.asList("server-id: \"abc-123\"  # mine")).serverId);
        assertEquals("first", TraceClient.parseServerWideConfig(Arrays.asList("server-id: first", "server-id: second")).serverId);
        assertEquals("good", TraceClient.parseServerWideConfig(Arrays.asList("server-id:", "server-id: has space", "server-id: good")).serverId);
        assertNull(TraceClient.parseServerWideConfig(Arrays.asList("server-id: _mine")).serverId, "starts with a letter or digit");
        assertEquals("good", TraceClient.parseServerWideConfig(Arrays.asList("server-id: -mine", "server-id: good")).serverId);
        StringBuilder overlong = new StringBuilder();
        for (int i = 0; i <= TraceClient.MAX_TAG_LENGTH; i++) {
            overlong.append('a');
        }
        assertNull(TraceClient.parseServerWideConfig(Arrays.asList("server-id: " + overlong)).serverId,
                "at most MAX_TAG_LENGTH characters");
        assertNull(TraceClient.parseServerWideConfig(Arrays.asList("  server-id: indented")).serverId, "column 0 only");
        assertNull(TraceClient.parseServerWideConfig(Arrays.asList("# server-id: commented")).serverId);
        TraceClient.ServerWideConfig inTags = TraceClient.parseServerWideConfig(Arrays.asList("tags:", "  server-id: x"));
        assertNull(inTags.serverId, "inside tags: it is a tag");
        assertEquals(Collections.singletonMap("server-id", "x"), inTags.tags);
    }

    @Test
    void installId_theServerIdLineNeedsNoSpaceAroundItsColonAndIsCaseSensitive() {
        // Unlike a tags: entry, where "a:b" is a string and not a pair, the
        // server-id: line takes its value straight after the colon, and
        // allows space before the colon.
        assertEquals("abc", TraceClient.parseServerWideConfig(Arrays.asList("server-id:abc")).serverId);
        assertEquals("abc", TraceClient.parseServerWideConfig(Arrays.asList("server-id  :  abc")).serverId);
        assertEquals("abc", TraceClient.parseServerWideConfig(Arrays.asList("server-id:\tabc\t")).serverId);

        // The name itself is matched exactly.
        assertNull(TraceClient.parseServerWideConfig(Arrays.asList("Server-Id: abc")).serverId);
        assertNull(TraceClient.parseServerWideConfig(Arrays.asList("server_id: abc")).serverId);
        assertNull(TraceClient.parseServerWideConfig(Arrays.asList("server-ids: abc")).serverId);
    }

    @Test
    void installId_unwritableConfigFallsBackToAnInMemoryIdWithoutThrowing(@TempDir Path scratch) throws Exception {
        // Arrange: a "plugins directory" that is a regular file, so nothing
        // under it can be created (permission bits are no use as root).
        Path notADirectory = scratch.resolve("plugins");
        Files.write(notADirectory, "not a directory".getBytes(StandardCharsets.UTF_8));

        // Act
        TraceClient first = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(notADirectory.toFile()).build());
        TraceClient second = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(notADirectory.toFile()).build();
        first.report("startup");
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        first.close();
        second.close();

        // Assert
        assertTrue(first.isEnabled());
        assertTrue(first.installId().matches(UUID_PATTERN), first.installId());
        assertNotEquals(first.installId(), second.installId(), "nothing persisted: each process gets its own");
        assertTrue(received.get(0).body.contains("\"install\":\"" + first.installId() + "\""), received.get(0).body);
        assertEquals("not a directory", new String(Files.readAllBytes(notADirectory), StandardCharsets.UTF_8));
    }

    @Test
    void installId_aConfigThatCannotBeAppendedToFallsBackToAnInMemoryIdAndIsLoggedFine(@TempDir Path plugins) throws Exception {
        // Arrange
        // The test above fails at the read. To fail at the append, the file
        // has to read fine and refuse a write, even as root: /proc/version
        // does, and has no enabled: or server-id: line in it.
        Path readOnly = Paths.get("/proc/version");
        assumeTrue(Files.isReadable(readOnly), "needs Linux's /proc");
        Path config = Files.createDirectory(plugins.resolve("trace")).resolve("config.yml");
        Files.createSymbolicLink(config, readOnly);
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.serverIdAppend");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);

        // Act
        TraceClient first = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(plugins.toFile()).logger(logger).build());
        TraceClient second = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        first.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        first.close();
        second.close();
        assertTrue(first.isEnabled());
        assertTrue(first.installId().matches(UUID_PATTERN), first.installId());
        assertNotEquals(first.installId(), second.installId(), "nothing persisted: each process gets its own");
        assertTrue(received.get(0).body.contains("\"install\":\"" + first.installId() + "\""), received.get(0).body);
        assertTrue(log.await(1, TimeUnit.SECONDS), "the fallback should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("could not write server-id"), log.records.get(0).getMessage());
    }

    @Test
    void installId_anUnreadableConfigIsLoggedFineAndNotAppendedTo(@TempDir Path scratch) throws Exception {
        // Arrange: as above, a "plugins directory" that is a regular file.
        Path notADirectory = scratch.resolve("plugins");
        Files.write(notADirectory, "not a directory".getBytes(StandardCharsets.UTF_8));
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.unreadableServerId");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(notADirectory.toFile()).logger(logger).build();
        client.close();

        // Assert: both happen on the building thread, so they are already logged.
        assertEquals(2, log.records.size(), log.records.toString());
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().startsWith("[trace] could not create server-wide config "),
                log.records.get(0).getMessage());
        assertEquals(Level.FINE, log.records.get(1).getLevel());
        assertEquals("[trace] using an in-memory server-id for this process: server-wide config was not readable",
                log.records.get(1).getMessage());
    }

    @Test
    void installId_isAppendedToAnEmptyConfigWithoutALeadingBlankLine(@TempDir Path plugins) throws Exception {
        // Arrange: the file exists but is empty, so build() does not create it.
        Path file = writeServerWideConfigExactly(plugins, "");

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        client.close();

        // Assert
        assertTrue(client.isEnabled(), "no enabled: line counts as enabled");
        assertEquals(TraceClient.SERVER_ID_COMMENT + "server-id: " + client.installId() + "\n",
                new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void installId_isAppendedDirectlyBelowAConfigThatEndsInANewline(@TempDir Path plugins) throws Exception {
        // Arrange
        Path file = writeServerWideConfigExactly(plugins, "enabled: true\n");

        // Act
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        client.close();

        // Assert
        assertEquals("enabled: true\n" + TraceClient.SERVER_ID_COMMENT + "server-id: " + client.installId() + "\n",
                new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void installId_anUnusableServerIdLineGetsAUsableOneAppendedAndKept(@TempDir Path plugins) throws Exception {
        // Arrange: an operator's hand-typed ID with a space in it.
        Path file = writeServerWideConfigExactly(plugins, "enabled: true\nserver-id: has space\n");

        // Act
        TraceClient first = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        String afterFirst = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        TraceClient second = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        first.close();
        second.close();

        // Assert
        assertTrue(first.installId().matches(UUID_PATTERN), first.installId());
        assertEquals("enabled: true\nserver-id: has space\n" + TraceClient.SERVER_ID_COMMENT + "server-id: " + first.installId() + "\n",
                afterFirst, "the unusable line is left alone, not rewritten");
        assertEquals(first.installId(), second.installId(), "the appended line is the one read from then on");
        assertEquals(afterFirst, new String(Files.readAllBytes(file), StandardCharsets.UTF_8), "and nothing more is appended");
    }

    @Test
    void installId_aDisabledClientNeitherGeneratesNorWritesOne(@TempDir Path plugins) throws Exception {
        Path file = plugins.resolve("trace").resolve("config.yml");

        // Environment: the file is not even created.
        environment.put("DO_NOT_TRACK", "1");
        TraceClient byEnvironment = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        assertNull(byEnvironment.installId());
        assertFalse(Files.exists(file), "an environment opt-out touches nothing on disk");
        environment.clear();
        environment.put("TRACE_USAGE_REPORTING", "off");
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build().installId());
        assertFalse(Files.exists(file));
        environment.clear();

        // The server-wide switch.
        writeServerWideConfigExactly(plugins, "enabled: false\n");
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build().installId());
        assertEquals("enabled: false\n", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));

        // The plugin's own switch, and no key.
        writeServerWideConfigExactly(plugins, "enabled: true\n");
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").enabled(false).serverWideConfig(plugins.toFile()).build().installId());
        assertNull(TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").serverWideConfig(plugins.toFile()).build().installId());
        assertEquals("enabled: true\n", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));

        // A file created by a disabled client has no server-id line in it.
        Files.delete(file);
        TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").enabled(false).serverWideConfig(plugins.toFile()).build();
        assertEquals(TraceClient.SERVER_WIDE_CONFIG_CONTENT, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));

        // An explicit ID is not sent either.
        assertNull(TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").enabled(false).installId("abc").build().installId());
    }

    @Test
    void installId_canBeGivenExplicitlyByAProgramThatIsNotAPlugin(@TempDir Path plugins) throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").installId("  cli-install-1  ").build();

        // Act
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        client.close();
        assertEquals("cli-install-1", client.installId());
        assertEquals("{\"application\":\"MyCli\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"cli-install-1\"}}",
                received.get(0).body);

        // None given, none sent; blank is none.
        assertNull(TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").build().installId());
        assertNull(TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").installId("   ").build().installId());
        assertNull(TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").installId(null).build().installId());
        StringBuilder overlong = new StringBuilder();
        for (int i = 0; i <= TraceClient.MAX_TAG_LENGTH; i++) {
            overlong.append('x');
        }
        assertThrows(IllegalArgumentException.class,
                () -> TraceClient.builder(baseUrl(), "MyCli", "1.2.3").installId(overlong.toString()));

        // An explicit ID wins over the server-wide file, which then gains no server-id.
        TraceClient both = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").installId("explicit")
                .serverWideConfig(plugins.toFile()).build();
        both.close();
        assertEquals("explicit", both.installId());
        assertEquals(TraceClient.SERVER_WIDE_CONFIG_CONTENT,
                new String(Files.readAllBytes(plugins.resolve("trace").resolve("config.yml")), StandardCharsets.UTF_8));
    }

    @Test
    void builder_rejectsAnOverlongInstallIdAndAcceptsOneAtTheLimit() {
        // Arrange
        StringBuilder max = new StringBuilder();
        for (int i = 0; i < TraceClient.MAX_TAG_LENGTH; i++) {
            max.append('i');
        }
        TraceClient.Builder builder = TraceClient.builder(baseUrl(), "MyCli", "1.2.3");

        // Act
        IllegalArgumentException tooLong = assertThrows(IllegalArgumentException.class,
                () -> builder.installId(max + "i"));
        builder.installId("  " + max + "  ");

        // Assert
        assertEquals("installId is longer than " + TraceClient.MAX_TAG_LENGTH + " characters", tooLong.getMessage());
        TraceClient client = builder.key("k").build();
        assertEquals(max.toString(), client.installId(), "surrounding whitespace does not count");
        client.close();
    }

    @Test
    void installId_anEventsOwnInstallTagWins(@TempDir Path plugins) throws Exception {
        // Arrange
        writeServerWideConfig(plugins, "tags:\n  install: from-tags-block\n");
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("install", "the-plugins-own");

        // Act
        String body = reportedBody(plugins, "startup", tags);

        // Assert
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"install\":\"the-plugins-own\",\"version\":\"1.2.3\"}}", body);
        assertEquals(Collections.singletonMap("install", "the-plugins-own"), tags, "the caller's map is not modified");

        // Without its own, the event gets the server-id, not the tags: block's entry.
        arrived = new CountDownLatch(1);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"test-server\"}}",
                reportedBody(plugins, "startup", null));
    }

    @Test
    void installIdFile_isCreatedOnceWithParentDirectoriesAndReused(@TempDir Path scratch) throws Exception {
        // Arrange
        Path file = scratch.resolve("nested").resolve("deeper").resolve("trace-install-id");

        // Act
        TraceClient first = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").installIdFile(file.toFile()).build();
        first.close();
        TraceClient second = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").installIdFile(file.toFile()).build();
        second.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        second.close();
        String id = first.installId();
        assertTrue(id.matches(UUID_PATTERN), id);
        assertEquals(id + "\n", new String(Files.readAllBytes(file), StandardCharsets.UTF_8), "parent directories are created");
        assertEquals(id, second.installId(), "the next run reuses it");
        assertEquals("{\"application\":\"MyCli\",\"name\":\"startup\",\"tags\":{\"version\":\"1.2.3\",\"install\":\"" + id + "\"}}",
                received.get(0).body);
    }

    @Test
    void installIdFromFile_readsTheFirstValidLineAndNeverRewritesTheFile(@TempDir Path scratch) throws Exception {
        Path file = scratch.resolve("trace-install-id");
        String content = "\n# not an id\n  my-own.id_1  \nsecond-id\n";
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));

        assertEquals("my-own.id_1", TraceClient.installIdFromFile(file.toFile()));
        assertEquals(content, new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void installIdFromFile_replacesAFileWithNoValidLine(@TempDir Path scratch) throws Exception {
        Path file = scratch.resolve("trace-install-id");
        StringBuilder overlong = new StringBuilder();
        for (int i = 0; i <= TraceClient.MAX_TAG_LENGTH; i++) {
            overlong.append('x');
        }
        Files.write(file, ("not an id\n" + overlong + "\n").getBytes(StandardCharsets.UTF_8));

        String made = TraceClient.installIdFromFile(file.toFile());

        assertTrue(made.matches(UUID_PATTERN), made);
        assertEquals(made, TraceClient.installIdFromFile(file.toFile()));
        assertEquals(made + "\n", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test
    void installIdFile_unwritableYieldsAnInMemoryIdWithoutThrowing(@TempDir Path scratch) throws Exception {
        // Arrange: a path under a regular file cannot be created, even as root.
        Path blocker = scratch.resolve("a-file");
        Files.write(blocker, "x".getBytes(StandardCharsets.UTF_8));
        Path file = blocker.resolve("trace-install-id");
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.FINE);
        RecordingHandler handler = new RecordingHandler();
        logger.addHandler(handler);

        // Act
        TraceClient client = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k")
                .installIdFile(file.toFile()).logger(logger).build());
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        client.close();
        assertTrue(client.isEnabled());
        assertTrue(client.installId().matches(UUID_PATTERN), client.installId());
        assertFalse(Files.exists(file));
        assertEquals("x", new String(Files.readAllBytes(blocker), StandardCharsets.UTF_8));
        assertNotEquals(client.installId(), TraceClient.installIdFromFile(file.toFile()), "in memory: a new one each process");
        assertTrue(received.get(0).body.contains("\"install\":\"" + client.installId() + "\""), received.get(0).body);
        assertTrue(handler.records.stream().anyMatch(r -> r.getLevel() == Level.FINE && r.getMessage().contains("install ID file")),
                "the fallback is logged at FINE");
    }

    @Test
    void installIdFile_aWriteThatFailsYieldsAnInMemoryIdAndIsLoggedFine(@TempDir Path scratch) throws Exception {
        // Arrange
        // The test above fails at the read. A link into a directory that does
        // not exist reads as "no such file", so the client goes on to write,
        // and that fails too, even as root.
        assumeTrue(File.separatorChar == '/', "needs POSIX symbolic links");
        Path target = scratch.resolve("missing").resolve("trace-install-id");
        Path link = Files.createSymbolicLink(scratch.resolve("trace-install-id"), target);
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.installIdFileWrite");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);

        // Act
        TraceClient client = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k")
                .installIdFile(link.toFile()).logger(logger).build());
        client.report("startup");

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS));
        client.close();
        assertTrue(client.installId().matches(UUID_PATTERN), client.installId());
        assertTrue(received.get(0).body.contains("\"install\":\"" + client.installId() + "\""), received.get(0).body);
        assertFalse(Files.exists(target));
        assertTrue(log.await(1, TimeUnit.SECONDS), "the fallback should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("could not write install ID file"), log.records.get(0).getMessage());
    }

    @Test
    void installIdFromFile_leavesAnUnreadableFileAlone(@TempDir Path scratch) throws Exception {
        // A directory cannot be read as a file.
        Path directory = Files.createDirectory(scratch.resolve("dir"));

        String made = TraceClient.installIdFromFile(directory.toFile());

        assertTrue(made.matches(UUID_PATTERN), made);
        assertTrue(Files.isDirectory(directory));
        try (java.util.stream.Stream<Path> entries = Files.list(directory)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    void installIdFile_aFileThatIsNotUtf8IsLeftAloneAndAnInMemoryIdIsUsed(@TempDir Path scratch) throws Exception {
        // Arrange
        // An ID line below a comment saved in ISO-8859-1: the 0xE9 byte is
        // not UTF-8, so the whole file fails to read, valid line and all.
        Path file = scratch.resolve("trace-install-id");
        byte[] content = "# café\nmy-own-id\n".getBytes(StandardCharsets.ISO_8859_1);
        Files.write(file, content);
        RecordingHandler log = new RecordingHandler();
        Logger logger = Logger.getLogger("TraceClientTest.installIdFileNotUtf8");
        logger.setLevel(Level.ALL);
        logger.addHandler(log);

        // Act
        TraceClient client = assertDoesNotThrow(() -> TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k")
                .installIdFile(file.toFile()).logger(logger).build());
        client.close();

        // Assert
        assertTrue(client.isEnabled());
        assertTrue(client.installId().matches(UUID_PATTERN), client.installId());
        assertArrayEquals(content, Files.readAllBytes(file), "a file that cannot be read is never overwritten");
        assertNotEquals(client.installId(), TraceClient.installIdFromFile(file.toFile()), "in memory: a new one each process");
        assertTrue(log.await(1, TimeUnit.SECONDS), "the fallback should be mentioned at FINE");
        assertEquals(Level.FINE, log.records.get(0).getLevel());
        assertTrue(log.records.get(0).getMessage().contains("could not read install ID file"), log.records.get(0).getMessage());
    }

    @Test
    void installIdFromFile_neverThrowsForABadPath() {
        for (File file : new File[] {null, new File(""), new File("   "), new File("bad\u0000name")}) {
            String made = assertDoesNotThrow(() -> TraceClient.installIdFromFile(file));
            assertTrue(made.matches(UUID_PATTERN), String.valueOf(file));
        }
    }

    @Test
    void installIdFile_aDisabledClientNeverReadsOrWritesIt(@TempDir Path scratch) throws Exception {
        Path directory = Files.createDirectory(scratch.resolve("data"));
        File file = directory.resolve("trace-install-id").toFile();

        environment.put("DO_NOT_TRACK", "1");
        TraceClient byEnvironment = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").installIdFile(file).build();
        environment.clear();
        environment.put("TRACE_USAGE_REPORTING", "off");
        TraceClient byVariable = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").installIdFile(file).build();
        environment.clear();
        TraceClient byConfig = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k").enabled(false).installIdFile(file).build();
        TraceClient byNoKey = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").installIdFile(file).build();

        for (TraceClient client : Arrays.asList(byEnvironment, byVariable, byConfig, byNoKey)) {
            assertNull(client.installId(), client.disabledReason());
        }
        try (java.util.stream.Stream<Path> entries = Files.list(directory)) {
            assertEquals(0, entries.count(), "nothing is written");
        }
    }

    @Test
    void installIdFile_precedenceIsExplicitThenFileThenServerWide(@TempDir Path scratch) throws Exception {
        Path plugins = scratch.resolve("plugins");
        Path idFile = scratch.resolve("trace-install-id");
        Path serverWide = plugins.resolve("trace").resolve("config.yml");

        // An explicit ID wins over the file, which is then not consulted.
        TraceClient explicit = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k")
                .installId("  abc-123  ").installIdFile(idFile.toFile()).serverWideConfig(plugins.toFile()).build();
        explicit.close();
        assertEquals("abc-123", explicit.installId());
        assertFalse(Files.exists(idFile), "the file is not consulted when an ID is given");

        // The file wins over the server-wide server-id, and the server-wide
        // file gains no server-id line.
        TraceClient fromFile = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k")
                .installIdFile(idFile.toFile()).serverWideConfig(plugins.toFile()).build();
        fromFile.close();
        assertEquals(new String(Files.readAllBytes(idFile), StandardCharsets.UTF_8).trim(), fromFile.installId());
        assertEquals(TraceClient.SERVER_WIDE_CONFIG_CONTENT, new String(Files.readAllBytes(serverWide), StandardCharsets.UTF_8));

        writeServerWideConfigExactly(plugins, "enabled: true\nserver-id: from-server-wide\n");
        TraceClient overServerId = TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k")
                .installIdFile(idFile.toFile()).serverWideConfig(plugins.toFile()).build();
        overServerId.close();
        assertEquals(fromFile.installId(), overServerId.installId());

        // The server-wide switch still disables a client given a file.
        writeServerWideConfigExactly(plugins, "enabled: false\n");
        Files.delete(idFile);
        assertNull(TraceClient.builder(baseUrl(), "MyCli", "1.2.3").key("k")
                .installIdFile(idFile.toFile()).serverWideConfig(plugins.toFile()).build().installId());
        assertFalse(Files.exists(idFile));
    }

    /**
     * Writes {@code content} as the server-wide file, followed by a fixed
     * {@code server-id: test-server} line so the tag tests get a predictable
     * {@code install} tag rather than a random one.
     */
    private static Path writeServerWideConfig(Path plugins, String content) throws java.io.IOException {
        return writeServerWideConfigExactly(plugins, content + "\nserver-id: test-server\n");
    }

    private static Path writeServerWideConfigExactly(Path plugins, String content) throws java.io.IOException {
        Path file = plugins.resolve("trace").resolve("config.yml");
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /** Builds a client over {@code plugins}, reports one event, and returns the body the server got. */
    private String reportedBody(Path plugins, String name, Map<String, String> tags) throws Exception {
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").serverWideConfig(plugins.toFile()).build();
        client.report(name, null, tags);
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the report should reach the server");
        client.close();
        return received.get(received.size() - 1).body;
    }

    private static Map<String, String> tagsOf(String content) {
        return TraceClient.parseServerWideConfig(Arrays.asList(content.split("\n", -1))).tags;
    }

    /** Waits until the shared server has received {@code count} reports, or 5 s pass. */
    private void awaitReceived(int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (received.size() < count && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
    }

    @Test
    void coalesce_sendsTheFirstAtOnceAndTheRepeatsAsOneCountedEventWhenTheWindowCloses() throws Exception {
        // Arrange
        // The flood that prompted this: another plugin dispatching /power
        // every 100 ms, each one a command event.
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .coalesceWindowMillis(1_000).build();
        Map<String, String> power = Collections.singletonMap("name", "power");

        // Act
        for (int i = 0; i < 50; i++) {
            client.report("command", null, power);
        }

        // Assert
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the first report goes out at once");
        Thread.sleep(200);
        assertEquals(1, received.size(), "the repeats wait for the window to close");
        awaitReceived(2);
        assertEquals(2, received.size(), "one event for all 49 repeats");
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"tags\":{\"name\":\"power\",\"version\":\"1.2.3\"}}",
                received.get(0).body);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":49.0,"
                        + "\"tags\":{\"name\":\"power\",\"version\":\"1.2.3\",\"coalesced\":\"49\"}}",
                received.get(1).body);
        client.close();
        assertEquals(2, received.size(), "nothing was left to flush");
    }

    @Test
    void coalesce_aSteadyFloodIsOneEventPerWindowAndAQuietWindowEnds() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .coalesceWindowMillis(300).build();

        // Act
        // About four windows of a report every 10 ms, then quiet.
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1_200);
        int reports = 0;
        while (System.nanoTime() < until) {
            client.report("command", null, Collections.singletonMap("name", "power"));
            reports++;
            Thread.sleep(10);
        }
        long quiet = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (client.coalescing() > 0 && System.nanoTime() < quiet) {
            Thread.sleep(20);
        }

        // Assert
        assertEquals(0, client.coalescing(), "a window that counted nothing ends");
        int sent = received.size();
        assertTrue(sent >= 3 && sent <= 7, "about one event per 300 ms window, but sent " + sent + " for " + reports);
        long total = 1;
        for (Received request : received.subList(1, sent)) {
            java.util.regex.Matcher count = java.util.regex.Pattern.compile("\"coalesced\":\"(\\d+)\"").matcher(request.body);
            assertTrue(count.find(), "every event after the first is a count: " + request.body);
            total += Long.parseLong(count.group(1));
        }
        assertEquals(reports, total, "the counts add up to every report");
        client.close();
        assertEquals(sent, received.size(), "nothing was left to flush");
    }

    @Test
    void coalesce_aWindowThatClosesLeavesOneOpenedLaterToItsOwnDeadline() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .coalesceWindowMillis(2_000).build();
        Map<String, String> early = Collections.singletonMap("name", "early");
        Map<String, String> late = Collections.singletonMap("name", "late");

        // Act
        // Half a window apart, so the flush that closes the first finds the
        // second still open.
        client.report("command", null, early);
        client.report("command", null, early);
        Thread.sleep(1_000);
        client.report("command", null, late);
        client.report("command", null, late);
        awaitReceived(3);

        // Assert
        assertEquals(3, received.size(), "two first reports and the early count");
        assertTrue(received.get(2).body.contains("\"name\":\"early\"") && received.get(2).body.contains("\"coalesced\":\"1\""),
                received.get(2).body);
        Thread.sleep(200);
        assertEquals(3, received.size(), "the late window has not closed yet");
        awaitReceived(4);
        assertEquals(4, received.size(), "it gets a flush of its own");
        assertTrue(received.get(3).body.contains("\"name\":\"late\"") && received.get(3).body.contains("\"coalesced\":\"1\""),
                received.get(3).body);
        client.close();
    }

    @Test
    void close_sendsEveryCountStillOpen() throws Exception {
        // Arrange
        // The real window, a minute: only close() can send these counts.
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        for (int i = 0; i < 10; i++) {
            client.report("command", null, Collections.singletonMap("name", "home"));
        }
        for (int i = 0; i < 3; i++) {
            client.report("command", null, Collections.singletonMap("name", "spawn"));
        }
        client.report("command", null, Collections.singletonMap("name", "once"));

        // Act
        long before = System.nanoTime();
        client.close();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);

        // Assert
        assertTrue(elapsedMs < 5_000, "close() must not wait for the window: took " + elapsedMs + " ms");
        List<String> bodies = received.stream().map(r -> r.body).collect(Collectors.toList());
        assertEquals(5, bodies.size(), "three first reports and two counts: " + bodies);
        assertTrue(bodies.contains("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":9.0,"
                + "\"tags\":{\"name\":\"home\",\"version\":\"1.2.3\",\"coalesced\":\"9\"}}"), bodies.toString());
        assertTrue(bodies.contains("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.0,"
                + "\"tags\":{\"name\":\"spawn\",\"version\":\"1.2.3\",\"coalesced\":\"2\"}}"), bodies.toString());
        assertEquals(1, bodies.stream().filter(b -> b.contains("\"once\"")).count(), "nothing to count, nothing more sent");
    }

    @Test
    void close_aCountThatFindsTheQueueFullIsDropped() throws Exception {
        // Arrange
        // Hold the sending thread on the first report, so nothing behind it
        // leaves the queue until close() has tried to queue the count.
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> bodies = new CopyOnWriteArrayList<>();
        HttpServer slow = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slow.createContext("/", exchange -> {
            bodies.add(new String(readAll(exchange.getRequestBody()), StandardCharsets.UTF_8));
            holding.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        slow.start();
        TraceClient client = TraceClient.builder("http://127.0.0.1:" + slow.getAddress().getPort(), "MyPlugin", "1.2.3")
                .key("k").build();
        client.report("command");
        client.report("command"); // counted, so close() has a count to send
        assertTrue(holding.await(5, TimeUnit.SECONDS), "the first report is in flight");
        for (int i = 0; i < TraceClient.QUEUE_CAPACITY; i++) {
            client.report("startup"); // never coalesced: every one takes a place in the queue
        }

        // Act
        Thread closing = new Thread(client::close);
        closing.start();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (client.coalescing() > 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(0, client.coalescing(), "close() has flushed");
        release.countDown();
        closing.join(10_000);

        // Assert
        assertFalse(closing.isAlive(), "close() returned");
        assertTrue(bodies.size() > 1, "the queue behind the first report drained");
        assertTrue(bodies.stream().noneMatch(b -> b.contains(TraceClient.COALESCED_TAG)), bodies.toString());
        slow.stop(0);
    }

    @Test
    void coalesce_keysOnNameValueAndTagsButNotTagOrder() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        Map<String, String> ab = new LinkedHashMap<>();
        ab.put("a", "1");
        ab.put("b", "2");
        Map<String, String> ba = new LinkedHashMap<>();
        ba.put("b", "2");
        ba.put("a", "1");

        // Act
        client.report("command", null, ab);
        client.report("command", null, ba); // the same event
        client.report("command", 2.5, ab); // a different value: a different event
        client.report("command", 2.5, ab);
        client.report("command", 2.5, ab);
        client.report("other", null, ab); // a different name
        client.report("command", null, Collections.singletonMap("a", "1")); // different tags
        client.report("command", Double.NaN, ab); // NaN is sent as no value: the first event again
        awaitReceived(4);
        Thread.sleep(200);
        int sentAtOnce = received.size();
        client.close();

        // Assert
        assertEquals(4, sentAtOnce, "one first report per distinct event");
        List<String> bodies = received.stream().map(r -> r.body).collect(Collectors.toList());
        assertEquals(6, bodies.size(), bodies.toString());
        assertTrue(bodies.contains("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.0,"
                + "\"tags\":{\"a\":\"1\",\"b\":\"2\",\"version\":\"1.2.3\",\"coalesced\":\"2\"}}"), bodies.toString());
        assertTrue(bodies.contains("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.5,"
                        + "\"tags\":{\"a\":\"1\",\"b\":\"2\",\"version\":\"1.2.3\",\"coalesced\":\"2\"}}"),
                "an event with a value keeps it; the count is the tag: " + bodies);
    }

    /** Starts {@code threads} threads that each run {@code work} with their index, all at once, and waits for them. */
    private static void runAtOnce(int threads, IntConsumer work) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> started = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int index = t;
            Thread thread = new Thread(() -> {
                try {
                    start.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
                work.accept(index);
            });
            thread.start();
            started.add(thread);
        }
        start.countDown();
        for (Thread thread : started) {
            thread.join(10_000);
            assertFalse(thread.isAlive(), "a reporting thread did not finish");
        }
    }

    @Test
    void coalesce_reportsFromManyThreadsAtOnceAreEachCountedExactlyOnce() throws Exception {
        // Arrange
        // report() runs on the host's threads -- a plugin's async tasks
        // beside the main thread -- so the window it shares must neither lose
        // a repeat nor count one twice.
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        Map<String, String> power = Collections.singletonMap("name", "power");
        int threads = 8;
        int each = 500;

        // Act
        // The real window, a minute: everything after the first report is
        // counted until close() sends the count.
        runAtOnce(threads, index -> {
            for (int i = 0; i < each; i++) {
                client.report("command", null, power);
            }
        });
        client.close();

        // Assert
        List<String> bodies = received.stream().map(r -> r.body).collect(Collectors.toList());
        assertEquals(2, bodies.size(), "one first report and one count: " + bodies);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"tags\":{\"name\":\"power\",\"version\":\"1.2.3\"}}",
                bodies.get(0));
        int repeats = threads * each - 1;
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":" + (double) repeats + ","
                        + "\"tags\":{\"name\":\"power\",\"version\":\"1.2.3\",\"coalesced\":\"" + repeats + "\"}}",
                bodies.get(1));
    }

    @Test
    void coalesce_distinctEventsFromManyThreadsAtOnceAreEachSentOnceAndCountedOnce() throws Exception {
        // Arrange
        // Every thread opens windows of its own at the same moment, so the
        // windows map and the queue count are changed from all of them at
        // once. Well under QUEUE_CAPACITY and MAX_COALESCED_EVENTS, so
        // nothing may be dropped.
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        int threads = 8;
        int events = 5;
        assertTrue(2 * threads * events <= TraceClient.QUEUE_CAPACITY);
        assertTrue(threads * events <= TraceClient.MAX_COALESCED_EVENTS);

        // Act
        runAtOnce(threads, index -> {
            for (int repeat = 0; repeat < 3; repeat++) {
                for (int e = 0; e < events; e++) {
                    client.report("command", null, Collections.singletonMap("name", index + "-" + e));
                }
            }
        });
        client.close();

        // Assert
        List<String> bodies = received.stream().map(r -> r.body).collect(Collectors.toList());
        assertEquals(2 * threads * events, bodies.size(), "a first report and a count per event: " + bodies);
        for (int t = 0; t < threads; t++) {
            for (int e = 0; e < events; e++) {
                String tags = "\"tags\":{\"name\":\"" + t + "-" + e + "\",\"version\":\"1.2.3\"";
                assertEquals(1, bodies.stream().filter(b -> b.equals(
                        "{\"application\":\"MyPlugin\",\"name\":\"command\"," + tags + "}}")).count(),
                        "one first report of " + t + "-" + e + ": " + bodies);
                assertEquals(1, bodies.stream().filter(b -> b.equals(
                        "{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.0," + tags + ",\"coalesced\":\"2\"}}")).count(),
                        "one count of 2 for " + t + "-" + e + ": " + bodies);
            }
        }
    }

    @Test
    void coalesce_neverHoldsBackStartup() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();

        // Act
        for (int i = 0; i < 5; i++) {
            client.report("startup");
        }
        awaitReceived(5);

        // Assert
        assertEquals(5, received.size(), "every startup goes out at once");
        assertEquals(0, client.coalescing(), "startup is never counted");
        client.close();
        assertEquals(5, received.size());
    }

    @Test
    void coalesce_countsAtMostMaxCoalescedEventsAndDropsNewOnesBeyond() throws Exception {
        // Arrange
        // Nothing listening, so the sends fail fast and the test is about
        // memory alone: a new name every call must not grow without bound.
        TraceClient client = TraceClient.builder("http://127.0.0.1:1", "MyPlugin", "1.2.3").key("k").build();

        // Act
        for (int i = 0; i < TraceClient.MAX_COALESCED_EVENTS * 4; i++) {
            client.report("command", null, Collections.singletonMap("name", "c" + i));
            client.report("command", null, Collections.singletonMap("name", "c" + i));
        }

        // Assert
        assertEquals(TraceClient.MAX_COALESCED_EVENTS, client.coalescing());
        assertDoesNotThrow(() -> client.report("startup"), "startup still goes to the queue");
        client.close();
        assertEquals(0, client.coalescing(), "close() lets go of every count");
    }

    @Test
    void coalesce_afterCloseNothingIsCountedOrSent() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        client.close();

        // Act
        client.report("command");
        client.report("command");

        // Assert
        assertEquals(0, client.coalescing());
        assertFalse(arrived.await(300, TimeUnit.MILLISECONDS), "nothing should be sent after close()");
    }

    @Test
    void coalesce_infiniteValuesAreTheSameEventAsNoValue() throws Exception {
        // Arrange
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();

        // Act
        client.report("command");
        client.report("command", Double.POSITIVE_INFINITY, null);
        client.report("command", Double.NEGATIVE_INFINITY, null);
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the first report goes out at once");
        Thread.sleep(200);
        int sentAtOnce = received.size();
        int counting = client.coalescing();
        client.close();

        // Assert
        assertEquals(1, sentAtOnce, "json() sends an infinite value as no value, so these are one event");
        assertEquals(1, counting);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.0,"
                + "\"tags\":{\"version\":\"1.2.3\",\"coalesced\":\"2\"}}", received.get(1).body);
    }

    @Test
    void coalesce_aTagWithANullKeyOrValueIsLeftOutOfTheEventAndNeverThrows() throws Exception {
        // Arrange
        // The key is built from a TreeMap, which throws on a null key: this
        // is only safe because withVersion() drops such tags first.
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k").build();
        Map<String, String> withNulls = new HashMap<>();
        withNulls.put("name", "home");
        withNulls.put("nullValue", null);
        withNulls.put(null, "nullKey");

        // Act
        assertDoesNotThrow(() -> client.report("command", null, withNulls));
        assertDoesNotThrow(() -> client.report("command", null, withNulls));
        client.report("command", null, Collections.singletonMap("name", "home"));
        assertTrue(arrived.await(5, TimeUnit.SECONDS), "the first report goes out at once");
        Thread.sleep(200);
        int sentAtOnce = received.size();
        client.close();

        // Assert
        assertEquals(1, sentAtOnce, "the same event as one without the null tags");
        assertEquals(2, received.size(), "and one count for both repeats");
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"tags\":{\"name\":\"home\",\"version\":\"1.2.3\"}}",
                received.get(0).body);
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":2.0,"
                + "\"tags\":{\"name\":\"home\",\"version\":\"1.2.3\",\"coalesced\":\"2\"}}", received.get(1).body);
    }

    @Test
    void coalesce_aCountCarriesTheInstallAndServerWideTagsBeforeItsOwn(@TempDir Path plugins) throws Exception {
        // Arrange
        writeServerWideConfig(plugins, "tags:\n  ci: \"true\"\n");
        TraceClient client = TraceClient.builder(baseUrl(), "MyPlugin", "1.2.3").key("k")
                .serverWideConfig(plugins.toFile()).build();

        // Act
        for (int i = 0; i < 4; i++) {
            client.report("command", null, Collections.singletonMap("name", "home"));
        }
        client.close();

        // Assert
        assertEquals(2, received.size(), "the first report and one count");
        assertEquals("{\"application\":\"MyPlugin\",\"name\":\"command\",\"value\":3.0,"
                        + "\"tags\":{\"name\":\"home\",\"version\":\"1.2.3\",\"install\":\"test-server\",\"ci\":\"true\",\"coalesced\":\"3\"}}",
                received.get(1).body);
    }

    @Test
    void withCount_addsTheCountOnlyWhenThereIsRoomAndNoneAlready() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("version", "1");
        assertEquals("7", TraceClient.withCount(tags, 7).get(TraceClient.COALESCED_TAG));
        assertFalse(tags.containsKey(TraceClient.COALESCED_TAG), "the caller's map is never modified");

        Map<String, String> own = Collections.singletonMap(TraceClient.COALESCED_TAG, "mine");
        assertEquals("mine", TraceClient.withCount(own, 7).get(TraceClient.COALESCED_TAG));

        Map<String, String> full = new LinkedHashMap<>();
        for (int i = 0; i < TraceClient.MAX_TAGS; i++) {
            full.put("t" + i, "v");
        }
        assertEquals(TraceClient.MAX_TAGS, TraceClient.withCount(full, 7).size(), "never past the server's tag limit");
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static final class RecordingHandler extends Handler {
        final List<LogRecord> records = new CopyOnWriteArrayList<>();
        private final CountDownLatch first = new CountDownLatch(1);

        @Override
        public void publish(LogRecord record) {
            records.add(record);
            first.countDown();
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return first.await(timeout, unit);
        }

        @Override public void flush() { }
        @Override public void close() { }
    }
}
