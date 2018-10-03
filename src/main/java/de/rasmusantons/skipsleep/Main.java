package de.rasmusantons.skipsleep;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	@Override
	public void onEnable() {
		PlayerSleepHandler playerSleepHandler = new PlayerSleepHandler(this);
		getServer().getPluginManager().registerEvents(playerSleepHandler, this);
		getCommand("blocksleep").setExecutor(playerSleepHandler);
		getCommand("unblocksleep").setExecutor(playerSleepHandler);
	}
}
