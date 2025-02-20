package com.yungnickyoung.minecraft.betterjungletemples.world.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yungnickyoung.minecraft.betterjungletemples.module.StructurePlacementTypeModule;
import com.yungnickyoung.minecraft.yungsapi.world.structure.exclusion.EnhancedExclusionZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;
import java.util.function.Function;

public class BetterJungleTemplePlacement extends RandomSpreadStructurePlacement {
    public static final Codec<BetterJungleTemplePlacement> CODEC = RecordCodecBuilder.<BetterJungleTemplePlacement>mapCodec(builder -> builder
            .group(
                    Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(placement -> placement.locateOffset()),
                    FrequencyReductionMethod.CODEC.optionalFieldOf("frequency_reduction_method", FrequencyReductionMethod.DEFAULT).forGetter(placement -> placement.frequencyReductionMethod()),
                    Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(placement -> placement.frequency()),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(placement -> placement.salt()),
                    ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(placement -> placement.exclusionZone()),
                    EnhancedExclusionZone.CODEC.optionalFieldOf("enhanced_exclusion_zone").forGetter(placement -> placement.enhancedExclusionZone),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("spacing").forGetter(placement -> placement.spacing()),
                    ExtraCodecs.NON_NEGATIVE_INT.fieldOf("separation").forGetter(placement -> placement.separation()),
                    RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(placement -> placement.spreadType()))
            .apply(builder, builder.stable(BetterJungleTemplePlacement::new)))
            .flatXmap(verifySpacing(), DataResult::success)
            .codec();

    private static Function<BetterJungleTemplePlacement, DataResult<BetterJungleTemplePlacement>> verifySpacing() {
        return placement -> placement.spacing() <= placement.separation()
                ? DataResult.error("EnhancedRandomSpread's spacing has to be larger than separation")
                : DataResult.success(placement);
    }

    private final Optional<EnhancedExclusionZone> enhancedExclusionZone;

    public BetterJungleTemplePlacement(Vec3i locateOffset,
                                       FrequencyReductionMethod frequencyReductionMethod,
                                       Float frequency,
                                       Integer salt,
                                       Optional<ExclusionZone> exclusionZone,
                                       Optional<EnhancedExclusionZone> enhancedExclusionZone,
                                       Integer spacing,
                                       Integer separation,
                                       RandomSpreadType randomSpreadType) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, randomSpreadType);
        this.enhancedExclusionZone = enhancedExclusionZone;
    }

    @Override
    public StructurePlacementType<?> type() {
        return StructurePlacementTypeModule.BETTER_JUNGLE_TEMPLE_PLACEMENT;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGenerator chunkGenerator, RandomState randomState, long seed, int chunkX, int chunkZ) {
        ChunkPos chunkPos = this.getPotentialStructureChunk(seed, chunkX, chunkZ);
        if (chunkPos.x == chunkX && chunkPos.z == chunkZ) {
            BlockPos structurePos = chunkPos.getMiddleBlockPosition(120);
            boolean isOceanOrRiverNear = chunkGenerator
                    .getBiomeSource()
                    .findBiomeHorizontal(structurePos.getX(), structurePos.getY(), structurePos.getZ(),
                            48, 2,
                            biomeHolder -> biomeHolder.is(BiomeTags.IS_RIVER) || biomeHolder.is(BiomeTags.IS_OCEAN),
                            randomState.oreRandom().at(structurePos), true,
                            randomState.sampler()
                    ) != null;
            return !isOceanOrRiverNear;
        }
        return false;
    }

    @Override
    public boolean isStructureChunk(ChunkGenerator chunkGenerator, RandomState randomState, long seed, int x, int z) {
        if (!super.isStructureChunk(chunkGenerator, randomState, seed, x, z)) {
            return false;
        }
        return this.enhancedExclusionZone.isEmpty()
                || !this.enhancedExclusionZone.get().isPlacementForbidden(chunkGenerator, randomState, seed, x, z);
    }
}
