package br.com.petrick.fornalhastech.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

/** Reuses vanilla flame behavior and sprite animation at 75% of its usual size. */
public final class TechFlameParticleProvider implements ParticleProvider<SimpleParticleType> {
    private final FlameParticle.Provider vanilla;

    public TechFlameParticleProvider(SpriteSet sprites) {
        vanilla = new FlameParticle.Provider(sprites);
    }

    @Override
    public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                   double x, double y, double z,
                                   double xSpeed, double ySpeed, double zSpeed) {
        Particle particle = vanilla.createParticle(type, level, x, y, z, xSpeed, ySpeed, zSpeed);
        if (particle != null) particle.scale(0.75F);
        return particle;
    }
}
