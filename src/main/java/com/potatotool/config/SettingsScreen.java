package com.potatotool.config;

import com.potatotool.PotatoToolMod;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public class SettingsScreen extends Screen {
   private final Screen parent;
   private ScannerConfig config;
   private int currentPage = 0;

   public SettingsScreen(Screen parent) {
      super(Component.literal("PotatoToolV2 Settings"));
      this.parent = parent;
      if (PotatoToolMod.getInstance() != null) {
         this.config = PotatoToolMod.getInstance().getConfig();
      }
   }

   protected void init() {
      super.init();
      if (this.config == null) {
         this.addRenderableWidget(
            Button.builder(Component.literal("Error: Config not loaded"), button -> this.onClose())
               .bounds(this.width / 2 - 100, this.height / 2, 200, 20)
               .build()
         );
      } else {
         int centerX = this.width / 2;
         int startY = 40;
         int buttonWidth = 200;
         int buttonHeight = 20;
         int spacing = 25;
         this.addRenderableWidget(Button.builder(Component.literal(this.currentPage == 0 ? "§a[Basic Settings]" : "Basic Settings"), button -> {
            this.currentPage = 0;
            this.refreshScreen();
         }).bounds(centerX - 205, startY, 100, 20).build());
         this.addRenderableWidget(Button.builder(Component.literal(this.currentPage == 1 ? "§a[Advanced Filters]" : "Advanced Filters"), button -> {
            this.currentPage = 1;
            this.refreshScreen();
         }).bounds(centerX + 105, startY, 100, 20).build());
         int currentY = startY + 30;
         if (this.currentPage == 0) {
            currentY = this.addBasicSettingsButtons(centerX, currentY, buttonWidth, buttonHeight, spacing);
         } else {
            currentY = this.addAdvancedFilterButtons(centerX, currentY, buttonWidth, buttonHeight, spacing);
         }

         this.addRenderableWidget(
            Button.builder(Component.literal("Done"), button -> this.onClose())
               .bounds(centerX - 100, this.height - 30, 200, 20)
               .build()
         );
      }
   }

   private int addBasicSettingsButtons(int centerX, int startY, int buttonWidth, int buttonHeight, int spacing) {
      int currentY = startY;
      int keyCount = this.config.apiKeys.size();
      String keyText = keyCount == 0 ? "No API Keys (Use /scannerkey add <key>)" : keyCount + " API Key(s) Configured";
      this.addRenderableWidget(
         Button.builder(Component.literal(keyText), button -> {})
            .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
            .build()
      );
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Weapons: " + (this.config.scanWeapons ? "§aON" : "§cOFF")), button -> {
         this.config.scanWeapons = !this.config.scanWeapons;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Armor: " + (this.config.scanArmor ? "§aON" : "§cOFF")), button -> {
         this.config.scanArmor = !this.config.scanArmor;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(
         Button.builder(Component.literal("Valuable Items: " + (this.config.valuableItemsEnabled ? "§aON" : "§cOFF")), button -> {
            this.config.valuableItemsEnabled = !this.config.valuableItemsEnabled;
            this.config.save();
            this.refreshScreen();
         }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build()
      );
      currentY += spacing;
      this.addRenderableWidget(
         Button.builder(Component.literal("Anniversary Hats: " + (this.config.scanAnniversaryHats ? "§aON" : "§cOFF")), button -> {
            this.config.scanAnniversaryHats = !this.config.scanAnniversaryHats;
            this.config.save();
            this.refreshScreen();
         }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build()
      );
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Fairy Armor: " + (this.config.fairyScanningEnabled ? "§aON" : "§cOFF")), button -> {
         this.config.fairyScanningEnabled = !this.config.fairyScanningEnabled;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(
         Button.builder(Component.literal("Crystal Armor: " + (this.config.crystalScanningEnabled ? "§aON" : "§cOFF")), button -> {
            this.config.crystalScanningEnabled = !this.config.crystalScanningEnabled;
            this.config.save();
            this.refreshScreen();
         }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build()
      );
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Exotic Armor: " + (this.config.exoticScanningEnabled ? "§aON" : "§cOFF")), button -> {
         this.config.exoticScanningEnabled = !this.config.exoticScanningEnabled;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("OG Fairy: " + (this.config.ogFairyScanningEnabled ? "§aON" : "§cOFF")), button -> {
         this.config.ogFairyScanningEnabled = !this.config.ogFairyScanningEnabled;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Bleached: " + (this.config.bleachedScanningEnabled ? "§aON" : "§cOFF")), button -> {
         this.config.bleachedScanningEnabled = !this.config.bleachedScanningEnabled;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      return currentY + spacing;
   }

   private int addAdvancedFilterButtons(int centerX, int startY, int buttonWidth, int buttonHeight, int spacing) {
      int currentY = startY;
      this.addRenderableWidget(
         Button.builder(
               Component.literal("Level Cap: " + (this.config.skyblockLevelCap == 0.0 ? "§cOFF" : "§a" + (int)this.config.skyblockLevelCap)), button -> {
                  if (this.config.skyblockLevelCap == 0.0) {
                     this.config.skyblockLevelCap = 100.0;
                  } else if (this.config.skyblockLevelCap == 100.0) {
                     this.config.skyblockLevelCap = 150.0;
                  } else if (this.config.skyblockLevelCap == 150.0) {
                     this.config.skyblockLevelCap = 200.0;
                  } else if (this.config.skyblockLevelCap == 200.0) {
                     this.config.skyblockLevelCap = 300.0;
                  } else if (this.config.skyblockLevelCap == 300.0) {
                     this.config.skyblockLevelCap = 400.0;
                  } else {
                     this.config.skyblockLevelCap = 0.0;
                  }

                  this.config.save();
                  this.refreshScreen();
               }
            )
            .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
            .build()
      );
      currentY += spacing;
      this.addRenderableWidget(
         Button.builder(
               Component.literal("Min Level: " + (this.config.minSkyblockLevel == 0.0 ? "§cOFF" : "§a" + (int)this.config.minSkyblockLevel)), button -> {
                  if (this.config.minSkyblockLevel == 0.0) {
                     this.config.minSkyblockLevel = 10.0;
                  } else if (this.config.minSkyblockLevel == 10.0) {
                     this.config.minSkyblockLevel = 25.0;
                  } else if (this.config.minSkyblockLevel == 25.0) {
                     this.config.minSkyblockLevel = 50.0;
                  } else if (this.config.minSkyblockLevel == 50.0) {
                     this.config.minSkyblockLevel = 100.0;
                  } else if (this.config.minSkyblockLevel == 100.0) {
                     this.config.minSkyblockLevel = 150.0;
                  } else {
                     this.config.minSkyblockLevel = 0.0;
                  }

                  this.config.save();
                  this.refreshScreen();
               }
            )
            .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
            .build()
      );
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Max Players: §a" + this.config.maxPlayersToScan), button -> {
         if (this.config.maxPlayersToScan == 10) {
            this.config.maxPlayersToScan = 25;
         } else if (this.config.maxPlayersToScan == 25) {
            this.config.maxPlayersToScan = 50;
         } else if (this.config.maxPlayersToScan == 50) {
            this.config.maxPlayersToScan = 75;
         } else if (this.config.maxPlayersToScan == 75) {
            this.config.maxPlayersToScan = 100;
         } else {
            this.config.maxPlayersToScan = 10;
         }

         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Soulbound Items: " + (this.config.scanSoulboundItems ? "§aON" : "§cOFF")), button -> {
         this.config.scanSoulboundItems = !this.config.scanSoulboundItems;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Pets: " + (this.config.scanPets ? "§aON" : "§cOFF")), button -> {
         this.config.scanPets = !this.config.scanPets;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Tools: " + (this.config.scanTools ? "§aON" : "§cOFF")), button -> {
         this.config.scanTools = !this.config.scanTools;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Accessories: " + (this.config.scanAccessories ? "§aON" : "§cOFF")), button -> {
         this.config.scanAccessories = !this.config.scanAccessories;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      currentY += spacing;
      this.addRenderableWidget(
         Button.builder(Component.literal("Legacy Reforges: " + (this.config.legacyReforgeEnabled ? "§aON" : "§cOFF")), button -> {
            this.config.legacyReforgeEnabled = !this.config.legacyReforgeEnabled;
            this.config.save();
            this.refreshScreen();
         }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build()
      );
      currentY += spacing;
      this.addRenderableWidget(Button.builder(Component.literal("Ghost Reforges: " + (this.config.ghostReforgeEnabled ? "§aON" : "§cOFF")), button -> {
         this.config.ghostReforgeEnabled = !this.config.ghostReforgeEnabled;
         this.config.save();
         this.refreshScreen();
      }).bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
      return currentY + spacing;
   }

   private String formatCoins(long value) {
      if (value >= 1000000000L) {
         return value / 1000000000L + "B";
      } else if (value >= 1000000L) {
         return value / 1000000L + "M";
      } else {
         return value >= 1000L ? value / 1000L + "K" : String.valueOf(value);
      }
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      super.extractRenderState(context, mouseX, mouseY, delta);
      String pageTitle = this.currentPage == 0 ? "Basic Settings" : "Advanced Filters";
      Component titleText = Component.literal(this.title.getString() + " - " + pageTitle);
      int titleWidth = this.font.width(titleText);
      context.text(this.font, titleText, (this.width - titleWidth) / 2, 15, 16777215, true);
      if (this.config != null) {
         if (this.currentPage == 0 && this.config.apiKeys.isEmpty()) {
            Component instructionText = Component.literal("§eAdd an API key with: §6/scannerkey add <key>");
            int instructionWidth = this.font.width(instructionText);
            context.text(this.font, instructionText, (this.width - instructionWidth) / 2, this.height - 50, 16777215, true);
         } else if (this.currentPage == 1) {
            Component instructionText = Component.literal("§7Click buttons to cycle through values");
            int instructionWidth = this.font.width(instructionText);
            context.text(this.font, instructionText, (this.width - instructionWidth) / 2, this.height - 50, 16777215, true);
         }
      }
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }

   private void refreshScreen() {
      this.clearWidgets();
      this.init();
   }
}
