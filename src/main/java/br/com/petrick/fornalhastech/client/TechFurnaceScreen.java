package br.com.petrick.fornalhastech.client;
import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
public class TechFurnaceScreen extends AbstractContainerScreen<TechFurnaceMenu>{
 private static final ResourceLocation TEX=new ResourceLocation(FornalhasTech.MODID,"textures/gui/tech_furnace.png");
 public TechFurnaceScreen(TechFurnaceMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=194;}
 @Override protected void renderBg(com.mojang.blaze3d.vertex.PoseStack pose,float partial,int mx,int my){RenderSystem.setShaderTexture(0,TEX);blit(pose,leftPos,topPos,0,0,imageWidth,imageHeight);}
 @Override protected void renderLabels(com.mojang.blaze3d.vertex.PoseStack pose,int mx,int my){font.draw(pose,title,8,6,0x404040);font.draw(pose,playerInventoryTitle,8,96,0x404040);}
 @Override public void render(com.mojang.blaze3d.vertex.PoseStack p,int mx,int my,float delta){renderBackground(p);super.render(p,mx,my,delta);renderTooltip(p,mx,my);}
}
