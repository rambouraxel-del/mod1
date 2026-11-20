# Statut

## v0.2 : terminé et vérifié (2026-11-20, `mod_version=0.2.0`)

- `./gradlew build` : OK, **120 tests JUnit** (dont 3 de `GeneratedResourcesTest` : lang en/fr identiques, modèles et textures présents).
- `./gradlew runData` : OK, `src/generated` régénéré.
- `./gradlew runGameTestServer` : OK, **28 GameTests** réussis.
- Contrats détaillés : `docs/ARCHITECTURE.md`, section « Contrats v0.2 ». Règles de design : `docs/DESIGN.md` §14 (santé), §15 (vergers), §16 (irrigation).

Contenu de la v0.2 :

- **Textures** : 144 sprites 16x16 générés par `tools/textures/generate_all.py` (grilles de `sprites.py` et `sprites_ext.py`, sans doublon de nom). Relancer le script ne modifie aucun PNG.
- **Serres et hiver** : tampon de serre (`ClimateApi.isSheltered`, +8 °C), multiplicateur d'hiver 0.15 par défaut (0.0 avant), 0.6 sous verre. Affinité hiver corrigée : oignon, ail et chou passent de IMPOSSIBLE à UNFAVORABLE (0.4).
- **Rotation** : monoculture ×0.8, précédent légumineux ×1.15 (module soil).
- **Vergers** (`orchard`) : six espèces (pomme, cerise, orange, olive, noix, pêche), saplings, feuillages à quatre stades par randomTick, arbres sauvages par biome, cinq fruits et cinq plats, marchand ambulant. Les oranges poussent en savane et jungle, les oliviers en savane seulement.
- **Eau** (`water`) : arroseurs cuivre (rayon 2) et fer (rayon 3) qui mouillent la terre (humidité 7) et donnent ×1.1 à la croissance; tonneau de pluie (niveau 0 à 8); sécheresse estivale (15 % par tentative, sans pluie ni arroseur). La sécheresse s'applique aussi aux cultures vanilla, qui ne sont notifiées qu'une fois (à Pre).
- **Santé** (`health`) : stress, flétrissement (`withered_crop`), maladies (oïdium, rouille, contagion), fongicide, pulvérisateur, particules.
- **Intégration** : configs COMMON `terresvivantes-water.toml`, `terresvivantes-health.toml`, `terresvivantes-orchard.toml` enregistrées; tag `terresvivantes:compostable` étendu aux fruits du verger (pomme vanille comprise).
- **Commande `/tv showcase [pos]`** (opérateur niveau 2) : plateforme de démonstration à 20 blocs au-dessus du joueur, avec une colonne de 4 terres humides (âges 0, 2, 5, 7) par culture du mod et par culture vanilla, un exemple de chaque bloc du mod, six saplings et quatre stades de feuillage par espèce. Le joueur est téléporté sur la rampe. Depuis la console, la position est obligatoire. Racine `/tv` unique dans `core/command/TVCommands`.

Limites connues :

- Les cultures du mod dont l'hiver est IMPOSSIBLE restent à 0 en hiver, serre comprise.
- Vérifications en jeu non faites : rendu des textures et modèles, HUD, almanach, plateforme `/tv showcase` (seul le GameTest de construction tourne).

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
- Les GameTests `template = ""` exigent un fichier `data/terresvivantes/structure/<nomdelamethode en minuscules>.nbt`.
- `run/config/terresvivantes-*.toml` (gitignoré) garde les valeurs d'une ancienne version : supprimer le fichier pour prendre un nouveau défaut (ex. hiver 0.0 → 0.15).
- Le bâton, drop du flétrissement, n'est pas dans `terresvivantes:compostable` (le design le demande; décision à prendre).
- Le tag compostable ne contient pas `withered_crop` (pas de BlockItem).

## En cours

- v0.3 « Biodiversité » : pollinisation, indice de biodiversité, pucerons (état `APHIDS`) et points d'extension faits (`docs/DESIGN.md` §17, `docs/ARCHITECTURE.md` « Contrats v0.3 »). Reste : modules `flora`, `insects`, `apiculture` (coquilles vides).

## Prochaines étapes

1. QA visuelle en client : `./gradlew runClient`, puis `/tv showcase` (une seule commande) pour contrôler d'un coup d'œil textures, modèles, stades et blocs du mod.
2. Vérifier les rendus contre la palette de `docs/DESIGN.md` §10 à partir de la planche `tools/textures/preview.png`.
3. Vérifier en jeu : sécheresse vanilla, HUD saison, almanach, lecture d'une sauvegarde v0.1 (`last_crops` absent).
4. v0.3 biodiversité : insectes et pollinisation (voir `docs/ROADMAP.md`). `OrchardApi.registerFruitSetModifier` est prêt pour la pollinisation.
5. Décider de l'utilisation de Parchment quand le dépôt sera accessible.
