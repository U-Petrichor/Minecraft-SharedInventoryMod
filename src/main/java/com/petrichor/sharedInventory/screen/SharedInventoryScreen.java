package com.petrichor.sharedInventory.screen;

import com.petrichor.sharedInventory.SharedInventoryMod;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.ToolType;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import com.petrichor.sharedInventory.network.LabelUpdatePacket;
import com.petrichor.sharedInventory.network.ToolUpdatePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


/**
 * 共享存储界面 — 客户端 GUI 渲染
 *
 * 渲染内容:
 *   - 主背景纹理 (320×256)
 *   - 私人背包标题 + 公共背包标题 + 页码
 *   - 熔炉叠加层 (火焰 + 箭头进度，仅熔炉模式)
 *   - 翻页按钮 (< >) + 页码输入框 + 跳转按钮
 *   - 工具切换按钮 (C/F/B/A/S)
 */
public class SharedInventoryScreen extends HandledScreen<SharedInventoryScreenHandler> implements SharedInventoryScreenHandler.ClientCallback {
    private static final Identifier TEXTURE = new Identifier("shared_inventory_mod", "textures/gui/shared_inventory.png");
    private static final Identifier FURNACE_TEXTURE = new Identifier("textures/gui/container/furnace.png");
    private static final Identifier BREWING_TEXTURE = new Identifier("textures/gui/container/brewing_stand.png");

    // 工作台叠加纹理 — 每种工具类型对应一张 overlay，绘制在工具区域 (221,178) 上方
    private static final Identifier CRAFTING_OVERLAY = new Identifier("shared_inventory_mod", "textures/gui/crafting_overlay.png");
    private static final Identifier FURNACE_OVERLAY  = new Identifier("shared_inventory_mod", "textures/gui/furnace_overlay.png");
    private static final Identifier BREWING_OVERLAY  = new Identifier("shared_inventory_mod", "textures/gui/brewing_overlay.png");
    private static final Identifier ANVIL_OVERLAY    = new Identifier("shared_inventory_mod", "textures/gui/anvil_overlay.png");
    private static final Identifier SMITHING_OVERLAY = new Identifier("shared_inventory_mod", "textures/gui/smithing_overlay.png");

    // 工具叠加区域尺寸 (与 ScreenHandler 中工具区域一致: 221,178 ~ 301,234)
    private static final int OVERLAY_REGION_X = 221;
    private static final int OVERLAY_REGION_Y = 178;
    private static final int OVERLAY_REGION_W = 80;
    private static final int OVERLAY_REGION_H = 56;

    // 纹理实际尺寸
    private static final int TEXTURE_WIDTH = 320;
    private static final int TEXTURE_HEIGHT = 256;

    // 标题颜色：更亮的白色
    private static final int TITLE_COLOR = 0xE0E0E0;

    private SharedInventoryPlayerEntity sharedInventoryPlayerEntity;
    private TextFieldWidget pageInputField;

    public SharedInventoryScreen(SharedInventoryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = TEXTURE_WIDTH;
        this.backgroundHeight = TEXTURE_HEIGHT;
        this.playerInventoryTitleY = -9999;
        if (inventory.player instanceof SharedInventoryPlayerEntity spe)
            this.sharedInventoryPlayerEntity = spe;
        else
            throw new IllegalStateException("Player must implement SharedInventoryPlayerEntity");
    }

    @Override
    public void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        // 精确 UV 坐标绘制，修复非2的幂纹理问题
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // 工作台叠加纹理 — 根据 activeTool 绘制对应的 overlay
        ToolType activeTool = this.handler.getActiveTool();
        Identifier overlayTexture = getOverlayTexture(activeTool);
        RenderSystem.setShaderTexture(0, overlayTexture);
        int overlayX = i + OVERLAY_REGION_X;
        int overlayY = j + OVERLAY_REGION_Y;
        this.drawTexture(matrices, overlayX, overlayY, 0, 0, OVERLAY_REGION_W, OVERLAY_REGION_H, OVERLAY_REGION_W, OVERLAY_REGION_H);

