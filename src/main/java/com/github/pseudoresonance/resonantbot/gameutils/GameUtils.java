package com.github.pseudoresonance.resonantbot.gameutils;

import com.github.pseudoresonance.resonantbot.CommandManager;
import com.github.pseudoresonance.resonantbot.api.Plugin;
import com.github.pseudoresonance.resonantbot.gameutils.rocketleague.RocketLeagueStats;

public class GameUtils extends Plugin {

	public void onEnable() {
		CommandManager.registerCommand("mc", new MCCommand(), this);
		CommandManager.registerCommand("rl", new RLCommand(), this);
	}
	
	public void onDisable() {
		RocketLeagueStats.shutdownAll();
	}
	
}
