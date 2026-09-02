package com.petrichor.sharedInventory.client;

import com.petrichor.sharedInventory.item.SharedInventoryBackpack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * 背包渲染器 — 在穿着共享背包的玩家背上渲染 3D 物品模型
 *
 * 1.21.9 架构:
 *   - updateRenderState 阶段创建 ItemRenderState
 *   - render 阶段将 ItemRenderState 提交到 OrderedRenderCommandQueue
 *
 * 通过 PlayerEntityRendererMixin 注入到玩家渲染流程中。
 */
public class BackpackFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public BackpackFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        // 从扩展的渲染状态获取背包物品栈
        ItemStack backpackStack = ItemStack.EMPTY;
        if (state instanceof BackpackRenderState backpackState) {
            backpackStack = backpackState.getBackpackStack();
        }

        if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof SharedInventoryBackpack)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null
                && client.player.getId() == state.id
                && client.options.getPerspective().isFirstPerson()) {
            return;
        }

        ItemRenderState backpackItemRenderState =
                ((BackpackRenderState) state).getBackpackItemRenderState();
        if (backpackItemRenderState.isEmpty()) return;

        matrices.push();
        matrices.translate(0.0F, 0.45F, 0.30F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        backpackItemRenderState.render(
                matrices,
                queue,
                light,
                OverlayTexture.DEFAULT_UV,
                state.outlineColor
        );
        matrices.pop();
    }
}
