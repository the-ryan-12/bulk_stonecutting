package org.vee.bulk_stonecutting.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Checkbox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.vee.bulk_stonecutting.client.ResizableCheckbox;

/// Swaps the box size a checkbox renders with, which also shifts the label since it is laid out from the box edge.
@Mixin(Checkbox.class)
public abstract class CheckboxResizableMixin implements ResizableCheckbox {
    @Unique
    private int bulk_stonecutting$boxSize = -1;

    @Override
    public void bulk_stonecutting$setBoxSize(int boxSize) {
        this.bulk_stonecutting$boxSize = boxSize;
    }

    @Redirect(
            method = "extractContents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Checkbox;getBoxSize(Lnet/minecraft/client/gui/Font;)I"))
    private int bulk_stonecutting$customBoxSize(Font font) {
        return this.bulk_stonecutting$boxSize > 0 ? this.bulk_stonecutting$boxSize : Checkbox.getBoxSize(font);
    }
}
