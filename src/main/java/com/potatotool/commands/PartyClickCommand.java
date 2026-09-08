package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PartyClickCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)ClientCommands.literal("ptparty")
            .then(ClientCommands.argument("username", StringArgumentType.word()).executes(context -> {
               String username = StringArgumentType.getString(context, "username");
               if (username == null || !username.matches("[A-Za-z0-9_]{1,16}")) {
                  ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cInvalid username"));
                  return 0;
               }

               Minecraft client = Minecraft.getInstance();
               if (client.player == null || client.player.connection == null) {
                  return 0;
               }

               client.player.connection.sendCommand("p " + username);
               return 1;
            }))
      );
   }
}
