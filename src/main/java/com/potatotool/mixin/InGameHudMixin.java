package com.potatotool.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {
   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void onRender(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
   }
}
