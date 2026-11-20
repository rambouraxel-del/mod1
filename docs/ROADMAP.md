# Terres Vivantes — Feuille de route

Chaque version doit être **jouable et testée** avant la suivante. Les valeurs de `docs/DESIGN.md` font foi pour v0.1 ; les versions suivantes sont des intentions à affiner avant leur lancement.

Ordre et dépendances : v0.1 (fondations) → v0.2 (eau, vergers, maladies) → v0.3 (insectes et pollinisation) → v0.4 (élevage) → v0.5 (transformation) → v0.6 (équipements) → v0.7 (économie).

---

## v0.1 — Fondations

Contenu : module `season`, `climate`, `soil`, `crop` (12 cultures), `compost`, `food` (10 plats), `tools` (4 outils), intégration vanilla, config, HUD. Voir `docs/DESIGN.md`.

### Critères de validation

**Saisons**
- [ ] Une année dure 4 × `daysPerSeason` jours ; la saison change au bon jour.
- [ ] `/tv season set <saison> [jour]` et `/tv season info` fonctionnent ; les autres joueurs non-op reçoivent un refus.
- [ ] Le changement de saison est conservé après redémarrage du serveur.
- [ ] Le HUD `Printemps · Jour 3/7` s'affiche en haut à gauche ; désactivable par config.
- [ ] À la connexion et au changement de jour, le client reçoit la saison correcte (payload).

**Climat**
- [ ] La température affichée par l'almanach respecte la formule (biome, saison, altitude, nuit).
- [ ] Une culture hors plage de tolérance croît à ×0.3 ; au-delà de 8 °C hors plage, elle ne croît pas.

**Sol**
- [ ] Une terre labourée reçoit des valeurs initiales 60/50/50/30 à la première lecture ; désert : N/P/K 30, organique 10 ; jungle : organique 80.
- [ ] Le facteur de Liebig suit les seuils (min < 20 → 0.25 ; < 40 → 0.6 ; sinon 1.0) ; bonus ×1.1 si organique > 60.
- [ ] Une terre remise en `dirt` perd ses données.
- [ ] Les engrais appliquent exactement les valeurs de la spec, bornées à 100, sur le bloc ciblé seul.
- [ ] Terre sèche (`MOISTURE = 0`) : croissance ×0.5 ; l'arrosoir règle `MOISTURE = 7` ; la sonde affiche la valeur.
- [ ] La sonde (`soil_probe`) affiche N, P, K, organique, humidité dans le chat.

**Cultures**
- [ ] Les 12 cultures sont plantables depuis leur graine, croissent selon la saison, et se récoltent.
- [ ] La récolte donne le produit (quantité dans la plage) et 1–2 graines.
- [ ] Clic droit sur une culture mature : récolte et replantation.
- [ ] Croissance par `GrowthModel` : pour `f < 1`, la part de tentatives annulées vaut environ `1 − f` ; pour `f > 1`, des stades supplémentaires apparaissent à environ `f − 1` (mesuré sur un échantillon).
- [ ] Hiver : aucune croissance en extérieur, pour toutes les cultures.
- [ ] Une légumineuse (soja) augmente N à chaque stade.
- [ ] L'almanach liste les cultures optimales à planter maintenant.

**Intégration vanilla**
- [ ] Blé, carotte, pomme de terre, betterave et tiges de citrouille/pastèque ralentissent selon saison, climat et sol.
- [ ] Avec `vanillaIntegration = false`, le comportement vanilla est restauré.

**Outils**
- [ ] `sickle` récolte et replante en 3×3 ; s'use à 250 utilisations puis casse.
- [ ] `watering_can` : 8 utilisations, remplissage sur eau, humidifie en 3×3 au max.

**Compost et fumier**
- [ ] Le bac accepte les matières, affiche 8 niveaux, passe en `ready` une fois plein après ~1 jour MC.
- [ ] Clic droit sur `ready` : 2 à 3 compost, bac vidé.
- [ ] Vaches, cochons, moutons et chevaux lâchent du fumier à une fréquence proche de la config.
- [ ] `charcoal` → 2 `wood_ash` en craft sans forme.

**Nourriture**
- [ ] Les 10 plats se craftent avec les ingrédients de la spec ; `grilled_corn` se cuit au four.
- [ ] Les produits crus sont comestibles, valeurs faibles.

**Configuration et stabilité**
- [ ] Chaque clé de `TVConfig` change le comportement attendu.
- [ ] Tests unitaires JUnit verts sur `logic/` (saison, climat, Liebig, croissance, compost).
- [ ] Partie multijoueur (2 joueurs) : saison et HUD synchronisés, pas d'erreur côté serveur ou client.
- [ ] Pas de crash ni d'erreur de log en 30 min de jeu normal.

---

## v0.2 — Vergers & eau

Contenu prévu :
- Arbres fruitiers (pommier, cerisier, oranger, olivier, noyer…) avec saisons de floraison et de fructification.
- Irrigation : arroseurs et canaux.
- Serres : verre au-dessus, croissance ×0.6 en hiver (au lieu de 0).
- Cultures qui flétrissent (`withered_crop`), reportées depuis v0.1.
- Maladies (mildiou, rouille) favorisées par l'humidité du sol (`MOISTURE`, déjà présente en v0.1).
- Rotation des cultures (pénalité si la même culture revient sur la même parcelle).

