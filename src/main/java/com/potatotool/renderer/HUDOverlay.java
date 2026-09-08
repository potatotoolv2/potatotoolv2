package com.potatotool.renderer;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HUDOverlay {
   public void render(GuiGraphicsExtractor context, float tickDelta) {
      if (context != null) {
         try {
            ScanResultsOverlay.renderWithDelta(context, tickDelta);
         } catch (Throwable t) {
            com.potatotool.PotatoToolMod.LOGGER.debug("HUD overlay render failed: " + t.getMessage());
         }
      }
   }
}
