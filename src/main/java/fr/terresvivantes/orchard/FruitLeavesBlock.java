package fr.terresvivantes.orchard;

import fr.terresvivantes.climate.ClimateApi;
import fr.terresvivantes.orchard.logic.FruitTreeDefinition;
import fr.terresvivantes.orchard.logic.OrchardLogic;
import fr.terresvivantes.season.SeasonApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Feuillage fruitier. Hérite des règles vanilla de feuilles (destruction par distance, persistance) et ajoute
 * le stade de fructification (0 feuillage, 1 floraison, 2 fruits verts, 3 fruits mûrs). Évolue par randomTick.
 */
public final class FruitLeavesBlock extends LeavesBlock {

    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, OrchardLogic.MAX_STAGE);

    private final FruitTreeDefinition def;

    public FruitLeavesBlock(FruitTreeDefinition def, BlockBehaviour.Properties properties) {
        super(properties);
        this.def = def;
        registerDefaultState(defaultBlockState().setValue(STAGE, 0));
    }

    public FruitTreeDefinition definition() {
        return def;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STAGE);
    }

    /** Toujours évaluée : les stades progressent même sur une feuille qui ne se détruit pas. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isDecaying(state)) {
            super.randomTick(state, level, pos, random);
            return;
        }
        int stage = state.getValue(STAGE);
        boolean warm = def.fruitingTemperature(ClimateApi.temperatureAt(level, pos));
        OrchardLogic.Step step = OrchardLogic.nextStage(def, stage, SeasonApi.getSeason(level), warm,
                random.nextDouble(), OrchardConfig.transitionChance(), OrchardApi.fruitSetFactor(level, pos, def));
        if (step.fruitFalls() && OrchardLogic.fallenFruitRemains(random.nextDouble())) {
            Block.popResource(level, pos, new ItemStack(OrchardModule.fruitItem(def)));
        }
        if (step.stage() != stage) {
            level.setBlock(pos, state.setValue(STAGE, step.stage()), Block.UPDATE_CLIENTS);
            if (step.stage() > stage) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    /** Clic main vide sur un stade 3 : récolte 1 à 3 fruits, la feuille revient au stade 0. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (state.getValue(STAGE) != OrchardLogic.MAX_STAGE) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            int count = OrchardLogic.yieldCount(def, level.getRandom().nextDouble());
            ItemStack fruit = new ItemStack(OrchardModule.fruitItem(def), count);
            player.getInventory().add(fruit);
            if (!fruit.isEmpty()) {
                player.drop(fruit, false);
            }
            server.setBlock(pos, state.setValue(STAGE, 0), Block.UPDATE_CLIENTS);
            server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                    1.0f, 0.8f + server.getRandom().nextFloat() * 0.4f);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Même condition que la destruction vanilla : distance maximale et non persistante. */
    private static boolean isDecaying(BlockState state) {
        return state.getValue(DISTANCE) == OrchardLogic.DECAY_DISTANCE && !state.getValue(PERSISTENT);
    }
}
