package com.dseel.imageframe.screen;

import com.dseel.imageframe.entity.ImageFrameEntity;
import com.dseel.imageframe.network.RemoveImageFramePacket;
import com.dseel.imageframe.network.SetImagePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * GUI для редактирования существующей ImageFrameEntity.
 * Открывается по Shift+RMB на уже висящей картинке.
 * Позволяет сменить URL, размер, или удалить entity.
 */
public class ImageFrameScreen extends Screen {

    private final ImageFrameEntity entity;
    private EditBox urlBox;
    private int selectedWidth;
    private int selectedHeight;

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
        this.selectedWidth  = entity.getWidth();
        this.selectedHeight = entity.getHeight();
    }

    @Override
    protected void init() {
        int cx = this.width  / 2;
        int cy = this.height / 2;

        urlBox = new EditBox(this.font, cx - 140, cy - 50, 280, 20,
                Component.translatable("gui.imageframe.url"));
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
            int bx  = startX + col * (btnW + gap);
            int by  = startY + row * (btnH + gap);
            this.addRenderableWidget(Button.builder(
                    Component.literal(SIZE_LABELS[i]), btn -> {
                        selectedWidth  = w;
                        selectedHeight = h;
                    }).bounds(bx, by, btnW, btnH).build());
        }

        // [Confirm] — обновить картинку
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.imageframe.confirm"),
                btn -> onConfirm()
        ).bounds(cx - 115, cy + 75, 110, 20).build());

        // [Remove] — удалить entity
        this.addRenderableWidget(Button.builder(
                Component.literal("✖ Remove"),
                btn -> onRemove()
        ).bounds(cx + 5, cy + 75, 110, 20).build());
    }

    private void onConfirm() {
        String url = urlBox.getValue().trim();
        if (!url.isEmpty()) {
            PacketDistributor.sendToServer(new SetImagePacket(
                    entity.getId(), url, selectedWidth, selectedHeight));
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
        int cx = this.width / 2, cy = this.height / 2;

        // Фон панели
        gfx.fill(cx - 155, cy - 75, cx + 155, cy + 105, 0xCC000000);
        gfx.fill(cx - 154, cy - 74, cx + 154, cy + 104, 0xFF1A1A2E);

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

        // Подсветка выбранного размера
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
        if (key == 256) { this.onClose(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
