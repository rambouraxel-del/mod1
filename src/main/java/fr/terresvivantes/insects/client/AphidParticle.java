package fr.terresvivantes.insects.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/** Puceron : petit point vert qui tombe lentement depuis une culture infestée. */
final class AphidParticle extends TextureSheetParticle {

    AphidParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                  SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.pickSprite(sprites);
        this.quadSize = 0.05f;
        this.lifetime = 20 + this.random.nextInt(10);
        this.gravity = 0.01f;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
