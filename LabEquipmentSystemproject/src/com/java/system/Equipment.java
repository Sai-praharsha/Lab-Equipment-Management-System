package com.java.system;
import java.util.Locale;


public class Equipment {
   
    private static final String[] CATEGORIES = {"Electronics", "Physics", "Tools", "Other"};

    private final int id;
    private String name;
    private String assetTag;  
    private String category;

    public Equipment(int id, String name, String assetTag, String category) {
        this.id = id;
        this.name = name;
        this.assetTag = assetTag;
        this.category = category;
    }

   
    public static String[] getCategories() {
        return CATEGORIES.clone();
    }

    public static boolean isValidCategory(String category) {
        for (String c : CATEGORIES) {
            if (c.equals(category)) {
                return true;
            }
        }
        return false;
    }

    
    public static String normalizeTag(String tag) {
        return tag.trim().toUpperCase(Locale.ROOT);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAssetTag() { return assetTag; }
    public String getCategory() { return category; }

    public void showDetails() {
        System.out.println("ID " + id + " | " + name + " | Tag: " + assetTag + " | Category: " + category);
    }
}
