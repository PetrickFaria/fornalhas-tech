package br.com.petrick.fornalhastech.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** Reuses vanilla flame motion and animation while rendering each spark at 25% scale. */
public final class SmallFlameParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final FlameParticle.Provider vanilla;

    public SmallFlameParticleProvider(SpriteSet sprites) {
        this.vanilla = new FlameParticle.Provider(sprites);
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = vanilla.createParticle(type, level, x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) particle.scale(0.25F);
        return particle;
    }
}
