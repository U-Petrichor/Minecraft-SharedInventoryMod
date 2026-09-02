package com.petrichor.sharedInventory.client;

import com.petrichor.sharedInventory.item.SharedInventoryBackpack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/**
 * 背包渲染器 — 在穿着共享背包的玩家背上渲染 3D 物品模型
 *
 * 1.21.2 架构变更:
 *   - FeatureRenderer 现在接收 EntityRenderState 而非 Entity
 *   - 背包数据通过 BackpackRenderState 扩展传递
 *
 * 通过 PlayerEntityRendererMixin 注入到玩家渲染流程中。
 * 避免第一人称视角下渲染（玩家看不到自己的背），缩放至 60% 并偏移至背部位置。
 */
public class BackpackFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public BackpackFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        // 从扩展的渲染状态获取背包物品栈
        ItemStack backpackStack = ItemStack.EMPTY;
        if (state instanceof BackpackRenderState backpackState) {
            backpackStack = backpackState.getBackpackStack();
        }

        if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof SharedInventoryBackpack)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        // 通过 ID 检查是否是当前玩家（第一人称）
        if (client.player != null && client.player.getId() == state.id && client.options.getPerspective().isFirstPerson()) return;

        matrices.push();

        ModelPart body = this.getContextModel().body;
        body.rotate(matrices);

        matrices.translate(0.0F, 0.45F, 0.30F);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180f));

        client.getItemRenderer().renderItem(backpackStack,
                net.minecraft.item.ModelTransformationMode.NONE,
                light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, null, 0);

        matrices.pop();
    }
}
