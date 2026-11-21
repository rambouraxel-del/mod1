package fr.terresvivantes.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garde-fou du serveur dédié : aucune classe compilée hors d'un package {@code *.client} ne doit référencer
 * {@code net.minecraft.client}. Sinon le chargement côté serveur échoue (NoClassDefFoundError, RuntimeDistCleaner).
 *
 * <p>Le test lit les octets des fichiers {@code .class} : le pool de constantes y stocke les noms de classes en
 * UTF-8 avec des {@code /}, donc une recherche de sous-chaîne suffit, sans dépendance ASM.
 * La liste blanche est vide : toute exception doit être justifiée ici.
 */
class ServerClassIsolationTest {

    private static final String CLIENT_REF = "net/minecraft/client/";
    private static final Path CLASSES = Path.of("build", "classes", "java", "main");

    /** Chemins relatifs (à {@code build/classes/java/main}) autorisés à référencer le client. Vide. */
    private static final Set<String> WHITELIST = Set.of();

    @Test
    void serverClassesDoNotReferenceMinecraftClient() throws IOException {
        assertTrue(Files.isDirectory(CLASSES), "Classes compilées introuvables : " + CLASSES.toAbsolutePath());
        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(CLASSES)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                String relative = CLASSES.relativize(file).toString().replace('\\', '/');
                if (relative.contains("/client/") || WHITELIST.contains(relative)) {
                    continue;
                }
                scanned++;
                String bytes = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                if (bytes.contains(CLIENT_REF)) {
                    offenders.add(relative);
                }
            }
        }
        assertTrue(scanned > 0, "Aucune classe serveur scannée");
        assertTrue(offenders.isEmpty(), "Classes hors *.client référençant net.minecraft.client : " + offenders);
    }
}
