package br.com.petrick.fornalhastech.client;

import static br.com.petrick.fornalhastech.menu.TechFurnaceLayout.*;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TechFurnaceScreen extends AbstractContainerScreen<TechFurnaceMenu> {
    private static final ResourceLocation FURNACE_SPRITES = new ResourceLocation("fornalhastech", "textures/gui/furnace_indicators.png");
    private static final String[] SLOT_LABELS = {"Núcleo", "Especialização", "Aprimorador"};
    static final int[] COLORS = {0xFF000000, 0xFFE83E3E, 0xFFE8EFF1, 0xFFFFC844, 0xFF43D878, 0xFF42D8ED, 0xFFAD704A};

    public TechFurnaceScreen(TechFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY = 999;
    }

    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        // Same metallic panel, now with independent full-size sections.
        GuiComponent.fill(pose, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFB8BCC0);
        frame(pose, 0, 0, imageWidth, imageHeight, 0xFF252A2E);
        frame(pose, 1, 1, imageWidth - 2, imageHeight - 2, 0xFFE0E3E5);
        frame(pose, 4, 20, imageWidth - 8, 35, 0xFF858C91);
        GuiComponent.fill(pose, leftPos + 5, topPos + 57, leftPos + imageWidth - 5, topPos + 58, 0xFF575F65);
        GuiComponent.fill(pose, leftPos + 5, topPos + 123, leftPos + imageWidth - 5, topPos + 124, 0xFF575F65);
        frame(pose, BUTTON_X, BUTTON_Y, 14, 14, menu.getAccentColor());
        String pGlyph = "P";
        float pX = leftPos + BUTTON_X + Math.round((14 - font.width(pGlyph)) / 2.0F) + 1;
        float pY = topPos + BUTTON_Y + Math.round((14 - font.lineHeight) / 2.0F);
        font.draw(pose, pGlyph, pX, pY, 0xFF17191B);

        // Draw each frame from its actual Slot position. AbstractContainerScreen
        // alone renders the 16x16 items and their native hit areas; no pose scaling.
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (i == 3) specialSlot(pose, slot.x - 1, slot.y - 1, 0xFF43D878);
            else if (i == 4) specialSlot(pose, slot.x - 1, slot.y - 1, 0xFFFFC844);
            else if (i == 5) specialSlot(pose, slot.x - 1, slot.y - 1, 0xFFE83E3E);
            else nativeSlot(pose, slot.x - 1, slot.y - 1);
        }

        // Original furnace sprites at 1:1; only their baked gray background is transparent.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, FURNACE_SPRITES);
        sprite(pose, FIRE_X, FIRE_Y, 56, 36, 14, 14);
        sprite(pose, ARROW_X, ARROW_Y, 79, 34, 24, 16);
        int flameRows = menu.getBurnProgress();
        if (flameRows > 0) sprite(pose, FIRE_X, FIRE_Y + 14 - flameRows,
                176, 14 - flameRows, 14, flameRows);
        int progress = menu.getCookProgress();
        if (progress > 0) sprite(pose, ARROW_X, ARROW_Y, 176, 14, progress, 16);
        RenderSystem.disableBlend();
    }

    private void sprite(PoseStack pose, int x, int y, int u, int v, int w, int h) {
        GuiComponent.blit(pose, leftPos + x, topPos + y, (float) u, (float) v, w, h, 256, 256);
    }

    private void nativeSlot(PoseStack pose, int x, int y) {
        GuiComponent.fill(pose, leftPos + x, topPos + y, leftPos + x + 18, topPos + y + 18, 0xFF373737);
        GuiComponent.fill(pose, leftPos + x + 1, topPos + y + 1, leftPos + x + 18, topPos + y + 18, 0xFFFFFFFF);
        GuiComponent.fill(pose, leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17, 0xFF8B8B8B);
    }

    private void specialSlot(PoseStack pose, int x, int y, int interiorColor) {
        GuiComponent.fill(pose, leftPos + x + 1, topPos + y + 1, leftPos + x + 17, topPos + y + 17, interiorColor);
        frame(pose, x, y, 18, 18, 0xFF000000);
    }

    private void frame(PoseStack pose, int x, int y, int width, int height, int color) {
        GuiComponent.fill(pose, leftPos + x, topPos + y, leftPos + x + width, topPos + y + 1, color);
        GuiComponent.fill(pose, leftPos + x, topPos + y + height - 1, leftPos + x + width, topPos + y + height, color);
        GuiComponent.fill(pose, leftPos + x, topPos + y, leftPos + x + 1, topPos + y + height, color);
        GuiComponent.fill(pose, leftPos + x + width - 1, topPos + y, leftPos + x + width, topPos + y + height, color);
    }

    @Override protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        Component title = Component.translatable("container.fornalhastech.tech_furnace");
        font.draw(pose, title, (imageWidth - font.width(title)) / 2.0F, 6, 0xFF17191B);
        for (int i = 0; i < SLOT_LABELS.length; i++) {
            font.draw(pose, SLOT_LABELS[i], SPECIAL_X + 8 + i * SPECIAL_STEP - font.width(SLOT_LABELS[i]) / 2.0F, LABEL_Y, 0xFF252A2E);
        }
        font.draw(pose, Component.translatable("container.inventory"), INVENTORY_X, INVENTORY_TITLE_Y, 0xFF33383B);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        int tier = menu.getCoreTier();
        if (tier > 0) {
            int selected = Math.max(0, Math.min(6, menu.getVisualColorIndex()));
            int color = COLORS[selected];
            // Frames coincide with vanilla's slot background origin (Slot.x/y minus one).
            frame(pose, INPUT_X - 1, INPUT_Y - 1, 18, 18, color);   // ingredient
            frame(pose, INPUT_X - 1, FUEL_Y - 1, 18, 18, color);   // fuel
            frame(pose, OUTPUT_X - 1, OUTPUT_Y - 1, 18, 18, color);  // result
        }
        renderTooltip(pose, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && inside(x, y, leftPos + BUTTON_X, topPos + BUTTON_Y, 14, 14)) {
            Minecraft.getInstance().setScreen(new TechFurnaceCustomizationScreen(menu));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}

