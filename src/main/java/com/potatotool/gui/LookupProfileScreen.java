package com.potatotool.gui;

import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import com.potatotool.util.PotatoTheme;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public class LookupProfileScreen extends Screen {
   private final Screen parent;
   private final ScannedPlayer player;
   private final List<String> lines = new ArrayList<>();
   private int scroll = 0;
   private int maxScroll = 0;
   private static final int LINE_H = 11;

   public LookupProfileScreen(Screen parent, ScannedPlayer player) {
      super(Component.literal("PotatoToolV2 Lookup"));
      this.parent = parent;
      this.player = player;
   }

   protected void init() {
      super.init();
      this.buildLines();
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.maxScroll <= 0) {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      } else {
         int delta = verticalAmount > 0.0 ? -22 : (verticalAmount < 0.0 ? 22 : 0);
         if (delta != 0) {
            this.scroll = Math.max(0, Math.min(this.maxScroll, this.scroll + delta));
            return true;
         } else {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
         }
      }
   }

   public boolean keyPressed(KeyEvent keyEvent) {
      if (keyEvent.isEscape()) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyEvent);
      }
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      super.extractRenderState(context, mouseX, mouseY, delta);
      int w = Math.min(940, this.width - 20);
      int h = Math.min(560, this.height - 20);
      int x = (this.width - w) / 2;
      int y = (this.height - h) / 2;
      context.fill(x, y, x + w, y + h, PotatoTheme.LOOKUP_EDGE);
      context.fill(x + 1, y + 1, x + w - 1, y + h - 1, PotatoTheme.PANEL);
      context.fill(x, y, x + w, y + 22, PotatoTheme.HEADER);
      int[] strip = PotatoTheme.nebulaStrip();
      int segW = Math.max(1, w / strip.length);
      for (int i = 0; i < strip.length; i++) {
         int sx = x + i * segW;
         int sw = i == strip.length - 1 ? x + w - sx : segW;
         if (sw > 0) {
            context.fill(sx, y + 22, sx + sw, y + 25, strip[(i + PotatoTheme.stripOffset(strip.length)) % strip.length]);
         }
      }
      String rank = this.player.getRankFormatted();
      String username = this.player.getUsername() == null ? "Unknown" : this.player.getUsername();
      String header = "PotatoToolV2 Lookup  ·  " + (rank.isEmpty() ? "" : rank + " ") + username;
      context.text(this.font, header, x + 8, y + 7, PotatoTheme.TEXT, false);
      context.text(this.font, "Press ESC to close", x + w - 96, y + 7, PotatoTheme.MUTED, false);
      int infoY = y + 28;
      String online = this.player.isOnlineAtScanTime() ? "§aONLINE NOW" : "§7OFFLINE";
      String infoLine = "SB Level: §b"
         + (int)this.player.getSkyblockLevel()
         + "§7  ·  "
         + com.potatotool.util.ProfileStyle.legacy(this.player.getSelectedProfile())
         + "§7  ·  Last Online: §f"
         + this.player.getLastLoginDisplayString()
         + "§7  ·  Status: "
         + online;
      context.text(this.font, infoLine, x + 8, infoY, -1907998, false);
      int listTop = y + 44;
      int listBottom = y + h - 12;
      int listH = Math.max(40, listBottom - listTop);
      context.fill(x + 6, listTop - 2, x + w - 6, listBottom + 2, 1879048192);
      int contentH = this.lines.size() * 11;
      this.maxScroll = Math.max(0, contentH - listH);
      if (this.scroll > this.maxScroll) {
         this.scroll = this.maxScroll;
      }

      int first = this.scroll / 11;
      int yOffset = -(this.scroll % 11);
      int drawY = listTop + yOffset;

      for (int i = first; i < this.lines.size() && drawY <= listBottom - 11; i++) {
         String line = this.lines.get(i);
         int color = line.startsWith("§6") ? -11410 : (line.startsWith("§8") ? -6642771 : -1381654);
         context.text(this.font, line, x + 10, drawY, color, false);
         drawY += 11;
      }

      if (this.maxScroll > 0) {
         int sbX0 = x + w - 10;
         int sbX1 = x + w - 7;
         int thumbH = Math.max(12, listH * listH / Math.max(1, contentH));
         int thumbY = listTop + (int)((long)this.scroll * (listH - thumbH) / this.maxScroll);
         context.fill(sbX0, listTop, sbX1, listBottom, 1426063360);
         context.fill(sbX0, thumbY, sbX1, thumbY + thumbH, -9793611);
      }
   }

   private void buildLines() {
      this.lines.clear();
      List<ScannedItem> items = this.player.getItems();
      if (items != null && !items.isEmpty()) {
         Map<String, List<ScannedItem>> byLocation = new LinkedHashMap<>();

         for (ScannedItem item : items) {
            if (item != null) {
               String loc = normalizeLocation(item.getLocation());
               byLocation.computeIfAbsent(loc, k -> new ArrayList<>()).add(item);
            }
         }

         String[] preferred = new String[]{"Armor", "Equipment", "Wardrobe", "Inventory", "Enderchest", "Backpack", "Vault"};
         for (String prefix : preferred) {
            List<String> match = new ArrayList<>();
            for (String key : byLocation.keySet()) {
               if (key.equals(prefix) || key.startsWith(prefix + " ")) {
                  match.add(key);
               }
            }

            match.sort((a, b) -> {
               int pa = locationPage(a);
               int pb = locationPage(b);
               return pa != pb ? Integer.compare(pa, pb) : a.compareToIgnoreCase(b);
            });
            for (String key : match) {
               this.addLocationBlock(byLocation, key);
            }
         }

         for (String key : new ArrayList<>(byLocation.keySet())) {
            this.addLocationBlock(byLocation, key);
         }
      } else {
         this.lines.add("§8No scanned items found for this profile.");
      }
   }

   private void addLocationBlock(Map<String, List<ScannedItem>> byLocation, String key) {
      List<ScannedItem> list = byLocation.remove(key);
      if (list != null && !list.isEmpty()) {
         this.lines.add("§6" + key + " §8(" + list.size() + ")");
         list.sort((a, b) -> {
            String an = a.getItemName() == null ? "" : a.getItemName().toLowerCase(Locale.ROOT);
            String bn = b.getItemName() == null ? "" : b.getItemName().toLowerCase(Locale.ROOT);
            return an.compareTo(bn);
         });

         for (ScannedItem item : list) {
            String hex = item.getHexColor();
            String hexPart = hex != null && !hex.isEmpty() ? "  §8#" + hex.replace("#", "").toUpperCase(Locale.ROOT) : "";
            String cat = item.getCategoryTag();
            String catPart = cat != null && !cat.isEmpty() ? "  §7[" + cat + "]" : "";
            String name = item.getItemName() == null ? item.getItemId() : item.getItemName();
            this.lines.add("§f• " + name + catPart + hexPart);
         }

         this.lines.add("§8");
      }
   }

   private static int locationPage(String key) {
      int space = key.lastIndexOf(' ');
      if (space < 0) {
         return 0;
      }

      try {
         return Integer.parseInt(key.substring(space + 1).trim());
      } catch (NumberFormatException e) {
         return 0;
      }
   }

   private static String normalizeLocation(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String s = raw.trim();
         String lower = s.toLowerCase(Locale.ROOT);
         if (lower.contains("ender")) {
            String rest = s.replaceAll("(?i)ender\\s*chest", "").trim();
            return rest.isEmpty() ? "Enderchest" : "Enderchest " + rest;
         } else if (lower.startsWith("backpack")) {
            return s;
         } else if (lower.startsWith("wardrobe")) {
            return s;
         } else if (lower.contains("armor")) {
            return "Armor";
         } else if (lower.contains("equipment")) {
            return "Equipment";
         } else if (lower.contains("vault")) {
            return "Vault";
         } else {
            return lower.contains("inventory") ? "Inventory" : s;
         }
      } else {
         return "Unknown";
      }
   }

   public void onClose() {
      if (this.minecraft != null) {
         this.minecraft.setScreen(this.parent);
      }
   }
}
