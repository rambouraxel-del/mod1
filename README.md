# Terres Vivantes

Mod pour **Minecraft 1.21.1** sur **NeoForge** : une vie agricole vivante. Les saisons, le sol, les cultures, les animaux,
la transformation et l'économie de village s'influencent entre eux. Rien n'est un bouton magique : il faut semer au bon
moment, nourrir ses bêtes, transformer ses récoltes et vendre au bon endroit.

Version : **0.8.0** · Identifiant du mod : `terresvivantes` · Licence : MIT (fichier `LICENSE` à ajouter avant la 1.0)

## Installation

1. Installer **NeoForge 21.1.267** pour **Minecraft 1.21.1**.
2. Copier `terresvivantes-0.8.0.jar` dans le dossier `mods/` de votre instance (client), ou du serveur.
3. Lancer le jeu. Le manuel du fermier est remis à la première connexion (une seule fois par joueur).

Le mod n'a pas d'autre dépendance que NeoForge et Minecraft. Pour compiler le jar depuis les sources : `./gradlew build`
(JDK 21), le jar est produit dans `build/libs/`.

## Fonctionnalités

**Saisons et climat.** Quatre saisons, une température locale (biome, altitude, nuit) et un affichage à l'écran. En hiver,
les cultures ralentissent presque à l'arrêt; une serre garde la chaleur et permet quelques cultures de printemps.

**Sol et cultures.** Chaque terre labourée a de l'azote, du phosphore, du potassium et de la matière organique. Compost,
fumier et cendre de bois les rechargent. Douze cultures (tomate, maïs, soja, laitue, oignon, ail, poivron, aubergine,
fraise, orge, chou, concombre) poussent plus ou moins bien selon la saison et le sol. La rotation protège la terre;
la monoculture attire les maladies.

**Eau et santé des cultures.** Arroseurs de cuivre et de fer, tonneau de pluie, sécheresse estivale. Oïdium, rouille,
pucerons et flétrissement se traitent avec le fongicide et le pulvérisateur; les coccinelles et l'épouvantail aident.

**Vergers.** Six espèces fruitières (pomme, cerise, orange, olive, noix, pêche), des feuillages qui changent et des fruits
à transformer. Un marchand ambulant passe de temps en temps.

**Biodiversité.** Fleurs sauvages, haies d'aubépine, papillons, coccinelles, lucioles. Plus il y a de pollinisateurs et
d'espèces, meilleures sont les récoltes. Apiculture complète : apiaire, extracteur, enfumoir, miels typés par la fleur.

**Élevage.** Vaches, moutons, cochons, poules, chèvres, lapins et canards ont des gènes (productivité, taille, résistance, fertilité) qui se
transmettent. Ils ont besoin de manger chaque jour, d'herbe hors hiver ou d'une auge. Nichoir, ensilage, aliment composé,
trousse vétérinaire et pierre à sel. Le canard donne œufs et viande. La tonte et la traite sont approximées, faute
d'événement dédié dans NeoForge.

**Transformation.** Meule et pressoir (farines, jus, huile d'olive), baratte, cuve à fromage et étagère d'affinage
(fromage de chèvre, fromage affiné), tonneau de fermentation (cidre, cornichons), séchoir, fumoir, marmite sur feu de camp
(douze plats). Les produits ont une qualité de 1 à 3 étoiles.

**Équipements et automatisation.** Grelinette (aère le sol), semoir, faucille en diamant, récolteuse, planteuse, caisse et
silo pour stocker. Corbeau et épouvantail protègent les cultures.

**Économie et village.** Monnaie cuivre, argent et or : 9 cuivre font 1 argent, 9 argent font 1 or. Le prix dépend de la
qualité, de la saison et de la saturation du marché. Le tableau d'annonces propose trois contrats, renouvelés à chaque aube; l'étal vend à
un villageois de jour. La guilde a un marchand, des échanges débloqués par réputation, et des titres de spécialisation
(maraîcher, arboriculteur, éleveur, fromager, apiculteur) pour qui vend beaucoup dans une filière.

**Manuel du fermier.** Un livre en jeu avec onze chapitres, en français et en anglais : sol, cultures, santé, vergers,
biodiversité, élevage, transformation, équipements, économie et commandes. Recette : un livre et des graines de blé.

## Commandes

| Commande | Droits | Effet |
|---|---|---|
| `/tv price` | tous | Prix de l'objet en main, et saturation du marché pour cet objet |
| `/tv reputation` | tous | Palier de réputation et points |
| `/tv titles` | tous | Titres de spécialisation de la guilde |
| `/tv season info` | opérateur (niveau 2) | Saison et jour du calendrier |
| `/tv season set` | opérateur (niveau 2) | Change la saison (tests et démonstration) |
| `/tv showcase [pos]` | opérateur (niveau 2) | Construit la vitrine de démonstration de toutes les sections |

## Documentation

- [docs/REPRISE.md](docs/REPRISE.md) : état du projet, problèmes connus, prochaines étapes (à lire en premier)
- [docs/STATUS.md](docs/STATUS.md) : avancement et vérifications par version
- [docs/BALANCE.md](docs/BALANCE.md) : équilibrage (maturation, rendements, prix, famine)
- [docs/PERFORMANCE.md](docs/PERFORMANCE.md) : mesures du coût serveur
- [docs/DESIGN.md](docs/DESIGN.md) : règles de design
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) : modules, contrats, datagen, configuration
- [docs/AGENT_GUIDE.md](docs/AGENT_GUIDE.md) : conventions de contribution

## Pour les développeurs

```bash
./gradlew build                # compilation + tests JUnit, produit build/libs/
./gradlew runClient            # lance le client de dev
./gradlew runServer            # lance le serveur de dev
./gradlew runGameTestServer    # lance les tests de jeu puis s'arrête
./gradlew runData              # génère les ressources (lang, modèles, recettes...) dans src/generated/resources
```

Les ressources générées par `runData` sont versionnées et incluses dans le jar.
