package com.dseel.imageframe.screen;

import com.dseel.imageframe.client.ImageCache;
import com.dseel.imageframe.entity.ImageFrameEntity;
import com.dseel.imageframe.network.RemoveImageFramePacket;
import com.dseel.imageframe.network.SetImagePacket;
import com.dseel.imageframe.network.SpawnImageFramePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public class ImageFrameScreen extends Screen {

    private final ImageFrameEntity entity;
    private EditBox urlBox;
    private int selectedWidth;
    private int selectedHeight;
    private boolean isMirrored;
    private ImageCache.TextureData previewData;

    private static final int[][] SIZES = {
            {1, 1}, {2, 1}, {2, 2},
            {3, 2}, {3, 3}, {4, 3},
            {4, 4}, {1, 2}, {1, 3}
    };

    private static final String[] SIZE_LABELS = {
            "1×1", "2×1", "2×2",
            "3×2", "3×3", "4×3",
            "4×4", "1×2", "1×3"
    };

    public ImageFrameScreen(ImageFrameEntity entity) {
        super(Component.translatable("gui.imageframe.title"));
        this.entity = entity;
        this.selectedWidth = entity.getWidth();
        this.selectedHeight = entity.getHeight();
        this.isMirrored = entity.isMirrored();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        urlBox = new EditBox(this.font, cx - 140, cy - 50, 280, 20,
                Component.translatable("gui.imageframe.url"));

        urlBox.setResponder(text -> previewData = null);
        urlBox.setMaxLength(512);
        urlBox.setValue(entity.getImageUrl());
        urlBox.setHint(Component.literal("https://example.com/image.png"));

        this.addRenderableWidget(urlBox);
        this.setInitialFocus(urlBox);

        int btnW = 46, btnH = 18, gap = 4;
        int startX = cx - 140; // Align with URL box
        int startY = cy - 10;

        for (int i = 0; i < SIZES.length; i++) {
            final int w = SIZES[i][0];
            final int h = SIZES[i][1];
            int col = i % 3, row = i / 3;
            int bx = startX + col * (btnW + gap);
            int by = startY + row * (btnH + gap);

            this.addRenderableWidget(Button.builder(
                    Component.literal(SIZE_LABELS[i]),
                    btn -> {
                        selectedWidth = w;
                        selectedHeight = h;
                    }
            ).bounds(bx, by, btnW, btnH).build());
        }

        // Mirror Toggle
        this.addRenderableWidget(Button.builder(
                Component.literal(isMirrored ? "Mirror: ON" : "Mirror: OFF"),
                btn -> {
                    isMirrored = !isMirrored;
                    btn.setMessage(Component.literal(isMirrored ? "Mirror: ON" : "Mirror: OFF"));
                }
        ).bounds(cx + 40, cy + 10, 100, 20).build());

        // Confirm
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.imageframe.confirm"),
                btn -> onConfirm()
        ).bounds(cx - 140, cy + 75, 135, 20).build());

        // Remove
        this.addRenderableWidget(Button.builder(
                Component.literal("✖ Remove"),
                btn -> onRemove()
        ).bounds(cx + 5, cy + 75, 135, 20).build());
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Disable blur
    }

    private void onConfirm() {
        String url = urlBox.getValue().trim();

        if (!url.isEmpty()) {
            if (entity.getPersistentData().contains("FrameId")) {
                PacketDistributor.sendToServer(new SpawnImageFramePacket(
                        entity.getPersistentData().getInt("FrameId"),
                        url,
                        selectedWidth,
                        selectedHeight,
                        isMirrored
                ));
            } else {
                PacketDistributor.sendToServer(new SetImagePacket(
                        entity.getId(),
                        url,
                        selectedWidth,
                        selectedHeight,
                        isMirrored
                ));
            }
        }

        this.onClose();
    }

    private void onRemove() {
        PacketDistributor.sendToServer(new RemoveImageFramePacket(entity.getId()));
        this.onClose();
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float delta) {
        this.renderBackground(gfx, mx, my, delta);

        int cx = this.width / 2;
        int cy = this.height / 2;

        // Modern Panel
        gfx.fill(cx - 160, cy - 145, cx + 160, cy + 110, 0xAA000000); // Shadow/Blur border
        gfx.fill(cx - 158, cy - 143, cx + 158, cy + 108, 0xFF2D2D3A); // Main bg
        gfx.fill(cx - 158, cy - 143, cx + 158, cy - 125, 0xFF3D3D4D); // Title bar

        // Preview Area Background
        gfx.fill(cx - 52, cy - 122, cx + 52, cy - 18, 0xFF1A1A24);

        String url = urlBox.getValue();
        if (previewData == null && !url.isEmpty()) {
            previewData = ImageCache.getTexture(url, 0);
        }

        ResourceLocation tex = (previewData != null) ? previewData.location() : MissingTextureAtlasSprite.getLocation();
        int tw = (previewData != null) ? previewData.width() : 16;
        int th = (previewData != null) ? previewData.height() : 16;

        // Preview Scaling
        float scale = Math.min(100f / tw, 100f / th);
        int dw = (int)(tw * scale);
        int dh = (int)(th * scale);
        int dx = cx - dw / 2;
        int dy = (cy - 70) - dh / 2;

        if (isMirrored) {
            gfx.blit(tex, dx, dy, dw, dh, tw, 0, -tw, th, tw, th);
        } else {
            gfx.blit(tex, dx, dy, dw, dh, 0, 0, tw, th, tw, th);
        }

        // Labels
        gfx.drawCenteredString(this.font,
                Component.translatable("gui.imageframe.title"),
                cx, cy - 139, 0xFFE0C080);

        gfx.drawString(this.font,
                Component.translatable("gui.imageframe.url_label"),
                cx - 140, cy - 63, 0xFFAAAAAA);

        gfx.drawString(this.font,
                Component.translatable("gui.imageframe.size_label"),
                cx - 140, cy - 18, 0xFFAAAAAA);

        gfx.drawCenteredString(this.font,
                "▶ " + selectedWidth + "×" + selectedHeight,
                cx + 100, cy - 18, 0xFF80E0C0);

        int btnW = 46, btnH = 18, gap = 4;
        int startX = cx - 140; 
        int startY = cy - 10;

        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i][0] == selectedWidth && SIZES[i][1] == selectedHeight) {
                int col = i % 3, row = i / 3;
                int bx = startX + col * (btnW + gap);
                int by = startY + row * (btnH + gap);
                gfx.fill(bx - 1, by - 1, bx + btnW + 1, by + btnH + 1, 0xFF80E0C0); // Brighter selection border
                gfx.fill(bx, by, bx + btnW, by + btnH, 0xFF1A1A2E); // Inner button fill for contrast
                break;
            }
        }

        super.render(gfx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}