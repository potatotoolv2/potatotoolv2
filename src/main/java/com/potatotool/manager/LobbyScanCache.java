package com.potatotool.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.potatotool.PotatoToolMod;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class LobbyScanCache {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Type LIST_TYPE = new TypeToken<List<LobbyScanCache.Lobby>>() {}.getType();
   private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("potato-tool-v2-lobbies.json");
   private static final int MAX = 200;
   private static final List<Lobby> lobbies = new ArrayList<>();
   private static boolean loaded;

   private LobbyScanCache() {
   }

   public static synchronized void record(int attempted, int scanned, int skipped, int noData, int hits) {
      if (attempted <= 0 && skipped <= 0 && scanned <= 0) {
         return;
      }

      load();
      lobbies.add(0, new Lobby(System.currentTimeMillis(), attempted, scanned, skipped, noData, hits));
      while (lobbies.size() > MAX) {
         lobbies.remove(lobbies.size() - 1);
      }

      save();
   }

   public static synchronized List<Lobby> snapshot() {
      load();
      return new ArrayList<>(lobbies);
   }

   private static void load() {
      if (loaded) {
         return;
      }

      loaded = true;
      if (!Files.isRegularFile(PATH)) {
         return;
      }

      try (Reader reader = Files.newBufferedReader(PATH)) {
         List<Lobby> parsed = GSON.fromJson(reader, LIST_TYPE);
         if (parsed != null) {
            lobbies.clear();
            lobbies.addAll(parsed);
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.warn("Failed to load lobby scan cache: " + e.getMessage());
      }
   }

   private static void save() {
      try {
         Files.createDirectories(PATH.getParent());
         try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(lobbies, writer);
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.warn("Failed to save lobby scan cache: " + e.getMessage());
      }
   }

   public static final class Lobby {
      public long at;
      public int attempted;
      public int scanned;
      public int skipped;
      public int noData;
      public int hits;

      public Lobby() {
      }

      public Lobby(long at, int attempted, int scanned, int skipped, int noData, int hits) {
         this.at = at;
         this.attempted = attempted;
         this.scanned = scanned;
         this.skipped = skipped;
         this.noData = noData;
         this.hits = hits;
      }
   }
}
