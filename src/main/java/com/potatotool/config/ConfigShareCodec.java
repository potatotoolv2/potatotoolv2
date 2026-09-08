package com.potatotool.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ConfigShareCodec {
   public static final String PREFIX = "PT2-";
   private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

   private ConfigShareCodec() {
   }

   public static String exportCode(ScannerConfig config) {
      Payload payload = Payload.from(config);
      byte[] json = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
      byte[] gzipped = gzip(json);
      return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(gzipped);
   }

   public static String importCode(ScannerConfig config, String raw) {
      if (config == null) {
         return "Config not loaded";
      }

      String code = normalize(raw);
      if (code.isEmpty()) {
         return "Paste a PT2 share code first";
      }

      try {
         byte[] decoded = Base64.getUrlDecoder().decode(code);
         byte[] json = gunzip(decoded);
         Payload payload = GSON.fromJson(new String(json, StandardCharsets.UTF_8), Payload.class);
         if (payload == null || payload.v != 1) {
            return "Unknown share code version";
         }

         payload.applyTo(config);
         ScannerConfig.sanitizeCakeYears(config);
         config.save();
         return null;
      } catch (Exception e) {
         return "Invalid share code";
      }
   }

   public static String normalize(String raw) {
      if (raw == null) {
         return "";
      }

      String s = raw.trim().replaceAll("\\s+", "");
      if (s.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
         s = s.substring(PREFIX.length());
      }

      return s;
   }

   private static byte[] gzip(byte[] data) {
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
         }

         return out.toByteArray();
      } catch (Exception e) {
         throw new IllegalStateException("Failed to pack share code", e);
      }
   }

   private static byte[] gunzip(byte[] data) {
      try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
         return gzip.readAllBytes();
      } catch (Exception e) {
         throw new IllegalArgumentException("Failed to unpack share code", e);
      }
   }

   public static final class Payload {
      int v = 1;
      Boolean crystal;
      Boolean bleached;
      Boolean ogFairy;
      Boolean fairy;
      Boolean exotic;
      Boolean cakes;
      List<Integer> cakeYears;
      Boolean legacy;
      Boolean ghost;
      Boolean valuable;
      Boolean hats;
      Boolean tracers;
      Boolean overlay;
      Boolean skins;
      Double skinMin;
      String skinMode;
      Boolean hexOn;
      String hexes;
      Boolean weapons;
      Boolean armor;
      Boolean accessories;
      Boolean tools;
      Boolean pets;
      Boolean consumables;
      Boolean cosmetics;
      Boolean dungeon;
      Boolean slayer;
      Boolean event;
      Boolean admin;
      Double minLvl;
      Double maxLvl;
      Integer maxPlayers;
      Boolean lowLvl;
      Boolean soulbound;
      Boolean coopSb;
      Boolean ironman;
      Boolean stranded;
      Boolean reqColor;
      Integer minColorDiff;
      Boolean seyOn;
      Boolean seyHat;
      Boolean seyJacket;
      Boolean seyTrousers;
      Boolean seyShoes;
      Boolean seyT1;
      Boolean seyT2;
      Boolean seyFade;
      Boolean seyTarget;
      Boolean seyExact;
      List<String> autoSeq;

      static Payload from(ScannerConfig c) {
         Payload p = new Payload();
         p.crystal = c.crystalScanningEnabled;
         p.bleached = c.bleachedScanningEnabled;
         p.ogFairy = c.ogFairyScanningEnabled;
         p.fairy = c.fairyScanningEnabled;
         p.exotic = c.exoticScanningEnabled;
         p.cakes = c.newYearCakeEnabled;
         p.cakeYears = c.newYearCakeSpecificYears == null ? new ArrayList<>() : new ArrayList<>(c.newYearCakeSpecificYears);
         p.legacy = c.legacyReforgeEnabled;
         p.ghost = c.ghostReforgeEnabled;
         p.valuable = c.valuableItemsEnabled;
         p.hats = c.scanAnniversaryHats;
         p.tracers = c.renderItemTracers;
         p.overlay = c.showPlayerOverlay;
         p.skins = c.scanCosmeticSkinsEnabled;
         p.skinMin = c.minCosmeticSkinValueMillions;
         p.skinMode = c.cosmeticSkinScanMode;
         p.hexOn = c.specificHexScanningEnabled;
         p.hexes = c.specificHexList;
         p.weapons = c.scanWeapons;
         p.armor = c.scanArmor;
         p.accessories = c.scanAccessories;
         p.tools = c.scanTools;
         p.pets = c.scanPets;
         p.consumables = c.scanConsumables;
         p.cosmetics = c.scanCosmetics;
         p.dungeon = c.scanDungeonItems;
         p.slayer = c.scanSlayerItems;
         p.event = c.scanEventItems;
         p.admin = c.scanAdminItems;
         p.minLvl = c.minSkyblockLevel;
         p.maxLvl = c.skyblockLevelCap;
         p.maxPlayers = c.maxPlayersToScan;
         p.lowLvl = c.scanLowLevelPlayers;
         p.soulbound = c.scanSoulboundItems;
         p.coopSb = c.scanCoopSoulboundItems;
         p.ironman = c.scanIronmanProfiles;
         p.stranded = c.scanStrandedProfiles;
         p.reqColor = c.requireCustomColor;
         p.minColorDiff = c.minColorDifference;
         p.seyOn = c.seymourScanningEnabled;
         p.seyHat = c.seymourScanTopHat;
         p.seyJacket = c.seymourScanJacket;
         p.seyTrousers = c.seymourScanTrousers;
         p.seyShoes = c.seymourScanShoes;
         p.seyT1 = c.seymourScanT1;
         p.seyT2 = c.seymourScanT2;
         p.seyFade = c.seymourScanFadeDyes;
         p.seyTarget = c.seymourScanTargetColors;
         p.seyExact = c.seymourExactHexOnly;
         p.autoSeq = c.automaticMacroSequence == null ? new ArrayList<>() : new ArrayList<>(c.automaticMacroSequence);
         return p;
      }

      void applyTo(ScannerConfig c) {
         if (this.crystal != null) {
            c.crystalScanningEnabled = this.crystal;
         }

         if (this.bleached != null) {
            c.bleachedScanningEnabled = this.bleached;
         }

         if (this.ogFairy != null) {
            c.ogFairyScanningEnabled = this.ogFairy;
         }

         if (this.fairy != null) {
            c.fairyScanningEnabled = this.fairy;
         }

         if (this.exotic != null) {
            c.exoticScanningEnabled = this.exotic;
         }

         if (this.cakes != null) {
            c.newYearCakeEnabled = this.cakes;
         }

         if (this.cakeYears != null) {
            c.newYearCakeSpecificYears = new ArrayList<>(this.cakeYears);
            c.cakeYearsListMode = true;
         }

         if (this.legacy != null) {
            c.legacyReforgeEnabled = this.legacy;
         }

         if (this.ghost != null) {
            c.ghostReforgeEnabled = this.ghost;
         }

         if (this.valuable != null) {
            c.valuableItemsEnabled = this.valuable;
         }

         if (this.hats != null) {
            c.scanAnniversaryHats = this.hats;
         }

         if (this.tracers != null) {
            c.renderItemTracers = this.tracers;
         }

         if (this.overlay != null) {
            c.showPlayerOverlay = this.overlay;
         }

         if (this.skins != null) {
            c.scanCosmeticSkinsEnabled = this.skins;
         }

         if (this.skinMin != null) {
            c.minCosmeticSkinValueMillions = this.skinMin;
         }

         if (this.skinMode != null && !this.skinMode.isBlank()) {
            c.cosmeticSkinScanMode = this.skinMode.toUpperCase(Locale.ROOT);
         }

         if (this.hexOn != null) {
            c.specificHexScanningEnabled = this.hexOn;
         }

         if (this.hexes != null) {
            c.specificHexList = this.hexes;
         }

         if (this.weapons != null) {
            c.scanWeapons = this.weapons;
         }

         if (this.armor != null) {
            c.scanArmor = this.armor;
         }

         if (this.accessories != null) {
            c.scanAccessories = this.accessories;
         }

         if (this.tools != null) {
            c.scanTools = this.tools;
         }

         if (this.pets != null) {
            c.scanPets = this.pets;
         }

         if (this.consumables != null) {
            c.scanConsumables = this.consumables;
         }

         if (this.cosmetics != null) {
            c.scanCosmetics = this.cosmetics;
         }

         if (this.dungeon != null) {
            c.scanDungeonItems = this.dungeon;
         }

         if (this.slayer != null) {
            c.scanSlayerItems = this.slayer;
         }

         if (this.event != null) {
            c.scanEventItems = this.event;
         }

         if (this.admin != null) {
            c.scanAdminItems = this.admin;
         }

         if (this.minLvl != null) {
            c.minSkyblockLevel = this.minLvl;
         }

         if (this.maxLvl != null) {
            c.skyblockLevelCap = this.maxLvl;
         }

         if (this.maxPlayers != null) {
            c.maxPlayersToScan = this.maxPlayers;
         }

         if (this.lowLvl != null) {
            c.scanLowLevelPlayers = this.lowLvl;
         }

         if (this.soulbound != null) {
            c.scanSoulboundItems = this.soulbound;
         }

         if (this.coopSb != null) {
            c.scanCoopSoulboundItems = this.coopSb;
         }

         if (this.ironman != null) {
            c.scanIronmanProfiles = this.ironman;
         }

         if (this.stranded != null) {
            c.scanStrandedProfiles = this.stranded;
         }

         if (this.reqColor != null) {
            c.requireCustomColor = this.reqColor;
         }

         if (this.minColorDiff != null) {
            c.minColorDifference = this.minColorDiff;
         }

         if (this.seyOn != null) {
            c.seymourScanningEnabled = this.seyOn;
         }

         if (this.seyHat != null) {
            c.seymourScanTopHat = this.seyHat;
         }

         if (this.seyJacket != null) {
            c.seymourScanJacket = this.seyJacket;
         }

         if (this.seyTrousers != null) {
            c.seymourScanTrousers = this.seyTrousers;
         }

         if (this.seyShoes != null) {
            c.seymourScanShoes = this.seyShoes;
         }

         if (this.seyT1 != null) {
            c.seymourScanT1 = this.seyT1;
         }

         if (this.seyT2 != null) {
            c.seymourScanT2 = this.seyT2;
         }

         if (this.seyFade != null) {
            c.seymourScanFadeDyes = this.seyFade;
         }

         if (this.seyTarget != null) {
            c.seymourScanTargetColors = this.seyTarget;
         }

         if (this.seyExact != null) {
            c.seymourExactHexOnly = this.seyExact;
         }

         if (this.autoSeq != null) {
            c.automaticMacroSequence = new ArrayList<>(this.autoSeq);
         }
      }
   }
}
