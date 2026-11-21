package fr.terresvivantes.husbandry;

import fr.terresvivantes.husbandry.logic.FeedType;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Propriété {@code fill} de l'auge : ce qui est visible sur le dessus (texture). MIX s'affiche comme le grain. */
public enum TroughFill implements StringRepresentable {
    EMPTY,
    HAY,
    GRAIN,
    SILAGE;

    /** Propriété affichée pour un stock et un type donnés. */
    public static TroughFill of(FeedType type, int stock) {
        if (type == null || stock <= 0) {
            return EMPTY;
        }
        return switch (type) {
            case HAY -> HAY;
            case GRAIN, MIX -> GRAIN;
            case SILAGE -> SILAGE;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
