package com.potatotool.renderer;

import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import com.potatotool.manager.PlayerScannerManager;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import com.potatotool.util.ColorAnalyzer;
import com.potatotool.util.PotatoTheme;
import com.potatotool.util.ProfileStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.client.DeltaTracker;
import org.lwjgl.glfw.GLFW;

public class ScanResultsOverlay {
   private static final List<ScanResultsOverlay.ClickableRegion> clickableRegions = new ArrayList<>();
   private static final Object clickableRegionsLock = new Object();
   private static boolean overlayDisabled = false;
   private static volatile int overlayX1;
   private static volatile int overlayY1;
   private static volatile int overlayX2;
   private static volatile int overlayY2;
   private static volatile boolean isHudDragging = false;
   private static int hudDragStartMouseX;
   private static int hudDragStartMouseY;
   private static int hudDragStartLayoutX;
   private static int hudDragStartLayoutY;
   private static final int MAX_VISIBLE_PLAYERS = 5;
   private static final int MAX_ITEMS_TO_SHOW = 4;
   private static final int BASE_PANEL_WIDTH = 420;
   private static final int BASE_LINE_HEIGHT = 10;
   private static final int BASE_PANEL_PADDING = 6;
   private static final int BORDER_HEIGHT = 2;
   private static int layoutX;
   private static int layoutY;
   private static int layoutPanelWidth;
   private static int layoutLineHeight;
   private static int layoutPanelPadding;
   private static int scrollOffset = 0;
   private static final int[] ISRAEL_COLORS = new int[]{-16762696, -1, -16762696, -1, -16762696, -1, -16762696, -1};
   private static final int[] POTATO_COLORS = new int[]{-12769004, -10731469, -7640812, -3890342, -2180985, -2968436, -3890342, -7640812};
   private static final int[] WHITE_COLORS = new int[]{-1, -1842205, -1, -2500135, -1, -1842205, -1, -2500135};
   private static final int POTATO_SHAPE_COLOR = -7640812;
   private static final int POTATO_SPACING = 18;
   private static final int BOTTOM_MARGIN = 420;

   public static void resetOverlay() {
      overlayDisabled = false;
   }

   public static List<ScanResultsOverlay.ClickableRegion> getClickableRegionsCopy() {
      synchronized (clickableRegionsLock) {
         return new ArrayList<>(clickableRegions);
      }
   }

