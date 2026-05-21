package net.ariatus.project.module.internal;

import net.ariatus.project.command.AriatusModuleCommand;
import net.ariatus.project.message.MessageService;
import org.bukkit.command.CommandSender;

public class TestModuleCommand implements AriatusModuleCommand {

    @Override
    public String name() {
        return "test";
    }

    @Override
    public String description() {
        return "Comando de prueba del TestModule";
    }

    @Override
    public String usage() {
        return "/ariatus test";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        MessageService.send(sender, "<gradient:#8A2BE2:#00D4FF>TestModule</gradient> &7comando ejecutado correctamente.");
        return true;
    }
}