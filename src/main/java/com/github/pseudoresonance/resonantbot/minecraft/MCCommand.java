package com.github.pseudoresonance.resonantbot.minecraft;

import java.awt.Color;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.net.ssl.HttpsURLConnection;

import com.github.pseudoresonance.resonantbot.api.CommandHandler;
import com.github.pseudoresonance.resonantbot.api.Plugin;
import com.github.pseudoresonance.resonantbot.apiplugin.Expirable;
import com.github.pseudoresonance.resonantbot.language.LanguageManager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MCCommand {

	private static CommandHandler cmd = null;
	
	private static ConcurrentHashMap<String, MCPlayer> players = new ConcurrentHashMap<String, MCPlayer>();
	
	private static long lastPurge = 0;

	public static void setup(Plugin plugin) {
		cmd = new CommandHandler("minecraft", "minecraft.mcCommandDescription");
		cmd.registerSubcommand("player", (e, command, args) -> {
			if (System.nanoTime() - lastPurge > 43200000000000L) {
				lastPurge = System.nanoTime();
				purge();
			}
			if (args.length > 0) {
				String uuid = "";
				String name = "";
				LinkedHashMap<String, LocalDateTime> previousNames = new LinkedHashMap<String, LocalDateTime>();
				MCPlayer player = getPlayer(args[0]);
				CompletableFuture<Message> placeholder = null;
				if (player == null) {
					placeholder = e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("main.fetchingData")).submit();
					try {
						URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + args[0]);
						HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
						try (InputStream in = connection.getInputStream()) {
							try (JsonReader jr = Json.createReader(in)) {
								JsonObject jo = jr.readObject();
								uuid = jo.getString("uuid");
								name = jo.getString("username");
								JsonArray usernameHistory = jo.getJsonArray("username_history");
								int min = 0, max = usernameHistory.size();
								if (max > 6)
									min = max - 6;
								for (int i = max - 2; i >= min; i--) {
									JsonObject entry = usernameHistory.getJsonObject(i + 1);
									LocalDateTime date = null;
									if (entry.containsKey("changed_at"))
										date = Instant.parse(entry.getString("changed_at")).atZone(ZoneId.systemDefault()).toLocalDateTime();
									previousNames.put(usernameHistory.getJsonObject(i).getString("username"), date);
								}
							} catch (JsonException ex) {
								e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("minecraft.invalidMinecraftAccount")).queue();
								return true;
							}
						}
					} catch (Exception ex) {
						e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("minecraft.invalidMinecraftAccount")).queue();
						ex.printStackTrace();
						return true;
					}
				} else {
					sendMessage(placeholder, player.getUUID(), player.getName(), player.getPreviousNames(), e);
				}
				if (!uuid.equals("") && !name.equals("")) {
					player = new MCPlayer(uuid, name, previousNames);
					players.put(name.toLowerCase(), player);
					sendMessage(placeholder, uuid, name, previousNames, e);
				}
			} else {
				e.getChannel().sendMessage(LanguageManager.getLanguage(e).getMessage("minecraft.invalidMinecraftAccount")).queue();
			}
			return true;
		});
		cmd.register(plugin);
	}

	private static void sendMessage(CompletableFuture<Message> placeholder, String uuid, String name, LinkedHashMap<String, LocalDateTime> previousNames, MessageReceivedEvent e) {
		EmbedBuilder build = new EmbedBuilder();
		build.setTitle(LanguageManager.getLanguage(e).getMessage("minecraft.minecraftAccountDetails", LanguageManager.escape(name)), "https://namemc.com/profile/" + uuid);
		build.setColor(new Color(0, 255, 0));
		build.setImage("https://visage.surgeplay.com/full/512/" + uuid + ".png");
		build.appendDescription(LanguageManager.getLanguage(e).getMessage("minecraft.uuid", uuid));
		if (previousNames.size() > 0) {
			String prevNamesStr = "";
			Iterator<Entry<String, LocalDateTime>> iter = previousNames.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<String, LocalDateTime> entry = iter.next();
				prevNamesStr += LanguageManager.getLanguage(e).getMessage("minecraft.previousNameEntry", LanguageManager.escape(entry.getKey()), LanguageManager.getLanguage(e).formatDateTime(entry.getValue()));
				if (iter.hasNext())
					prevNamesStr += "\n";
			}
			build.addField(LanguageManager.getLanguage(e).getMessage("minecraft.previousNames"), prevNamesStr, false);
		}
		if (placeholder != null) {
			try {
				Message msg = placeholder.get();
				msg.editMessage(build.build()).override(true).queue();
			} catch (InterruptedException | ExecutionException ex) {
				e.getChannel().sendMessage(build.build()).queue();
			}
		} else
			e.getChannel().sendMessage(build.build()).queue();
	}
	
	public static MCPlayer getPlayer(String name) {
		if (players.containsKey(name.toLowerCase())) {
			MCPlayer player = players.get(name.toLowerCase());
			if (player.isExpired()) {
				players.remove(name.toLowerCase());
			} else
				return player;
		}
		return null;
	}
	
	public static void purge() {
		for (String name : players.keySet()) {
			if (players.get(name).isExpired())
				players.remove(name);
		}
	}
	
	public static class MCPlayer extends Expirable {
		
		private final String uuid;
		private final String name;
		private final LinkedHashMap<String, LocalDateTime> previousNames;
		
		public MCPlayer(String uuid, String name, LinkedHashMap<String, LocalDateTime> previousNames) {
			super(15, TimeUnit.MINUTES);
			this.uuid = uuid;
			this.name = name;
			this.previousNames = previousNames;
		}
		
		public String getUUID() {
			return this.uuid;
		}
		
		public String getName() {
			return this.name;
		}
		
		public LinkedHashMap<String, LocalDateTime> getPreviousNames() {
			return this.previousNames;
		}

	}

}
