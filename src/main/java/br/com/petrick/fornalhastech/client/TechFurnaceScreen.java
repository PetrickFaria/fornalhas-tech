package br.com.petrick.fornalhastech.client;

import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import br.com.petrick.fornalhastech.network.ModNetwork;
import br.com.petrick.fornalhastech.network.SetVisualColorPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TechFurnaceScreen extends AbstractContainerScreen<TechFurnaceMenu> {
    private static final ResourceLocation TEX = new ResourceLocation(FornalhasTech.MODID, "textures/gui/tech_furnace.png");
    private static final int TEX_WIDTH = 176;
    private static final int TEX_HEIGHT = 208;
    private static final int[] PALETTE = {0xFF9AA7AD, 0xFFE9585D, 0xFFE4E9EA, 0xFFFFC84C, 0xFF45D88A, 0xFF48D6ED, 0xFFB88764};
    private static final String[] COLOR_NAMES = {"Automática", "Vermelho", "Branco", "Dourado", "Verde", "Ciano", "Marrom"};
    private boolean paletteOpen;

    public TechFurnaceScreen(TechFurnaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = TEX_WIDTH;
        imageHeight = TEX_HEIGHT;
        inventoryLabelY = 112;
    }

    @Override
    protected void renderBg(com.mojang.blaze3d.vertex.PoseStack pose, float partial, int mx, int my) {
        RenderSystem.setShaderTexture(0, TEX);
        blit(pose, leftPos, topPos, 0, 0, 0, imageWidth, imageHeight, TEX_WIDTH, TEX_HEIGHT);

        // The fuel gauge sits between the input and fuel slots, matching the reference layout.
        int burn = menu.getBurnProgress();
        if (burn > 0) {
            GuiComponent.fill(pose, leftPos + 58, topPos + 89 - burn, leftPos + 62, topPos + 89, 0xFFFFA03A);
            GuiComponent.fill(pose, leftPos + 59, topPos + 89 - burn, leftPos + 61, topPos + 89 - Math.max(0, burn - 2), 0xFFFFD56A);
        }
        // Fill the right-facing progress arrow in the same central gap as the reference.
        int progress = menu.getCookProgress();
        if (progress > 0) {
            int accent = menu.getAccentColor();
            GuiComponent.fill(pose, leftPos + 82, topPos + 75, leftPos + 82 + progress, topPos + 81, accent);
            GuiComponent.fill(pose, leftPos + 82, topPos + 75, leftPos + 82 + progress, topPos + 76, 0xFFDDFBFF);
        }
        drawAccentCircuit(pose, menu.getAccentColor());
    }

    @Override
    protected void renderLabels(com.mojang.blaze3d.vertex.PoseStack pose, int mx, int my) {
        font.draw(pose, title, (imageWidth - font.width(title)) / 2.0F, 5, 0xFFE6EAEC);
        drawCompactCentered(pose, "Núcleo", 65, 42);
        drawCompactCentered(pose, "Módulo", 91, 42);
        drawCompactCentered(pose, "Aprimorador", 117, 42);
        font.draw(pose, playerInventoryTitle, 7, inventoryLabelY, 0xFFD3DADD);
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.PoseStack pose, int mx, int my, float delta) {
        renderBackground(pose);
        super.render(pose, mx, my, delta);
        renderSpecialSlotBorders(pose);
        renderColorButton(pose);
        if (paletteOpen) renderPalette(pose);
        if (isColorButtonHovered(mx, my)) renderTooltip(pose, Component.literal("Personalizar cor visual: " + COLOR_NAMES[menu.getVisualColorIndex()]), mx, my);
        renderTooltip(pose, mx, my);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, leftPos + 155, topPos + 6, 14, 14)) {
            paletteOpen = !paletteOpen;
            return true;
        }
        if (paletteOpen && button == 0) {
            for (int i = 0; i < PALETTE.length; i++) {
                int col = i % 4, row = i / 4;
                int x = leftPos + 35 + col * 27, y = topPos + 91 + row * 25;
                if (inside(mouseX, mouseY, x, y, 19, 19)) {
                    ModNetwork.CHANNEL.sendToServer(new SetVisualColorPacket(menu.getBlockPos(), i));
                    paletteOpen = false;
                    return true;
                }
            }
            if (!inside(mouseX, mouseY, leftPos + 25, topPos + 75, 126, 76)) paletteOpen = false;
            else return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawAccentCircuit(com.mojang.blaze3d.vertex.PoseStack pose, int color) {
        // Discreet edge circuits: the fixed special-slot borders remain part of the neutral texture.
        GuiComponent.fill(pose, leftPos + 12, topPos + 9, leftPos + 25, topPos + 10, color);
        GuiComponent.fill(pose, leftPos + 12, topPos + 9, leftPos + 13, topPos + 17, color);
        GuiComponent.fill(pose, leftPos + 151, topPos + 28, leftPos + 163, topPos + 29, color);
        GuiComponent.fill(pose, leftPos + 162, topPos + 29, leftPos + 163, topPos + 37, color);
        GuiComponent.fill(pose, leftPos + 13, topPos + 111, leftPos + 24, topPos + 112, color);
        GuiComponent.fill(pose, leftPos + 13, topPos + 112, leftPos + 14, topPos + 117, color);
    }

    private void drawCompactCentered(com.mojang.blaze3d.vertex.PoseStack pose, String label, int centerX, int y) {
        pose.pushPose();
        pose.translate(centerX, y, 0);
        pose.scale(0.7F, 0.7F, 1.0F);
        font.draw(pose, label, -font.width(label) / 2.0F, 0, 0xFFE1E5E7);
        pose.popPose();
    }

    private void renderSpecialSlotBorders(com.mojang.blaze3d.vertex.PoseStack pose) {
        // Repaint the rims after vanilla slot backgrounds so the fixed colors stay visible.
        int[] colors = {0xFF31C86A, 0xFFE8C036, 0xFFDD444C};
        int[] xs = {55, 81, 107};
        for (int i = 0; i < xs.length; i++) {
            int x = leftPos + xs[i], y = topPos + 20, c = colors[i];
            GuiComponent.fill(pose, x, y, x + 19, y + 1, c);
            GuiComponent.fill(pose, x, y + 1, x + 1, y + 19, c);
            GuiComponent.fill(pose, x + 18, y + 1, x + 19, y + 19, c);
            GuiComponent.fill(pose, x + 1, y + 18, x + 18, y + 19, c);
        }
    }

    private void renderColorButton(com.mojang.blaze3d.vertex.PoseStack pose) {
        int x = leftPos + 155, y = topPos + 6;
        GuiComponent.fill(pose, x, y, x + 14, y + 14, 0xFF11171B);
        GuiComponent.fill(pose, x + 1, y + 1, x + 13, y + 2, 0xFF87949A);
        GuiComponent.fill(pose, x + 1, y + 2, x + 2, y + 13, 0xFF59646A);
        GuiComponent.fill(pose, x + 2, y + 12, x + 13, y + 13, 0xFF283137);
        GuiComponent.fill(pose, x + 12, y + 2, x + 13, y + 12, menu.getAccentColor());
        font.draw(pose, "C", x + 4, y + 2, 0xFFE8EEF0);
    }

    private void renderPalette(com.mojang.blaze3d.vertex.PoseStack pose) {
        GuiComponent.fill(pose, leftPos + 25, topPos + 75, leftPos + 151, topPos + 151, 0xF00A0E11);
        GuiComponent.fill(pose, leftPos + 25, topPos + 75, leftPos + 151, topPos + 77, 0xFF87949A);
        GuiComponent.fill(pose, leftPos + 25, topPos + 149, leftPos + 151, topPos + 151, 0xFF283137);
        font.draw(pose, "Cor visual", leftPos + 31, topPos + 80, 0xFFE6EAEC);
        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % 4, row = i / 4;
            int x = leftPos + 35 + col * 27, y = topPos + 91 + row * 25;
            boolean selected = menu.getVisualColorIndex() == i;
            int border = selected ? 0xFFFFFFFF : 0xFF252D32;
            GuiComponent.fill(pose, x, y, x + 19, y + 19, border);
            GuiComponent.fill(pose, x + 2, y + 2, x + 17, y + 17, PALETTE[i]);
            if (i == 0) {
                GuiComponent.fill(pose, x + 4, y + 8, x + 15, y + 10, 0xFF4C555A);
            }
        }
    }

    private boolean isColorButtonHovered(double x, double y) {
        return inside(x, y, leftPos + 155, topPos + 6, 14, 14);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
