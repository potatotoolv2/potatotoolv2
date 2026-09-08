package com.potatotool.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EditionedCollectibles {
   private static final Pattern ID_EDITION_SUFFIX = Pattern.compile("_(\\d{1,6})$");

   private EditionedCollectibles() {
   }

   public static boolean isSpaceHelmet(String itemId, String displayName) {
      String id = norm(itemId);
      if (id.contains("SPACE_HELM") || id.contains("SPACE_HELMET")) {
         return true;
      }

      String name = displayName == null ? "" : displayName.toLowerCase();
      return name.contains("space helmet");
   }

   public static boolean isBasketOfHope(String itemId, String displayName) {
      String id = norm(itemId);
      if (id.equals("POTATO_BASKET") || id.equals("BASKET_OF_HOPE") || id.contains("BASKET_OF_HOPE")) {
         return true;
      }

      String name = displayName == null ? "" : displayName.toLowerCase();
      return name.contains("basket of hope");
   }

   public static boolean isTracked(String itemId, String displayName) {
      return isSpaceHelmet(itemId, displayName) || isBasketOfHope(itemId, displayName);
   }

   public static Integer editionFromItemId(String itemId) {
      if (itemId == null || itemId.isBlank()) {
         return null;
      }

      Matcher m = ID_EDITION_SUFFIX.matcher(itemId.trim());
      if (!m.find()) {
         return null;
      }

      try {
         int n = Integer.parseInt(m.group(1));
         return n > 0 ? n : null;
      } catch (NumberFormatException ignored) {
         return null;
      }
   }

   public static Integer parsePositiveInt(String raw) {
      if (raw == null || raw.isBlank()) {
         return null;
      }

      String digits = raw.replaceAll("[^0-9]", "");
      if (digits.isEmpty()) {
         return null;
      }

      try {
         int n = Integer.parseInt(digits);
         return n > 0 ? n : null;
      } catch (NumberFormatException ignored) {
         return null;
      }
   }

   public static String formatEditionSerial(Integer edition, String serial) {
      String serialClean = serial == null ? "" : serial.trim();
      String editionText = edition != null && edition > 0 ? String.valueOf(edition) : "";
      if (serialClean.isEmpty() && !editionText.isEmpty()) {
         serialClean = editionText;
      }

      if (editionText.isEmpty() && serialClean.isEmpty()) {
         return "";
      }

      if (!editionText.isEmpty() && !serialClean.isEmpty()) {
         if (editionText.equals(serialClean)) {
            return "Edition " + editionText + " · Serial " + serialClean;
         }

         return "Edition " + editionText + " · Serial " + serialClean;
      }

      return !editionText.isEmpty() ? "Edition " + editionText : "Serial " + serialClean;
   }

   private static String norm(String itemId) {
      return itemId == null ? "" : itemId.toUpperCase().replace('-', '_').trim();
   }
}
