package net.cobblers.irt;

import java.util.ArrayList;
import java.util.List;

public class IRTCustomCategoryList {
    private List<customCategory> categoryList;

    public IRTCustomCategoryList() {
        categoryList = new ArrayList<>();
    }

    public void addCategory(String categoryName, List<String> literalItemNames) {
        categoryList.add(new customCategory(categoryName, literalItemNames));
    }

    public void addCategory(String categoryName, String semanticKeyword) {
        categoryList.add(new customCategory(categoryName, semanticKeyword));
    }

    public void addCategory(String categoryName, String semanticKeyword, String... semanticRejections) {
        categoryList.add(new customCategory(categoryName, semanticKeyword, semanticRejections));
    }

    public String match(String itemName) {
        for (customCategory customCategory : categoryList) {
            if (customCategory.contains(itemName)) {
                return customCategory.name;
            }
        }

        return null;
    }

    private class customCategory {
        public final String name;
        private final String type;
        private List<String> literalMembers;
        private String semanticKeyword;
        private List<String> semanticRejections;

        public customCategory(String categoryName, List<String> members) {
            name = categoryName;
            literalMembers = members;
            type = "literal";
        }

        public customCategory(String categoryName, String keyword) {
            name = categoryName;
            semanticKeyword = keyword;
            type = "semantic";
            semanticRejections = new ArrayList<>();
        }

        public customCategory(String categoryName, String keyword, String... rejections) {
            name = categoryName;
            semanticKeyword = keyword;
            type = "semantic";
            semanticRejections = new ArrayList<>();

            for (String rejection : rejections) {
                if (rejection != null) {
                    semanticRejections.add(rejection);
                }
            }
        }

        public boolean contains(String item) {
            if (type.equals("literal")) {
                return literalMembers.contains(item);
            }

            if (item.contains(semanticKeyword)) {
                for (String semanticRejection : semanticRejections) {
                    if (item.contains(semanticRejection)) {
                        return false;
                    }
                }

                return true;
            }
            else {
                return false;
            }
        }
    }
}
