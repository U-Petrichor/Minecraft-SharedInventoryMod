package com.umut.sharedInventory.client;

import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.item.SharedInventoryBackpack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

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
        matrices.multiply(net.minecraft.util.math.Vec3f.POSITIVE_Z.getDegreesQuaternion(180));

        client.getItemRenderer().renderItem(backpackStack,
                ModelTransformation.Mode.NONE,
                light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, 0);

        matrices.pop();
    }
}
