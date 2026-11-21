# Équilibrage v0.9

Généré par `src/test/java/fr/terresvivantes/balance/BalanceSimulationTest.java` (`./gradlew test`).
Les tableaux « après » sont régénérés à chaque exécution; le bloc « avant » est conservé entre les marqueurs.

## Avant (valeurs v0.8)

<!-- AVANT:BEGIN -->
## (a) Maturation (jours MC moyens, champ de jour)

| Culture | Optimale | Possible | Hors saison (défavorable) | Serre hiver | Sol épuisé (optimale) |
|---|---|---|---|---|---|
| tomato | 1.1 j | 1.6 j | 4.8 j | 2.6 j | 4.1 j |
| corn | 1.1 j | 1.6 j | 4.9 j | 2.6 j | 4.2 j |
| soybean | 1.1 j | 1.5 j | 4.9 j | 2.6 j | 4.2 j |
| lettuce | 1.5 j | 2.3 j | 3.8 j | 2.4 j | 5.8 j |
| onion | 1.5 j | 2.3 j | 3.8 j | 2.4 j | 5.7 j |
| garlic | 1.7 j | 1.7 j | 3.6 j | 2.8 j | 6.8 j |
| bell_pepper | 1.1 j | 1.6 j | 4.8 j | 2.6 j | 4.1 j |
| eggplant | 1.1 j | 1.5 j | 4.8 j | 2.6 j | 4.2 j |
| strawberry | 1.3 j | 1.3 j | 4.8 j | 2.1 j | 4.9 j |
| barley | 1.3 j | 1.3 j | 4.8 j | 2.1 j | 4.9 j |
| cabbage | 1.7 j | 1.7 j | 3.5 j | 2.8 j | 6.8 j |
| cucumber | 1.1 j | 1.6 j | 4.9 j | 2.6 j | 4.2 j |

## (b) Récoltes avant épuisement du sol (sol neuf, sans engrais)

| Régime | Récoltes avant épuisement (min(N,P,K) < 20) |
|---|---|
| Monoculture tomate | 11 |
| Monoculture maïs | 11 |
| Monoculture soja (légumineuse) | 16 |
| Rotation soja / tomate | 16 |

## (c) Pertes par maladie sur une saison (champ 9x9, été, comparé au même champ sans maladie)

| Parcelle | Perte de rendement sur la saison |
|---|---|
| Monoculture tomate (oïdium) | 18 % |
| Monoculture maïs (rouille) | 18 % |
| Diversifiée (9 espèces) | 7 % |

## (d) Revenu par jour MC (champ 9x9 vendu à l'étal, saturation incluse)

| Culture | Jours par cycle | Rendement moyen | Valeur de base x rendement | Pièces de cuivre / jour |
|---|---|---|---|---|
| tomato | 1.1 j | 2.5 | 15.0 | 188 |
| corn | 1.1 j | 2.0 | 12.0 | 149 |
| soybean | 1.1 j | 3.0 | 15.0 | 225 |
| lettuce | 1.5 j | 2.5 | 10.0 | 136 |
| onion | 1.5 j | 2.0 | 10.0 | 109 |
| garlic | 1.7 j | 2.5 | 15.0 | 119 |
| bell_pepper | 1.1 j | 2.0 | 14.0 | 148 |
| eggplant | 1.1 j | 1.5 | 10.5 | 114 |
| strawberry | 1.3 j | 2.0 | 16.0 | 259 |
| barley | 1.3 j | 3.0 | 12.0 | 189 |
| cabbage | 1.7 j | 2.5 | 12.5 | 120 |
| cucumber | 1.1 j | 2.5 | 15.0 | 187 |

Médiane : 149. Meilleure culture / médiane = 1.74.

## Chaînes transformées


Lait de départ ★2 ; fromage affiné après 6 jours : ★3.

| Chaîne | Valeur brute (1 unité) | Valeur transformée par unité brute | Ratio | Délai (jours MC) |
|---|---|---|---|---|
| Lait → crème | 8 | 16.0 | 2.00 × | 0.0 |
| Lait → beurre | 8 | 13.0 | 1.63 × | 0.0 |
| Lait → fromage affiné | 8 | 21.0 | 2.63 × | 7.0 |
| Olive → huile d'olive | 6 | 11.0 | 1.83 × | 0.0 |
| Pomme → cidre | 5 | 12.0 | 2.40 × | 2.0 |

## Contrats contre vente directe

Récompense d'un contrat par unité / vente à l'étal (90 %) : **+44 %**.

## (e) Élevage : 10 vaches sur 28 jours MC (productivité 6)

