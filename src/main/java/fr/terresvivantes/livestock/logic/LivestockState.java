package fr.terresvivantes.livestock.logic;

/**
 * État vital d'un animal d'élevage (Data Attachment d'entité). {@code fed} et {@code health} sont bornés à 0 à 100.
 * Aucun import net.minecraft.
 *
 * @param fed        satiété, 0 à 100
 * @param health     santé, 0 à 100
 * @param ailment    état sanitaire
 * @param lastFedDay jour de la saison (index absolu, {@code SeasonApi.dayIndex}) du dernier apport de nourriture
 */
public record LivestockState(int fed, int health, Ailment ailment, long lastFedDay) {

    public static final int MAX = 100;

    public LivestockState {
        fed = clamp(fed);
        health = clamp(health);
        ailment = ailment == null ? Ailment.NONE : ailment;
    }

    /** État d'un nouvel animal : à demi rassasié, en pleine santé. */
    public static LivestockState initial() {
        return new LivestockState(50, MAX, Ailment.NONE, 0L);
    }

    public LivestockState withFed(int value) {
        return new LivestockState(value, health, ailment, lastFedDay);
    }

    public LivestockState withHealth(int value) {
        return new LivestockState(fed, value, ailment, lastFedDay);
    }

    public LivestockState withAilment(Ailment value) {
        return new LivestockState(fed, health, value, lastFedDay);
    }

    public LivestockState withLastFedDay(long value) {
        return new LivestockState(fed, health, ailment, value);
    }

    /** Borne dans [0, 100]. */
    public static int clamp(int value) {
        return Math.max(0, Math.min(MAX, value));
    }
}
