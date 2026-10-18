package br.com.petrick.fornalhastech.client;

import br.com.petrick.fornalhastech.block.TechFurnaceBlock;
import br.com.petrick.fornalhastech.blockentity.TechFurnaceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import java.util.Map;
import java.util.WeakHashMap;

/** Two cached texture passes interpolate five neutral frames at the existing recessed depth.
 * Only masked pixels render. There is no static emissive surface underneath in the world model.
 * Each sheet contains N,S,W,E,U in five 64px rows. All metal stays in the baked model.
 */
public final class TechFurnaceEnergyRenderer implements BlockEntityRenderer<TechFurnaceEntity> {
    private static final float D = 0.372F / 16.0F;
    private static final float[][][] VERTICES = {
        {{1,1,D},{1,0,D},{0,0,D},{0,1,D}},
        {{0,1,1-D},{0,0,1-D},{1,0,1-D},{1,1,1-D}},
        {{D,1,0},{D,0,0},{D,0,1},{D,1,1}},
        {{1-D,1,1},{1-D,0,1},{1-D,0,0},{1-D,1,0}},
        {{0,1-D,0},{0,1-D,1},{1,1-D,1},{1,1-D,0}}
    };
    private static final float[][] NORMALS = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0}};
    private static final float[][] UV = {{0,0},{0,1},{1,1},{1,0}};
    // Exactly 24 recessed corner caps, generated from the unchanged cube envelopes.
    private static final float[][][] CORNER_VERTICES = {
        {{-0.028735351562F,0.046630859375F,0.015869140625F},{-0.028735351562F,0.015869140625F,0.015869140625F},{-0.028735351562F,0.015869140625F,0.046630859375F},{-0.028735351562F,0.046630859375F,0.046630859375F}},
        {{0.015869140625F,-0.028735351562F,0.046630859375F},{0.015869140625F,-0.028735351562F,0.015869140625F},{0.046630859375F,-0.028735351562F,0.015869140625F},{0.046630859375F,-0.028735351562F,0.046630859375F}},
        {{0.046630859375F,0.046630859375F,-0.028735351562F},{0.046630859375F,0.015869140625F,-0.028735351562F},{0.015869140625F,0.015869140625F,-0.028735351562F},{0.015869140625F,0.046630859375F,-0.028735351562F}},
        {{-0.028735351562F,0.046630859375F,0.953369140625F},{-0.028735351562F,0.015869140625F,0.953369140625F},{-0.028735351562F,0.015869140625F,0.984130859375F},{-0.028735351562F,0.046630859375F,0.984130859375F}},
        {{0.015869140625F,-0.028735351562F,0.984130859375F},{0.015869140625F,-0.028735351562F,0.953369140625F},{0.046630859375F,-0.028735351562F,0.953369140625F},{0.046630859375F,-0.028735351562F,0.984130859375F}},
        {{0.015869140625F,0.046630859375F,1.028735351562F},{0.015869140625F,0.015869140625F,1.028735351562F},{0.046630859375F,0.015869140625F,1.028735351562F},{0.046630859375F,0.046630859375F,1.028735351562F}},
        {{-0.028735351562F,0.984130859375F,0.015869140625F},{-0.028735351562F,0.953369140625F,0.015869140625F},{-0.028735351562F,0.953369140625F,0.046630859375F},{-0.028735351562F,0.984130859375F,0.046630859375F}},
        {{0.015869140625F,1.028735351562F,0.015869140625F},{0.015869140625F,1.028735351562F,0.046630859375F},{0.046630859375F,1.028735351562F,0.046630859375F},{0.046630859375F,1.028735351562F,0.015869140625F}},
        {{0.046630859375F,0.984130859375F,-0.028735351562F},{0.046630859375F,0.953369140625F,-0.028735351562F},{0.015869140625F,0.953369140625F,-0.028735351562F},{0.015869140625F,0.984130859375F,-0.028735351562F}},
        {{-0.028735351562F,0.984130859375F,0.953369140625F},{-0.028735351562F,0.953369140625F,0.953369140625F},{-0.028735351562F,0.953369140625F,0.984130859375F},{-0.028735351562F,0.984130859375F,0.984130859375F}},
        {{0.015869140625F,1.028735351562F,0.953369140625F},{0.015869140625F,1.028735351562F,0.984130859375F},{0.046630859375F,1.028735351562F,0.984130859375F},{0.046630859375F,1.028735351562F,0.953369140625F}},
        {{0.015869140625F,0.984130859375F,1.028735351562F},{0.015869140625F,0.953369140625F,1.028735351562F},{0.046630859375F,0.953369140625F,1.028735351562F},{0.046630859375F,0.984130859375F,1.028735351562F}},
        {{1.028735351562F,0.046630859375F,0.046630859375F},{1.028735351562F,0.015869140625F,0.046630859375F},{1.028735351562F,0.015869140625F,0.015869140625F},{1.028735351562F,0.046630859375F,0.015869140625F}},
        {{0.953369140625F,-0.028735351562F,0.046630859375F},{0.953369140625F,-0.028735351562F,0.015869140625F},{0.984130859375F,-0.028735351562F,0.015869140625F},{0.984130859375F,-0.028735351562F,0.046630859375F}},
        {{0.984130859375F,0.046630859375F,-0.028735351562F},{0.984130859375F,0.015869140625F,-0.028735351562F},{0.953369140625F,0.015869140625F,-0.028735351562F},{0.953369140625F,0.046630859375F,-0.028735351562F}},
        {{1.028735351562F,0.046630859375F,0.984130859375F},{1.028735351562F,0.015869140625F,0.984130859375F},{1.028735351562F,0.015869140625F,0.953369140625F},{1.028735351562F,0.046630859375F,0.953369140625F}},
        {{0.953369140625F,-0.028735351562F,0.984130859375F},{0.953369140625F,-0.028735351562F,0.953369140625F},{0.984130859375F,-0.028735351562F,0.953369140625F},{0.984130859375F,-0.028735351562F,0.984130859375F}},
        {{0.953369140625F,0.046630859375F,1.028735351562F},{0.953369140625F,0.015869140625F,1.028735351562F},{0.984130859375F,0.015869140625F,1.028735351562F},{0.984130859375F,0.046630859375F,1.028735351562F}},
        {{1.028735351562F,0.984130859375F,0.046630859375F},{1.028735351562F,0.953369140625F,0.046630859375F},{1.028735351562F,0.953369140625F,0.015869140625F},{1.028735351562F,0.984130859375F,0.015869140625F}},
        {{0.953369140625F,1.028735351562F,0.015869140625F},{0.953369140625F,1.028735351562F,0.046630859375F},{0.984130859375F,1.028735351562F,0.046630859375F},{0.984130859375F,1.028735351562F,0.015869140625F}},
        {{0.984130859375F,0.984130859375F,-0.028735351562F},{0.984130859375F,0.953369140625F,-0.028735351562F},{0.953369140625F,0.953369140625F,-0.028735351562F},{0.953369140625F,0.984130859375F,-0.028735351562F}},
        {{1.028735351562F,0.984130859375F,0.984130859375F},{1.028735351562F,0.953369140625F,0.984130859375F},{1.028735351562F,0.953369140625F,0.953369140625F},{1.028735351562F,0.984130859375F,0.953369140625F}},
        {{0.953369140625F,1.028735351562F,0.953369140625F},{0.953369140625F,1.028735351562F,0.984130859375F},{0.984130859375F,1.028735351562F,0.984130859375F},{0.984130859375F,1.028735351562F,0.953369140625F}},
        {{0.953369140625F,0.984130859375F,1.028735351562F},{0.953369140625F,0.953369140625F,1.028735351562F},{0.984130859375F,0.953369140625F,1.028735351562F},{0.984130859375F,0.984130859375F,1.028735351562F}}
    };
    private static final float[][] CORNER_NORMALS = {{-1F,0F,0F},{0F,-1F,0F},{0F,0F,-1F},{-1F,0F,0F},{0F,-1F,0F},{0F,0F,1F},{-1F,0F,0F},{0F,1F,0F},{0F,0F,-1F},{-1F,0F,0F},{0F,1F,0F},{0F,0F,1F},{1F,0F,0F},{0F,-1F,0F},{0F,0F,-1F},{1F,0F,0F},{0F,-1F,0F},{0F,0F,1F},{1F,0F,0F},{0F,1F,0F},{0F,0F,-1F},{1F,0F,0F},{0F,1F,0F},{0F,0F,1F}};
    private static final float CORNER_U = 0.132812500000F;
    private static final float CORNER_V = 0.023437500000F;
    private static final RenderType[] PASSES = new RenderType[EnergyFlowPhase.FRAMES];
    private static final RenderType[] FIRE_PASSES = new RenderType[5];
    private static final float FIRE_LEFT = 4.75F / 16F;
    private static final float FIRE_RIGHT = 11.25F / 16F;
    private static final float FIRE_BOTTOM = 2.50F / 16F;
    private static final float FIRE_TOP = 5.75F / 16F;
    private static final float FIRE_NEAR = 0.371F / 16F;
    private static final float FIRE_FAR = 1F - FIRE_NEAR;
    // One face is selected directly from FACING. This mirrors the blockstate's
    // north/east/south/west model rotation without a second, sign-sensitive yaw.
    private static final float[][] FIRE_NORTH = {
        {FIRE_LEFT,FIRE_TOP,FIRE_NEAR},{FIRE_RIGHT,FIRE_TOP,FIRE_NEAR},
        {FIRE_RIGHT,FIRE_BOTTOM,FIRE_NEAR},{FIRE_LEFT,FIRE_BOTTOM,FIRE_NEAR}
    };
    private static final float[][] FIRE_EAST = {
        {FIRE_FAR,FIRE_TOP,FIRE_LEFT},{FIRE_FAR,FIRE_TOP,FIRE_RIGHT},
        {FIRE_FAR,FIRE_BOTTOM,FIRE_RIGHT},{FIRE_FAR,FIRE_BOTTOM,FIRE_LEFT}
    };
    private static final float[][] FIRE_SOUTH = {
        {FIRE_RIGHT,FIRE_TOP,FIRE_FAR},{FIRE_LEFT,FIRE_TOP,FIRE_FAR},
        {FIRE_LEFT,FIRE_BOTTOM,FIRE_FAR},{FIRE_RIGHT,FIRE_BOTTOM,FIRE_FAR}
    };
    private static final float[][] FIRE_WEST = {
        {FIRE_NEAR,FIRE_TOP,FIRE_RIGHT},{FIRE_NEAR,FIRE_TOP,FIRE_LEFT},
        {FIRE_NEAR,FIRE_BOTTOM,FIRE_LEFT},{FIRE_NEAR,FIRE_BOTTOM,FIRE_RIGHT}
    };
    private static final float[][] FIRE_UV = {{0,0},{1,0},{1,1},{0,1}};
    private static final float[][] FIRE_NORMALS = {{0,0,-1},{1,0,0},{0,0,1},{-1,0,0}};
    static {
        for (int i = 0; i < PASSES.length; i++) {
            PASSES[i] = RenderType.entityTranslucentEmissive(new ResourceLocation(
                "fornalhastech", "textures/block/energy_flow_" + i + ".png"));
        }
        for (int i = 0; i < FIRE_PASSES.length; i++) {
            FIRE_PASSES[i] = RenderType.entityTranslucentEmissive(new ResourceLocation(
                "fornalhastech", "textures/block/fire_panel_" + i + ".png"));
        }
    }
    // Weak keys release clocks when the client unloads/replaces a block entity.
    private final Map<TechFurnaceEntity, EnergyFlowPhase> clocks = new WeakHashMap<>();

    public TechFurnaceEnergyRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TechFurnaceEntity entity, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        if (entity.getLevel() == null) return;
        BlockState state = entity.getBlockState();
        if (state.getValue(TechFurnaceBlock.MODE) == 0 && state.getValue(TechFurnaceBlock.LIT)) {
            int fireFrame = (int) (((entity.getLevel().getGameTime() + (double) partialTick) / 3.0) % 5.0);
            drawFire(poses.last(), buffers.getBuffer(FIRE_PASSES[fireFrame]),
                    state.getValue(TechFurnaceBlock.FACING));
        }
        if (state.getValue(TechFurnaceBlock.MODE) != 0
                || state.getValue(TechFurnaceBlock.VISUAL_COLOR) == 0) return;
        double time = entity.getLevel().getGameTime() + (double) partialTick;
        double phase = clocks.computeIfAbsent(entity, ignored -> new EnergyFlowPhase())
                .sample(time, state.getValue(TechFurnaceBlock.LIT));
        int frame = (int) phase;
        float blend = (float) (phase - frame);
        int color = Minecraft.getInstance().getBlockColors().getColor(state,
                entity.getLevel(), entity.getBlockPos(), 0);
        float r = ((color >> 16) & 255) / 255F;
        float g = ((color >> 8) & 255) / 255F;
        float b = (color & 255) / 255F;
        float rotation = switch (state.getValue(TechFurnaceBlock.FACING)) {
            case EAST -> -90F;
            case SOUTH -> -180F;
            case WEST -> -270F;
            default -> 0F;
        };
        poses.pushPose();
        poses.translate(.5, .5, .5);
        poses.mulPose(Vector3f.YP.rotationDegrees(rotation));
        poses.translate(-.5, -.5, -.5);
        // Opaque current frame, then next-frame alpha: linear interpolation without
        // extra model surfaces. Identical vertices/depth and LEQUAL avoid depth flicker.
        draw(poses.last(), buffers.getBuffer(PASSES[frame]), r, g, b, 1F);
        if (blend > 0F) draw(poses.last(), buffers.getBuffer(PASSES[(frame + 1) % 5]), r, g, b, blend);
        poses.popPose();
    }

    private static void drawFire(PoseStack.Pose pose, VertexConsumer out, Direction facing) {
        float[][] vertices = switch (facing) {
            case EAST -> FIRE_EAST;
            case SOUTH -> FIRE_SOUTH;
            case WEST -> FIRE_WEST;
            default -> FIRE_NORTH;
        };
        float[] normal = FIRE_NORMALS[switch (facing) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        }];
        for (int i = 0; i < vertices.length; i++) {
            float[] p = vertices[i];
            out.vertex(pose.pose(), p[0], p[1], p[2]).color(1F, 1F, 1F, 1F)
                .uv(FIRE_UV[i][0], FIRE_UV[i][1])
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), normal[0], normal[1], normal[2]).endVertex();
        }
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer out, float r, float g, float b, float alpha) {
        for (int face = 0; face < 5; face++) {
            float[] n = NORMALS[face];
            for (int vertex = 0; vertex < 4; vertex++) {
                float[] p = VERTICES[face][vertex];
                out.vertex(pose.pose(), p[0], p[1], p[2]).color(r, g, b, alpha)
                    .uv(UV[vertex][0], (face + UV[vertex][1]) / 5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.normal(), n[0], n[1], n[2]).endVertex();
            }
        }
        for (int face = 0; face < CORNER_VERTICES.length; face++) {
            float[] n = CORNER_NORMALS[face];
            for (float[] p : CORNER_VERTICES[face]) {
                out.vertex(pose.pose(), p[0], p[1], p[2]).color(r, g, b, alpha)
                    .uv(CORNER_U, CORNER_V)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(pose.normal(), n[0], n[1], n[2]).endVertex();
            }
        }
    }

    @Override public int getViewDistance() { return 256; }
}