| Régime | Satiété moyenne (fin) | Santé moyenne (fin) | Vivantes à la fin | Lait ★1 / ★2 / ★3 | Valeur lait / jour (troupeau) | Repas de foin / vache |
|---|---|---|---|---|---|---|
| Bien nourries (pâturage, auge en hiver) | 85 / 100 | 100 / 100 | 10 / 10 | 0 / 0 / 280 | 90 | 111.0 |
| Mal nourries (pâturage 1 cycle sur 3, rien en hiver) | 0 / 100 | 0 / 100 | 0 / 10 | 76 / 0 / 0 | 16 | 0.0 |

## Bilan des cibles

| Cible | Attendu | Obtenu | Atteinte |
|---|---|---|---|
| Maturation optimale (toutes cultures) | 1 à 2 j | 1.1 à 1.7 j | oui |
| Serre hiver (toutes cultures) | 2 à 3 j | 2.1 à 2.8 j | oui |
| Hors saison défavorable (toutes cultures) | 3 à 5 j | 3.5 à 4.9 j | oui |
| Sol neuf, monoculture tomate et maïs | 8 à 12 récoltes | 11 et 11 | oui |
| Rotation soja / tomate vs monoculture tomate | plus longue | 16 contre 11 | oui |
| Perte en monoculture (tomate) | 15 à 30 % | 18 % | oui |
| Perte en parcelle diversifiée | < 8 % | 7 % | oui |
| Aucune culture ne domine | meilleure / médiane < 2 | 1.74 | oui |
| Lait → crème (par unité brute) | 1,5 à 3 × | 2.00 × | oui |
| Lait → beurre (par unité brute) | 1,5 à 3 × | 1.63 × | oui |
| Olive → huile d'olive (par unité brute) | 1,5 à 3 × | 1.83 × | oui |
| Pomme → cidre (par unité brute) | 1,5 à 3 × | 2.40 × | oui |
| Fromage affiné : meilleur revenu par vache | au-dessus de la crème, du beurre et du lait | 21 contre 16, 13, 8 | oui |
| Fromage de chèvre (base) < fromage affiné (base) | < 14 pièces | 11 pièces | oui |
| Contrat contre vente directe | +30 à 60 % | +44 % | oui |
| Famine : survie à satiété 0 (santé pleine) | >= 4 j MC | 5.0 j MC | oui |
<!-- AVANT:END -->

## Après (valeurs v0.9)

## (a) Maturation (jours MC moyens, champ de jour)

| Culture | Optimale | Possible | Hors saison (défavorable) | Serre hiver | Sol épuisé (optimale) |
|---|---|---|---|---|---|
| tomato | 1.1 j | 1.6 j | 4.8 j | 2.6 j | 4.1 j |
| corn | 1.1 j | 1.6 j | 4.9 j | 2.6 j | 4.2 j |
| soybean | 1.1 j | 1.5 j | 4.9 j | 2.6 j | 4.2 j |
| lettuce | 1.5 j | 2.3 j | 3.8 j | 2.4 j | 5.8 j |
| onion | 1.5 j | 2.3 j | 3.8 j | 2.4 j | 5.7 j |
| garlic | 1.7 j | 1.7 j | 3.6 j | 2.8 j | 6.8 j |
| bell_pepper | 1.1 j | 1.6 j | 4.8 j | 2.6 j | 4.1 j |
| eggplant | 1.1 j | 1.5 j | 4.8 j | 2.6 j | 4.2 j |
| strawberry | 1.3 j | 1.3 j | 4.8 j | 2.1 j | 4.9 j |
| barley | 1.3 j | 1.3 j | 4.8 j | 2.1 j | 4.9 j |
| cabbage | 1.7 j | 1.7 j | 3.5 j | 2.8 j | 6.8 j |
| cucumber | 1.1 j | 1.6 j | 4.9 j | 2.6 j | 4.2 j |

## (b) Récoltes avant épuisement du sol (sol neuf, sans engrais)

| Régime | Récoltes avant épuisement (min(N,P,K) < 20) |
|---|---|
| Monoculture tomate | 11 |
| Monoculture maïs | 11 |
| Monoculture soja (légumineuse) | 16 |
| Rotation soja / tomate | 16 |

## (c) Pertes par maladie sur une saison (champ 9x9, été, comparé au même champ sans maladie)

| Parcelle | Perte de rendement sur la saison |
|---|---|
| Monoculture tomate (oïdium) | 18 % |
| Monoculture maïs (rouille) | 18 % |
| Diversifiée (9 espèces) | 7 % |

## (d) Revenu par jour MC (champ 9x9 vendu à l'étal, saturation incluse)

