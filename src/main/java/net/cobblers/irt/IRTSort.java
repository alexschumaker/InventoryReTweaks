package net.cobblers.irt;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import java.util.*;

public class IRTSort {
    public static List<ItemStack> basicSort(List<ItemStack> container) {
        List<ItemStack> sortedList = new ArrayList<>();

        int containerSize = container.size();
        if (containerSize == 0) {
            return sortedList;
        }

        // Prepare the sorted list.
        for (int i = 0; i < containerSize; i++) {
            Item item = container.get(i).getItem();
            boolean sorted = false;
            if (container.get(i).isEmpty()) {
                continue;
            }

            Integer[] sortPrio = IRTConfig.basicSortMap.get(item.toString());

            if (sortPrio == null) {
                System.err.println("Invalid Item: " + item + ". Aborting sort.");
                return sortedList;
            }

            for (int j = 0; j < sortedList.size(); j++) {
                Integer[] comparePrio = IRTConfig.basicSortMap.get(sortedList.get(j).getItem().toString());

                if (sortPrio[0] < comparePrio[0]) {
                    sortedList.add(j, container.get(i));
                    sorted = true;
                    break;
                }
                else if (sortPrio[0].equals(comparePrio[0])) {
                    if (sortPrio[1] <= comparePrio[1]) {
                        sortedList.add(j, container.get(i));
                        sorted = true;
                        break;
                    }
                }
            }

            if (!sorted) {
                sortedList.add(container.get(i));
            }
        }

        return sortedList;
    }

    public static List<ItemStack> groupedSort(List<ItemStack> container, int containerSize) {
        List<ItemStack> sortedContainer = new ArrayList<>();

        for (int i = 0; i < containerSize; i++) {
            sortedContainer.add(new ItemStack(Items.AIR));
        }

        // get counts
        int total = 0;
        int emptySlots = 0;
        HashMap<Integer, List<ItemStack>> categories = new HashMap<>();
        List<Integer> categoryOrder = new ArrayList<>();

        for (ItemStack itemStack : container) {
            if (itemStack.isEmpty()) {
                emptySlots++;
            }
            else {
                total++;
                int category = IRTConfig.basicSortMap.get(itemStack.getItem().toString())[0];

                if (categories.containsKey(category)) {
                    categories.get(category).add(itemStack);
                }
                else {
                    categories.put(category, new ArrayList<>(List.of(itemStack)));
                }
            }
        }

        categories.forEach((key, stacks) -> {
            categories.put(key, basicSort(stacks));
        });

        // sort categories by size
        for (int category : categories.keySet()) {
            for (Integer topCategory : categoryOrder) {
                if (categories.get(category).size() > categories.get(topCategory).size()) {
                    categoryOrder.add(categoryOrder.indexOf(topCategory), category);
                    break;
                }
            }
            if (!categoryOrder.contains(category)) {
                categoryOrder.add(category);
            }
        }


        ////////////////////////////////////////////////////////////////////////
        //////////////////// row by row
        ////////////////////////////////////////////////////////////////////////
        int[] colWidths = containerSize == 27 ? new int[]{3, 3, 3} : new int[]{5, 4};
        int colHeight = containerSize == 54 ? 6 : 3;

        int remainingFreeSlots = containerSize - total;

        Iterator<Integer> categoryOrders = categoryOrder.iterator();
        Iterator<ItemStack> categoryItems;
        if (categoryOrders.hasNext()) {
            categoryItems = categories.get(categoryOrders.next()).iterator();
        }
        else {
            return sortedContainer;
        }

        for (int col = 0; col < colWidths.length; col++) {
            for (int row = 0; row < colHeight; row++) {
                for (int sCol = 0; sCol < colWidths[col]; sCol++) {
                    int currentSlot = getContainerSlotFromColCoordinates(row, sCol, col, colWidths);

                    if (categoryItems.hasNext()) {
                        sortedContainer.set(currentSlot, categoryItems.next());
                    }
                    else if (remainingFreeSlots >= colWidths[col] - sCol) {
                        remainingFreeSlots -= colWidths[col] - sCol;
                        categoryItems = categoryOrders.hasNext() ? categories.get(categoryOrders.next()).iterator() : Collections.emptyIterator();
                        break;
                    }
                    else {
                        categoryItems = categoryOrders.hasNext() ? categories.get(categoryOrders.next()).iterator() : Collections.emptyIterator();
                        if (categoryItems.hasNext()) {
                            sortedContainer.set(currentSlot, categoryItems.next());
                        }
                        else {
                            break;
                        }
                    }
                }
            }
        }

        return sortedContainer;
    }

