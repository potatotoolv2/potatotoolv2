package com.potatotool.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potatotool.model.ScannedItem;

public final class ItemNames {
   private ItemNames() {
   }

   public static String resolve(String rawName, String itemId) {
      String fromRaw = plainText(rawName);
      if (!fromRaw.isEmpty() && !looksLikeRawItemId(fromRaw, itemId)) {
         return fromRaw;
      }

      String fromId = formatItemId(itemId);
      if (!fromId.isEmpty()) {
         return fromId;
      }

      return fromRaw.isEmpty() ? "Unknown Item" : fromRaw;
   }

   private static boolean looksLikeRawItemId(String name, String itemId) {
      if (name == null || name.isEmpty()) {
         return true;
      }

      if (itemId != null && name.equalsIgnoreCase(itemId)) {
         return true;
      }

      return name.equals(name.toUpperCase()) && name.contains("_");
   }

   public static String resolve(ScannedItem item) {
      if (item == null) {
         return "Unknown Item";
      }

      return resolve(item.getItemName(), item.getItemId());
   }

   public static String plainText(String raw) {
      if (raw == null || raw.isBlank()) {
         return "";
      }

      String s = raw.trim();
      s = s.replace("\\u0026", "&").replace("\\u00a7", "§").replace("\\u00A7", "§");
      if ((s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) || (s.startsWith("'") && s.endsWith("'") && s.length() >= 2)) {
         s = s.substring(1, s.length() - 1);
      }

      s = s.replace("\\\"", "\"").replace("\\/", "/");
      String stripped = s.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
      if (stripped.contains("text")) {
         try {
            if (stripped.startsWith("{") || stripped.startsWith("[")) {
               String extracted = walkJsonText(JsonParser.parseString(stripped)).replaceAll("\\s+", " ").trim();
               if (!extracted.isEmpty()) {
                  return extracted;
               }
            }
         } catch (Exception ignored) {
         }

         String fromSnbt = extractSnbtText(stripped);
         if (!fromSnbt.isEmpty()) {
            return fromSnbt;
         }
      }

      if (stripped.startsWith("{") || stripped.startsWith("[")) {
         return "";
      }

      return stripped;
   }

   private static String extractSnbtText(String raw) {
      StringBuilder out = new StringBuilder();
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("text\\s*[:=]\\s*\"([^\"]*)\"").matcher(raw);
      while (m.find()) {
         out.append(m.group(1));
      }

      if (out.length() == 0) {
         m = java.util.regex.Pattern.compile("text\\s*[:=]\\s*'([^']*)'").matcher(raw);
         while (m.find()) {
            out.append(m.group(1));
         }
      }

      return out.toString().replaceAll("\\s+", " ").trim();
   }

   private static String walkJsonText(JsonElement el) {
      if (el == null || el.isJsonNull()) {
         return "";
      }

      if (el.isJsonPrimitive()) {
         return el.getAsString();
      }

      StringBuilder out = new StringBuilder();
      if (el.isJsonArray()) {
         JsonArray arr = el.getAsJsonArray();
         for (JsonElement child : arr) {
            out.append(walkJsonText(child));
         }
      } else if (el.isJsonObject()) {
         JsonObject obj = el.getAsJsonObject();
         if (obj.has("text") && obj.get("text").isJsonPrimitive()) {
            out.append(obj.get("text").getAsString());
         }

         if (obj.has("extra") && obj.get("extra").isJsonArray()) {
            out.append(walkJsonText(obj.get("extra")));
         }
      }

      return out.toString();
   }

   public static String formatItemId(String itemId) {
      if (itemId == null || itemId.isBlank()) {
         return "";
      }

      String[] parts = itemId.replace('-', '_').split("_");
      StringBuilder formatted = new StringBuilder();
      for (String part : parts) {
         if (part == null || part.isEmpty()) {
            continue;
         }

         if (formatted.length() > 0) {
            formatted.append(' ');
         }

         formatted.append(Character.toUpperCase(part.charAt(0)));
         if (part.length() > 1) {
            formatted.append(part.substring(1).toLowerCase());
         }
      }

      return formatted.toString();
   }
}
