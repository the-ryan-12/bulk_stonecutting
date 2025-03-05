package org.vee.bulk_stonecutting.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.StonecutterScreen;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vee.bulk_stonecutting.client.ModConfig;
import org.vee.bulk_stonecutting.client.ResizableCheckbox;

/// Adds a checkbox to the stonecutter gui that can be checked to enable/disable the bulk stone cutting functionality
@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin extends HandledScreen<StonecutterScreenHandler> {
    @Unique
    private CheckboxWidget massCraftCheckbox;
    @Unique
    private final int X_OFFSET = -82;
    @Unique
    private final int Y_OFFSET = -96;
    @Unique
    private final int SIZE = 10;

    public StonecutterScreenMixin(StonecutterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        CheckboxWidget.Builder cbwBuilder = CheckboxWidget.builder(Text.literal("Mass crafting"), client.textRenderer)
                .checked(ModConfig.isMassCraftCheckEnabled())
                .callback(((checkbox, checked) -> ModConfig.setMassCraftCheckEnabled(checked)));
        massCraftCheckbox = cbwBuilder.build();
        this.addDrawableChild(massCraftCheckbox);

        client.execute(() -> {
            int newX = client.getWindow().getScaledWidth()/2 + X_OFFSET;
            int newY = client.getWindow().getScaledHeight()/2 + Y_OFFSET;
            ((ResizableCheckbox) massCraftCheckbox).bulk_stonecutting$needsResize(true);
            massCraftCheckbox.setDimensions(SIZE, SIZE);
            massCraftCheckbox.setPosition(newX, newY);
        });
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if(client == null) return;

        int newX = client.getWindow().getScaledWidth()/2 + X_OFFSET;
        int newY = client.getWindow().getScaledHeight()/2 + Y_OFFSET;

        if(massCraftCheckbox.getX() != newX || massCraftCheckbox.getY() != newY) {
            massCraftCheckbox.setPosition(newX, newY);
        }
        massCraftCheckbox.renderWidget(context, mouseX, mouseY, delta);
    }


}