package com.petrichor.sharedInventory.client;

import com.petrichor.sharedInventory.item.SharedInventoryBackpack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/**
 * 背包渲染器 — 在穿着共享背包的玩家背上渲染 3D 物品模型
 *
 * 1.21.9 架构变更:
 *   - FeatureRenderer.render 现在接收 OrderedRenderCommandQueue 而非 VertexConsumerProvider
 *   - ItemRenderer.renderItem 签名完全改变，需要使用新的 ItemRenderState 系统
 *   - 暂时禁用 3D 渲染，等待 API 稳定后再实现
 *
 * 通过 PlayerEntityRendererMixin 注入到玩家渲染流程中。
 */
public class BackpackFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    public BackpackFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light,
                       PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        // 从扩展的渲染状态获取背包物品栈
        ItemStack backpackStack = ItemStack.EMPTY;
        if (state instanceof BackpackRenderState backpackState) {
            backpackStack = backpackState.getBackpackStack();
        }

        if (backpackStack.isEmpty() || !(backpackStack.getItem() instanceof SharedInventoryBackpack)) return;

        // TODO: 1.21.9+ 需要使用新的 ItemRenderState API 渲染物品
        // 暂时跳过 3D 背包渲染
    }
}