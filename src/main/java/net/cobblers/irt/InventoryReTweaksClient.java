package net.cobblers.irt;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.impl.client.screen.ScreenEventFactory;
import net.fabricmc.fabric.mixin.screen.ScreenAccessor.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.data.Main;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Hashtable;

import java.util.ArrayList;
import java.util.List;

public class InventoryReTweaksClient implements ClientModInitializer {
    public static KeyBinding testKeybind;
    private int moveSlot = 0;
    private final List<Item> itemList = new ArrayList<>();
    private final Hashtable<String, Integer> itemMap = new Hashtable<>();
    private final Hashtable<String, Integer> categoryMap = new Hashtable<>();
    private final Hashtable<String, Integer[]> basicSortMap = new Hashtable<>();

    @Override
    public void onInitializeClient() {
        System.out.println(IRTConfig.initConfig() ? "Initializing config." : "Config found.");

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

                PlayerInventory inv = client.player.getInventory();
                System.out.println(inv.size() + " | " + client.player.currentScreenHandler.slots);
//                System.out.println(moveSlot);

                if (moveSlot >= client.player.currentScreenHandler.slots.size()) {
                    moveSlot = 0;
                }

                try {
                    System.out.println(client.player.currentScreenHandler.getType() == ScreenHandlerType.GENERIC_9X3);
                } catch (UnsupportedOperationException e) {
                    System.out.println("Shit got fucked.");
                }

                DefaultedList<ItemStack> screenStacks = client.player.currentScreenHandler.getStacks();
                DefaultedList<Slot> screenSlots = client.player.currentScreenHandler.slots;

                sortCurrentContainerSegmented(client);

//                for (int i = 0; i < inv.size(); i++) {
////                    System.out.println("Current Slot: " + i + " -> " + client.player.playerScreenHandler.getSlotIndex(inv, i));
//                    ItemStack itemStack = inv.getStack(i);
//
//                    if (!itemStack.getItem().equals(Items.AIR)) {
//                        System.out.println("Item Found: " + i + " | Moving to " + moveSlot);
//
//                        assert client.interactionManager != null;
//                        int syncID = client.player.currentScreenHandler.syncId;
//                        client.interactionManager.clickSlot(syncID, moveSlot, i, SlotActionType.SWAP, client.player);
////                        client.interactionManager.clickSlot(syncID, i, 0, SlotActionType.PICKUP_ALL, client.player);
//
//                    }
//                }
//                moveSlot++;

                IRTConfig.writeDefaultItemDB(IRTSortMap.itemDB);
                System.out.println(IRTSortMap.itemDB.categoryExists("uncategorized"));
                System.out.println(Arrays.toString(IRTSortMap.itemDB.listCategories()));
            }
        });
    }

    private void sortCurrentContainerSegmented(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        int containerSize = getContainerSize(client.player.currentScreenHandler);
        List<ItemStack> container = client.player.currentScreenHandler.getStacks().stream().toList().subList(0, containerSize);

        List<ItemStack> sortedList = IRTSort.groupedSort(container, containerSize);

        for (int i = 0; i < containerSize; i++) {
            recursiveSortExecute(client, client.player.currentScreenHandler.slots, sortedList, i);
        }
    }

