package com.potatotool.manager;

import com.potatotool.PotatoToolMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ChatMacroRunner {
   public static final int DELAY_TICKS = 100;
   public static final ChatMacroPreset[] PRESETS = new ChatMacroPreset[]{
      new ChatMacroPreset("Hub", "/hub"),
      new ChatMacroPreset("Dhub", "/warp dhub"),
      new ChatMacroPreset("Spider", "/warp spider"),
      new ChatMacroPreset("End", "/warp end"),
      new ChatMacroPreset("Forge", "/warp forge"),
      new ChatMacroPreset("Park", "/warp park")
   };

   private static volatile boolean running;
   private static volatile boolean cancelNextEsc;
   private static volatile boolean pausedForApiWindow;
   private static int ticksUntilNext;
   private static int nextIndex;
   private static List<String> commands = new ArrayList<>();
   private static int lastSentIndex = -1;

   private ChatMacroRunner() {
   }

   public static boolean isRunning() {
      return running;
   }

   public static int getNextIndex() {
      return nextIndex;
   }

   public static int getLastSentIndex() {
      return lastSentIndex;
   }

   public static int getTicksUntilNext() {
      return ticksUntilNext;
   }

   public static int getCommandCount() {
      return commands.size();
   }

   public static String statusLine() {
      if (!running) {
         return "Idle · Start to loop the sequence (5s between warps)";
      }

      if (pausedForApiWindow) {
         long waitMs = windowWaitMs();
         return "Paused · 300 API uses / 5 min · resume in " + formatWait(waitMs) + " · ESC cancels";
      }

      double seconds = ticksUntilNext / 20.0;
      String next = nextIndex >= 0 && nextIndex < commands.size() ? commands.get(nextIndex) : "?";
      return "Running " + (lastSentIndex + 1) + "/" + commands.size() + " · next " + next + " in " + String.format(java.util.Locale.ROOT, "%.1f", seconds) + "s · ESC cancels";
   }

   private static long windowWaitMs() {
      if (PotatoToolMod.getInstance() == null || PotatoToolMod.getInstance().getConfig() == null) {
         return 0L;
      }

      return PotatoToolMod.getInstance().getConfig().millisUntilApiWindowOpens();
   }

   private static String formatWait(long waitMs) {
      long sec = Math.max(0L, (waitMs + 999L) / 1000L);
      long min = sec / 60L;
      long rem = sec % 60L;
      return min > 0L ? min + "m " + rem + "s" : rem + "s";
   }

   public static void start(List<String> sequence) {
      List<String> cleaned = new ArrayList<>();
      if (sequence != null) {
         for (String raw : sequence) {
            if (raw != null && !raw.isBlank()) {
               cleaned.add(raw.trim());
            }
         }
      }

      if (cleaned.isEmpty()) {
         running = false;
         return;
      }

      commands = cleaned;
      nextIndex = 0;
      lastSentIndex = -1;
      ticksUntilNext = 0;
      running = true;
      pausedForApiWindow = false;
      cancelNextEsc = false;
      PotatoToolMod.LOGGER.info("Automatic chat macros started (" + commands.size() + " steps, looping)");
   }

   public static void cancel(Minecraft client) {
      if (!running) {
         return;
      }

      running = false;
      pausedForApiWindow = false;
      ticksUntilNext = 0;
      cancelNextEsc = true;
      if (client != null && client.player != null) {
         client.player.sendSystemMessage(Component.literal("§5[PotatoToolV2] §7Automatic warps cancelled."));
      }
   }

   public static void tick(Minecraft client) {
      if (client == null) {
         return;
      }

      if (running && isEscapeDown(client)) {
         cancel(client);
         if (client.screen instanceof PauseScreen) {
            client.setScreen(null);
         }

         return;
      }

      if (cancelNextEsc) {
         if (client.screen instanceof PauseScreen) {
            client.setScreen(null);
         }

         if (!isEscapeDown(client)) {
            cancelNextEsc = false;
         }
      }

      if (!running) {
         return;
      }

      long waitMs = windowWaitMs();
      if (waitMs > 0L) {
         if (!pausedForApiWindow) {
            pausedForApiWindow = true;
            if (client.player != null) {
               client.player.sendSystemMessage(
                  Component.literal("§5[PotatoToolV2] §eAutomatic paused — 300 API uses in 5 minutes. Resuming in §f" + formatWait(waitMs) + "§e.")
               );
            }
         }

         return;
      }

      if (pausedForApiWindow) {
         pausedForApiWindow = false;
         if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("§5[PotatoToolV2] §aAutomatic resumed."));
         }
      }

      if (client.player == null || client.getConnection() == null) {
         return;
      }

      if (client.screen instanceof ChatScreen) {
         return;
      }

      if (ticksUntilNext > 0) {
         ticksUntilNext--;
         return;
      }

      if (nextIndex < 0 || nextIndex >= commands.size()) {
         nextIndex = 0;
      }

      sendCommand(client, commands.get(nextIndex));
      lastSentIndex = nextIndex;
      nextIndex = (nextIndex + 1) % commands.size();
      ticksUntilNext = DELAY_TICKS;
   }

   private static boolean isEscapeDown(Minecraft client) {
      try {
         long handle = client.getWindow().handle();
         return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
      } catch (Throwable t) {
         return false;
      }
   }

   private static void sendCommand(Minecraft client, String raw) {
      if (client.player == null || client.player.connection == null || raw == null) {
         return;
      }

      String cmd = raw.trim();
      if (cmd.startsWith("/")) {
         cmd = cmd.substring(1);
      }

      if (cmd.isEmpty()) {
         return;
      }

      client.player.connection.sendCommand(cmd);
      client.player.sendSystemMessage(Component.literal("§5[PotatoToolV2] §7Sent §f/" + cmd));
      PotatoToolMod.notifyWarpSent();
   }

   public static String labelForCommand(String command) {
      if (command == null) {
         return "";
      }

      for (ChatMacroPreset preset : PRESETS) {
         if (preset.command.equalsIgnoreCase(command.trim())) {
            return preset.label;
         }
      }

      return command;
   }

   public record ChatMacroPreset(String label, String command) {
   }
}
