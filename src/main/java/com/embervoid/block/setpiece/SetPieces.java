package com.embervoid.block.setpiece;

import com.embervoid.Embervoid;
import com.embervoid.block.setpiece.SetPieceKind.Mode;
import com.embervoid.registry.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The moving set pieces (PLAN 20.2): blocks with a GeckoLib model that turn, swing, breathe and burn
 * (models and textures by {@code tools/setpiece_art.py}, datagen in {@code client.setpiece.SetPieceDataGen}).
 * {@link #trigger} is the server API; {@link ArenaEvents} delivers a boss fight's events to the pieces of its arena.
 *
 * <p>Models are authored facing north with the block's centre at the model origin, 16 px = 1 block. A piece that
 * hangs or spans past its block renders outside its bounding box; only its own block is solid.
 */
public final class SetPieces {
	private static final Map<String, SetPieceBlock> BY_NAME = new LinkedHashMap<>();
	private static final List<SetPieceKind> KINDS = new ArrayList<>();

	// --- Ashen Fields: the mill and the granary ---

	/** The windmill's sail wheel on its axle block: four blades four blocks long, a tail and rope stays, turning. */
	public static final SetPieceBlock WINDMILL_SAILS = wood("windmill_sails", SetPieceKind.builder("windmill_sails")
			.box(0, 0, 0, 16, 16, 16).big(128));
	/** The same wheel with torn, half-missing sailcloth. */
	public static final SetPieceBlock WINDMILL_SAILS_TORN = wood("windmill_sails_torn", SetPieceKind.builder("windmill_sails_torn")
			.box(0, 0, 0, 16, 16, 16).big(128));
	/** A hoist beam out of a granary wall: pulley, chain and a crate swaying under it. */
	public static final SetPieceBlock GRANARY_HOIST = wood("granary_hoist", SetPieceKind.builder("granary_hoist")
			.box(5, 8, 0, 11, 16, 16).big(64).actions("swing")
			.reacts(ArenaEvents.SUMMON, "swing").reacts(ArenaEvents.BOSS_HIT_HEAVY, "swing"));
	/** Market scales hanging from a beam, swaying. */
	public static final SetPieceBlock HANGING_SCALES = iron("hanging_scales", SetPieceKind.builder("hanging_scales")
			.box(6, 8, 6, 10, 16, 10).big(64).actions("swing")
			.reacts(ArenaEvents.SUMMON, "swing").reacts(ArenaEvents.HAZARD, "swing").reacts(ArenaEvents.BOSS_HIT_HEAVY, "swing"));
	/** The great stone wheel of an ash mill in its frame: still until the storm spins it up. */
	public static final SetPieceBlock ASH_MILL = stone("ash_mill", SetPieceKind.builder("ash_mill").triggered().noIdle()
			.state("still", false).state("spinning", true).actions("spin_up")
			.box(0, 0, 0, 16, 16, 16).big(128)
			.reacts("hazard ash_storm", "spinning+spin_up 160").reacts(ArenaEvents.SUMMON, "spinning+spin_up 80")
			.reacts(ArenaEvents.KILL, "still").reacts(ArenaEvents.RESET, "still"));

	// --- Royal Woods: the hanged ---

	/** An iron cage swinging on its chain, a bundle of bones inside unless the NBT says {@code Bones: false}. */
	public static final SetPieceBlock HANGING_CAGE = iron("hanging_cage", SetPieceKind.builder("hanging_cage")
			.box(6, 0, 6, 10, 16, 10).big(64).actions("swing").bonesToggle()
			.reacts(ArenaEvents.SUMMON, "swing").reacts(ArenaEvents.BOSS_HIT_HEAVY, "swing").reacts(ArenaEvents.HAZARD, "swing"));
	/** A great wooden wheel in a wall or on a gallows post: still, or turning through a fight. */
	public static final SetPieceBlock GREAT_GEAR = wood("great_gear", SetPieceKind.builder("great_gear").triggered().noIdle()
			.state("still", false).state("turning", true)
			.box(0, 0, 0, 16, 16, 16).big(96)
			.reacts(ArenaEvents.SUMMON, "turning").reacts(ArenaEvents.KILL, "still").reacts(ArenaEvents.RESET, "still"));

	// --- Sunken Necropolis: bells, grates, braziers ---

	/** A bronze bell in a yoke: a slow sway, and a toll when the fight says so. */
	public static final SetPieceBlock GREAT_BELL = bronze("great_bell", SetPieceKind.builder("great_bell").triggered()
			.actions("toll").box(2, 10, 2, 14, 16, 14).big(96)
			.reacts(ArenaEvents.SUMMON, "toll").reacts(ArenaEvents.PHASE, "toll").reacts("raise_dead", "toll").reacts(ArenaEvents.KILL, "toll"));
	/** A sluice grate in a wall that lifts when the flood rises. */
	public static final SetPieceBlock SLUICE_GRATE = iron("sluice_grate", SetPieceKind.builder("sluice_grate").triggered().noIdle()
			.state("closed", false).state("open", true).actions("lift")
			.box(0, 0, 6, 16, 16, 10).big(48)
			.reacts("hazard flood", "open+lift").reacts(ArenaEvents.KILL, "closed").reacts(ArenaEvents.RESET, "closed"));
	/** A great iron brazier: cold, or burning with a smoke column. Over a campfire it follows the campfire. */
	public static final SetPieceBlock GREAT_BRAZIER = lit("great_brazier", SetPieceKind.builder("great_brazier").triggered().noIdle()
			.state("cold", false).state("lit", true).lit("lit", 13).followsCampfire()
			.box(1, 0, 1, 15, 16, 15).big(64)
			.puff("lit", "signal", 0, 1.0, 0, 0.3, 3).puff("lit", "ember", 0, 0.9, 0, 0.5, 4)
			.reacts(ArenaEvents.PHASE, "lit").reacts(ArenaEvents.RESET, "cold"));

	// --- Smoldering Citadel: the gate, the forge, the lift ---

	/** The gate in a gatehouse lintel: a portcullis that slams down and winds back up, gear wheels at its sides. */
	public static final SetPieceBlock PORTCULLIS = iron("portcullis", SetPieceKind.builder("portcullis").triggered().noIdle()
			.state("open", true).state("closed", true).actions("slam", "raise")
			.box(0, 0, 0, 16, 16, 16).big(96)
			.reacts(ArenaEvents.SUMMON, "closed+slam").reacts(ArenaEvents.KILL, "open+raise").reacts(ArenaEvents.RESET, "open+raise"));
	/** Forge bellows breathing, a puff of smoke and sparks from the nozzle. */
	public static final SetPieceBlock FORGE_BELLOWS = wood("forge_bellows", SetPieceKind.builder("forge_bellows")
			.box(1, 0, 0, 15, 10, 16).big(48)
			.puff(null, "soot", 0, 0.45, -0.95, 0.2, 60).puff(null, "spark", 0, 0.45, -0.95, 0.1, 60));
	/** A winch: a drum turning, its chain paying out with a bucket on the end. */
	public static final SetPieceBlock CHAIN_WINCH = wood("chain_winch", SetPieceKind.builder("chain_winch")
			.box(2, 0, 2, 14, 16, 14).big(64));

	// --- Ember Sanctum and the hub ---

	/** A censer on a chain: swaying, smoking once it is lit. */
	public static final SetPieceBlock CENSER = lit("censer", SetPieceKind.builder("censer").triggered()
			.state("cold", false).state("lit", false).lit("lit", 10)
			.box(5, 0, 5, 11, 16, 11).big(48)
			.puff("lit", "soot", 0, 0.2, 0, 0.15, 3)
			.reacts(ArenaEvents.PHASE, "lit").reacts(ArenaEvents.RESET, "cold"));

	public static final List<SetPieceBlock> ALL = List.copyOf(BY_NAME.values());

	public static final BlockEntityType<SetPieceBlockEntity> BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
			Embervoid.id("set_piece"), FabricBlockEntityTypeBuilder.create(SetPieceBlockEntity::new, ALL.toArray(Block[]::new)).build());

	public static void register() {
		Embervoid.LOGGER.debug("Registering set pieces");
		Objects.requireNonNull(SetPieceConfig.SECTION);
		SetPieceCap.register();
	}

	public static List<SetPieceKind> kinds() {
		return List.copyOf(KINDS);
	}

	public static SetPieceBlock byName(String name) {
		SetPieceBlock block = BY_NAME.get(name);
		if (block == null) {
			throw new IllegalArgumentException("no set piece " + name);
		}
		return block;
	}

	static SetPieceKind fallbackKind() {
		return KINDS.get(0);
	}

	/**
	 * Tells the set piece at {@code pos} to do something: {@code always} / {@code triggered} (its mode), a state name
	 * ({@code open}, {@code lit}, ...), {@code <state> <ticks>} (that state for a while, then its default), an action
	 * name ({@code toll}, {@code swing}) or {@code <state>+<action>}. Returns false if there is no set piece there or
	 * it did not understand the event.
	 */
	public static boolean trigger(ServerWorld world, BlockPos pos, String event) {
		return world.getBlockEntity(pos) instanceof SetPieceBlockEntity piece && piece.apply(event);
	}

	/**
	 * Every great bell within {@code radius} blocks of {@code pos} (in loaded chunks) tolls: a Grave Warden ringing its
	 * hand bell under a chapel's bell.
	 */
	public static int tollBellsNear(ServerWorld world, BlockPos pos, double radius) {
		return ArenaEvents.fire(world, pos, (int) Math.ceil(radius), "raise_dead");
	}

	/** The block entity of a set piece at {@code pos}, or null. */
	public static SetPieceBlockEntity at(ServerWorld world, BlockPos pos) {
		return world.getBlockEntity(pos) instanceof SetPieceBlockEntity piece ? piece : null;
	}

	/** Whether the piece's default mode is {@link Mode#ALWAYS}. */
	public static boolean isAlways(SetPieceBlock block) {
		return block.kind().defaultMode() == Mode.ALWAYS;
	}

	private static SetPieceBlock wood(String name, SetPieceKind.Builder kind) {
		return add(name, kind.build(), settings(MapColor.BROWN, 2.0f, BlockSoundGroup.WOOD), false);
	}

	private static SetPieceBlock iron(String name, SetPieceKind.Builder kind) {
		return add(name, kind.build(), settings(MapColor.IRON_GRAY, 3.0f, BlockSoundGroup.CHAIN), false);
	}

	private static SetPieceBlock bronze(String name, SetPieceKind.Builder kind) {
		return add(name, kind.build(), settings(MapColor.GOLD, 3.0f, BlockSoundGroup.COPPER), false);
	}

	private static SetPieceBlock stone(String name, SetPieceKind.Builder kind) {
		return add(name, kind.build(), settings(MapColor.STONE_GRAY, 3.5f, BlockSoundGroup.STONE), false);
	}

	private static SetPieceBlock lit(String name, SetPieceKind.Builder kind) {
		return add(name, kind.build(), settings(MapColor.IRON_GRAY, 3.0f, BlockSoundGroup.CHAIN), true);
	}

	private static AbstractBlock.Settings settings(MapColor color, float strength, BlockSoundGroup sounds) {
		return AbstractBlock.Settings.create().mapColor(color).strength(strength).sounds(sounds).nonOpaque()
				.pistonBehavior(PistonBehavior.BLOCK);
	}

	private static SetPieceBlock add(String name, SetPieceKind kind, AbstractBlock.Settings settings, boolean lit) {
		SetPieceBlock block = lit ? new LitSetPieceBlock(settings, kind) : new SetPieceBlock(settings, kind);
		ModBlocks.register(name, block);
		BY_NAME.put(name, block);
		KINDS.add(kind);
		return block;
	}

	private SetPieces() {}
}
