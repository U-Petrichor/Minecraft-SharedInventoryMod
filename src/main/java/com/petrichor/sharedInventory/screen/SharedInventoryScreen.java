package com.petrichor.sharedInventory.screen;

import com.petrichor.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.petrichor.sharedInventory.inventory.ToolType;
import com.petrichor.sharedInventory.inventory.PrivateInventory;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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
public class SharedInventoryScreen extends HandledScreen<SharedInventoryScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("shared_inventory_mod", "textures/gui/shared_inventory.png");
    private static final Identifier FURNACE_TEXTURE = new Identifier("textures/gui/container/furnace.png");

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
        // 隐藏默认的 "物品栏" 标题
        this.playerInventoryTitleY = -9999;
        if (inventory.player instanceof SharedInventoryPlayerEntity)
            this.sharedInventoryPlayerEntity = (SharedInventoryPlayerEntity) inventory.player;
    }

    @Override
    public void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        // 精确 UV 坐标绘制，修复非2的幂纹理问题
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // 熔炉叠加绘制（仅在熔炉模式时）
        if (this.handler.getActiveTool() == ToolType.FURNACE) {
            RenderSystem.setShaderTexture(0, FURNACE_TEXTURE);
            int furnaceBaseX = i + 221;
            int furnaceBaseY = j + 178;
            // Input Slot
            this.drawTexture(matrices, furnaceBaseX, furnaceBaseY, 55, 16, 18, 18);
            // Fuel Slot
            this.drawTexture(matrices, furnaceBaseX, furnaceBaseY + 36, 55, 52, 18, 18);
            // Output Slot (large slot 26x26)
            this.drawTexture(matrices, furnaceBaseX + 38, furnaceBaseY + 18, 111, 31, 26, 26);

            // Flame
            if (this.handler.isBurning()) {
                int m = this.handler.getFuelProgress();
                this.drawTexture(matrices, furnaceBaseX + 2, furnaceBaseY + 29 + 12 - m, 56, 36 + 12 - m, 14, m + 1);
            }

            // Arrow
            int n = this.handler.getCookProgress();
            this.drawTexture(matrices, furnaceBaseX + 22, furnaceBaseY + 18, 176, 14, n + 1, 16);
        }

        // 标题文字 - 使用加粗样式和更亮的颜色
        Text privateTitle = new TranslatableText("screen.shared_inventory_mod.shared_inventory_screen.private_inventory").shallowCopy().formatted(net.minecraft.util.Formatting.BOLD);
        Text publicTitle = new TranslatableText("screen.shared_inventory_mod.shared_inventory_screen.public_inventory").shallowCopy().formatted(net.minecraft.util.Formatting.BOLD);

        textRenderer.draw(matrices, privateTitle, i + 24, j + 20, TITLE_COLOR);
        textRenderer.draw(matrices, publicTitle, i + 243, j + 21, TITLE_COLOR);

        // 页码显示
        textRenderer.draw(matrices,
                Text.of(sharedInventoryPlayerEntity.shared$getPrivateInventory().getCurrentPage() + "/" + sharedInventoryPlayerEntity.shared$getPrivateInventory().getPrivateStackMaxPage()),
                i + 200, j + 22, TITLE_COLOR);
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

        int guiLeft = (this.width - this.backgroundWidth) / 2;
        int guiTop = (this.height - this.backgroundHeight) / 2;

        // 翻页按钮，从 (152,22) 开始
        addDrawableChild(new ButtonWidget(
                guiLeft + 152, guiTop + 22,
                12, 12,
                Text.of("<"),
                button -> this.handler.onPreviousPageButtonClicked()
        ));
        addDrawableChild(new ButtonWidget(
                guiLeft + 164, guiTop + 22,
                12, 12,
                Text.of(">"),
                button -> this.handler.onNextPageButtonClicked()
        ));

        // 页码输入框和跳转按钮
        this.pageInputField = new TextFieldWidget(
                this.textRenderer,
                guiLeft + 178,
                guiTop + 22,
                20,
                12,
                Text.of("")
        );
        this.pageInputField.setMaxLength(2);
        addDrawableChild(this.pageInputField);

        addDrawableChild(new ButtonWidget(
                guiLeft + 152, guiTop + 22 + 14,
                24, 12,
                new TranslatableText("screen.shared_inventory_mod.shared_inventory_screen.button3"),
                button -> {
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
                }
        ));

        // 工具切换按钮，位于 (213,154) 区域
        int toolBtnX = guiLeft + 213;
        int toolBtnY = guiTop + 154;
        int toolBtnWidth = 16;
        int toolBtnHeight = 12;
        int toolBtnSpacing = 2;

        String[] toolLabels = {"C", "F", "B", "A", "S"}; // Crafting, Furnace, Brewing, Anvil, Smithing
        for (int idx = 0; idx < ToolType.VALUES.length; idx++) {
            ToolType tool = ToolType.VALUES[idx];
            addDrawableChild(new ButtonWidget(
                    toolBtnX + idx * (toolBtnWidth + toolBtnSpacing),
                    toolBtnY,
                    toolBtnWidth,
                    toolBtnHeight,
                    Text.of(toolLabels[idx]),
                    button -> this.handler.setActiveTool(tool)
            ));
        }

        // 隐藏默认的窗口标题（Player43 之类）
        titleX = -9999;
        titleY = -9999;
    }
}