| Culture | Jours par cycle | Rendement moyen | Valeur de base x rendement | Pièces de cuivre / jour |
|---|---|---|---|---|
| tomato | 1.1 j | 2.5 | 15.0 | 188 |
| corn | 1.1 j | 2.0 | 12.0 | 149 |
| soybean | 1.1 j | 3.0 | 15.0 | 225 |
| lettuce | 1.5 j | 2.5 | 10.0 | 136 |
| onion | 1.5 j | 2.0 | 10.0 | 109 |
| garlic | 1.7 j | 2.5 | 15.0 | 119 |
| bell_pepper | 1.1 j | 2.0 | 14.0 | 148 |
| eggplant | 1.1 j | 1.5 | 10.5 | 114 |
| strawberry | 1.3 j | 2.0 | 16.0 | 259 |
| barley | 1.3 j | 3.0 | 12.0 | 189 |
| cabbage | 1.7 j | 2.5 | 12.5 | 120 |
| cucumber | 1.1 j | 2.5 | 15.0 | 187 |

Médiane : 149. Meilleure culture / médiane = 1.74.

## Chaînes transformées


Lait de départ ★2 ; fromage affiné après 6 jours : ★3.

| Chaîne | Valeur brute (1 unité) | Valeur transformée par unité brute | Ratio | Délai (jours MC) |
|---|---|---|---|---|
| Lait → crème | 8 | 16.0 | 2.00 × | 0.0 |
| Lait → beurre | 8 | 13.0 | 1.63 × | 0.0 |
| Lait → fromage affiné | 8 | 21.0 | 2.63 × | 7.0 |
| Olive → huile d'olive | 6 | 11.0 | 1.83 × | 0.0 |
| Pomme → cidre | 5 | 12.0 | 2.40 × | 2.0 |

## Contrats contre vente directe

Récompense d'un contrat par unité / vente à l'étal (90 %) : **+44 %**.

## (e) Élevage : 10 vaches sur 28 jours MC (productivité 6)

| Régime | Satiété moyenne (fin) | Santé moyenne (fin) | Vivantes à la fin | Lait ★1 / ★2 / ★3 | Valeur lait / jour (troupeau) | Repas de foin / vache |
|---|---|---|---|---|---|---|
| Bien nourries (pâturage, auge en hiver) | 85 / 100 | 100 / 100 | 10 / 10 | 0 / 0 / 280 | 90 | 111.0 |
| Mal nourries (pâturage 1 cycle sur 3, rien en hiver) | 0 / 100 | 0 / 100 | 0 / 10 | 76 / 0 / 0 | 16 | 0.0 |

## (f) Vignoble et riz : revenu par bloc et par jour MC (vente à l'étal, 90 %)

Un bloc = une case de culture (un plant de riz, un cep de vigne) ; les cultures du champ sont ramenées à la case.

| Bloc | Cycle (jours MC) | Pièces de cuivre / jour / bloc |
|---|---|---|
| tomato | 1.1 j | 2.31 |
| corn | 1.1 j | 1.84 |
| soybean | 1.1 j | 2.78 |
| lettuce | 1.5 j | 1.68 |
| onion | 1.5 j | 1.35 |
| garlic | 1.7 j | 1.47 |
| bell_pepper | 1.1 j | 1.83 |
| eggplant | 1.1 j | 1.40 |
| strawberry | 1.3 j | 3.20 |
| barley | 1.3 j | 2.33 |
| cabbage | 1.7 j | 1.48 |
| cucumber | 1.1 j | 2.31 |
| rice | 14.0 j (été) | 0.36 |
| red_wine_vine | 1 an de vigne ; chaîne 8.0 j | 2.14 |

## Bilan des cibles

