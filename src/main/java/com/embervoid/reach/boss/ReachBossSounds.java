package com.embervoid.reach.boss;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Realm 1 bosses and the Ash of Rematch (R40.3a): vanilla files remapped in sounds.json. */
public final class ReachBossSounds {
	public static final SoundEvent ASH_COLOSSUS_AMBIENT = ModSounds.register("entity.ash_colossus.ambient");
	public static final SoundEvent ASH_COLOSSUS_HURT = ModSounds.register("entity.ash_colossus.hurt");
	public static final SoundEvent ASH_COLOSSUS_DEATH = ModSounds.register("entity.ash_colossus.death");
	public static final SoundEvent ASH_COLOSSUS_STEP = ModSounds.register("entity.ash_colossus.step");
	public static final SoundEvent ASH_COLOSSUS_FIST = ModSounds.register("entity.ash_colossus.fist");
	public static final SoundEvent ASH_COLOSSUS_DRAG = ModSounds.register("entity.ash_colossus.drag");
	public static final SoundEvent ASH_COLOSSUS_LEAP = ModSounds.register("entity.ash_colossus.leap");
	public static final SoundEvent ASH_COLOSSUS_BOULDER = ModSounds.register("entity.ash_colossus.boulder");
	public static final SoundEvent ASH_COLOSSUS_SMASH = ModSounds.register("entity.ash_colossus.smash");
	public static final SoundEvent ASH_COLOSSUS_STORM = ModSounds.register("entity.ash_colossus.storm");
	public static final SoundEvent ASH_COLOSSUS_SHED = ModSounds.register("entity.ash_colossus.shed");

	public static final SoundEvent GRIEVING_TREE_AMBIENT = ModSounds.register("entity.grieving_tree.ambient");
	public static final SoundEvent GRIEVING_TREE_HURT = ModSounds.register("entity.grieving_tree.hurt");
	public static final SoundEvent GRIEVING_TREE_DEATH = ModSounds.register("entity.grieving_tree.death");
	public static final SoundEvent GRIEVING_TREE_STEP = ModSounds.register("entity.grieving_tree.step");
	public static final SoundEvent GRIEVING_TREE_ROOT_LINES = ModSounds.register("entity.grieving_tree.root_lines");
	public static final SoundEvent GRIEVING_TREE_BRANCH_SWEEP = ModSounds.register("entity.grieving_tree.branch_sweep");
	public static final SoundEvent GRIEVING_TREE_ROOT_CAGE = ModSounds.register("entity.grieving_tree.root_cage");
	public static final SoundEvent GRIEVING_TREE_UPROOT = ModSounds.register("entity.grieving_tree.uproot");
	public static final SoundEvent GRIEVING_TREE_STUN = ModSounds.register("entity.grieving_tree.stun");
	public static final SoundEvent GRIEVING_TREE_THRALLS = ModSounds.register("entity.grieving_tree.thralls");

	public static final SoundEvent TREE_LANTERN_SHATTER = ModSounds.register("entity.tree_lantern.shatter");

	public static final SoundEvent CRYPT_KEEPER_AMBIENT = ModSounds.register("entity.crypt_keeper.ambient");
	public static final SoundEvent CRYPT_KEEPER_HURT = ModSounds.register("entity.crypt_keeper.hurt");
	public static final SoundEvent CRYPT_KEEPER_DEATH = ModSounds.register("entity.crypt_keeper.death");
	public static final SoundEvent CRYPT_KEEPER_STEP = ModSounds.register("entity.crypt_keeper.step");
	public static final SoundEvent CRYPT_KEEPER_SWING = ModSounds.register("entity.crypt_keeper.swing");
	public static final SoundEvent CRYPT_KEEPER_OVERHEAD = ModSounds.register("entity.crypt_keeper.overhead");
	public static final SoundEvent CRYPT_KEEPER_SWEEP = ModSounds.register("entity.crypt_keeper.sweep");
	public static final SoundEvent CRYPT_KEEPER_LANTERN_THROW = ModSounds.register("entity.crypt_keeper.lantern_throw");
	public static final SoundEvent CRYPT_KEEPER_THRUST = ModSounds.register("entity.crypt_keeper.thrust");
	public static final SoundEvent CRYPT_KEEPER_LANTERN_SHATTER = ModSounds.register("entity.crypt_keeper.lantern_shatter");
	public static final SoundEvent CRYPT_KEEPER_RAISE_DEAD = ModSounds.register("entity.crypt_keeper.raise_dead");

	public static final SoundEvent SMOLDERING_LORD_AMBIENT = ModSounds.register("entity.smoldering_lord.ambient");
	public static final SoundEvent SMOLDERING_LORD_HURT = ModSounds.register("entity.smoldering_lord.hurt");
	public static final SoundEvent SMOLDERING_LORD_DEATH = ModSounds.register("entity.smoldering_lord.death");
	public static final SoundEvent SMOLDERING_LORD_STEP = ModSounds.register("entity.smoldering_lord.step");
	public static final SoundEvent SMOLDERING_LORD_MORTAR = ModSounds.register("entity.smoldering_lord.mortar");
	public static final SoundEvent SMOLDERING_LORD_MORTAR_IMPACT = ModSounds.register("entity.smoldering_lord.mortar_impact");
	public static final SoundEvent SMOLDERING_LORD_FLAME_LINE = ModSounds.register("entity.smoldering_lord.flame_line");
	public static final SoundEvent SMOLDERING_LORD_SWING = ModSounds.register("entity.smoldering_lord.swing");
	public static final SoundEvent SMOLDERING_LORD_SLAM = ModSounds.register("entity.smoldering_lord.slam");
	public static final SoundEvent SMOLDERING_LORD_LUNGE = ModSounds.register("entity.smoldering_lord.lunge");
	public static final SoundEvent SMOLDERING_LORD_DESCEND = ModSounds.register("entity.smoldering_lord.descend");
	public static final SoundEvent SMOLDERING_LORD_SHATTER = ModSounds.register("entity.smoldering_lord.shatter");
	public static final SoundEvent SMOLDERING_LORD_STORM = ModSounds.register("entity.smoldering_lord.storm");
	public static final SoundEvent SMOLDERING_LORD_FOG = ModSounds.register("entity.smoldering_lord.fog");
	public static final SoundEvent SMOLDERING_LORD_DARKNESS = ModSounds.register("entity.smoldering_lord.darkness");

	public static final SoundEvent ASH_OF_REMATCH_USE = ModSounds.register("item.ash_of_rematch.use");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private ReachBossSounds() {}
}
