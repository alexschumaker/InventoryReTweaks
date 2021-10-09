package net.cobblers.irt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRTItemDB {
    protected Map<String, List<String>> itemMap;
    private Map<String, Integer[]> itemHashMap = null;
    private Map<String, Integer> categoryMap = new HashMap<>();

    public IRTItemDB() {
        itemMap = new HashMap<>();
    }

    public IRTItemDB(Map<String, List<String>> existingMap) {
        itemMap = existingMap;
    }

    public void addCategory(String name) {
        itemMap.put(name, new ArrayList<>());

        if (itemHashMap != null) {
            categoryMap.put(name, getMaxCategory() + 1);
        }
    }

    public void addItem(String category, String item) {
        if (!categoryExists(category)) {
            addCategory(category);
        }

        itemMap.get(category).add(item);

        if (itemHashMap != null) {
            itemHashMap.put(item, new Integer[]{categoryMap.get(category), getMaxItem(category) + 1});
        }
    }

    public void addItemPriority(String category, String item, int prio) {
        if (!categoryExists(category)) {
            addCategory(category);
        }

        itemMap.get(category).add(item);

        if (itemHashMap == null) {
            initHashMap();
        }

        itemHashMap.put(item, new Integer[]{categoryMap.get(category), prio});
    }

    public void addItemsPriority(String category, List<String> items, int prio) {
        if (itemHashMap == null) {
            initHashMap();
        }

        for (String item : items) {
            addItemPriority(category, item, prio);
        }
    }

    public void addItems(String category, List<String> items) {
        itemMap.get(category).addAll(items);

        if (itemHashMap == null) {
            initHashMap();
        }

        int itemIndex = getMaxItem(category) + 1;
        int categoryIndex = categoryMap.get(category);
        for (String item : items) {
            itemHashMap.put(item, new Integer[]{categoryIndex, itemIndex});
            itemIndex++;
        }
    }

    public List<String> getCategory(String name) {
        return itemMap.get(name);
    }

    public int getCategorySize(String name) {
        return itemMap.get(name).size();
    }

    public boolean categoryExists(String name) {
        return itemMap.containsKey(name);
    }

    public String[] listCategories() {
        return itemMap.keySet().toArray(new String[0]);
    }

    public Integer[] getSortData(String item) {
        if (itemHashMap == null) {
            initHashMap();
        }

        return itemHashMap.getOrDefault(item, null);
    }

    private void initHashMap() {
        itemHashMap = new HashMap<>();

        String[] categories = this.listCategories();

        for (int g = 0; g < categories.length; g++) {
            List<String> items = itemMap.get(categories[g]);
            categoryMap.put(categories[g], g);

            for (int i = 0; i < items.size(); i++) {
                itemHashMap.put(items.get(i), new Integer[]{g, i});
            }
        }
    }

    private int getMaxCategory() {
        int output = -1;

        for (String s : categoryMap.keySet()) {
            if (categoryMap.get(s) > output) {
                output = categoryMap.get(s);
            }
        }

        return output;
    }

    private int getMaxItem(String category) {
        int output = -1;
        int categoryIndex = categoryMap.get(category);

        for (String item : itemHashMap.keySet()) {
            Integer[] data = itemHashMap.get(item);
            if (data[0] == categoryIndex && data[1] > output) {
                output = data[1];
            }
        }

        return output;
    }
}
