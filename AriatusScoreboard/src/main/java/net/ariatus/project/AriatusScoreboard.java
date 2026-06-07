package net.ariatus.project;

import net.ariatus.project.command.AriatusScoreboardCommand;
import net.ariatus.project.listener.ScoreboardListener;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.service.AnimationService;
import net.ariatus.project.service.PlaceholderService;
import net.ariatus.project.service.ScoreboardService;
import org.bukkit.Bukkit;

public class AriatusScoreboard extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;
    private ScoreboardService scoreboardService;
    private PlaceholderService placeholderService;
    private AnimationService animationService;

    @Override
    public String id() {
        return "scoreboard";
    }

    @Override
    public String name() {
        return "AriatusScoreboard";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");
        loadConfig("nametags.yml");
        loadConfig("belowname.yml");
        loadConfig("animations.yml");

        this.animationService = new AnimationService(this);
        this.animationService.load();

        this.placeholderService = new PlaceholderService(this, animationService);

        this.scoreboardService = new ScoreboardService(this, placeholderService, animationService);
        this.scoreboardService.start();

        commands().register(this, new AriatusScoreboardCommand(this, scoreboardService));
        listeners().register(this, new ScoreboardListener(scoreboardService));

        Bukkit.getOnlinePlayers().forEach(scoreboardService::create);

        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusScoreboard activado.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        if (scoreboardService != null) {
            scoreboardService.shutdown();
        }

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusScoreboard desactivado.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}