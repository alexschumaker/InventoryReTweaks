package net.cobblers.irt;

import java.util.HashMap;
import java.util.Map;

public class IRTPlayerInventoryModel {
    private static final char[] validRowCoords = {'A', 'B', 'C', 'D'};
    private final HashMap<String, String> ruleSet;

    public IRTPlayerInventoryModel(Map<String, String> rules) {
        ruleSet = new HashMap<>(rules);

        for (String rule : ruleSet.keySet()) {
            String[] rectCorners = rule.split("-");
            if (rectCorners.length > 1) {
                getIndexArrayFromRect(rule);
            }
        }
    }

    // TODO: support for non-left-to-right and non-top-to-bottom orientations
    public int[] getIndexArrayFromRect(String rect) {
        try {
            String[] corners = rect.split("-");
            int c1 = getIndexFromCoordinate(corners[0]);
            int c2 = getIndexFromCoordinate(corners[1]);

            int width = (c2 % 9 - c1 % 9);
            int height = (Math.floorDiv(c2, 9) - Math.floorDiv(c1, 9));

            int[] indeces = new int[width * height];

            int i = 0;
            for (int h = 0; h < height; h++) {
                for (int w = 0; w < width; w++) {
                    indeces[i] = c1 + (h*9) + w;
                    i++;
                }
            }

            return i == indeces.length ? indeces : null;

        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public int getIndexFromCoordinate(String coordinate) {
        char[] coordArray = coordinate.toCharArray();

        try {
            for (int i = 0; i < validRowCoords.length; i++) {
                if (validRowCoords[i] == coordArray[0]) {
                    return (i * 9) + (int) coordArray[1];
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            return -1;
        }

        return -1;
    }
}
