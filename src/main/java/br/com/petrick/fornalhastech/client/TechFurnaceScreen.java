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
    static final int[] COLORS = {0xFF000000, 0xFFE83E3E, 0xFFE8EFF1, 0xFFFFC844, 0xFF43D878, 0xFF42D8ED, 0xFFAD704A};

    public TechFurnaceScreen(TechFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 999;
    }

    @Override protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEX);
        blit(pose, leftPos, topPos, 0, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        // Vanilla-style flame meter: the remaining burn time fills upward.
        int flameRows = menu.getBurnProgress();
        int[] widths = {2, 4, 4, 6, 8, 8, 10, 12, 12, 10, 8, 8, 6, 4};
        int[] offsets = {5, 4, 4, 3, 2, 2, 1, 0, 0, 1, 2, 2, 3, 4};
        for (int row = 14 - flameRows; row < 14; row++) {
            if (row < 0 || row >= widths.length) continue;
            int y = topPos + 36 + row;
            int x = leftPos + 56 + offsets[row];
            int width = widths[row];
            GuiComponent.fill(pose, x, y, x + width, y + 1, row < 5 ? 0xFFFFD66A : 0xFFE7772D);
        }

        // Progress arrow is filled from left to right, independently of fuel.
        int progress = menu.getCookProgress();
        if (progress > 0) {
            int fillWidth = Math.min(24, progress);
            GuiComponent.fill(pose, leftPos + 80, topPos + 41, leftPos + 80 + fillWidth, topPos + 45, menu.getAccentColor());
            GuiComponent.fill(pose, leftPos + 80, topPos + 41, leftPos + 80 + fillWidth, topPos + 42, 0xFFE7F2F4);
            if (fillWidth >= 21) GuiComponent.fill(pose, leftPos + 100, topPos + 39, leftPos + 104, topPos + 47, menu.getAccentColor());
        }
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
        font.draw(pose, Component.translatable("container.inventory"), 8, 73, 0xFF33383B);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        super.render(pose, mouseX, mouseY, partialTick);
        int tier = menu.getCoreTier();
        if (tier > 0) {
            int selected = Math.max(0, Math.min(6, menu.getVisualColorIndex()));
            int color = COLORS[selected];
            // Frames coincide with vanilla's slot background origin (Slot.x/y minus one).
            frame(pose, 7, 16, 18, 18, color);    // core
            frame(pose, 55, 16, 18, 18, color);   // ingredient
            frame(pose, 55, 52, 18, 18, color);   // fuel
            frame(pose, 115, 34, 18, 18, color);  // result
        }
        renderTooltip(pose, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && inside(x, y, leftPos + 155, topPos + 5, 14, 14)) {
            Minecraft.getInstance().setScreen(new TechFurnaceCustomizationScreen(menu));
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
