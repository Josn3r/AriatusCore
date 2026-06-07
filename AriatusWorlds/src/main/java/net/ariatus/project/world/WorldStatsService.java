package net.ariatus.project.world;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Villager;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class WorldStatsService {

    public WorldStats collect(World world) {
        int loadedChunks = world.getLoadedChunks().length;
        int players = world.getPlayers().size();

        int entities = 0;
        int livingEntities = 0;
        int mobs = 0;
        int animals = 0;
        int monsters = 0;
        int droppedItems = 0;
        int armorStands = 0;
        int villagers = 0;
        int tileEntities = 0;

        for (Entity entity : world.getEntities()) {
            entities++;

            if (entity instanceof LivingEntity) {
                livingEntities++;
            }

            if (entity instanceof Mob) {
                mobs++;
            }

            if (entity instanceof Monster) {
                monsters++;
            }

            if (entity instanceof Ageable && !(entity instanceof Villager)) {
                animals++;
            }

            if (entity instanceof Item) {
                droppedItems++;
            }

            if (entity instanceof ArmorStand) {
                armorStands++;
            }

            if (entity instanceof Villager) {
                villagers++;
            }
        }

        for (Chunk chunk : world.getLoadedChunks()) {
            tileEntities += chunk.getTileEntities().length;
        }

        return new WorldStats(
                world.getName(),
                loadedChunks,
                players,
                entities,
                livingEntities,
                mobs,
                animals,
                monsters,
                droppedItems,
                armorStands,
                villagers,
                tileEntities,
                world.getWorldBorder().getSize(),
                world.getPVP(),
                world.getDifficulty().name(),
                world.getFullTime(),
                world.hasStorm(),
                world.isThundering()
        );
    }

    public Map<String, Integer> topEntities(World world, int limit) {
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (Entity entity : world.getEntities()) {
            String key = entity.getType().name();

            counts.merge(key, 1, Integer::sum);
        }

        return sortAndLimit(counts, limit);
    }

    public Map<String, Integer> topBlockEntities(World world, int limit) {
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (Chunk chunk : world.getLoadedChunks()) {
            for (BlockState blockState : chunk.getTileEntities()) {
                String key = blockState.getType().name();

                counts.merge(key, 1, Integer::sum);
            }
        }

        return sortAndLimit(counts, limit);
    }

    private Map<String, Integer> sortAndLimit(Map<String, Integer> input, int limit) {
        return input.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(1, limit))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));
    }
}