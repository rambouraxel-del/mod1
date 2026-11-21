package fr.terresvivantes.preserving;

import fr.terresvivantes.processing.ProcessingBlockEntity;

/** Applique un ajustement de vitesse à la progression d'une machine (indice 0 de son ContainerData). */
final class PreservingSpeed {

    private PreservingSpeed() {
    }

    static void adjust(ProcessingBlockEntity machine, int delta) {
        if (delta != 0) {
            machine.dataAccess().set(0, Math.max(0, machine.progress() + delta));
        }
    }
}
