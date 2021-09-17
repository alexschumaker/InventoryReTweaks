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
            System.out.println("No container to sort.");
            return sortedList;
        }

        // Prepare the sorted list.
        for (int i = 0; i < containerSize; i++) {
            Item item = container.get(i).getItem();
            boolean sorted = false;
            if (container.get(i).isEmpty()) {
                continue;
            }

            Integer[] sortPrio = IRTSortMap.basicSortMap.get(item.toString());
            System.out.println(Arrays.toString(sortPrio));
            if (sortPrio == null) {
                System.out.println("Invalid Item: " + item + ". Aborting sort.");
                return sortedList;
            }
            for (int j = 0; j < sortedList.size(); j++) {
                Integer[] comparePrio = IRTSortMap.basicSortMap.get(sortedList.get(j).getItem().toString());
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
                int category = IRTSortMap.basicSortMap.get(itemStack.getItem().toString())[0];

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
}