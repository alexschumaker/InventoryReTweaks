package net.cobblers.irt;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class IRTConfig {
    private static final String configPath = "config/irt/";

    public static final List<String> fullItemList = new ArrayList<>();
    public static final Hashtable<String, Integer> categoryMap = new Hashtable<>();
    public static final Hashtable<String, Integer[]> basicSortMap = new Hashtable<>();
    public static final IRTItemDB itemDB = new IRTItemDB();
    public static final IRTItemDB customCategories = new IRTItemDB();
    public static final IRTItemDB playerCategories = new IRTItemDB();
    protected static Hashtable<String, IRTRuleSet> playerInventoryRuleSets = new Hashtable<>();

    public IRTConfig() {
    }

    static {
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
            fullItemList.add(item.toString());
            if (item.getGroup() != null) {
                String group = item.getGroup().getName();

                if (!itemDB.categoryExists(group)) {
                    itemDB.addCategory(group);
                }

                itemDB.addItem(group, item.toString());
            } else {
                // item has no group
                uncategorizedItems.add(item);
            }
        }

        itemDB.addCategory("uncategorized");
        for (Item uncategorizedItem : uncategorizedItems) {
            itemDB.addItem("uncategorized", uncategorizedItem.toString());
        }

        System.out.println(initConfig() ? "Initializing config." : "Config found.");
        initCustomCategories();




        // build hashtable where every item is mapped to its default Mojang category
        uncategorizedItems = new ArrayList<>();
        int categoryIndex = 0;
        int itemIndex = 0;
        for (Item item : itemList) {
            if (item.getGroup() == null) {
                uncategorizedItems.add(item);
                continue;
            }

            String categoryName = item.getGroup().getName();
            if (!categoryMap.containsKey(categoryName)) {
                categoryMap.put(categoryName, categoryIndex);
                categoryIndex++;
            }

            basicSortMap.put(item.toString(), new Integer[]{categoryMap.get(categoryName), itemIndex});
            itemIndex++;
        }




        // put uncategorized items in the "other" category
        categoryMap.put("other", categoryIndex);
        for (Item item : uncategorizedItems) {
            basicSortMap.put(item.toString(), new Integer[]{categoryIndex, itemIndex});
            itemIndex++;
        }
        categoryIndex++;

        // now overwrite items with custom categories
        for (String customCategoryName : customCategories.listCategories()) {
            categoryMap.put(customCategoryName, categoryIndex);

            for (String itemName : customCategories.getCategory(customCategoryName)) {
                basicSortMap.put(itemName, new Integer[]{categoryIndex, basicSortMap.get(itemName)[1]});
            }
            categoryIndex++;
        }

        initPlayerInventoryModel();
        playerInventoryRuleSets.forEach((s, irtRuleSet) -> irtRuleSet.transposeRowItems());
    }

    static boolean initConfig() {
        boolean init = false;

        File config = new File(configPath);
        if (config.mkdirs()) {
            init = true;
        }

        // compare default to existing from file and replace if changed
        if (IRTConfig.getDefaultItemDB() == null || !Objects.equals(IRTConfig.getDefaultItemDB().itemMap, IRTConfig.itemDB.itemMap)) {
            IRTConfig.writeDefaultItemDB(IRTConfig.itemDB);
        }

        // create the custom files if they don't exist
        for (String fileName : new String[]{"IRTCustomCategories.cfg", "IRTCustomInventory.cfg"}) {
            File customConfigFile = new File(configPath + fileName);
            if (!customConfigFile.exists()) {
                try {
                    if (customConfigFile.createNewFile()) {
                        System.out.println("Custom DB file created.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return init;
    }

    private static void initCustomCategories() {
        // get custom IRT categories from IRTCustomCategories.cfg
        List<String> lines;

        try {
            lines = Files.readAllLines(Paths.get(configPath + "IRTCustomCategories.cfg"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to process custom category file.");
            return;
        }

        String processingTag = null;
        for (String s : lines) {
            String line = s.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("@")) {
                processingTag = line.subSequence(1, line.length()).toString();
                continue;
            }

            String[] nameSplit = line.split(":");
            if (nameSplit.length > 1) {
                IRTItemDB currentDB = Objects.equals(processingTag, "Custom") ? customCategories : playerCategories;
                String categoryName = nameSplit[0];
                currentDB.addCategory(categoryName);

                String[] semanticSplit = nameSplit[1].split("\\|");
                String[] literalSplit = nameSplit[1].split(",");

                if (literalSplit.length > 1) {
                    for (String itemName : literalSplit) {
                        itemName = itemName.strip();
                        if (playerCategories.categoryExists(itemName)) {
                            currentDB.addItems(categoryName, playerCategories.getCategory(itemName));
                        }
                        else if (customCategories.categoryExists(itemName)) { // this isn't actually referring to an item, but rather a category that already exists.
                            currentDB.addItems(categoryName, customCategories.getCategory(itemName));
                        }
                        else if (itemDB.categoryExists(itemName)) {
                            currentDB.addItems(categoryName, itemDB.getCategory(itemName));
                        }
                        else {
                            itemName = itemName.strip();
                            currentDB.addItem(categoryName, itemName);
                        }

                    }
                }
                else {
                    for (String itemName : IRTConfig.fullItemList) {
                        if (itemName.contains(semanticSplit[0].strip())) {
                            if (semanticSplit.length > 1) {
                                boolean negated = false;

                                for (int i = 1; i < semanticSplit.length; i++) {
                                    if (negated) break;
                                    negated = itemName.contains(semanticSplit[i].strip());
                                }

                                if (negated) continue;
                            }

                            currentDB.addItem(categoryName, itemName);
                        }
                    }
                }
            }
        }
    }

    private static void initPlayerInventoryModel() {
        // get custom IRT categories from IRTCustomCategories.cfg
        List<String> lines;

        try {
            lines = Files.readAllLines(Paths.get(configPath + "IRTCustomInventory.cfg"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to process custom inventory file.");
            return;
        }

        String processingRule = null;
        for (String s : lines) {
            String line = s.strip();

            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            if (line.endsWith(":")) {
                processingRule = line.substring(0, line.length()-1);
                playerInventoryRuleSets.put(processingRule, new IRTRuleSet(processingRule));
            }
            else {
                playerInventoryRuleSets.get(processingRule).addRule(line);
            }
        }

//        for (var rule : playerInventoryRuleSets.keySet()) {
//            System.out.println(playerInventoryRuleSets.get(rule).ruleItemsMap);
//            System.out.println(playerInventoryRuleSets.get(rule).ruleSlotMap);
//        }
    }

    public static IRTItemDB getDefaultItemDB() {
        File defaultDB = new File(configPath + "IRTDefaultDB.cfg");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        if (!defaultDB.exists()) {
            return null;
        }

        try (FileReader defaultDBReader = new FileReader(configPath + "IRTDefaultDB.cfg"))
        {
            return new IRTItemDB(gson.fromJson(defaultDBReader, HashMap.class));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void writeDefaultItemDB(IRTItemDB itemDB) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter defaultDBWriter = new FileWriter(configPath + "IRTDefaultDB.cfg")) {
            defaultDBWriter.write(gson.toJson(itemDB.itemMap));
            defaultDBWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
