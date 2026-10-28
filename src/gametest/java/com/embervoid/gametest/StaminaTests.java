package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.StaminaManager;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stamina and the special-attack framework on a real survival player: the debug sweep spends its
 * cost, starts its cooldown and pushes what is in its arc; with no stamina it fails and tells the client; creative
 * spends nothing; stamina comes back after the regen delay; every change reaches the owner's client.
 */
public final class StaminaTests implements FabricGameTest {
	/** The debug sweep's cost ({@code DebugSweepItem}). */
	private static final float SWEEP_COST = 30;

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stamina", tickLimit = 100)
	public void debugSweepSpendsPushesAndCoolsDown(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 4);
			StaminaRecorder sync = new StaminaRecorder();
			ServerPlayerEntity player = arena.join(4, 3, sync);
			// Yaw 0 looks south (+z): one zombie in front of the sweep, one behind it.
			player.setYaw(0);
			player.setHeadYaw(0);
			Item sweep = Registries.ITEM.get(Embervoid.id("debug_sweep"));
			player.setStackInHand(Hand.MAIN_HAND, new ItemStack(sweep));
			ZombieEntity front = context.spawnMob(EntityType.ZOMBIE, new BlockPos(4, 1, 5));
			ZombieEntity behind = context.spawnMob(EntityType.ZOMBIE, new BlockPos(4, 1, 1));
			float max = StaminaManager.getMax(player);
			int sent = sync.values.size();

			ActionResult used = sweep.use(context.getWorld(), player, Hand.MAIN_HAND).getResult();
			context.assertTrue(used.isAccepted(), "the sweep was refused with full stamina: " + used);
			context.assertTrue(StaminaManager.get(player) == max - SWEEP_COST,
					"stamina after the sweep: " + StaminaManager.get(player) + ", expected " + (max - SWEEP_COST));
			context.assertTrue(player.getItemCooldownManager().isCoolingDown(sweep), "no cooldown after the sweep");
			context.assertTrue(front.getVelocity().z > 0.1, "the zombie in front was not pushed away: " + front.getVelocity());
			context.assertTrue(behind.getVelocity().horizontalLengthSquared() < 1.0E-4,
					"the zombie behind was pushed: " + behind.getVelocity());
			context.assertTrue(sync.values.size() > sent && !sync.lastFailed() && sync.lastValue() == max - SWEEP_COST,
					"the client was not sent the new stamina: " + sync.values);

			// While cooling down the vanilla cooldown blocks the item; once over, an empty pool refuses the sweep.
			StaminaManager.set(player, 0);
			player.getItemCooldownManager().remove(sweep);
			front.setVelocity(Vec3d.ZERO);
			ActionResult refused = sweep.use(context.getWorld(), player, Hand.MAIN_HAND).getResult();
			context.assertTrue(!refused.isAccepted(), "the sweep ran with no stamina: " + refused);
			context.assertTrue(!player.getItemCooldownManager().isCoolingDown(sweep), "a refused sweep started the cooldown");
			context.assertTrue(front.getVelocity().horizontalLengthSquared() < 1.0E-4, "a refused sweep pushed the zombie");
			context.assertTrue(sync.lastFailed(), "the client was not told the spend failed (no HUD flash)");

			player.getAbilities().creativeMode = true;
			StaminaManager.set(player, max);
			sweep.use(context.getWorld(), player, Hand.MAIN_HAND);
			context.assertTrue(StaminaManager.get(player) == max, "creative spent stamina: " + StaminaManager.get(player));
			context.assertTrue(player.getItemCooldownManager().isCoolingDown(sweep), "creative: no cooldown");
			player.getAbilities().creativeMode = false;

			front.discard();
			behind.discard();
			arena.finish();
		});
	}

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "stamina", tickLimit = 200)
	public void staminaRegeneratesAfterTheDelay(TestContext context) {
		BossArena arena = new BossArena(context, null);
		int delay = EmbervoidConfig.get().stamina().regenDelayTicks();
		arena.step(() -> {
			arena.floor(4, 4, 2);
			StaminaRecorder sync = new StaminaRecorder();
			ServerPlayerEntity player = arena.join(4, 4, sync);
			float max = StaminaManager.getMax(player);
			context.assertTrue(StaminaManager.tryConsume(player, 50), "a spend from a full pool failed");
			float spent = StaminaManager.get(player);
			arena.after(delay - 2, () -> {
				context.assertTrue(StaminaManager.get(player) == spent,
						"regenerated before the delay: " + StaminaManager.get(player) + " after " + (delay - 2) + " ticks");
				int sent = sync.values.size();
				arena.after(12, () -> {
					context.assertTrue(StaminaManager.get(player) > spent, "no regen after the delay");
					context.assertTrue(sync.values.size() > sent && sync.lastValue() == StaminaManager.get(player),
							"regen not synced to the client: " + sync.values);
					arena.await(() -> StaminaManager.get(player) >= max, 150, "a full pool", () -> {
						context.assertTrue(StaminaManager.get(player) == max, "regen went past the max: " + StaminaManager.get(player));
						arena.finish();
					});
				});
			});
		});
	}

	/** A connection that records every stamina sync (value, failed spend) sent to its player. */
	static final class StaminaRecorder extends ClientConnection {
		final List<Float> values = new ArrayList<>();
		final List<Boolean> failed = new ArrayList<>();

		StaminaRecorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload && payload.getChannel().equals(StaminaManager.SYNC_PACKET)) {
				PacketByteBuf buf = new PacketByteBuf(payload.getData().copy());
				values.add(buf.readFloat());
				buf.readVarInt();
				failed.add(buf.readBoolean());
			}
		}

		float lastValue() {
			return values.get(values.size() - 1);
		}

		boolean lastFailed() {
			return !failed.isEmpty() && failed.get(failed.size() - 1);
		}
	}
}
