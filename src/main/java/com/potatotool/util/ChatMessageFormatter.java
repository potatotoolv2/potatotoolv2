package com.potatotool.util;

import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public class ChatMessageFormatter {
   private static final int MAX_ITEMS_SHOWN = 4;
   private static final String ISRAEL_SEGMENT = "§9━§f━";
   private static final String POTATO_SEGMENT = "§6━";

   private static String getBorderTheme() {
      if (PotatoToolMod.getInstance() != null) {
         ScannerConfig c = PotatoToolMod.getInstance().getConfig();
         if (c != null && c.hudBorderTheme != null && !c.hudBorderTheme.isEmpty()) {
            return c.hudBorderTheme.toUpperCase();
         }
      }

      return "WHITE";
   }

   private static Component themedBorder() {
      return switch (getBorderTheme()) {
         case "ISRAEL" -> Component.literal(ISRAEL_SEGMENT.repeat(6));
         case "POTATO" -> Component.literal(POTATO_SEGMENT.repeat(8));
         case "BLUE", "NEBULA", "RAINBOW" -> PotatoTheme.chatBorder();
         default -> Component.literal("§f" + "━".repeat(18));
      };
   }

   public static void sendPlayerScanResults(ScannedPlayer player, int totalPlayersInLobby, int newlyScannedCount, int apiDisabledCount) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         sendPlayerBlock(player);
      }
   }

   public static void sendPlayerBlock(ScannedPlayer player) {
      sendMessage(themedBorder());
      int level = (int)player.getSkyblockLevel();
      String rank = player.getRankFormatted();
      MutableComponent header = Component.literal("");
      if (rank != null && !rank.isEmpty()) {
         header.append(Component.literal(rank + " "));
      }

      header.append(clickablePartyUsername(player.getUsername(), player.getRankNameColor()));
      header.append(clickablePartyLink(player.getUsername()));
      sendMessage(header);

      MutableComponent meta = Component.literal("§7Lv ");
      meta.append(Component.literal(levelColor(level) + "§l" + level));

      String selected = player.getSelectedProfile();
      if (selected != null && !selected.isEmpty()) {
         meta.append(Component.literal(" §8| "));
         meta.append(ProfileStyle.component(selected));
         meta.append(Component.literal(" §a★"));
      }

      List<ScannedPlayer.ProfileInfo> profiles = player.getProfiles();
      if (profiles != null) {
         for (ScannedPlayer.ProfileInfo info : profiles) {
            if (info.isSelected()) {
               continue;
            }

            String name = info.getName() == null || info.getName().isBlank() ? "Unknown" : info.getName();
            meta.append(Component.literal(" §8| "));
            meta.append(ProfileStyle.component(name));
            meta.append(Component.literal(" §7Lv " + levelColor(info.getLevel()) + info.getLevel()));
            meta.append(Component.literal(ProfileStyle.modeTag(info.getGameMode())));
         }
      }

      List<ScannedItem> items = player.getDisplayableItems();
      int itemCount = items.size();
      if (itemCount > 0) {
         meta.append(Component.literal(" §8| §f" + itemCount + (itemCount == 1 ? " item" : " items")));
         int seymourTotal = player.getSeymourPieceCount();
         if (seymourTotal > 0) {
            meta.append(Component.literal(" §8| §dSeymour " + seymourTotal));
            int seymourRare = player.getSeymourRareCount();
            if (seymourRare > 0) {
               meta.append(Component.literal(" §7(" + seymourRare + " rare)"));
            }
         }
      }

      sendMessage(meta);

      if (itemCount > 0) {
         int shown = Math.min(MAX_ITEMS_SHOWN, itemCount);
         for (int i = 0; i < shown; i++) {
            sendMessage(formatItemChatLine(items.get(i)));
         }

         if (itemCount > MAX_ITEMS_SHOWN) {
            int extra = itemCount - MAX_ITEMS_SHOWN;
            MutableComponent hover = Component.empty();
            for (int i = MAX_ITEMS_SHOWN; i < itemCount; i++) {
               if (i > MAX_ITEMS_SHOWN) {
                  hover.append(Component.literal("\n"));
               }

               hover.append(formatItemHoverLine(items.get(i)));
            }

            sendMessage(
               Component.literal("§8+" + extra + " more")
                  .withStyle(s -> s.withHoverEvent(new ShowText(hover)))
            );
         }
      }

      sendMessage(themedBorder());
   }

   private static String itemCategoryLabel(ScannedItem item) {
      if (item.getCategory() == ScannedItem.ItemCategory.CAKE && item.getCakeYear() != null) {
         return "Y" + item.getCakeYear();
      }

      String catTag = item.getCategoryTag();
      return catTag == null || catTag.isEmpty() ? "Item" : catTag;
   }

   private static String formatLocation(String location) {
      if (location == null || location.isBlank()) {
         return "";
      }

      if (location.regionMatches(true, 0, "Ender Chest", 0, "Ender Chest".length())) {
         return "Enderchest" + location.substring("Ender Chest".length());
      }

      return location;
   }

   private static MutableComponent formatItemChatLine(ScannedItem item) {
      String itemName = ItemNames.resolve(item);
      String catTag = itemCategoryLabel(item);
      MutableComponent itemLine = Component.literal("§8• ");
      String hex = item.getHexColor();
      if (hex != null && !hex.isEmpty()) {
         int rgb = ColorAnalyzer.hexToInt(hex);
         TextColor textColor = TextColor.fromRgb(rgb);
         itemLine.append(Component.literal(itemName).withStyle(s -> s.withColor(textColor)));
         String hexDisplay = hex.startsWith("#") ? hex : "#" + hex;
         itemLine.append(Component.literal(" ").append(Component.literal(hexDisplay).withStyle(s -> s.withColor(textColor))));
      } else {
         itemLine.append(Component.literal(getCategoryColor(item.getCategory()) + itemName));
      }

      itemLine.append(Component.literal("  " + getCategoryColor(item.getCategory()) + catTag));
      String loc = formatLocation(item.getLocation());
      if (!loc.isEmpty()) {
         itemLine.append(Component.literal(" §8| §7" + loc));
      }

      String profile = item.getProfileName();
      if (profile != null && !profile.isBlank()) {
         itemLine.append(Component.literal(" "));
         itemLine.append(ProfileStyle.component(profile));
      }

      return itemLine;
   }

   private static MutableComponent formatItemHoverLine(ScannedItem item) {
      String itemName = ItemNames.resolve(item);
      String catTag = itemCategoryLabel(item);
      String hex = item.getHexColor();
      String loc = formatLocation(item.getLocation());
      String profile = item.getProfileName();
      MutableComponent line = Component.literal("• ");
      if (hex != null && !hex.isEmpty()) {
         int rgb = ColorAnalyzer.hexToInt(hex);
         String hexDisplay = hex.startsWith("#") ? hex : "#" + hex;
         line.append(Component.literal(itemName).withStyle(s -> s.withColor(TextColor.fromRgb(rgb))));
         line.append(Component.literal(" " + hexDisplay).withStyle(s -> s.withColor(TextColor.fromRgb(rgb))));
      } else {
         line.append(Component.literal(itemName));
      }

      line.append(Component.literal("  " + getCategoryColor(item.getCategory()) + catTag + (loc.isEmpty() ? "" : " §8| §7" + loc)));
      if (profile != null && !profile.isBlank()) {
         line.append(Component.literal(" "));
         line.append(ProfileStyle.component(profile));
      }

      return line;
   }

   public static void sendLobbyScanResults(Collection<ScannedPlayer> scannedPlayers, int playersTargeted, int noDataCount, int newPlayersScanned) {
      int withItems = (int)scannedPlayers.stream().filter(ScannedPlayer::hasDisplayableItems).count();
      sendLobbySummary(playersTargeted, newPlayersScanned, noDataCount, 0, withItems);
   }

   public static void sendLobbyScanSummaryOnly(
      Collection<ScannedPlayer> scannedPlayers, int playersTargeted, int noDataCount, int newPlayersScanned
   ) {
      sendLobbyScanSummaryOnly(scannedPlayers, playersTargeted, noDataCount, newPlayersScanned, 0, -1);
   }

   public static void sendLobbyScanSummaryOnly(
      Collection<ScannedPlayer> scannedPlayers,
      int playersTargeted,
      int noDataCount,
      int newPlayersScanned,
      int alreadySeen,
      int hitsThisRun
   ) {
      int withItems = hitsThisRun >= 0
         ? hitsThisRun
         : (int)scannedPlayers.stream().filter(ScannedPlayer::hasDisplayableItems).count();
      sendLobbySummary(playersTargeted, newPlayersScanned, noDataCount, alreadySeen, withItems);
   }

   public static void sendAutoScanReport(int playersTargeted, int newPlayersScanned, int noDataCount) {
      sendAutoScanReport(playersTargeted, newPlayersScanned, noDataCount, 0, -1);
   }

   public static void sendAutoScanReport(int playersTargeted, int newPlayersScanned, int noDataCount, int alreadySeen, int hitsThisRun) {
      int withItems = hitsThisRun;
      if (withItems < 0) {
         withItems = 0;
         if (PotatoToolMod.getInstance() != null && PotatoToolMod.getInstance().getDataStorage() != null) {
            withItems = PotatoToolMod.getInstance().getDataStorage().getPlayersWithDisplayableItems().size();
         }
      }

      sendLobbySummary(playersTargeted, newPlayersScanned, noDataCount, alreadySeen, withItems);
   }

   public static void sendLobbySummary(int attempted, int newPlayers, int noData, int alreadySeen, int hits) {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return;
      }

      MutableComponent line = PotatoTheme.branded("PT2");
      line.append(Component.literal(" §8· §e" + newPlayers + " §6new"));
      line.append(Component.literal(" §8· §7" + alreadySeen + " in cache"));
      line.append(Component.literal(" §8· §c" + noData + " no api"));
      if (hits > 0) {
         line.append(Component.literal(" §8· §a" + hits + " hit" + (hits == 1 ? "" : "s")));
      }

      sendMessage(line);
      if (newPlayers == 0 && attempted > 0 && alreadySeen == 0) {
         sendMessage(Component.literal("§8Add a key: §e/scannerkey add <key> §8· §bdeveloper.hypixel.net"));
      }
   }

   public static MutableComponent clickablePartyUsername(String username) {
      return clickablePartyUsername(username, "§e");
   }

   public static MutableComponent clickablePartyUsername(String username, String color) {
      String code = color == null || color.isBlank() ? "§e" : color;
      return username != null && !username.isEmpty()
         ? Component.literal(code + username)
            .withStyle(
               s -> s.withUnderlined(true)
                  .withClickEvent(new RunCommand("/ptparty " + username))
                  .withHoverEvent(new ShowText(Component.literal("Click to party " + username)))
            )
         : Component.literal("");
   }

   private static MutableComponent clickablePartyLink(String username) {
      return username != null && !username.isEmpty()
         ? Component.literal(" §a[PARTY]")
            .withStyle(
               s -> s.withClickEvent(new RunCommand("/ptparty " + username))
                  .withHoverEvent(new ShowText(Component.literal("Party " + username)))
            )
         : Component.literal("");
   }

   private static void sendMessage(Component message) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         client.player.sendSystemMessage(message);
      }
   }

   private static String levelColor(int level) {
      if (level >= 500) {
         return "§d";
      }
      if (level >= 400) {
         return "§5";
      }
      if (level >= 300) {
         return "§9";
      }
      if (level >= 200) {
         return "§6";
      }
      if (level >= 150) {
         return "§e";
      }
      if (level >= 100) {
         return "§2";
      }
      return level >= 50 ? "§a" : "§7";
   }

   private static String getCategoryColor(ScannedItem.ItemCategory category) {
      if (category == null) {
         return "§7";
      }

      return switch (category) {
         case VALUABLE -> "§6";
         case CRYSTAL -> "§c";
         case OG_FAIRY -> "§5";
         case FAIRY -> "§d";
         case BLEACHED -> "§f";
         case EXOTIC -> "§e";
         case SPECIFIC_HEX -> "§3";
         case GLITCHED -> "§5";
         case LEGACY_REFORGE -> "§c";
         case GHOST_REFORGE -> "§d";
         case CAKE -> "§e";
         case COSMETIC_SKIN -> "§d";
         case SEYMOUR_T1 -> "§a";
         case SEYMOUR_T2 -> "§e";
         case SEYMOUR_T3 -> "§c";
         default -> "§7";
      };
   }
}
