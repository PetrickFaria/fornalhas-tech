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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TechFurnaceScreen extends AbstractContainerScreen<TechFurnaceMenu> {
    static final ResourceLocation TEX = new ResourceLocation(FornalhasTech.MODID,"textures/gui/tech_furnace.png");
    static final int[] COLORS={0xFF000000,0xFFE83E3E,0xFFE8EFF1,0xFFFFC844,0xFF43D878,0xFF42D8ED,0xFFAD704A};
    public TechFurnaceScreen(TechFurnaceMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=176;imageHeight=230;inventoryLabelY=999;}
    @Override protected void renderBg(PoseStack pose,float partial,int mx,int my){
        RenderSystem.setShaderTexture(0,TEX); blit(pose,leftPos,topPos,0,0,0,imageWidth,imageHeight,imageWidth,imageHeight);
        // Battery fill uses the image's existing horizontal FE bar without modifying its border.
        int filled=(int)(54L*menu.getEnergy()/menu.getMaxEnergy());
        if(filled>0)GuiComponent.fill(pose,leftPos+38,topPos+92,leftPos+38+filled,topPos+97,menu.getAccentColor());
        if(menu.isGenerating()){
            GuiComponent.fill(pose,leftPos+53,topPos+77,leftPos+57,topPos+87,0xFFFFA42A);
            GuiComponent.fill(pose,leftPos+54,topPos+75,leftPos+56,topPos+79,0xFFFFE67A);
        }
        int p=menu.getCookProgress(); if(p>0){
            int w=Math.min(20,p*20/48); GuiComponent.fill(pose,leftPos+92,topPos+78,leftPos+92+w,topPos+84,menu.getAccentColor());
            if(w>0)GuiComponent.fill(pose,leftPos+92,topPos+78,leftPos+92+w,topPos+79,0xFFE7F2F4);
            if(w>=18)GuiComponent.fill(pose,leftPos+108,topPos+76,leftPos+112,topPos+86,menu.getAccentColor());
        }
    }
    private void frame(PoseStack p,int x,int y,int w,int h,int c){
        GuiComponent.fill(p,leftPos+x,topPos+y,leftPos+x+w,topPos+y+1,c);
        GuiComponent.fill(p,leftPos+x,topPos+y+h-1,leftPos+x+w,topPos+y+h,c);
        GuiComponent.fill(p,leftPos+x,topPos+y,leftPos+x+1,topPos+y+h,c);
        GuiComponent.fill(p,leftPos+x+w-1,topPos+y,leftPos+x+w,topPos+y+h,c);
    }
    @Override protected void renderLabels(PoseStack pose,int mx,int my) { }
    @Override public void render(PoseStack pose,int mx,int my,float delta){renderBackground(pose);super.render(pose,mx,my,delta);
        int tier=menu.getCoreTier(); if(tier>0){int c=COLORS[tier]; frame(pose,79,19,18,18,c);frame(pose,47,52,18,18,c);frame(pose,47,102,18,18,c);frame(pose,123,69,26,26,c);}
        if(mx>=leftPos+37&&mx<leftPos+95&&my>=topPos+90&&my<topPos+99)renderTooltip(pose,Component.literal(menu.getEnergy()+" / "+menu.getMaxEnergy()+" FE"),mx,my);
        else renderTooltip(pose,mx,my);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0&&inside(x,y,leftPos+155,topPos+6,14,14)){
            Minecraft.getInstance().setScreen(new TechFurnaceCustomizationScreen(menu));return true;
        }
        return super.mouseClicked(x,y,button);
    }
    private static boolean inside(double x,double y,int l,int t,int w,int h){return x>=l&&x<l+w&&y>=t&&y<t+h;}
}
