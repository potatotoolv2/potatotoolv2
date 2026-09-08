package com.potatotool.model;

public class ScannedItem {
   private String itemId;
   private String itemName;
   private String reforge;
   private String hexColor;
   private ScannedItem.ItemCategory category;
   private String location;
   private Integer cakeYear;
   private boolean scuffed;
   private Integer dungeonStars;
   private boolean recombobulated;
   private Double valueMillions;
   private boolean cosmeticSkinApplied = false;
   private String seymourMatchName;
   private String valuableLabel;
   private String profileName;

   public ScannedItem(String itemId, String itemName) {
      this.itemId = itemId;
      this.itemName = itemName;
      this.category = ScannedItem.ItemCategory.NORMAL;
      this.location = "Unknown";
   }

   public ScannedItem(String itemId, String itemName, String location) {
      this.itemId = itemId;
      this.itemName = itemName;
      this.category = ScannedItem.ItemCategory.NORMAL;
      this.location = location;
   }

   public String getItemId() {
      return this.itemId;
   }

   public String getItemName() {
      return this.itemName;
   }

   public void setItemName(String itemName) {
      this.itemName = itemName;
   }

   public void setValuableLabel(String valuableLabel) {
      this.valuableLabel = valuableLabel;
   }

   public String getResolvedDisplayName() {
      return com.potatotool.util.ItemNames.resolve(this);
   }

   public String getReforge() {
      return this.reforge;
   }

   public void setReforge(String reforge) {
      this.reforge = reforge;
   }

   public String getHexColor() {
      return this.hexColor;
   }

   public void setHexColor(String hexColor) {
      this.hexColor = hexColor;
   }

   public ScannedItem.ItemCategory getCategory() {
      return this.category;
   }

   public void setCategory(ScannedItem.ItemCategory category) {
      this.category = category;
   }

   public String getSeymourMatchName() {
      return this.seymourMatchName;
   }

   public void setSeymourMatchName(String seymourMatchName) {
      this.seymourMatchName = seymourMatchName;
   }

   public String getLocation() {
      return this.location;
   }

   public void setLocation(String location) {
      this.location = location;
   }

   public String getProfileName() {
      return this.profileName;
   }

   public void setProfileName(String profileName) {
      this.profileName = profileName;
   }

   public String getLocationWithProfile() {
      String loc = this.location == null ? "" : this.location;
      String profile = this.profileName == null ? "" : this.profileName;
      if (!loc.isEmpty() && !profile.isEmpty()) {
         return loc + " · " + profile;
      }

      return !profile.isEmpty() ? profile : loc;
   }

   public Integer getCakeYear() {
      return this.cakeYear;
   }

   public void setCakeYear(Integer cakeYear) {
      this.cakeYear = cakeYear;
   }

   public boolean isScuffed() {
      return this.scuffed;
   }

   public void setScuffed(boolean scuffed) {
      this.scuffed = scuffed;
   }

   public Integer getDungeonStars() {
      return this.dungeonStars;
   }

   public void setDungeonStars(Integer dungeonStars) {
      this.dungeonStars = dungeonStars;
   }

   public boolean isRecombobulated() {
      return this.recombobulated;
   }

   public void setRecombobulated(boolean recombobulated) {
      this.recombobulated = recombobulated;
   }

   public Double getValueMillions() {
      return this.valueMillions;
   }

   public void setValueMillions(Double valueMillions) {
      this.valueMillions = valueMillions;
   }

   public boolean isCosmeticSkinApplied() {
      return this.cosmeticSkinApplied;
   }

   public void setCosmeticSkinApplied(boolean cosmeticSkinApplied) {
      this.cosmeticSkinApplied = cosmeticSkinApplied;
   }

   public String getLocationFormatted() {
      return this.location != null && !this.location.isEmpty() ? "§8[" + this.location + "]" : "";
   }

   public boolean isSpecial() {
      return this.category != null && this.category != ScannedItem.ItemCategory.NORMAL;
   }

   public static boolean isMentionableCategory(ScannedItem.ItemCategory c) {
      return c == ScannedItem.ItemCategory.FAIRY
         || c == ScannedItem.ItemCategory.OG_FAIRY
         || c == ScannedItem.ItemCategory.CRYSTAL
         || c == ScannedItem.ItemCategory.BLEACHED
         || c == ScannedItem.ItemCategory.EXOTIC
         || c == ScannedItem.ItemCategory.SPECIFIC_HEX
         || c == ScannedItem.ItemCategory.VALUABLE
         || c == ScannedItem.ItemCategory.GLITCHED
         || c == ScannedItem.ItemCategory.SEYMOUR_T1
         || c == ScannedItem.ItemCategory.SEYMOUR_T2
         || c == ScannedItem.ItemCategory.SEYMOUR_T3;
   }

