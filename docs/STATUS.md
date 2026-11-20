# Statut

## v0.3 « Biodiversité » : terminé et vérifié (`mod_version=0.3.0`)

- `./gradlew build` : OK, **180 tests JUnit** (dont `GeneratedResourcesTest`, avec le contrôle render_type des blocs à texture transparente).
- `./gradlew runData` : OK, `src/generated` régénéré.
- `./gradlew runGameTestServer` : **40 GameTests** réussis, trois passages de suite.
- Contrats : `docs/ARCHITECTURE.md` « Contrats v0.3 » et « Contrats apiculture ». Règles : `docs/DESIGN.md` §17.

Contenu de la v0.3 :

- **Pollinisation et biodiversité** (`pollination`) : score et indice avec cache, effets sur cultures, vergers et santé.
- **Pucerons** (état `APHIDS`) et coccinelles qui les nettoient.
- **Flore** (`flora`) : clover, lavender, chamomile, borage (pots, teintures, propagation); haie d'aubépine (habitat).
- **Insectes** (`insects`) : papillons (4 variantes), coccinelle, pucerons, lucioles, filet, bocal à papillon.
- **Apiculture** (`apiculture`) : apiaire peuplé (0 à 100), extracteur, enfumoir, cadres, cire; miel typé par la fleur
  dominante (`ApicultureApi.registerNectarSource`, trèfle et lavande déclarés par l'intégrateur).
- **Intégration** : configs COMMON `terresvivantes-flora.toml`, `terresvivantes-insects.toml`,
  `terresvivantes-apiculture.toml` enregistrées dans `TerresVivantes`.
- **Rendu** : feuillages, saplings, cultures, fleurs, haie, flétrissement, apiaire et extracteur déclarent une couche
  de rendu (`render_type` cutout ou cutout_mipped). Causait un fond noir sur les textures transparentes.
- **Textures** : `tools/textures/generate_all.py` produit les 171 sprites 16x16 et les 5 textures d'entités 32x32
  (`insects_textures.py`). Sortie déterministe.
- **Commande `/tv showcase`** : ajoute les 4 fleurs, une haie, un apiaire peuplé avec son extracteur, deux papillons et
  une coccinelle (persistants).

Problèmes connus :

- QA visuelle en client non terminée : le rendu logiciel (Xvfb) est lent et laisse des chunks noirs. Procédure qui
  fonctionne : `xvfb-run` avec `LIBGL_ALWAYS_SOFTWARE=1`, monde superplat créatif, `allowCommands` via nbtlib,
  `--quickPlaySingleplayer`, puis `/tv showcase`.
- Sprites encore faibles : `bee_smoker`, `aphid` et `pollen` (particules), sprinklers.
- Citrouille et pastèque vanilla hors pollinisation (tige, hors contrat `CropGrowthHooks`).
- Entités sans loot (papillons, coccinelles).
- Pas d'interface graphique (GUI) pour l'apiaire.
- Pas de relâcher des insectes depuis les bocaux.
- Parchment non utilisé : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy.
- Les tests de jeu de la graine de blé et des coccinelles sont rendus déterministes par une bascule (`HealthConfig.disableRandomEventsForTest`) et un délai long (coccinelles : 1600 ticks, probabilité d'échec ~1e-11).
- Le bâton, drop du flétrissement, n'est pas dans `terresvivantes:compostable`; décision à prendre.

Prochaines étapes :

1. **v0.4 Élevage** : module `livestock` (génome par Data Attachment d'entité : productivité, taille, résistance,
   fertilité 0 à 10, héritage moyen des parents avec mutation; besoins : nourriture quotidienne, pâturage hors hiver;
   reproduction au printemps et en été; `LivestockApi`; `livestock_ledger` qui affiche gènes et état).
   En parallèle : bâtiments et alimentation (`feeding_trough`, `nesting_box`, ensilage et `feed_mix` à partir de
   orge, maïs et soja); santé animale (`veterinary_kit`); canard et productions (lait, laine, œufs selon gènes
   et alimentation); textures v0.4.
2. QA visuelle en client (procédure ci-dessus) et vérification des rendus contre `docs/DESIGN.md` §10.
3. Ensuite : v0.5 transformation, v0.6 équipements, v0.7 économie (`docs/ROADMAP.md`).

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
