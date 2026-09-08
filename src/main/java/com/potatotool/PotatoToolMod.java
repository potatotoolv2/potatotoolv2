package com.potatotool;

import com.potatotool.api.HypixelApiClient;
import com.potatotool.commands.FilterCommand;
import com.potatotool.commands.GuiCommand;
import com.potatotool.commands.HelpCommand;
import com.potatotool.commands.CakeCommand;
import com.potatotool.commands.PartyClickCommand;
import com.potatotool.commands.PotatoToolCommand;
import com.potatotool.commands.ShareCommand;
import com.potatotool.commands.ScanLobbyCommand;
import com.potatotool.commands.ScanPlayerCommand;
import com.potatotool.commands.ScannerKeyCommand;
import com.potatotool.config.ModMenuIntegration;
import com.potatotool.config.ModernSettingsScreen;
import com.potatotool.config.ScannerConfig;
import com.potatotool.manager.ChatMacroRunner;
import com.potatotool.manager.OnlineNotifier;
import com.potatotool.manager.DataStorageManager;
import com.potatotool.manager.PlayerScannerManager;
import com.potatotool.manager.ScannedUuidCache;
import com.potatotool.renderer.HUDOverlay;
import com.potatotool.renderer.ScanResultsOverlay;
import com.potatotool.renderer.ScannerRenderer;
import com.potatotool.util.DefaultArmorColorsLoader;
import com.potatotool.util.TooltipAugmenter;
import com.potatotool.util.ValuableItemsLoader;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.KeyMapping.Category;
import com.mojang.blaze3d.platform.InputConstants.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PotatoToolMod implements ClientModInitializer {
   public static final String MOD_ID = "potato-tool-v2";
   public static final Logger LOGGER = LoggerFactory.getLogger("potato-tool-v2");
   private static PotatoToolMod instance;
   private static KeyMapping partyUnderCursorKey;
   private ScannerConfig config;
   private HypixelApiClient apiClient;
   private PlayerScannerManager playerScanner;
   private DataStorageManager dataStorage;
   private ScannerRenderer renderer;
   private static volatile String partyUnderCursorNextTick = null;
   private static final int LOBBY_STABLE_TICKS_REQUIRED = 6;
   private static final int LOBBY_STABLE_TICKS_AUTOMATIC = 3;
   private static final int WARP_SCAN_COOLDOWN_TICKS = 4;
   private static final int MIN_TAB_PLAYERS_FOR_AUTO_SCAN = 1;
   private static int lobbyStableTicks = 0;
   private static int warpScanCooldownTicks = 0;
   private static Set<String> lastLobbyTabForStable = new HashSet<>();
   private static boolean autoScanPending = false;
   private static boolean forceScanAfterWarp = false;
   private static Set<String> lastAutoScannedLobbyTab = new HashSet<>();
   private static String lastIslandLine = "";
   private static String lastScannedIsland = "";
   private static String pendingIslandLine = "";
   private static int islandChangeStableTicks = 0;
   private static final int ISLAND_CHANGE_STABLE_TICKS = 10;

   public void onInitializeClient() {
      instance = this;
      LOGGER.info("Initializing PotatoToolV2 for Minecraft 26.1.2");

      try {
         this.config = ScannerConfig.load();
         LOGGER.info("Configuration loaded successfully");
         DefaultArmorColorsLoader.load();
         ValuableItemsLoader.load();
         ScannedUuidCache.load();
         TooltipAugmenter.init();
         this.apiClient = new HypixelApiClient(this.config);
         this.dataStorage = new DataStorageManager(this.config);
         this.playerScanner = new PlayerScannerManager(this.apiClient, this.config, this.dataStorage);
         this.renderer = new ScannerRenderer(this.config, this.dataStorage);
         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> ModernSettingsScreen.tickOpen(client));
         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> ModMenuIntegration.tickOpenConfig(client));

         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> tickLobbyChangeDetection(client));
         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> tickHudNameClick(client));
         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> ChatMacroRunner.tick(client));
         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> OnlineNotifier.tick(client));
         partyUnderCursorKey = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.potato_tool.party_under_cursor", Type.KEYSYM, 80, Category.MISC)
         );
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> tickPartyUnderCursorKey(client));
         ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> {
            HelpCommand.register(dispatcher);
            GuiCommand.register(dispatcher);
            ScanPlayerCommand.register(dispatcher, this.playerScanner);
            ScannerKeyCommand.register(dispatcher, this.config);
            ScanLobbyCommand.register(dispatcher, this.playerScanner);
            PotatoToolCommand.register(dispatcher);
            FilterCommand.register(dispatcher, this.config);
            PartyClickCommand.register(dispatcher);
            ShareCommand.register(dispatcher);
            CakeCommand.register(dispatcher);
         });
         ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
            ScanResultsOverlay.resetOverlay();
            if (this.playerScanner != null) {
               this.playerScanner.clearScannedPlayers();
            }

            lastAutoScannedLobbyTab.clear();
            lastIslandLine = "";
            lastScannedIsland = "";
            pendingIslandLine = "";
            islandChangeStableTicks = 0;
            forceScanAfterWarp = false;
            warpScanCooldownTicks = 0;
            if (this.config.getRandomApiKey() == null) {
               LOGGER.warn("§c§lNO API KEY! Add key with: /scannerkey add <key>");
            } else {
               setAutoScanPending(true);
            }
         });
         HUDOverlay hudOverlay = new HUDOverlay();
         HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("potato-tool-v2", "overlay"), (graphics, deltaTracker) -> {
            try {
               hudOverlay.render(graphics, 0.0F);
            } catch (Throwable var4) {
            }
         });
         LOGGER.info("PotatoToolV2 initialized successfully for 26.1.2!");
      } catch (Exception e) {
         LOGGER.error("Failed to initialize PotatoToolV2", e);
      }
   }

   public static void setAutoScanPending(boolean pending) {
      autoScanPending = pending;
      if (pending) {
         lobbyStableTicks = 0;
      }
   }

   public static void notifyWarpSent() {
      autoScanPending = true;
      forceScanAfterWarp = true;
      lobbyStableTicks = 0;
      warpScanCooldownTicks = WARP_SCAN_COOLDOWN_TICKS;
      lastLobbyTabForStable.clear();
      lastAutoScannedLobbyTab.clear();
      lastIslandLine = "";
      lastScannedIsland = "";
      pendingIslandLine = "";
      islandChangeStableTicks = 0;
      if (instance != null) {
         if (instance.dataStorage != null) {
            instance.dataStorage.clearLastScannedLobbyTabNames();
         }
      }

      LOGGER.info("Auto-scan armed for next lobby after warp");
   }

   private static boolean isOnHypixel(Minecraft client) {
      if (client == null) {
         return false;
      }

      ServerData entry = client.getCurrentServer();
      if (entry != null) {
         String address = entry.ip;
         if (address != null && address.toLowerCase().contains("hypixel")) {
            return true;
         }
      }

      return instance != null && instance.playerScanner != null && instance.playerScanner.isOnHypixelSkyblock(client);
   }

   private static double tabJaccard(Set<String> a, Set<String> b) {
      if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
         return 0.0;
      }

      int intersection = 0;
      Set<String> smaller = a.size() <= b.size() ? a : b;
      Set<String> larger = smaller == a ? b : a;
      for (String n : smaller) {
         if (larger.contains(n)) {
            intersection++;
         }
      }

      int union = a.size() + b.size() - intersection;
      return union == 0 ? 0.0 : (double) intersection / (double) union;
   }

   private static void tickPartyUnderCursorKey(Minecraft client) {
      if (client != null && client.player != null) {
         if (partyUnderCursorNextTick != null) {
            String username = partyUnderCursorNextTick;
            partyUnderCursorNextTick = null;
            if (client.screen == null) {
               client.setScreen(new ChatScreen("/party " + username, false));
            }
         } else if (client.screen == null) {
            if (partyUnderCursorKey != null && partyUnderCursorKey.consumeClick()) {
               try {
                  int mx = ScanResultsOverlay.getScaledMouseXForClick(client);
                  int my = ScanResultsOverlay.getScaledMouseYForClick(client);

                  for (ScanResultsOverlay.ClickableRegion r : ScanResultsOverlay.getClickableRegionsCopy()) {
                     if (r.contains(mx, my) && r.username != null && !r.username.isEmpty()) {
                        partyUnderCursorNextTick = r.username;
                        return;
                     }
                  }
               } catch (Throwable var5) {
               }
            }
         }
      }
   }

   private static void tickHudNameClick(Minecraft client) {
      if (client != null && client.player != null) {
         if (client.screen == null) {
            try {
               if (!client.mouseHandler.isLeftPressed()) {
                  return;
               }

               int mx = ScanResultsOverlay.getScaledMouseXForClick(client);
               int my = ScanResultsOverlay.getScaledMouseYForClick(client);

               for (ScanResultsOverlay.ClickableRegion r : ScanResultsOverlay.getClickableRegionsCopy()) {
                  if (r.contains(mx, my) && r.username != null && !r.username.isEmpty()) {
                     String command = "/party " + r.username;
                     client.setScreen(new ChatScreen(command, false));
                     return;
                  }
               }
            } catch (Throwable var6) {
            }
         }
      }
   }

   private static void markNewLobby(String reason) {
      if (instance != null && instance.getDataStorage() != null) {
         instance.getDataStorage().clearLastScannedLobbyTabNames();
      }

      lastAutoScannedLobbyTab.clear();
      lastScannedIsland = "";
      ScanResultsOverlay.resetOverlay();
      autoScanPending = true;
      lobbyStableTicks = 0;
      LOGGER.info("Lobby change detected (" + reason + ") — auto-scan pending");
   }

   private static String normalizeIslandKey(String raw) {
      if (raw == null) {
         return "";
      }

      return raw.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
         .replace('\u2388', ' ')
         .toLowerCase()
         .replaceAll("[^a-z0-9 '\\-]", " ")
         .replaceAll("\\s+", " ")
         .trim();
   }

   private static void tickLobbyChangeDetection(Minecraft client) {
      if (instance == null || instance.playerScanner == null || instance.getDataStorage() == null) {
         return;
      }

      if (client.getConnection() == null || client.level == null) {
         return;
      }

      if (instance.getConfig().getRandomApiKey() == null) {
         return;
      }

      if (!isOnHypixel(client)) {
         return;
      }

      if (warpScanCooldownTicks > 0) {
         warpScanCooldownTicks--;
      }

      Set<String> current = PlayerScannerManager.getCurrentTabNamesLowercase(client);
      String island = normalizeIslandKey(PlayerScannerManager.getSkyblockIslandLine(client));
      if (!island.isEmpty()) {
         if (!lastIslandLine.isEmpty() && !island.equals(lastIslandLine)) {
            if (!island.equals(pendingIslandLine)) {
               pendingIslandLine = island;
               islandChangeStableTicks = 0;
            } else {
               islandChangeStableTicks++;
               if (islandChangeStableTicks >= ISLAND_CHANGE_STABLE_TICKS) {
                  markNewLobby("island " + lastIslandLine + " -> " + island);
                  forceScanAfterWarp = true;
                  lastIslandLine = island;
                  pendingIslandLine = "";
                  islandChangeStableTicks = 0;
               }
            }
         } else {
            lastIslandLine = island;
            pendingIslandLine = "";
            islandChangeStableTicks = 0;
         }
      }

      if (!instance.getConfig().lobbyScanningEnabled || !autoScanPending) {
         return;
      }

      if (instance.getConfig().millisUntilApiWindowOpens() > 0L) {
         return;
      }

      if (instance.playerScanner.isLobbyScanRunning()) {
         return;
      }

      if (warpScanCooldownTicks > 0) {
         return;
      }

      if (current.size() < MIN_TAB_PLAYERS_FOR_AUTO_SCAN) {
         lobbyStableTicks = 0;
         return;
      }

      if (!current.equals(lastLobbyTabForStable)) {
         lastLobbyTabForStable = new HashSet<>(current);
         lobbyStableTicks = 0;
         return;
      }

      lobbyStableTicks++;
      int needed = ChatMacroRunner.isRunning() || forceScanAfterWarp ? LOBBY_STABLE_TICKS_AUTOMATIC : LOBBY_STABLE_TICKS_REQUIRED;
      if (lobbyStableTicks < needed) {
         return;
      }

      boolean alreadyScannedThisIsland = !forceScanAfterWarp
         && !lastScannedIsland.isEmpty()
         && (island.isEmpty() || island.equals(lastScannedIsland));
      boolean alreadyScannedRoster = !forceScanAfterWarp
         && !lastAutoScannedLobbyTab.isEmpty()
         && tabJaccard(current, lastAutoScannedLobbyTab) >= 0.55;
      if (alreadyScannedThisIsland || alreadyScannedRoster) {
         autoScanPending = false;
         lobbyStableTicks = 0;
         return;
      }

      lastAutoScannedLobbyTab = new HashSet<>(current);
      if (!island.isEmpty()) {
         lastScannedIsland = island;
      }

      autoScanPending = false;
      forceScanAfterWarp = false;
      lobbyStableTicks = 0;
      LOGGER.info("Auto-scan firing (" + current.size() + " players" + (island.isEmpty() ? "" : ", " + island) + ")");
      instance.runLobbyScan(client);
   }

   public static PotatoToolMod getInstance() {
      return instance;
   }

   public ScannerConfig getConfig() {
      return this.config;
   }

   public HypixelApiClient getApiClient() {
      return this.apiClient;
   }

   public DataStorageManager getDataStorage() {
      return this.dataStorage;
   }

   public PlayerScannerManager getPlayerScanner() {
      return this.playerScanner;
   }

   public com.potatotool.manager.AuctionNotificationManager getAuctionNotifier() {
      return null;
   }

   public void saveConfig() {
      if (this.config != null) {
         this.config.save();
      }
   }

   public static void openSettingsScreen(Minecraft client) {
      if (client != null) {
         ModMenuIntegration.openConfigScreen(client);
      }
   }

   public void runLobbyScan(Minecraft client) {
      if (this.playerScanner != null && client.getConnection() != null) {
         if (this.playerScanner.isLobbyScanRunning()) {
            setAutoScanPending(true);
            return;
         }

         this.playerScanner.scanLobbyAsync(client, true);
         LOGGER.info("Lobby scan started");
      }
   }

   public void clearHudAndRescheduleLobbyScanIfOnHypixel(Minecraft client) {
      if (client != null) {
         if (this.dataStorage != null) {
            this.dataStorage.clearLastScannedLobbyTabNames();
         }

         ScanResultsOverlay.resetOverlay();
      }
   }
}
