package fr.terresvivantes.datagen;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie les ressources versionnées par datagen (src/generated) sans lancer Minecraft :
 * clés de langue identiques en en_us et fr_fr, un modèle par item et blockstate par bloc,
 * et textures référencées présentes dans les assets.
 * Lance {@code ./gradlew runData} avant ce test si les ressources ont changé.
 */
class GeneratedResourcesTest {

    private static final Path GENERATED = Paths.get("src/generated/resources/assets/terresvivantes");
    private static final Path TEXTURE_ROOTS = Paths.get("src/main/resources/assets/terresvivantes/textures");
    private static final Path GENERATED_TEXTURES = Paths.get("src/generated/resources/assets/terresvivantes/textures");

    private static final Pattern LANG_ENTRY = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern TEXTURES_BLOCK = Pattern.compile("\"textures\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern MOD_TEXTURE = Pattern.compile("\"terresvivantes:([^\"]+)\"");
    private static final Pattern TRANSPARENT_RENDER_TYPE =
            Pattern.compile("\"render_type\"\s*:\s*\"minecraft:(cutout|cutout_mipped|translucent)\"");

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Set<String> langKeys(Path file) throws IOException {
        Set<String> keys = new TreeSet<>();
        Matcher m = LANG_ENTRY.matcher(read(file));
        while (m.find()) {
            keys.add(m.group(1));
        }
        return keys;
    }

    private static Path lang(String locale) {
        return GENERATED.resolve("lang").resolve(locale + ".json");
    }

    @Test
    void enUsAndFrFrHaveTheSameKeys() throws IOException {
        Set<String> en = langKeys(lang("en_us"));
        Set<String> fr = langKeys(lang("fr_fr"));
        Set<String> onlyEn = new TreeSet<>(en);
        onlyEn.removeAll(fr);
        Set<String> onlyFr = new TreeSet<>(fr);
        onlyFr.removeAll(en);
        assertTrue(onlyEn.isEmpty() && onlyFr.isEmpty(),
                "clés absentes de fr_fr : " + onlyEn + " ; absentes de en_us : " + onlyFr);
        assertTrue(!en.isEmpty(), "aucune clé de langue générée");
    }

    @Test
    void everyRegisteredItemAndBlockHasAModel() throws IOException {
        Set<String> items = new TreeSet<>();
        Set<String> blocks = new TreeSet<>();
        for (String key : langKeys(lang("en_us"))) {
            if (key.startsWith("item.terresvivantes.")) {
                items.add(key.substring("item.terresvivantes.".length()));
            } else if (key.startsWith("block.terresvivantes.")) {
                blocks.add(key.substring("block.terresvivantes.".length()));
            }
        }
        List<String> missing = new ArrayList<>();
        for (String item : items) {
            if (!Files.exists(GENERATED.resolve("models/item/" + item + ".json"))) {
                missing.add("models/item/" + item + ".json");
            }
        }
        for (String block : blocks) {
            if (!Files.exists(GENERATED.resolve("blockstates/" + block + ".json"))) {
                missing.add("blockstates/" + block + ".json");
            }
        }
        assertTrue(missing.isEmpty(), "modèles manquants : " + missing);
        assertTrue(items.size() >= 30, "trop peu d'items enregistrés : " + items.size());
    }

    @Test
    void everyReferencedTextureExists() throws IOException {
        List<String> missing = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> models = Files.walk(GENERATED.resolve("models"))) {
            for (Path model : models.filter(p -> p.toString().endsWith(".json")).toList()) {
                Matcher block = TEXTURES_BLOCK.matcher(read(model));
                while (block.find()) {
                    Matcher tex = MOD_TEXTURE.matcher(block.group(1));
                    while (tex.find()) {
                        String rel = tex.group(1) + ".png";
                        checked++;
                        boolean found = Files.exists(TEXTURE_ROOTS.resolve(rel.replaceFirst("^textures/", "")))
                                || Files.exists(GENERATED_TEXTURES.resolve(rel.replaceFirst("^textures/", "")));
                        if (!found) {
                            missing.add(model.getFileName() + " -> " + tex.group(1));
                        }
                    }
                }
            }
        }
        assertTrue(checked > 0, "aucune texture référencée trouvée dans les modèles générés");
        assertTrue(missing.isEmpty(), "textures manquantes : " + missing);
    }

    /**
     * Une texture de bloc qui contient des pixels transparents (alpha &lt; 255) doit être rendue avec une couche
     * cutout, cutout_mipped ou translucent : sinon le fond transparent apparaît en noir en jeu.
     */
    @Test
    void blockModelsWithTransparentTexturesDeclareACutoutRenderType() throws IOException {
        List<String> offenders = new ArrayList<>();
        int transparentModels = 0;
        try (Stream<Path> models = Files.walk(GENERATED.resolve("models/block"))) {
            for (Path model : models.filter(p -> p.toString().endsWith(".json")).toList()) {
                String json = read(model);
                Matcher block = TEXTURES_BLOCK.matcher(json);
                boolean transparent = false;
                while (block.find()) {
                    Matcher tex = MOD_TEXTURE.matcher(block.group(1));
                    while (tex.find()) {
                        Path png = TEXTURE_ROOTS.resolve(tex.group(1) + ".png");
                        if (Files.exists(png) && hasTransparentPixel(png)) {
                            transparent = true;
                        }
                    }
                }
                if (!transparent) {
                    continue;
                }
                transparentModels++;
                if (!TRANSPARENT_RENDER_TYPE.matcher(json).find()) {
                    offenders.add(GENERATED.relativize(model).toString());
                }
            }
        }
        assertTrue(transparentModels > 0, "aucun modèle de bloc à texture transparente trouvé : test inutile");
        assertTrue(offenders.isEmpty(),
                "modèles à texture transparente sans render_type cutout/cutout_mipped/translucent : " + offenders);
    }

    private static boolean hasTransparentPixel(Path png) throws IOException {
        BufferedImage image = ImageIO.read(new File(png.toString()));
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) < 255) {
                    return true;
                }
            }
        }
        return false;
    }
}
