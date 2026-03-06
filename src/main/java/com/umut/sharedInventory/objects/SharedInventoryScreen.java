package com.umut.sharedInventory.objects;

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



public class SharedInventoryScreen extends HandledScreen<SharedInventoryScreenHandler> {
    // GUI 纹理的路径，本例中使用发射器中的纹理
    //1.19以下和以上是不一样的,这个是1.18.2的写法,1.19以上是 Identifier.ofVanilla("minecraft", "textures/gui/container/dispenser.png");
    private static final Identifier TEXTURE = new Identifier("shared_inventory_mod", "textures/gui/shared_inventory.png");
    private final Text privateInventory=new TranslatableText("screen.shared_inventory_mod.shared_inventory_screen.private_inventory");
    private SharedInventoryPlayerEntity shardInventoryPlayerEntity;
    private TextFieldWidget pageInputField;

    public SharedInventoryScreen(SharedInventoryScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth=256;
        this.backgroundHeight=256;
        this.playerInventoryTitleY = 165;
        if(inventory.player instanceof SharedInventoryPlayerEntity)
            this.shardInventoryPlayerEntity =(SharedInventoryPlayerEntity)inventory.player;
    }



    //drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY)：
    //
    //这是 Minecraft 1.17 到 1.19.2 的写法。
    //
    //使用 MatrixStack 来处理 GUI 的渲染。
    //
    //drawBackground(DrawContext context, float delta, int mouseX, int mouseY)：
    //
    //这是 Minecraft 1.20 及以上版本 的写法。
    //
    //使用 DrawContext 来处理 GUI 的渲染，这是一个更高级的封装类，简化了渲染逻辑。
    @Override
    public void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY){
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        textRenderer.draw(matrices, privateInventory, i+8, j+6, 0x404040);
        textRenderer.draw(matrices, Text.of(shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().getCurrentPage()+"/20"), i+48, j+6, 0x404040);
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

        this.pageInputField = new TextFieldWidget(
                this.textRenderer,
                ((this.width - this.backgroundWidth) / 2) + 146,
                ((this.height - this.backgroundHeight) / 2) + 162,
                20,
                12,
                Text.of(shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().getCurrentPage()+"")
        );
        this.pageInputField.setMaxLength(2);
        addDrawableChild(this.pageInputField);

        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        //布置切换页数的按钮
        addDrawableChild(new ButtonWidget(
                ((this.width - this.backgroundWidth) / 2)+64, // X 坐标
                ((this.height - this.backgroundHeight) / 2)+162, // Y 坐标
                26, // 宽度
                12, // 高度
                new TranslatableText("screen.shared_inventory_mod.shared_inventory_screen.button1"),
                button -> this.handler.onPrevious_PageButtonClicked()
        ));
        addDrawableChild(new ButtonWidget(
                ((this.width - this.backgroundWidth) / 2)+92, // X 坐标
                ((this.height - this.backgroundHeight) / 2)+162, // Y 坐标
                26, // 宽度
                12, // 高度
                new TranslatableText("screen.shared_inventory_mod.shared_inventory_screen.button2"),
                button -> this.handler.onNext_PageButtonClicked()
        ));

        addDrawableChild(new ButtonWidget(
                ((this.width - this.backgroundWidth) / 2)+120, // X 坐标
                ((this.height - this.backgroundHeight) / 2)+162, // Y 坐标
                24, // 宽度
                12, // 高度
                new TranslatableText("screen.shared_inventory_mod.shared_inventory_screen.button3"),
                button -> {
                    int page;
                    try {
                        page = Integer.parseInt(this.pageInputField.getText());
                        if (page >= 1 && page <= shardInventoryPlayerEntity.shared_inventory1_18_2$getPrivateInventory().getPrivateStackMaxPage())
                            this.handler.onCurrentButtonClicked(page); // 调用处理逻辑
                    } catch (NumberFormatException e) {

                    }
                    finally {
                        this.pageInputField.setText(""); // 清空输入框
                    }
                }
        ));
    }
}
