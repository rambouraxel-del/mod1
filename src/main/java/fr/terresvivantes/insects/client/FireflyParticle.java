package fr.terresvivantes.insects.client;

import fr.terresvivantes.insects.logic.FireflyLogic;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Luciole : point lumineux pleine luminosité, dérive lente et aléatoire, durée de vie de 60 à 100 ticks.
 * Purement client. Le compteur de lucioles actives borne leur nombre autour du joueur.
 */
final class FireflyParticle extends TextureSheetParticle {

    private static final AtomicInteger ACTIVE = new AtomicInteger();
    /** Lumière maximale (blocs et ciel à 15), pour que la luciole brille la nuit. */
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Amplitude de la dérive, en blocs par tick. */
    private static final double DRIFT = 0.01;

    private boolean counted;

    FireflyParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.pickSprite(sprites);
        this.quadSize = 0.05f;
        this.lifetime = FireflyLogic.lifetime(this.random.nextDouble());
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.rCol = 0.95f;
        this.gCol = 0.95f;
        this.bCol = 0.45f;
        ACTIVE.incrementAndGet();
        this.counted = true;
    }

    static int active() {
        return ACTIVE.get();
    }

    @Override
    public void tick() {
        super.tick();
        this.xd = this.xd * 0.95 + (this.random.nextDouble() - 0.5) * DRIFT;
        this.yd = this.yd * 0.95 + (this.random.nextDouble() - 0.5) * DRIFT;
        this.zd = this.zd * 0.95 + (this.random.nextDouble() - 0.5) * DRIFT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void remove() {
        if (this.counted) {
            ACTIVE.decrementAndGet();
            this.counted = false;
        }
        super.remove();
    }
}
