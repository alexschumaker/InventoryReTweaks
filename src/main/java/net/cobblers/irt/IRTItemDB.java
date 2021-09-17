package net.cobblers.irt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IRTItemDB {
    protected Map<String, List<String>> itemMap;

    public IRTItemDB() {
        itemMap = new HashMap<>();
    }

    public IRTItemDB(Map<String, List<String>> existingMap) {
        itemMap = existingMap;
    }

    public void addCategory(String name) {
        itemMap.put(name, new ArrayList<>());
    }

    public void addItem(String category, String item) {
        itemMap.get(category).add(item);
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
}