//    private void sortCurrentContainerBasic(MinecraftClient client) {
//        if (client.player == null) {
//            System.out.println("Player entity not found.");
//            return;
//        }
//
//        ScreenHandler handler = client.player.currentScreenHandler;
//        DefaultedList<Slot> container = handler.slots;
//
//        int containerSize = getContainerSize(handler);
//        if (containerSize == 0) {
//            System.out.println("No container to sort.");
//            return;
//        }
//
//        // Prepare the sorted list.
//        List<ItemStack> sortedList = new ArrayList<>();
//        for (int i = 0; i < containerSize; i++) {
//            Item item = container.get(i).getStack().getItem();
//            boolean sorted = false;
//            if (container.get(i).getStack().isEmpty()) {
//                continue;
//            }
//
//            Integer[] sortPrio = basicSortMap.get(item.toString());
//            if (sortPrio == null) {
//                System.out.println(item.toString());
//                return;
//            }
//            for (int j = 0; j < sortedList.size(); j++) {
//                Integer[] comparePrio = basicSortMap.get(sortedList.get(j).getItem().toString());
//                if (sortPrio[0] < comparePrio[0]) {
//                    sortedList.add(j, container.get(i).getStack());
//                    sorted = true;
//                    break;
//                }
//                else if (sortPrio[0] == comparePrio[0]) {
//                    if (sortPrio[1] <= comparePrio[1]) {
//                        sortedList.add(j, container.get(i).getStack());
//                        sorted = true;
//                        break;
//                    }
//                }
//            }
//
//            if (!sorted) {
//                sortedList.add(container.get(i).getStack());
//            }
//        }
//        for (int i = 0; i < sortedList.size(); i++) {
//            System.out.println(i + ": " + sortedList.get(i).getItem().toString());
//        }
//
//        // Sort the container.
//        for (int i = 0; i < containerSize; i++) {
//            recursiveSortExecute(client, container, sortedList, i);
//        }
//    }

    private void recursiveSortExecute(MinecraftClient client, DefaultedList<Slot> container, List<ItemStack> sortedList, int currentSlot) {
        ItemStack sortedItem;
        if (currentSlot >= sortedList.size()) {
            sortedItem = new ItemStack(Items.AIR);
        }
        else {
            sortedItem = sortedList.get(currentSlot);
        }

        // If we have the correct item or are empty, then we're done
        if (container.get(currentSlot).getStack() == sortedItem || container.get(currentSlot).getStack().isEmpty()) {
            return;
        }

        System.out.println(container.get(currentSlot).getStack().getItem() + " -> " + container.get(sortedList.indexOf(container.get(currentSlot).getStack())).getStack().getItem() + " | " + currentSlot + " -> " + sortedList.indexOf(container.get(currentSlot).getStack()));
        swapContainerSlots(client, currentSlot, sortedList.indexOf(container.get(currentSlot).getStack()));
        recursiveSortExecute(client, container, sortedList, currentSlot);
    }

    private int getContainerSize(ScreenHandler handler) {
        int maxContainerSize = 0;
        try {
            if (handler.getType() == ScreenHandlerType.GENERIC_9X3) {
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
            System.out.println("Invalid container type.");
            return 0;
        }

        return maxContainerSize;
    }

    public void swapContainerSlots(@NotNull MinecraftClient client, int slot1, int slot2) {
        assert client.player != null;
        ScreenHandler handler = client.player.currentScreenHandler;
        int maxContainerSize = getContainerSize(handler);

        if (!((0 <= slot1 && slot1 < maxContainerSize) && (0 <= slot2 && slot2 < maxContainerSize))) {
            System.out.println("Slot is out of range for active container.");
            return;
        }

        assert client.interactionManager != null;
        int[] transactions = {slot1, slot2, slot1};

        for (int slot : transactions) {
            if (!(handler.getSlot(slot).getStack().isEmpty() && client.player.getInventory().getStack(0).isEmpty())) {
//                System.out.println(handler.getSlot(slot).getStack().getItem().toString() + " -> " + client.player.getInventory().getStack(0).getItem().toString());
                client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.SWAP, client.player);
            }
        }
    }

//    private void setUpSortTables() {
//        if (itemList.size() > 0 && itemMap.size() > 0) {
//            System.out.println("Hash tables already created.");
//            System.out.println(itemList);
//            System.out.println(itemMap);
//            System.out.println(categoryMap);
//            return;
//        }
//
//        if (itemList.size() == 0) {
//            for (int i = 0; i < Items.class.getFields().length; i++) {
//                if (Items.class.getFields()[i].getType().equals(Item.class)) {
//                    try {
//                        itemList.add((Item) Items.class.getFields()[i].get(null));
//                    } catch (IllegalAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//
//        List<Item> uncatigorizedItems = new ArrayList<>();
//        int g = 0;
//        for (int i = 0; i < itemList.size(); i++) {
//            Item item = itemList.get(i);
//
//            Integer[] sortCriteria = {0,0};
//            if (item.getGroup() != null) {
//                String group = item.getGroup().getName();
//                System.out.println(item + " - " + group);
//                itemMap.put(item.toString(), i);
//                sortCriteria[1] = i;
//
//                if (!categoryMap.containsKey(group)) {
//                    categoryMap.put(group, g);
//                    g++;
//                }
//
//                sortCriteria[0] = categoryMap.get(group);
//                basicSortMap.put(item.toString(), sortCriteria);
//            }
//            else {
//                System.out.println(item + " has no group.");
//                uncatigorizedItems.add(item);
//            }
//        }
//
//        int i = 0;
//        for (Item uncatigorizedItem : uncatigorizedItems) {
//            basicSortMap.put(uncatigorizedItem.toString(), new Integer[]{g, i});
//            i++;
//        }
//    }
}