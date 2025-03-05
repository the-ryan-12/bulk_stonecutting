package org.vee.bulk_stonecutting.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vee.bulk_stonecutting.client.ModConfig;

/// Checks for a shift click in the output slot of a stonecutter, and if the checkbox is enabled, re-places another input stack.
@Mixin(Mouse.class)
public class StonecutterMouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Make sure player is in game and left-clicked with l shift
        if(client.currentScreen == null || client.player == null) return;
        ClientPlayerEntity player = client.player;
        if(button != 0) return;
        if(action != 1) return;
        if(GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) != GLFW.GLFW_PRESS) return;

        // Verify current screen is the stone cutter's
        if(!(client.currentScreen instanceof HandledScreen<?> activeScreen)) return;
        if(!(activeScreen.getScreenHandler() instanceof StonecutterScreenHandler scHandler)) return;

        // id 0 is input, id 1 is output ONLY on stone cutter screen
        Slot hoveredSlot = ((HandledScreenAccessor)activeScreen).getFocusedSlot();
        if(hoveredSlot == null || hoveredSlot.id != 1) return;
        if(!hoveredSlot.hasStack()) return;

        if(client.interactionManager == null) return;
        ScreenHandler handler = player.currentScreenHandler;

        boolean isMassCraftEnabled = ModConfig.isMassCraftCheckEnabled();
        if(!isMassCraftEnabled) return;

        ItemStack inputStack = handler.getSlot(0).getStack();
        if(inputStack.isEmpty()) return;

        int invSlot = -1;
        for(int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            // Check if the item is the same
            if (stack.getItem() == inputStack.getItem()) {
                invSlot = i;
                // If the stack isn't a max stack, keep checking in case we do find a max stack
                if(stack.getCount() >= stack.getMaxCount())
                    break;
            }
        }
        // If no stacks found, return
        if(invSlot == -1) return;
        invSlot = getScreenHandlerSlot(invSlot);

        int recipeSelected = scHandler.getSelectedRecipe();

        if(isInventoryFull(player.getInventory())) {
            for(int i = 0; i < inputStack.getMaxCount(); i++) {
                client.interactionManager.clickSlot(scHandler.syncId, hoveredSlot.id, 0, SlotActionType.THROW, client.player);
            }
        } else {
            // Do the regular shift+left click action
            client.interactionManager.clickSlot(scHandler.syncId, hoveredSlot.id, 0, SlotActionType.QUICK_MOVE, client.player);
        }


        // Grab new stack
        client.interactionManager.clickSlot(scHandler.syncId, invSlot, 0, SlotActionType.PICKUP, client.player);

        // Place in stone cutter if it was grabbed properly
        if(!scHandler.getCursorStack().isEmpty()) {
            client.interactionManager.clickSlot(scHandler.syncId, 0, 0, SlotActionType.PICKUP, client.player);
        }

        // Click the recipe button again
        client.interactionManager.clickButton(scHandler.syncId, recipeSelected);

        ci.cancel();
    }

    @Unique
    private int getScreenHandlerSlot(int invSlot) {
        if (invSlot < 9) {
            invSlot += 27;
        } else {
            invSlot -= 9;
        }
        invSlot += 2;
        return invSlot;
    }

    @Unique
    private boolean isInventoryFull(PlayerInventory inventory) {
        for(ItemStack stack : inventory.main) {
            if(stack.isEmpty()) return false;
        }
        return true;
    }
}