        // 熔炉动态叠加绘制（火焰 + 箭头进度，仅熔炉模式时）
        if (activeTool == ToolType.FURNACE) {
            RenderSystem.setShaderTexture(0, FURNACE_TEXTURE);
            int furnaceBaseX = i + OVERLAY_REGION_X;
            int furnaceBaseY = j + OVERLAY_REGION_Y;

            // Flame — overlay y=19~35 (16px), 火焰底部对齐 y=35，向上增长
            // 原版火焰纹理: src (176, 0), 14×13; 映射到 16px 高度范围
            if (this.handler.isBurning()) {
                int m = this.handler.getFuelProgress(); // 0~13
                int scaledHeight = m * 16 / 13; // 映射到 0~16
                this.drawTexture(matrices, furnaceBaseX + 3, furnaceBaseY + 35 - scaledHeight, 176, 13 - m, 14, m + 1);
            }

            // Arrow — overlay y=19~35 (16px), 中心 y=27
            // 先绘制空箭头背景 (原版 furnace.png (79,34) 处的空箭头, 24×16)
            this.drawTexture(matrices, furnaceBaseX + 20, furnaceBaseY + 19, 79, 34, 24, 16);
            // 再绘制进度填充 (原版 furnace.png (176,14) 处的箭头填充, 24×16)
            int n = this.handler.getCookProgress();
            if (n > 0) {
                this.drawTexture(matrices, furnaceBaseX + 20, furnaceBaseY + 19, 176, 14, n + 1, 16);
            }
        }

        // 酿造台动态叠加绘制（燃料条 + 气泡 + 酿造箭头，仅酿造模式时）
        if (activeTool == ToolType.BREWING) {
            RenderSystem.setShaderTexture(0, BREWING_TEXTURE);
            int brewingBaseX = i + OVERLAY_REGION_X;
            int brewingBaseY = j + OVERLAY_REGION_Y;

            // 燃料条 — overlay (20, 22)~(37, 28), 原版 src (176, 29), 宽 0~18 × 高 4
            // 映射到 18×6 范围: 拉伸高度 4→6
            int fuel = this.handler.getBrewFuel();
            int fuelWidth = Math.min(18, (18 * fuel + 20 - 1) / 20);
            if (fuelWidth > 0) {
                this.drawTexture(matrices, brewingBaseX + 20, brewingBaseY + 22, 176, 29, fuelWidth, 4);
            }

            // 酿造进度
            int brewTime = this.handler.getBrewTime();
            if (brewTime > 0) {
                // 气泡动画 — overlay (23, 20) 上方, 原版 src (185, 0), 宽 12 × 高 0~29
                // 气泡从底部向上增长，底部对齐 y=20
                int[] BUBBLE_PROGRESS = {29, 24, 20, 16, 11, 6, 0};
                int bubbleHeight = BUBBLE_PROGRESS[(brewTime / 2) % 7];
                if (bubbleHeight > 0) {
                    this.drawTexture(matrices, brewingBaseX + 23, brewingBaseY + 20 - bubbleHeight, 185, 29 - bubbleHeight, 12, bubbleHeight);
                }

                // 酿造箭头 — overlay (60, 2), 原版 src (176, 0), 宽 9 × 高 0~28
                int arrowProgress = (400 - brewTime) * 28 / 400;
                if (arrowProgress > 0) {
                    this.drawTexture(matrices, brewingBaseX + 60, brewingBaseY + 2, 176, 0, 9, arrowProgress);
                }
            }
        }

        // 标题文字 - 使用加粗样式和更亮的颜色
        Text privateTitle = Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.private_inventory").copy().formatted(net.minecraft.util.Formatting.BOLD);
        Text publicTitle = Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.public_inventory").copy().formatted(net.minecraft.util.Formatting.BOLD);

        textRenderer.draw(matrices, privateTitle, i + 24, j + 20, TITLE_COLOR);
        textRenderer.draw(matrices, publicTitle, i + 243, j + 21, TITLE_COLOR);

