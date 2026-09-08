package com.potatotool.util;

import com.potatotool.PotatoToolMod;
import com.potatotool.config.ScannerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public final class PotatoTheme {
   public static final int BG = 0xE80B0718;
   public static final int PANEL = 0xF016102A;
   public static final int HEADER = 0xFF16103A;
   public static final int BORDER = 0xFF4A3480;
   public static final int CARD = 0xCC120C24;
   public static final int HUD_CARD = 0x55070B12;
   public static final int HUD_EDGE = 0x26FFFFFF;
   public static final int HUD_DIVIDER = 0x30FFFFFF;
   public static final int ACCENT_BLUE = 0xFF6EA8FF;
   public static final int ACCENT_VIOLET = 0xFFA78BFA;
   public static final int TAB = 0xFF8B6CFF;
   public static final int TEXT = 0xFFF0E9FF;
   public static final int MUTED = 0xFF9B8EC4;
   public static final int LOOKUP_EDGE = 0xFF2A1B4A;

   private static final int FLOW_DEEP = 0x2F6FE0;
   private static final int FLOW_LIGHT = 0x9BD4FF;

   private static final int[] CRYSTAL_EGGS = {0xFFC6A3D4, 0xFFB88BC9, 0xFFA875BD, 0xFF9C64B3, 0xFF8E51A6};
   private static final int[] OG_FAIRY_EGGS = {0xFFE5CCFF, 0xFFCC99FF, 0xFFB266FF, 0xFF9933FF, 0xFF7F00FF};
   private static final int[] FAIRY_EGGS = {0xFFFF99CC, 0xFFFF66B2};

   private PotatoTheme() {
   }

   public static boolean fairyAccentEnabled() {
      ScannerConfig config = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
      return config != null && config.hudFairyAccentEnabled;
   }

   public static int[] activeEggs() {
      if (!fairyAccentEnabled()) {
         int[] out = new int[CRYSTAL_EGGS.length + OG_FAIRY_EGGS.length];
         System.arraycopy(CRYSTAL_EGGS, 0, out, 0, CRYSTAL_EGGS.length);
         System.arraycopy(OG_FAIRY_EGGS, 0, out, CRYSTAL_EGGS.length, OG_FAIRY_EGGS.length);
         return out;
      }

      int[] out = new int[CRYSTAL_EGGS.length + OG_FAIRY_EGGS.length + FAIRY_EGGS.length];
      System.arraycopy(CRYSTAL_EGGS, 0, out, 0, CRYSTAL_EGGS.length);
      System.arraycopy(OG_FAIRY_EGGS, 0, out, CRYSTAL_EGGS.length, OG_FAIRY_EGGS.length);
      System.arraycopy(FAIRY_EGGS, 0, out, CRYSTAL_EGGS.length + OG_FAIRY_EGGS.length, FAIRY_EGGS.length);
      return out;
   }

   public static int egg(int index) {
      int[] eggs = activeEggs();
      return eggs[Math.floorMod(index, eggs.length)];
   }

   public static int eggNow() {
      return egg((int)(System.currentTimeMillis() / 850L));
   }

   public static int eggShifted(int extra) {
      return egg((int)(System.currentTimeMillis() / 850L) + extra);
   }

   public static int[] nebulaStrip() {
      return new int[]{
         0xFF3D5AFE,
         0xFF6366F1,
         eggShifted(0),
         0xFF8B5CF6,
         0xFFA78BFA,
         eggShifted(4),
         0xFF4F46E5,
         0xFF6EA8FF
      };
   }

   public static int[] eggParade() {
      return activeEggs();
   }

   public static int blueFlow() {
      return blueFlowAt(0.0);
   }

   private static int blueFlowAt(double offset) {
      double phase = (Math.sin(System.currentTimeMillis() / 480.0 + offset) + 1.0) * 0.5;
      return blendRgb(FLOW_DEEP, FLOW_LIGHT, phase);
   }

   public static int[] blueFlowStrip() {
      int[] out = new int[8];

      for (int i = 0; i < out.length; i++) {
         out[i] = 0xFF000000 | blueFlowAt(i * 0.55);
      }

      return out;
   }

   public static int blueFlowShifted(int index) {
      return 0xFF000000 | blueFlowAt(index * 0.35);
   }

   public static int accentNow() {
      ScannerConfig config = PotatoToolMod.getInstance() != null ? PotatoToolMod.getInstance().getConfig() : null;
      return guiAccentRgb(config);
   }

   public static int stripOffset(int length) {
      if (length <= 0) {
         return 0;
      }

      long t = System.currentTimeMillis();
      return (int)(t % 700L * length / 700L) % length;
   }

   public static MutableComponent branded(String text) {
      int rgb = accentNow() & 0xFFFFFF;
      return Component.literal(text).withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
   }

   public static int blendRgb(int a, int b, double t) {
      double clamped = Math.max(0.0, Math.min(1.0, t));
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int rr = (int)Math.round(ar + (br - ar) * clamped);
      int rg = (int)Math.round(ag + (bg - ag) * clamped);
      int rb = (int)Math.round(ab + (bb - ab) * clamped);
      return rr << 16 | rg << 8 | rb;
   }

   public static int guiAccentRgb(ScannerConfig config) {
      if (config == null) {
         return 0x6EA8FF;
      }

      String key = config.guiAccentTheme == null ? "" : config.guiAccentTheme.trim().toUpperCase();
      return switch (key) {
         case "BLUE_FLOW" -> blueFlow();
         case "VIOLET_FLOW" -> {
            double t = System.currentTimeMillis() / 520.0;
            double phase = (Math.sin(t) + 1.0) * 0.5;
            yield blendRgb(0x7F00FF, 0x6EA8FF, phase);
         }
         case "NEBULA" -> blendRgb(0x6366F1, eggNow() & 0xFFFFFF, 0.35);
         case "STATIC_CYAN" -> 0x4A9FFF;
         case "STATIC_GREEN" -> 0x6FB14A;
         case "STATIC_PINK" -> 0xA54A6F;
         case "STATIC_BROWN" -> 0xA56F4A;
         case "STATIC_BLUE" -> 0x4A8FD4;
         default -> {
            int rgb = config.guiAccentRgb & 0xFFFFFF;
            yield rgb == 0 ? 0x6EA8FF : rgb;
         }
      };
   }

   public static int argbAccent(ScannerConfig config) {
      return 0xFF000000 | (guiAccentRgb(config) & 0xFFFFFF);
   }
}
