package com.potatotool.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.potatotool.PotatoToolMod;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;

public class ScannerConfig {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("potato-tool-v2.json");
   private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("potato-tool.json");
   public List<String> apiKeys = new ArrayList<>();
   public List<Long> apiKeyAddedAt = new ArrayList<>();
   public List<Integer> apiKeyUsages = new ArrayList<>();
   public static final int API_KEY_USAGE_LIMIT = 5000;
   public static final int API_WINDOW_LIMIT = 300;
   public static final long API_WINDOW_MS = 300000L;
   private transient long lastUsageSaveMs;
   private transient Map<String, Deque<Long>> apiKeyWindows;
   public boolean lobbyScanningEnabled = true;
   public boolean islandScanningEnabled = true;
   public boolean crystalScanningEnabled = true;
   public boolean bleachedScanningEnabled = true;
   public boolean ogFairyScanningEnabled = true;
   public boolean fairyScanningEnabled = true;
   public boolean exoticScanningEnabled = true;
   public boolean specificHexScanningEnabled = false;
   public String specificHexList = "";
   public boolean seymourScanningEnabled = true;
   public boolean seymourScanTopHat = true;
   public boolean seymourScanJacket = true;
   public boolean seymourScanTrousers = true;
   public boolean seymourScanShoes = true;
   public boolean seymourScanTargetColors = true;
   public boolean seymourScanFadeDyes = false;
   public boolean seymourScanT1 = true;
   public boolean seymourScanT2 = true;
   public boolean seymourExactHexOnly = false;
   public int seymourTargetStage = 0;
   public int seymourStageTolerance = 2;
   public boolean seymourShowHighFades = false;
   public boolean seymourPieceSpecificEnabled = true;
   public String seymourArmorNameFilters = "all";
   public int seymourMaxColorDistance = 40;
   public boolean newYearCakeEnabled = true;
   public int newYearCakeMaxYear = 500;
   public int newYearCakeOnlyYearAndBefore = 0;
   public List<Integer> newYearCakeSpecificYears = defaultCakeYears();
   public boolean cakeYearsListMode = true;
   public boolean legacyReforgeEnabled = true;
   public boolean ghostReforgeEnabled = true;
   public boolean valuableItemsEnabled = true;
   public boolean scanAnniversaryHats = false;
   public boolean renderItemTracers = true;
   public boolean showPlayerOverlay = true;
   public boolean ahNotificationsEnabled = false;
   public int ahPollIntervalSeconds = 6;
   public int ahTagsPerPoll = 6;
   public int ahMaxAlertsPerPoll = 4;
   public int ahNotifyMinPriceMillions = 0;
   public int ahNotifyMaxPriceMillions = 0;
   public boolean ahSkinScanningEnabled = true;
   public int ahSkinMinValueMillions = 0;
   public int ahSkinMaxValueMillions = 0;
   public String ahSkinMuteList = "";
   public boolean ahNotifyFairy = true;
   public boolean ahNotifyExotic = true;
   public boolean ahNotifySkins = true;
   public boolean ahNotifyArmorSkins = true;
   public boolean ahNotifyPetSkins = false;
   public boolean ahNotifyCrystal = true;
   public boolean ahNotifyOgFairy = true;
   public boolean ahNotifyBleached = true;
   public boolean ahNotifyGlitched = true;
   public boolean ahNotifySeymourT1 = true;
   public boolean ahNotifySeymourT2 = true;
   public boolean ahNotifySeymourSpecificHexes = true;
   public boolean ahNotifySpecificHexes = true;
   public String ahSpecificHexList = "";
   public String ahSeymourHexList = "";
   public List<String> ahWatchTags = new ArrayList<>();
   public boolean ahAutoBuyEnabled = false;
   public int ahAutoBuyMinPriceMillions = 0;
   public int ahAutoBuyMaxPriceMillions = 0;
   public int ahAutoBuyCooldownSeconds = 2;
   public boolean ahAutoBuySnipe = true;
   public boolean ahAutoBuyExotic = false;
   public boolean ahAutoBuyGlitched = false;
   public boolean ahAutoBuySeymourT1 = false;
   public boolean ahAutoBuySeymourT2 = false;
   public boolean ahAutoBuySeymourHex = false;
   public boolean ahAutoBuyArmorSkin = false;
   public boolean ahAutoBuyPetSkin = false;
   public String ahAutoBuyNameContains = "";
   public String ahAutoBuyHexList = "";
   public String ahAutoBuyItemPriceRules = "";
   public int ahSnipeMinScore = 45;
   public boolean ahAdaptiveLatencyMode = true;
   public int ahDuplicateSuppressSeconds = 20;
   public String ahProfitPreset = "AGGRESSIVE";
   public String ahProfilePreset = "SNIPING";
   public String ahHexMatchMode = "EXACT";
   public int ahHexDistanceMax = 18;
   public int ahHexStageTolerance = 2;
   public String hudAnchor = "TOP_LEFT";
   public int hudOffsetX = 80;
   public int hudOffsetY = 10;
   public double hudScale = 1.0;
   public String hudBorderTheme = "WHITE";
   public boolean hudFairyAccentEnabled = false;
   public int hudColorRgb = 0xFFFFFF;
   public int guiAccentRgb = 0x6EA8FF;
   public String guiAccentTheme = "BLUE_FLOW";
   public int guiAlpha = 150;
   public boolean guiDarkMode = true;
   public String discordWebhookUrl = "";
   public boolean discordWebhookEnabled = false;
   public boolean webhookNotifyFairy = true;
   public boolean webhookNotifyOgFairy = true;
   public boolean webhookNotifyCrystal = true;
   public boolean webhookNotifyExotic = true;
   public boolean webhookNotifySeymour = true;
   public boolean webhookNotifySkins = true;
   public boolean webhookNotifyBleached = false;
   public boolean webhookNotifyGlitched = true;
   public boolean webhookNotifyOnline = true;
   public boolean notifierEnabled = false;
   public List<String> notifierNames = new ArrayList<>();
   public int notifierIntervalSeconds = 60;
   public boolean useCategoryHighlightColors = true;
   public int slotHighlightAlpha = 136;
   public int highlightCrystalRgb = 6737151;
   public int highlightExoticRgb = 5635925;
   public int highlightOgFairyRgb = 16733695;
   public int highlightFairyRgb = 16746700;
   public int highlightBleachedRgb = 13805177;
   public int highlightGlitchedRgb = 16733525;
   public int highlightSeymourT1Rgb = 5614335;
   public int highlightSeymourT2Rgb = 16777045;
   public int highlightSeymourT3Rgb = 16733525;
   public int highlightSpecificHexRgb = 6750156;
   public boolean highlightCrystalEnabled = true;
   public boolean highlightExoticEnabled = true;
   public boolean highlightOgFairyEnabled = true;
   public boolean highlightFairyEnabled = true;
   public boolean highlightBleachedEnabled = true;
   public boolean highlightGlitchedEnabled = true;
   public boolean highlightSeymourT1Enabled = true;
   public boolean highlightSeymourT2Enabled = true;
   public boolean highlightSeymourT3Enabled = true;
   public boolean highlightSpecificHexEnabled = true;
   private transient String specificHexCacheRaw;
   private transient Set<String> specificHexCache = Collections.emptySet();
   public boolean customTitleScreenEnabled = false;
   public boolean scanWeapons = true;
   public boolean scanArmor = true;
   public boolean scanAccessories = true;
   public boolean scanTools = true;
   public boolean scanPets = true;
   public boolean scanConsumables = false;
   public boolean scanCosmetics = true;
   public boolean scanCosmeticSkinsEnabled = true;
   public double minCosmeticSkinValueMillions = 1.0;
   public String cosmeticSkinScanMode = "BOTH";
   public double skyblockLevelCap = 500.0;
   public double minSkyblockLevel = 0.0;
   public boolean scanLowLevelPlayers = true;
   public Set<String> itemWhitelist = new HashSet<>();
   public Set<String> itemBlacklist = new HashSet<>();
   public boolean useWhitelist = false;
   public boolean scanDungeonItems = true;
   public boolean scanSlayerItems = true;
   public boolean scanEventItems = true;
   public boolean scanAdminItems = true;
   public boolean scanMuseumItems = false;
   public Set<String> customLegacyReforges = new HashSet<>();
   public Set<String> customGhostReforges = new HashSet<>();
   public boolean requireCustomColor = true;
   public int minColorDifference = 50;
   public boolean scanSoulboundItems = false;
   public boolean scanCoopSoulboundItems = false;
   public boolean scanIronmanProfiles = true;
   public boolean scanStrandedProfiles = true;
   public int maxPlayersToScan = 0;
   public int apiDelayMs = 100;
   public boolean skipApiErrorPlayers = true;
   public List<String> automaticMacroSequence = new ArrayList<>();

