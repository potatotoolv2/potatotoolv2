package com.potatotool.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.potatotool.config.ScannerConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class FilterCommand {
   public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, ScannerConfig config) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal(
                              "filter"
                           )
                           .then(
                              ((LiteralArgumentBuilder)ClientCommands.literal("level")
                                    .then(
                                       ClientCommands.literal("max")
                                          .then(
                                             ClientCommands.argument("level", DoubleArgumentType.doubleArg(0.0, 500.0))
                                                .executes(ctx -> setMaxLevel(ctx, config))
                                          )
                                    ))
                                 .then(
                                    ClientCommands.literal("min")
                                       .then(
                                          ClientCommands.argument("level", DoubleArgumentType.doubleArg(0.0, 500.0))
                                             .executes(ctx -> setMinLevel(ctx, config))
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal(
                                                                                    "toggle"
                                                                                 )
                                                                                 .then(
                                                                                    ClientCommands.literal("weapons")
                                                                                       .then(
                                                                                          ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                                             .executes(ctx -> toggleWeapons(ctx, config))
                                                                                       )
                                                                                 ))
                                                                              .then(
                                                                                 ClientCommands.literal("armor")
                                                                                    .then(
                                                                                       ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                                          .executes(ctx -> toggleArmor(ctx, config))
                                                                                    )
                                                                              ))
                                                                           .then(
                                                                              ClientCommands.literal("accessories")
                                                                                 .then(
                                                                                    ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                                       .executes(ctx -> toggleAccessories(ctx, config))
                                                                                 )
                                                                           ))
                                                                        .then(
                                                                           ClientCommands.literal("tools")
                                                                              .then(
                                                                                 ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                                    .executes(ctx -> toggleTools(ctx, config))
                                                                              )
                                                                        ))
                                                                     .then(
                                                                        ClientCommands.literal("pets")
                                                                           .then(
                                                                              ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                                 .executes(ctx -> togglePets(ctx, config))
                                                                           )
                                                                     ))
                                                                  .then(
                                                                     ClientCommands.literal("consumables")
                                                                        .then(
                                                                           ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                              .executes(ctx -> toggleConsumables(ctx, config))
                                                                        )
                                                                  ))
                                                               .then(
                                                                  ClientCommands.literal("cosmetics")
                                                                     .then(
                                                                        ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                           .executes(ctx -> toggleCosmetics(ctx, config))
                                                                     )
                                                               ))
                                                            .then(
                                                               ClientCommands.literal("dungeon")
                                                                  .then(
                                                                     ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                        .executes(ctx -> toggleDungeon(ctx, config))
                                                                  )
                                                            ))
                                                         .then(
                                                            ClientCommands.literal("slayer")
                                                               .then(
                                                                  ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                     .executes(ctx -> toggleSlayer(ctx, config))
                                                               )
                                                         ))
                                                      .then(
                                                         ClientCommands.literal("event")
                                                            .then(
                                                               ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                  .executes(ctx -> toggleEvent(ctx, config))
                                                            )
                                                      ))
                                                   .then(
                                                      ClientCommands.literal("admin")
                                                         .then(
                                                            ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                               .executes(ctx -> toggleAdmin(ctx, config))
                                                         )
                                                   ))
                                                .then(
                                                   ClientCommands.literal("valuable")
                                                      .then(
                                                         ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                            .executes(ctx -> toggleValuable(ctx, config))
                                                      )
                                                ))
                                             .then(
                                                ClientCommands.literal("legacy")
                                                   .then(
                                                      ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                         .executes(ctx -> toggleLegacy(ctx, config))
                                                   )
                                             ))
                                          .then(
                                             ClientCommands.literal("ghost")
                                                .then(
                                                   ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> toggleGhost(ctx, config))
                                                )
                                          ))
                                       .then(
                                          ClientCommands.literal("crystal")
                                             .then(
                                                ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> toggleCrystal(ctx, config))
                                             )
                                       ))
                                    .then(
                                       ClientCommands.literal("fairy")
                                          .then(ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> toggleFairy(ctx, config)))
                                    ))
                                 .then(
                                    ClientCommands.literal("bleached")
                                       .then(ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> toggleBleached(ctx, config)))
                                 ))
                              .then(
                                 ClientCommands.literal("soulbound")
                                    .then(ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> toggleSoulbound(ctx, config)))
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal("blacklist")
                                    .then(
                                       ClientCommands.literal("add")
                                          .then(ClientCommands.argument("itemId", StringArgumentType.string()).executes(ctx -> addBlacklist(ctx, config)))
                                    ))
                                 .then(
                                    ClientCommands.literal("remove")
                                       .then(ClientCommands.argument("itemId", StringArgumentType.string()).executes(ctx -> removeBlacklist(ctx, config)))
                                 ))
                              .then(ClientCommands.literal("list").executes(ctx -> listBlacklist(ctx, config))))
                           .then(ClientCommands.literal("clear").executes(ctx -> clearBlacklist(ctx, config)))
                     ))
                  .then(
                     ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal(
                                       "whitelist"
                                    )
                                    .then(
                                       ClientCommands.literal("add")
                                          .then(ClientCommands.argument("itemId", StringArgumentType.string()).executes(ctx -> addWhitelist(ctx, config)))
                                    ))
                                 .then(
                                    ClientCommands.literal("remove")
                                       .then(ClientCommands.argument("itemId", StringArgumentType.string()).executes(ctx -> removeWhitelist(ctx, config)))
                                 ))
                              .then(ClientCommands.literal("list").executes(ctx -> listWhitelist(ctx, config))))
                           .then(ClientCommands.literal("clear").executes(ctx -> clearWhitelist(ctx, config))))
                        .then(
                           ClientCommands.literal("enable")
                              .then(ClientCommands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> enableWhitelist(ctx, config)))
                        )
                  ))
               .then(ClientCommands.literal("status").executes(ctx -> showStatus(ctx, config))))
            .then(ClientCommands.literal("reset").executes(ctx -> resetFilters(ctx, config)))
      );
   }

   private static int setMaxLevel(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      double level = DoubleArgumentType.getDouble(ctx, "level");
      config.skyblockLevelCap = level;
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aSet max Skyblock level to: §e" + (int)level + (level == 0.0 ? " §7(no limit)" : "")));
      return 1;
   }

   private static int setMinLevel(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      double level = DoubleArgumentType.getDouble(ctx, "level");
      config.minSkyblockLevel = level;
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aSet min Skyblock level to: §e" + (int)level));
      return 1;
   }

   private static int toggleWeapons(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanWeapons = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aWeapon scanning: " + (config.scanWeapons ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleArmor(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanArmor = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aArmor scanning: " + (config.scanArmor ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleAccessories(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanAccessories = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aAccessory scanning: " + (config.scanAccessories ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleTools(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanTools = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aTool scanning: " + (config.scanTools ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int togglePets(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanPets = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aPet scanning: " + (config.scanPets ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleConsumables(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanConsumables = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aConsumable scanning: " + (config.scanConsumables ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleCosmetics(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanCosmetics = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aCosmetic scanning: " + (config.scanCosmetics ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleDungeon(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanDungeonItems = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aDungeon item scanning: " + (config.scanDungeonItems ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleSlayer(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanSlayerItems = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aSlayer item scanning: " + (config.scanSlayerItems ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleEvent(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanEventItems = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aEvent item scanning: " + (config.scanEventItems ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleAdmin(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanAdminItems = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aAdmin item scanning: " + (config.scanAdminItems ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleValuable(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.valuableItemsEnabled = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aValuable item scanning: " + (config.valuableItemsEnabled ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleLegacy(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.legacyReforgeEnabled = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aLegacy reforge scanning: " + (config.legacyReforgeEnabled ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleGhost(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.ghostReforgeEnabled = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aGhost reforge scanning: " + (config.ghostReforgeEnabled ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleCrystal(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.crystalScanningEnabled = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aCrystal armor scanning: " + (config.crystalScanningEnabled ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleFairy(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.fairyScanningEnabled = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aFairy armor scanning: " + (config.fairyScanningEnabled ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleBleached(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.bleachedScanningEnabled = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aBleached armor scanning: " + (config.bleachedScanningEnabled ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int toggleSoulbound(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanSoulboundItems = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aSoulbound item scanning: " + (config.scanSoulboundItems ? "§2✓ Enabled" : "§c✗ Disabled")));
      return 1;
   }

   private static int addBlacklist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      String itemId = StringArgumentType.getString(ctx, "itemId");
      config.itemBlacklist.add(itemId);
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aAdded §e" + itemId + " §ato blacklist"));
      return 1;
   }

   private static int removeBlacklist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      String itemId = StringArgumentType.getString(ctx, "itemId");
      config.itemBlacklist.remove(itemId);
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aRemoved §e" + itemId + " §afrom blacklist"));
      return 1;
   }

   private static int listBlacklist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      if (config.itemBlacklist.isEmpty()) {
         ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§7Blacklist is empty"));
      } else {
         ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§6§lBlacklisted Items:"));

         for (String item : config.itemBlacklist) {
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  §7- §e" + item));
         }
      }

      return 1;
   }

   private static int clearBlacklist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.itemBlacklist.clear();
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aCleared blacklist"));
      return 1;
   }

   private static int addWhitelist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      String itemId = StringArgumentType.getString(ctx, "itemId");
      config.itemWhitelist.add(itemId);
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aAdded §e" + itemId + " §ato whitelist"));
      return 1;
   }

   private static int removeWhitelist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      String itemId = StringArgumentType.getString(ctx, "itemId");
      config.itemWhitelist.remove(itemId);
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aRemoved §e" + itemId + " §afrom whitelist"));
      return 1;
   }

   private static int listWhitelist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      if (config.itemWhitelist.isEmpty()) {
         ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§7Whitelist is empty"));
      } else {
         ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§6§lWhitelisted Items:"));

         for (String item : config.itemWhitelist) {
            ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  §7- §e" + item));
         }
      }

      return 1;
   }

   private static int clearWhitelist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.itemWhitelist.clear();
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aCleared whitelist"));
      return 1;
   }

   private static int enableWhitelist(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.useWhitelist = BoolArgumentType.getBool(ctx, "enabled");
      config.save();
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("§aWhitelist mode: " + (config.useWhitelist ? "§2✓ Enabled §7(ONLY scan whitelisted items)" : "§c✗ Disabled")));
      return 1;
   }

   private static int showStatus(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§6§m          §r §6§lFilter Status §6§m          "));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§e§lLevel Filters:"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  §7Min Level: §e" + (int)config.minSkyblockLevel));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  §7Max Level: §e" + (config.skyblockLevelCap == 0.0 ? "No limit" : (int)config.skyblockLevelCap)));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§e§lItem Categories:"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanWeapons ? "§2✓" : "§c✗") + " §7Weapons"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanArmor ? "§2✓" : "§c✗") + " §7Armor"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanAccessories ? "§2✓" : "§c✗") + " §7Accessories"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanTools ? "§2✓" : "§c✗") + " §7Tools"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanPets ? "§2✓" : "§c✗") + " §7Pets"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanConsumables ? "§2✓" : "§c✗") + " §7Consumables"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanCosmetics ? "§2✓" : "§c✗") + " §7Cosmetics"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§e§lSpecial Items:"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.valuableItemsEnabled ? "§2✓" : "§c✗") + " §7Valuable Items"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.scanAnniversaryHats ? "§2✓" : "§c✗") + " §7Anniversary Hats"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(
            Component.literal(
               "  "
                  + (config.newYearCakeEnabled ? "§2✓" : "§c✗")
                  + " §7Cake years: §e"
                  + (config.newYearCakeSpecificYears == null || config.newYearCakeSpecificYears.isEmpty()
                     ? "none"
                     : config.newYearCakeSpecificYears.size() + " selected")
            )
         );
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.legacyReforgeEnabled ? "§2✓" : "§c✗") + " §7Legacy Reforges"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.ghostReforgeEnabled ? "§2✓" : "§c✗") + " §7Ghost Reforges"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.crystalScanningEnabled ? "§2✓" : "§c✗") + " §7Crystal Armor"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.fairyScanningEnabled ? "§2✓" : "§c✗") + " §7Fairy Armor"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.bleachedScanningEnabled ? "§2✓" : "§c✗") + " §7Bleached Armor"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanDungeonItems ? "§2✓" : "§c✗") + " §7Dungeon Items"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanSlayerItems ? "§2✓" : "§c✗") + " §7Slayer Items"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanEventItems ? "§2✓" : "§c✗") + " §7Event Items"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.scanAdminItems ? "§2✓" : "§c✗") + " §7Admin Items"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal(""));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§e§lOther:"));
      ((FabricClientCommandSource)ctx.getSource())
         .sendFeedback(Component.literal("  " + (config.scanSoulboundItems ? "§2✓" : "§c✗") + " §7Soulbound Items"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  " + (config.useWhitelist ? "§2✓" : "§c✗") + " §7Whitelist Mode"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  §7Blacklist: §e" + config.itemBlacklist.size() + " items"));
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("  §7Whitelist: §e" + config.itemWhitelist.size() + " items"));
      return 1;
   }

   private static int resetFilters(CommandContext<FabricClientCommandSource> ctx, ScannerConfig config) {
      config.scanWeapons = true;
      config.scanArmor = true;
      config.scanAccessories = true;
      config.scanTools = true;
      config.scanPets = true;
      config.scanConsumables = false;
      config.scanCosmetics = true;
      config.scanDungeonItems = true;
      config.scanSlayerItems = true;
      config.scanEventItems = true;
      config.scanAdminItems = true;
      config.valuableItemsEnabled = true;
      config.scanAnniversaryHats = false;
      config.resetCakeYearsToDefaults();
      config.legacyReforgeEnabled = true;
      config.ghostReforgeEnabled = true;
      config.crystalScanningEnabled = true;
      config.fairyScanningEnabled = true;
      config.bleachedScanningEnabled = true;
      config.scanSoulboundItems = false;
      config.skyblockLevelCap = 500.0;
      config.minSkyblockLevel = 0.0;
      config.useWhitelist = false;
      config.save();
      ((FabricClientCommandSource)ctx.getSource()).sendFeedback(Component.literal("§aReset all filters to default"));
      return 1;
   }
}
