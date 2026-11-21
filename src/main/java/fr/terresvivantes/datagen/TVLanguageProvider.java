package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

/** Genere le fichier de langue en_us ou fr_fr a partir des hooks des modules. */
public final class TVLanguageProvider extends LanguageProvider {
    private final boolean french;

    public TVLanguageProvider(PackOutput output, boolean french) {
        super(output, TerresVivantes.MODID, french ? "fr_fr" : "en_us");
        this.french = french;
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + TerresVivantes.MODID, "Terres Vivantes");
        for (var module : TerresVivantes.modules()) {
            module.registerLang((key, enUs, frFr) -> add(key, french ? frFr : enUs));
        }
    }
}
