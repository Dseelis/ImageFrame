package com.dseel.imageframe.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("imageframe");

    // url -> loaded ResourceLocation
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED  = ConcurrentHashMap.newKeySet();

    /**
     * Returns cached texture location, or kicks off async download.
     * Returns null if not yet loaded (caller should render a placeholder).
     */
    public static ResourceLocation getOrLoad(String url) {
        if (url == null || url.isEmpty()) return null;
        if (FAILED.contains(url))         return null;

        ResourceLocation cached = CACHE.get(url);
        if (cached != null) return cached;

        if (PENDING.add(url)) {   // add() returns false if already present
            fetchAsync(url);
        }
        return null;
    }

    private static void fetchAsync(String url) {
        CompletableFuture.supplyAsync(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);
                conn.setRequestMethod("GET");
                // Some servers block Java's default UA
                conn.setRequestProperty("User-Agent", "Minecraft/ImageFrameMod");
                conn.setInstanceFollowRedirects(true);

                int status = conn.getResponseCode();
                if (status != 200) {
                    throw new RuntimeException("HTTP " + status);
                }

                try (InputStream is = conn.getInputStream()) {
                    return NativeImage.read(is);
                }
            } catch (Exception e) {
                LOGGER.warn("[ImageFrame] Failed to load '{}': {}", url, e.getMessage());
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).thenAccept(image -> {
            PENDING.remove(url);
            if (image == null) {
                FAILED.add(url);
                return;
            }
            // Texture registration MUST happen on the main render thread
            Minecraft.getInstance().execute(() -> {
                try {
                    DynamicTexture texture = new DynamicTexture(image);
                    String key = "imageframe_" + Math.abs(url.hashCode());
                    ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("imageframe", key);
                    Minecraft.getInstance().getTextureManager().register(loc, texture);
                    CACHE.put(url, loc);
                    LOGGER.info("[ImageFrame] Loaded texture for '{}'", url);
                } catch (Exception e) {
                    LOGGER.warn("[ImageFrame] Failed to register texture for '{}': {}", url, e.getMessage());
                    FAILED.add(url);
                }
            });
        });
    }

    /** Remove a single URL from the cache and release its GPU texture. */
    public static void evict(String url) {
        ResourceLocation loc = CACHE.remove(url);
        if (loc != null) {
            Minecraft.getInstance().execute(() ->
                    Minecraft.getInstance().getTextureManager().release(loc));
        }
        FAILED.remove(url);
        PENDING.remove(url);
    }

    public static void clearAll() {
        for (String url : CACHE.keySet()) evict(url);
        FAILED.clear();
        PENDING.clear();
    }
}
