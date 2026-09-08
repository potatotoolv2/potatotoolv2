package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.potatotool.PotatoToolMod;
import com.potatotool.api.HypixelApiClient;
import com.potatotool.config.ScannerConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class ScannerKeyCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, ScannerConfig config) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("scannerkey")
                  .then(
                     ClientCommands.literal("add")
                        .then(ClientCommands.argument("key", StringArgumentType.string()).executes(context -> executeAdd(context, config)))
                  ))
               .then(ClientCommands.literal("list").executes(context -> executeList(context, config))))
            .then(
               ClientCommands.literal("remove")
                  .then(ClientCommands.argument("index", IntegerArgumentType.integer(0)).executes(context -> executeRemove(context, config)))
            )
      );
   }

   private static int executeAdd(CommandContext<FabricClientCommandSource> context, ScannerConfig config) {
      String key = StringArgumentType.getString(context, "key");
      config.addApiKey(key);
      config.save();
      HypixelApiClient apiClient = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getApiClient() : null;
      if (apiClient != null) {
         apiClient.clearRateLimitCooldown();
      }

      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("§aAdded API key. §7(Throttle cleared – try §e/scan lobby§7 again.)"));
      if (apiClient != null) {
         String trimmedKey = key != null ? key.trim().replaceAll("\\s+", "") : "";
         if (!trimmedKey.isEmpty()) {
            apiClient.validateApiKey(trimmedKey)
               .thenAccept(
                  valid -> ((FabricClientCommandSource)context.getSource())
                     .getClient()
                     .execute(
                        () -> {
                           if (valid) {
                              ((FabricClientCommandSource)context.getSource())
                                 .sendFeedback(Component.literal("§aKey validated successfully. §7You can use §e/scan lobby§7 now."));
                           } else {
                              ((FabricClientCommandSource)context.getSource())
                                 .sendFeedback(
                                    Component.literal(
                                       "§eKey added but validation failed (invalid/expired or rate limited). §7Check at §bdeveloper.hypixel.net§7 or try §e/scan lobby§7."
                                    )
                                 );
                           }
                        }
                     )
               );
         }
      }

      return 1;
   }

   private static int executeList(CommandContext<FabricClientCommandSource> context, ScannerConfig config) {
      if (config.apiKeys.isEmpty()) {
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§cNo API keys configured"));
         return 0;
      }

      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§eAPI Keys:"));

      for (int i = 0; i < config.apiKeys.size(); i++) {
         String key = config.apiKeys.get(i);
         String masked = key.length() > 8 ? key.substring(0, 8) + "..." : key;
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(String.format("§7[%d] %s", i, masked)));
      }

      return 1;
   }

   private static int executeRemove(CommandContext<FabricClientCommandSource> context, ScannerConfig config) {
      int index = IntegerArgumentType.getInteger(context, "index");
      if (index >= 0 && index < config.apiKeys.size()) {
         config.removeApiKey(index);
         config.save();
         ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§aRemoved API key"));
         return 1;
      } else {
         ((FabricClientCommandSource)context.getSource()).sendError(Component.literal("§cInvalid index"));
         return 0;
      }
   }
}
