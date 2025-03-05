package org.vee.bulk_stonecutting.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.MultilineTextWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vee.bulk_stonecutting.client.ResizableCheckbox;

/// Extension class that re-sizes a checkbox if the width/height property is set.
@Mixin(CheckboxWidget.class)
public abstract class CheckboxWidgetResizableMixin extends ClickableWidget implements ResizableCheckbox {
    @Shadow @Final private static Identifier SELECTED_TEXTURE;
    @Shadow @Final private static Identifier TEXTURE;
    @Shadow @Final private MultilineTextWidget textWidget;

    public CheckboxWidgetResizableMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Shadow public abstract boolean isChecked();

    @Unique
    private boolean doResize = false;

    @Override public void bulk_stonecutting$needsResize(boolean resize) {
        doResize = resize;
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void resizeGuiCheckbox(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if(!doResize) return;
        if(getHeight() != getWidth()) return;

        Identifier identifier = isChecked() ? SELECTED_TEXTURE : TEXTURE;
        int size = getHeight();
        context.drawGuiTexture(RenderLayer::getGuiTextured, identifier, getX(), getY(), size, size, ColorHelper.getWhite(this.alpha));

        int j = getX() + size + 4;
        int k = getY() + size / 2 - textWidget.getHeight() / 2;

        // Text resizing attempt does not work :(
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawText(client.textRenderer, Text.literal("Mass Crafting"), j, k, 16777215, true);

        ci.cancel();
    }
}
