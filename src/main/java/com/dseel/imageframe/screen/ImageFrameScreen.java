package com.dseel.imageframe.screen;

import com.dseel.imageframe.client.ImageCache;
import com.dseel.imageframe.entity.ImageFrameEntity;
import com.dseel.imageframe.network.RemoveImageFramePacket;
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
    private ResourceLocation previewTexture;

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
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        urlBox = new EditBox(this.font, cx - 140, cy - 50, 280, 20,
                Component.translatable("gui.imageframe.url"));

        urlBox.setResponder(text -> previewTexture = null);

        urlBox.setMaxLength(512);
        urlBox.setValue(entity.getImageUrl());
        urlBox.setHint(Component.literal("https://example.com/image.png"));

        this.addRenderableWidget(urlBox);
        this.setInitialFocus(urlBox);

        int btnW = 52, btnH = 18, gap = 4;
        int startX = cx - (3 * btnW + 2 * gap) / 2;
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

        // Confirm
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.imageframe.confirm"),
                btn -> onConfirm()
        ).bounds(cx - 115, cy + 75, 110, 20).build());

        // Remove
        this.addRenderableWidget(Button.builder(
                Component.literal("✖ Remove"),
                btn -> onRemove()
        ).bounds(cx + 5, cy + 75, 110, 20).build());
    }

    private void onConfirm() {
        String url = urlBox.getValue().trim();

        if (!url.isEmpty()) {

            var player = this.minecraft.player;
            if (player == null) return;

            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            int facing = player.getDirection().get3DDataValue();

            PacketDistributor.sendToServer(new SpawnImageFramePacket(
                    x,
                    y,
                    z,
                    facing,
                    url,
                    selectedWidth,
                    selectedHeight
            ));
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

        gfx.fill(cx - 155, cy - 75, cx + 155, cy + 105, 0xCC000000);
        gfx.fill(cx - 154, cy - 74, cx + 154, cy + 104, 0xFF1A1A2E);

        String url = urlBox.getValue();

        if (previewTexture == null && !url.isEmpty()) {
            previewTexture = ImageCache.getTexture(url, 0);
        }

        ResourceLocation tex = previewTexture;
        if (tex == null) tex = MissingTextureAtlasSprite.getLocation();

        int px = this.width / 2 - 50;
        int py = this.height / 2 - 120;

        gfx.blit(tex, px, py, 0, 0, 100, 100, 100, 100);

        gfx.drawCenteredString(this.font, "Preview", this.width / 2, py - 10, 0xFFFFFF);

        gfx.drawCenteredString(this.font,
                Component.translatable("gui.imageframe.title"),
                cx, cy - 70, 0xFFE0C080);

        gfx.drawString(this.font,
                Component.translatable("gui.imageframe.url_label"),
                cx - 140, cy - 63, 0xFFAAAAAA);

        gfx.drawString(this.font,
                Component.translatable("gui.imageframe.size_label"),
                cx - 140, cy - 18, 0xFFAAAAAA);

        gfx.drawCenteredString(this.font,
                "▶ " + selectedWidth + "×" + selectedHeight,
                cx + 100, cy - 18, 0xFF80E0C0);

        int btnW = 52, btnH = 18, gap = 4;
        int startX = cx - (3 * btnW + 2 * gap) / 2;
        int startY = cy - 10;

        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i][0] == selectedWidth && SIZES[i][1] == selectedHeight) {
                int col = i % 3, row = i / 3;
                int bx = startX + col * (btnW + gap);
                int by = startY + row * (btnH + gap);
                gfx.fill(bx - 1, by - 1, bx + btnW + 1, by + btnH + 1, 0xFF80E0C0);
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