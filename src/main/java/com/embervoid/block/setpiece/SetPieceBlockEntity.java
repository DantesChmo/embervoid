package com.embervoid.block.setpiece;

import com.embervoid.block.setpiece.SetPieceKind.Mode;
import com.embervoid.block.setpiece.SetPieceKind.Puff;
import com.embervoid.block.setpiece.SetPieceKind.Reaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The block entity of every set piece (PLAN 20.1). Holds the mode, the state and small options in NBT, synced to the
 * clients through the update packet; the GeckoLib controller on the client picks the loop from them, actions go
 * through GeckoLib's triggered animations. No server ticking: timed states use a scheduled block tick.
 */
public class SetPieceBlockEntity extends BlockEntity implements GeoBlockEntity {
	public static final String CONTROLLER = "main";
	/** Set by the client: plays the animation files' sound keyframes ({@code creak}, {@code toll}, ...). */
	public static Consumer<SoundKeyframeEvent<SetPieceBlockEntity>> soundHandler = event -> {};
	private static final Map<String, RawAnimation> LOOPS = new ConcurrentHashMap<>();

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private Mode mode;
	private String state;
	private boolean bones = true;
	@Nullable
	private List<Reaction> reactionOverrides;
	/** Per-position phase so pieces of one kind do not move in step. */
	private final double phase;
	/** Test hooks (not saved): the last arena event received, how many actions were triggered. */
	private String lastEvent = "";
	private int actionsTriggered;
	private int clientAge;

	public SetPieceBlockEntity(BlockPos pos, BlockState state) {
		super(SetPieces.BLOCK_ENTITY, pos, state);
		SetPieceKind kind = kind();
		this.mode = kind.defaultMode();
		this.state = kind.defaultState();
		this.phase = (Math.floorMod(pos.getX() * 31L + pos.getZ() * 17L + pos.getY() * 7L, 97L)) * 1.37;
	}

	public SetPieceKind kind() {
		return getCachedState().getBlock() instanceof SetPieceBlock block ? block.kind() : SetPieces.fallbackKind();
	}

	public Mode mode() {
		return mode;
	}

	public String state() {
		return state;
	}

	public boolean showsBones() {
		return bones;
	}

	public String lastEvent() {
		return lastEvent;
	}

	public int actionsTriggered() {
		return actionsTriggered;
	}

	/** The reactions this piece uses: a template's override or the kind's table. */
	public List<Reaction> reactions() {
		return reactionOverrides != null ? reactionOverrides : kind().reactions();
	}

	// --- Server-side changes ---

	public void setMode(Mode mode) {
		if (this.mode != mode) {
			this.mode = mode;
			sync();
		}
	}

	/** Sets a state the kind knows; others are ignored. Returns whether it changed. */
	public boolean setState(String state) {
		if (!kind().hasState(state) || this.state.equals(state)) {
			return false;
		}
		this.state = state;
		if (world != null && !world.isClient && getCachedState().contains(LitSetPieceBlock.LIT)) {
			boolean lit = state.equals(kind().litState());
			if (getCachedState().get(LitSetPieceBlock.LIT) != lit) {
				world.setBlockState(pos, getCachedState().with(LitSetPieceBlock.LIT, lit), Block.NOTIFY_ALL);
			}
		}
		sync();
		return true;
	}

	/** Sets a state for {@code ticks} server ticks, then back to the default state. */
	public void setStateFor(String state, int ticks) {
		if (setState(state) || !state.equals(kind().defaultState())) {
			if (world instanceof ServerWorld server) {
				server.scheduleBlockTick(pos, getCachedState().getBlock(), Math.max(1, ticks));
			}
		}
	}

	void timedStateOver() {
		setState(kind().defaultState());
	}

	public void setBones(boolean bones) {
		if (this.bones != bones) {
			this.bones = bones;
			sync();
		}
	}

	/** Plays a one-shot action on every client watching this piece. */
	public void playAction(String action) {
		if (!kind().actions().contains(action) || world == null) {
			return;
		}
		actionsTriggered++;
		triggerAnim(CONTROLLER, action);
	}

	/** An arena event reached this piece: apply the matching reactions. */
	public void onArenaEvent(String event) {
		lastEvent = event;
		if (kind().followsCampfire() && hasCampfireBelow()) {
			return; // the campfire under it decides; the fight's blocks() restores that
		}
		for (Reaction reaction : reactions()) {
			if (reaction.matches(event)) {
				apply(reaction.effect());
			}
		}
	}

	/**
	 * Applies an effect: a mode ({@code always} / {@code triggered}), a state, {@code <state> <ticks>},
	 * {@code <state>+<action>}, or an action. Returns false if nothing in it was understood.
	 */
	public boolean apply(String effect) {
		String[] words = effect.trim().split("\\s+");
		if (words.length == 0 || words[0].isEmpty()) {
			return false;
		}
		Mode newMode = Mode.byId(words[0]);
		if (newMode != null) {
			setMode(newMode);
			return true;
		}
		boolean understood = false;
		int ticks = -1;
		if (words.length > 1) {
			try {
				ticks = Integer.parseInt(words[1]);
			} catch (NumberFormatException ignored) {
				// not a timed state
			}
		}
		for (String part : words[0].split("\\+")) {
			SetPieceKind kind = kind();
			if (kind.hasState(part)) {
				if (ticks > 0) {
					setStateFor(part, ticks);
				} else {
					setState(part);
				}
				understood = true;
			} else if (kind.actions().contains(part)) {
				playAction(part);
				understood = true;
			}
		}
		return understood;
	}

