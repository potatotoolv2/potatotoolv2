package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class CakeCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal(
                                 "ptcake"
                              )
                              .then(
                                 ClientCommands.literal("add")
                                    .then(
                                       ClientCommands.argument("year", IntegerArgumentType.integer(1, 500)).executes(ctx -> {
                                          return add(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "year"));
                                       })
                                    )
                              ))
                           .then(
                              ClientCommands.literal("remove")
                                 .then(
                                    ClientCommands.argument("year", IntegerArgumentType.integer(1, 500)).executes(ctx -> {
                                       return remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "year"));
                                    })
                                 )
                           ))
                        .then(ClientCommands.literal("defaults").executes(ctx -> defaults(ctx.getSource()))))
                     .then(ClientCommands.literal("clear").executes(ctx -> clear(ctx.getSource()))))
                  .then(ClientCommands.literal("list").executes(ctx -> list(ctx.getSource())))
      );
   }

   private static int add(FabricClientCommandSource source, int year) {
      ScannerConfig config = config();
      if (config == null) {
         source.sendError(Component.literal("§cConfig not loaded"));
         return 0;
      }

      if (!config.addCakeYear(year)) {
         source.sendFeedback(Component.literal("§eYear §6" + year + " §eis already on the cake list"));
         return 1;
      }

      config.save();
      source.sendFeedback(Component.literal("§aAdded cake year §e" + year));
      return 1;
   }

   private static int remove(FabricClientCommandSource source, int year) {
      ScannerConfig config = config();
      if (config == null) {
         source.sendError(Component.literal("§cConfig not loaded"));
         return 0;
      }

      if (!config.removeCakeYear(year)) {
         source.sendFeedback(Component.literal("§eYear §6" + year + " §ewas not on the cake list"));
         return 1;
      }

      config.save();
      source.sendFeedback(Component.literal("§aRemoved cake year §e" + year));
      return 1;
   }

   private static int defaults(FabricClientCommandSource source) {
      ScannerConfig config = config();
      if (config == null) {
         source.sendError(Component.literal("§cConfig not loaded"));
         return 0;
      }

      config.resetCakeYearsToDefaults();
      config.save();
      source.sendFeedback(Component.literal("§aReset cake years to defaults (1–25, 67, 69, 100, 200, 300, 400, 500)"));
      return 1;
   }

   private static int clear(FabricClientCommandSource source) {
      ScannerConfig config = config();
      if (config == null) {
         source.sendError(Component.literal("§cConfig not loaded"));
         return 0;
      }

      config.newYearCakeSpecificYears.clear();
      config.cakeYearsListMode = true;
      config.save();
      source.sendFeedback(Component.literal("§aCleared cake year list"));
      return 1;
   }

   private static int list(FabricClientCommandSource source) {
      ScannerConfig config = config();
      if (config == null) {
         source.sendError(Component.literal("§cConfig not loaded"));
         return 0;
      }

      String years = config.newYearCakeSpecificYears == null || config.newYearCakeSpecificYears.isEmpty()
         ? "§7none"
         : "§e" + config.newYearCakeSpecificYears.stream().map(String::valueOf).collect(Collectors.joining("§7, §e"));
      source.sendFeedback(Component.literal("§6Cake years: " + years));
      return 1;
   }

   private static ScannerConfig config() {
      return PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
   }
}
