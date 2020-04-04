package com.github.pseudoresonance.resonantbot.minecraft;

import com.github.pseudoresonance.resonantbot.api.Plugin;

public class Minecraft extends Plugin {

	public void onEnable() {
		MCCommand.setup(this);
	}
	
	public void onDisable() {
	}
	
}
