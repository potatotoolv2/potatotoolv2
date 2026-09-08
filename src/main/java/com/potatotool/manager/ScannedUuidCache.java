package com.potatotool.manager;

import com.potatotool.PotatoToolMod;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;

public final class ScannedUuidCache {
   private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("potato-tool-v2-scanned.csv");
   private static final String HEADER = "uuid,username,timestamp";
   private static final Object LOCK = new Object();
   private static final Set<String> UUIDS = ConcurrentHashMap.newKeySet();
   private static boolean loaded;

   private ScannedUuidCache() {
   }

   public static void load() {
      synchronized (LOCK) {
         if (loaded) {
            return;
         }

         loaded = true;
         try {
            Files.createDirectories(PATH.getParent());
            if (!Files.isRegularFile(PATH)) {
               Files.writeString(PATH, HEADER + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
               seedFromHitCache();
               PotatoToolMod.LOGGER.info("Created scan cache CSV (" + UUIDS.size() + " players): " + PATH);
               return;
            }

            List<String> lines = Files.readAllLines(PATH, StandardCharsets.UTF_8);
            for (String raw : lines) {
               String uuid = parseUuid(raw);
               if (uuid != null) {
                  UUIDS.add(uuid);
               }
            }

            PotatoToolMod.LOGGER.info("Loaded " + UUIDS.size() + " scanned players from " + PATH.getFileName());
         } catch (Exception e) {
            PotatoToolMod.LOGGER.warn("Failed to load scan cache CSV: " + e.getMessage());
         }
      }
   }

   public static boolean contains(String uuid) {
      load();
      String normalized = normalize(uuid);
      return normalized != null && UUIDS.contains(normalized);
   }

   public static void record(String uuid, String username) {
      load();
      String normalized = normalize(uuid);
      if (normalized == null) {
         return;
      }

      synchronized (LOCK) {
         if (!UUIDS.add(normalized)) {
            return;
         }

         String name = username == null ? "" : username.replace(",", " ").replace("\"", "").trim();
         String line = normalized + "," + name + "," + System.currentTimeMillis() + System.lineSeparator();
         try {
            Files.writeString(PATH, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         } catch (Exception e) {
            UUIDS.remove(normalized);
            PotatoToolMod.LOGGER.warn("Failed to append scan cache CSV: " + e.getMessage());
         }
      }
   }

   public static int size() {
      load();
      return UUIDS.size();
   }

   public static Path path() {
      return PATH;
   }

   private static void seedFromHitCache() {
      try {
         for (HitCache.Hit hit : HitCache.snapshot()) {
            String uuid = normalize(hit == null ? null : hit.uuid);
            if (uuid == null || !UUIDS.add(uuid)) {
               continue;
            }

            String name = hit.username == null ? "" : hit.username.replace(",", " ").replace("\"", "").trim();
            long at = hit.at > 0L ? hit.at : System.currentTimeMillis();
            String line = uuid + "," + name + "," + at + System.lineSeparator();
            Files.writeString(PATH, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.warn("Could not seed scan cache from hits: " + e.getMessage());
      }
   }

   private static String parseUuid(String raw) {
      if (raw == null) {
         return null;
      }

      String line = raw.trim();
      if (line.isEmpty() || line.startsWith("uuid") || line.startsWith("#")) {
         return null;
      }

      int comma = line.indexOf(',');
      String uuid = comma < 0 ? line : line.substring(0, comma);
      return normalize(uuid);
   }

   private static String normalize(String uuid) {
      if (uuid == null || uuid.isBlank()) {
         return null;
      }

      String n = uuid.replace("-", "").trim().toLowerCase(Locale.ROOT);
      return n.length() == 32 ? n : (n.isEmpty() ? null : n);
   }
}
