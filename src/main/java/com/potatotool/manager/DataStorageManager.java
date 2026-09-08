package com.potatotool.manager;

import com.potatotool.config.ScannerConfig;
import com.potatotool.model.ScannedItem;
import com.potatotool.model.ScannedPlayer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class DataStorageManager {
   private final ScannerConfig config;
   private final Map<String, ScannedPlayer> scannedPlayers;
   private final List<ScannedItem> islandItems;
   private volatile Set<String> lastScannedLobbyTabNames = new HashSet<>();

   public DataStorageManager(ScannerConfig config) {
      this.config = config;
      this.scannedPlayers = new ConcurrentHashMap<>();
      this.islandItems = new ArrayList<>();
   }

   public void setLastScannedLobbyTabNames(Set<String> names) {
      this.lastScannedLobbyTabNames = names == null ? new HashSet<>() : new HashSet<>(names);
   }

   public Set<String> getLastScannedLobbyTabNames() {
      Set<String> s = this.lastScannedLobbyTabNames;
      return s == null ? Collections.emptySet() : new HashSet<>(s);
   }

   public void clearLastScannedLobbyTabNames() {
      this.lastScannedLobbyTabNames = new HashSet<>();
   }

   public void addScannedPlayer(ScannedPlayer player) {
      this.scannedPlayers.put(player.getUuid(), player);
   }

   public void clearScannedPlayers() {
      this.scannedPlayers.clear();
   }

   public Collection<ScannedPlayer> getAllScannedPlayers() {
      return this.scannedPlayers.values();
   }

   public List<ScannedPlayer> getPlayersByUsernames(Set<String> usernamesLowercase) {
      if (usernamesLowercase != null && !usernamesLowercase.isEmpty()) {
         List<ScannedPlayer> result = new ArrayList<>();

         for (ScannedPlayer player : this.scannedPlayers.values()) {
            if (player != null && player.getUsername() != null && usernamesLowercase.contains(player.getUsername().toLowerCase())) {
               result.add(player);
            }
         }

         return result;
      } else {
         return Collections.emptyList();
      }
   }

   public List<ScannedPlayer> getPlayersWithSpecialItems() {
      List<ScannedPlayer> result = new ArrayList<>();

      for (ScannedPlayer player : this.scannedPlayers.values()) {
         if (player.hasSpecialItems()) {
            result.add(player);
         }
      }

      return result;
   }

   public ScannedPlayer getPlayerByUsername(String username) {
      if (username != null && !username.isEmpty()) {
         String lower = username.toLowerCase();

         for (ScannedPlayer player : this.scannedPlayers.values()) {
            if (player.getUsername() != null && player.getUsername().toLowerCase().equals(lower)) {
               return player;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public ScannedPlayer getPlayerByUuid(String uuid) {
      if (uuid != null && !uuid.isEmpty()) {
         String normalized = uuid.replace("-", "").toLowerCase();

         for (Entry<String, ScannedPlayer> entry : this.scannedPlayers.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.replace("-", "").toLowerCase().equals(normalized)) {
               return entry.getValue();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public List<ScannedPlayer> getPlayersWithMentionableItems() {
      List<ScannedPlayer> result = new ArrayList<>();

      for (ScannedPlayer player : this.scannedPlayers.values()) {
         if (player.hasMentionableItems()) {
            result.add(player);
         }
      }

      return result;
   }

   public List<ScannedPlayer> getPlayersWithDisplayableItems() {
      List<ScannedPlayer> result = new ArrayList<>();

      for (ScannedPlayer player : this.scannedPlayers.values()) {
         if (player.hasDisplayableItems()) {
            result.add(player);
         }
      }

      return result;
   }

   public List<ScannedPlayer> getPlayersWithDisplayableItemsByUsernames(Set<String> usernamesLowercase) {
      if (usernamesLowercase != null && !usernamesLowercase.isEmpty()) {
         List<ScannedPlayer> result = new ArrayList<>();

         for (ScannedPlayer player : this.scannedPlayers.values()) {
            if (player != null
               && player.getUsername() != null
               && player.hasDisplayableItems()
               && usernamesLowercase.contains(player.getUsername().toLowerCase())) {
               result.add(player);
            }
         }

         return result;
      } else {
         return Collections.emptyList();
      }
   }

   public List<ScannedPlayer> getPlayersWithItemContaining(String keyword) {
      if (keyword != null && !keyword.isEmpty()) {
         String lower = keyword.toLowerCase();
         List<ScannedPlayer> result = new ArrayList<>();

         for (ScannedPlayer player : this.scannedPlayers.values()) {
            for (ScannedItem item : player.getItems()) {
               if (item != null && item.isMentionable()) {
                  String name = item.getItemName();
                  if (name != null && name.toLowerCase().contains(lower)) {
                     result.add(player);
                     break;
                  }
               }
            }
         }

         return result;
      } else {
         return Collections.emptyList();
      }
   }

   public List<ScannedPlayer> getPlayersWithItemAndHex(String itemKeyword, String hex) {
      if (itemKeyword != null && !itemKeyword.isEmpty() && hex != null && !hex.isEmpty()) {
         String hexNorm = hex.replace("#", "").trim().toUpperCase();
         if (hexNorm.length() != 6) {
            return Collections.emptyList();
         }

         try {
            Integer.parseInt(hexNorm, 16);
         } catch (NumberFormatException e) {
            return Collections.emptyList();
         }

         String kwLower = itemKeyword.toLowerCase().trim();
         List<ScannedPlayer> result = new ArrayList<>();

         for (ScannedPlayer player : this.scannedPlayers.values()) {
            for (ScannedItem item : player.getItems()) {
               if (item != null) {
                  String itemHex = item.getHexColor();
                  if (itemHex != null && !itemHex.isEmpty()) {
                     String itemHexNorm = itemHex.replace("#", "").trim().toUpperCase();
                     if (itemHexNorm.length() == 6 && itemHexNorm.equals(hexNorm)) {
                        String id = item.getItemId();
                        String name = item.getItemName();
                        boolean match = id != null && id.toLowerCase().contains(kwLower) || name != null && name.toLowerCase().contains(kwLower);
                        if (!match && item.getCategory() != null) {
                           match = categoryMatchesKeyword(item.getCategory(), kwLower);
                        }

                        if (match) {
                           result.add(player);
                           break;
                        }
                     }
                  }
               }
            }
         }

         return result;
      } else {
         return Collections.emptyList();
      }
   }

   private static boolean categoryMatchesKeyword(ScannedItem.ItemCategory category, String kwLower) {
      if (kwLower == null) {
         return false;
      }

      switch (category) {
         case EXOTIC:
            return kwLower.equals("exotic");
         case CRYSTAL:
            return kwLower.equals("crystal");
         case FAIRY:
            return kwLower.equals("fairy");
         case OG_FAIRY:
            return kwLower.equals("og fairy") || kwLower.equals("ogfairy");
         case BLEACHED:
            return kwLower.equals("bleached");
         case SPECIFIC_HEX:
            return kwLower.equals("specific hex") || kwLower.equals("target hex") || kwLower.equals("hex");
         case GLITCHED:
            return kwLower.equals("glitched");
         case SEYMOUR_T1:
            return kwLower.equals("seymour") || kwLower.equals("seymour t1") || kwLower.equals("t1");
         case SEYMOUR_T2:
            return kwLower.equals("seymour") || kwLower.equals("seymour t2") || kwLower.equals("t2");
         case SEYMOUR_T3:
            return kwLower.equals("seymour") || kwLower.equals("seymour t3") || kwLower.equals("t3");
         default:
            return false;
      }
   }

   public List<ScannedItem> getIslandItems() {
      synchronized (this.islandItems) {
         return new ArrayList<>(this.islandItems);
      }
   }
}
