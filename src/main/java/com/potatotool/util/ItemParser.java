package com.potatotool.util;

import com.google.gson.JsonObject;
import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedItem;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.core.component.DataComponents;

public class ItemParser {
   private static final Set<String> LEGACY_REFORGES = new HashSet<>(Arrays.asList("Demonic", "Strong", "Hurtful", "Forceful", "Rich", "Odd"));
   private static final Set<String> GHOST_REFORGES = new HashSet<>(
      Arrays.asList("Godly", "Unpleasant", "Superior", "Zealous", "Keen", "Strange", "Shiny", "Vivid")
   );

   public static ScannedItem parseItem(JsonObject itemJson, ScannerConfig config) {
      if (!itemJson.has("id")) {
         return null;
      }

      String itemId = itemJson.get("id").getAsString();
      String itemName = itemJson.has("tag") && itemJson.getAsJsonObject("tag").has("display")
         ? itemJson.getAsJsonObject("tag").getAsJsonObject("display").get("Name").getAsString()
         : itemId;
      ScannedItem item = new ScannedItem(itemId, itemName);
      boolean hasCosmeticDye = false;
      String cosmeticDyeToken = null;
      if (itemJson.has("tag")) {
         JsonObject tag = itemJson.getAsJsonObject("tag");
         if (tag.has("ExtraAttributes")) {
            JsonObject extra = tag.getAsJsonObject("ExtraAttributes");
            if (extra.has("dye_item") || extra.has("dye")) {
               hasCosmeticDye = true;
            }

            if (extra.has("dye_item") && !extra.get("dye_item").isJsonNull()) {
               cosmeticDyeToken = extra.get("dye_item").getAsString();
            } else if (extra.has("dye") && !extra.get("dye").isJsonNull()) {
               cosmeticDyeToken = extra.get("dye").toString();
            }

            if (cosmeticDyeToken == null) {
               cosmeticDyeToken = inferDyeTokenFromRaw(extra.toString());
            }

            if (extra.has("modifier")) {
               String reforge = extra.get("modifier").getAsString();
               item.setReforge(reforge);
               categorizeByReforge(item, reforge);
            }

            SeymourAnalyzer.PieceType piece = SeymourAnalyzer.detectPieceType(itemName, itemId);
            if (piece != null && config.seymourScanningEnabled && SeymourAnalyzer.isPieceEnabled(config, piece) && extra.has("color")) {
               String parsed = ColorAnalyzer.normalizeHex(extra.get("color").getAsString());
               if (parsed != null) {
                  String hexColor = "#" + parsed;
                  item.setHexColor(hexColor);
                  SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hexColor, itemName, itemId, config);
                  if (match != null && SeymourAnalyzer.passesScanFilters(hexColor, match, config) && SeymourAnalyzer.isTierEnabled(config, match.getTier())) {
                     item.setSeymourMatchName(SeymourAnalyzer.prettyMatchLabel(match.getMatchedName()));
                     item.setCategory(SeymourAnalyzer.toCategory(match.getTier()));
                     return item;
                  }
               }
            }

            if (item.getHexColor() == null && extra.has("color")) {
               String parsed = ColorAnalyzer.normalizeHex(extra.get("color").getAsString());
               if (parsed != null) {
                  String hexColor = "#" + parsed;
                  item.setHexColor(hexColor);
                  categorizeByColor(item, hexColor, config, hasCosmeticDye, cosmeticDyeToken);
               }
            }
         }

         if (tag.has("display") && tag.getAsJsonObject("display").has("color")) {
            int color = tag.getAsJsonObject("display").get("color").getAsInt();
            String hexColor = String.format("#%06X", color);
            item.setHexColor(hexColor);
            SeymourAnalyzer.PieceType piece = SeymourAnalyzer.detectPieceType(itemName, itemId);
            if (piece != null && config.seymourScanningEnabled && SeymourAnalyzer.isPieceEnabled(config, piece)) {
               SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hexColor, itemName, itemId, config);
               if (match != null && SeymourAnalyzer.passesScanFilters(hexColor, match, config) && SeymourAnalyzer.isTierEnabled(config, match.getTier())) {
                  item.setSeymourMatchName(SeymourAnalyzer.prettyMatchLabel(match.getMatchedName()));
                  item.setCategory(SeymourAnalyzer.toCategory(match.getTier()));
                  return item;
               }
            }

            categorizeByColor(item, hexColor, config, hasCosmeticDye, cosmeticDyeToken);
         }
      }

      return item;
   }

   public static ScannedItem parseItemStack(ItemStack stack, ScannerConfig config) {
      return parseItemStack(stack, config, true);
   }

   public static ScannedItem parseItemStackForOverlay(ItemStack stack, ScannerConfig config) {
      return parseItemStack(stack, config, false);
   }

   public static ScannedItem parseItemStack(ItemStack stack, ScannerConfig config, boolean classifySeymour) {
      if (stack.isEmpty()) {
         return null;
      }

      String itemName = stack.getHoverName().getString();
      CustomData customData = (CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      CompoundTag nbt = customData != null ? customData.copyTag() : null;
      String skyblockId = extractSkyblockItemId(nbt);
      String itemId = skyblockId != null && !skyblockId.isEmpty() ? skyblockId : stack.getItem().toString();
      ScannedItem item = new ScannedItem(itemId, itemName);
      boolean hasCosmeticDye = false;
      String cosmeticDyeToken = null;
      CompoundTag extra = getExtraAttributes(nbt);
      if (extra != null) {
         if (extra.keySet().contains("dye_item") || extra.keySet().contains("dye")) {
            hasCosmeticDye = true;
         }

         if (extra.keySet().contains("dye_item")) {
            Tag rawDye = extra.get("dye_item");
            if (rawDye != null) {
               cosmeticDyeToken = rawDye.toString().replace("\"", "");
            }
         } else if (extra.keySet().contains("dye")) {
            Tag rawDye = extra.get("dye");
            if (rawDye != null) {
               cosmeticDyeToken = rawDye.toString().replace("\"", "");
            }
         }

         if (cosmeticDyeToken == null && classifySeymour) {
            cosmeticDyeToken = inferDyeTokenFromRaw(extra.toString());
         }

         if (classifySeymour && extra.keySet().contains("modifier")) {
            Tag modElement = extra.get("modifier");
            if (modElement != null) {
               String reforge = modElement.toString().replace("\"", "");
               item.setReforge(reforge);
               categorizeByReforge(item, reforge);
            }
         }

         if (classifySeymour) {
         SeymourAnalyzer.PieceType piece = SeymourAnalyzer.detectPieceType(itemName, itemId);
         if (piece != null && config.seymourScanningEnabled && SeymourAnalyzer.isPieceEnabled(config, piece) && extra.keySet().contains("color")) {
            Tag raw = extra.get("color");
            if (raw != null) {
               String parsed = ColorAnalyzer.normalizeHex(raw.toString().replace("\"", ""));
               if (parsed != null) {
                  String hexColor = "#" + parsed;
                  item.setHexColor(hexColor);
                  SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hexColor, itemName, itemId, config);
                  if (match != null && SeymourAnalyzer.passesScanFilters(hexColor, match, config) && SeymourAnalyzer.isTierEnabled(config, match.getTier())) {
                     item.setSeymourMatchName(SeymourAnalyzer.prettyMatchLabel(match.getMatchedName()));
                     item.setCategory(SeymourAnalyzer.toCategory(match.getTier()));
                     return item;
                  }
               }
            }
         }
         }
      }

      if (classifySeymour && cosmeticDyeToken == null && nbt != null) {
         cosmeticDyeToken = inferDyeTokenFromRaw(nbt.toString());
      }

      DyedItemColor colorComp = (DyedItemColor)stack.getOrDefault(DataComponents.DYED_COLOR, null);
      if (colorComp != null) {
         int color = colorComp.rgb();
         String hexColor = String.format("#%06X", color & 16777215);
         item.setHexColor(hexColor);
         SeymourAnalyzer.PieceType piece = SeymourAnalyzer.detectPieceType(itemName, itemId);
         if (classifySeymour && piece != null && config.seymourScanningEnabled && SeymourAnalyzer.isPieceEnabled(config, piece)) {
            SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hexColor, itemName, itemId, config);
            if (match != null && SeymourAnalyzer.passesScanFilters(hexColor, match, config) && SeymourAnalyzer.isTierEnabled(config, match.getTier())) {
               item.setSeymourMatchName(SeymourAnalyzer.prettyMatchLabel(match.getMatchedName()));
               item.setCategory(SeymourAnalyzer.toCategory(match.getTier()));
               return item;
            }
         }

         categorizeByColor(item, hexColor, config, hasCosmeticDye, cosmeticDyeToken);
         return item;
      } else {
         if (nbt != null
            && nbt.keySet().contains("display")
            && nbt.get("display") instanceof CompoundTag display
            && display.keySet().contains("color")) {
            Tag colorElement = display.get("color");
            if (colorElement instanceof IntTag) {
               int color = ((IntTag)colorElement).intValue();
               String hexColor = String.format("#%06X", color);
               item.setHexColor(hexColor);
               SeymourAnalyzer.PieceType piece = SeymourAnalyzer.detectPieceType(itemName, itemId);
               if (classifySeymour && piece != null && config.seymourScanningEnabled && SeymourAnalyzer.isPieceEnabled(config, piece)) {
                  SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hexColor, itemName, itemId, config);
                  if (match != null && SeymourAnalyzer.passesScanFilters(hexColor, match, config) && SeymourAnalyzer.isTierEnabled(config, match.getTier())) {
                     item.setSeymourMatchName(SeymourAnalyzer.prettyMatchLabel(match.getMatchedName()));
                     item.setCategory(SeymourAnalyzer.toCategory(match.getTier()));
                     return item;
                  }
               }

               categorizeByColor(item, hexColor, config, hasCosmeticDye, cosmeticDyeToken);
            }
         }

         if (item.getHexColor() == null && extra != null && extra.keySet().contains("color")) {
            Tag raw = extra.get("color");
            if (raw != null) {
               String parsed = ColorAnalyzer.normalizeHex(raw.toString().replace("\"", ""));
               if (parsed != null) {
                  String hexColor = "#" + parsed;
                  item.setHexColor(hexColor);
                  SeymourAnalyzer.PieceType piece = SeymourAnalyzer.detectPieceType(itemName, itemId);
                  if (classifySeymour && piece != null && config.seymourScanningEnabled && SeymourAnalyzer.isPieceEnabled(config, piece)) {
                     SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hexColor, itemName, itemId, config);
                     if (match != null && SeymourAnalyzer.passesScanFilters(hexColor, match, config) && SeymourAnalyzer.isTierEnabled(config, match.getTier())) {
                        item.setSeymourMatchName(SeymourAnalyzer.prettyMatchLabel(match.getMatchedName()));
                     item.setCategory(SeymourAnalyzer.toCategory(match.getTier()));
                        return item;
                     }
                  }

                  categorizeByColor(item, hexColor, config, hasCosmeticDye, cosmeticDyeToken);
               }
            }
         }

         return item;
      }
   }

   private static void categorizeByReforge(ScannedItem item, String reforge) {
      if (LEGACY_REFORGES.contains(reforge)) {
         item.setCategory(ScannedItem.ItemCategory.LEGACY_REFORGE);
      } else if (GHOST_REFORGES.contains(reforge)) {
         item.setCategory(ScannedItem.ItemCategory.GHOST_REFORGE);
      }
   }

   private static void categorizeByColor(ScannedItem item, String hexColor, ScannerConfig config, boolean hasCosmeticDye, String cosmeticDyeToken) {
      categorizeByColor(item, hexColor, config, hasCosmeticDye, cosmeticDyeToken, 0L);
   }

   private static void categorizeByColor(
      ScannedItem item, String hexColor, ScannerConfig config, boolean hasCosmeticDye, String cosmeticDyeToken, long timestampMs
   ) {
      if (config.matchesSpecificHex(hexColor)) {
         item.setCategory(ScannedItem.ItemCategory.SPECIFIC_HEX);
         return;
      }

      if (item.getCategory() != null && item.getCategory() != ScannedItem.ItemCategory.NORMAL) {
         return;
      }

      boolean fairyArmorPiece = ColorAnalyzer.isFairyArmorId(item.getItemId());
      boolean crystalArmorPiece = ColorAnalyzer.isCrystalArmorId(item.getItemId());
      if (!fairyArmorPiece && config.ogFairyScanningEnabled && ColorAnalyzer.isOgFairyDyeToken(cosmeticDyeToken)) {
         item.setCategory(ScannedItem.ItemCategory.OG_FAIRY);
         return;
      }

      if (!fairyArmorPiece && config.fairyScanningEnabled && ColorAnalyzer.isFairyDyeToken(cosmeticDyeToken)) {
         item.setCategory(ScannedItem.ItemCategory.FAIRY);
         return;
      }

      if (ColorAnalyzer.isGlitchedDungeonArmor(item.getItemId(), hexColor, cosmeticDyeToken)
         || ColorAnalyzer.isBlackWitherArmorByName(item.getItemName(), hexColor, cosmeticDyeToken)) {
         item.setCategory(ScannedItem.ItemCategory.GLITCHED);
         return;
      }

      if (!fairyArmorPiece && config.ogFairyScanningEnabled && ColorAnalyzer.isOgFairyMatch(hexColor, item.getItemId(), timestampMs)) {
         item.setCategory(ScannedItem.ItemCategory.OG_FAIRY);
         return;
      }

      if (!fairyArmorPiece && !crystalArmorPiece && config.crystalScanningEnabled && ColorAnalyzer.isCrystalColor(hexColor)) {
         item.setCategory(ScannedItem.ItemCategory.CRYSTAL);
         return;
      }

      if (!fairyArmorPiece && config.fairyScanningEnabled && ColorAnalyzer.isFairyColor(hexColor)) {
         item.setCategory(ScannedItem.ItemCategory.FAIRY);
         return;
      }

      if (!fairyArmorPiece && config.bleachedScanningEnabled && ColorAnalyzer.isBleachedColor(hexColor, item.getItemId())) {
         item.setCategory(ScannedItem.ItemCategory.BLEACHED);
         return;
      }

      if (DefaultArmorColorsLoader.shouldIgnoreColor(item.getItemId())) {
         return;
      }

      if (!hasCosmeticDye
         && !ColorAnalyzer.isNonFairyCosmeticDye(cosmeticDyeToken)
         && config.exoticScanningEnabled
         && isLikelySkyBlockItemId(item.getItemId())
         && ColorAnalyzer.isExoticColor(hexColor, item.getItemId(), false, cosmeticDyeToken)) {
         item.setCategory(ScannedItem.ItemCategory.EXOTIC);
      }
   }

   private static boolean isLikelySkyBlockItemId(String itemId) {
      if (itemId != null && !itemId.isEmpty()) {
         String n = itemId.toUpperCase();
         return !n.startsWith("MINECRAFT:");
      } else {
         return false;
      }
   }

   private static CompoundTag getExtraAttributes(CompoundTag nbt) {
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

   private static String extractSkyblockItemId(CompoundTag nbt) {
      CompoundTag extra = getExtraAttributes(nbt);
      if (extra != null && extra.keySet().contains("id")) {
         Tag idRaw = extra.get("id");
         if (idRaw == null) {
            return null;
         }

         String id = idRaw.toString().replace("\"", "").trim();
         return id.isEmpty() ? null : id;
      } else {
         return null;
      }
   }

   private static String parseColorStringToHex(String raw) {
      return ColorAnalyzer.normalizeHex(raw);
   }

   private static String inferDyeTokenFromRaw(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String u = raw.toUpperCase();
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
}
