package com.potatotool.client;

import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;

public class TransparentMenuButton extends Button {
   private static final int FILL_COLOR = 905969663;
   private static final int FILL_HOVER = 1358954495;
   private static final int BORDER_HIGHLIGHT = -1862270977;

   public TransparentMenuButton(int x, int y, int width, int height, Component message, OnPress onPress) {
      super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
   }

   public void extractContents(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         int color = this.isHovered() ? 1358954495 : 905969663;
         context.fill(
            this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color
         );
         if (this.isHovered() || this.isFocused()) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            context.fill(x, y, x + w, y + 2, -1862270977);
            context.fill(x, y + h - 2, x + w, y + h, -1862270977);
            context.fill(x, y, x + 2, y + h, -1862270977);
            context.fill(x + w - 2, y, x + w, y + h, -1862270977);
         }

         context.centeredText(
            mc.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, -1
         );
      }
   }
}
