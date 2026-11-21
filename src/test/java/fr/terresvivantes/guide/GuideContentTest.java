package fr.terresvivantes.guide;

import fr.terresvivantes.guide.logic.GuideBook;
import fr.terresvivantes.guide.logic.GuideChapter;
import fr.terresvivantes.guide.logic.GuideLogic;
import fr.terresvivantes.guide.logic.GuidePage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contenu du Manuel du fermier : chaque langue se lit, fr_fr et en_us ont la même structure,
 * et chaque icône pointe vers un item du mod. Les items viennent des lang et des modèles générés
 * (lancer {@code ./gradlew runData} avant ce test si le contenu d'items a changé).
 */
class GuideContentTest {

    private static final Path CHAPTERS_ROOT = Paths.get("src/main/resources/assets/terresvivantes/guide");
    private static final Path GENERATED = Paths.get("src/generated/resources/assets/terresvivantes");
    private static final String MOD = "terresvivantes";
    private static final Pattern LANG_ENTRY = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:");

    /** Ordre et identifiants attendus, dans l'ordre des noms de fichiers. */
    private static final List<String> EXPECTED_IDS = List.of("welcome", "soil", "crops", "health", "orchards",
            "biodiversity", "livestock", "processing", "equipment", "economy", "commands", "vines_rice_breeds");

    private static List<Path> chapterFiles(String lang) throws IOException {
        Path dir = CHAPTERS_ROOT.resolve(lang).resolve("chapters");
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
    }

    private static List<GuideChapter> parseAll(String lang) throws IOException {
        List<GuideChapter> chapters = new ArrayList<>();
        for (Path file : chapterFiles(lang)) {
            chapters.add(GuideLogic.parseChapter(Files.readString(file, StandardCharsets.UTF_8)));
        }
        return chapters;
    }

    private static Set<String> modItemIds() throws IOException {
        Set<String> ids = new TreeSet<>();
        Matcher m = LANG_ENTRY.matcher(Files.readString(GENERATED.resolve("lang").resolve("en_us.json"), StandardCharsets.UTF_8));
        while (m.find()) {
            if (m.group(1).startsWith("item." + MOD + ".")) {
                ids.add(m.group(1).substring(("item." + MOD + ".").length()));
            }
        }
        try (Stream<Path> models = Files.list(GENERATED.resolve("models").resolve("item"))) {
            models.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .forEach(n -> ids.add(n.substring(0, n.length() - ".json".length())));
        }
        return ids;
    }

    @Test
    void bothLanguagesHaveTheSameChapterFiles() throws IOException {
        List<String> en = chapterFiles("en_us").stream().map(p -> p.getFileName().toString()).toList();
        List<String> fr = chapterFiles("fr_fr").stream().map(p -> p.getFileName().toString()).toList();
        assertEquals(en, fr, "les fichiers de chapitres doivent être identiques en en_us et fr_fr");
    }

    @Test
    void bothLanguagesHaveTheSameStructure() throws IOException {
        List<GuideChapter> en = parseAll("en_us");
        List<GuideChapter> fr = parseAll("fr_fr");
        assertEquals(EXPECTED_IDS.size(), en.size(), "nombre de chapitres en en_us");
        assertEquals(en.size(), fr.size(), "même nombre de chapitres en fr_fr et en_us");
        for (int i = 0; i < en.size(); i++) {
            assertEquals(EXPECTED_IDS.get(i), en.get(i).id(), "identifiant du chapitre " + i + " en en_us");
            assertEquals(en.get(i).id(), fr.get(i).id(), "identifiant du chapitre " + i);
            assertEquals(en.get(i).pages().size(), fr.get(i).pages().size(),
                    "même nombre de pages pour le chapitre " + en.get(i).id());
            for (int p = 0; p < en.get(i).pages().size(); p++) {
                assertEquals(en.get(i).pages().get(p).icon(), fr.get(i).pages().get(p).icon(),
                        "icône identique en fr et en pour " + en.get(i).id() + " page " + p);
            }
        }
    }

    @Test
    void chaptersHaveThreeToSixPages() throws IOException {
        for (String lang : List.of("en_us", "fr_fr")) {
            for (GuideChapter chapter : parseAll(lang)) {
                int pages = chapter.pages().size();
                assertTrue(pages >= 3 && pages <= 6, lang + " " + chapter.id() + " : " + pages + " pages (attendu 3 à 6)");
            }
        }
    }

    @Test
    void everyIconReferencesAnExistingModItem() throws IOException {
        Set<String> items = modItemIds();
        assertTrue(!items.isEmpty(), "aucun item du mod trouvé dans src/generated");
        List<String> missing = new ArrayList<>();
        for (String lang : List.of("en_us", "fr_fr")) {
            for (GuideChapter chapter : parseAll(lang)) {
                for (GuidePage page : chapter.pages()) {
                    String icon = page.icon();
                    if (icon == null) {
                        continue;
                    }
                    if (!icon.startsWith(MOD + ":")) {
                        missing.add(lang + " " + chapter.id() + " : icône hors du mod " + icon);
                    } else if (!items.contains(icon.substring(MOD.length() + 1))) {
                        missing.add(lang + " " + chapter.id() + " : item inconnu " + icon);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "icônes invalides : " + missing);
    }

    @Test
    void textsFitTheBookPages() throws IOException {
        List<String> problems = new ArrayList<>();
        for (String lang : List.of("en_us", "fr_fr")) {
            for (GuideChapter chapter : parseAll(lang)) {
                if (chapter.title().length() > 28) {
                    problems.add(lang + " titre de chapitre trop long : " + chapter.title());
                }
                for (GuidePage page : chapter.pages()) {
                    if (page.title().isBlank() || page.title().length() > 40) {
                        problems.add(lang + " " + chapter.id() + " titre de page : " + page.title());
                    }
                    if (page.text().isBlank() || page.text().length() > 300) {
                        problems.add(lang + " " + chapter.id() + " texte de " + page.title()
                                + " : " + page.text().length() + " caractères (max 300)");
                    }
                    assertFalse(page.text().contains("TODO"), "texte provisoire dans " + chapter.id());
                }
            }
        }
        assertTrue(problems.isEmpty(), "pages trop longues : " + problems);
    }

    @Test
    void bookNavigationCoversCoverThenEveryPage() {
        GuideBook book = new GuideBook(List.of(
                new GuideChapter("a", "A", List.of(new GuidePage("a1", "t", null), new GuidePage("a2", "t", null))),
                new GuideChapter("b", "B", List.of(new GuidePage("b1", "t", "terresvivantes:almanac")))));
        assertEquals(4, book.stepCount(), "couverture + 3 pages");
        assertEquals(-1, book.chapterOfStep(0));
        assertEquals(0, book.chapterOfStep(1));
        assertEquals(0, book.chapterOfStep(2));
        assertEquals(1, book.chapterOfStep(3));
        assertEquals(3, book.firstStepOf(1), "le chapitre B commence après les deux pages de A");
        assertEquals("b1", book.pageAt(3).title());
        assertEquals(null, book.pageAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> book.pageAt(4));
    }

    @Test
    void malformedChapterIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GuideLogic.parseChapter("{\"id\":\"x\",\"title\":\"X\",\"pages\":[]}"));
        assertThrows(IllegalArgumentException.class,
                () -> GuideLogic.parseChapter("{\"id\":\"x\",\"title\":\"X\",\"pages\":[{\"title\":\"t\"}]}"));
    }
}