   public static boolean isPointInOverlayForDrag(int mx, int my) {
      if (mx >= overlayX1 && mx <= overlayX2 && my >= overlayY1 && my <= overlayY2) {
         for (ScanResultsOverlay.ClickableRegion r : getClickableRegionsCopy()) {
            if (r.contains(mx, my)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static void startHudDrag(Minecraft client) {
      ScannerConfig config = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
      if (config != null && client.getWindow() != null) {
         isHudDragging = true;
         hudDragStartMouseX = getScaledMouseXForClick(client);
         hudDragStartMouseY = getScaledMouseYForClick(client);
         int screenW = client.getWindow().getGuiScaledWidth();
         int screenH = client.getWindow().getGuiScaledHeight();
         double scale = Math.max(0.5, Math.min(2.0, config.hudScale));
         int panelWidth = (int)(420.0 * scale);
         String anchor = config.hudAnchor != null ? config.hudAnchor.toUpperCase() : "TOP_LEFT";
         if (!anchor.equals("TOP_RIGHT") && !anchor.equals("BOTTOM_LEFT") && !anchor.equals("BOTTOM_RIGHT")) {
            anchor = "TOP_LEFT";
         }

         hudDragStartLayoutX = resolveHudX(anchor, screenW, config.hudOffsetX, panelWidth);
         hudDragStartLayoutY = resolveHudY(anchor, screenH, config.hudOffsetY);
      }
   }

   public static void stopHudDrag() {
      isHudDragging = false;
   }

   public static boolean isHudDragging() {
      return isHudDragging;
   }

   private static boolean isLeftMouseButtonPressed(Minecraft client) {
      if (client != null && client.getWindow() != null) {
         try {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), 0) == 1;
         } catch (Throwable t) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void applyLayoutToConfig(ScannerConfig config, String anchor, int screenW, int screenH, int targetLayoutX, int targetLayoutY) {
      switch (anchor) {
         case "TOP_RIGHT":
         case "BOTTOM_RIGHT":
            config.hudOffsetX = screenW - layoutPanelWidth - targetLayoutX;
            break;
         default:
            config.hudOffsetX = targetLayoutX;
      }

      switch (anchor) {
         case "BOTTOM_LEFT":
         case "BOTTOM_RIGHT":
            config.hudOffsetY = screenH - 420 - targetLayoutY;
            break;
         default:
            config.hudOffsetY = targetLayoutY;
      }
   }

   public static int getScaledMouseXForClick(Minecraft client) {
      return client != null && client.getWindow() != null
         ? (int)(client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth())
         : 0;
   }

   public static int getScaledMouseYForClick(Minecraft client) {
      return client != null && client.getWindow() != null
         ? (int)(client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight())
         : 0;
   }

   private static int[] getBorderColors(ScannerConfig config) {
      if (config == null) {
         return WHITE_COLORS;
      }

      String theme = config.hudBorderTheme != null ? config.hudBorderTheme.toUpperCase() : "WHITE";
      switch (theme) {
         case "CUSTOM": {
            int c = 0xFF000000 | (config.hudColorRgb & 16777215);
            int hi = 0xFF000000 | Math.min(16777215, (config.hudColorRgb & 16777215) + 0x202020);
            return new int[]{c, hi, c, hi, c, hi, c, hi};
         }
         case "ISRAEL":
            return ISRAEL_COLORS;
         case "POTATO":
            return POTATO_COLORS;
         case "RAINBOW":
            return PotatoTheme.eggParade();
         case "NEBULA":
            return PotatoTheme.nebulaStrip();
         case "BLUE":
            return PotatoTheme.blueFlowStrip();
         default:
            return WHITE_COLORS;
      }
   }

   private static int getBorderOffset(ScannerConfig config) {
      if (config == null) {
         return 0;
      }

      String theme = config.hudBorderTheme != null ? config.hudBorderTheme.toUpperCase() : "WHITE";
      if ("POTATO".equals(theme) || "ISRAEL".equals(theme) || "CUSTOM".equals(theme) || "WHITE".equals(theme)) {
         return 0;
      }

      return PotatoTheme.stripOffset(getBorderColors(config).length);
   }

   private static int getScaledMouseX(Minecraft client) {
      return client != null && client.getWindow() != null
         ? (int)(client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth())
         : 0;
   }

   private static int getScaledMouseY(Minecraft client) {
      return client != null && client.getWindow() != null
         ? (int)(client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight())
         : 0;
   }

   public static void renderWithDelta(GuiGraphicsExtractor context, float tickDelta) {
      render(context, null);
   }

   public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
      {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.font != null && context != null) {
            try {
               if (PotatoToolMod.getInstance() == null) {
                  return;
               }

               if (PotatoToolMod.getInstance().getDataStorage() == null) {
                  return;
               }

               if (PotatoToolMod.getInstance().getConfig() == null) {
                  return;
               }

               if (!PotatoToolMod.getInstance().getConfig().showPlayerOverlay) {
                  return;
               }

               Collection<ScannedPlayer> players;
               try {
                  Set<String> tabNames = PlayerScannerManager.getCurrentTabNamesLowercase(client);
                  players = PotatoToolMod.getInstance().getDataStorage().getPlayersWithDisplayableItemsByUsernames(tabNames);
               } catch (Throwable e) {
                  PotatoToolMod.LOGGER.debug("HUD player list failed: " + e.getMessage());
                  return;
               }

               if (players == null || players.isEmpty()) {
                  return;
               }

               try {
                  context.fill(0, 0, 0, 0, 0);
               } catch (NoSuchMethodError | NoSuchFieldError e) {
                  PotatoToolMod.LOGGER.debug("HUD draw probe failed: " + e.getMessage());
                  return;
               }

               ScannerConfig config = PotatoToolMod.getInstance().getConfig();
               int screenW = client.getWindow().getGuiScaledWidth();
               int screenH = client.getWindow().getGuiScaledHeight();
               double scale = Math.max(0.5, Math.min(2.0, config.hudScale));
               String anchor = config.hudAnchor != null ? config.hudAnchor.toUpperCase() : "TOP_LEFT";
               if (!anchor.equals("TOP_RIGHT") && !anchor.equals("BOTTOM_LEFT") && !anchor.equals("BOTTOM_RIGHT")) {
                  anchor = "TOP_LEFT";
               }

               layoutPanelWidth = (int)(420.0 * scale);
               layoutLineHeight = Math.max(6, (int)(10.0 * scale));
               layoutPanelPadding = Math.max(4, (int)(6.0 * scale));
               layoutX = resolveHudX(anchor, screenW, config.hudOffsetX);
               layoutY = resolveHudY(anchor, screenH, config.hudOffsetY);
               if (isHudDragging && !isLeftMouseButtonPressed(client)) {
                  stopHudDrag();
               }

               if (isHudDragging && isLeftMouseButtonPressed(client)) {
                  int mx = getScaledMouseX(client);
                  int my = getScaledMouseY(client);
                  int targetLayoutX = hudDragStartLayoutX + (mx - hudDragStartMouseX);
                  int targetLayoutY = hudDragStartLayoutY + (my - hudDragStartMouseY);
                  int maxX = Math.max(0, screenW - layoutPanelWidth);
                  int maxY = Math.max(0, screenH - 80);
                  targetLayoutX = Math.max(0, Math.min(maxX, targetLayoutX));
                  targetLayoutY = Math.max(0, Math.min(maxY, targetLayoutY));
                  applyLayoutToConfig(config, anchor, screenW, screenH, targetLayoutX, targetLayoutY);
                  config.save();
               }

               int y = layoutY;
               int playerIndex = 0;
               int totalFiltered = 0;
               synchronized (clickableRegionsLock) {
                  clickableRegions.clear();
               }

               for (ScannedPlayer player : players) {
                  if (player != null && player.hasDisplayableItems()) {
                     totalFiltered++;
                     if (playerIndex >= 5) {
                        break;
                     }

                     int newY = renderPlayerCard(context, player, layoutX, y, client);
                     if (newY >= 0) {
                        y = newY;
                        y += (int)(8.0 * scale);
                        playerIndex++;
                     }
                  }
               }

               if (playerIndex == 0) {
                  return;
               }

               if (totalFiltered > 5) {
                  try {
                     Component scrollHint = Component.literal("§7... and " + (totalFiltered - 5) + " more players");
                     context.text(client.font, scrollHint.getVisualOrderText(), layoutX + layoutPanelPadding, y, -5592406, true);
                  } catch (Exception var17) {
                  }
               }

               overlayX1 = layoutX;
               overlayY1 = layoutY;
               overlayX2 = layoutX + layoutPanelWidth;
               overlayY2 = y + layoutLineHeight;
            } catch (Throwable t) {
               PotatoToolMod.LOGGER.debug("HUD overlay frame skipped: " + t.getMessage());
            }
         }
      }
   }

   private static void disableOverlay(Throwable t) {
      overlayDisabled = true;
      PotatoToolMod.LOGGER.error("HUD overlay disabled after error (e.g. DrawContext API change). Restart game to retry.", t);
   }

   private static int resolveHudX(String anchor, int screenW, int offsetX) {
      return resolveHudX(anchor, screenW, offsetX, layoutPanelWidth);
   }

   private static int resolveHudX(String anchor, int screenW, int offsetX, int panelWidth) {
      switch (anchor) {
         case "TOP_RIGHT":
         case "BOTTOM_RIGHT":
            return screenW - panelWidth - offsetX;
         default:
            return offsetX;
      }
   }

   private static int resolveHudY(String anchor, int screenH, int offsetY) {
      switch (anchor) {
         case "BOTTOM_LEFT":
         case "BOTTOM_RIGHT":
            return Math.max(0, screenH - 420 - offsetY);
         default:
            return offsetY;
      }
   }

   private static void drawBorder(GuiGraphicsExtractor context, int x, int y, int width, int height) {
      ScannerConfig config = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
      int[] colors = getBorderColors(config);
      int offset = getBorderOffset(config);
      int numColors = colors.length;
      int segmentWidth = Math.max(1, width / numColors);

      for (int i = 0; i < numColors; i++) {
         int segX = x + i * segmentWidth;
         int segW = i == numColors - 1 ? x + width - segX : segmentWidth;
         if (segW > 0) {
            context.fill(segX, y, segX + segW, y + height, colors[(i + offset) % numColors]);
         }
      }
   }

   private static void drawBorderVertical(GuiGraphicsExtractor context, int x, int y, int width, int height) {
      ScannerConfig config = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
      int[] colors = getBorderColors(config);
      int offset = getBorderOffset(config);
      int numColors = colors.length;
      int segmentHeight = Math.max(1, height / numColors);

      for (int i = 0; i < numColors; i++) {
         int segY = y + i * segmentHeight;
         int segH = i == numColors - 1 ? y + height - segY : segmentHeight;
         if (segH > 0) {
            context.fill(x, segY, x + width, segY + segH, colors[(i + offset) % numColors]);
         }
      }
   }

   private static void drawLittlePotato(GuiGraphicsExtractor context, int x, int y) {
      context.fill(x + 2, y, x + 4, y + 1, -7640812);
      context.fill(x + 1, y + 1, x + 5, y + 2, -7640812);
      context.fill(x, y + 2, x + 6, y + 3, -7640812);
      context.fill(x + 1, y + 3, x + 5, y + 4, -7640812);
      context.fill(x + 2, y + 4, x + 4, y + 5, -7640812);
   }

   private static void drawPotatoDecorations(GuiGraphicsExtractor context, int cardX, int cardY, int pw, int totalHeight) {
      ScannerConfig config = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
      if (config != null) {
         String theme = config.hudBorderTheme != null ? config.hudBorderTheme.toUpperCase() : "";
         if ("POTATO".equals(theme)) {
            int innerYTop = cardY + 2;
            int innerYBottom = cardY + totalHeight - 2 - 5;
            int left = cardX + 2 + 2;
            int right = cardX + pw - 2 - 8;

            for (int px = left; px <= right; px += 18) {
               drawLittlePotato(context, px, innerYTop);
               drawLittlePotato(context, px, innerYBottom);
            }
         }
      }
   }

   private static int renderPlayerCard(GuiGraphicsExtractor context, ScannedPlayer player, int x, int y, Minecraft client) {
      if (player == null) {
         return -1;
      }

      if (context == null) {
         return -1;
      }

      if (client != null && client.font != null) {
         try {
            int startY = y;
            int cardX = x;
            int cardY = y;
            int pw = layoutPanelWidth;
            int lh = layoutLineHeight;
            int pad = layoutPanelPadding;
            int headerHeight = lh + 4;
            int profileHeight = lh + 2;
            int displayableCount = player.getDisplayableItemCount();
            int itemsHeight = Math.min(displayableCount, 4) * lh + (displayableCount > 4 ? lh : 0);
            int totalHeight = headerHeight + profileHeight + itemsHeight + pad * 2 + 8;
            context.fill(cardX, cardY, cardX + pw, cardY + totalHeight, PotatoTheme.HUD_CARD);
            context.fill(cardX, cardY, cardX + 1, cardY + totalHeight, PotatoTheme.HUD_EDGE);
            context.fill(cardX + pw - 1, cardY, cardX + pw, cardY + totalHeight, PotatoTheme.HUD_EDGE);
            drawBorder(context, cardX, cardY, pw, 2);
            drawBorder(context, cardX, cardY + totalHeight - 2, pw, 2);
            drawBorderVertical(context, cardX, cardY, 2, totalHeight);
            drawBorderVertical(context, cardX + pw - 2, cardY, 2, totalHeight);
            drawPotatoDecorations(context, cardX, cardY, pw, totalHeight);
            y += pad;
            String username = player.getUsername();
            if (username == null || username.isEmpty()) {
               username = "Unknown";
            }

            double skyblockLevel = player.getSkyblockLevel();
            String levelColor = getLevelColor(skyblockLevel);
            if (levelColor == null) {
               levelColor = "7";
            }

            String rankPrefix = player.getRankFormatted();
            if (rankPrefix == null) {
               rankPrefix = "";
            }

            String rankNameColor = player.getRankNameColor();
            if (rankNameColor == null) {
               rankNameColor = "§f";
            }

            String levelPart = "§" + levelColor + "[ " + (int)skyblockLevel + " ] ";
            String beforeName = rankPrefix + (rankPrefix.isEmpty() ? "" : " ") + levelPart;
            String headerText = beforeName + rankNameColor + username;
            int nameStartX = cardX + pad + client.font.width(Component.literal(beforeName).getVisualOrderText());
            int nameWidth = client.font.width(Component.literal(rankNameColor + username).getVisualOrderText());
            int nameEndX = nameStartX + nameWidth;
            context.text(client.font, Component.literal(headerText).getVisualOrderText(), cardX + pad, y, -1, true);
            int hitPad = 4;
            synchronized (clickableRegionsLock) {
               clickableRegions.add(new ScanResultsOverlay.ClickableRegion(nameStartX - hitPad, y - 1, nameEndX + hitPad, y + lh + 2 + hitPad, username));
            }

            y += lh + 2;
            String profileName = player.getSelectedProfile();
            if (profileName != null && !profileName.isEmpty()) {
               Component profileText = Component.literal(ProfileStyle.legacy(profileName) + " §a★");
               context.text(client.font, profileText.getVisualOrderText(), cardX + pad, y, -2039584, true);
               y += lh + 2;
            }

            context.fill(cardX + pad, y, cardX + pw - pad, y + 1, PotatoTheme.HUD_DIVIDER);
            y += 4;
            int itemCount = displayableCount;
            int seymourTotal = player.getSeymourPieceCount();
            int seymourRare = player.getSeymourRareCount();
            String itemsHeaderText = "§aItems: §f" + itemCount;
            if (seymourTotal > 0) {
               itemsHeaderText = itemsHeaderText
                  + "  §8|  §dSeymour: §f"
                  + seymourTotal
                  + " total §7(§e"
                  + seymourRare
                  + " rare§7)";
            }

            Component itemsHeader = Component.literal(itemsHeaderText);
            context.text(client.font, itemsHeader.getVisualOrderText(), cardX + pad, y, -1, true);
            y += lh + 2;
            List<ScannedItem> items = player.getDisplayableItems();
            if (items != null && !items.isEmpty()) {
               int displayedItems = 0;

               for (ScannedItem item : items) {
                  if (item != null && displayedItems < 4) {
                     boolean show = true;
                     if (show) {
                        try {
                           String categoryTag = item.getCategoryTag();
                           if (categoryTag == null || categoryTag.isEmpty()) {
                              categoryTag = "Item";
                           }

                           String itemName = resolveHudItemName(item);

                           if (item.getCategory() == ScannedItem.ItemCategory.CAKE && item.getCakeYear() != null) {
                              categoryTag = "Cake §6(Y" + item.getCakeYear() + ")";
                           }

                           String hexColor = item.getHexColor();
                           MutableComponent itemText;
                           if (hexColor != null && !hexColor.isEmpty()) {
                              int rgb = ColorAnalyzer.hexToInt(hexColor);
                              TextColor textColor = TextColor.fromRgb(rgb);
                              String hexDisplay = hexColor.startsWith("#") ? hexColor : "#" + hexColor;
                              itemText = Component.literal("  §7• ")
                                 .append(Component.literal(itemName).withStyle(s -> s.withColor(textColor)))
                                 .append(
                                    Component.literal(" ").append(Component.literal(hexDisplay).withStyle(s -> s.withColor(textColor)))
                                 )
                                 .append(Component.literal(" §f" + categoryTag + locationSuffix(item) + scuffedStarsRecombSuffix(item)));
                           } else {
                              String categoryColor = getCategoryColor(item.getCategory());
                              if (categoryColor == null) {
                                 categoryColor = "§f";
                              }

                              itemText = Component.literal(
                                 "  §7• " + categoryColor + itemName + " §f" + categoryTag + locationSuffix(item) + scuffedStarsRecombSuffix(item)
                              );
                           }

                           context.text(client.font, itemText.getVisualOrderText(), cardX + pad, y, -1, true);
                           y += lh;
                           displayedItems++;
                        } catch (Exception e) {
                           PotatoToolMod.LOGGER.warn("Failed to render item in HUD: " + e.getMessage());
                        }
                     }
                  }
               }

               if (itemCount > 4) {
                  try {
                     Component moreText = Component.literal("§7  +§e" + (itemCount - 4) + " §fitem" + (itemCount - 4 == 1 ? "" : "s"));
                     int moreLineY = y;
                     int moreTextW = client.font.width(moreText);
                     context.text(client.font, moreText.getVisualOrderText(), cardX + pad, y, -1, true);
                     y += lh;
                     int mouseX = getScaledMouseX(client);
                     int mouseY = getScaledMouseY(client);
                     if (mouseX >= cardX + pad && mouseX <= cardX + pad + moreTextW && mouseY >= moreLineY && mouseY < moreLineY + lh) {
                        List<Component> tooltipLines = new ArrayList<>();
                        int skipped = 0;

                        for (ScannedItem item : items) {
                           if (item != null
                              && (
                                 item.isMentionable()
                                    || item.getCategory() == ScannedItem.ItemCategory.CAKE
                                    || item.getCategory() == ScannedItem.ItemCategory.COSMETIC_SKIN
                              )) {
                              if (skipped < 4) {
                                 skipped++;
                              } else {
                                 String categoryTag = item.getCategoryTag() != null ? item.getCategoryTag() : "Item";
                                 if (item.getCategory() == ScannedItem.ItemCategory.CAKE && item.getCakeYear() != null) {
                                    categoryTag = "Cake §6(Y" + item.getCakeYear() + ")";
                                 }

                                 String itemName = resolveHudItemName(item);

                                 String hexColor = item.getHexColor();
                                 String scrSuffix = scuffedStarsRecombSuffix(item);
                                 MutableComponent line;
                                 if (hexColor != null && !hexColor.isEmpty()) {
                                    int rgb = ColorAnalyzer.hexToInt(hexColor);
                                    String hexDisplay = hexColor.startsWith("#") ? hexColor : "#" + hexColor;
                                    line = Component.literal("  §7• ")
                                       .append(Component.literal(itemName).withStyle(s -> s.withColor(TextColor.fromRgb(rgb))))
                                       .append(Component.literal(" " + hexDisplay + " §f" + categoryTag + locationSuffix(item) + scrSuffix));
                                 } else {
                                    String cc = getCategoryColor(item.getCategory());
                                    if (cc == null) {
                                       cc = "§f";
                                    }

                                    line = Component.literal("  §7• " + cc + itemName + " §f" + categoryTag + locationSuffix(item) + scrSuffix);
                                 }

                                 tooltipLines.add(line);
                              }
                           }
                        }

                        if (!tooltipLines.isEmpty()) {
                           context.setTooltipForNextFrame(client.font, tooltipLines, Optional.empty(), mouseX + 12, mouseY + 12);
                        }
                     }
                  } catch (Exception var51) {
                  }
               }
            }

            return startY + totalHeight;
         } catch (Exception e) {
            PotatoToolMod.LOGGER.error("Error rendering player card for " + (player != null ? player.getUsername() : "null"), e);
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static String resolveHudItemName(ScannedItem item) {
      String itemName = com.potatotool.util.ItemNames.resolve(item);
      if (itemName.length() > 42) {
         itemName = itemName.substring(0, 39) + "...";
      }

      return itemName;
   }

   private static String formatItemId(String itemId) {
      if (itemId != null && !itemId.isEmpty()) {
         try {
            String[] parts = itemId.split("_");
            if (parts.length == 0) {
               return itemId;
            }

            StringBuilder formatted = new StringBuilder();

            for (String part : parts) {
               if (part != null && !part.isEmpty()) {
                  if (formatted.length() > 0) {
                     formatted.append(" ");
                  }

                  if (part.length() == 1) {
                     formatted.append(part.toUpperCase());
                  } else {
                     formatted.append(part.substring(0, 1).toUpperCase());
                     formatted.append(part.substring(1).toLowerCase());
                  }
               }
            }

            String result = formatted.toString();
            return result.isEmpty() ? "Unknown" : result;
         } catch (Exception e) {
            return itemId;
         }
      } else {
         return "Unknown";
      }
   }

   private static String getLevelColor(double level) {
      if (Double.isNaN(level) || Double.isInfinite(level) || level < 0.0) {
         return "7";
      } else if (level < 50.0) {
         return "7";
      } else if (level < 100.0) {
         return "a";
      } else if (level < 150.0) {
         return "2";
      } else if (level < 200.0) {
         return "e";
      } else if (level < 300.0) {
         return "6";
      } else if (level < 400.0) {
         return "9";
      } else {
         return level < 500.0 ? "5" : "d";
      }
   }

   private static String locationSuffix(ScannedItem item) {
      if (item == null) {
         return "";
      }

      String loc = item.getLocation();
      String profile = item.getProfileName();
      StringBuilder sb = new StringBuilder();
      if (loc != null && !loc.isEmpty()) {
         String shown = loc.regionMatches(true, 0, "Ender Chest", 0, "Ender Chest".length())
            ? "Enderchest" + loc.substring("Ender Chest".length())
            : loc;
         sb.append(" §7(§b").append(shown).append("§7)");
      }

      if (profile != null && !profile.isBlank()) {
         sb.append(" ").append(ProfileStyle.legacy(profile));
      }

      return sb.toString();
   }

   private static String scuffedStarsRecombSuffix(ScannedItem item) {
      if (item == null) {
         return "";
      }

      boolean hasStars = item.getDungeonStars() != null && item.getDungeonStars() >= 0;
      boolean hasRecomb = item.isRecombobulated();
      return !item.isScuffed() && !hasStars && !hasRecomb ? " §aClean" : " §7Scuffed";
   }

   private static String getCategoryColor(ScannedItem.ItemCategory category) {
      if (category == null) {
         return "§7";
      }

      switch (category) {
         case VALUABLE:
            return "§6";
         case LEGACY_REFORGE:
            return "§c";
         case GHOST_REFORGE:
            return "§d";
         case CRYSTAL:
            return "§b";
         case FAIRY:
            return "§d";
         case BLEACHED:
            return "§f";
         case OG_FAIRY:
            return "§5";
         case GLITCHED:
            return "§4";
         case EXOTIC:
            return "§e";
         case SPECIFIC_HEX:
            return "§3";
         case CAKE:
            return "§6";
         case COSMETIC_SKIN:
            return "§d";
         case SEYMOUR_T1:
            return "§a";
         case SEYMOUR_T2:
            return "§e";
         case SEYMOUR_T3:
            return "§c";
         default:
            return "§7";
      }
   }

   public static void scrollUp() {
      scrollOffset = Math.max(0, scrollOffset - 5);
   }

   public static void scrollDown() {
      scrollOffset += 5;
   }

   public static void resetScroll() {
      scrollOffset = 0;
   }

   public static final class ClickableRegion {
      public final int x1;
      public final int y1;
      public final int x2;
      public final int y2;
      public final String username;

      public ClickableRegion(int x1, int y1, int x2, int y2, String username) {
         this.x1 = x1;
         this.y1 = y1;
         this.x2 = x2;
         this.y2 = y2;
         this.username = username != null ? username : "";
      }

      public boolean contains(int mx, int my) {
         return mx >= this.x1 && mx <= this.x2 && my >= this.y1 && my <= this.y2;
      }
   }
}
