package com.dseel.imageframe.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ImageCache {

    public record TextureData(ResourceLocation location, int width, int height) {}

    private static final Map<String, TextureData> CACHE = new HashMap<>();
    private static final Map<String, List<TextureData>> GIF_CACHE = new HashMap<>();
    private static final Set<String> LOADING = new HashSet<>();

    public static TextureData getTexture(String url, int time) {
        if (url == null || url.isEmpty()) return null;

        if (url.toLowerCase().endsWith(".gif")) {
            List<TextureData> frames = GIF_CACHE.get(url);

            if (frames != null && !frames.isEmpty()) {
                int tick = (int)(System.currentTimeMillis() / 100);
                return frames.get(tick % frames.size());
            }

            loadGifAsync(url);
            return null;
        }

        if (CACHE.containsKey(url)) return CACHE.get(url);

        loadImageAsync(url);
        return null;
    }

    private static void loadImageAsync(String url) {
        if (LOADING.contains(url)) return;
        LOADING.add(url);

        CompletableFuture.runAsync(() -> {
            try {
                InputStream stream = openStream(url);
                BufferedImage buffered = ImageIO.read(stream);
                if (buffered == null) return;

                int width = buffered.getWidth();
                int height = buffered.getHeight();

                NativeImage image = new NativeImage(width, height, true);

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int argb = buffered.getRGB(x, y);

                        int a = (argb >> 24) & 255;
                        int r = (argb >> 16) & 255;
                        int g = (argb >> 8) & 255;
                        int b = argb & 255;

                        int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                        image.setPixelRGBA(x, y, abgr);
                    }
                }

                Minecraft.getInstance().execute(() -> {
                    DynamicTexture texture = new DynamicTexture(image);
                    texture.setFilter(false, false);

                    ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                            "imageframe",
                            "img_" + Math.abs(url.hashCode())
                    );

                    Minecraft.getInstance().getTextureManager().register(loc, texture);
                    CACHE.put(url, new TextureData(loc, width, height));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void loadGifAsync(String url) {
        if (LOADING.contains(url)) return;
        LOADING.add(url);

        CompletableFuture.runAsync(() -> {
            try {
                InputStream stream = openStream(url);
                ImageInputStream imageStream = ImageIO.createImageInputStream(stream);

                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (!readers.hasNext()) return;

                ImageReader reader = readers.next();
                reader.setInput(imageStream);

                List<BufferedImage> framesRaw = new ArrayList<>();
                BufferedImage master = null;

                for (int i = 0; ; i++) {
                    try {
                        BufferedImage frame = reader.read(i);

                        if (master == null) {
                            master = new BufferedImage(
                                    frame.getWidth(),
                                    frame.getHeight(),
                                    BufferedImage.TYPE_INT_ARGB
                            );
                        }

                        Graphics2D g = master.createGraphics();
                        g.drawImage(frame, 0, 0, null);
                        g.dispose();

                        BufferedImage copy = new BufferedImage(
                                master.getWidth(),
                                master.getHeight(),
                                BufferedImage.TYPE_INT_ARGB
                        );

                        copy.getGraphics().drawImage(master, 0, 0, null);
                        framesRaw.add(copy);

                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                }

                Minecraft.getInstance().execute(() -> {
                    List<TextureData> frames = new ArrayList<>();

                    for (int j = 0; j < framesRaw.size(); j++) {
                        BufferedImage buffered = framesRaw.get(j);
                        int width = buffered.getWidth();
                        int height = buffered.getHeight();

                        NativeImage img = new NativeImage(width, height, true);

                        for (int x = 0; x < width; x++) {
                            for (int y = 0; y < height; y++) {

                                int argb = buffered.getRGB(x, y);

                                int a = (argb >> 24) & 255;
                                int r = (argb >> 16) & 255;
                                int g = (argb >> 8) & 255;
                                int b = argb & 255;

                                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                                img.setPixelRGBA(x, y, abgr);
                            }
                        }

                        DynamicTexture texture = new DynamicTexture(img);
                        texture.setFilter(false, false);

                        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                                "imageframe",
                                "gif_" + Math.abs(url.hashCode()) + "_" + j
                        );

                        Minecraft.getInstance().getTextureManager().register(loc, texture);
                        frames.add(new TextureData(loc, width, height));
                    }

                    GIF_CACHE.put(url, frames);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static InputStream openStream(String url) throws Exception {
        URLConnection conn = new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept", "image/*");
        return conn.getInputStream();
    }
}