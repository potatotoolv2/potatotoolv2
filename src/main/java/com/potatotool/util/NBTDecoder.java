package com.potatotool.util;

import com.google.gson.JsonObject;
import com.potatotool.PotatoToolMod;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class NBTDecoder {
   public static JsonObject decodeInventoryData(String base64Data) {
      try {
         byte[] compressed = Base64.getDecoder().decode(base64Data);
         ByteArrayInputStream byteStream = new ByteArrayInputStream(compressed);
         GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
         new DataInputStream(gzipStream);
         CompoundTag nbt = NbtIo.readCompressed(new ByteArrayInputStream(compressed), NbtAccounter.unlimitedHeap());
         return nbtToJson(nbt);
      } catch (Exception e) {
         PotatoToolMod.LOGGER.error("Failed to decode NBT data", e);
         return null;
      }
   }

   private static JsonObject nbtToJson(CompoundTag nbt) {
      JsonObject json = new JsonObject();

      for (String key : nbt.keySet()) {
         Tag element = nbt.get(key);
         if (element != null) {
            try {
               if (element instanceof CompoundTag) {
                  json.add(key, nbtToJson((CompoundTag)element));
               } else if (element instanceof StringTag) {
                  String value = element.toString();
                  if (value.startsWith("\"") && value.endsWith("\"")) {
                     value = value.substring(1, value.length() - 1);
                  }

                  json.addProperty(key, value);
               } else if (element instanceof IntTag) {
                  json.addProperty(key, ((IntTag)element).intValue());
               } else if (element instanceof LongTag) {
                  json.addProperty(key, ((LongTag)element).longValue());
               } else if (element instanceof DoubleTag) {
                  json.addProperty(key, ((DoubleTag)element).doubleValue());
               } else if (element instanceof FloatTag) {
                  json.addProperty(key, ((FloatTag)element).floatValue());
               } else if (element instanceof ByteTag) {
                  json.addProperty(key, ((ByteTag)element).byteValue());
               } else if (element instanceof ShortTag) {
                  json.addProperty(key, ((ShortTag)element).shortValue());
               }
            } catch (Exception e) {
               PotatoToolMod.LOGGER.debug("Failed to convert NBT element {}: {}", key, e.getMessage());
            }
         }
      }

      return json;
   }
}
