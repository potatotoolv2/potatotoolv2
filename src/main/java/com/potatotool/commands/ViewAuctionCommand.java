package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.potatotool.manager.AuctionNotificationManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class ViewAuctionCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, AuctionNotificationManager notifier) {
      dispatcher.register(
         (LiteralArgumentBuilder)ClientCommands.literal("ptview")
            .then(ClientCommands.argument("uuid", StringArgumentType.string()).executes(ctx -> execute(ctx, notifier)))
      );
   }

   private static int execute(CommandContext<FabricClientCommandSource> context, AuctionNotificationManager notifier) {
      Minecraft client = Minecraft.getInstance();
      if (client == null || client.player == null) {
         ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cClient/player not available."));
         return 0;
      } else if (notifier == null) {
         ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cAH notifier not ready."));
         return 0;
      } else {
         String rawUuid = StringArgumentType.getString(context, "uuid");
         boolean ok = notifier.requestViewOnly(rawUuid, client);
         if (!ok) {
            ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cInvalid auction uuid."));
            return 0;
         } else {
            ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§aViewing auction §e" + rawUuid));
            return 1;
         }
      }
   }
}
