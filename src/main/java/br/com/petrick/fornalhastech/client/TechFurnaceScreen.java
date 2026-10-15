package br.com.petrick.fornalhastech.client;

import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TechFurnaceScreen extends AbstractContainerScreen<TechFurnaceMenu> {
    private static final ResourceLocation TEX = new ResourceLocation(FornalhasTech.MODID, "textures/gui/tech_furnace.png");
    private static final int TEX_WIDTH = 176;
    private static final int TEX_HEIGHT = 220;

    public TechFurnaceScreen(TechFurnaceMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = TEX_WIDTH;
        imageHeight = TEX_HEIGHT;
        inventoryLabelY = 118;
    }

    @Override
    protected void renderBg(com.mojang.blaze3d.vertex.PoseStack pose, float partial, int mx, int my) {
        RenderSystem.setShaderTexture(0, TEX);
        blit(pose, leftPos, topPos, 0, 0, 0, imageWidth, imageHeight, TEX_WIDTH, TEX_HEIGHT);

        // Burn indicator: orange fill rises from the bottom of its outlined track.
        int burn = menu.getBurnProgress();
        if (burn > 0) {
            GuiComponent.fill(pose, leftPos + 77, topPos + 108 - burn, leftPos + 81, topPos + 108, 0xFFFF9B35);
            GuiComponent.fill(pose, leftPos + 78, topPos + 108 - burn, leftPos + 80, topPos + 108 - Math.max(0, burn - 2), 0xFFFFD45A);
        }
        // Processing arrow fills left-to-right while a valid recipe is burning.
        int progress = menu.getCookProgress();
        if (progress > 0) {
            GuiComponent.fill(pose, leftPos + 83, topPos + 64, leftPos + 83 + progress, topPos + 70, 0xFF41C7D8);
            GuiComponent.fill(pose, leftPos + 83, topPos + 64, leftPos + 83 + progress, topPos + 65, 0xFFB8F6FA);
        }
    }

    @Override
    protected void renderLabels(com.mojang.blaze3d.vertex.PoseStack pose, int mx, int my) {
        font.draw(pose, title, 8, 8, 0xE3E8EA);
        font.draw(pose, playerInventoryTitle, 8, inventoryLabelY, 0xC5CED2);
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.PoseStack pose, int mx, int my, float delta) {
        renderBackground(pose);
        super.render(pose, mx, my, delta);
        renderTooltip(pose, mx, my);
    }
}
