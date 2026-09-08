package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class HelpCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("scanner").executes(context -> showHelp(context)))
            .then(ClientCommands.literal("help").executes(context -> showHelp(context)))
      );
   }

   private static int showHelp(CommandContext<FabricClientCommandSource> context) {
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("§6§m                    §r §6§lPotatoToolV2 Commands §6§m                    "));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lScanning Commands:"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/scan lobby §7- Scan all players in current lobby"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/scan list §7- Diary: list cached players with special items"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/scan search <item> §7- Diary: who has that item (e.g. lapis boots)"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/scanplayer <username> §7- Fresh API lookup + profile viewer"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/lookup <username> §7- Alias for scanplayer lookup"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lAPI Key Management:"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/scannerkey add <key> §7- Add a Hypixel API key"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/scannerkey list §7- List all configured API keys"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/scannerkey remove <index> §7- Remove API key by index"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lFilter Commands:"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/filter status §7- Show all current filter settings"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/filter reset §7- Reset all filters to defaults"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/filter level max <0-500> §7- Set max Skyblock level"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/filter level min <0-500> §7- Set min Skyblock level"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/filter toggle <type> <true/false> §7- Toggle item category"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lFilter Types:"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §7Categories: §fweapons, armor, accessories, tools, pets, consumables, cosmetics"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §7Special: §fdungeon, slayer, event, admin, valuable, legacy, ghost"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §7Armor: §fcrystal, fairy, bleached, soulbound"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lBlacklist/Whitelist:"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/filter blacklist add/remove/list/clear §7- Manage blacklist"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/filter whitelist add/remove/list/clear §7- Manage whitelist"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/filter whitelist enable <true/false> §7- Toggle whitelist mode"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lGUI:"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/potatotoolv2 §7- Open settings (items, level, skin value, automatic warps)"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/ptshare export §7- Copy a shareable config code"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/ptshare import <code> §7- Import a friend's config code"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/ptcake add/remove/list/defaults §7- Cake years"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/scannergui §7 or §6/scannersettings §7- Same settings menu"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6Mods → PotatoToolV2 → Configure §7- Mod menu config"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lExamples:"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/scan lobby §7- Scan everyone in your lobby"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §6/filter toggle weapons true §7- Enable weapon scanning"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/filter level max 150 §7- Only scan players level 150 and below"));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("  §6/filter whitelist add MIDAS_SWORD §7- Add Midas Sword to whitelist"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("§e§lQuick Links:"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §9§nGet API Key: §bhttps://developer.hypixel.net/"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal("  §9§nMore Help: §bType §6/scanner help §bor check GUI"));
      ((FabricClientCommandSource)context.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)context.getSource())
         .sendFeedback(Component.literal("§6§m                                                                          "));
      return 1;
   }
}
