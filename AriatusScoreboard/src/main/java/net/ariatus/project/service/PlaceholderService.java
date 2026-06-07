package net.ariatus.project.service;

import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.api.chunk.ChunkPreloadService;
import net.ariatus.project.api.chunk.ChunkPreloadTaskView;
import net.ariatus.project.api.economy.Currency;
import net.ariatus.project.api.economy.EconomyService;
import net.ariatus.project.api.profile.ExperienceProvider;
import net.ariatus.project.api.profile.ProfileService;
import net.ariatus.project.api.profile.ProfileView;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderService {

    private static final Pattern ANIMATION_PATTERN =
            Pattern.compile("%animation_([a-zA-Z0-9_-]+)%");

    private final AriatusScoreboard module;
    private final AnimationService animationService;

    public PlaceholderService(AriatusScoreboard module, AnimationService animationService) {
        this.module = module;
        this.animationService = animationService;
    }

    public String apply(Player player, String text) {
        if (text == null) {
            return "";
        }

        String result = text;

        result = applyAnimations(result);

        result = result
                .replace("%player_name%", player.getName())
                .replace("%player_uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%health%", String.valueOf((int) Math.ceil(player.getHealth())))
                .replace("%max_health%", String.valueOf((int) Math.ceil(player.getMaxHealth())))
                .replace("%level%", String.valueOf(player.getLevel()))
                .replace("%food%", String.valueOf(player.getFoodLevel()))
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .replace("%date%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("%perm-prefix%", "");

        result = applyProfilePlaceholders(player, result);
        result = applyEconomyPlaceholders(player, result);
        result = applyChunkPlaceholders(player, result);

        result = applyAnimations(result);

        result = applyProfilePlaceholders(player, result);
        result = applyEconomyPlaceholders(player, result);
        result = applyChunkPlaceholders(player, result);

        return result;
    }

    public String joinLines(Player player, java.util.List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        return lines.stream()
                .map(line -> apply(player, line))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String applyAnimations(String text) {
        Matcher matcher = ANIMATION_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String animationId = matcher.group(1);
            String frame = animationService.frame(animationId);

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(frame));
        }

        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private String applyProfilePlaceholders(Player player, String text) {
        Optional<ProfileView> optionalProfile = profile(player);

        if (optionalProfile.isEmpty()) {
            return text
                    .replace("%profile_level%", "0")
                    .replace("%profile_experience%", "0")
                    .replace("%profile_required_experience%", "0")
                    .replace("%profile_remaining_experience%", "0")
                    .replace("%profile_experience_progress%", "0")
                    .replace("%profile_total_experience%", "0")
                    .replace("%profile_reputation%", "0")
                    .replace("%profile_playtime%", "0s")
                    .replace("%profile_kills%", "0")
                    .replace("%profile_deaths%", "0")
                    .replace("%profile_blocks_broken%", "0")
                    .replace("%profile_blocks_placed%", "0");
        }

        ProfileView profile = optionalProfile.get();

        long requiredExperience = requiredExperience(profile);
        long remainingExperience = Math.max(0, requiredExperience - profile.experience());
        int progress = experienceProgress(profile.experience(), requiredExperience);

        return text
                .replace("%profile_level%", String.valueOf(profile.level()))
                .replace("%profile_experience%", String.valueOf(profile.experience()))
                .replace("%profile_required_experience%", String.valueOf(requiredExperience))
                .replace("%profile_remaining_experience%", String.valueOf(remainingExperience))
                .replace("%profile_experience_progress%", String.valueOf(progress))
                .replace("%profile_total_experience%", String.valueOf(profile.totalExperience()))
                .replace("%profile_reputation%", String.valueOf(profile.reputation()))
                .replace("%profile_playtime%", formatPlaytime(profile.playtimeSeconds()))
                .replace("%profile_kills%", String.valueOf(profile.kills()))
                .replace("%profile_deaths%", String.valueOf(profile.deaths()))
                .replace("%profile_blocks_broken%", String.valueOf(profile.blocksBroken()))
                .replace("%profile_blocks_placed%", String.valueOf(profile.blocksPlaced()));
    }

    private String applyEconomyPlaceholders(Player player, String text) {
        EconomyService economyService = economy();

        if (economyService == null) {
            return applyEconomyFallbacks(text);
        }

        BigDecimal coins = economyService.balance(player.getUniqueId(), Currency.COINS)
                .exceptionally(throwable -> BigDecimal.ZERO)
                .join();

        BigDecimal odrys = economyService.balance(player.getUniqueId(), Currency.ODRYS)
                .exceptionally(throwable -> BigDecimal.ZERO)
                .join();

        return text
                .replace("%economy_coins%", economyService.format(Currency.COINS, coins))
                .replace("%economy_coins_raw%", coins.toPlainString())
                .replace("%economy_odrys%", economyService.format(Currency.ODRYS, odrys))
                .replace("%economy_odrys_raw%", odrys.toPlainString());
    }

    private String applyEconomyFallbacks(String text) {
        return text
                .replace("%economy_coins%", "$0.00")
                .replace("%economy_coins_raw%", "0.00")
                .replace("%economy_odrys%", "0 Odrys")
                .replace("%economy_odrys_raw%", "0");
    }

    private String applyChunkPlaceholders(Player player, String text) {
        ChunkPreloadTaskView task = activeChunkTaskFor(player);

        if (task == null) {
            return applyChunkFallbacks(text);
        }

        return text
                .replace("%chunks_state%", task.state().name())
                .replace("%chunks_world%", task.worldId())
                .replace("%chunks_radius%", String.valueOf(task.radius()))
                .replace("%chunks_processed%", formatNumber(task.processedChunks()))
                .replace("%chunks_total%", formatNumber(task.totalChunks()))
                .replace("%chunks_progress%", String.format("%.2f", task.progress()))
                .replace("%chunks_current_chunk_x%", String.valueOf(task.currentChunkX()))
                .replace("%chunks_current_chunk_z%", String.valueOf(task.currentChunkZ()));
    }

    private String applyChunkFallbacks(String text) {
        return text
                .replace("%chunks_state%", "NONE")
                .replace("%chunks_world%", "N/A")
                .replace("%chunks_radius%", "0")
                .replace("%chunks_processed%", "0")
                .replace("%chunks_total%", "0")
                .replace("%chunks_progress%", "0.00")
                .replace("%chunks_current_chunk_x%", "0")
                .replace("%chunks_current_chunk_z%", "0");
    }

    private ChunkPreloadTaskView activeChunkTaskFor(Player player) {
        ChunkPreloadService chunkService = chunkService();

        if (chunkService == null) {
            return null;
        }

        String worldName = player.getWorld().getName();

        return chunkService.tasks()
                .stream()
                .filter(task -> task.worldName().equalsIgnoreCase(worldName)
                        || task.worldId().equalsIgnoreCase(worldName))
                .findFirst()
                .orElseGet(() -> chunkService.tasks()
                        .stream()
                        .filter(task -> task.running() || task.paused())
                        .findFirst()
                        .orElse(null));
    }

    private ChunkPreloadService chunkService() {
        try {
            return module.services().require(ChunkPreloadService.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatNumber(long value) {
        return String.format("%,d", value);
    }

    private EconomyService economy() {
        try {
            return module.services().require(EconomyService.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Optional<ProfileView> profile(Player player) {
        try {
            ProfileService profileService = module.services().require(ProfileService.class);
            return profileService.profile(player);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private long requiredExperience(ProfileView profile) {
        try {
            ExperienceProvider experienceProvider = module.services().require(ExperienceProvider.class);
            return experienceProvider.requiredExperience(profile.level());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int experienceProgress(long currentExperience, long requiredExperience) {
        if (requiredExperience <= 0) {
            return 0;
        }

        double progress = (double) currentExperience / (double) requiredExperience;
        return (int) Math.max(0, Math.min(100, Math.round(progress * 100)));
    }

    private String formatPlaytime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }

        return secs + "s";
    }
}