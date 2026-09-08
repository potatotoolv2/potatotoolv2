package com.potatotool.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potatotool.PotatoToolMod;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class ValuableItemsLoader {
   private static final Set<String> NEVER_VALUABLE = Set.of("EMBER_ROD", "ENDER_RELIC", "ENDER_ARTIFACT", "WITHER_ARTIFACT");
   private static Set<String> valuableItems = new HashSet<>();
   private static boolean loaded = false;

   public static void load() {
      if (!loaded) {
         try {
            InputStream stream = ValuableItemsLoader.class.getResourceAsStream("/ValuableItems.json");
            if (stream == null) {
               PotatoToolMod.LOGGER.error("Could not find ValuableItems.json");
               return;
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            JsonArray itemsArray = json.getAsJsonArray("items");

            for (int i = 0; i < itemsArray.size(); i++) {
               valuableItems.add(itemsArray.get(i).getAsString());
            }

            PotatoToolMod.LOGGER.info("Loaded " + valuableItems.size() + " valuable items");
            loaded = true;
         } catch (Exception e) {
            PotatoToolMod.LOGGER.error("Failed to load valuable items", e);
         }
      }
   }

   public static boolean isValuable(String itemId) {
      if (!loaded) {
         load();
      }

      if (itemId == null || itemId.isBlank()) {
         return false;
      }

      String id = itemId.toUpperCase();
      if (NEVER_VALUABLE.contains(id)) {
         return false;
      }

      if (valuableItems.contains(itemId) || valuableItems.contains(id)) {
         return true;
      }

      return id.startsWith("DCTR_SPACE_HELM")
         || id.equals("GARDEN_SPACE_HELMET")
         || id.equals("RAFFLE_SPACE_HELMET")
         || id.equals("BASKET_OF_HOPE");
   }
}
