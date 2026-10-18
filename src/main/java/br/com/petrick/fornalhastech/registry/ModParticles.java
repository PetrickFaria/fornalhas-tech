package br.com.petrick.fornalhastech.registry;

import br.com.petrick.fornalhastech.FornalhasTech;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Small visual-only spark particle used by the active base furnace. */
public final class ModParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FornalhasTech.MODID);
    public static final RegistryObject<SimpleParticleType> SMALL_FIRE =
            TYPES.register("small_fire", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
