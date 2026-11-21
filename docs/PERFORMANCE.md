# Performance serveur

Mesures du coût d'un tick de niveau serveur (`LevelTickEvent.Pre` à `Post`), sur le test de charge
`PerformanceGameTests` (batch `perf_load`, gabarit `perf_arena` 32 x 4 x 32).

## Charge du test

- 500 cultures du mod sur farmland (25 x 20, quatre espèces).
- 50 animaux du tag `livestock` (cow, sheep, pig, chicken, goat : 10 de chaque).
- 4 récolteuses (`harvester`, orientation sud), sans cultures mûres à récolter.
- 10 meules (`millstone`) avec 64 blés chacune, donc actives pendant toute la mesure.

Le test mesure 200 ticks sur l'arène vide (ligne de base), puis 200 ticks avec la charge. Il échoue si le
surcoût dépasse 15 ms/tick.

## Résultats

| Phase | ms/tick (moyenne sur 200 ticks) |
|---|---|
| Ligne de base (arène vide) | 0.221 |
| Charge complète | 3.783 |
| Surcoût ajouté | 3.562 (seuil 15) |

Mesure faite sur le commit d'intégration de la v0.8 avec `./gradlew runGameTestServer` (106 tests requis verts, dernier
passage). Le tableau donne le dernier passage. Les trois passages finaux ont donné 3,41, 3,66 et 3,78 ms/tick en charge; le
test ne vérifie que le seuil.

## Garde-fous déjà en place

- `PollinationApi` : score mis en cache par tranche de 16 blocs, durée 600 ticks (`ChunkScoreCache`).
- Arroseurs : cycle de 100 ticks par compteur, sans scan hors de ce cycle.
- Besoins du bétail : étalés par `tickCount % TICK_INTERVAL`, décalés par identifiant d'animal.
- Automatisation : sortie immédiate si la configuration est désactivée; cycles espacés de `intervalTicks`.
- Tableau d'annonces : recalcul seulement au changement de jour MC.

## Limites

- La charge reste en dessous du seuil. Pas de profilage par composant : le test donne le coût total seulement.
- Les cultures sont posées à des âges fixes et les animaux ne sont pas nourris; un monde réel a plus d'entités.
- Les scans de la mise en cache de la pollinisation (`countHives`, environ 40 000 lectures de blocs par tranche) ne
  sont mesurés qu'au premier calcul de chaque tranche toutes les 30 s.
