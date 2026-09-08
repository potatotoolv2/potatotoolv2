package com.potatotool.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient.Builder;

public class HypixelApiClient {
   private static final String HYPIXEL_API_BASE = "https://api.hypixel.net";
   private static final int MAX_CONNECTIONS_TO_HOST = 25;
   private final OkHttpClient httpClient;
   private final ScannerConfig config;
   private final AtomicInteger keyIndex = new AtomicInteger(0);
   private final Map<String, Long> keyThrottledUntilMs = new ConcurrentHashMap<>();
   private final AtomicBoolean rateLimitNotificationShown = new AtomicBoolean(false);
   private final AtomicBoolean invalidKeyNotificationShown = new AtomicBoolean(false);
   private final AtomicLong lastInvalidKeyTime = new AtomicLong(0L);

   public HypixelApiClient(ScannerConfig config) {
      this.config = config;
      Dispatcher dispatcher = new Dispatcher();
      dispatcher.setMaxRequests(24);
      dispatcher.setMaxRequestsPerHost(8);
      this.httpClient = new Builder()
         .dispatcher(dispatcher)
         .connectionPool(new ConnectionPool(16, 1L, TimeUnit.MINUTES))
         .connectTimeout(5L, TimeUnit.SECONDS)
         .readTimeout(12L, TimeUnit.SECONDS)
         .writeTimeout(5L, TimeUnit.SECONDS)
         .retryOnConnectionFailure(false)
         .build();
   }

