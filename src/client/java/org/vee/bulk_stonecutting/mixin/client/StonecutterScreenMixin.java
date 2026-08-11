package org.vee.bulk_stonecutting.mixin.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vee.bulk_stonecutting.client.ModConfig;

/// Adds a checkbox to the stonecutter gui that can be checked to enable/disable the bulk stone cutting functionality,
/// and re-places another input stack when the output slot is shift-clicked while it is enabled.
@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin extends AbstractContainerScreen<StonecutterMenu> {
    @Unique
    private static final int CHECKBOX_GAP = 4;
    @Unique
    private static final int SLOT_SIZE = 16;
    @Unique
    private static final int HOTBAR_SIZE = 9;
    @Unique
    private static final int INVENTORY_ROWS_SIZE = 27;
    /// Menu slot index the player inventory starts at, after the input and result slots.
    @Unique
    private static final int MENU_INVENTORY_START = 2;
    /// Button number that makes a THROW click drop the whole stack instead of a single item.
    @Unique
    private static final int DROP_STACK_BUTTON = 1;

    public StonecutterScreenMixin(StonecutterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // StonecutterScreen does not declare init(), so this overrides the inherited AbstractContainerScreen.init()
    // instead of using @Inject, which can only target methods the class declares itself.
    @Override
    protected void init() {
        super.init();

        Checkbox massCraftCheckbox = Checkbox.builder(Component.literal("Mass crafting"), Minecraft.getInstance().font)
                .selected(ModConfig.isMassCraftCheckEnabled())
                .onValueChange((checkbox, selected) -> ModConfig.setMassCraftCheckEnabled(selected))
                .build();
        massCraftCheckbox.setPosition(this.leftPos, this.topPos - massCraftCheckbox.getHeight() - CHECKBOX_GAP);
        this.addRenderableWidget(massCraftCheckbox);
    }

    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z", at = @At("HEAD"), cancellable = true)
    private void bulk_stonecutting$massCraftOnResultClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.isMassCraftCheckEnabled()) return;
        if (event.button() != 0 || !event.hasShiftDown()) return;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        if (player == null || gameMode == null) return;

        StonecutterMenu menu = this.getMenu();
        // Resolve the click from its own coordinates instead of trusting the last rendered hover.
        Slot clickedSlot = bulk_stonecutting$slotAt(menu, event.x(), event.y());
        if (clickedSlot == null || clickedSlot.index != StonecutterMenu.RESULT_SLOT) return;
        if (!clickedSlot.hasItem()) return;

        // Acting while something is on the cursor would swap stacks around instead of crafting.
        if (!menu.getCarried().isEmpty()) return;

        ItemStack inputStack = menu.getSlot(StonecutterMenu.INPUT_SLOT).getItem();
        if (inputStack.isEmpty()) return;

        Inventory inventory = player.getInventory();
        // Without a stack to refill with there is nothing to mass craft, so let vanilla handle the click.
        ItemStack refillTemplate = inputStack.copy();
        if (bulk_stonecutting$findRefillSlot(inventory, refillTemplate) == -1) return;

        // Taking the result can clear the input and reset the selection, so capture both first.
        int selectedRecipe = menu.getSelectedRecipeIndex();
        int containerId = menu.containerId;

        if (bulk_stonecutting$canInventoryAccept(inventory, clickedSlot.getItem())) {
            gameMode.handleContainerInput(containerId, clickedSlot.index, 0, ContainerInput.QUICK_MOVE, player);
        } else {
            gameMode.handleContainerInput(containerId, clickedSlot.index, DROP_STACK_BUTTON, ContainerInput.THROW, player);
        }

        if (bulk_stonecutting$refillInput(menu, gameMode, player, refillTemplate, containerId)) {
            bulk_stonecutting$reselectRecipe(menu, gameMode, player, containerId, selectedRecipe);
        }

        cir.setReturnValue(true);
    }

    /// Puts another input stack into the stonecutter, and reports whether the input was actually refilled.
    @Unique
    private boolean bulk_stonecutting$refillInput(
            StonecutterMenu menu, MultiPlayerGameMode gameMode, LocalPlayer player, ItemStack refillTemplate, int containerId) {
        // Only refill an input that the craft actually emptied, never swap onto a stack that is still there.
        if (!menu.getSlot(StonecutterMenu.INPUT_SLOT).getItem().isEmpty()) return false;
        if (!menu.getCarried().isEmpty()) return false;

        int inventorySlot = bulk_stonecutting$findRefillSlot(player.getInventory(), refillTemplate);
        if (inventorySlot == -1) return false;
        int menuSlot = bulk_stonecutting$toMenuSlot(inventorySlot);

        gameMode.handleContainerInput(containerId, menuSlot, 0, ContainerInput.PICKUP, player);
        if (menu.getCarried().isEmpty()) return false;

        if (menu.getSlot(StonecutterMenu.INPUT_SLOT).getItem().isEmpty()) {
            gameMode.handleContainerInput(containerId, StonecutterMenu.INPUT_SLOT, 0, ContainerInput.PICKUP, player);
        }

        if (!menu.getCarried().isEmpty()) {
            // Put the stack back where it came from rather than leaving it stranded on the cursor.
            gameMode.handleContainerInput(containerId, menuSlot, 0, ContainerInput.PICKUP, player);
            return false;
        }

        return true;
    }

    /// Re-selects the recipe both locally and on the server, since a new input resets the selection.
    @Unique
    private void bulk_stonecutting$reselectRecipe(
            StonecutterMenu menu, MultiPlayerGameMode gameMode, LocalPlayer player, int containerId, int recipeIndex) {
        if (recipeIndex < 0 || recipeIndex >= menu.getNumberOfVisibleRecipes()) return;

        menu.clickMenuButton(player, recipeIndex);
        gameMode.handleInventoryButtonClick(containerId, recipeIndex);
    }

    @Unique
    private Slot bulk_stonecutting$slotAt(StonecutterMenu menu, double x, double y) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, SLOT_SIZE, SLOT_SIZE, x, y)) {
                return slot;
            }
        }
        return null;
    }

    @Unique
    private int bulk_stonecutting$findRefillSlot(Inventory inventory, ItemStack refillTemplate) {
        List<ItemStack> items = inventory.getNonEquipmentItems();
        int found = -1;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, refillTemplate)) continue;
            found = i;
            // Keep looking unless this is already a full stack, so the biggest stack wins.
            if (stack.getCount() >= stack.getMaxStackSize()) break;
        }
        return found;
    }

    /// Mirrors the inventory space check the game uses when a quick move looks for somewhere to put a stack.
    @Unique
    private boolean bulk_stonecutting$canInventoryAccept(Inventory inventory, ItemStack stack) {
        for (ItemStack slotStack : inventory.getNonEquipmentItems()) {
            if (slotStack.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(slotStack, stack)
                    && slotStack.isStackable()
                    && slotStack.getCount() < slotStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private int bulk_stonecutting$toMenuSlot(int inventorySlot) {
        if (inventorySlot < HOTBAR_SIZE) {
            inventorySlot += INVENTORY_ROWS_SIZE;
        } else {
            inventorySlot -= HOTBAR_SIZE;
        }
        return inventorySlot + MENU_INVENTORY_START;
    }
}
