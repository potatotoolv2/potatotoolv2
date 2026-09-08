package com.potatotool.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.potatotool.PotatoToolMod;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefaultArmorColorsLoader {
   private static final Map<String, String> DEFAULT_COLORS = new HashMap<>();
   private static final Set<String> VALUABLE_ITEMS = new HashSet<>();
   private static boolean loaded = false;

   public static void load() {
      if (!loaded) {
         try {
            InputStream colorStream = DefaultArmorColorsLoader.class.getResourceAsStream("/DefaultArmorColors.json");
            if (colorStream != null) {
               InputStreamReader reader = new InputStreamReader(colorStream);
               Gson gson = new Gson();
               JsonObject data = (JsonObject)gson.fromJson(reader, JsonObject.class);

               for (String key : data.keySet()) {
                  JsonObject itemData = data.getAsJsonObject(key);
                  if (itemData.has("hexCode")) {
                     String hexCode = itemData.get("hexCode").getAsString();
                     DEFAULT_COLORS.put(key, hexCode);
                  }
               }

               PotatoToolMod.LOGGER.info("Loaded " + DEFAULT_COLORS.size() + " default armor colors");
               reader.close();
            }

            InputStream valueStream = DefaultArmorColorsLoader.class.getResourceAsStream("/ValuableItems.json");
            if (valueStream != null) {
               InputStreamReader reader = new InputStreamReader(valueStream);
               Gson gson = new Gson();
               JsonObject data = (JsonObject)gson.fromJson(reader, JsonObject.class);
               if (data.has("items")) {
                  data.getAsJsonArray("items").forEach(item -> VALUABLE_ITEMS.add(item.getAsString()));
               }

               PotatoToolMod.LOGGER.info("Loaded " + VALUABLE_ITEMS.size() + " valuable items");
               reader.close();
            }

            loaded = true;
         } catch (Exception e) {
            PotatoToolMod.LOGGER.error("Failed to load default armor colors", e);
         }
      }
   }

   public static String getDefaultColor(String itemId) {
      if (!loaded) {
         load();
      }

      if (itemId == null) {
         return null;
      }

      String direct = DEFAULT_COLORS.get(itemId);
      if (direct != null) {
         return direct;
      }

      String normalized = normalizeItemIdKey(itemId);
      return DEFAULT_COLORS.get(normalized);
   }

   public static boolean isValuableItem(String itemId) {
      if (!loaded) {
         load();
      }

      return VALUABLE_ITEMS.contains(itemId);
   }

   public static boolean shouldIgnoreColor(String itemId) {
      String defaultColor = getDefaultColor(itemId);
      return "ignore".equals(defaultColor);
   }

   public static boolean hasCustomColor(String itemId, String currentHex) {
      if (currentHex == null) {
         return false;
      }

      String defaultColor = getDefaultColor(itemId);
      if (defaultColor == null) {
         return false;
      }

      if ("ignore".equals(defaultColor)) {
         return false;
      }

      if ("leather".equals(defaultColor)) {
         return !currentHex.equalsIgnoreCase("#A06540");
      }

      if ("fairy".equals(defaultColor)) {
         return true;
      }

      if ("crystal".equals(defaultColor)) {
         return true;
      }

      String normalizedCurrent = currentHex.replace("#", "").toUpperCase();
      String normalizedDefault = defaultColor.replace("#", "").toUpperCase();
      return !normalizedCurrent.equals(normalizedDefault);
   }

   public static Set<String> getKnownItemIds() {
      if (!loaded) {
         load();
      }

      return new HashSet<>(DEFAULT_COLORS.keySet());
   }

   private static String normalizeItemIdKey(String raw) {
      return raw == null ? "" : raw.toUpperCase().replace("MINECRAFT:", "").trim();
   }
}
