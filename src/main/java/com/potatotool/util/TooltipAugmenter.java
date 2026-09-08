package com.potatotool.util;

import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item.TooltipContext;

public final class TooltipAugmenter {
   private static boolean initialized = false;
   private static final DateTimeFormatter CREATED_FMT = DateTimeFormatter.ofPattern("EEE dd.MM.yyyy h:mm:ss a", Locale.ENGLISH)
      .withZone(ZoneId.systemDefault());
   private static final String NO_SKIN_MATCH = "__NONE__";
   private static final Map<String, String> SKIN_NAME_GUESS_CACHE = new ConcurrentHashMap<>();
   private static final Pattern RAW_CREATED_NUMERIC = Pattern.compile(
      "(?i)(created(?:_?at)?|creation(?:_?time|_?date)?|timestamp|date)\\s*[:=]\\s*\"?(-?\\d{10,20})\"?"
   );
   private static final Pattern RAW_CREATED_TEXT = Pattern.compile("(?i)(created(?:_?at)?|creation(?:_?time|_?date)?|date)\\s*[:=]\\s*\"([^\"]{8,64})\"");

   private TooltipAugmenter() {
   }

   public static synchronized void init() {
      if (!initialized) {
         initialized = true;
         ItemTooltipCallback.EVENT.register(TooltipAugmenter::onTooltip);
         PotatoToolMod.LOGGER.info("Tooltip augmenter initialized");
      }
   }

   private static void onTooltip(ItemStack stack, TooltipContext context, TooltipFlag type, List<Component> lines) {
      PotatoToolMod mod = PotatoToolMod.getInstance();
      if (mod != null && stack != null && !stack.isEmpty()) {
         ScannerConfig config = mod.getConfig();
         if (config != null) {
            String itemName = stripFormatting(stack.getHoverName().getString());
            boolean hasCreatedLine = false;

            for (Component line : lines) {
               if (line != null && line.getString() != null) {
                  String s = stripFormatting(line.getString());
                  if (s.contains("PotatoToolV2 Analysis") || s.contains("Seymour Analysis")) {
                     return;
                  }

                  if (s.startsWith("Created at:")) {
                     hasCreatedLine = true;
                  }
               }
            }

            String itemId = extractSkyblockItemId(stack);
            if (itemId == null || itemId.isEmpty()) {
               itemId = stack.getItem().toString();
            }

            String createdAt = extractCreatedAtText(stack);
            if (createdAt == null || createdAt.isBlank()) {
               createdAt = extractCreatedAtFromTooltipLines(lines);
            }

            String hex = extractHexFromStack(stack);
            boolean hasHexAnalysis = hex != null;
            if (!hasHexAnalysis) {
               if (!hasCreatedLine && createdAt != null && !createdAt.isBlank()) {
                  int insertAt = Math.min(1, lines.size());
                  lines.add(insertAt, Component.literal("§7Created at: §f" + createdAt));
               }
            } else {
               String cosmeticDyeToken = extractCosmeticDyeToken(stack);
               boolean isSeymourPiece = hasHexAnalysis && isSeymourPiece(itemName, itemId);
               String hexType = hasHexAnalysis ? classifyHexType(itemId, itemName, hex, cosmeticDyeToken, config) : null;
               SeymourAnalyzer.MatchResult seymourMatch = hasHexAnalysis ? SeymourAnalyzer.classifyTier(hex, itemName, config) : null;
               String specialPattern = hasHexAnalysis && isSeymourPiece ? SeymourAnalyzer.patternDisplayName(SeymourAnalyzer.detectSpecialPattern(hex)) : null;
               List<Component> addonLines = new ArrayList<>();
               addonLines.add(Component.literal("§9PotatoToolV2 Analysis"));
               if (hasHexAnalysis) {
                  String hexUpper = hex.toUpperCase(Locale.ROOT);
                  addonLines.add(Component.literal("§7Hex: " + legacyHexColorPrefix(hexUpper) + hexUpper));
                  if (!isSeymourPiece) {
                     addonLines.add(Component.literal("§7Type: " + colorizeType(hexType)));
                  }

                  if (specialPattern != null) {
                     addonLines.add(Component.literal("§7Pattern: §d" + specialPattern));
                  }

                  if (seymourMatch != null) {
                     String pool = seymourMatch.isCustom() ? "Custom" : (seymourMatch.isFade() ? "Fade" : "Target");
                     addonLines.add(Component.literal("§7Closest: §f" + safeText(seymourMatch.getMatchedName()) + " §7(§a" + pool + "§7)"));
                     addonLines.add(
                        Component.literal("§7dE: §b" + format(seymourMatch.getDeltaE()) + " §8| §7Abs: §f" + seymourMatch.getAbsoluteDistance())
                     );
                     addonLines.add(Component.literal("§7Tier: " + colorizeTierLabel(seymourMatch) + " §8(" + pool + ")"));
                  }
               }

               if (createdAt != null && !createdAt.isBlank()) {
                  addonLines.add(Component.literal("§7Created at: §f" + createdAt));
               }

               int insertAt = Math.min(1, lines.size());

               for (Component line : addonLines) {
                  lines.add(insertAt++, line);
               }
            }
         }
      }
   }

