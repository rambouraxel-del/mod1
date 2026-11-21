# Reprise du projet (après v0.8 Polish)

Document de reprise : à lire en premier. Il décrit l'état exact, ce qui est fait, ce qui reste et comment travailler.
Détails par version : `docs/STATUS.md`. Règles de design : `docs/DESIGN.md`. Contrats entre modules : `docs/ARCHITECTURE.md`.
Consignes pour les agents : `docs/AGENT_GUIDE.md`. Équilibrage : `docs/BALANCE.md`. Performances : `docs/PERFORMANCE.md`.

## 1. État exact

- Version : `mod_version=0.9.0` (NeoForge 21.1.267, Minecraft 1.21.1). v0.9 « Vignoble, riz et races » livrée.
- Branche : `claude/terres-vivantes-minecraft-g4o0hy`, poussée sur `origin`. Le commit de tête est celui de cette
  documentation (`git log -1`).
- Intégration v0.9 : trois fusions `--no-ff` dans la branche (`git log --merges`) :
  - `worktree-agent-a2dc450b0e8f6c9e1` : vignoble (`vineyard`);
  - `worktree-agent-aade54efa10d82d74` : riz (`rice`). Conflit : `economy/base_prices.json` (union des entrées);
  - `worktree-agent-a3136148f06b6b2e2` : races (`breeds`). Conflit : `TerresVivantes.MODULES` (vignoble et races gardés).
  Les fichiers générés (`src/generated`) sont régénérés par `runData`, pas fusionnés.
- Intégration v0.9 (corrections à l'intégration) : branchements sur les APIs publiques (`CropGrowthHooks`,
  `VetApi`, `LivestockApi`, `VineyardApi`), récolteuse qui récolte les vignes, tag `compostable` élargi, Highland en
  écouteur de besoins, jumeaux qui héritent de la race, affinage du vin rouge (nouveau), vitrine v0.9, chapitre 12 du manuel.
- Vérifications sur ce commit :
  - `./gradlew cleanTest build` : OK, 384 tests JUnit, 0 échec.
  - `./gradlew runData` : OK, `src/generated` régénéré.
  - `./gradlew runGameTestServer` : 127/127 à trois passages consécutifs.
  - `./gradlew runServer` : « Done (0.396s) », arrêt par SIGTERM avec sauvegarde des mondes.
- Modules : 36, listés dans `docs/STATUS.md`. Configs COMMON : `terresvivantes-season`, `-soil`, `-compost`, `-crop`,
  `-water`, `-health`, `-orchard`, `-flora`, `-insects`, `-apiculture`, `-livestock`, `-husbandry`, `-vet`, `-duck`,
  `-automation`, `-pests`, `-guide` (manuel), `-vineyard`. Les autres modules n'ont pas de config.
- Arbre de travail propre après le commit. Worktrees de `.claude/worktrees` et branches fusionnées supprimés.
- Licence : fichier `LICENSE` présent (MIT, `mod_license` dans `gradle.properties`).

## 2. Fonctionnalités réalisées, par version

- **v0.1 Fondations** : saisons, climat local, sol (N/P/K, matière organique, compost, fumier, cendre), 12 cultures
  (graines, récolte à main vide), 10 plats, outils (faucille, arrosoir, sonde, almanach).
- **v0.2 Vergers, eau, santé** : serres et hiver, rotation des cultures, six espèces fruitières, arroseurs, tonneau de pluie,
  sécheresse, maladies et flétrissement, fongicide, `/tv showcase`.
- **v0.3 Biodiversité** : indice de pollinisation et de biodiversité, flore sauvage et haies, insectes (papillons, coccinelles,
  pucerons, lucioles), apiculture (apiaire, extracteur, miels typés).
- **v0.4 Élevage** : génome des animaux, besoins et reproduction, auge, nichoir, ensilage, santé animale (maladies,
  parasites, sel), canard avec œufs et productions.
- **v0.5 Transformation** : qualité générique (1 à 3 étoiles), infrastructure de machines (recettes JSON, conteneur, sortie),
  meule, pressoir, baratte, cuve à fromage, étagère d'affinage, tonneau, séchoir, fumoir, marmite (12 plats).
