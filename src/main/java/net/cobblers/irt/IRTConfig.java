package net.cobblers.irt;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class IRTConfig {
    private static final String configPath = "config/irt/";
    private final IRTItemDB itemDB = IRTSortMap.itemDB;

    public static boolean initConfig() {
        boolean init = false;

        File config = new File(configPath);
        if (config.mkdirs()) {
            init = true;
        }

        // compare default to existing from file and replace if changed
        if (IRTConfig.getDefaultItemDB() == null || !Objects.equals(IRTConfig.getDefaultItemDB().itemMap, IRTSortMap.itemDB.itemMap)) {
            IRTConfig.writeDefaultItemDB(IRTSortMap.itemDB);
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
}
