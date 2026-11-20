package fr.terresvivantes.soil;

import fr.terresvivantes.crop.CropApi;
import fr.terresvivantes.crop.CropGrowthHooks;
import fr.terresvivantes.crop.logic.CropDefinition;
import fr.terresvivantes.crop.logic.CropDefinitions;
import fr.terresvivantes.soil.logic.RotationModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Modifier de croissance de la rotation, enregistré par {@link SoilModule} (voir {@link RotationModel}). */
final class SoilRotation implements CropGrowthHooks.GrowthModifier {

    @Override
    public float modify(ServerLevel level, BlockPos cropPos, BlockState state, CropDefinition def) {
        BlockPos farmland = cropPos.below();
        String last = SoilApi.getLastCrop(level, farmland);
        if (last == null) {
            return 1.0f;
        }
        boolean previousLegume = CropDefinitions.byId(last).map(CropDefinition::legume).orElse(false);
        boolean currentLegume = def != null && def.legume();
        return RotationModel.factor(
                SoilApi.getRepeatCount(level, farmland),
                last.equals(CropApi.cropIdOf(state)),
                previousLegume,
                currentLegume);
    }
}
