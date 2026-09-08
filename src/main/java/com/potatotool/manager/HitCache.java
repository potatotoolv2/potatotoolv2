package com.potatotool.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.potatotool.PotatoToolMod;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;

public final class HitCache {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Type LIST_TYPE = new TypeToken<List<HitCache.Hit>>() {}.getType();
   private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("potato-tool-v2-hits.json");
   private static final int MAX = 1000;
   private static final List<Hit> hits = new ArrayList<>();
   private static boolean loaded;

   private HitCache() {
   }

   public static synchronized void record(ScannedPlayer player) {
      if (player == null || player.getUsername() == null || player.getUsername().isBlank() || !player.hasDisplayableItems()) {
         return;
      }

      load();
      String name = player.getUsername();
      String key = name.toLowerCase(Locale.ROOT);
      hits.removeIf(h -> h.username != null && h.username.toLowerCase(Locale.ROOT).equals(key));
      hits.add(0, new Hit(name, player.getUuid(), System.currentTimeMillis(), (int)player.getSkyblockLevel(), summarize(player)));
      while (hits.size() > MAX) {
         hits.remove(hits.size() - 1);
      }

      save();
   }

   public static synchronized List<Hit> snapshot() {
      load();
      return new ArrayList<>(hits);
   }

   public static synchronized void clear() {
      hits.clear();
      save();
   }

   public static synchronized int size() {
      load();
      return hits.size();
   }

   private static String summarize(ScannedPlayer player) {
      StringBuilder sb = new StringBuilder();
      for (ScannedItem item : player.getItems()) {
         if (item == null || !item.isSpecial()) {
            continue;
         }

         String tag = item.getCategoryTag();
         if (tag == null || tag.isBlank()) {
            continue;
         }

         if (sb.indexOf(tag) >= 0) {
            continue;
         }

         if (sb.length() > 0) {
            sb.append(", ");
         }

         sb.append(tag);
         if (sb.length() > 80) {
            break;
         }
      }

      return sb.toString();
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
         List<Hit> parsed = GSON.fromJson(reader, LIST_TYPE);
         if (parsed != null) {
            hits.clear();
            hits.addAll(parsed);
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.warn("Failed to load hit cache: " + e.getMessage());
      }
   }

   private static void save() {
      try {
         Files.createDirectories(PATH.getParent());
         try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(hits, writer);
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.warn("Failed to save hit cache: " + e.getMessage());
      }
   }

   public static final class Hit {
      public String username;
      public String uuid;
      public long at;
      public int level;
      public String summary;

      public Hit() {
      }

      public Hit(String username, String uuid, long at, int level, String summary) {
         this.username = username;
         this.uuid = uuid;
         this.at = at;
         this.level = level;
         this.summary = summary;
      }
   }
}
