package com.potatotool.mixin;

import com.potatotool.renderer.ScanResultsOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
   private static final int GLFW_MOUSE_BUTTON_LEFT = 0;
   private static final int GLFW_PRESS = 1;
   private static final int GLFW_RELEASE = 0;

   @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
   private void hypixelScanner_onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.player != null && client.screen == null) {
         int button = buttonInfo != null ? buttonInfo.button() : -1;
         if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_RELEASE) {
            if (ScanResultsOverlay.isHudDragging()) {
               ScanResultsOverlay.stopHudDrag();
               ci.cancel();
            }
         } else if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            try {
               int mx = ScanResultsOverlay.getScaledMouseXForClick(client);
               int my = ScanResultsOverlay.getScaledMouseYForClick(client);
               if (ScanResultsOverlay.isPointInOverlayForDrag(mx, my)) {
                  ScanResultsOverlay.startHudDrag(client);
                  ci.cancel();
                  return;
               }

               for (ScanResultsOverlay.ClickableRegion r : ScanResultsOverlay.getClickableRegionsCopy()) {
                  if (r.contains(mx, my) && r.username != null && !r.username.isEmpty()) {
                     client.setScreen(new ChatScreen("/party " + r.username, false));
                     ci.cancel();
                     return;
                  }
               }
            } catch (Throwable var12) {
            }
         }
      }
   }
}
