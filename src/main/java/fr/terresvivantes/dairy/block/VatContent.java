package fr.terresvivantes.dairy.block;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Contenu visible de la cuve à fromage : vide, lait, ou caillé (état du modèle du dessus). */
public enum VatContent implements StringRepresentable {
    EMPTY,
    MILK,
    CURD;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
