package com.potatotool.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ProfileStyle {
   private ProfileStyle() {
   }

   public static String color(String profileName) {
      if (profileName == null || profileName.isBlank()) {
         return "§7";
      }

      String lower = profileName.toLowerCase();
      if (lower.contains("apple") || lower.contains("strawberry") || lower.contains("tomato") || lower.contains("watermelon")) {
         return "§c";
      }
      if (lower.contains("pomegranate") || lower.contains("raspberry")) {
         return "§4";
      }
      if (lower.contains("banana") || lower.contains("lemon") || lower.contains("pineapple")) {
         return "§e";
      }
      if (lower.contains("mango") || lower.contains("orange") || lower.contains("papaya") || lower.contains("peach")) {
         return lower.contains("peach") ? "§d" : "§6";
      }
      if (lower.contains("blueberry")) {
         return "§9";
      }
      if (lower.contains("grape")) {
         return "§5";
      }
      if (lower.contains("coconut")) {
         return "§f";
      }
      if (lower.contains("lime") || lower.contains("kiwi") || lower.contains("pear") || lower.contains("cucumber") || lower.contains("zucchini")) {
         return lower.contains("kiwi") ? "§2" : "§a";
      }
      return "§b";
   }

   public static String emoji(String profileName) {
      if (profileName == null) {
         return "★";
      }

      String lower = profileName.toLowerCase();
      if (lower.contains("apple")) {
         return "\ud83c\udf4e";
      }
      if (lower.contains("banana")) {
         return "\ud83c\udf4c";
      }
      if (lower.contains("blueberry")) {
         return "\ud83e\uded0";
      }
      if (lower.contains("coconut")) {
         return "\ud83e\udd65";
      }
      if (lower.contains("cucumber") || lower.contains("zucchini")) {
         return "\ud83e\udd52";
      }
      if (lower.contains("grape")) {
         return "\ud83c\udf47";
      }
      if (lower.contains("kiwi")) {
         return "\ud83e\udd5d";
      }
      if (lower.contains("lemon") || lower.contains("lime")) {
         return "\ud83c\udf4b";
      }
      if (lower.contains("mango")) {
         return "\ud83e\udd6d";
      }
      if (lower.contains("orange")) {
         return "\ud83c\udf4a";
      }
      if (lower.contains("papaya")) {
         return "\ud83c\udf48";
      }
      if (lower.contains("peach")) {
         return "\ud83c\udf51";
      }
      if (lower.contains("pear")) {
         return "\ud83c\udf50";
      }
      if (lower.contains("pineapple")) {
         return "\ud83c\udf4d";
      }
      if (lower.contains("pomegranate")) {
         return "\ud83c\udf4e";
      }
      if (lower.contains("raspberry")) {
         return "\ud83e\uded0";
      }
      if (lower.contains("strawberry")) {
         return "\ud83c\udf53";
      }
      if (lower.contains("tomato")) {
         return "\ud83c\udf45";
      }
      if (lower.contains("watermelon")) {
         return "\ud83c\udf49";
      }
      return "★";
   }

   public static String legacy(String profileName) {
      if (profileName == null || profileName.isBlank()) {
         return "§7-";
      }

      return color(profileName) + emoji(profileName) + " " + profileName;
   }

   public static MutableComponent component(String profileName) {
      return Component.literal(legacy(profileName));
   }

   public static String modeTag(String gameMode) {
      if (gameMode == null || gameMode.isBlank() || "normal".equalsIgnoreCase(gameMode)) {
         return "";
      }

      String mode = gameMode.toLowerCase();
      if (mode.contains("iron")) {
         return " §8IM";
      }
      if (mode.contains("strand")) {
         return " §8S";
      }
      if (mode.contains("bingo")) {
         return " §8B";
      }
      return " §8" + gameMode;
   }
}
