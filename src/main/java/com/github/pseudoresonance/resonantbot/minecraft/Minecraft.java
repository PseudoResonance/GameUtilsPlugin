package com.github.pseudoresonance.resonantbot.minecraft;

import com.github.pseudoresonance.resonantbot.CommandManager;
import com.github.pseudoresonance.resonantbot.api.Plugin;

public class Minecraft extends Plugin {

	public void onEnable() {
		CommandManager.registerCommand("mc", new MCCommand(), this);
	}
	
	public void onDisable() {
	}
	
}
