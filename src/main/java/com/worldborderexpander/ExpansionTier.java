package com.worldborderexpander;

public class ExpansionTier {

    private final String id;
    private final String displayName;
    private final int xpCost;
    private final double expansionBlocks;
    private final String iconMaterial;
    private final String description;

    public ExpansionTier(String id, String displayName, int xpCost, double expansionBlocks,
                         String iconMaterial, String description) {
        this.id = id;
        this.displayName = displayName;
        this.xpCost = xpCost;
        this.expansionBlocks = expansionBlocks;
        this.iconMaterial = iconMaterial;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getXpCost() { return xpCost; }
    public double getExpansionBlocks() { return expansionBlocks; }
    public String getIconMaterial() { return iconMaterial; }
    public String getDescription() { return description; }
}