| Cible | Attendu | Obtenu | Atteinte |
|---|---|---|---|
| Maturation optimale (toutes cultures) | 1 à 2 j | 1.1 à 1.7 j | oui |
| Serre hiver (toutes cultures) | 2 à 3 j | 2.1 à 2.8 j | oui |
| Hors saison défavorable (toutes cultures) | 3 à 5 j | 3.5 à 4.9 j | oui |
| Sol neuf, monoculture tomate et maïs | 8 à 12 récoltes | 11 et 11 | oui |
| Rotation soja / tomate vs monoculture tomate | plus longue | 16 contre 11 | oui |
| Perte en monoculture (tomate) | 15 à 30 % | 18 % | oui |
| Perte en parcelle diversifiée | < 8 % | 7 % | oui |
| Aucune culture ne domine | meilleure / médiane < 2 | 1.74 | oui |
| Lait → crème (par unité brute) | 1,5 à 3 × | 2.00 × | oui |
| Lait → beurre (par unité brute) | 1,5 à 3 × | 1.63 × | oui |
| Olive → huile d'olive (par unité brute) | 1,5 à 3 × | 1.83 × | oui |
| Pomme → cidre (par unité brute) | 1,5 à 3 × | 2.40 × | oui |
| Fromage affiné : meilleur revenu par vache | au-dessus de la crème, du beurre et du lait | 21 contre 16, 13, 8 | oui |
| Fromage de chèvre (base) < fromage affiné (base) | < 14 pièces | 11 pièces | oui |
| Contrat contre vente directe | +30 à 60 % | +44 % | oui |
| Famine : survie à satiété 0 (santé pleine) | >= 4 j MC | 5.0 j MC | oui |
| Vin rouge ★3 (vigne) : revenu par bloc parmi les meilleurs | au-dessus de la médiane des blocs | rang 6 sur 14, 2.14 pièces/j (médiane 1.84) | oui |
| Vin rouge : lent (chaîne après récolte, cycle annuel) | >= 6 j de chaîne, 1 récolte par an | 8.0 j de chaîne, 1 récolte par an | oui |
| Riz : revenu par bloc (rizière) | au-dessus de 0 (informatif) | 0.36 pièces/j | oui |

## Changements et justifications

<!-- CHANGES:BEGIN -->
**v0.9 : vignoble et riz (section f).**

- Définition de « parmi les meilleurs » : au-dessus de la médiane des blocs (cultures, riz et vigne). Le vin rouge ★3 est au-dessus (2,14 pièces par jour et par cep, rang 6 sur 14, médiane 1,84). Il n'est pas dans les trois premiers : la fraise (3,20) et le soja (2,78) restent devant. Un seuil « rang 1 à 3 » a été écarté : les valeurs actuelles ne l'atteignent pas.
- Lent : 8 jours MC de chaîne après la récolte (jus 120 ticks, vin jeune 2 jours, vin rouge 6 jours) et une seule récolte par an.
- Hypothèses vigne : qualité de départ ★2 pour les raisins (`VineyardLogic.GRAPE_QUALITY`, comme le lait ★2 du simulateur), 3 grappes par an en moyenne (2 à 4), année de 28 jours MC (4 saisons de 7 jours).
- Affinage du vin rouge : +1 étoile par 6 jours MC d'affinage (144000 ticks), ajouté à l'écouteur de sortie du module vineyard. Avant ce changement, le vin rouge gardait la qualité de son vin jeune (aucun gain). Une base ★2 donne ★3.
- Riz : cycle de 14 jours MC en été, 2 à 3 panicules par cycle, chaque panicule meulée en 2 riz (recette `rice_millstone`), riz vendu à l'étal à qualité ★1. 0,36 pièces par jour et par case de rizière : le riz est une denrée de base (les plats à base de riz ne sont pas simulés).
- Contenants : le vin jeune consomme une bouteille à la fermentation, comme le vin blanc, et la rend en buvant. Le vin rouge ne consomme pas de bouteille : l'étagère d'affinage n'a pas d'emplacement de contenant, et une recette avec contenant ne s'y lancerait jamais.

**v0.8 (valeurs v0.7 -> v0.8).**

Données et constantes (valeurs v0.7 -> v0.8) :

1. **Consommation de stade supprimée** (`CropDefinitions`, toutes cultures : stade 0/0/0; la récolte garde sa dose).
   Un stade consommait la dose 7 fois par cycle : deux récoltes seulement avant épuisement; une dose à la récolte
   donne 11 récoltes (tomate, maïs). Le mécanisme (`CropGrowth.consumeStage`) reste en place pour les données futures.
