package com.potatotool.client;

import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

public class PotatoToolTitleScreen extends Screen {
   private static final int BUTTON_WIDTH = 220;
   private static final int BUTTON_HEIGHT = 32;
   private static final int BUTTON_SPACING = 8;
   private static final int TITLE_Y_OFFSET = -80;
   private static final float TITLE_SCALE = 2.8F;
   private static final int STAR_COUNT = 140;
   private static final int GRADIENT_STEPS = 16;
   private static int staticStarW = -1;
   private static int staticStarH = -1;
   private static int[] staticStarX;
   private static int[] staticStarY;
   private static int[] staticStarSize;
   private static int[] staticStarBright;

   public PotatoToolTitleScreen() {
      super(Component.literal("PotatoToolV2"));
   }

   protected void init() {
      super.init();
      if (this.minecraft != null && this.minecraft.getWindow() != null) {
         this.minecraft.getWindow().setTitle("PotatoToolV2");
         WindowIconHelper.setWindowIconIfNeeded(this.minecraft.getWindow().handle());
      }

      int centerX = this.width / 2;
      int startY = this.height / 2 + -80;
      this.addRenderableWidget(new TransparentMenuButton(centerX - 110, startY, 220, 32, Component.literal("Singleplayer"), b -> {
         if (this.minecraft != null) {
            this.minecraft.execute(() -> this.minecraft.setScreen(new SelectWorldScreen(this)));
         }
      }));
      startY += 40;
      this.addRenderableWidget(new TransparentMenuButton(centerX - 110, startY, 220, 32, Component.literal("Multiplayer"), b -> {
         if (this.minecraft != null) {
            this.minecraft.execute(() -> this.minecraft.setScreen(new JoinMultiplayerScreen(this)));
         }
      }));
      startY += 40;
      this.addRenderableWidget(new TransparentMenuButton(centerX - 110, startY, 220, 32, Component.literal("Options"), b -> {
         if (this.minecraft != null) {
            this.minecraft.execute(() -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options, false)));
         }
      }));
      startY += 40;
      int half = 114;
      this.addRenderableWidget(new TransparentMenuButton(centerX - half, startY, half - 4, 32, Component.literal("Quit Game"), b -> {
         if (this.minecraft != null) {
            this.minecraft.stop();
         }
      }));
      this.addRenderableWidget(
         new TransparentMenuButton(
            centerX + 4,
            startY,
            half - 4,
            32,
            Component.literal("Mods"),
            b -> {
               if (this.minecraft != null) {
                  this.minecraft
                     .execute(
                        () -> {
                           try {
                              this.minecraft
                                 .setScreen(
                                    (Screen)Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class).newInstance(this)
                                 );
                           } catch (Throwable t) {
                              this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options, false));
                           }
                        }
                     );
               }
            }
         )
      );
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      this.renderGalaxyBackground(context);
      this.renderTitle(context);
      super.extractRenderState(context, mouseX, mouseY, delta);
      this.renderLoggedIn(context);
   }

   public void renderGalaxyBackground(GuiGraphicsExtractor context) {
      renderGalaxyBackgroundStatic(context, this.width, this.height);
   }

   public static void renderGalaxyBackgroundStatic(GuiGraphicsExtractor context, int w, int h) {
      for (int i = 0; i < 16; i++) {
         float t = i / 15.0F;
         int r = Mth.lerpInt(t, 28, 72);
         int g = Mth.lerpInt(t, 12, 36);
         int b = Mth.lerpInt(t, 78, 150);
         int y = h * i / 16;
         int y2 = h * (i + 1) / 16;
         context.fill(0, y, w, y2, 0xFF000000 | r << 16 | g << 8 | b);
      }

      context.fill(0, 0, w / 2, h / 2, 0x332A1850);
      context.fill(w / 2, 0, w, h / 2, 0x22184870);
      context.fill(0, h / 2, w / 2, h, 0x44120830);
      context.fill(w / 2, h / 2, w, h, 0x33102048);
      if (staticStarW != w || staticStarH != h || staticStarX == null) {
         staticStarW = w;
         staticStarH = h;
         Random rng = new Random(439041101L);
         staticStarX = new int[140];
         staticStarY = new int[140];
         staticStarSize = new int[140];
         staticStarBright = new int[140];

         for (int i = 0; i < 140; i++) {
            staticStarX[i] = rng.nextInt(w);
            staticStarY[i] = rng.nextInt(h);
            staticStarSize[i] = rng.nextInt(2) + 1;
            staticStarBright[i] = 180 + rng.nextInt(75);
         }
      }

      for (int i = 0; i < 140; i++) {
         int bright = staticStarBright[i];
         int c = 0xFF000000 | bright << 16 | bright << 8 | bright;
         context.fill(staticStarX[i], staticStarY[i], staticStarX[i] + staticStarSize[i], staticStarY[i] + staticStarSize[i], c);
      }
   }

   private void renderTitle(GuiGraphicsExtractor context) {
      String title = "POTATO TOOL V2";
      int centerX = this.width / 2;
      int y = this.height / 2 + -80 - 50;
      int totalW = 0;
      int[] widths = new int[title.length()];
      for (int i = 0; i < title.length(); i++) {
         widths[i] = this.font.width(String.valueOf(title.charAt(i)));
         totalW += widths[i];
      }

      context.pose().pushMatrix();
      context.pose().translate(centerX, y);
      context.pose().scale(2.8F, 2.8F);
      context.pose().translate(-centerX, -y);
      int x = centerX - totalW / 2;
      for (int i = 0; i < title.length(); i++) {
         char ch = title.charAt(i);
         int color = ch == ' ' ? -1 : com.potatotool.util.PotatoTheme.eggShifted(i);
         context.text(this.font, String.valueOf(ch), x, y, color, false);
         x += widths[i];
      }

      context.pose().popMatrix();
   }

   private void renderLoggedIn(GuiGraphicsExtractor context) {
      if (this.minecraft != null && this.minecraft.getUser() != null) {
         String name = this.minecraft.getUser().getName();
         if (name != null && !name.isEmpty()) {
            String line = "Logged in as " + name;
            context.text(this.font, line, 8, this.height - 20, -520093697, false);
         }
      }
   }

   public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float partialTick) {
   }
}
