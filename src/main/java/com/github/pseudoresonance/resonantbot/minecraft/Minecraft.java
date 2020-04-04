package com.github.pseudoresonance.resonantbot.minecraft;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.pseudoresonance.resonantbot.api.Plugin;

public class Minecraft extends Plugin {
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public void onEnable() {
		MCCommand.setup(this);
		scheduler.scheduleAtFixedRate(() -> {
			MCCommand.purge();
		}, 12, 12, TimeUnit.HOURS);
	}
	
	public void onDisable() {
		scheduler.shutdownNow();
	}
	
}
