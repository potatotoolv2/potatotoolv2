package com.potatotool.manager;

import com.potatotool.PotatoToolMod;
import com.potatotool.api.HypixelApiClient;
import com.potatotool.config.ScannerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class OnlineNotifier {
   private static int tickCounter;
   private static int nextNameIndex;
   private static volatile boolean checking;
   private static final Map<String, Boolean> lastOnline = new ConcurrentHashMap<>();
   private static final Map<String, String> uuidByName = new ConcurrentHashMap<>();
   private static final Map<String, String> lastStatusLine = new ConcurrentHashMap<>();

   private OnlineNotifier() {
   }

   public static String statusOf(String username) {
      if (username == null) {
         return "—";
      }

      String line = lastStatusLine.get(username.toLowerCase(Locale.ROOT));
      return line == null ? "—" : line;
   }

   public static void tick(Minecraft client) {
      if (client == null) {
         return;
      }

      PotatoToolMod mod = PotatoToolMod.getInstance();
      if (mod == null || mod.getConfig() == null || mod.getApiClient() == null) {
         return;
      }

      ScannerConfig config = mod.getConfig();
      if (!config.notifierEnabled || config.notifierNames == null || config.notifierNames.isEmpty()) {
         return;
      }

      int intervalSec = Math.max(15, Math.min(600, config.notifierIntervalSeconds));
      int intervalTicks = intervalSec * 20;
      tickCounter++;
      if (tickCounter < Math.max(40, intervalTicks / Math.max(1, config.notifierNames.size()))) {
         return;
      }

      tickCounter = 0;
      if (checking) {
         return;
      }

      List<String> names = new ArrayList<>();
      for (String raw : config.notifierNames) {
         if (raw != null && !raw.isBlank()) {
            names.add(raw.trim());
         }
      }

      if (names.isEmpty()) {
         return;
      }

      if (nextNameIndex >= names.size()) {
         nextNameIndex = 0;
      }

      String name = names.get(nextNameIndex);
      nextNameIndex = (nextNameIndex + 1) % names.size();
      checking = true;
      HypixelApiClient api = mod.getApiClient();
      Thread t = new Thread(() -> {
         try {
            checkOne(client, api, config, name);
         } finally {
            checking = false;
         }
      }, "PotatoToolV2-Notifier");
      t.setDaemon(true);
      t.start();
   }

   public static void checkNow(Minecraft client) {
      PotatoToolMod mod = PotatoToolMod.getInstance();
      if (mod == null || mod.getConfig() == null || client == null) {
         return;
      }

      ScannerConfig config = mod.getConfig();
      if (config.notifierNames == null || config.notifierNames.isEmpty()) {
         client.execute(() -> {
            if (client.player != null) {
               client.player.sendSystemMessage(Component.literal("§c[PotatoToolV2] Add a username first."));
            }
         });
         return;
      }

      if (mod.getApiClient() == null) {
         return;
      }

      List<String> names = new ArrayList<>();
      for (String raw : config.notifierNames) {
         if (raw != null && !raw.isBlank()) {
            names.add(raw.trim());
         }
      }

      client.execute(() -> {
         if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("§b[PotatoToolV2] §7Checking " + names.size() + " player(s)…"));
         }
      });
      HypixelApiClient api = mod.getApiClient();
      Thread t = new Thread(() -> {
         for (String name : names) {
            checkOne(client, api, config, name);
         }
      }, "PotatoToolV2-NotifierNow");
      t.setDaemon(true);
      t.start();
   }

   private static void checkOne(Minecraft client, HypixelApiClient api, ScannerConfig config, String username) {
      String key = username.toLowerCase(Locale.ROOT);
      String uuid = uuidByName.get(key);
      if (uuid == null) {
         uuid = api.resolveUsernameToUuid(username);
         if (uuid != null) {
            uuidByName.put(key, uuid);
         }
      }

      if (uuid == null) {
         lastStatusLine.put(key, "unknown");
         return;
      }

      if (config.getRandomApiKey() == null) {
         lastStatusLine.put(key, "no API key");
         return;
      }

      HypixelApiClient.SessionStatus status = api.getPlayerStatusBlocking(uuid);
      if (status == null) {
         lastStatusLine.put(key, "error");
         return;
      }

      boolean online = status.online;
      String where = status.gameLabel();
      lastStatusLine.put(key, online ? "online · " + where : "offline");
      Boolean was = lastOnline.put(key, online);
      if (online && (was == null || !was)) {
         String msg = "§b[PotatoToolV2] §f" + username + " §ais online §7(" + where + ")";
         client.execute(() -> {
            if (client.player != null) {
               client.player.sendSystemMessage(Component.literal(msg));
            }
         });
         DiscordWebhook.notifyOnline(username, where, config);
      }
   }
}
