package com.potatotool.config;

import com.potatotool.PotatoToolMod;
import com.potatotool.gui.LookupProfileScreen;
import com.potatotool.manager.AuctionNotificationManager;
import com.potatotool.manager.ChatMacroRunner;
import com.potatotool.manager.DiscordWebhook;
import com.potatotool.manager.HitCache;
import com.potatotool.manager.OnlineNotifier;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import com.potatotool.util.CosmeticSkinValuesLoader;
import com.potatotool.util.DefaultArmorColorsLoader;
import com.potatotool.util.PotatoTheme;
import com.potatotool.util.SeymourAnalyzer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;

public class ModMenuIntegration {
   private static volatile boolean openConfigNextTick = false;

   public static void openConfigScreen(Minecraft client) {
      if (client != null) {
         openConfigNextTick = true;
      }
   }

   public static void tickOpenConfig(Minecraft client) {
      if (openConfigNextTick && client != null) {
         openConfigNextTick = false;
         client.setScreen(new ModMenuIntegration.PotatoToolConfigScreen(client.screen));
      }
   }

   public static class PotatoToolConfigScreen extends Screen {
      private final Screen parent;
      private int currentPage = 0;
      private final Map<String, Boolean> keyValidStatus = new HashMap<>();
      private static final int SIDEBAR_WIDTH = 180;
      private static final int TITLE_BAR_HEIGHT = 34;
      private static final int PADDING = 12;
      private static final int NAV_ITEM_HEIGHT = 26;
      private static final int LINE_HEIGHT = 22;
      private static final int BTN_HEIGHT = 22;
      private static final int DEFAULT_ACCENT = 4878245;
      private int TEXT_WHITE = 0xFFE6EAF0;
      private int TEXT_MUTED = 0xFF8B929C;
      private boolean darkGui = true;
      private static final int HIGHLIGHT_SWATCH_SIZE = 18;
      private static final int HIGHLIGHT_SWATCH_GAP = 4;
      private static final String[] GUI_ACCENT_THEME_KEYS = new String[]{
         "STATIC_BLUE", "BLUE_FLOW", "VIOLET_FLOW", "NEBULA", "STATIC_CYAN", "STATIC_GREEN", "STATIC_PINK", "STATIC_BROWN"
      };
      private static final String[] GUI_ACCENT_THEME_LABELS = new String[]{
         "Blue", "Blue Flow", "Violet Flow", "Nebula", "Cyan", "Green", "Pink", "Brown"
      };
      private static final int[] GUI_ACCENT_THEME_COLORS = new int[]{
         0x4A8FD4, 0x6EA8FF, 0xA78BFA, 0x8B6CFF, 4892159, 7316810, 10832495, 10841930
      };
      private static final int[] HIGHLIGHT_SWATCHES = new int[]{6737151, 5635925, 16733695, 16746700, 13805177, 16733525, 5614335, 16777045, 6750156, 16777215};
      private static final String[] HIGHLIGHT_COLOR_LABELS = new String[]{
         "Crystal", "Exotic", "OG Fairy", "Fairy", "Bleached", "Glitched", "Seymour T1", "Seymour T2", "Specific Hex", "Seymour T3"
      };
      private static final int APPEARANCE_HEX_INPUT_W = 88;
      private static final int APPEARANCE_OPACITY_INPUT_W = 72;
      private static final String[] NAV_LABELS = new String[]{
         "API Keys", "Lookup", "Categories", "Special", "Skins", "Advanced", "HUD", "Appearance", "Seymour", "Automatic", "Webhook", "Notifier", "Hits"
      };
      private int layoutW = 800;
      private int layoutH = 600;
      private int panelX = 0;
      private int panelY = 0;
      private int panelW = 980;
      private int panelH = 700;
      private static final int MIN_PANEL_W = 560;
      private static final int MIN_PANEL_H = 420;
      private boolean dragging = false;
      private double dragStartX = 0.0;
      private double dragStartY = 0.0;
      private int dragStartPanelX = 0;
      private int dragStartPanelY = 0;
      private boolean macroDragging = false;
      private String macroDragCommand = null;
      private int macroDragFromSequence = -1;
      private int macroHoverInsert = -1;
      private double visualPanelX = 0.0;
      private double visualPanelY = 0.0;
      private boolean resizing = false;
      private double resizeStartX = 0.0;
      private double resizeStartY = 0.0;
      private int resizeStartW = 0;
      private int resizeStartH = 0;
      private static final int RESIZE_HANDLE = 28;
      private static final int[] PRESET_W = new int[]{760, 980, 1220};
      private static final int[] PRESET_H = new int[]{540, 700, 820};
      private final StringBuilder apiKeyInput = new StringBuilder();
      private boolean apiKeyInputFocused = false;
      private final StringBuilder webhookUrlInput = new StringBuilder();
      private boolean webhookUrlFocused = false;
      private String webhookTestStatus = "";
      private final StringBuilder notifierNameInput = new StringBuilder();
      private boolean notifierNameFocused = false;
      private int hitsScroll = 0;
      private final StringBuilder cakeSpecificYearsInput = new StringBuilder();
      private boolean cakeSpecificYearsFocused = false;
      private final StringBuilder cakeMaxYearInput = new StringBuilder();
      private boolean cakeMaxYearFocused = false;
      private final List<int[]> cakeChipHitboxes = new ArrayList<>();
      private int[] cakeAddBoxHit = new int[]{0, 0, 0, 0};
      private int[] cakeAddBtnHit = new int[]{0, 0, 0, 0};
      private int[] cakeDefaultsHit = new int[]{0, 0, 0, 0};
      private int[] cakeClearHit = new int[]{0, 0, 0, 0};
      private final StringBuilder shareCodeInput = new StringBuilder();
      private boolean shareCodeFocused = false;
      private String shareStatus = "";
      private int[] shareCopyHit = new int[]{0, 0, 0, 0};
      private int[] shareImportHit = new int[]{0, 0, 0, 0};
      private int[] shareInputHit = new int[]{0, 0, 0, 0};
      private final StringBuilder specificHexScanInput = new StringBuilder();
      private boolean specificHexScanFocused = false;
      private final StringBuilder minLevelInput = new StringBuilder();
      private final StringBuilder maxLevelInput = new StringBuilder();
      private boolean minLevelFocused = false;
      private boolean maxLevelFocused = false;
      private static final int LEVEL_INPUT_WIDTH = 120;
      private final StringBuilder minSkinValueInput = new StringBuilder();
      private boolean minSkinValueFocused = false;
      private static final String[] SKIN_SCAN_MODES = new String[]{"BOTH", "APPLIED_ONLY", "UNAPPLIED_ONLY"};
      private final StringBuilder lookupIgnInput = new StringBuilder();
      private boolean lookupIgnFocused = false;
      private volatile String lookupScanStatus = "";
      private ScannedPlayer lookupSelectedPlayer = null;
      private final List<String> lookupPreviewLines = new ArrayList<>();
      private int lookupPreviewScroll = 0;
      private static final int LOOKUP_INPUT_WIDTH = 260;
      private static final int LOOKUP_PREVIEW_LINE_H = 11;
      private static final int LOOKUP_PREVIEW_TOP = 160;
      private final StringBuilder seymourArmorFilterInput = new StringBuilder();
      private boolean seymourArmorFilterFocused = false;
      private final StringBuilder ahWatchTagsInput = new StringBuilder();
      private final StringBuilder ahSeymourHexInput = new StringBuilder();
      private final StringBuilder ahSkinMuteInput = new StringBuilder();
      private boolean ahWatchTagsFocused = false;
      private boolean ahSeymourHexFocused = false;
      private boolean ahSkinMuteFocused = false;
      private static final String[] AH_QUICK_TYPES = new String[]{
         "FAIRY", "OG_FAIRY", "EXOTIC", "CRYSTAL", "BLEACHED", "GLITCHED", "SEYMOUR_T1", "SEYMOUR_T2", "ARMOR_SKIN"
      };
      private static final String[] AH_NOTIFY_FILTER_LABELS = new String[]{
         "Fairy", "OG Fairy", "Exotic", "Armor Skin", "Pet Skin", "Crystal", "Bleached", "Glitched", "Seymour T1", "Seymour T2", "Seymour Hex"
      };
      private static final int[] AH_NOTIFY_FILTER_COLORS = new int[]{
         16746700, 16733695, 5635925, 6737151, 5627374, 6737151, 13805177, 16733525, 16777045, 16755285, 13404415
      };
      private static final int AH_FILTER_CHIP_GAP = 4;
      private static final int AH_FILTER_MIN_CHIP_W = 108;
      private int ahQuickTypeIndex = 0;
      private final StringBuilder ahQuickSetInput = new StringBuilder();
      private final StringBuilder ahQuickSkinInput = new StringBuilder();
      private boolean ahQuickSetFocused = false;
      private boolean ahQuickSkinFocused = false;
      private final List<String> ahQuickSetSuggestions = new ArrayList<>();
      private final List<String> ahQuickSkinSuggestionTags = new ArrayList<>();
      private final List<String> ahQuickSkinSuggestionLabels = new ArrayList<>();
      private String ahQuickSelectedSet = "";
      private String ahQuickSelectedSkin = "";
      private String ahQuickActionStatus = "";
      private final StringBuilder hudColorInput = new StringBuilder();
      private boolean hudColorFocused = false;
      private final StringBuilder guiOpacityInput = new StringBuilder();
      private boolean guiOpacityFocused = false;
      private final StringBuilder highlightHexInput = new StringBuilder();
      private int highlightHexFocusedRow = -1;
      private final StringBuilder numberInput = new StringBuilder();
      private int numberInputField = -1;
      private static final int NUM_APPEARANCE_SLOT_ALPHA = 1;
      private static final int NUM_SEYMOUR_TARGET_STAGE = 2;
      private static final int NUM_SEYMOUR_STAGE_TOLERANCE = 3;
      private static final int NUM_AH_POLL_INTERVAL = 4;
      private static final int NUM_AH_TAGS_PER_POLL = 5;
      private static final int NUM_AH_MAX_ALERTS = 6;
      private static final int NUM_AH_ALERT_MIN_PRICE = 7;
      private static final int NUM_AH_ALERT_MAX_PRICE = 8;
      private static final int NUM_AH_SKIN_MIN_VALUE = 9;
      private static final int NUM_AH_SKIN_MAX_VALUE = 10;
      private static final int NUM_AH_DEDUPE_SECONDS = 11;
      private static final int NUM_AH_HEX_DELTA = 12;
      private static final int NUM_AH_HEX_STAGE_TOLERANCE = 13;
      private static final int NUM_AUTOBUY_MIN_PRICE = 14;
      private static final int NUM_AUTOBUY_MAX_PRICE = 15;
      private static final int NUM_AUTOBUY_COOLDOWN = 16;
      private int ahPageScroll = 0;
      private int ahPageMaxScroll = 0;
      private static final int AH_SCROLL_STEP = 18;
      private final StringBuilder manualBuyUuidInput = new StringBuilder();
      private boolean manualBuyUuidFocused = false;
      private String autoBuyStatusLine = "";
      private final StringBuilder autoBuyNameInput = new StringBuilder();
      private final StringBuilder autoBuyHexInput = new StringBuilder();
      private final StringBuilder autoBuyRulesInput = new StringBuilder();
      private boolean autoBuyNameFocused = false;
      private boolean autoBuyHexFocused = false;
      private boolean autoBuyRulesFocused = false;
      private static final float DISCORD_TEXT_SCALE = 0.75F;
      private static final int DISCORD_BTN_X_OFFSET = 8;
      private static final String DISCORD_INVITE = "https://discord.gg/PvEYc4Kwy";
      private static final int DISCORD_COLOR = -10983950;
      private static final int DEFAULT_PANEL_W = 980;
      private static final int DEFAULT_PANEL_H = 700;
      private static final int ADVANCED_LEVEL_SECTION_H = 98;
      private static final String[] HUD_ANCHORS = new String[]{"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"};
      private static final String[] HUD_THEMES = new String[]{"WHITE", "BLUE", "NEBULA", "RAINBOW", "ISRAEL", "POTATO", "CUSTOM"};

      public PotatoToolConfigScreen(Screen parent) {
         super(Component.literal("PotatoToolV2"));
         this.parent = parent;
      }

      private void closeAndReturn() {
         if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
         }
      }

      private int contentX() {
         return 192;
      }

      private int contentWidth() {
         return Math.max(200, this.layoutW - 180 - 24);
      }

      private int contentY() {
         return 40;
      }

      protected void init() {
         super.init();
         int screenW = this.minecraft != null && this.minecraft.getWindow() != null ? this.minecraft.getWindow().getGuiScaledWidth() : this.width;
         int screenH = this.minecraft != null && this.minecraft.getWindow() != null ? this.minecraft.getWindow().getGuiScaledHeight() : this.height;
         if (this.panelW < 560 || this.panelH < 420) {
            this.panelW = Math.min(980, Math.max(560, screenW - 80));
            this.panelH = Math.min(700, Math.max(420, screenH - 80));
         }

         this.panelW = Math.min(screenW - 40, Math.max(560, this.panelW));
         this.panelH = Math.min(screenH - 40, Math.max(420, this.panelH));
         this.panelX = Math.max(0, Math.min(screenW - this.panelW, this.panelX));
         this.panelY = Math.max(0, Math.min(screenH - this.panelH, this.panelY));
         if (this.panelX + this.panelW > screenW || this.panelY + this.panelH > screenH) {
            this.panelX = (screenW - this.panelW) / 2;
            this.panelY = (screenH - this.panelH) / 2;
         }

         this.layoutW = this.panelW;
         this.layoutH = this.panelH;
         this.setFocused(null);
         if (this.webhookUrlInput.length() == 0) {
            ScannerConfig cfg = this.getConfig();
            if (cfg != null && cfg.discordWebhookUrl != null && !cfg.discordWebhookUrl.isBlank()) {
               this.webhookUrlInput.append(cfg.discordWebhookUrl);
            }
         }
      }

