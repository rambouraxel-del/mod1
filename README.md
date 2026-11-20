# Terres Vivantes

Mod Minecraft pour **NeoForge 1.21.1** sur la vie agricole : saisons, climat, sol, cultures, compost, nourriture et outils.

Identifiant du mod : `terresvivantes` · Package racine : `fr.terresvivantes` · Licence : MIT · Version : 0.1.0

## Prérequis

- JDK 21
- Connexion réseau pour télécharger Minecraft, NeoForge et les dépendances Maven (le wrapper Gradle 8.14.3 est inclus)

## Commandes

```bash
./gradlew build                # compilation + tests JUnit, produit build/libs/
./gradlew runClient            # lance le client de dev
./gradlew runServer            # lance le serveur de dev
./gradlew runGameTestServer    # lance les tests de jeu puis s'arrête
./gradlew runData              # génère les ressources (lang, modèles, recettes...) dans src/generated/resources
```

Les ressources générées par `runData` sont versionnées et incluses dans le jar.

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) : structure, modules, datagen, réseau, config
- [docs/AGENT_GUIDE.md](docs/AGENT_GUIDE.md) : conventions et règles de contribution
- [docs/STATUS.md](docs/STATUS.md) : avancement
