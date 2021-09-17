package net.cobblers.irt;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

public class IRTSortMap {
    public static final Hashtable<String, Integer> itemMap = new Hashtable<>();
    public static final Hashtable<String, Integer> categoryMap = new Hashtable<>();
    public static final Hashtable<String, Integer[]> basicSortMap = new Hashtable<>();
    public static final IRTItemDB itemDB = new IRTItemDB();

    public IRTSortMap() {
    }

    static {
        // get full list of items
        List<Item> itemList = new ArrayList<>();
        for (int i = 0; i < Items.class.getFields().length; i++) {
            if (Items.class.getFields()[i].getType().equals(Item.class)) {
                try {
                    itemList.add((Item) Items.class.getFields()[i].get(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        // default itemDB
        List<Item> uncategorizedItems = new ArrayList<>();
        for (Item item : itemList) {
            if (item.getGroup() != null) {
                String group = item.getGroup().getName();
                System.out.println(item + " - " + group);

                if (!itemDB.categoryExists(group)) {
                    itemDB.addCategory(group);
                }

                itemDB.addItem(group, item.toString());
            } else {
                System.out.println(item + " has no group.");
                uncategorizedItems.add(item);
            }
        }

        itemDB.addCategory("uncategorized");
        for (Item uncategorizedItem : uncategorizedItems) {
            itemDB.addItem("uncategorized", uncategorizedItem.toString());
        }

        // old map
        String[] customCategories = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "crimson", "warped"};
        uncategorizedItems = new ArrayList<>();
        int g = 0;
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);

            Integer[] sortCriteria = {0,0};
            boolean isCustom = false;
            String customCategory = "";

            for (String cc : customCategories) {
                if (item.toString().contains(cc.subSequence(0, cc.length()))) {
                    if (cc.equals("oak") && item.toString().contains("dark_oak".subSequence(0, 8))) {
                        continue;
                    }

                    isCustom = true;
                    customCategory = cc;
                    break;
                }
            }

            if (item.getGroup() != null || isCustom) {
                String group = isCustom ? customCategory : item.getGroup().getName();
                System.out.println(item + " - " + group);
                itemMap.put(item.toString(), i);
                sortCriteria[1] = i;

                if (!categoryMap.containsKey(group)) {
                    categoryMap.put(group, g);
                    g++;
                }

                sortCriteria[0] = categoryMap.get(group);
                basicSortMap.put(item.toString(), sortCriteria);
            }
            else {
                System.out.println(item + " has no group.");
                uncategorizedItems.add(item);
            }
        }

        int i = 0;
        for (Item uncategorizedItem : uncategorizedItems) {
            basicSortMap.put(uncategorizedItem.toString(), new Integer[]{g, i});
            i++;
        }
    }
}