   private String getNextAvailableApiKey() {
      List<String> keys = this.config.apiKeys;
      if (keys != null && !keys.isEmpty()) {
         long now = System.currentTimeMillis();
         int n = keys.size();
         int start = (this.keyIndex.getAndIncrement() & 2147483647) % n;

         for (int j = 0; j < n; j++) {
            String key = keys.get((start + j) % n);
            if (key != null) {
               Long until = this.keyThrottledUntilMs.get(key);
               if ((until == null || until <= now) && this.config.getApiWindowRemaining(key) > 0) {
                  return key;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public JsonObject getSkyblockProfilesBlocking(String uuid) {
      String apiKey = this.getNextAvailableApiKey();
      if (apiKey == null) {
         return null;
      }

      String url = "https://api.hypixel.net/v2/skyblock/profiles?uuid=" + uuid;
      Request request = new okhttp3.Request.Builder().url(url).header("API-Key", apiKey).build();

      try {
         Response response = this.httpClient.newCall(request).execute();
         this.config.recordApiKeyUse(apiKey);

         JsonObject var8;
         label79: {
            try {
               int code = response.code();
               String body = response.body() != null ? response.body().string() : "";
               if (response.isSuccessful() && !body.isEmpty()) {
                  var8 = JsonParser.parseString(body).getAsJsonObject();
                  break label79;
               }

               if (code == 403) {
                  this.handleInvalidApiKey(body);
               } else if (code == 429) {
                  this.handleRateLimit(apiKey, response, body);
               }
            } catch (Throwable var10) {
               if (response != null) {
                  try {
                     response.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (response != null) {
               response.close();
            }

            return null;
         }

         if (response != null) {
            response.close();
         }

         return var8;
      } catch (java.net.SocketTimeoutException timeout) {
         this.config.recordApiKeyUse(apiKey);
         return null;
      } catch (IOException var11) {
         return null;
      } catch (Exception e) {
         PotatoToolMod.LOGGER.debug("Profile fetch " + uuid + ": " + e.getMessage());
      }

      return null;
   }

   public CompletableFuture<JsonObject> getSkyblockProfiles(String uuid) {
      return CompletableFuture.supplyAsync(() -> this.getSkyblockProfilesBlocking(uuid));
   }

   public String resolveUsernameToUuid(String username) {
      if (username == null || (username = username.trim()).isEmpty()) {
         return null;
      }

      if (username.length() >= 2 && username.length() <= 16) {
         String encoded;
         try {
            encoded = URLEncoder.encode(username, "UTF-8");
         } catch (UnsupportedEncodingException e) {
            encoded = username;
         }

         String url = "https://api.mojang.com/users/profiles/minecraft/" + encoded;
         Request request = new okhttp3.Request.Builder().url(url).build();

         try {
            Response response = this.httpClient.newCall(request).execute();

            Object var15;
            label89: {
               String var8;
               label90: {
                  String body;
                  try {
                     if (response.isSuccessful() && response.body() != null) {
                        body = response.body().string();
                        if (body.isEmpty()) {
                           var15 = null;
                           break label89;
                        }

                        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                        var8 = obj.has("id") ? obj.get("id").getAsString() : null;
                        break label90;
                     }

                     body = null;
                  } catch (Throwable var11) {
                     if (response != null) {
                        try {
                           response.close();
                        } catch (Throwable var9) {
                           var11.addSuppressed(var9);
                        }
                     }

                     throw var11;
                  }

                  if (response != null) {
                     response.close();
                  }

                  return body;
               }

               if (response != null) {
                  response.close();
               }

               return var8;
            }

            if (response != null) {
               response.close();
            }

            return (String)var15;
         } catch (Exception e) {
            return null;
         }
      } else {
         return null;
      }
   }

   public CompletableFuture<HypixelApiClient.PlayerInfo> getPlayerInfo(String uuid) {
      return CompletableFuture.supplyAsync(() -> {
         String apiKey = this.getNextAvailableApiKey();
         if (apiKey == null) {
            return null;
         }

         String url = "https://api.hypixel.net/player?uuid=" + uuid;
         Request request = new okhttp3.Request.Builder().url(url).header("API-Key", apiKey).build();

         try {
            Response response = this.httpClient.newCall(request).execute();
            this.config.recordApiKeyUse(apiKey);

            HypixelApiClient.PlayerInfo var12;
            label114: {
               try {
                  if (response.isSuccessful() && response.body() != null) {
                     String body = response.body().string();
                     JsonObject result = JsonParser.parseString(body).getAsJsonObject();
                     if (result.has("player") && !result.get("player").isJsonNull()) {
                        JsonObject player = result.getAsJsonObject("player");
                        String rank = null;
                        if (player.has("newPackageRank")) {
                           rank = player.get("newPackageRank").getAsString();
                        } else if (player.has("monthlyPackageRank")) {
                           rank = player.get("monthlyPackageRank").getAsString();
                        } else if (player.has("packageRank")) {
                           rank = player.get("packageRank").getAsString();
                        } else if (player.has("rank")) {
                           rank = player.get("rank").getAsString();
                        }

                        Long lastLogin = null;
                        if (player.has("lastLogin") && !player.get("lastLogin").isJsonNull()) {
                           try {
                              lastLogin = player.get("lastLogin").getAsLong();
                           } catch (Exception var15) {
                           }
                        }

                        Long lastLogout = null;
                        if (player.has("lastLogout") && !player.get("lastLogout").isJsonNull()) {
                           try {
                              lastLogout = player.get("lastLogout").getAsLong();
                           } catch (Exception var14) {
                           }
                        }

                        var12 = new HypixelApiClient.PlayerInfo(rank, lastLogin, lastLogout);
                        break label114;
                     }
                  } else {
                     int code = response.code();
                     String body = response.body() != null ? response.body().string() : "";
                     if (code == 403) {
                        this.handleInvalidApiKey(body);
                     } else if (code == 429) {
                        this.handleRateLimit(apiKey, response, body);
                     }
                  }
               } catch (Throwable t$) {
                  if (response != null) {
                     try {
                        response.close();
                     } catch (Throwable x2) {
                        t$.addSuppressed(x2);
                     }
                  }

                  throw t$;
               }

               if (response != null) {
                  response.close();
               }

               return null;
            }

            if (response != null) {
               response.close();
            }

            return var12;
         } catch (Exception var17) {
            return null;
         }
      });
   }

   public CompletableFuture<String> getPlayerRank(String uuid) {
      return this.getPlayerInfo(uuid).thenApply(info -> info != null ? info.rank : null);
   }

   public CompletableFuture<Boolean> validateApiKey(String apiKey) {
      return CompletableFuture.supplyAsync(() -> {
         String url = "https://api.hypixel.net/key";
         Request request = new okhttp3.Request.Builder().url(url).header("API-Key", apiKey).build();

         try {
            Response response = this.httpClient.newCall(request).execute();

            Boolean var11;
            label61: {
               Boolean var7;
               try {
                  if (response.isSuccessful()) {
                     PotatoToolMod.LOGGER.info("API key validated successfully");
                     var11 = true;
                     break label61;
                  }

                  int code = response.code();
                  String body = response.body() != null ? response.body().string() : "";
                  if (code == 403) {
                     this.handleInvalidApiKey(body);
                  } else if (code == 429) {
                     this.handleRateLimit(apiKey, response, body);
                  }

                  var7 = false;
               } catch (Throwable t$) {
                  if (response != null) {
                     try {
                        response.close();
                     } catch (Throwable x2) {
                        t$.addSuppressed(x2);
                     }
                  }

                  throw t$;
               }

               if (response != null) {
                  response.close();
               }

               return var7;
            }

            if (response != null) {
               response.close();
            }

            return var11;
         } catch (IOException e) {
            PotatoToolMod.LOGGER.error("Failed to validate API key", e);
            return false;
         }
      });
   }

   public HypixelApiClient.SessionStatus getPlayerStatusBlocking(String uuid) {
      String apiKey = this.getNextAvailableApiKey();
      if (apiKey == null || uuid == null || uuid.isBlank()) {
         return null;
      }

      String clean = uuid.replace("-", "");
      String url = "https://api.hypixel.net/v2/status?uuid=" + clean;
      Request request = new okhttp3.Request.Builder().url(url).header("API-Key", apiKey).build();

      try (Response response = this.httpClient.newCall(request).execute()) {
         this.config.recordApiKeyUse(apiKey);
         if (!response.isSuccessful() || response.body() == null) {
            int code = response.code();
            String body = response.body() != null ? response.body().string() : "";
            if (code == 403) {
               this.handleInvalidApiKey(body);
            } else if (code == 429) {
               this.handleRateLimit(apiKey, response, body);
            }

            return null;
         }

         JsonObject root = JsonParser.parseString(response.body().string()).getAsJsonObject();
         if (!root.has("success") || !root.get("success").getAsBoolean()) {
            return new HypixelApiClient.SessionStatus(false, "", "");
         }

         if (!root.has("session") || root.get("session").isJsonNull()) {
            return new HypixelApiClient.SessionStatus(false, "", "");
         }

         JsonObject session = root.getAsJsonObject("session");
         boolean online = session.has("online") && session.get("online").getAsBoolean();
         String game = session.has("gameType") && !session.get("gameType").isJsonNull() ? session.get("gameType").getAsString() : "";
         String mode = session.has("mode") && !session.get("mode").isJsonNull() ? session.get("mode").getAsString() : "";
         return new HypixelApiClient.SessionStatus(online, game, mode);
      } catch (Exception e) {
         PotatoToolMod.LOGGER.debug("Status fetch failed: " + e.getMessage());
         return null;
      }
   }

   private void handleRateLimit(String keyUsed, Response response, String body) {
      long currentTime = System.currentTimeMillis();
      String retryAfterHeader = response.header("Retry-After");
      String ratelimitResetHeader = response.header("Ratelimit-Reset");
      int cooldownSeconds = 60;
      if (retryAfterHeader != null) {
         try {
            cooldownSeconds = Integer.parseInt(retryAfterHeader);
         } catch (NumberFormatException var14) {
         }
      }

      if (ratelimitResetHeader != null && cooldownSeconds == 60) {
         try {
            int resetIn = Integer.parseInt(ratelimitResetHeader);
            if (resetIn > 0 && resetIn <= 300) {
               cooldownSeconds = resetIn;
            }
         } catch (NumberFormatException var13) {
         }
      }

      if (!body.isEmpty()) {
         try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("global") && json.get("global").getAsBoolean()) {
               PotatoToolMod.LOGGER.error("§c§lGLOBAL RATE LIMIT! All API keys from your IP are blocked!");
            }

            if (json.has("cooldown")) {
               cooldownSeconds = json.get("cooldown").getAsInt();
            } else if (json.has("retry_after")) {
               cooldownSeconds = json.get("retry_after").getAsInt();
            }
         } catch (Exception var12) {
         }
      }

      int MAX_COOLDOWN_SECONDS = 300;
      if (cooldownSeconds > MAX_COOLDOWN_SECONDS) {
         cooldownSeconds = MAX_COOLDOWN_SECONDS;
      }

      this.keyThrottledUntilMs.put(keyUsed, currentTime + cooldownSeconds * 1000L);
      if (this.getNextAvailableApiKey() == null && this.rateLimitNotificationShown.compareAndSet(false, true)) {
         this.notifyPlayerRateLimit(cooldownSeconds);
         int delay = cooldownSeconds + 5;
         Thread t = new Thread(() -> {
            try {
               Thread.sleep(delay * 1000L);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }

            this.rateLimitNotificationShown.set(false);
         }, "PotatoTool-RateLimitReset");
         t.setDaemon(true);
         t.start();
      }

      PotatoToolMod.LOGGER.warn("§e§lKey rate limited (cooldown " + cooldownSeconds + "s); other keys may still work.");
   }

   private void notifyPlayerRateLimit(int cooldownSeconds) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.player != null) {
            int minutes = cooldownSeconds / 60;
            int seconds = cooldownSeconds % 60;
            String timeStr;
            if (minutes > 0) {
               timeStr = minutes + "m " + seconds + "s";
            } else {
               timeStr = seconds + "s";
            }

            client.execute(
               () -> {
                  if (client.player != null) {
                     client.player
                        .sendSystemMessage(
                           Component.literal(
                              "§c§l⚠ API THROTTLED §r§7 | §eWait §c" + timeStr + " §ebefore scanning again. §7Add more keys: §6/scannerkey add <key>"
                           ));
                     client.player.sendOverlayMessage(Component.literal("§c§lCooldown: " + timeStr));
                  }
               }
            );
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.error("Failed to send rate limit notification: " + e.getMessage());
      }
   }

   public boolean isRateLimited() {
      return this.getNextAvailableApiKey() == null && this.config.apiKeys != null && !this.config.apiKeys.isEmpty();
   }

   public int getRemainingCooldown() {
      if (this.getNextAvailableApiKey() != null) {
         return 0;
      }

      List<String> keys = this.config.apiKeys;
      if (keys != null && !keys.isEmpty()) {
         long now = System.currentTimeMillis();
         int minRemaining = Integer.MAX_VALUE;

         for (String key : keys) {
            Long until = this.keyThrottledUntilMs.get(key);
            if (until != null && until > now) {
               int sec = (int)((until - now) / 1000L);
               if (sec < minRemaining) {
                  minRemaining = sec;
               }
            }
         }

         int windowSec = (int)((this.config.millisUntilApiWindowOpens() + 999L) / 1000L);
         if (windowSec > 0 && (minRemaining == Integer.MAX_VALUE || windowSec < minRemaining)) {
            minRemaining = windowSec;
         }

         return minRemaining == Integer.MAX_VALUE ? 0 : minRemaining;
      } else {
         return 0;
      }
   }

   public void clearRateLimitCooldown() {
      this.keyThrottledUntilMs.clear();
      this.rateLimitNotificationShown.set(false);
   }

   private void handleInvalidApiKey(String body) {
      long currentTime = System.currentTimeMillis();
      long lastNotificationTime = this.lastInvalidKeyTime.get();
      if (currentTime - lastNotificationTime > 1000L) {
         PotatoToolMod.LOGGER.error("§c§lAPI KEY EXPIRED OR INVALID! Code 403");
      }

      String errorReason = "expired or invalid";
      if (!body.isEmpty()) {
         try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("cause")) {
               errorReason = json.get("cause").getAsString().toLowerCase();
            }
         } catch (Exception var8) {
         }
      }

      if (currentTime - lastNotificationTime > 30000L && this.invalidKeyNotificationShown.compareAndSet(false, true)) {
         this.lastInvalidKeyTime.set(currentTime);
         this.notifyPlayerInvalidKey(errorReason);
         Thread t = new Thread(() -> {
            try {
               Thread.sleep(30000L);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }

            this.invalidKeyNotificationShown.set(false);
         }, "PotatoTool-InvalidKeyReset");
         t.setDaemon(true);
         t.start();
      }
   }

   private void notifyPlayerInvalidKey(String reason) {
      try {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.player != null) {
            client.execute(
               () -> {
                  if (client.player != null) {
                     client.player
                        .sendSystemMessage(
                           Component.literal(
                              "§c§l✖ API KEY EXPIRED! §r§e\n§eYour Hypixel API key is "
                                 + reason
                                 + ".\n§eScanning has been paused.\n§7Get a new key: §6/api new §7(on Hypixel)\n§7Then add it: §6/scannerkey add <key>"
                           ));
                     client.player.sendOverlayMessage(Component.literal("§c§lAPI KEY EXPIRED - Get a new key with /api new"));
                  }
               }
            );
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.error("Failed to send invalid key notification: " + e.getMessage());
      }
   }

   public static final class PlayerInfo {
      public final String rank;
      public final Long lastLogin;
      public final Long lastLogout;

      public PlayerInfo(String rank, Long lastLogin, Long lastLogout) {
         this.rank = rank;
         this.lastLogin = lastLogin;
         this.lastLogout = lastLogout;
      }
   }

   public static final class SessionStatus {
      public final boolean online;
      public final String gameType;
      public final String mode;

      public SessionStatus(boolean online, String gameType, String mode) {
         this.online = online;
         this.gameType = gameType == null ? "" : gameType;
         this.mode = mode == null ? "" : mode;
      }

      public String gameLabel() {
         if (!this.online) {
            return "offline";
         }

         if (this.gameType.isBlank()) {
            return "Hypixel";
         }

         return this.mode.isBlank() ? this.gameType : this.gameType + " · " + this.mode;
      }
   }
}
