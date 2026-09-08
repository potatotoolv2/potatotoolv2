package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.potatotool.PotatoToolMod;
import com.potatotool.config.ConfigShareCodec;
import com.potatotool.config.ScannerConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.ClickEvent.CopyToClipboard;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.client.Minecraft;

public class ShareCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("ptshare")
                  .then(ClientCommands.literal("export").executes(ctx -> exportCode(ctx.getSource()))))
               .then(
                  ClientCommands.literal("import")
                     .then(ClientCommands.argument("code", StringArgumentType.greedyString()).executes(ctx -> {
                        return importCode(ctx.getSource(), StringArgumentType.getString(ctx, "code"));
                     }))
               ))
            .then(ClientCommands.literal("importclip").executes(ctx -> importClipboard(ctx.getSource())))
      );
   }

   private static int exportCode(FabricClientCommandSource source) {
      ScannerConfig config = config();
      if (config == null) {
         source.sendError(Component.literal("§cConfig not loaded"));
         return 0;
      }

      String code = ConfigShareCodec.exportCode(config);
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.keyboardHandler != null) {
         client.keyboardHandler.setClipboard(code);
      }

      source.sendFeedback(Component.literal("§aShare code copied. API keys and webhook are not included."));
      source.sendFeedback(
         Component.literal("§e§n" + (code.length() > 48 ? code.substring(0, 48) + "…" : code))
            .withStyle(s -> s.withClickEvent(new CopyToClipboard(code)).withHoverEvent(new ShowText(Component.literal("Click to copy full code"))))
      );
      return 1;
   }

   private static int importClipboard(FabricClientCommandSource source) {
      Minecraft client = Minecraft.getInstance();
      if (client == null || client.keyboardHandler == null) {
         source.sendError(Component.literal("§cClipboard not available"));
         return 0;
      }

      return importCode(source, client.keyboardHandler.getClipboard());
   }

   private static int importCode(FabricClientCommandSource source, String raw) {
      ScannerConfig config = config();
      if (config == null) {
         source.sendError(Component.literal("§cConfig not loaded"));
         return 0;
      }

      String error = ConfigShareCodec.importCode(config, raw);
      if (error != null) {
         source.sendError(Component.literal("§c" + error));
         return 0;
      }

      source.sendFeedback(Component.literal("§aImported share code. API keys and webhook were left alone."));
      return 1;
   }

   private static ScannerConfig config() {
      return PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
   }
}
