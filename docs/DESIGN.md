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

## 18. Élevage (v0.4 : modules `livestock`, `husbandry`, `vet`, `duck`)

Livré en v0.4 : génome et état vital des animaux d'élevage, héritage, besoins quotidiens, reproduction saisonnière,
`LivestockApi`, registre d'élevage, bâtiments d'alimentation et de ponte, santé animale, canard. Les écarts sont en 18.7.

### 18.1 Animaux et génome

- Animaux concernés : tag d'entités `terresvivantes:livestock` (vache, vache à champignons, cochon, mouton, poule, chèvre,
  lapin, et `terresvivantes:duck`, optionnel).
- Génome `Genome` : productivité, taille, résistance, fertilité, entiers de 0 à 10. Data Attachment d'entité, sérialisé,
  sans `copyOnDeath`. Génome initial : chaque trait tiré dans [4, 6].
- Hérédité : moyenne des parents par trait (arrondi aléatoire si impair), mutation de ±1 avec une chance de 15 %, bornée à [0, 10].
- `GenomeLogic` et `LivestockLogic` sont purs; `LivestockCodecs` porte la persistance.

### 18.2 Besoins

- Attachment `livestock_state` : `fed` (0 à 100), `health` (0 à 100), `ailment` (`NONE`, `SICK`, `PARASITES`), `lastFedDay`.
  Valeur initiale : satiété 50, santé 100, sans maladie.
- Cycle de 1200 ticks par animal, décalé par `id % 1200`.
- Par cycle : consommation −8 de satiété (−12 en hiver); pâturage +10 pour les broutards hors hiver sur `grass_block`;
  santé −5 à satiété nulle, +2 au-dessus de 60.
- Sources de nourriture tierces : `LivestockApi.registerFeedSource` (auges).
- Maladies (`vet`) : `SICK` par surpopulation ou sous-alimentation, `PARASITES` sur pâturage humide ou sous pluie,
  contagion entre voisins. Pierre à sel : +1 de santé par cycle à proximité. Santé à 0 : mort (`VetConfig.killAtZeroHealth`).

### 18.3 Reproduction

- Nourriture de reproduction refusée si la satiété est inférieure à 40, ou hors printemps et été (`breedingSeasonsOnly`, vrai par défaut).
- Jumeaux : chance de `fertilité moyenne × 3 %` (plafonnée à 1).

### 18.4 Productions et `LivestockApi`

- `productionMultiplier(animal)` = (0.6 + 0.08 × productivité) × (1.1 si satiété > 60; 0.5 si < 20; 1 sinon)
  × (0.5 si malade; 1 sinon), puis modificateurs enregistrés.
- Ponte : poules et canards pondent à la cadence du multiplicateur. Un nichoir proche reçoit l'œuf (8 emplacements).
- Laine et lait : calculés à partir du multiplicateur (qualité du lait de 0 à 3 étoiles), sans événement de tonte ou de traite NeoForge.

### 18.5 Registre d'élevage

- Item `livestock_ledger` (`stacksTo 1`, texture `item/livestock_ledger`) : clic droit sur un animal d'élevage affiche dans le chat
  l'espèce, les gènes, la nourriture, la santé et l'état sanitaire.
- Affection : non définie par le design; affichée comme l'état amoureux vanilla.
- Lecture seule : aucun état n'est modifié.

### 18.6 Bâtiments et produits (livrés)

- **husbandry** : `feeding_trough` (contient foin, blé, aliment composé ou ensilage; nourrit les animaux proches);
  `nesting_box` (8 emplacements, les œufs de poule et de canard y entrent); ensilage; `feed_mix`.
- **vet** : `veterinary_kit` (soigne un animal malade); `salt_lick` (bloc préventif).
- **duck** : entité `terresvivantes:duck` (mâle ou femelle, nage, ponte), œufs de canard, viande crue et cuite.

### 18.7 Écarts et points ouverts

