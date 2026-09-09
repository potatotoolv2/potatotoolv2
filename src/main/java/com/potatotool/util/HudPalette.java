package com.potatotool.util;

import com.potatotool.config.ScannerConfig;
import java.util.Locale;

/**
 * Colour schemes for the scan results overlay. Each preset covers the card fill, its edges, the
 * row divider and the animated border strip, so the HUD stays internally consistent.
 */
public enum HudPalette {
   MIDNIGHT("Midnight", "Deep navy card, blue border", 0x99101828, 0x405B9DFF, 0x22FFFFFF, 0x2F6FE0, 0x9BD4FF),
   GRAPHITE("Graphite", "Neutral dark card, amber border", 0x99141416, 0x40E0A458, 0x22FFFFFF, 0x8A5A2B, 0xE0A458),
   NORD("Nord", "Arctic slate card, cyan border", 0x992E3440, 0x4088C0D0, 0x24FFFFFF, 0x5E81AC, 0x8FBCBB),
   MOCHA("Mocha", "Warm brown card, tan border", 0x991E1B19, 0x40D4A373, 0x22FFFFFF, 0x8C5F3B, 0xE7C296),
   CLEAR("Clear", "Barely-there card, white border", 0x4C05070C, 0x2EFFFFFF, 0x1CFFFFFF, 0x9AA4B2, 0xF2F5F9);

   private static final int STRIP_LENGTH = 8;

   private final String label;
   private final String blurb;
   private final int card;
   private final int edge;
   private final int divider;
   private final int stripFrom;
   private final int stripTo;

   HudPalette(String label, String blurb, int card, int edge, int divider, int stripFrom, int stripTo) {
      this.label = label;
      this.blurb = blurb;
      this.card = card;
      this.edge = edge;
      this.divider = divider;
      this.stripFrom = stripFrom;
      this.stripTo = stripTo;
   }

   public String label() {
      return this.label;
   }

   public String blurb() {
      return this.blurb;
   }

   public int card() {
      return this.card;
   }

   public int edge() {
      return this.edge;
   }

   public int divider() {
      return this.divider;
   }

   /** Mid-point of the strip, for swatches and any single-colour use. */
   public int accent() {
      return 0xFF000000 | PotatoTheme.blendRgb(this.stripFrom, this.stripTo, 0.5);
   }

   /**
    * Static gradient across the border. Ends meet in the middle so the strip reads as one smooth
    * sweep instead of a hard seam when it wraps.
    */
   public int[] strip() {
      int[] out = new int[STRIP_LENGTH];

      for (int i = 0; i < STRIP_LENGTH; i++) {
         double t = (double)i / (STRIP_LENGTH - 1);
         double eased = t <= 0.5 ? t * 2.0 : (1.0 - t) * 2.0;
         out[i] = 0xFF000000 | PotatoTheme.blendRgb(this.stripFrom, this.stripTo, eased);
      }

      return out;
   }

   /** Same gradient, breathing slowly so the border has a little life without cycling hues. */
   public int[] animatedStrip() {
      int[] out = new int[STRIP_LENGTH];
      double drift = Math.sin(System.currentTimeMillis() / 900.0) * 0.18;

      for (int i = 0; i < STRIP_LENGTH; i++) {
         double t = (double)i / (STRIP_LENGTH - 1);
         double eased = t <= 0.5 ? t * 2.0 : (1.0 - t) * 2.0;
         out[i] = 0xFF000000 | PotatoTheme.blendRgb(this.stripFrom, this.stripTo, Math.max(0.0, Math.min(1.0, eased + drift)));
      }

      return out;
   }

   public static HudPalette byKey(String key) {
      if (key != null) {
         String normalised = key.trim().toUpperCase(Locale.ROOT);

         for (HudPalette palette : values()) {
            if (palette.name().equals(normalised)) {
               return palette;
            }
         }
      }

      return MIDNIGHT;
   }

   public static HudPalette of(ScannerConfig config) {
      return config == null ? MIDNIGHT : byKey(config.hudPreset);
   }
}
