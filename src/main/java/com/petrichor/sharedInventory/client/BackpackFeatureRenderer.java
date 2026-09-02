package com.petrichor.sharedInventory.client;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.item.SharedInventoryBackpack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * 背包渲染器 — 在穿着共享背包的玩家背上渲染 3D 物品模型
 *
 * 通过 PlayerEntityRendererMixin 注入到玩家渲染流程中。
 * 避免第一人称视角下渲染（玩家看不到自己的背），缩放至 60% 并偏移至背部位置。
 */
public class BackpackFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public BackpackFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity entity, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {
        ItemStack backpackStack = ItemStack.EMPTY;
        if (entity instanceof SharedInventoryPlayerEntity sharedPlayer) {
            backpackStack = sharedPlayer.shared$getBackpackStack();
        }
        if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof SharedInventoryBackpack)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == entity && client.options.getPerspective().isFirstPerson()) return;

        matrices.push();

        ModelPart body = this.getContextModel().body;
        body.rotate(matrices);

        matrices.translate(0.0F, 0.45F, 0.30F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));

        client.getItemRenderer().renderItem(backpackStack,
                ModelTransformationMode.NONE,
                light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);

        matrices.pop();
    }
}
