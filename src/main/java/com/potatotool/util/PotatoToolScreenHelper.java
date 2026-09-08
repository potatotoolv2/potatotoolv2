package com.potatotool.util;

import com.potatotool.PotatoToolMod;
import com.potatotool.client.PotatoToolTitleScreen;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

public final class PotatoToolScreenHelper {
   public static boolean hasPotatoToolParent(Screen screen) {
      if (screen == null) {
         return false;
      }

      Class<?> c = screen.getClass();

      while (c != null) {
         try {
            Field parentField = c.getDeclaredField("parent");
            parentField.setAccessible(true);
            Object parent = parentField.get(screen);
            if (parent instanceof PotatoToolTitleScreen) {
               return true;
            }

            return false;
         } catch (NoSuchFieldException var4) {
            c = c.getSuperclass();
         } catch (IllegalAccessException ignored) {
            return false;
         }
      }

      return false;
   }

   public static boolean isPotatoToolChildScreen() {
      if (PotatoToolMod.getInstance() == null) {
         return false;
      } else if (!PotatoToolMod.getInstance().getConfig().customTitleScreenEnabled) {
         return false;
      } else {
         Minecraft client = Minecraft.getInstance();
         if (client != null && client.screen != null) {
            Screen screen = client.screen;
            return screen instanceof JoinMultiplayerScreen || screen instanceof SelectWorldScreen || screen instanceof OptionsScreen;
         } else {
            return false;
         }
      }
   }
}
