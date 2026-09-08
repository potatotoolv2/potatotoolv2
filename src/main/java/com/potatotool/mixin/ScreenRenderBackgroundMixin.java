package com.potatotool.mixin;

import com.potatotool.util.PotatoToolScreenHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenRenderBackgroundMixin {
   @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
   private void hypixelScanner_skipBackgroundWhenPotatoToolChild(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Screen self = (Screen)(Object)this;
      if (PotatoToolScreenHelper.isPotatoToolChildScreen()) {
         ci.cancel();
      }
   }
}
