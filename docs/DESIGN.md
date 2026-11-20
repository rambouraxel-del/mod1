# Terres Vivantes — Design v0.1 « Fondations »

Référence de conception pour les agents développeurs. Ce document décrit **ce qui doit être fait en v0.1**. La suite est dans `docs/ROADMAP.md`.

Convention : `(proposé)` marque une valeur ou un choix complété par l'auteur du design, non fixé par la spécification d'origine. Les décisions de l'orchestrateur sont en section 13.

---

## 1. Principe et vue d'ensemble

Simulation crédible mais accessible. Tous les systèmes interagissent :

```
saison → climat → croissance
sol → croissance
cultures → sol (consommation N/P/K/organique)
animaux → fumier → compost / sol
```

Cible : Minecraft 1.21.1, NeoForge (`neo_version` 21.1.267), modid `terresvivantes`, package racine `fr.terresvivantes`.

### 1.1 Architecture de code (contrainte)

Chaque module suit la même structure :

| Élément | Rôle |
|---|---|
| `<module>/<Module>Module.java` | Enregistrement NeoForge : registres, événements, commandes, payloads |
| `<module>/logic/<Module>Logic.java` | **Logique pure Java**, sans dépendance à Minecraft ni NeoForge, testable en JUnit |

Règle : toute formule de ce document (saison, température, Liebig, modèle de croissance, remplissage du compost…) est implémentée dans `logic/` et couverte par des tests unitaires. Les classes `*Module` ne font que brancher la logique sur le jeu.

Modules : `core` (config, réseau, registres, onglet créatif), `season`, `climate`, `soil`, `crop`, `compost`, `food`, `tools`.

---

## 2. Module `season`

### 2.1 Calendrier

- Année = 4 saisons × `daysPerSeason` jours (config, défaut **7**). Ordre : printemps, été, automne, hiver.
- Index de jour absolu :
  `dayIndex = floor(overworld.dayTime / 24000) + offset`
  où `offset` est une valeur persistée dans une `SavedData` (monde).
