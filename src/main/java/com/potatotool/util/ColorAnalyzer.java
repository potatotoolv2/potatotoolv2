package com.potatotool.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ColorAnalyzer {
   private static final Set<String> BASIC_LEATHER_ARMOR = Set.of("LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS");
   private static final Set<String> OG_FAIRY_HEXES = new HashSet<>();
   private static final Set<String> CRYSTAL_HEXES = new HashSet<>();
   private static final Set<String> FAIRY_HEXES = new HashSet<>();
   private static final Set<String> BLEACHED_HEXES = new HashSet<>();
   private static final Map<String, String> NECRON_HEXES = new HashMap<>();
   private static final Map<String, String> STORM_HEXES = new HashMap<>();
   private static final Map<String, String> GOLDOR_HEXES = new HashMap<>();
   private static final Map<String, String> MAXOR_HEXES = new HashMap<>();
   private static final Set<String> DUNGEON_ARMOR_IDS = new HashSet<>();

   private static boolean isBasicLeatherArmor(String itemId) {
      if (itemId != null && !itemId.isEmpty()) {
         String n = itemId.toUpperCase().replace("MINECRAFT:", "").trim();
         return BASIC_LEATHER_ARMOR.contains(n)
            ? true
            : n.contains("LEATHER") && (n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("HELMET") || n.contains("BOOTS"));
      } else {
         return false;
      }
   }

   public static boolean isGlitchedDungeonArmor(String itemId, String hexColor) {
      return isGlitchedDungeonArmor(itemId, hexColor, null);
   }

   public static boolean isGlitchedDungeonArmor(String itemId, String hexColor, String cosmeticDyeToken) {
      if (itemId != null && hexColor != null && !hexColor.isEmpty()) {
         String normalizedItemId = normalizeItemId(itemId);
         if (!isWitherArmorPiece(normalizedItemId)) {
            return false;
         }

         String normalized = hexColor.replace("#", "").toUpperCase();
         if ("000000".equals(normalized)) {
            return !isPureBlackDyeToken(cosmeticDyeToken);
         }

         String pieceType = detectPieceTypeSuffix(normalizedItemId);
         String setKey = detectWitherSet(normalizedItemId);
         if (pieceType != null && setKey != null) {
            Set<String> otherSetsColors = new HashSet<>();

            for (String candidateSet : new String[]{"POWER_WITHER", "WISE_WITHER", "TANK_WITHER", "SPEED_WITHER"}) {
               if (!candidateSet.equals(setKey)) {
                  String c = getWitherSetPieceColor(candidateSet, pieceType);
                  if (c != null && !c.isEmpty()) {
                     otherSetsColors.add(c);
                  }
               }
            }

            return otherSetsColors.contains(normalized);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isBlackWitherArmorByName(String itemName, String hexColor, String cosmeticDyeToken) {
      if (itemName != null && hexColor != null) {
         String normalized = hexColor.replace("#", "").toUpperCase().trim();
         if (!"000000".equals(normalized)) {
            return false;
         }

         if (isPureBlackDyeToken(cosmeticDyeToken)) {
            return false;
         }

         String n = itemName.toLowerCase();
         return !n.contains("chestplate") && !n.contains("leggings") && !n.contains("boots")
            ? false
            : n.contains("necron") || n.contains("storm") || n.contains("goldor") || n.contains("maxor");
      } else {
         return false;
      }
   }

   public static boolean isPureBlackDyeToken(String rawToken) {
      if (rawToken != null && !rawToken.isEmpty()) {
         String normalized = normalizeDyeToken(rawToken);
         return normalized.contains("PURE_BLACK");
      } else {
         return false;
      }
   }

   public static boolean isFairyDyeToken(String rawToken) {
      if (rawToken != null && !rawToken.isEmpty()) {
         String normalized = normalizeDyeToken(rawToken);
         return normalized.contains("OG_FAIRY") ? false : normalized.contains("FAIRY");
      } else {
         return false;
      }
   }

   public static boolean isCrystalArmorId(String itemId) {
      String n = normalizeItemId(itemId);
      return n.startsWith("CRYSTAL_")
         && (n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("BOOTS"));
   }

   public static boolean isAnniversaryHatId(String itemId) {
      String n = normalizeItemId(itemId);
      return n.equals("PARTY_HAT_CRAB")
         || n.equals("PARTY_HAT_CRAB_ANIMATED")
         || n.equals("PARTY_HAT_SLOTH")
         || n.equals("BALLOON_HAT_2024")
         || n.equals("BALLOON_HAT_2025")
         || n.equals("SALMON_HAT")
         || n.startsWith("PARTY_HAT_CRAB")
         || n.startsWith("BALLOON_HAT_");
   }

   public static boolean isFairyArmorId(String itemId) {
      String n = normalizeItemId(itemId);
      return n.startsWith("FAIRY_")
         && (n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("BOOTS"));
   }

   public static boolean isUniqueOgFairyColor(String hexColor) {
      if (!isOGFairyColor(hexColor)) {
         return false;
      }

      return !isFairyColor(hexColor);
   }

   public static boolean isOgFairyMatch(String hexColor, String itemId, long timestampMs) {
      if (isFairyArmorId(itemId)) {
         return false;
      }

      if (isUniqueOgFairyColor(hexColor)) {
         return true;
      }

      return timestampMs > 0L && timestampMs < 1601510400000L && isFairyColor(hexColor);
   }

   public static boolean isOgFairyDyeToken(String rawToken) {
      if (rawToken != null && !rawToken.isEmpty()) {
         String normalized = normalizeDyeToken(rawToken);
         return normalized.contains("OG_FAIRY");
      } else {
         return false;
      }
   }

   private static String normalizeDyeToken(String rawToken) {
      return rawToken == null ? "" : rawToken.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
   }

   private static String normalizeItemId(String itemId) {
      return itemId == null ? "" : itemId.toUpperCase().replace("MINECRAFT:", "").trim();
   }

   private static String detectPieceTypeSuffix(String normalizedItemId) {
      if (normalizedItemId.endsWith("_CHESTPLATE")) {
         return "_CHESTPLATE";
      } else if (normalizedItemId.endsWith("_LEGGINGS")) {
         return "_LEGGINGS";
      } else {
         return normalizedItemId.endsWith("_BOOTS") ? "_BOOTS" : null;
      }
   }

   private static String detectWitherSet(String normalizedItemId) {
      if (normalizedItemId.startsWith("POWER_WITHER") || normalizedItemId.startsWith("NECRON")) {
         return "POWER_WITHER";
      } else if (normalizedItemId.startsWith("WISE_WITHER") || normalizedItemId.startsWith("STORM")) {
         return "WISE_WITHER";
      } else if (normalizedItemId.startsWith("TANK_WITHER") || normalizedItemId.startsWith("GOLDOR")) {
         return "TANK_WITHER";
      } else {
         return !normalizedItemId.startsWith("SPEED_WITHER") && !normalizedItemId.startsWith("MAXOR") ? null : "SPEED_WITHER";
      }
   }

   private static boolean isWitherArmorPiece(String normalizedItemId) {
      if (normalizedItemId == null || normalizedItemId.isEmpty()) {
         return false;
      } else if (detectPieceTypeSuffix(normalizedItemId) == null) {
         return false;
      } else {
         return DUNGEON_ARMOR_IDS.contains(normalizedItemId) ? true : detectWitherSet(normalizedItemId) != null;
      }
   }

   private static String getWitherSetPieceColor(String setKey, String pieceSuffix) {
      return switch (setKey) {
         case "POWER_WITHER" -> (String)NECRON_HEXES.get("POWER_WITHER" + pieceSuffix);
         case "WISE_WITHER" -> (String)STORM_HEXES.get("WISE_WITHER" + pieceSuffix);
         case "TANK_WITHER" -> (String)GOLDOR_HEXES.get("TANK_WITHER" + pieceSuffix);
         case "SPEED_WITHER" -> (String)MAXOR_HEXES.get("SPEED_WITHER" + pieceSuffix);
         default -> null;
      };
   }

   private static final double FAIRY_MATCH_DISTANCE = 10.0;

   public static String normalizeHex(String hexColor) {
      if (hexColor == null || hexColor.isBlank()) {
         return null;
      }

      String s = hexColor.trim().replace("#", "").toUpperCase();
      if (s.contains(":")) {
         String[] parts = s.split(":");
         if (parts.length != 3) {
            return null;
         }

         try {
            int r = Math.max(0, Math.min(255, Integer.parseInt(parts[0].trim())));
            int g = Math.max(0, Math.min(255, Integer.parseInt(parts[1].trim())));
            int b = Math.max(0, Math.min(255, Integer.parseInt(parts[2].trim())));
            return String.format("%06X", r << 16 | g << 8 | b);
         } catch (NumberFormatException e) {
            return null;
         }
      }

      s = s.replaceAll("[^0-9A-F]", "");
      if (s.length() == 6 && isHex6(s)) {
         return s;
      }

      if (s.matches("[0-9]+")) {
         try {
            long v = Long.parseLong(s);
            if (v >= 0L && v <= 16777215L) {
               return String.format("%06X", (int)v);
            }
         } catch (NumberFormatException ignored) {
         }
      }

      return null;
   }

   private static boolean isHex6(String s) {
      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F')) {
            return false;
         }
      }

      return true;
   }

   private static boolean matchesHexSet(String hexColor, Set<String> hexes, double maxDistance) {
      String normalized = normalizeHex(hexColor);
      if (normalized == null) {
         return false;
      }

      if (hexes.contains(normalized)) {
         return true;
      }

      for (String candidate : hexes) {
         if (calculateColorDifference(normalized, candidate) <= maxDistance) {
            return true;
         }
      }

      return false;
   }

   public static boolean isFairyColor(String hexColor) {
      return matchesHexSet(hexColor, FAIRY_HEXES, FAIRY_MATCH_DISTANCE);
   }

   public static boolean isOGFairyColor(String hexColor) {
      return matchesHexSet(hexColor, OG_FAIRY_HEXES, FAIRY_MATCH_DISTANCE);
   }

   public static boolean isCrystalColor(String hexColor) {
      String normalized = normalizeHex(hexColor);
      return normalized != null && CRYSTAL_HEXES.contains(normalized);
   }

   public static boolean isBleachedColor(String hexColor) {
      return isBleachedHex(hexColor);
   }

   public static boolean isBleachedColor(String hexColor, String itemId) {
      if (!isBleachedHex(hexColor)) {
         return false;
      }

      if (isBasicLeatherArmor(itemId) || isDefaultFarmingArmorId(itemId)) {
         return false;
      }

      String defaultColor = DefaultArmorColorsLoader.getDefaultColor(itemId);
      if (defaultColor == null || defaultColor.isBlank()) {
         return false;
      }

      if ("leather".equalsIgnoreCase(defaultColor)
         || "ignore".equalsIgnoreCase(defaultColor)
         || "fairy".equalsIgnoreCase(defaultColor)
         || "crystal".equalsIgnoreCase(defaultColor)) {
         return false;
      }

      String normalizedDefault = defaultColor.replace("#", "").toUpperCase();
      return !BLEACHED_HEXES.contains(normalizedDefault);
   }

   private static boolean isBleachedHex(String hexColor) {
      if (hexColor != null && !hexColor.isEmpty()) {
         String normalized = hexColor.replace("#", "").toUpperCase();
         return BLEACHED_HEXES.contains(normalized);
      } else {
         return false;
      }
   }

   public static boolean isStandardColor(String hexColor, String itemId) {
      if (hexColor == null || hexColor.isEmpty()) {
         return true;
      }

      if (!DefaultArmorColorsLoader.hasCustomColor(itemId, hexColor)) {
         return true;
      }

      String normalized = hexColor.replace("#", "").toUpperCase();
      Set<String> standardColors = new HashSet<>();
      standardColors.add("A06540");
      standardColors.add("FFFFFF");
      standardColors.add("000000");
      standardColors.add("FF0000");
      standardColors.add("00FF00");
      standardColors.add("0000FF");
      standardColors.add("FFFF00");
      standardColors.add("00FFFF");
      standardColors.add("FF00FF");
      return standardColors.contains(normalized);
   }

   public static boolean isExoticColor(String hexColor, String itemId) {
      return isExoticColor(hexColor, itemId, false, null);
   }

   public static boolean isExoticColor(String hexColor, String itemId, boolean hasCosmeticDye, String cosmeticDyeToken) {
      if (hasCosmeticDye || isNonFairyCosmeticDye(cosmeticDyeToken)) {
         return false;
      }

      if (hexColor != null && !hexColor.isEmpty()) {
         String normalized = hexColor.replace("#", "").toUpperCase();
         if (isRancherBootsDefaultColor(itemId, normalized)) {
            return false;
         } else if (isBasicLeatherArmor(itemId) || isDefaultFarmingArmorId(itemId)) {
            return false;
         } else {
            return isStandardColor(hexColor, itemId)
               ? false
               : !isCrystalColor(hexColor) && !isFairyColor(hexColor) && !isOGFairyColor(hexColor) && !isBleachedColor(hexColor, itemId);
         }
      } else {
         return false;
      }
   }

   public static boolean isNonFairyCosmeticDye(String cosmeticDyeToken) {
      if (cosmeticDyeToken == null || cosmeticDyeToken.isBlank()) {
         return false;
      }

      return !isFairyDyeToken(cosmeticDyeToken) && !isOgFairyDyeToken(cosmeticDyeToken);
   }

   public static boolean isDefaultFarmingArmorId(String itemId) {
      String n = normalizeItemId(itemId);
      if (n.isEmpty()) {
         return false;
      }

      return n.startsWith("FARM_SUIT_")
         || n.startsWith("FARM_ARMOR_")
         || n.startsWith("FARMHAND_")
         || n.startsWith("HAYMAKER_")
         || n.startsWith("PUMPKIN_")
         || n.startsWith("SPROUT_")
         || n.startsWith("MELON_")
         || n.startsWith("TATER_")
         || n.startsWith("CROPIE_")
         || n.startsWith("SQUASH_")
         || n.startsWith("FERMENTO_")
         || n.startsWith("HELIANTHUS_")
         || n.startsWith("FIG_")
         || n.startsWith("CANOPY_");
   }

   private static boolean isRancherBootsDefaultColor(String itemId, String normalizedHex) {
      if (itemId != null && normalizedHex != null) {
         String id = normalizeItemId(itemId);
         String hex = normalizeHex(normalizedHex);
         return !id.contains("RANCHERS_BOOTS") ? false : "BB5500".equals(hex) || "CC5500".equals(hex);
      } else {
         return false;
      }
   }

   public static double calculateColorDifference(String hex1, String hex2) {
      hex1 = hex1.replace("#", "");
      hex2 = hex2.replace("#", "");

      try {
         int color1 = Integer.parseInt(hex1, 16);
         int color2 = Integer.parseInt(hex2, 16);
         int r1 = color1 >> 16 & 0xFF;
         int g1 = color1 >> 8 & 0xFF;
         int b1 = color1 & 0xFF;
         int r2 = color2 >> 16 & 0xFF;
         int g2 = color2 >> 8 & 0xFF;
         int b2 = color2 & 0xFF;
         return Math.sqrt(Math.pow(r1 - r2, 2.0) + Math.pow(g1 - g2, 2.0) + Math.pow(b1 - b2, 2.0));
      } catch (NumberFormatException e) {
         return 0.0;
      }
   }

   public static int hexToInt(String hexColor) {
      hexColor = hexColor.replace("#", "");

      try {
         return Integer.parseInt(hexColor, 16);
      } catch (NumberFormatException e) {
         return 16777215;
      }
   }

   public static String intToHex(int color) {
      return String.format("#%06X", color & 16777215);
   }

   static {
      NECRON_HEXES.put("POWER_WITHER_CHESTPLATE", "E7413C");
      NECRON_HEXES.put("POWER_WITHER_LEGGINGS", "E75C3C");
      NECRON_HEXES.put("POWER_WITHER_BOOTS", "E76E3C");
      STORM_HEXES.put("WISE_WITHER_CHESTPLATE", "1793C4");
      STORM_HEXES.put("WISE_WITHER_LEGGINGS", "17A8C4");
      STORM_HEXES.put("WISE_WITHER_BOOTS", "1CD4E4");
      GOLDOR_HEXES.put("TANK_WITHER_CHESTPLATE", "45413C");
      GOLDOR_HEXES.put("TANK_WITHER_LEGGINGS", "65605A");
      GOLDOR_HEXES.put("TANK_WITHER_BOOTS", "88837E");
      MAXOR_HEXES.put("SPEED_WITHER_CHESTPLATE", "4A14B7");
      MAXOR_HEXES.put("SPEED_WITHER_LEGGINGS", "5D2FB9");
      MAXOR_HEXES.put("SPEED_WITHER_BOOTS", "8969C8");
      DUNGEON_ARMOR_IDS.addAll(NECRON_HEXES.keySet());
      DUNGEON_ARMOR_IDS.addAll(STORM_HEXES.keySet());
      DUNGEON_ARMOR_IDS.addAll(GOLDOR_HEXES.keySet());
      DUNGEON_ARMOR_IDS.addAll(MAXOR_HEXES.keySet());
      OG_FAIRY_HEXES.add("FFCCFF");
      OG_FAIRY_HEXES.add("FF99FF");
      OG_FAIRY_HEXES.add("E5CCFF");
      OG_FAIRY_HEXES.add("CC99FF");
      OG_FAIRY_HEXES.add("FF66FF");
      OG_FAIRY_HEXES.add("FF33FF");
      OG_FAIRY_HEXES.add("B266FF");
      OG_FAIRY_HEXES.add("9933FF");
      OG_FAIRY_HEXES.add("FF00FF");
      OG_FAIRY_HEXES.add("CC00CC");
      OG_FAIRY_HEXES.add("7F00FF");
      OG_FAIRY_HEXES.add("6600CC");
      OG_FAIRY_HEXES.add("990099");
      OG_FAIRY_HEXES.add("660066");
      OG_FAIRY_HEXES.add("4C0099");
      OG_FAIRY_HEXES.add("330066");
      OG_FAIRY_HEXES.add("FFCCE5");
      OG_FAIRY_HEXES.add("FF99CC");
      OG_FAIRY_HEXES.add("FF66B2");
      OG_FAIRY_HEXES.add("CC0066");
      OG_FAIRY_HEXES.add("99004C");
      OG_FAIRY_HEXES.add("660033");
      CRYSTAL_HEXES.add("FCF3FF");
      CRYSTAL_HEXES.add("EFE1F5");
      CRYSTAL_HEXES.add("E5D1ED");
      CRYSTAL_HEXES.add("D9C1E3");
      CRYSTAL_HEXES.add("C6A3D4");
      CRYSTAL_HEXES.add("B88BC9");
      CRYSTAL_HEXES.add("A875BD");
      CRYSTAL_HEXES.add("9C64B3");
      CRYSTAL_HEXES.add("8E51A6");
      CRYSTAL_HEXES.add("7E4196");
      CRYSTAL_HEXES.add("6A2C82");
      CRYSTAL_HEXES.add("63237D");
      CRYSTAL_HEXES.add("5D1C78");
      CRYSTAL_HEXES.add("54146E");
      CRYSTAL_HEXES.add("46085E");
      CRYSTAL_HEXES.add("1F0030");
      FAIRY_HEXES.add("FFCCE5");
      FAIRY_HEXES.add("FF99CC");
      FAIRY_HEXES.add("FF66B2");
      FAIRY_HEXES.add("FF3399");
      FAIRY_HEXES.add("FF007F");
      FAIRY_HEXES.add("CC0066");
      FAIRY_HEXES.add("99004C");
      FAIRY_HEXES.add("660033");
      BLEACHED_HEXES.add("A06540");
   }
}
