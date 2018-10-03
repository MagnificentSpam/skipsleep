package de.rasmusantons.skipsleep;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class PlayerSleepHandler implements Listener, CommandExecutor {

	private Main main;
	private BukkitTask sleepTask = null;
	private HashSet<UUID> blockingPlayers;

	public PlayerSleepHandler(Main main) {
		this.main = main;
		blockingPlayers = new HashSet<>();
	}

	@EventHandler
	public void onPlayerSleep(PlayerBedEnterEvent event) {
		if (sleepTask != null)
			return;
		BaseComponent[] message = new ComponentBuilder(event.getPlayer().getPlayerListName()
				+ " möchte schlafen - klicke hier, um das zu verhindern")
				.color(net.md_5.bungee.api.ChatColor.GRAY)
				.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/blocksleep"))
				.create();
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			p.setSleepingIgnored(false);
			if (p.equals(event.getPlayer()))
				continue;
			p.spigot().sendMessage(message);
		}
		blockingPlayers.clear();
		sleepTask = Bukkit.getScheduler().runTaskLater(main, new SleepActionRunnable(), 20 * 10);
	}

	@EventHandler
	public void onPlayerSleepCancel(PlayerBedLeaveEvent event) {
		long timeOfDay = event.getPlayer().getWorld().getTime();
		if (sleepTask == null || timeOfDay < 12300 || timeOfDay > 23850)
			return;
		boolean anyPlayerSleeping = false;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (p.isSleeping()) {
				anyPlayerSleeping = true;
				break;
			}
		}
		if (!anyPlayerSleeping) {
			sleepTask.cancel();
			sleepTask = null;
			Bukkit.getServer().broadcastMessage(ChatColor.GRAY
					+ "Schlafen abgebrochen, da niemand mehr schläft");
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (sleepTask == null)
			return;
		BaseComponent[] message = new ComponentBuilder(
				"ein Spieler möchte schlafen - klicke hier, um das zu verhindern")
				.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/blocksleep"))
				.color(net.md_5.bungee.api.ChatColor.GRAY)
				.create();
		event.getPlayer().spigot().sendMessage(message);
	}

	@Override
	public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
		if (!(commandSender instanceof Player)) {
			commandSender.sendMessage("nur für Players");
			return true;
		}
		Player player = (Player) commandSender;
		if (sleepTask == null) {
			player.sendMessage(ChatColor.RED + "Es schläft niemand!");
			return true;
		}
		if ("blocksleep".equals(command.getName())) {
			if (blockingPlayers.contains(player.getUniqueId())) {
				player.sendMessage(ChatColor.RED + "Du blockierst Schlafen bereits!");
				return true;
			}
			blockingPlayers.add(player.getUniqueId());
			if (player.isSleepingIgnored())
				player.setSleepingIgnored(false);
			Bukkit.getServer().broadcastMessage(String.format("%s%s blockiert Schlafen",
					ChatColor.GRAY, commandSender.getName()));
			BaseComponent[] message = new ComponentBuilder("klicke hier, um das Schlafen zu erlauben")
					.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/unblocksleep"))
					.color(net.md_5.bungee.api.ChatColor.GRAY)
					.create();
			player.spigot().sendMessage(message);
			return true;
		} else if ("unblocksleep".equals(command.getName())) {
			if (!blockingPlayers.contains(player.getUniqueId())) {
				player.sendMessage(ChatColor.RED + "Du blockierst Schlafen nicht!");
				return true;
			}
			blockingPlayers.remove(player.getUniqueId());
			Bukkit.getServer().broadcastMessage(String.format("%s%s blockiert Schlafen nicht mehr",
					ChatColor.GRAY, commandSender.getName()));
			return true;
		} else {
			Bukkit.getServer().broadcastMessage(String.format("%s used the invalid command \"%s\"", player.getPlayerListName(), command.getName()));
		}
		return false;
	}

	private class SleepActionRunnable implements Runnable {
		@Override public void run() {
			boolean someoneBlocksSleep = false;
			for (Player p : Bukkit.getServer().getOnlinePlayers()) {
				if (blockingPlayers.contains(p.getUniqueId())) {
					someoneBlocksSleep = true;
				} else {
					p.setSleepingIgnored(true);
				}
			}
			if (someoneBlocksSleep) {
				sleepTask = Bukkit.getScheduler().runTaskLater(main, new SleepActionRunnable(), 20);
			} else {
				sleepTask = null;
			}
		}
	}
}
