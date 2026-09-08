package com.potatotool.mixin;

import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedItem;
import com.potatotool.util.ColorAnalyzer;
import com.potatotool.util.CosmeticSkinValuesLoader;
import com.potatotool.util.ItemParser;
import com.potatotool.util.SeymourAnalyzer;
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
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.core.component.DataComponents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;
   @Shadow
   @Final
   protected AbstractContainerMenu menu;
   private static final int SLOT_SIZE = 16;
   private static final int VALUABLE_HIGHLIGHT_COLOR = -2013200640;
   private static final DateTimeFormatter CREATED_FMT = DateTimeFormatter.ofPattern("EEE dd.MM.yyyy h:mm:ss a", Locale.ENGLISH)
      .withZone(ZoneId.systemDefault());
   private static final String NO_SKIN_MATCH = "__NONE__";
   private static final Map<String, String> SKIN_NAME_GUESS_CACHE = new ConcurrentHashMap<>();
   private static final Pattern RAW_CREATED_NUMERIC = Pattern.compile(
      "(?i)(created(?:_?at)?|creation(?:_?time|_?date)?|timestamp|date)\\s*[:=]\\s*\"?(-?\\d{10,20})\"?"
   );
   private static final Pattern RAW_CREATED_TEXT = Pattern.compile("(?i)(created(?:_?at)?|creation(?:_?time|_?date)?|date)\\s*[:=]\\s*\"([^\"]{8,64})\"");

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      PotatoToolMod mod = PotatoToolMod.getInstance();
      if (mod != null) {
         ScannerConfig config = mod.getConfig();
         if (config != null) {
            ItemStack hoveredStack = null;
            ScannedItem hoveredScanned = null;
            int containerMinX = Integer.MAX_VALUE;
            int containerMaxX = Integer.MIN_VALUE;
            int slotCount = this.menu.getItems().size();

            for (int i = 0; i < slotCount; i++) {
               Slot slot = this.menu.getSlot(i);
               ItemStack stack = slot.getItem();
               if (!stack.isEmpty()) {
                  int slotX = this.leftPos + slot.x;
                  int slotY = this.topPos + slot.y;
                  if (slotX < containerMinX) {
                     containerMinX = slotX;
                  }

                  if (slotX + 16 > containerMaxX) {
                     containerMaxX = slotX + 16;
                  }

                  if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
                     hoveredStack = stack;
                  }

                  ScannedItem scanned = ItemParser.parseItemStackForOverlay(stack, config);
                  if (scanned != null && isHighlightEnabledForCategory(scanned, config)) {
                     drawSlotHexBorder(context, slotX, slotY, getSlotHighlightColor(scanned, config));
                  }

                  if (hoveredStack == stack) {
                     hoveredScanned = scanned;
                  }
               }
            }

            if (hoveredStack != null && !hoveredStack.isEmpty()) {
               int fallbackCenter = 160;
               Minecraft client = Minecraft.getInstance();
               if (client != null && client.getWindow() != null) {
                  fallbackCenter = client.getWindow().getGuiScaledWidth() / 2;
               }

               int containerCenterX = containerMinX <= containerMaxX ? (containerMinX + containerMaxX) / 2 : fallbackCenter;
               renderHoverAnalysisPanel(context, mouseX, mouseY, hoveredStack, hoveredScanned, config, this.topPos, containerCenterX);
               this.renderPinnedCreatedBadge(context, hoveredStack, hoveredScanned, config, this.topPos, containerCenterX);
            }
         }
      }
   }

   private void renderPinnedCreatedBadge(
      GuiGraphicsExtractor context, ItemStack hoveredStack, ScannedItem hoveredScanned, ScannerConfig config, int containerTop, int containerCenterX
   ) {
      if (hoveredStack != null && !hoveredStack.isEmpty()) {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.font != null) {
            String created = extractCreatedAtText(hoveredStack);
            if (created != null && !created.isEmpty()) {
               String title = "Created at";
               String value = created;
               int pad = 4;
               int titleH = 10;
               int lineH = 10;
               int w = Math.max(client.font.width(title), client.font.width(value)) + pad * 2;
               int h = pad * 2 + titleH + lineH;
               int screenW = client.getWindow() != null ? client.getWindow().getGuiScaledWidth() : 320;
               int screenH = client.getWindow() != null ? client.getWindow().getGuiScaledHeight() : 240;
               int margin = 6;
               int x = Math.max(margin, Math.min(screenW - w - margin, containerCenterX - w / 2));
               int hoverPanelH = estimateHoverPanelHeight(hoveredStack, hoveredScanned, config);
               int y = hoverPanelH > 0 ? containerTop - hoverPanelH - h - 10 : containerTop - h - 6;
               y = Math.max(margin, y);
               if (y + h > screenH - margin) {
                  y = Math.max(margin, screenH - h - margin);
               }

               context.fill(x, y, x + w, y + h, -586412521);
               context.fill(x, y, x + w, y + 1, -13742849);
               context.fill(x, y + h - 1, x + w, y + h, -13742849);
               context.fill(x, y, x + 1, y + h, -13742849);
               context.fill(x + w - 1, y, x + w, y + h, -13742849);
               context.text(client.font, title, x + pad, y + pad, -7820801, false);
               context.text(client.font, value, x + pad, y + pad + titleH, -1513240, false);
            }
         }
      }
   }

   private static void renderHoverAnalysisPanel(
      GuiGraphicsExtractor context, int mouseX, int mouseY, ItemStack stack, ScannedItem scanned, ScannerConfig config, int containerTop, int containerCenterX
   ) {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.font != null && config != null) {
         String itemName = stripFormatting(stack.getHoverName().getString());
         if (scanned == null) {
            scanned = ItemParser.parseItemStack(stack, config);
         }

         String hex = scanned != null ? normalizeHexWithHash(scanned.getHexColor()) : null;
         if (hex == null) {
            hex = extractHexFromStack(stack);
         }

         boolean hasHex = hex != null;
         String itemIdForType = extractSkyblockItemId(stack);
         if (itemIdForType == null || itemIdForType.isEmpty()) {
            itemIdForType = stack.getItem().toString();
         }

         String skinTag = extractSkinTag(stack, itemIdForType, itemName);
         CosmeticSkinValuesLoader.SkinMarketStats skinStats = skinTag != null ? CosmeticSkinValuesLoader.getOrRequestSkinMarketStats(skinTag) : null;
         boolean hasSkinMarket = skinTag != null;
         if (hasHex || hasSkinMarket) {
            String cosmeticDyeToken = extractCosmeticDyeToken(stack);
            int hexRgb = hasHex ? parseHexColorInt(hex) : 16777215;
            String hexType = hasHex ? classifyHexType(itemIdForType, itemName, hex, cosmeticDyeToken, config) : "STANDARD";
            boolean isSeymourPiece = hasHex && isSeymourPiece(itemName, itemIdForType);
            String specialPattern = hasHex && isSeymourPiece ? SeymourAnalyzer.patternDisplayName(SeymourAnalyzer.detectSpecialPattern(hex)) : null;
            List<String> lines = new ArrayList<>();
            if (!hasHex && hasSkinMarket) {
               lines.add("Skin Market Analysis");
            } else {
               lines.add("PotatoToolV2 Analysis");
               lines.add("Hex: " + hex);
               if (!isSeymourPiece) {
                  lines.add("Piece: " + itemName);
                  lines.add("Type: " + hexType);
               }

               if (specialPattern != null) {
                  lines.add("Pattern: " + specialPattern);
               }
            }

            if (hasHex && isSeymourPiece && config.seymourScanningEnabled) {
               SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hex, itemName, config);
               if (match != null) {
                  String pool = match.isCustom() ? "Custom" : (match.isFade() ? "Fade" : "Target");
                  lines.add("Closest: " + safeText(match.getMatchedName()));
                  lines.add("Target: #" + safeText(match.getMatchedHex()));
                  lines.add("dE: " + format(match.getDeltaE()) + "  Abs: " + match.getAbsoluteDistance());
                  lines.add("Tier: " + displayTierLabel(match) + "  Pool: " + pool);
               } else {
                  lines.add("Closest: none");
                  lines.add("Tier: no Seymour match");
               }
            }

            if (hasSkinMarket) {
               lines.add("Skin Market:");
               Double fallbackWeekly = CosmeticSkinValuesLoader.getValueMillions(skinTag);
               long lastReqAt = CosmeticSkinValuesLoader.getLastStatsRequestAtMs(skinTag);
               boolean inFlight = CosmeticSkinValuesLoader.isStatsRequestInFlight(skinTag);
               boolean timedOut = skinStats == null && !inFlight && lastReqAt > 0L && System.currentTimeMillis() - lastReqAt > 10000L;
               if (skinStats == null && !timedOut) {
                  lines.add("Last sale: Loading...");
                  lines.add("Unapplied avg: Loading...");
                  lines.add("Applied avg: Loading...");
               } else {
                  Long lastCoins = skinStats != null ? skinStats.lastSaleCoins : null;
                  Long lastAt = skinStats != null ? skinStats.lastSaleAtMs : null;
                  Double weeklyM = skinStats != null ? skinStats.weeklyAvgMillions : null;
                  if (weeklyM == null && fallbackWeekly != null) {
                     weeklyM = fallbackWeekly;
                  }

                  String lastSale = lastCoins != null ? formatCoins(lastCoins) + " coins" : "n/a";
                  if (lastAt != null) {
                     lastSale = lastSale + " (" + CREATED_FMT.format(Instant.ofEpochMilli(lastAt)) + ")";
                  }

                  lines.add("Last sale: " + lastSale);
                  Double unappliedM = skinStats != null ? skinStats.unappliedAvgMillions : null;
                  Double appliedM = skinStats != null ? skinStats.appliedAvgMillions : null;
                  lines.add("Unapplied avg: " + (unappliedM != null ? formatMillions(unappliedM) : (weeklyM != null ? formatMillions(weeklyM) : "n/a")));
                  lines.add("Applied avg: " + (appliedM != null ? formatMillions(appliedM) : "n/a"));
                  lines.add("Last-3 avg: " + (weeklyM != null ? formatMillions(weeklyM) : "n/a"));
               }
            }

            int pad = 6;
            int lineH = 11;
            int w = 168;

            for (String line : lines) {
               int tw = client.font.width(line);
               if (tw + pad * 2 > w) {
                  w = tw + pad * 2;
               }
            }

            int h = pad * 2 + lines.size() * lineH;
            int screenW = client.getWindow() != null ? client.getWindow().getGuiScaledWidth() : 320;
            int screenH = client.getWindow() != null ? client.getWindow().getGuiScaledHeight() : 240;
            int margin = 6;
            int x = Math.max(margin, Math.min(screenW - w - margin, containerCenterX - w / 2));
            int y = Math.max(margin, containerTop - h - 6);
            if (y + h > screenH - margin) {
               y = Math.max(margin, screenH - h - margin);
            }

            int borderColor = !hasHex && hasSkinMarket ? -13661301 : (isSeymourPiece ? -12949761 : -11898971);
            context.fill(x, y, x + w, y + h, -535817962);
            context.fill(x, y, x + w, y + 1, borderColor);
            context.fill(x, y + h - 1, x + w, y + h, borderColor);
            context.fill(x, y, x + 1, y + h, borderColor);
            context.fill(x + w - 1, y, x + w, y + h, borderColor);
            context.fill(x + 1, y + pad + lineH - 1, x + w - 1, y + pad + lineH, 1429890815);
            int ty = y + pad;

            for (int i = 0; i < lines.size(); i++) {
               int color = -1513240;
               if (i == 0) {
                  color = -5323009;
               }

               if (lines.get(i).startsWith("Hex:")) {
                  color = 0xFF000000 | hexRgb;
               }

               if (lines.get(i).startsWith("Type:")) {
                  color = typeColor(hexType);
               }

               if (lines.get(i).startsWith("Pattern:")) {
                  color = -2585601;
               }

               if (lines.get(i).startsWith("Skin Market")) {
                  color = -7345944;
               }

               if (lines.get(i).startsWith("Closest:")) {
                  color = -2103553;
               }

               if (lines.get(i).startsWith("Target:")) {
                  String targetHex = extractHexFromLabelLine(lines.get(i));
                  int targetRgb = parseHexColorInt(targetHex);
                  color = 0xFF000000 | targetRgb;
               }

               if (lines.get(i).startsWith("dE:")) {
                  color = -7697665;
               }

               if (lines.get(i).startsWith("Tier: T0")) {
                  color = -13378049;
               }

               if (lines.get(i).startsWith("Tier: T1")) {
                  color = -11162881;
               }

               if (lines.get(i).startsWith("Tier: T2")) {
                  color = -171;
               }

               if (lines.get(i).startsWith("Tier: T3")) {
                  color = -43691;
               }

               if (lines.get(i).startsWith("Tier: no")) {
                  color = -32640;
               }

               if (lines.get(i).startsWith("Last sale:")) {
                  color = -11393;
               }

               if (lines.get(i).startsWith("Unapplied avg:") || lines.get(i).startsWith("Applied avg:") || lines.get(i).startsWith("Last-3 avg:")) {
                  color = -7345944;
               }

               if (lines.get(i).startsWith("Created at:")) {
                  color = -3415809;
               }

               context.text(client.font, lines.get(i), x + pad, ty, color, false);
               ty += lineH;
            }
         }
      }
   }

   private static int estimateHoverPanelHeight(ItemStack stack, ScannedItem scanned, ScannerConfig config) {
      if (stack != null && !stack.isEmpty()) {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.font != null && config != null) {
            String itemName = stripFormatting(stack.getHoverName().getString());
            if (scanned == null) {
               scanned = ItemParser.parseItemStack(stack, config);
            }

            String hex = scanned != null ? normalizeHexWithHash(scanned.getHexColor()) : null;
            if (hex == null) {
               hex = extractHexFromStack(stack);
            }

            boolean hasHex = hex != null;
            String itemIdForType = extractSkyblockItemId(stack);
            if (itemIdForType == null || itemIdForType.isEmpty()) {
               itemIdForType = stack.getItem().toString();
            }

            String skinTag = extractSkinTag(stack, itemIdForType, itemName);
            CosmeticSkinValuesLoader.SkinMarketStats skinStats = skinTag != null ? CosmeticSkinValuesLoader.getOrRequestSkinMarketStats(skinTag) : null;
            boolean hasSkinMarket = skinTag != null;
            if (!hasHex && !hasSkinMarket) {
               return 0;
            }

            int lines = 1;
            boolean isSeymourPiece = hasHex && isSeymourPiece(itemName, itemIdForType);
            String specialPattern = hasHex && isSeymourPiece ? SeymourAnalyzer.patternDisplayName(SeymourAnalyzer.detectSpecialPattern(hex)) : null;
            if (hasHex) {
               lines++;
               if (!isSeymourPiece) {
                  lines += 2;
               }

               if (specialPattern != null) {
                  lines++;
               }
            }

            if (hasHex && isSeymourPiece && config.seymourScanningEnabled) {
               SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hex, itemName, config);
               lines += match != null ? 4 : 2;
            }

            if (hasSkinMarket) {
               lines += 4;
               Double fallbackWeekly = CosmeticSkinValuesLoader.getValueMillions(skinTag);
               long lastReqAt = CosmeticSkinValuesLoader.getLastStatsRequestAtMs(skinTag);
               boolean inFlight = CosmeticSkinValuesLoader.isStatsRequestInFlight(skinTag);
               boolean timedOut = skinStats == null && !inFlight && lastReqAt > 0L && System.currentTimeMillis() - lastReqAt > 10000L;
               if (timedOut && fallbackWeekly == null && skinStats == null) {
               }
            }

            int pad = 6;
            int lineH = 11;
            return pad * 2 + lines * lineH;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private static int typeColor(String type) {
      return switch (type) {
         case "GLITCHED" -> -43691;
         case "OG_FAIRY" -> -43521;
         case "FAIRY" -> -30516;
         case "CRYSTAL" -> -3364097;
         case "BLEACHED" -> -3364250;
         case "EXOTIC" -> -11141291;
         case "SPECIFIC_HEX" -> -10027060;
         default -> -5197648;
      };
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
         DyedItemColor colorComp = (DyedItemColor)stack.getOrDefault(DataComponents.DYED_COLOR, null);
         if (colorComp != null) {
            int rgb = colorComp.rgb();
            return String.format("#%06X", rgb & 16777215);
         } else {
            return null;
         }
      } else {
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

   private static String normalizeHexWithHash(String raw) {
      if (raw == null) {
         return null;
      }

      String h = SeymourAnalyzer.normalizeHex(raw);
      return h == null ? null : "#" + h;
   }

   private static String stripFormatting(String s) {
      return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
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
            Instant instant = Instant.parse(v);
            return CREATED_FMT.format(instant);
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

   private static int parseHexColorInt(String hexWithHash) {
      String h = SeymourAnalyzer.normalizeHex(hexWithHash);
      if (h == null) {
         return 16777215;
      }

      try {
         return Integer.parseInt(h, 16) & 16777215;
      } catch (NumberFormatException e) {
         return 16777215;
      }
   }

   private static void drawSlotHexBorder(GuiGraphicsExtractor context, int slotX, int slotY, int argbOrRgb) {
      int color = (argbOrRgb & 0xFF000000) == 0 ? 0xFF000000 | argbOrRgb & 16777215 : argbOrRgb | 0xFF000000;
      context.fill(slotX, slotY, slotX + 16, slotY + 1, color);
      context.fill(slotX, slotY + 16 - 1, slotX + 16, slotY + 16, color);
      context.fill(slotX, slotY, slotX + 1, slotY + 16, color);
      context.fill(slotX + 16 - 1, slotY, slotX + 16, slotY + 16, color);
   }

   private static boolean isArmorLikeItem(ItemStack stack, ScannedItem scanned) {
      if (stack != null && !stack.isEmpty()) {
         String id = scanned != null ? scanned.getItemId() : null;
         if (id != null && !id.isBlank()) {
            String u = id.toUpperCase(Locale.ROOT);
            if (u.contains("HELMET")
               || u.contains("CHESTPLATE")
               || u.contains("LEGGINGS")
               || u.contains("BOOTS")
               || u.contains("TOP_HAT")
               || u.contains("JACKET")
               || u.contains("TROUSERS")
               || u.contains("SHOES")) {
               return true;
            }
         }

         String reg = stack.getItem().toString().toUpperCase(Locale.ROOT);
         if (!reg.contains("_HELMET") && !reg.contains("_CHESTPLATE") && !reg.contains("_LEGGINGS") && !reg.contains("_BOOTS")) {
            String name = stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
            return name.contains("helmet")
               || name.contains("chestplate")
               || name.contains("leggings")
               || name.contains("boots")
               || name.contains("top hat")
               || name.contains("jacket")
               || name.contains("trousers")
               || name.contains("shoes");
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static String extractHexFromLabelLine(String line) {
      if (line != null && !line.isBlank()) {
         int hash = line.indexOf(35);
         return hash >= 0 && hash + 7 <= line.length() ? line.substring(hash, hash + 7) : null;
      } else {
         return null;
      }
   }

   private static String displayTierLabel(SeymourAnalyzer.MatchResult match) {
      if (match == null) {
         return "T3";
      } else {
         return !match.isCustom() && match.getRawTier() <= 0 ? "T0" : "T" + match.getTier();
      }
   }

   private static int getSlotHighlightColor(ScannedItem scanned, ScannerConfig config) {
      if (scanned == null || scanned.getCategory() == null || config == null) {
         return -2013200640;
      }

      if (!config.useCategoryHighlightColors) {
         return -2013200640;
      }

      int rgb = switch (scanned.getCategory()) {
         case CRYSTAL -> config.highlightCrystalRgb;
         case EXOTIC -> config.highlightExoticRgb;
         case OG_FAIRY -> config.highlightOgFairyRgb;
         case FAIRY -> config.highlightFairyRgb;
         case BLEACHED -> config.highlightBleachedRgb;
         case GLITCHED -> config.highlightGlitchedRgb;
         case SEYMOUR_T1 -> config.highlightSeymourT1Rgb;
         case SEYMOUR_T2 -> config.highlightSeymourT2Rgb;
         case SEYMOUR_T3 -> config.highlightSeymourT3Rgb;
         case SPECIFIC_HEX -> config.highlightSpecificHexRgb;
         default -> 65280;
      } & 16777215;
      int alpha = Math.max(0, Math.min(255, config.slotHighlightAlpha));
      return alpha << 24 | rgb;
   }

   private static boolean isHighlightEnabledForCategory(ScannedItem scanned, ScannerConfig config) {
      if (scanned != null && scanned.getCategory() != null && config != null) {
         return switch (scanned.getCategory()) {
            case CRYSTAL -> config.highlightCrystalEnabled;
            case OG_FAIRY -> config.highlightOgFairyEnabled;
            case FAIRY -> config.highlightFairyEnabled;
            default -> false;
         };
      } else {
         return false;
      }
   }
}
