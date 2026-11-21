package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class TVItemModelProvider extends ItemModelProvider {
    public TVItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, TerresVivantes.MODID, helper);
    }

    @Override
    protected void registerModels() {
        for (var module : TerresVivantes.modules()) {
            module.registerItemModels(this);
        }
    }
}
