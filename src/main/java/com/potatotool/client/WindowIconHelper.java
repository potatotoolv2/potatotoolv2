package com.potatotool.client;

import com.potatotool.PotatoToolMod;
import java.io.InputStream;
import java.nio.ByteBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryUtil;

public final class WindowIconHelper {
   public static boolean setWindowIconIfNeeded(long glfwWindowHandle) {
      if (glfwWindowHandle == 0L) {
         return false;
      }

      InputStream in = WindowIconHelper.class.getResourceAsStream("/assets/potato-tool/textures/icon.png");
      if (in == null) {
         in = WindowIconHelper.class.getResourceAsStream("assets/potato-tool/textures/icon.png");
      }

      boolean var41;
      try {
         if (in == null) {
            return false;
         }

         NativeImage image = NativeImage.read(in);

         label286: {
            boolean argb;
            try {
               int w = image.getWidth();
               int h = image.getHeight();
               if (w > 0 && h > 0 && w <= 256 && h <= 256) {
                  int[] argbx = image.getPixels();
                  int size = w * h * 4;
                  ByteBuffer pixels = MemoryUtil.memAlloc(size);

                  try {
                     for (int c : argbx) {
                        pixels.put((byte)(c >> 16 & 0xFF));
                        pixels.put((byte)(c >> 8 & 0xFF));
                        pixels.put((byte)(c & 0xFF));
                        pixels.put((byte)(c >> 24 & 0xFF));
                     }

                     pixels.flip();
                     GLFWImage glfwImg = GLFWImage.malloc();
                     glfwImg.set(w, h, pixels);
                     Buffer images = GLFWImage.malloc(1);
                     images.put(0, glfwImg);
                     images.position(0);
                     GLFW.glfwSetWindowIcon(glfwWindowHandle, images);
                     glfwImg.free();
                     images.free();
                     var41 = true;
                     break label286;
                  } finally {
                     MemoryUtil.memFree(pixels);
                  }
               }

               argb = false;
            } catch (Throwable var35) {
               if (image != null) {
                  try {
                     image.close();
                  } catch (Throwable var33) {
                     var35.addSuppressed(var33);
                  }
               }

               throw var35;
            }

            if (image != null) {
               image.close();
            }

            return argb;
         }

         if (image != null) {
            image.close();
         }
      } catch (Throwable t) {
         PotatoToolMod.LOGGER.warn("Could not set window icon: {}", t.getMessage());
         return false;
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (Exception var32) {
            }
         }
      }

      return var41;
   }
}