   public ScannerConfig() {
      this.customLegacyReforges.add("Demonic");
      this.customLegacyReforges.add("Strong");
      this.customLegacyReforges.add("Hurtful");
      this.customLegacyReforges.add("Forceful");
      this.customLegacyReforges.add("Rich");
      this.customLegacyReforges.add("Odd");
      this.customGhostReforges.add("Godly");
      this.customGhostReforges.add("Unpleasant");
      this.customGhostReforges.add("Superior");
      this.customGhostReforges.add("Zealous");
      this.customGhostReforges.add("Keen");
      this.customGhostReforges.add("Strange");
      this.customGhostReforges.add("Shiny");
      this.customGhostReforges.add("Vivid");
   }

   public static ScannerConfig load() {
      Path loadPath = Files.exists(CONFIG_PATH) ? CONFIG_PATH : (Files.exists(LEGACY_CONFIG_PATH) ? LEGACY_CONFIG_PATH : CONFIG_PATH);
      if (Files.exists(loadPath)) {
         try {
            String rawJson = Files.readString(loadPath);
            boolean hasSeymourTierKeys = rawJson.contains("\"seymourScanT1\"") || rawJson.contains("\"seymourScanT2\"");
            boolean hasAhSkinScanningKey = rawJson.contains("\"ahSkinScanningEnabled\"");
            boolean hasAhNotifyOgFairyKey = rawJson.contains("\"ahNotifyOgFairy\"");
            boolean hasAhNotifyBleachedKey = rawJson.contains("\"ahNotifyBleached\"");
            boolean hasHighlightSpecificHexToggleKey = rawJson.contains("\"highlightSpecificHexEnabled\"");
            boolean hasHighlightSpecificHexColorKey = rawJson.contains("\"highlightSpecificHexRgb\"");
            boolean hasGuiDarkModeKey = rawJson.contains("\"guiDarkMode\"");
            boolean hasCakeYearsListModeKey = rawJson.contains("\"cakeYearsListMode\"");
            ScannerConfig config = (ScannerConfig)GSON.fromJson(rawJson, ScannerConfig.class);
            if (config.apiKeys == null) {
               config.apiKeys = new ArrayList<>();
            }

            if (config.apiKeyAddedAt == null) {
               config.apiKeyAddedAt = new ArrayList<>();
            }

            if (config.apiKeyUsages == null) {
               config.apiKeyUsages = new ArrayList<>();
            }

            config.ahAutoBuyEnabled = false;
            config.customTitleScreenEnabled = false;

            if (config.newYearCakeSpecificYears == null) {
               config.newYearCakeSpecificYears = new ArrayList<>();
            }

            if (config.ahWatchTags == null) {
               config.ahWatchTags = new ArrayList<>();
            }

            if (config.automaticMacroSequence == null) {
               config.automaticMacroSequence = new ArrayList<>();
            }

            for (int i = 0; i < config.automaticMacroSequence.size(); i++) {
               String cmd = config.automaticMacroSequence.get(i);
               if (cmd != null && cmd.trim().equalsIgnoreCase("/warp mines")) {
                  config.automaticMacroSequence.set(i, "/warp forge");
               }
            }

            config.seymourScanFadeDyes = false;

            if (config.discordWebhookUrl == null) {
               config.discordWebhookUrl = "";
            }

            if (config.notifierNames == null) {
               config.notifierNames = new ArrayList<>();
            }

            config.notifierIntervalSeconds = Math.max(15, Math.min(600, config.notifierIntervalSeconds));

            if (!hasGuiDarkModeKey) {
               config.guiDarkMode = true;
            }

            config.ahAutoBuyEnabled = false;

            if (config.ahSeymourHexList == null) {
               config.ahSeymourHexList = "";
            }

            if (config.ahSkinMuteList == null) {
               config.ahSkinMuteList = "";
            }

            if (config.ahAutoBuyNameContains == null) {
               config.ahAutoBuyNameContains = "";
            }

            if (config.ahAutoBuyHexList == null) {
               config.ahAutoBuyHexList = "";
            }

            if (config.ahAutoBuyItemPriceRules == null) {
               config.ahAutoBuyItemPriceRules = "";
            }

            if (config.ahProfitPreset == null || config.ahProfitPreset.isBlank()) {
               config.ahProfitPreset = "AGGRESSIVE";
            }

            if (config.ahProfilePreset == null || config.ahProfilePreset.isBlank()) {
               config.ahProfilePreset = "SNIPING";
            }

            if (config.ahHexMatchMode == null || config.ahHexMatchMode.isBlank()) {
               config.ahHexMatchMode = "EXACT";
            }

            if (config.specificHexList == null) {
               config.specificHexList = "";
            }

            if (!hasAhSkinScanningKey) {
               config.ahSkinScanningEnabled = true;
            }

            if (!hasAhNotifyOgFairyKey) {
               config.ahNotifyOgFairy = true;
            }

            if (!hasAhNotifyBleachedKey) {
               config.ahNotifyBleached = true;
            }

            if (!config.ahNotifyArmorSkins && !config.ahNotifyPetSkins && config.ahNotifySkins) {
               config.ahNotifyArmorSkins = true;
            }

            if (config.newYearCakeOnlyYearAndBefore <= 0
               && config.newYearCakeSpecificYears.size() == 3
               && config.newYearCakeSpecificYears.contains(69)
               && config.newYearCakeSpecificYears.contains(100)
               && config.newYearCakeSpecificYears.contains(222)) {
               config.newYearCakeSpecificYears = new ArrayList<>();
            }

            if (!hasCakeYearsListModeKey && (config.newYearCakeSpecificYears == null || config.newYearCakeSpecificYears.isEmpty())) {
               config.newYearCakeSpecificYears = defaultCakeYears();
            }

            config.cakeYearsListMode = true;
            sanitizeCakeYears(config);

            if (config.cosmeticSkinScanMode == null || config.cosmeticSkinScanMode.isEmpty()) {
               config.cosmeticSkinScanMode = "BOTH";
            }

            boolean missingSeymourConfig = config.seymourArmorNameFilters == null
               && config.seymourMaxColorDistance == 0
               && !config.seymourScanTargetColors
               && !config.seymourScanFadeDyes;
            if (missingSeymourConfig) {
               config.seymourScanningEnabled = true;
               config.seymourScanTopHat = true;
               config.seymourScanJacket = true;
               config.seymourScanTrousers = true;
               config.seymourScanShoes = true;
               config.seymourScanTargetColors = true;
               config.seymourScanFadeDyes = false;
               config.seymourScanT1 = true;
               config.seymourScanT2 = true;
               config.seymourExactHexOnly = false;
               config.seymourTargetStage = 0;
               config.seymourStageTolerance = 2;
               config.seymourPieceSpecificEnabled = true;
               config.seymourShowHighFades = false;
               config.seymourMaxColorDistance = 40;
            }

            if (!hasSeymourTierKeys) {
               config.seymourScanT1 = true;
               config.seymourScanT2 = true;
            }

            config.seymourMaxColorDistance = Math.max(0, Math.min(255, config.seymourMaxColorDistance));
            config.seymourTargetStage = Math.max(0, Math.min(200, config.seymourTargetStage));
            config.seymourStageTolerance = Math.max(0, Math.min(50, config.seymourStageTolerance));
            if (config.seymourArmorNameFilters == null || config.seymourArmorNameFilters.trim().isEmpty()) {
               config.seymourArmorNameFilters = "all";
            }

            config.minSkyblockLevel = 0.0;
            config.scanLowLevelPlayers = true;
            if (config.skyblockLevelCap > 0.0 && config.skyblockLevelCap < 500.0) {
               config.skyblockLevelCap = 500.0;
            }

            config.save();

            boolean missingHighlightConfig = config.slotHighlightAlpha == 0
               && config.highlightCrystalRgb == 0
               && config.highlightExoticRgb == 0
               && config.highlightOgFairyRgb == 0
               && config.highlightFairyRgb == 0
               && config.highlightBleachedRgb == 0
               && config.highlightGlitchedRgb == 0
               && config.highlightSeymourT1Rgb == 0
               && config.highlightSeymourT2Rgb == 0
               && config.highlightSeymourT3Rgb == 0
               && config.highlightSpecificHexRgb == 0;
            if (missingHighlightConfig) {
               config.useCategoryHighlightColors = true;
               config.slotHighlightAlpha = 136;
               config.highlightCrystalRgb = 6737151;
               config.highlightExoticRgb = 5635925;
               config.highlightOgFairyRgb = 16733695;
               config.highlightFairyRgb = 16746700;
               config.highlightBleachedRgb = 13805177;
               config.highlightGlitchedRgb = 16733525;
               config.highlightSeymourT1Rgb = 5614335;
               config.highlightSeymourT2Rgb = 16777045;
               config.highlightSeymourT3Rgb = 16733525;
               config.highlightSpecificHexRgb = 6750156;
            }

            boolean missingHighlightToggleConfig = !config.highlightCrystalEnabled
               && !config.highlightExoticEnabled
               && !config.highlightOgFairyEnabled
               && !config.highlightFairyEnabled
               && !config.highlightBleachedEnabled
               && !config.highlightGlitchedEnabled
               && !config.highlightSeymourT1Enabled
               && !config.highlightSeymourT2Enabled
               && !config.highlightSeymourT3Enabled
               && !config.highlightSpecificHexEnabled;
            if (missingHighlightToggleConfig) {
               config.highlightCrystalEnabled = true;
               config.highlightExoticEnabled = true;
               config.highlightOgFairyEnabled = true;
               config.highlightFairyEnabled = true;
               config.highlightBleachedEnabled = true;
               config.highlightGlitchedEnabled = true;
               config.highlightSeymourT1Enabled = true;
               config.highlightSeymourT2Enabled = true;
               config.highlightSeymourT3Enabled = true;
               config.highlightSpecificHexEnabled = true;
            }

            config.slotHighlightAlpha = Math.max(0, Math.min(255, config.slotHighlightAlpha));
            config.highlightCrystalRgb = sanitizeRgbInt(config.highlightCrystalRgb, 6737151);
            config.highlightExoticRgb = sanitizeRgbInt(config.highlightExoticRgb, 5635925);
            config.highlightOgFairyRgb = sanitizeRgbInt(config.highlightOgFairyRgb, 16733695);
            config.highlightFairyRgb = sanitizeRgbInt(config.highlightFairyRgb, 16746700);
            config.highlightBleachedRgb = sanitizeRgbInt(config.highlightBleachedRgb, 13805177);
            config.highlightGlitchedRgb = sanitizeRgbInt(config.highlightGlitchedRgb, 16733525);
            config.highlightSeymourT1Rgb = sanitizeRgbInt(config.highlightSeymourT1Rgb, 5614335);
            config.highlightSeymourT2Rgb = sanitizeRgbInt(config.highlightSeymourT2Rgb, 16777045);
            config.highlightSeymourT3Rgb = sanitizeRgbInt(config.highlightSeymourT3Rgb, 16733525);
            config.highlightSpecificHexRgb = sanitizeRgbInt(config.highlightSpecificHexRgb, 6750156);
            if (!hasHighlightSpecificHexToggleKey) {
               config.highlightSpecificHexEnabled = true;
            }

            if (!hasHighlightSpecificHexColorKey) {
               config.highlightSpecificHexRgb = 6750156;
            }

            config.ahPollIntervalSeconds = Math.max(1, Math.min(60, config.ahPollIntervalSeconds));
            config.ahTagsPerPoll = Math.max(1, Math.min(30, config.ahTagsPerPoll));
            config.ahMaxAlertsPerPoll = Math.max(1, Math.min(20, config.ahMaxAlertsPerPoll));
            config.ahSkinMinValueMillions = Math.max(0, Math.min(50000, config.ahSkinMinValueMillions));
            config.ahSkinMaxValueMillions = Math.max(0, Math.min(50000, config.ahSkinMaxValueMillions));
            config.ahAutoBuyMaxPriceMillions = Math.max(0, Math.min(50000, config.ahAutoBuyMaxPriceMillions));
            config.ahAutoBuyCooldownSeconds = Math.max(0, Math.min(60, config.ahAutoBuyCooldownSeconds));
            config.ahSnipeMinScore = Math.max(0, Math.min(100, config.ahSnipeMinScore));
            config.ahDuplicateSuppressSeconds = Math.max(0, Math.min(300, config.ahDuplicateSuppressSeconds));
            config.ahHexDistanceMax = Math.max(0, Math.min(441, config.ahHexDistanceMax));
            config.ahHexStageTolerance = Math.max(0, Math.min(50, config.ahHexStageTolerance));
            config.ahProfitPreset = sanitizeMode(config.ahProfitPreset, "AGGRESSIVE", "SAFE", "AGGRESSIVE", "SNIPE_ONLY");
            config.ahProfilePreset = sanitizeMode(config.ahProfilePreset, "SNIPING", "SAFE", "SNIPING", "COLLECTING");
            config.ahHexMatchMode = sanitizeMode(config.ahHexMatchMode, "EXACT", "EXACT", "WITHIN_DELTA", "STAGE_TOLERANCE");
            if (config.ahSkinMaxValueMillions > 0 && config.ahSkinMinValueMillions > config.ahSkinMaxValueMillions) {
               config.ahSkinMinValueMillions = config.ahSkinMaxValueMillions;
            }

            sanitizeAhWatchTags(config);
            sanitizeApiKeys(config);
            ensureApiKeyTimestamps(config);
            PotatoToolMod.LOGGER.info("Loaded configuration (" + config.apiKeys.size() + " API key(s))");
            return config;
         } catch (IOException e) {
            PotatoToolMod.LOGGER.error("Failed to load config", e);
         }
      }

      ScannerConfig config = new ScannerConfig();
      config.save();
      return config;
   }