    public static List<ItemStack> inventorySort(List<ItemStack> container, String ruleSetName) {
        IRTRuleSet ruleSet = IRTConfig.playerInventoryRuleSets.get(ruleSetName);
        List<ItemStack> sortedContainer = new ArrayList<>();
            for (int i = 0; i < 36; i++) {
                sortedContainer.add(new ItemStack(Items.AIR));
            }

        for (int slot : ruleSet.lockedSlots) {
            sortedContainer.set(slot, container.get(slot));
        }

        // separate items that have rules in playerInventoryRuleSets (defined by IRTCustomInventory.cfg)
        List<List<ItemStack>> ruledItems = new ArrayList<>();
        for (int i = 0; i < ruleSet.ruleCount(); i++) {
            ruledItems.add(new ArrayList<>());
        }

        List<ItemStack> unruledItems = new ArrayList<>();

        for (int i = 0; i < container.size(); i++) {
            if (ruleSet.lockedSlots.contains(i)) continue;

            ItemStack stack = container.get(i);
            if (stack.isEmpty()) continue;

            String itemName = stack.getItem().toString();
            if (ruleSet.itemRuleMap.containsKey(stack.getItem().toString())) {
                ruledItems.get(ruleSet.itemRuleMap.get(itemName).index).add(stack);
            }
            else {
                unruledItems.add(stack);
            }
        }

        // sort each rule's items
        for (int i = 0; i < ruledItems.size(); i++) {
            List<ItemStack> sortedRuleItems = basicSort(ruledItems.get(i));

            if (ruleSet.rules.get(i).cycle) {
                ruledItems.set(i, ruleSet.rules.get(i).cycleShift(basicSort(ruledItems.get(i))));
            }
            else {
                ruledItems.set(i, basicSort(ruledItems.get(i)));
            }
        }

        // insert them into the appropriate slots
        for (int i = 0; i < ruleSet.ruleCount(); i++) {
            List<Integer> ruleSlots = ruleSet.rules.get(i).slots;
            List<ItemStack> ruleItems = ruledItems.get(i);

            int numItemsToSort = ruleItems.size();
            for (int j = 0; j < Math.min(ruleSlots.size(), numItemsToSort); j++) {
                sortedContainer.set(ruleSlots.get(j), ruleItems.get(0));
                ruleItems.remove(0);
            }

            // overflow counts as unruled
            unruledItems.addAll(ruleItems);
        }

        // sort the remaining, unruled items and put them in whatever empty, unlocked slots are left
        unruledItems = basicSort(unruledItems);
        List<Integer> unRuledSlots = ruleSet.unRuledSlots;

        for (int i = 0; i < Math.min(unRuledSlots.size(), unruledItems.size()); i++) {
            sortedContainer.set(unRuledSlots.get(i), unruledItems.get(0));
            unruledItems.remove(0);
        }

        while (unruledItems.size() > 0) {
            int remainingItemCount = unruledItems.size();
            for (int i = 0; i < sortedContainer.size(); i++) {
                if (sortedContainer.get(i).isEmpty() && !ruleSet.lockedSlots.contains(i)) {
                    sortedContainer.set(i, unruledItems.get(0));
                    unruledItems.remove(0);
                    break;
                }
            }

            if (unruledItems.size() == remainingItemCount) {
                for (int i = 0; i < remainingItemCount; i++) {
                    sortedContainer.set(ruleSet.lockedSlots.get(i), unruledItems.get(0));
                    unruledItems.remove(0);
                }
            }
        }


//        IRTConfig.playerInventoryRuleSets.get(ruleSetName).cycleIncrement(getNameList(sortedContainer));
        return sortedContainer;
    }

    private static int getContainerSlotFromColCoordinates(int row, int sCol, int col, int[] colWidths) {
        int containerWidth = 0;
        for (int colWidth : colWidths) {
            containerWidth += colWidth;
        }

        int currentIndex;
        currentIndex = row * containerWidth;

        for (int i = 0; i < col; i++) {
            currentIndex += colWidths[i];
        }

        currentIndex += sCol;

        return currentIndex;
    }

    private static List<String> getNameList(List<ItemStack> stackList) {
        List<String> nameList = new ArrayList<>();

        for (ItemStack stack : stackList) {
            nameList.add(stack.getItem().toString());
        }

        return nameList;
    }
}