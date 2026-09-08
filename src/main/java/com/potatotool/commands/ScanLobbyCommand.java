package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.potatotool.PotatoToolMod;
import com.potatotool.manager.LobbyScanCache;
import com.potatotool.manager.PlayerScannerManager;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import com.potatotool.util.ChatMessageFormatter;
import com.potatotool.util.ColorAnalyzer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public class ScanLobbyCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PlayerScannerManager scanner) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("scan")
                  .then(ClientCommands.literal("lobby").executes(context -> scanLobby(context, scanner))))
               .then(ClientCommands.literal("list").executes(ScanLobbyCommand::scanList)))
            .then(
               ClientCommands.literal("search")
                  .then(ClientCommands.argument("keyword", StringArgumentType.greedyString()).executes(ScanLobbyCommand::scanSearch))
            )
      );
   }

   private static int scanList(CommandContext<FabricClientCommandSource> context) {
      FabricClientCommandSource source = (FabricClientCommandSource)context.getSource();
      List<LobbyScanCache.Lobby> lobbies = LobbyScanCache.snapshot();
      if (!lobbies.isEmpty()) {
         source.sendFeedback(Component.literal("§6§l── Lobbies ──"));
         int shown = Math.min(12, lobbies.size());
         for (int i = 0; i < shown; i++) {
            LobbyScanCache.Lobby lobby = lobbies.get(i);
            source.sendFeedback(Component.literal(formatLobbyLine(i + 1, lobby)));
         }

         source.sendFeedback(Component.literal(""));
      }

      List<ScannedPlayer> with = PotatoToolMod.getInstance().getDataStorage().getPlayersWithDisplayableItems();
      source.sendFeedback(Component.literal("§6§l── Hits (cached) ──"));
      if (with.isEmpty()) {
         source.sendFeedback(Component.literal("§7No cached players with special items. Run §e/scan lobby §7first."));
         return 1;
      }

      source.sendFeedback(Component.literal("§a" + with.size() + " §7player(s) with items:"));

      for (ScannedPlayer p : with) {
         MutableComponent line = Component.literal("§8[§e" + (int)p.getSkyblockLevel() + "§8] ");
         line.append(ChatMessageFormatter.clickablePartyUsername(p.getUsername()));
         line.append(Component.literal(" §8· §f" + p.getDisplayableItemCount()));
         source.sendFeedback(line);
      }

      source.sendFeedback(Component.literal("§8────────"));
      return 1;
   }

   private static String formatLobbyLine(int index, LobbyScanCache.Lobby lobby) {
      String time = formatLobbyTime(lobby.at);
      String hits = lobby.hits > 0 ? " §8· §a" + lobby.hits + " hit" + (lobby.hits == 1 ? "" : "s") : "";
      return "§8"
         + index
         + ". §e"
         + lobby.scanned
         + " §6new §8· §7"
         + lobby.skipped
         + " in cache §8· §c"
         + lobby.noData
         + " no api"
         + hits
         + " §8"
         + time;
   }

   private static String formatLobbyTime(long at) {
      if (at <= 0L) {
         return "";
      }

      long diff = System.currentTimeMillis() - at;
      long min = diff / 60000L;
      if (min < 1L) {
         return "now";
      }
      if (min < 60L) {
         return min + "m ago";
      }

      long hr = min / 60L;
      if (hr < 24L) {
         return hr + "h ago";
      }

      return hr / 24L + "d ago";
   }

   private static int scanSearch(CommandContext<FabricClientCommandSource> context) {
      String keyword = StringArgumentType.getString(context, "keyword");
      List<ScannedPlayer> with = PotatoToolMod.getInstance().getDataStorage().getPlayersWithItemContaining(keyword);
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§6§l── Who has \"" + keyword + "\"? ──"));
      if (with.isEmpty()) {
         ((FabricClientCommandSource)context.getSource())
            .sendFeedback(Component.literal("§6No cached player has that item. Run §e/scan lobby §6or §e/scanplayer <name>§6."));
         return 1;
      }

      for (ScannedPlayer p : with) {
         for (ScannedItem item : p.getItems()) {
            if (item != null && item.isMentionable()) {
               String name = item.getResolvedDisplayName();
               if (name != null && name.toLowerCase().contains(keyword.toLowerCase())) {
                  MutableComponent line = Component.literal("§c[§6 " + (int)p.getSkyblockLevel() + " §e] ");
                  line.append(ChatMessageFormatter.clickablePartyUsername(p.getUsername()));
                  line.append(Component.literal(" §7: "));
                  String hex = item.getHexColor();
                  if (hex != null && !hex.isEmpty()) {
                     int rgb = ColorAnalyzer.hexToInt(hex);
                     TextColor tc = TextColor.fromRgb(rgb);
                     String hexDisplay = hex.startsWith("#") ? hex : "#" + hex;
                     line.append(Component.literal(name).withStyle(s -> s.withColor(tc)));
                     line.append(Component.literal(" ").append(Component.literal(hexDisplay).withStyle(s -> s.withColor(tc))));
                     line.append(Component.literal(" §7(" + item.getCategoryTag() + ") §8[" + item.getLocationWithProfile() + "]"));
                  } else {
                     line.append(Component.literal("§f" + name + " §7(" + item.getCategoryTag() + ") §8[" + item.getLocationWithProfile() + "]"));
                  }

                  ((FabricClientCommandSource)context.getSource()).sendFeedback(line);
               }
            }
         }
      }

      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§6§l────────────────────────"));
      return 1;
   }

   private static int scanLobby(CommandContext<FabricClientCommandSource> context, PlayerScannerManager scanner) {
      Minecraft client = Minecraft.getInstance();
      if (client.level == null) {
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§cYou must be in a world to scan!"));
         return 0;
      } else if (client.getConnection() == null) {
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§cNot connected to a server!"));
         return 0;
      } else if (PotatoToolMod.getInstance().getConfig().getRandomApiKey() == null) {
         ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§c§l✘ NO API KEY CONFIGURED!"));
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§eYou need to add a Hypixel API key first:"));
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§71. Get a key: §bhttps://developer.hypixel.net/"));
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§72. Add it: §e/scannerkey add <your-key>"));
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§73. Or use GUI: §e/potatotool"));
         return 0;
      } else {
         CompletableFuture<Void> scanFuture = scanner.scanLobbyAsync(client);
         AtomicInteger lastCount = new AtomicInteger(0);
         Thread updateThread = new Thread(() -> {
            try {
               while (!scanFuture.isDone()) {
                  Thread.sleep(500L);
                  int currentCount = PotatoToolMod.getInstance().getDataStorage().getAllScannedPlayers().size();
                  if (currentCount > lastCount.get()) {
                     lastCount.set(currentCount);
                  }
               }
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
         });
         updateThread.start();
         scanFuture.thenRun(() -> client.execute(() -> {
               Collection<ScannedPlayer> scanned = scanner.getScannedPlayersInCurrentLobby(client);
               int attempted = scanner.getPlayersAttemptedLastRun();
               int newScanned = scanner.getNewPlayersScannedLastRun();
               int noDataCount = Math.max(0, attempted - newScanned);
               int alreadySeen = scanner.getAlreadySeenLastRun();
               int hits = scanner.getHitsLastRun();
               ChatMessageFormatter.sendLobbyScanSummaryOnly(scanned, attempted, noDataCount, newScanned, alreadySeen, hits);
            }))
            .exceptionally(
               throwable -> {
                  PotatoToolMod.LOGGER.error("Error during scan", throwable);
                  client.execute(
                     () -> ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cScan failed: " + throwable.getMessage()))
                  );
                  return null;
               }
            );
         return 1;
      }
   }
}
