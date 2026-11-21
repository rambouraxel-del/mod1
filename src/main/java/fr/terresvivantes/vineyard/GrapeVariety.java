package fr.terresvivantes.vineyard;

import net.minecraft.util.StringRepresentable;

/** Variété de vigne : cépage rouge ou blanc. Porte la propriété de bloc {@code variety}. */
public enum GrapeVariety implements StringRepresentable {
    RED("red"),
    WHITE("white");

    private final String serialized;

    GrapeVariety(String serialized) {
        this.serialized = serialized;
    }

    @Override
    public String getSerializedName() {
        return serialized;
    }
}
