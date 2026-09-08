package com.potatotool.mixin;

import com.potatotool.util.PotatoToolScreenHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
public class ButtonWidgetMixin {
   private static final int FILL_COLOR = 905969663;
   private static final int FILL_HOVER = 1358954495;
   private static final int BORDER_HIGHLIGHT = -1862270977;

   @Inject(method = "extractWidgetRenderState", at = @At("HEAD"), cancellable = true)
   private void hypixelScanner_renderTransparentIfPotatoToolChild(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (PotatoToolScreenHelper.isPotatoToolChildScreen()) {
         AbstractButton self = (AbstractButton)(Object)this;
         Minecraft mc = Minecraft.getInstance();
         if (mc != null) {
            int color = self.isHovered() ? 1358954495 : 905969663;
            context.fill(
               self.getX(), self.getY(), self.getX() + self.getWidth(), self.getY() + self.getHeight(), color
            );
            if (self.isHovered() || self.isFocused()) {
               int x = self.getX();
               int y = self.getY();
               int w = self.getWidth();
               int h = self.getHeight();
               context.fill(x, y, x + w, y + 2, -1862270977);
               context.fill(x, y + h - 2, x + w, y + h, -1862270977);
               context.fill(x, y, x + 2, y + h, -1862270977);
               context.fill(x + w - 2, y, x + w, y + h, -1862270977);
            }

            context.centeredText(
               mc.font, self.getMessage(), self.getX() + self.getWidth() / 2, self.getY() + (self.getHeight() - 8) / 2, -1
            );
            ci.cancel();
         }
      }
   }
}
