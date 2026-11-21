# Changelog

Chaque version correspond à un commit d'intégration `feat(vX.Y)` et à un `mod_version` de `gradle.properties`.
Le détail et les vérifications de chaque version sont dans `docs/STATUS.md`.

## 0.9.0 — Vignoble, riz et races

- Vignoble : treillis, boutures rouge et blanche, vigne à 4 stades, récolte à la main et par la récolteuse, taille d'hiver,
  vinification (jus, vin jeune, vin rouge affiné, vin blanc), millésime.
- Riz : rizière (plant dans l'eau), 7 stades, panicules, meule, bol de riz, risotto, galette de riz.
- Races : Holstein, Highland, Charolais (vaches); Marans, Leghorn, Sussex (poules). Attribution par climat, hérédité 50/50
  (jumeaux compris), effets sur la production, la viande, la ponte, la maladie et la faim d'hiver.
- API publiques : hooks de culture (`CropGrowthHooks`), `VetApi.registerDiseaseChanceModifier`,
  `LivestockApi.registerTwinListener`, `VineyardApi`, `RiceApi.plant`.
- Vitrine : section v0.9 (vignes, rizière, enclos des races). Manuel : chapitre 12.

## 0.8.0 — Polish

- Sprites faibles repris; manuel du fermier en jeu (fr et en, 11 chapitres); équilibrage (`docs/BALANCE.md`, simulateur).
- Serveur dédié sans classe client; test de charge serveur (`docs/PERFORMANCE.md`).

## 0.7.0 — Économie

- Monnaie cuivre, argent et or (9:1); prix (base, qualité, saison, saturation); réputation par paliers.
- Tableau d'annonces, étal, guilde (marchand, échanges, titres de spécialisation), bonus de qualité des machines.

## 0.6.0 — Équipements et automatisation

- Grelinette, semoir, faucille en diamant, récolteuse, planteuse, caisse, silo, corbeau, épouvantail.

## 0.5.0 — Transformation

- Qualité générique (1 à 3 étoiles); infrastructure des machines (recettes JSON); meule, pressoir, baratte, cuve à fromage,
  étagère d'affinage, tonneau, séchoir, fumoir, marmite (12 plats).

## 0.4.0 — Élevage

- Génome des animaux, besoins et reproduction, auge, nichoir, ensilage, santé animale, canard (œufs et productions).

## 0.3.0 — Biodiversité

- Pollinisation et indice de biodiversité; flore sauvage et haies; insectes (papillons, coccinelles, pucerons, lucioles);
  apiculture (apiaire, extracteur, miels typés).

## 0.2.0 — Vergers, eau, santé

- Serres et hiver, rotation des cultures, six espèces fruitières, arroseurs, tonneau de pluie, sécheresse, maladies,
  fongicide, `/tv showcase`.

## 0.1.0 — Fondations

- Saisons, climat local, sol (N/P/K, matière organique, compost, fumier, cendre), 12 cultures, 10 plats, outils (faucille,
  arrosoir, sonde, almanach).
