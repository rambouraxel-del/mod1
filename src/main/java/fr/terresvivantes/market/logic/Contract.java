package fr.terresvivantes.market.logic;

/**
 * Contrat de livraison (v0.7), logique pure. {@code product} est l'identifiant complet de l'item
 * (ex. {@code terresvivantes:tomato}); {@code acceptor} est l'UUID du joueur qui a accepté (vide sinon).
 * Un contrat est immuable : les changements produisent une nouvelle instance.
 */
public record Contract(long id, String product, int quantity, int minQuality, long deadlineDay,
                       int reward, int reputation, State state, String acceptor, int delivered) {

    /** Cycle de vie : ouvert, accepté, livré, expiré. */
    public enum State { OPEN, ACCEPTED, DONE, EXPIRED }

    public Contract {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantité de contrat non positive : " + quantity);
        }
        if (delivered < 0) {
            throw new IllegalArgumentException("livraisons négatives : " + delivered);
        }
        acceptor = acceptor == null ? "" : acceptor;
    }

    /** Unités encore à livrer. */
    public int remaining() {
        return Math.max(0, quantity - delivered);
    }

    /** Même contrat, accepté par ce joueur. */
    public Contract accepted(String player) {
        return new Contract(id, product, quantity, minQuality, deadlineDay, reward, reputation, State.ACCEPTED,
                player, delivered);
    }

    /** Même contrat, avec {@code units} unités livrées de plus. */
    public Contract withDelivered(int units) {
        return new Contract(id, product, quantity, minQuality, deadlineDay, reward, reputation, state, acceptor,
                delivered + units);
    }

    /** Même contrat, dans un autre état. */
    public Contract withState(State newState) {
        return new Contract(id, product, quantity, minQuality, deadlineDay, reward, reputation, newState, acceptor,
                delivered);
    }
}