   private static String colorizeType(String type) {
      return switch (type) {
         case "GLITCHED" -> "§cGLITCHED";
         case "OG_FAIRY" -> "§dOG_FAIRY";
         case "FAIRY" -> "§dFAIRY";
         case "CRYSTAL" -> "§5CRYSTAL";
         case "BLEACHED" -> "§6BLEACHED";
         case "EXOTIC" -> "§aEXOTIC";
         case "SPECIFIC_HEX" -> "§3SPECIFIC_HEX";
         default -> "§7STANDARD";
      };
   }

   private static String colorizeTier(int tier) {
      return switch (tier) {
         case 1 -> "§bT1";
         case 2 -> "§eT2";
         default -> "§cT3";
      };
   }

   private static String colorizeTierLabel(SeymourAnalyzer.MatchResult match) {
      return match != null && !match.isCustom() && match.getRawTier() <= 0 ? "§3T0" : colorizeTier(match != null ? match.getTier() : 3);
   }

   private static boolean isSeymourPiece(String itemName, String itemId) {
      if (SeymourAnalyzer.detectPieceType(itemName) != null) {
         return true;
      } else if (itemId != null && !itemId.isEmpty()) {
         String id = itemId.toUpperCase(Locale.ROOT);
         return id.contains("VELVET_TOP_HAT") || id.contains("CASHMERE_JACKET") || id.contains("SATIN_TROUSERS") || id.contains("OXFORD_SHOES");
      } else {
         return false;
      }
   }

   private static String classifyHexType(String itemId, String itemName, String hexWithHash, String cosmeticDyeToken, ScannerConfig config) {
      String hex = hexWithHash == null ? "" : hexWithHash;
      if (config != null && config.matchesSpecificHex(hex)) {
         return "SPECIFIC_HEX";
      } else if (ColorAnalyzer.isFairyArmorId(itemId) || ColorAnalyzer.isCrystalArmorId(itemId)) {
         return ColorAnalyzer.isExoticColor(hex, itemId, false, cosmeticDyeToken) ? "EXOTIC" : "STANDARD";
      } else if (ColorAnalyzer.isOgFairyDyeToken(cosmeticDyeToken)) {
         return "OG_FAIRY";
      } else if (ColorAnalyzer.isFairyDyeToken(cosmeticDyeToken)) {
         return "FAIRY";
      } else if (ColorAnalyzer.isGlitchedDungeonArmor(itemId, hex, cosmeticDyeToken)) {
         return "GLITCHED";
      } else if (ColorAnalyzer.isBlackWitherArmorByName(itemName, hex, cosmeticDyeToken)) {
         return "GLITCHED";
      } else if (ColorAnalyzer.isOgFairyMatch(hex, itemId, 0L)) {
         return "OG_FAIRY";
      } else if (ColorAnalyzer.isCrystalColor(hex)) {
         return "CRYSTAL";
      } else if (ColorAnalyzer.isFairyColor(hex)) {
         return "FAIRY";
      } else if (ColorAnalyzer.isBleachedColor(hex, itemId)) {
         return "BLEACHED";
      } else if (ColorAnalyzer.isNonFairyCosmeticDye(cosmeticDyeToken)) {
         return "STANDARD";
      } else {
         return ColorAnalyzer.isExoticColor(hex, itemId, false, cosmeticDyeToken) ? "EXOTIC" : "STANDARD";
      }
   }

