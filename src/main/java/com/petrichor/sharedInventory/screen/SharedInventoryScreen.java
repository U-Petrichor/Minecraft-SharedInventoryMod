package com.petrichor.sharedInventory.screen;

import com.petrichor.sharedInventory.SharedInventoryMod;
import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.ToolType;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import com.petrichor.sharedInventory.network.LabelUpdatePayload;
import com.petrichor.sharedInventory.network.ToolUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
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
    private static final Identifier TEXTURE = Identifier.of("shared_inventory_mod", "textures/gui/shared_inventory.png");
    private static final Identifier FURNACE_LIT_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/lit_progress");
    private static final Identifier FURNACE_BURN_PROGRESS_TEXTURE = Identifier.ofVanilla("container/furnace/burn_progress");
    private static final Identifier BREWING_FUEL_LENGTH_TEXTURE = Identifier.ofVanilla("container/brewing_stand/fuel_length");
    private static final Identifier BREWING_BREW_PROGRESS_TEXTURE = Identifier.ofVanilla("container/brewing_stand/brew_progress");
    private static final Identifier BREWING_BUBBLES_TEXTURE = Identifier.ofVanilla("container/brewing_stand/bubbles");

    // 工作台叠加纹理 — 每种工具类型对应一张 overlay，绘制在工具区域 (221,178) 上方
    private static final Identifier CRAFTING_OVERLAY = Identifier.of("shared_inventory_mod", "textures/gui/crafting_overlay.png");
    private static final Identifier FURNACE_OVERLAY  = Identifier.of("shared_inventory_mod", "textures/gui/furnace_overlay.png");
    private static final Identifier BREWING_OVERLAY  = Identifier.of("shared_inventory_mod", "textures/gui/brewing_overlay.png");
    private static final Identifier ANVIL_OVERLAY    = Identifier.of("shared_inventory_mod", "textures/gui/anvil_overlay.png");
    private static final Identifier SMITHING_OVERLAY = Identifier.of("shared_inventory_mod", "textures/gui/smithing_overlay.png");

    // 工具叠加区域尺寸 (与 ScreenHandler 中工具区域一致: 221,178 ~ 301,234)
    private static final int OVERLAY_REGION_X = 221;
    private static final int OVERLAY_REGION_Y = 178;
    private static final int OVERLAY_REGION_W = 80;
    private static final int OVERLAY_REGION_H = 56;

    // 纹理实际尺寸
    private static final int TEXTURE_WIDTH = 320;
    private static final int TEXTURE_HEIGHT = 256;

    // 标题颜色：更亮的白色
    // 1.21.7 GUI 渲染使用完整 ARGB；必须显式提供不透明 Alpha。
    private static final int TITLE_COLOR = 0xFFE0E0E0;

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
    public void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        // 主背景纹理 — 1.21.2 API: 需要 Function<Identifier, RenderLayer>
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // 工作台叠加纹理 — 根据 activeTool 绘制对应的 overlay
        ToolType activeTool = this.handler.getActiveTool();
        Identifier overlayTexture = getOverlayTexture(activeTool);
        int overlayX = i + OVERLAY_REGION_X;
        int overlayY = j + OVERLAY_REGION_Y;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, overlayTexture, overlayX, overlayY, 0, 0, OVERLAY_REGION_W, OVERLAY_REGION_H, OVERLAY_REGION_W, OVERLAY_REGION_H);

        // Furnace animated sprites (1.21.2 GUI sprite atlas)
        if (activeTool == ToolType.FURNACE) {
            int furnaceBaseX = i + OVERLAY_REGION_X;
            int furnaceBaseY = j + OVERLAY_REGION_Y;

            if (this.handler.isBurning()) {
                int flameHeight = this.handler.getFuelProgress() + 1;
                // 1.21.2 API: drawGuiTexture(renderLayer, texture, texWidth, texHeight, u, v, x, y, width, height)
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FURNACE_LIT_PROGRESS_TEXTURE, 14, 14, 0, 14 - flameHeight, furnaceBaseX + 3, furnaceBaseY + 35 - flameHeight, 14, flameHeight);
            }

            int cookProgress = this.handler.getCookProgress();
            if (cookProgress > 0) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, FURNACE_BURN_PROGRESS_TEXTURE, 24, 16, 0, 0, furnaceBaseX + 20, furnaceBaseY + 19, cookProgress + 1, 16);
            }
        }

        // Brewing stand animated sprites (1.21.2 GUI sprite atlas)
        if (activeTool == ToolType.BREWING) {
            int brewingBaseX = i + OVERLAY_REGION_X;
            int brewingBaseY = j + OVERLAY_REGION_Y;

            int fuel = this.handler.getBrewFuel();
            int fuelWidth = Math.min(18, (18 * fuel + 20 - 1) / 20);
            if (fuelWidth > 0) {
                // 1.21.2 API: drawGuiTexture(renderLayer, texture, texWidth, texHeight, u, v, x, y, width, height)
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BREWING_FUEL_LENGTH_TEXTURE, 18, 4, 0, 0, brewingBaseX + 20, brewingBaseY + 22, fuelWidth, 4);
            }

            int brewTime = this.handler.getBrewTime();
            if (brewTime > 0) {
                int[] BUBBLE_PROGRESS = {29, 24, 20, 16, 11, 6, 0};
                int bubbleHeight = BUBBLE_PROGRESS[(brewTime / 2) % 7];
                if (bubbleHeight > 0) {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BREWING_BUBBLES_TEXTURE, 12, 29, 0, 29 - bubbleHeight, brewingBaseX + 23, brewingBaseY + 20 - bubbleHeight, 12, bubbleHeight);
                }

                int arrowProgress = (400 - brewTime) * 28 / 400;
                if (arrowProgress > 0) {
                    context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, BREWING_BREW_PROGRESS_TEXTURE, 9, 28, 0, 0, brewingBaseX + 60, brewingBaseY + 2, 9, arrowProgress);
                }
            }
        }

        // 标题文字
        Text privateTitle = Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.private_inventory").copy().formatted(net.minecraft.util.Formatting.BOLD);
        Text publicTitle = Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.public_inventory").copy().formatted(net.minecraft.util.Formatting.BOLD);

        context.drawText(textRenderer, privateTitle, i + 24, j + 20, TITLE_COLOR, false);
        context.drawText(textRenderer, publicTitle, i + 243, j + 21, TITLE_COLOR, false);

        // 页码显示 (123,22)
        context.drawText(textRenderer,
                Text.of(sharedInventoryPlayerEntity.shared$getPrivateInventory().getCurrentPage() + "/" + sharedInventoryPlayerEntity.shared$getPrivateInventory().getPrivateStackMaxPage()),
                i + 123, j + 22, TITLE_COLOR, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        this.handler.setClientCallback(this);

        int guiLeft = (this.width - this.backgroundWidth) / 2;
        int guiTop = (this.height - this.backgroundHeight) / 2;

        // 翻页按钮 (95,19) 开始
        addDrawableChild(ButtonWidget.builder(Text.of("<"), button -> this.handler.onPreviousPageButtonClicked())
                .dimensions(guiLeft + 95, guiTop + 19, 12, 12).build());
        addDrawableChild(ButtonWidget.builder(Text.of(">"), button -> this.handler.onNextPageButtonClicked())
                .dimensions(guiLeft + 109, guiTop + 19, 12, 12).build());

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
                .dimensions(guiLeft + 177, guiTop + 19, 24, 12).build());

        // 工具切换按钮 — 物品图标，1px间距
        int toolBtnX = guiLeft + 213;
        int toolBtnY = guiTop + 146;
        int toolBtnSize = 18;
        int toolBtnSpacing = 1;

        Item[] toolIcons = {
                Items.CRAFTING_TABLE,   // C - 合成台
                Items.FURNACE,          // F - 熔炉
                Items.BREWING_STAND,    // B - 酿造台
                Items.ANVIL,            // A - 铁砧
                Items.SMITHING_TABLE    // S - 锻造台
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
        ClientPlayNetworking.send(new com.petrichor.sharedInventory.network.PageUpdatePayload(page));
    }

    @Override
    public void sendLabelUpdate(int action, int page, String label) {
        ClientPlayNetworking.send(new LabelUpdatePayload(action, page, label));
    }

    private void sendToolUpdate(ToolType tool) {
        this.handler.setActiveTool(tool);
        ClientPlayNetworking.send(new ToolUpdatePayload(tool.ordinal()));
    }
}
