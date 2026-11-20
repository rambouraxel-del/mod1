package fr.terresvivantes.datagen;

import fr.terresvivantes.TerresVivantes;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class TVBlockStateProvider extends BlockStateProvider {
    public TVBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, TerresVivantes.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (var module : TerresVivantes.modules()) {
            module.registerBlockStates(this);
        }
    }
}
