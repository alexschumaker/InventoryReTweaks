package net.cobblers.irt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Hashtable;

import java.util.ArrayList;
import java.util.List;

public class InventoryReTweaksClient implements ClientModInitializer {
    public static KeyBinding testKeybind;
    private final List<Item> itemList = new ArrayList<>();
    private final Hashtable<String, Integer> itemMap = new Hashtable<>();
    private final Hashtable<String, Integer> categoryMap = new Hashtable<>();
    private final Hashtable<String, Integer[]> basicSortMap = new Hashtable<>();

    @Override
    public void onInitializeClient() {
//        System.out.println(IRTConfig.initConfig() ? "Initializing config." : "Config found.");
//        System.out.println(IRTItemData.itemDB.categoryExists("uncategorized"));
//        System.out.println(Arrays.toString(IRTItemData.itemDB.listCategories()));
//        System.out.println(Arrays.toString(IRTConfig.basicSortMap.keySet().toArray()));

        testKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.irt.keybind1",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.irt.keybinds"
        ));

        // Send keyboard events while menu is open since fabric doesn't for some reason.
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register((screen1, key, scancode, modifiers) -> {
                if (client.player != null && key == GLFW.GLFW_KEY_V) {
                    KeyBinding.onKeyPressed(InputUtil.fromKeyCode(key, scancode));
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (testKeybind.wasPressed()) {
                assert client.player != null;

                var handler = client.player.currentScreenHandler;

                try {
                    if (handler == client.player.playerScreenHandler) {
                        sortPlayerInventory(client.player.playerScreenHandler, client);
                    }
                    else {
                        System.out.println(handler.getType() == ScreenHandlerType.GENERIC_9X3);
                        sortCurrentContainerSegmented(handler, client);
                    }
                } catch (UnsupportedOperationException e) {
                    System.out.println("Shit got fucked.");
                }
            }
        });
    }

    private void sortCurrentContainerSegmented(ScreenHandler currentScreenHandler, MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        int containerSize = getContainerSize(currentScreenHandler);
        List<ItemStack> container = currentScreenHandler.getStacks().stream().toList().subList(0, containerSize);

        List<ItemStack> sortedList = IRTSort.groupedSort(container, containerSize);

        for (int i = 0; i < containerSize; i++) {
            recursiveSortExecute(client, currentScreenHandler, currentScreenHandler.slots, sortedList, i);
        }
    }

    private void sortPlayerInventory(ScreenHandler currentScreenHandler, MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        List<ItemStack> container = currentScreenHandler.getStacks().stream().toList().subList(9, 45);
        List<ItemStack> sortedList = IRTSort.inventorySort(container, "Default");

        for (int i = 0; i < sortedList.size(); i++) {
            inventoryRecursiveSortExecute(client, currentScreenHandler, currentScreenHandler.slots.subList(9, 9 + sortedList.size()), sortedList, i);
        }
    }

    private void recursiveSortExecute(MinecraftClient client, ScreenHandler currentScreenHandler, DefaultedList<Slot> container, List<ItemStack> sortedList, int currentSlot) {
        ItemStack sortedItem;

        if (currentSlot >= sortedList.size()) {
            sortedItem = new ItemStack(Items.AIR);
        }
        else {
            sortedItem = sortedList.get(currentSlot);
        }

        // If we have the correct item or are empty, then we're done
        Slot currentContainerItem = container.get(currentSlot);
        if (currentContainerItem.getStack() == sortedItem || currentContainerItem.getStack().isEmpty()) {
            return;
        }

//        System.out.println(container.get(currentContainerSlot).getStack().getItem() + " -> " + container.get(sortedList.indexOf(container.get(currentContainerSlot).getStack())).getStack().getItem() + " | " + currentSlot + " -> " + sortedList.indexOf(container.get(currentContainerSlot).getStack()));
        swapContainerSlots(client, currentScreenHandler, currentSlot, sortedList.indexOf(currentContainerItem.getStack()));
        recursiveSortExecute(client, currentScreenHandler, container, sortedList, currentSlot);
    }

    private void inventoryRecursiveSortExecute(MinecraftClient client, ScreenHandler currentScreenHandler, List<Slot> container, List<ItemStack> sortedList, int currentSlot) {
        ItemStack sortedItem;

        if (currentSlot >= sortedList.size()) {
            sortedItem = new ItemStack(Items.AIR);
        }
        else {
            sortedItem = sortedList.get(currentSlot);
        }

        // If we have the correct item or are empty, then we're done
        ItemStack currentContainerItem = container.get(currentSlot).getStack();
        if (currentContainerItem == sortedItem || currentContainerItem.isEmpty()) {
            return;
        }

//        System.out.println(container.get(currentSlot).getStack().getItem() + " -> " + container.get(sortedList.indexOf(container.get(currentSlot).getStack())).getStack().getItem() + " | " + currentSlot + " -> " + sortedList.indexOf(container.get(currentSlot).getStack()));
        swapInventorySlots(client, currentScreenHandler, currentSlot, sortedList.indexOf(currentContainerItem));
        inventoryRecursiveSortExecute(client, currentScreenHandler, container, sortedList, currentSlot);
    }

    private int getContainerSize(ScreenHandler handler) {
        int maxContainerSize = 0;
        try {
            if (handler instanceof PlayerScreenHandler) {
                return handler.slots.size();
            }
            else if (handler.getType() == ScreenHandlerType.GENERIC_9X3) {
                maxContainerSize = 27;
            }
            else if (handler.getType() == ScreenHandlerType.GENERIC_9X6) {
                maxContainerSize = 54;
            }
            else {
                System.out.println("Invalid container type.");
                return 0;
            }

        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
            System.out.println("Invalid container type.");
            return 0;
        }

        return maxContainerSize;
    }

    public void swapContainerSlots(@NotNull MinecraftClient client, ScreenHandler currentScreenHandler, int slot1, int slot2) {
        assert client.player != null;
        int maxContainerSize = getContainerSize(currentScreenHandler);

        if (!((0 <= slot1 && slot1 < maxContainerSize) && (0 <= slot2 && slot2 < maxContainerSize))) {
            System.out.println("Slot is out of range for active container.");
            return;
        }

        assert client.interactionManager != null;
        int[] transactions = {slot1, slot2, slot1};

        for (int slot : transactions) {
            if (!(currentScreenHandler.getSlot(slot).getStack().isEmpty() && client.player.getInventory().getStack(0).isEmpty())) {
//                System.out.println(handler.getSlot(slot).getStack().getItem().toString() + " -> " + client.player.getInventory().getStack(0).getItem().toString());
                client.interactionManager.clickSlot(currentScreenHandler.syncId, slot, 0, SlotActionType.SWAP, client.player);
            }
        }
    }

    public void swapInventorySlots(@NotNull MinecraftClient client, ScreenHandler currentScreenHandler, int slot1, int slot2) {
        assert client.player != null;
        slot1 = slot1 + 9;
        slot2 = slot2 + 9;
        if (slot2 > 35) slot2 = slot2 - 36;

        int maxContainerSize = getContainerSize(currentScreenHandler);

        if (!((0 <= slot1 && slot1 < maxContainerSize) && (0 <= slot2 && slot2 < maxContainerSize))) {
            System.out.println("Slot is out of range for active container.");
            return;
        }

        assert client.interactionManager != null;
//        System.out.println(currentScreenHandler.getSlot(slot1).getStack().getItem().toString() + " -> " + client.player.getInventory().getStack(slot2).getItem().toString());
        client.interactionManager.clickSlot(currentScreenHandler.syncId, slot1, slot2, SlotActionType.SWAP, client.player);
    }
}