# Statut

## v0.6 « Équipements & automatisation » : livrée et vérifiée (`mod_version=0.6.0`)

- `./gradlew build` : OK, **309 tests JUnit** (dont `GeneratedResourcesTest`).
- `./gradlew runData` : OK, `src/generated` régénéré.
- `./gradlew runGameTestServer` : **93 GameTests** réussis, trois passages de suite (93/93 à chaque fois), aucune erreur
  « Parsing error » / « Couldn't parse » dans le log, aucun avertissement de tag.
- Contrats : `docs/ARCHITECTURE.md` « Contrats v0.6 »; règles : `docs/DESIGN.md` §20.

Contenu de la v0.6 :

- **Implement** (`implement`) : grelinette (labour 3x3; aération limitée à une fois par bloc et par jour MC, mémorisée
  dans les données de sol, champ optionnel `aerated_day`), semoir (3x3 ou ligne de 5 accroupi), faucille en diamant (5x5).
- **Automatisation** (`automation`) : récolteuse (volume 5x5x3; cultures mûres et feuilles fruitières mûres via
  `OrchardApi.harvestRipeLeaves`), planteuse (zone 5x5). Cadence et rayon dans `terresvivantes-automation.toml`.
- **Stockage** (`storage`) : caisse (8 stacks, recette 8 planches autour d'un lingot de fer, vérifiée dans le JSON généré),
  silo (colonne de 1 à 8 blocs, 16 stacks par bloc).
- **Ravageurs** (`pests`) : corbeau (picore les cultures mûres hors hiver), épouvantail (rayon 12, use après plusieurs jours,
  réduit les ravageurs). Config `terresvivantes-pests.toml`.
- **Vitrine** `/tv showcase` : section équipements (z 23 à 31; récolteuse face à une rangée de blé mûr avec le coffre
  derrière, planteuse, caisse de tomates, silo de 3 blocs, épouvantail, 2 corbeaux persistants). Profondeur de la
  plateforme portée à 32. Vérifiée par le GameTest `showcaseBuildsExpectedBlocks`.
- **Corrections d'intégration** : tag `terresvivantes:crow_habitat` corrigé (`#c:is_plains` au lieu de l'inexistant
  `#minecraft:is_plains`, qui produisait un avertissement au chargement).

Problèmes connus :

- **Machines invisibles en jeu (QA client)** : signalé en QA. Hypothèse « BaseEntityBlock.getRenderShape = INVISIBLE » vérifiée :
  aucun bloc du mod n'hérite de `BaseEntityBlock`, et `RenderShapeGameTests` confirme que tous les blocs `terresvivantes:*`
  renvoient `RenderShape.MODEL`. Les blockstates et modèles des machines existent et sont corrects. **Cause non trouvée.**
  Pistes à vérifier en client : rendu des `BlockEntity` (sans BlockEntityRenderer, la BE ne dessine rien), textures
  absentes à l'exécution, couche de rendu. Le garde-fou `RenderShapeGameTests` reste en place.
- Rendu de la caisse et du silo non vérifié visuellement (client).
- Bonus de lapins près de l'épouvantail : non fait (prévu au design).
- Silo : pas de rendu du contenu, pas de GUI.
- Procédure de QA client inchangée (`xvfb-run` avec `LIBGL_ALWAYS_SOFTWARE=1`, `/tv showcase`).

Prochaines étapes :

1. QA visuelle en client (v0.5 et v0.6) : machines (rendu invisible à diagnostiquer), caisse, silo, champs de la vitrine.
2. **v0.7 Économie** : marché de village et étal, contrats de livraison, réputation, monnaie, spécialisation.

## v0.5 « Transformation » : livrée et vérifiée (`mod_version=0.5.0`)

- `./gradlew build` : OK, **263 tests JUnit** (dont `GeneratedResourcesTest`).
- `./gradlew runData` : OK, `src/generated` régénéré.
- `./gradlew runGameTestServer` : **75 GameTests** réussis, trois passages de suite (75/75 à chaque fois), aucune erreur
  « Parsing error » / « Couldn't parse » dans le log.
- Contrats : `docs/ARCHITECTURE.md` « Contrats v0.5 »; règles : `docs/DESIGN.md` §19.

Contenu de la v0.5 :

- **Qualité générique** (`core.quality`) : composant `terresvivantes:quality` sur tout item, infobulle « Qualité: ★★☆ ».
  `milk_quality` du canard migré vers ce composant et retiré.
