package fr.terresvivantes.core.debug;

import fr.terresvivantes.core.TVModule;
import fr.terresvivantes.datagen.LangSink;
import net.neoforged.bus.api.IEventBus;

/** Module outils de développement : pas de contenu enregistré, seulement les textes de {@code /tv showcase}. */
public final class DebugModule implements TVModule {

    @Override
    public void register(IEventBus modBus) {
    }

    @Override
    public void registerLang(LangSink sink) {
        sink.add("terresvivantes.command.showcase.built",
                "Showcase built at %1$s, %2$s blocks placed.",
                "Démonstration construite en %1$s, %2$s blocs posés.");
        sink.add("terresvivantes.command.showcase.need_pos",
                "From the console, give a position: /tv showcase <pos>.",
                "Depuis la console, indiquez une position : /tv showcase <pos>.");
    }
}
