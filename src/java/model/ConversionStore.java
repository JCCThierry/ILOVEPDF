package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConversionStore {

    public static class Meta {
        public final String token;
        public final String filename;
        public final String contentType;
        public final Path outputPath;
        public final long createdAt;

        public Meta(String token, String filename, String contentType, Path outputPath) {
            this.token = token;
            this.filename = filename;
            this.contentType = contentType;
            this.outputPath = outputPath;
            this.createdAt = System.currentTimeMillis();
        }
    }

    private static final Map<String, Meta> DB = new ConcurrentHashMap<>();
    private static final long TTL_MS = 30L * 60L * 1000L; // 30 minutes

    public static void put(Meta meta) {
        cleanup();
        DB.put(meta.token, meta);
    }

    public static Meta get(String token) {
        cleanup();
        return DB.get(token);
    }

    private static void cleanup() {
        long now = System.currentTimeMillis();
        DB.values().removeIf(m -> {
            boolean expired = (now - m.createdAt) > TTL_MS;
            if (expired) {
                try { Files.deleteIfExists(m.outputPath); } catch (IOException ignored) {}
            }
            return expired;
        });
    }
}
