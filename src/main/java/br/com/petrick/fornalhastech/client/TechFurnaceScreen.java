package br.com.petrick.fornalhastech.client;

import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TechFurnaceScreen extends AbstractContainerScreen<TechFurnaceMenu> {
    static final ResourceLocation TEX = new ResourceLocation(FornalhasTech.MODID, "textures/gui/tech_furnace.png");
    private static final String[] SLOT_LABELS = {"Núcleo", "Especialização", "Aprimorador"};
    private static final int[] FLAME_WIDTHS = {2, 4, 4, 6, 8, 8, 10, 12, 12, 10, 8, 8, 6, 4};
    private static final int[] FLAME_OFFSETS = {5, 4, 4, 3, 2, 2, 1, 0, 0, 1, 2, 2, 3, 4};
    static final int[] COLORS = {0xFF000000, 0xFFE83E3E, 0xFFE8EFF1, 0xFFFFC844, 0xFF43D878, 0xFF42D8ED, 0xFFAD704A};

    public TechFurnaceScreen(TechFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 202;
        inventoryLabelY = 999;
    }

    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEX);
        GuiComponent.fill(pose, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFB8BCC0);
        frame(pose, 0, 0, imageWidth, imageHeight, 0xFF252A2E);
        frame(pose, 1, 1, imageWidth - 2, imageHeight - 2, 0xFFE0E3E5);
        frame(pose, 4, 20, imageWidth - 8, 35, 0xFF858C91);
        // Reuse the approved processing and inventory artwork without resampling.
        blit(pose, leftPos + 32, topPos + 58, 0, 0, 42, 176, 144, 176, 186);
        GuiComponent.fill(pose, leftPos + 5, topPos + 56, leftPos + 235, topPos + 57, 0xFF575F65);
        frame(pose, 219, 5, 14, 14, menu.getAccentColor());
        font.draw(pose, "P", leftPos + 223, topPos + 8, 0xFF17191B);

        // Fixed-color interiors and black frames for the three special slots.
        specialSlot(pose, 39, 23, 0xFF43D878);  // core: green
        specialSlot(pose, 111, 23, 0xFFFFC844); // module: yellow
        specialSlot(pose, 183, 23, 0xFFE83E3E); // accelerator: red

        // Vanilla-style flame meter: the remaining burn time fills upward.
        int flameRows = menu.getBurnProgress();
        for (int row = 14 - flameRows; row < 14; row++) {
            if (row < 0 || row >= FLAME_WIDTHS.length) continue;
            int y = topPos + 78 + row;
            int x = leftPos + 88 + FLAME_OFFSETS[row];
            int width = FLAME_WIDTHS[row];
            GuiComponent.fill(pose, x, y, x + width, y + 1, row < 5 ? 0xFFFFD66A : 0xFFE7772D);
        }

        // Progress arrow is filled from left to right, independently of fuel.
        int progress = menu.getCookProgress();
        if (progress > 0) {
            int fillWidth = Math.min(24, progress);
            GuiComponent.fill(pose, leftPos + 112, topPos + 83, leftPos + 112 + fillWidth, topPos + 87, menu.getAccentColor());
            GuiComponent.fill(pose, leftPos + 112, topPos + 83, leftPos + 112 + fillWidth, topPos + 84, 0xFFE7F2F4);
            if (fillWidth >= 21) GuiComponent.fill(pose, leftPos + 132, topPos + 81, leftPos + 136, topPos + 89, menu.getAccentColor());
        }
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
            font.draw(pose, SLOT_LABELS[i], 48 + i * 72 - font.width(SLOT_LABELS[i]) / 2.0F, 44, 0xFF252A2E);
        }
        font.draw(pose, Component.translatable("container.inventory"), 40, 109, 0xFF33383B);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        int tier = menu.getCoreTier();
        if (tier > 0) {
            int selected = Math.max(0, Math.min(6, menu.getVisualColorIndex()));
            int color = COLORS[selected];
            // Frames coincide with vanilla's slot background origin (Slot.x/y minus one).
            frame(pose, 87, 58, 18, 18, color);   // ingredient
            frame(pose, 87, 94, 18, 18, color);   // fuel
            frame(pose, 147, 76, 18, 18, color);  // result
        }
        renderTooltip(pose, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && inside(x, y, leftPos + 219, topPos + 5, 14, 14)) {
            Minecraft.getInstance().setScreen(new TechFurnaceCustomizationScreen(menu));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