- **v0.6 Équipements** : grelinette, semoir, faucille en diamant, récolteuse, planteuse, caisse, silo, corbeau, épouvantail.
- **v0.7 Économie** : monnaie 9:1 (cuivre, argent, or) avec conversions dans les deux sens; prix (base, qualité, saison,
  saturation); réputation par paliers; tableau d'annonces avec contrats quotidiens; étal qui vend à un villageois de jour;
  guilde (marchand, échanges par niveau, verrou par réputation, titres de spécialisation à 256 unités); bonus de qualité
  des machines de la filière pour le dernier joueur; commandes `/tv price`, `/tv reputation`, `/tv titles`.
- **v0.8 Polish** : sprites faibles repris; manuel du fermier en jeu (item, livre fr et en, 11 chapitres, remise à la première
  connexion); équilibrage des cultures, de la maladie, des prix et des contrats (`docs/BALANCE.md`, simulateur JUnit);
  famine ramenée à 5 jours MC; serveur dédié sans classe client (test de garde-fou); test de charge serveur
  (`docs/PERFORMANCE.md`, surcoût de 3,6 ms/tick pour 500 cultures et 50 animaux, seuil 15).

- **v0.9 Vignoble, riz et races** : vigne sur treillis (4 stades, récolte à la main et par la récolteuse, taille d'hiver),
  vinification (pressoir, tonneau, étagère d'affinage avec gain d'étoile pour le vin rouge, millésime); rizière (plant dans
  l'eau, panicules, meule, bol de riz, risotto, galette); races des vaches et des poules (attribution par climat, hérédité
  à moitié, jumeaux compris, effets sur la production, la viande, la ponte, la maladie et la faim d'hiver); vitrine v0.9;
  chapitre 12 du manuel.

## 3. Travaux en cours

Aucun. Pause propre : tout est commité et poussé, aucun travail partiellement écrit.
La QA visuelle et de gameplay sur un vrai PC reste à faire par l'utilisateur (`docs/QA_CHECKLIST.md`).

## 4. Problèmes connus et dettes

- **QA visuelle client à faire sur vraie GPU** : rendu logiciel (Xvfb) insuffisant. Liste complète : `docs/QA_CHECKLIST.md`.
- **Machines invisibles en jeu (client)** : non confirmé. Analyse statique OK : `getRenderShape` des blocs du mod est
  `MODEL` (garde-fou `RenderShapeGameTests`), blockstates et modèles présents. À vérifier sur une vraie carte graphique avant
  de chercher une cause dans le code. Si confirmé, pistes : rendu des `BlockEntity` sans BlockEntityRenderer, textures à
  l'exécution, couche de rendu.
- **Rendu des races non vérifié** : superposition `BreedLayer` (Holstein, Highland, Charolais, Marans, Leghorn, Sussex).
- **Tonte et traite approximées** : NeoForge 21.1 n'expose pas d'événement dédié; la laine et le lait sont calculés à
  partir du multiplicateur.
- **Auge sans bord creux** : le modèle n'a pas de rebord évidé.
- **Bonus de lapins de l'épouvantail** : non fait (prévu au design).
- **Vignoble, hypothèses non testées en jeu** :
  - qualité de départ ★2 des raisins (`VineyardLogic.GRAPE_QUALITY`, choix d'équilibrage);
  - gain d'affinage du vin rouge (+1 étoile par 6 jours MC, nouveau mécanisme) : pas de GameTest de vieillissement;
  - cycle annuel de la vigne (une récolte par an, taille d'hiver) : pas de test de bout en bout.
- **Riz** : la règle « pas de sécheresse sous l'eau » (`WaterGrowth`) est en code, sans GameTest dédié. La pollinisation
  ne s'applique pas au riz (choix documenté, `DESIGN.md` §22.2).
- **Highland** : la compensation d'hiver arrive après le cycle de santé; à satiété nulle en hiver elle perd encore un point
  de santé ce cycle-là.
- **Équilibrage** (`docs/BALANCE.md`) :
  - « parmi les meilleurs » (vin rouge ★3) défini comme « au-dessus de la médiane des blocs » : le vin rouge est 6e sur 14
    (2,14 pièces/jour et par cep). La fraise (3,20) et le soja (2,78) restent devant. Décision à prendre : garder, ou baisser
    le prix du vin rouge pour le ramener dans le top 3;
  - la famine dure 5 jours MC à satiété nulle (perte de 1 par cycle, santé entière);
  - le lait ne varie qu'en qualité selon l'alimentation, jamais en quantité;
  - les étagères d'affinage (capacité) et la nuit (lumière insuffisante) ne sont pas simulées.
- **Économie** : bonus de qualité des machines testé seulement par le mapping (JUnit), pas de GameTest de bout en bout;
  réputation après mort non testée; achat PNJ et contrats testés par GameTest, pas en jeu.
- **Combustible du fumoir** : les bûches sont comprises (à confirmer en équilibrage).
- **Citrouille et pastèque** hors pollinisation. Entités sans loot (papillons, coccinelles).
- **Pas de GUI** pour l'apiaire ni pour le silo. Le silo n'affiche pas son contenu.
- **Historique git** : l'attribution `Co-Authored-By` des commits est hétérogène (plusieurs modèles selon les sessions).
  Le commit de v0.9 porte le trailer demandé dans la consigne d'intégration, qui ne correspond pas au modèle réellement utilisé.
- **Parchment non utilisé** : `maven.parchmentmc.org` redirige vers un hôte bloqué par le proxy. Les noms de paramètres sont
  ceux de Mojang.

## 5. Méthode d'orchestration (ce qui a fonctionné)

a. Contrats séquentiels dans le dépôt principal : façades, hooks, modules vides enregistrés, documentation.
b. Modules en parallèle, en worktrees isolés (`.claude/worktrees`). Règles : ne modifier que son package; les configs
   `XxxConfig` sont branchées par l'intégrateur; ne pas committer `src/generated` dans un worktree; `build`, `runData` et
   `runGameTestServer` doivent être verts avant de rendre la main.
c. Textures en worktree dédié (`tools/textures`, Pillow, grilles 16x16). Un seul script idempotent : `generate_all.py`.
d. Intégrateur (dans le dépôt principal) : fusions `--no-ff`, conflits sur les binaires (PNG de pièces, aperçu des textures)
   résolus puis régénérés par le script, configs, `TVCommands` pour la racine `/tv`, `runData`, GameTests (trois passages),
   documentation, push, nettoyage des worktrees et des branches locales.
e. Gradle en premier plan, avec timeout; un seul Gradle à la fois. Une recette ou un tag manquant n'apparaît dans le log
   qu'après `runData`: le relancer après une fusion. `./gradlew --stop` en fin de session.
f. Les JUnit sont rejouables avec `./gradlew cleanTest test` (Gradle peut considérer la tâche à jour sans cela).
g. Worktrees : leurs `src/generated` sont modifiés (régénérés, non commités); ils se jettent avec `git worktree remove --force`
   une fois la branche fusionnée. Un conflit de liste (`TerresVivantes.MODULES`) se résout en gardant toutes les lignes.
h. Contraintes de contenu testées : le manuel impose 3 à 6 pages par chapitre et 300 caractères au plus par page
   (`GuideContentTest`). Pour ajouter une information à un chapitre plein, réécrire une page plutôt qu'en ajouter une.

## 6. Procédure de QA visuelle client

- `xvfb-run` avec `LIBGL_ALWAYS_SOFTWARE=1`, monde superplat créatif, `allowCommands` via nbtlib,
  `--quickPlaySingleplayer`, puis `/tv showcase`. Les captures vont dans `docs/qa`.
- À vérifier dans l'ordre : machines de transformation (rangée z 17), marmite, silo, caisse, champs de la vitrine (z 23 à 31),
  section économie (z 34 à 39), enclos d'élevage, livre du manuel du fermier (clic droit sur l'item).

## 7. Prochaines étapes

1. **Release 1.0** :
   - QA manuelle par l'utilisateur sur un vrai PC, avec la checklist `docs/QA_CHECKLIST.md` (visuel par module, gameplay
     par module, serveur dédié). Consigner les écarts; corriger les points bloquants; le reste va en 1.0.x ou en 1.1;
   - jar : `./gradlew build` produit `build/libs/terresvivantes-<version>.jar` (version `1.0.0` dans `gradle.properties`
     au moment de la release);
   - changelog : `CHANGELOG.md`, v0.1 à v0.9 (fait); ajouter la section 1.0 à la sortie;
   - captures : à produire pendant la QA visuelle (procédure section 6, sur carte réelle);
   - README joueur : à relire après la QA visuelle.
2. **v1.1** (idées, à cadrer dans `docs/ROADMAP.md`) :
   - apiculture avancée : reines, génétique des colonies;
   - météo extrême : grêle, sécheresse prolongée;
   - saisons visuelles : couleur du feuillage et de l'herbe selon la saison.