- Mooshroom compté comme broutard (le design ne le listait pas).
- Tonte et traite : approximées sans événement NeoForge 21.1 dédié.
- Auge sans bord creux (le modèle n'a pas de rebord évidé).
- Attachments non synchronisés vers le client : le registre écrit dans le chat du serveur.
- QA visuelle en client du canard et de l'élevage à faire.

### 18.8 Critères de validation (v0.4)

- `./gradlew build` : tests JUnit verts (232, dont la logique des modules livestock, husbandry, vet et duck).
- `./gradlew runGameTestServer` : 54 GameTests verts (40 de la v0.3 et 14 de l'élevage : livestock 5, husbandry 3, vet 3, duck 3).
- `./gradlew runData` : `src/generated` régénéré et `GeneratedResourcesTest` vert.

---

## 19. Transformation (v0.5 : `processing`, `milling`, `dairy`, `preserving`, `kitchen`)

Objectif : transformer les récoltes et les produits d'élevage en produits plus utiles, avec une qualité qui se
transmet des entrées à la sortie. Une machine = un bloc + une BlockEntity + des recettes JSON; l'infrastructure est
commune (`processing`).

### 19.1 Infrastructure (livré)

- **Qualité générique** (`core.quality`) : composant `terresvivantes:quality` (entier 1 à 3) sur tout item qui le porte.
  L'infobulle affiche « Qualité: ★★☆ » (clé `tooltip.terresvivantes.quality`). Le lait de la traite le reçoit
  (ancien composant `milk_quality` du canard retiré).
- **Recette** (`terresvivantes:processing`), données dans `data/terresvivantes/recipe/` :
  `machine`, `ingredients` (liste de `{ingredient, count}`), `container` (optionnel, un `Ingredient` item ou tag consommé en 1 unité; l'ancien format chaîne reste lisible),
  `result` (`{id, count}`), `time` (ticks, 1 à 2 400 000), `quality` (`none`, `inherit_min`, `inherit_avg`, `age`).
- **Qualité de sortie** : `inherit_min` = plus basse des entrées; `inherit_avg` = moyenne arrondie; `age` = base =
  plus basse, le gain avec le temps vient de `QualityLogic.ageQuality` (étagère d'affinage, module dairy); `none` = pas
  de composant. Une entrée sans composant compte pour 1. Si aucune entrée n'a de qualité, la sortie n'en a pas.
- **Machine** : N entrées (1 à 4), conteneur optionnel, une sortie. Haut = entrées, côtés = conteneur, bas = sortie.
  Progression synchronisée par `ContainerData`; propriété `active` pour le visuel. Le ticker sort tout de suite si les
  entrées sont vides; la recette est cherchée seulement quand le contenu change.
- **Écran générique** : `ProcessingMenu` / `ProcessingScreen`, fond `textures/gui/processing.png` (176 x 166).
  Clic droit sur la machine : menu.

### 19.2 Milling (livré : meule)

- `millstone` : 1 entrée, pas de conteneur. Blé → farine de blé (×1, 100 ticks, `inherit_min`).
- Livré : farines d'orge et de maïs; pressoir (jus de pomme, pêche, orange; huile d'olive avec flacon).

### 19.3 Dairy (livré)

- Baratte : lait → beurre + crème.
- Cuve à fromage : lait + qualité → caillé (qualité héritée).
- Étagère d'affinage : caillé → fromage jeune → fromage affiné selon les jours (qualité `age`, `ageQuality`).
- Fromage de chèvre (variante du même chemin). Lait sans composant `milk_source` = vache; `goat` = chèvre (traite vanilla).
- Restes : le seau vide est rendu par le framework (slot conteneur, sortie, sinon éjection).

### 19.4 Preserving (livré)

- Tonneau de fermentation (plusieurs jours de jeu) : jus → cidre ou vinaigre; chou → choucroute; concombre → cornichons.
- Séchoir : fruits secs, viande séchée.
- Fumoir : viandes et poissons fumés; une recette par produit, combustible = tag `terresvivantes:smoker_fuel`
  (bûches, charbon, charbon de bois).

### 19.5 Kitchen (livré)

- Marmite (`cooking_pot`) : 4 entrées et un conteneur (bol). Recette de référence : omelette au fromage (160 ticks).
- 12 plats : pizza_slice, quiche, cheese_omelette, apple_tart, cheese_sandwich, olive_bread, duck_confit,
  corn_tortilla, pancakes, vegetable_gratin, french_onion_soup, butter_cookie.

### 19.6 Critères de validation (v0.5, état de cette livraison)

- `./gradlew build` : tests JUnit verts (logique qualité, logique de transformation, modules existants).
- `./gradlew runGameTestServer` : 75 GameTests verts, trois passages (dont la chaîne lait de vache qualité 3 → caillé →
  fromage affiné 3, la traite d'une chèvre → fromage de chèvre, l'huile d'olive, la marmite à l'omelette et la vitrine).
- `./gradlew runData` : `src/generated` régénéré, `GeneratedResourcesTest` vert.
- Non vérifié : rendu visuel client des machines et de l'écran (`tools/textures/sprites_v05.py`).
- Non couvert par GameTest : l'héritage à deux entrées (3 et 2 donne 2) est testé en JUnit (`ProcessingLogicTest`), car la
  meule n'a qu'une entrée.

## 20. Équipements & automatisation (v0.6 : `implement`, `automation`, `storage`, `pests`)

### 20.1 Implement (livré)

- Grelinette : clic droit sur herbe ou terre labourée, zone 3x3 selon les règles de la houe. Sur une terre labourée,
  aère le sol : +5 de matière organique, au plus une fois par bloc et par jour MC (`SoilApi.aerate`, mémorisé dans
  l'attachment de chunk, champ optionnel `aerated_day`). Refus : sans effet, sans usure.
- Semoir : clic droit sur terre labourée, plante la graine de la main secondaire sur une zone 3x3, ou une ligne de 5 cases
  si le joueur est accroupi. Une graine et une unité de durabilité par case plantée.
- Faucille en diamant : récolte la zone 5x5 (rayon 2) des cultures mûres.

### 20.2 Automation (livré)

- Récolteuse : zone de volume 5x5x3 devant la machine (la case de culture au niveau de la machine et deux couches au-dessus).
  Récolte les cultures mûres (replantées) et les feuilles fruitières mûres (`OrchardApi.harvestRipeLeaves`). Les récoltes
  vont dans son inventaire, poussé vers le conteneur derrière ou dessous.
- Planteuse : zone 5x5 devant la machine; plante les graines de son inventaire sur les terres labourées.
- Cadence : `terresvivantes-automation.toml` (activé par défaut, cycle de 40 ticks, rayon 2). Cinq cases par cycle.
  Redstone alimentée : pause.

### 20.3 Storage (livré)

- Caisse : un seul type d'item, 8 stacks (512 pour une stack de 64). Rendu sur la face avant; exposée aux trémies.
- Silo : colonne empilable de 1 à 8 blocs, 16 stacks par bloc (48 pour trois blocs). Insertion par le haut; extraction
  par le bas seulement. Accepte les items du tag `terresvivantes:silo_storable` (produits agricoles).
- Recette de la caisse : 8 planches (tag `minecraft:planks`) autour d'un lingot de fer. Silo : 6 lingots de fer cerclant
  3 planches.

### 20.4 Pests (livré)

- Corbeau : picore les cultures mûres (âge 4 et plus), hors hiver; spawn naturel plafonné (`CrowLogic.MAX_CROWS_PER_AREA`) et
  désactivable (`terresvivantes-pests.toml`). Biomes : tag `terresvivantes:crow_habitat` (`#c:is_plains`, `#minecraft:is_forest`).
- Épouvantail : éloigne les corbeaux dans son rayon (`SCARECROW_RADIUS`, 12 blocs par défaut) et réduit l'apparition des ravageurs
  (`HealthApi.registerPestSuppressor`). Son efficacité s'use après plusieurs jours (`ScarecrowLogic.isStale`).
- Non fait : bonus de lapins près de l'épouvantail (prévu au design, reporté).

### 20.5 Critères de validation (v0.6, état de cette livraison)

- `./gradlew build` : 309 tests JUnit verts (dont `GeneratedResourcesTest`).
- `./gradlew runData` : `src/generated` régénéré.
- `./gradlew runGameTestServer` : 93 GameTests verts, trois passages (dont la récolteuse qui prend une feuille fruitière mûre
  dans sa couche supérieure, et la vitrine avec sa section équipements).
- Non vérifié : rendu client des machines, caisse et silo (voir `docs/STATUS.md`, problèmes connus).

## 21. Économie (v0.7 : `economy`, `market`, `guild`)

### 21.1 Monnaie (livré : `economy`)

- Trois pièces : `copper_coin`, `silver_coin`, `gold_coin`. 1 argent = 20 cuivre, 1 or = 20 argent = 400 cuivre.
- Conversion grosse vers petite : 1 argent donne 20 cuivre, 1 or donne 20 argent (recettes sans forme, `EconomyModule`).
- Non fait : conversion petite vers grosse (20 cuivre vers 1 argent). Une recette de grille contient 9 ingrédients au plus et
  consomme un item par emplacement : elle ne peut pas compter 20 pièces. Solution prévue : change par le marchand de la guilde.

### 21.2 Modèle de prix (livré : `economy.logic.PriceModel`, `BasePrices`)

- Prix par unité en cuivre : `base x qualité x saison x saturation`, arrondi, au moins 1 pour un item vendable.
- Base : `data/terresvivantes/economy/base_prices.json` (id vers prix en cuivre, qualité 1). Les items du mod sont sans espace
  de noms; le vanilla est complet (`minecraft:wheat`). Un transformé vaut plus que son brut (vérifié par test JUnit).
- Qualité (composant `terresvivantes:quality`, v0.5) : x1, x1.25, x1.5 pour les qualités 1, 2, 3. Absente : x1.
- Saison : produit hors de sa saison optimale x1.3; en pleine saison x0.85. Saison optimale : `CropDefinition.optimal` pour
  les cultures, `FruitTreeDefinition.ripeSeasons` pour les fruits. Produit transformé ou sans saison : x1.
- Offre et demande : `MarketData` (SavedData par dimension). Chaque vente ajoute 1 point de saturation; la saturation
  décroît de 10 % par jour MC. Prix x max(0.4, 1 - 0.02 x points).

### 21.3 Réputation (livré : `economy`)

- Data attachment `terresvivantes:reputation` sur le joueur : entier persistant, copié à la mort (`copyOnDeath`).
- Paliers : Inconnu 0, Habitué 50, Fournisseur 200, Producteur réputé 500, Maître fermier 1200.

### 21.4 Commandes (livré)

- `/tv price` : prix actuel de l'item tenu en main (permission 0).
- `/tv reputation` : réputation et palier du joueur (permission 0).
- `/tv season` et `/tv showcase` restent réservés aux opérateurs (niveau 2), filtrage au niveau de chaque sous-arbre.

### 21.5 Market (spec, non livré : module vide `market`)

- `market_board` (tableau d'annonces) : 3 contrats de livraison générés chaque jour MC selon la saison et la réputation.
  Exemple : « Livrer 32 tomates qualité ≥ 2 avant 3 jours : 60 cuivre + 10 réputation ». Livraison en déposant les produits
  dans le tableau. Expiration : pénalité de réputation.
- `market_stall` (étal) : le joueur y dépose des produits. Toutes les 2400 ticks, un acheteur PNJ (villageois à 16 blocs au plus)
  achète 1 item au prix `EconomyApi.priceOf`; les pièces vont dans le stock de l'étal.

### 21.6 Guilde (spec, non livré : module vide `guild`)

- Profession de villageois « marchand de la guilde » (POI `guild_desk`). Vend graines rares, saplings et machines; achète les
  produits en pièces.
- Offres débloquées selon le palier de réputation (`ReputationLogic.Tier`).
- Titres de spécialisation (Maraîcher, Arboriculteur, Éleveur, Fromager, Apiculteur) selon les volumes livrés, avec +5 % de
  chance de qualité supérieure dans leur filière. Reporté, à faire si le temps le permet.

### 21.7 Critères de validation (v0.7)

- Tests JUnit purs : conversions, qualité, saison, saturation (50 ventes puis 5 jours : le prix remonte), paliers, table de prix.
- GameTests : tomate hors saison plus chère qu'en saison; 50 ventes font baisser le prix; la réputation survit à la sauvegarde NBT.
- Non vérifié : achat PNJ, contrats, réputation après mort (pas de test de mort en GameTest).
