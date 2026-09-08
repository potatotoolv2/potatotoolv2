package com.potatotool.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potatotool.PotatoToolMod;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient.Builder;

public class CosmeticSkinValuesLoader {
   private static final String COFLNET_API = "https://sky.coflnet.com/api";
   private static final String COFLNET_SOLD = "https://sky.coflnet.com/api/auctions/tag/";
   private static final String COFLNET_ITEMS = "https://sky.coflnet.com/api/items";
   private static final long COFLNET_RATE_LIMIT_MS = 2100L;
   private static final long STATS_REQUEST_COOLDOWN_MS = 20000L;
   private static final int SOLD_PAGE_SIZE = 1000;
   private static final long DAY_MS = TimeUnit.DAYS.toMillis(1L);
   private static final long WEEK_MS = TimeUnit.DAYS.toMillis(7L);
   private static final Map<String, Double> valueMillionsByItemId = new ConcurrentHashMap<>();
   private static final Map<String, CosmeticSkinValuesLoader.SkinMarketStats> statsByItemId = new ConcurrentHashMap<>();
   private static final Map<String, Long> statsRequestAtByTag = new ConcurrentHashMap<>();
   private static final Set<String> statsRequestsInFlight = ConcurrentHashMap.newKeySet();
   private static final Set<String> knownSkinItemIds = ConcurrentHashMap.newKeySet();
   private static final Set<String> weeklyValueFromCofl = ConcurrentHashMap.newKeySet();
   private static final Set<String> FIRE_SALE_ARMOR_FALLBACK_TAGS = Set.of(
      "BLAZING_CRIMSON",
      "CORRUPT_WITHER_GOGGLES",
      "GREAT_SHARK_MAGMA_LORD",
      "MAGMA_LORD_RAGNAROK",
      "METEOR_MAGMA_LORD",
      "FERMENTO_ULTIMATE",
      "GEMSTONE_DIVAN",
      "GLACIAL_DIVAN",
      "PALADIN_MINERAL",
      "DUMPSTER_DIVER",
      "WITHER_AWAKENED",
      "WITHER_GOGGLES_CELESTIAL",
      "WITHER_GOGGLES_CYBERPUNK",
      "STORM_CELESTIAL",
      "NECRON_CELESTIAL",
      "MAXOR_CELESTIAL",
      "GOLDOR_CELESTIAL",
      "SORROW_SAMURAI",
      "REAPER_SPECTRE",
      "THUNDER_BIKER_PINK",
      "THUNDER_BIKER_AQUA",
      "THUNDER_BIKER_BLACK",
      "THUNDER_BIKER_AZURE",
      "THUNDER_BIKER_TEAL",
      "SHARK_SCALE_HOODIE_SEAL",
      "SHARK_SCALE_HOODIE_PENGUIN",
      "SHARK_SCALE_HOODIE_FOX",
      "EINARY_RED_HOODIE",
      "PRECURSOR_EYE_OCULUS",
      "BIOHAZARD_SPACE_YELLOW",
      "BIOHAZARD_SPACE_TEAL",
      "BIOHAZARD_SPACE_RED",
      "BIOHAZARD_SPACE_BLACK_ICE",
      "BIOHAZARD_SPACE_SNOWFLAKE"
   );
   private static final AtomicBoolean loaded = new AtomicBoolean(false);
   private static final AtomicBoolean refreshStarted = new AtomicBoolean(false);
   private static final OkHttpClient httpClient = new Builder().connectTimeout(10L, TimeUnit.SECONDS).readTimeout(20L, TimeUnit.SECONDS).build();
   private static final ExecutorService statsExecutor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "CoflnetSkinStats");
      t.setDaemon(true);
      return t;
   });

   public static void load() {
      if (!loaded.get()) {
         synchronized (CosmeticSkinValuesLoader.class) {
            if (loaded.get()) {
               return;
            }

            try {
               knownSkinItemIds.addAll(FIRE_SALE_ARMOR_FALLBACK_TAGS);
               InputStream stream = CosmeticSkinValuesLoader.class.getResourceAsStream("/CosmeticSkinValues.json");
               if (stream != null) {
                  JsonObject json = JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
                  if (json.has("values")) {
                     JsonObject values = json.getAsJsonObject("values");

                     for (String key : values.keySet()) {
                        String normalized = normalizeTag(key);
                        if (normalized != null) {
                           knownSkinItemIds.add(normalized);
                           JsonElement el = values.get(key);
                           if (el != null && el.isJsonPrimitive()) {
                              try {
                                 double millions = el.getAsDouble();
                                 if (millions > 0.0) {
                                    valueMillionsByItemId.put(normalized, millions);
                                 }
                              } catch (Exception var11) {
                              }
                           }
                        }
                     }
                  }
               }

               PotatoToolMod.LOGGER
                  .info(
                     "Loaded "
                        + valueMillionsByItemId.size()
                        + " cosmetic skin values (fallback), "
                        + knownSkinItemIds.size()
                        + " known skin tags; fetching last-3 SkyCofl sales in background"
                  );
               loaded.set(true);
            } catch (Exception e) {
               PotatoToolMod.LOGGER.error("Failed to load cosmetic skin values", e);
               loaded.set(true);
            }
         }

         startCoflnetRefresh();
      }
   }

   private static void startCoflnetRefresh() {
      if (refreshStarted.compareAndSet(false, true)) {
         Thread t = new Thread(() -> {
            try {
               Set<String> inFallback = new HashSet<>(valueMillionsByItemId.keySet());
               Set<String> tagsToFetch = new HashSet<>(inFallback);
               CosmeticSkinValuesLoader.SkinTagSets coflnetSkinTags = fetchCoflnetSkinTags();
               if (coflnetSkinTags != null) {
                  knownSkinItemIds.addAll(coflnetSkinTags.knownTags);
                  tagsToFetch.addAll(coflnetSkinTags.priceTags);
               }

               if (tagsToFetch.isEmpty()) {
                  return;
               }

               List<String> ordered = new ArrayList<>(tagsToFetch);
               ordered.sort((a, b) -> {
                  boolean aIn = inFallback.contains(a);
                  boolean bIn = inFallback.contains(b);
                  if (aIn != bIn) {
                     return aIn ? -1 : 1;
                  } else {
                     return 0;
                  }
               });
               int updated = 0;

               for (String tag : ordered) {
                  try {
                     CosmeticSkinValuesLoader.SkinMarketStats stats = fetchSkinMarketStats(tag);
                     if (stats != null) {
                        statsByItemId.put(tag, stats);
                        if (stats.weeklyAvgMillions != null && stats.weeklyAvgMillions > 0.0) {
                           valueMillionsByItemId.put(tag, stats.weeklyAvgMillions);
                           weeklyValueFromCofl.add(tag);
                           updated++;
                        }
                     }
                  } catch (Exception e) {
                     PotatoToolMod.LOGGER.debug("Coflnet fetch " + tag + ": " + e.getMessage());
                  }

                  Thread.sleep(2100L);
               }

               PotatoToolMod.LOGGER.info("SkyCofl skin refresh: known tags=" + knownSkinItemIds.size() + ", updated values=" + updated);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
         }, "CoflnetSkinPrices");
         t.setDaemon(true);
         t.start();
      }
   }

   private static CosmeticSkinValuesLoader.SkinTagSets fetchCoflnetSkinTags() {
      Request request = new okhttp3.Request.Builder().url("https://sky.coflnet.com/api/items").build();

      try {
         Response response = httpClient.newCall(request).execute();

         CosmeticSkinValuesLoader.SkinTagSets var14;
         label91: {
            Object body;
            try {
               if (response.isSuccessful() && response.body() != null) {
                  String bodyx = response.body().string();
                  JsonArray arr = JsonParser.parseString(bodyx).getAsJsonArray();
                  CosmeticSkinValuesLoader.SkinTagSets out = new CosmeticSkinValuesLoader.SkinTagSets();

                  for (JsonElement el : arr) {
                     if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        String tag = obj.has("tag") ? normalizeTag(obj.get("tag").getAsString()) : null;
                        if (tag != null && !tag.isEmpty()) {
                           String nameUpper = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString().toUpperCase(Locale.ROOT) : "";
                           if (isClassicSkinTagForPricing(tag)) {
                              out.knownTags.add(tag);
                              out.priceTags.add(tag);
                           } else if (looksLikeFireSaleArmorSkin(tag, nameUpper)) {
                              out.knownTags.add(tag);
                           }
                        }
                     }
                  }

                  var14 = out;
                  break label91;
               }

               body = null;
            } catch (Throwable var11) {
               if (response != null) {
                  try {
                     response.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (response != null) {
               response.close();
            }

            return (CosmeticSkinValuesLoader.SkinTagSets)body;
         }

         if (response != null) {
            response.close();
         }

         return var14;
      } catch (Exception e) {
         PotatoToolMod.LOGGER.debug("Coflnet /api/items: " + e.getMessage());
         return null;
      }
   }

   private static CosmeticSkinValuesLoader.SkinMarketStats fetchSkinMarketStats(String itemTag) {
      List<SalePoint> unapplied = fetchRecentSales(itemTag, false);
      List<SalePoint> applied = fetchRecentSales(itemTag, true);
      unapplied.sort((a, b) -> Long.compare(b.atMs, a.atMs));
      applied.sort((a, b) -> Long.compare(b.atMs, a.atMs));
      Double unappliedAvg = averageLastThree(unapplied);
      Double appliedAvg = averageLastThree(applied);
      Double primary = unappliedAvg != null ? unappliedAvg : appliedAvg;
      List<SalePoint> newest = !unapplied.isEmpty() ? unapplied : applied;
      SalePoint last = newest.isEmpty() ? null : newest.get(0);
      Double dailyM = averageLastThree(newest.stream().filter(s -> s.atMs >= System.currentTimeMillis() - DAY_MS).toList());
      if (primary == null && last != null) {
         primary = last.coins / 1000000.0;
      }

      return new CosmeticSkinValuesLoader.SkinMarketStats(
         primary, dailyM, last != null ? last.coins : null, last != null ? last.atMs : null, unappliedAvg, appliedAvg
      );
   }

   private static List<SalePoint> fetchRecentSales(String itemTag, boolean appliedSkin) {
      if (!appliedSkin) {
         return fetchSoldUrl(itemTag, "https://sky.coflnet.com/api/auctions/tag/" + encodeTag(itemTag) + "/sold?pageSize=12");
      }

      String encoded = encodeTag(itemTag);
      String[] urls = new String[]{
         "https://sky.coflnet.com/api/auctions/tag/" + encoded + "/sold?pageSize=12&Skin=" + encoded,
         "https://sky.coflnet.com/api/auctions/tag/" + encoded + "/sold?pageSize=12&PetSkin=" + encoded
      };
      for (String url : urls) {
         List<SalePoint> sales = fetchSoldUrl(itemTag, url);
         if (!sales.isEmpty()) {
            return sales;
         }
      }

      return new ArrayList<>();
   }

   private static String encodeTag(String itemTag) {
      try {
         return URLEncoder.encode(itemTag, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         return itemTag;
      }
   }

   private static List<SalePoint> fetchSoldUrl(String itemTag, String url) {
      Request request = new okhttp3.Request.Builder().url(url).header("User-Agent", "PotatoToolV2").build();
      List<SalePoint> sales = new ArrayList<>();

      try (Response response = httpClient.newCall(request).execute()) {
         if (response.code() == 429 || response.code() == 404 || !response.isSuccessful() || response.body() == null) {
            return sales;
         }

         JsonElement parsed = JsonParser.parseString(response.body().string());
         if (!parsed.isJsonArray()) {
            return sales;
         }

         java.util.HashSet<String> seen = new java.util.HashSet<>();
         for (JsonElement el : parsed.getAsJsonArray()) {
            if (!el.isJsonObject()) {
               continue;
            }

            JsonObject ob = el.getAsJsonObject();
            long amount = 0L;
            if (ob.has("highestBidAmount") && ob.get("highestBidAmount").isJsonPrimitive()) {
               amount = ob.get("highestBidAmount").getAsLong();
            } else if (ob.has("startingBid") && ob.get("startingBid").isJsonPrimitive()) {
               amount = ob.get("startingBid").getAsLong();
            }

            if (amount <= 0L) {
               continue;
            }

            long soldAt = parseSaleTimeMs(ob);
            String uuid = ob.has("uuid") && ob.get("uuid").isJsonPrimitive() ? ob.get("uuid").getAsString() : amount + ":" + soldAt;
            if (!seen.add(uuid)) {
               continue;
            }

            sales.add(new SalePoint(amount, soldAt));
         }
      } catch (Exception e) {
         PotatoToolMod.LOGGER.debug("SkyCofl sold fetch " + itemTag + ": " + e.getMessage());
      }

      sales.sort((a, b) -> Long.compare(b.atMs, a.atMs));
      return sales;
   }

   private static long parseSaleTimeMs(JsonObject ob) {
      for (String key : new String[]{"end", "start"}) {
         if (!ob.has(key) || !ob.get(key).isJsonPrimitive()) {
            continue;
         }

         String raw = ob.get(key).getAsString();
         if (raw == null || raw.isEmpty()) {
            continue;
         }

         try {
            String toParse = raw;
            if (!toParse.endsWith("Z") && !toParse.contains("+") && toParse.length() >= 19) {
               toParse = toParse.substring(0, 19) + "Z";
            }

            return Instant.parse(toParse).toEpochMilli();
         } catch (Exception ignored) {
         }
      }

      return 0L;
   }

   private static Double averageLastThree(List<SalePoint> newestFirst) {
      if (newestFirst == null || newestFirst.isEmpty()) {
         return null;
      }

      List<Long> prices = new ArrayList<>();
      for (SalePoint sale : newestFirst) {
         if (sale.coins > 0L) {
            prices.add(sale.coins);
         }

         if (prices.size() == 3) {
            break;
         }
      }

      if (prices.isEmpty()) {
         return null;
      }

      if (prices.size() == 3) {
         int outlier = -1;
         for (int i = 0; i < 3; i++) {
            double otherAvg = (prices.get((i + 1) % 3) + prices.get((i + 2) % 3)) / 2.0;
            if (otherAvg > 0.0 && prices.get(i) >= otherAvg * 1.70) {
               outlier = i;
               break;
            }
         }

         if (outlier >= 0) {
            prices.remove(outlier);
         }
      }

      double sum = 0.0;
      for (long price : prices) {
         sum += price;
      }

      return sum / prices.size() / 1000000.0;
   }

   private static final class SalePoint {
      final long coins;
      final long atMs;

      SalePoint(long coins, long atMs) {
         this.coins = coins;
         this.atMs = atMs;
      }
   }

   public static Double getValueMillions(String itemId) {
      return getValueMillions(itemId, false);
   }

   public static Double getValueMillions(String itemId, boolean applied) {
      if (!loaded.get()) {
         load();
      }

      String normalized = normalizeTag(itemId);
      if (normalized == null) {
         return null;
      }

      CosmeticSkinValuesLoader.SkinMarketStats stats = statsByItemId.get(normalized);
      if (stats != null) {
         if (applied && stats.appliedAvgMillions != null && stats.appliedAvgMillions > 0.0) {
            return stats.appliedAvgMillions;
         }

         if (!applied && stats.unappliedAvgMillions != null && stats.unappliedAvgMillions > 0.0) {
            return stats.unappliedAvgMillions;
         }

         if (stats.weeklyAvgMillions != null && stats.weeklyAvgMillions > 0.0) {
            return stats.weeklyAvgMillions;
         }
      }

      return valueMillionsByItemId.get(normalized);
   }

   public static CosmeticSkinValuesLoader.SkinMarketStats getSkinMarketStats(String itemId) {
      if (!loaded.get()) {
         load();
      }

      String normalized = normalizeTag(itemId);
      return normalized == null ? null : statsByItemId.get(normalized);
   }

   public static CosmeticSkinValuesLoader.SkinMarketStats getOrRequestSkinMarketStats(String itemId) {
      if (!loaded.get()) {
         load();
      }

      String normalized = normalizeTag(itemId);
      if (normalized == null) {
         return null;
      }

      CosmeticSkinValuesLoader.SkinMarketStats cached = statsByItemId.get(normalized);
      long now = System.currentTimeMillis();
      Long lastReq = statsRequestAtByTag.get(normalized);
      boolean stale = lastReq == null || now - lastReq > 20000L;
      if (stale && statsRequestsInFlight.add(normalized)) {
         statsRequestAtByTag.put(normalized, now);
         statsExecutor.submit(() -> {
            try {
               CosmeticSkinValuesLoader.SkinMarketStats stats = fetchSkinMarketStats(normalized);
               if (stats != null) {
                  statsByItemId.put(normalized, stats);
                  if (stats.weeklyAvgMillions != null && stats.weeklyAvgMillions > 0.0) {
                     valueMillionsByItemId.put(normalized, stats.weeklyAvgMillions);
                     weeklyValueFromCofl.add(normalized);
                  }
               }
            } catch (Exception var5x) {
            } finally {
               statsRequestsInFlight.remove(normalized);
            }
         });
      }

      return cached;
   }

   public static boolean isStatsRequestInFlight(String itemId) {
      if (!loaded.get()) {
         load();
      }

      String normalized = normalizeTag(itemId);
      return normalized != null && statsRequestsInFlight.contains(normalized);
   }

   public static long getLastStatsRequestAtMs(String itemId) {
      if (!loaded.get()) {
         load();
      }

      String normalized = normalizeTag(itemId);
      if (normalized == null) {
         return 0L;
      }

      Long at = statsRequestAtByTag.get(normalized);
      return at == null ? 0L : at;
   }

   public static boolean hasValue(String itemId) {
      return getValueMillions(itemId) != null;
   }

   public static boolean hasWeeklyValueFromCofl(String itemId) {
      if (!loaded.get()) {
         load();
      }

      String normalized = normalizeTag(itemId);
      return normalized != null && weeklyValueFromCofl.contains(normalized);
   }

   public static boolean isKnownSkinItemId(String itemId) {
      if (!loaded.get()) {
         load();
      }

      String normalized = normalizeTag(itemId);
      return normalized != null && knownSkinItemIds.contains(normalized);
   }

   public static Set<String> getKnownSkinItemIds() {
      if (!loaded.get()) {
         load();
      }

      return new HashSet<>(knownSkinItemIds);
   }

   private static boolean isClassicSkinTagForPricing(String upperTag) {
      return upperTag.startsWith("PET_SKIN_") || upperTag.contains("_SKIN_") || upperTag.startsWith("PARTY_HAT");
   }

   private static boolean looksLikeFireSaleArmorSkin(String upperTag, String upperName) {
      if (upperTag == null || upperTag.isEmpty()) {
         return false;
      } else if (upperTag.startsWith("PET_SKIN_")) {
         return false;
      } else if (FIRE_SALE_ARMOR_FALLBACK_TAGS.contains(upperTag)) {
         return true;
      } else if (upperName == null || !upperName.contains("SKIN")) {
         return false;
      } else if (upperName.contains("MINION SKIN")) {
         return false;
      } else if (upperName.contains("BARN SKIN")) {
         return false;
      } else if (upperName.contains("BACKPACK SKIN")) {
         return false;
      } else {
         return upperName.contains("POWER ORB SKIN") ? false : !upperName.contains("GREENHOUSE SKIN");
      }
   }

   private static String normalizeTag(String raw) {
      if (raw == null) {
         return null;
      }

      String t = raw.trim().toUpperCase(Locale.ROOT);
      return t.isEmpty() ? null : t;
   }

   public static final class SkinMarketStats {
      public final Double weeklyAvgMillions;
      public final Double dailyAvgMillions;
      public final Long lastSaleCoins;
      public final Long lastSaleAtMs;
      public final Double unappliedAvgMillions;
      public final Double appliedAvgMillions;

      public SkinMarketStats(Double weeklyAvgMillions, Double dailyAvgMillions, Long lastSaleCoins, Long lastSaleAtMs) {
         this(weeklyAvgMillions, dailyAvgMillions, lastSaleCoins, lastSaleAtMs, weeklyAvgMillions, null);
      }

      public SkinMarketStats(
         Double weeklyAvgMillions,
         Double dailyAvgMillions,
         Long lastSaleCoins,
         Long lastSaleAtMs,
         Double unappliedAvgMillions,
         Double appliedAvgMillions
      ) {
         this.weeklyAvgMillions = weeklyAvgMillions;
         this.dailyAvgMillions = dailyAvgMillions;
         this.lastSaleCoins = lastSaleCoins;
         this.lastSaleAtMs = lastSaleAtMs;
         this.unappliedAvgMillions = unappliedAvgMillions;
         this.appliedAvgMillions = appliedAvgMillions;
      }
   }

   private static final class SkinTagSets {
      final Set<String> knownTags = new LinkedHashSet<>();
      final Set<String> priceTags = new LinkedHashSet<>();
   }
}
