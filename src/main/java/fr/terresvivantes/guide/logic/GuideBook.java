package fr.terresvivantes.guide.logic;

import java.util.List;

/**
 * Le manuel complet, avec sa navigation linéaire.
 * Étape 0 : la couverture (table des matières). Étapes 1 à n : les pages, chapitre après chapitre.
 */
public record GuideBook(List<GuideChapter> chapters) {

    public GuideBook {
        chapters = List.copyOf(chapters);
        if (chapters.isEmpty()) {
            throw new IllegalArgumentException("le manuel doit avoir au moins un chapitre");
        }
    }

    /** Nombre de pages, sans la couverture. */
    public int pageCount() {
        int count = 0;
        for (GuideChapter chapter : chapters) {
            count += chapter.pages().size();
        }
        return count;
    }

    /** Nombre d'étapes, couverture comprise. */
    public int stepCount() {
        return 1 + pageCount();
    }

    /** Étape de la première page d'un chapitre. */
    public int firstStepOf(int chapterIndex) {
        if (chapterIndex < 0 || chapterIndex >= chapters.size()) {
            throw new IndexOutOfBoundsException("chapitre " + chapterIndex);
        }
        int step = 1;
        for (int i = 0; i < chapterIndex; i++) {
            step += chapters.get(i).pages().size();
        }
        return step;
    }

    /** Index du chapitre qui contient l'étape, ou -1 pour la couverture. */
    public int chapterOfStep(int step) {
        checkStep(step);
        if (step == 0) {
            return -1;
        }
        int chapter = 0;
        while (step >= firstStepOf(chapter) + chapters.get(chapter).pages().size()) {
            chapter++;
        }
        return chapter;
    }

    /** Page de l'étape, ou {@code null} pour la couverture. */
    public GuidePage pageAt(int step) {
        int chapter = chapterOfStep(step);
        if (chapter < 0) {
            return null;
        }
        return chapters.get(chapter).pages().get(step - firstStepOf(chapter));
    }

    private void checkStep(int step) {
        if (step < 0 || step >= stepCount()) {
            throw new IndexOutOfBoundsException("étape " + step + " hors de 0.." + (stepCount() - 1));
        }
    }
}
