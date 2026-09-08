package com.potatotool.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedItem;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SeymourAnalyzer {
   private static final double DELTA_T0 = 1.0;
   private static final double DELTA_T1 = 2.0;
   private static final double DELTA_T2 = 5.0;
   private static final Map<String, String> TARGET_COLORS = new LinkedHashMap<>();
   private static final Map<String, String> FADE_DYES = new LinkedHashMap<>();
   private static final Map<String, SeymourAnalyzer.LAB> LAB_CACHE = new ConcurrentHashMap<>();
   private static final Pattern STAGE_PATTERN = Pattern.compile("(?i)\\bstage\\s*(\\d{1,3})\\b");
   private static final List<SeymourAnalyzer.MatchPriority> DEFAULT_PRIORITY_ORDER = Arrays.asList(
      SeymourAnalyzer.MatchPriority.SEARCH,
      SeymourAnalyzer.MatchPriority.DUPE,
      SeymourAnalyzer.MatchPriority.WORD,
      SeymourAnalyzer.MatchPriority.PATTERN,
      SeymourAnalyzer.MatchPriority.NORMAL_T0,
      SeymourAnalyzer.MatchPriority.NORMAL_T1,
      SeymourAnalyzer.MatchPriority.NORMAL_T2,
      SeymourAnalyzer.MatchPriority.FADE_T0,
      SeymourAnalyzer.MatchPriority.FADE_T1,
      SeymourAnalyzer.MatchPriority.FADE_T2
   );

   private SeymourAnalyzer() {
   }

   public static SeymourAnalyzer.PieceType detectPieceType(String displayName) {
      return detectPieceType(displayName, null);
   }

   public static SeymourAnalyzer.PieceType detectPieceType(String displayName, String itemId) {
      SeymourAnalyzer.PieceType fromName = detectPieceTypeFromText(displayName);
      if (fromName != null) {
         return fromName;
      }

      SeymourAnalyzer.PieceType fromIdName = detectPieceTypeFromText(itemId);
      if (fromIdName != null) {
         return fromIdName;
      }

      SeymourAnalyzer.PieceType fromId = detectPieceTypeFromItemId(itemId);
      return fromId != null ? fromId : detectPieceTypeFromItemId(displayName);
   }

   private static SeymourAnalyzer.PieceType detectPieceTypeFromText(String displayName) {
      if (displayName == null || displayName.isEmpty()) {
         return null;
      }

      String clean = stripFormatting(displayName).toLowerCase();
      if (clean.contains("velvet top hat")) {
         return SeymourAnalyzer.PieceType.TOP_HAT;
      } else if (clean.contains("cashmere jacket")) {
         return SeymourAnalyzer.PieceType.JACKET;
      } else if (clean.contains("satin trousers")) {
         return SeymourAnalyzer.PieceType.TROUSERS;
      } else {
         return clean.contains("oxford shoes") ? SeymourAnalyzer.PieceType.SHOES : null;
      }
   }

   private static SeymourAnalyzer.PieceType detectPieceTypeFromItemId(String itemId) {
      if (itemId == null || itemId.isBlank()) {
         return null;
      }

      String id = itemId.toUpperCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
      if (id.contains("VELVET_TOP_HAT") || id.contains("VELVETTOPHAT")) {
         return SeymourAnalyzer.PieceType.TOP_HAT;
      } else if (id.contains("CASHMERE_JACKET") || id.contains("CASHMEREJACKET")) {
         return SeymourAnalyzer.PieceType.JACKET;
      } else if (id.contains("SATIN_TROUSERS") || id.contains("SATINTROUSERS")) {
         return SeymourAnalyzer.PieceType.TROUSERS;
      } else if (id.contains("OXFORD_SHOES") || id.contains("OXFORDSHOES")) {
         return SeymourAnalyzer.PieceType.SHOES;
      } else {
         return null;
      }
   }

   public static boolean isPieceEnabled(ScannerConfig config, SeymourAnalyzer.PieceType pieceType) {
      if (config != null && pieceType != null) {
         return switch (pieceType) {
            case TOP_HAT -> config.seymourScanTopHat;
            case JACKET -> config.seymourScanJacket;
            case TROUSERS -> config.seymourScanTrousers;
            case SHOES -> config.seymourScanShoes;
         };
      } else {
         return false;
      }
   }

   public static boolean isTierEnabled(ScannerConfig config, int tier) {
      if (config == null) {
         return false;
      }

      return switch (tier) {
         case 1 -> config.seymourScanT1;
         case 2 -> config.seymourScanT2;
         default -> false;
      };
   }

   public static SeymourAnalyzer.MatchResult classifyTier(String hexColor, String itemDisplayName, ScannerConfig config) {
      return classifyTier(hexColor, itemDisplayName, null, config);
   }

   public static SeymourAnalyzer.MatchResult classifyTier(String hexColor, String itemDisplayName, String itemId, ScannerConfig config) {
      if (hexColor != null && config != null) {
         String sourceHex = normalizeHex(hexColor);
         if (sourceHex == null) {
            return null;
         }

         SeymourAnalyzer.PieceType pieceType = detectPieceType(itemDisplayName, itemId);
         if (pieceType == null) {
            return null;
         }

         List<SeymourAnalyzer.ColorMatch> normalMatches = new ArrayList<>();
         List<SeymourAnalyzer.ColorMatch> fadeMatches = new ArrayList<>();
         if (config.seymourScanTargetColors) {
            normalMatches = findMatchesInMap(sourceHex, pieceType, TARGET_COLORS, false, config);
            normalMatches.removeIf(m -> isStagedFadeName(m.name));
            normalMatches.sort(Comparator.comparingDouble(m -> m.deltaE));
         }

         List<SeymourAnalyzer.ColorMatch> merged = new ArrayList<>();
         merged.addAll(limit(normalMatches, 5));
         merged.addAll(limit(fadeMatches, 5));
         merged.sort(Comparator.comparingDouble(m -> m.deltaE));
         List<SeymourAnalyzer.ColorMatch> top10 = limit(merged, 10);
         if (top10.isEmpty()) {
            return null;
         }

         List<SeymourAnalyzer.ColorMatch> exact = new ArrayList<>();
         List<SeymourAnalyzer.ColorMatch> nonExact = new ArrayList<>();

         for (SeymourAnalyzer.ColorMatch m : top10) {
            if (m.deltaE < 0.01) {
               exact.add(m);
            } else {
               nonExact.add(m);
            }
         }

         List<SeymourAnalyzer.ColorMatch> prioritized = new ArrayList<>();
         List<SeymourAnalyzer.ColorMatch> high = new ArrayList<>();

         for (SeymourAnalyzer.ColorMatch m : nonExact) {
            if (m.rawTier <= 2) {
               prioritized.add(m);
            } else {
               high.add(m);
            }
         }

         prioritized.sort((a, b) -> {
            int pa = getPriorityIndex(getMatchPriority(a));
            int pb = getPriorityIndex(getMatchPriority(b));
            return pa != pb ? Integer.compare(pa, pb) : Double.compare(a.deltaE, b.deltaE);
         });
         high.sort(Comparator.comparingDouble(m -> m.deltaE));
         List<SeymourAnalyzer.ColorMatch> ordered = new ArrayList<>();
         ordered.addAll(exact);
         ordered.addAll(prioritized);
         ordered.addAll(high);
         if (ordered.isEmpty()) {
            return null;
         }

         SeymourAnalyzer.ColorMatch best = ordered.get(0);
         int mappedTier = mapToDisplayTier(best.rawTier, best.isFade);
         return new SeymourAnalyzer.MatchResult(
            mappedTier, best.rawTier, best.deltaE, best.absoluteDistance, best.name, best.targetHex, best.isCustom, best.isFade
         );
      } else {
         return null;
      }
   }

   public static boolean passesScanFilters(String sourceHex, SeymourAnalyzer.MatchResult match, ScannerConfig config) {
      if (match != null && config != null) {
         if (config.seymourExactHexOnly) {
            String source = normalizeHex(sourceHex);
            String target = normalizeHex(match.getMatchedHex());
            if (source == null || target == null || !source.equals(target)) {
               return false;
            }
         }

         int targetStage = Math.max(0, config.seymourTargetStage);
         if (targetStage > 0) {
            int matchedStage = extractStageNumber(match.getMatchedName());
            if (matchedStage <= 0) {
               return false;
            }

            int tolerance = Math.max(0, config.seymourStageTolerance);
            if (Math.abs(matchedStage - targetStage) > tolerance) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static String prettyMatchLabel(String raw) {
      if (raw == null || raw.isBlank()) {
         return "";
      }

      String name = raw.trim().replaceAll("\\s+", " ");
      String lower = name.toLowerCase();
      String[] suffixes = new String[]{"chestplate", "leggings", "helmet", "boots", "trousers", "jacket", "hat", "hood", "cap"};
      for (String suffix : suffixes) {
         if (lower.endsWith(" " + suffix)) {
            String set = name.substring(0, name.length() - suffix.length()).trim();
            if (set.isEmpty()) {
               return name;
            }

            String setLower = set.toLowerCase();
            if (setLower.endsWith("dye") || setLower.endsWith("armor")) {
               return set;
            }

            return set + " Armor";
         }
      }

      return name;
   }

   public static ScannedItem.ItemCategory toCategory(int tier) {
      return switch (tier) {
         case 1 -> ScannedItem.ItemCategory.SEYMOUR_T1;
         case 2 -> ScannedItem.ItemCategory.SEYMOUR_T2;
         default -> ScannedItem.ItemCategory.SEYMOUR_T3;
      };
   }

   public static String normalizeHex(String raw) {
      if (raw == null) {
         return null;
      }

      String s = raw.replace("#", "").trim().toUpperCase();
      if (s.length() != 6) {
         return null;
      }

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         boolean ok = c >= '0' && c <= '9' || c >= 'A' && c <= 'F';
         if (!ok) {
            return null;
         }
      }

      return s;
   }

   public static String detectSpecialPattern(String hexColor) {
      String hex = normalizeHex(hexColor);
      if (hex == null) {
         return null;
      } else {
         char[] c = hex.toCharArray();
         if (c[0] == c[1] && c[2] == c[3] && c[4] == c[5]) {
            return "paired";
         } else if (c[0] == c[3] && c[1] == c[4] && c[2] == c[5]) {
            return "repeating";
         } else if (c[0] == c[5] && c[1] == c[4] && c[2] == c[3]) {
            return "palindrome";
         } else {
            return c[0] == c[2] && c[2] == c[4] ? "axbxcx_" + Character.toUpperCase(c[0]) : null;
         }
      }
   }

   public static String patternDisplayName(String pattern) {
      if (pattern != null && !pattern.isBlank()) {
         String p = pattern.toLowerCase();

         return switch (p) {
            case "paired" -> "PAIRED";
            case "repeating" -> "REPEATING";
            case "palindrome" -> "PALINDROME";
            default -> p.startsWith("axbxcx") ? "AxBxCx" : pattern.toUpperCase();
         };
      } else {
         return null;
      }
   }

   private static boolean isStagedFadeName(String name) {
      if (name == null || name.isBlank()) {
         return false;
      }

      return STAGE_PATTERN.matcher(name).find();
   }

   public static int extractStageNumber(String name) {
      if (name != null && !name.isBlank()) {
         Matcher matcher = STAGE_PATTERN.matcher(name);
         if (!matcher.find()) {
            return -1;
         }

         try {
            return Integer.parseInt(matcher.group(1));
         } catch (NumberFormatException ignored) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static List<SeymourAnalyzer.ColorMatch> findMatchesInMap(
      String sourceHex, SeymourAnalyzer.PieceType pieceType, Map<String, String> map, boolean isFade, ScannerConfig config
   ) {
      List<SeymourAnalyzer.ColorMatch> matches = new ArrayList<>();
      SeymourAnalyzer.LAB sourceLab = getLabForHex(sourceHex);

      for (Entry<String, String> e : map.entrySet()) {
         String name = e.getKey();
         String targetHex = normalizeHex(e.getValue());
         if (name != null
            && targetHex != null
            && !isStagedFadeName(name)
            && (!config.seymourPieceSpecificEnabled || canMatchPiece(name, pieceType))
            && matchesArmorFilter(name, config.seymourArmorNameFilters)) {
            SeymourAnalyzer.LAB targetLab = getLabForHex(targetHex);
            double delta = calculateDeltaEWithLab(sourceLab, targetLab);
            int abs = calculateAbsoluteDistance(sourceHex, targetHex);
            int rawTier = calculateRawTier(delta, isFade);
            matches.add(new SeymourAnalyzer.ColorMatch(name, targetHex, delta, abs, rawTier, false, isFade));
         }
      }

      return matches;
   }

   private static boolean matchesArmorFilter(String candidateName, String rawFilter) {
      if (rawFilter != null && !rawFilter.trim().isEmpty()) {
         String name = candidateName.toLowerCase();
         String[] parts = rawFilter.toLowerCase().split("[,;\\n|]+");

         for (String p : parts) {
            String token = p.trim();
            if (!token.isEmpty()) {
               if ("all".equals(token) || "*".equals(token)) {
                  return true;
               }

               if (name.contains(token)) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static SeymourAnalyzer.MatchPriority getMatchPriority(SeymourAnalyzer.ColorMatch m) {
      if (m.isFade) {
         if (m.rawTier <= 0) {
            return SeymourAnalyzer.MatchPriority.FADE_T0;
         } else {
            return m.rawTier == 1 ? SeymourAnalyzer.MatchPriority.FADE_T1 : SeymourAnalyzer.MatchPriority.FADE_T2;
         }
      } else if (m.rawTier <= 0) {
         return SeymourAnalyzer.MatchPriority.NORMAL_T0;
      } else {
         return m.rawTier == 1 ? SeymourAnalyzer.MatchPriority.NORMAL_T1 : SeymourAnalyzer.MatchPriority.NORMAL_T2;
      }
   }

   private static int getPriorityIndex(SeymourAnalyzer.MatchPriority p) {
      int idx = DEFAULT_PRIORITY_ORDER.indexOf(p);
      return idx >= 0 ? idx : Integer.MAX_VALUE;
   }

   private static int calculateRawTier(double delta, boolean isFade) {
      if (isFade) {
         if (delta <= 1.0) {
            return 0;
         } else if (delta <= 2.0) {
            return 1;
         } else {
            return delta <= 5.0 ? 2 : 3;
         }
      } else if (delta <= 1.0) {
         return 0;
      } else if (delta <= 2.0) {
         return 1;
      } else {
         return delta <= 5.0 ? 2 : 3;
      }
   }

   private static int mapToDisplayTier(int rawTier, boolean isFade) {
      if (isFade) {
         if (rawTier <= 1) {
            return 1;
         } else {
            return rawTier == 2 ? 2 : 3;
         }
      } else if (rawTier <= 1) {
         return 1;
      } else {
         return rawTier == 2 ? 2 : 3;
      }
   }

   private static List<SeymourAnalyzer.ColorMatch> limit(List<SeymourAnalyzer.ColorMatch> src, int n) {
      return src.size() <= n ? src : new ArrayList<>(src.subList(0, n));
   }

   private static boolean canMatchPiece(String targetName, SeymourAnalyzer.PieceType pieceType) {
      if (pieceType == null) {
         return true;
      }

      String n = targetName.toLowerCase();
      boolean hasPieceHint = n.contains("helmet")
         || n.contains("hat")
         || n.contains("hood")
         || n.contains("cap")
         || n.contains("crown")
         || n.contains("mask")
         || n.contains("chestplate")
         || n.contains("chest")
         || n.contains("tunic")
         || n.contains("jacket")
         || n.contains("shirt")
         || n.contains("vest")
         || n.contains("robe")
         || n.contains("leggings")
         || n.contains("pants")
         || n.contains("trousers")
         || n.contains("boots")
         || n.contains("shoes")
         || n.contains("sandals")
         || n.contains("sneakers");
      if (!hasPieceHint) {
         return true;
      }

      return switch (pieceType) {
         case TOP_HAT -> n.contains("helmet") || n.contains("hat") || n.contains("hood") || n.contains("cap") || n.contains("crown") || n.contains("mask");
         case JACKET -> n.contains("chestplate")
            || n.contains("chest")
            || n.contains("tunic")
            || n.contains("jacket")
            || n.contains("shirt")
            || n.contains("vest")
            || n.contains("robe");
         case TROUSERS -> n.contains("leggings") || n.contains("pants") || n.contains("trousers");
         case SHOES -> n.contains("boots") || n.contains("shoes") || n.contains("sandals") || n.contains("sneakers");
      };
   }

   private static String stripFormatting(String s) {
      return s == null ? "" : s.replaceAll("§[0-9a-fk-or]", "");
   }

   private static void loadColorDatabase() {
      TARGET_COLORS.clear();
      FADE_DYES.clear();
      LAB_CACHE.clear();

      try (InputStream in = SeymourAnalyzer.class.getResourceAsStream("/seymour-colors.json")) {
         if (in != null) {
            Reader reader = new InputStreamReader(in);
            JsonObject root = (JsonObject)new Gson().fromJson(reader, JsonObject.class);
            if (root != null) {
               JsonObject target = root.has("TARGET_COLORS") ? root.getAsJsonObject("TARGET_COLORS") : null;
               if (target != null) {
                  for (Entry<String, JsonElement> e : target.entrySet()) {
                     String hex = normalizeHex(e.getValue().getAsString());
                     if (hex != null) {
                        TARGET_COLORS.put(e.getKey(), hex);
                     }
                  }
               }

               JsonObject fade = root.has("FADE_DYES") ? root.getAsJsonObject("FADE_DYES") : null;
               if (fade != null) {
                  for (Entry<String, JsonElement> e : fade.entrySet()) {
                     String hex = normalizeHex(e.getValue().getAsString());
                     if (hex != null) {
                        FADE_DYES.put(e.getKey(), hex);
                     }
                  }
               }

               PotatoToolMod.LOGGER.info("Loaded Seymour color database: " + TARGET_COLORS.size() + " targets, " + FADE_DYES.size() + " fade dyes");
            }
         } else {
            PotatoToolMod.LOGGER.warn("Seymour colors database not found: /seymour-colors.json");
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.error("Failed to load Seymour color database", e);
      }
   }

   private static SeymourAnalyzer.LAB getLabForHex(String hex) {
      String key = normalizeHex(hex);
      return key == null ? new SeymourAnalyzer.LAB(0.0, 0.0, 0.0) : LAB_CACHE.computeIfAbsent(key, SeymourAnalyzer::hexToLab);
   }

   private static int calculateAbsoluteDistance(String hexA, String hexB) {
      SeymourAnalyzer.RGB a = hexToRgb(hexA);
      SeymourAnalyzer.RGB b = hexToRgb(hexB);
      return Math.abs(a.r - b.r) + Math.abs(a.g - b.g) + Math.abs(a.b - b.b);
   }

   private static double calculateDeltaEWithLab(SeymourAnalyzer.LAB a, SeymourAnalyzer.LAB b) {
      double dl = a.l - b.l;
      double da = a.a - b.a;
      double db = a.b - b.b;
      return Math.sqrt(dl * dl + da * da + db * db);
   }

   private static SeymourAnalyzer.LAB hexToLab(String hex) {
      SeymourAnalyzer.RGB rgb = hexToRgb(hex);
      SeymourAnalyzer.XYZ xyz = rgbToXyz(rgb);
      return xyzToLab(xyz);
   }

   private static SeymourAnalyzer.RGB hexToRgb(String hex) {
      String s = normalizeHex(hex);
      return s == null
         ? new SeymourAnalyzer.RGB(0, 0, 0)
         : new SeymourAnalyzer.RGB(Integer.parseInt(s.substring(0, 2), 16), Integer.parseInt(s.substring(2, 4), 16), Integer.parseInt(s.substring(4, 6), 16));
   }

   private static SeymourAnalyzer.XYZ rgbToXyz(SeymourAnalyzer.RGB rgb) {
      double r = rgb.r / 255.0;
      double g = rgb.g / 255.0;
      double b = rgb.b / 255.0;
      r = r > 0.04045 ? Math.pow((r + 0.055) / 1.055, 2.4) : r / 12.92;
      g = g > 0.04045 ? Math.pow((g + 0.055) / 1.055, 2.4) : g / 12.92;
      b = b > 0.04045 ? Math.pow((b + 0.055) / 1.055, 2.4) : b / 12.92;
      double x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) * 100.0;
      double y = (r * 0.2126729 + g * 0.7151522 + b * 0.072175) * 100.0;
      double z = (r * 0.0193339 + g * 0.119192 + b * 0.9503041) * 100.0;
      return new SeymourAnalyzer.XYZ(x, y, z);
   }

   private static SeymourAnalyzer.LAB xyzToLab(SeymourAnalyzer.XYZ xyz) {
      double x = xyz.x / 95.047;
      double y = xyz.y / 100.0;
      double z = xyz.z / 108.883;
      x = x > 0.008856 ? Math.pow(x, 0.3333333333333333) : 7.787 * x + 0.13793103448275862;
      y = y > 0.008856 ? Math.pow(y, 0.3333333333333333) : 7.787 * y + 0.13793103448275862;
      z = z > 0.008856 ? Math.pow(z, 0.3333333333333333) : 7.787 * z + 0.13793103448275862;
      double l = 116.0 * y - 16.0;
      double a = 500.0 * (x - y);
      double b = 200.0 * (y - z);
      return new SeymourAnalyzer.LAB(l, a, b);
   }

   static {
      loadColorDatabase();
   }

   private static final class ColorMatch {
      final String name;
      final String targetHex;
      final double deltaE;
      final int absoluteDistance;
      final int rawTier;
      final boolean isCustom;
      final boolean isFade;

      ColorMatch(String name, String targetHex, double deltaE, int absoluteDistance, int rawTier, boolean isCustom, boolean isFade) {
         this.name = name;
         this.targetHex = targetHex;
         this.deltaE = deltaE;
         this.absoluteDistance = absoluteDistance;
         this.rawTier = rawTier;
         this.isCustom = isCustom;
         this.isFade = isFade;
      }
   }

   private static final class LAB {
      final double l;
      final double a;
      final double b;

      LAB(double l, double a, double b) {
         this.l = l;
         this.a = a;
         this.b = b;
      }
   }

   private enum MatchPriority {
      SEARCH,
      DUPE,
      WORD,
      PATTERN,
      FADE_T0,
      FADE_T1,
      FADE_T2,
      NORMAL_T0,
      NORMAL_T1,
      NORMAL_T2;
   }

   public static final class MatchResult {
      private final int tier;
      private final int rawTier;
      private final double deltaE;
      private final int absoluteDistance;
      private final String matchedName;
      private final String matchedHex;
      private final boolean custom;
      private final boolean fade;

      public MatchResult(int tier, int rawTier, double deltaE, int absoluteDistance, String matchedName, String matchedHex, boolean custom, boolean fade) {
         this.tier = tier;
         this.rawTier = rawTier;
         this.deltaE = deltaE;
         this.absoluteDistance = absoluteDistance;
         this.matchedName = matchedName;
         this.matchedHex = matchedHex;
         this.custom = custom;
         this.fade = fade;
      }

      public int getTier() {
         return this.tier;
      }

      public int getRawTier() {
         return this.rawTier;
      }

      public double getDeltaE() {
         return this.deltaE;
      }

      public int getAbsoluteDistance() {
         return this.absoluteDistance;
      }

      public String getMatchedName() {
         return this.matchedName;
      }

      public String getMatchedHex() {
         return this.matchedHex;
      }

      public boolean isCustom() {
         return this.custom;
      }

      public boolean isFade() {
         return this.fade;
      }
   }

   public enum PieceType {
      TOP_HAT,
      JACKET,
      TROUSERS,
      SHOES;
   }

   private static final class RGB {
      final int r;
      final int g;
      final int b;

      RGB(int r, int g, int b) {
         this.r = r;
         this.g = g;
         this.b = b;
      }
   }

   private static final class XYZ {
      final double x;
      final double y;
      final double z;

      XYZ(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
