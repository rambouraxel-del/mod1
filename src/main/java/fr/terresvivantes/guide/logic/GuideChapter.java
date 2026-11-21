package fr.terresvivantes.guide.logic;

import java.util.List;

/** Un chapitre du manuel : identifiant stable, titre et pages dans l'ordre de lecture. */
public record GuideChapter(String id, String title, List<GuidePage> pages) {

    public GuideChapter {
        pages = List.copyOf(pages);
    }
}
