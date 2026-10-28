package com.embervoid.client.reach.gear;

import com.embervoid.Embervoid;
import com.embervoid.anim.WeaponKind;
import com.embervoid.client.anim.AttackAnimationsClient;
import com.embervoid.fx.particle.EmberParticles;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.reach.gear.WardensScourgeItem;
import com.embervoid.reach.gear.WhipArc;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The Warden's Scourge's lash, drawn on the client for every player holding one: a chain of iron links
 * from the handle in the hand, a rope simulated in the world (gravity, drag, links kept at their length, resting on
 * the ground). Held, under a block of it hangs from the hand. A swing (the attack-clip
 * packet, so other players' swings too) pays it out and lays it along {@link WhipArc#swing}, the same curve the server
 * hits along, rolling out over the right shoulder until the tip cracks; then the rope is let go, keeps its speed,
 * falls and is coiled back in. Lash flicks it straight out and yanks it back.
 *
 * <p>The lash starts where the handle's tip is drawn: {@code GearItemRendererMixin} hands over the matrix the in-hand
 * model is drawn with. In third person that is in the world pass (camera-relative, turned by the camera); in first
 * person the hand is drawn after the world with its own projection, so its point is carried into the world pass's
 * projection one frame later. Physics and drawing run after the entities (the lash is depth-tested against the world).
 */
public final class ScourgeLashes {
	/** The Scourge's model in a hand: the handle; the inventory icon is the whole coiled whip. */
	public static final Identifier IN_HAND_MODEL = Embervoid.id("item/wardens_scourge_in_hand");
	private static final Identifier TEXTURE = Embervoid.id("textures/entity/wardens_scourge_lash.png");
	/** Where the lash leaves the handle in the in-hand model: the tip ring of tools/weapon_model_art.py's handle, mid-depth. */
	private static final Vector4f ROOT = new Vector4f(8.8f / 16, 8.8f / 16, 0.5f, 1);
	private static final int LINKS = 20;
	/** Lash length hanging from the hand when nothing is swung, blocks. */
	private static final double IDLE_LENGTH = 0.8;
	/** Ticks a crack takes to pay the lash out to its full length (it comes over meanwhile). */
	private static final float PAYOUT_TICKS = 2.5f;
	/** How fast the lash is coiled back in once let go, blocks per tick. */
	private static final double RETRACT_SPEED = 0.6;
	/** Lash's yank back: the speed it is given toward the hand when let go, blocks per tick. */
	private static final double YANK = 0.8;
	/** Physics step in ticks, and the most steps a frame runs (a long frame skips time). */
	private static final float STEP = 0.25f;
	private static final int MAX_STEPS = 16;
	private static final double GRAVITY = 0.06;
	/** Share of speed kept per tick. */
	private static final double DRAG = 0.9;
	private static final int ITERATIONS = 12;
	/** The lash's half-thickness against the ground. */
	private static final double THICKNESS = 0.03;
	/** Width at the handle and at the tip, blocks; the popper at the tip. */
	private static final float WIDTH_ROOT = 0.075f;
	private static final float WIDTH_TIP = 0.035f;
	private static final float WIDTH_POPPER = 0.06f;
	/** Blocks of lash one repeat of the link texture covers. */
	private static final double TEXTURE_LENGTH = 0.3;
	/** A lash not drawn for this many frames starts over hanging when seen again. */
	private static final int STALE_FRAMES = 20;
	/** The heat pass: how much wider than the links the glow is drawn, and its colours from the handle to the tip. */
	private static final float HEAT_WIDTH = 2.6f;
	private static final float[] HEAT_ROOT = {0.85f, 0.25f, 0.08f};
	private static final float[] HEAT_TIP = {1.0f, 0.78f, 0.40f};
	private static final float[] FLASH = {1.0f, 0.96f, 0.82f};
	/** How fast the heat and the crack's flash fade, per tick. */
	private static final float HEAT_FADE = 0.10f;
	private static final float FLASH_FADE = 0.30f;

	private static final Map<Integer, Lash> LASHES = new HashMap<>();
	@Nullable
	private static LivingEntity holder;
	private static int frame;

	public static void register() {
		AttackAnimationsClient.listen((player, clip, ticks) -> {
			if (clip.equals(WeaponKind.WHIP.swings().get(0).id())) {
				lash(player).swing(Swing.CRACK, now(player.getWorld(), 0));
			} else if (WeaponKind.WHIP.special() != null && clip.equals(WeaponKind.WHIP.special().id())) {
				lash(player).swing(Swing.LASH, now(player.getWorld(), 0));
			}
		});
		WorldRenderEvents.START.register(context -> frame++);
		WorldRenderEvents.AFTER_ENTITIES.register(ScourgeLashes::render);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LASHES.clear());
	}

	/** Whether {@code stack} is drawn with {@link #IN_HAND_MODEL} in {@code mode}. */
	public static boolean inHand(ItemStack stack, ModelTransformationMode mode) {
		return stack.isOf(GearWeapons.WARDENS_SCOURGE) && InHandModels.isHand(mode);
	}

	/** The entity whose held item is being drawn ({@code null} after it), from {@code GearItemRendererMixin}. */
	public static void holding(@Nullable LivingEntity entity) {
		holder = entity;
	}

	/**
	 * The in-hand model of {@code stack} is being drawn with {@code pose} (the model's own units, after its display
	 * transform): if it is the main-hand Scourge of the entity being drawn, the lash starts at its handle's tip.
	 */
	public static void drawn(ItemStack stack, ModelTransformationMode mode, Matrix4f pose) {
		if (holder == null || !inHand(stack, mode) || !holder.getMainHandStack().isOf(GearWeapons.WARDENS_SCOURGE)) {
			return;
		}
		boolean rightHand = mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
		if (rightHand != (holder.getMainArm() == Arm.RIGHT)) {
			return;
		}
		Vector4f at = pose.transform(new Vector4f(ROOT));
		Lash lash = lash(holder);
		if (mode.isFirstPerson()) {
			lash.handView = new Vector3f(at.x, at.y, at.z);
			lash.handProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
			lash.handFrame = frame;
		} else {
			lash.view = new Vector3f(at.x, at.y, at.z);
			lash.viewFrame = frame;
		}
	}

	private static Lash lash(Entity entity) {
		return LASHES.computeIfAbsent(entity.getId(), id -> new Lash());
	}

	private static float now(World world, float tickDelta) {
		return world.getTime() + tickDelta;
	}

	private static void render(WorldRenderContext context) {
		ClientWorld world = context.world();
		if (LASHES.isEmpty() || world == null) {
			return;
		}
		float tickDelta = context.tickDelta();
		float now = now(world, tickDelta);
		Vec3d camera = context.camera().getPos();
		Matrix4f view = context.matrixStack().peek().getPositionMatrix();
		Matrix3f normals = context.matrixStack().peek().getNormalMatrix();
		Matrix4f toWorld = new Matrix4f(view).invert();
		Matrix4f fromClip = new Matrix4f(context.projectionMatrix()).invert();
		VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
		RenderLayer layer = RenderLayer.getEntityCutoutNoCull(TEXTURE);
		VertexConsumer buffer = immediate.getBuffer(layer);
		VertexConsumerProvider.Immediate effects = MinecraftClient.getInstance().getBufferBuilders().getEffectVertexConsumers();
		VertexConsumer glow = effects.getBuffer(RenderLayer.getLightning());
		for (Iterator<Map.Entry<Integer, Lash>> it = LASHES.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Lash> entry = it.next();
			if (!(world.getEntityById(entry.getKey()) instanceof AbstractClientPlayerEntity player) || !player.isAlive()
					|| !WardensScourgeItem.holds(player)) {
				it.remove();
				continue;
			}
			Lash lash = entry.getValue();
			Vec3d anchor = lash.anchor(camera, toWorld, fromClip);
			if (anchor == null) {
				continue;
			}
			lash.update(world, player, anchor, now, tickDelta);
			lash.draw(world, buffer, view, normals, camera);
			lash.drawHeat(glow, view, camera);
		}
		immediate.draw(layer);
		effects.draw(RenderLayer.getLightning());
	}

	private enum Swing {
		/** The crack: over the right shoulder, rolling out onto the hit curve; let go once the tip has overshot. */
		CRACK(WhipArc.CRACK_WINDUP, 0, 9),
		/** Lash: a short flick straight out, then yanked back and coiled in at once. */
		LASH(WhipArc.LASH_WINDUP, YANK, 5);

		final float windup;
		final double yank;
		/** Ticks after the swing starts until the lash is coiled back in. */
		final float retractFrom;

		Swing(float windup, double yank, float retractFrom) {
			this.windup = windup;
			this.yank = yank;
			this.retractFrom = retractFrom;
		}
	}

	/** One player's lash: its links in the world and their speeds (blocks per tick). */
	private static final class Lash {
		final Vec3d[] pos = new Vec3d[LINKS + 1];
		final Vec3d[] speed = new Vec3d[LINKS + 1];
		double length = IDLE_LENGTH;
		@Nullable
		Swing swing;
		float swingStart;
		boolean kinematic;
		/** How hot the lash runs (0–1): up through a swing, fading after; and the white flash of the crack itself. */
		float heat;
		float flash;
		boolean cracked;
		float last = Float.NaN;
		float pending;
		int drawnFrame = Integer.MIN_VALUE;
		@Nullable
		Vector3f view;
		int viewFrame = Integer.MIN_VALUE;
		@Nullable
		Vector3f handView;
		@Nullable
		Matrix4f handProjection;
		int handFrame = Integer.MIN_VALUE;

		void swing(Swing swing, float start) {
			this.swing = swing;
			swingStart = start;
			cracked = false;
		}

		/** Hot streaks and a spark or two off the tip while the lash flies. */
		private void tipSparks(ClientWorld world, float dt) {
			if (pos[LINKS] == null || speed[LINKS] == null || dt <= 0) {
				return;
			}
			Vec3d tip = pos[LINKS];
			Vec3d v = speed[LINKS];
			if (v.lengthSquared() < 0.04) {
				return;
			}
			int links = (int) (LINKS * 0.6);
			for (int i = links; i < LINKS; i += 2) {
				world.addParticle(EmberParticles.WHIP_LINE, pos[i].x, pos[i].y, pos[i].z, 0, 0, 0);
			}
			world.addParticle(EmberParticles.CINDER_SPARK, tip.x, tip.y, tip.z, v.x * 0.15 + (world.random.nextDouble() - 0.5) * 0.1,
					0.05 + world.random.nextDouble() * 0.05, v.z * 0.15 + (world.random.nextDouble() - 0.5) * 0.1);
		}

		/** The crack: a ring of sparks and a puff of soot off the tip (the server adds the report, the burst and the ring). */
		private void crackSparks(ClientWorld world) {
			if (pos[LINKS] == null) {
				return;
			}
			Vec3d tip = pos[LINKS];
			for (int i = 0; i < 10; i++) {
				double angle = world.random.nextDouble() * Math.PI * 2;
				double speed = 0.15 + world.random.nextDouble() * 0.2;
				world.addParticle(EmberParticles.CINDER_SPARK, tip.x, tip.y, tip.z, Math.cos(angle) * speed,
						0.1 + world.random.nextDouble() * 0.15, Math.sin(angle) * speed);
			}
			world.addParticle(EmberParticles.SOOT, tip.x, tip.y, tip.z, 0, 0.02, 0);
		}

		/** Where the lash starts this frame, or null if the handle was not drawn lately. */
		@Nullable
		Vec3d anchor(Vec3d camera, Matrix4f toWorld, Matrix4f fromClip) {
			if (view != null && viewFrame == frame) {
				Vector3f at = toWorld.transformPosition(new Vector3f(view));
				return camera.add(at.x, at.y, at.z);
			}
			if (handView != null && handProjection != null && frame - handFrame <= 2) {
				// The hand's projection to the screen, and back out with the world's: the same spot on screen.
				Vector4f clip = handProjection.transform(new Vector4f(handView, 1));
				Vector4f eye = fromClip.transform(clip);
				Vector3f at = toWorld.transformPosition(new Vector3f(eye.x / eye.w, eye.y / eye.w, eye.z / eye.w));
				return camera.add(at.x, at.y, at.z);
			}
			return null;
		}

		void update(ClientWorld world, PlayerEntity player, Vec3d anchor, float now, float tickDelta) {
			if (pos[0] == null || frame - drawnFrame > STALE_FRAMES) {
				hang(anchor);
				last = now;
			}
			drawnFrame = frame;
			float dt = MathHelper.clamp(now - last, 0, MAX_STEPS * STEP);
			last = now;
			WhipArc arc = ((WardensScourgeItem) player.getMainHandStack().getItem()).arc();
			float age = now - swingStart;
			heat = Math.max(0, heat - HEAT_FADE * dt);
			flash = Math.max(0, flash - FLASH_FADE * dt);
			// Let go while the tip still overshoots, so the lash goes on moving.
			if (swing != null && age < arc.crackAge(arc.length()) + 0.5f) {
				layOut(world, player, arc, anchor, age, tickDelta, dt);
				kinematic = true;
				pending = 0;
				heat = Math.min(1, age / arc.crackAge(arc.length()));
				if (!cracked && age >= arc.crackAge(arc.length()) - 0.5f) {
					cracked = true;
					flash = 1;
					crackSparks(world);
				}
				tipSparks(world, dt);
				return;
			}
			cracked = false;
			if (kinematic) {
				kinematic = false;
				if (swing != null && swing.yank > 0) {
					for (int i = 1; i <= LINKS; i++) {
						speed[i] = speed[i].add(anchor.subtract(pos[i]).normalize().multiply(swing.yank));
					}
				}
			}
			if (swing == null || age > swing.retractFrom) {
				length = Math.max(IDLE_LENGTH, length - RETRACT_SPEED * dt);
				if (length <= IDLE_LENGTH) {
					swing = null;
				}
			}
			pending += dt;
			for (int steps = 0; pending >= STEP && steps < MAX_STEPS; steps++) {
				step(world, anchor, STEP);
				pending -= STEP;
			}
		}

		/** All links straight down from the hand. */
		private void hang(Vec3d anchor) {
			length = IDLE_LENGTH;
			swing = null;
			kinematic = false;
			for (int i = 0; i <= LINKS; i++) {
				pos[i] = anchor.add(0, -length * i / LINKS, 0);
				speed[i] = Vec3d.ZERO;
			}
		}

		/** The swing places every link on its curve; a block in the way stops the rest of the lash there. */
		private void layOut(ClientWorld world, PlayerEntity player, WhipArc arc, Vec3d anchor, float age, float tickDelta, float dt) {
			length = Math.min(arc.length(), IDLE_LENGTH + (arc.length() - IDLE_LENGTH) * age / PAYOUT_TICKS);
			Vec3d eye = player.getCameraPosVec(tickDelta);
			float yaw = player.getYaw(tickDelta);
			float pitch = player.getPitch(tickDelta);
			boolean leftHanded = player.getMainArm() == Arm.LEFT;
			// The curve starts at the hand of the hit frame; the drawn hand is elsewhere, so the root end is moved
			// onto it, less so toward the tip.
			Vec3d offset = anchor.subtract(WhipArc.toWorld(eye, yaw, pitch, leftHanded, WhipArc.HAND));
			Vec3d stop = null;
			for (int i = 0; i <= LINKS; i++) {
				double share = (double) i / LINKS;
				Vec3d target = WhipArc.toWorld(eye, yaw, pitch, leftHanded, arc.swing(age, share * length, swing.windup))
						.add(offset.multiply(1 - share));
				if (i == 0) {
					target = anchor;
				} else if (stop != null) {
					target = stop;
				} else {
					HitResult hit = world.raycast(new RaycastContext(pos[i - 1], target, RaycastContext.ShapeType.COLLIDER,
							RaycastContext.FluidHandling.NONE, player));
					if (hit.getType() != HitResult.Type.MISS) {
						stop = hit.getPos();
						target = stop;
					}
				}
				speed[i] = dt > 0 ? target.subtract(pos[i]).multiply(1 / dt) : speed[i];
				pos[i] = target;
			}
		}

		/** One physics step of {@code h} ticks: gravity and drag, the links held at their length, the ground. */
		private void step(ClientWorld world, Vec3d anchor, float h) {
			double keep = Math.pow(DRAG, h);
			Vec3d[] next = new Vec3d[LINKS + 1];
			next[0] = anchor;
			for (int i = 1; i <= LINKS; i++) {
				speed[i] = speed[i].add(0, -GRAVITY * h, 0).multiply(keep);
				next[i] = pos[i].add(speed[i].multiply(h));
			}
			double rest = length / LINKS;
			for (int n = 0; n < ITERATIONS; n++) {
				for (int i = 0; i < LINKS; i++) {
					Vec3d d = next[i + 1].subtract(next[i]);
					double len = d.length();
					if (len < 1e-6) {
						continue;
					}
					Vec3d fix = d.multiply((len - rest) / len);
					if (i == 0) {
						next[1] = next[1].subtract(fix);
					} else {
						next[i] = next[i].add(fix.multiply(0.5));
						next[i + 1] = next[i + 1].subtract(fix.multiply(0.5));
					}
				}
			}
			for (int i = 1; i <= LINKS; i++) {
				Vec3d grounded = ground(world, next[i]);
				Vec3d moved = grounded.subtract(pos[i]).multiply(1 / h);
				// Resting on the ground: it stops falling and drags.
				speed[i] = grounded == next[i] ? moved : new Vec3d(moved.x * 0.5, 0, moved.z * 0.5);
				pos[i] = grounded;
			}
		}

		/** {@code at}, pushed up out of a block it sank into. */
		private static Vec3d ground(ClientWorld world, Vec3d at) {
			BlockPos block = BlockPos.ofFloored(at.x, at.y - THICKNESS, at.z);
			BlockState state = world.getBlockState(block);
			VoxelShape shape = state.getCollisionShape(world, block);
			if (shape.isEmpty()) {
				return at;
			}
			double top = block.getY() + shape.getMax(Direction.Axis.Y);
			double bottom = block.getY() + shape.getMin(Direction.Axis.Y);
			return at.y - THICKNESS < top && at.y >= bottom ? new Vec3d(at.x, top + THICKNESS, at.z) : at;
		}

		/** Two crossed ribbons per link (as vanilla draws a leash), narrowing to the tip; the last link is the popper. */
		void draw(ClientWorld world, VertexConsumer buffer, Matrix4f view, Matrix3f normals, Vec3d camera) {
			for (int i = 0; i < LINKS; i++) {
				Vec3d a = pos[i].subtract(camera);
				Vec3d b = pos[i + 1].subtract(camera);
				Vec3d dir = b.subtract(a);
				if (dir.lengthSquared() < 1e-8) {
					continue;
				}
				dir = dir.normalize();
				Vec3d across = dir.crossProduct(Math.abs(dir.y) > 0.99 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0)).normalize();
				Vec3d other = dir.crossProduct(across);
				boolean popper = i == LINKS - 1;
				float widthA = popper ? WIDTH_POPPER : MathHelper.lerp((float) i / LINKS, WIDTH_ROOT, WIDTH_TIP);
				float widthB = popper ? WIDTH_POPPER * 0.6f : MathHelper.lerp((float) (i + 1) / LINKS, WIDTH_ROOT, WIDTH_TIP);
				float u0 = popper ? 0.5f : 0;
				// The links keep their size whatever the length; the popper is always whole.
				float v0 = popper ? 0 : (float) (i * length / LINKS / TEXTURE_LENGTH);
				float v1 = popper ? 1 : (float) ((i + 1) * length / LINKS / TEXTURE_LENGTH);
				int lightA = WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(pos[i]));
				int lightB = WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(pos[i + 1]));
				ribbon(buffer, view, normals, a, b, across, other, widthA, widthB, u0, v0, v1, lightA, lightB);
				ribbon(buffer, view, normals, a, b, other, across, widthA, widthB, u0, v0, v1, lightA, lightB);
			}
		}

		/**
		 * The heat pass: the lash drawn again wider on the additive layer, dull red at the handle to a bright tip, as hot
		 * as the swing has made it, and white all along for the crack's own instant.
		 */
		void drawHeat(VertexConsumer buffer, Matrix4f view, Vec3d camera) {
			if (heat <= 0.02f && flash <= 0.02f) {
				return;
			}
			for (int i = 0; i < LINKS; i++) {
				if (pos[i] == null || pos[i + 1] == null) {
					return;
				}
				Vec3d a = pos[i].subtract(camera);
				Vec3d b = pos[i + 1].subtract(camera);
				Vec3d dir = b.subtract(a);
				if (dir.lengthSquared() < 1e-8) {
					continue;
				}
				dir = dir.normalize();
				Vec3d across = dir.crossProduct(Math.abs(dir.y) > 0.99 ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0)).normalize();
				Vec3d other = dir.crossProduct(across);
				float shareA = (float) i / LINKS;
				float shareB = (float) (i + 1) / LINKS;
				// Heat grows toward the tip: the tip is what moves and burns.
				float alphaA = heatAlpha(shareA);
				float alphaB = heatAlpha(shareB);
				float widthA = MathHelper.lerp(shareA, WIDTH_ROOT, WIDTH_TIP) * HEAT_WIDTH * (1 + flash);
				float widthB = MathHelper.lerp(shareB, WIDTH_ROOT, WIDTH_TIP) * HEAT_WIDTH * (1 + flash);
				glowRibbon(buffer, view, a, b, across, widthA, widthB, shareA, shareB, alphaA, alphaB);
				glowRibbon(buffer, view, a, b, other, widthA, widthB, shareA, shareB, alphaA, alphaB);
			}
		}

		private float heatAlpha(float share) {
			float along = 0.15f + 0.85f * share * share;
			return Math.min(1, heat * along * 0.75f + flash * 0.9f);
		}

		private void glowRibbon(VertexConsumer buffer, Matrix4f view, Vec3d a, Vec3d b, Vec3d side, float widthA, float widthB,
				float shareA, float shareB, float alphaA, float alphaB) {
			Vec3d sa = side.multiply(widthA / 2);
			Vec3d sb = side.multiply(widthB / 2);
			float[] ca = heatColor(shareA);
			float[] cb = heatColor(shareB);
			glowVertex(buffer, view, a.subtract(sa), ca, alphaA);
			glowVertex(buffer, view, a.add(sa), ca, alphaA);
			glowVertex(buffer, view, b.add(sb), cb, alphaB);
			glowVertex(buffer, view, b.subtract(sb), cb, alphaB);
			// The other winding too: the layer culls back faces.
			glowVertex(buffer, view, b.subtract(sb), cb, alphaB);
			glowVertex(buffer, view, b.add(sb), cb, alphaB);
			glowVertex(buffer, view, a.add(sa), ca, alphaA);
			glowVertex(buffer, view, a.subtract(sa), ca, alphaA);
		}

		private float[] heatColor(float share) {
			float[] c = new float[3];
			for (int k = 0; k < 3; k++) {
				float hot = MathHelper.lerp(share, HEAT_ROOT[k], HEAT_TIP[k]);
				c[k] = MathHelper.lerp(Math.min(1, flash), hot, FLASH[k]);
			}
			return c;
		}

		private static void glowVertex(VertexConsumer buffer, Matrix4f view, Vec3d at, float[] rgb, float alpha) {
			buffer.vertex(view, (float) at.x, (float) at.y, (float) at.z).color(rgb[0], rgb[1], rgb[2], alpha).next();
		}

		private static void ribbon(VertexConsumer buffer, Matrix4f view, Matrix3f normals, Vec3d a, Vec3d b, Vec3d side,
				Vec3d normal, float widthA, float widthB, float u0, float v0, float v1, int lightA, int lightB) {
			Vec3d sa = side.multiply(widthA / 2);
			Vec3d sb = side.multiply(widthB / 2);
			vertex(buffer, view, normals, a.subtract(sa), u0, v0, lightA, normal);
			vertex(buffer, view, normals, a.add(sa), u0 + 0.5f, v0, lightA, normal);
			vertex(buffer, view, normals, b.add(sb), u0 + 0.5f, v1, lightB, normal);
			vertex(buffer, view, normals, b.subtract(sb), u0, v1, lightB, normal);
		}

		private static void vertex(VertexConsumer buffer, Matrix4f view, Matrix3f normals, Vec3d at, float u, float v,
				int light, Vec3d normal) {
			buffer.vertex(view, (float) at.x, (float) at.y, (float) at.z).color(255, 255, 255, 255).texture(u, v)
					.overlay(OverlayTexture.DEFAULT_UV).light(light)
					.normal(normals, (float) normal.x, (float) normal.y, (float) normal.z).next();
		}
	}

	private ScourgeLashes() {}
}
