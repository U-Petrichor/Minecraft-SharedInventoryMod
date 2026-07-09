package com.umut.sharedInventory.screen;

import com.umut.sharedInventory.SharedInventoryMod;
import com.umut.sharedInventory.inventory.SharedInventoryPlayerEntity;
import com.umut.sharedInventory.inventory.ToolType;
import com.umut.sharedInventory.network.LabelUpdatePacket;
import com.umut.sharedInventory.network.ToolUpdatePacket;
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

public class SharedInventoryScreen extends HandledScreen<SharedInventoryScreenHandler> implements SharedInventoryScreenHandler.ClientCallback {
    private static final Identifier TEXTURE = new Identifier("shared_inventory_mod", "textures/gui/shared_inventory.png");
    private static final Identifier FURNACE_TEXTURE = new Identifier("textures/gui/container/furnace.png");
    private static final Identifier BREWING_TEXTURE = new Identifier("textures/gui/container/brewing_stand.png");

    private static final Identifier CRAFTING_OVERLAY = new Identifier("shared_inventory_mod", "textures/gui/crafting_overlay.png");
    private static final Identifier FURNACE_OVERLAY  = new Identifier("shared_inventory_mod", "textures/gui/furnace_overlay.png");
    private static final Identifier BREWING_OVERLAY  = new Identifier("shared_inventory_mod", "textures/gui/brewing_overlay.png");
    private static final Identifier ANVIL_OVERLAY    = new Identifier("shared_inventory_mod", "textures/gui/anvil_overlay.png");
    private static final Identifier SMITHING_OVERLAY = new Identifier("shared_inventory_mod", "textures/gui/smithing_overlay.png");

    private static final int OVERLAY_REGION_X = 221;
    private static final int OVERLAY_REGION_Y = 178;
    private static final int OVERLAY_REGION_W = 80;
    private static final int OVERLAY_REGION_H = 56;

    private static final int TEXTURE_WIDTH = 320;
    private static final int TEXTURE_HEIGHT = 256;

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
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        ToolType activeTool = this.handler.getActiveTool();
        Identifier overlayTexture = getOverlayTexture(activeTool);
        RenderSystem.setShaderTexture(0, overlayTexture);
        int overlayX = i + OVERLAY_REGION_X;
        int overlayY = j + OVERLAY_REGION_Y;
        this.drawTexture(matrices, overlayX, overlayY, 0, 0, OVERLAY_REGION_W, OVERLAY_REGION_H, OVERLAY_REGION_W, OVERLAY_REGION_H);

        if (activeTool == ToolType.FURNACE) {
            RenderSystem.setShaderTexture(0, FURNACE_TEXTURE);
            int furnaceBaseX = i + OVERLAY_REGION_X;
            int furnaceBaseY = j + OVERLAY_REGION_Y;

            if (this.handler.isBurning()) {
                int m = this.handler.getFuelProgress();
                this.drawTexture(matrices, furnaceBaseX + 3, furnaceBaseY + 34 - m, 176, 13 - m, 14, m + 1);
            }

            this.drawTexture(matrices, furnaceBaseX + 20, furnaceBaseY + 19, 79, 34, 24, 16);
            int n = this.handler.getCookProgress();
            if (n > 0) {
                this.drawTexture(matrices, furnaceBaseX + 20, furnaceBaseY + 19, 176, 14, n + 1, 16);
            }
        }

        if (activeTool == ToolType.BREWING) {
            RenderSystem.setShaderTexture(0, BREWING_TEXTURE);
            int brewingBaseX = i + OVERLAY_REGION_X;
            int brewingBaseY = j + OVERLAY_REGION_Y;

            int fuel = this.handler.getBrewFuel();
            int fuelWidth = Math.min(18, (18 * fuel + 20 - 1) / 20);
            if (fuelWidth > 0) {
                this.drawTexture(matrices, brewingBaseX + 20, brewingBaseY + 22, 176, 29, fuelWidth, 4);
            }

            int brewTime = this.handler.getBrewTime();
            if (brewTime > 0) {
                int[] BUBBLE_PROGRESS = {29, 24, 20, 16, 11, 6, 0};
                int bubbleHeight = BUBBLE_PROGRESS[(brewTime / 2) % 7];
                if (bubbleHeight > 0) {
                    this.drawTexture(matrices, brewingBaseX + 23, brewingBaseY + 20 - bubbleHeight, 185, 29 - bubbleHeight, 12, bubbleHeight);
                }

                int arrowProgress = (400 - brewTime) * 28 / 400;
                if (arrowProgress > 0) {
                    this.drawTexture(matrices, brewingBaseX + 60, brewingBaseY + 2, 176, 0, 9, arrowProgress);
                }
            }
        }

        Text privateTitle = Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.private_inventory").copy().formatted(net.minecraft.util.Formatting.BOLD);
        Text publicTitle = Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.public_inventory").copy().formatted(net.minecraft.util.Formatting.BOLD);

        textRenderer.draw(matrices, privateTitle, i + 24, j + 20, TITLE_COLOR);
        textRenderer.draw(matrices, publicTitle, i + 243, j + 21, TITLE_COLOR);

        textRenderer.draw(matrices,
                Text.literal(sharedInventoryPlayerEntity.shared$getPrivateInventory().getCurrentPage() + "/" + sharedInventoryPlayerEntity.shared$getPrivateInventory().getPrivateStackMaxPage()),
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

        addDrawableChild(new ButtonWidget(
                guiLeft + 95, guiTop + 19,
                12, 12,
                Text.literal("<"),
                button -> this.handler.onPreviousPageButtonClicked()
        ));
        addDrawableChild(new ButtonWidget(
                guiLeft + 109, guiTop + 19,
                12, 12,
                Text.literal(">"),
                button -> this.handler.onNextPageButtonClicked()
        ));

        this.pageInputField = new TextFieldWidget(
                this.textRenderer,
                guiLeft + 155,
                guiTop + 19,
                20,
                12,
                Text.literal("")
        );
        this.pageInputField.setMaxLength(2);
        addDrawableChild(this.pageInputField);

        addDrawableChild(new ButtonWidget(
                guiLeft + 177, guiTop + 19,
                24, 12,
                Text.translatable("screen.shared_inventory_mod.shared_inventory_screen.button3"),
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

        int toolBtnX = guiLeft + 213;
        int toolBtnY = guiTop + 146;
        int toolBtnSize = 18;
        int toolBtnSpacing = 1;

        Item[] toolIcons = {
                Items.CRAFTING_TABLE,
                Items.FURNACE,
                Items.BREWING_STAND,
                Items.ANVIL,
                Items.SMITHING_TABLE
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

        titleX = -9999;
        titleY = -9999;
    }

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
