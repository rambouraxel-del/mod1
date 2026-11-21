# Checklist de QA (Release 1.0)

Vérifications à faire par l'utilisateur sur un vrai PC (carte graphique réelle, pas de rendu logiciel), avant la 1.0.
Cocher chaque case; noter le module, la version et la capture si un point échoue. Les captures vont dans `docs/qa/`.

## Préparation

- [ ] Compiler le jar : `./gradlew build`, puis vérifier `build/libs/terresvivantes-1.0.0.jar` (version dans `gradle.properties`).
- [ ] Monde de test créatif superplat, avec commandes autorisées.
- [ ] `/tv showcase` (opérateur) : la plateforme se construit sans erreur dans le journal.
- [ ] Dans le journal client : aucune erreur « Couldn't parse », « Parsing error » ni « Missing model » pour `terresvivantes:`.

## Visuel (rendu client)

- [ ] **Machines de transformation** (rangée z 17) : meule, pressoir, baratte, cuve, étagère, tonneau, séchoir, fumoir
  visibles, texture correcte. Problème connu : machines invisibles signalées depuis v0.5, à confirmer ou écarter sur carte réelle.
- [ ] **Marmite** sur feu de camp allumé (z 20) : flamme visible.
- [ ] **Caisse et silo** (z 24 et z 27) : rendu du bloc; le silo n'affiche pas son contenu (connu).
- [ ] **Tableau d'annonces, étal, bureau de la guilde** (z 34 à 39) : rendu, marchand persistant visible.
- [ ] **Enclos d'élevage** (z 9 à 15) : auge, nichoir, pierre à sel, clôture, vache, mouton, poule, canards.
- [ ] **Enclos des races** (z 48 à 54) : trois vaches et trois poules; **superposition de race** visible
  (Holstein, Highland, Charolais; Marans, Leghorn, Sussex). Problème connu : rendu des races non vérifié.
- [ ] **Vignes** (z 42, x 1 et x 3) : treillis, croix de feuillage, grappes rouges et blanches au stade 3.
- [ ] **Vignes aux stades 0 à 2** (z 42, à poser en survie) : texture différente selon le stade.
- [ ] **Rizière** (x 8 à 12, z 42 à 46) : riz mûr dans l'eau, bord libre.
- [ ] **Livre du manuel du fermier** (clic droit sur l'item) : douze chapitres en français, chapitre 12 « Vignes, riz et races »
  lisible, aucune page qui déborde.
- [ ] **Cultures de la vitrine** (z 0 à 3) : stades 0, 2, 5, 7, les vanilla compris.

## Gameplay par module

- [ ] **Saisons** (`/tv season set`, HUD) : changement visible, la croissance suit la saison.
- [ ] **Climat et serres** : température locale cohérente avec le biome; la serre protège en hiver.
- [ ] **Sol** : N/P/K baisse après récolte; compost, fumier et cendre rechargent; rotation (soja) améliore le sol.
- [ ] **Cultures** : plantation sur terre labourée, récolte au clic droit à maturité, replantation à l'âge 0.
- [ ] **Vergers** : six espèces, feuillages à quatre stades, fruits à récolter, marchand ambulant.
- [ ] **Eau** : arroseur (rayon 2 et 3), tonneau de pluie, sécheresse estivale (terre qui sèche).
- [ ] **Santé des cultures** : oïdium ou rouille sur tomate ou blé, fongicide et pulvérisateur, flétrissement.
- [ ] **Pollinisation et biodiversité** : pollinisateurs (apiaire, papillons) modifient les rendements.
- [ ] **Flore** : fleurs sauvages, teintures, haie d'aubépine.
- [ ] **Insectes** : papillons, coccinelles (contre les pucerons), lucioles la nuit, filet et bocal.
- [ ] **Apiculture** : apiaire peuplé, extracteur, enfumoir, miel typé par la fleur. Pas de GUI (connu).
- [ ] **Élevage** : besoins quotidiens, pâturage sur herbe hors hiver, reproduction au printemps et en été, registre d'élevage.
- [ ] **Santé animale** : maladie par surpopulation, parasites, trousse vétérinaire, pierre à sel.
- [ ] **Canard** : mâle et femelle, nage, ponte, œufs et viande. Rendu non vérifié en client (v0.4).
- [ ] **Transformation** : meule (blé → farine), pressoir (pommes → jus), baratte (lait → crème → beurre), étagère (fromage affiné).
- [ ] **Conservation** : tonneau (cidre, cornichons), séchoir, fumoir (combustible : bûches, charbon).
- [ ] **Cuisine** : marmite, 12 plats, effets de nutrition.
- [ ] **Équipements** : grelinette (aération limitée à une fois par jour MC), semoir, faucille en diamant.
- [ ] **Automatisation** : récolteuse (récolte aussi les vignes au stade 3 et les feuilles fruitières mûres), planteuse.
- [ ] **Stockage** : caisse (8 cases), silo (1 à 8 blocs).
- [ ] **Ravageurs** : corbeau (picore les cultures mûres hors hiver), épouvantail (réduit les ravageurs).
- [ ] **Économie** : `/tv price`, `/tv reputation`, `/tv titles`; monnaie 9:1 (conversions dans les deux sens).
- [ ] **Marché** : contrats du tableau d'annonces, étal qui vend à un villageois de jour.
- [ ] **Guilde** : échanges par niveau, verrou par réputation, titre de spécialisation à 256 unités, bonus de qualité.
- [ ] **Vignoble** : pressoir (2 raisins rouges et une bouteille → jus), tonneau (jus → vin jeune; bouteille consommée,
  rendue en buvant le vin jeune), étagère (vin jeune → vin rouge en 6 jours, une étoile de plus), tonneau (4 raisins blancs
  → vin blanc). Vin rouge sans bouteille. Millésime affiché au survol.
- [ ] **Riz** : plant sur l'eau au-dessus d'une terre; casse si l'eau disparaît; meule (panicule → 2 riz); bol de riz, risotto, galette.
- [ ] **Races** : attribution à l'arrivée; héritage des petits et des jumeaux; Charolais (viande +1), Holstein (production),
  Highland (hiver), Marans (œuf en plus), Leghorn (ponte), Sussex (maladie).
- [ ] **Manuel** : remise du livre à la première connexion, pas de remise en double à la reconnexion.

## Serveur dédié

- [ ] Serveur dédié sans client : démarrage sans erreur, `Done`, arrêt propre (sauvegarde des mondes).
- [ ] Monde chargé avec des vignes, un riz, des animaux de race : aucune erreur de chargement de race (`Race inconnue`).

## Verdict

- [ ] Aucun point bloquant ouvert. Les points « connus » de `docs/REPRISE.md`, section 4, sont acceptés ou corrigés.
