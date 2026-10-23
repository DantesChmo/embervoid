package com.embervoid;

import com.embervoid.block.prop.Props;
import com.embervoid.command.ModCommands;
import com.embervoid.command.PerfCommands;
import com.embervoid.config.ConfigSync;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.entity.mob.MobEntities;
import com.embervoid.hub.Hub;
import com.embervoid.hub.stele.CleftStele;
import com.embervoid.mob.MobLevels;
import com.embervoid.moon.MoonInit;
import com.embervoid.nightmob.NightMobs;
import com.embervoid.player.FlaskKeeper;
import com.embervoid.player.FlaskRefill;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.RemnantDeathHandler;
import com.embervoid.player.StaminaManager;
import com.embervoid.reach.boss.ReachBosses;
import com.embervoid.reach.citadel.Citadel;
import com.embervoid.reach.gear.ReachGear;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.realm.RealmInit;
import com.embervoid.registry.DebugItems;
import com.embervoid.registry.ModBlocks;
import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItemGroups;
import com.embervoid.registry.ModItems;
import com.embervoid.registry.ModSounds;
import com.embervoid.registry.ModStructures;
import com.embervoid.reliquary.ReliquaryInit;
import com.embervoid.sanctum.Sanctum;
import com.embervoid.stats.StatEffects;
import com.embervoid.vampire.VampireInit;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Embervoid implements ModInitializer {
	public static final String MOD_ID = "embervoid";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		EmbervoidConfig.load();

		// Order matters: blocks before items (BlockItems), items before item groups.
		ModSounds.register();
		ModBlocks.register();
		Props.register();
		ModItems.register();
		DebugItems.register();
		ModEntities.register();
		MobEntities.register();
		MobLevels.register();
		NightMobs.register();
		ReachMobs.register();
		ModItemGroups.register();
		ModStructures.register();
		ConfigSync.register();
		PlayerDataManager.register();
		StaminaManager.register();
		FlaskRefill.register();
		FlaskKeeper.register();
		RemnantDeathHandler.register();
		StatEffects.register();
		ModCommands.register();
		PerfCommands.register();
		RealmInit.register();
		// Also registers the progress API.
		Sanctum.register();
		Hub.register();
		CleftStele.register();
		MoonInit.register();
		VampireInit.register();
		ReachGear.register();
		ReliquaryInit.register();
		ReachBosses.register();
		Citadel.register();

		LOGGER.info("Embervoid: the embers stir...");
	}
}
