package fr.terresvivantes.guide.client;

import com.mojang.logging.LogUtils;
import fr.terresvivantes.TerresVivantes;
import fr.terresvivantes.guide.logic.GuideBook;
import fr.terresvivantes.guide.logic.GuideChapter;
import fr.terresvivantes.guide.logic.GuideLogic;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Côté client : charge les chapitres du manuel selon la langue du jeu, avec repli en_us, puis ouvre l'écran.
 * Chaque chapitre est un fichier {@code guide/<lang>/chapters/<nn_nom>.json}; l'ordre est celui des noms de fichiers.
 */
public final class GuideClient {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "guide/";
    private static final String FALLBACK_LANG = "en_us";

    private GuideClient() {
    }

    /** Ouvre le manuel dans l'écran client. Appelé seulement côté client. */
    public static void openManual() {
        Minecraft minecraft = Minecraft.getInstance();
        GuideBook book = load(minecraft.getResourceManager(), minecraft.getLanguageManager().getSelected());
        minecraft.setScreen(new GuideScreen(book));
    }

    /** Charge le manuel pour une langue donnée; un chapitre absent dans cette langue vient de en_us. */
    static GuideBook load(ResourceManager resources, String language) {
        String lang = language == null ? FALLBACK_LANG : language.toLowerCase(Locale.ROOT);
        Map<String, GuideChapter> primary = readLocale(resources, lang);
        Map<String, GuideChapter> fallback = lang.equals(FALLBACK_LANG) ? primary : readLocale(resources, FALLBACK_LANG);

        TreeSet<String> names = new TreeSet<>(primary.keySet());
        names.addAll(fallback.keySet());
        List<GuideChapter> chapters = new ArrayList<>();
        for (String name : names) {
            GuideChapter chapter = primary.containsKey(name) ? primary.get(name) : fallback.get(name);
            chapters.add(chapter);
        }
        return new GuideBook(chapters);
    }

    private static Map<String, GuideChapter> readLocale(ResourceManager resources, String lang) {
        Map<String, GuideChapter> chapters = new TreeMap<>();
        Map<ResourceLocation, Resource> found = resources.listResources(ROOT + lang + "/chapters",
                location -> location.getNamespace().equals(TerresVivantes.MODID) && location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            String path = entry.getKey().getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            try {
                chapters.put(name, GuideLogic.parseChapter(read(entry.getValue())));
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Chapitre du manuel illisible : {} ({})", entry.getKey(), e.getMessage());
            }
        }
        return chapters;
    }

    private static String read(Resource resource) throws IOException {
        try (InputStream in = resource.open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
