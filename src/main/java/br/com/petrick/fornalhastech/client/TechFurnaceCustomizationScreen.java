package br.com.petrick.fornalhastech.client;

import br.com.petrick.fornalhastech.FornalhasTech;
import br.com.petrick.fornalhastech.menu.TechFurnaceMenu;
import br.com.petrick.fornalhastech.network.ModNetwork;
import br.com.petrick.fornalhastech.network.SetVisualColorPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class TechFurnaceCustomizationScreen extends Screen {
    private static final ResourceLocation TEX=new ResourceLocation(FornalhasTech.MODID,"textures/gui/tech_furnace_customization.png");
    private final TechFurnaceMenu menu;
    private int left,top;
    // 75% of the main screen's 14x14 button, rounded to an integer pixel size.
    private static final int RETURN_X=157, RETURN_Y=7, RETURN_WIDTH=11, RETURN_HEIGHT=11;
    private boolean returning;
    private int returnTicks;
    public TechFurnaceCustomizationScreen(TechFurnaceMenu menu){super(Component.translatable("container.fornalhastech.customization"));this.menu=menu;}
    @Override protected void init(){left=(width-176)/2;top=(height-89)/2;}
    @Override public void render(PoseStack pose,int mx,int my,float delta){renderBackground(pose);RenderSystem.setShaderTexture(0,TEX);GuiComponent.blit(pose,left,top,0,0,0,176,89,176,89);
        int tier=menu.getCoreTier();
        for(int i=0;i<7;i++)if(i>tier)GuiComponent.fill(pose,left+12+i*23,top+41,left+30+i*23,top+59,0xFF393939);
        super.render(pose,mx,my,delta);
        // Cover the old bitmap arrow and draw the same P control as the main GUI.
        GuiComponent.fill(pose,left+154,top+6,left+170,top+20,0xFFC6C6C6);
        neutralFrame(pose,left+RETURN_X,top+RETURN_Y,RETURN_WIDTH,RETURN_HEIGHT);
        pose.pushPose();
        pose.translate(left+RETURN_X+RETURN_WIDTH/2.0F,top+RETURN_Y+RETURN_HEIGHT/2.0F,0);
        pose.scale(0.75F,0.75F,1.0F);
        font.draw(pose,"P",-font.width("P")/2.0F,-font.lineHeight/2.0F,0xFF17191B);
        pose.popPose();
        int selected=Math.max(0,Math.min(6,menu.getVisualColorIndex()));
        frame(pose,left+12+selected*23,top+41,18,18,TechFurnaceScreen.COLORS[selected]);
        Component title=Component.translatable("container.fornalhastech.customization");
        font.draw(pose,title,left+(176-font.width(title))/2.0F,top+8,0xFF111111);
        if(mx>=left+RETURN_X&&mx<left+RETURN_X+RETURN_WIDTH&&my>=top+RETURN_Y&&my<top+RETURN_Y+RETURN_HEIGHT)renderTooltip(pose,Component.literal("Voltar"),mx,my);
        else for(int i=0;i<7;i++)if(mx>=left+12+i*23&&mx<left+30+i*23&&my>=top+41&&my<top+59){renderTooltip(pose,Component.literal(i>tier?"Bloqueada":"Selecionar cor"),mx,my);break;}
    }
    private void frame(PoseStack pose,int x,int y,int w,int h,int color){
        GuiComponent.fill(pose,x,y,x+w,y+1,color);
        GuiComponent.fill(pose,x,y+h-1,x+w,y+h,color);
        GuiComponent.fill(pose,x,y,x+1,y+h,color);
        GuiComponent.fill(pose,x+w-1,y,x+w,y+h,color);
    }
    private void neutralFrame(PoseStack pose,int x,int y,int w,int h){
        GuiComponent.fill(pose,x,y,x+w,y+h,0xFF101010);
        GuiComponent.fill(pose,x+1,y+1,x+w-1,y+2,0xFFF0F0F0);
        GuiComponent.fill(pose,x+1,y+2,x+2,y+h-1,0xFFB8B8B8);
        GuiComponent.fill(pose,x+2,y+h-2,x+w-1,y+h-1,0xFF555555);
        GuiComponent.fill(pose,x+w-2,y+2,x+w-1,y+h-2,0xFF777777);
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(button!=0)return super.mouseClicked(x,y,button);
        if(x>=left+RETURN_X&&x<left+RETURN_X+RETURN_WIDTH&&y>=top+RETURN_Y&&y<top+RETURN_Y+RETURN_HEIGHT){returnToMain();return true;}
        int tier=menu.getCoreTier();for(int i=0;i<7;i++)if(x>=left+12+i*23&&x<left+30+i*23&&y>=top+41&&y<top+59){if(i<=tier && !returning){returning=true;ModNetwork.CHANNEL.sendToServer(new SetVisualColorPacket(menu.getBlockPos(),i));}return true;}
        return super.mouseClicked(x,y,button);
    }
    private void returnToMain(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.player.containerMenu!=menu){mc.setScreen(null);return;}
        if(!returning){returning=true;ModNetwork.CHANNEL.sendToServer(new SetVisualColorPacket(menu.getBlockPos(),-1));}
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(Minecraft.getInstance().options.keyInventory.matches(key,scan)){returnToMain();return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void tick(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.player.containerMenu!=menu)mc.setScreen(null);
        if(returning && ++returnTicks>=40){returning=false;returnTicks=0;}
    }
    @Override public void onClose(){returnToMain();}
}

