package com.potatotool.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.potatotool.PotatoToolMod;
import com.potatotool.api.HypixelApiClient;
import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import com.potatotool.util.ChatMessageFormatter;
import com.potatotool.util.ColorAnalyzer;
import com.potatotool.util.EditionedCollectibles;
import com.potatotool.util.CosmeticSkinValuesLoader;
import com.potatotool.util.DefaultArmorColorsLoader;
import com.potatotool.util.SeymourAnalyzer;
import com.potatotool.util.ValuableItemsLoader;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.DisplaySlot;

public class PlayerScannerManager {
   private final HypixelApiClient apiClient;
   private final ScannerConfig config;
   private final DataStorageManager dataStorage;
   private final Set<String> scannedPlayers;
   private volatile Semaphore apiConcurrencySemaphore;
   private final ExecutorService scanExecutor = Executors.newFixedThreadPool(80, r -> {
      Thread t = new Thread(r);
      t.setDaemon(true);
      t.setName("PotatoToolV2-Worker");
      return t;
   });
   private static Field profileIdField;
   private static Field profileNameField;
   private static final Pattern VALID_IGN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
   private static final Pattern NPC_TEN_CHAR_IGN = Pattern.compile("^[a-z0-9]{10}$");
   private static final Set<String> SKIP_NAMES = Set.of("watchdog", "hypixel", "npc");
   private final AtomicInteger newPlayersScannedThisRun = new AtomicInteger(0);
   private final AtomicInteger playersAttemptedThisRun = new AtomicInteger(0);
   private final AtomicInteger alreadySeenThisRun = new AtomicInteger(0);
   private final AtomicInteger hitsThisRun = new AtomicInteger(0);
   private final AtomicInteger lobbyScansInFlight = new AtomicInteger(0);

   public PlayerScannerManager(HypixelApiClient apiClient, ScannerConfig config, DataStorageManager dataStorage) {
      this.apiClient = apiClient;
      this.config = config;
      this.dataStorage = dataStorage;
      this.scannedPlayers = ConcurrentHashMap.newKeySet();
   }

   public void clearScannedPlayers() {
      this.scannedPlayers.clear();
      this.dataStorage.clearScannedPlayers();
   }

   public void clearScanAttemptsOnly() {
      this.scannedPlayers.clear();
   }

   public static Set<String> getCurrentTabNamesLowercase(Minecraft client) {
      if (client == null || client.getConnection() == null) {
         return Collections.emptySet();
      }

      Set<String> out = new HashSet<>();
      for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
         if (entry.getProfile() == null) {
            continue;
         }

         String name = getProfileName(entry.getProfile());
         UUID uuid = getProfileId(entry.getProfile());
         if (isRealLobbyPlayer(name, uuid)) {
            out.add(name.toLowerCase());
         }
      }

