package net.cobblers.irt;

import net.minecraft.item.ItemStack;

import java.util.*;

public class IRTRuleSet {
    protected String ruleSetName;

    protected List<Rule> rules = new ArrayList<>();
//    protected HashMap<Integer, List<String>> ruleItemsMap = new HashMap<>();
//    protected HashMap<Integer, List<Integer>> ruleSlotMap = new HashMap<>();
    protected HashMap<String, Rule> itemRuleMap = new HashMap<>();
    protected List<Integer> unRuledSlots = new ArrayList<>();
    protected List<Integer> lockedSlots = new ArrayList<>();
    protected static Hashtable<String, Integer[]> playerInvSortMap = new Hashtable<>();

    private static final HashMap<String, Integer> rowMap = new HashMap<>(){{
        put("A", 0);
        put("B", 1);
        put("C", 2);
        put("D", 3);
    }};

    private static final HashMap<Integer, String> rowMapT = new HashMap<>(){{
        put(0, "A");
        put(1, "B");
        put(2, "C");
        put(3, "D");
    }};

    public IRTRuleSet(String name) {
        ruleSetName = name;

        for (int i = 0; i < 36; i++) {
            unRuledSlots.add(i);
        }
    }

    public void addRule(String rule) {
        Rule newRule = new Rule(rules.size());

        var subRules = rule.split(" ");
        if (subRules.length > 2) {
            System.err.printf("Rule '%s' is formatted incorrectly. There should only be one space in a rule -- the one separating the coordinate definition from the item list.%n", rule);
        }

        // parse param
        char param = subRules[0].charAt(subRules[0].length()-1);
        boolean hasParam = false;
        try {
            int i = Integer.parseInt(String.valueOf(param));
        } catch (Exception e) {
            hasParam = subRules[0].length() > 1;
        }

        if (hasParam) {
            subRules[0] = subRules[0].substring(0, subRules[0].length()-1);
        }

        newRule.slots = parseSlots(subRules[0], param == 'r');
//        ruleSlotMap.put(ruleSlotMap.size(), parseSlots(subRules[0]));
        newRule.items = parseItems(subRules[1]);
//        ruleItemsMap.put(ruleItemsMap.size(), parseItems(subRules[1]));

        if (Objects.equals(subRules[1], "LOCKED")) {
            lockedSlots.addAll(parseSlots(subRules[0], false));
            newRule.locked = true;
        }

        newRule.cycle = param == 'c';

        rules.add(newRule);
    }

    private List<String> parseItems(String itemRule) {
        List<String> items = new ArrayList<>();

        var ruleList = itemRule.split(",");

        for (String rule : ruleList) {
            if (Objects.equals(rule, "LOCKED")) {
                break;
            }
            else if (IRTConfig.playerCategories.categoryExists(rule)) {
                items.addAll(IRTConfig.playerCategories.getCategory(rule));
            }
            else if (IRTConfig.customCategories.categoryExists(rule)) {
                items.addAll(IRTConfig.customCategories.getCategory(rule));
            }
            else if (IRTConfig.itemDB.categoryExists(rule)) {
                items.addAll(IRTConfig.itemDB.getCategory(rule));
            }
            else if (IRTConfig.basicSortMap.containsKey(rule)) {
                items.add(rule);
            }
            else {
                System.err.printf("Rule entry '%s' is neither a known category nor item.", rule);
            }
        }

        return items;
    }

