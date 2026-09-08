package com.potatotool.mixin;

import com.potatotool.PotatoToolMod;
import com.potatotool.client.WindowIconHelper;
import java.io.IOException;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.PackResources;
import com.mojang.blaze3d.platform.IconSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
   @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
   private void hypixelScanner_setPotatoIcon(PackResources resourcePack, IconSet icons, CallbackInfo ci) throws IOException {
      if (PotatoToolMod.getInstance() != null) {
         if (PotatoToolMod.getInstance().getConfig().customTitleScreenEnabled) {
            Window self = (Window)(Object)this;
            WindowIconHelper.setWindowIconIfNeeded(self.handle());
            ci.cancel();
         }
      }
   }
}