      return out;
   }

   public List<ScannedPlayer> getScannedPlayersInCurrentLobby(Minecraft client) {
      Set<String> names = getCurrentTabNamesLowercase(client);
      return names.isEmpty() ? Collections.emptyList() : this.dataStorage.getPlayersByUsernames(names);
   }

   public void removeFromAttempted(String uuidNoDashes) {
      if (uuidNoDashes != null) {
         this.scannedPlayers.remove(uuidNoDashes);
      }
   }

   public int getNewPlayersScannedLastRun() {
      return this.newPlayersScannedThisRun.get();
   }

   public int getPlayersAttemptedLastRun() {
      return this.playersAttemptedThisRun.get();
   }

   public int getAlreadySeenLastRun() {
      return this.alreadySeenThisRun.get();
   }

   public int getHitsLastRun() {
      return this.hitsThisRun.get();
   }

   public static UUID getProfileId(GameProfile profile) {
      try {
         return (UUID)profileIdField.get(profile);
      } catch (Exception e) {
         System.err.println("Failed to get profile ID: " + e.getMessage());
         return null;
      }
   }

   public static String getProfileName(GameProfile profile) {
      try {
         return (String)profileNameField.get(profile);
      } catch (Exception e) {
         System.err.println("Failed to get profile name: " + e.getMessage());
         return null;
      }
   }

   private static String formatUuidForApi(UUID uuid) {
      return uuid.toString().replace("-", "");
   }

   public void scanLobby(Minecraft client) {
      this.scanLobbyAsync(client, false);
   }

   public CompletableFuture<Void> scanLobbyAsync(Minecraft client) {
      return this.scanLobbyAsync(client, false);
   }

   public boolean isLobbyScanRunning() {
      return this.lobbyScansInFlight.get() > 0;
   }

   public CompletableFuture<Void> scanLobbyAsync(Minecraft client, boolean sendAutoScanReportWhenDone) {
      if (client.getConnection() == null) {
         PotatoToolMod.LOGGER.error("No network handler!");
         return CompletableFuture.completedFuture(null);
      }

      if (client.level == null) {
         PotatoToolMod.LOGGER.error("World is null!");
         return CompletableFuture.completedFuture(null);
      }

      if (this.apiClient.isRateLimited()) {
         int remaining = this.apiClient.getRemainingCooldown();
         int min = remaining / 60;
         int sec = remaining % 60;
         String timeStr = min > 0 ? min + "m " + sec + "s" : sec + "s";
         client.execute(
            () -> {
               if (client.player != null) {
                  client.player
                     .sendSystemMessage(
                        Component.literal(
                           "§c§l⚠ API THROTTLED §r§7 | §eWait §c" + timeStr + " §ebefore scanning. §7Add more keys: §6/scannerkey add <key>"
                        ));
               }
            }
         );
         return CompletableFuture.completedFuture(null);
      }

      long startTime = System.currentTimeMillis();
      this.newPlayersScannedThisRun.set(0);
      this.playersAttemptedThisRun.set(0);
      this.alreadySeenThisRun.set(0);
      this.hitsThisRun.set(0);
      int keys = Math.max(1, this.config.getApiKeyCount());
      int permits = Math.min(12, Math.max(3, keys * 4));
      this.apiConcurrencySemaphore = new Semaphore(permits);
      Collection<PlayerInfo> tabList = client.getConnection().getOnlinePlayers();
      List<Entry<String, String>> toScan = this.collectLobbyScanTargets(client);
      PotatoToolMod.LOGGER.info(
         "Lobby scan: " + toScan.size() + " new (tab had " + tabList.size() + ", already seen " + this.alreadySeenThisRun.get() + ", keys: " + keys + ")"
      );

      int maxToScan = sendAutoScanReportWhenDone ? 0 : this.config.maxPlayersToScan;
      if (maxToScan > 0 && toScan.size() > maxToScan) {
         toScan = new ArrayList<>(toScan.subList(0, maxToScan));
      }

      Set<String> lobbyTabNames = new HashSet<>();

      for (Entry<String, String> e : toScan) {
         lobbyTabNames.add(e.getKey().toLowerCase());
      }

      this.dataStorage.setLastScannedLobbyTabNames(lobbyTabNames);
      List<CompletableFuture<Void>> futures = new ArrayList<>();

      for (Entry<String, String> e : toScan) {
         String name = e.getKey();
         String uuidNoDashes = e.getValue();
         CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
               this.scanPlayerSync(name, uuidNoDashes, sendAutoScanReportWhenDone && ChatMacroRunner.isRunning(), true);
            } catch (Exception ex) {
               PotatoToolMod.LOGGER.error("Error scanning " + name + ": " + ex.getMessage());
            }
         }, this.scanExecutor);
         futures.add(future);
      }

      int attempted = toScan.size();
      this.playersAttemptedThisRun.set(attempted);
      int skipped = this.alreadySeenThisRun.get();
      if (futures.isEmpty()) {
         PotatoToolMod.LOGGER.warn("No valid players found to scan!");
         LobbyScanCache.record(0, 0, skipped, 0, 0);
         if (sendAutoScanReportWhenDone) {
            client.execute(() -> ChatMessageFormatter.sendLobbySummary(0, 0, 0, skipped, 0));
         }
         return CompletableFuture.completedFuture(null);
      }

      this.lobbyScansInFlight.incrementAndGet();
      int attemptedFinal = attempted;
      int skippedFinal = skipped;
      return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((ok, err) -> {
         this.lobbyScansInFlight.decrementAndGet();
         this.apiConcurrencySemaphore = null;
      }).thenRun(() -> {
         int newScanned = this.getNewPlayersScannedLastRun();
         int noData = Math.max(0, attemptedFinal - newScanned);
         int hits = this.getHitsLastRun();
         LobbyScanCache.record(attemptedFinal, newScanned, skippedFinal, noData, hits);
         PotatoToolMod.LOGGER.info(
            "Lobby done: " + newScanned + " new, " + skippedFinal + " seen, " + hits + " hits, " + noData + " no data"
         );
         if (sendAutoScanReportWhenDone) {
            client.execute(() -> ChatMessageFormatter.sendLobbySummary(attemptedFinal, newScanned, noData, skippedFinal, hits));
         }
      });
   }

   private void scanPlayerSync(String username, String uuid) {
      this.scanPlayerSync(username, uuid, false);
   }

   private void scanPlayerSync(String username, String uuid, boolean automaticDiscord) {
      this.scanPlayerSync(username, uuid, automaticDiscord, false);
   }

   private void scanPlayerSync(String username, String uuid, boolean automaticDiscord, boolean skipPlayerInfo) {
      if (this.config.getRandomApiKey() != null) {
         Semaphore sem = this.apiConcurrencySemaphore;
         if (sem != null) {
            try {
               sem.acquire();
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return;
            }
         }

         try {
            JsonObject response = this.apiClient.getSkyblockProfilesBlocking(uuid);
            if (response == null) {
               return;
            }

            this.scannedPlayers.add(uuid);
            ScannedUuidCache.record(uuid, username);

            if (!response.has("success") || !response.get("success").getAsBoolean()) {
               return;
            }

            if (!response.has("profiles") || response.get("profiles").isJsonNull()) {
               return;
            }

            JsonArray profiles = response.getAsJsonArray("profiles");
            if (profiles.size() != 0) {
               ScannedPlayer player = new ScannedPlayer(username, uuid);
               this.processAllProfiles(player, profiles);
               if (!player.getProfiles().isEmpty()) {
                  this.dataStorage.addScannedPlayer(player);
                  this.newPlayersScannedThisRun.incrementAndGet();
                  if (!skipPlayerInfo) {
                     this.apiClient.getPlayerInfo(uuid).thenAccept(info -> {
                        if (info != null) {
                           if (info.rank != null) {
                              player.setRank(info.rank);
                           }

                           if (info.lastLogin != null) {
                              player.setLastLoginTimestamp(info.lastLogin);
                           }

                           if (info.lastLogout != null) {
                              player.setLastLogoutTimestamp(info.lastLogout);
                           }
                        }
                     });
                  }
                  if (player.getSpecialItemCount() > 0) {
                     PotatoToolMod.LOGGER.info("✓ " + username + " - Lv" + (int)player.getSkyblockLevel() + " - " + player.getSpecialItemCount() + " items");
                  }

                  ScanAlertService.onPlayerHit(player, automaticDiscord);

                  if (player.hasDisplayableItems()) {
                     this.hitsThisRun.incrementAndGet();
                     Minecraft c = Minecraft.getInstance();
                     if (c != null) {
                        c.execute(() -> ChatMessageFormatter.sendPlayerBlock(player));
                     }

                     return;
                  }
               }

               return;
            }
         } catch (Exception e) {
            PotatoToolMod.LOGGER.error("Error scanning " + username + ": " + e.getMessage());
            return;
         } finally {
            if (sem != null) {
               sem.release();
            }
         }
      }
   }

   public void scanUsernamesAndAddToCache(List<String> usernames, Runnable onComplete) {
      if (this.config.getRandomApiKey() == null) {
         PotatoToolMod.LOGGER.warn("No API key - add one in API Keys tab");
         if (onComplete != null && Minecraft.getInstance() != null) {
            Minecraft.getInstance().execute(() -> {
               if (Minecraft.getInstance().player != null) {
                  Minecraft.getInstance().player.sendSystemMessage(Component.literal("§cAdd an API key in the API Keys tab first."));
               }

               if (onComplete != null) {
                  onComplete.run();
               }
            });
         }
      } else {
         List<String> list = new ArrayList<>();

         for (String s : usernames) {
            String t = s.trim();
            if (t.length() >= 2 && t.length() <= 16) {
               list.add(t);
            }
         }

         if (list.isEmpty()) {
            if (onComplete != null && Minecraft.getInstance() != null) {
               Minecraft.getInstance().execute(onComplete);
            }
         } else {
            Semaphore prev = this.apiConcurrencySemaphore;
            this.apiConcurrencySemaphore = new Semaphore(Math.min(10, Math.max(3, list.size() / 2)));
            this.scanExecutor
               .execute(
                  () -> {
                     try {
                        int added = 0;

                        for (int i = 0; i < list.size(); i++) {
                           String name = list.get(i);
                           String uuid = this.apiClient.resolveUsernameToUuid(name);
                           if (uuid != null) {
                              this.scannedPlayers.add(uuid);
                              this.scanPlayerSync(name, uuid);
                              added++;
                           }

                           if (i < list.size() - 1) {
                              Thread.sleep(450L);
                           }
                        }

                        int addedFinal = added;
                        Minecraft c = Minecraft.getInstance();
                        if (c != null) {
                           c.execute(
                              () -> {
                                 if (c.player != null) {
                                    c.player
                                       .sendSystemMessage(
                                          Component.literal("§aLookup: added §f" + addedFinal + "§a players to cache. Use Search above (hex + item)."));
                                 }

                                 if (onComplete != null) {
                                    onComplete.run();
                                 }
                              }
                           );
                        } else if (onComplete != null) {
                           onComplete.run();
                        }
                     } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                     } finally {
                        this.apiConcurrencySemaphore = prev;
                     }
                  }
               );
         }
      }
   }

   public CompletableFuture<ScannedPlayer> lookupPlayerFresh(String username) {
      return CompletableFuture.supplyAsync(() -> {
         if (username == null) {
            return null;
         }

         String clean = username.trim();
         if (clean.length() < 2 || clean.length() > 16) {
            return null;
         }

         if (this.config.getRandomApiKey() == null) {
            return null;
         }

         String uuid = this.apiClient.resolveUsernameToUuid(clean);
         if (uuid != null && !uuid.isEmpty()) {
            this.removeFromAttempted(uuid);
            this.scanPlayerSync(clean, uuid);
            ScannedPlayer player = this.dataStorage.getPlayerByUuid(uuid);
            if (player == null) {
               player = this.dataStorage.getPlayerByUsername(clean);
            }

            if (player == null) {
               return null;
            }

            try {
               HypixelApiClient.PlayerInfo info = this.apiClient.getPlayerInfo(uuid).get(3L, TimeUnit.SECONDS);
               if (info != null) {
                  if (info.rank != null) {
                     player.setRank(info.rank);
                  }

                  if (info.lastLogin != null) {
                     player.setLastLoginTimestamp(info.lastLogin);
                  }

                  if (info.lastLogout != null) {
                     player.setLastLogoutTimestamp(info.lastLogout);
                  }
               }
            } catch (Exception var6) {
            }

            return player;
         } else {
            return null;
         }
      }, this.scanExecutor);
   }

   private CompletableFuture<Void> scanPlayerAsync(String username, String uuid) {
      String apiKey = this.config.getRandomApiKey();
      if (apiKey == null) {
         PotatoToolMod.LOGGER.error("No API key configured! Add one with /scannerkey add <key>");
         return CompletableFuture.completedFuture(null);
      } else {
         return this.apiClient
            .getSkyblockProfiles(uuid)
            .thenAccept(
               response -> {
                  if (response == null) {
                     PotatoToolMod.LOGGER.warn("No API response for " + username + " - possibly rate limited or invalid key");
                  } else {
                     PotatoToolMod.LOGGER.info("Processing API response for " + username);
                     if (response.has("success") && response.get("success").getAsBoolean()) {
                        if (response.has("profiles") && !response.get("profiles").isJsonNull()) {
                           JsonArray profiles = response.getAsJsonArray("profiles");
                           if (profiles.size() == 0) {
                              PotatoToolMod.LOGGER.info(username + " has no Skyblock profiles");
                           } else {
                              PotatoToolMod.LOGGER.info(username + " has " + profiles.size() + " profiles");
                              ScannedPlayer player = new ScannedPlayer(username, uuid);
                              JsonObject selectedProfile = null;

                              for (JsonElement profileElement : profiles) {
                                 JsonObject profile = profileElement.getAsJsonObject();
                                 if (profile.has("selected") && profile.get("selected").getAsBoolean()) {
                                    selectedProfile = profile;
                                    break;
                                 }
                              }

                              if (selectedProfile == null && profiles.size() > 0) {
                                 selectedProfile = profiles.get(0).getAsJsonObject();
                              }

                              if (selectedProfile != null) {
                                 PotatoToolMod.LOGGER.info("Processing selected profile for " + username);
                                 this.processProfile(player, selectedProfile);
                                 this.dataStorage.addScannedPlayer(player);
                                 this.apiClient.getPlayerInfo(uuid).thenAccept(info -> {
                                    if (info != null) {
                                       if (info.rank != null) {
                                          player.setRank(info.rank);
                                       }

                                       if (info.lastLogin != null) {
                                          player.setLastLoginTimestamp(info.lastLogin);
                                       }

                                       if (info.lastLogout != null) {
                                          player.setLastLogoutTimestamp(info.lastLogout);
                                       }
                                    }
                                 });
                                 if (player.getSpecialItemCount() > 0) {
                                    PotatoToolMod.LOGGER
                                       .info("✓ " + username + " - Lv" + (int)player.getSkyblockLevel() + " - " + player.getSpecialItemCount() + " items");
                                 } else {
                                    PotatoToolMod.LOGGER.info("✓ " + username + " - Lv" + (int)player.getSkyblockLevel() + " - no special items found");
                                 }

                                 ScanAlertService.onPlayerHit(player);
                              } else {
                                 PotatoToolMod.LOGGER.warn(username + " has no selected profile!");
                              }
                           }
                        } else {
                           PotatoToolMod.LOGGER.info(username + " has no Skyblock profiles");
                        }
                     } else {
                        if (response.has("cause")) {
                           PotatoToolMod.LOGGER.warn("API error for " + username + ": " + response.get("cause").getAsString());
                        }
                     }
                  }
               }
            )
            .exceptionally(throwable -> {
               PotatoToolMod.LOGGER.error("Exception scanning " + username + ": " + throwable.getMessage());
               return null;
            });
      }
   }

   public boolean isOnHypixelSkyblock(Minecraft client) {
      if (client.getCurrentServer() != null) {
         String serverAddress = client.getCurrentServer().ip.toLowerCase();
         if (!serverAddress.contains("hypixel")) {
            return false;
         }
      }

      if (client.level != null) {
         Scoreboard scoreboard = client.level.getScoreboard();
         if (scoreboard != null) {
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective != null) {
               String title = objective.getDisplayName().getString().toLowerCase();
               return title.contains("skyblock") || title.contains("skiblock");
            }
         }
      }

      return false;
   }

   public static String getSkyblockIslandLine(Minecraft client) {
      try {
         if (client == null || client.level == null) {
            return "";
         }

         Scoreboard scoreboard = client.level.getScoreboard();
         if (scoreboard == null) {
            return "";
         }

         Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
         if (objective == null) {
            return "";
         }

         String title = objective.getDisplayName().getString().toLowerCase();
         if (!title.contains("skyblock") && !title.contains("skiblock")) {
            return "";
         }

         for (PlayerTeam team : scoreboard.getPlayerTeams()) {
            String line = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
            String plain = line.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
            if (plain.indexOf('\u2388') >= 0) {
               return plain.replace("\u2388", "").trim().toLowerCase();
            }
         }
      } catch (Throwable ignored) {
      }

      return "";
   }

   public int getPlayerCountInRange(Minecraft client) {
      if (client == null || client.getConnection() == null) {
         return 0;
      }

      Set<String> seenUuids = new HashSet<>();
      int count = 0;
      for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
         if (entry.getProfile() == null) {
            continue;
         }

         GameProfile profile = entry.getProfile();
         UUID uuid = getProfileId(profile);
         String name = getProfileName(profile);
         if (!isRealLobbyPlayer(name, uuid) || isLocalScanTarget(client, name, uuid)) {
            continue;
         }

         String uuidNoDashes = formatUuidForApi(uuid);
         if (seenUuids.add(uuidNoDashes)) {
            count++;
         }
      }

      return count;
   }

   private List<Entry<String, String>> collectLobbyScanTargets(Minecraft client) {
      List<Entry<String, String>> toScan = new ArrayList<>();
      if (client == null || client.getConnection() == null) {
         return toScan;
      }

      int skipped = 0;
      Set<String> seenUuids = new HashSet<>();
      for (PlayerInfo entry : client.getConnection().getOnlinePlayers()) {
         if (entry.getProfile() == null) {
            continue;
         }

         GameProfile profile = entry.getProfile();
         UUID uuid = getProfileId(profile);
         String name = getProfileName(profile);
         if (!isRealLobbyPlayer(name, uuid) || isLocalScanTarget(client, name, uuid)) {
            continue;
         }

         String uuidNoDashes = formatUuidForApi(uuid);
         if (!seenUuids.add(uuidNoDashes)) {
            continue;
         }

         if (this.alreadyInScanCache(uuidNoDashes)) {
            skipped++;
            continue;
         }

         toScan.add(new SimpleEntry<>(name, uuidNoDashes));
      }

      this.alreadySeenThisRun.set(skipped);
      return toScan;
   }

   private boolean alreadyInScanCache(String uuidNoDashes) {
      if (uuidNoDashes == null || uuidNoDashes.isEmpty()) {
         return false;
      }

      if (ScannedUuidCache.contains(uuidNoDashes) || this.scannedPlayers.contains(uuidNoDashes)) {
         return true;
      }

      return this.dataStorage != null && this.dataStorage.getPlayerByUuid(uuidNoDashes) != null;
   }

   private static boolean isLocalScanTarget(Minecraft client, String name, UUID uuid) {
      if (client == null) {
         return false;
      }

      try {
         if (client.getUser() != null) {
            String userName = client.getUser().getName();
            if (userName != null && name != null && userName.equalsIgnoreCase(name)) {
               return true;
            }
         }
      } catch (Throwable ignored) {
      }

      if (client.player == null) {
         return false;
      }

      try {
         GameProfile self = client.player.getGameProfile();
         String selfName = getProfileName(self);
         UUID selfId = getProfileId(self);
         if (selfName != null && name != null && selfName.equalsIgnoreCase(name)) {
            return true;
         }

         return selfId != null && uuid != null && selfId.equals(uuid);
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static boolean isRealLobbyPlayer(String name, UUID uuid) {
      if (name == null || uuid == null) {
         return false;
      }

      String trimmed = name.trim();
      if (!VALID_IGN.matcher(trimmed).matches() || SKIP_NAMES.contains(trimmed.toLowerCase())) {
         return false;
      }

      if (NPC_TEN_CHAR_IGN.matcher(trimmed).matches()) {
         return false;
      }

      String uuidStr = uuid.toString();
      if (uuidStr.startsWith("00000000-0000-") || uuidStr.endsWith("-0000-000000000000") || uuidStr.contains("-0000aaa")) {
         return false;
      }

      int version = uuid.version();
      return version == 3 || version == 4;
   }

   public void scanPlayer(String username, String uuid) {
      if (!this.alreadyInScanCache(uuid)) {
         this.scannedPlayers.add(uuid);
         if (this.config.getRandomApiKey() == null) {
            PotatoToolMod.LOGGER.error("No API key configured! Add one with /scannerkey add <key>");
         } else {
            this.apiClient
               .getSkyblockProfiles(uuid)
               .thenAccept(
                  response -> {
                     Minecraft client = Minecraft.getInstance();
                     if (response != null) {
                        ScannedUuidCache.record(uuid, username);
                     }
                     if (response == null) {
                        PotatoToolMod.LOGGER.warn("No response from API for player: " + username);
                        if (client != null && client.player != null) {
                           client.execute(
                              () -> client.player
                                 .sendSystemMessage(
                                    Component.literal("§cCould not load profile for §e" + username + " §7(timeout/rate limit or API off)"))
                           );
                        }
                     } else if (!response.has("success") || !response.get("success").getAsBoolean()) {
                        PotatoToolMod.LOGGER.warn("API returned error for player: " + username);
                        if (client != null && client.player != null) {
                           client.execute(
                              () -> client.player
                                 .sendSystemMessage(Component.literal("§cAPI error for §e" + username + "§7. Check key or wait if rate limited."))
                           );
                        }
                     } else if (response.has("profiles") && !response.get("profiles").isJsonNull()) {
                        JsonArray profiles = response.getAsJsonArray("profiles");
                        if (profiles.size() == 0) {
                           if (client != null && client.player != null) {
                              client.execute(() -> client.player.sendSystemMessage(Component.literal("§7No Skyblock profiles for §e" + username)));
                           }
                        } else {
                           ScannedPlayer player = new ScannedPlayer(username, uuid);
                           this.processAllProfiles(player, profiles);
                           if (!player.getProfiles().isEmpty()) {
                              this.dataStorage.addScannedPlayer(player);
                              this.apiClient.getPlayerInfo(uuid).thenAccept(info -> {
                                 if (info != null) {
                                    if (info.rank != null) {
                                       player.setRank(info.rank);
                                    }

                                    if (info.lastLogin != null) {
                                       player.setLastLoginTimestamp(info.lastLogin);
                                    }

                                    if (info.lastLogout != null) {
                                       player.setLastLogoutTimestamp(info.lastLogout);
                                    }
                                 }
                              });
                              if (client != null) {
                                 client.execute(
                                    () -> {
                                       ScanAlertService.onPlayerHit(player);
                                       if (player.hasMentionableItems()) {
                                          ChatMessageFormatter.sendPlayerScanResults(player, this.getPlayerCountInRange(client), 1, 0);
                                       } else {
                                          client.player
                                             .sendSystemMessage(
                                                Component.literal(
                                                   "§7Scanned §e"
                                                      + username
                                                      + " §7(level §f"
                                                      + (int)player.getSkyblockLevel()
                                                      + "§7, §f"
                                                      + player.getSpecialItemCount()
                                                      + " §7special items)"
                                                ));
                                       }
                                    }
                                 );
                              }
                           }
                        }
                     } else {
                        if (client != null && client.player != null) {
                           client.execute(() -> client.player.sendSystemMessage(Component.literal("§7No Skyblock profiles for §e" + username)));
                        }
                     }
                  }
               )
               .exceptionally(
                  throwable -> {
                     PotatoToolMod.LOGGER.error("Exception scanning player " + username, throwable);
                     Minecraft c = Minecraft.getInstance();
                     if (c != null && c.player != null) {
                        c.execute(
                           () -> c.player
                              .sendSystemMessage(Component.literal("§cCould not load profile for §e" + username + " §7(timeout/API off)"))
                        );
                     }

                     return null;
                  }
               );
         }
      }
   }

   private void processAllProfiles(ScannedPlayer player, JsonArray profiles) {
      if (player == null || profiles == null) {
         return;
      }

      for (JsonElement profileElement : profiles) {
         if (profileElement == null || !profileElement.isJsonObject()) {
            continue;
         }

         this.processProfile(player, profileElement.getAsJsonObject());
      }

      player.setCurrentScanProfile(null);
   }

   private void processProfile(ScannedPlayer player, JsonObject profile) {
      String profileName = "Unknown";
      if (profile.has("cute_name") && !profile.get("cute_name").isJsonNull()) {
         try {
            profileName = profile.get("cute_name").getAsString();
         } catch (Exception e) {
            PotatoToolMod.LOGGER.warn("Failed to get profile name: " + e.getMessage());
         }
      }

      String gameMode = profile.has("game_mode") && !profile.get("game_mode").isJsonNull() ? profile.get("game_mode").getAsString() : "normal";
      boolean selected = profile.has("selected") && profile.get("selected").getAsBoolean();
      boolean isIronman = gameMode.equalsIgnoreCase("ironman");
      boolean isStranded = gameMode.equalsIgnoreCase("island");
      int profileLevel = 0;
      JsonObject memberData = this.findMemberData(player, profile);
      if (memberData != null && memberData.has("leveling") && memberData.getAsJsonObject("leveling").has("experience")) {
         double exp = memberData.getAsJsonObject("leveling").get("experience").getAsDouble();
         profileLevel = (int)Math.floor(exp / 100.0);
      }

      player.addProfile(new ScannedPlayer.ProfileInfo(profileName, profileLevel, selected, gameMode, !hasInventoryApi(memberData)));
      if (selected || player.getSelectedProfile() == null || player.getSelectedProfile().isBlank()) {
         player.setSelectedProfile(profileName);
         player.setSkyblockLevel(profileLevel);
      }

      if (isIronman && !this.config.scanIronmanProfiles) {
         PotatoToolMod.LOGGER.info("Skipping ironman profile " + profileName);
         return;
      }

      if (isStranded && !this.config.scanStrandedProfiles) {
         PotatoToolMod.LOGGER.info("Skipping stranded profile " + profileName);
         return;
      }

      if (memberData == null) {
         PotatoToolMod.LOGGER.warn("No member data found for " + player.getUsername() + " on " + profileName);
         return;
      }

      player.setCurrentScanProfile(profileName);
      if (this.shouldScanPlayerInventories(profileLevel)) {
         this.scanInventories(player, memberData);
         this.scanEnderChest(player, memberData);
         this.scanBackpacks(player, memberData);
         this.scanEquipment(player, memberData);
         this.scanWardrobe(player, memberData);
         this.scanPets(player, memberData);
      } else {
         PotatoToolMod.LOGGER.info(
            "Skipping " + player.getUsername() + " profile " + profileName + " - Level " + profileLevel + " outside scan range"
         );
      }
   }

   private JsonObject findMemberData(ScannedPlayer player, JsonObject profile) {
      if (profile == null || !profile.has("members")) {
         return null;
      }

      try {
         JsonObject members = profile.getAsJsonObject("members");
         if (members == null) {
            return null;
         }

         String playerUuid = player.getUuid() == null ? "" : player.getUuid().replace("-", "");
         for (String key : members.keySet()) {
            if (key.replace("-", "").equals(playerUuid)) {
               return members.getAsJsonObject(key);
            }
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.warn("Failed to find member data: " + e.getMessage());
      }

      return null;
   }

   private boolean shouldScanPlayerInventories(ScannedPlayer player) {
      return this.shouldScanPlayerInventories(player.getSkyblockLevel());
   }

   private boolean shouldScanPlayerInventories(double level) {
      if (this.config.seymourScanningEnabled && level >= 0.0 && level <= 500.0) {
         return true;
      }

      if (this.config.skyblockLevelCap > 0.0 && level > this.config.skyblockLevelCap) {
         return false;
      }

      if (level < this.config.minSkyblockLevel) {
         return false;
      }

      return this.config.scanLowLevelPlayers || level >= 50.0;
   }

   private static boolean hasInventoryApi(JsonObject memberData) {
      if (memberData == null || !memberData.has("inventory")) {
         return false;
      }

      JsonObject inventory = memberData.getAsJsonObject("inventory");
      return inventory.has("inv_contents") && inventory.getAsJsonObject("inv_contents").has("data");
   }

   private void scanInventories(ScannedPlayer player, JsonObject memberData) {
      int foundInventories = 0;
      JsonObject inventoryContainer = null;
      if (memberData.has("inventory")) {
         inventoryContainer = memberData.getAsJsonObject("inventory");
         if (inventoryContainer.has("inv_contents") && inventoryContainer.getAsJsonObject("inv_contents").has("data")) {
            String invData = inventoryContainer.getAsJsonObject("inv_contents").get("data").getAsString();
            this.parseInventoryData(player, invData, "Inventory");
            foundInventories++;
         }

         if (inventoryContainer.has("bag_contents")) {
            JsonObject bagContents = inventoryContainer.getAsJsonObject("bag_contents");

            for (String bagKey : bagContents.keySet()) {
               JsonElement bagElement = bagContents.get(bagKey);
               if (bagElement.isJsonObject() && bagElement.getAsJsonObject().has("data")) {
                  String bagData = bagElement.getAsJsonObject().get("data").getAsString();
                  String bagName = this.formatBagName(bagKey);
                  this.parseInventoryData(player, bagData, bagName);
                  foundInventories++;
               }
            }
         }

         if (inventoryContainer.has("personal_vault_contents") && inventoryContainer.getAsJsonObject("personal_vault_contents").has("data")) {
            String vaultData = inventoryContainer.getAsJsonObject("personal_vault_contents").get("data").getAsString();
            this.parseInventoryData(player, vaultData, "Vault");
            foundInventories++;
         }

      } else {
         PotatoToolMod.LOGGER.warn("No inventory container in memberData!");
      }
   }

   private String formatBagName(String bagKey) {
      if (bagKey.matches("\\d+")) {
         return "Bag " + (Integer.parseInt(bagKey) + 1);
      }

      String[] parts = bagKey.replace("_", " ").toLowerCase().split(" ");
      StringBuilder result = new StringBuilder();

      for (String part : parts) {
         if (result.length() > 0) {
            result.append(" ");
         }

         if (part.length() > 0) {
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
               result.append(part.substring(1));
            }
         }
      }

      return result.toString();
   }

   private void scanEnderChest(ScannedPlayer player, JsonObject memberData) {
      if (memberData.has("inventory")) {
         JsonObject inventoryContainer = memberData.getAsJsonObject("inventory");
         if (!inventoryContainer.has("ender_chest_contents")) {
         } else {
            JsonObject enderObj = inventoryContainer.getAsJsonObject("ender_chest_contents");
            if (enderObj.has("data")) {
               String enderData = enderObj.get("data").getAsString();
               this.parseInventoryData(player, enderData, "Ender Chest");
            }
         }
      }
   }

   private void scanBackpacks(ScannedPlayer player, JsonObject memberData) {
      if (memberData.has("inventory")) {
         JsonObject inventoryContainer = memberData.getAsJsonObject("inventory");
         int backpackCount = 0;
         if (inventoryContainer.has("backpack_contents")) {
            JsonObject backpacks = inventoryContainer.getAsJsonObject("backpack_contents");
            List<String> keys = new ArrayList<>(backpacks.keySet());
            keys.sort((a, b) -> {
               boolean aNum = a.matches("\\d+");
               boolean bNum = b.matches("\\d+");
               if (aNum && bNum) {
                  return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
               } else if (aNum) {
                  return -1;
               } else {
                  return bNum ? 1 : a.compareTo(b);
               }
            });

            for (String key : keys) {
               if (backpacks.get(key).isJsonObject()) {
                  JsonObject backpack = backpacks.getAsJsonObject(key);
                  if (backpack.has("data")) {
                     String backpackData = backpack.get("data").getAsString();
                     String displayName = key.matches("\\d+") ? "Backpack " + (Integer.parseInt(key) + 1) : "Backpack " + key;
                     this.parseInventoryData(player, backpackData, displayName);
                     backpackCount++;
                  }
               }
            }
         }

         if (backpackCount > 0) {
         }
      }
   }

   private void scanEquipment(ScannedPlayer player, JsonObject memberData) {
      if (memberData.has("inventory")) {
         JsonObject inventoryContainer = memberData.getAsJsonObject("inventory");
         int equipmentFound = 0;
         if (inventoryContainer.has("inv_armor") && inventoryContainer.getAsJsonObject("inv_armor").has("data")) {
            String armorData = inventoryContainer.getAsJsonObject("inv_armor").get("data").getAsString();
            this.parseInventoryData(player, armorData, "Armor");
            equipmentFound++;
         }

         if (inventoryContainer.has("equipment_contents") && inventoryContainer.getAsJsonObject("equipment_contents").has("data")) {
            String equipData = inventoryContainer.getAsJsonObject("equipment_contents").get("data").getAsString();
            this.parseInventoryData(player, equipData, "Equipment");
            equipmentFound++;
         }

         if (inventoryContainer.has("wardrobe_equipped_slot")) {
            int equippedSlot = inventoryContainer.get("wardrobe_equipped_slot").getAsInt();
         }

         if (equipmentFound > 0) {
         }
      }
   }

   private void scanPets(ScannedPlayer player, JsonObject memberData) {
      if (!this.config.scanCosmeticSkinsEnabled) {
         return;
      }

      JsonArray pets = null;
      if (memberData.has("pets_data") && memberData.getAsJsonObject("pets_data").has("pets")) {
         JsonElement petsEl = memberData.getAsJsonObject("pets_data").get("pets");
         if (petsEl != null && petsEl.isJsonArray()) {
            pets = petsEl.getAsJsonArray();
         }
      } else if (memberData.has("pets") && memberData.get("pets").isJsonArray()) {
         pets = memberData.getAsJsonArray("pets");
      }

      if (pets == null) {
         return;
      }

      String mode = this.config.cosmeticSkinScanMode != null ? this.config.cosmeticSkinScanMode : "BOTH";
      boolean includeUnapplied = "BOTH".equals(mode) || "UNAPPLIED_ONLY".equals(mode);
      if (!includeUnapplied) {
         return;
      }

      for (JsonElement el : pets) {
         if (!el.isJsonObject()) {
            continue;
         }

         JsonObject pet = el.getAsJsonObject();
         if (!pet.has("skin") || pet.get("skin").isJsonNull()) {
            continue;
         }

         String skinId = pet.get("skin").getAsString();
         if (skinId == null || skinId.isEmpty()) {
            continue;
         }

         if (!skinId.startsWith("PET_SKIN_") && !skinId.contains("_SKIN_")) {
            skinId = "PET_SKIN_" + skinId;
         }

         Double valueM = CosmeticSkinValuesLoader.getValueMillions(skinId, false);
         if (valueM == null || valueM < this.config.minCosmeticSkinValueMillions) {
            continue;
         }

         String petType = pet.has("type") ? pet.get("type").getAsString() : "Pet";
         ScannedItem skinItem = new ScannedItem(skinId, petType.replace('_', ' ') + " (pet skin)", "Pets");
         skinItem.setCategory(ScannedItem.ItemCategory.COSMETIC_SKIN);
         skinItem.setValueMillions(valueM);
         skinItem.setCosmeticSkinApplied(false);
         player.addSpecialItem(skinItem);
      }
   }

   private void scanWardrobe(ScannedPlayer player, JsonObject memberData) {
      if (memberData.has("inventory")) {
         JsonObject inventoryContainer = memberData.getAsJsonObject("inventory");
         if (inventoryContainer.has("wardrobe_contents") && inventoryContainer.getAsJsonObject("wardrobe_contents").has("data")) {
            String wardrobeData = inventoryContainer.getAsJsonObject("wardrobe_contents").get("data").getAsString();
            this.parseInventoryData(player, wardrobeData, "Wardrobe");
         }
      }
   }

   private CompoundTag getNbtCompound(CompoundTag parent, String key) {
      Tag element = parent.get(key);
      return element instanceof CompoundTag ? (CompoundTag)element : new CompoundTag();
   }

   private String getNbtString(CompoundTag parent, String key) {
      try {
         if (!parent.keySet().contains(key)) {
            return "";
         }

         Tag element = parent.get(key);
         if (element == null) {
            return "";
         }

         try {
            java.lang.reflect.Method valueMethod = element.getClass().getMethod("value");
            Object raw = valueMethod.invoke(element);
            if (raw instanceof String s && !s.isBlank()) {
               return s;
            }
         } catch (ReflectiveOperationException ignored) {
         }

         if (element instanceof CompoundTag compound) {
            return compound.toString();
         }

         String value = element.toString();
         if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
         }

         return value.replace("\\\"", "\"").replace("\\\\", "\\");
      } catch (Exception e) {
         return "";
      }
   }

   private int getNbtInt(CompoundTag parent, String key) {
      try {
         if (!parent.keySet().contains(key)) {
            return 0;
         } else {
            Tag element = parent.get(key);
            if (element == null) {
               return 0;
            } else if (element instanceof IntTag intTag) {
               return intTag.intValue();
            } else {
               try {
                  java.lang.reflect.Method valueMethod = element.getClass().getMethod("value");
                  Object raw = valueMethod.invoke(element);
                  if (raw instanceof Number n) {
                     return n.intValue();
                  }
               } catch (ReflectiveOperationException ignored) {
               }

               String parsed = ColorAnalyzer.normalizeHex(element.toString());
               return parsed == null ? 0 : ColorAnalyzer.hexToInt(parsed);
            }
         }
      } catch (Exception e) {
         return 0;
      }
   }

   private static String pagedLocation(String location, int slot) {
      if (location == null || location.isBlank()) {
         return location;
      }

      if ("Ender Chest".equalsIgnoreCase(location) || "Enderchest".equalsIgnoreCase(location)) {
         int page = slot / 45 + 1;
         return page < 1 ? "Enderchest" : "Enderchest " + page;
      }

      if ("Wardrobe".equalsIgnoreCase(location)) {
         int page = slot / 9 + 1;
         return page < 1 ? "Wardrobe" : "Wardrobe " + page;
      }

      return location;
   }

   private void parseInventoryData(ScannedPlayer player, String base64Data, String location) {
      try {
         byte[] compressed = Base64.getDecoder().decode(base64Data);
         ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
         CompoundTag nbt = NbtIo.readCompressed(byteStream, NbtAccounter.unlimitedHeap());
         if (nbt.keySet().contains("i")) {
            try {
               if (nbt.get("i") instanceof ListTag itemsList) {
                  for (int i = 0; i < itemsList.size(); i++) {
                     Tag itemElement = (Tag)itemsList.get(i);
                     if (itemElement instanceof CompoundTag itemNbt) {
                        this.processItem(player, itemNbt, pagedLocation(location, i));
                     }
                  }
               }
            } catch (Exception e) {
               PotatoToolMod.LOGGER.error("Error parsing inventory items list", e);
            }
         }
      } catch (Exception var13) {
      }
   }

   private void processItem(ScannedPlayer player, CompoundTag itemNbt, String location) {
      CompoundTag tag = itemNbt.keySet().contains("tag") ? this.getNbtCompound(itemNbt, "tag") : itemNbt;
      CompoundTag extra = null;
      if (tag.keySet().contains("ExtraAttributes")) {
         extra = this.getNbtCompound(tag, "ExtraAttributes");
      } else if (itemNbt.keySet().contains("ExtraAttributes")) {
         extra = this.getNbtCompound(itemNbt, "ExtraAttributes");
      }

      if (extra == null) {
         return;
      }

      String itemId = extra.keySet().contains("id") ? this.getNbtString(extra, "id") : null;
      if (itemId == null || itemId.isEmpty() || this.config.itemBlacklist.contains(itemId)) {
         return;
      }

      if (this.config.useWhitelist && !this.config.itemWhitelist.isEmpty() && !this.config.itemWhitelist.contains(itemId)) {
         return;
      }

      boolean isSoulbound = extra.keySet().contains("donated_museum")
         || tag.keySet().contains("display")
            && this.getNbtCompound(tag, "display").keySet().contains("Lore")
            && this.getNbtString(this.getNbtCompound(tag, "display"), "Lore").contains("Soulbound");
      if (isSoulbound && !this.config.scanSoulboundItems) {
         return;
      }

      String displayName = itemId;
      if (tag.keySet().contains("display")) {
         CompoundTag display = this.getNbtCompound(tag, "display");
         if (display.keySet().contains("Name")) {
            displayName = com.potatotool.util.ItemNames.resolve(this.getNbtString(display, "Name"), itemId);
         }
      }

      if (displayName == null || displayName.isBlank() || displayName.equals(itemId)) {
         displayName = com.potatotool.util.ItemNames.resolve(displayName, itemId);
      }

      if (extra.keySet().contains("party_hat_color")) {
         String hatColor = this.getNbtString(extra, "party_hat_color");
         if (hatColor != null && !hatColor.isEmpty()) {
            displayName = displayName + " (" + hatColor + ")";
         }
      }

      ScannedItem item = new ScannedItem(itemId, displayName, location);
      item.setScuffed(displayName != null && displayName.toLowerCase().contains("scuffed"));
      if (extra.keySet().contains("dungeon_item_level")) {
         int stars = this.getNbtInt(extra, "dungeon_item_level");
         if (stars >= 0 && stars <= 10) {
            item.setDungeonStars(stars);
         }
      }

      if (extra.keySet().contains("recombobulated") && this.getNbtInt(extra, "recombobulated") != 0) {
         item.setRecombobulated(true);
      }

      SeymourAnalyzer.PieceType seymourPiece = SeymourAnalyzer.detectPieceType(displayName, itemId);
      boolean scanningSeymourPiece = seymourPiece != null;
      if (scanningSeymourPiece) {
         player.addSeymourPiece();
      }
      if (scanningSeymourPiece && !(this.config.seymourScanningEnabled && SeymourAnalyzer.isPieceEnabled(this.config, seymourPiece))) {
         return;
      }

      boolean anniversaryHat = ColorAnalyzer.isAnniversaryHatId(itemId);
      if (anniversaryHat && !this.config.scanAnniversaryHats) {
         return;
      }

      boolean editionedCollectible = EditionedCollectibles.isTracked(itemId, displayName);
      if (!scanningSeymourPiece
         && !anniversaryHat
         && !editionedCollectible
         && !this.shouldScanItemType(itemId)
         && !ColorAnalyzer.isFairyArmorId(itemId)
         && !ColorAnalyzer.isCrystalArmorId(itemId)) {
         return;
      }

      String hexColor = this.extractItemHex(extra, tag);
      if (hexColor != null) {
         item.setHexColor(hexColor);
      }

      if (anniversaryHat) {
         item.setCategory(ScannedItem.ItemCategory.VALUABLE);
         player.addSpecialItem(item);
         return;
      }

      if (this.applyEditionedCollectible(item, extra, itemId, displayName)) {
         player.addSpecialItem(item);
         return;
      }

      if (this.config.valuableItemsEnabled && ValuableItemsLoader.isValuable(itemId)) {
         item.setCategory(ScannedItem.ItemCategory.VALUABLE);
         player.addSpecialItem(item);
         return;
      }

      if (this.config.scanCosmeticSkinsEnabled && this.config.scanCosmetics && extra.keySet().contains("skin")) {
         String skinId = this.getNbtString(extra, "skin");
         if (skinId != null && !skinId.isEmpty()) {
            Double valueM = CosmeticSkinValuesLoader.getValueMillions(skinId, true);
            String mode = this.config.cosmeticSkinScanMode != null ? this.config.cosmeticSkinScanMode : "BOTH";
            boolean includeApplied = "BOTH".equals(mode) || "APPLIED_ONLY".equals(mode);
            if (valueM != null && valueM >= this.config.minCosmeticSkinValueMillions && includeApplied) {
               ScannedItem skinItem = new ScannedItem(skinId, displayName != null ? displayName + " (skin)" : "Skin", location);
               skinItem.setCategory(ScannedItem.ItemCategory.COSMETIC_SKIN);
               skinItem.setValueMillions(valueM);
               skinItem.setCosmeticSkinApplied(true);
               player.addSpecialItem(skinItem);
            }
         }
      }

      if (this.config.scanCosmeticSkinsEnabled && this.config.minCosmeticSkinValueMillions >= 0.0 && this.config.scanCosmetics) {
         String idLower = itemId.toLowerCase();
         boolean isCosmeticSkin = idLower.contains("_skin_") || idLower.contains("cape") || idLower.contains("cloak");
         if (isCosmeticSkin) {
            Double valueM = CosmeticSkinValuesLoader.getValueMillions(itemId, false);
            String mode = this.config.cosmeticSkinScanMode != null ? this.config.cosmeticSkinScanMode : "BOTH";
            boolean includeUnapplied = "BOTH".equals(mode) || "UNAPPLIED_ONLY".equals(mode);
            if (valueM != null && valueM >= this.config.minCosmeticSkinValueMillions && includeUnapplied) {
               item.setCategory(ScannedItem.ItemCategory.COSMETIC_SKIN);
               item.setValueMillions(valueM);
               item.setCosmeticSkinApplied(false);
               player.addSpecialItem(item);
               return;
            }
         }
      }

      if (this.config.newYearCakeEnabled && isNewYearCake(itemId, displayName)) {
         Integer year = parseCakeYearFromDisplay(displayName);
         if (year != null && this.config.wantsCakeYear(year)) {
            item.setCategory(ScannedItem.ItemCategory.CAKE);
            item.setCakeYear(year);
            player.addSpecialItem(item);
            return;
         }
      }

      if (extra.keySet().contains("modifier")) {
         String reforge = this.getNbtString(extra, "modifier");
         item.setReforge(reforge);
         if (this.config.legacyReforgeEnabled && this.isLegacyReforge(reforge)) {
            item.setCategory(ScannedItem.ItemCategory.LEGACY_REFORGE);
            player.addSpecialItem(item);
            return;
         }

         if (this.config.ghostReforgeEnabled && this.isGhostReforge(reforge)) {
            item.setCategory(ScannedItem.ItemCategory.GHOST_REFORGE);
            player.addSpecialItem(item);
            return;
         }
      }

      if (hexColor == null) {
         return;
      }

      boolean hasCosmeticDye = extra.keySet().contains("dye_item") || extra.keySet().contains("dye");
      String cosmeticDyeToken = extra.keySet().contains("dye_item")
         ? this.getNbtString(extra, "dye_item")
         : (extra.keySet().contains("dye") ? this.getNbtString(extra, "dye") : null);
      long timestampMs = this.getNbtLong(extra, "timestamp");
      if (timestampMs <= 0L) {
         timestampMs = this.getNbtLong(extra, "originTimestamp");
      }

      if (scanningSeymourPiece && this.config.seymourScanningEnabled) {
         SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(hexColor, displayName, itemId, this.config);
         if (match != null && SeymourAnalyzer.isTierEnabled(this.config, match.getTier())) {
            item.setSeymourMatchName(SeymourAnalyzer.prettyMatchLabel(match.getMatchedName()));
            item.setCategory(SeymourAnalyzer.toCategory(match.getTier()));
            player.addSpecialItem(item);
            return;
         }
      }

      if (this.tryAddSpecificHexMatch(player, item, hexColor, displayName)) {
         return;
      }

      boolean fairyArmorPiece = ColorAnalyzer.isFairyArmorId(itemId);
      boolean crystalArmorPiece = ColorAnalyzer.isCrystalArmorId(itemId);
      if (!fairyArmorPiece && this.config.ogFairyScanningEnabled && ColorAnalyzer.isOgFairyDyeToken(cosmeticDyeToken)) {
         item.setCategory(ScannedItem.ItemCategory.OG_FAIRY);
         player.addSpecialItem(item);
         return;
      }

      if (!fairyArmorPiece && this.config.fairyScanningEnabled && ColorAnalyzer.isFairyDyeToken(cosmeticDyeToken)) {
         item.setCategory(ScannedItem.ItemCategory.FAIRY);
         player.addSpecialItem(item);
         return;
      }

      if (ColorAnalyzer.isGlitchedDungeonArmor(itemId, hexColor, cosmeticDyeToken)
         || ColorAnalyzer.isBlackWitherArmorByName(displayName, hexColor, cosmeticDyeToken)) {
         item.setCategory(ScannedItem.ItemCategory.GLITCHED);
         player.addSpecialItem(item);
         return;
      }

      if (!fairyArmorPiece && this.config.ogFairyScanningEnabled && ColorAnalyzer.isOgFairyMatch(hexColor, itemId, timestampMs)) {
         item.setCategory(ScannedItem.ItemCategory.OG_FAIRY);
         player.addSpecialItem(item);
         return;
      }

      if (!fairyArmorPiece && !crystalArmorPiece && this.config.crystalScanningEnabled && ColorAnalyzer.isCrystalColor(hexColor)) {
         item.setCategory(ScannedItem.ItemCategory.CRYSTAL);
         player.addSpecialItem(item);
         return;
      }

      if (!fairyArmorPiece && this.config.fairyScanningEnabled && ColorAnalyzer.isFairyColor(hexColor)) {
         item.setCategory(ScannedItem.ItemCategory.FAIRY);
         player.addSpecialItem(item);
         return;
      }

      if (!fairyArmorPiece
         && this.config.bleachedScanningEnabled
         && !isBasicLeatherByDisplayName(displayName)
         && ColorAnalyzer.isBleachedColor(hexColor, itemId)) {
         item.setCategory(ScannedItem.ItemCategory.BLEACHED);
         player.addSpecialItem(item);
         return;
      }

      if (DefaultArmorColorsLoader.shouldIgnoreColor(itemId)) {
         return;
      }

      if (!hasCosmeticDye
         && !ColorAnalyzer.isNonFairyCosmeticDye(cosmeticDyeToken)
         && this.config.exoticScanningEnabled
         && ColorAnalyzer.isExoticColor(hexColor, itemId, false, cosmeticDyeToken)
         && !isBasicLeatherByDisplayName(displayName)) {
         item.setCategory(ScannedItem.ItemCategory.EXOTIC);
         player.addSpecialItem(item);
      }
   }

   private String extractItemHex(CompoundTag extra, CompoundTag tag) {
      if (extra != null && extra.keySet().contains("color")) {
         String fromTag = this.colorTagToHex(extra.get("color"));
         if (fromTag != null) {
            return fromTag;
         }

         String parsed = ColorAnalyzer.normalizeHex(this.getNbtString(extra, "color"));
         if (parsed != null) {
            return "#" + parsed;
         }
      }

      if (tag != null && tag.keySet().contains("display")) {
         CompoundTag display = this.getNbtCompound(tag, "display");
         if (display.keySet().contains("color")) {
            String fromDisplay = this.colorTagToHex(display.get("color"));
            if (fromDisplay != null) {
               return fromDisplay;
            }

            return String.format("#%06X", this.getNbtInt(display, "color") & 16777215);
         }
      }

      return null;
   }

   private String colorTagToHex(Tag colorTag) {
      if (colorTag == null) {
         return null;
      }

      if (colorTag instanceof IntTag intColor) {
         return String.format("#%06X", intColor.intValue() & 16777215);
      }

      try {
         java.lang.reflect.Method valueMethod = colorTag.getClass().getMethod("value");
         Object raw = valueMethod.invoke(colorTag);
         if (raw instanceof Number n) {
            return String.format("#%06X", n.intValue() & 16777215);
         }

         if (raw instanceof String s) {
            String parsed = ColorAnalyzer.normalizeHex(s);
            return parsed == null ? null : "#" + parsed;
         }
      } catch (ReflectiveOperationException ignored) {
      }

      String parsed = ColorAnalyzer.normalizeHex(colorTag.toString());
      return parsed == null ? null : "#" + parsed;
   }

   private long getNbtLong(CompoundTag parent, String key) {
      try {
         if (parent == null || !parent.keySet().contains(key)) {
            return 0L;
         }

         Tag element = parent.get(key);
         if (element instanceof IntTag intTag) {
            return intTag.intValue();
         }

         String raw = element == null ? "" : element.toString().replace("L", "").replace("\"", "").trim();
         if (raw.isEmpty()) {
            return 0L;
         }

         return Long.parseLong(raw);
      } catch (Exception e) {
         return 0L;
      }
   }

   private boolean tryAddSpecificHexMatch(ScannedPlayer player, ScannedItem item, String hexColor, String displayName) {
      if (!this.config.matchesSpecificHex(hexColor)) {
         return false;
      }

      item.setHexColor(hexColor);
      item.setCategory(ScannedItem.ItemCategory.SPECIFIC_HEX);
      player.addSpecialItem(item);
      return true;
   }

   private static boolean isNewYearCake(String itemId, String displayName) {
      if (itemId == null) {
         itemId = "";
      }

      String idLower = itemId.toLowerCase();
      if (idLower.contains("new_year_cake") || idLower.contains("newyearcake")) {
         return true;
      } else {
         return displayName == null ? false : displayName.toLowerCase().contains("new year cake");
      }
   }

   private static String parseColorStringToHex(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String s = raw.trim();
         if (s.contains(":")) {
            String[] parts = s.split(":");
            if (parts.length != 3) {
               return null;
            }

            try {
               int r = Math.max(0, Math.min(255, Integer.parseInt(parts[0].trim())));
               int g = Math.max(0, Math.min(255, Integer.parseInt(parts[1].trim())));
               int b = Math.max(0, Math.min(255, Integer.parseInt(parts[2].trim())));
               return String.format("%02X%02X%02X", r, g, b);
            } catch (NumberFormatException e) {
               return null;
            }
         } else {
            s = s.replace("#", "").toUpperCase();
            if (s.length() != 6) {
               return null;
            }

            for (int i = 0; i < s.length(); i++) {
               char c = s.charAt(i);
               boolean ok = c >= '0' && c <= '9' || c >= 'A' && c <= 'F';
               if (!ok) {
                  return null;
               }
            }

            return s;
         }
      } else {
         return null;
      }
   }

   private static Integer parseCakeYearFromDisplay(String displayName) {
      if (displayName != null && !displayName.isEmpty()) {
         Matcher m = Pattern.compile("(?i)year\\s*(?:#)?\\s*(\\d+)").matcher(displayName);
         if (m.find()) {
            return Integer.parseInt(m.group(1));
         }

         m = Pattern.compile("\\((\\d+)\\)").matcher(displayName);
         return m.find() ? Integer.parseInt(m.group(1)) : null;
      } else {
         return null;
      }
   }

   private static boolean isBasicLeatherByDisplayName(String displayName) {
      if (displayName == null) {
         return false;
      }

      String n = displayName.toUpperCase().replaceAll("§[0-9A-FK-OR]", "").trim();
      return n.contains("LEATHER") && (n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("HELMET") || n.contains("BOOTS"));
   }

   private boolean applyEditionedCollectible(ScannedItem item, CompoundTag extra, String itemId, String displayName) {
      Integer edition = this.readEditionNumber(extra, itemId);
      String serial = this.readSerialNumber(extra, edition);
      if (EditionedCollectibles.isSpaceHelmet(itemId, displayName)) {
         boolean garden = edition == null && (serial == null || serial.isBlank());
         if (garden) {
            item.setItemName("Garden Space Helmet");
            item.setValuableLabel("Garden (less valuable)");
         } else {
            item.setItemName("Space Helmet");
            item.setValuableLabel(EditionedCollectibles.formatEditionSerial(edition, serial));
         }

         item.setCategory(ScannedItem.ItemCategory.VALUABLE);
         return true;
      }

      if (EditionedCollectibles.isBasketOfHope(itemId, displayName)) {
         item.setItemName("Basket of Hope");
         String label = EditionedCollectibles.formatEditionSerial(edition, serial);
         item.setValuableLabel(label.isEmpty() ? "Basket of Hope" : label);
         item.setCategory(ScannedItem.ItemCategory.VALUABLE);
         return true;
      }

      return false;
   }

   private Integer readEditionNumber(CompoundTag extra, String itemId) {
      if (extra != null && extra.keySet().contains("edition")) {
         int n = this.getNbtInt(extra, "edition");
         if (n > 0) {
            return n;
         }

         Integer parsed = EditionedCollectibles.parsePositiveInt(this.getNbtString(extra, "edition"));
         if (parsed != null) {
            return parsed;
         }
      }

      return EditionedCollectibles.editionFromItemId(itemId);
   }

   private String readSerialNumber(CompoundTag extra, Integer edition) {
      if (extra != null) {
         if (extra.keySet().contains("serial")) {
            String serial = this.getNbtString(extra, "serial");
            if (serial != null && !serial.isBlank()) {
               return serial.replaceAll("[^0-9A-Za-z]", "");
            }
         }

         if (extra.keySet().contains("serial_number")) {
            String serial = this.getNbtString(extra, "serial_number");
            if (serial != null && !serial.isBlank()) {
               return serial.replaceAll("[^0-9A-Za-z]", "");
            }

            int n = this.getNbtInt(extra, "serial_number");
            if (n > 0) {
               return String.valueOf(n);
            }
         }
      }

      return edition != null && edition > 0 ? String.valueOf(edition) : null;
   }

   private boolean shouldScanItemType(String itemId) {
      String idLower = itemId.toLowerCase();
      if (idLower.contains("sword") || idLower.contains("bow") || idLower.contains("wand") || idLower.contains("blade") || idLower.contains("scythe")) {
         return this.config.scanWeapons;
      } else if (idLower.contains("helmet") || idLower.contains("chestplate") || idLower.contains("leggings") || idLower.contains("boots")) {
         return this.config.scanArmor;
      } else if (idLower.contains("pickaxe")
         || idLower.contains("axe")
         || idLower.contains("hoe")
         || idLower.contains("shovel")
         || idLower.contains("drill")
         || idLower.contains("gauntlet")) {
         return this.config.scanTools;
      } else if (idLower.contains("pet") || idLower.contains("_skin_")) {
         return this.config.scanPets;
      } else if (idLower.contains("talisman")
         || idLower.contains("ring")
         || idLower.contains("artifact")
         || idLower.contains("relic")
         || idLower.contains("charm")) {
         return this.config.scanAccessories;
      } else if (idLower.contains("potion") || idLower.contains("candy")) {
         return this.config.scanConsumables;
      } else if (idLower.contains("cake")) {
         return this.config.scanConsumables || this.config.newYearCakeEnabled;
      } else if (idLower.contains("hat") || idLower.contains("cape") || idLower.contains("cloak")) {
         return this.config.scanCosmetics;
      } else if (idLower.contains("dungeon")
         || idLower.contains("catacombs")
         || idLower.contains("necron")
         || idLower.contains("wither")
         || idLower.contains("shadow_assassin")) {
         return this.config.scanDungeonItems;
      } else if (idLower.contains("revenant") || idLower.contains("tarantula") || idLower.contains("sven") || idLower.contains("voidgloom")) {
         return this.config.scanSlayerItems;
      } else if (idLower.contains("jerry") || idLower.contains("spooky") || idLower.contains("gift") || idLower.contains("present")) {
         return this.config.scanEventItems;
      } else {
         return !idLower.contains("admin") && !idLower.contains("prismarine") && !idLower.contains("dirt_rod") && !idLower.contains("raygun")
            ? true
            : this.config.scanAdminItems;
      }
   }

   private boolean isLegacyReforge(String reforge) {
      for (String custom : this.config.customLegacyReforges) {
         if (reforge.equalsIgnoreCase(custom)) {
            return true;
         }
      }

      return false;
   }

   private boolean isGhostReforge(String reforge) {
      for (String custom : this.config.customGhostReforges) {
         if (reforge.equalsIgnoreCase(custom)) {
            return true;
         }
      }

      return false;
   }

   static {
      try {
         profileIdField = GameProfile.class.getDeclaredField("id");
         profileIdField.setAccessible(true);
         profileNameField = GameProfile.class.getDeclaredField("name");
         profileNameField.setAccessible(true);
      } catch (Exception e) {
         System.err.println("CRITICAL: Failed to initialize GameProfile reflection: " + e.getMessage());
      }
   }
}