- **Infrastructure** (`processing`) : recette `terresvivantes:processing` (JSON). Le conteneur est un `Ingredient`
  (item ou tag; l'ancien format chaîne reste lisible). Durée bornée à 2 400 000 ticks. Les restes des ingrédients
  consommés (seau vide) sont placés dans le slot conteneur, sinon dans la sortie, sinon éjectés.
- **Meule et pressoir** (`milling`) : blé → farine de blé, orge → farine d'orge, maïs → farine de maïs; pommes, pêches,
  oranges → jus; olives (4) + flacon → huile d'olive.
- **Laiterie** (`dairy`) : baratte (lait → crème, crème → beurre), cuve à fromage (lait → caillé, lait de chèvre →
  fromage de chèvre), étagère d'affinage (caillé → fromage jeune → fromage affiné). Un seau sans composant
  `milk_source` est du lait de vache (ingrédient `neoforge:difference`). Traite d'une chèvre vanilla : composant `goat`.
- **Conservation** (`preserving`) : tonneau de fermentation (jus → cidre, concombres + vinaigre → cornichons),
  séchoir, fumoir (une recette par produit, combustible = tag `terresvivantes:smoker_fuel`: bûches, charbon, charbon de bois).
- **Cuisine** (`kitchen`) : marmite sur source de chaleur (feu de camp), 12 plats. Test de référence : omelette au fromage.
- **Vitrine** `/tv showcase` : rangée de transformation (une machine de chaque sorte, marmite sur feu de camp allumé).
- Textures v0.5 : `tools/textures/sprites_v05.py`; les jus de pomme et de pêche sont désormais générés (plus de recoloriage
  à la main). `generate_all.py` est idempotent.

Problèmes connus :

- Tonte et traite approximées : NeoForge 21.1 n'expose pas d'événement dédié (voir v0.4).
- Pas de QA visuelle en client pour la v0.5 : meule, pressoir, machines, écrans, jus. Procédure v0.3 (`xvfb-run`).
- Tests lents : la chaîne d'affinage (24000 et 144000 ticks) est avancée à la main dans les GameTests.
- Le combustible du fumoir inclut les bûches (`logs_that_burn`) : choix d'équilibrage à confirmer.

Prochaines étapes :

1. QA visuelle en client : élevage (v0.4) et machines de transformation (v0.5).
2. **v0.6 Équipements & automatisation** (`docs/ROADMAP.md`) : semoir, charrue attelée, silos, granges, convoyeurs et
   trémies agricoles, épouvantail.

## v0.4 « Élevage » : livrée et vérifiée (`mod_version=0.4.0`)

- `./gradlew build` : OK, **232 tests JUnit** (dont `GeneratedResourcesTest`).
- `./gradlew runData` : OK, `src/generated` régénéré.
- `./gradlew runGameTestServer` : **54 GameTests** réussis, trois passages de suite exigés; 30 passages consécutifs verts sur la version finale.
- Contrats : `docs/ARCHITECTURE.md` « Contrats v0.4 » et « Contrats v0.4 suite »; règles : `docs/DESIGN.md` §18.

Contenu de la v0.4 :

- **Élevage** (`livestock`) : génome (productivité, taille, résistance, fertilité, 0 à 10) en Data Attachment; besoins
  (cycle de 1200 ticks, pâturage hors hiver); reproduction au printemps et en été; `LivestockApi` (dont `NeedsListener`,
  `FeedSource`); registre d'élevage (`livestock_ledger`, texture dédiée).
- **Bâtiments et alimentation** (`husbandry`) : auge `feeding_trough` (foin, blé, aliment composé, ensilage; nourrit
  les animaux proches), nichoir `nesting_box` (8 emplacements; poules et canards y pondent), ensilage, `feed_mix`.
- **Santé animale** (`vet`) : maladies par surpopulation ou sous-alimentation, parasites sur pâturage humide, contagion,
  `veterinary_kit`, pierre à sel `salt_lick` (+1 de santé par cycle à proximité).
- **Canard** (`duck`) : entité `terresvivantes:duck` (mâle et femelle, nage, ponte), œufs, viande crue et cuite,
  productions selon le génome; qualité du lait en composant `terresvivantes:milk_quality`.
- **Intégration** : configs COMMON `terresvivantes-livestock`, `-husbandry`, `-vet`, `-duck` enregistrées dans
  `TerresVivantes`. Ponte des poules et des canards interceptée par le nichoir. Textures v0.4 (`tools/textures/sprites_v04.py`).
- **Commande `/tv showcase`** : section élevage (auge pleine de foin, nichoir, pierre à sel, enclos avec une vache, un
  mouton, une poule, deux canards mâle et femelle, persistants).
- **Stabilité des tests de jeu** : une zone de test n'est pas toujours « entity ticking », donc ses entités peuvent
  ne pas tiquer ou ne pas être visibles. Les coccinelles sont ticquées explicitement, la vitrine vérifie les animaux
  d'après le retour de spawn, et le tirage de maladie est fixé dans le test de nourriture (`VetApi.setRandomForTest`).

Problèmes connus et écarts :

- Tonte et traite approximées : NeoForge 21.1 n'expose pas d'événement de tonte ni de traite. La laine et le lait
  sont calculés à partir du multiplicateur, sans événement réel.
- Auge sans bord creux : le modèle n'a pas de rebord évidé.
- QA visuelle en client à faire pour le canard et l'élevage (captures `docs/qa` de la v0.3 seulement).
- Santé à 0 : l'animal meurt (`VetConfig.killAtZeroHealth`, vrai par défaut).
- Affection (registre) : non définie par le design; affichée comme état amoureux vanilla.
- Attachments non synchronisés vers le client : le registre écrit dans le chat du serveur.

Prochaines étapes :

1. **v0.5 Transformation** (`docs/ROADMAP.md`).
2. QA visuelle en client (procédure v0.3) : canard, élevage, auge, nichoir, pierre à sel.
3. Hook dédié pour la tonte et la traite, quand NeoForge en fournira un.

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