2. **Maturation** (`growthDays`) : 6 à 14 j -> 1,55 j (cultures d'été et printemps fraise/orge), 1,7 j (ail, chou),
   1,8 j (laitue, oignon). Les trois contraintes croisées (optimale = D / 1,25 ou 1,5 ou 1,0 selon la saison, serre = D / 0,6,
   défavorable = D / 0,4 x saison) bornent D entre 1,5 et 1,8 : tout le tableau tombe dans les cibles.
3. **Rendements** : tomate 2-4 -> 2-3, concombre 2-4 -> 2-3, fraise 2-4 -> 1-3, laitue 1-2 -> 2-3, chou 1-2 -> 2-3,
   ail 1-2 -> 2-3 : valeur d'une récolte (rendement x prix) ramenée entre 9 et 16 cuivre, meilleure / médiane 3,1 -> 1,7.
4. **Soja, consommation à la récolte** : K 2 -> 1 : la rotation soja / tomate passe de 3 à 16 récoltes (contre 11 en
   monoculture de tomate), car la légumineuse fixe l'azote et la rotation épargne le potassium.
5. **Taux de base d'infection** (`HealthModel.BASE_INFECTION_CHANCE`) : 0,4 % -> 0,25 % : la parcelle diversifiée passe
   de 8 % à 7 % de perte (cible < 8 %).
6. **Facteur de monoculture** (`HealthModel.MONOCULTURE_FACTOR`) : x2 -> x2,5 : compense la baisse du taux de base; la
   monoculture de tomate reste à 18 % (cible 15-30 %). Le facteur environnemental (x4) est inchangé.
7. **Prix de base** (`base_prices.json`) : crème 9 -> 6 (deux crèmes par lait = 16 cuivre, sous le fromage à 21);
   fromage affiné 24 -> 14 (fromage affiné ★3 à 21 cuivre par lait, ratio 2,6 x au lieu de 9 x); olive 9 -> 6 et huile
   14 -> 22 (ratio 1,8 x au lieu de 0,4 x : une huile valait moins que ses olives).
8. **Recette de caillage** (`dairy_cheese_vat_curd`) : 1 lait -> 2 caillés -> 1 caillé : le fromage affiné passe de
   la valeur du lait brut en combinaison avec le prix ci-dessus (la transformation reste de 7 jours MC).
9. **Recette d'huile** (`press_olive_oil`) : 4 olives -> 2 olives pour une huile (prix ci-dessus).
10. **Contrats** (`ContractGenerator.REWARD_PERCENT` 150 -> 130) : récompense = quantité x prix x 1,3, soit +44 % sur
    la vente à l'étal (90 %), dans la cible +30 à 60 % (était +67 %).
11. **Fromage de chèvre** (`base_prices.json`) : 16 -> 11. Sans affinage, il reste sous le fromage affiné (14 de base,
    inchangé), qui se multiplie en plus par la qualité ★.
12. **Famine** (`LivestockLogic.STARVATION_DAMAGE`) : 5 -> 1 perte de santé par cycle de 1200 ticks. Un animal à 100 de
    santé et satiété 0 survit 100 cycles, soit 5 jours MC (cible : au moins 4). Une perte de 1,25 par cycle donnerait
    exactement 4 jours, mais la santé est un entier : le choix retenu est 5. Tests : `LivestockLogicTest`
    (`starvingAnimalSurvivesAtLeastFourDays`), GameTest `unfedAnimalLosesHealth` (santé 99 après un cycle).

Tests adaptés (valeurs figées par l'ancien équilibrage, à l'origine de chaque changement) :

- `CropDefinitionsTest` : tomate (consommation de stade, maturation 1,55 j, rendement 2-3) et ail (1,7 j, plus « la plus
  lente » ne tient plus : test renommé et borné entre 1,5 et 1,8 j).
- `HealthModelTest` : taux de base 0,25 %, environnement x4 sur 0,25 % = 1 %, monoculture x2,5.
- `MarketLogicTest` : récompense x1,3 au lieu de x1,5.
- GameTests : `DairyGameTests` (caillé attendu : 1 au lieu de 2, deux tests), `MillingGameTests` (deux olives dans la presse
  au lieu de quatre), `TVIntegrationGameTests.growthDepletesSoil` remplacé par `harvestDepletesSoil` (la consommation
  n'a plus lieu à la croissance mais à la récolte : même vérification, au moment où la règle l'impose).

Points relevés mais non modifiés (hors cibles de cette version) :

- **Lait et alimentation** : le mod ne change que la qualité du lait selon l'alimentation, jamais la quantité
  (`ProductionLogic`, traite vanilla). Une vache bien nourrie et une vache mal nourrie donnent le même nombre de seaux.
- **Famine** : traitée (point 12). À satiété nulle, un animal meurt désormais en 5 jours MC au lieu d'un, ce qui laisse
  le temps de réagir; le relevé (e) « mal nourries » meurt toujours, mais plus tard.
- **Maturation et lumière** : le simulateur suppose la lumière suffisante toute la journée, comme la formule de
  `growthDays`; la nuit (lumière insuffisante) n'est pas simulée.
- **Source des configs** : `SeasonConfig` et `SoilConfig` lisent des specs NeoForge absentes des tests unitaires;
  leurs défauts sont recopiés dans le simulateur (à garder synchronisés).
- **Fromage affiné** : la capacité des étagères (pipeline de 7 jours par vache) n'est pas simulée.
<!-- CHANGES:END -->
