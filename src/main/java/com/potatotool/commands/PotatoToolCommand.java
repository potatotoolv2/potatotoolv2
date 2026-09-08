package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.potatotool.PotatoToolMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class PotatoToolCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal("potatotoolv2").executes(context -> openSettings(context)));
      dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal("potatotool").executes(context -> openSettings(context)));
   }

   private static int openSettings(CommandContext<FabricClientCommandSource> context) {
      Minecraft client = Minecraft.getInstance();
      if (client == null) {
         ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cClient not available."));
         return 0;
      } else if (PotatoToolMod.getInstance() != null && PotatoToolMod.getInstance().getConfig() != null) {
         PotatoToolMod.openSettingsScreen(client);
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§aOpening scanner settings... §7(PotatoToolV2)"));
         return 1;
      } else {
         ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cMod or config not loaded."));
         return 0;
      }
   }
}
