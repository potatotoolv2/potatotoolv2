package com.potatotool.util;

import com.potatotool.config.ScannerConfig;
import java.util.Locale;

/**
 * Full colour schemes for the settings interface. Unlike the accent themes these cover every
 * surface in the panel, so switching preset restyles the whole GUI rather than one highlight.
 *
 * <p>All values are plain RGB. Panel surfaces get their alpha from {@code guiAlpha} at draw time;
 * text and accents are always drawn opaque.
 */
public enum GuiPalette {
   MIDNIGHT(
      "Midnight",
      "Navy and blue",
      false,
      0x121826,
      0x0D121C,
      0x151C2B,
      0x1D2536,
      0x28324A,
      0x2E3B55,
      0xE6ECF5,
      0x8A95A8,
      0x5B9DFF
   ),
   GRAPHITE(
      "Graphite",
      "Grey and amber",
      false,
      0x18181A,
      0x121213,
      0x1D1D20,
      0x232326,
      0x2F2F34,
      0x35353A,
      0xE8E8EA,
      0x8E8E96,
      0xE0A458
   ),
   NORD(
      "Nord",
      "Slate and cyan",
      false,
      0x2E3440,
      0x272C36,
      0x333B4A,
      0x3B4252,
      0x4C566A,
      0x434C5E,
      0xECEFF4,
      0x9BA6B8,
      0x88C0D0
   ),
   MOCHA(
      "Mocha",
      "Brown and tan",
      false,
      0x1E1B19,
      0x171412,
      0x252120,
      0x2C2725,
      0x3A3330,
      0x3D3633,
      0xEDE4DC,
      0xA1928A,
      0xD4A373
   ),
   DAYLIGHT(
      "Daylight",
      "Light theme",
      true,
      0xF4F6FA,
      0xE9EDF4,
      0xFFFFFF,
      0xFFFFFF,
      0xDCE6F5,
      0xCBD5E4,
      0x1B2430,
      0x64707F,
      0x2F6FE0
   );

   private final String label;
   private final String blurb;
   private final boolean light;
   private final int background;
   private final int sidebar;
   private final int titleBar;
   private final int surface;
   private final int surfaceHover;
   private final int border;
   private final int textPrimary;
   private final int textMuted;
   private final int accent;

   GuiPalette(
      String label,
      String blurb,
      boolean light,
      int background,
      int sidebar,
      int titleBar,
      int surface,
      int surfaceHover,
      int border,
      int textPrimary,
      int textMuted,
      int accent
   ) {
      this.label = label;
      this.blurb = blurb;
      this.light = light;
      this.background = background;
      this.sidebar = sidebar;
      this.titleBar = titleBar;
      this.surface = surface;
      this.surfaceHover = surfaceHover;
      this.border = border;
      this.textPrimary = textPrimary;
      this.textMuted = textMuted;
      this.accent = accent;
   }

   public String label() {
      return this.label;
   }

   public String blurb() {
      return this.blurb;
   }

   public boolean isLight() {
      return this.light;
   }

   public int backgroundRgb() {
      return this.background;
   }

   public int sidebarRgb() {
      return this.sidebar;
   }

   public int titleBarRgb() {
      return this.titleBar;
   }

   public int surfaceRgb() {
      return this.surface;
   }

   public int surfaceHoverRgb() {
      return this.surfaceHover;
   }

   public int borderRgb() {
      return this.border;
   }

   public int accentRgb() {
      return this.accent;
   }

   public int textPrimary() {
      return 0xFF000000 | this.textPrimary;
   }

   public int textMuted() {
      return 0xFF000000 | this.textMuted;
   }

   public int accent() {
      return 0xFF000000 | this.accent;
   }

   /** Accent dimmed toward the background, for selected rows that should not shout. */
   public int accentWash(int alpha) {
      return alpha << 24 | PotatoTheme.blendRgb(this.background, this.accent, 0.30);
   }

   public int surface(int alpha) {
      return alpha << 24 | this.surface;
   }

   public int surfaceHover(int alpha) {
      return alpha << 24 | this.surfaceHover;
   }

   public int background(int alpha) {
      return alpha << 24 | this.background;
   }

   public int sidebar(int alpha) {
      return alpha << 24 | this.sidebar;
   }

   public int titleBar(int alpha) {
      return alpha << 24 | this.titleBar;
   }

   public int border(int alpha) {
      return alpha << 24 | this.border;
   }

   /** Screen dimmer drawn behind the panel. */
   public int scrim() {
      return this.light ? 0x55101820 : 0x99060810;
   }

   public static GuiPalette byKey(String key) {
      if (key != null) {
         String normalised = key.trim().toUpperCase(Locale.ROOT);

         for (GuiPalette palette : values()) {
            if (palette.name().equals(normalised)) {
               return palette;
            }
         }
      }

      return MIDNIGHT;
   }

   public static GuiPalette of(ScannerConfig config) {
      return config == null ? MIDNIGHT : byKey(config.guiPreset);
   }
}
