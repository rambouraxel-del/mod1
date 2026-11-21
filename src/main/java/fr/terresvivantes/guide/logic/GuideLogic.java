package fr.terresvivantes.guide.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lecture pure des chapitres du manuel (JSON). Aucune dépendance à Minecraft ni à une bibliothèque : testable en JUnit.
 *
 * <p>Format d'un chapitre ({@code assets/terresvivantes/guide/<lang>/chapters/*.json}) :
 * <pre>
 * { "id": "soil", "title": "Le sol",
 *   "pages": [ { "title": "...", "text": "...", "icon": "terresvivantes:soil_probe" } ] }
 * </pre>
 * {@code icon} est facultatif.
 */
public final class GuideLogic {

    private GuideLogic() {
    }

    /** Lit un chapitre. Lève {@link IllegalArgumentException} si le JSON est invalide ou un champ obligatoire manque. */
    public static GuideChapter parseChapter(String json) {
        if (!(MiniJson.parse(json) instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("le chapitre doit être un objet JSON");
        }
        String id = string(root, "id");
        String title = string(root, "title");
        if (!(root.get("pages") instanceof List<?> array)) {
            throw new IllegalArgumentException("champ tableau manquant : pages (chapitre " + id + ")");
        }
        if (array.isEmpty()) {
            throw new IllegalArgumentException("chapitre " + id + " sans page");
        }
        List<GuidePage> pages = new ArrayList<>();
        for (Object element : array) {
            if (!(element instanceof Map<?, ?> page)) {
                throw new IllegalArgumentException("chapitre " + id + " : page qui n'est pas un objet");
            }
            String icon = page.containsKey("icon") ? string(page, "icon") : null;
            pages.add(new GuidePage(string(page, "title"), string(page, "text"), icon));
        }
        return new GuideChapter(id, title, pages);
    }

    private static String string(Map<?, ?> object, String key) {
        if (!(object.get(key) instanceof String value)) {
            throw new IllegalArgumentException("champ texte manquant : " + key);
        }
        return value;
    }
}
