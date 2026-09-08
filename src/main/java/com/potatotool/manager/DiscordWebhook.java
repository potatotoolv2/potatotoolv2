package com.potatotool.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class DiscordWebhook {
   private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
   private static final OkHttpClient HTTP = new OkHttpClient.Builder()
      .connectTimeout(4L, TimeUnit.SECONDS)
      .readTimeout(6L, TimeUnit.SECONDS)
      .writeTimeout(6L, TimeUnit.SECONDS)
      .build();

   private DiscordWebhook() {
   }

   public static boolean isConfigured(ScannerConfig config) {
      return config != null && config.discordWebhookEnabled && isValidUrl(config.discordWebhookUrl);
   }

   public static boolean isValidUrl(String url) {
      if (url == null) {
         return false;
      }

      String u = url.trim();
      return u.startsWith("https://discord.com/api/webhooks/")
         || u.startsWith("https://discordapp.com/api/webhooks/")
         || u.startsWith("https://canary.discord.com/api/webhooks/")
         || u.startsWith("https://ptb.discord.com/api/webhooks/");
   }

   public static void notifyHit(ScannedPlayer player, ScannerConfig config) {
      if (!isConfigured(config) || player == null || !player.hasDisplayableItems() || !matchesFilters(player, config)) {
         return;
      }

      List<ScannedItem> hits = new ArrayList<>();
      for (ScannedItem item : player.getItems()) {
         if (item != null && item.isSpecial() && matchesItemFilter(item, config)) {
            hits.add(item);
         }
      }

      if (hits.isEmpty()) {
         return;
      }

      StringBuilder body = new StringBuilder();
      int shown = 0;
      for (ScannedItem item : hits) {
         if (shown >= 8) {
            body.append("\n+").append(hits.size() - shown).append(" more");
            break;
         }

         if (shown > 0) {
            body.append("\n");
         }

         String kind = item.getKindLabel();
         String name = item.getCleanItemName();
         body.append("**").append(kind == null || kind.isBlank() ? "Hit" : kind).append("**");
         if (!name.isEmpty() && (kind == null || !kind.contains(name))) {
            body.append(" — ").append(name);
         }

         if (item.getHexColor() != null && !item.getHexColor().isBlank()) {
            String hex = item.getHexColor().startsWith("#") ? item.getHexColor() : "#" + item.getHexColor();
            body.append("  `").append(hex).append("`");
         }

         String loc = item.getLocationWithProfile();
         if (loc != null && !loc.isBlank() && !"Unknown".equalsIgnoreCase(loc)) {
            body.append(" · ").append(loc);
         }

         shown++;
      }

      JsonObject embed = new JsonObject();
      embed.addProperty("title", player.getUsername());
      embed.addProperty("description", body.toString());
      embed.addProperty("color", colorFor(hits.get(0)));
      embed.addProperty("timestamp", Instant.now().toString());
      JsonObject thumb = new JsonObject();
      thumb.addProperty("url", "https://mc-heads.net/avatar/" + player.getUsername() + "/64");
      embed.add("thumbnail", thumb);
      JsonArray fields = new JsonArray();
      fields.add(field("Level", String.valueOf((int)player.getSkyblockLevel()), true));
      fields.add(field("Hits", String.valueOf(hits.size()), true));
      embed.add("fields", fields);
      JsonObject footer = new JsonObject();
      footer.addProperty("text", "PotatoToolV2 · Automatic");
      embed.add("footer", footer);
      send(config.discordWebhookUrl, embed);
   }

   public static void notifyOnline(String username, String game, ScannerConfig config) {
      if (!isConfigured(config) || !config.webhookNotifyOnline || username == null) {
         return;
      }

      JsonObject embed = new JsonObject();
      embed.addProperty("title", username);
      embed.addProperty("description", "**Online** · " + (game == null || game.isBlank() ? "Hypixel" : game));
      embed.addProperty("color", 5025616);
      embed.addProperty("timestamp", Instant.now().toString());
      JsonObject thumb = new JsonObject();
      thumb.addProperty("url", "https://mc-heads.net/avatar/" + username + "/64");
      embed.add("thumbnail", thumb);
      JsonObject footer = new JsonObject();
      footer.addProperty("text", "PotatoToolV2 · Notifier");
      embed.add("footer", footer);
      send(config.discordWebhookUrl, embed);
   }

   public static void sendTest(String url, java.util.function.Consumer<Boolean> done) {
      Thread t = new Thread(() -> {
         JsonObject embed = new JsonObject();
         embed.addProperty("title", "PotatoToolV2");
         embed.addProperty("description", "Webhook connected.");
         embed.addProperty("color", 4882644);
         embed.addProperty("timestamp", Instant.now().toString());
         JsonObject footer = new JsonObject();
         footer.addProperty("text", "Test ping");
         embed.add("footer", footer);
         boolean ok = send(url, embed);
         if (done != null) {
            done.accept(ok);
         }
      }, "PotatoToolV2-WebhookTest");
      t.setDaemon(true);
      t.start();
   }

   public static boolean matchesFilters(ScannedPlayer player, ScannerConfig config) {
      if (player == null || config == null) {
         return false;
      }

      for (ScannedItem item : player.getItems()) {
         if (matchesItemFilter(item, config)) {
            return true;
         }
      }

      return false;
   }

   private static boolean matchesItemFilter(ScannedItem item, ScannerConfig config) {
      if (item == null || item.getCategory() == null || config == null) {
         return false;
      }

      return switch (item.getCategory()) {
         case FAIRY -> config.webhookNotifyFairy;
         case OG_FAIRY -> config.webhookNotifyOgFairy;
         case CRYSTAL -> config.webhookNotifyCrystal;
         case EXOTIC -> config.webhookNotifyExotic;
         case SEYMOUR_T1, SEYMOUR_T2, SEYMOUR_T3 -> config.webhookNotifySeymour;
         case COSMETIC_SKIN -> config.webhookNotifySkins;
         case BLEACHED -> config.webhookNotifyBleached;
         case GLITCHED -> config.webhookNotifyGlitched;
         default -> false;
      };
   }

   private static int colorFor(ScannedItem item) {
      if (item == null || item.getCategory() == null) {
         return 4882644;
      }

      return switch (item.getCategory()) {
         case SEYMOUR_T3 -> 0xE74C3C;
         case SEYMOUR_T2 -> 0xF1C40F;
         case SEYMOUR_T1 -> 0x2ECC71;
         case FAIRY -> 0xE91E8C;
         case OG_FAIRY -> 0x9B59B6;
         case CRYSTAL -> 0x3498DB;
         case EXOTIC -> 0x1ABC9C;
         case COSMETIC_SKIN -> 0xE67E22;
         case GLITCHED -> 0x8E44AD;
         default -> 4882644;
      };
   }

   private static JsonObject field(String name, String value, boolean inline) {
      JsonObject f = new JsonObject();
      f.addProperty("name", name);
      f.addProperty("value", value == null || value.isBlank() ? "—" : value);
      f.addProperty("inline", inline);
      return f;
   }

   private static boolean send(String url, JsonObject embed) {
      if (!isValidUrl(url) || embed == null) {
         return false;
      }

      JsonArray embeds = new JsonArray();
      embeds.add(embed);
      JsonObject root = new JsonObject();
      root.addProperty("username", "PotatoToolV2");
      root.add("embeds", embeds);
      RequestBody body = RequestBody.create(root.toString(), JSON);
      Request request = new Request.Builder().url(url.trim()).header("User-Agent", "PotatoToolV2").post(body).build();

      try (Response response = HTTP.newCall(request).execute()) {
         boolean ok = response.isSuccessful() || response.code() == 204;
         if (!ok) {
            PotatoToolMod.LOGGER.warn("Discord webhook failed: HTTP " + response.code());
         }

         return ok;
      } catch (Exception e) {
         PotatoToolMod.LOGGER.warn("Discord webhook error: " + e.getMessage());
         return false;
      }
   }
}