	/** The great brazier: lit when the campfire under it burns. */
	void followCampfire() {
		if (world == null || kind().litState() == null) {
			return;
		}
		BlockState below = world.getBlockState(pos.down());
		if (below.getBlock() instanceof CampfireBlock) {
			setState(below.get(CampfireBlock.LIT) ? kind().litState() : kind().defaultState());
		}
	}

	/** Whether a campfire stands under this piece (then arena events do not light it; the campfire does). */
	public boolean hasCampfireBelow() {
		return world != null && world.getBlockState(pos.down()).getBlock() instanceof CampfireBlock;
	}

	private void sync() {
		markDirty();
		if (world != null && !world.isClient) {
			world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
		}
	}

	// --- NBT ---

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.putString("Mode", mode.id());
		nbt.putString("State", state);
		if (!bones) {
			nbt.putBoolean("Bones", false);
		}
		if (reactionOverrides != null) {
			NbtList list = new NbtList();
			for (Reaction reaction : reactionOverrides) {
				list.add(NbtString.of(reaction.toString()));
			}
			nbt.put("Reactions", list);
		}
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		Mode read = Mode.byId(nbt.getString("Mode"));
		mode = read != null ? read : kind().defaultMode();
		String readState = nbt.getString("State");
		state = kind().hasState(readState) ? readState : kind().defaultState();
		bones = !nbt.contains("Bones") || nbt.getBoolean("Bones");
		if (nbt.contains("Reactions", NbtElement.LIST_TYPE)) {
			List<Reaction> overrides = new ArrayList<>();
			for (NbtElement element : nbt.getList("Reactions", NbtElement.STRING_TYPE)) {
				overrides.add(Reaction.parse(element.asString()));
			}
			reactionOverrides = overrides;
		} else {
			reactionOverrides = null;
		}
	}

	@Override
	@Nullable
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return createNbt();
	}

	// --- GeckoLib ---

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		AnimationController<SetPieceBlockEntity> controller = new AnimationController<>(this, CONTROLLER, 4, animation -> {
			if (!SetPieceConfig.get().animate()) {
				return PlayState.STOP;
			}
			String loop = kind().loopFor(mode, state);
			return loop == null ? PlayState.STOP : animation.setAndContinue(LOOPS.computeIfAbsent(loop, name -> RawAnimation.begin().thenLoop(name)));
		});
		for (String action : kind().actions()) {
			controller.triggerableAnim(action, RawAnimation.begin().thenPlay("action." + action));
		}
		controller.setSoundKeyframeHandler(event -> soundHandler.accept(event));
		controllers.add(controller);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object blockEntity) {
		return RenderUtils.getCurrentTick() + phase;
	}

	// --- Client particles ---

	/** Client tick: the kind's puffs (smoke over a lit censer, the bellows' breath). */
	void clientTick() {
		clientAge++;
		World world = this.world;
		if (world == null || !SetPieceConfig.get().animate()) {
			return;
		}
		Direction facing = getCachedState().get(SetPieceBlock.FACING);
		for (Puff puff : kind().puffs()) {
			if (puff.state() != null && !puff.state().equals(state)) {
				continue;
			}
			if ((clientAge + (int) phase) % Math.max(1, puff.every()) != 0) {
				continue;
			}
			ParticleEffect particle = particle(puff.particle());
			if (particle == null) {
				continue;
			}
			double[] offset = turn(puff.x(), puff.z(), facing);
			double x = pos.getX() + 0.5 + offset[0] + (world.random.nextDouble() - 0.5) * puff.spread();
			double y = pos.getY() + puff.y() + (world.random.nextDouble() - 0.5) * puff.spread() * 0.5;
			double z = pos.getZ() + 0.5 + offset[1] + (world.random.nextDouble() - 0.5) * puff.spread();
			world.addParticle(particle, x, y, z, 0, 0.02 + world.random.nextDouble() * 0.02, 0);
		}
	}

	/** A north-facing offset turned to the block's facing. */
	static double[] turn(double x, double z, Direction facing) {
		return switch (facing) {
			case EAST -> new double[]{-z, x};
			case SOUTH -> new double[]{-x, -z};
			case WEST -> new double[]{z, -x};
			default -> new double[]{x, z};
		};
	}

	@Nullable
	private static ParticleEffect particle(String name) {
		return switch (name) {
			case "smoke" -> ParticleTypes.SMOKE;
			case "large_smoke" -> ParticleTypes.LARGE_SMOKE;
			case "campfire" -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
			case "signal" -> ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
			case "flame" -> ParticleTypes.FLAME;
			case "ember" -> ParticleTypes.LAVA;
			case "soul" -> ParticleTypes.SOUL_FIRE_FLAME;
			case "ash" -> ParticleTypes.ASH;
			default -> null;
		};
	}
}
