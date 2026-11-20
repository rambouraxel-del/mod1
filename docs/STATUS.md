# Statut

## v0.2 : contrats et serres (vérifiée le 2026-11-20)

- `./gradlew build` : OK, **77 tests JUnit** (dont combinaison des modifiers, rendement, rotation).
- `./gradlew runData` : OK (lang de la sonde de sol ajoutée).
- `./gradlew runGameTestServer` : OK, **18 GameTests** (5 nouveaux : abri sous verre, blé sous verre en hiver qui pousse, blé sans verre qui ne pousse pas, 3 récoltes → répétition 2, graine → PlantListener TV et vanilla).
- Contrats détaillés : `docs/ARCHITECTURE.md`, section « Contrats v0.2 ».
- Limites connues : la serre n'agit pas sur les cultures du mod (affinité hiver = impossible); la sonde de sol et la lecture d'une sauvegarde v0.1 (`last_crops` absent) n'ont pas été vérifiées en jeu; le rendement vanilla n'est pas appliqué à la casse.

## Terminé et vérifié : v0.1

Vérifié le 2026-11-20 sur la branche `claude/terres-vivantes-minecraft-g4o0hy` :

- `./gradlew build` : OK, **65 tests JUnit** (logique pure des modules + `GeneratedResourcesTest`).
- `./gradlew runData` : OK, régénère `src/generated` (lang en_us/fr_fr, modèles, blockstates, loot, recettes, tags).
- `./gradlew runGameTestServer` : OK, **13 GameTests** réussis, aucune erreur de parsing de recette (1309 recettes chargées).

Fonctionnalités de la v0.1 :

- **Saison** : calendrier (printemps, été, automne, hiver), commandes `/tv season`, synchronisation client, HUD.
- **Climat** : température locale (biome, saison, altitude, nuit) utilisée par les cultures et l'almanach.
- **Sol** : terre labourée avec N/P/K et matière organique (données en Data Attachment de chunk), bac à compost, compost, fumier, cendre de bois, bone meal sur terre labourée.
- **Cultures** : 12 cultures du mod (tomate, maïs, soja, laitue, oignon, ail, poivron, aubergine, fraise, orge, chou, concombre), graines, récolte par clic droit à main vide, intégration des cultures vanilla (option `vanillaIntegration`).
- **Alimentation** : 10 plats (recettes JSON, cuisson, nutrition et effets).
- **Outils** : faucille, arrosoir, sonde de sol, almanach (saison, température, cultures optimales pour la saison et la température locale).
- **Configs COMMON** par module : `terresvivantes-season.toml`, `terresvivantes-soil.toml`, `terresvivantes-compost.toml`, `terresvivantes-crop.toml`.

Tests de jeu (13) : smoke du mod ; intégration saison / culture / sol / engrais (hiver sans croissance, été avec croissance, sol appauvri après croissance, compost qui augmente N/P/K) ; compost ; cultures (récolte à maturité, pas de récolte immature) ; saison (`/tv season set`, hiver plus froid) ; sol (engrais azote) ; outils.

## Problèmes connus

- Parchment non configuré : `maven.parchmentmc.org` redirige vers `ldtteam.jfrog.io`, bloqué par le proxy de l'environnement. Les noms de paramètres restent les noms Mojang.
- Bruit attendu en environnement sans accès Internet : `api.minecraftservices.com` (clés Yggdrasil) et `server.properties` absent au premier lancement.
- Les tests de ressources (`GeneratedResourcesTest`) lisent `src/generated` : relancer `./gradlew runData` après tout changement de datagen.
- Le test d'hiver et le test d'été forcent les randomTicks sur un sol fertile et humide : ils valident la saison, pas le climat réel du biome de test.
- Aucun contrôle visuel en client (textures, modèles, HUD, almanach) n'a été fait : vérification uniquement serveur et datagen.

## En cours

- Rien pour l'instant.

## Prochaines étapes

1. QA client visuelle : `./gradlew runClient`, contrôle des modèles de bloc (bac à compost, niveaux), des stades de culture et du HUD saison.
2. Vérifier les rendus de textures contre la palette de `docs/DESIGN.md`.
3. Décider de l'utilisation de Parchment quand le dépôt sera accessible.
4. Planifier la v0.2 (voir `docs/ROADMAP.md`).
