package com.potatotool.mixin;

import com.potatotool.PotatoToolMod;
import com.potatotool.client.PotatoToolTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
   @Inject(method = "init", at = @At("HEAD"), cancellable = true)
   private void replaceWithPotatoTool(CallbackInfo ci) {
      if (PotatoToolMod.getInstance() != null) {
         if (PotatoToolMod.getInstance().getConfig().customTitleScreenEnabled) {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
               client.setScreen(new PotatoToolTitleScreen());
               ci.cancel();
            }
         }
      }
   }
}
