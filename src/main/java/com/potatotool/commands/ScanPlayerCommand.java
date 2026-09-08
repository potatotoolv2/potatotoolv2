package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.potatotool.PotatoToolMod;
import com.potatotool.gui.LookupProfileScreen;
import com.potatotool.manager.PlayerScannerManager;
import com.potatotool.util.ChatMessageFormatter;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class ScanPlayerCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PlayerScannerManager playerScanner) {
      dispatcher.register(
         (LiteralArgumentBuilder)ClientCommands.literal("scanplayer")
            .then(ClientCommands.argument("username", StringArgumentType.string()).executes(context -> execute(context, playerScanner)))
      );
      dispatcher.register(
         (LiteralArgumentBuilder)ClientCommands.literal("lookup")
            .then(ClientCommands.argument("username", StringArgumentType.string()).executes(context -> execute(context, playerScanner)))
      );
   }

   private static int execute(CommandContext<FabricClientCommandSource> context, PlayerScannerManager playerScanner) {
      try {
         String username = StringArgumentType.getString(context, "username");
         Minecraft client = Minecraft.getInstance();
         if (client == null) {
            ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cClient is null"));
            return 0;
         } else {
            ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§7Looking up §e" + username + "§7 (fresh API scan)..."));
            playerScanner.lookupPlayerFresh(username)
               .thenAccept(
                  player -> {
                     Minecraft c = Minecraft.getInstance();
                     if (c != null) {
                        c.execute(
                           () -> {
                              if (player == null) {
                                 ((FabricClientCommandSource)context.getSource())
                                    .sendError(Component.literal("§cLookup failed for §e" + username + "§c. Check name/API key."));
                              } else {
                                 if (player.isOnlineAtScanTime()) {
                                    ((FabricClientCommandSource)context.getSource())
                                       .sendFeedback(Component.literal("§a§lONLINE ALERT §8» §e" + player.getUsername() + " §ais online now."));
                                 } else {
                                    ((FabricClientCommandSource)context.getSource())
                                       .sendFeedback(
                                          Component.literal(
                                             "§7Status §8» §e" + player.getUsername() + " §7last online: §f" + player.getLastLoginDisplayString()
                                          )
                                       );
                                 }

                                 if (player.hasMentionableItems()) {
                                    ChatMessageFormatter.sendPlayerScanResults(player, 0, 0, 0);
                                 }

                                 c.setScreen(new LookupProfileScreen(c.screen, player));
                              }
                           }
                        );
                     }
                  }
               )
               .exceptionally(ex -> {
                  Minecraft c = Minecraft.getInstance();
                  if (c != null) {
                     c.execute(() -> ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cLookup error: " + ex.getMessage())));
                  }

                  return null;
               });
            return 1;
         }
      } catch (Exception e) {
         ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cCommand error: " + e.getMessage()));
         PotatoToolMod.LOGGER.error("Error in /scanplayer command", e);
         return 0;
      }
   }
}