      public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
         if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
         }

         int mx = (int)click.x();
         int my = (int)click.y();
         if (mx >= this.panelX && mx < this.panelX + this.panelW && my >= this.panelY && my < this.panelY + this.panelH) {
            int lx = mx - this.panelX;
            int ly = my - this.panelY;
            if (lx >= this.panelW - 28 && ly >= this.panelH - 28) {
               this.resizing = true;
               this.resizeStartX = click.x();
               this.resizeStartY = click.y();
               this.resizeStartW = this.panelW;
               this.resizeStartH = this.panelH;
               return true;
            }

            if (ly < TITLE_BAR_HEIGHT) {
               int dtW = this.font.width("Discord");
               int dScaledW = dtW;
               int dScaledH = 9;
               int discordX = this.panelW - 14 - dScaledW;
               int discordY0 = (TITLE_BAR_HEIGHT - dScaledH) / 2;
               ScannerConfig titleCfg = this.getConfig();
               String modeLabel = titleCfg != null && titleCfg.guiDarkMode ? "Dark" : "Light";
               int modeW = this.font.width(modeLabel);
               int modeX = discordX - 12 - modeW;
               if (lx >= modeX && lx < modeX + modeW && ly >= discordY0 && ly < discordY0 + dScaledH) {
                  if (titleCfg != null) {
                     titleCfg.guiDarkMode = !titleCfg.guiDarkMode;
                     titleCfg.save();
                  }

                  return true;
               }

               if (lx >= discordX && lx < discordX + dScaledW && ly >= discordY0 && ly < discordY0 + dScaledH) {
                  if (this.minecraft != null) {
                     this.minecraft.keyboardHandler.setClipboard("https://discord.gg/PvEYc4Kwy");
                     if (this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.literal("§aDiscord link copied! §7https://discord.gg/PvEYc4Kwy"));
                     }
                  }

                  return true;
               } else {
                  this.dragging = true;
                  this.dragStartX = click.x();
                  this.dragStartY = click.y();
                  this.dragStartPanelX = this.panelX;
                  this.dragStartPanelY = this.panelY;
                  return true;
               }
            } else {
               int navY0 = TITLE_BAR_HEIGHT + 8;

               for (int i = 0; i < NAV_LABELS.length; i++) {
                  int y0 = navY0 + i * 24;
                  if (lx >= 0 && lx < 180 && ly >= y0 && ly < y0 + 22) {
                     if (this.cakeSpecificYearsFocused) {
                        this.applyCakeSpecificYearsInput(this.getConfig());
                        this.cakeSpecificYearsFocused = false;
                     }

                     if (this.cakeMaxYearFocused) {
                        this.applyCakeMaxYearInput(this.getConfig());
                        this.cakeMaxYearFocused = false;
                     }

                     if (this.specificHexScanFocused) {
                        this.applySpecificHexScanInput(this.getConfig());
                        this.specificHexScanFocused = false;
                     }

                     if (this.minLevelFocused || this.maxLevelFocused) {
                        this.applyLevelInputs(this.getConfig());
                        this.minLevelFocused = false;
                        this.maxLevelFocused = false;
                     }

                     if (this.webhookUrlFocused) {
                        this.applyWebhookUrl(this.getConfig());
                        this.webhookUrlFocused = false;
                     }

                     this.notifierNameFocused = false;

                     if (this.minSkinValueFocused) {
                        this.applyMinSkinValueInput(this.getConfig());
                        this.minSkinValueFocused = false;
                     }

                     this.applyAndClearAutoBuyInputs(this.getConfig());
                     this.applyAndClearSeymourInputs(this.getConfig());
                     this.applyAndClearAuctionInputs(this.getConfig());
                     this.applyAndClearAppearanceInputs(this.getConfig());
                     this.applyAndClearNumberInput(this.getConfig());
                     this.lookupIgnFocused = false;
                     this.currentPage = i;
                     this.apiKeyInputFocused = false;
                     return true;
                  }
               }

               int doneW = 120;
               int doneX = this.layoutW / 2 - doneW / 2;
               int doneY = this.layoutH - 28;
               int sizeRowY = this.layoutH - 28 - 22 - 8;
               if (ly >= sizeRowY && ly < sizeRowY + 22) {
                  int sizeLabelW = this.font.width("Size: ");

                  for (int i = 0; i < PRESET_W.length; i++) {
                     int sx = this.contentX() + sizeLabelW + 8 + i * 72;
                     if (lx >= sx && lx < sx + 68) {
                        int screenW = this.minecraft != null && this.minecraft.getWindow() != null
                           ? this.minecraft.getWindow().getGuiScaledWidth()
                           : this.width;
                        int screenH = this.minecraft != null && this.minecraft.getWindow() != null
                           ? this.minecraft.getWindow().getGuiScaledHeight()
                           : this.height;
                        this.panelW = Math.max(560, Math.min(PRESET_W[i], screenW - this.panelX - 20));
                        this.panelH = Math.max(420, Math.min(PRESET_H[i], screenH - this.panelY - 20));
                        this.layoutW = this.panelW;
                        this.layoutH = this.panelH;
                        return true;
                     }
                  }
               }

               if (lx >= doneX && lx < doneX + doneW && ly >= doneY && ly < doneY + 22) {
                  if (this.cakeSpecificYearsFocused) {
                     this.applyCakeSpecificYearsInput(this.getConfig());
                     this.cakeSpecificYearsFocused = false;
                  }

                  if (this.cakeMaxYearFocused) {
                     this.applyCakeMaxYearInput(this.getConfig());
                     this.cakeMaxYearFocused = false;
                  }

                  if (this.specificHexScanFocused) {
                     this.applySpecificHexScanInput(this.getConfig());
                     this.specificHexScanFocused = false;
                  }

                  if (this.minLevelFocused || this.maxLevelFocused) {
                     this.applyLevelInputs(this.getConfig());
                     this.minLevelFocused = false;
                     this.maxLevelFocused = false;
                  }

                  if (this.minSkinValueFocused) {
                     this.applyMinSkinValueInput(this.getConfig());
                     this.minSkinValueFocused = false;
                  }

                  if (this.webhookUrlFocused || this.webhookUrlInput.length() > 0) {
                     this.applyWebhookUrl(this.getConfig());
                     this.webhookUrlFocused = false;
                  }

                  if (this.hudColorFocused) {
                     this.applyHudColorInput(this.getConfig());
                     this.hudColorFocused = false;
                  }

                  this.applyAndClearAutoBuyInputs(this.getConfig());
                  this.applyAndClearSeymourInputs(this.getConfig());
                  this.applyAndClearAuctionInputs(this.getConfig());
                  this.applyAndClearAppearanceInputs(this.getConfig());
                  this.applyAndClearNumberInput(this.getConfig());
                  this.closeAndReturn();
                  return true;
               } else {
                  ScannerConfig config = this.getConfig();
                  int cx = this.contentX();
                  int cw = this.contentWidth();
                  int cy = this.contentY();
                  if (this.numberInputField >= 0 && !this.isFocusedNumberInputHit(lx, ly, cx, cy, cw)) {
                     this.applyAndClearNumberInput(config);
                  }

                  if (this.currentPage == 0) {
                     int y = cy + 40;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        this.apiKeyInputFocused = true;
                        return true;
                     }

                     this.apiKeyInputFocused = false;
                     y += 26;
                     if (lx >= cx && lx < cx + 120 && ly >= y && ly < y + 22) {
                        String key = this.apiKeyInput.toString().trim();
                        if (!key.isEmpty()) {
                           config.addApiKey(key);
                           config.save();
                           this.apiKeyInput.setLength(0);
                        }

                        return true;
                     }

                     if (lx >= cx + 128 && lx < cx + 228 && ly >= y && ly < y + 22) {
                        if (this.minecraft != null) {
                           this.minecraft.keyboardHandler.setClipboard("https://developer.hypixel.net/");
                        }

                        return true;
                     }

                     ScannerConfig.ensureApiKeyTimestamps(config);
                     y += 30;

                     for (int i = 0; i < config.apiKeys.size(); i++) {
                        String key = config.apiKeys.get(i);
                        int removeX = cx + cw - 70;
                        int copyX = removeX - 64;
                        if (lx >= copyX && lx < copyX + 60 && ly >= y && ly < y + 22) {
                           if (this.minecraft != null) {
                              this.minecraft.keyboardHandler.setClipboard(key);
                              if (this.minecraft.player != null) {
                                 this.minecraft.player.sendSystemMessage(Component.literal("§aCopied API key #" + i + " to clipboard"));
                              }
                           }

                           return true;
                        }

                        if (lx >= removeX && lx < removeX + 60 && ly >= y && ly < y + 22) {
                           config.removeApiKey(i);
                           this.keyValidStatus.remove(key);
                           config.save();
                           return true;
                        }

                        y += 22;
                        if (y > this.layoutH - 60) {
                           break;
                        }
                     }
                  } else if (this.currentPage == 3) {
                     this.apiKeyInputFocused = false;
                     int specialRows = 14;
                     int hexRowY = cy + 40 + specialRows * 22 + 4;
                     int hexInputX = cx + 220;
                     if (ly >= hexRowY && ly < hexRowY + 22 && lx >= hexInputX && lx < hexInputX + 260) {
                        if (this.cakeMaxYearFocused) {
                           this.applyCakeMaxYearInput(config);
                           this.cakeMaxYearFocused = false;
                        }

                        this.specificHexScanFocused = true;
                        this.specificHexScanInput.setLength(0);
                        this.specificHexScanInput.append(config.specificHexList != null ? config.specificHexList : "");
                        return true;
                     }

                     if (this.handleCakeYearClicks(config, lx, ly)) {
                        return true;
                     }

                     if (this.specificHexScanFocused) {
                        this.applySpecificHexScanInput(config);
                        this.specificHexScanFocused = false;
                     }

                     if (this.cakeSpecificYearsFocused) {
                        this.applyCakeSpecificYearsInput(config);
                        this.cakeSpecificYearsFocused = false;
                     }

                     if (this.cakeMaxYearFocused) {
                        this.applyCakeMaxYearInput(config);
                        this.cakeMaxYearFocused = false;
                     }
                  } else if (this.currentPage == 4 && config != null) {
                     this.apiKeyInputFocused = false;
                     int y = cy + 40;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.scanCosmeticSkinsEnabled = !config.scanCosmeticSkinsEnabled;
                        config.save();
                        return true;
                     }

                     int skinInputY = cy + 40 + 22 + 4;
                     int inputX = cx + 220;
                     if (ly >= skinInputY && ly < skinInputY + 22 && lx >= inputX && lx < inputX + 120) {
                        this.minSkinValueFocused = true;
                        this.minSkinValueInput.setLength(0);
                        this.minSkinValueInput.append(String.valueOf((int)config.minCosmeticSkinValueMillions));
                        return true;
                     }

                     int scanModeY = cy + 40 + 22 + 4 + 22 + 22 + 2 + 22 + 4;
                     int segW = Math.max(1, (cw - 4) / 3);
                     if (ly >= scanModeY && ly < scanModeY + 22) {
                        for (int i = 0; i < SKIN_SCAN_MODES.length; i++) {
                           int sx = cx + i * (segW + 2);
                           int sw = i == SKIN_SCAN_MODES.length - 1 ? cx + cw - sx : segW;
                           if (lx >= sx && lx < sx + sw) {
                              if (this.minSkinValueFocused) {
                                 this.applyMinSkinValueInput(config);
                                 this.minSkinValueFocused = false;
                              }

                              config.cosmeticSkinScanMode = SKIN_SCAN_MODES[i];
                              config.save();
                              return true;
                           }
                        }
                     }
                  } else if (this.currentPage == 5) {
                     this.apiKeyInputFocused = false;
                     if (this.handleShareClicks(config, lx, ly)) {
                        return true;
                     }

                     int row0 = cy + 40 + 22 + 4;
                     int row1 = cy + 40 + 26 + 24;
                     int inputX = cx + 220;
                     if (ly >= row0 && ly < row0 + 22 && lx >= inputX && lx < inputX + 120) {
                        this.minLevelFocused = true;
                        this.maxLevelFocused = false;
                        if (this.minLevelInput.length() == 0) {
                           this.minLevelInput.append((int)config.minSkyblockLevel);
                        }

                        return true;
                     }

                     if (ly >= row1 && ly < row1 + 22 && lx >= inputX && lx < inputX + 120) {
                        this.maxLevelFocused = true;
                        this.minLevelFocused = false;
                        if (this.maxLevelInput.length() == 0) {
                           this.maxLevelInput.append((int)config.skyblockLevelCap);
                        }

                        return true;
                     }

                     if (this.minLevelFocused || this.maxLevelFocused) {
                        this.applyLevelInputs(config);
                        this.minLevelFocused = false;
                        this.maxLevelFocused = false;
                     }
                  } else if (this.currentPage == 6 && config != null) {
                     this.apiKeyInputFocused = false;
                     int y = cy + 40 + 14;
                     int anchorW = 88;
                     int anchorGap = 8;
                     int anchorPer = wrapPerRow(anchorW, anchorGap, cw);
                     for (int i = 0; i < HUD_ANCHORS.length; i++) {
                        int col = i % anchorPer;
                        int row = i / anchorPer;
                        int x = cx + col * (anchorW + anchorGap);
                        int ty = y + row * 26;
                        if (lx >= x && lx < x + anchorW && ly >= ty && ly < ty + 22) {
                           config.hudAnchor = HUD_ANCHORS[i];
                           config.save();
                           return true;
                        }
                     }

                     y += ((HUD_ANCHORS.length + anchorPer - 1) / anchorPer) * 26 + 12 + 14;
                     int themeW = 90;
                     int themeGap = 8;
                     int themePer = wrapPerRow(themeW, themeGap, cw);
                     for (int i = 0; i < HUD_THEMES.length; i++) {
                        int col = i % themePer;
                        int row = i / themePer;
                        int x = cx + col * (themeW + themeGap);
                        int ty = y + row * 26;
                        if (lx >= x && lx < x + themeW && ly >= ty && ly < ty + 22) {
                           config.hudBorderTheme = HUD_THEMES[i];
                           config.save();
                           return true;
                        }
                     }

                     y += ((HUD_THEMES.length + themePer - 1) / themePer) * 26 + 12;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.hudFairyAccentEnabled = !config.hudFairyAccentEnabled;
                        config.save();
                        return true;
                     }

                     y += 28 + 14;
                     if (ly >= y && ly < y + 22 && lx >= cx && lx < cx + 88) {
                        this.hudColorFocused = true;
                        this.hudColorInput.setLength(0);
                        this.hudColorInput.append(String.format("#%06X", config.hudColorRgb & 16777215));
                        return true;
                     }

                     y += 28;
                     int swatchW = 18;
                     int swatchGap = 6;
                     int swatchPer = wrapPerRow(swatchW, swatchGap, cw);
                     for (int i = 0; i < HIGHLIGHT_SWATCHES.length; i++) {
                        int col = i % swatchPer;
                        int row = i / swatchPer;
                        int sx = cx + col * (swatchW + swatchGap);
                        int sy = y + row * 24;
                        if (lx >= sx && lx < sx + swatchW && ly >= sy && ly < sy + swatchW) {
                           config.hudColorRgb = HIGHLIGHT_SWATCHES[i] & 16777215;
                           config.hudBorderTheme = "CUSTOM";
                           config.save();
                           this.hudColorFocused = false;
                           return true;
                        }
                     }

                     if (this.hudColorFocused) {
                        this.applyHudColorInput(config);
                        this.hudColorFocused = false;
                     }
                  } else if (this.currentPage == 7 && config != null) {
                     this.apiKeyInputFocused = false;
                     int opacityY = cy + 40;
                     if (lx >= cx && lx < cx + cw && ly >= opacityY && ly < opacityY + 22) {
                        config.guiDarkMode = !config.guiDarkMode;
                        config.save();
                        return true;
                     }

                     opacityY += 28;
                     int opacityX = this.getAppearanceOpacityInputX(cx);
                     boolean clickInOpacityInput = lx >= opacityX && lx < opacityX + 72 && ly >= opacityY && ly < opacityY + 22;
                     int clickedHexInputRow = -1;

                     for (int row = 0; row < HIGHLIGHT_COLOR_LABELS.length; row++) {
                        int rowY = this.getAppearanceHexInputY(cy, row);
                        int inputX = this.getAppearanceHexInputX(cx);
                        if (lx >= inputX && lx < inputX + 88 && ly >= rowY && ly < rowY + 18) {
                           clickedHexInputRow = row;
                           break;
                        }
                     }

                     if (this.isAppearanceInputFocused() && !clickInOpacityInput && clickedHexInputRow < 0) {
                        this.applyAndClearAppearanceInputs(config);
                     }

                     if (clickInOpacityInput) {
                        if (this.highlightHexFocusedRow >= 0) {
                           this.applyHighlightHexInput(config);
                        }

                        this.highlightHexFocusedRow = -1;
                        this.guiOpacityFocused = true;
                        this.guiOpacityInput.setLength(0);
                        this.guiOpacityInput.append(config.guiAlpha);
                        return true;
                     }

                     if (clickedHexInputRow >= 0) {
                        if (this.guiOpacityFocused) {
                           this.applyGuiOpacityInput(config);
                        }

                        if (this.highlightHexFocusedRow >= 0 && this.highlightHexFocusedRow != clickedHexInputRow) {
                           this.applyHighlightHexInput(config);
                        }

                        this.guiOpacityFocused = false;
                        this.highlightHexFocusedRow = clickedHexInputRow;
                        this.highlightHexInput.setLength(0);
                        this.highlightHexInput.append(String.format("#%06X", this.getHighlightColorByRow(config, clickedHexInputRow) & 16777215));
                        return true;
                     }

                     int y = opacityY + 22 + 6 + 16;
                     int themeButtonW = 100;
                     int themeGap = 10;
                     int themePerRow = this.appearanceThemePerRow(cw);

                     for (int i = 0; i < GUI_ACCENT_THEME_KEYS.length; i++) {
                        int row = i / themePerRow;
                        int col = i % themePerRow;
                        int x = cx + col * (themeButtonW + themeGap);
                        int ty = y + row * 28;
                        if (lx >= x && lx < x + themeButtonW && ly >= ty && ly < ty + 24) {
                           this.setGuiAccentTheme(config, GUI_ACCENT_THEME_KEYS[i], GUI_ACCENT_THEME_COLORS[i]);
                           config.save();
                           return true;
                        }
                     }

                     int themeRows = (GUI_ACCENT_THEME_KEYS.length + themePerRow - 1) / themePerRow;
                     y += themeRows * 28 + 12;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.useCategoryHighlightColors = !config.useCategoryHighlightColors;
                        config.save();
                        return true;
                     }

                     y += 28;
                     if (this.isNumberInputHit(lx, ly, cx, y, cw)) {
                        this.focusNumberInput(1, config.slotHighlightAlpha, config);
                        return true;
                     }

                     y += 30;

                     for (int row = 0; row < HIGHLIGHT_COLOR_LABELS.length; row++) {
                        if (this.tryApplyHighlightColorClick(lx, ly, cx, cw, y, row, config)) {
                           return true;
                        }

                        y += this.highlightRowHeight(cw);
                     }
                  } else if (this.currentPage == 8 && config != null) {
                     this.apiKeyInputFocused = false;
                     int y = cy + 40;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanningEnabled = !config.seymourScanningEnabled;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanTopHat = !config.seymourScanTopHat;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanJacket = !config.seymourScanJacket;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanTrousers = !config.seymourScanTrousers;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanShoes = !config.seymourScanShoes;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanT1 = !config.seymourScanT1;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanT2 = !config.seymourScanT2;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourExactHexOnly = !config.seymourExactHexOnly;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (this.isNumberInputHit(lx, ly, cx, y, cw)) {
                        if (this.seymourArmorFilterFocused) {
                           this.applyAndClearSeymourInputs(config);
                        }

                        this.focusNumberInput(2, config.seymourTargetStage, config);
                        return true;
                     }

                     y += 22;
                     if (this.isNumberInputHit(lx, ly, cx, y, cw)) {
                        if (this.seymourArmorFilterFocused) {
                           this.applyAndClearSeymourInputs(config);
                        }

                        this.focusNumberInput(3, config.seymourStageTolerance, config);
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.highlightSeymourT1Enabled = !config.highlightSeymourT1Enabled;
                        config.save();
                        return true;
                     }

                     y += 28;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanTargetColors = !config.seymourScanTargetColors;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourScanFadeDyes = !config.seymourScanFadeDyes;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourShowHighFades = !config.seymourShowHighFades;
                        config.save();
                        return true;
                     }

                     y += 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        config.seymourPieceSpecificEnabled = !config.seymourPieceSpecificEnabled;
                        config.save();
                        return true;
                     }

                     y += 32;
                     int inputX = cx + 210;
                     if (ly >= y && ly < y + 22 && lx >= inputX && lx < inputX + 260) {
                        this.seymourArmorFilterFocused = true;
                        this.seymourArmorFilterInput.setLength(0);
                        this.seymourArmorFilterInput.append(config.seymourArmorNameFilters != null ? config.seymourArmorNameFilters : "all");
                        return true;
                     }

                     if (this.seymourArmorFilterFocused) {
                        this.applyAndClearSeymourInputs(config);
                     }
                  } else if (this.currentPage == 9 && config != null) {
                     this.apiKeyInputFocused = false;
                     this.webhookUrlFocused = false;
                     this.notifierNameFocused = false;
                     if (this.handleAutomaticClick(lx, ly, cx, cy, cw, config)) {
                        return true;
                     }
                  } else if (this.currentPage == 10 && config != null) {
                     this.apiKeyInputFocused = false;
                     this.notifierNameFocused = false;
                     if (this.handleWebhookClick(lx, ly, cx, cy, cw, config)) {
                        return true;
                     }
                  } else if (this.currentPage == 11 && config != null) {
                     this.apiKeyInputFocused = false;
                     this.webhookUrlFocused = false;
                     if (this.handleNotifierClick(lx, ly, cx, cy, cw, config)) {
                        return true;
                     }
                  } else if (this.currentPage == 12 && config != null) {
                     this.apiKeyInputFocused = false;
                     this.webhookUrlFocused = false;
                     this.notifierNameFocused = false;
                     if (this.handleHitsClick(lx, ly, cx, cy, cw, config)) {
                        return true;
                     }
                  } else if (this.currentPage == 1) {
                     this.apiKeyInputFocused = false;
                     int y0 = cy + 40;
                     int ignInputX = cx + 160;
                     if (lx >= ignInputX && lx < ignInputX + 260 && ly >= y0 && ly < y0 + 22) {
                        this.lookupIgnFocused = true;
                        return true;
                     }

                     int y1 = y0 + 22 + 8;
                     if (lx >= cx && lx < cx + 120 && ly >= y1 && ly < y1 + 22) {
                        this.lookupIgnFocused = false;
                        this.triggerLookupFromTab();
                        return true;
                     }

                     if (lx >= cx + 126 && lx < cx + 266 && ly >= y1 && ly < y1 + 22) {
                        this.lookupIgnFocused = false;
                        if (this.minecraft != null && this.lookupSelectedPlayer != null) {
                           this.minecraft.setScreen(new LookupProfileScreen(this.minecraft.screen, this.lookupSelectedPlayer));
                        }

                        return true;
                     }

                     this.lookupIgnFocused = false;
                  } else {
                     this.apiKeyInputFocused = false;
                  }

                  return super.mouseClicked(click, doubled);
               }
            }
         } else {
            this.apiKeyInputFocused = false;
            this.cakeSpecificYearsFocused = false;
            if (this.cakeMaxYearFocused) {
               this.applyCakeMaxYearInput(this.getConfig());
               this.cakeMaxYearFocused = false;
            }

            if (this.specificHexScanFocused) {
               this.applySpecificHexScanInput(this.getConfig());
               this.specificHexScanFocused = false;
            }

            if (this.minLevelFocused || this.maxLevelFocused) {
               this.applyLevelInputs(this.getConfig());
               this.minLevelFocused = false;
               this.maxLevelFocused = false;
            }

            if (this.minSkinValueFocused) {
               this.applyMinSkinValueInput(this.getConfig());
               this.minSkinValueFocused = false;
            }

            this.applyAndClearAutoBuyInputs(this.getConfig());
            this.applyAndClearSeymourInputs(this.getConfig());
            this.applyAndClearAuctionInputs(this.getConfig());
            this.applyAndClearAppearanceInputs(this.getConfig());
            this.applyAndClearNumberInput(this.getConfig());
            return false;
         }
      }

      private void applyDragFromMouse(double mouseX, double mouseY) {
         int screenW = this.minecraft != null && this.minecraft.getWindow() != null ? this.minecraft.getWindow().getGuiScaledWidth() : this.width;
         int screenH = this.minecraft != null && this.minecraft.getWindow() != null ? this.minecraft.getWindow().getGuiScaledHeight() : this.height;
         double targetX = this.dragStartPanelX + (mouseX - this.dragStartX);
         double targetY = this.dragStartPanelY + (mouseY - this.dragStartY);
         targetX = Math.max(0, Math.min(screenW - this.panelW, targetX));
         targetY = Math.max(0, Math.min(screenH - this.panelH, targetY));
         if (this.visualPanelX == 0.0 && this.visualPanelY == 0.0 && this.panelX == 0 && this.panelY == 0) {
            this.visualPanelX = this.panelX;
            this.visualPanelY = this.panelY;
         }
         this.visualPanelX += (targetX - this.visualPanelX) * 0.78;
         this.visualPanelY += (targetY - this.visualPanelY) * 0.78;
         this.panelX = (int)Math.round(this.visualPanelX);
         this.panelY = (int)Math.round(this.visualPanelY);
      }

      private void applyResizeFromMouse(double mouseX, double mouseY) {
         int screenW = this.minecraft != null && this.minecraft.getWindow() != null ? this.minecraft.getWindow().getGuiScaledWidth() : this.width;
         int screenH = this.minecraft != null && this.minecraft.getWindow() != null ? this.minecraft.getWindow().getGuiScaledHeight() : this.height;
         int maxW = Math.max(560, screenW - this.panelX - 20);
         int maxH = Math.max(420, screenH - this.panelY - 20);
         int nw = (int)(this.resizeStartW + (mouseX - this.resizeStartX));
         int nh = (int)(this.resizeStartH + (mouseY - this.resizeStartY));
         this.panelW = Math.max(560, Math.min(maxW, nw));
         this.panelH = Math.max(420, Math.min(maxH, nh));
         this.layoutW = this.panelW;
         this.layoutH = this.panelH;
      }

      public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
         if (this.currentPage == 99) {
            int left = this.panelX + this.contentX();
            int right = left + this.contentWidth();
            int top = this.panelY + this.contentY() + 40;
            int bottom = this.panelY + this.getAhContentBottomY();
            if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
               this.updateAhScrollBounds(this.contentWidth());
               if (this.ahPageMaxScroll > 0) {
                  int delta = (int)(-verticalAmount * 18.0);
                  this.ahPageScroll = Math.max(-this.ahPageMaxScroll, Math.min(0, this.ahPageScroll + delta));
               }

               return true;
            }
         }

         if (this.currentPage == 12) {
            int delta = (int)(-verticalAmount * 18.0);
            this.hitsScroll = Math.max(0, this.hitsScroll + delta);
            return true;
         }

         if (this.currentPage == 1 && !this.lookupPreviewLines.isEmpty()) {
            int resultsY0 = this.contentY() + 160;
            int resultsH = this.layoutH - resultsY0 - 50;
            int listTop = this.panelY + resultsY0;
            int listBottom = listTop + resultsH;
            if (mouseY >= listTop
               && mouseY < listBottom
               && mouseX >= this.panelX + this.contentX()
               && mouseX < this.panelX + this.contentX() + this.contentWidth()) {
               int lineH = 11;
               int totalContentH = this.lookupPreviewLines.size() * lineH;
               int maxScroll = Math.max(0, totalContentH - resultsH);
               int delta = (int)(-verticalAmount * lineH * 2.0);
               this.lookupPreviewScroll = Math.max(0, Math.min(maxScroll, this.lookupPreviewScroll + delta));
               return true;
            }
         }

         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }

      public boolean mouseDragged(MouseButtonEvent click, double mouseX, double mouseY) {
         double px = click.x();
         double py = click.y();
         if (this.macroDragging) {
            this.macroHoverInsert = this.sequenceInsertIndex((int)px - this.panelX, (int)py - this.panelY);
            return true;
         }
         if (this.dragging) {
            this.applyDragFromMouse(px, py);
            return true;
         } else if (this.resizing) {
            this.applyResizeFromMouse(px, py);
            return true;
         } else {
            return super.mouseDragged(click, mouseX, mouseY);
         }
      }

      public boolean mouseReleased(MouseButtonEvent click) {
         if (click.button() == 0 && this.macroDragging) {
            this.finishMacroDrag((int)click.x() - this.panelX, (int)click.y() - this.panelY, this.getConfig());
            return true;
         }
         if (click.button() == 0) {
            this.dragging = false;
            this.resizing = false;
            this.visualPanelX = this.panelX;
            this.visualPanelY = this.panelY;
         }

         if (click.button() == 0) {
            ScannerConfig config = this.getConfig();
            int mx = (int)click.x();
            int my = (int)click.y();
            if (mx >= this.panelX && mx < this.panelX + this.panelW && my >= this.panelY && my < this.panelY + this.panelH) {
               int lx = mx - this.panelX;
               int ly = my - this.panelY;
               int dtW = this.font.width("Discord");
               int dScaledW = dtW;
               int dScaledH = 9;
               int discordX = this.panelW - 14 - dScaledW;
               int discordY0 = (TITLE_BAR_HEIGHT - dScaledH) / 2;
               if (ly < TITLE_BAR_HEIGHT && lx >= discordX && lx < discordX + dScaledW && ly >= discordY0 && ly < discordY0 + dScaledH) {
                  if (this.minecraft != null) {
                     this.minecraft.keyboardHandler.setClipboard("https://discord.gg/PvEYc4Kwy");
                     if (this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(Component.literal("§aDiscord link copied! §7https://discord.gg/PvEYc4Kwy"));
                     }
                  }

                  return true;
               }

               int cx = this.contentX();
               int cw = this.contentWidth();
               int cy = this.contentY();
               if (this.currentPage == 2) {
                  String[] labels = new String[]{"Weapons", "Armor", "Accessories", "Tools", "Pets", "Dungeon Items", "Slayer Items"};

                  for (int i = 0; i < labels.length; i++) {
                     int y = cy + 40 + i * 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        switch (i) {
                           case 0:
                              config.scanWeapons = !config.scanWeapons;
                              config.save();
                              break;
                           case 1:
                              config.scanArmor = !config.scanArmor;
                              config.save();
                              break;
                           case 2:
                              config.scanAccessories = !config.scanAccessories;
                              config.save();
                              break;
                           case 3:
                              config.scanTools = !config.scanTools;
                              config.save();
                              break;
                           case 4:
                              config.scanPets = !config.scanPets;
                              config.save();
                              break;
                           case 5:
                              config.scanDungeonItems = !config.scanDungeonItems;
                              config.save();
                              break;
                           case 6:
                              config.scanSlayerItems = !config.scanSlayerItems;
                              config.save();
                        }

                        return true;
                     }
                  }
               } else if (this.currentPage == 3) {
                  for (int i = 0; i < 14; i++) {
                     int y = cy + 40 + i * 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        switch (i) {
                           case 0:
                              config.crystalScanningEnabled = !config.crystalScanningEnabled;
                              config.save();
                              break;
                           case 1:
                              config.bleachedScanningEnabled = !config.bleachedScanningEnabled;
                              config.save();
                              break;
                           case 2:
                              config.ogFairyScanningEnabled = !config.ogFairyScanningEnabled;
                              config.save();
                              break;
                           case 3:
                              config.fairyScanningEnabled = !config.fairyScanningEnabled;
                              config.save();
                              break;
                           case 4:
                              config.exoticScanningEnabled = !config.exoticScanningEnabled;
                              config.save();
                              break;
                           case 5:
                              config.newYearCakeEnabled = !config.newYearCakeEnabled;
                              config.save();
                              break;
                           case 6:
                              config.legacyReforgeEnabled = !config.legacyReforgeEnabled;
                              config.save();
                              break;
                           case 7:
                              config.ghostReforgeEnabled = !config.ghostReforgeEnabled;
                              config.save();
                              break;
                           case 8:
                              config.valuableItemsEnabled = !config.valuableItemsEnabled;
                              config.save();
                              break;
                           case 9:
                              config.scanAnniversaryHats = !config.scanAnniversaryHats;
                              config.save();
                              break;
                           case 10:
                              config.renderItemTracers = !config.renderItemTracers;
                              config.save();
                              break;
                           case 11:
                              config.showPlayerOverlay = !config.showPlayerOverlay;
                              config.save();
                              break;
                           case 12:
                              config.scanCosmeticSkinsEnabled = !config.scanCosmeticSkinsEnabled;
                              config.save();
                              break;
                           case 13:
                              config.specificHexScanningEnabled = !config.specificHexScanningEnabled;
                              config.save();
                        }

                        return true;
                     }
                  }
               } else if (this.currentPage == 4 && config != null) {
                  int y = cy + 40;
                  if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                     config.scanCosmeticSkinsEnabled = !config.scanCosmeticSkinsEnabled;
                     config.save();
                     return true;
                  }

                  int skinInputY = cy + 40 + 22 + 4;
                  int inputX = cx + 220;
                  if (ly >= skinInputY && ly < skinInputY + 22 && lx >= inputX && lx < inputX + 120) {
                     this.minSkinValueFocused = true;
                     this.minSkinValueInput.setLength(0);
                     this.minSkinValueInput.append(String.valueOf((int)config.minCosmeticSkinValueMillions));
                     return true;
                  }

                  int scanModeY = cy + 40 + 22 + 4 + 22 + 22 + 2 + 22 + 4;
                  int segW = Math.max(1, (cw - 4) / 3);
                  if (ly >= scanModeY && ly < scanModeY + 22) {
                     for (int i = 0; i < SKIN_SCAN_MODES.length; i++) {
                        int sx = cx + i * (segW + 2);
                        int sw = i == SKIN_SCAN_MODES.length - 1 ? cx + cw - sx : segW;
                        if (lx >= sx && lx < sx + sw) {
                           if (this.minSkinValueFocused) {
                              this.applyMinSkinValueInput(config);
                              this.minSkinValueFocused = false;
                           }

                           config.cosmeticSkinScanMode = SKIN_SCAN_MODES[i];
                           config.save();
                           return true;
                        }
                     }
                  }
               } else if (this.currentPage == 5) {
                  int firstToggleY = cy + 40 + 98;

                  for (int i = 0; i < 7; i++) {
                     int y = firstToggleY + i * 22;
                     if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
                        switch (i) {
                           case 0:
                              config.scanLowLevelPlayers = !config.scanLowLevelPlayers;
                              config.save();
                              break;
                           case 1:
                              config.skipApiErrorPlayers = !config.skipApiErrorPlayers;
                              config.save();
                              break;
                           case 2:
                              config.scanSoulboundItems = !config.scanSoulboundItems;
                              config.save();
                              break;
                           case 3:
                              config.scanCoopSoulboundItems = !config.scanCoopSoulboundItems;
                              config.save();
                              break;
                           case 4:
                              config.scanIronmanProfiles = !config.scanIronmanProfiles;
                              config.save();
                              break;
                           case 5:
                              config.scanStrandedProfiles = !config.scanStrandedProfiles;
                              config.save();
                              break;
                           case 6:
                              config.requireCustomColor = !config.requireCustomColor;
                              config.save();
                        }

                        return true;
                     }
                  }
               }
            }
         }

         return super.mouseReleased(click);
      }

      public boolean keyPressed(KeyEvent input) {
         if (this.hudColorFocused) {
            if (input.isEscape() || input.key() == 257 || input.key() == 335) {
               this.applyHudColorInput(this.getConfig());
               this.hudColorFocused = false;
               return true;
            }

            if (input.key() == 259 && this.hudColorInput.length() > 0) {
               this.hudColorInput.setLength(this.hudColorInput.length() - 1);
               return true;
            }

            if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
               String pasted = this.minecraft.keyboardHandler.getClipboard();
               if (pasted != null) {
                  for (int i = 0; i < pasted.length() && this.hudColorInput.length() < 7; i++) {
                     char c = pasted.charAt(i);
                     if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c == '#' && this.hudColorInput.length() == 0) {
                        this.hudColorInput.append(Character.toUpperCase(c));
                     }
                  }
               }

               return true;
            }
         }

         if (this.isAppearanceInputFocused()) {
            if (input.isEscape() || input.key() == 257 || input.key() == 335) {
               this.applyAndClearAppearanceInputs(this.getConfig());
               return true;
            }

            if (input.key() == 259) {
               StringBuilder target = this.guiOpacityFocused ? this.guiOpacityInput : this.highlightHexInput;
               if (target.length() > 0) {
                  target.setLength(target.length() - 1);
               }

               return true;
            } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
               String pasted = this.minecraft.keyboardHandler.getClipboard();
               if (pasted != null && !pasted.isEmpty()) {
                  if (this.guiOpacityFocused) {
                     for (int i = 0; i < pasted.length() && this.guiOpacityInput.length() < 3; i++) {
                        char c = pasted.charAt(i);
                        if (c >= '0' && c <= '9') {
                           this.guiOpacityInput.append(c);
                        }
                     }
                  } else if (this.highlightHexFocusedRow >= 0) {
                     for (int i = 0; i < pasted.length() && this.highlightHexInput.length() < 7; i++) {
                        char c = pasted.charAt(i);
                        if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c == '#' && this.highlightHexInput.length() == 0) {
                           this.highlightHexInput.append(Character.toUpperCase(c));
                        }
                     }
                  }
               }

               return true;
            } else {
               return true;
            }
         } else if (this.numberInputField >= 0) {
            if (input.isEscape()) {
               this.applyAndClearNumberInput(this.getConfig());
               return true;
            }

            if (input.key() == 257 || input.key() == 335) {
               this.applyAndClearNumberInput(this.getConfig());
               return true;
            }

            if (input.key() == 259) {
               if (this.numberInput.length() > 0) {
                  this.numberInput.setLength(this.numberInput.length() - 1);
               }

               return true;
            } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
               String pasted = this.minecraft.keyboardHandler.getClipboard();
               if (pasted != null && !pasted.isEmpty()) {
                  for (int i = 0; i < pasted.length() && this.numberInput.length() < 8; i++) {
                     char c = pasted.charAt(i);
                     if (c >= '0' && c <= '9') {
                        this.numberInput.append(c);
                     }
                  }
               }

               return true;
            } else {
               return true;
            }
         } else if (!this.minLevelFocused && !this.maxLevelFocused) {
            if (this.specificHexScanFocused) {
               if (input.isEscape()) {
                  this.applySpecificHexScanInput(this.getConfig());
                  this.specificHexScanFocused = false;
                  return true;
               }

               if (input.key() == 257 || input.key() == 335) {
                  this.applySpecificHexScanInput(this.getConfig());
                  this.specificHexScanFocused = false;
                  return true;
               }

               if (input.key() == 259) {
                  if (this.specificHexScanInput.length() > 0) {
                     this.specificHexScanInput.setLength(this.specificHexScanInput.length() - 1);
                  }

                  return true;
               } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                  String pasted = this.minecraft.keyboardHandler.getClipboard();
                  if (pasted != null && !pasted.isEmpty()) {
                     for (int i = 0; i < pasted.length() && this.specificHexScanInput.length() < 240; i++) {
                        char c = pasted.charAt(i);
                        if (c >= ' ' && c < 127) {
                           this.specificHexScanInput.append(c);
                        }
                     }
                  }

                  return true;
               } else {
                  return true;
               }
            } else if (this.cakeMaxYearFocused) {
               if (input.isEscape()) {
                  this.applyCakeMaxYearInput(this.getConfig());
                  this.cakeMaxYearFocused = false;
                  return true;
               }

               if (input.key() == 257 || input.key() == 335) {
                  this.applyCakeMaxYearInput(this.getConfig());
                  this.cakeMaxYearFocused = false;
                  return true;
               }

               if (input.key() == 259) {
                  if (this.cakeMaxYearInput.length() > 0) {
                     this.cakeMaxYearInput.setLength(this.cakeMaxYearInput.length() - 1);
                  }

                  return true;
               } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                  String pasted = this.minecraft.keyboardHandler.getClipboard();
                  if (pasted != null && !pasted.isEmpty()) {
                     for (int i = 0; i < pasted.length() && this.cakeMaxYearInput.length() < 4; i++) {
                        char c = pasted.charAt(i);
                        if (c >= '0' && c <= '9') {
                           this.cakeMaxYearInput.append(c);
                        }
                     }
                  }

                  return true;
               } else {
                  return true;
               }
            } else if (this.cakeSpecificYearsFocused) {
               if (input.isEscape()) {
                  this.cakeSpecificYearsFocused = false;
                  this.cakeSpecificYearsInput.setLength(0);
                  return true;
               }

               if (input.key() == 257) {
                  this.applyCakeSpecificYearsInput(this.getConfig());
                  this.cakeSpecificYearsFocused = false;
                  return true;
               }

               if (input.key() == 259 && this.cakeSpecificYearsInput.length() > 0) {
                  this.cakeSpecificYearsInput.setLength(this.cakeSpecificYearsInput.length() - 1);
                  return true;
               }

               if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                  String pasted = this.minecraft.keyboardHandler.getClipboard();
                  if (pasted != null && !pasted.isEmpty()) {
                     for (int i = 0; i < pasted.length() && this.cakeSpecificYearsInput.length() < 3; i++) {
                        char c = pasted.charAt(i);
                        if (c >= '0' && c <= '9') {
                           this.cakeSpecificYearsInput.append(c);
                        }
                     }
                  }

                  return true;
               } else {
                  return true;
               }
            } else if (this.shareCodeFocused) {
               if (input.isEscape()) {
                  this.shareCodeFocused = false;
                  return true;
               }

               if (input.key() == 257) {
                  ScannerConfig cfg = this.getConfig();
                  String raw = this.shareCodeInput.length() > 0
                     ? this.shareCodeInput.toString()
                     : this.minecraft != null && this.minecraft.keyboardHandler != null ? this.minecraft.keyboardHandler.getClipboard() : "";
                  String error = ConfigShareCodec.importCode(cfg, raw);
                  this.shareStatus = error == null ? "Imported share code" : error;
                  this.shareCodeFocused = false;
                  return true;
               }

               if (input.key() == 259 && this.shareCodeInput.length() > 0) {
                  this.shareCodeInput.setLength(this.shareCodeInput.length() - 1);
                  return true;
               }

               if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                  String pasted = this.minecraft.keyboardHandler.getClipboard();
                  if (pasted != null && !pasted.isEmpty()) {
                     this.shareCodeInput.setLength(0);
                     for (int i = 0; i < pasted.length() && this.shareCodeInput.length() < 4000; i++) {
                        char c = pasted.charAt(i);
                        if (c >= ' ' && c < 127) {
                           this.shareCodeInput.append(c);
                        }
                     }
                  }

                  return true;
               } else {
                  return true;
               }
            } else {
               if (this.apiKeyInputFocused) {
                  if (input.isEscape()) {
                     this.apiKeyInputFocused = false;
                     return true;
                  }

                  if (input.key() == 259 && this.apiKeyInput.length() > 0) {
                     this.apiKeyInput.setLength(this.apiKeyInput.length() - 1);
                     return true;
                  }

                  if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                     String pasted = this.minecraft.keyboardHandler.getClipboard();
                     if (pasted != null && !pasted.isEmpty()) {
                        for (int i = 0; i < pasted.length() && this.apiKeyInput.length() < 100; i++) {
                           char c = pasted.charAt(i);
                           if (c >= ' ' && c < 127) {
                              this.apiKeyInput.append(c);
                           }
                        }
                     }

                     return true;
                  }
               }

               if (this.webhookUrlFocused) {
                  if (input.isEscape()) {
                     this.applyWebhookUrl(this.getConfig());
                     this.webhookUrlFocused = false;
                     return true;
                  }

                  if (input.key() == 259 && this.webhookUrlInput.length() > 0) {
                     this.webhookUrlInput.setLength(this.webhookUrlInput.length() - 1);
                     return true;
                  }

                  if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                     String pasted = this.minecraft.keyboardHandler.getClipboard();
                     if (pasted != null) {
                        for (int i = 0; i < pasted.length() && this.webhookUrlInput.length() < 256; i++) {
                           this.webhookUrlInput.append(pasted.charAt(i));
                        }
                     }

                     return true;
                  }
               }

               if (this.notifierNameFocused) {
                  if (input.isEscape() || input.key() == 257 || input.key() == 335) {
                     this.notifierNameFocused = false;
                     return true;
                  }

                  if (input.key() == 259 && this.notifierNameInput.length() > 0) {
                     this.notifierNameInput.setLength(this.notifierNameInput.length() - 1);
                     return true;
                  }
               }

               if (this.minSkinValueFocused) {
                  if (input.isEscape()) {
                     this.applyMinSkinValueInput(this.getConfig());
                     this.minSkinValueFocused = false;
                     return true;
                  }

                  if (input.key() == 257 || input.key() == 335) {
                     this.applyMinSkinValueInput(this.getConfig());
                     this.minSkinValueFocused = false;
                     return true;
                  }

                  if (input.key() == 259 && this.minSkinValueInput.length() > 0) {
                     this.minSkinValueInput.setLength(this.minSkinValueInput.length() - 1);
                     return true;
                  }

                  if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                     String pasted = this.minecraft.keyboardHandler.getClipboard();
                     if (pasted != null && !pasted.isEmpty()) {
                        for (int i = 0; i < pasted.length() && this.minSkinValueInput.length() < 20; i++) {
                           char c = pasted.charAt(i);
                           if (c >= '0' && c <= '9' || c == '.') {
                              this.minSkinValueInput.append(c);
                           }
                        }
                     }

                     return true;
                  } else {
                     return true;
                  }
               } else if (this.seymourArmorFilterFocused) {
                  if (input.isEscape()) {
                     this.applyAndClearSeymourInputs(this.getConfig());
                     return true;
                  }

                  if (input.key() == 257 || input.key() == 335) {
                     this.applyAndClearSeymourInputs(this.getConfig());
                     return true;
                  }

                  if (input.key() == 259) {
                     if (this.seymourArmorFilterInput.length() > 0) {
                        this.seymourArmorFilterInput.setLength(this.seymourArmorFilterInput.length() - 1);
                     }

                     return true;
                  } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                     String pasted = this.minecraft.keyboardHandler.getClipboard();
                     if (pasted != null && !pasted.isEmpty()) {
                        for (int i = 0; i < pasted.length() && this.seymourArmorFilterInput.length() < 120; i++) {
                           char c = pasted.charAt(i);
                           if (c >= ' ' && c < 127) {
                              this.seymourArmorFilterInput.append(c);
                           }
                        }
                     }

                     return true;
                  } else {
                     return true;
                  }
               } else if (this.manualBuyUuidFocused) {
                  if (input.isEscape()) {
                     this.manualBuyUuidFocused = false;
                     return true;
                  }

                  if (input.key() == 257 || input.key() == 335) {
                     this.triggerManualBuyNow();
                     return true;
                  }

                  if (input.key() == 259) {
                     if (this.manualBuyUuidInput.length() > 0) {
                        this.manualBuyUuidInput.setLength(this.manualBuyUuidInput.length() - 1);
                     }

                     return true;
                  } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                     String pasted = this.minecraft.keyboardHandler.getClipboard();
                     if (pasted != null && !pasted.isEmpty()) {
                        for (int i = 0; i < pasted.length() && this.manualBuyUuidInput.length() < 48; i++) {
                           char c = pasted.charAt(i);
                           boolean ok = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c == '-';
                           if (ok) {
                              this.manualBuyUuidInput.append(Character.toLowerCase(c));
                           }
                        }
                     }

                     return true;
                  } else {
                     return true;
                  }
               } else if (!this.autoBuyNameFocused && !this.autoBuyHexFocused && !this.autoBuyRulesFocused) {
                  if (!this.ahWatchTagsFocused && !this.ahSeymourHexFocused && !this.ahSkinMuteFocused && !this.ahQuickSetFocused && !this.ahQuickSkinFocused) {
                     if (!this.lookupIgnFocused) {
                        return super.keyPressed(input);
                     }

                     if (input.isEscape()) {
                        this.lookupIgnFocused = false;
                        return true;
                     }

                     if (input.key() == 257 || input.key() == 335) {
                        this.lookupIgnFocused = false;
                        this.triggerLookupFromTab();
                        return true;
                     }

                     if (input.key() == 259) {
                        if (this.lookupIgnInput.length() > 0) {
                           this.lookupIgnInput.setLength(this.lookupIgnInput.length() - 1);
                        }

                        return true;
                     } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                        String pasted = this.minecraft.keyboardHandler.getClipboard();
                        if (pasted != null && !pasted.isEmpty()) {
                           for (int i = 0; i < pasted.length() && this.lookupIgnInput.length() < 16; i++) {
                              char c = pasted.charAt(i);
                              boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-';
                              if (ok) {
                                 this.lookupIgnInput.append(c);
                              }
                           }
                        }

                        return true;
                     } else {
                        return true;
                     }
                  } else {
                     if (input.isEscape() || input.key() == 257 || input.key() == 335) {
                        this.applyAndClearAuctionInputs(this.getConfig());
                        return true;
                     }

                     if (input.key() == 259) {
                        StringBuilder target = this.ahWatchTagsFocused
                           ? this.ahWatchTagsInput
                           : (
                              this.ahSeymourHexFocused
                                 ? this.ahSeymourHexInput
                                 : (this.ahSkinMuteFocused ? this.ahSkinMuteInput : (this.ahQuickSetFocused ? this.ahQuickSetInput : this.ahQuickSkinInput))
                           );
                        if (target.length() > 0) {
                           target.setLength(target.length() - 1);
                        }

                        if (this.ahQuickSetFocused) {
                           this.refreshAhQuickSetSuggestions(this.ahQuickSetInput.toString());
                        }

                        if (this.ahQuickSkinFocused) {
                           this.refreshAhQuickSkinSuggestions(this.ahQuickSkinInput.toString());
                        }

                        return true;
                     } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                        String pasted = this.minecraft.keyboardHandler.getClipboard();
                        if (pasted != null && !pasted.isEmpty()) {
                           StringBuilder target = this.ahWatchTagsFocused
                              ? this.ahWatchTagsInput
                              : (
                                 this.ahSeymourHexFocused
                                    ? this.ahSeymourHexInput
                                    : (this.ahSkinMuteFocused ? this.ahSkinMuteInput : (this.ahQuickSetFocused ? this.ahQuickSetInput : this.ahQuickSkinInput))
                              );
                           int maxLen = this.ahWatchTagsFocused ? 500 : (this.ahSeymourHexFocused ? 200 : (this.ahSkinMuteFocused ? 300 : 120));

                           for (int i = 0; i < pasted.length() && target.length() < maxLen; i++) {
                              char c = pasted.charAt(i);
                              if (c >= ' ' && c < 127) {
                                 target.append(c);
                              }
                           }

                           if (this.ahQuickSetFocused) {
                              this.refreshAhQuickSetSuggestions(this.ahQuickSetInput.toString());
                           }

                           if (this.ahQuickSkinFocused) {
                              this.refreshAhQuickSkinSuggestions(this.ahQuickSkinInput.toString());
                           }
                        }

                        return true;
                     } else {
                        return true;
                     }
                  }
               } else {
                  if (input.isEscape() || input.key() == 257 || input.key() == 335) {
                     this.applyAndClearAutoBuyInputs(this.getConfig());
                     return true;
                  }

                  if (input.key() == 259) {
                     StringBuilder target = this.autoBuyNameFocused
                        ? this.autoBuyNameInput
                        : (this.autoBuyHexFocused ? this.autoBuyHexInput : this.autoBuyRulesInput);
                     if (target.length() > 0) {
                        target.setLength(target.length() - 1);
                     }

                     return true;
                  } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
                     String pasted = this.minecraft.keyboardHandler.getClipboard();
                     if (pasted != null && !pasted.isEmpty()) {
                        StringBuilder target = this.autoBuyNameFocused
                           ? this.autoBuyNameInput
                           : (this.autoBuyHexFocused ? this.autoBuyHexInput : this.autoBuyRulesInput);
                        int maxLen = this.autoBuyNameFocused ? 160 : (this.autoBuyHexFocused ? 220 : 260);

                        for (int i = 0; i < pasted.length() && target.length() < maxLen; i++) {
                           char c = pasted.charAt(i);
                           if (c >= ' ' && c < 127) {
                              target.append(c);
                           }
                        }
                     }

                     return true;
                  } else {
                     return true;
                  }
               }
            }
         } else {
            if (input.isEscape()) {
               this.minLevelFocused = false;
               this.maxLevelFocused = false;
               this.minLevelInput.setLength(0);
               this.maxLevelInput.setLength(0);
               return true;
            }

            if (input.key() == 257) {
               this.applyLevelInputs(this.getConfig());
               this.minLevelFocused = false;
               this.maxLevelFocused = false;
               return true;
            }

            if (input.key() == 259) {
               if (this.minLevelFocused && this.minLevelInput.length() > 0) {
                  this.minLevelInput.setLength(this.minLevelInput.length() - 1);
               }

               if (this.maxLevelFocused && this.maxLevelInput.length() > 0) {
                  this.maxLevelInput.setLength(this.maxLevelInput.length() - 1);
               }

               return true;
            } else if (input.key() == 86 && (input.modifiers() & 2) != 0 && this.minecraft != null) {
               String pasted = this.minecraft.keyboardHandler.getClipboard();
               if (pasted != null && !pasted.isEmpty()) {
                  StringBuilder target = this.minLevelFocused ? this.minLevelInput : this.maxLevelInput;

                  for (int i = 0; i < pasted.length() && target.length() < 8; i++) {
                     char c = pasted.charAt(i);
                     if (c >= '0' && c <= '9') {
                        target.append(c);
                     }
                  }
               }

               return true;
            } else {
               return true;
            }
         }
      }

      public boolean charTyped(CharacterEvent input) {
         if (this.numberInputField >= 0 && input.isAllowedChatCharacter() && this.numberInput.length() < 8) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= '0' && ch.charAt(0) <= '9') {
               this.numberInput.append(ch.charAt(0));
               return true;
            }
         }

         if (this.hudColorFocused && input.isAllowedChatCharacter() && this.hudColorInput.length() < 7) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               boolean hex = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
               boolean hash = c == '#' && this.hudColorInput.length() == 0;
               if (hex || hash) {
                  this.hudColorInput.append(Character.toUpperCase(c));
                  return true;
               }
            }
         }

         if (this.guiOpacityFocused && input.isAllowedChatCharacter() && this.guiOpacityInput.length() < 3) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= '0' && ch.charAt(0) <= '9') {
               this.guiOpacityInput.append(ch);
               return true;
            }
         }

         if (this.highlightHexFocusedRow >= 0 && input.isAllowedChatCharacter() && this.highlightHexInput.length() < 7) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               boolean hex = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
               boolean hash = c == '#' && this.highlightHexInput.length() == 0;
               if (hex || hash) {
                  this.highlightHexInput.append(Character.toUpperCase(c));
                  return true;
               }
            }
         }

         if (this.minLevelFocused && input.isAllowedChatCharacter() && this.minLevelInput.length() < 8) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= '0' && ch.charAt(0) <= '9') {
               this.minLevelInput.append(ch);
               return true;
            }
         }

         if (this.maxLevelFocused && input.isAllowedChatCharacter() && this.maxLevelInput.length() < 8) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= '0' && ch.charAt(0) <= '9') {
               this.maxLevelInput.append(ch);
               return true;
            }
         }

         if (this.cakeMaxYearFocused && input.isAllowedChatCharacter() && this.cakeMaxYearInput.length() < 4) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= '0' && ch.charAt(0) <= '9') {
               this.cakeMaxYearInput.append(ch);
               return true;
            }
         }

         if (this.specificHexScanFocused && input.isAllowedChatCharacter() && this.specificHexScanInput.length() < 240) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               boolean ok = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c == '#' || c == ',' || c == ';' || c == ' ' || c == '\t';
               if (ok) {
                  this.specificHexScanInput.append(Character.toUpperCase(c));
                  return true;
               }
            }
         }

         if (this.cakeSpecificYearsFocused && input.isAllowedChatCharacter() && this.cakeSpecificYearsInput.length() < 3) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= '0' && ch.charAt(0) <= '9') {
               this.cakeSpecificYearsInput.append(ch);
               return true;
            }
         }

         if (this.shareCodeFocused && input.isAllowedChatCharacter() && this.shareCodeInput.length() < 4000) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               if (c >= ' ' && c < 127) {
                  this.shareCodeInput.append(c);
                  return true;
               }
            }
         }

         if (this.apiKeyInputFocused && input.isAllowedChatCharacter() && this.apiKeyInput.length() < 100) {
            this.apiKeyInput.append(input.codepointAsString());
            return true;
         }

         if (this.webhookUrlFocused && input.isAllowedChatCharacter() && this.webhookUrlInput.length() < 256) {
            this.webhookUrlInput.append(input.codepointAsString());
            return true;
         }

         if (this.notifierNameFocused && input.isAllowedChatCharacter() && this.notifierNameInput.length() < 16) {
            this.notifierNameInput.append(input.codepointAsString());
            return true;
         }

         if (this.minSkinValueFocused && input.isAllowedChatCharacter() && this.minSkinValueInput.length() < 20) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               if (c >= '0' && c <= '9' || c == '.') {
                  this.minSkinValueInput.append(ch);
                  return true;
               }
            }
         }

         if (this.seymourArmorFilterFocused && input.isAllowedChatCharacter() && this.seymourArmorFilterInput.length() < 120) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               if (c >= ' ' && c < 127) {
                  this.seymourArmorFilterInput.append(ch);
                  return true;
               }
            }
         }

         if (this.ahWatchTagsFocused && input.isAllowedChatCharacter() && this.ahWatchTagsInput.length() < 500) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= ' ' && ch.charAt(0) < 127) {
               this.ahWatchTagsInput.append(ch);
               return true;
            }
         }

         if (this.ahSeymourHexFocused && input.isAllowedChatCharacter() && this.ahSeymourHexInput.length() < 200) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= ' ' && ch.charAt(0) < 127) {
               this.ahSeymourHexInput.append(ch);
               return true;
            }
         }

         if (this.ahSkinMuteFocused && input.isAllowedChatCharacter() && this.ahSkinMuteInput.length() < 300) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= ' ' && ch.charAt(0) < 127) {
               this.ahSkinMuteInput.append(ch);
               return true;
            }
         }

         if (this.ahQuickSetFocused && input.isAllowedChatCharacter() && this.ahQuickSetInput.length() < 120) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= ' ' && ch.charAt(0) < 127) {
               this.ahQuickSetInput.append(ch);
               this.refreshAhQuickSetSuggestions(this.ahQuickSetInput.toString());
               return true;
            }
         }

         if (this.ahQuickSkinFocused && input.isAllowedChatCharacter() && this.ahQuickSkinInput.length() < 120) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= ' ' && ch.charAt(0) < 127) {
               this.ahQuickSkinInput.append(ch);
               this.refreshAhQuickSkinSuggestions(this.ahQuickSkinInput.toString());
               return true;
            }
         }

         if (this.manualBuyUuidFocused && input.isAllowedChatCharacter() && this.manualBuyUuidInput.length() < 48) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               boolean ok = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c == '-';
               if (ok) {
                  this.manualBuyUuidInput.append(Character.toLowerCase(c));
                  return true;
               }
            }
         }

         if (this.autoBuyNameFocused && input.isAllowedChatCharacter() && this.autoBuyNameInput.length() < 160) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= ' ' && ch.charAt(0) < 127) {
               this.autoBuyNameInput.append(ch);
               return true;
            }
         }

         if (this.autoBuyHexFocused && input.isAllowedChatCharacter() && this.autoBuyHexInput.length() < 220) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               boolean ok = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F' || c == '#' || c == ',' || c == ';' || c == ' ' || c == '\t';
               if (ok) {
                  this.autoBuyHexInput.append(Character.toUpperCase(c));
                  return true;
               }
            }
         }

         if (this.autoBuyRulesFocused && input.isAllowedChatCharacter() && this.autoBuyRulesInput.length() < 260) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0 && ch.charAt(0) >= ' ' && ch.charAt(0) < 127) {
               this.autoBuyRulesInput.append(ch);
               return true;
            }
         }

         if (this.lookupIgnFocused && input.isAllowedChatCharacter() && this.lookupIgnInput.length() < 16) {
            String ch = input.codepointAsString();
            if (ch != null && ch.length() > 0) {
               char c = ch.charAt(0);
               if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-') {
                  this.lookupIgnInput.append(c);
               }

               return true;
            }
         }

         return super.charTyped(input);
      }

      private ScannerConfig getConfig() {
         if (PotatoToolMod.getInstance() != null) {
            ScannerConfig c = PotatoToolMod.getInstance().getConfig();
            if (c != null) {
               return c;
            }
         }

         try {
            return ScannerConfig.load();
         } catch (Throwable t) {
            return new ScannerConfig();
         }
      }

      private void applyCakeSpecificYearsInput(ScannerConfig config) {
         if (config != null) {
            String s = this.cakeSpecificYearsInput.toString().trim().replaceAll("[\\[\\]]", "");
            if (!s.isEmpty()) {
               for (String part : s.split("[, \\t]+")) {
                  part = part.trim();
                  if (!part.isEmpty()) {
                     try {
                        int year = Integer.parseInt(part);
                        config.addCakeYear(year);
                     } catch (NumberFormatException var8) {
                     }
                  }
               }

               config.save();
            }

            this.cakeSpecificYearsInput.setLength(0);
         }
      }

      private void applyCakeMaxYearInput(ScannerConfig config) {
         if (config != null) {
            try {
               String s = this.cakeMaxYearInput.toString().trim();
               if (!s.isEmpty()) {
                  int v = Integer.parseInt(s);
                  config.newYearCakeMaxYear = Math.max(1, Math.min(500, v));
               }
            } catch (NumberFormatException var4) {
            }

            this.cakeMaxYearInput.setLength(0);
            config.save();
         }
      }

      private void applySpecificHexScanInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.specificHexScanInput.toString();
            Set<String> hexes = ScannerConfig.parseHexList(raw);
            config.specificHexList = hexes.isEmpty() ? "" : String.join(", ", hexes);
            this.specificHexScanInput.setLength(0);
            config.save();
         }
      }

      private void applyLevelInputs(ScannerConfig config) {
         if (config != null) {
            try {
               String minS = this.minLevelInput.toString().trim();
               if (!minS.isEmpty()) {
                  int v = Integer.parseInt(minS);
                  config.minSkyblockLevel = Math.max(0, Math.min(1000, v));
               }
            } catch (NumberFormatException var5) {
            }

            this.minLevelInput.setLength(0);

            try {
               String maxS = this.maxLevelInput.toString().trim();
               if (!maxS.isEmpty()) {
                  int v = Integer.parseInt(maxS);
                  config.skyblockLevelCap = Math.max(0, Math.min(1000, v));
               }
            } catch (NumberFormatException var4) {
            }

            this.maxLevelInput.setLength(0);
            config.save();
         }
      }

      private void applyMinSkinValueInput(ScannerConfig config) {
         if (config != null) {
            try {
               String s = this.minSkinValueInput.toString().trim();
               if (!s.isEmpty()) {
                  double v = Double.parseDouble(s);
                  config.minCosmeticSkinValueMillions = Math.max(0.0, v);
               }
            } catch (NumberFormatException var5) {
            }

            this.minSkinValueInput.setLength(0);
            config.save();
         }
      }

      private void applyAutoBuyNameInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.autoBuyNameInput.toString().trim();
            config.ahAutoBuyNameContains = raw;
            this.autoBuyNameInput.setLength(0);
            config.save();
         }
      }

      private void applyAutoBuyHexInput(ScannerConfig config) {
         if (config != null) {
            Set<String> hexes = ScannerConfig.parseHexList(this.autoBuyHexInput.toString());
            config.ahAutoBuyHexList = hexes.isEmpty() ? "" : String.join(", ", hexes);
            this.autoBuyHexInput.setLength(0);
            config.save();
         }
      }

      private void applyAutoBuyRulesInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.autoBuyRulesInput.toString().trim();
            config.ahAutoBuyItemPriceRules = raw;
            this.autoBuyRulesInput.setLength(0);
            config.save();
         }
      }

      private void applyAndClearAutoBuyInputs(ScannerConfig config) {
         if (this.autoBuyNameFocused) {
            this.applyAutoBuyNameInput(config);
         }

         if (this.autoBuyHexFocused) {
            this.applyAutoBuyHexInput(config);
         }

         if (this.autoBuyRulesFocused) {
            this.applyAutoBuyRulesInput(config);
         }

         this.manualBuyUuidFocused = false;
         this.autoBuyNameFocused = false;
         this.autoBuyHexFocused = false;
         this.autoBuyRulesFocused = false;
      }

      private void triggerManualBuyNow() {
         String raw = this.manualBuyUuidInput.toString().trim();
         if (raw.isEmpty()) {
            this.autoBuyStatusLine = "Enter an auction UUID first.";
         } else {
            PotatoToolMod mod = PotatoToolMod.getInstance();
            AuctionNotificationManager notifier = mod != null ? mod.getAuctionNotifier() : null;
            if (notifier != null && this.minecraft != null && this.minecraft.player != null) {
               boolean ok = notifier.requestAutoBuy(raw, this.minecraft);
               this.autoBuyStatusLine = ok ? "Buying auction: " + raw : "Invalid auction UUID.";
               if (ok) {
                  this.manualBuyUuidFocused = false;
               }
            } else {
               this.autoBuyStatusLine = "AH notifier not ready.";
            }
         }
      }

      private void applySeymourArmorFilterInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.seymourArmorFilterInput.toString().trim();
            config.seymourArmorNameFilters = raw.isEmpty() ? "all" : raw;
            this.seymourArmorFilterInput.setLength(0);
            config.save();
         }
      }

      private void applyAndClearSeymourInputs(ScannerConfig config) {
         if (this.seymourArmorFilterFocused) {
            this.applySeymourArmorFilterInput(config);
         }

         this.seymourArmorFilterFocused = false;
      }

      private void applyAhWatchTagsInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.ahWatchTagsInput.toString().trim();
            LinkedHashSet<String> tags = new LinkedHashSet<>();
            if (!raw.isEmpty()) {
               for (String part : raw.split("[,;\\n\\r\\t ]+")) {
                  for (String t : expandAhWatchTagInput(part)) {
                     if (!t.isEmpty()) {
                        tags.add(t);
                     }
                  }
               }
            }

            config.ahWatchTags = new ArrayList<>(tags);
            this.ahWatchTagsInput.setLength(0);
            config.save();
         }
      }

      private void applyAhSeymourHexInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.ahSeymourHexInput.toString().trim();
            if (raw.isEmpty()) {
               config.ahSeymourHexList = "";
            } else {
               LinkedHashSet<String> hexes = new LinkedHashSet<>();

               for (String part : raw.split("[,;\\n\\r\\t ]+")) {
                  String h = SeymourAnalyzer.normalizeHex(part);
                  if (h != null) {
                     hexes.add(h);
                  }
               }

               config.ahSeymourHexList = String.join(", ", hexes);
            }

            this.ahSeymourHexInput.setLength(0);
            config.save();
         }
      }

      private void applyAhSkinMuteInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.ahSkinMuteInput.toString().trim();
            if (raw.isEmpty()) {
               config.ahSkinMuteList = "";
            } else {
               LinkedHashSet<String> tokens = new LinkedHashSet<>();

               for (String part : raw.split("[,;\\n\\r\\t]+")) {
                  String token = part == null ? "" : part.trim().toUpperCase().replace(' ', '_');
                  if (!token.isEmpty()) {
                     tokens.add(token);
                  }
               }

               config.ahSkinMuteList = String.join(", ", tokens);
            }

            this.ahSkinMuteInput.setLength(0);
            config.save();
         }
      }

      private void applyAndClearAuctionInputs(ScannerConfig config) {
         if (this.ahWatchTagsFocused) {
            this.applyAhWatchTagsInput(config);
         }

         if (this.ahSeymourHexFocused) {
            this.applyAhSeymourHexInput(config);
         }

         if (this.ahSkinMuteFocused) {
            this.applyAhSkinMuteInput(config);
         }

         if (this.ahQuickSetFocused) {
            this.applyAhQuickSetInput();
         }

         if (this.ahQuickSkinFocused) {
            this.applyAhQuickSkinInput();
         }

         this.ahWatchTagsFocused = false;
         this.ahSeymourHexFocused = false;
         this.ahSkinMuteFocused = false;
         this.ahQuickSetFocused = false;
         this.ahQuickSkinFocused = false;
      }

      private void applyAhQuickSetInput() {
         String raw = this.ahQuickSetInput.toString().trim().toUpperCase().replace(' ', '_');
         if (!raw.isEmpty()) {
            this.ahQuickSelectedSet = this.resolveAhQuickSetTag(raw);
         }

         this.ahQuickSetInput.setLength(0);
         this.refreshAhQuickSetSuggestions("");
      }

      private void applyAhQuickSkinInput() {
         String raw = this.ahQuickSkinInput.toString().trim().toUpperCase().replace(' ', '_');
         if (!raw.isEmpty()) {
            this.ahQuickSelectedSkin = this.resolveAhQuickSkinTag(raw);
         }

         this.ahQuickSkinInput.setLength(0);
         this.refreshAhQuickSkinSuggestions("");
      }

      private int getAhFilterColumns(int contentWidth) {
         int cols = (contentWidth + 4) / 112;
         return Math.max(2, Math.min(4, cols));
      }

      private int getAhFilterChipWidth(int contentWidth, int columns) {
         return columns <= 1 ? contentWidth : Math.max(80, (contentWidth - (columns - 1) * 4) / columns);
      }

      private boolean getAhNotifyFilterByIndex(ScannerConfig config, int index) {
         if (config == null) {
            return false;
         }

         return switch (index) {
            case 0 -> config.ahNotifyFairy;
            case 1 -> config.ahNotifyOgFairy;
            case 2 -> config.ahNotifyExotic;
            case 3 -> config.ahNotifyArmorSkins;
            case 4 -> config.ahNotifyPetSkins;
            case 5 -> config.ahNotifyCrystal;
            case 6 -> config.ahNotifyBleached;
            case 7 -> config.ahNotifyGlitched;
            case 8 -> config.ahNotifySeymourT1;
            case 9 -> config.ahNotifySeymourT2;
            case 10 -> config.ahNotifySeymourSpecificHexes;
            default -> false;
         };
      }

      private void setAhNotifyFilterByIndex(ScannerConfig config, int index, boolean enabled) {
         if (config != null) {
            switch (index) {
               case 0:
                  config.ahNotifyFairy = enabled;
                  break;
               case 1:
                  config.ahNotifyOgFairy = enabled;
                  break;
               case 2:
                  config.ahNotifyExotic = enabled;
                  break;
               case 3:
                  config.ahNotifyArmorSkins = enabled;
                  break;
               case 4:
                  config.ahNotifyPetSkins = enabled;
                  break;
               case 5:
                  config.ahNotifyCrystal = enabled;
                  break;
               case 6:
                  config.ahNotifyBleached = enabled;
                  break;
               case 7:
                  config.ahNotifyGlitched = enabled;
                  break;
               case 8:
                  config.ahNotifySeymourT1 = enabled;
                  break;
               case 9:
                  config.ahNotifySeymourT2 = enabled;
                  break;
               case 10:
                  config.ahNotifySeymourSpecificHexes = enabled;
            }
         }
      }

      private int countEnabledAhFilters(ScannerConfig config) {
         int n = 0;

         for (int i = 0; i < AH_NOTIFY_FILTER_LABELS.length; i++) {
            if (this.getAhNotifyFilterByIndex(config, i)) {
               n++;
            }
         }

         return n;
      }

      private int getFooterTopY() {
         return this.layoutH - 28 - 22 - 8;
      }

      private int getAhContentBottomY() {
         return this.getFooterTopY() - 6;
      }

      private int getAhVisibleContentHeight() {
         int top = this.contentY() + 40;
         return Math.max(60, this.getAhContentBottomY() - top);
      }

      private int estimateAhContentHeight(int contentWidth) {
         int y = 0;
         y += 48;
         y += 22;
         y += 22;
         int filterCols = this.getAhFilterColumns(contentWidth);
         int filterRows = (AH_NOTIFY_FILTER_LABELS.length + filterCols - 1) / filterCols;
         y += filterRows * 26 + 2;
         y += 30;
         y += 92;
         y += 228;
         y += 24;
         y += 24;
         y += 32;
         y += 22;
         y += 22;
         y += 24;
         y += 46;
         y += 22;
         y += 58;
         y += 26;
         y += 26;
         return y + 22;
      }

      private void updateAhScrollBounds(int contentWidth) {
         int contentH = this.estimateAhContentHeight(contentWidth);
         int visibleH = this.getAhVisibleContentHeight();
         this.ahPageMaxScroll = Math.max(0, contentH - visibleH);
         if (this.ahPageScroll < -this.ahPageMaxScroll) {
            this.ahPageScroll = -this.ahPageMaxScroll;
         }

         if (this.ahPageScroll > 0) {
            this.ahPageScroll = 0;
         }
      }

      private int computeAhQuickButtonY(int quickSetY, int setSugCount, int skinSugCount) {
         int quickSetSugStartY = quickSetY + 22 + 2;
         int quickSkinY = quickSetSugStartY + setSugCount * 12;
         int quickSkinSugStartY = quickSkinY + 22 + 2;
         return quickSkinSugStartY + skinSugCount * 12 + 4;
      }

      private int[] clampAhSuggestionCountsToSpace(int quickSetY, int setDesired, int skinDesired) {
         int setCount = Math.max(0, setDesired);
         int skinCount = Math.max(0, skinDesired);
         int contentBottom = this.getAhContentBottomY();

         while ((setCount > 0 || skinCount > 0) && this.computeAhQuickButtonY(quickSetY, setCount, skinCount) + 22 > contentBottom) {
            if (skinCount > 0) {
               skinCount--;
            } else {
               setCount--;
            }
         }

         return new int[]{setCount, skinCount};
      }

      private void applyAhFilterPreset(ScannerConfig config, int preset) {
         if (config != null) {
            if (preset == 0) {
               for (int i = 0; i < AH_NOTIFY_FILTER_LABELS.length; i++) {
                  this.setAhNotifyFilterByIndex(config, i, true);
               }

               config.ahNotificationsEnabled = true;
               this.ahQuickActionStatus = "AH filters: all enabled";
            } else if (preset == 1) {
               this.setAhNotifyFilterByIndex(config, 0, false);
               this.setAhNotifyFilterByIndex(config, 1, false);
               this.setAhNotifyFilterByIndex(config, 2, true);
               this.setAhNotifyFilterByIndex(config, 3, true);
               this.setAhNotifyFilterByIndex(config, 4, true);
               this.setAhNotifyFilterByIndex(config, 5, false);
               this.setAhNotifyFilterByIndex(config, 6, false);
               this.setAhNotifyFilterByIndex(config, 7, true);
               this.setAhNotifyFilterByIndex(config, 8, true);
               this.setAhNotifyFilterByIndex(config, 9, true);
               this.setAhNotifyFilterByIndex(config, 10, true);
               config.ahNotificationsEnabled = true;
               this.ahQuickActionStatus = "AH filters: core enabled";
            } else {
               for (int i = 0; i < AH_NOTIFY_FILTER_LABELS.length; i++) {
                  this.setAhNotifyFilterByIndex(config, i, false);
               }

               this.ahQuickActionStatus = "AH filters: all disabled";
            }

            config.save();
         }
      }

      private static String normalizeAhWatchTagInput(String raw) {
         if (raw == null) {
            return "";
         }

         String t = raw.trim().toUpperCase().replace(' ', '_');

         return switch (t) {
            case "NECRON_HELMET" -> "POWER_WITHER_HELMET";
            case "NECRON_CHESTPLATE" -> "POWER_WITHER_CHESTPLATE";
            case "NECRON_LEGGINGS" -> "POWER_WITHER_LEGGINGS";
            case "NECRON_BOOTS" -> "POWER_WITHER_BOOTS";
            case "MAXOR_HELMET" -> "SPEED_WITHER_HELMET";
            case "MAXOR_CHESTPLATE" -> "SPEED_WITHER_CHESTPLATE";
            case "MAXOR_LEGGINGS" -> "SPEED_WITHER_LEGGINGS";
            case "MAXOR_BOOTS" -> "SPEED_WITHER_BOOTS";
            case "STORM_HELMET" -> "WISE_WITHER_HELMET";
            case "STORM_CHESTPLATE" -> "WISE_WITHER_CHESTPLATE";
            case "STORM_LEGGINGS" -> "WISE_WITHER_LEGGINGS";
            case "STORM_BOOTS" -> "WISE_WITHER_BOOTS";
            case "GOLDOR_HELMET" -> "TANK_WITHER_HELMET";
            case "GOLDOR_CHESTPLATE" -> "TANK_WITHER_CHESTPLATE";
            case "GOLDOR_LEGGINGS" -> "TANK_WITHER_LEGGINGS";
            case "GOLDOR_BOOTS" -> "TANK_WITHER_BOOTS";
            default -> t;
         };
      }

      private static List<String> expandAhWatchTagInput(String raw) {
         ArrayList<String> out = new ArrayList<>();
         String t = normalizeAhWatchTagInput(raw);
         if (t.isEmpty()) {
            return out;
         }

         if (t.endsWith("_ARMOR")) {
            String base = t.substring(0, t.length() - "_ARMOR".length());
            String[] pieces = new String[]{"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};

            for (String piece : pieces) {
               String expanded = normalizeAhWatchTagInput(base + "_" + piece);
               if (!expanded.isEmpty()) {
                  out.add(expanded);
               }
            }

            return out;
         } else {
            out.add(t);
            return out;
         }
      }

      private String resolveAhQuickSetTag(String candidateRaw) {
         String candidate = normalizeAhWatchTagInput(candidateRaw);
         if (candidate.isEmpty()) {
            return "";
         }

         List<String> pool = this.getAllSetTags();
         if (pool.contains(candidate)) {
            return candidate;
         }

         if (candidate.endsWith("_ARMOR")) {
            String base = candidate.substring(0, candidate.length() - "_ARMOR".length());
            String[] pieces = new String[]{"CHESTPLATE", "LEGGINGS", "HELMET", "BOOTS"};

            for (String piece : pieces) {
               String expanded = normalizeAhWatchTagInput(base + "_" + piece);
               if (pool.contains(expanded)) {
                  return expanded;
               }
            }
         }

         this.refreshAhQuickSetSuggestions(candidateRaw);
         return !this.ahQuickSetSuggestions.isEmpty() ? this.ahQuickSetSuggestions.get(0) : candidate;
      }

      private String resolveAhQuickSkinTag(String candidateRaw) {
         String candidate = normalizeAhWatchTagInput(candidateRaw);
         if (candidate.isEmpty()) {
            return "";
         }

         List<String> pool = this.getAllSkinTags();
         if (pool.contains(candidate)) {
            return candidate;
         }

         this.refreshAhQuickSkinSuggestions(candidateRaw);
         return !this.ahQuickSkinSuggestionTags.isEmpty() ? this.ahQuickSkinSuggestionTags.get(0) : candidate;
      }

      private String getAhQuickType() {
         int idx = Math.max(0, Math.min(AH_QUICK_TYPES.length - 1, this.ahQuickTypeIndex));
         return AH_QUICK_TYPES[idx];
      }

      private String getAhQuickTypeLabel() {
         return this.getAhQuickType().replace('_', ' ');
      }

      private List<String> getAllSetTags() {
         ArrayList<String> out = new ArrayList<>();

         for (String id : DefaultArmorColorsLoader.getKnownItemIds()) {
            if (id != null) {
               String t = id.trim().toUpperCase();
               if (!t.isEmpty() && !t.contains("_SKIN_") && !t.startsWith("PET_SKIN_")) {
                  out.add(t);
               }
            }
         }

         Collections.sort(out);
         return out;
      }

      private List<String> getAllSkinTags() {
         ArrayList<String> out = new ArrayList<>();

         for (String id : CosmeticSkinValuesLoader.getKnownSkinItemIds()) {
            if (id != null) {
               String t = id.trim().toUpperCase();
               if (!t.isEmpty() && isLikelyArmorSkinTag(t)) {
                  out.add(t);
               }
            }
         }

         Collections.sort(out);
         return out;
      }

      private static boolean isLikelyArmorSkinTag(String tag) {
         if (tag == null || tag.isEmpty()) {
            return false;
         } else if (tag.startsWith("PET_SKIN_")) {
            return false;
         } else if (tag.endsWith("_PERSONALITY")) {
            return false;
         } else if (tag.contains("_BARN_")) {
            return false;
         } else if (tag.contains("GREENHOUSE")) {
            return false;
         } else {
            return tag.contains("_BACKPACK") ? false : !tag.endsWith("_FLUX");
         }
      }

      private static String toFriendlySkinLabel(String tag) {
         if (tag != null && !tag.isEmpty()) {
            String s = tag.trim().toUpperCase();
            if (s.startsWith("PET_SKIN_")) {
               s = s.substring("PET_SKIN_".length());
            } else if (s.contains("_SKIN_")) {
               s = s.replace("_SKIN_", " ");
            }

            s = s.replace('_', ' ');
            StringBuilder out = new StringBuilder();
            boolean cap = true;

            for (int i = 0; i < s.length(); i++) {
               char c = s.charAt(i);
               if (c == ' ') {
                  cap = true;
                  out.append(c);
               } else if (cap) {
                  out.append(Character.toUpperCase(c));
                  cap = false;
               } else {
                  out.append(Character.toLowerCase(c));
               }
            }

            return out.toString().trim() + " Skin";
         } else {
            return "";
         }
      }

      private static boolean startsWithWordOrText(String sourceUpper, String queryUpper) {
         if (queryUpper != null && !queryUpper.isEmpty()) {
            if (sourceUpper != null && !sourceUpper.isEmpty()) {
               if (sourceUpper.startsWith(queryUpper)) {
                  return true;
               }

               String[] words = sourceUpper.split("[^A-Z0-9]+");

               for (String w : words) {
                  if (w.startsWith(queryUpper)) {
                     return true;
                  }
               }

               return false;
            } else {
               return false;
            }
         } else {
            return true;
         }
      }

      private void refreshAhQuickSetSuggestions(String queryRaw) {
         String q = queryRaw == null ? "" : queryRaw.trim().toUpperCase().replace(' ', '_');
         this.ahQuickSetSuggestions.clear();
         List<String> pool = this.getAllSetTags();

         for (String tag : pool) {
            if (q.isEmpty() || tag.startsWith(q)) {
               this.ahQuickSetSuggestions.add(tag);
               if (this.ahQuickSetSuggestions.size() >= 6) {
                  break;
               }
            }
         }

         if (this.ahQuickSetSuggestions.isEmpty() && !q.isEmpty()) {
            for (String tag : pool) {
               if (tag.contains(q)) {
                  this.ahQuickSetSuggestions.add(tag);
                  if (this.ahQuickSetSuggestions.size() >= 6) {
                     break;
                  }
               }
            }
         }
      }

      private void refreshAhQuickSkinSuggestions(String queryRaw) {
         String q = queryRaw == null ? "" : queryRaw.trim().toUpperCase().replace(' ', '_');
         String qWord = q.replace('_', ' ');
         this.ahQuickSkinSuggestionTags.clear();
         this.ahQuickSkinSuggestionLabels.clear();

         for (String tag : this.getAllSkinTags()) {
            String label = toFriendlySkinLabel(tag);
            String labelUpper = label.toUpperCase();
            boolean match = q.isEmpty() || startsWithWordOrText(tag, q) || startsWithWordOrText(labelUpper, qWord) || tag.contains(q);
            if (match) {
               this.ahQuickSkinSuggestionTags.add(tag);
               this.ahQuickSkinSuggestionLabels.add(label + "  §8(" + tag + ")");
               if (this.ahQuickSkinSuggestionTags.size() >= 8) {
                  break;
               }
            }
         }
      }

      private void addAhTagToWatchlist(ScannerConfig config, String tag) {
         if (config != null && tag != null && !tag.isEmpty()) {
            if (config.ahWatchTags == null) {
               config.ahWatchTags = new ArrayList<>();
            }

            for (String t : expandAhWatchTagInput(tag)) {
               if (!t.isEmpty() && !config.ahWatchTags.contains(t)) {
                  config.ahWatchTags.add(t);
               }
            }

            config.save();
         }
      }

      private void enableAhTypeForQuickScan(ScannerConfig config, String type) {
         if (config != null && type != null) {
            switch (type) {
               case "FAIRY":
                  config.ahNotifyFairy = true;
                  break;
               case "OG_FAIRY":
                  config.ahNotifyOgFairy = true;
                  break;
               case "EXOTIC":
                  config.ahNotifyExotic = true;
                  break;
               case "CRYSTAL":
                  config.ahNotifyCrystal = true;
                  break;
               case "BLEACHED":
                  config.ahNotifyBleached = true;
                  break;
               case "GLITCHED":
                  config.ahNotifyGlitched = true;
                  break;
               case "SEYMOUR_T1":
                  config.ahNotifySeymourT1 = true;
                  break;
               case "SEYMOUR_T2":
                  config.ahNotifySeymourT2 = true;
                  break;
               case "ARMOR_SKIN":
                  config.ahNotifyArmorSkins = true;
            }

            config.ahNotificationsEnabled = true;
            config.save();
         }
      }

      private void triggerAhQuickScanNow(ScannerConfig config) {
         if (config != null) {
            String type = this.getAhQuickType();
            this.enableAhTypeForQuickScan(config, type);
            String setTag = this.ahQuickSelectedSet == null ? "" : this.ahQuickSelectedSet.trim();
            String skinTag = this.ahQuickSelectedSkin == null ? "" : this.ahQuickSelectedSkin.trim();
            if ("ARMOR_SKIN".equals(type) && skinTag.isEmpty()) {
               this.ahQuickActionStatus = "Pick a skin tag first for ARMOR SKIN active scan.";
            } else {
               if (!setTag.isEmpty() && !"ARMOR_SKIN".equals(type)) {
                  this.addAhTagToWatchlist(config, setTag);
               }

               if (!skinTag.isEmpty() && "ARMOR_SKIN".equals(type)) {
                  this.addAhTagToWatchlist(config, skinTag);
               }

               if (PotatoToolMod.getInstance() != null && PotatoToolMod.getInstance().getAuctionNotifier() != null) {
                  ArrayList<String> forcedTags = new ArrayList<>();
                  if (!setTag.isEmpty() && !"ARMOR_SKIN".equals(type)) {
                     forcedTags.add(setTag);
                  }

                  if (!skinTag.isEmpty() && "ARMOR_SKIN".equals(type)) {
                     forcedTags.add(skinTag);
                  }

                  if (forcedTags.isEmpty()) {
                     PotatoToolMod.getInstance().getAuctionNotifier().requestImmediatePoll();
                     this.ahQuickActionStatus = "Active scan now: " + this.getAhQuickTypeLabel() + " (auto tag pool)";
                  } else {
                     PotatoToolMod.getInstance().getAuctionNotifier().requestImmediatePollForTags(forcedTags);
                     this.ahQuickActionStatus = "Active scan now: " + this.getAhQuickTypeLabel() + " on " + String.join(", ", forcedTags);
                  }
               } else {
                  this.ahQuickActionStatus = "AH notifier not ready";
               }
            }
         }
      }

      public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
         super.extractRenderState(context, mouseX, mouseY, delta);
         if (this.dragging) {
            this.applyDragFromMouse(mouseX, mouseY);
         }

         if (this.resizing) {
            this.applyResizeFromMouse(mouseX, mouseY);
         }

         ScannerConfig config = this.getConfig();
         this.darkGui = config == null || config.guiDarkMode;
         if (this.darkGui) {
            this.TEXT_WHITE = 0xFFF0E9FF;
            this.TEXT_MUTED = 0xFF9B8EC4;
         } else {
            this.TEXT_WHITE = 0xFF1B2430;
            this.TEXT_MUTED = 0xFF5C6775;
         }

         int alpha = this.getGuiAlpha(config);
         int bx = this.panelX;
         int by = this.panelY;
         int lmx = mouseX - this.panelX;
         int lmy = mouseY - this.panelY;
         if (this.currentPage == 6 || this.currentPage == 7 || this.currentPage == 8 || this.currentPage == 9) {
            int maxW = Math.max(560, this.width - 40);
            int maxH = Math.max(420, this.height - 40);
            int targetW;
            int targetH;
            if (this.currentPage == 9) {
               targetW = Math.min(maxW, 860);
               targetH = Math.min(maxH, 640);
            } else if (this.currentPage == 8) {
               targetW = Math.min(maxW, 790);
               targetH = Math.min(maxH, 640);
            } else if (this.currentPage == 7) {
               targetW = Math.min(maxW, 820);
               targetH = Math.min(maxH, 760);
            } else {
               targetW = Math.min(maxW, 790);
               targetH = Math.min(maxH, 620);
            }

            if (this.panelW < targetW) {
               this.panelW = targetW;
            }

            if (this.panelH < targetH) {
               this.panelH = targetH;
            }

            this.panelX = Math.max(0, Math.min(this.width - this.panelW, this.panelX));
            this.panelY = Math.max(0, Math.min(this.height - this.panelH, this.panelY));
            this.layoutW = this.panelW;
            this.layoutH = this.panelH;
            bx = this.panelX;
            by = this.panelY;
            lmx = mouseX - this.panelX;
            lmy = mouseY - this.panelY;
         }

         context.fill(0, 0, this.width, this.height, this.darkGui ? 0x99080512 : 0x55081A28);
         int glassA = Math.max(90, Math.min(170, alpha * 2 / 3));
         int accentRgb = PotatoTheme.guiAccentRgb(config);
         int accent = 0xFF000000 | accentRgb;
         int bgDark;
         int bgSidebar;
         int borderColor;
         int selectedBg;
         int btnBg;
         int btnHover;
         int titleBarBg;
         int navSelectedText;
         if (this.darkGui) {
            bgDark = glassA << 24 | 0x12101C;
            bgSidebar = glassA << 24 | 0x0C0A16;
            borderColor = 0xAA000000 | accentRgb;
            selectedBg = 0x55000000 | accentRgb;
            btnBg = Math.min(210, glassA + 40) << 24 | 0x1A1628;
            btnHover = Math.min(230, glassA + 70) << 24 | 0x2A2448;
            titleBarBg = glassA << 24 | 0x161028;
            navSelectedText = 0xFFF0E9FF;
         } else {
            bgDark = glassA << 24 | 0xF4F7FB;
            bgSidebar = glassA << 24 | 0xE8EEF6;
            borderColor = 0xAA7EB6E0;
            selectedBg = 0x664A8FD4;
            btnBg = Math.min(200, glassA + 30) << 24 | 0xFFFFFF;
            btnHover = Math.min(220, glassA + 50) << 24 | 0xD6E8FA;
            titleBarBg = glassA << 24 | 0xF8FBFF;
            navSelectedText = 0xFF1B3A5C;
         }

         context.fill(bx - 1, by - 1, bx + this.panelW + 1, by + this.panelH + 1, borderColor);
         context.fill(bx, by, bx + this.panelW, by + TITLE_BAR_HEIGHT, titleBarBg);
         context.fill(bx, by + TITLE_BAR_HEIGHT - 1, bx + this.panelW, by + TITLE_BAR_HEIGHT, borderColor);
         context.fill(bx + 10, by + 11, bx + 14, by + 23, accent);
         context.text(this.font, this.title, bx + 20, by + 11, this.TEXT_WHITE, false);
         String discordText = "Discord";
         int discordTextW = this.font.width(discordText);
         int discordScaledW = discordTextW;
         int discordScaledH = 9;
         int discordBtnX = bx + this.panelW - 14 - discordScaledW;
         int discordBtnY = by + (TITLE_BAR_HEIGHT - discordScaledH) / 2;
         String modeLabel = this.darkGui ? "Dark" : "Light";
         int modeW = this.font.width(modeLabel);
         int modeBtnX = discordBtnX - 12 - modeW;
         boolean modeHover = mouseX >= modeBtnX
            && mouseX < modeBtnX + modeW
            && mouseY >= discordBtnY
            && mouseY < discordBtnY + discordScaledH;
         context.text(this.font, modeLabel, modeBtnX, discordBtnY, modeHover ? 0xFF4A8FD4 : this.TEXT_MUTED, false);
         boolean discordHover = mouseX >= discordBtnX
            && mouseX < discordBtnX + discordScaledW
            && mouseY >= discordBtnY
            && mouseY < discordBtnY + discordScaledH;
         context.text(this.font, discordText, discordBtnX, discordBtnY, discordHover ? 0xFF4A8FD4 : this.TEXT_MUTED, false);
         if (discordHover) {
            context.text(this.font, "discord.gg/PvEYc4Kwy", mouseX + 6, mouseY - 4, this.TEXT_WHITE, false);
         }

         context.fill(bx, by + TITLE_BAR_HEIGHT, bx + 180, by + this.panelH, bgSidebar);
         context.fill(bx + 180 - 1, by + TITLE_BAR_HEIGHT, bx + 180, by + this.panelH, borderColor);
         int navY0 = TITLE_BAR_HEIGHT + 8;

         for (int i = 0; i < NAV_LABELS.length; i++) {
            int y0 = navY0 + i * 24;
            if (this.currentPage == i) {
               context.fill(bx, by + y0, bx + 180 - 1, by + y0 + 22, selectedBg);
               context.fill(bx, by + y0, bx + 2, by + y0 + 22, accent);
            }

            context.text(this.font, NAV_LABELS[i], bx + 14, by + y0 + 7, this.currentPage == i ? navSelectedText : this.TEXT_MUTED, false);
         }

         context.fill(bx + 180, by + TITLE_BAR_HEIGHT, bx + this.panelW, by + this.panelH, bgDark);
         int cx = this.contentX();
         int cw = this.contentWidth();
         int cy = this.contentY();

         String subtitle = switch (this.currentPage) {
            case 0 -> "API keys";
            case 1 -> "Lookup";
            case 2 -> "Item categories";
            case 3 -> "Special items";
            case 4 -> "Skins";
            case 5 -> "Advanced";
            case 6 -> "HUD";
            case 7 -> "Appearance";
            case 8 -> "Seymour";
            case 9 -> "Automatic";
            case 10 -> "Webhook";
            case 11 -> "Notifier";
            case 12 -> "Hits";
            default -> "";
         };
         context.text(this.font, subtitle, bx + cx, by + cy + 2, this.TEXT_MUTED, false);
         if (this.currentPage == 0) {
            ScannerConfig.ensureApiKeyTimestamps(config);
            int y = cy + 40;
            context.fill(bx + cx, by + y, bx + cx + cw, by + y + 22, this.apiKeyInputFocused ? btnHover : btnBg);
            context.fill(bx + cx + 1, by + y + 1, bx + cx + cw - 1, by + y + 22 - 1, alpha << 24 | 1118484);
            String inputDisplay = this.apiKeyInput.length() > 0 ? this.apiKeyInput.toString() : "Paste key here";
            if (this.apiKeyInput.length() > 0) {
               context.text(this.font, inputDisplay, bx + cx + 6, by + y + 6, 0xFFE6EAF0, false);
            } else {
               context.text(this.font, inputDisplay, bx + cx + 6, by + y + 6, 0xFF6E7680, false);
            }

            if (this.apiKeyInputFocused) {
               context.text(this.font, "_", bx + cx + 6 + this.font.width(inputDisplay), by + y + 6, 0xFF4A8FD4, false);
            }

            y += 26;
            boolean addHover = lmx >= cx && lmx < cx + 120 && lmy >= y && lmy < y + 22;
            context.fill(bx + cx, by + y, bx + cx + 120, by + y + 22, addHover ? btnHover : btnBg);
            context.text(this.font, "+ Add key", bx + cx + 10, by + y + 6, 0xFF2F7CC8, false);
            boolean getHover = lmx >= cx + 128 && lmx < cx + 228 && lmy >= y && lmy < y + 22;
            context.fill(bx + cx + 128, by + y, bx + cx + 228, by + y + 22, getHover ? btnHover : btnBg);
            context.text(this.font, "Get key", bx + cx + 138, by + y + 6, 0xFF7EB6E0, false);
            y += 34;

            for (int i = 0; i < config.apiKeys.size(); i++) {
               String key = config.apiKeys.get(i);
               String masked = key.length() > 12 ? key.substring(0, 12) + "…" : key;
               long addedAt = config.apiKeyAddedAt != null && i < config.apiKeyAddedAt.size() ? config.apiKeyAddedAt.get(i) : 0L;
               String activeStr = ScannerConfig.getKeyActiveDuration(addedAt);
               Boolean valid = this.keyValidStatus.get(key);
               String statusStr = valid == null ? "—" : (valid ? "Valid" : "Invalid");
               int remaining = config.getApiKeyRemaining(i);
               int windowLeft = config.getApiWindowRemaining(key);
               String usage = remaining + "/" + ScannerConfig.API_KEY_USAGE_LIMIT + "  " + windowLeft + "/300 5m";
               int removeX = cx + cw - 70;
               int copyX = removeX - 64;
               int usageW = this.font.width(usage);
               int usageX = copyX - 10 - usageW;
               String rowText = "[" + i + "] " + masked + "  " + activeStr + "  " + statusStr;
               context.text(this.font, this.fitText(rowText, Math.max(80, usageX - cx - 8)), bx + cx, by + y + 4, this.TEXT_WHITE, false);
               int usageColor = remaining <= 0 ? 0xFFCF6679 : remaining < 800 ? 0xFFE0B04A : 0xFF7EB6E0;
               context.text(this.font, usage, bx + usageX, by + y + 4, usageColor, false);
               boolean copyHover = lmx >= copyX && lmx < copyX + 60 && lmy >= y && lmy < y + 22;
               context.fill(bx + copyX, by + y, bx + copyX + 60, by + y + 22, copyHover ? btnHover : btnBg);
               context.text(this.font, "Copy", bx + copyX + 14, by + y + 6, 0xFF2F7CC8, false);
               boolean removeHover = lmx >= removeX && lmx < removeX + 60 && lmy >= y && lmy < y + 22;
               context.fill(bx + removeX, by + y, bx + removeX + 60, by + y + 22, removeHover ? 0xFF3A2428 : btnBg);
               context.text(this.font, "Remove", bx + removeX + 8, by + y + 6, 0xFFD08080, false);
               y += 22;
               if (y > this.layoutH - 60) {
                  break;
               }
            }

            context.text(
               this.font, config.apiKeys.size() + " key(s)  ·  remaining uses / 5000", bx + cx, by + this.layoutH - 36, this.TEXT_MUTED, false
            );
         } else if (this.currentPage == 1) {
            this.drawLookupPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 2) {
            this.drawToggle(context, bx, by, cx, cy + 40, cw, "Weapons", config.scanWeapons, lmx, lmy);
            this.drawToggle(context, bx, by, cx, cy + 40 + 22, cw, "Armor", config.scanArmor, lmx, lmy);
            this.drawToggle(context, bx, by, cx, cy + 40 + 44, cw, "Accessories", config.scanAccessories, lmx, lmy);
            this.drawToggle(context, bx, by, cx, cy + 40 + 66, cw, "Tools", config.scanTools, lmx, lmy);
            this.drawToggle(context, bx, by, cx, cy + 40 + 88, cw, "Pets", config.scanPets, lmx, lmy);
            this.drawToggle(context, bx, by, cx, cy + 40 + 110, cw, "Dungeon Items", config.scanDungeonItems, lmx, lmy);
            this.drawToggle(context, bx, by, cx, cy + 40 + 132, cw, "Slayer Items", config.scanSlayerItems, lmx, lmy);
            context.text(this.font, "More options: use config file or future update", bx + cx, by + cy + 40 + 176, this.TEXT_MUTED, false);
         } else if (this.currentPage == 3) {
            this.drawSpecialPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 4) {
            this.drawSkinsPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 5) {
            this.drawAdvancedPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 6) {
            this.drawHudPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 7) {
            this.drawAppearancePage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 8) {
            this.drawSeymourPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 9) {
            this.drawAutomaticPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 10) {
            this.drawWebhookPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 11) {
            this.drawNotifierPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         } else if (this.currentPage == 12) {
            this.drawHitsPage(context, config, bx, by, cx, cy, cw, lmx, lmy, btnBg, btnHover);
         }

         int doneX = this.layoutW / 2 - 60;
         int doneY = this.layoutH - 28;
         boolean doneHover = lmx >= doneX && lmx < doneX + 120 && lmy >= doneY && lmy < doneY + 22;
         context.fill(bx + doneX, by + doneY, bx + doneX + 120, by + doneY + 22, doneHover ? btnHover : btnBg);
         context.text(this.font, CommonComponents.GUI_DONE.getString(), bx + doneX + 40, by + doneY + 6, this.TEXT_WHITE, false);
         int sizeRowY = this.layoutH - 28 - 22 - 8;
         int sizeLabelW = this.font.width("Size: ");
         context.text(this.font, "Size:", bx + cx, by + sizeRowY + 4, this.TEXT_MUTED, false);
         String[] sizeLabels = new String[]{"Small", "Medium", "Large"};

         for (int i = 0; i < PRESET_W.length; i++) {
            int sx = cx + sizeLabelW + 8 + i * 72;
            boolean sizeHover = lmx >= sx && lmx < sx + 68 && lmy >= sizeRowY && lmy < sizeRowY + 22;
            boolean sizeActive = this.panelW == PRESET_W[i] && this.panelH == PRESET_H[i];
            context.fill(bx + sx, by + sizeRowY, bx + sx + 68, by + sizeRowY + 22, sizeHover ? btnHover : (sizeActive ? selectedBg : btnBg));
            context.text(this.font, sizeLabels[i], bx + sx + 8, by + sizeRowY + 4, sizeActive ? -7820545 : this.TEXT_WHITE, false);
         }

         if (this.panelW > 28 && this.panelH > 28) {
            int rhx = bx + this.panelW - 28;
            int rhy = by + this.panelH - 28;
            boolean resizeHover = mouseX >= rhx && mouseX < bx + this.panelW && mouseY >= rhy && mouseY < by + this.panelH;
            context.fill(rhx, rhy, bx + this.panelW, by + this.panelH, resizeHover ? 1358954495 : 682273450);
            context.text(this.font, "Resize", bx + this.panelW - 38, by + this.panelH - 10, resizeHover ? -3355444 : this.TEXT_MUTED, false);
         }
      }


      private List<String> ensureMacroSequence(ScannerConfig config) {
         if (config.automaticMacroSequence == null) {
            config.automaticMacroSequence = new ArrayList<>();
         }

         return config.automaticMacroSequence;
      }

      private int[] automaticPresetRect(int index, int cx, int cy, int cw) {
         int chipW = 104;
         int chipH = 24;
         int gap = 6;
         int perRow = Math.max(1, (cw + gap) / (chipW + gap));
         int col = index % perRow;
         int row = index / perRow;
         int x = cx + col * (chipW + gap);
         int y = cy + 36 + row * (chipH + gap);
         return new int[]{x, y, chipW, chipH};
      }

      private int automaticBoardY(int cx, int cy, int cw) {
         int rows = (Math.max(1, ChatMacroRunner.PRESETS.length) + Math.max(1, (cw + 6) / 110) - 1) / Math.max(1, (cw + 6) / 110);
         return cy + 36 + rows * 30 + 22;
      }

      private int[] automaticBoardRect(int cx, int cy, int cw) {
         int y = this.automaticBoardY(cx, cy, cw);
         int h = Math.max(120, this.layoutH - y - 96);
         return new int[]{cx, y, cw, h};
      }

      private int[] automaticBlockRect(int index, int cx, int cy, int cw) {
         int[] board = this.automaticBoardRect(cx, cy, cw);
         int blockH = 28;
         int gap = 6;
         int x = board[0] + 10;
         int y = board[1] + 12 + index * (blockH + gap);
         int w = board[2] - 20;
         return new int[]{x, y, w, blockH};
      }

      private int[] automaticButtonRect(int which, int cx, int cw) {
         int y = this.layoutH - 86;
         int w = 92;
         int gap = 8;
         int x = cx + which * (w + gap);
         return new int[]{x, y, w, 22};
      }

      private int sequenceInsertIndex(int lx, int ly) {
         int cx = this.contentX();
         int cy = this.contentY();
         int cw = this.contentWidth();
         int[] board = this.automaticBoardRect(cx, cy, cw);
         if (lx < board[0] || lx >= board[0] + board[2] || ly < board[1] || ly >= board[1] + board[3]) {
            return -1;
         }

         ScannerConfig config = this.getConfig();
         int count = this.ensureMacroSequence(config).size();
         if (this.macroDragFromSequence >= 0) {
            count = Math.max(0, count - 1);
         }

         for (int i = 0; i < Math.max(1, count); i++) {
            int[] r = this.automaticBlockRect(i, cx, cy, cw);
            if (ly < r[1] + r[3] / 2) {
               return i;
            }
         }

         return count;
      }

      private boolean handleAutomaticClick(int lx, int ly, int cx, int cy, int cw, ScannerConfig config) {
         List<String> sequence = this.ensureMacroSequence(config);
         int[] start = this.automaticButtonRect(0, cx, cw);
         int[] stop = this.automaticButtonRect(1, cx, cw);
         int[] clear = this.automaticButtonRect(2, cx, cw);
         if (lx >= start[0] && lx < start[0] + start[2] && ly >= start[1] && ly < start[1] + start[3]) {
            if (!sequence.isEmpty() && this.minecraft != null) {
               List<String> copy = new ArrayList<>(sequence);
               this.minecraft.setScreen(null);
               ChatMacroRunner.start(copy);
            }

            return true;
         }

         if (lx >= stop[0] && lx < stop[0] + stop[2] && ly >= stop[1] && ly < stop[1] + stop[3]) {
            ChatMacroRunner.cancel(this.minecraft);
            return true;
         }

         if (lx >= clear[0] && lx < clear[0] + clear[2] && ly >= clear[1] && ly < clear[1] + clear[3]) {
            sequence.clear();
            config.save();
            return true;
         }

         for (int i = 0; i < sequence.size(); i++) {
            int[] r = this.automaticBlockRect(i, cx, cy, cw);
            if (lx >= r[0] && lx < r[0] + r[2] && ly >= r[1] && ly < r[1] + r[3]) {
               int removeX = r[0] + r[2] - 18;
               if (lx >= removeX) {
                  sequence.remove(i);
                  config.save();
                  return true;
               }

               this.macroDragging = true;
               this.macroDragCommand = sequence.get(i);
               this.macroDragFromSequence = i;
               this.macroHoverInsert = i;
               return true;
            }
         }

         for (int i = 0; i < ChatMacroRunner.PRESETS.length; i++) {
            int[] r = this.automaticPresetRect(i, cx, cy, cw);
            if (lx >= r[0] && lx < r[0] + r[2] && ly >= r[1] && ly < r[1] + r[3]) {
               this.macroDragging = true;
               this.macroDragCommand = ChatMacroRunner.PRESETS[i].command();
               this.macroDragFromSequence = -1;
               this.macroHoverInsert = this.sequenceInsertIndex(lx, ly);
               return true;
            }
         }

         return false;
      }

      private void finishMacroDrag(int lx, int ly, ScannerConfig config) {
         String command = this.macroDragCommand;
         int from = this.macroDragFromSequence;
         this.macroDragging = false;
         this.macroDragCommand = null;
         this.macroDragFromSequence = -1;
         this.macroHoverInsert = -1;
         if (command == null || config == null) {
            return;
         }

         List<String> sequence = this.ensureMacroSequence(config);
         int insert = this.sequenceInsertIndex(lx, ly);
         if (from >= 0) {
            if (from >= 0 && from < sequence.size()) {
               sequence.remove(from);
            }

            if (insert < 0) {
               config.save();
               return;
            }

            if (insert > from) {
               insert--;
            }

            insert = Math.max(0, Math.min(sequence.size(), insert));
            sequence.add(insert, command);
            config.save();
            return;
         }

         if (insert < 0) {
            sequence.add(command);
            config.save();
            return;
         }

         insert = Math.max(0, Math.min(sequence.size(), insert));
         sequence.add(insert, command);
         config.save();
      }

      private void drawAutomaticPage(
         GuiGraphicsExtractor context,
         ScannerConfig config,
         int bx,
         int by,
         int cx,
         int cy,
         int cw,
         int lmx,
         int lmy,
         int btnBg,
         int btnHover
      ) {
         List<String> sequence = this.ensureMacroSequence(config);
         context.text(this.font, "Presets — drag onto the board", bx + cx, by + cy + 20, this.TEXT_MUTED, false);

         for (int i = 0; i < ChatMacroRunner.PRESETS.length; i++) {
            ChatMacroRunner.ChatMacroPreset preset = ChatMacroRunner.PRESETS[i];
            int[] r = this.automaticPresetRect(i, cx, cy, cw);
            boolean hover = lmx >= r[0] && lmx < r[0] + r[2] && lmy >= r[1] && lmy < r[1] + r[3];
            int fill = hover ? btnHover : btnBg;
            context.fill(bx + r[0] - 1, by + r[1] - 1, bx + r[0] + r[2] + 1, by + r[1] + r[3] + 1, 0x887EB6E0);
            context.fill(bx + r[0], by + r[1], bx + r[0] + r[2], by + r[1] + r[3], fill);
            context.text(this.font, preset.label(), bx + r[0] + 8, by + r[1] + 8, this.TEXT_WHITE, false);
         }

         int[] board = this.automaticBoardRect(cx, cy, cw);
         boolean overBoard = lmx >= board[0] && lmx < board[0] + board[2] && lmy >= board[1] && lmy < board[1] + board[3];
         context.fill(bx + board[0] - 1, by + board[1] - 1, bx + board[0] + board[2] + 1, by + board[1] + board[3] + 1, overBoard && this.macroDragging ? 0xFF4A8FD4 : 0xAA7EB6E0);
         context.fill(bx + board[0], by + board[1], bx + board[0] + board[2], by + board[1] + board[3], this.darkGui ? 0x66101620 : 0x55F8FBFF);
         context.text(this.font, "Sequence board", bx + board[0] + 8, by + board[1] - 12, this.TEXT_MUTED, false);
         if (sequence.isEmpty() && !this.macroDragging) {
            context.text(this.font, "Drop warp blocks here. They run in order, 5s apart, looping.", bx + board[0] + 12, by + board[1] + 16, -8355712, false);
         }

         int drawIndex = 0;
         int insert = this.macroDragging ? this.sequenceInsertIndex(lmx, lmy) : -1;
         for (int i = 0; i < sequence.size(); i++) {
            if (this.macroDragging && this.macroDragFromSequence == i) {
               continue;
            }

            if (insert == drawIndex) {
               context.fill(bx + board[0] + 10, by + board[1] + 12 + drawIndex * 34 - 3, bx + board[0] + board[2] - 10, by + board[1] + 12 + drawIndex * 34, 0xFF6EC1FF);
            }

            int[] r = this.automaticBlockRect(drawIndex, cx, cy, cw);
            boolean hover = lmx >= r[0] && lmx < r[0] + r[2] && lmy >= r[1] && lmy < r[1] + r[3];
            context.fill(bx + r[0] - 1, by + r[1] - 1, bx + r[0] + r[2] + 1, by + r[1] + r[3] + 1, 0x887EB6E0);
            context.fill(bx + r[0], by + r[1], bx + r[0] + r[2], by + r[1] + r[3], hover ? btnHover : (this.darkGui ? 0xCC1C1C24 : 0x88FFFFFF));
            context.fill(bx + r[0], by + r[1], bx + r[0] + 3, by + r[1] + r[3], 0xFF4A8FD4);
            String label = (drawIndex + 1) + "  " + ChatMacroRunner.labelForCommand(sequence.get(i)) + "  " + sequence.get(i);
            context.text(this.font, label, bx + r[0] + 10, by + r[1] + 9, this.TEXT_WHITE, false);
            context.text(this.font, "x", bx + r[0] + r[2] - 14, by + r[1] + 9, hover ? -43691 : this.TEXT_MUTED, false);
            drawIndex++;
         }

         if (insert == drawIndex && this.macroDragging) {
            context.fill(bx + board[0] + 10, by + board[1] + 12 + drawIndex * 34 - 3, bx + board[0] + board[2] - 10, by + board[1] + 12 + drawIndex * 34, 0xFF6EC1FF);
         }

         String[] btnLabels = new String[]{
            ChatMacroRunner.isRunning() ? "Restart" : "Start loop",
            "Stop",
            "Clear"
         };
         for (int i = 0; i < 3; i++) {
            int[] r = this.automaticButtonRect(i, cx, cw);
            boolean hover = lmx >= r[0] && lmx < r[0] + r[2] && lmy >= r[1] && lmy < r[1] + r[3];
            int bg = i == 0 ? (hover ? 0xFF2F6F4A : 0xFF245A3C) : (hover ? btnHover : btnBg);
            context.fill(bx + r[0] - 1, by + r[1] - 1, bx + r[0] + r[2] + 1, by + r[1] + r[3] + 1, 0x887EB6E0);
            context.fill(bx + r[0], by + r[1], bx + r[0] + r[2], by + r[1] + r[3], bg);
            context.text(this.font, btnLabels[i], bx + r[0] + 10, by + r[1] + 7, i == 0 ? -1 : this.TEXT_WHITE, false);
         }

         context.text(this.font, ChatMacroRunner.statusLine(), bx + cx, by + this.layoutH - 108, ChatMacroRunner.isRunning() ? -256 : this.TEXT_MUTED, false);
         context.text(this.font, "ESC cancels anytime. Commands are sent in chat (Hypixel-allowed).", bx + cx, by + this.layoutH - 96, -8355712, false);
         if (this.macroDragging && this.macroDragCommand != null) {
            String ghost = ChatMacroRunner.labelForCommand(this.macroDragCommand);
            int gw = this.font.width(ghost) + 16;
            context.fill(bx + lmx + 8, by + lmy + 8, bx + lmx + 8 + gw, by + lmy + 30, this.darkGui ? 0xEE161C28 : 0xEEF8FBFF);
            context.fill(bx + lmx + 7, by + lmy + 7, bx + lmx + 9 + gw, by + lmy + 31, 0x884A8FD4);
            context.text(this.font, ghost, bx + lmx + 16, by + lmy + 14, this.TEXT_WHITE, false);
         }
      }

      private void applyWebhookUrl(ScannerConfig config) {
         if (config == null) {
            return;
         }

         config.discordWebhookUrl = this.webhookUrlInput.toString().trim();
         config.save();
      }

      private boolean handleWebhookClick(int lx, int ly, int cx, int cy, int cw, ScannerConfig config) {
         int y = cy + 40;
         if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
            config.discordWebhookEnabled = !config.discordWebhookEnabled;
            config.save();
            return true;
         }

         y += 28;
         if (ly >= y && ly < y + 22 && lx >= cx && lx < cx + cw) {
            this.webhookUrlFocused = true;
            if (this.webhookUrlInput.length() == 0 && config.discordWebhookUrl != null) {
               this.webhookUrlInput.append(config.discordWebhookUrl);
            }

            return true;
         }

         if (this.webhookUrlFocused) {
            this.webhookUrlFocused = false;
            this.applyWebhookUrl(config);
         }

         y += 26;
         if (lx >= cx && lx < cx + 90 && ly >= y && ly < y + 22) {
            String url = this.webhookUrlInput.length() > 0 ? this.webhookUrlInput.toString().trim() : config.discordWebhookUrl;
            this.webhookTestStatus = "Sending…";
            DiscordWebhook.sendTest(url, ok -> {
               Minecraft client = this.minecraft;
               if (client != null) {
                  client.execute(() -> this.webhookTestStatus = ok ? "Sent" : "Failed");
               } else {
                  this.webhookTestStatus = ok ? "Sent" : "Failed";
               }
            });
            return true;
         }

         y += 32;
         y += 16;
         boolean[] flags = new boolean[]{
            config.webhookNotifyFairy,
            config.webhookNotifyOgFairy,
            config.webhookNotifyCrystal,
            config.webhookNotifyExotic,
            config.webhookNotifySeymour,
            config.webhookNotifySkins,
            config.webhookNotifyGlitched,
            config.webhookNotifyBleached,
            config.webhookNotifyOnline
         };
         for (int i = 0; i < flags.length; i++) {
            int rowY = y + i * 22;
            if (lx >= cx && lx < cx + cw && ly >= rowY && ly < rowY + 22) {
               switch (i) {
                  case 0 -> config.webhookNotifyFairy = !config.webhookNotifyFairy;
                  case 1 -> config.webhookNotifyOgFairy = !config.webhookNotifyOgFairy;
                  case 2 -> config.webhookNotifyCrystal = !config.webhookNotifyCrystal;
                  case 3 -> config.webhookNotifyExotic = !config.webhookNotifyExotic;
                  case 4 -> config.webhookNotifySeymour = !config.webhookNotifySeymour;
                  case 5 -> config.webhookNotifySkins = !config.webhookNotifySkins;
                  case 6 -> config.webhookNotifyGlitched = !config.webhookNotifyGlitched;
                  case 7 -> config.webhookNotifyBleached = !config.webhookNotifyBleached;
                  case 8 -> config.webhookNotifyOnline = !config.webhookNotifyOnline;
               }

               config.save();
               return true;
            }
         }

         return false;
      }

      private boolean handleNotifierClick(int lx, int ly, int cx, int cy, int cw, ScannerConfig config) {
         if (config.notifierNames == null) {
            config.notifierNames = new ArrayList<>();
         }

         int y = cy + 40;
         if (lx >= cx && lx < cx + cw && ly >= y && ly < y + 22) {
            config.notifierEnabled = !config.notifierEnabled;
            config.save();
            return true;
         }

         y += 28;
         if (lx >= cx && lx < cx + 28 && ly >= y && ly < y + 22) {
            config.notifierIntervalSeconds = Math.max(15, config.notifierIntervalSeconds - 15);
            config.save();
            return true;
         }

         if (lx >= cx + 160 && lx < cx + 188 && ly >= y && ly < y + 22) {
            config.notifierIntervalSeconds = Math.min(600, config.notifierIntervalSeconds + 15);
            config.save();
            return true;
         }

         y += 28;
         int inputW = Math.min(220, cw - 90);
         if (lx >= cx && lx < cx + inputW && ly >= y && ly < y + 22) {
            this.notifierNameFocused = true;
            return true;
         }

         if (lx >= cx + inputW + 8 && lx < cx + inputW + 78 && ly >= y && ly < y + 22) {
            String name = this.notifierNameInput.toString().trim();
            boolean exists = false;
            if (name.length() >= 2 && name.length() <= 16) {
               for (String existing : config.notifierNames) {
                  if (existing != null && existing.equalsIgnoreCase(name)) {
                     exists = true;
                     break;
                  }
               }

               if (!exists) {
                  config.notifierNames.add(name);
                  config.save();
                  this.notifierNameInput.setLength(0);
               }
            }

            this.notifierNameFocused = false;
            return true;
         }

         if (lx >= cx + inputW + 86 && lx < cx + inputW + 176 && ly >= y && ly < y + 22) {
            OnlineNotifier.checkNow(this.minecraft);
            return true;
         }

         this.notifierNameFocused = false;
         y += 30;
         for (int i = 0; i < config.notifierNames.size(); i++) {
            int removeX = cx + cw - 70;
            if (lx >= removeX && lx < removeX + 60 && ly >= y && ly < y + 22) {
               config.notifierNames.remove(i);
               config.save();
               return true;
            }

            y += 22;
            if (y > this.layoutH - 60) {
               break;
            }
         }

         return false;
      }

      private boolean handleHitsClick(int lx, int ly, int cx, int cy, int cw, ScannerConfig config) {
         int y = cy + 40;
         if (lx >= cx && lx < cx + 90 && ly >= y && ly < y + 22) {
            HitCache.clear();
            this.hitsScroll = 0;
            return true;
         }

         y += 28;
         java.util.List<HitCache.Hit> hits = HitCache.snapshot();
         int rowH = 16;
         int start = Math.max(0, this.hitsScroll / rowH);
         int vis = Math.max(1, (this.layoutH - y - 50) / rowH);
         for (int i = start; i < Math.min(hits.size(), start + vis); i++) {
            int rowY = y + (i - start) * rowH;
            if (ly >= rowY && ly < rowY + rowH && lx >= cx && lx < cx + cw) {
               HitCache.Hit hit = hits.get(i);
               if (this.minecraft != null && hit.username != null) {
                  this.minecraft.keyboardHandler.setClipboard(hit.username);
                  if (this.minecraft.player != null) {
                     this.minecraft.player.sendSystemMessage(Component.literal("§aCopied §f" + hit.username));
                  }
               }

               return true;
            }
         }

         return false;
      }

      private void drawWebhookPage(
         GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover
      ) {
         int y = cy + 40;
         this.drawToggle(context, bx, by, cx, y, cw, "Enable Discord webhook", config.discordWebhookEnabled, lmx, lmy);
         y += 28;
         String shown = this.webhookUrlFocused
            ? this.webhookUrlInput.toString()
            : (config.discordWebhookUrl == null || config.discordWebhookUrl.isBlank() ? "Paste webhook URL" : config.discordWebhookUrl);
         context.fill(bx + cx, by + y, bx + cx + cw, by + y + 22, this.webhookUrlFocused ? btnHover : btnBg);
         context.text(
            this.font,
            this.fitText(shown, cw - 12),
            bx + cx + 6,
            by + y + 6,
            this.webhookUrlFocused || config.discordWebhookUrl != null && !config.discordWebhookUrl.isBlank() ? TEXT_WHITE : TEXT_MUTED,
            false
         );
         y += 26;
         boolean testHover = lmx >= cx && lmx < cx + 90 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx, by + y, bx + cx + 90, by + y + 22, testHover ? btnHover : btnBg);
         context.text(this.font, "Test", bx + cx + 30, by + y + 6, 0xFF2F7CC8, false);
         if (this.webhookTestStatus != null && !this.webhookTestStatus.isEmpty()) {
            context.text(this.font, this.webhookTestStatus, bx + cx + 100, by + y + 6, TEXT_MUTED, false);
         }

         y += 32;
         context.text(this.font, "Ping Discord when Automatic is running and a scan finds:", bx + cx, by + y, TEXT_MUTED, false);
         y += 16;
         this.drawToggle(context, bx, by, cx, y, cw, "Fairy", config.webhookNotifyFairy, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "OG Fairy", config.webhookNotifyOgFairy, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Crystal", config.webhookNotifyCrystal, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Exotic", config.webhookNotifyExotic, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Seymour", config.webhookNotifySeymour, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Skins", config.webhookNotifySkins, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Glitched", config.webhookNotifyGlitched, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Bleached", config.webhookNotifyBleached, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Watched player comes online", config.webhookNotifyOnline, lmx, lmy);
      }

      private void drawNotifierPage(
         GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover
      ) {
         if (config.notifierNames == null) {
            config.notifierNames = new ArrayList<>();
         }

         int y = cy + 40;
         this.drawToggle(context, bx, by, cx, y, cw, "Enable online notifier", config.notifierEnabled, lmx, lmy);
         y += 28;
         context.text(this.font, "Check every", bx + cx, by + y + 6, TEXT_MUTED, false);
         boolean minusHover = lmx >= cx && lmx < cx + 28 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx, by + y, bx + cx + 28, by + y + 22, minusHover ? btnHover : btnBg);
         context.text(this.font, "-", bx + cx + 10, by + y + 6, TEXT_WHITE, false);
         context.text(this.font, config.notifierIntervalSeconds + "s", bx + cx + 40, by + y + 6, 0xFF2F7CC8, false);
         boolean plusHover = lmx >= cx + 160 && lmx < cx + 188 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx + 160, by + y, bx + cx + 188, by + y + 22, plusHover ? btnHover : btnBg);
         context.text(this.font, "+", bx + cx + 170, by + y + 6, TEXT_WHITE, false);
         y += 28;
         int inputW = Math.min(220, cw - 90);
         String nameShown = this.notifierNameInput.length() > 0 ? this.notifierNameInput.toString() : "Add username";
         context.fill(bx + cx, by + y, bx + cx + inputW, by + y + 22, this.notifierNameFocused ? btnHover : btnBg);
         context.text(this.font, nameShown, bx + cx + 6, by + y + 6, this.notifierNameInput.length() > 0 ? TEXT_WHITE : TEXT_MUTED, false);
         boolean addHover = lmx >= cx + inputW + 8 && lmx < cx + inputW + 78 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx + inputW + 8, by + y, bx + cx + inputW + 78, by + y + 22, addHover ? btnHover : btnBg);
         context.text(this.font, "Add", bx + cx + inputW + 28, by + y + 6, 0xFF2F7CC8, false);
         boolean nowHover = lmx >= cx + inputW + 86 && lmx < cx + inputW + 176 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx + inputW + 86, by + y, bx + cx + inputW + 176, by + y + 22, nowHover ? btnHover : btnBg);
         context.text(this.font, "Check now", bx + cx + inputW + 96, by + y + 6, TEXT_WHITE, false);
         y += 30;
         for (int i = 0; i < config.notifierNames.size(); i++) {
            String n = config.notifierNames.get(i);
            int removeX = cx + cw - 70;
            context.text(this.font, n, bx + cx, by + y + 6, TEXT_WHITE, false);
            context.text(this.font, OnlineNotifier.statusOf(n), bx + cx + 130, by + y + 6, TEXT_MUTED, false);
            boolean removeHover = lmx >= removeX && lmx < removeX + 60 && lmy >= y && lmy < y + 22;
            context.fill(bx + removeX, by + y, bx + removeX + 60, by + y + 22, removeHover ? btnHover : btnBg);
            context.text(this.font, "Remove", bx + removeX + 8, by + y + 6, 0xFFB05050, false);
            y += 22;
            if (y > this.layoutH - 60) {
               break;
            }
         }
      }

      private void drawHitsPage(
         GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover
      ) {
         int y = cy + 40;
         java.util.List<HitCache.Hit> hits = HitCache.snapshot();
         boolean clearHover = lmx >= cx && lmx < cx + 90 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx, by + y, bx + cx + 90, by + y + 22, clearHover ? btnHover : btnBg);
         context.text(this.font, "Clear", bx + cx + 28, by + y + 6, TEXT_WHITE, false);
         context.text(this.font, hits.size() + " hit(s)  ·  click a name to copy", bx + cx + 100, by + y + 6, TEXT_MUTED, false);
         y += 28;
         int rowH = 16;
         int vis = Math.max(1, (this.layoutH - y - 50) / rowH);
         int maxScroll = Math.max(0, (hits.size() - vis) * rowH);
         this.hitsScroll = Math.max(0, Math.min(maxScroll, this.hitsScroll));
         int start = this.hitsScroll / rowH;
         for (int i = start; i < Math.min(hits.size(), start + vis); i++) {
            HitCache.Hit hit = hits.get(i);
            int rowY = y + (i - start) * rowH;
            boolean hover = lmx >= cx && lmx < cx + cw && lmy >= rowY && lmy < rowY + rowH;
            if (hover) {
               context.fill(bx + cx, by + rowY, bx + cx + cw, by + rowY + rowH, 0x332F7CC8);
            }

            String when = ScannerConfig.getKeyActiveDuration(hit.at);
            String line = hit.username + "  lv" + hit.level + "  " + (hit.summary == null ? "" : hit.summary) + "  " + when;
            context.text(this.font, this.fitText(line, cw - 8), bx + cx + 2, by + rowY + 3, TEXT_WHITE, false);
         }
      }

      private void drawToggle(GuiGraphicsExtractor context, int bx, int by, int x, int y, int w, String label, boolean on, int mouseX, int mouseY) {
         boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 22;
         if (hover) {
            context.fill(bx + x, by + y, bx + x + w, by + y + 22, 1076899888);
         }

         context.text(this.font, (on ? "§a[ON] " : "§c[OFF] ") + "§r" + label, bx + x + 4, by + y + 4, this.TEXT_WHITE, false);
      }

      private void drawSegmentedModeRow(
         GuiGraphicsExtractor context,
         int bx,
         int by,
         int x,
         int y,
         int segW,
         int segGap,
         int lmx,
         int lmy,
         String label,
         String value,
         String a,
         String b,
         String c,
         int btnBg,
         int btnHover
      ) {
         context.text(this.font, label, bx + x, by + y + 4, this.TEXT_WHITE, false);
         String current = value == null ? "" : value.toUpperCase(Locale.ROOT);
         String[] vals = new String[]{a, b, c};
         int startX = x + 190;

         for (int i = 0; i < vals.length; i++) {
            int sx = startX + i * (segW + segGap);
            boolean selected = vals[i].equals(current);
            boolean hover = lmx >= sx && lmx < sx + segW && lmy >= y && lmy < y + 22;
            int bg = selected ? 1344299168 : (hover ? btnHover : btnBg);
            context.fill(bx + sx, by + y, bx + sx + segW, by + y + 22, bg);
            context.text(this.font, vals[i].replace('_', ' '), bx + sx + 6, by + y + 4, selected ? -5713665 : this.TEXT_WHITE, false);
         }
      }

      private String fitText(String text, int maxWidth) {
         if (text != null && !text.isEmpty() && maxWidth > 0) {
            if (this.font.width(text) <= maxWidth) {
               return text;
            }

            String base = text;

            while (base.length() > 1 && this.font.width(base + "...") > maxWidth) {
               base = base.substring(0, base.length() - 1);
            }

            return base + "...";
         } else {
            return "";
         }
      }

      private void drawAhFilterChip(GuiGraphicsExtractor context, int bx, int by, int x, int y, int w, String label, int color, boolean enabled, int lmx, int lmy) {
         boolean hover = lmx >= x && lmx < x + w && lmy >= y && lmy < y + 22;
         int fill = enabled ? (hover ? 1744830464 | color : 1426063360 | color) : (hover ? 1077294646 : 807674916);
         int border = enabled ? 0xFF000000 | color : (hover ? -9145228 : -11316397);
         int left = bx + x;
         int top = by + y;
         int right = left + w;
         int bottom = top + 22;
         context.fill(left, top, right, bottom, fill);
         context.fill(left, top, right, top + 1, border);
         context.fill(left, bottom - 1, right, bottom, border);
         context.fill(left, top, left + 1, bottom, border);
         context.fill(right - 1, top, right, bottom, border);
         String text = this.fitText((enabled ? "ON " : "OFF ") + label, w - 8);
         context.text(this.font, text, left + 4, top + 6, enabled ? this.TEXT_WHITE : this.TEXT_MUTED, false);
      }

      private void drawSpecialPage(GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover) {
         int y = cy + 40;
         String[] labels = new String[]{
            "Crystal scanning",
            "Bleached scanning",
            "OG Fairy",
            "Fairy scanning",
            "Exotic scanning",
            "New Year Cakes",
            "Legacy Reforge",
            "Ghost Reforge",
            "Valuable items",
            "Anniversary hats (crab/sloth/balloon)",
            "Item tracers",
            "Player overlay",
            "Cosmetic skins (above Xm)",
            "Specific hex scanning"
         };
         boolean[] values = new boolean[]{
            config.crystalScanningEnabled,
            config.bleachedScanningEnabled,
            config.ogFairyScanningEnabled,
            config.fairyScanningEnabled,
            config.exoticScanningEnabled,
            config.newYearCakeEnabled,
            config.legacyReforgeEnabled,
            config.ghostReforgeEnabled,
            config.valuableItemsEnabled,
            config.scanAnniversaryHats,
            config.renderItemTracers,
            config.showPlayerOverlay,
            config.scanCosmeticSkinsEnabled,
            config.specificHexScanningEnabled
         };

         for (int i = 0; i < labels.length; i++) {
            this.drawToggle(context, bx, by, cx, y + i * 22, cw, labels[i], values[i], lmx, lmy);
         }

         y += labels.length * 22 + 4;
         int inputX = cx + 220;
         int alpha = this.getGuiAlpha(config);
         context.text(this.font, "Specific hexes to flag (comma/space):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.specificHexScanFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String specificHexDisplay = this.specificHexScanFocused
            ? this.specificHexScanInput.toString()
            : (config.specificHexList == null ? "" : config.specificHexList);
         if (specificHexDisplay.isEmpty()) {
            context.text(this.font, "191919, 16FBF8, 901C0A", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = this.fitText(specificHexDisplay, 250);
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.specificHexScanFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         y += 26;
         this.drawCakeYearsEditor(context, config, bx, by, cx, y, cw, lmx, lmy, btnBg, btnHover, alpha);
      }

      private boolean hit(int[] box, int lx, int ly) {
         return box != null && lx >= box[0] && lx < box[0] + box[2] && ly >= box[1] && ly < box[1] + box[3];
      }

      private void drawCakeYearsEditor(
         GuiGraphicsExtractor context,
         ScannerConfig config,
         int bx,
         int by,
         int cx,
         int y,
         int cw,
         int lmx,
         int lmy,
         int btnBg,
         int btnHover,
         int alpha
      ) {
         this.cakeChipHitboxes.clear();
         context.text(this.font, "Cake years — click a year to remove", bx + cx, by + y, this.TEXT_WHITE, false);
         y += 16;
         int x = cx;
         int maxX = cx + cw;
         List<Integer> years = config.newYearCakeSpecificYears == null ? List.<Integer>of() : config.newYearCakeSpecificYears;
         if (years.isEmpty()) {
            context.text(this.font, "No years selected", bx + cx, by + y + 4, this.TEXT_MUTED, false);
            y += 22;
         } else {
            for (Integer year : years) {
               if (year == null) {
                  continue;
               }

               String label = year + " ×";
               int chipW = Math.max(32, this.font.width(label) + 12);
               if (x + chipW > maxX) {
                  x = cx;
                  y += 22;
               }

               boolean hover = lmx >= x && lmx < x + chipW && lmy >= y && lmy < y + 18;
               this.cakeChipHitboxes.add(new int[]{x, y, chipW, 18, year});
               context.fill(bx + x, by + y, bx + x + chipW, by + y + 18, hover ? 0xFF3A2428 : btnBg);
               context.text(this.font, label, bx + x + 6, by + y + 5, hover ? 0xFFD08080 : this.TEXT_WHITE, false);
               x += chipW + 4;
            }

            y += 22;
         }

         int addW = 64;
         int btnW = 52;
         int defW = 72;
         int clearW = 52;
         this.cakeAddBoxHit = new int[]{cx, y, addW, 22};
         this.cakeAddBtnHit = new int[]{cx + addW + 6, y, btnW, 22};
         this.cakeDefaultsHit = new int[]{cx + addW + 6 + btnW + 6, y, defW, 22};
         this.cakeClearHit = new int[]{cx + addW + 6 + btnW + 6 + defW + 6, y, clearW, 22};
         boolean addHover = this.hit(this.cakeAddBoxHit, lmx, lmy);
         context.fill(bx + cx, by + y, bx + cx + addW, by + y + 22, this.cakeSpecificYearsFocused || addHover ? btnHover : btnBg);
         context.fill(bx + cx + 1, by + y + 1, bx + cx + addW - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String addDisplay = this.cakeSpecificYearsFocused ? this.cakeSpecificYearsInput.toString() : "";
         if (addDisplay.isEmpty()) {
            context.text(this.font, this.cakeSpecificYearsFocused ? "_" : "year", bx + cx + 6, by + y + 6, this.TEXT_MUTED, false);
         } else {
            context.text(this.font, addDisplay, bx + cx + 6, by + y + 6, this.TEXT_WHITE, false);
            if (this.cakeSpecificYearsFocused) {
               context.text(this.font, "_", bx + cx + 6 + this.font.width(addDisplay), by + y + 6, this.TEXT_WHITE, false);
            }
         }

         boolean addBtnHover = this.hit(this.cakeAddBtnHit, lmx, lmy);
         context.fill(bx + this.cakeAddBtnHit[0], by + y, bx + this.cakeAddBtnHit[0] + btnW, by + y + 22, addBtnHover ? btnHover : btnBg);
         context.text(this.font, "Add", bx + this.cakeAddBtnHit[0] + 14, by + y + 6, 0xFF7EB6E0, false);
         boolean defHover = this.hit(this.cakeDefaultsHit, lmx, lmy);
         context.fill(bx + this.cakeDefaultsHit[0], by + y, bx + this.cakeDefaultsHit[0] + defW, by + y + 22, defHover ? btnHover : btnBg);
         context.text(this.font, "Defaults", bx + this.cakeDefaultsHit[0] + 8, by + y + 6, 0xFF2F7CC8, false);
         boolean clearHover = this.hit(this.cakeClearHit, lmx, lmy);
         context.fill(bx + this.cakeClearHit[0], by + y, bx + this.cakeClearHit[0] + clearW, by + y + 22, clearHover ? 0xFF3A2428 : btnBg);
         context.text(this.font, "Clear", bx + this.cakeClearHit[0] + 10, by + y + 6, 0xFFD08080, false);
         y += 24;
         context.text(this.font, "Defaults: 1–25, 67, 69, 100, 200, 300, 400, 500", bx + cx, by + y, this.TEXT_MUTED, false);
      }

      private void drawSkinsPage(GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover) {
         int y = cy + 40;
         this.drawToggle(context, bx, by, cx, y, cw, "Enable cosmetic skin scanning", config.scanCosmeticSkinsEnabled, lmx, lmy);
         y += 26;
         int inputX = cx + 220;
         context.text(this.font, "Min value (millions):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.text(this.font, "← click to type (no upper limit)", bx + cx, by + y + 14, this.TEXT_MUTED, false);
         boolean skinInputHover = lmx >= inputX && lmx < inputX + 120 && lmy >= y && lmy < y + 22;
         context.fill(bx + inputX, by + y, bx + inputX + 120, by + y + 22, this.minSkinValueFocused ? btnHover : (skinInputHover ? btnHover : btnBg));
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 120 - 1, by + y + 22 - 1, this.getGuiAlpha(config) << 24 | 1710618);
         String skinValDisplay = this.minSkinValueFocused
            ? (this.minSkinValueInput.length() > 0 ? this.minSkinValueInput.toString() : "")
            : String.valueOf(
               config.minCosmeticSkinValueMillions == (int)config.minCosmeticSkinValueMillions
                  ? (int)config.minCosmeticSkinValueMillions
                  : String.format("%.1f", config.minCosmeticSkinValueMillions)
            );
         if (this.minSkinValueFocused && this.minSkinValueInput.length() == 0) {
            context.text(this.font, "type here...", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            context.text(this.font, skinValDisplay, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
         }

         if (this.minSkinValueFocused) {
            context.text(this.font, "_", bx + inputX + 4 + this.font.width(skinValDisplay), by + y + 4, this.TEXT_WHITE, false);
         }

         y += 46;
         context.text(this.font, "Scan mode:", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         y += 26;
         int segW = Math.max(1, (cw - 4) / 3);
         String mode = config.cosmeticSkinScanMode != null ? config.cosmeticSkinScanMode : "BOTH";
         int accentRgb = this.resolveGuiAccentRgb(config);
         int selectedBg = 1342177280 | accentRgb;
         int selectedBorder = 0xFF000000 | accentRgb;

         for (int i = 0; i < SKIN_SCAN_MODES.length; i++) {
            int sx = cx + i * (segW + 2);
            int sw = i == SKIN_SCAN_MODES.length - 1 ? cx + cw - sx : segW;
            boolean sel = SKIN_SCAN_MODES[i].equals(mode);
            boolean hover = lmx >= sx && lmx < sx + sw && lmy >= y && lmy < y + 22;
            if (sel) {
               context.fill(bx + sx, by + y, bx + sx + sw, by + y + 22, selectedBg);
               context.fill(bx + sx, by + y, bx + sx + sw, by + y + 2, selectedBorder);
               context.fill(bx + sx, by + y + 22 - 2, bx + sx + sw, by + y + 22, selectedBorder);
               context.fill(bx + sx, by + y, bx + sx + 2, by + y + 22, selectedBorder);
               context.fill(bx + sx + sw - 2, by + y, bx + sx + sw, by + y + 22, selectedBorder);
            } else {
               context.fill(bx + sx, by + y, bx + sx + sw, by + y + 22, hover ? btnHover : btnBg);
            }

            String lbl = SKIN_SCAN_MODES[i].equals("BOTH") ? "Both" : (SKIN_SCAN_MODES[i].equals("APPLIED_ONLY") ? "Applied only" : "Unapplied only");
            context.text(this.font, lbl, bx + sx + 6, by + y + 4, this.TEXT_WHITE, false);
         }
      }

      private void drawNumberRow(
         GuiGraphicsExtractor context,
         ScannerConfig config,
         int bx,
         int by,
         int x,
         int y,
         int w,
         String label,
         int value,
         int fieldId,
         int lmx,
         int lmy,
         int btnBg,
         int btnHover
      ) {
         context.text(this.font, label, bx + x, by + y + 4, this.TEXT_WHITE, false);
         int inputX = this.getNumberInputX(x, w);
         boolean hover = lmx >= inputX && lmx < inputX + 120 && lmy >= y && lmy < y + 22;
         boolean focused = this.numberInputField == fieldId;
         context.fill(bx + inputX, by + y, bx + inputX + 120, by + y + 22, focused ? btnHover : (hover ? btnHover : btnBg));
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 120 - 1, by + y + 22 - 1, this.getGuiAlpha(config) << 24 | 1710618);
         String shown = focused ? this.numberInput.toString() : String.valueOf(value);
         if (shown.isEmpty()) {
            shown = "type...";
         }

         context.text(this.font, shown, bx + inputX + 4, by + y + 4, focused && this.numberInput.length() == 0 ? this.TEXT_MUTED : this.TEXT_WHITE, false);
         if (focused) {
            context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
         }
      }

      private void drawAdvancedPage(GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover) {
         int y = cy + 40;
         context.text(this.font, "Type level numbers — click each box below to type, then Enter", bx + cx, by + y, this.TEXT_WHITE, false);
         y += 26;
         int inputX = cx + 220;
         context.text(this.font, "Min SkyBlock level:", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.text(this.font, "← click to type (0–1000)", bx + cx, by + y + 14, this.TEXT_MUTED, false);
         boolean minHover = lmx >= inputX && lmx < inputX + 120 && lmy >= y && lmy < y + 22;
         context.fill(bx + inputX, by + y, bx + inputX + 120, by + y + 22, this.minLevelFocused ? btnHover : (minHover ? btnHover : btnBg));
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 120 - 1, by + y + 22 - 1, this.getGuiAlpha(config) << 24 | 1710618);
         String minDisplay = this.minLevelFocused
            ? (this.minLevelInput.length() > 0 ? this.minLevelInput.toString() : "")
            : String.valueOf((int)config.minSkyblockLevel);
         if (this.minLevelFocused && this.minLevelInput.length() == 0) {
            context.text(this.font, "type here...", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            context.text(this.font, minDisplay, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
         }

         if (this.minLevelFocused) {
            context.text(this.font, "_", bx + inputX + 4 + this.font.width(minDisplay), by + y + 4, this.TEXT_WHITE, false);
         }

         y += 24;
         context.text(this.font, "Max level cap (0 = no cap):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.text(this.font, "← click to type (0–1000)", bx + cx, by + y + 14, this.TEXT_MUTED, false);
         boolean maxHover = lmx >= inputX && lmx < inputX + 120 && lmy >= y && lmy < y + 22;
         context.fill(bx + inputX, by + y, bx + inputX + 120, by + y + 22, this.maxLevelFocused ? btnHover : (maxHover ? btnHover : btnBg));
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 120 - 1, by + y + 22 - 1, this.getGuiAlpha(config) << 24 | 1710618);
         String maxDisplay = this.maxLevelFocused
            ? (this.maxLevelInput.length() > 0 ? this.maxLevelInput.toString() : "")
            : String.valueOf((int)config.skyblockLevelCap);
         if (this.maxLevelFocused && this.maxLevelInput.length() == 0) {
            context.text(this.font, "type here...", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            context.text(this.font, maxDisplay, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
         }

         if (this.maxLevelFocused) {
            context.text(this.font, "_", bx + inputX + 4 + this.font.width(maxDisplay), by + y + 4, this.TEXT_WHITE, false);
         }

         y += 26;
         y += 30;
         this.drawToggle(context, bx, by, cx, y, cw, "Scan low-level players", config.scanLowLevelPlayers, lmx, lmy);
         this.drawToggle(context, bx, by, cx, y + 22, cw, "Skip API error players", config.skipApiErrorPlayers, lmx, lmy);
         this.drawToggle(context, bx, by, cx, y + 44, cw, "Soulbound items", config.scanSoulboundItems, lmx, lmy);
         this.drawToggle(context, bx, by, cx, y + 66, cw, "Coop soulbound", config.scanCoopSoulboundItems, lmx, lmy);
         this.drawToggle(context, bx, by, cx, y + 88, cw, "Ironman profiles", config.scanIronmanProfiles, lmx, lmy);
         this.drawToggle(context, bx, by, cx, y + 110, cw, "Stranded profiles", config.scanStrandedProfiles, lmx, lmy);
         this.drawToggle(context, bx, by, cx, y + 132, cw, "Require custom armor color", config.requireCustomColor, lmx, lmy);
         context.text(this.font, "Max players (0=no limit): " + config.maxPlayersToScan, bx + cx, by + y + 176, this.TEXT_MUTED, false);
         context.text(this.font, "API delay (ms): " + config.apiDelayMs, bx + cx, by + y + 198, this.TEXT_MUTED, false);
         this.drawShareEditor(context, config, bx, by, cx, y + 224, cw, lmx, lmy, btnBg, btnHover);
      }

      private void drawShareEditor(
         GuiGraphicsExtractor context,
         ScannerConfig config,
         int bx,
         int by,
         int cx,
         int y,
         int cw,
         int lmx,
         int lmy,
         int btnBg,
         int btnHover
      ) {
         context.text(this.font, "Share config — copy a code or paste a friend’s", bx + cx, by + y, this.TEXT_WHITE, false);
         y += 16;
         context.text(this.font, "Does not include API keys or webhook URL", bx + cx, by + y, this.TEXT_MUTED, false);
         y += 18;
         this.shareInputHit = new int[]{cx, y, cw, 22};
         boolean inputHover = this.hit(this.shareInputHit, lmx, lmy);
         context.fill(bx + cx, by + y, bx + cx + cw, by + y + 22, this.shareCodeFocused || inputHover ? btnHover : btnBg);
         String shown = this.shareCodeFocused || this.shareCodeInput.length() > 0 ? this.shareCodeInput.toString() : "Paste PT2- share code here";
         String clipped = this.fitText(shown, cw - 16);
         context.text(this.font, clipped, bx + cx + 6, by + y + 6, this.shareCodeInput.length() > 0 ? this.TEXT_WHITE : this.TEXT_MUTED, false);
         if (this.shareCodeFocused) {
            context.text(this.font, "_", bx + cx + 6 + this.font.width(clipped), by + y + 6, this.TEXT_WHITE, false);
         }

         y += 28;
         this.shareCopyHit = new int[]{cx, y, 132, 22};
         this.shareImportHit = new int[]{cx + 140, y, 150, 22};
         boolean copyHover = this.hit(this.shareCopyHit, lmx, lmy);
         boolean importHover = this.hit(this.shareImportHit, lmx, lmy);
         context.fill(bx + cx, by + y, bx + cx + 132, by + y + 22, copyHover ? btnHover : btnBg);
         context.text(this.font, "Copy share code", bx + cx + 10, by + y + 6, 0xFF7EB6E0, false);
         context.fill(bx + cx + 140, by + y, bx + cx + 290, by + y + 22, importHover ? btnHover : btnBg);
         context.text(this.font, "Import pasted code", bx + cx + 150, by + y + 6, 0xFF2F7CC8, false);
         if (this.shareStatus != null && !this.shareStatus.isEmpty()) {
            context.text(this.font, this.shareStatus, bx + cx, by + y + 28, this.TEXT_MUTED, false);
         }
      }

      private boolean handleCakeYearClicks(ScannerConfig config, int lx, int ly) {
         if (config == null) {
            return false;
         }

         for (int[] chip : this.cakeChipHitboxes) {
            if (this.hit(chip, lx, ly)) {
               config.removeCakeYear(chip[4]);
               config.save();
               return true;
            }
         }

         if (this.hit(this.cakeAddBoxHit, lx, ly)) {
            this.cakeSpecificYearsFocused = true;
            this.cakeSpecificYearsInput.setLength(0);
            return true;
         }

         if (this.hit(this.cakeAddBtnHit, lx, ly)) {
            this.applyCakeSpecificYearsInput(config);
            this.cakeSpecificYearsFocused = false;
            return true;
         }

         if (this.hit(this.cakeDefaultsHit, lx, ly)) {
            config.resetCakeYearsToDefaults();
            config.save();
            this.cakeSpecificYearsInput.setLength(0);
            this.cakeSpecificYearsFocused = false;
            return true;
         }

         if (this.hit(this.cakeClearHit, lx, ly)) {
            config.newYearCakeSpecificYears = new ArrayList<>();
            config.cakeYearsListMode = true;
            config.save();
            return true;
         }

         return false;
      }

      private boolean handleShareClicks(ScannerConfig config, int lx, int ly) {
         if (config == null) {
            return false;
         }

         if (this.hit(this.shareInputHit, lx, ly)) {
            this.shareCodeFocused = true;
            return true;
         }

         if (this.hit(this.shareCopyHit, lx, ly)) {
            String code = ConfigShareCodec.exportCode(config);
            if (this.minecraft != null && this.minecraft.keyboardHandler != null) {
               this.minecraft.keyboardHandler.setClipboard(code);
            }

            this.shareCodeInput.setLength(0);
            this.shareCodeInput.append(code);
            this.shareStatus = "Copied share code to clipboard";
            this.shareCodeFocused = false;
            return true;
         }

         if (this.hit(this.shareImportHit, lx, ly)) {
            String raw = this.shareCodeInput.length() > 0
               ? this.shareCodeInput.toString()
               : this.minecraft != null && this.minecraft.keyboardHandler != null ? this.minecraft.keyboardHandler.getClipboard() : "";
            String error = ConfigShareCodec.importCode(config, raw);
            this.shareStatus = error == null ? "Imported share code" : error;
            this.shareCodeFocused = false;
            return true;
         }

         if (this.shareCodeFocused) {
            this.shareCodeFocused = false;
         }

         return false;
      }

      private static int wrapPerRow(int itemW, int gap, int width) {
         return Math.max(1, (width + gap) / (itemW + gap));
      }

      private void drawHudPage(GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover) {
         int y = cy + 40;
         int anchorIdx = indexOf(HUD_ANCHORS, config.hudAnchor);
         if (anchorIdx < 0) {
            anchorIdx = 0;
         }

         context.text(this.font, "Anchor", bx + cx, by + y, this.TEXT_MUTED, false);
         y += 14;
         int anchorW = 88;
         int anchorGap = 8;
         int anchorPer = wrapPerRow(anchorW, anchorGap, cw);
         for (int i = 0; i < HUD_ANCHORS.length; i++) {
            int col = i % anchorPer;
            int row = i / anchorPer;
            int x = cx + col * (anchorW + anchorGap);
            int ty = y + row * 26;
            boolean hover = lmx >= x && lmx < x + anchorW && lmy >= ty && lmy < ty + 22;
            context.fill(bx + x, by + ty, bx + x + anchorW, by + ty + 22, hover ? btnHover : btnBg);
            context.text(this.font, HUD_ANCHORS[i].replace("_", " "), bx + x + 6, by + ty + 6, anchorIdx == i ? -7820545 : this.TEXT_WHITE, false);
         }

         y += ((HUD_ANCHORS.length + anchorPer - 1) / anchorPer) * 26 + 12;
         int themeIdx = indexOf(HUD_THEMES, config.hudBorderTheme);
         if (themeIdx < 0) {
            themeIdx = 0;
         }

         context.text(this.font, "Border theme: " + config.hudBorderTheme, bx + cx, by + y, this.TEXT_MUTED, false);
         y += 14;
         int themeW = 90;
         int themeGap = 8;
         int themePer = wrapPerRow(themeW, themeGap, cw);
         for (int i = 0; i < HUD_THEMES.length; i++) {
            int col = i % themePer;
            int row = i / themePer;
            int x = cx + col * (themeW + themeGap);
            int ty = y + row * 26;
            boolean hover = lmx >= x && lmx < x + themeW && lmy >= ty && lmy < ty + 22;
            context.fill(bx + x, by + ty, bx + x + themeW, by + ty + 22, hover ? btnHover : btnBg);
            context.text(this.font, HUD_THEMES[i], bx + x + 6, by + ty + 6, themeIdx == i ? -7820545 : this.TEXT_WHITE, false);
         }

         y += ((HUD_THEMES.length + themePer - 1) / themePer) * 26 + 12;
         this.drawToggle(context, bx, by, cx, y, cw, "Fairy hex accent on HUD / chat borders", config.hudFairyAccentEnabled, lmx, lmy);
         y += 28;
         context.text(this.font, "HUD color (CUSTOM theme)", bx + cx, by + y, this.TEXT_MUTED, false);
         y += 14;
         int rgb = config.hudColorRgb & 16777215;
         context.fill(bx + cx, by + y, bx + cx + 88, by + y + 22, this.hudColorFocused ? btnHover : btnBg);
         context.fill(bx + cx + 1, by + y + 1, bx + cx + 87, by + y + 21, 0xFF000000 | rgb);
         String hexShown = this.hudColorFocused ? this.hudColorInput.toString() : String.format("#%06X", rgb);
         context.text(this.font, hexShown, bx + cx + 6, by + y + 6, 0xFFFFFFFF, false);
         if (this.hudColorFocused) {
            context.text(this.font, "_", bx + cx + 6 + this.font.width(hexShown), by + y + 6, 0xFFFFFFFF, false);
         }

         y += 28;
         int swatchW = 18;
         int swatchGap = 6;
         int swatchPer = wrapPerRow(swatchW, swatchGap, cw);
         for (int i = 0; i < HIGHLIGHT_SWATCHES.length; i++) {
            int col = i % swatchPer;
            int row = i / swatchPer;
            int sx = cx + col * (swatchW + swatchGap);
            int sy = y + row * 24;
            boolean hover = lmx >= sx && lmx < sx + swatchW && lmy >= sy && lmy < sy + swatchW;
            int sw = HIGHLIGHT_SWATCHES[i] & 16777215;
            context.fill(bx + sx, by + sy, bx + sx + swatchW, by + sy + swatchW, hover || sw == rgb ? -7820545 : btnBg);
            context.fill(bx + sx + 2, by + sy + 2, bx + sx + swatchW - 2, by + sy + swatchW - 2, 0xFF000000 | sw);
         }

         y += ((HIGHLIGHT_SWATCHES.length + swatchPer - 1) / swatchPer) * 24 + 10;
         context.text(
            this.font,
            "Offset X: " + config.hudOffsetX + "   Offset Y: " + config.hudOffsetY + "   Scale: " + config.hudScale,
            bx + cx,
            by + y,
            this.TEXT_MUTED,
            false
         );
         y += 14;
         context.text(this.font, "Pick CUSTOM or a swatch to set the overlay border color. Drag the HUD in-game to move it.", bx + cx, by + y, this.TEXT_MUTED, false);
      }

      private int getAppearanceOpacityInputX(int cx) {
         return cx + 150;
      }

      private int getAppearanceHexInputX(int cx) {
         return cx + 158;
      }

      private int appearanceThemePerRow(int cw) {
         return wrapPerRow(100, 10, cw);
      }

      private int getAppearanceHighlightStartY(int cy, int cw) {
         int y = cy + 40;
         y += 28;
         y += 28;
         y += 16;
         int themeRows = (GUI_ACCENT_THEME_KEYS.length + this.appearanceThemePerRow(cw) - 1) / this.appearanceThemePerRow(cw);
         y += themeRows * 28 + 12;
         y += 28;
         y += 30;
         return y;
      }

      private int getAppearanceHexInputY(int cy, int cw, int row) {
         return this.getAppearanceHighlightStartY(cy, cw) + row * this.highlightRowHeight(cw) + 2;
      }

      private int highlightRowHeight(int cw) {
         int swatchPer = wrapPerRow(16, 4, cw);
         int swatchRows = (HIGHLIGHT_SWATCHES.length + swatchPer - 1) / swatchPer;
         return 22 + swatchRows * 18 + 8;
      }

      private int getNumberInputX(int x, int w) {
         return x + w - 120;
      }

      private boolean isNumberInputHit(int lx, int ly, int x, int y, int w) {
         int inputX = this.getNumberInputX(x, w);
         return lx >= inputX && lx < inputX + 120 && ly >= y && ly < y + 22;
      }

      private int getAppearanceHighlightStartY(int cy) {
         return this.getAppearanceHighlightStartY(cy, this.contentWidth());
      }

      private int getAppearanceHexInputY(int cy, int row) {
         return this.getAppearanceHexInputY(cy, this.contentWidth(), row);
      }

      private boolean isAppearanceInputFocused() {
         return this.guiOpacityFocused || this.highlightHexFocusedRow >= 0;
      }

      private void applyHudColorInput(ScannerConfig config) {
         if (config == null) {
            return;
         }

         String raw = this.hudColorInput.toString().trim().replace("#", "");
         if (raw.length() == 6) {
            try {
               config.hudColorRgb = Integer.parseInt(raw, 16) & 16777215;
               config.hudBorderTheme = "CUSTOM";
               config.save();
            } catch (NumberFormatException ignored) {
            }
         }
      }

      private void applyGuiOpacityInput(ScannerConfig config) {
         if (config != null) {
            String raw = this.guiOpacityInput.toString().trim();
            if (!raw.isEmpty()) {
               try {
                  int v = Integer.parseInt(raw);
                  config.guiAlpha = Math.max(0, Math.min(255, v));
                  config.save();
               } catch (NumberFormatException var4) {
               }
            }

            this.guiOpacityInput.setLength(0);
         }
      }

      private static String normalizeHex6(String raw) {
         if (raw == null) {
            return null;
         }

         String s = raw.trim().toUpperCase().replace("#", "");
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

      private void applyHighlightHexInput(ScannerConfig config) {
         if (config != null && this.highlightHexFocusedRow >= 0 && this.highlightHexFocusedRow < HIGHLIGHT_COLOR_LABELS.length) {
            String h = normalizeHex6(this.highlightHexInput.toString());
            if (h != null) {
               try {
                  int rgb = Integer.parseInt(h, 16);
                  this.setHighlightColorByRow(config, this.highlightHexFocusedRow, rgb);
                  config.save();
               } catch (NumberFormatException var4) {
               }
            }

            this.highlightHexInput.setLength(0);
         }
      }

      private void applyAndClearAppearanceInputs(ScannerConfig config) {
         if (this.guiOpacityFocused) {
            this.applyGuiOpacityInput(config);
         }

         if (this.highlightHexFocusedRow >= 0) {
            this.applyHighlightHexInput(config);
         }

         this.guiOpacityFocused = false;
         this.highlightHexFocusedRow = -1;
         this.highlightHexInput.setLength(0);
      }

      private int getGuiAlpha(ScannerConfig config) {
         return config == null ? 230 : Math.max(0, Math.min(255, config.guiAlpha));
      }

      private void setGuiAccentTheme(ScannerConfig config, String themeKey, int fallbackRgb) {
         if (config != null) {
            String key = themeKey == null ? "" : themeKey.trim().toUpperCase(Locale.ROOT);
            if (key.isEmpty()) {
               key = "STATIC_BLUE";
            }

            config.guiAccentTheme = key;
            config.guiAccentRgb = fallbackRgb & 16777215;
         }
      }

      private int resolveGuiAccentRgb(ScannerConfig config) {
         return PotatoTheme.guiAccentRgb(config);
      }

      private static int blendRgb(int a, int b, double t) {
         double clamped = Math.max(0.0, Math.min(1.0, t));
         int ar = a >> 16 & 0xFF;
         int ag = a >> 8 & 0xFF;
         int ab = a & 0xFF;
         int br = b >> 16 & 0xFF;
         int bg = b >> 8 & 0xFF;
         int bb = b & 0xFF;
         int rr = (int)Math.round(ar + (br - ar) * clamped);
         int rg = (int)Math.round(ag + (bg - ag) * clamped);
         int rb = (int)Math.round(ab + (bb - ab) * clamped);
         return rr << 16 | rg << 8 | rb;
      }

      private void focusNumberInput(int fieldId, int currentValue, ScannerConfig config) {
         if (this.numberInputField >= 0 && this.numberInputField != fieldId) {
            this.applyAndClearNumberInput(config);
         }

         this.numberInputField = fieldId;
         this.numberInput.setLength(0);
         this.numberInput.append(Math.max(0, currentValue));
      }

      private boolean isFocusedNumberInputHit(int lx, int ly, int cx, int cy, int cw) {
         if (this.numberInputField < 0) {
            return false;
         }

         int rowY = this.getNumberFieldRowY(this.numberInputField, cx, cy, cw);
         return rowY < 0 ? false : this.isNumberInputHit(lx, ly, cx, rowY, cw);
      }

      private int getNumberFieldRowY(int fieldId, int cx, int cy, int cw) {
         return switch (fieldId) {
            case 1 -> {
               int y = cy + 40;
               y += 30;
               int themePerRow = Math.max(2, Math.min(6, (cw + 6) / 98));
               int themeRows = (GUI_ACCENT_THEME_KEYS.length + themePerRow - 1) / themePerRow;
               y += themeRows * 24 + 8;
               y += 26;
               yield y;
            }
            case 2, 3 -> {
               int y = cy + 40 + 176;
               yield fieldId == 2 ? y : y + 22;
            }
            case 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 -> {
               int y = cy + 40 + this.ahPageScroll;
               y += 48;
               ScannerConfig cfg = this.getConfig();
               if (cfg != null && !cfg.ahSkinScanningEnabled) {
                  y += 22;
               }

               y += 22;
               int filterCols = this.getAhFilterColumns(cw);
               int filterRows = (AH_NOTIFY_FILTER_LABELS.length + filterCols - 1) / filterCols;
               int presetsY = y + filterRows * 26 + 2;
               int row0 = presetsY + 22 + 6;
               row0 += 92;

               int offset = switch (fieldId) {
                  case 4 -> 0;
                  case 5 -> 1;
                  case 6 -> 2;
                  case 7 -> 3;
                  case 8 -> 4;
                  case 9 -> 5;
                  case 10 -> 6;
                  case 11 -> 7;
                  case 12 -> 8;
                  case 13 -> 9;
                  default -> 0;
               };
               yield row0 + offset * 22;
            }
            case 14, 15, 16 -> {
               int y = cy + 40;
               int manualRowY = y + 22;
               int afkHeaderY = manualRowY + 22 + 2 + 22 + 8;
               int toggleStartY = afkHeaderY + 22;
               int row0 = toggleStartY + 220 + 6;
               int offset = fieldId == 14 ? 0 : (fieldId == 15 ? 1 : 2);
               yield row0 + offset * 22;
            }
            default -> -1;
         };
      }

      private void applyAndClearNumberInput(ScannerConfig config) {
         if (this.numberInputField >= 0 && config != null) {
            String raw = this.numberInput.toString().trim();
            if (!raw.isEmpty()) {
               try {
                  int parsed = Integer.parseInt(raw);
                  this.setNumberFieldValue(config, this.numberInputField, parsed);
                  if (config.ahNotifyMaxPriceMillions > 0 && config.ahNotifyMinPriceMillions > config.ahNotifyMaxPriceMillions) {
                     config.ahNotifyMinPriceMillions = config.ahNotifyMaxPriceMillions;
                  }

                  if (config.ahSkinMaxValueMillions > 0 && config.ahSkinMinValueMillions > config.ahSkinMaxValueMillions) {
                     config.ahSkinMinValueMillions = config.ahSkinMaxValueMillions;
                  }

                  if (config.ahAutoBuyMaxPriceMillions > 0 && config.ahAutoBuyMinPriceMillions > config.ahAutoBuyMaxPriceMillions) {
                     config.ahAutoBuyMinPriceMillions = config.ahAutoBuyMaxPriceMillions;
                  }

                  config.save();
               } catch (NumberFormatException var4) {
               }
            }

            this.numberInputField = -1;
            this.numberInput.setLength(0);
         } else {
            this.numberInputField = -1;
            this.numberInput.setLength(0);
         }
      }

      private void setNumberFieldValue(ScannerConfig config, int fieldId, int rawValue) {
         int v = Math.max(0, rawValue);
         switch (fieldId) {
            case 1:
               config.slotHighlightAlpha = Math.max(0, Math.min(255, v));
               break;
            case 2:
               config.seymourTargetStage = Math.max(0, Math.min(200, v));
               break;
            case 3:
               config.seymourStageTolerance = Math.max(0, Math.min(50, v));
               break;
            case 4:
               config.ahPollIntervalSeconds = Math.max(1, Math.min(60, v));
               break;
            case 5:
               config.ahTagsPerPoll = Math.max(1, Math.min(30, v));
               break;
            case 6:
               config.ahMaxAlertsPerPoll = Math.max(1, Math.min(20, v));
               break;
            case 7:
               config.ahNotifyMinPriceMillions = Math.max(0, Math.min(50000, v));
               break;
            case 8:
               config.ahNotifyMaxPriceMillions = Math.max(0, Math.min(50000, v));
               break;
            case 9:
               config.ahSkinMinValueMillions = Math.max(0, Math.min(50000, v));
               break;
            case 10:
               config.ahSkinMaxValueMillions = Math.max(0, Math.min(50000, v));
               break;
            case 11:
               config.ahDuplicateSuppressSeconds = Math.max(0, Math.min(300, v));
               break;
            case 12:
               config.ahHexDistanceMax = Math.max(0, Math.min(441, v));
               break;
            case 13:
               config.ahHexStageTolerance = Math.max(0, Math.min(50, v));
               break;
            case 14:
               config.ahAutoBuyMinPriceMillions = Math.max(0, Math.min(50000, v));
               break;
            case 15:
               config.ahAutoBuyMaxPriceMillions = Math.max(0, Math.min(50000, v));
               break;
            case 16:
               config.ahAutoBuyCooldownSeconds = Math.max(0, Math.min(60, v));
         }
      }

      private void drawAppearancePage(
         GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover
      ) {
         int y = cy + 40;
         this.drawToggle(context, bx, by, cx, y, cw, "Dark mode", config.guiDarkMode, lmx, lmy);
         y += 28;
         int opacityX = this.getAppearanceOpacityInputX(cx);
         int alpha = this.getGuiAlpha(config);
         context.text(this.font, "GUI opacity (0-255):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + opacityX, by + y, bx + opacityX + 72, by + y + 22, this.guiOpacityFocused ? btnHover : btnBg);
         context.fill(bx + opacityX + 1, by + y + 1, bx + opacityX + 72 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String opacityDisplay = this.guiOpacityFocused ? this.guiOpacityInput.toString() : String.valueOf(config.guiAlpha);
         if (opacityDisplay.isEmpty()) {
            opacityDisplay = "230";
         }

         context.text(this.font, opacityDisplay, bx + opacityX + 4, by + y + 4, this.TEXT_WHITE, false);
         if (this.guiOpacityFocused) {
            context.text(this.font, "_", bx + opacityX + 4 + this.font.width(opacityDisplay), by + y + 4, this.TEXT_WHITE, false);
         }

         y += 28;
         context.text(this.font, "GUI color theme", bx + cx, by + y, this.TEXT_MUTED, false);
         y += 16;
         int themeButtonW = 100;
         int themeGap = 10;
         int themePerRow = this.appearanceThemePerRow(cw);
         String selectedTheme = config.guiAccentTheme == null ? "STATIC_BLUE" : config.guiAccentTheme.toUpperCase(Locale.ROOT);

         for (int i = 0; i < GUI_ACCENT_THEME_KEYS.length; i++) {
            int row = i / themePerRow;
            int col = i % themePerRow;
            int x = cx + col * (themeButtonW + themeGap);
            int ty = y + row * 28;
            boolean hover = lmx >= x && lmx < x + themeButtonW && lmy >= ty && lmy < ty + 24;
            boolean selected = GUI_ACCENT_THEME_KEYS[i].equals(selectedTheme);
            int border = selected ? 0xFF000000 | this.resolveGuiAccentRgb(config) : (hover ? btnHover : btnBg);
            context.fill(bx + x, by + ty, bx + x + themeButtonW, by + ty + 24, border);
            context.fill(bx + x + 1, by + ty + 1, bx + x + themeButtonW - 1, by + ty + 24 - 1, 0xFF000000 | GUI_ACCENT_THEME_COLORS[i]);
            context.text(this.font, GUI_ACCENT_THEME_LABELS[i], bx + x + 6, by + ty + 6, this.TEXT_WHITE, false);
         }

         int themeRows = (GUI_ACCENT_THEME_KEYS.length + themePerRow - 1) / themePerRow;
         y += themeRows * 28 + 12;
         this.drawToggle(context, bx, by, cx, y, cw, "Use category slot highlight colors", config.useCategoryHighlightColors, lmx, lmy);
         y += 28;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Slot highlight alpha (0-255):", config.slotHighlightAlpha, 1, lmx, lmy, btnBg, btnHover);
         y += 30;

         for (int row = 0; row < HIGHLIGHT_COLOR_LABELS.length; row++) {
            y = this.drawHighlightColorRow(context, config, bx, by, cx, y, cw, lmx, lmy, row, btnBg, btnHover);
         }

         context.text(this.font, "Tip: type a hex or click swatches. Press Enter to apply.", bx + cx, by + y + 4, this.TEXT_MUTED, false);
      }

      private int drawHighlightColorRow(
         GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int y, int cw, int lmx, int lmy, int row, int btnBg, int btnHover
      ) {
         String label = row >= 0 && row < HIGHLIGHT_COLOR_LABELS.length ? HIGHLIGHT_COLOR_LABELS[row] : "Type " + row;
         int currentRgb = this.getHighlightColorByRow(config, row);
         boolean enabled = this.isHighlightEnabledByRow(config, row);
         String hex = String.format("#%06X", currentRgb & 16777215);
         context.text(this.font, label + ":", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         int toggleX = cx + 118;
         int toggleW = 32;
         boolean toggleHover = lmx >= toggleX && lmx < toggleX + toggleW && lmy >= y + 2 && lmy < y + 2 + 18;
         context.fill(bx + toggleX, by + y + 2, bx + toggleX + toggleW, by + y + 2 + 18, toggleHover ? btnHover : btnBg);
         context.text(this.font, enabled ? "ON" : "OFF", bx + toggleX + 4, by + y + 6, enabled ? -9371792 : -36752, false);
         int inputX = this.getAppearanceHexInputX(cx);
         boolean inputFocused = this.highlightHexFocusedRow == row;
         int alpha = this.getGuiAlpha(config);
         context.fill(bx + inputX, by + y + 2, bx + inputX + 88, by + y + 2 + 18, inputFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 3, bx + inputX + 88 - 1, by + y + 2 + 18 - 1, alpha << 24 | 1710618);
         String shownHex = inputFocused ? this.highlightHexInput.toString() : hex;
         if (shownHex == null || shownHex.isEmpty()) {
            shownHex = "#000000";
         }

         context.text(this.font, shownHex, bx + inputX + 4, by + y + 6, enabled ? 0xFF000000 | currentRgb & 16777215 : this.TEXT_MUTED, false);
         if (inputFocused) {
            context.text(this.font, "_", bx + inputX + 4 + this.font.width(shownHex), by + y + 6, this.TEXT_WHITE, false);
         }

         int swatchW = 16;
         int swatchGap = 4;
         int swatchPer = wrapPerRow(swatchW, swatchGap, cw);
         int swatchY = y + 22;
         for (int i = 0; i < HIGHLIGHT_SWATCHES.length; i++) {
            int col = i % swatchPer;
            int srow = i / swatchPer;
            int sx = cx + col * (swatchW + swatchGap);
            int sy = swatchY + srow * 18;
            boolean hover = lmx >= sx && lmx < sx + swatchW && lmy >= sy && lmy < sy + swatchW;
            int border = hover ? btnHover : btnBg;
            if ((HIGHLIGHT_SWATCHES[i] & 16777215) == (currentRgb & 16777215)) {
               border = -7820545;
            }

            context.fill(bx + sx, by + sy, bx + sx + swatchW, by + sy + swatchW, border);
            int fill = 0xFF000000 | HIGHLIGHT_SWATCHES[i] & 16777215;
            if (!enabled) {
               fill = 0xFF000000 | (HIGHLIGHT_SWATCHES[i] & 16777215) >> 1;
            }

            context.fill(bx + sx + 2, by + sy + 2, bx + sx + swatchW - 2, by + sy + swatchW - 2, fill);
         }

         int swatchRows = (HIGHLIGHT_SWATCHES.length + swatchPer - 1) / swatchPer;
         return y + 22 + swatchRows * 18 + 8;
      }

      private boolean tryApplyHighlightColorClick(int lx, int ly, int cx, int cw, int y, int row, ScannerConfig config) {
         int toggleX = cx + 118;
         int toggleW = 32;
         if (lx >= toggleX && lx < toggleX + toggleW && ly >= y + 2 && ly < y + 2 + 18) {
            this.setHighlightEnabledByRow(config, row, !this.isHighlightEnabledByRow(config, row));
            config.save();
            return true;
         }

         int swatchW = 16;
         int swatchGap = 4;
         int swatchPer = wrapPerRow(swatchW, swatchGap, cw);
         int swatchY = y + 22;
         for (int i = 0; i < HIGHLIGHT_SWATCHES.length; i++) {
            int col = i % swatchPer;
            int srow = i / swatchPer;
            int sx = cx + col * (swatchW + swatchGap);
            int sy = swatchY + srow * 18;
            if (lx >= sx && lx < sx + swatchW && ly >= sy && ly < sy + swatchW) {
               this.setHighlightColorByRow(config, row, HIGHLIGHT_SWATCHES[i]);
               config.save();
               return true;
            }
         }

         return false;
      }

      private boolean isHighlightEnabledByRow(ScannerConfig config, int row) {
         return switch (row) {
            case 0 -> config.highlightCrystalEnabled;
            case 1 -> config.highlightExoticEnabled;
            case 2 -> config.highlightOgFairyEnabled;
            case 3 -> config.highlightFairyEnabled;
            case 4 -> config.highlightBleachedEnabled;
            case 5 -> config.highlightGlitchedEnabled;
            case 6 -> config.highlightSeymourT1Enabled;
            case 7 -> config.highlightSeymourT2Enabled;
            case 8 -> config.highlightSpecificHexEnabled;
            case 9 -> config.highlightSeymourT3Enabled;
            default -> true;
         };
      }

      private void setHighlightEnabledByRow(ScannerConfig config, int row, boolean enabled) {
         switch (row) {
            case 0:
               config.highlightCrystalEnabled = enabled;
               break;
            case 1:
               config.highlightExoticEnabled = enabled;
               break;
            case 2:
               config.highlightOgFairyEnabled = enabled;
               break;
            case 3:
               config.highlightFairyEnabled = enabled;
               break;
            case 4:
               config.highlightBleachedEnabled = enabled;
               break;
            case 5:
               config.highlightGlitchedEnabled = enabled;
               break;
            case 6:
               config.highlightSeymourT1Enabled = enabled;
               break;
            case 7:
               config.highlightSeymourT2Enabled = enabled;
               break;
            case 8:
               config.highlightSpecificHexEnabled = enabled;
               break;
            case 9:
               config.highlightSeymourT3Enabled = enabled;
         }
      }

      private int getHighlightColorByRow(ScannerConfig config, int row) {
         return switch (row) {
            case 0 -> config.highlightCrystalRgb;
            case 1 -> config.highlightExoticRgb;
            case 2 -> config.highlightOgFairyRgb;
            case 3 -> config.highlightFairyRgb;
            case 4 -> config.highlightBleachedRgb;
            case 5 -> config.highlightGlitchedRgb;
            case 6 -> config.highlightSeymourT1Rgb;
            case 7 -> config.highlightSeymourT2Rgb;
            case 8 -> config.highlightSpecificHexRgb;
            default -> config.highlightSeymourT3Rgb;
         };
      }

      private void setHighlightColorByRow(ScannerConfig config, int row, int rgb) {
         int color = rgb & 16777215;
         switch (row) {
            case 0:
               config.highlightCrystalRgb = color;
               break;
            case 1:
               config.highlightExoticRgb = color;
               break;
            case 2:
               config.highlightOgFairyRgb = color;
               break;
            case 3:
               config.highlightFairyRgb = color;
               break;
            case 4:
               config.highlightBleachedRgb = color;
               break;
            case 5:
               config.highlightGlitchedRgb = color;
               break;
            case 6:
               config.highlightSeymourT1Rgb = color;
               break;
            case 7:
               config.highlightSeymourT2Rgb = color;
               break;
            case 8:
               config.highlightSpecificHexRgb = color;
               break;
            case 9:
               config.highlightSeymourT3Rgb = color;
         }
      }

      private void drawSeymourPage(GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover) {
         int y = cy + 40;
         context.text(this.font, "Piece + Tier Filters", bx + cx, by + y - 10, -7820545, false);
         this.drawToggle(context, bx, by, cx, y, cw, "Enable Seymour scanning", config.seymourScanningEnabled, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Scan Velvet Top Hat", config.seymourScanTopHat, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Scan Cashmere Jacket", config.seymourScanJacket, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Scan Satin Trousers", config.seymourScanTrousers, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Scan Oxford Shoes", config.seymourScanShoes, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Scan Seymour Tier T1", config.seymourScanT1, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Scan Seymour Tier T2", config.seymourScanT2, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Exact hex only (no nearest stage)", config.seymourExactHexOnly, lmx, lmy);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Target dye stage (0 = off):", config.seymourTargetStage, 2, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(
            context, config, bx, by, cx, y, cw, "Stage tolerance (0 = exact stage):", config.seymourStageTolerance, 3, lmx, lmy, btnBg, btnHover
         );
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Highlight Seymour Tier T1", config.highlightSeymourT1Enabled, lmx, lmy);
         y += 28;
         context.text(this.font, "Matching Sources", bx + cx, by + y - 10, -7820545, false);
         this.drawToggle(context, bx, by, cx, y, cw, "Color DB: target set", config.seymourScanTargetColors, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Color DB: fade set (off — stages ignored)", config.seymourScanFadeDyes, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Show high fade matches", config.seymourShowHighFades, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Match to correct piece only", config.seymourPieceSpecificEnabled, lmx, lmy);
         y += 32;
         int inputX = cx + 210;
         int alpha = this.getGuiAlpha(config);
         context.text(this.font, "Armor name filter (comma-separated):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.seymourArmorFilterFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String filterValue = this.seymourArmorFilterFocused
            ? this.seymourArmorFilterInput.toString()
            : (config.seymourArmorNameFilters != null ? config.seymourArmorNameFilters : "all");
         if (filterValue.isEmpty()) {
            filterValue = "all";
         }

         if (this.seymourArmorFilterFocused && this.seymourArmorFilterInput.length() == 0) {
            context.text(this.font, "all", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = filterValue.length() > 26 ? filterValue.substring(0, 23) + "..." : filterValue;
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.seymourArmorFilterFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         context.text(
            this.font, "Examples: all, pure, fairy, exo (stage filter uses Target/Tolerance above)", bx + cx, by + y + 22 + 6, this.TEXT_MUTED, false
         );
      }

      private void drawAutoBuyPage(GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover) {
         int y = cy + 40;
         context.text(this.font, "Buy Now (Manual)", bx + cx, by + y + 4, -7820545, false);
         y += 22;
         int manualInputX = cx + 160;
         int manualInputW = Math.max(140, Math.min(280, cw - 260));
         int buyBtnW = 86;
         int buyBtnX = Math.min(cx + cw - buyBtnW, manualInputX + manualInputW + 6);
         int alpha = this.getGuiAlpha(config);
         context.text(this.font, "Auction UUID:", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         boolean uuidHover = lmx >= manualInputX && lmx < manualInputX + manualInputW && lmy >= y && lmy < y + 22;
         context.fill(
            bx + manualInputX, by + y, bx + manualInputX + manualInputW, by + y + 22, this.manualBuyUuidFocused ? btnHover : (uuidHover ? btnHover : btnBg)
         );
         context.fill(bx + manualInputX + 1, by + y + 1, bx + manualInputX + manualInputW - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String uuidDisplay = this.manualBuyUuidInput.length() > 0 ? this.manualBuyUuidInput.toString() : "paste auction uuid";
         context.text(
            this.font,
            this.fitText(uuidDisplay, manualInputW - 10),
            bx + manualInputX + 4,
            by + y + 4,
            this.manualBuyUuidInput.length() > 0 ? this.TEXT_WHITE : this.TEXT_MUTED,
            false
         );
         if (this.manualBuyUuidFocused) {
            String shown = this.fitText(uuidDisplay, manualInputW - 10);
            context.text(this.font, "_", bx + manualInputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
         }

         boolean buyNowHover = lmx >= buyBtnX && lmx < buyBtnX + buyBtnW && lmy >= y && lmy < y + 22;
         context.fill(bx + buyBtnX, by + y, bx + buyBtnX + buyBtnW, by + y + 22, buyNowHover ? btnHover : btnBg);
         context.text(this.font, "Buy now", bx + buyBtnX + 18, by + y + 4, -9371792, false);
         y += 24;
         String status = this.autoBuyStatusLine == null ? "" : this.autoBuyStatusLine.trim();
         if (status.isEmpty()) {
            context.text(this.font, "Instantly buys one listing using /viewauction + buy/confirm clicks.", bx + cx, by + y + 4, this.TEXT_MUTED, false);
         } else {
            int color = status.startsWith("Buying") ? -9371792 : -32640;
            context.text(this.font, this.fitText(status, cw - 8), bx + cx, by + y + 4, color, false);
         }

         y += 30;
         context.text(this.font, "AFK Auto-Buy (Sniper Rules)", bx + cx, by + y + 4, -7820545, false);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Enable AH skin scanning (armor + pet skins)", config.ahSkinScanningEnabled, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Enable AFK Auto Buy", config.ahAutoBuyEnabled, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow SNIPE alerts", config.ahAutoBuySnipe, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow Exotic alerts", config.ahAutoBuyExotic, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow Glitched alerts", config.ahAutoBuyGlitched, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow Seymour T1 alerts", config.ahAutoBuySeymourT1, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow Seymour T2 alerts", config.ahAutoBuySeymourT2, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow Seymour Hex alerts", config.ahAutoBuySeymourHex, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow Armor Skin alerts", config.ahAutoBuyArmorSkin, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Allow Pet Skin alerts", config.ahAutoBuyPetSkin, lmx, lmy);
         y += 28;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Min price (M, 0 = no floor):", config.ahAutoBuyMinPriceMillions, 14, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Max price (M, 0 = no cap):", config.ahAutoBuyMaxPriceMillions, 15, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Delay between buys (sec):", config.ahAutoBuyCooldownSeconds, 16, lmx, lmy, btnBg, btnHover);
         y += 30;
         int inputX = cx + 230;
         context.text(this.font, "Name must contain (optional, comma-separated):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.autoBuyNameFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String nameRaw = this.autoBuyNameFocused
            ? this.autoBuyNameInput.toString()
            : (config.ahAutoBuyNameContains == null ? "" : config.ahAutoBuyNameContains);
         if (nameRaw.isEmpty()) {
            context.text(this.font, "any name", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = this.fitText(nameRaw, 250);
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.autoBuyNameFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         y += 24;
         context.text(this.font, "Exact hex list (optional, comma-separated):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.autoBuyHexFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String hexRaw = this.autoBuyHexFocused ? this.autoBuyHexInput.toString() : (config.ahAutoBuyHexList == null ? "" : config.ahAutoBuyHexList);
         if (hexRaw.isEmpty()) {
            context.text(this.font, "any hex", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = this.fitText(hexRaw, 250);
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.autoBuyHexFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         y += 24;
         context.text(this.font, "Item-specific rules (name=maxM, comma-separated):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.autoBuyRulesFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String rulesRaw = this.autoBuyRulesFocused
            ? this.autoBuyRulesInput.toString()
            : (config.ahAutoBuyItemPriceRules == null ? "" : config.ahAutoBuyItemPriceRules);
         if (rulesRaw.isEmpty()) {
            context.text(this.font, "e.g. aqua bunny skin=200", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = this.fitText(rulesRaw, 250);
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.autoBuyRulesFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         y += 26;
         context.text(
            this.font, "Tip: item rule overrides type toggles/price range but still blocks above market.", bx + cx, by + y + 4, this.TEXT_MUTED, false
         );
      }

      private void drawAuctionHousePage(
         GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover
      ) {
         this.updateAhScrollBounds(cw);
         int y = cy + 40 + this.ahPageScroll;
         context.text(this.font, "Passive AH Alerts (live listings)", bx + cx, by + y + 4, -7820545, false);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Enable AH notifications", config.ahNotificationsEnabled, lmx, lmy);
         y += 22;
         this.drawToggle(context, bx, by, cx, y, cw, "Enable skin scanning (armor + pet)", config.ahSkinScanningEnabled, lmx, lmy);
         y += 26;
         if (!config.ahSkinScanningEnabled) {
            context.text(this.font, "AH skin scanning is OFF. Skin alerts/scan-now matches are disabled.", bx + cx, by + y + 4, -24416, false);
            y += 22;
         }

         int enabledCount = this.countEnabledAhFilters(config);
         context.text(
            this.font, "Alert filters (" + enabledCount + "/" + AH_NOTIFY_FILTER_LABELS.length + " enabled):", bx + cx, by + y + 4, this.TEXT_WHITE, false
         );
         y += 22;
         int filterCols = this.getAhFilterColumns(cw);
         int filterChipW = this.getAhFilterChipWidth(cw, filterCols);
         int filterRows = (AH_NOTIFY_FILTER_LABELS.length + filterCols - 1) / filterCols;
         int filterTopY = y;

         for (int i = 0; i < AH_NOTIFY_FILTER_LABELS.length; i++) {
            int row = i / filterCols;
            int col = i % filterCols;
            int fx = cx + col * (filterChipW + 4);
            int fy = filterTopY + row * 26;
            this.drawAhFilterChip(
               context, bx, by, fx, fy, filterChipW, AH_NOTIFY_FILTER_LABELS[i], AH_NOTIFY_FILTER_COLORS[i], this.getAhNotifyFilterByIndex(config, i), lmx, lmy
            );
         }

         int presetsY = filterTopY + filterRows * 26 + 2;
         int presetGap = 4;
         int presetW = (cw - presetGap * 2) / 3;
         int x0 = cx;
         int x1 = x0 + presetW + presetGap;
         int x2 = x1 + presetW + presetGap;
         boolean allHover = lmx >= x0 && lmx < x0 + presetW && lmy >= presetsY && lmy < presetsY + 22;
         boolean coreHover = lmx >= x1 && lmx < x1 + presetW && lmy >= presetsY && lmy < presetsY + 22;
         boolean noneHover = lmx >= x2 && lmx < x2 + presetW && lmy >= presetsY && lmy < presetsY + 22;
         context.fill(bx + x0, by + presetsY, bx + x0 + presetW, by + presetsY + 22, allHover ? btnHover : btnBg);
         context.fill(bx + x1, by + presetsY, bx + x1 + presetW, by + presetsY + 22, coreHover ? btnHover : btnBg);
         context.fill(bx + x2, by + presetsY, bx + x2 + presetW, by + presetsY + 22, noneHover ? btnHover : btnBg);
         context.text(this.font, "All On", bx + x0 + 10, by + presetsY + 5, -9371792, false);
         context.text(this.font, "Core", bx + x1 + 14, by + presetsY + 5, -12176, false);
         context.text(this.font, "None", bx + x2 + 14, by + presetsY + 5, -36752, false);
         y = presetsY + 22 + 6;
         this.drawToggle(context, bx, by, cx, y, cw, "Adaptive fast mode after alerts", config.ahAdaptiveLatencyMode, lmx, lmy);
         y += 22;
         int segGap = 4;
         int segW = Math.max(64, Math.min(110, cw - 190 - segGap * 2));
         segW = Math.max(64, segW / 3);
         this.drawSegmentedModeRow(
            context, bx, by, cx, y, segW, segGap, lmx, lmy, "Profile preset:", config.ahProfilePreset, "SAFE", "SNIPING", "COLLECTING", btnBg, btnHover
         );
         y += 22;
         this.drawSegmentedModeRow(
            context, bx, by, cx, y, segW, segGap, lmx, lmy, "Profit preset:", config.ahProfitPreset, "SAFE", "AGGRESSIVE", "SNIPE_ONLY", btnBg, btnHover
         );
         y += 22;
         this.drawSegmentedModeRow(
            context,
            bx,
            by,
            cx,
            y,
            segW,
            segGap,
            lmx,
            lmy,
            "Hex match mode:",
            config.ahHexMatchMode,
            "EXACT",
            "WITHIN_DELTA",
            "STAGE_TOLERANCE",
            btnBg,
            btnHover
         );
         y += 26;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Poll interval (sec):", config.ahPollIntervalSeconds, 4, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Tags per poll:", config.ahTagsPerPoll, 5, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Max alerts per poll:", config.ahMaxAlertsPerPoll, 6, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Alert price min (M, 0=off):", config.ahNotifyMinPriceMillions, 7, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Alert price max (M, 0=off):", config.ahNotifyMaxPriceMillions, 8, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Skin value min (M, 0=off):", config.ahSkinMinValueMillions, 9, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Skin value max (M, 0=off):", config.ahSkinMaxValueMillions, 10, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Duplicate suppress (sec):", config.ahDuplicateSuppressSeconds, 11, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Hex delta max:", config.ahHexDistanceMax, 12, lmx, lmy, btnBg, btnHover);
         y += 22;
         this.drawNumberRow(context, config, bx, by, cx, y, cw, "Hex stage tolerance:", config.ahHexStageTolerance, 13, lmx, lmy, btnBg, btnHover);
         y += 30;
         int inputX = cx + 230;
         int alpha = this.getGuiAlpha(config);
         context.text(this.font, "Active scan watch tags (optional):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.ahWatchTagsFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String watchRaw = this.ahWatchTagsFocused
            ? this.ahWatchTagsInput.toString()
            : (config.ahWatchTags != null && !config.ahWatchTags.isEmpty() ? String.join(", ", config.ahWatchTags) : "");
         if (watchRaw.isEmpty()) {
            context.text(this.font, "blank = passive auto pool; scan-now can override", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = watchRaw.length() > 28 ? watchRaw.substring(0, 25) + "..." : watchRaw;
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.ahWatchTagsFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         y += 24;
         context.text(this.font, "Seymour hex filter (e.g. 16FBF8, 901C0A):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.ahSeymourHexFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String seyHexRaw = this.ahSeymourHexFocused ? this.ahSeymourHexInput.toString() : (config.ahSeymourHexList == null ? "" : config.ahSeymourHexList);
         if (seyHexRaw.isEmpty()) {
            context.text(this.font, "(optional)", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = seyHexRaw.length() > 28 ? seyHexRaw.substring(0, 25) + "..." : seyHexRaw;
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.ahSeymourHexFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         y += 24;
         context.text(this.font, "Mute skins (tags/names, comma-separated):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + inputX, by + y, bx + inputX + 260, by + y + 22, this.ahSkinMuteFocused ? btnHover : btnBg);
         context.fill(bx + inputX + 1, by + y + 1, bx + inputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String muteRaw = this.ahSkinMuteFocused ? this.ahSkinMuteInput.toString() : (config.ahSkinMuteList == null ? "" : config.ahSkinMuteList);
         if (muteRaw.isEmpty()) {
            context.text(this.font, "(none)", bx + inputX + 4, by + y + 4, this.TEXT_MUTED, false);
         } else {
            String shown = muteRaw.length() > 28 ? muteRaw.substring(0, 25) + "..." : muteRaw;
            context.text(this.font, shown, bx + inputX + 4, by + y + 4, this.TEXT_WHITE, false);
            if (this.ahSkinMuteFocused) {
               context.text(this.font, "_", bx + inputX + 4 + this.font.width(shown), by + y + 4, this.TEXT_WHITE, false);
            }
         }

         y += 32;
         context.text(this.font, "Active Scan Now (one-time, instant)", bx + cx, by + y + 4, -7820545, false);
         y += 22;
         int typeX = cx + 170;
         int typeW = 110;
         int contentBottomY = this.getAhContentBottomY();
         boolean leftHover = lmx >= typeX && lmx < typeX + 20 && lmy >= y && lmy < y + 22;
         boolean midHover = lmx >= typeX + 20 && lmx < typeX + 20 + typeW && lmy >= y && lmy < y + 22;
         boolean rightHover = lmx >= typeX + 20 + typeW && lmx < typeX + 40 + typeW && lmy >= y && lmy < y + 22;
         context.text(this.font, "Type:", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         context.fill(bx + typeX, by + y, bx + typeX + 20, by + y + 22, leftHover ? btnHover : btnBg);
         context.fill(bx + typeX + 20, by + y, bx + typeX + 20 + typeW, by + y + 22, midHover ? btnHover : btnBg);
         context.fill(bx + typeX + 20 + typeW, by + y, bx + typeX + 40 + typeW, by + y + 22, rightHover ? btnHover : btnBg);
         context.text(this.font, "<", bx + typeX + 7, by + y + 4, this.TEXT_WHITE, false);
         context.text(this.font, this.getAhQuickTypeLabel(), bx + typeX + 24, by + y + 4, -7820545, false);
         context.text(this.font, ">", bx + typeX + 20 + typeW + 7, by + y + 4, this.TEXT_WHITE, false);
         y += 24;
         int quickInputX = cx + 180;
         boolean armorSkinType = "ARMOR_SKIN".equals(this.getAhQuickType());
         context.text(this.font, "Set filter (non-skin types):", bx + cx, by + y + 4, armorSkinType ? this.TEXT_MUTED : this.TEXT_WHITE, false);
         context.fill(bx + quickInputX, by + y, bx + quickInputX + 260, by + y + 22, this.ahQuickSetFocused ? btnHover : btnBg);
         context.fill(bx + quickInputX + 1, by + y + 1, bx + quickInputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String setDisplay = this.ahQuickSetFocused ? this.ahQuickSetInput.toString() : (this.ahQuickSelectedSet == null ? "" : this.ahQuickSelectedSet);
         if (setDisplay.isEmpty()) {
            setDisplay = "type set tag or choose below";
         }

         context.text(
            this.font,
            setDisplay.length() > 30 ? setDisplay.substring(0, 27) + "..." : setDisplay,
            bx + quickInputX + 4,
            by + y + 4,
            this.ahQuickSetFocused ? this.TEXT_WHITE : (this.ahQuickSelectedSet != null && !this.ahQuickSelectedSet.isEmpty() ? this.TEXT_WHITE : this.TEXT_MUTED),
            false
         );
         if (this.ahQuickSetFocused) {
            context.text(
               this.font,
               "_",
               bx + quickInputX + 4 + this.font.width(setDisplay.length() > 30 ? setDisplay.substring(0, 27) + "..." : setDisplay),
               by + y + 4,
               this.TEXT_WHITE,
               false
            );
         }

         this.refreshAhQuickSetSuggestions(this.ahQuickSetFocused ? this.ahQuickSetInput.toString() : this.ahQuickSelectedSet);
         int setSugCountDesired = Math.min(2, this.ahQuickSetSuggestions.size());
         this.refreshAhQuickSkinSuggestions(this.ahQuickSkinFocused ? this.ahQuickSkinInput.toString() : this.ahQuickSelectedSkin);
         int skinSugCountDesired = Math.min(3, this.ahQuickSkinSuggestionTags.size());
         int[] ahSugCounts = this.clampAhSuggestionCountsToSpace(y, setSugCountDesired, skinSugCountDesired);
         int setSugCount = ahSugCounts[0];
         int skinSugCount = ahSugCounts[1];
         int sugY = y + 22 + 2;

         for (int i = 0; i < setSugCount; i++) {
            boolean h = lmx >= quickInputX && lmx < quickInputX + 260 && lmy >= sugY + i * 12 && lmy < sugY + i * 12 + 12;
            context.fill(bx + quickInputX, by + sugY + i * 12, bx + quickInputX + 260, by + sugY + i * 12 + 12, h ? 1077557818 : 1075847200);
            context.text(this.font, this.ahQuickSetSuggestions.get(i), bx + quickInputX + 3, by + sugY + i * 12 + 2, this.TEXT_MUTED, false);
         }

         y = sugY + setSugCount * 12;
         context.text(this.font, "Skin filter (ARMOR SKIN type):", bx + cx, by + y + 4, armorSkinType ? this.TEXT_WHITE : this.TEXT_MUTED, false);
         context.fill(bx + quickInputX, by + y, bx + quickInputX + 260, by + y + 22, this.ahQuickSkinFocused ? btnHover : btnBg);
         context.fill(bx + quickInputX + 1, by + y + 1, bx + quickInputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String skinDisplay = this.ahQuickSkinFocused ? this.ahQuickSkinInput.toString() : (this.ahQuickSelectedSkin == null ? "" : this.ahQuickSelectedSkin);
         if (skinDisplay.isEmpty()) {
            skinDisplay = "type skin prefix (e.g. BE)";
         }

         context.text(
            this.font,
            skinDisplay.length() > 30 ? skinDisplay.substring(0, 27) + "..." : skinDisplay,
            bx + quickInputX + 4,
            by + y + 4,
            this.ahQuickSkinFocused ? this.TEXT_WHITE : (this.ahQuickSelectedSkin != null && !this.ahQuickSelectedSkin.isEmpty() ? this.TEXT_WHITE : this.TEXT_MUTED),
            false
         );
         if (this.ahQuickSkinFocused) {
            context.text(
               this.font,
               "_",
               bx + quickInputX + 4 + this.font.width(skinDisplay.length() > 30 ? skinDisplay.substring(0, 27) + "..." : skinDisplay),
               by + y + 4,
               this.TEXT_WHITE,
               false
            );
         }

         int skinSugY = y + 22 + 2;

         for (int i = 0; i < skinSugCount; i++) {
            boolean h = lmx >= quickInputX && lmx < quickInputX + 260 && lmy >= skinSugY + i * 12 && lmy < skinSugY + i * 12 + 12;
            context.fill(bx + quickInputX, by + skinSugY + i * 12, bx + quickInputX + 260, by + skinSugY + i * 12 + 12, h ? 1077557818 : 1075847200);
            context.text(this.font, this.ahQuickSkinSuggestionLabels.get(i), bx + quickInputX + 3, by + skinSugY + i * 12 + 2, this.TEXT_MUTED, false);
         }

         y = skinSugY + skinSugCount * 12 + 4;
         if (y + 22 <= contentBottomY) {
            boolean addSetHover = lmx >= cx && lmx < cx + 94 && lmy >= y && lmy < y + 22;
            boolean addSkinHover = lmx >= cx + 100 && lmx < cx + 194 && lmy >= y && lmy < y + 22;
            boolean scanNowHover = lmx >= cx + 200 && lmx < cx + 324 && lmy >= y && lmy < y + 22;
            context.fill(bx + cx, by + y, bx + cx + 94, by + y + 22, addSetHover ? btnHover : btnBg);
            context.text(this.font, "Save set", bx + cx + 18, by + y + 4, this.TEXT_WHITE, false);
            context.fill(bx + cx + 100, by + y, bx + cx + 194, by + y + 22, addSkinHover ? btnHover : btnBg);
            context.text(this.font, "Save skin", bx + cx + 116, by + y + 4, this.TEXT_WHITE, false);
            context.fill(bx + cx + 200, by + y, bx + cx + 324, by + y + 22, scanNowHover ? btnHover : btnBg);
            context.text(this.font, "Run scan now", bx + cx + 230, by + y + 4, -9371792, false);
         }

         String status = "AH notifier idle";
         if (PotatoToolMod.getInstance() != null && PotatoToolMod.getInstance().getAuctionNotifier() != null) {
            status = PotatoToolMod.getInstance().getAuctionNotifier().getStatusLine();
         }

         y += 26;
         if (y + 22 <= contentBottomY) {
            context.text(this.font, status, bx + cx, by + y + 4, this.TEXT_MUTED, false);
         }

         if (this.ahQuickActionStatus != null && !this.ahQuickActionStatus.isEmpty() && y + 44 <= contentBottomY) {
            y += 22;
            context.text(this.font, this.ahQuickActionStatus, bx + cx, by + y + 4, this.TEXT_MUTED, false);
         }

         if (this.ahPageMaxScroll > 0) {
            String scrollInfo = "Scroll " + -this.ahPageScroll + "/" + this.ahPageMaxScroll;
            context.text(
               this.font, scrollInfo, bx + cx + cw - this.font.width(scrollInfo), by + this.getAhContentBottomY() - 10, this.TEXT_MUTED, false
            );
         }
      }

      private void drawLookupPage(GuiGraphicsExtractor context, ScannerConfig config, int bx, int by, int cx, int cy, int cw, int lmx, int lmy, int btnBg, int btnHover) {
         int y = cy + 40;
         int alpha = this.getGuiAlpha(config);
         context.text(this.font, "IGN Lookup (fresh API scan, not cache):", bx + cx, by + y + 4, this.TEXT_WHITE, false);
         int ignInputX = cx + 220;
         context.fill(bx + ignInputX, by + y, bx + ignInputX + 260, by + y + 22, this.lookupIgnFocused ? btnHover : btnBg);
         context.fill(bx + ignInputX + 1, by + y + 1, bx + ignInputX + 260 - 1, by + y + 22 - 1, alpha << 24 | 1710618);
         String ignDisplay = this.lookupIgnInput.length() > 0 ? this.lookupIgnInput.toString() : "Type player IGN";
         context.text(this.font, ignDisplay, bx + ignInputX + 6, by + y + 6, this.lookupIgnInput.length() > 0 ? this.TEXT_WHITE : this.TEXT_MUTED, false);
         if (this.lookupIgnFocused) {
            context.text(
               this.font, "_", bx + ignInputX + 6 + this.font.width(this.lookupIgnInput.toString()), by + y + 6, this.TEXT_WHITE, false
            );
         }

         y += 30;
         boolean lookupHover = lmx >= cx && lmx < cx + 120 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx, by + y, bx + cx + 120, by + y + 22, lookupHover ? btnHover : btnBg);
         context.text(this.font, "Lookup Now", bx + cx + 24, by + y + 6, -9371792, false);
         boolean openHover = lmx >= cx + 126 && lmx < cx + 266 && lmy >= y && lmy < y + 22;
         context.fill(bx + cx + 126, by + y, bx + cx + 266, by + y + 22, openHover ? btnHover : btnBg);
         context.text(this.font, "Open Viewer", bx + cx + 160, by + y + 6, this.lookupSelectedPlayer != null ? -7811841 : this.TEXT_MUTED, false);
         if (!this.lookupScanStatus.isEmpty()) {
            context.text(this.font, this.lookupScanStatus, bx + cx + 274, by + y + 6, this.TEXT_MUTED, false);
         }

         int resultsY0 = cy + 160;
         int resultsH = this.layoutH - resultsY0 - 50;
         if (resultsH < 60) {
            resultsH = 60;
         }

         int lineH = 11;
         int totalContentH = this.lookupPreviewLines.size() * lineH;
         int maxScroll = Math.max(0, totalContentH - resultsH);
         if (this.lookupPreviewScroll > maxScroll) {
            this.lookupPreviewScroll = maxScroll;
         }

         int firstVisible = maxScroll > 0 ? this.lookupPreviewScroll / lineH : 0;
         int visibleRows = Math.max(0, resultsH / lineH);
         int listLeft = bx + cx;
         int listTop = by + resultsY0;
         int listBottom = listTop + resultsH;
         context.fill(listLeft, listTop, listLeft + cw, listBottom, alpha << 24 | 1315860);
         if (this.lookupSelectedPlayer == null && this.lookupScanStatus.isEmpty()) {
            context.text(
               this.font, "Lookup a player to see level, last online, status, and items by location.", bx + cx + 4, by + resultsY0 + 8, this.TEXT_MUTED, false
            );
         } else {
            context.enableScissor(listLeft, listTop, listLeft + cw - (maxScroll > 0 ? 8 : 0), listBottom);

            for (int i = 0; i < visibleRows && firstVisible + i < this.lookupPreviewLines.size(); i++) {
               int rowY = resultsY0 + i * lineH;
               String line = this.lookupPreviewLines.get(firstVisible + i);
               int color = line.startsWith("§6") ? -11410 : (line.startsWith("§8") ? this.TEXT_MUTED : this.TEXT_WHITE);
               context.text(this.font, line, bx + cx + 4, by + rowY + 1, color, false);
            }

            context.disableScissor();
            if (maxScroll > 0) {
               int sbX = bx + cx + cw - 6;
               int sbH = resultsH;
               int thumbH = Math.max(20, sbH * sbH / Math.max(1, totalContentH));
               int thumbY = listTop + (int)((long)this.lookupPreviewScroll * (sbH - thumbH) / maxScroll);
               context.fill(sbX, listTop, sbX + 4, listBottom, alpha << 24 | 2763306);
               context.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, alpha << 24 | 5921370);
            }
         }
      }

      private void triggerLookupFromTab() {
         String ign = this.lookupIgnInput.toString().trim();
         if (ign.isEmpty()) {
            this.lookupScanStatus = "Type an IGN first.";
         } else if (PotatoToolMod.getInstance() != null && PotatoToolMod.getInstance().getPlayerScanner() != null) {
            this.lookupScanStatus = "Looking up " + ign + "...";
            PotatoToolMod.getInstance()
               .getPlayerScanner()
               .lookupPlayerFresh(ign)
               .thenAccept(
                  player -> {
                     Minecraft c = Minecraft.getInstance();
                     if (c != null) {
                        c.execute(
                           () -> {
                              if (player == null) {
                                 this.lookupSelectedPlayer = null;
                                 this.lookupPreviewLines.clear();
                                 this.lookupScanStatus = "Lookup failed for " + ign + ".";
                              } else {
                                 this.lookupSelectedPlayer = player;
                                 this.lookupPreviewScroll = 0;
                                 this.rebuildLookupPreviewLines(player);
                                 if (player.isOnlineAtScanTime()) {
                                    this.lookupScanStatus = "ONLINE: " + player.getUsername();
                                    if (this.minecraft != null && this.minecraft.player != null) {
                                       this.minecraft
                                          .player
                                          .sendSystemMessage(Component.literal("§a§lONLINE ALERT §8» §e" + player.getUsername() + " §ais online now."));
                                    }
                                 } else {
                                    this.lookupScanStatus = "Last online: " + player.getLastLoginDisplayString();
                                 }
                              }
                           }
                        );
                     }
                  }
               )
               .exceptionally(ex -> {
                  Minecraft c = Minecraft.getInstance();
                  if (c != null) {
                     c.execute(() -> this.lookupScanStatus = "Lookup error.");
                  }

                  return null;
               });
         } else {
            this.lookupScanStatus = "Scanner unavailable.";
         }
      }

      private void rebuildLookupPreviewLines(ScannedPlayer player) {
         this.lookupPreviewLines.clear();
         if (player != null) {
            String rank = player.getRankFormatted() == null ? "" : player.getRankFormatted().replaceAll("§.", "").trim();
            this.lookupPreviewLines.add("§6" + (rank.isEmpty() ? "" : rank + " ") + player.getUsername());
            this.lookupPreviewLines
               .add(
                  "§fSB Level: "
                     + (int)player.getSkyblockLevel()
                     + "  Profile: "
                     + (player.getSelectedProfile() == null ? "-" : player.getSelectedProfile())
                     + "  Last Online: "
                     + player.getLastLoginDisplayString()
               );
            this.lookupPreviewLines.add("§fStatus: " + (player.isOnlineAtScanTime() ? "ONLINE" : "OFFLINE"));
            this.lookupPreviewLines.add("§8");
            Map<String, Integer> counts = new LinkedHashMap<>();
            List<ScannedItem> items = player.getItems();
            if (items != null) {
               for (ScannedItem it : items) {
                  if (it != null) {
                     String loc = it.getLocation();
                     if (loc == null || loc.isEmpty()) {
                        loc = "Unknown";
                     }

                     counts.put(loc, counts.getOrDefault(loc, 0) + 1);
                  }
               }

               for (Entry<String, Integer> e : counts.entrySet()) {
                  this.lookupPreviewLines.add("§6" + e.getKey() + " §8(" + e.getValue() + ")");
               }

               this.lookupPreviewLines.add("§8");
               this.lookupPreviewLines.add("§fTip: click Open Viewer for full item list.");
            }
         }
      }

      private static int indexOf(String[] arr, String s) {
         for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(s)) {
               return i;
            }
         }

         return -1;
      }
   }
}
