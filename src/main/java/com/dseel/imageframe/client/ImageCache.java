package com.dseel.imageframe.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("ImageFrame");

    public record TextureData(ResourceLocation location, int width, int height) {}

    private static final Map<String, TextureData> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<TextureData>> GIF_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    public static TextureData getTexture(String url, int time) {
        if (url == null || url.isEmpty()) return null;
        if (FAILED.contains(url)) return null;

        List<TextureData> frames = GIF_CACHE.get(url);
        if (frames != null && !frames.isEmpty()) {
            int tick = (int)(System.currentTimeMillis() / 100);
            return frames.get(tick % frames.size());
        }

        if (CACHE.containsKey(url)) return CACHE.get(url);

        loadMediaAsync(url);
        return null;
    }

    private static void loadMediaAsync(String originalUrl) {
        if (LOADING.contains(originalUrl)) return;
        LOADING.add(originalUrl);

        CompletableFuture.runAsync(() -> {
            try {
                String resolvedUrl = resolveUrl(originalUrl);

                URLConnection conn = new URL(resolvedUrl).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "image/*,*/*");
                if (conn instanceof HttpURLConnection http) {
                    http.setInstanceFollowRedirects(true);
                }

                String contentType = conn.getContentType();
                boolean isGif = contentType != null
                        ? contentType.toLowerCase().contains("image/gif")
                        : resolvedUrl.toLowerCase().contains(".gif");

                try (InputStream stream = conn.getInputStream()) {
                    if (isGif) {
                        loadGifImage(stream, originalUrl);
                    } else {
                        loadStaticImage(stream, originalUrl);
                    }
                }

            } catch (FileNotFoundException e) {
                FAILED.add(originalUrl);
                LOGGER.debug("[ImageFrame] URL not found (expired/deleted): {}", originalUrl);
            } catch (IOException e) {
                FAILED.add(originalUrl);
                LOGGER.warn("[ImageFrame] Failed to load image ({}): {}", e.getClass().getSimpleName(), originalUrl);
            } catch (Exception e) {
                FAILED.add(originalUrl);
                LOGGER.error("[ImageFrame] Unexpected error loading: {}", originalUrl, e);
            } finally {
                LOADING.remove(originalUrl);
            }
        });
    }

    private static void loadGifImage(InputStream stream, String originalUrl) throws Exception {
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(stream)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                loadStaticImage(stream, originalUrl);
                return;
            }

            ImageReader reader = readers.next();
            reader.setInput(imageStream);

            List<BufferedImage> framesRaw = new ArrayList<>();
            BufferedImage master = null;

            for (int i = 0; ; i++) {
                try {
                    BufferedImage frame = reader.read(i);

                    if (master == null) {
                        master = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    }

                    Graphics2D g = master.createGraphics();
                    g.drawImage(frame, 0, 0, null);
                    g.dispose();

                    BufferedImage copy = new BufferedImage(master.getWidth(), master.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    copy.getGraphics().drawImage(master, 0, 0, null);
                    framesRaw.add(copy);

                } catch (IndexOutOfBoundsException e) {
                    break;
                }
            }

            final List<BufferedImage> finalFramesRaw = framesRaw;
            Minecraft.getInstance().execute(() -> {
                List<TextureData> frames = new ArrayList<>();

                for (int j = 0; j < finalFramesRaw.size(); j++) {
                    BufferedImage buffered = finalFramesRaw.get(j);
                    NativeImage img = bufferedToNative(buffered);

                    DynamicTexture texture = new DynamicTexture(img);
                    texture.setFilter(false, false);

                    ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                            "imageframe",
                            "gif_" + Math.abs(originalUrl.hashCode()) + "_" + j
                    );

                    Minecraft.getInstance().getTextureManager().register(loc, texture);
                    frames.add(new TextureData(loc, buffered.getWidth(), buffered.getHeight()));
                }

                GIF_CACHE.put(originalUrl, frames);
            });
        }
    }

    private static void loadStaticImage(InputStream stream, String originalUrl) throws Exception {
        BufferedImage buffered = ImageIO.read(stream);
        if (buffered == null) return;

        NativeImage image = bufferedToNative(buffered);
        int width = buffered.getWidth();
        int height = buffered.getHeight();

        Minecraft.getInstance().execute(() -> {
            DynamicTexture texture = new DynamicTexture(image);
            texture.setFilter(false, false);

            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "imageframe",
                    "img_" + Math.abs(originalUrl.hashCode())
            );

            Minecraft.getInstance().getTextureManager().register(loc, texture);
            CACHE.put(originalUrl, new TextureData(loc, width, height));
        });
    }

    private static NativeImage bufferedToNative(BufferedImage buffered) {
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
                image.setPixelRGBA(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        return image;
    }

    private static String resolveUrl(String url) {
        try {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains("tenor.com") || lowerUrl.contains("giphy.com")) {
                if (lowerUrl.contains("/view/") || lowerUrl.contains("/gifs/") || !lowerUrl.contains(".gif")) {
                    URLConnection conn = new URL(url).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                    if (conn instanceof HttpURLConnection http) {
                        http.setInstanceFollowRedirects(true);
                    }

                    try (InputStream is = conn.getInputStream();
                         Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
                        if (scanner.hasNext()) {
                            String html = scanner.next();
                            String extracted = extractGifFromHtml(html);
                            if (extracted != null) return extracted;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[ImageFrame] Could not resolve URL, using original: {}", url);
        }
        return url;
    }

    private static String extractGifFromHtml(String html) {
        String ogImage = findAttributeInTag(html, "property", "og:image", "content");
        if (ogImage != null && ogImage.contains(".gif")) return ogImage;

        String imageSrc = findAttributeInTag(html, "rel", "image_src", "href");
        if (imageSrc != null && imageSrc.contains(".gif")) return imageSrc;

        String contentUrl = findAttributeInTag(html, "itemprop", "contentUrl", "content");
        if (contentUrl != null && contentUrl.contains(".gif")) return contentUrl;

        String twitterImage = findAttributeInTag(html, "name", "twitter:image", "content");
        if (twitterImage != null && twitterImage.contains(".gif")) return twitterImage;

        return null;
    }

    private static String findAttributeInTag(String html, String keyAttr, String keyVal, String targetAttr) {
        String patternStr = "<(?:meta|link)\\b[^>]*?" + Pattern.quote(keyAttr) + "\\s*=\\s*[\"']" + Pattern.quote(keyVal) + "[\"'][^>]*>";
        Pattern p = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {
            String tag = m.group(0);
            Pattern targetPattern = Pattern.compile(Pattern.quote(targetAttr) + "\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            Matcher targetMatcher = targetPattern.matcher(tag);
            if (targetMatcher.find()) return targetMatcher.group(1);
        }
        return null;
    }
}