# Statut

## Terminé et vérifié

- Squelette ModDevGradle NeoForge 1.21.1 (NeoForge 21.1.267, MDG 2.0.148, Gradle 8.14.3, JDK 21).
- `./gradlew build` : compilation OK, 4 tests JUnit OK (`SeasonLogicTest`).
- `./gradlew runData` : OK, génère `en_us.json` et `fr_fr.json`.
- `./gradlew runGameTestServer` : démarre, exécute le test de fumée (1/1 réussi), s'arrête proprement.
- Architecture modulaire : `TVModule`, hooks datagen, sept modules stub (season, climate, soil, crop, compost, food, tools).

## En cours

- Rien pour l'instant (Phase 0 terminée).

## Problèmes connus

- Parchment non configuré : `maven.parchmentmc.org` redirige vers `ldtteam.jfrog.io`, bloqué par le proxy de l'environnement. Les noms de paramètres restent les noms Mojang.
- Les modules sont des stubs : aucun bloc, item ni recette réel.
- `Enabled Gametest Namespaces: []` au lancement de runGameTestServer : le test de fumée s'exécute quand même.
- Bruit attendu en environnement sans accès Internet : `api.minecraftservices.com` (clés Yggdrasil) et `server.properties` absent au premier lancement.

## Prochaines étapes

1. Implémenter les modules (blocs, items, logique pure) en parallèle, chacun dans son package.
2. Ajouter les textures via scripts Python dans `tools/`.
3. Décider de l'utilisation de Parchment quand le dépôt sera accessible.
4. Remplacer le test de fumée par de vrais tests de jeu.
