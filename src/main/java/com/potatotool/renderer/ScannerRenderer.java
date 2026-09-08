package com.potatotool.renderer;

import com.potatotool.config.ScannerConfig;
import com.potatotool.manager.DataStorageManager;
import net.minecraft.client.Minecraft;

public class ScannerRenderer {
   private final ScannerConfig config;
   private final DataStorageManager dataStorage;

   public ScannerRenderer(ScannerConfig config, DataStorageManager dataStorage) {
      this.config = config;
      this.dataStorage = dataStorage;
   }

   public void render() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         ;
      }
   }
}