   private static String extractSkyblockItemId(ItemStack stack) {
      CompoundTag extra = getExtraAttributes(stack);
      if (extra != null && extra.keySet().contains("id")) {
         Tag idTag = extra.get("id");
         return idTag == null ? null : idTag.toString().replace("\"", "");
      } else {
         return null;
      }
   }

   private static String extractCosmeticDyeToken(ItemStack stack) {
      CompoundTag extra = getExtraAttributes(stack);
      String token = null;
      if (extra != null) {
         if (extra.keySet().contains("dye_item")) {
            Tag raw = extra.get("dye_item");
            if (raw != null) {
               token = raw.toString().replace("\"", "");
            }
         } else if (extra.keySet().contains("dye")) {
            Tag raw = extra.get("dye");
            if (raw != null) {
               token = raw.toString().replace("\"", "");
            }
         }

         if (token == null) {
            token = inferDyeTokenFromRaw(extra.toString());
         }
      }

      if (token == null) {
         CompoundTag nbt = getNbt(stack);
         if (nbt != null) {
            token = inferDyeTokenFromRaw(nbt.toString());
         }
      }

      return token;
   }

   private static String extractHexFromStack(ItemStack stack) {
      CompoundTag nbt = getNbt(stack);
      if (nbt == null) {
         return null;
      }

      if (nbt.contains("color")) {
         String raw = nbt.getString("color").orElse("");
         if (raw != null && raw.contains(":")) {
            String parsed = parseColorStringToHex(raw);
            if (parsed != null) {
               return "#" + parsed;
            }
         }
      }

      DyedItemColor colorComp = (DyedItemColor)stack.getOrDefault(DataComponents.DYED_COLOR, null);
      if (colorComp != null) {
         int rgb = colorComp.rgb();
         return String.format("#%06X", rgb & 16777215);
      }

      if (nbt.keySet().contains("display")
         && nbt.get("display") instanceof CompoundTag display
         && display.keySet().contains("color")
         && display.get("color") instanceof IntTag n) {
         int rgb = n.intValue();
         return String.format("#%06X", rgb & 16777215);
      }

      CompoundTag extra = getExtraAttributes(stack);
      if (extra != null && extra.keySet().contains("color")) {
         Tag colorRaw = extra.get("color");
         if (colorRaw != null) {
            String parsed = parseColorStringToHex(colorRaw.toString().replace("\"", ""));
            if (parsed != null) {
               return "#" + parsed;
            }
         }
      }

      return null;
   }

   private static CompoundTag getExtraAttributes(ItemStack stack) {
      CompoundTag nbt = getNbt(stack);
      if (nbt == null) {
         return null;
      } else if (nbt.keySet().contains("ExtraAttributes") && nbt.get("ExtraAttributes") instanceof CompoundTag c) {
         return c;
      } else {
         return !nbt.keySet().contains("id")
               && !nbt.keySet().contains("modifier")
               && !nbt.keySet().contains("color")
               && !nbt.keySet().contains("dye_item")
               && !nbt.keySet().contains("uuid")
            ? null
            : nbt;
      }
   }

   private static CompoundTag getNbt(ItemStack stack) {
      CustomData customData = (CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      return customData != null ? customData.copyTag() : null;
   }

   private static String parseColorStringToHex(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String s = raw.trim();
         if (s.contains(":")) {
            String[] parts = s.split(":");
            if (parts.length != 3) {
               return null;
            }

            try {
               int r = Math.max(0, Math.min(255, Integer.parseInt(parts[0].trim())));
               int g = Math.max(0, Math.min(255, Integer.parseInt(parts[1].trim())));
               int b = Math.max(0, Math.min(255, Integer.parseInt(parts[2].trim())));
               return String.format("%02X%02X%02X", r, g, b);
            } catch (NumberFormatException e) {
               return null;
            }
         } else {
            s = s.replace("#", "").toUpperCase(Locale.ROOT);
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
      } else {
         return null;
      }
   }

   private static String inferDyeTokenFromRaw(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String u = raw.toUpperCase(Locale.ROOT);
         if (u.contains("OG_FAIRY_DYE") || u.contains("OG_FAIRY")) {
            return "OG_FAIRY_DYE";
         } else if (u.contains("FAIRY_DYE")) {
            return "FAIRY_DYE";
         } else {
            return u.contains("PURE_BLACK") ? "PURE_BLACK_DYE" : null;
         }
      } else {
         return null;
      }
   }