        // 页码显示 (123,22)
        textRenderer.draw(matrices,
                Text.of(sharedInventoryPlayerEntity.shared$getPrivateInventory().getCurrentPage() + "/" + sharedInventoryPlayerEntity.shared$getPrivateInventory().getPrivateStackMaxPage()),
                i + 123, j + 22, TITLE_COLOR);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        this.handler.setClientCallback(this);

        int guiLeft = (this.width - this.backgroundWidth) / 2;
        int guiTop = (this.height - this.backgroundHeight) / 2;

        // 翻页按钮 (95,19) 开始
        addDrawableChild(ButtonWidget.builder(Text.of("<"), button -> this.handler.onPreviousPageButtonClicked())
                .size(12, 12).position(guiLeft + 95, guiTop + 19).build());
        addDrawableChild(ButtonWidget.builder(Text.of(">"), button -> this.handler.onNextPageButtonClicked())
                .size(12, 12).position(guiLeft + 109, guiTop + 19).build());

        // 页码输入框和跳转按钮 (155,19)
        this.pageInputField = new TextFieldWidget(
                this.textRenderer,
                guiLeft + 155,
                guiTop + 19,
                20,
                12,
                Text.of("")
        );
        this.pageInputField.setMaxLength(2);
        addDrawableChild(this.pageInputField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.button3"), button -> {
                    int page;
                    try {
                        page = Integer.parseInt(this.pageInputField.getText());
                        if (page >= 1 && page <= sharedInventoryPlayerEntity.shared$getPrivateInventory().getPrivateStackMaxPage())
                            this.handler.onCurrentButtonClicked(page);
                    } catch (NumberFormatException e) {
                        // ignore
                    } finally {
                        this.pageInputField.setText("");
                    }
                })
                .size(24, 12).position(guiLeft + 177, guiTop + 19).build());

        // 工具切换按钮 — 物品图标，1px间距
        int toolBtnX = guiLeft + 213;
        int toolBtnY = guiTop + 146;
        int toolBtnSize = 18;
        int toolBtnSpacing = 1;

        Item[] toolIcons = {
                Items.CRAFTING_TABLE,   // C - 合成台
                Items.FURNACE,          // 熔炉
                Items.BREWING_STAND,    // 酿造台
                Items.ANVIL,            // 铁砧
                Items.SMITHING_TABLE    // 锻造台
        };
        for (int idx = 0; idx < ToolType.VALUES.length; idx++) {
            ToolType tool = ToolType.VALUES[idx];
            addDrawableChild(new ItemButtonWidget(
                    toolBtnX + idx * (toolBtnSize + toolBtnSpacing),
                    toolBtnY,
                    toolBtnSize,
                    toolBtnSize,
                    toolIcons[idx],
                    button -> sendToolUpdate(tool)
            ));
        }

        // 隐藏默认的窗口标题（Player43 之类）
        titleX = -9999;
        titleY = -9999;
    }

    // === ClientCallback 实现 — 将网络包发送逻辑从 ScreenHandler 移至客户端 Screen ===

    private static Identifier getOverlayTexture(ToolType tool) {
        switch (tool) {
            case FURNACE:  return FURNACE_OVERLAY;
            case BREWING:  return BREWING_OVERLAY;
            case ANVIL:    return ANVIL_OVERLAY;
            case SMITHING: return SMITHING_OVERLAY;
            default:       return CRAFTING_OVERLAY;
        }
    }

    @Override
    public void sendPageUpdate(int page) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeInt(page);
        ClientPlayNetworking.send(SharedInventoryMod.PAGE_UPDATE_ID, buf);
    }

    @Override
    public void sendLabelUpdate(int action, int page, String label) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        LabelUpdatePacket.encode(new LabelUpdatePacket(action, page, label), buf);
        ClientPlayNetworking.send(SharedInventoryMod.LABEL_UPDATE_ID, buf);
    }

    private void sendToolUpdate(ToolType tool) {
        this.handler.setActiveTool(tool);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        ToolUpdatePacket.encode(new ToolUpdatePacket(tool), buf);
        ClientPlayNetworking.send(SharedInventoryMod.TOOL_UPDATE_ID, buf);
    }
}
