package com.minelittlepony.client.render.entity.npc.textures;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.village.VillagerType;

import com.minelittlepony.util.ResourceUtil;

import java.util.*;

/**
 * Cached pool of villager textures.
 */
public class PonyTextures<T extends LivingEntity & VillagerDataContainer> implements TextureSupplier<T> {

    private final TextureSupplier<String> formatter;

    private final Identifier fallback;

    private final Map<String, Identifier> cache = new HashMap<>();

    private final ResourceManager resourceManager = MinecraftClient.getInstance().getResourceManager();

    /**
     * Creates a new profession cache
     *
     * @param formatter Formatter used when creating new textures
     * @param keyMapper Mapper to convert integer ids into a string value for format insertion
     * @param fallback  The default if any generated textures fail to load. This is stored in place of failing textures.
     */
    public PonyTextures(TextureSupplier<String> formatter) {
        this.formatter = formatter;
        this.fallback = formatter.supplyTexture("villager_pony");
    }

    @Override
    public Identifier supplyTexture(T entity) {
        VillagerData t = entity.getVillagerData();

        return getTexture(t.getType(), t.getProfession());
    }

    private Identifier getTexture(final VillagerType type, final VillagerProfession profession) {


        String key = ResourceUtil.format("pony/%s/%s", type, profession);

        if (cache.containsKey(key)) {
            return cache.get(key); // People often complain that villagers cause lag,
                                   // so let's do better than Mojang and rather NOT go
                                   // through all the lambda generations if we can avoid it.
        }

        Identifier result = verifyTexture(formatter.supplyTexture(key)).orElseGet(() -> {
            if (type == VillagerType.PLAINS) {
                // if texture loading fails, use the fallback.
                return fallback;
            }

            return getTexture(VillagerType.PLAINS, profession);
        });

        cache.put(key, result);
        return result;
    }

    protected Optional<Identifier> verifyTexture(Identifier texture) {
        return resourceManager.getResource(texture).map(i -> texture);
    }

    public static boolean isBestPony(LivingEntity entity) {
        if (!entity.hasCustomName()) {
            return false;
        }
        String name = entity.getCustomName().getString();
        return "Derpy".equals(name) || (entity.isBaby() && "Dinky".equals(name));
    }

    public static boolean isCrownPony(LivingEntity entity) {
        return isBestPony(entity) && entity.getUuid().getLeastSignificantBits() % 20 == 0;
    }
}