   private static String extractSkinTag(ItemStack stack, String itemId, String itemName) {
      String direct = normalizeToken(itemId);
      if (!looksLikeSkinVariantTag(direct) && !CosmeticSkinValuesLoader.isKnownSkinItemId(direct)) {
         CompoundTag extra = getExtraAttributes(stack);
         if (extra != null) {
            if (extra.keySet().contains("skin")) {
               Tag skinRaw = extra.get("skin");
               String fromSkin = findPotentialSkinIdInRaw(skinRaw == null ? null : skinRaw.toString());
               if (fromSkin != null) {
                  return fromSkin;
               }
            }

            for (String key : extra.keySet()) {
               if (key != null && key.toLowerCase(Locale.ROOT).contains("skin")) {
                  Tag raw = extra.get(key);
                  String fromAny = findPotentialSkinIdInRaw(raw == null ? null : raw.toString());
                  if (fromAny != null) {
                     return fromAny;
                  }
               }
            }

            String fromExtraRaw = findPotentialSkinIdInRaw(extra.toString());
            if (fromExtraRaw != null) {
               return fromExtraRaw;
            }
         }

         CompoundTag nbt = getNbt(stack);
         if (nbt != null) {
            String fromNbtRaw = findPotentialSkinIdInRaw(nbt.toString());
            if (fromNbtRaw != null) {
               return fromNbtRaw;
            }
         }

         return resolveSkinTagFromItemName(itemName);
      } else {
         return direct;
      }
   }

