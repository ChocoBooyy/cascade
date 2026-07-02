package dev.chocoboy.cascade.testmod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.chocoboy.cascade.neoforge.Vfx;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod("cascadetest")
public final class CascadeTestMod {

    public CascadeTestMod() {
        // register the third-party contain component at construction, before datapacks reload or effects send
        Vfx.registerComponent("contain",
                StreamCodec.composite(ByteBufCodecs.FLOAT, ContainSpec::radius, ContainSpec::new),
                RecordCodecBuilder.<ContainSpec>mapCodec(i -> i.group(
                        Codec.FLOAT.fieldOf("radius").forGetter(ContainSpec::radius)
                ).apply(i, ContainSpec::new)));
        NeoForge.EVENT_BUS.register(CascadeTestCommands.class);
    }
}
