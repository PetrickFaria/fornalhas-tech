package br.com.petrick.fornalhastech.registry;

import br.com.petrick.fornalhastech.FornalhasTech;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Furnace-local particle type used to render vanilla flame at reduced scale. */
public final class ModParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FornalhasTech.MODID);
    public static final RegistryObject<SimpleParticleType> TECH_FLAME =
            TYPES.register("tech_flame", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