   private static String findPotentialSkinIdInRaw(String raw) {
      if (raw != null && !raw.isBlank()) {
         String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", " ");

         for (String token : cleaned.split("\\s+")) {
            if (token != null && !token.isBlank() && (looksLikeSkinVariantTag(token) || CosmeticSkinValuesLoader.isKnownSkinItemId(token))) {
               return token;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean looksLikeSkinVariantTag(String token) {
      if (token != null && !token.isBlank()) {
         String t = token.toUpperCase(Locale.ROOT);
         return t.startsWith("PET_SKIN_")
            || t.contains("_SKIN_")
            || t.startsWith("PARTY_HAT")
            || t.contains("CAPE")
            || t.contains("CLOAK")
            || t.endsWith("_BABY")
            || t.contains("_BABY_");
      } else {
         return false;
      }
   }

   private static String normalizeToken(String raw) {
      if (raw == null) {
         return null;
      }

      String norm = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_");
      norm = norm.replaceAll("^_+", "").replaceAll("_+$", "");
      return norm.isEmpty() ? null : norm;
   }

   private static String resolveSkinTagFromItemName(String itemName) {
      if (itemName != null && !itemName.isBlank()) {
         String key = normalizeToken(itemName);
         if (key != null && !key.isBlank()) {
            String cached = SKIN_NAME_GUESS_CACHE.get(key);
            if (cached != null) {
               return "__NONE__".equals(cached) ? null : cached;
            }

            String upperName = itemName.toUpperCase(Locale.ROOT);
            if (!upperName.contains("SKIN") && !upperName.contains("PARTY HAT")) {
               SKIN_NAME_GUESS_CACHE.put(key, "__NONE__");
               return null;
            }

            Set<String> words = extractNameWords(upperName);
            if (words.isEmpty()) {
               SKIN_NAME_GUESS_CACHE.put(key, "__NONE__");
               return null;
            }

            String best = null;
            int bestScore = -1;

            for (String known : CosmeticSkinValuesLoader.getKnownSkinItemIds()) {
               if (known != null && !known.isBlank()) {
                  String tag = known.toUpperCase(Locale.ROOT);
                  if (looksLikeSkinVariantTag(tag) || CosmeticSkinValuesLoader.isKnownSkinItemId(tag)) {
                     int score = 0;
                     boolean allWordsFound = true;

                     for (String w : words) {
                        if (!tag.contains(w)) {
                           allWordsFound = false;
                           break;
                        }

                        score += Math.max(1, w.length());
                     }

                     if (allWordsFound) {
                        if (tag.startsWith("PET_SKIN_")) {
                           score += 2;
                        }

                        if (tag.contains("_SKIN_")) {
                           score++;
                        }

                        if (score > bestScore) {
                           bestScore = score;
                           best = tag;
                        }
                     }
                  }
               }
            }

            SKIN_NAME_GUESS_CACHE.put(key, best == null ? "__NONE__" : best);
            return best == null ? null : best;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Set<String> extractNameWords(String upperName) {
      if (upperName != null && !upperName.isBlank()) {
         String cleaned = upperName.replaceAll("[^A-Z0-9]+", " ").trim();
         if (cleaned.isEmpty()) {
            return Collections.emptySet();
         }

         Set<String> out = new HashSet<>();

         for (String token : cleaned.split("\\s+")) {
            if (token != null
               && !token.isBlank()
               && !"SKIN".equals(token)
               && !"PET".equals(token)
               && !"THE".equals(token)
               && !"A".equals(token)
               && !"AN".equals(token)) {
               out.add(token);
            }
         }

         return out;
      } else {
         return Collections.emptySet();
      }
   }

   private static String extractCreatedAtText(ItemStack stack) {
      CompoundTag nbt = getNbt(stack);
      CompoundTag extra = getExtraAttributes(stack);
      if (nbt == null && extra == null) {
         return null;
      }

      String parsedFromExtra = extractCreatedFromCompound(extra);
      if (parsedFromExtra != null) {
         return parsedFromExtra;
      }

      String parsedFromNbt = extractCreatedFromCompound(nbt);
      if (parsedFromNbt != null) {
         return parsedFromNbt;
      }

      String parsedFromRaw = extractCreatedFromRawNbt(extra, nbt);
      if (parsedFromRaw != null) {
         return parsedFromRaw;
      }

      if (nbt != null && nbt.contains("uuid")) {
         String u = nbt.getString("uuid").orElse("").trim();
         String parsed = parseCreatedFromUuid(u);
         if (parsed != null) {
            return parsed;
         }
      }

      if (extra != null && extra.keySet().contains("uuid")) {
         Tag rawUuid = extra.get("uuid");
         if (rawUuid != null) {
            String u = rawUuid.toString().replace("\"", "").trim();
            String parsed = parseCreatedFromUuid(u);
            if (parsed != null) {
               return parsed;
            }
         }
      }

      return null;
   }

   private static String extractCreatedFromCompound(CompoundTag compound) {
      if (compound == null) {
         return null;
      }

      String[] exactKeys = new String[]{
         "timestamp", "created", "created_at", "creation_time", "date", "createdAt", "creationDate", "date_created", "item_created_at"
      };

      for (String key : exactKeys) {
         String parsed = parseCreatedFromField(compound, key);
         if (parsed != null) {
            return parsed;
         }
      }

      for (String key : compound.keySet()) {
         if (key != null && !key.isBlank()) {
            String norm = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (norm.contains("created") || norm.contains("timestamp") || norm.contains("date")) {
               String parsed = parseCreatedFromField(compound, key);
               if (parsed != null) {
                  return parsed;
               }
            }
         }
      }

      return null;
   }

   private static String parseCreatedFromField(CompoundTag compound, String key) {
      if (compound != null && key != null && !key.isBlank()) {
         if (!compound.keySet().contains(key)) {
            return null;
         } else {
            Tag raw = compound.get(key);
            if (raw == null) {
               return null;
            } else {
               String v = raw.toString().replace("\"", "").trim();
               String parsed = parseCreatedTimestamp(v);
               if (parsed != null) {
                  return parsed;
               } else {
                  return looksHumanReadableDate(v) ? v : null;
               }
            }
         }
      } else {
         return null;
      }
   }

   private static String extractCreatedFromRawNbt(CompoundTag extra, CompoundTag nbt) {
      StringBuilder raw = new StringBuilder();
      if (extra != null) {
         raw.append(extra.toString());
      }

      if (nbt != null) {
         raw.append(' ').append(nbt.toString());
      }

      if (raw.length() == 0) {
         return null;
      }

      Matcher numeric = RAW_CREATED_NUMERIC.matcher(raw);

      while (numeric.find()) {
         String parsed = parseCreatedTimestamp(numeric.group(2));
         if (parsed != null) {
            return parsed;
         }
      }

      Matcher text = RAW_CREATED_TEXT.matcher(raw);

      while (text.find()) {
         String candidate = text.group(2).trim();
         String parsed = parseCreatedTimestamp(candidate);
         if (parsed != null) {
            return parsed;
         }

         if (looksHumanReadableDate(candidate)) {
            return candidate;
         }
      }

      return null;
   }

   private static String extractCreatedAtFromTooltipLines(List<Component> lines) {
      if (lines != null && !lines.isEmpty()) {
         for (Component line : lines) {
            if (line != null) {
               String s = stripFormatting(line.getString());
               if (s != null && !s.isBlank() && (s.startsWith("Created at:") || s.startsWith("Created:"))) {
                  int colon = s.indexOf(58);
                  if (colon >= 0 && colon + 1 < s.length()) {
                     String candidate = s.substring(colon + 1).trim();
                     if (!candidate.isBlank()) {
                        String parsed = parseCreatedTimestamp(candidate);
                        if (parsed != null) {
                           return parsed;
                        }

                        if (looksHumanReadableDate(candidate)) {
                           return candidate;
                        }
                     }
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String parseCreatedTimestamp(String v) {
      if (v != null && !v.isEmpty()) {
         if (v.matches("^-?\\d{10,20}$")) {
            try {
               long n = Long.parseLong(v);
               long ms = normalizeEpochToMillis(n);
               if (ms > 946684800000L && ms < 4102444800000L) {
                  return CREATED_FMT.format(Instant.ofEpochMilli(ms));
               }
            } catch (NumberFormatException var6) {
            }
         }

         try {
            return CREATED_FMT.format(Instant.parse(v));
         } catch (DateTimeParseException var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static long normalizeEpochToMillis(long n) {
      if (n > 0L && n < 10000000000L) {
         return n * 1000L;
      } else if (n >= 10000000000000000L) {
         return n / 1000000L;
      } else {
         return n >= 10000000000000L ? n / 1000L : n;
      }
   }

   private static boolean looksHumanReadableDate(String v) {
      if (v == null) {
         return false;
      } else {
         String s = v.trim();
         if (s.length() < 8) {
            return false;
         } else if (!s.matches(".*\\d.*")) {
            return false;
         } else {
            return s.matches("^-?\\d+$")
               ? false
               : s.contains("-")
                  || s.contains("/")
                  || s.contains(":")
                  || s.contains(" ")
                  || s.toLowerCase(Locale.ROOT).matches(".*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec).*");
         }
      }
   }

   private static String parseCreatedFromUuid(String rawUuid) {
      if (rawUuid != null && !rawUuid.isEmpty()) {
         try {
            UUID uuid = UUID.fromString(rawUuid);
            int version = uuid.version();
            if (version == 1) {
               long t100 = uuid.timestamp();
               long unixMs = (t100 - 122192928000000000L) / 10000L;
               if (unixMs > 946684800000L && unixMs < 4102444800000L) {
                  return CREATED_FMT.format(Instant.ofEpochMilli(unixMs));
               }
            } else if (version == 7) {
               long ms = uuid.getMostSignificantBits() >>> 16;
               if (ms > 946684800000L && ms < 4102444800000L) {
                  return CREATED_FMT.format(Instant.ofEpochMilli(ms));
               }
            }
         } catch (Exception var7) {
         }

         return null;
      } else {
         return null;
      }
   }

   private static String stripFormatting(String s) {
      return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
   }

   private static String safeText(String s) {
      return s == null ? "-" : stripFormatting(s);
   }

   private static String format(double v) {
      return String.format(Locale.US, "%.2f", v);
   }

   private static String formatMillions(double v) {
      return v >= 10.0 ? String.format(Locale.US, "%.1fm", v) : String.format(Locale.US, "%.2fm", v);
   }

   private static String formatCoins(long coins) {
      return String.format(Locale.US, "%,d", Math.max(0L, coins));
   }

   private static String legacyHexColorPrefix(String hexWithHash) {
      String h = SeymourAnalyzer.normalizeHex(hexWithHash);
      if (h == null) {
         return "§f";
      }

      StringBuilder sb = new StringBuilder("§x");

      for (int i = 0; i < h.length(); i++) {
         sb.append('§').append(h.charAt(i));
      }

      return sb.toString();
   }
}
