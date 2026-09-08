package com.potatotool.config;

import com.potatotool.PotatoToolMod;
import com.potatotool.util.PotatoTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public class ModernSettingsScreen extends Screen {
   private final Screen parent;
   private ScannerConfig config;
   private int currentTab = 1;
   private int cakeAddDraft = 26;
   private String shareStatus = "";
   private static final int PANEL_W = 520;
   private static final int HEADER_H = 56;
   private static final int TAB_H = 24;
   private static final int BTN_H = 22;
   private static final int SPACING = 22;
   private static final int SECTION_GAP = 8;
   private static final int BG = PotatoTheme.BG;
   private static final int PANEL = PotatoTheme.PANEL;
   private static final int BORDER = PotatoTheme.BORDER;
   private static final int HEADER = PotatoTheme.HEADER;
   private static final int TAB_ACTIVE = PotatoTheme.TAB;
   private static final int TEXT_MAIN = PotatoTheme.TEXT;
   private static final int TEXT_MUTED = PotatoTheme.MUTED;
   private final List<ModernSettingsScreen.SectionLabel> sectionLabels = new ArrayList<>();
   private static volatile Screen pendingOpen = null;

   public ModernSettingsScreen(Screen parent) {
      super(Component.literal("PotatoToolV2 Settings"));
      this.parent = parent;
      if (PotatoToolMod.getInstance() != null) {
         this.config = PotatoToolMod.getInstance().getConfig();
      }
   }

   public static void open(Minecraft client) {
      if (client != null) {
         Screen parent = client.screen;
         pendingOpen = new ModernSettingsScreen(parent);
      }
   }

   public static void tickOpen(Minecraft client) {
      Screen next = pendingOpen;
      if (next != null && client != null) {
         pendingOpen = null;
         client.setScreen(next);
      }
   }

   protected void init() {
      super.init();
      this.sectionLabels.clear();
      if (PotatoToolMod.getInstance() != null && this.config == null) {
         this.config = PotatoToolMod.getInstance().getConfig();
      }

      if (this.config == null) {
         this.addRenderableWidget(
            Button.builder(Component.literal("§cConfig not loaded"), b -> this.onClose())
               .bounds(this.width / 2 - 100, this.height / 2 - 10, 200, 20)
               .build()
         );
      } else {
         int cx = this.width / 2;
         int pw = Math.min(520, this.width - 32);
         int px = cx - pw / 2;
         int top = 24;
         int tabY = top + 56 + 6;
         int contentY = tabY + 24 + 14;
         int tabW = (pw - 32) / 5;
         int tabGap = 4;
         this.addRenderableWidget(this.tab("Items", 0, px + 8, tabY, tabW, 24));
         this.addRenderableWidget(this.tab("Level", 1, px + 8 + tabW + tabGap, tabY, tabW, 24));
         this.addRenderableWidget(this.tab("Cakes", 2, px + 8 + (tabW + tabGap) * 2, tabY, tabW, 24));
         this.addRenderableWidget(this.tab("Skin", 3, px + 8 + (tabW + tabGap) * 3, tabY, tabW, 24));
         this.addRenderableWidget(this.tab("Share", 4, px + 8 + (tabW + tabGap) * 4, tabY, tabW, 24));
         int y = contentY;
         switch (this.currentTab) {
            case 0:
               y = this.buildItemsTab(px + 12, y, pw - 24, 22, 22);
               break;
            case 1:
               y = this.buildLevelTab(px + 12, y, pw - 24, 22, 22);
               break;
            case 2:
               y = this.buildCakesTab(px + 12, y, pw - 24, 22, 22);
               break;
            case 3:
               y = this.buildSkinValueTab(px + 12, y, pw - 24, 22, 22);
               break;
            case 4:
               y = this.buildShareTab(px + 12, y, pw - 24, 22, 22);
         }

         this.addRenderableWidget(Button.builder(Component.literal("§aSave & close"), b -> {
            this.config.save();
            this.onClose();
         }).bounds(cx - 80, this.height - 32, 160, 22).build());
      }
   }

   private Button tab(String label, int index, int x, int y, int w, int h) {
      boolean active = this.currentTab == index;
      Component t = Component.literal(active ? "§f§l" + label : "§7" + label);
      return Button.builder(t, b -> {
         if (this.currentTab != index) {
            this.currentTab = index;
            this.refresh();
         }
      }).bounds(x, y, w, h).build();
   }

   private int section(String text, int x, int y, int w) {
      this.sectionLabels.add(new ModernSettingsScreen.SectionLabel(text, x, y, w));
      return y + 14;
   }

   private int buildItemsTab(int x, int y, int w, int btnH, int spacing) {
      y = this.section("§e◆ Item categories", x, y, w);
      y += 8;
      y = this.toggle("Exotic armor", this.config.exoticScanningEnabled, v -> this.config.exoticScanningEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Fairy armor", this.config.fairyScanningEnabled, v -> this.config.fairyScanningEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("OG Fairy armor", this.config.ogFairyScanningEnabled, v -> this.config.ogFairyScanningEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Crystal armor", this.config.crystalScanningEnabled, v -> this.config.crystalScanningEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Bleached armor", this.config.bleachedScanningEnabled, v -> this.config.bleachedScanningEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Valuable items", this.config.valuableItemsEnabled, v -> this.config.valuableItemsEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Anniversary hats (crab/sloth/balloon)", this.config.scanAnniversaryHats, v -> this.config.scanAnniversaryHats = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("New Year cakes", this.config.newYearCakeEnabled, v -> this.config.newYearCakeEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Legacy reforges", this.config.legacyReforgeEnabled, v -> this.config.legacyReforgeEnabled = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Ghost reforges", this.config.ghostReforgeEnabled, v -> this.config.ghostReforgeEnabled = v, x, y, w, btnH);
      y += spacing + 4;
      y = this.section("§9◆ Item types to scan", x, y, w);
      y += 8;
      y = this.toggle("Weapons", this.config.scanWeapons, v -> this.config.scanWeapons = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Armor", this.config.scanArmor, v -> this.config.scanArmor = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Accessories", this.config.scanAccessories, v -> this.config.scanAccessories = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Tools", this.config.scanTools, v -> this.config.scanTools = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Pets", this.config.scanPets, v -> this.config.scanPets = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Consumables", this.config.scanConsumables, v -> this.config.scanConsumables = v, x, y, w, btnH);
      y += spacing;
      return this.toggle("Cosmetics", this.config.scanCosmetics, v -> this.config.scanCosmetics = v, x, y, w, btnH);
   }

   private int buildLevelTab(int x, int y, int w, int btnH, int spacing) {
      y = this.section("§a◆ Level filters", x, y, w);
      y += 8;
      y = this.cycle(
         "Min Skyblock level",
         new int[]{0, 10, 25, 50, 75, 100, 150, 200},
         (int)this.config.minSkyblockLevel,
         v -> this.config.minSkyblockLevel = v.intValue(),
         x,
         y,
         w,
         btnH
      );
      y += spacing;
      y = this.cycle(
         "Max Skyblock level (cap)",
         new int[]{0, 50, 100, 150, 200, 250, 300, 400, 500},
         (int)this.config.skyblockLevelCap,
         v -> this.config.skyblockLevelCap = v.intValue(),
         x,
         y,
         w,
         btnH
      );
      y += spacing;
      y = this.cycle(
         "Max players to scan (0 = entire lobby)",
         new int[]{0, 25, 50, 75, 100, 150, 200},
         this.config.maxPlayersToScan,
         v -> this.config.maxPlayersToScan = v,
         x,
         y,
         w,
         btnH
      );
      y += spacing;
      return this.toggle("Scan low-level players", this.config.scanLowLevelPlayers, v -> this.config.scanLowLevelPlayers = v, x, y, w, btnH);
   }

   private int buildCakesTab(int x, int y, int w, int btnH, int spacing) {
      y = this.section("§6◆ Cake years to scan", x, y, w);
      y += 8;
      y = this.toggle("New Year cakes", this.config.newYearCakeEnabled, v -> this.config.newYearCakeEnabled = v, x, y, w, btnH);
      y += spacing;
      int addW = 70;
      int stepW = 28;
      this.addRenderableWidget(
         Button.builder(Component.literal("−"), b -> {
            this.cakeAddDraft = Math.max(1, this.cakeAddDraft - 1);
            this.refresh();
         }).bounds(x, y, stepW, btnH).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§e" + this.cakeAddDraft), b -> {}).bounds(x + stepW + 4, y, 56, btnH).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("+"), b -> {
            this.cakeAddDraft = Math.min(500, this.cakeAddDraft + 1);
            this.refresh();
         }).bounds(x + stepW + 64, y, stepW, btnH).build()
      );
      this.addRenderableWidget(Button.builder(Component.literal("§aAdd"), b -> {
         this.config.addCakeYear(this.cakeAddDraft);
         this.config.save();
         this.refresh();
      }).bounds(x + stepW + 96, y, addW, btnH).build());
      this.addRenderableWidget(Button.builder(Component.literal("§7Defaults"), b -> {
         this.config.resetCakeYearsToDefaults();
         this.config.save();
         this.refresh();
      }).bounds(x + stepW + 170, y, 80, btnH).build());
      this.addRenderableWidget(Button.builder(Component.literal("§cClear"), b -> {
         this.config.newYearCakeSpecificYears = new ArrayList<>();
         this.config.cakeYearsListMode = true;
         this.config.save();
         this.refresh();
      }).bounds(x + stepW + 254, y, 60, btnH).build());
      y += spacing + 4;
      this.sectionLabels.add(new ModernSettingsScreen.SectionLabel("§7Click a year to remove it", x, y, w));
      y += 16;
      int chipX = x;
      int rowW = w;
      for (Integer year : this.config.newYearCakeSpecificYears == null ? List.<Integer>of() : new ArrayList<>(this.config.newYearCakeSpecificYears)) {
         if (year == null) {
            continue;
         }

         int chipW = 44;
         if (chipX + chipW > x + rowW) {
            chipX = x;
            y += 24;
         }

         int removeYear = year;
         this.addRenderableWidget(Button.builder(Component.literal("§f" + year + " §c×"), b -> {
            this.config.removeCakeYear(removeYear);
            this.config.save();
            this.refresh();
         }).bounds(chipX, y, chipW, 20).build());
         chipX += chipW + 4;
      }

      return y + 24;
   }

   private int buildShareTab(int x, int y, int w, int btnH, int spacing) {
      y = this.section("§b◆ Share settings", x, y, w);
      y += 8;
      this.sectionLabels.add(new ModernSettingsScreen.SectionLabel("§7Copies a PT2- code. API keys and webhook stay private.", x, y, w));
      y += 18;
      this.addRenderableWidget(Button.builder(Component.literal("§aCopy share code"), b -> {
         String code = ConfigShareCodec.exportCode(this.config);
         if (this.minecraft != null && this.minecraft.keyboardHandler != null) {
            this.minecraft.keyboardHandler.setClipboard(code);
         }

         this.shareStatus = "Copied to clipboard";
         this.refresh();
      }).bounds(x, y, (w - 8) / 2, btnH).build());
      this.addRenderableWidget(Button.builder(Component.literal("§eImport from clipboard"), b -> {
         String raw = this.minecraft != null && this.minecraft.keyboardHandler != null ? this.minecraft.keyboardHandler.getClipboard() : "";
         String error = ConfigShareCodec.importCode(this.config, raw);
         this.shareStatus = error == null ? "Imported share code" : error;
         this.refresh();
      }).bounds(x + (w - 8) / 2 + 8, y, (w - 8) / 2, btnH).build());
      y += spacing + 8;
      if (this.shareStatus != null && !this.shareStatus.isEmpty()) {
         this.sectionLabels.add(new ModernSettingsScreen.SectionLabel("§7" + this.shareStatus, x, y, w));
         y += 16;
      }

      this.sectionLabels.add(new ModernSettingsScreen.SectionLabel("§7Or use §e/ptshare export §7and §e/ptshare import <code>", x, y, w));
      y += spacing + 8;
      return this.buildAdvancedTab(x, y, w, btnH, spacing);
   }

   private int buildSkinValueTab(int x, int y, int w, int btnH, int spacing) {
      y = this.section("§d◆ Skin / armor color value", x, y, w);
      y += 8;
      y = this.toggle("Only custom-colored armor", this.config.requireCustomColor, v -> this.config.requireCustomColor = v, x, y, w, btnH);
      y += spacing;
      y = this.cycle(
         "Min color difference from default",
         new int[]{0, 25, 50, 75, 100, 150, 200},
         this.config.minColorDifference,
         v -> this.config.minColorDifference = v,
         x,
         y,
         w,
         btnH
      );
      y += spacing;
      this.sectionLabels.add(new ModernSettingsScreen.SectionLabel("§7Higher = only more distinct colors count", x, y, w));
      return y + 16;
   }

   private int buildAdvancedTab(int x, int y, int w, int btnH, int spacing) {
      y = this.section("§c◆ Scan options", x, y, w);
      y += 8;
      y = this.toggle("Soulbound items", this.config.scanSoulboundItems, v -> this.config.scanSoulboundItems = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Coop soulbound", this.config.scanCoopSoulboundItems, v -> this.config.scanCoopSoulboundItems = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Ironman profiles", this.config.scanIronmanProfiles, v -> this.config.scanIronmanProfiles = v, x, y, w, btnH);
      y += spacing;
      y = this.toggle("Stranded profiles", this.config.scanStrandedProfiles, v -> this.config.scanStrandedProfiles = v, x, y, w, btnH);
      y += spacing + 4;
      y = this.section("§b◆ Display", x, y, w);
      y += 8;
      y = this.toggle("Show player overlay (HUD)", this.config.showPlayerOverlay, v -> this.config.showPlayerOverlay = v, x, y, w, btnH);
      y += spacing + 4;
      y = this.section("§6◆ API", x, y, w);
      y += 8;
      int keys = this.config.getApiKeyCount();
      String keyText = keys == 0 ? "§cNo API keys" : "§a" + keys + " API key(s)";
      this.addRenderableWidget(
         Button.builder(Component.literal("§e\ud83d\udd11 " + keyText + " §7— use §e/scannerkey add <key>"), b -> {})
            .bounds(x, y, w, btnH)
            .build()
      );
      return y + spacing;
   }

   private int toggle(String label, boolean current, Consumer<Boolean> set, int x, int y, int w, int h) {
      String onOff = current ? "§a ON" : "§c OFF";
      this.addRenderableWidget(Button.builder(Component.literal(label + " §8[" + onOff + "§8]"), b -> {
         set.accept(!current);
         this.config.save();
         this.refresh();
      }).bounds(x, y, w, h).build());
      return y;
   }

   private int cycle(String label, int[] values, int current, Consumer<Integer> set, int x, int y, int w, int h) {
      int idx = 0;

      for (int i = 0; i < values.length; idx = i++) {
         if (values[i] == current) {
            idx = i;
            break;
         }

         if (values[i] > current) {
            break;
         }
      }

      int effective = values[idx];
      int next = values[(idx + 1) % values.length];
      String display = formatCycleDisplay(label, effective);
      this.addRenderableWidget(Button.builder(Component.literal(label + " §8[§e" + display + "§8]"), b -> {
         set.accept(next);
         this.config.save();
         this.refresh();
      }).bounds(x, y, w, h).build());
      return y;
   }

   private static String formatCycleDisplay(String label, int value) {
      String lower = label.toLowerCase();
      if (value != 0 || !lower.contains("min") && !lower.contains("minimum")) {
         return value != 0 || !lower.contains("max") && !lower.contains("players") && !lower.contains("cap") ? String.valueOf(value) : "Full lobby";
      } else {
         return "Any";
      }
   }

   private void refresh() {
      this.clearWidgets();
      this.init();
   }

   public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
      super.extractRenderState(ctx, mouseX, mouseY, delta);
      int cx = this.width / 2;
      int pw = Math.min(520, this.width - 32);
      int px = cx - pw / 2;
      int top = 24;
      int panelH = this.height - top * 2;
      ctx.fill(0, 0, this.width, this.height, PotatoTheme.BG);
      ctx.fill(px - 2, top - 2, px + pw + 2, top + panelH + 2, PotatoTheme.BORDER);
      ctx.fill(px, top, px + pw, top + panelH, PotatoTheme.PANEL);
      ctx.fill(px, top, px + pw, top + 56, PotatoTheme.HEADER);
      ctx.fill(px, top + 56 - 1, px + pw, top + 56, PotatoTheme.BORDER);
      int[] strip = PotatoTheme.blueFlowStrip();
      int segW = Math.max(1, pw / strip.length);

      for (int i = 0; i < strip.length; i++) {
         int sx = px + i * segW;
         int sw = i == strip.length - 1 ? px + pw - sx : segW;
         if (sw > 0) {
            ctx.fill(sx, top + 56, sx + sw, top + 56 + 3, strip[(i + PotatoTheme.stripOffset(strip.length)) % strip.length]);
         }
      }

      MutableComponent title = PotatoTheme.branded("PotatoToolV2");
      title.append(Component.literal(" §8— §fSettings"));
      int tw = this.font.width(title);
      ctx.text(this.font, title, px + (pw - tw) / 2, top + 8, PotatoTheme.TEXT, false);
      String[] tabNames = new String[]{"Items", "Level", "Cakes", "Skin", "Share"};
      ctx.text(this.font, Component.literal("§7" + tabNames[this.currentTab]), px + 12, top + 56 + 8, PotatoTheme.MUTED, false);
      int tabW = (pw - 32) / 5;
      int tabGap = 4;
      int tabY = top + 56 + 6;
      int indX = px + 8 + this.currentTab * (tabW + tabGap);
      ctx.fill(indX, tabY + 24, indX + tabW, tabY + 24 + 2, 0xFF000000 | PotatoTheme.accentNow());

      for (ModernSettingsScreen.SectionLabel s : this.sectionLabels) {
         ctx.fill(s.x, s.y + 4, s.x + s.w, s.y + 5, PotatoTheme.BORDER);
         ctx.text(this.font, Component.literal(s.text), s.x, s.y - 2, PotatoTheme.MUTED, false);
      }

      String hint = "§7Save & close saves all changes.";
      if (this.config != null && this.config.apiKeys != null && this.config.apiKeys.isEmpty()) {
         hint = "§eAdd API key: §6/scannerkey add <key>";
      }

      int hw = this.font.width(hint);
      ctx.text(this.font, Component.literal(hint), cx - hw / 2, this.height - 42, -6250336, false);
   }

   public void onClose() {
      if (this.config != null) {
         this.config.save();
      }

      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   public boolean isPauseScreen() {
      return true;
   }

   private static class SectionLabel {
      final String text;
      final int x;
      final int y;
      final int w;

      SectionLabel(String text, int x, int y, int w) {
         this.text = text;
         this.x = x;
         this.y = y;
         this.w = w;
      }
   }
}
