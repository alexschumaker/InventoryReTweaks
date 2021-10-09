package net.cobblers.irt;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IRTConfig {
    private static final String configPath = "config/irt/";
    private final IRTItemDB itemDB = IRTItemData.itemDB;
    public static final List<IRTPlayerInventoryModel> playerInventoryModels = new ArrayList<>();
    public static final IRTItemDB customPlayerInventoryCategories = new IRTItemDB();

    public static boolean initConfig() {
        boolean init = false;

        File config = new File(configPath);
        if (config.mkdirs()) {
            init = true;
        }

        // compare default to existing from file and replace if changed
        if (IRTConfig.getDefaultItemDB() == null || !Objects.equals(IRTConfig.getDefaultItemDB().itemMap, IRTItemData.itemDB.itemMap)) {
            IRTConfig.writeDefaultItemDB(IRTItemData.itemDB);
        }

        // create the custom file if it doesn't exist
        File customConfigFile = new File(configPath + "IRTCustomCategories.cfg");
        if (!customConfigFile.exists()) {
            try {
                if (customConfigFile.createNewFile()) {
                    System.out.println("Custom DB file created.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return init;
    }

    public static boolean initPlayerInventoryModel() {
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(configPath + "IRTPlayerInventory.cfg"));
        } catch (IOException e) {
            e.printStackTrace();
            File customPlayerFile = new File(configPath + "IRTPlayerInventory.cfg");
            if (!customPlayerFile.exists()) {
                try {
                    customPlayerFile.createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return true;
                }
            }
            return true;
        }

        // look through each line of the file to set up the custom player inventory config
        HashMap<String, String> currentRuleSet = null;
        int i = 0;
        for (String s : lines) {
            i++;
            String line = s.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.charAt(line.length() - 1) == ':') {
                if (currentRuleSet != null) {
                    playerInventoryModels.add(new IRTPlayerInventoryModel(currentRuleSet));
                }

                currentRuleSet = new HashMap<>();
            } else if (line.startsWith("###") && line.endsWith("###")) {
                playerInventoryModels.add(new IRTPlayerInventoryModel(currentRuleSet));
                createCustomPlayerCategories(i, lines);
                break;
            } else if (currentRuleSet != null) {
                String[] mapping = line.split(":");
                currentRuleSet.put(mapping[0], mapping[1]);
            }
        }

        return false;
    }

    private static void createCustomPlayerCategories(int startingLine, List<String> lines) {
        String currentCategory = null;
        for (int i = startingLine; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {continue;}

            if (line.charAt(line.length() - 1) == ':') {
                currentCategory = line.split(":")[0];
                customPlayerInventoryCategories.addCategory(currentCategory);
                continue;
            }

            if (currentCategory != null) {
                String item = line.split(" ")[0];
                int prio  = Integer.parseInt(line.split(" ")[1]);

                if (IRTItemData.playerInvDB.categoryExists(item)) {
                    customPlayerInventoryCategories.addItemsPriority(currentCategory, IRTItemData.playerInvDB.getCategory(item), prio);
                }
                else if (IRTItemData.itemDB.categoryExists(item)) {
                    customPlayerInventoryCategories.addItemsPriority(currentCategory, IRTItemData.itemDB.getCategory(item), prio);
                }
                else {
                    customPlayerInventoryCategories.addItemPriority(currentCategory, item, prio);
                }
            }
        }
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

//        try {
//            String defaultDBJson = Files.readString(Path.of(configPath,"IRTDefaultDB.cfg"));
//            return new IRTItemDB(gson.fromJson(defaultDBJson, HashMap.class));
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
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

    public static void initPlayerDB() {

    }
}