- Saison : `seasonIndex = (dayIndex / daysPerSeason) % 4` (division entière, modulo sur l'année).
- Jour dans la saison (1-based) : `dayInSeason = (dayIndex % daysPerSeason) + 1`.
- Si `doDaylightCycle` est désactivé, `dayTime` ne change pas et la saison est figée (à documenter en jeu).
- Seul l'**overworld** porte la saison. Les autres dimensions n'ont ni saison ni climat (proposé).

### 2.2 Commandes (opérateur niveau 2)

| Commande | Effet |
|---|---|
| `/tv season set <spring\|summer\|autumn\|winter> [day]` | Positionne la saison. `day` optionnel (1 à `daysPerSeason`, défaut 1). Recalcule `offset` : `offset = cible − floor(dayTime/24000)`. |
| `/tv season info` | Affiche saison, jour dans la saison, `dayIndex`. |

### 2.3 Synchronisation client

- Un **payload réseau** envoie au client : saison, jour dans la saison et `daysPerSeason`.
- Envoi : à la connexion du joueur, à chaque changement de jour, et après `/tv season set`.
- Les valeurs de gameplay (`daysPerSeason`, multiplicateurs) sont lues **côté serveur** ; le client ne calcule pas la croissance, il n'affiche que ce qu'il reçoit.

### 2.4 HUD

- Optionnel, activé par défaut (config `hud.enabled`, côté client).
- Position : haut à gauche.
- Texte : `Printemps · Jour 3/7`.

### 2.5 Multiplicateurs de croissance saisonniers

| Saison | Multiplicateur |
|---|---|
| Printemps | 1.0 |
| Été | 1.2 |
| Automne | 0.8 |
| Hiver | 0.15 (v0.2 ; 0.0 en v0.1) |

S'applique aux cultures vanilla et aux cultures du mod. **En v0.2, l'hiver ralentit fortement la croissance en extérieur (0.15)**. Sous serre en hiver : multiplicateur 0.6 et affinité évaluée comme au printemps (voir 6.2).

---

## 3. Module `climate`

### 3.1 Température locale

Formule, en °C :

```
T = baseBiome + offsetSaison + offsetAltitude + offsetJourNuit
```

| Terme | Valeur |
|---|---|
| `baseBiome` | température de base vanilla du biome × 25 (ex. plaines 0.8 → 20 °C ; désert 2.0 → 50 °C ; neige −0.5 → −12.5 °C). Source : `Biome#getBaseTemperature()`. |
| `offsetSaison` | Printemps 0, Été +8, Automne −2, Hiver −12 |
| `offsetAltitude` | `−0.06 × max(0, y − 64)`, soit −0.6 °C par tranche de 10 blocs au-dessus de y=64. Pas de bonus sous y=64. |
| `offsetJourNuit` | −4 °C la nuit (nuit = `dayTime` ∈ [13000, 23000[, plage vanilla). 0 le jour. |

### 3.2 Plage de tolérance des cultures

Chaque culture a une plage `[tempMin, tempMax]`. Facteur appliqué au facteur de croissance `f` (section 6.2) :

| Température | Facteur |
|---|---|
| Dans la plage | ×1.0 |
| Hors plage, à moins de 8 °C | ×0.3 |
| Hors plage de plus de 8 °C (au-delà de `tempMin − 8` ou `tempMax + 8`) | ×0 |

---

## 4. Module `soil`

### 4.1 Données

- **Data Attachment par chunk** : `Map<long, SoilData>`, clé = `BlockPos.asLong()`.
- Données présentes **uniquement pour les terres labourées** (`farmland`).
- `SoilData` : `N`, `P`, `K`, `organic` (0–100 chacun). L'humidité n'y est **pas** stockée (voir 4.4).
- **Initialisation paresseuse** (à la première lecture) :

| Biome | N | P | K | Organique |
|---|---|---|---|---|
| Autres biomes (défaut) | 60 | 50 | 50 | 30 |
| Désert | 30 | 30 | 30 | 10 |
| Jungle | 60 | 50 | 50 | 80 |

- Retour de la terre labourée en `dirt` (piétinement, pelle, etc.) : **suppression** de l'entrée `SoilData`.

### 4.2 Facteur de Liebig

Facteur de fertilité calculé sur le **nutriment le plus faible** parmi N, P, K :

| min(N, P, K) | Facteur |
|---|---|
| < 20 | 0.25 |
| 20 à < 40 | 0.6 |
| ≥ 40 | 1.0 |

Bonus : si `organic > 60`, facteur × **1.1**.

### 4.3 Consommation

- Chaque **stade de croissance** consomme N/P/K selon le profil de la culture (voir 6.3).
- La **récolte** consomme davantage (valeur par culture, voir 6.3).
- Les **légumineuses** (soja) ajoutent **+N** à chaque stade (valeur par culture).
- Multiplicateur global : config `soilConsumptionMultiplier` (défaut 1.0). Toutes les consommations sont multipliées par ce facteur. Bornes des valeurs : 0–100.

### 4.4 Humidité (propriété vanilla)

- L'humidité n'est **pas** stockée dans `SoilData`. On utilise la propriété vanilla `MOISTURE` (0–7) du bloc `farmland`.
- `MOISTURE = 7` : terre humide. `MOISTURE = 0` : terre sèche.
- La sonde (`soil_probe`) affiche cette valeur.
- Effet en v0.1 : terre sèche (`MOISTURE = 0`) → facteur de sol ×0.5 (inclus dans `facteurSol`, section 6.2).
- L'arrosoir (`watering_can`) règle `MOISTURE = 7` (section 9).

### 4.5 Engrais

Clic droit sur une terre labourée : l'effet s'applique au **bloc ciblé seul** (pas de zone 3×3 en v0.1). Valeurs ajoutées, bornées à 100 :

| Engrais | Effets |
|---|---|
| `compost` | +15 N, +15 P, +15 K, +20 organique |
| `manure` | +25 N, +10 organique |
| `bone_meal` (vanilla) | effet vanilla conservé, **plus** +15 P |
| `wood_ash` | +20 K |

---

## 5. Intégration vanilla

- Cultures vanilla concernées : blé, carotte, pomme de terre, betterave, tiges de citrouille et de pastèque.
- Hook : `CropGrowEvent.Pre`.
- Facteur appliqué : `f = saison × affinité × climat × sol` (section 6.2). Pour les cultures vanilla, l'affinité vaut 1.0 (proposé : pas de table d'affinité vanilla).
- La logique d'application est celle de `GrowthModel` (section 6.2), partagée avec les cultures du mod.
- Activable/désactivable par config `vanillaIntegration` (défaut **true**). Si désactivé, les cultures vanilla gardent leur comportement vanilla.

---

## 6. Module `crop`

### 6.1 Bloc et croissance

- Bloc générique `TVCropBlock`, `age` ∈ [0, 7], 4 visuels `stage0`…`stage3` (proposé : âge 0–1 → stage0, 2–3 → stage1, 4–5 → stage2, 6–7 → stage3).
- Chaque culture du mod est une instance de `CropDefinition` (enum ou registre Java) :

```
CropDefinition {
  id
  seasons: optimal[], possible[], defavorable[]
  tempMin, tempMax
  consommation: N, P, K  (par stade)
  consommationRecolte: N, P, K  (proposé)
  vitesse  (durée de maturation en jours MC, à f = 1)
  produit, quantiteProduit (min–max)
  graines: quantite (1–2)
  legumineuse: bool
}
```

### 6.2 Facteur de croissance et `GrowthModel`

```
f = multSaison × affinite × facteurClimat × facteurSol
```

- `affinite` selon la saison de la culture : optimale ×1.25, possible ×1.0, défavorable ×0.4, impossible ×0.
- `multSaison` : section 2.5. Hiver extérieur = 0.15 (v0.2). La plantation reste autorisée en hiver.
- Sous serre en hiver (v0.2) : `multSaison` = 0.6 et l'affinité est évaluée comme au **printemps** (`CropLogic.affinitySeason`).
- `facteurClimat` : section 3.2.
- `facteurSol` = facteur de Liebig (4.2) × bonus organique (×1.1 si organique > 60) × ×0.5 si terre sèche (`MOISTURE = 0`, 4.4).
- Bornes théoriques de `f` : 0 à environ 1.65 (1.2 × 1.25 × 1.0 × 1.1).

**Application** (classe pure `crop/logic/GrowthModel`, partagée par les cultures du mod et les cultures vanilla) :

| Valeur de `f` | Effet d'une tentative de croissance |
|---|---|
| `f < 1` | Annulée (DENY) avec probabilité `1 − f` ; sinon croissance normale |
| `f = 1` | Croissance normale |
| `f > 1` | Croissance normale, plus une chance `min(f − 1, 1)` d'un **stade supplémentaire** |

- Cultures du mod : `GrowthModel` est appelé à chaque randomTick de `TVCropBlock`.
- Cultures vanilla : `GrowthModel` est appelé dans `CropGrowEvent.Pre` (DENY si annulé). Le stade supplémentaire est appliqué par le mod (incrément d'âge).
- **Flétrissement (`withered_crop`) : v0.2**, voir section 14. En v0.1, une culture à `f = 0` ne faisait que stagner.

### 6.3 Les 12 cultures (proposé)

Valeurs complétées. Saison : O = optimal, P = possible, D = défavorable. Hiver : **défavorable pour l'ail, le chou et l'oignon (v0.2)**, impossible pour les autres. Consommation par stade, en N/P/K. Vitesse en jours MC (à f = 1). Rendement : quantité min–max du produit, plus 1–2 graines à la récolte.

| Culture (id) | O | P | D | Temp (°C) | Conso N/P/K (stade) | Récolte N/P/K | Vitesse (j) | Produit (qté) |
|---|---|---|---|---|---|---|---|---|
| `tomato` | Été | Printemps | Automne | 12–32 | 2/1/2 | 3/2/3 | 6 | `tomato` (2–4) |
| `corn` | Été | Printemps | Automne | 15–35 | 3/1/1 | 4/2/2 | 8 | `corn` (1–3) |
| `soybean` (légumineuse, +N/stade) | Été | Printemps | Automne | 15–30 | 0/1/1 (+2 N) | 1/2/2 | 6 | `soybean` (2–4) |
| `lettuce` | Printemps | Automne | Été | 5–24 | 2/1/1 | 2/1/1 | 3 | `lettuce` (1–2) |
| `onion` | Printemps | Automne | Été | 8–28 | 1/2/1 | 2/3/2 | 9 | `onion` (1–3) |
| `garlic` | Automne | Printemps | Été | 2–24 | 1/2/1 | 2/3/2 | 14 (plantation lente) | `garlic` (1–2) |
| `bell_pepper` | Été | Printemps | Automne | 16–30 | 2/1/2 | 3/2/3 | 7 | `bell_pepper` (1–3) |
| `eggplant` | Été | Printemps | Automne | 18–32 | 2/1/2 | 3/2/3 | 8 | `eggplant` (1–2) |
| `strawberry` | Printemps | Été | Automne | 8–26 | 1/2/2 | 2/3/3 | 5 | `strawberry` (2–4) |
| `barley` | Printemps | Été | Automne | 5–25 | 2/1/1 | 2/2/2 | 7 | `barley` (2–4) |
| `cabbage` | Automne | Printemps | Été | 4–22 | 2/1/1 | 3/2/2 | 8 | `cabbage` (1–2) |
| `cucumber` | Été | Printemps | Automne | 16–30 | 2/1/2 | 3/2/3 | 5 | `cucumber` (2–4) |

Chaque culture expose : `<id>_seeds` (graine) et `<id>` (produit). Les noms d'items suivent l'`id` (ex. `tomato_seeds`, `tomato`).

### 6.4 Interactions

- **Plantation** : graine sur terre labourée, avec la règle vanilla de la lumière et du support.
- **Récolte à maturité** : produit (quantité selon 6.3) + graines (1–2).
- **Clic droit sur une culture mature** : récolte **et replantation** (qualité de vie, sans outil).
- Les cultures immatures n'ont pas de clic droit de récolte.

### 6.5 Sources de graines

- Herbes hautes (loot modifier) : 8 % de chance, une graine aléatoire parmi les 12.
- Marchand ambulant (vanilla wandering trader) : offres de graines (proposé : une graine par offre, prix bas).

---

## 7. Module `compost`

### 7.1 Bac à compost `compost_bin` (bois)

- 8 niveaux de remplissage.
- Matières acceptées et valeur de remplissage (proposé) :

| Matière | Niveaux |
|---|---|
| Produits de culture (`<id>`) | 1 |
| Graines (`<id>_seeds`) | 1 |
| Feuilles (tag `leaves`) | 1 |
| Herbe (`grass`, `tall_grass`) | 1 |
| `manure` | 2 |

- Quand le bac est plein : **maturation d'environ 1 jour MC** → état `ready`.
- Maturation par `randomTick`, conforme à la spec (décision 5).
- État `ready` : clic droit → **2 à 3 compost**, puis le bac se vide.

### 7.2 Fumier

- Vaches, cochons, moutons, chevaux lâchent `manure` de temps en temps.
- Chance : config `manureChance` (défaut proposé : environ 1 fumier par jour MC et par animal).

### 7.3 Cendre

- `wood_ash` : recette sans forme, **1 `charcoal` → 2 `wood_ash`**.

---

## 8. Module `food`

### 8.1 Plats (10)

Nutrition et saturation (proposé), à régler en équilibrage.

| Plat (id) | Ingrédients (proposé) | Mise en œuvre | Nutrition | Saturation |
|---|---|---|---|---|
| `tomato_soup` | 2 `tomato` + bol | Établi | 6 | 0.6 |
| `garden_salad` | `lettuce` + `tomato` + `cucumber` + bol | Établi | 7 | 0.7 |
| `ratatouille` | `eggplant` + `bell_pepper` + `tomato` + `onion` + bol | Établi | 10 | 0.8 |
| `corn_bread` | 2 `corn` + 1 `wheat` | Établi | 6 | 0.6 |
| `garlic_bread` | `bread` + `garlic` | Établi | 7 | 0.7 |
| `stuffed_pepper` | `bell_pepper` + `barley` + `onion` | Établi | 8 | 0.7 |
| `strawberry_jam` | 4 `strawberry` + `sugar` + bol | Établi | 4 | 0.4 |
| `vegetable_stew` | `carrot` + `potato` + `cabbage` + `onion` + bol | Établi | 8 | 0.8 |
| `barley_bread` | 3 `barley` | Établi | 5 | 0.6 |
| `grilled_corn` | `corn` | **Four** (cuisson) | 5 | 0.6 |

### 8.2 Produits crus

Tous les produits crus sont comestibles, valeurs faibles (proposé : nutrition 1–2, saturation 0.1–0.2). Y compris `garlic` et `onion`.

---

## 9. Module `tools`

| Outil | Recette (proposé) | Usage | Durabilité / utilisations |
|---|---|---|---|
| `soil_probe` | 1 bâton + 1 `iron_nugget` | Clic droit sur terre labourée : affiche dans le chat N, P, K, organique et l'humidité (`MOISTURE`) | — |
| `almanac` | 1 `book` + 1 `wheat` | Clic droit : saison, jour, température locale, cultures **optimales à planter maintenant** (affinité optimale ET température dans la plage) | — |
| `sickle` | 2 `iron_ingot` + 1 bâton | Clic droit sur une culture mature : récolte **et replante** la zone 3×3 | 250 |
| `watering_can` | 3 `iron_ingot` + 1 `bucket` (proposé) | Remplir : clic sur eau. Utiliser : règle `MOISTURE = 7` sur la terre labourée 3×3 | 8 utilisations |

---

## 10. Identité visuelle

- Palette chaude et naturelle (ocres, verts feuillage, terres).
- Pixel art 16×16, cohérent avec les textures vanilla.
- Particules de croissance (ex. particules `HAPPY_VILLAGER`) lors d'un stade gagné.
- Sons vanilla adaptés (ex. sons de plantation et de récolte dérivés de `block.grass`, `block.crop`).

---

## 11. Configuration (`TVConfig`, type COMMON)

| Clé | Défaut | Description |
|---|---|---|
| `daysPerSeason` | 7 | Nombre de jours par saison |
| `seasonMultipliers.spring` | 1.0 | Multiplicateur de croissance, printemps |
| `seasonMultipliers.summer` | 1.2 | Idem, été |
| `seasonMultipliers.autumn` | 0.8 | Idem, automne |
| `seasonMultipliers.winter` | 0.15 (v0.2 ; 0.0 en v0.1) | Idem, hiver extérieur |
| `greenhouseWinterMultiplier` | 0.6 | Multiplicateur d'hiver sous serre (season) |
| `hud.enabled` | true | Affiche le HUD saison (côté client) |
| `manureChance` | proposé : 1/24000 par tick et par animal (~1 par jour MC) | Chance de lâcher du fumier |
| `soilConsumptionMultiplier` | 1.0 | Multiplicateur global de consommation N/P/K/organique |
| `vanillaIntegration` | true | Active l'intégration saison/climat/sol sur les cultures vanilla |

Note (proposé) : `daysPerSeason` et les multiplicateurs sont **lus côté serveur**. Un `COMMON` n'est pas synchronisé par NeoForge ; le client ne reçoit que ce qui est affiché (payload saison).

---

## 12. Réseau et persistance

| Élément | Mécanisme |
|---|---|
| Saison (offset) | `SavedData` au niveau du monde |
| Sol (N/P/K/organique) | Data Attachment par chunk |
| Humidité du sol | Propriété vanilla `MOISTURE` du `farmland` (pas de donnée propre au mod) |
| Synchronisation saison | Payload réseau : à la connexion, au changement de jour, après `/tv season set` |
| Configuration | `ModConfigSpec` COMMON |

---

## 13. Décisions

Décisions de l'orchestrateur, intégrées dans le document.

| # | Sujet | Décision |
|---|---|---|
| 1 | Sols de biome | Désert : N/P/K = 30, organique = 10. Jungle : organique = 80. Autres biomes : défauts 60/50/50/30. (4.1) |
| 2 | Engrais | Effet sur le bloc ciblé seul, pas de 3×3 en v0.1. (4.5) |
| 3 | Humidité | Non stockée dans `SoilData` ; propriété vanilla `MOISTURE` du `farmland`, affichée par la sonde. Terre sèche (`MOISTURE = 0`) : croissance ×0.5. L'arrosoir règle `MOISTURE = 7`. (4.4, 6.2, 9) |
| 4 | Flétrissement (`withered_crop`) | Livré en v0.2 (section 14). (6.2) |
| 5 | Compost | Maturation par `randomTick`. (7.1) |
| 6 | Altitude et nuit | Altitude : `−0.06 × max(0, y − 64)`. Nuit : `dayTime` ∈ [13000, 23000[. (3.1) |
| 8 | Correction v0.2 : hiver | Multiplicateur d'hiver extérieur 0.15 (au lieu de 0). Sous serre : 0.6 et affinité du printemps. Ail, chou, oignon défavorables en hiver. (2.5, 6.2, 6.3) |
| 7 | Application des multiplicateurs | `f = saison × affinité × climat × sol`. Si `f < 1` : tentative annulée avec probabilité `1 − f`. Si `f > 1` : chance `min(f − 1, 1)` d'un stade supplémentaire. Cultures du mod (randomTick) et vanilla (`CropGrowEvent.Pre`) partagent la classe pure `crop/logic/GrowthModel`. (6.2, 5) |

---

## 14. Module `health` (v0.2)

### 14.1 Santé d'une culture

Donnée par position de culture, en Data Attachment de chunk (`HealthChunkData`), retirée quand le bloc n'est plus une culture.

| Champ | Rôle |
|---|---|
| `stress` | +1 à chaque tentative de croissance à facteur 0 (conditions impossibles : affinité impossible, gel); −1 à chaque croissance réussie, sans passer sous 0. |
| `disease` | `NONE`, `MILDEW` (oïdium) ou `RUST` (rouille). |
| `diseaseTicks` | Tentatives de croissance subies pendant la maladie. |
| `immuneUntilDay` | Dernier jour (index absolu) d'immunité : fin de la saison du traitement. |

### 14.2 Flétrissement

- Stress ≥ 4 : la culture devient `withered_crop` (bloc en croix sur terre labourée, ne pousse pas).
- Drops : 1 bâton, et 25 % de chance de graine de blé. Le bâton est à signaler à l'intégrateur pour le tag compostable.
- Activable : `terresvivantes-health.toml`, clé `witheringEnabled` (défaut vrai).

### 14.3 Maladies

Sensibilités :

| Maladie | Cultures |
|---|---|
| Oïdium (`MILDEW`) | tomate, concombre, fraise, laitue, poivron, aubergine, pomme de terre (vanilla) |
| Rouille (`RUST`) | orge, maïs, soja, blé (vanilla) |

Chance d'infection par tentative : **0.4 %** × facteurs (multiplicateurs, pas d'addition) :

| Facteur | Condition | Multiplicateur |
|---|---|---|
| Oïdium, humidité | sol saturé (`MOISTURE` 7) et (pluie ou température > 22 °C) | ×4 |
| Rouille, climat | pluie et 12 à 24 °C | ×4 |
| Monoculture | même culture récoltée au moins 2 fois de suite (`SoilApi.getRepeatCount` ≥ 2) | ×2 |
| Densité | au moins 6 cultures identiques dans la zone 5×5 | ×1.5 |
| Immunité | culture immunisée | 0 |

- Contagion : une culture malade infecte chaque voisin identique du rayon 1 avec 10 % par tentative (immunité respectée).
- Effets : croissance ×0.3 et rendement ×0.5 (modifiers `CropGrowthHooks`).
- Après 6 tentatives malade : flétrissement.
- Visuel : particules `spore_mildew` (blanc-gris) et `spore_rust` (orangé), à chaque tentative d'une culture malade.
- Config : `diseasesEnabled` (défaut vrai).

### 14.4 Traitements

| Outil | Recette | Effet |
|---|---|---|
| Fongicide (`fungicide`) | cuivre + fiole d'eau + poudre d'os → 2 | Clic droit sur une culture : guérit cette culture, immunité jusqu'à la fin de la saison. |
| Pulvérisateur (`sprayer`) | fer, cuivre, bâton | Clic droit : traite la culture et sa zone 3×3. 16 charges (durabilité). Vide : tenir du fongicide en main secondaire et cliquer sur une culture pour recharger (un fongicide). |

La sonde de sol, sur une culture, affiche le stress, la maladie et l'immunité.

## 15. Module `orchard` (v0.2) : vergers

### 15.1 Espèces

Six espèces (`FruitTreeDefinitions`) : pomme (vanille), cerise, orange, olive, noix, pêche. Chacune a une plage de températures, une floraison, des saisons de maturité et un rendement.

| Espèce | Fruit | Mûrit | Biomes sauvages |
|---|---|---|---|
| Pommier | `minecraft:apple` | automne | plaines, forêt |
| Cerisier | `cherry` | printemps, été | plaines, forêt |
| Oranger | `orange` | hiver | savane, jungle |
| Olivier | `olive` | automne | savane |
| Noyer | `walnut` | automne | plaines, forêt |
| Pêcher | `peach` | été | plaines |

### 15.2 Feuillage à quatre stades

- Sapling : pousse en arbre par randomTick, avec `saplingChance` (défaut 0.1) multipliée par le facteur saisonnier, en lumière suffisante.
- Feuillage (`<espèce>_leaves`, stades 0 à 3) : passe au stade suivant par randomTick avec `transitionChance` (défaut 0.25), si la saison et la température le permettent.
- Le passage du stade 1 au stade 2 noue les fruits. `OrchardApi.registerFruitSetModifier` multiplie cette chance (pollinisation prévue en v0.3).
- Feuillage non persistant à la distance 7 du tronc : il disparaît, comme le feuillage vanille.
- Fruits : comestibles (nutrition 1 à 3), dans le tag compostable. Une partie tombe au sol (`FALLEN_FRUIT_CHANCE` 0.5).

### 15.3 Arbres sauvages, marchand, plats

- Arbres sauvages : une `WildFruitTreeFeature` par espèce, dans les biomes du tableau 15.1 (`neoforge:add_features`).
- Marchand ambulant : un sapling de chaque espèce dans les offres générales.
- Plats (5) : tarte aux cerises (`cherry_pie`), jus d'orange (`orange_juice`), salade de fruits (`fruit_salad`), pain aux noix (`walnut_bread`), confiture de pêche (`peach_jam`).

Configuration : `terresvivantes-orchard.toml` (`transitionChance`, `saplingChance`).

## 16. Irrigation (module `water`, v0.2)

### 16.1 Arroseurs

| Bloc | Rayon | Matière |
|---|---|---|
| `copper_sprinkler` | 2 | cuivre |
| `iron_sprinkler` | 3 | fer |

- Un arroseur est actif s'il repose sur de l'eau ou s'il est alimenté par un tonneau voisin qui a de l'eau. Il met alors à 7 l'humidité des terres labourées de son carré (même niveau et niveau inférieur).
- Une culture dont la terre est dans la zone d'un arroseur actif pousse ×1.1.
- Cycle de 100 ticks (`SPRINKLER_TICKS`). Le tonneau ne fournit de l'eau qu'à un cycle sur quatre.

### 16.2 Tonneau de pluie

- `rain_barrel` : niveau 0 à 8, comparateur = niveau. Se remplit par randomTick sous la pluie à ciel ouvert, au seau d'eau (niveau 8), à la fiole d'eau (un niveau).
- L'arrosoir vide se remplit au tonneau (un niveau). Les arroseurs voisins y puisent.

### 16.3 Sécheresse estivale

- En été, sans pluie et sans arroseur actif, chaque tentative de croissance d'une culture (mod ou vanilla) retire 1 d'humidité à la terre, avec la chance `droughtChance` (défaut 0.15). `droughtEnabled` coupe l'effet.
- Effet voulu : l'arrosage devient une décision en été, sans punir les cultures sous la pluie ou sous arroseur.

Configuration : `terresvivantes-water.toml`.

## 17. Biodiversité (v0.3 : pollinisation, indice, modules à venir)

Implémenté en v0.3 : le package `pollination` (score, indice, caches, points d'extension), les effets sur les
cultures, les vergers et la santé (pucerons, réduction par biodiversité), l'almanach, et les modules `flora`,
`insects`, `apiculture`. Les écarts par rapport au plan ci-dessous sont listés en 17.4.

### 17.1 Pollinisation

`PollinationApi.score(ServerLevel, BlockPos)` ∈ [0, 1] : somme bornée puis plafond à 1.

| Composante | Rayon | Contribution | Plafond |
|---|---|---|---|
| Abeilles vanilla (`Bee`) | 16 | 0.1 chacune | 0.4 (4 abeilles) |
| Ruches et nids vanilla (`BeehiveBlock`) | 24 | 0.2 chacun | 0.4 (2 ruches) |
| Types de fleurs (tag `minecraft:flowers`) | 8 | 0.05 par type | 0.3 |
| Sources enregistrées (apiaires, papillons du mod) | variable | somme | bornée par la source |

- Cache : `ChunkScoreCache`, clé (dimension, chunk, tranche verticale de 16 blocs), valable 600 ticks. Le score est
  calculé au centre de la tranche : toutes les positions de la tranche partagent la valeur pendant ce délai.
  `scoreUncached` lit sans cache (tests, diagnostic).
- Effets, appliqués par `PollinationModule` :
  - cultures dont `CropDefinition.needsPollination` (concombre, fraise, aubergine, poivron) : rendement
    `YieldModifier` ×(0.6 + 0.7 × score), soit ×0.6 sans pollinisateur, ×1.3 au max. Les autres cultures : ×1.
  - arbres fruitiers : fructification `OrchardApi` ×(0.5 + score); rendement ×(0.6 + 0.7 × score).
- Pumpkin et melon vanilla : non couverts. Leurs fruits poussent sur la tige (`StemBlock`), hors du contrat
  `CropGrowthHooks`; à traiter avec le module qui touche leur croissance.
- Papillons du mod : pas encore d'entité. Le module insects les comptera via `registerPollinatorSource`, en
  appliquant lui-même le cap de 0.2 (0.05 par papillon dans le rayon 12).

### 17.2 Indice de biodiversité

`BiodiversityApi.index(ServerLevel, BlockPos)` ∈ [0, 1] : fleurs (rayon 8, 0.05 par type, plafond 0.3), feuillage
fruitier (rayon 6, 0.005 par feuille, plafond 0.25), abeilles (rayon 16, 0.05 chacune, plafond 0.2) et habitats
enregistrés (haies, via `registerHabitatSource`). Total plafonné à 1. Même cache que la pollinisation.

- Santé : chance d'infection par maladie ×(1 − 0.5 × indice). Les pucerons suivront la même règle (module insects).
- Almanach : affiche la pollinisation et la biodiversité locales, en pourcentage.

### 17.3 Ravageur : pucerons (état `APHIDS` de `Disease`)

- Croissance ×0.5, rendement ×0.8, pas de flétrissement. Se propage aux voisines identiques comme une maladie.
- Apparition : `HealthApi.infest` (culture saine uniquement). Retrait : `HealthApi.cure`. Aucune infection spontanée.
- `HealthApi.registerPestSuppressor` : multiplicateurs de la chance d'apparition, lus par `HealthApi.pestSuppression`.
  Le module insects les utilisera (coccinelles à proximité).
- Le fongicide retire aussi les pucerons (il remet la santé à zéro); c'est un choix temporaire.

### 17.4 Modules livrés et écarts

- **flora** : fleurs sauvages clover, lavender, chamomile, borage (pots, teintures, worldgen par
  biome, propagation lente `FloraSpread`); haie d'aubépine `hawthorn_hedge` (mur, fleurit au printemps, habitat).
- **insects** : papillons (4 variantes), coccinelle (nettoie les pucerons dans un rayon de 2), pucerons (`infest`,
  suppression par `registerPestSuppressor`), lucioles (particules nocturnes). Filet à papillons, bocaux
  `butterfly_jar` et `ladybug_jar`. Écart : pas de relâcher depuis les bocaux en v0.3.
- **apiculture** : `apiary` (4 cadres, BlockEntity, population 0 à 100), `honey_extractor`, `bee_smoker`, cire.
  Miel typé par `ApicultureApi.nectarOf` : sources enregistrées (trèfle et lavande, déclarées dans `TerresVivantes`),
  feuillage fruitier en fleur (orchard), puis tag `minecraft:flowers` (miel sauvage). Écart : pas d'interface GUI de
  l'apiaire; la population se règle par la BlockEntity.

### 17.5 Critères de validation (v0.3)

- [x] Score croissant avec des fleurs variées et une abeille (GameTest `PollinationGameTests`).
- [x] Concombre sans pollinisateur : rendement moyen inférieur à celui avec pollinisateurs (test pur).
- [x] Maladie : chance réduite par la biodiversité (test pur).
- [x] Coccinelle qui nettoie une culture infestée (GameTest `InsectsGameTests`, délai 1600 ticks).
- [x] Miel de trèfle obtenu avec de vraies fleurs de trèfle (GameTest `ApicultureGameTests`).
- [ ] Abeilles sauvages qui butinent dans l'apiaire : non vérifié en jeu.

## 18. Élevage (v0.4 : module `livestock`, modules à venir)

Implémenté en v0.4 : le génome et l'état vital des animaux d'élevage, l'héritage, les besoins quotidiens, la
reproduction saisonnière, `LivestockApi`, le registre d'élevage, et la ponte modulée. Les modules `husbandry`,
`vet` et `duck` sont enregistrés comme squelettes vides. Les écarts sont listés en 18.7.

### 18.1 Animaux et génome

- Animaux concernés : tag d'entités `terresvivantes:livestock` (`data/terresvivantes/tags/entity_type/livestock.json`) :
  vache, vache à champignons, cochon, mouton, poule, chèvre, lapin, et `terresvivantes:duck` (optionnel, `required: false`).
- Génome `Genome` : productivité, taille, résistance, fertilité, entiers de 0 à 10. Data Attachment d'entité `genome`,
  sérialisé, sans `copyOnDeath`.
- Génome initial : chaque trait tiré dans [4, 6]. Attribué à l'arrivée d'un animal sans génome (`EntityJoinLevelEvent`, serveur).
- Hérédité (`BabyEntitySpawnEvent`) : moyenne des deux parents par trait (arrondi tiré au hasard si impair), puis
  mutation de ±1 avec la chance de 15 %, bornée à [0, 10]. L'enfant reste dans [min − 1, max + 1].
- `GenomeLogic` (pure) porte ces règles; `LivestockCodecs` (package `livestock`) porte la persistance.

### 18.2 Besoins

- Attachment `livestock_state` : `fed` (0 à 100), `health` (0 à 100), `ailment` (`NONE`, `SICK`, `PARASITES`),
  `lastFedDay` (jour de saison du dernier apport). Valeur initiale : satiété 50, santé 100, sans maladie.
- Cycle toutes les 1200 ticks par animal (`EntityTickEvent.Post`, décalé par `id % 1200` pour étaler la charge).
- Par cycle :
  - consommation : −8 de satiété (−12 en hiver);
  - pâturage : +10 si l'espèce broute (vache, vache à champignons, mouton, chèvre, lapin), hors hiver, et si le bloc
    sous l'animal est un bloc d'herbe (`grass_block`); le jour du repas est alors mis à jour;
  - santé : satiété à 0 donne −5; satiété au-dessus de 60 donne +2.
- Sources de nourriture tierces : `LivestockApi.registerFeedSource` (ajout de satiété à chaque cycle, pour les mangeoires).
- Pas de mort par manque de nourriture en v0.4 : la santé peut tomber à 0 sans effet (réservé au module vet).

### 18.3 Reproduction

- `PlayerInteractEvent.EntityInteract` : la nourriture de reproduction est refusée (annulation, message dans la barre
  d'action) si la satiété est inférieure à 40, ou, si `LivestockConfig.breedingSeasonsOnly` est vrai (défaut), hors
  printemps et été.
- Jumeaux : chance de `fertilité moyenne × 3 %` (plafonnée à 1) qu'un second bébé apparaisse, avec son génome hérité.

### 18.4 Productions et `LivestockApi`

- `productionMultiplier(animal)` = (0.6 + 0.08 × productivité) × (1.1 si satiété > 60; 0.5 si < 20; 1 sinon)
  × (0.5 si malade; 1 sinon), multiplié par les modificateurs enregistrés (`registerProductionModifier`).
- Effet déjà appliqué : la ponte des poules. Chaque cycle de 1200 ticks, `eggTime` est décalé de
  (multiplicateur − 1) × 1200 ticks, arrondi probabiliste. La cadence de ponte vaut donc le multiplicateur.
- Non appliqué en v0.4 : la laine (NeoForge 21.1 n'expose pas d'événement de tonte; à traiter avec un hook dédié)
  et le lait (réservé au module `duck`, puis au module productions).

### 18.5 Registre d'élevage

- Item `livestock_ledger` (`stacksTo 1`) : clic droit sur un animal d'élevage affiche dans le chat : espèce, gènes,
  nourriture (et jour du dernier repas), santé et état sanitaire, affection.
- Affection : non définie par le design; affichée comme l'état amoureux vanilla (amoureux, calme, trop jeune).
- Lecture seule : aucun état n'est modifié (GameTest `ledgerHasNoSideEffects`).
- Texture provisoire : `item/almanac`, en attendant `item/livestock_ledger` (basculement par l'intégrateur).

### 18.6 Modules à venir (squelettes en v0.4)

- **husbandry** (bâtiments et alimentation) :
  - `feeding_trough` : contient foin, blé, aliment composé ou ensilage; nourrit les animaux d'élevage dans un rayon de 6
    via `LivestockApi.registerFeedSource`;
  - `nesting_box` : les poules y pondent (le module husbandry alimente le contrat de ponte);
  - ensilage : craft direct à partir de foin; `feed_mix` : orge, maïs et soja.
- **vet** (santé animale) :
  - maladies `SICK` par surpopulation (plus de 8 animaux dans un rayon de 8 blocs) ou par satiété basse prolongée;
  - `PARASITES` sur pâturage humide ou sous pluie; contagion entre animaux voisins;
  - `veterinary_kit` pour soigner; `salt_lick` (bloc préventif).
- **duck** (canard et productions) : entité `terresvivantes:duck` (nage, génome), œufs de canard, plumes,
  viande crue et cuite; qualité du lait, de la laine et des œufs selon le génome et l'alimentation.

### 18.7 Écarts et points ouverts

- Mooshroom compté comme broutard (le design ne le listait pas).
- Laine et lait : pas de multiplicateur appliqué en v0.4 (voir 18.4).
- Attachments d'entité non synchronisés vers le client : le registre écrit dans le chat du serveur.
- Santé 0 sans effet; pas de mort, pas de maladie spontanée en v0.4.

### 18.8 Critères de validation (v0.4)

- `./gradlew build` : tests JUnit de `GenomeLogic` et `LivestockLogic` verts.
- `./gradlew runGameTestServer` : 45 GameTests verts (40 existants et 5 de `LivestockGameTests` : héritage borné,
  animal non nourri qui perd sa santé, pâturage en été, reproduction refusée en hiver et sous 40, registre sans effet de bord).
