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
| Hiver | 0.0 |

S'applique aux cultures vanilla et aux cultures du mod. **En v0.1, l'hiver bloque toute croissance en extérieur** (les serres arrivent en v0.2).

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
- `multSaison` : section 2.5. Hiver = 0 pour toutes les cultures en v0.1. La plantation reste autorisée en hiver ; la culture ne pousse pas.
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
- **Flétrissement (`withered_crop`) : hors v0.1**, reporté en v0.2. En v0.1, une culture à `f = 0` ne fait que stagner.

### 6.3 Les 12 cultures (proposé)

Valeurs complétées. Saison : O = optimal, P = possible, D = défavorable ; hiver = impossible pour toutes (v0.1). Consommation par stade, en N/P/K. Vitesse en jours MC (à f = 1). Rendement : quantité min–max du produit, plus 1–2 graines à la récolte.

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
| `seasonMultipliers.winter` | 0.0 | Idem, hiver |
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
| 4 | Flétrissement (`withered_crop`) | Reporté en v0.2. (6.2) |
| 5 | Compost | Maturation par `randomTick`. (7.1) |
| 6 | Altitude et nuit | Altitude : `−0.06 × max(0, y − 64)`. Nuit : `dayTime` ∈ [13000, 23000[. (3.1) |
| 7 | Application des multiplicateurs | `f = saison × affinité × climat × sol`. Si `f < 1` : tentative annulée avec probabilité `1 − f`. Si `f > 1` : chance `min(f − 1, 1)` d'un stade supplémentaire. Cultures du mod (randomTick) et vanilla (`CropGrowEvent.Pre`) partagent la classe pure `crop/logic/GrowthModel`. (6.2, 5) |