### Critères de validation
- [ ] Chaque arbre fruitier fleurit à sa saison, fructifie, et donne des fruits à la récolte.
- [ ] Un arroseur humidifie une zone définie ; un canal alimente la zone sans intervention.
- [ ] Une serre au-dessus de verre maintient une croissance en hiver ×0.6 (mesurée).
- [ ] Une culture ratée plusieurs fois (3 tentatives, cf. spec) flétrit en `withered_crop`.
- [ ] Par temps humide, le mildiou apparaît plus souvent ; un traitement l'élimine.
- [ ] La rotation pénalise un champ resemée avec la même culture, et ne pénalise pas une rotation.
- [ ] Les tests unitaires couvrent les nouvelles formules ; pas de régression v0.1.

---

## v0.3 — Biodiversité

Contenu prévu :
- Insectes : abeilles sauvages, papillons, coccinelles (qui mangent les pucerons) ; pucerons nuisibles.
- Pollinisation : bonus de rendement selon la présence d'abeilles et de fleurs.
- Apiculture : ruches à cadres, miel par fleur butinée, cire.
- Haies et fleurs sauvages (décor et source de pollinisateurs).

### Critères de validation
- [ ] Les abeilles sauvages apparaissent dans les zones fleuries et butinent les cultures pollinisées.
- [ ] Une culture avec pollinisation donne un rendement supérieur à une culture sans, mesuré.
- [ ] Une coccinelle réduit les pucerons d'une parcelle ; sans coccinelle, les pucerons s'installent.
- [ ] Une ruche produit du miel (proportionnel aux fleurs proches) et de la cire, récoltables avec les bons outils.
- [ ] Haies et fleurs sauvages se posent et se cassent sans erreur.

---

## v0.4 — Élevage

Contenu prévu :
- Races, génétique simple (gènes de production, taille, résistance).
- Alimentation : rations, foin, ensilage.
- Santé et maladies des animaux.
- Nouvelles productions : lait de chèvre, laine colorée, œufs de cane.
- Nouvelles espèces : chèvre domestique, canard, oie, lapin d'élevage.

### Critères de validation
- [ ] Les nouvelles espèces apparaissent, se reproduisent et héritent des gènes de leurs parents.
- [ ] Une ration complète donne une production supérieure à une ration pauvre, mesuré.
- [ ] Une maladie se déclare sur un animal mal nourri et se soigne.
- [ ] Lait de chèvre, laine colorée et œufs de cane se récoltent et se craftent.
- [ ] La génétique et la santé n'ont pas de régression sur le fumier de v0.1.

---

## v0.5 — Transformation

Contenu prévu :
- Moulin (blé, orge → farine).
- Presse (huile, jus, cidre).
- Baratte (crème → beurre).
- Fromagerie et cave d'affinage.
- Fumoir et séchoir.
- Bocaux de conserve.
- Cuisine avancée (plats à plusieurs étapes).

### Critères de validation
- [ ] Chaque machine transforme ses entrées dans les proportions annoncées, et s'arrête à court d'ingrédient.
- [ ] Les bocaux conservent un produit pendant une durée mesurée, puis se dégradent.
- [ ] Un fromage passe par la fromagerie puis la cave, avec une durée d'affinage visible.
- [ ] Les nouvelles recettes de cuisine se craftent et les plats ont une nutrition cohérente avec v0.1.

---

## v0.6 — Équipements & automatisation

Contenu prévu :
- Semoir, charrue attelée.
- Silos et granges (stockage).
- Convoyeurs et trémies agricoles.
- Épouvantail (protège des corbeaux et oiseaux).

### Critères de validation
- [ ] Le semoir plante une rangée entière avec une graine par case, à la bonne saison.
- [ ] La charrue attelée laboure une bande et s'arrête sur un obstacle.
- [ ] Silos et granges conservent les produits sans les perdre à la déconnexion ou au redémarrage.
- [ ] Un convoyeur relie une trémie à un silo sans intervention.
- [ ] L'épouvantail réduit les dégâts d'oiseaux mesurés sur une parcelle.

---

## v0.7 — Économie

Contenu prévu :
- Marché de village, étals.
- Contrats de livraison.
- Réputation.
- Spécialisation des exploitations.
- Monnaie : pièces de cuivre et d'argent.

### Critères de validation
- [ ] Un étal vend un produit à un prix affiché ; l'achat débite la monnaie et crédite le vendeur.
- [ ] Un contrat de livraison se signe, se remplit, et paie à l'échéance ; un contrat manqué baisse la réputation.
- [ ] La réputation influence les prix ou l'accès aux contrats, mesuré.
- [ ] Une exploitation spécialisée (ex. élevage) bénéficie d'un bonus défini, visible dans l'interface.
- [ ] La monnaie persiste sur le serveur et se synchronise au client sans erreur.
