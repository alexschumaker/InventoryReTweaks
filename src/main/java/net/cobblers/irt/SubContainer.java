package net.cobblers.irt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubContainer {
    public static List<Integer> indices = new ArrayList<>();
    public static int width;
    public static int height;
    public static Map<Integer, int[]> rows = new HashMap<>();

    public SubContainer(List<Integer> subIndices) {
        indices = subIndices;

        // confirm rectangular
        int rownum = 0;
        for (int i = 0; i < indices.size(); i++) {
            if (i == 0) {
                rows.put(0, new int[]{indices.get(i)});
                continue;
            }

            if (indices.get(i) > 1 + indices.get(i-1) || indices.get(i) % 9 == 0) {
                rownum++;
                rows.put(rownum, new int[]{indices.get(i)});
            }
            else {
                int[] row = rows.get(rownum);
                int[] newrow = new int[row.length + 1];
                System.arraycopy(row, 0, newrow, 0, row.length + 1);
                newrow[row.length] = indices.get(i);

                rows.put(rownum, newrow);
            }
        }

        width = rows.get(0).length;
        height = rows.size();
        for (int i = 0; i < rows.keySet().size(); i++) {
            if (width != rows.get(i).length) {
                System.out.println("Inconsistent row lengths. Sub container must be rectangular.");
            }
        }
    }

    public int getIndexByCoordinates(int row, int col) {
        return rows.get(row)[col];
    }
}
