package com.potatotool.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScannedPlayer {
   private final String username;
   private final String uuid;
   private double skyblockLevel;
   private String selectedProfile;
   private String rank;
   private final List<ScannedItem> items;
   private long scanTimestamp;
   private long lastLoginTimestamp;
   private long lastLogoutTimestamp;
   private int seymourPieceCount;
   private final List<ProfileInfo> profiles;
   private String currentScanProfile;

   public ScannedPlayer(String username, String uuid) {
      this.username = username;
      this.uuid = uuid;
      this.items = new ArrayList<>();
      this.profiles = new ArrayList<>();
      this.scanTimestamp = System.currentTimeMillis();
      this.rank = null;
   }

   public String getUsername() {
      return this.username;
   }

   public String getUuid() {
      return this.uuid;
   }

   public double getSkyblockLevel() {
      return this.skyblockLevel;
   }

   public void setSkyblockLevel(double skyblockLevel) {
      this.skyblockLevel = skyblockLevel;
   }

   public String getSelectedProfile() {
      return this.selectedProfile;
   }

   public void setSelectedProfile(String selectedProfile) {
      this.selectedProfile = selectedProfile;
   }

   public String getRank() {
      return this.rank;
   }

   public void setRank(String rank) {
      this.rank = rank;
   }

   public long getLastLoginTimestamp() {
      return this.lastLoginTimestamp;
   }

   public void setLastLoginTimestamp(long lastLoginTimestamp) {
      this.lastLoginTimestamp = lastLoginTimestamp;
   }

   public long getLastLogoutTimestamp() {
      return this.lastLogoutTimestamp;
   }

   public void setLastLogoutTimestamp(long lastLogoutTimestamp) {
      this.lastLogoutTimestamp = lastLogoutTimestamp;
   }

   public boolean isOnlineAtScanTime() {
      return this.lastLoginTimestamp > 0L && this.lastLogoutTimestamp > 0L && this.lastLoginTimestamp > this.lastLogoutTimestamp;
   }

   public String getLastLoginDisplayString() {
      if (this.lastLoginTimestamp <= 0L) {
         return "-";
      } else {
         long now = System.currentTimeMillis();
         long diff = now - this.lastLoginTimestamp;
         if (diff < 0L) {
            return "-";
         } else {
            long sec = diff / 1000L;
            long min = sec / 60L;
            long hr = min / 60L;
            long day = hr / 24L;
            if (day > 30L) {
               Instant instant = Instant.ofEpochMilli(this.lastLoginTimestamp);
               return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d"));
            } else if (day > 0L) {
               return day + "d ago";
            } else if (hr > 0L) {
               return hr + "h ago";
            } else {
               return min > 0L ? min + "m ago" : "just now";
            }
         }
      }
   }

   public String getRankFormatted() {
      if (this.rank != null && !this.rank.isEmpty()) {
         switch (this.rank.toUpperCase()) {
            case "VIP":
               return "§a[VIP]";
            case "VIP_PLUS":
               return "§a[VIP§6+§a]";
            case "MVP":
               return "§b[MVP]";
            case "MVP_PLUS":
               return "§b[MVP§c+§b]";
            case "MVP_PLUS_PLUS":
               return "§6[MVP§c++§6]";
            case "YOUTUBER":
               return "§c[§fYOUTUBE§c]";
            case "ADMIN":
               return "§c[ADMIN]";
            case "MODERATOR":
               return "§2[MOD]";
            case "HELPER":
               return "§9[HELPER]";
            default:
               return "§7[" + this.rank + "]";
         }
      } else {
         return "";
      }
   }

   public String getRankNameColor() {
      if (this.rank != null && !this.rank.isEmpty()) {
         switch (this.rank.toUpperCase()) {
            case "VIP":
            case "VIP_PLUS":
               return "§a";
            case "MVP":
            case "MVP_PLUS":
            case "MVP_PLUS_PLUS":
               return "§b";
            case "YOUTUBER":
               return "§f";
            case "ADMIN":
               return "§c";
            case "MODERATOR":
               return "§2";
            case "HELPER":
               return "§9";
            default:
               return "§f";
         }
      } else {
         return "§f";
      }
   }

   public List<ScannedItem> getItems() {
      return this.items;
   }

   public void addItem(ScannedItem item) {
      this.items.add(item);
   }

   public void addSpecialItem(ScannedItem item) {
      if (item != null && item.isSpecial()) {
         if ((item.getProfileName() == null || item.getProfileName().isBlank()) && this.currentScanProfile != null) {
            item.setProfileName(this.currentScanProfile);
         }

         this.items.add(item);
      }
   }

   public void setCurrentScanProfile(String currentScanProfile) {
      this.currentScanProfile = currentScanProfile;
   }

   public List<ProfileInfo> getProfiles() {
      return this.profiles;
   }

   public void addProfile(ProfileInfo profile) {
      if (profile != null) {
         this.profiles.add(profile);
      }
   }

   public static final class ProfileInfo {
      private final String name;
      private final int level;
      private final boolean selected;
      private final String gameMode;
      private final boolean apiDisabled;

      public ProfileInfo(String name, int level, boolean selected, String gameMode) {
         this(name, level, selected, gameMode, false);
      }

      public ProfileInfo(String name, int level, boolean selected, String gameMode, boolean apiDisabled) {
         this.name = name;
         this.level = level;
         this.selected = selected;
         this.gameMode = gameMode;
         this.apiDisabled = apiDisabled;
      }

      public String getName() {
         return this.name;
      }

      public int getLevel() {
         return this.level;
      }

      public boolean isSelected() {
         return this.selected;
      }

      public String getGameMode() {
         return this.gameMode;
      }

      /** True when this profile's inventory API is off, so its items could not be read. */
      public boolean isApiDisabled() {
         return this.apiDisabled;
      }
   }

   public int getHighestLevel() {
      int highest = 0;

      for (ProfileInfo profile : this.profiles) {
         highest = Math.max(highest, profile.getLevel());
      }

      return highest;
   }

   public int getApiDisabledProfileCount() {
      int count = 0;

      for (ProfileInfo profile : this.profiles) {
         if (profile.isApiDisabled()) {
            count++;
         }
      }

      return count;
   }

   public boolean hasSpecialItems() {
      return this.items.stream().anyMatch(ScannedItem::isSpecial);
   }

   public boolean hasMentionableItems() {
      return this.items.stream().anyMatch(item -> item != null && item.isMentionable());
   }

   public int getMentionableItemCount() {
      return (int)this.items.stream().filter(ScannedItem::isMentionable).count();
   }

   public boolean hasCakeItems() {
      return this.items.stream().anyMatch(item -> item != null && item.getCategory() == ScannedItem.ItemCategory.CAKE);
   }

   public int getCakeItemCount() {
      return (int)this.items.stream().filter(item -> item != null && item.getCategory() == ScannedItem.ItemCategory.CAKE).count();
   }

   public boolean hasCosmeticSkinItems() {
      return this.items.stream().anyMatch(item -> item != null && item.getCategory() == ScannedItem.ItemCategory.COSMETIC_SKIN);
   }

   public int getCosmeticSkinItemCount() {
      return (int)this.items.stream().filter(item -> item != null && item.getCategory() == ScannedItem.ItemCategory.COSMETIC_SKIN).count();
   }

   public boolean hasDisplayableItems() {
      return this.hasMentionableItems() || this.hasCakeItems() || this.hasCosmeticSkinItems();
   }

   public int getDisplayableItemCount() {
      return this.getDisplayableItems().size();
   }

   public List<ScannedItem> getDisplayableItems() {
      List<ScannedItem> out = new ArrayList<>();
      for (ScannedItem item : this.items) {
         if (item != null
            && (
               item.isMentionable()
                  || item.getCategory() == ScannedItem.ItemCategory.CAKE
                  || item.getCategory() == ScannedItem.ItemCategory.COSMETIC_SKIN
            )) {
            out.add(item);
         }
      }

      return out;
   }

   public int getSpecialItemCount() {
      return (int)this.items.stream().filter(ScannedItem::isSpecial).count();
   }

   public void addSeymourPiece() {
      this.seymourPieceCount++;
   }

   public int getSeymourPieceCount() {
      return this.seymourPieceCount;
   }

   public int getSeymourRareCount() {
      return (int)this.items.stream().filter(item -> item != null && item.isSeymour()).count();
   }
}
