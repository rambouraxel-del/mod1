package fr.terresvivantes.health.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Spore : petite particule translucide, teintée par maladie, qui retombe lentement. */
final class SporeParticle extends TextureSheetParticle {

    SporeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                  SpriteSet sprites, float[] color) {
        super(level, x, y, z, dx, dy, dz);
        this.pickSprite(sprites);
        this.rCol = color[0];
        this.gCol = color[1];
        this.bCol = color[2];
        this.quadSize = 0.06f;
        this.lifetime = 30 + this.random.nextInt(20);
        this.gravity = 0.02f;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
