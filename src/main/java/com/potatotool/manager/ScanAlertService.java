package com.potatotool.manager;

import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ScanAlertService {
   private static final Map<String, Long> lastWebhookAt = new ConcurrentHashMap<>();

   private ScanAlertService() {
   }

   public static void onPlayerHit(ScannedPlayer player) {
      onPlayerHit(player, false);
   }

   public static void onPlayerHit(ScannedPlayer player, boolean automaticScan) {
      if (player == null || !player.hasDisplayableItems()) {
         return;
      }

      HitCache.record(player);
      if (!automaticScan) {
         return;
      }
      ScannerConfig config = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
      if (config == null || !DiscordWebhook.isConfigured(config) || !DiscordWebhook.matchesFilters(player, config)) {
         return;
      }

      String key = player.getUuid() != null && !player.getUuid().isBlank()
         ? player.getUuid().replace("-", "").toLowerCase()
         : (player.getUsername() == null ? "" : player.getUsername().toLowerCase());
      long now = System.currentTimeMillis();
      Long prev = lastWebhookAt.get(key);
      if (prev != null && now - prev < 45000L) {
         return;
      }

      lastWebhookAt.put(key, now);
      Thread t = new Thread(() -> DiscordWebhook.notifyHit(player, config), "PotatoToolV2-Webhook");
      t.setDaemon(true);
      t.start();
   }
}