   private static void sanitizeApiKeys(ScannerConfig config) {
      if (config.apiKeys != null) {
         for (int i = 0; i < config.apiKeys.size(); i++) {
            String k = config.apiKeys.get(i);
            if (k != null) {
               String s = k.trim().replaceAll("\\s+", "");
               if (!s.equals(k)) {
                  config.apiKeys.set(i, s);
               }
            }
         }
      }
   }

   private static void sanitizeAhWatchTags(ScannerConfig config) {
      if (config.ahWatchTags != null) {
         List<String> clean = new ArrayList<>();

         for (String raw : config.ahWatchTags) {
            if (raw != null) {
               String s = raw.trim().toUpperCase().replace(' ', '_');
               if (!s.isEmpty() && !clean.contains(s)) {
                  clean.add(s);
               }
            }
         }

         config.ahWatchTags = clean;
      }
   }

   public static String normalizeHexToken(String raw) {
      if (raw == null) {
         return null;
      }

      String s = raw.trim().toUpperCase().replace("#", "");
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

   public static List<Integer> defaultCakeYears() {
      TreeSet<Integer> years = new TreeSet<>();
      for (int year = 1; year <= 25; year++) {
         years.add(year);
      }

      years.add(67);
      years.add(69);
      years.add(100);
      years.add(200);
      years.add(300);
      years.add(400);
      years.add(500);
      return new ArrayList<>(years);
   }

   public static void sanitizeCakeYears(ScannerConfig config) {
      if (config == null) {
         return;
      }

      TreeSet<Integer> clean = new TreeSet<>();
      if (config.newYearCakeSpecificYears != null) {
         for (Integer year : config.newYearCakeSpecificYears) {
            if (year != null && year >= 1 && year <= 500) {
               clean.add(year);
            }
         }
      }

      config.newYearCakeSpecificYears = new ArrayList<>(clean);
   }

   public boolean wantsCakeYear(int year) {
      return this.newYearCakeEnabled && this.newYearCakeSpecificYears != null && this.newYearCakeSpecificYears.contains(year);
   }

   public boolean addCakeYear(int year) {
      if (year < 1 || year > 500) {
         return false;
      }

      if (this.newYearCakeSpecificYears == null) {
         this.newYearCakeSpecificYears = new ArrayList<>();
      }

      if (this.newYearCakeSpecificYears.contains(year)) {
         return false;
      }

      this.newYearCakeSpecificYears.add(year);
      sanitizeCakeYears(this);
      this.cakeYearsListMode = true;
      return true;
   }

   public boolean removeCakeYear(int year) {
      if (this.newYearCakeSpecificYears == null) {
         return false;
      }

      boolean removed = this.newYearCakeSpecificYears.remove(Integer.valueOf(year));
      if (removed) {
         this.cakeYearsListMode = true;
      }

      return removed;
   }

   public void resetCakeYearsToDefaults() {
      this.newYearCakeSpecificYears = defaultCakeYears();
      this.cakeYearsListMode = true;
   }

   public static Set<String> parseHexList(String raw) {
      LinkedHashSet<String> out = new LinkedHashSet<>();
      if (raw != null && !raw.isBlank()) {
         for (String part : raw.split("[,;\\s]+")) {
            String h = normalizeHexToken(part);
            if (h != null) {
               out.add(h);
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public Set<String> getSpecificHexSet() {
      String raw = this.specificHexList == null ? "" : this.specificHexList;
      if (this.specificHexCacheRaw == null || !this.specificHexCacheRaw.equals(raw)) {
         this.specificHexCacheRaw = raw;
         this.specificHexCache = parseHexList(raw);
      }

      return this.specificHexCache;
   }

   public boolean matchesSpecificHex(String hexColor) {
      if (!this.specificHexScanningEnabled) {
         return false;
      }

      String h = normalizeHexToken(hexColor);
      return h != null && this.getSpecificHexSet().contains(h);
   }

   private static int sanitizeRgbInt(int raw, int fallback) {
      int rgb = raw & 16777215;
      int fb = fallback & 16777215;
      return rgb >= 0 && rgb <= 16777215 ? rgb : fb;
   }

   private static String sanitizeMode(String raw, String fallback, String... allowed) {
      if (raw != null && !raw.isBlank()) {
         String v = raw.trim().toUpperCase(Locale.ROOT);

         for (String a : allowed) {
            if (a.equals(v)) {
               return v;
            }
         }

         return fallback;
      } else {
         return fallback;
      }
   }

   public static void ensureApiKeyTimestamps(ScannerConfig config) {
      if (config.apiKeys == null) {
         config.apiKeys = new ArrayList<>();
      }

      if (config.apiKeyAddedAt == null) {
         config.apiKeyAddedAt = new ArrayList<>();
      }

      if (config.apiKeyUsages == null) {
         config.apiKeyUsages = new ArrayList<>();
      }

      int n = config.apiKeys.size();
      while (config.apiKeyAddedAt.size() < n) {
         config.apiKeyAddedAt.add(0L);
      }

      while (config.apiKeyAddedAt.size() > n) {
         config.apiKeyAddedAt.remove(config.apiKeyAddedAt.size() - 1);
      }

      while (config.apiKeyUsages.size() < n) {
         config.apiKeyUsages.add(0);
      }

      while (config.apiKeyUsages.size() > n) {
         config.apiKeyUsages.remove(config.apiKeyUsages.size() - 1);
      }
   }

   private Map<String, Deque<Long>> apiWindows() {
      if (this.apiKeyWindows == null) {
         this.apiKeyWindows = new ConcurrentHashMap<>();
      }

      return this.apiKeyWindows;
   }

   private Deque<Long> windowFor(String key) {
      return this.apiWindows().computeIfAbsent(key, k -> new ArrayDeque<>());
   }

   private void pruneApiWindow(Deque<Long> window, long now) {
      while (!window.isEmpty() && now - window.peekFirst() >= API_WINDOW_MS) {
         window.removeFirst();
      }
   }

   public synchronized int getApiWindowUsed(String key) {
      if (key == null || key.isBlank()) {
         return 0;
      }

      long now = System.currentTimeMillis();
      Deque<Long> window = this.windowFor(key);
      this.pruneApiWindow(window, now);
      return window.size();
   }

   public synchronized int getApiWindowRemaining(String key) {
      return Math.max(0, API_WINDOW_LIMIT - this.getApiWindowUsed(key));
   }

   public synchronized boolean hasApiWindowCapacity() {
      if (this.apiKeys == null || this.apiKeys.isEmpty()) {
         return false;
      }

      for (String key : this.apiKeys) {
         if (key != null && this.getApiWindowRemaining(key) > 0) {
            return true;
         }
      }

      return false;
   }

   public synchronized long millisUntilApiWindowOpens() {
      if (this.hasApiWindowCapacity()) {
         return 0L;
      }

      if (this.apiKeys == null || this.apiKeys.isEmpty()) {
         return API_WINDOW_MS;
      }

      long now = System.currentTimeMillis();
      long wait = Long.MAX_VALUE;
      for (String key : this.apiKeys) {
         if (key == null) {
            continue;
         }

         Deque<Long> window = this.windowFor(key);
         this.pruneApiWindow(window, now);
         if (window.size() < API_WINDOW_LIMIT) {
            return 0L;
         }

         Long oldest = window.peekFirst();
         if (oldest != null) {
            wait = Math.min(wait, oldest + API_WINDOW_MS - now);
         }
      }

      return wait == Long.MAX_VALUE ? API_WINDOW_MS : Math.max(0L, wait);
   }

   public synchronized boolean recordApiKeyUse(String key) {
      if (key == null || key.isBlank() || this.apiKeys == null) {
         return false;
      }

      ensureApiKeyTimestamps(this);
      int idx = -1;
      for (int i = 0; i < this.apiKeys.size(); i++) {
         if (key.equals(this.apiKeys.get(i))) {
            idx = i;
            break;
         }
      }

      if (idx < 0) {
         return false;
      }

      long now = System.currentTimeMillis();
      Deque<Long> window = this.windowFor(key);
      this.pruneApiWindow(window, now);
      if (window.size() >= API_WINDOW_LIMIT) {
         return false;
      }

      window.addLast(now);
      int used = Math.max(0, this.apiKeyUsages.get(idx));
      if (used < API_KEY_USAGE_LIMIT) {
         this.apiKeyUsages.set(idx, used + 1);
      }

      if (now - this.lastUsageSaveMs >= 4000L || used + 1 >= API_KEY_USAGE_LIMIT) {
         this.lastUsageSaveMs = now;
         this.save();
      }

      return true;
   }

   public synchronized int getApiKeyRemaining(int index) {
      ensureApiKeyTimestamps(this);
      if (index < 0 || index >= this.apiKeys.size()) {
         return 0;
      }

      int used = index < this.apiKeyUsages.size() ? Math.max(0, this.apiKeyUsages.get(index)) : 0;
      return Math.max(0, API_KEY_USAGE_LIMIT - used);
   }

   public synchronized int getApiKeyRemaining(String key) {
      if (key == null || this.apiKeys == null) {
         return 0;
      }

      for (int i = 0; i < this.apiKeys.size(); i++) {
         if (key.equals(this.apiKeys.get(i))) {
            return this.getApiKeyRemaining(i);
         }
      }

      return 0;
   }

   public void save() {
      try {
         Files.createDirectories(CONFIG_PATH.getParent());

         try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
         }
      } catch (IOException e) {
         PotatoToolMod.LOGGER.error("Failed to save config", e);
      }
   }

   public synchronized String getRandomApiKey() {
      if (this.apiKeys != null && !this.apiKeys.isEmpty()) {
         ensureApiKeyTimestamps(this);
         List<Integer> usable = new ArrayList<>();
         for (int i = 0; i < this.apiKeys.size(); i++) {
            String key = this.apiKeys.get(i);
            if (this.getApiWindowRemaining(key) > 0) {
               usable.add(i);
            }
         }

         if (usable.isEmpty()) {
            return null;
         }

         String k = this.apiKeys.get(usable.get(new Random().nextInt(usable.size())));
         return k != null ? k.trim().replaceAll("\\s+", "") : null;
      } else {
         return null;
      }
   }

   public synchronized void addApiKey(String key) {
      if (this.apiKeys == null) {
         this.apiKeys = new ArrayList<>();
      }

      if (this.apiKeyAddedAt == null) {
         this.apiKeyAddedAt = new ArrayList<>();
      }

      if (this.apiKeyUsages == null) {
         this.apiKeyUsages = new ArrayList<>();
      }

      if (key != null) {
         key = key.trim().replaceAll("\\s+", "");
         if (!key.isEmpty()) {
            this.apiKeys.add(key);
            this.apiKeyAddedAt.add(System.currentTimeMillis());
            this.apiKeyUsages.add(0);
         }
      }
   }

   public synchronized void removeApiKey(int index) {
      if (index >= 0 && index < this.apiKeys.size()) {
         this.apiKeys.remove(index);
         if (this.apiKeyAddedAt != null && index < this.apiKeyAddedAt.size()) {
            this.apiKeyAddedAt.remove(index);
         }

         if (this.apiKeyUsages != null && index < this.apiKeyUsages.size()) {
            this.apiKeyUsages.remove(index);
         }
      }
   }

   public static String getKeyActiveDuration(long addedAtMs) {
      if (addedAtMs <= 0L) {
         return "Unknown";
      }

      long ms = System.currentTimeMillis() - addedAtMs;
      if (ms < 60000L) {
         return ms / 1000L + "s";
      }

      if (ms < 3600000L) {
         return ms / 60000L + "m";
      }

      if (ms < 86400000L) {
         return ms / 3600000L + "h";
      }

      long days = ms / 86400000L;
      return days < 365L ? days + "d" : days / 365L + "y";
   }

   public int getApiKeyCount() {
      return this.apiKeys == null ? 0 : this.apiKeys.size();
   }
}
