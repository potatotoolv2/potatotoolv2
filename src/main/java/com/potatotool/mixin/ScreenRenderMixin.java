package com.potatotool.mixin;

import com.potatotool.client.PotatoToolTitleScreen;
import com.potatotool.util.PotatoToolScreenHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenRenderMixin {
   @Inject(method = "extractRenderState", at = @At("HEAD"))
   private void hypixelScanner_renderPotatoToolBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen self = (Screen)(Object)this;
      if (PotatoToolScreenHelper.isPotatoToolChildScreen()) {
         PotatoToolTitleScreen.renderGalaxyBackgroundStatic(context, self.width, self.height);
      }
   }
}
