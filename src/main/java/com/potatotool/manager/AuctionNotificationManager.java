package com.potatotool.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import com.potatotool.util.ColorAnalyzer;
import com.potatotool.util.CosmeticSkinValuesLoader;
import com.potatotool.util.DefaultArmorColorsLoader;
import com.potatotool.util.SeymourAnalyzer;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent.CopyToClipboard;
import net.minecraft.network.chat.ClickEvent.OpenUrl;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient.Builder;

public class AuctionNotificationManager {
   private static final String COFL_API = "https://sky.coflnet.com/api";
   private static final String HYPIXEL_AH_API = "https://api.hypixel.net/v2/skyblock/auctions";
   private static final String COFL_ITEM_BIN_API = "https://sky.coflnet.com/api/item/price/%s/bin";
   private static final String COFL_PRICE_NBT_API = "https://sky.coflnet.com/api/price/nbt";
   private static final int FETCH_CONCURRENCY = 32;
   private static final double SKIN_SNIPE_RATIO = 0.8;
   private static final int HYPIXEL_MAX_PAGES_PER_POLL = 60;
   private static final int HYPIXEL_HEAD_PAGE_BIAS = 5;
   private static final int HYPIXEL_MIN_PAGES_PASSIVE = 18;
   private static final int HYPIXEL_MIN_PAGES_HOT = 30;
   private static final int HYPIXEL_MIN_PAGES_FORCED = 24;
   private static final int HYPIXEL_MIN_PAGES_FAST_INTERVAL = 40;
   private static final int HYPIXEL_FULL_SWEEP_PAGE_CAP = 260;
   private static final long ADAPTIVE_FAST_WINDOW_MS = 45000L;
   private static final long ALERT_DEDUPE_CACHE_TTL_MS = TimeUnit.HOURS.toMillis(6L);
   private static final long PRICE_CACHE_OK_TTL_MS = TimeUnit.MINUTES.toMillis(5L);
   private static final long PRICE_CACHE_MISS_TTL_MS = TimeUnit.MINUTES.toMillis(2L);
   private static final Pattern ENCHANT_ENTRY_PATTERN = Pattern.compile("([a-z0-9_]+)\\s*:\\s*(\\d+)", 2);
   private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
   private static final Map<String, String> TAG_ALIASES = createTagAliases();
   private static final OkHttpClient HTTP = new Builder().connectTimeout(4L, TimeUnit.SECONDS).readTimeout(6L, TimeUnit.SECONDS).build();
   private final ScannerConfig config;
   private final ExecutorService pollExecutor = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "PotatoToolV2-AH-Notifier");
      t.setDaemon(true);
      return t;
   });
   private final ExecutorService fetchExecutor = Executors.newFixedThreadPool(32, r -> {
      Thread t = new Thread(r, "PotatoToolV2-AH-TagFetch");
      t.setDaemon(true);
      return t;
   });
   private final AtomicBoolean pollInFlight = new AtomicBoolean(false);
   private final ConcurrentLinkedQueue<AuctionNotificationManager.PendingAlert> pendingAlerts = new ConcurrentLinkedQueue<>();
   private final ConcurrentLinkedQueue<AuctionNotificationManager.PendingAutoBuy> pendingAutoBuys = new ConcurrentLinkedQueue<>();
   private final Map<String, Long> seenAuctionIds = new ConcurrentHashMap<>();
   private final Map<String, Long> queuedAutoBuyIds = new ConcurrentHashMap<>();
   private final Map<String, Long> recentAlertFingerprints = new ConcurrentHashMap<>();
   private final Map<String, AuctionNotificationManager.PriceCacheEntry> coflPriceCache = new ConcurrentHashMap<>();
   private volatile String statusLine = "AH notifier idle";
   private volatile long cooldownUntilMs = 0L;
   private volatile int tagCursor = 0;
   private volatile int hypixelPageCursor = 0;
   private volatile List<String> forcedTagsForNextPoll = null;
   private volatile List<String> hotTags = null;
   private volatile long hotTagsUntilMs = 0L;
   private volatile long adaptiveFastUntilMs = 0L;
   private int pollTickCounter = 0;
   private long lastSeenCleanupMs = 0L;
   private static final long HOT_TAGS_WINDOW_MS = 90000L;
   private static final int HOT_TAGS_INTERVAL_TICKS = 5;
   private static final long PASSIVE_MIN_FRESH_WINDOW_MS = 25000L;
   private static final long PASSIVE_MAX_FRESH_WINDOW_MS = 1800000L;
   private static final long AUTO_BUY_TIMEOUT_MS = 12000L;
   private static final long AUTO_BUY_ACTION_COOLDOWN_MS = 180L;
   private static final long AUTO_BUY_VIEW_RESEND_MS = 1200L;
   private volatile long sessionPolls = 0L;
   private volatile long sessionSeen = 0L;
   private volatile long sessionAlerts = 0L;
   private volatile long sessionSnipes = 0L;
   private volatile long sessionAutoQueued = 0L;
   private volatile String autoBuyUuid = null;
   private volatile long autoBuyStartedMs = 0L;
   private volatile long autoBuyLastActionMs = 0L;
   private volatile long nextAutoBuyAllowedMs = 0L;
   private volatile int autoBuyStage = 0;
   private volatile int autoBuyViewResends = 0;

   public AuctionNotificationManager(ScannerConfig config) {
      this.config = config;
   }

   public String getStatusLine() {
      return this.statusLine;
   }

   public void requestImmediatePoll() {
      List<String> manualTargets = this.getConfiguredManualWatchTags();
      if (!manualTargets.isEmpty()) {
         this.forcedTagsForNextPoll = manualTargets;
         this.hotTags = manualTargets;
         this.hotTagsUntilMs = System.currentTimeMillis() + 90000L;
      }

      this.submitImmediatePoll("AH immediate poll error: ");
   }

   public void requestImmediatePollForTags(List<String> tags) {
      LinkedHashSet<String> clean = new LinkedHashSet<>();
      if (tags != null) {
         for (String raw : tags) {
            for (String t : expandWatchTag(raw)) {
               if (t != null && !t.isBlank()) {
                  clean.add(t);
               }
            }
         }
      }

      if (clean.isEmpty()) {
         this.requestImmediatePoll();
      } else {
         List<String> selected = new ArrayList<>(clean);
         this.forcedTagsForNextPoll = selected;
         this.hotTags = selected;
         this.hotTagsUntilMs = System.currentTimeMillis() + 90000L;
         this.submitImmediatePoll("AH immediate poll error: ");
      }
   }

   private void submitImmediatePoll(String errorPrefix) {
      if (this.pollInFlight.compareAndSet(false, true)) {
         this.pollExecutor.submit(() -> {
            try {
               this.pollOnce();
            } catch (Throwable t) {
               this.statusLine = errorPrefix + t.getClass().getSimpleName();
            } finally {
               this.pollInFlight.set(false);
            }
         });
      }
   }

   public void tick(Minecraft client) {
      this.drainAutoBuys(client);
      this.drainAlerts(client);
      this.tickAutoBuy(client);
      if (client == null || client.player == null) {
         this.statusLine = "AH passive scan paused: no player";
      } else if (!this.config.ahNotificationsEnabled) {
         this.statusLine = "AH passive scan is OFF (enable notifications)";
      } else if (!isOnHypixel(client)) {
         this.statusLine = "AH passive scan waiting for Hypixel";
      } else if (System.currentTimeMillis() >= this.cooldownUntilMs) {
         int intervalTicks = Math.max(20, Math.min(1200, this.config.ahPollIntervalSeconds * 20));
         if (this.isHotModeActive()) {
            intervalTicks = Math.min(intervalTicks, 5);
         }

         if (this.config.ahAdaptiveLatencyMode && this.isAdaptiveFastModeActive()) {
            intervalTicks = Math.min(intervalTicks, 2);
         }

         this.pollTickCounter++;
         if (this.pollTickCounter >= intervalTicks) {
            this.pollTickCounter = 0;
            if (this.pollInFlight.compareAndSet(false, true)) {
               this.pollExecutor.submit(() -> {
                  try {
                     this.pollOnce();
                  } catch (Throwable t) {
                     PotatoToolMod.LOGGER.warn("AH notifier poll failed: " + t.getMessage());
                     this.statusLine = "AH poll error: " + t.getClass().getSimpleName();
                  } finally {
                     this.pollInFlight.set(false);
                  }
               });
            }
         }
      }
   }

   private void pollOnce() {
      List<String> watchTags = this.forcedTagsForNextPoll;
      boolean forced = watchTags != null && !watchTags.isEmpty();
      if (forced) {
         this.forcedTagsForNextPoll = null;
      } else {
         List<String> hot = this.hotTags;
         if (hot != null && !hot.isEmpty() && this.isHotModeActive()) {
            watchTags = hot;
         } else {
            watchTags = this.getEffectiveWatchTags();
         }
      }

      if (watchTags.isEmpty()) {
         this.statusLine = "AH notifier: no tags to watch";
      } else {
         int tagsPerPoll = forced ? watchTags.size() : Math.max(1, Math.min(30, this.config.ahTagsPerPoll));
         if (!forced) {
            int autoFloor = Math.min(60, Math.max(8, watchTags.size() / 35));
            tagsPerPoll = Math.max(tagsPerPoll, autoFloor);
            if (this.config.ahAdaptiveLatencyMode && this.isAdaptiveFastModeActive()) {
               tagsPerPoll = Math.max(tagsPerPoll, Math.min(60, Math.max(14, watchTags.size() / 20)));
            }
         }

         tagsPerPoll = Math.max(1, Math.min(tagsPerPoll, watchTags.size()));
         int maxAlertsPerPoll = Math.max(1, Math.min(20, this.config.ahMaxAlertsPerPoll));
         String profilePreset = normalizeProfilePreset(this.config.ahProfilePreset);
         if ("SAFE".equals(profilePreset)) {
            tagsPerPoll = Math.min(tagsPerPoll, Math.max(6, Math.min(12, watchTags.size())));
            maxAlertsPerPoll = Math.min(maxAlertsPerPoll, 2);
         } else if ("SNIPING".equals(profilePreset)) {
            tagsPerPoll = Math.max(tagsPerPoll, Math.min(60, Math.max(20, watchTags.size() / 18)));
            maxAlertsPerPoll = Math.max(maxAlertsPerPoll, 8);
         } else if ("COLLECTING".equals(profilePreset)) {
            tagsPerPoll = Math.max(tagsPerPoll, Math.min(60, Math.max(30, watchTags.size() / 12)));
            maxAlertsPerPoll = Math.max(maxAlertsPerPoll, 12);
         }

         Set<String> seymourHexFilter = parseHexFilterSet(this.config.ahSeymourHexList);
         Set<String> specificHexFilter = parseHexFilterSet(this.config.ahSpecificHexList);
         Set<String> skinMuteFilters = parseTokenFilterSet(this.config.ahSkinMuteList);
         long nowMs = System.currentTimeMillis();
         long pollIntervalMs = Math.max(1, this.config.ahPollIntervalSeconds) * 1000L;
         long cyclesForAllTags = (long)Math.ceil((double)watchTags.size() / Math.max(1, tagsPerPoll));
         long fullCycleMs = Math.max(1L, cyclesForAllTags) * pollIntervalMs;
         long freshWindowMs = Math.max(25000L, Math.min(1800000L, Math.max(pollIntervalMs * 4L, fullCycleMs * 2L)));
         boolean passiveNewOnly = !forced;
         int alertsQueued = 0;
         int auctionsSeen = 0;
         int tagsPolled = 0;
         int invalidTags = 0;
         List<String> selectedTags = new ArrayList<>(tagsPerPoll);

         for (int i = 0; i < tagsPerPoll; i++) {
            selectedTags.add(watchTags.get(Math.floorMod(this.tagCursor + i, watchTags.size())));
         }

         Set<String> selectedTagSet = new LinkedHashSet<>();

         for (String t : selectedTags) {
            String u = upper(t);
            if (u != null && !u.isBlank()) {
               selectedTagSet.add(u);
            }
         }

         boolean hotMode = this.isHotModeActive();
         boolean fullSweepRequested = false;
         int hypixelPlannedPages = 0;
         boolean usedHypixelSource = this.hasConfiguredApiKey();
         int hypixelTotalPagesSeen = -1;
         if (usedHypixelSource) {
            int pageCursorLocal = Math.max(0, this.hypixelPageCursor);
            String apiKey = this.getAnyConfiguredApiKey();
            LinkedHashSet<Integer> pagePlan = new LinkedHashSet<>();
            List<AuctionNotificationManager.HypixelPagePayload> prefetchedPages = new ArrayList<>(1);
            int rotatingPages = 0;
            if (this.config.ahPollIntervalSeconds <= 1) {
               JsonObject firstRoot = this.fetchHypixelAuctionsPage(0, apiKey);
               int pagesRequested = Math.max(40, forced ? 24 : 30);
               if (this.config.ahAdaptiveLatencyMode && this.isAdaptiveFastModeActive()) {
                  pagesRequested = Math.max(pagesRequested, 60);
               }

               pagesRequested = Math.max(1, Math.min(60, pagesRequested));
               if (firstRoot != null && getBoolean(firstRoot, "success")) {
                  prefetchedPages.add(new AuctionNotificationManager.HypixelPagePayload(0, firstRoot));
                  int totalPages = (int)getLong(firstRoot, "totalPages", -1L);
                  if (totalPages <= 0) {
                     for (int i = 1; i < pagesRequested; i++) {
                        pagePlan.add(i);
                     }
                  } else {
                     hypixelTotalPagesSeen = totalPages;
                     int bounded = Math.max(1, Math.min(Math.min(260, 60), totalPages));
                     pagesRequested = Math.min(pagesRequested, bounded);
                     int headPages = Math.min(Math.max(1, 5), pagesRequested);

                     for (int i = 1; i < headPages; i++) {
                        pagePlan.add(i);
                     }

                     rotatingPages = Math.max(0, pagesRequested - headPages);
                     int span = Math.max(1, totalPages - 1);

                     for (int i = 0; i < rotatingPages; i++) {
                        int page = 1 + Math.floorMod(pageCursorLocal + i, span);
                        pagePlan.add(page);
                     }

                     fullSweepRequested = totalPages <= pagesRequested;
                  }
               } else {
                  for (int i = 0; i < pagesRequested; i++) {
                     pagePlan.add(i);
                  }
               }
            } else {
               int basePages = Math.max(1, Math.min(60, selectedTags.size()));
               int minPages = forced ? 24 : (hotMode ? 30 : 18);
               int pagesRequested = Math.max(basePages, minPages);
               if (this.config.ahPollIntervalSeconds <= 2) {
                  pagesRequested = Math.max(pagesRequested, 40);
               }

               if (this.config.ahAdaptiveLatencyMode && this.isAdaptiveFastModeActive()) {
                  pagesRequested = Math.max(pagesRequested, 60);
               }

               pagesRequested = Math.max(1, Math.min(60, pagesRequested));
               int headPages = Math.min(5, pagesRequested);

               for (int i = 0; i < headPages; i++) {
                  pagePlan.add(i);
               }

               rotatingPages = Math.max(0, pagesRequested - pagePlan.size());

               for (int i = 0; i < rotatingPages; i++) {
                  pagePlan.add(Math.max(0, pageCursorLocal + i));
               }
            }

            hypixelPlannedPages = pagePlan.size() + prefetchedPages.size();
            List<CompletableFuture<AuctionNotificationManager.HypixelPagePayload>> pageFutures = new ArrayList<>(pagePlan.size());

            for (Integer requestedPage : pagePlan) {
               int page = requestedPage == null ? 0 : requestedPage;
               pageFutures.add(
                  CompletableFuture.supplyAsync(
                     () -> new AuctionNotificationManager.HypixelPagePayload(page, this.fetchHypixelAuctionsPage(page, apiKey)), this.fetchExecutor
                  )
               );
            }

            for (AuctionNotificationManager.HypixelPagePayload pagePayload : prefetchedPages) {
               JsonObject root = pagePayload.payload;
               if (root != null) {
                  JsonArray arr = root.has("auctions") && root.get("auctions").isJsonArray() ? root.getAsJsonArray("auctions") : null;
                  if (arr != null) {
                     tagsPolled++;

                     for (JsonElement el : arr) {
                        if (el.isJsonObject()) {
                           JsonObject rawAuction = el.getAsJsonObject();
                           String auctionUuid = getString(rawAuction, "uuid");
                           if (auctionUuid != null && !auctionUuid.isEmpty()) {
                              if (passiveNewOnly) {
                                 long rawStartMs = getLong(rawAuction, "start", -1L);
                                 if (rawStartMs <= 0L || rawStartMs > nowMs + 5000L || nowMs - rawStartMs > freshWindowMs) {
                                    continue;
                                 }
                              }

                              auctionsSeen++;
                              Long previous = this.seenAuctionIds.putIfAbsent(auctionUuid, nowMs);
                              if (previous == null) {
                                 JsonObject auction = this.normalizeHypixelAuction(rawAuction);
                                 if (auction != null) {
                                    String auctionTag = upper(getString(auction, "tag"));
                                    if (auctionTag != null
                                       && !auctionTag.isBlank()
                                       && (!forced || selectedTagSet.contains(auctionTag) || this.allowAppliedSkinAuctionOutsideSelectedTags(auction))
                                       && (!passiveNewOnly || isFreshPassiveListing(auction, nowMs, freshWindowMs))) {
                                       AuctionNotificationManager.PendingAlert alert = this.classifyAuction(
                                          auction, seymourHexFilter, specificHexFilter, skinMuteFilters, null
                                       );
                                       if (alert != null && !this.isDuplicateAlertSuppressed(alert, auction, nowMs)) {
                                          if (alertsQueued < maxAlertsPerPoll) {
                                             this.pendingAlerts.add(alert);
                                             alertsQueued++;
                                          }

                                          this.queueAutoBuyIfMatched(alert);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            Iterator var80 = pageFutures.iterator();

            label391:
            while (true) {
               AuctionNotificationManager.HypixelPagePayload pagePayload;
               while (true) {
                  if (!var80.hasNext()) {
                     if (!forced && !hotMode && !fullSweepRequested && hypixelTotalPagesSeen > 0) {
                        int cursorAdvance = Math.max(1, rotatingPages);
                        this.hypixelPageCursor = Math.floorMod(pageCursorLocal + cursorAdvance, hypixelTotalPagesSeen);
                     }
                     break label391;
                  }

                  CompletableFuture<AuctionNotificationManager.HypixelPagePayload> future = (CompletableFuture<AuctionNotificationManager.HypixelPagePayload>)var80.next();

                  try {
                     pagePayload = future.join();
                     break;
                  } catch (Throwable ignored) {
                  }
               }

               if (pagePayload != null) {
                  JsonObject root = pagePayload.payload;
                  if (root != null && getBoolean(root, "success")) {
                     int pages = (int)getLong(root, "totalPages", -1L);
                     if (pages > 0) {
                        hypixelTotalPagesSeen = pages;
                     }

                     JsonArray arr = root.has("auctions") && root.get("auctions").isJsonArray() ? root.getAsJsonArray("auctions") : null;
                     if (arr != null) {
                        tagsPolled++;

                        for (JsonElement el : arr) {
                           if (el.isJsonObject()) {
                              JsonObject rawAuction = el.getAsJsonObject();
                              String auctionUuid = getString(rawAuction, "uuid");
                              if (auctionUuid != null && !auctionUuid.isEmpty()) {
                                 if (passiveNewOnly) {
                                    long rawStartMs = getLong(rawAuction, "start", -1L);
                                    if (rawStartMs <= 0L || rawStartMs > nowMs + 5000L || nowMs - rawStartMs > freshWindowMs) {
                                       continue;
                                    }
                                 }

                                 auctionsSeen++;
                                 Long previous = this.seenAuctionIds.putIfAbsent(auctionUuid, nowMs);
                                 if (previous == null) {
                                    JsonObject auction = this.normalizeHypixelAuction(rawAuction);
                                    if (auction != null) {
                                       String auctionTag = upper(getString(auction, "tag"));
                                       if (auctionTag != null
                                          && !auctionTag.isBlank()
                                          && (!forced || selectedTagSet.contains(auctionTag) || this.allowAppliedSkinAuctionOutsideSelectedTags(auction))
                                          && (!passiveNewOnly || isFreshPassiveListing(auction, nowMs, freshWindowMs))) {
                                          AuctionNotificationManager.PendingAlert alert = this.classifyAuction(
                                             auction, seymourHexFilter, specificHexFilter, skinMuteFilters, null
                                          );
                                          if (alert != null && !this.isDuplicateAlertSuppressed(alert, auction, nowMs)) {
                                             if (alertsQueued < maxAlertsPerPoll) {
                                                this.pendingAlerts.add(alert);
                                                alertsQueued++;
                                             }

                                             this.queueAutoBuyIfMatched(alert);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         } else {
            List<CompletableFuture<AuctionNotificationManager.TagPayload>> futures = new ArrayList<>(selectedTags.size());

            for (String tag : selectedTags) {
               futures.add(CompletableFuture.supplyAsync(() -> new AuctionNotificationManager.TagPayload(tag, this.fetchActiveBins(tag)), this.fetchExecutor));
            }

            Iterator var63 = futures.iterator();

            label521:
            while (true) {
               AuctionNotificationManager.TagPayload tagPayload;
               while (true) {
                  if (!var63.hasNext()) {
                     if (!forced && !this.isHotModeActive()) {
                        this.tagCursor = (this.tagCursor + selectedTags.size()) % Math.max(1, watchTags.size());
                     }
                     break label521;
                  }

                  CompletableFuture<AuctionNotificationManager.TagPayload> future = (CompletableFuture<AuctionNotificationManager.TagPayload>)var63.next();

                  try {
                     tagPayload = future.join();
                     break;
                  } catch (Throwable ignored) {
                  }
               }

               JsonElement payload = tagPayload.payload;
               if (payload != null) {
                  if (payload.isJsonObject()) {
                     String slug = getString(payload.getAsJsonObject(), "slug");
                     if ("item_not_found".equalsIgnoreCase(slug)) {
                        invalidTags++;
                     }
                  } else if (payload.isJsonArray()) {
                     tagsPolled++;
                     JsonArray arr = payload.getAsJsonArray();
                     Double tagLbinMillions = computeTagLbinMillions(arr);

                     for (JsonElement el : arr) {
                        if (el.isJsonObject()) {
                           JsonObject auction = el.getAsJsonObject();
                           String auctionUuid = getString(auction, "uuid");
                           if (auctionUuid != null && !auctionUuid.isEmpty()) {
                              auctionsSeen++;
                              Long previous = this.seenAuctionIds.putIfAbsent(auctionUuid, nowMs);
                              if (previous == null && (!passiveNewOnly || isFreshPassiveListing(auction, nowMs, freshWindowMs))) {
                                 AuctionNotificationManager.PendingAlert alert = this.classifyAuction(
                                    auction, seymourHexFilter, specificHexFilter, skinMuteFilters, tagLbinMillions
                                 );
                                 if (alert != null && !this.isDuplicateAlertSuppressed(alert, auction, nowMs)) {
                                    if (alertsQueued < maxAlertsPerPoll) {
                                       this.pendingAlerts.add(alert);
                                       alertsQueued++;
                                    }

                                    this.queueAutoBuyIfMatched(alert);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         this.cleanupSeenCache();
         if (!usedHypixelSource && tagsPolled == 0 && invalidTags > 0) {
            this.statusLine = "AH poll: " + invalidTags + " invalid tag(s). Fix watch tags.";
         } else {
            this.sessionPolls++;
            this.sessionSeen += auctionsSeen;
            this.sessionAlerts += alertsQueued;
            if (this.config.ahAdaptiveLatencyMode && alertsQueued > 0) {
               this.adaptiveFastUntilMs = nowMs + 45000L;
            }

            String freshNote = passiveNewOnly ? " | new<=" + freshWindowMs / 1000L + "s" : " | active-scan";
            String sessionNote = " | S:"
               + this.sessionPolls
               + " seen:"
               + this.sessionSeen
               + " alerts:"
               + this.sessionAlerts
               + " snipes:"
               + this.sessionSnipes
               + " qBuy:"
               + this.sessionAutoQueued;
            if (usedHypixelSource) {
               String planNote = hypixelPlannedPages > 0 ? " (planned " + hypixelPlannedPages + ")" : "";
               String sweepNote = fullSweepRequested ? " | full-sweep" : "";
               this.statusLine = "AH polled "
                  + tagsPolled
                  + " Hypixel page(s)"
                  + planNote
                  + ", seen "
                  + auctionsSeen
                  + ", alerts "
                  + alertsQueued
                  + freshNote
                  + sweepNote
                  + sessionNote;
            } else {
               this.statusLine = "AH polled "
                  + tagsPolled
                  + "/"
                  + watchTags.size()
                  + " tag(s), seen "
                  + auctionsSeen
                  + ", alerts "
                  + alertsQueued
                  + freshNote
                  + (invalidTags > 0 ? " (" + invalidTags + " invalid tag(s))" : "")
                  + sessionNote;
            }
         }
      }
   }

   private JsonElement fetchActiveBins(String tag) {
      try {
         String url = "https://sky.coflnet.com/api/auctions/tag/" + URLEncoder.encode(tag, StandardCharsets.UTF_8) + "/active/bin";
         Request req = new okhttp3.Request.Builder().url(url).build();
         Response res = HTTP.newCall(req).execute();

         Object var11;
         label78: {
            JsonElement var12;
            label79: {
               label80: {
                  try {
                     if (res.code() == 429) {
                        this.cooldownUntilMs = System.currentTimeMillis() + 20000L;
                        this.statusLine = "AH rate limited (429), cooling down";
                        var11 = null;
                        break label78;
                     }

                     if (res.isSuccessful() && res.body() != null) {
                        String body = res.body().string();
                        if (body != null && !body.isEmpty()) {
                           var12 = JsonParser.parseString(body);
                           break label79;
                        }

                        var12 = null;
                        break label80;
                     }

                     var11 = null;
                  } catch (Throwable var8) {
                     if (res != null) {
                        try {
                           res.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (res != null) {
                     res.close();
                  }

                  return (JsonElement)var11;
               }

               if (res != null) {
                  res.close();
               }

               return var12;
            }

            if (res != null) {
               res.close();
            }

            return var12;
         }

         if (res != null) {
            res.close();
         }

         return (JsonElement)var11;
      } catch (Exception e) {
         return null;
      }
   }

   private JsonObject fetchHypixelAuctionsPage(int page, String apiKey) {
      try {
         String url = "https://api.hypixel.net/v2/skyblock/auctions?page=" + Math.max(0, page);
         Request req = new okhttp3.Request.Builder().url(url).build();
         Response res = HTTP.newCall(req).execute();

         Object var14;
         label89: {
            JsonObject var9;
            label90: {
               Object parsed;
               label91: {
                  try {
                     if (res.code() == 429) {
                        this.cooldownUntilMs = System.currentTimeMillis() + 10000L;
                        this.statusLine = "AH rate limited (Hypixel 429), cooling down";
                        var14 = null;
                        break label89;
                     }

                     if (res.isSuccessful() && res.body() != null) {
                        String body = res.body().string();
                        if (body != null && !body.isEmpty()) {
                           JsonElement parsedx = JsonParser.parseString(body);
                           var9 = parsedx.isJsonObject() ? parsedx.getAsJsonObject() : null;
                           break label90;
                        }

                        parsed = null;
                        break label91;
                     }

                     var14 = null;
                  } catch (Throwable var11) {
                     if (res != null) {
                        try {
                           res.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  if (res != null) {
                     res.close();
                  }

                  return (JsonObject)var14;
               }

               if (res != null) {
                  res.close();
               }

               return (JsonObject)parsed;
            }

            if (res != null) {
               res.close();
            }

            return var9;
         }

         if (res != null) {
            res.close();
         }

         return (JsonObject)var14;
      } catch (Exception ignored) {
         return null;
      }
   }

   private JsonObject normalizeHypixelAuction(JsonObject raw) {
      if (raw == null) {
         return null;
      }

      if (raw.has("bin") && !raw.get("bin").isJsonNull()) {
         try {
            if (!raw.get("bin").getAsBoolean()) {
               return null;
            }
         } catch (Exception var13) {
         }
      }

      JsonObject out = new JsonObject();
      String uuid = getString(raw, "uuid");
      if (uuid != null && !uuid.isBlank()) {
         out.addProperty("uuid", uuid);
         out.addProperty("startingBid", getLong(raw, "starting_bid", 0L));
         String seller = getString(raw, "auctioneer");
         if (seller != null && !seller.isBlank()) {
            out.addProperty("seller", seller);
         }

         String itemBytes = getString(raw, "item_bytes");
         if (itemBytes != null && !itemBytes.isBlank()) {
            out.addProperty("itemBytes", itemBytes);
         }

         String itemName = getString(raw, "item_name");
         if (itemName == null || itemName.isBlank()) {
            itemName = getString(raw, "extra");
         }

         if (itemName != null && !itemName.isBlank()) {
            out.addProperty("itemName", itemName);
         }

         long startMs = getLong(raw, "start", -1L);
         if (startMs > 0L) {
            out.addProperty("start", Long.toString(startMs));
         }

         Map<String, String> flat = extractFlatNbtFromHypixelItemBytes(getString(raw, "item_bytes"));
         if (!flat.isEmpty()) {
            JsonObject flatObj = new JsonObject();

            for (Entry<String, String> e : flat.entrySet()) {
               if (e.getKey() != null && e.getValue() != null) {
                  flatObj.addProperty(e.getKey(), e.getValue());
               }
            }

            out.add("flatNbt", flatObj);
            String tag = upper(flat.get("id"));
            if (tag != null && !tag.isBlank()) {
               out.addProperty("tag", tag);
            }
         }

         if (!out.has("tag")) {
            String fallback = upper(getString(raw, "category"));
            if (fallback != null && !fallback.isBlank()) {
               out.addProperty("tag", fallback);
            }
         }

         return out;
      } else {
         return null;
      }
   }

   private static Map<String, String> extractFlatNbtFromHypixelItemBytes(String itemBytes) {
      Map<String, String> out = new LinkedHashMap<>();
      if (itemBytes != null && !itemBytes.isBlank()) {
         try {
            byte[] raw = Base64.getDecoder().decode(itemBytes);
            CompoundTag root = NbtIo.readCompressed(new ByteArrayInputStream(raw), NbtAccounter.unlimitedHeap());
            if (root == null) {
               return out;
            }

            CompoundTag item = root;
            if (root.keySet().contains("i") && root.get("i") instanceof ListTag list && !list.isEmpty()) {
               Tag first = (Tag)list.get(0);
               if (first instanceof CompoundTag compound) {
                  item = compound;
               }
            }

            CompoundTag tag = getNbtCompound(item, "tag");
            CompoundTag extra = getNbtCompound(tag, "ExtraAttributes");

            for (String key : extra.keySet()) {
               Tag element = extra.get(key);
               if (element != null) {
                  out.put(key.toLowerCase(Locale.ROOT), nbtElementToString(element));
               }
            }

            CompoundTag display = getNbtCompound(tag, "display");
            if (display.keySet().contains("color") && !out.containsKey("color") && display.get("color") instanceof IntTag n) {
               out.put("color", Integer.toString(n.intValue()));
            }
         } catch (Exception var10) {
         }

         return out;
      } else {
         return out;
      }
   }

   private static CompoundTag getNbtCompound(CompoundTag parent, String key) {
      if (parent != null && key != null && !key.isBlank()) {
         if (!parent.keySet().contains(key)) {
            return new CompoundTag();
         } else {
            return parent.get(key) instanceof CompoundTag compound ? compound : new CompoundTag();
         }
      } else {
         return new CompoundTag();
      }
   }

   private static String nbtElementToString(Tag element) {
      if (element == null) {
         return "";
      } else {
         String v = element.toString();
         if (v == null) {
            return "";
         } else {
            return v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2 ? v.substring(1, v.length() - 1) : v;
         }
      }
   }

   private boolean hasConfiguredApiKey() {
      return this.getAnyConfiguredApiKey() != null;
   }

   private String getAnyConfiguredApiKey() {
      if (this.config != null && this.config.apiKeys != null && !this.config.apiKeys.isEmpty()) {
         for (String raw : this.config.apiKeys) {
            if (raw != null) {
               String k = raw.trim().replaceAll("\\s+", "");
               if (!k.isEmpty()) {
                  return k;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private AuctionNotificationManager.PendingAlert classifyAuction(
      JsonObject auction, Set<String> seymourHexFilter, Set<String> specificHexFilter, Set<String> skinMuteFilters, Double tagLbinMillions
   ) {
      String uuid = getString(auction, "uuid");
      String tag = upper(getString(auction, "tag"));
      String seller = normalizeSellerName(getString(auction, "seller"));
      String itemName = getString(auction, "itemName");
      if (itemName == null || itemName.isEmpty()) {
         itemName = tag != null ? tag : "Unknown Item";
      }

      long price = getLong(auction, "startingBid", 0L);
      if (!withinMillionsRange(price, this.config.ahNotifyMinPriceMillions, this.config.ahNotifyMaxPriceMillions)) {
         return null;
      }

      Map<String, String> flat = extractFlatNbt(auction);
      boolean hasCosmeticDye = flat.containsKey("dye_item") || flat.containsKey("dye");
      String cosmeticDyeToken = flat.containsKey("dye_item") ? flat.get("dye_item") : flat.get("dye");
      String hex = extractHex(auction, flat);
      String normalizedHex = SeymourAnalyzer.normalizeHex(hex);
      String displayHex = normalizedHex == null ? null : "#" + normalizedHex;
      boolean plainFairyArmor = isPlainFairyArmorAuction(tag, itemName, displayHex, hasCosmeticDye);
      Long tagMarketRefCoins = tagLbinMillions != null && tagLbinMillions > 0.0 ? Math.max(1L, Math.round(tagLbinMillions * 1000000.0)) : null;
      Long enrichedMarketRefCoins = this.estimateMarketRefCoins(auction, tag, flat, tagMarketRefCoins);
      boolean skinAuction = isSkinAuction(tag, itemName, flat);
      String hexMode = normalizeHexMode(this.config.ahHexMatchMode);
      int hexDistance = Math.max(0, Math.min(441, this.config.ahHexDistanceMax));
      int stageTolerance = Math.max(0, Math.min(50, this.config.ahHexStageTolerance));
      if (normalizedHex != null) {
         if (this.config.ahNotifySpecificHexes && matchesHexFilter(normalizedHex, specificHexFilter, hexMode, hexDistance)) {
            return new AuctionNotificationManager.PendingAlert(
               uuid, "SPECIFIC HEX", itemName, tag, seller, price, displayHex, null, enrichedMarketRefCoins, "HIGH", null
            );
         }

         SeymourAnalyzer.PieceType piece = SeymourAnalyzer.detectPieceType(itemName, tag);
         if (piece != null) {
            String specialPattern = SeymourAnalyzer.patternDisplayName(SeymourAnalyzer.detectSpecialPattern(normalizedHex));
            SeymourAnalyzer.MatchResult match = SeymourAnalyzer.classifyTier(displayHex, itemName, tag, this.config);
            if (this.config.ahNotifySeymourSpecificHexes
               && matchesSeymourHexFilter(normalizedHex, displayHex, itemName, seymourHexFilter, hexMode, hexDistance, stageTolerance, match)) {
               String extra = match == null ? null : "Closest: " + safeText(match.getMatchedName()) + " dE " + format(match.getDeltaE());
               if (specialPattern != null) {
                  extra = extra == null ? "Pattern: " + specialPattern : extra + " | Pattern: " + specialPattern;
               }

               return new AuctionNotificationManager.PendingAlert(
                  uuid, "SEYMOUR HEX", itemName, tag, seller, price, displayHex, extra, enrichedMarketRefCoins, null, null
               );
            }

            if (match != null && SeymourAnalyzer.passesScanFilters(displayHex, match, this.config)) {
               if (this.config.ahNotifySeymourT1 && match.getTier() == 1) {
                  String extra = "Closest: " + safeText(match.getMatchedName()) + " dE " + format(match.getDeltaE());
                  if (specialPattern != null) {
                     extra = extra + " | Pattern: " + specialPattern;
                  }

                  return new AuctionNotificationManager.PendingAlert(
                     uuid, "SEYMOUR T1", itemName, tag, seller, price, displayHex, extra, enrichedMarketRefCoins, null, null
                  );
               }

               if (this.config.ahNotifySeymourT2 && match.getTier() == 2) {
                  String extra = "Closest: " + safeText(match.getMatchedName()) + " dE " + format(match.getDeltaE());
                  if (specialPattern != null) {
                     extra = extra + " | Pattern: " + specialPattern;
                  }

                  return new AuctionNotificationManager.PendingAlert(
                     uuid, "SEYMOUR T2", itemName, tag, seller, price, displayHex, extra, enrichedMarketRefCoins, null, null
                  );
               }
            }
         }

         if (this.config.ahNotifyGlitched
            && (
               ColorAnalyzer.isGlitchedDungeonArmor(tag, displayHex, cosmeticDyeToken)
                  || ColorAnalyzer.isBlackWitherArmorByName(itemName, normalizedHex, cosmeticDyeToken)
            )) {
            return new AuctionNotificationManager.PendingAlert(
               uuid, "GLITCHED", itemName, tag, seller, price, displayHex, null, enrichedMarketRefCoins, null, null
            );
         }

         if (this.config.ahNotifyOgFairy && (ColorAnalyzer.isOgFairyDyeToken(cosmeticDyeToken) || ColorAnalyzer.isOgFairyMatch(displayHex, tag, 0L))) {
            if (plainFairyArmor) {
               return null;
            }

            return new AuctionNotificationManager.PendingAlert(
               uuid, "OG FAIRY", itemName, tag, seller, price, displayHex, null, enrichedMarketRefCoins, null, null
            );
         }

         if (this.config.ahNotifyFairy && (ColorAnalyzer.isFairyDyeToken(cosmeticDyeToken) || ColorAnalyzer.isFairyColor(displayHex))) {
            if (plainFairyArmor) {
               return null;
            }

            return new AuctionNotificationManager.PendingAlert(
               uuid, "FAIRY", itemName, tag, seller, price, displayHex, null, enrichedMarketRefCoins, null, null
            );
         }

         if (this.config.ahNotifyCrystal && ColorAnalyzer.isCrystalColor(displayHex) && !ColorAnalyzer.isCrystalArmorId(tag)) {
            return new AuctionNotificationManager.PendingAlert(
               uuid, "CRYSTAL", itemName, tag, seller, price, displayHex, null, enrichedMarketRefCoins, null, null
            );
         }

         if (this.config.ahNotifyBleached && (hasDyeToken(cosmeticDyeToken, "BLEACHED") || ColorAnalyzer.isBleachedColor(displayHex, tag))) {
            return new AuctionNotificationManager.PendingAlert(
               uuid, "BLEACHED", itemName, tag, seller, price, displayHex, null, enrichedMarketRefCoins, null, null
            );
         }

         if (this.config.ahNotifyExotic && !hasCosmeticDye && ColorAnalyzer.isExoticColor(displayHex, tag)) {
            return new AuctionNotificationManager.PendingAlert(
               uuid, "EXOTIC", itemName, tag, seller, price, displayHex, null, enrichedMarketRefCoins, null, null
            );
         }
      }

      if (!skinAuction) {
         return null;
      }

      if (!this.config.ahSkinScanningEnabled) {
         return null;
      }

      if (!isDirectSkinAuction(tag, itemName, flat) && looksLikeEquipmentItem(tag, itemName, flat)) {
         return null;
      }

      if (isMutedSkin(tag, itemName, skinMuteFilters)) {
         return null;
      }

      String skinVariantTag = extractSkinVariantTag(tag, flat);
      String canonicalSkinTag = resolveCanonicalSkinTag(tag, skinVariantTag, flat);
      String displaySkinName = decorateGenericSkinName(itemName, canonicalSkinTag != null ? canonicalSkinTag : skinVariantTag);
      CosmeticSkinValuesLoader.SkinMarketStats canonicalStats = canonicalSkinTag != null
         ? CosmeticSkinValuesLoader.getOrRequestSkinMarketStats(canonicalSkinTag)
         : null;
      Double statsWeeklyM = canonicalStats != null ? canonicalStats.weeklyAvgMillions : null;
      boolean hasCoflWeekly = statsWeeklyM != null && statsWeeklyM > 0.0;
      Double referenceValueM = hasCoflWeekly ? statsWeeklyM : (canonicalSkinTag != null ? CosmeticSkinValuesLoader.getValueMillions(canonicalSkinTag) : null);
      if (referenceValueM == null) {
         referenceValueM = getAuctionSkinValueMillions(tag, flat);
      }

      Double listingValueM = price > 0L ? price / 1000000.0 : null;
      Double dailyAvgM = canonicalStats != null ? canonicalStats.dailyAvgMillions : null;
      Double lastSaleM = canonicalStats != null && canonicalStats.lastSaleCoins != null && canonicalStats.lastSaleCoins > 0L
         ? canonicalStats.lastSaleCoins.longValue() / 1000000.0
         : null;
      Double marketValueM = blendSkinMarketValueMillions(referenceValueM, dailyAvgM, tagLbinMillions, lastSaleM, hasCoflWeekly, isDirectSkinTagForValue(tag));
      String marketConfidence = computeMarketConfidence(hasCoflWeekly, dailyAvgM, tagLbinMillions, lastSaleM);
      Long marketRefCoins = marketValueM != null && marketValueM > 0.0 ? Math.max(1L, Math.round(marketValueM * 1000000.0)) : null;
      boolean isSnipe = listingValueM != null && marketValueM != null && marketValueM > 0.0 && listingValueM <= marketValueM * 0.8;
      int skinMinM = Math.max(0, this.config.ahSkinMinValueMillions);
      int skinCapM = Math.max(0, this.config.ahSkinMaxValueMillions);
      if (!isSnipe) {
         if ("SNIPE_ONLY".equals(normalizeProfitPreset(this.config.ahProfitPreset))) {
            return null;
         }

         if (skinMinM > 0 && marketValueM != null && marketValueM < skinMinM) {
            return null;
         }

         if (skinCapM > 0 && marketValueM != null && marketValueM > skinCapM) {
            return null;
         }
      }

      String petSkinState = detectPetSkinState(tag, itemName, flat, canonicalSkinTag);
      boolean petSkin = petSkinState != null || isPetSkinTag(canonicalSkinTag);
      if (petSkin && petSkinState == null) {
         petSkinState = containsSkinMarker(flat) && !isPetSkinTag(tag) ? "Applied" : "Unapplied";
      }

      String rawState = petSkin ? petSkinState : detectArmorSkinState(tag, flat);
      String skinStateLabel = toSkinStateLabel(rawState);
      String stateConfidence = computeStateConfidence(rawState, tag, flat);
      if (petSkin && !this.config.ahNotifyPetSkins) {
         return null;
      }

      if (!petSkin && !this.config.ahNotifyArmorSkins) {
         return null;
      }

      String extra = null;
      if (skinStateLabel != null && marketValueM != null) {
         extra = "State: " + skinStateLabel + " | Value: " + formatMillions(marketValueM) + "m";
      } else if (skinStateLabel != null) {
         extra = "State: " + skinStateLabel;
      } else if (marketValueM != null) {
         extra = "Value: " + formatMillions(marketValueM) + "m";
      }

      if (extra != null) {
         extra = extra + " | Conf: " + marketConfidence + "/" + stateConfidence;
      }

      if (isSnipe) {
         String statePrefix = skinStateLabel != null ? "State: " + skinStateLabel + " | " : "";
         double dealPct = marketValueM > 0.0 ? listingValueM / marketValueM * 100.0 : 100.0;
         int snipeScore = computeSnipeScore(marketValueM, listingValueM, dealPct, marketConfidence, stateConfidence, skinStateLabel);
         int minScore = resolveMinSnipeScore(this.config);
         if (snipeScore < minScore) {
            return null;
         }

         extra = statePrefix + "Deal: " + format(dealPct) + "% of value | Ref: " + formatMillions(marketValueM) + "m | Score: " + snipeScore;
         this.sessionSnipes++;
         return new AuctionNotificationManager.PendingAlert(
            uuid, "SNIPE", displaySkinName, tag, seller, price, displayHex, extra, marketRefCoins, marketConfidence, snipeScore
         );
      } else {
         return new AuctionNotificationManager.PendingAlert(
            uuid, petSkin ? "PET SKIN" : "ARMOR SKIN", displaySkinName, tag, seller, price, displayHex, extra, marketRefCoins, marketConfidence, null
         );
      }
   }

   private static boolean hasDyeToken(String rawToken, String tokenPart) {
      if (rawToken != null && !rawToken.isBlank() && tokenPart != null && !tokenPart.isBlank()) {
         String normalizedRaw = rawToken.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
         String normalizedPart = tokenPart.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
         return normalizedRaw.contains(normalizedPart);
      } else {
         return false;
      }
   }

   private static boolean isPlainFairyArmorAuction(String tag, String itemName, String displayHex, boolean hasCosmeticDye) {
      String t = upper(tag);
      if (t == null) {
         return false;
      }

      if (!t.startsWith("FAIRY_")) {
         return false;
      }

      if (hasCosmeticDye) {
         return false;
      }

      if (displayHex != null && !displayHex.isBlank()) {
         try {
            if (!DefaultArmorColorsLoader.hasCustomColor(t, displayHex)) {
               return true;
            }
         } catch (Throwable var8) {
         }

         String n = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
         boolean fairyName = n.contains("fairy");
         boolean armorName = n.contains("helmet") || n.contains("chestplate") || n.contains("leggings") || n.contains("boots");
         return fairyName && armorName && ColorAnalyzer.isFairyColor(displayHex);
      } else {
         return true;
      }
   }

   private static Map<String, String> extractFlatNbt(JsonObject auction) {
      Map<String, String> out = new LinkedHashMap<>();
      if (auction != null && auction.has("flatNbt") && auction.get("flatNbt").isJsonObject()) {
         JsonObject flat = auction.getAsJsonObject("flatNbt");

         for (Entry<String, JsonElement> e : flat.entrySet()) {
            if (e.getValue() != null && !e.getValue().isJsonNull()) {
               String v = e.getValue().isJsonPrimitive() ? e.getValue().getAsString() : e.getValue().toString();
               out.put(e.getKey().toLowerCase(Locale.ROOT), v);
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static String extractHex(JsonObject auction, Map<String, String> flat) {
      if (flat.containsKey("color")) {
         String parsed = parseColorStringToHex(flat.get("color"));
         if (parsed != null) {
            return "#" + parsed;
         }
      }

      if (auction.has("nbtData") && auction.get("nbtData").isJsonObject()) {
         JsonObject nbtData = auction.getAsJsonObject("nbtData");
         if (nbtData.has("data") && nbtData.get("data").isJsonObject()) {
            JsonObject data = nbtData.getAsJsonObject("data");
            if (data.has("color")) {
               String parsed = parseColorStringToHex(data.get("color").getAsString());
               if (parsed != null) {
                  return "#" + parsed;
               }
            }
         }
      }

      return null;
   }

   private static String parseColorStringToHex(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String s = raw.trim();
         if (s.contains(":")) {
            String[] p = s.split(":");
            if (p.length != 3) {
               return null;
            }

            try {
               int r = Integer.parseInt(p[0].trim());
               int g = Integer.parseInt(p[1].trim());
               int b = Integer.parseInt(p[2].trim());
               r = Math.max(0, Math.min(255, r));
               g = Math.max(0, Math.min(255, g));
               b = Math.max(0, Math.min(255, b));
               return String.format("%02X%02X%02X", r, g, b);
            } catch (NumberFormatException e) {
               return null;
            }
         } else {
            if (s.matches("^\\d{1,8}$")) {
               try {
                  int rgb = Integer.parseInt(s);
                  if (rgb >= 0 && rgb <= 16777215) {
                     return String.format("%06X", rgb & 16777215);
                  }
               } catch (NumberFormatException var7) {
               }
            }

            s = s.replace("#", "").toUpperCase(Locale.ROOT);
            if (s.length() != 6) {
               return null;
            }

            for (int i = 0; i < s.length(); i++) {
               char c = s.charAt(i);
               if ((c < '0' || c > '9') && (c < 'A' || c > 'F')) {
                  return null;
               }
            }

            return s;
         }
      } else {
         return null;
      }
   }

   private static boolean isSkinAuction(String tag, String itemName, Map<String, String> flat) {
      if (isDirectSkinAuction(tag, itemName, flat)) {
         return true;
      } else if (!containsSkinMarker(flat)) {
         return false;
      } else {
         return looksLikePetAuction(tag, itemName, flat) ? true : looksLikeArmorWearable(tag, itemName);
      }
   }

   private static boolean isDirectSkinAuction(String tag, String itemName, Map<String, String> flat) {
      String t = upper(tag);
      if (isDirectSkinTagForValue(t)) {
         return true;
      } else if (t != null && CosmeticSkinValuesLoader.isKnownSkinItemId(t)) {
         return true;
      } else {
         String variant = upper(extractSkinVariantTag(t, flat));
         if (variant == null || !isDirectSkinTagForValue(variant) && !CosmeticSkinValuesLoader.isKnownSkinItemId(variant)) {
            String n = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
            return n.endsWith(" skin") || n.contains(" pet skin");
         } else {
            return true;
         }
      }
   }

   private static boolean isPetSkinTag(String tag) {
      String t = upper(tag);
      return t != null && t.startsWith("PET_SKIN_");
   }

   private static String detectPetSkinState(String tag, String itemName, Map<String, String> flat, String canonicalSkinTag) {
      if (isPetSkinTag(canonicalSkinTag)) {
         return containsSkinMarker(flat) && !isPetSkinTag(tag) ? "Applied" : "Unapplied";
      } else {
         boolean petAuction = looksLikePetAuction(tag, itemName, flat);
         if (petAuction && containsSkinMarker(flat)) {
            return "Applied";
         } else {
            return isPetSkinTag(tag) ? "Unapplied" : null;
         }
      }
   }

   private static String detectArmorSkinState(String tag, Map<String, String> flat) {
      return containsSkinMarker(flat) && !isDirectSkinTagForValue(tag) ? "Applied" : "Unapplied";
   }

   private static String toSkinStateLabel(String rawState) {
      if (rawState != null && !rawState.isBlank()) {
         String s = rawState.trim();
         if (s.equalsIgnoreCase("Applied")) {
            return "Applied Skin";
         } else if (s.equalsIgnoreCase("Unapplied")) {
            return "Unapplied Skin";
         } else {
            return s.toLowerCase(Locale.ROOT).contains("skin") ? s : s + " Skin";
         }
      } else {
         return null;
      }
   }

   private static boolean looksLikePetAuction(String tag, String itemName, Map<String, String> flat) {
      String t = upper(tag);
      if (t != null && (t.equals("PET") || t.startsWith("PET_") || t.endsWith("_PET")) && !t.startsWith("PET_SKIN_")) {
         return true;
      }

      String n = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
      if (n.contains("[lvl")) {
         return true;
      }

      if (flat != null && !flat.isEmpty()) {
         for (String key : flat.keySet()) {
            if (key != null) {
               String k = key.toLowerCase(Locale.ROOT);
               if (!k.equals("type") && !k.equals("exp") && !k.equals("tier") && !k.equals("active") && !k.equals("candyused")) {
                  if (!k.contains("petinfo")
                     && !k.contains("pet_info")
                     && !k.contains("pettype")
                     && !k.contains("pet_type")
                     && !k.contains("helditem")
                     && !k.contains("petsoulbound")) {
                     continue;
                  }

                  return true;
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean containsSkinMarker(Map<String, String> flat) {
      if (flat != null && !flat.isEmpty()) {
         if (flat.containsKey("skin")) {
            return true;
         }

         for (Entry<String, String> e : flat.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            if (key != null) {
               String k = key.toLowerCase(Locale.ROOT);
               if (k.equals("skin") || k.equals("pet_skin") || k.equals("item_skin") || k.endsWith("_skin")) {
                  return true;
               }
            }

            if (value != null) {
               String v = value.toUpperCase(Locale.ROOT);
               if (v.contains("PET_SKIN_") || v.contains("_SKIN_")) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean looksLikeArmorWearable(String tag, String itemName) {
      String t = upper(tag);
      if (t != null) {
         if (t.endsWith("_HELMET") || t.endsWith("_CHESTPLATE") || t.endsWith("_LEGGINGS") || t.endsWith("_BOOTS")) {
            return true;
         }

         if (t.startsWith("POWER_WITHER_") || t.startsWith("WISE_WITHER_") || t.startsWith("TANK_WITHER_") || t.startsWith("SPEED_WITHER_")) {
            return true;
         }

         if (t.startsWith("MOLTEN_")) {
            return true;
         }

         if (t.contains("HELMET") || t.contains("CHESTPLATE") || t.contains("LEGGINGS") || t.contains("BOOTS")) {
            return true;
         }
      }

      String n = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
      return n.contains("helmet")
         || n.contains("chestplate")
         || n.contains("leggings")
         || n.contains("boots")
         || n.contains("hat")
         || n.contains("hood")
         || n.contains("mask");
   }

   private static Double getAuctionSkinValueMillions(String tag, Map<String, String> flat) {
      String t = upper(tag);
      if (t != null) {
         Double direct = CosmeticSkinValuesLoader.getValueMillions(t);
         if (direct != null) {
            return direct;
         }
      }

      if (flat != null) {
         String fromSkinKey = findKnownSkinIdInRaw(flat.get("skin"));
         if (fromSkinKey != null) {
            Double v = CosmeticSkinValuesLoader.getValueMillions(fromSkinKey);
            if (v != null) {
               return v;
            }
         }

         for (Entry<String, String> e : flat.entrySet()) {
            String key = e.getKey();
            if (key != null && key.toLowerCase(Locale.ROOT).contains("skin")) {
               String candidate = findKnownSkinIdInRaw(e.getValue());
               if (candidate != null) {
                  Double v = CosmeticSkinValuesLoader.getValueMillions(candidate);
                  if (v != null) {
                     return v;
                  }
               }
            }
         }
      }

      return null;
   }

   private static String resolveCanonicalSkinTag(String tag, String skinVariantTag, Map<String, String> flat) {
      String direct = upper(tag);
      if (CosmeticSkinValuesLoader.isKnownSkinItemId(direct)) {
         return direct;
      }

      String variant = upper(skinVariantTag);
      if (CosmeticSkinValuesLoader.isKnownSkinItemId(variant)) {
         return variant;
      }

      if (flat != null) {
         String fromSkin = findKnownSkinIdInRaw(flat.get("skin"));
         if (fromSkin != null) {
            return upper(fromSkin);
         }

         for (Entry<String, String> e : flat.entrySet()) {
            String key = e.getKey();
            if (key != null && key.toLowerCase(Locale.ROOT).contains("skin")) {
               String fromAny = findKnownSkinIdInRaw(e.getValue());
               if (fromAny != null) {
                  return upper(fromAny);
               }
            }
         }
      }

      if (variant != null) {
         String prefixed = "PET_SKIN_" + variant;
         if (CosmeticSkinValuesLoader.isKnownSkinItemId(prefixed)) {
            return prefixed;
         }

         if (variant.startsWith("PET_")) {
            String petPrefixed = "PET_SKIN_" + variant.substring("PET_".length());
            if (CosmeticSkinValuesLoader.isKnownSkinItemId(petPrefixed)) {
               return petPrefixed;
            }
         }

         if (variant.startsWith("PET_SKIN_")) {
            return variant;
         }
      }

      return direct;
   }

   private static String findKnownSkinIdInRaw(String raw) {
      if (raw != null && !raw.isBlank()) {
         String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", " ");

         for (String token : cleaned.split("\\s+")) {
            if (token != null && !token.isBlank() && CosmeticSkinValuesLoader.hasValue(token)) {
               return token;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static String extractSkinVariantTag(String itemTag, Map<String, String> flat) {
      String directTag = upper(itemTag);
      if (looksLikeSkinVariantTag(directTag)) {
         return directTag;
      }

      if (flat != null && !flat.isEmpty()) {
         String fromSkin = findPotentialSkinIdInRaw(flat.get("skin"));
         if (fromSkin != null) {
            return fromSkin;
         }

         for (Entry<String, String> e : flat.entrySet()) {
            String key = e.getKey();
            if (key != null && key.toLowerCase(Locale.ROOT).contains("skin")) {
               String fromAny = findPotentialSkinIdInRaw(e.getValue());
               if (fromAny != null) {
                  return fromAny;
               }
            }
         }

         return directTag;
      } else {
         return directTag;
      }
   }

   private static String findPotentialSkinIdInRaw(String raw) {
      if (raw != null && !raw.isBlank()) {
         String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", " ");

         for (String token : cleaned.split("\\s+")) {
            if (token != null && !token.isBlank() && (CosmeticSkinValuesLoader.isKnownSkinItemId(token) || looksLikeSkinVariantTag(token))) {
               return token;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static boolean looksLikeSkinVariantTag(String token) {
      if (token != null && !token.isBlank()) {
         String t = token.toUpperCase(Locale.ROOT);
         return t.startsWith("PET_SKIN_") || t.contains("_SKIN_") || t.endsWith("_BABY") || t.contains("_BABY_");
      } else {
         return false;
      }
   }

   private static String decorateGenericSkinName(String itemName, String skinVariantTag) {
      if (itemName == null || itemName.isBlank()) {
         return itemName;
      }

      if (skinVariantTag != null && !skinVariantTag.isBlank()) {
         String cleanName = itemName.trim();
         if (!cleanName.equalsIgnoreCase("Baby Skin")) {
            return itemName;
         }

         String babyVariant = describeBabyVariant(skinVariantTag);
         return babyVariant != null && !babyVariant.isBlank() ? "Baby Skin (" + babyVariant + ")" : itemName;
      } else {
         return itemName;
      }
   }

   private static String describeBabyVariant(String skinVariantTag) {
      String tag = upper(skinVariantTag);
      if (tag != null && !tag.isBlank()) {
         String core = tag;
         if (core.endsWith("_BABY")) {
            core = core.substring(0, core.length() - 5);
         } else if (core.contains("_BABY_")) {
            core = core.replace("_BABY_", "_");
         }

         return !core.equals("PROTECTOR")
               && !core.equals("OLD")
               && !core.equals("HOLY")
               && !core.equals("STRONG")
               && !core.equals("UNSTABLE")
               && !core.equals("WISE")
               && !core.equals("YOUNG")
               && !core.equals("SUPERIOR")
            ? titleCaseWords(core)
            : titleCaseWords(core) + " Dragon";
      } else {
         return null;
      }
   }

   private static String titleCaseWords(String upperUnderscore) {
      if (upperUnderscore != null && !upperUnderscore.isBlank()) {
         String[] parts = upperUnderscore.split("_+");
         StringBuilder out = new StringBuilder();

         for (String p : parts) {
            if (p != null && !p.isBlank()) {
               if (out.length() > 0) {
                  out.append(' ');
               }

               String lower = p.toLowerCase(Locale.ROOT);
               out.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
            }
         }

         return out.toString();
      } else {
         return "";
      }
   }

   private static Double chooseDisplayedSkinValueMillions(Double referenceValueM, Double tagLbinValueM, boolean hasCoflWeekly, boolean directSkinTag) {
      if (hasCoflWeekly && referenceValueM != null && referenceValueM > 0.0) {
         return referenceValueM;
      }

      if (directSkinTag && tagLbinValueM != null && tagLbinValueM > 0.0) {
         if (referenceValueM == null || referenceValueM <= 0.0) {
            return tagLbinValueM;
         }

         double ratio = tagLbinValueM / Math.max(0.001, referenceValueM);
         if (ratio >= 1.2 || ratio <= 0.8) {
            return tagLbinValueM;
         }
      }

      if (referenceValueM != null && referenceValueM > 0.0) {
         return referenceValueM;
      } else {
         return tagLbinValueM != null && tagLbinValueM > 0.0 ? tagLbinValueM : null;
      }
   }

   private static Double blendSkinMarketValueMillions(
      Double weeklyRefM, Double dailyM, Double lbinM, Double lastSaleM, boolean hasCoflWeekly, boolean directSkinTag
   ) {
      double totalW = 0.0;
      double sum = 0.0;
      if (hasCoflWeekly && weeklyRefM != null && weeklyRefM > 0.0) {
         sum += weeklyRefM * 0.55;
         totalW += 0.55;
      } else if (weeklyRefM != null && weeklyRefM > 0.0) {
         sum += weeklyRefM * 0.4;
         totalW += 0.4;
      }

      if (dailyM != null && dailyM > 0.0) {
         sum += dailyM * 0.3;
         totalW += 0.3;
      }

      if (lbinM != null && lbinM > 0.0) {
         double w = directSkinTag ? 0.2 : 0.12;
         sum += lbinM * w;
         totalW += w;
      }

      if (lastSaleM != null && lastSaleM > 0.0) {
         sum += lastSaleM * 0.15;
         totalW += 0.15;
      }

      return totalW <= 0.0 ? null : sum / totalW;
   }

   private static String computeMarketConfidence(boolean hasCoflWeekly, Double dailyM, Double lbinM, Double lastSaleM) {
      int signals = 0;
      if (hasCoflWeekly) {
         signals += 2;
      }

      if (dailyM != null && dailyM > 0.0) {
         signals++;
      }

      if (lbinM != null && lbinM > 0.0) {
         signals++;
      }

      if (lastSaleM != null && lastSaleM > 0.0) {
         signals++;
      }

      if (signals >= 4) {
         return "HIGH";
      } else {
         return signals >= 2 ? "MED" : "LOW";
      }
   }

   private static String computeStateConfidence(String rawState, String tag, Map<String, String> flat) {
      if (rawState != null && !rawState.isBlank()) {
         String t = upper(tag);
         boolean direct = isDirectSkinTagForValue(t);
         boolean marker = containsSkinMarker(flat);
         if (direct && marker) {
            return "HIGH";
         } else {
            return !direct && !marker ? "LOW" : "MED";
         }
      } else {
         return "LOW";
      }
   }

   private static int computeSnipeScore(
      Double marketValueM, Double listingValueM, double dealPct, String marketConfidence, String stateConfidence, String skinStateLabel
   ) {
      if (marketValueM != null && !(marketValueM <= 0.0) && listingValueM != null && !(listingValueM <= 0.0)) {
         double discountPct = Math.max(0.0, 100.0 - dealPct);
         double marketScale = Math.log10(Math.max(1.0, marketValueM)) * 8.0;
         int marketConf = "HIGH".equals(marketConfidence) ? 12 : ("MED".equals(marketConfidence) ? 7 : 2);
         int stateConf = "HIGH".equals(stateConfidence) ? 8 : ("MED".equals(stateConfidence) ? 4 : 1);
         int unappliedBonus = skinStateLabel != null && skinStateLabel.toLowerCase(Locale.ROOT).contains("unapplied") ? 6 : 0;
         int score = (int)Math.round(discountPct * 1.35 + marketScale + marketConf + stateConf + unappliedBonus);
         return Math.max(0, Math.min(100, score));
      } else {
         return 0;
      }
   }

   private static boolean looksLikeEquipmentItem(String tag, String itemName, Map<String, String> flat) {
      String t = upper(tag);
      if (t == null
         || !t.endsWith("_NECKLACE")
            && !t.endsWith("_CLOAK")
            && !t.endsWith("_BELT")
            && !t.endsWith("_GAUNTLET")
            && !t.endsWith("_BRACELET")
            && !t.endsWith("_GLOVES")
            && !t.contains("EQUIPMENT")) {
         String n = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
         if (!n.contains("necklace")
            && !n.contains("cloak")
            && !n.contains("belt")
            && !n.contains("gauntlet")
            && !n.contains("bracelet")
            && !n.contains("gloves")) {
            if (flat != null) {
               for (String k : flat.keySet()) {
                  if (k != null) {
                     String lk = k.toLowerCase(Locale.ROOT);
                     if (lk.contains("equipment")) {
                        return true;
                     }
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean isDirectSkinTagForValue(String tag) {
      String t = upper(tag);
      if (t == null || t.isBlank()) {
         return false;
      } else if (t.equals("PET")) {
         return false;
      } else {
         return t.startsWith("PET_") && !t.startsWith("PET_SKIN_")
            ? false
            : t.startsWith("PET_SKIN_") || t.contains("_SKIN_") || t.startsWith("PARTY_HAT") || CosmeticSkinValuesLoader.isKnownSkinItemId(t);
      }
   }

   private static Double computeTagLbinMillions(JsonArray auctions) {
      if (auctions != null && !auctions.isEmpty()) {
         long min = Long.MAX_VALUE;

         for (JsonElement el : auctions) {
            if (el.isJsonObject()) {
               long bid = getLong(el.getAsJsonObject(), "startingBid", 0L);
               if (bid > 0L && bid < min) {
                  min = bid;
               }
            }
         }

         return min == Long.MAX_VALUE ? null : min / 1000000.0;
      } else {
         return null;
      }
   }

   private static Set<String> parseHexFilterSet(String raw) {
      if (raw != null && !raw.isEmpty()) {
         Set<String> out = new LinkedHashSet<>();

         for (String part : raw.split("[,;\\n\\r\\t ]+")) {
            String h = SeymourAnalyzer.normalizeHex(part);
            if (h != null) {
               out.add(h);
            }
         }

         return out;
      } else {
         return Collections.emptySet();
      }
   }

   private static Set<String> parseTokenFilterSet(String raw) {
      if (raw != null && !raw.isBlank()) {
         Set<String> out = new LinkedHashSet<>();

         for (String part : raw.split("[,;\\n\\r\\t]+")) {
            String p = normalizeToken(part);
            if (p != null) {
               out.add(p);
            }
         }

         return out;
      } else {
         return Collections.emptySet();
      }
   }

   private static boolean isMutedSkin(String tag, String itemName, Set<String> muteFilters) {
      if (muteFilters != null && !muteFilters.isEmpty()) {
         String tagNorm = normalizeToken(tag);
         String nameNorm = normalizeToken(itemName);

         for (String f : muteFilters) {
            if (f != null && !f.isBlank()) {
               if (tagNorm == null || !tagNorm.equals(f) && !tagNorm.contains(f) && !f.contains(tagNorm)) {
                  if (nameNorm == null || !nameNorm.equals(f) && !nameNorm.contains(f)) {
                     continue;
                  }

                  return true;
               }

               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static String normalizeToken(String raw) {
      if (raw == null) {
         return null;
      }

      String norm = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
      norm = norm.replaceAll("^_+", "").replaceAll("_+$", "");
      return norm.isEmpty() ? null : norm;
   }

   private List<String> getEffectiveWatchTags() {
      boolean wantSkinTags = this.config.ahSkinScanningEnabled && (this.config.ahNotifyArmorSkins || this.config.ahNotifyPetSkins);
      boolean wantArmorTags = this.config.ahNotifyFairy
         || this.config.ahNotifyOgFairy
         || this.config.ahNotifyCrystal
         || this.config.ahNotifyBleached
         || this.config.ahNotifyExotic
         || this.config.ahNotifyGlitched
         || this.config.ahSkinScanningEnabled && this.config.ahNotifyArmorSkins
         || this.config.ahNotifySeymourT1
         || this.config.ahNotifySeymourT2
         || this.config.ahNotifySeymourSpecificHexes;
      LinkedHashSet<String> normalized = new LinkedHashSet<>();
      if (wantSkinTags) {
         int skinMinM = Math.max(0, this.config.ahSkinMinValueMillions);
         int skinCapM = Math.max(0, this.config.ahSkinMaxValueMillions);
         List<String> skinTags = new ArrayList<>(CosmeticSkinValuesLoader.getKnownSkinItemIds());
         Collections.sort(skinTags);
         Iterator var7 = skinTags.iterator();

         label164:
         while (true) {
            String skinId;
            while (true) {
               if (!var7.hasNext()) {
                  break label164;
               }

               skinId = (String)var7.next();
               if (skinId != null && !skinId.isBlank() && !"ignore".equalsIgnoreCase(skinId)) {
                  if (skinMinM <= 0 && skinCapM <= 0) {
                     break;
                  }

                  Double valueM = CosmeticSkinValuesLoader.getValueMillions(skinId);
                  if ((skinMinM <= 0 || valueM == null || !(valueM < skinMinM)) && (skinCapM <= 0 || valueM == null || !(valueM > skinCapM))) {
                     break;
                  }
               }
            }

            for (String t : expandWatchTag(skinId)) {
               if (t != null && !t.isBlank()) {
                  normalized.add(t);
               }
            }
         }
      }

      if (wantArmorTags) {
         List<String> armorTags = new ArrayList<>(DefaultArmorColorsLoader.getKnownItemIds());
         Collections.sort(armorTags);

         for (String armorId : armorTags) {
            if (armorId != null && !armorId.isBlank() && !"ignore".equalsIgnoreCase(armorId)) {
               for (String t : expandWatchTag(armorId)) {
                  if (t != null && !t.isBlank()) {
                     normalized.add(t);
                  }
               }
            }
         }

         for (String seymourTag : List.of("VELVET_TOP_HAT", "CASHMERE_JACKET", "SATIN_TROUSERS", "OXFORD_SHOES")) {
            for (String t : expandWatchTag(seymourTag)) {
               if (t != null && !t.isBlank()) {
                  normalized.add(t);
               }
            }
         }
      }

      if (this.config.ahSkinScanningEnabled && this.config.ahNotifyPetSkins) {
         for (String t : expandWatchTag("PET")) {
            if (t != null && !t.isBlank()) {
               normalized.add(t);
            }
         }
      }

      return new ArrayList<>(normalized);
   }

   private boolean allowAppliedSkinAuctionOutsideSelectedTags(JsonObject auction) {
      if (!this.config.ahSkinScanningEnabled) {
         return false;
      }

      if (!this.config.ahNotifyArmorSkins && !this.config.ahNotifyPetSkins) {
         return false;
      }

      if (auction == null) {
         return false;
      }

      String tag = upper(getString(auction, "tag"));
      String itemName = getString(auction, "itemName");
      Map<String, String> flat = extractFlatNbt(auction);
      return isSkinAuction(tag, itemName, flat);
   }

   private List<String> getConfiguredManualWatchTags() {
      if (this.config.ahWatchTags != null && !this.config.ahWatchTags.isEmpty()) {
         LinkedHashSet<String> clean = new LinkedHashSet<>();

         for (String tag : this.config.ahWatchTags) {
            for (String t : expandWatchTag(tag)) {
               if (t != null && !t.isBlank()) {
                  clean.add(t);
               }
            }
         }

         return clean.isEmpty() ? Collections.emptyList() : new ArrayList<>(clean);
      } else {
         return Collections.emptyList();
      }
   }

   private void drainAlerts(Minecraft client) {
      if (client != null && client.player != null) {
         for (int drained = 0; drained < 4; drained++) {
            AuctionNotificationManager.PendingAlert a = this.pendingAlerts.poll();
            if (a == null) {
               break;
            }

            this.sendChatAlert(client, a);
         }
      }
   }

   private void drainAutoBuys(Minecraft client) {
      if (client != null && client.player != null) {
         if (this.autoBuyUuid == null) {
            long now = System.currentTimeMillis();
            if (now >= this.nextAutoBuyAllowedMs) {
               AuctionNotificationManager.PendingAutoBuy pending = this.pendingAutoBuys.poll();
               if (pending != null) {
                  if (this.requestAutoBuy(pending.uuid, client)) {
                     int cooldownSec = Math.max(0, Math.min(60, this.config.ahAutoBuyCooldownSeconds));
                     this.nextAutoBuyAllowedMs = now + cooldownSec * 1000L;
                     if (pending.reason != null && !pending.reason.isBlank()) {
                        client.player.sendSystemMessage(Component.literal("§5[PotatoToolV2] §dAuto-buy rule matched: §f" + pending.reason));
                     }
                  }
               }
            }
         }
      }
   }

   private void sendChatAlert(Minecraft client, AuctionNotificationManager.PendingAlert alert) {
      String item = sanitizeName(alert.itemName);
      String price = formatCoins(alert.price);
      String hex = alert.hex != null ? " §8| §7Hex: §f" + alert.hex : "";
      String extra = alert.extra != null && !alert.extra.isBlank() ? " §8| §b" + alert.extra : "";
      String conf = alert.confidence != null && !alert.confidence.isBlank() ? " §8| §7Conf: " + alert.confidence : "";
      String score = alert.snipeScore != null ? " §8| §cScore: §f" + alert.snipeScore : "";
      String typeColor = colorForType(alert.typeLabel);
      MutableComponent line = Component.literal(
         "§5§l[PotatoToolV2] §d§lAH ALERT §8» " + typeColor + alert.typeLabel + " §f" + item + " §8| §6" + price + " coins" + hex + extra + conf + score + " "
      );
      String viewCommand = "/ptview " + alert.uuid;
      String rawViewCommand = "/viewauction " + alert.uuid;
      String listingUrl = "https://sky.coflnet.com/auction/" + alert.uuid;
      MutableComponent open = Component.literal("§a[View]")
         .withStyle(
            s -> s.withClickEvent(new RunCommand(viewCommand))
               .withHoverEvent(new ShowText(Component.literal("View only (cancels auto-buy for this listing)")))
         );
      MutableComponent buy = Component.literal(" §c[Buy]")
         .withStyle(
            s -> s.withClickEvent(new RunCommand("/ptbuy " + alert.uuid)).withHoverEvent(new ShowText(Component.literal("Auto-buy this listing")))
         );
      MutableComponent copy = Component.literal(" §b[Copy]")
         .withStyle(s -> s.withClickEvent(new CopyToClipboard(rawViewCommand)).withHoverEvent(new ShowText(Component.literal("Copy /viewauction command"))));
      MutableComponent cofl = Component.literal(" §9[Cofl]")
         .withStyle(s -> s.withClickEvent(new OpenUrl(URI.create(listingUrl))).withHoverEvent(new ShowText(Component.literal("Open on CoflNet"))));
      line.append(open);
      line.append(buy);
      line.append(copy);
      line.append(cofl);
      String seller = normalizeSellerName(alert.seller);
      if (seller != null) {
         MutableComponent sellerPrefix = Component.literal(" §8| §7Seller: ");
         MutableComponent sellerName = Component.literal("§b" + seller)
            .withStyle(s -> s.withClickEvent(new RunCommand("/ah " + seller)).withHoverEvent(new ShowText(Component.literal("Run /ah " + seller))));
         line.append(sellerPrefix);
         line.append(sellerName);
      }

      client.player.sendSystemMessage(line);
   }

   public boolean requestAutoBuy(String rawUuid, Minecraft client) {
      String uuid = normalizeAuctionUuid(rawUuid);
      if (uuid == null) {
         return false;
      }

      this.autoBuyUuid = uuid;
      this.autoBuyStartedMs = System.currentTimeMillis();
      this.autoBuyLastActionMs = 0L;
      this.autoBuyStage = 1;
      this.autoBuyViewResends = 0;
      if (client != null && client.player != null) {
         sendViewAuctionCommand(client, uuid);
         this.autoBuyLastActionMs = System.currentTimeMillis();
         client.player.sendSystemMessage(Component.literal("§5[PotatoToolV2] §fAuto-buy started for §e" + uuid));
      }

      return true;
   }

   public boolean requestViewOnly(String rawUuid, Minecraft client) {
      String uuid = normalizeAuctionUuid(rawUuid);
      if (uuid == null) {
         return false;
      }

      this.queuedAutoBuyIds.remove(uuid);
      this.pendingAutoBuys.removeIf(p -> p != null && uuid.equals(p.uuid));
      if (uuid.equals(this.autoBuyUuid)) {
         this.clearAutoBuy("§7View clicked: cancelled auto-buy for " + uuid, client);
      }

      sendViewAuctionCommand(client, uuid);
      return true;
   }

   private void queueAutoBuyIfMatched(AuctionNotificationManager.PendingAlert alert) {
      if (alert != null && this.config.ahAutoBuyEnabled) {
         AuctionNotificationManager.AutoBuyPriceRule forcedRule = matchAutoBuyRule(alert.itemName, alert.price, this.config.ahAutoBuyItemPriceRules);
         boolean forcedByRule = forcedRule != null;
         if (forcedByRule || this.isAutoBuyTypeAllowed(alert.typeLabel)) {
            if (!forcedByRule) {
               int minPriceM = Math.max(0, this.config.ahAutoBuyMinPriceMillions);
               int maxPriceM = Math.max(0, this.config.ahAutoBuyMaxPriceMillions);
               if (!withinMillionsRange(alert.price, minPriceM, maxPriceM)) {
                  return;
               }
            }

            if (alert.marketRefCoins == null || alert.marketRefCoins <= 0L || alert.price <= alert.marketRefCoins) {
               if (!forcedByRule && isSkinAutoBuyType(alert.typeLabel)) {
                  int skinMinValueM = Math.max(0, this.config.ahSkinMinValueMillions);
                  int skinMaxValueM = Math.max(0, this.config.ahSkinMaxValueMillions);
                  if (skinMinValueM > 0 || skinMaxValueM > 0) {
                     if (alert.marketRefCoins == null || alert.marketRefCoins <= 0L) {
                        return;
                     }

                     long minValueCoins = skinMinValueM > 0 ? skinMinValueM * 1000000L : 0L;
                     long maxValueCoins = skinMaxValueM > 0 ? skinMaxValueM * 1000000L : Long.MAX_VALUE;
                     if (alert.marketRefCoins < minValueCoins || alert.marketRefCoins > maxValueCoins) {
                        return;
                     }
                  }
               }

               if (!forcedByRule) {
                  if (!matchesAutoBuyNameFilter(alert.itemName, this.config.ahAutoBuyNameContains)) {
                     return;
                  }

                  if (!matchesAutoBuyHexFilter(alert.hex, this.config.ahAutoBuyHexList)) {
                     return;
                  }
               }

               long now = System.currentTimeMillis();
               Long previous = this.queuedAutoBuyIds.putIfAbsent(alert.uuid, now);
               if (previous == null) {
                  String reason = forcedByRule
                     ? "Rule: " + forcedRule.display + " | " + sanitizeName(alert.itemName)
                     : alert.typeLabel + " " + sanitizeName(alert.itemName);
                  this.pendingAutoBuys.add(new AuctionNotificationManager.PendingAutoBuy(alert.uuid, reason));
                  this.sessionAutoQueued++;
               }
            }
         }
      }
   }

   private boolean isAutoBuyTypeAllowed(String type) {
      if (type == null) {
         return false;
      }

      return switch (type) {
         case "SNIPE" -> this.config.ahAutoBuySnipe;
         case "EXOTIC" -> this.config.ahAutoBuyExotic;
         case "GLITCHED" -> this.config.ahAutoBuyGlitched;
         case "SEYMOUR T1" -> this.config.ahAutoBuySeymourT1;
         case "SEYMOUR T2" -> this.config.ahAutoBuySeymourT2;
         case "SEYMOUR HEX" -> this.config.ahAutoBuySeymourHex;
         case "ARMOR SKIN" -> this.config.ahAutoBuyArmorSkin;
         case "PET SKIN" -> this.config.ahAutoBuyPetSkin;
         default -> false;
      };
   }

   private static boolean isSkinAutoBuyType(String type) {
      return "SNIPE".equals(type) || "ARMOR SKIN".equals(type) || "PET SKIN".equals(type);
   }

   private static boolean matchesAutoBuyNameFilter(String itemName, String rawFilter) {
      if (rawFilter != null && !rawFilter.isBlank()) {
         String n = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);

         for (String token : rawFilter.split("[,;\\n\\r]+")) {
            String t = token == null ? "" : token.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && n.contains(t)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static boolean matchesAutoBuyHexFilter(String hex, String rawFilter) {
      if (rawFilter != null && !rawFilter.isBlank()) {
         String normalized = SeymourAnalyzer.normalizeHex(hex);
         if (normalized == null) {
            return false;
         }

         Set<String> allowed = parseHexFilterSet(rawFilter);
         return allowed.isEmpty() || allowed.contains(normalized);
      } else {
         return true;
      }
   }

   private static AuctionNotificationManager.AutoBuyPriceRule matchAutoBuyRule(String itemName, long priceCoins, String rawRules) {
      if (rawRules != null && !rawRules.isBlank()) {
         String name = itemName == null ? "" : itemName.toLowerCase(Locale.ROOT);
         if (name.isBlank()) {
            return null;
         }

         for (AuctionNotificationManager.AutoBuyPriceRule rule : parseAutoBuyRules(rawRules)) {
            if (rule != null && rule.nameToken != null && !rule.nameToken.isBlank() && name.contains(rule.nameToken) && priceCoins <= rule.maxPriceCoins) {
               return rule;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static List<AuctionNotificationManager.AutoBuyPriceRule> parseAutoBuyRules(String rawRules) {
      List<AuctionNotificationManager.AutoBuyPriceRule> out = new ArrayList<>();
      if (rawRules != null && !rawRules.isBlank()) {
         for (String part : rawRules.split("[,;\\n\\r]+")) {
            if (part != null) {
               String entry = part.trim();
               if (!entry.isEmpty()) {
                  AuctionNotificationManager.AutoBuyPriceRule parsed = parseAutoBuyRule(entry);
                  if (parsed != null) {
                     out.add(parsed);
                  }
               }
            }
         }

         return out;
      } else {
         return out;
      }
   }

   private static AuctionNotificationManager.AutoBuyPriceRule parseAutoBuyRule(String entry) {
      if (entry == null) {
         return null;
      }

      String s = entry.trim();
      if (s.isEmpty()) {
         return null;
      }

      int split = -1;
      int opLen = 0;

      for (String op : new String[]{"<=", "=", ":", "<"}) {
         int idx = s.indexOf(op);
         if (idx > 0) {
            split = idx;
            opLen = op.length();
            break;
         }
      }

      String left;
      String right;
      if (split > 0 && opLen > 0) {
         left = s.substring(0, split).trim().toLowerCase(Locale.ROOT);
         right = s.substring(split + opLen).trim().toLowerCase(Locale.ROOT);
      } else {
         String lower = s.toLowerCase(Locale.ROOT);
         int under = lower.lastIndexOf(" under ");
         if (under <= 0) {
            return null;
         }

         left = lower.substring(0, under).trim();
         right = lower.substring(under + " under ".length()).trim();
      }

      if (!left.isEmpty() && !right.isEmpty()) {
         right = right.replace("million", "m").replace("millions", "m").replace("coins", "").trim();
         if (right.endsWith("m")) {
            right = right.substring(0, right.length() - 1).trim();
         }

         if (right.isEmpty()) {
            return null;
         }

         try {
            double maxM = Double.parseDouble(right);
            if (!(maxM > 0.0)) {
               return null;
            }

            long maxCoins = Math.max(1L, Math.round(maxM * 1000000.0));
            String display = left + " <= " + format(maxM) + "m";
            return new AuctionNotificationManager.AutoBuyPriceRule(left, maxCoins, display);
         } catch (NumberFormatException ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   private void tickAutoBuy(Minecraft client) {
      String uuid = this.autoBuyUuid;
      if (uuid != null) {
         if (client != null && client.player != null) {
            if (!isOnHypixel(client)) {
               this.clearAutoBuy("§cAuto-buy cancelled: not on Hypixel", client);
            } else {
               long now = System.currentTimeMillis();
               if (now - this.autoBuyStartedMs > 12000L) {
                  this.clearAutoBuy("§cAuto-buy timeout for " + uuid, client);
               } else if (this.autoBuyStage == 1
                  && (client.screen == null || client.player.containerMenu == null || client.player.containerMenu.slots.isEmpty())
                  && now - this.autoBuyLastActionMs >= 1200L
                  && this.autoBuyViewResends < 2) {
                  this.autoBuyViewResends++;
                  sendViewAuctionCommand(client, uuid);
                  this.autoBuyLastActionMs = now;
               } else if (now - this.autoBuyLastActionMs >= 180L) {
                  AbstractContainerMenu handler = client.player.containerMenu;
                  if (handler != null && handler.slots != null && !handler.slots.isEmpty()) {
                     if (this.autoBuyStage == 1) {
                        int buySlotId = findAutoBuySlotId(handler, false);
                        if (buySlotId >= 0 && clickSlot(client, handler, buySlotId)) {
                           this.autoBuyStage = 2;
                           this.autoBuyLastActionMs = now;
                        }
                     } else {
                        if (this.autoBuyStage == 2) {
                           int confirmSlotId = findAutoBuySlotId(handler, true);
                           if (confirmSlotId >= 0 && clickSlot(client, handler, confirmSlotId)) {
                              this.clearAutoBuy("§aAuto-buy click sent for " + uuid, client);
                              return;
                           }

                           if (client.screen == null && now - this.autoBuyLastActionMs > 700L) {
                              this.clearAutoBuy("§aAuto-buy executed for " + uuid, client);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static String normalizeAuctionUuid(String raw) {
      if (raw == null) {
         return null;
      }

      String trimmed = raw.trim();
      if (trimmed.isEmpty()) {
         return null;
      }

      String[] parts = trimmed.split("\\s+");
      String candidate = parts[parts.length - 1].trim();
      return !candidate.matches("[A-Za-z0-9-]{8,64}") ? null : candidate;
   }

   private static void sendViewAuctionCommand(Minecraft client, String uuid) {
      if (client != null && client.player != null && client.player.connection != null) {
         client.player.connection.sendCommand("viewauction " + uuid);
      }
   }

   private void clearAutoBuy(String message, Minecraft client) {
      this.autoBuyUuid = null;
      this.autoBuyStartedMs = 0L;
      this.autoBuyLastActionMs = 0L;
      this.autoBuyStage = 0;
      this.autoBuyViewResends = 0;
      if (client != null && client.player != null && message != null && !message.isBlank()) {
         client.player.sendSystemMessage(Component.literal("§5[PotatoToolV2] " + message));
      }
   }

   private static int findAutoBuySlotId(AbstractContainerMenu handler, boolean confirmPhase) {
      if (handler != null && handler.slots != null && !handler.slots.isEmpty()) {
         int bestSlot = -1;
         int bestScore = Integer.MIN_VALUE;

         for (Slot slot : handler.slots) {
            if (slot != null) {
               ItemStack stack = slot.getItem();
               if (stack != null && !stack.isEmpty()) {
                  int score = scoreAutoBuySlot(stack, confirmPhase);
                  if (score > bestScore) {
                     bestScore = score;
                     bestSlot = slot.index;
                  }
               }
            }
         }

         return bestScore >= 60 ? bestSlot : -1;
      } else {
         return -1;
      }
   }

   private static int scoreAutoBuySlot(ItemStack stack, boolean confirmPhase) {
      String name = stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
      String nbt = extractStackNbtText(stack);
      int score = 0;
      if (confirmPhase) {
         if (name.contains("confirm")) {
            score += 120;
         }

         if (nbt.contains("confirm")) {
            score += 100;
         }

         if (nbt.contains("click to confirm")) {
            score += 60;
         }

         if (name.contains("cancel") || name.contains("back")) {
            score -= 80;
         }
      } else {
         if (name.contains("buy item right now")) {
            score += 140;
         }

         if (nbt.contains("buy item right now")) {
            score += 120;
         }

         if (nbt.contains("buy it now")) {
            score += 95;
         }

         if (name.contains("buy now")) {
            score += 75;
         }

         if (name.contains("buy")) {
            score += 30;
         }

         if (nbt.contains("click to purchase")) {
            score += 30;
         }

         if (name.contains("confirm")) {
            score -= 60;
         }
      }

      return score;
   }

   private static boolean clickSlot(Minecraft client, AbstractContainerMenu handler, int slotId) {
      if (client == null || client.gameMode == null || client.player == null || handler == null) {
         return false;
      }

      if (slotId < 0) {
         return false;
      }

      client.gameMode.handleContainerInput(handler.containerId, slotId, 0, ContainerInput.PICKUP, client.player);
      return true;
   }

   private static String extractStackNbtText(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         try {
            CustomData customData = (CustomData)stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            if (customData == null) {
               return "";
            }

            CompoundTag nbt = customData.copyTag();
            return nbt == null ? "" : nbt.toString().toLowerCase(Locale.ROOT);
         } catch (Throwable ignored) {
            return "";
         }
      } else {
         return "";
      }
   }

   private static String colorForType(String typeLabel) {
      if (typeLabel == null) {
         return "§f";
      }

      return switch (typeLabel) {
         case "SNIPE" -> "§c§l";
         case "GLITCHED" -> "§c§l";
         case "EXOTIC" -> "§a§l";
         case "FAIRY" -> "§d§l";
         case "OG FAIRY" -> "§5§l";
         case "CRYSTAL" -> "§b§l";
         case "BLEACHED" -> "§6§l";
         case "SEYMOUR T1" -> "§b§l";
         case "SEYMOUR T2" -> "§e§l";
         case "SEYMOUR HEX" -> "§d§l";
         case "SPECIFIC HEX" -> "§a§l";
         case "PET SKIN" -> "§6§l";
         case "ARMOR SKIN" -> "§3§l";
         default -> "§f";
      };
   }

   private static String sanitizeName(String raw) {
      if (raw != null && !raw.isEmpty()) {
         String s = raw.replaceAll("§[0-9A-FK-ORa-fk-or]", "").trim();
         if (s.length() > 42) {
            s = s.substring(0, 39) + "...";
         }

         return s;
      } else {
         return "Unknown Item";
      }
   }

   private static String stripFormatting(String raw) {
      return raw != null && !raw.isEmpty() ? raw.replaceAll("§[0-9A-FK-ORa-fk-or]", "").trim() : "";
   }

   private static String formatCoins(long value) {
      return value <= 0L ? "0" : String.format(Locale.US, "%,d", value);
   }

   private static String safeText(String s) {
      return s == null ? "-" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "").trim();
   }

   private static String format(double v) {
      return String.format(Locale.US, "%.2f", v);
   }

   private static String formatMillions(double v) {
      return Math.abs(v - Math.rint(v)) < 0.05 ? String.format(Locale.US, "%.0f", v) : String.format(Locale.US, "%.1f", v);
   }

   private static boolean withinMillionsRange(long coins, int minMillions, int maxMillions) {
      if (coins <= 0L) {
         return minMillions <= 0;
      }

      long minCoins = Math.max(0L, Math.max(0, minMillions) * 1000000L);
      long maxCoins = Math.max(0L, Math.max(0, maxMillions) * 1000000L);
      return minCoins > 0L && coins < minCoins ? false : maxCoins <= 0L || coins <= maxCoins;
   }

   private boolean isHotModeActive() {
      long now = System.currentTimeMillis();
      if (this.hotTagsUntilMs <= now) {
         this.hotTagsUntilMs = 0L;
         this.hotTags = null;
         return false;
      } else {
         return this.hotTags != null && !this.hotTags.isEmpty();
      }
   }

   private boolean isAdaptiveFastModeActive() {
      if (!this.config.ahAdaptiveLatencyMode) {
         return false;
      } else {
         long now = System.currentTimeMillis();
         if (this.adaptiveFastUntilMs <= now) {
            this.adaptiveFastUntilMs = 0L;
            return false;
         } else {
            return true;
         }
      }
   }

   private static String normalizeHexMode(String rawMode) {
      if (rawMode != null && !rawMode.isBlank()) {
         String m = rawMode.trim().toUpperCase(Locale.ROOT);
         return !"WITHIN_DELTA".equals(m) && !"STAGE_TOLERANCE".equals(m) && !"EXACT".equals(m) ? "EXACT" : m;
      } else {
         return "EXACT";
      }
   }

   private static String normalizeProfilePreset(String raw) {
      if (raw != null && !raw.isBlank()) {
         String v = raw.trim().toUpperCase(Locale.ROOT);
         return !"SAFE".equals(v) && !"SNIPING".equals(v) && !"COLLECTING".equals(v) ? "SNIPING" : v;
      } else {
         return "SNIPING";
      }
   }

   private static String normalizeProfitPreset(String raw) {
      if (raw != null && !raw.isBlank()) {
         String v = raw.trim().toUpperCase(Locale.ROOT);
         return !"SAFE".equals(v) && !"AGGRESSIVE".equals(v) && !"SNIPE_ONLY".equals(v) ? "AGGRESSIVE" : v;
      } else {
         return "AGGRESSIVE";
      }
   }

   private static int resolveMinSnipeScore(ScannerConfig config) {
      int base = Math.max(0, Math.min(100, config.ahSnipeMinScore));

      return switch (normalizeProfitPreset(config.ahProfitPreset)) {
         case "SAFE" -> Math.max(base, 75);
         case "SNIPE_ONLY" -> Math.max(25, base - 15);
         default -> base;
      };
   }

   private boolean isDuplicateAlertSuppressed(AuctionNotificationManager.PendingAlert alert, JsonObject auction, long nowMs) {
      if (alert == null) {
         return true;
      }

      int windowSec = Math.max(0, Math.min(300, this.config.ahDuplicateSuppressSeconds));
      if (windowSec <= 0) {
         return false;
      }

      String seller = normalizeToken(getString(auction, "seller"));
      String name = normalizeToken(alert.itemName);
      String hex = SeymourAnalyzer.normalizeHex(alert.hex);
      long roundedPrice = alert.price <= 0L ? 0L : (alert.price + 49999L) / 50000L * 50000L;
      String fingerprint = (alert.typeLabel == null ? "-" : alert.typeLabel)
         + "|"
         + (name == null ? "-" : name)
         + "|"
         + (alert.itemTag == null ? "-" : alert.itemTag)
         + "|"
         + roundedPrice
         + "|"
         + (hex == null ? "-" : hex)
         + "|"
         + (seller == null ? "-" : seller);
      long windowMs = windowSec * 1000L;
      Long previous = this.recentAlertFingerprints.put(fingerprint, nowMs);
      return previous != null && nowMs - previous < windowMs;
   }

   private static boolean matchesHexFilter(String candidateHex, Set<String> filterSet, String mode, int maxDistance) {
      String hex = SeymourAnalyzer.normalizeHex(candidateHex);
      if (hex != null && filterSet != null && !filterSet.isEmpty()) {
         String normalizedMode = normalizeHexMode(mode);
         int distCap = Math.max(0, Math.min(441, maxDistance));

         for (String raw : filterSet) {
            String target = SeymourAnalyzer.normalizeHex(raw);
            if (target != null) {
               if ("EXACT".equals(normalizedMode)) {
                  if (hex.equals(target)) {
                     return true;
                  }
               } else if (rgbDistance(hex, target) <= distCap) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static boolean matchesSeymourHexFilter(
      String candidateHex,
      String displayHex,
      String itemName,
      Set<String> filterSet,
      String mode,
      int maxDistance,
      int stageTolerance,
      SeymourAnalyzer.MatchResult precomputedMatch
   ) {
      String hex = SeymourAnalyzer.normalizeHex(candidateHex);
      if (hex != null && filterSet != null && !filterSet.isEmpty()) {
         String normalizedMode = normalizeHexMode(mode);
         int distCap = Math.max(0, Math.min(441, maxDistance));
         int stageTol = Math.max(0, Math.min(50, stageTolerance));
         if (!"EXACT".equals(normalizedMode) && !"WITHIN_DELTA".equals(normalizedMode)) {
            ScannerConfig seymourConfig = new ScannerConfig();
            SeymourAnalyzer.MatchResult itemMatch = precomputedMatch != null
               ? precomputedMatch
               : SeymourAnalyzer.classifyTier(displayHex, itemName, seymourConfig);
            int itemStage = itemMatch == null ? -1 : parseStageFromName(itemMatch.getMatchedName());
            if (itemStage <= 0) {
               return matchesHexFilter(hex, filterSet, "WITHIN_DELTA", distCap);
            }

            for (String raw : filterSet) {
               String targetHex = SeymourAnalyzer.normalizeHex(raw);
               if (targetHex != null) {
                  SeymourAnalyzer.MatchResult targetMatch = SeymourAnalyzer.classifyTier("#" + targetHex, itemName, seymourConfig);
                  int targetStage = targetMatch == null ? -1 : parseStageFromName(targetMatch.getMatchedName());
                  if (targetStage > 0 && Math.abs(itemStage - targetStage) <= stageTol) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return matchesHexFilter(hex, filterSet, normalizedMode, distCap);
         }
      } else {
         return false;
      }
   }

   private static int parseStageFromName(String text) {
      return SeymourAnalyzer.extractStageNumber(text);
   }

   private static int rgbDistance(String leftHex, String rightHex) {
      String l = SeymourAnalyzer.normalizeHex(leftHex);
      String r = SeymourAnalyzer.normalizeHex(rightHex);
      if (l != null && r != null) {
         int lr = Integer.parseInt(l.substring(0, 2), 16);
         int lg = Integer.parseInt(l.substring(2, 4), 16);
         int lb = Integer.parseInt(l.substring(4, 6), 16);
         int rr = Integer.parseInt(r.substring(0, 2), 16);
         int rg = Integer.parseInt(r.substring(2, 4), 16);
         int rb = Integer.parseInt(r.substring(4, 6), 16);
         return Math.abs(lr - rr) + Math.abs(lg - rg) + Math.abs(lb - rb);
      } else {
         return Integer.MAX_VALUE;
      }
   }

   private Long estimateMarketRefCoins(JsonObject auction, String tag, Map<String, String> flat, Long fallbackTagRefCoins) {
      long now = System.currentTimeMillis();
      String itemBytes = getString(auction, "itemBytes");
      String cacheKey = buildPriceCacheKey(tag, flat, itemBytes);
      if (cacheKey != null) {
         AuctionNotificationManager.PriceCacheEntry cached = this.coflPriceCache.get(cacheKey);
         if (cached != null) {
            long age = now - cached.atMs;
            long ttl = cached.miss ? PRICE_CACHE_MISS_TTL_MS : PRICE_CACHE_OK_TTL_MS;
            if (age >= 0L && age <= ttl) {
               return cached.miss ? fallbackTagRefCoins : cached.coins;
            }
         }
      }

      Long priced = this.fetchNbtMarketPrice(itemBytes);
      if (priced == null) {
         priced = this.fetchTagMarketPrice(tag);
         if (priced != null) {
            priced = applyFallbackEnhancementMultiplier(priced, flat);
         }
      }

      if (priced == null) {
         priced = fallbackTagRefCoins;
      }

      if (cacheKey != null) {
         this.coflPriceCache.put(cacheKey, new AuctionNotificationManager.PriceCacheEntry(priced == null ? 0L : priced, now, priced == null));
      }

      return priced;
   }

   private static String buildPriceCacheKey(String tag, Map<String, String> flat, String itemBytes) {
      String t = upper(tag);
      if (t != null && !t.isBlank() || itemBytes != null && !itemBytes.isBlank()) {
         StringBuilder sb = new StringBuilder();
         if (t != null && !t.isBlank()) {
            sb.append(t);
         }

         if (flat != null && !flat.isEmpty()) {
            String stars = flat.get("dungeon_item_level");
            String recomb = flat.get("rarity_upgrades");
            String hp = flat.get("hot_potato_count");
            String aow = flat.get("art_of_war_count");
            String enrich = flat.get("enchantments");
            if (stars != null) {
               sb.append("|s=").append(stars);
            }

            if (recomb != null) {
               sb.append("|r=").append(recomb);
            }

            if (hp != null) {
               sb.append("|hp=").append(hp);
            }

            if (aow != null) {
               sb.append("|aow=").append(aow);
            }

            if (enrich != null) {
               sb.append("|e=").append(enrich.hashCode());
            }
         }

         if (itemBytes != null && !itemBytes.isBlank()) {
            sb.append("|b=").append(itemBytes.hashCode());
         }

         return sb.toString();
      } else {
         return null;
      }
   }

   private Long fetchNbtMarketPrice(String itemBytes) {
      if (itemBytes != null && !itemBytes.isBlank()) {
         try {
            JsonObject body = new JsonObject();
            body.addProperty("fullInventoryNbt", itemBytes);
            body.addProperty("returnMedian", true);
            body.addProperty("returnLbin", true);
            body.addProperty("returnBreakDown", false);
            body.addProperty("returnEstimatedInternalData", false);
            Request req = new okhttp3.Request.Builder().url("https://sky.coflnet.com/api/price/nbt").post(RequestBody.create(body.toString(), JSON)).build();
            Response res = HTTP.newCall(req).execute();

            Long var7;
            label71: {
               Object parsed;
               label72: {
                  Object raw;
                  try {
                     if (res.isSuccessful() && res.body() != null) {
                        String rawx = res.body().string();
                        if (rawx != null && !rawx.isBlank()) {
                           JsonElement parsedx = JsonParser.parseString(rawx);
                           var7 = extractCoinsFromAnyPricePayload(parsedx);
                           break label71;
                        }

                        parsed = null;
                        break label72;
                     }

                     raw = null;
                  } catch (Throwable var9) {
                     if (res != null) {
                        try {
                           res.close();
                        } catch (Throwable var8) {
                           var9.addSuppressed(var8);
                        }
                     }

                     throw var9;
                  }

                  if (res != null) {
                     res.close();
                  }

                  return (Long)raw;
               }

               if (res != null) {
                  res.close();
               }

               return (Long)parsed;
            }

            if (res != null) {
               res.close();
            }

            return var7;
         } catch (Exception ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   private Long fetchTagMarketPrice(String tag) {
      String normalized = normalizeWatchTag(tag);
      if (normalized != null && !normalized.isBlank()) {
         try {
            String url = String.format(Locale.ROOT, "https://sky.coflnet.com/api/item/price/%s/bin", URLEncoder.encode(normalized, StandardCharsets.UTF_8));
            Request req = new okhttp3.Request.Builder().url(url).build();
            Response res = HTTP.newCall(req).execute();

            Long var8;
            label71: {
               Object parsed;
               label72: {
                  Object raw;
                  try {
                     if (res.isSuccessful() && res.body() != null) {
                        String rawx = res.body().string();
                        if (rawx != null && !rawx.isBlank()) {
                           JsonElement parsedx = JsonParser.parseString(rawx);
                           var8 = extractCoinsFromAnyPricePayload(parsedx);
                           break label71;
                        }

                        parsed = null;
                        break label72;
                     }

                     raw = null;
                  } catch (Throwable var10) {
                     if (res != null) {
                        try {
                           res.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (res != null) {
                     res.close();
                  }

                  return (Long)raw;
               }

               if (res != null) {
                  res.close();
               }

               return (Long)parsed;
            }

            if (res != null) {
               res.close();
            }

            return var8;
         } catch (Exception ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Long extractCoinsFromAnyPricePayload(JsonElement payload) {
      if (payload != null && !payload.isJsonNull()) {
         if (!payload.isJsonArray()) {
            if (!payload.isJsonObject()) {
               return null;
            } else {
               JsonObject obj = payload.getAsJsonObject();
               Long lbin = extractLongField(obj, "lbin");
               if (lbin != null && lbin > 0L) {
                  return lbin;
               } else {
                  Long median = extractLongField(obj, "median");
                  if (median != null && median > 0L) {
                     return median;
                  } else {
                     Long mean = extractLongField(obj, "mean");
                     if (mean != null && mean > 0L) {
                        return mean;
                     } else {
                        Long avg = extractLongField(obj, "average");
                        if (avg != null && avg > 0L) {
                           return avg;
                        } else {
                           Long price = extractLongField(obj, "price");
                           if (price != null && price > 0L) {
                              return price;
                           } else {
                              return obj.has("data") ? extractCoinsFromAnyPricePayload(obj.get("data")) : null;
                           }
                        }
                     }
                  }
               }
            }
         } else {
            JsonArray arr = payload.getAsJsonArray();
            Long best = null;

            for (JsonElement el : arr) {
               Long v = extractCoinsFromAnyPricePayload(el);
               if (v != null && v > 0L && (best == null || v < best)) {
                  best = v;
               }
            }

            return best;
         }
      } else {
         return null;
      }
   }

   private static Long extractLongField(JsonObject obj, String key) {
      if (obj != null && key != null && obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            JsonElement el = obj.get(key);
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber()) {
               return Math.round(el.getAsDouble());
            } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
               String s = el.getAsString();
               return s != null && !s.isBlank() ? Math.round(Double.parseDouble(s)) : null;
            } else {
               return null;
            }
         } catch (Exception ignored) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Long applyFallbackEnhancementMultiplier(Long baseCoins, Map<String, String> flat) {
      if (baseCoins != null && baseCoins > 0L && flat != null && !flat.isEmpty()) {
         double multiplier = 1.0;
         int stars = safeInt(flat.get("dungeon_item_level"));
         if (stars > 0) {
            multiplier += Math.min(0.45, stars * 0.03);
         }

         int recomb = safeInt(flat.get("rarity_upgrades"));
         if (recomb > 0) {
            multiplier += 0.1;
         }

         int hotPotato = safeInt(flat.get("hot_potato_count"));
         if (hotPotato > 0) {
            multiplier += Math.min(0.08, hotPotato * 0.003);
         }

         int artOfWar = safeInt(flat.get("art_of_war_count"));
         if (artOfWar > 0) {
            multiplier += 0.04;
         }

         String enchants = flat.get("enchantments");
         if (enchants != null && !enchants.isBlank()) {
            int score = 0;
            Matcher m = ENCHANT_ENTRY_PATTERN.matcher(enchants);

            while (m.find()) {
               int level = safeInt(m.group(2));
               if (level >= 6) {
                  score += level - 5;
               }
            }

            if (score > 0) {
               multiplier += Math.min(0.25, score * 0.01);
            }
         }

         return Math.max(1L, Math.round(baseCoins.longValue() * multiplier));
      } else {
         return baseCoins;
      }
   }

   private static int safeInt(String raw) {
      if (raw != null && !raw.isBlank()) {
         try {
            return Integer.parseInt(raw.replaceAll("[^0-9-]", ""));
         } catch (NumberFormatException ignored) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private void cleanupSeenCache() {
      long now = System.currentTimeMillis();
      if (now - this.lastSeenCleanupMs >= 120000L || this.seenAuctionIds.size() >= 40000) {
         this.lastSeenCleanupMs = now;
         long cutoff = now - TimeUnit.HOURS.toMillis(24L);
         this.seenAuctionIds.entrySet().removeIf(e -> e.getValue() < cutoff);
         this.queuedAutoBuyIds.entrySet().removeIf(e -> e.getValue() < cutoff);
         this.recentAlertFingerprints.entrySet().removeIf(e -> e.getValue() < now - ALERT_DEDUPE_CACHE_TTL_MS);
         this.coflPriceCache.entrySet().removeIf(e -> {
            AuctionNotificationManager.PriceCacheEntry v = e.getValue();
            if (v == null) {
               return true;
            }

            long ttl = v.miss ? PRICE_CACHE_MISS_TTL_MS : PRICE_CACHE_OK_TTL_MS;
            return v.atMs < now - ttl;
         });
      }
   }

   private static Map<String, String> createTagAliases() {
      Map<String, String> out = new LinkedHashMap<>();
      out.put("NECRON_HELMET", "POWER_WITHER_HELMET");
      out.put("NECRON_CHESTPLATE", "POWER_WITHER_CHESTPLATE");
      out.put("NECRON_LEGGINGS", "POWER_WITHER_LEGGINGS");
      out.put("NECRON_BOOTS", "POWER_WITHER_BOOTS");
      out.put("MAXOR_HELMET", "SPEED_WITHER_HELMET");
      out.put("MAXOR_CHESTPLATE", "SPEED_WITHER_CHESTPLATE");
      out.put("MAXOR_LEGGINGS", "SPEED_WITHER_LEGGINGS");
      out.put("MAXOR_BOOTS", "SPEED_WITHER_BOOTS");
      out.put("STORM_HELMET", "WISE_WITHER_HELMET");
      out.put("STORM_CHESTPLATE", "WISE_WITHER_CHESTPLATE");
      out.put("STORM_LEGGINGS", "WISE_WITHER_LEGGINGS");
      out.put("STORM_BOOTS", "WISE_WITHER_BOOTS");
      out.put("GOLDOR_HELMET", "TANK_WITHER_HELMET");
      out.put("GOLDOR_CHESTPLATE", "TANK_WITHER_CHESTPLATE");
      out.put("GOLDOR_LEGGINGS", "TANK_WITHER_LEGGINGS");
      out.put("GOLDOR_BOOTS", "TANK_WITHER_BOOTS");
      return out;
   }

   private static String normalizeWatchTag(String raw) {
      String t = upper(raw);
      if (t != null && !t.isBlank()) {
         t = t.replace(' ', '_');
         return TAG_ALIASES.containsKey(t) ? TAG_ALIASES.get(t) : t;
      } else {
         return null;
      }
   }

   private static List<String> expandWatchTag(String raw) {
      String t = normalizeWatchTag(raw);
      if (t != null && !t.isBlank()) {
         if (t.endsWith("_ARMOR")) {
            String base = t.substring(0, t.length() - "_ARMOR".length());
            List<String> out = new ArrayList<>(4);
            String[] pieces = new String[]{"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};

            for (String piece : pieces) {
               String expanded = normalizeWatchTag(base + "_" + piece);
               if (expanded != null && !expanded.isBlank()) {
                  out.add(expanded);
               }
            }

            return out;
         } else {
            return Collections.singletonList(t);
         }
      } else {
         return Collections.emptyList();
      }
   }

   private static boolean isOnHypixel(Minecraft client) {
      if (client == null) {
         return false;
      }

      ServerData entry = client.getCurrentServer();
      if (entry == null) {
         return false;
      }

      String addr = entry.ip;
      return addr != null && addr.toLowerCase(Locale.ROOT).contains("hypixel");
   }

   private static String upper(String s) {
      return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
   }

   private static String normalizeSellerName(String raw) {
      if (raw == null) {
         return null;
      }

      String cleaned = raw.trim().replaceAll("[^A-Za-z0-9_]", "");
      if (cleaned.isEmpty()) {
         return null;
      }

      if (cleaned.length() > 16) {
         cleaned = cleaned.substring(0, 16);
      }

      return cleaned;
   }

   private static boolean isFreshPassiveListing(JsonObject auction, long nowMs, long windowMs) {
      long listedAtMs = parseAuctionStartMs(auction);
      if (listedAtMs <= 0L) {
         return false;
      } else {
         return listedAtMs > nowMs + 5000L ? false : nowMs - listedAtMs <= windowMs;
      }
   }

   private static long parseAuctionStartMs(JsonObject auction) {
      String start = getString(auction, "start");
      if (start != null && !start.isBlank()) {
         String s = start.trim();
         if (s.matches("^-?\\d{10,20}$")) {
            try {
               long n = Long.parseLong(s);
               long ms = normalizeEpochToMillis(n);
               return ms > 0L ? ms : -1L;
            } catch (NumberFormatException ignored) {
               return -1L;
            }
         } else {
            try {
               return !s.endsWith("Z") && !s.contains("+") ? LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli() : Instant.parse(s).toEpochMilli();
            } catch (DateTimeParseException ignored) {
               return -1L;
            }
         }
      } else {
         return -1L;
      }
   }

   private static long normalizeEpochToMillis(long n) {
      if (n > 0L && n < 10000000000L) {
         return n * 1000L;
      } else if (n >= 10000000000000000L) {
         return n / 1000000L;
      } else {
         return n >= 10000000000000L ? n / 1000L : n;
      }
   }

   private static String getString(JsonObject obj, String key) {
      if (obj != null && key != null && obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            return obj.get(key).getAsString();
         } catch (Exception e) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static long getLong(JsonObject obj, String key, long fallback) {
      if (obj != null && key != null && obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            return obj.get(key).getAsLong();
         } catch (Exception e) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private static boolean getBoolean(JsonObject obj, String key) {
      if (obj != null && key != null && obj.has(key) && !obj.get(key).isJsonNull()) {
         try {
            return obj.get(key).getAsBoolean();
         } catch (Exception e) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static final class AutoBuyPriceRule {
      final String nameToken;
      final long maxPriceCoins;
      final String display;

      AutoBuyPriceRule(String nameToken, long maxPriceCoins, String display) {
         this.nameToken = nameToken;
         this.maxPriceCoins = maxPriceCoins;
         this.display = display;
      }
   }

   private static final class HypixelPagePayload {
      final int requestedPage;
      final JsonObject payload;

      HypixelPagePayload(int requestedPage, JsonObject payload) {
         this.requestedPage = requestedPage;
         this.payload = payload;
      }
   }

   private static final class PendingAlert {
      final String uuid;
      final String typeLabel;
      final String itemName;
      final String itemTag;
      final String seller;
      final long price;
      final String hex;
      final String extra;
      final Long marketRefCoins;
      final String confidence;
      final Integer snipeScore;

      PendingAlert(String uuid, String typeLabel, String itemName, String itemTag, long price, String hex, String extra) {
         this(uuid, typeLabel, itemName, itemTag, null, price, hex, extra, null, null, null);
      }

      PendingAlert(String uuid, String typeLabel, String itemName, String itemTag, long price, String hex, String extra, Long marketRefCoins) {
         this(uuid, typeLabel, itemName, itemTag, null, price, hex, extra, marketRefCoins, null, null);
      }

      PendingAlert(
         String uuid,
         String typeLabel,
         String itemName,
         String itemTag,
         long price,
         String hex,
         String extra,
         Long marketRefCoins,
         String confidence,
         Integer snipeScore
      ) {
         this(uuid, typeLabel, itemName, itemTag, null, price, hex, extra, marketRefCoins, confidence, snipeScore);
      }

      PendingAlert(
         String uuid,
         String typeLabel,
         String itemName,
         String itemTag,
         String seller,
         long price,
         String hex,
         String extra,
         Long marketRefCoins,
         String confidence,
         Integer snipeScore
      ) {
         this.uuid = uuid;
         this.typeLabel = typeLabel;
         this.itemName = itemName;
         this.itemTag = itemTag;
         this.seller = seller;
         this.price = price;
         this.hex = hex;
         this.extra = extra;
         this.marketRefCoins = marketRefCoins;
         this.confidence = confidence;
         this.snipeScore = snipeScore;
      }
   }

   private static final class PendingAutoBuy {
      final String uuid;
      final String reason;

      PendingAutoBuy(String uuid, String reason) {
         this.uuid = uuid;
         this.reason = reason;
      }
   }

   private static final class PriceCacheEntry {
      final long coins;
      final long atMs;
      final boolean miss;

      PriceCacheEntry(long coins, long atMs, boolean miss) {
         this.coins = coins;
         this.atMs = atMs;
         this.miss = miss;
      }
   }

   private static final class TagPayload {
      final String tag;
      final JsonElement payload;

      TagPayload(String tag, JsonElement payload) {
         this.tag = tag;
         this.payload = payload;
      }
   }
}
