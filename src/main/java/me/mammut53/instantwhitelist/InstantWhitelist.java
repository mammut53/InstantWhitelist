package me.mammut53.instantwhitelist;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public final class InstantWhitelist extends JavaPlugin {

    private static Plugin PLUGIN;
    private static Logger LOGGER;

    @Override
    public void onEnable() {
        PLUGIN = this;
        LOGGER = PLUGIN.getLogger();

        new Thread(() -> {
            WatchService watchService = null;
            try {
                watchService = FileSystems.getDefault().newWatchService();
                getServer().getWorldContainer().toPath().register(watchService, ENTRY_MODIFY);
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean poll = true;
            while(poll) {

                WatchKey key = null;
                try {
                    assert watchService != null;
                    key = watchService.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                assert key != null;
                for(WatchEvent<?> event : key.pollEvents()) {

                    if(!event.context().toString().equals("whitelist.json")) {
                        continue;
                    }

                    LOGGER.fine("Modification of whitelist.json detected, reloading whitelist and kicking trespassing players");

                    Bukkit.getScheduler().runTask(PLUGIN, () -> {
                        Bukkit.reloadWhitelist();

                        for(Player player : Bukkit.getOnlinePlayers()) {
                            if(player.isWhitelisted()) {
                                continue;
                            }

                            // This is intentionally a normal text and not a translation string
                            //
                            // Explanation: The notchian client does not have a translation string for
                            // this message. The notchian server as well as Spigot, sends this message
                            // always in English as a normal text.
                            player.kick(Component.text("You are not whitelisted on this server!"));
                        }
                    });
                }

                poll = key.reset();
            }
        }).start();
    }

}
