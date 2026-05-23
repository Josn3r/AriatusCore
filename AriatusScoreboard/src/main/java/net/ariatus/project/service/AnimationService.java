package net.ariatus.project.service;

import net.ariatus.project.AriatusScoreboard;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationService {

    private static final String CONFIG_FILE = "animations.yml";

    private final AriatusScoreboard module;

    private final Map<String, AnimationState> animations = new HashMap<>();
    private int ticks;

    public AnimationService(AriatusScoreboard module) {
        this.module = module;
    }

    public void load() {
        animations.clear();

        ConfigurationSection section = module.config(CONFIG_FILE);

        if (section == null) {
            module.logger().warn(module, "animations.yml no pudo cargarse.");
            return;
        }

        for (String animationId : section.getKeys(false)) {
            int updateTicks = section.getInt(animationId + ".update-ticks", 20);
            List<String> texts = section.getStringList(animationId + ".texts");

            if (texts.isEmpty()) {
                module.logger().warn(module, "Animación sin textos: " + animationId);
                continue;
            }

            animations.put(
                    animationId.toLowerCase(),
                    new AnimationState(animationId.toLowerCase(), updateTicks, texts, 0, 0)
            );

            module.logger().info(module, "Animación cargada: " + animationId + " (" + texts.size() + " frames)");
        }
    }

    public void tick() {
        ticks++;

        for (AnimationState state : animations.values()) {
            if (state.updateTicks() <= 0) {
                continue;
            }

            if (ticks % state.updateTicks() != 0) {
                continue;
            }

            int nextFrame = state.frame() + 1;

            if (nextFrame >= state.texts().size()) {
                nextFrame = 0;
            }

            animations.put(
                    state.id(),
                    new AnimationState(
                            state.id(),
                            state.updateTicks(),
                            state.texts(),
                            nextFrame,
                            ticks
                    )
            );
        }
    }

    public String frame(String animationId) {
        if (animationId == null || animationId.isBlank()) {
            return "";
        }

        AnimationState state = animations.get(animationId.toLowerCase());

        if (state == null) {
            return "";
        }

        return state.texts().get(state.frame());
    }

    public boolean exists(String animationId) {
        return animations.containsKey(animationId.toLowerCase());
    }

    public void reload() {
        load();
    }

    private record AnimationState(
            String id,
            int updateTicks,
            List<String> texts,
            int frame,
            int lastTick
    ) {
    }
}