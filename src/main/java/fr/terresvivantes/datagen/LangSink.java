package fr.terresvivantes.datagen;

/** Recoit une traduction avec sa version en_us et fr_fr; le provider garde la bonne langue. */
@FunctionalInterface
public interface LangSink {
    void add(String key, String enUs, String frFr);
}