    private List<Integer> parseSlots(String coordinateRule, boolean reversed) {
        List<Integer> slots = new ArrayList<>();

        var rules = coordinateRule.split(",");

        for (String rule : rules) {
            if (rule.contains("-")) { // parse rectangle
                var rectVerts = rule.split("-");
                String rule1 = rectVerts[0];
                String rule2 = rectVerts[1];

                String row1, row2;
                int col1, col2;

                try {
                    row1 = rule1.substring(0,1);
                    col1 = Integer.parseInt(rule1.substring(1,2));
                    row2 = rule2.substring(0,1);
                    col2 = Integer.parseInt(rule2.substring(1,2));

                } catch(NumberFormatException e) {
                    e.printStackTrace();
                    System.err.printf("Unable to parse a column number in %s", rule);
                    break;
                }


                int rowInc = rowMap.get(row1) > rowMap.get(row2) ? -1 : 1;
                int colInc = col1 > col2 ? -1 : 1;

                for (int r = rowMap.get(row1); rowInc > 0 ? r <= rowMap.get(row2) : r >= rowMap.get(row2) ; r = r + rowInc) {
                    for (int c = col1; colInc > 0 ? c <= col2 : c >= col2; c = c + colInc) {
                        int slot = translateCoordinate(rowMapT.get(r) + c);
                        slots.add(slot);
                        unRuledSlots.remove((Integer) slot);
                    }
                }
            }
            else if (rule.length() == 1) { // parse entire row/col
                try {
                    Integer.parseInt(rule);

                    for (int i = 0; i < 4; i++) {
                        int slot = translateCoordinate(rowMapT.get(i) + rule);
                        slots.add(slot);
                        unRuledSlots.remove((Integer) slot);
                    }
                } catch(NumberFormatException e) {
                    for (int i = 0; i < 9; i++) {
                        int slot = translateCoordinate(rule + i);
                        slots.add(slot);
                        unRuledSlots.remove((Integer) slot);
                    }
                }
            }
            else { // single slot
                int slot = translateCoordinate(rule);
                slots.add(slot);
                unRuledSlots.remove((Integer) slot);
            }
        }

        if (reversed) { // reverse
            List<Integer> reversedSlots = new ArrayList<>();
            for (int i = slots.size() - 1; i >= 0; i--) {
                reversedSlots.add(slots.get(i));
            }
            slots = reversedSlots;
        }

        return slots;
    }

    public void transposeRowItems() {
        itemRuleMap = new HashMap<>();

        for (Rule rule : rules) {
            for (String item : rule.items) {
                itemRuleMap.put(item, rule);
            }
        }
    }

//    public void initCycles(List<String> inventory) {
//        for (Rule rule : rules) {
//            if (rule.cycle) {
//                boolean asc = true;
//                if (rule.slots.size() > 1) {
//                    asc = rule.slots.get(0) < rule.slots.get(1);
//                }
//
//                int inc = asc ? 1 : -1;
//                int start = asc ? 0 : inventory.size() - 1;
//
//                for (int i = start; inc > 0 ? i < inventory.size() : i >= 0; i = i + inc) {
//                    String itemName = inventory.get(i);
//                    if (rule.items.contains(itemName)) {
//                        playerInvSortMap.put(itemName, new Integer[]{0, i});
//                    }
//                }
//            }
//        }
//
//        cycleIncrement(inventory);
//    }

//    public void cycleIncrement(List<String> inventory) {
//        for (Rule rule : rules) {
//            if (rule.cycle) {
//                List<String> ruleItems = new ArrayList<>();
//                List<String> newItems = new ArrayList<>();
//
//                for (String itemName : inventory) {
//                    if (rule.items.contains(itemName)) {
//                        if (playerInvSortMap.get(itemName)[1] < 0) {
//                            newItems.add(itemName);
//                        }
//                        else {
//                            ruleItems.add(itemName);
//                        }
//                    }
//                }
//
//                for (String newItem : newItems) {
//                    playerInvSortMap.put(newItem, new Integer[]{0, ruleItems.size()});
//                    ruleItems.add(newItem);
//                }
//
//                for (String ruleItem : ruleItems) {
//                    Integer[] prio = playerInvSortMap.get(ruleItem);
//                    playerInvSortMap.put(ruleItem, new Integer[]{0, (prio[1] + 1) % ruleItems.size()});
//                }
//            }
//        }
//    }

    private int translateCoordinate(String coordinate) {
        if (coordinate.length() != 2) {
            System.err.printf("Invalid coordinate: %s", coordinate);
            return -1;
        }

        String row = coordinate.substring(0,1);
        int col = Integer.parseInt(coordinate.substring(1,2));

        return (rowMap.get(row) * 9) + col - 1;
    }

    public int ruleCount() {
        return rules.size();
    }

    public static class Rule {
        public int index;
        public List<Integer> slots;
        public List<String> items;
        public boolean locked = false;
        public boolean cycle = false;
        public int cycleState = 0;

        public Rule(int index) {
            this.index = index;
        }

        public List<ItemStack> cycleShift(List<ItemStack> sortedItems) {
            int len = sortedItems.size();
            if (len == 0) return sortedItems;
            List<ItemStack> shiftedItems = new ArrayList<>(sortedItems);

            for (int i = 0; i < len; i++) {
                shiftedItems.set((i + this.cycleState) % len, sortedItems.get(i));
            }

            this.cycleState = (this.cycleState + 1) % len;
            return shiftedItems;
        }
    }
}