   public boolean isMentionable() {
      return isMentionableCategory(this.category);
   }

   public static boolean isSeymourCategory(ScannedItem.ItemCategory c) {
      return c == ItemCategory.SEYMOUR_T1 || c == ItemCategory.SEYMOUR_T2 || c == ItemCategory.SEYMOUR_T3;
   }

   public boolean isSeymour() {
      return isSeymourCategory(this.category);
   }

   public String getCategoryTag() {
      if (this.category == null) {
         return "";
      }

      switch (this.category) {
         case VALUABLE:
            return this.valuableLabel != null && !this.valuableLabel.isBlank() ? this.valuableLabel : "Valuable";
         case CRYSTAL:
            return "Crystal";
         case OG_FAIRY:
            return "OG Fairy";
         case FAIRY:
            return "Fairy";
         case BLEACHED:
            return "Bleached";
         case EXOTIC:
            return "Exotic";
         case SPECIFIC_HEX:
            return "Specific Hex";
         case GLITCHED:
            return "Glitched";
         case LEGACY_REFORGE:
            return "Legacy";
         case GHOST_REFORGE:
            return "Ghost";
         case CAKE:
            return "Cake";
         case COSMETIC_SKIN:
            String skinLabel = this.cosmeticSkinApplied ? "Applied" : "Unapplied";
            if (this.valueMillions != null && this.valueMillions > 0.0) {
               return "Skin "
                  + skinLabel
                  + " ("
                  + (this.valueMillions >= 1.0 ? this.valueMillions.intValue() + "m" : String.format("%.1fm", this.valueMillions))
                  + ")";
            }

            return "Cosmetic " + skinLabel;
         case SEYMOUR_T1:
            return this.seymourTag("T1");
         case SEYMOUR_T2:
            return this.seymourTag("T2");
         case SEYMOUR_T3:
            return this.seymourTag("T3");
         default:
            return "";
      }
   }

   public String getDiscordLine() {
      String tag = this.getCategoryTag();
      String name = this.cleanedItemName();
      boolean seymour = this.category == ItemCategory.SEYMOUR_T1
         || this.category == ItemCategory.SEYMOUR_T2
         || this.category == ItemCategory.SEYMOUR_T3;
      StringBuilder sb = new StringBuilder(tag == null ? "" : tag);
      if (!seymour && !name.isEmpty() && (tag == null || !tag.contains(name))) {
         if (sb.length() > 0) {
            sb.append(" — ");
         }

         sb.append(name);
      }

      if (this.hexColor != null && !this.hexColor.isBlank()) {
         String hex = this.hexColor.startsWith("#") ? this.hexColor : "#" + this.hexColor;
         sb.append(" (").append(hex).append(")");
      }

      return sb.toString();
   }

   private String seymourTag(String tier) {
      if (this.seymourMatchName != null && !this.seymourMatchName.isBlank()) {
         return "Seymour " + tier + " of " + this.seymourMatchName;
      }

      String piece = this.cleanedItemName();
      if (piece.isEmpty() && this.itemId != null && !this.itemId.isBlank()) {
         piece = this.itemId.replace('_', ' ').toLowerCase();
      }

      return piece.isEmpty() ? "Seymour " + tier : "Seymour " + tier + " of " + piece;
   }

   private String cleanedItemName() {
      return com.potatotool.util.ItemNames.resolve(this);
   }

   public String getCleanItemName() {
      return this.cleanedItemName();
   }

   public String getKindLabel() {
      if (this.category == null) {
         return "";
      }

      return switch (this.category) {
         case SEYMOUR_T1 -> "Seymour T1";
         case SEYMOUR_T2 -> "Seymour T2";
         case SEYMOUR_T3 -> "Seymour T3";
         case COSMETIC_SKIN -> this.cosmeticSkinApplied ? "Applied skin" : "Unapplied skin";
         default -> this.getCategoryTag();
      };
   }

   public enum ItemCategory {
      NORMAL,
      VALUABLE,
      CRYSTAL,
      OG_FAIRY,
      FAIRY,
      BLEACHED,
      EXOTIC,
      SPECIFIC_HEX,
      GLITCHED,
      LEGACY_REFORGE,
      GHOST_REFORGE,
      CAKE,
      COSMETIC_SKIN,
      SEYMOUR_T1,
      SEYMOUR_T2,
      SEYMOUR_T3;
   }
}
