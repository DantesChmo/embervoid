package com.embervoid.gametest;

import com.embervoid.anim.AttackAnimations;
import com.embervoid.anim.WeaponKind;
import com.embervoid.reach.gear.GearWeaponItem;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.sanctum.gear.SanctumWeapons;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Player attack animations (PLAN 17.1): the trigger goes out on a swing and on a special with our weapons, to the
 * player and everyone tracking them and nobody else, and never for vanilla weapons; every weapon is registered with
 * a kind whose swing is as long as its attack cooldown. How the animations look is not checked.
 */
public final class AttackAnimationTests implements FabricGameTest {
	/** Far beyond entity tracking range: the outsider never tracks the attacker. */
	private static final int OUTSIDER_DISTANCE = 400;

	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "attack_anim_swing", tickLimit = 200)
	public void swingAndSpecialReachTrackers(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 3);
			Recorder self = new Recorder();
			Recorder watcher = new Recorder();
			Recorder outsider = new Recorder();
			ServerPlayerEntity player = arena.join(4, 3, self);
			watch(player, self, watcher, outsider);
			arena.join(4, 6, watcher);
			arena.joinAt(context.getAbsolute(new Vec3d(4.5, 1, 4.5 + OUTSIDER_DISTANCE)), outsider);
			hold(player, SanctumWeapons.SANCTUM_LONGSWORD);
			// Give the server a few ticks to start tracking the players near each other.
			arena.after(5, () -> {
				context.assertTrue(self.clips.isEmpty() && watcher.clips.isEmpty(), "animations before any swing: " + watcher.clips);
				swing(player);
				expect(context, "the first swing", self, watcher, outsider, 1, "sword_swing_1", 13, false);
				swing(player);
				expect(context, "a swing in the first half of the last", self, watcher, outsider, 1, "sword_swing_1", 13, false);
			});
			arena.after(5 + 7, () -> {
				swing(player);
				expect(context, "the next swing", self, watcher, outsider, 2, "sword_swing_2", 13, false);
			});
			arena.after(5 + 7 + 7, () -> {
				ItemStack sword = player.getMainHandStack();
				context.assertTrue(sword.use(context.getWorld(), player, Hand.MAIN_HAND).getResult().isAccepted(), "Wide Cleave refused");
				expect(context, "Wide Cleave", self, watcher, outsider, 3, "sword_wide_cleave", 14, false);
				// The client swings its arm for the special: that swing falls into the special's first half.
				swing(player);
				expect(context, "the swing after Wide Cleave", self, watcher, outsider, 3, "sword_wide_cleave", 14, false);
			});
			arena.after(5 + 7 + 7 + 20, () -> {
				// A left-handed player: the clip is mirrored.
				player.setMainArm(Arm.LEFT);
				hold(player, GearWeapons.ROYAL_ASH_RAPIER);
				swing(player);
				expect(context, "a left-handed thrust", self, watcher, outsider, 4, "rapier_thrust", 10, true);
				player.setMainArm(Arm.RIGHT);
			});
			arena.after(5 + 7 + 7 + 20 + 20, () -> {
				for (Item vanilla : List.of(Items.DIAMOND_SWORD, Items.NETHERITE_AXE, Items.TRIDENT, Items.STICK)) {
					hold(player, vanilla);
					swing(player);
				}
				player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.CROSSBOW));
				player.getInventory().insertStack(new ItemStack(Items.ARROW, 16));
				player.getMainHandStack().use(context.getWorld(), player, Hand.MAIN_HAND);
				player.stopUsingItem();
				context.assertTrue(self.clips.size() == 4 && watcher.clips.size() == 4,
						"vanilla weapons sent " + self.clips.subList(Math.min(4, self.clips.size()), self.clips.size()));
				arena.finish();
			});
		});
	}

	/** The Huntsman's Crossbow: loading plays, letting go early stops it, a shot plays the recoil. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "attack_anim_crossbow", tickLimit = 100)
	public void crossbowLoadAndFire(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, 3);
			Recorder self = new Recorder();
			Recorder watcher = new Recorder();
			Recorder outsider = new Recorder();
			ServerPlayerEntity player = arena.join(4, 3, self);
			watch(player, self, watcher, outsider);
			arena.join(4, 6, watcher);
			arena.joinAt(context.getAbsolute(new Vec3d(4.5, 1, 4.5 + OUTSIDER_DISTANCE)), outsider);
			arena.after(5, () -> {
				ItemStack crossbow = new ItemStack(GearWeapons.HUNTSMANS_CROSSBOW);
				player.setStackInHand(Hand.MAIN_HAND, crossbow);
				player.getInventory().insertStack(new ItemStack(Items.ARROW, 16));
				swing(player);
				context.assertTrue(self.clips.isEmpty(), "a crossbow swing played " + self.clips);
				crossbow.use(context.getWorld(), player, Hand.MAIN_HAND);
				expect(context, "loading", self, watcher, outsider, 1, "crossbow_load", CrossbowItem.getPullTime(crossbow), false);
				player.stopUsingItem();
				expect(context, "letting go", self, watcher, outsider, 2, null, 0, false);
				CrossbowItem.setCharged(crossbow, true);
				crossbow.use(context.getWorld(), player, Hand.MAIN_HAND);
				expect(context, "the shot", self, watcher, outsider, 3, "crossbow_fire", WeaponKind.CROSSBOW_FIRE.ticks(), false);
				arena.finish();
			});
		});
	}

	/** Every weapon of ours has a kind, a swing as long as its attack cooldown, and vanilla weapons have none. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void everyWeaponHasItsClips(TestContext context) {
		List<Item> weapons = new ArrayList<>(GearWeapons.MELEE);
		weapons.addAll(SanctumWeapons.ALL);
		for (Item item : weapons) {
			WeaponKind kind = AttackAnimations.kindOf(new ItemStack(item));
			context.assertTrue(kind != null && !kind.swings().isEmpty(), item + " has no swing: " + kind);
			int cooldown = WeaponKind.cooldownTicks(((GearWeaponItem) item).spec().speed());
			for (WeaponKind.Clip clip : kind.swings()) {
				context.assertTrue(clip.ticks() == cooldown, item + ": " + clip + " for a cooldown of " + cooldown);
			}
		}
		ItemStack crossbow = new ItemStack(GearWeapons.HUNTSMANS_CROSSBOW);
		context.assertTrue(AttackAnimations.kindOf(crossbow) == WeaponKind.CROSSBOW, "the Huntsman's Crossbow kind");
		context.assertTrue(WeaponKind.CROSSBOW_LOAD.ticks() == CrossbowItem.getPullTime(crossbow),
				"load clip " + WeaponKind.CROSSBOW_LOAD.ticks() + " for a charge of " + CrossbowItem.getPullTime(crossbow));
		context.assertTrue(AttackAnimations.registered().size() == weapons.size() + 1, "registered " + AttackAnimations.registered());
		for (Item vanilla : List.of(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.NETHERITE_AXE, Items.TRIDENT,
				Items.CROSSBOW, Items.BOW)) {
			context.assertTrue(AttackAnimations.kindOf(new ItemStack(vanilla)) == null, vanilla + " has our animations");
		}
		context.complete();
	}

	/** The recorders keep only packets about {@code player}, the test's attacker (other tests run alongside). */
	private static void watch(ServerPlayerEntity player, Recorder... recorders) {
		for (Recorder recorder : recorders) {
			recorder.attacker = player.getId();
		}
	}

	/**
	 * Puts a new {@code item} in the main hand. Test players do not tick on their own (no real connection), and the
	 * attack speed follows the hand on a tick.
	 */
	private static void hold(ServerPlayerEntity player, Item item) {
		player.setStackInHand(Hand.MAIN_HAND, new ItemStack(item));
		player.playerTick();
	}

	private static void swing(ServerPlayerEntity player) {
		player.networkHandler.onHandSwing(new HandSwingC2SPacket(Hand.MAIN_HAND));
	}

	/** {@code count} clips so far for the player and the watcher, the last one as given; none for the outsider. */
	private static void expect(TestContext context, String what, Recorder self, Recorder watcher, Recorder outsider, int count,
			@Nullable String clip, int ticks, boolean mirrored) {
		for (Recorder recorder : List.of(self, watcher)) {
			String who = (recorder == self ? "the player" : "the watcher") + ", " + what + ": ";
			context.assertTrue(recorder.clips.size() == count, who + recorder.clips.size() + " clips, expected " + count + ": " + recorder.clips);
			Sent last = recorder.clips.get(count - 1);
			Sent expected = new Sent(self.attacker, clip == null ? null : new Identifier("embervoid", clip), ticks, mirrored);
			context.assertTrue(last.equals(expected), who + last + ", expected " + expected);
		}
		context.assertTrue(outsider.clips.isEmpty(), "sent " + OUTSIDER_DISTANCE + " blocks away: " + outsider.clips);
	}

	private record Sent(int entity, @Nullable Identifier clip, int ticks, boolean mirrored) {
	}

	/** A connection that records the attack animation packets about the test's attacker sent to it. */
	private static final class Recorder extends ClientConnection {
		final List<Sent> clips = new ArrayList<>();
		int attacker = -1;

		Recorder() {
			super(NetworkSide.SERVERBOUND);
		}

		@Override
		public void send(Packet<?> packet) {
			send(packet, null);
		}

		@Override
		public void send(Packet<?> packet, @Nullable PacketCallbacks callbacks) {
			if (packet instanceof CustomPayloadS2CPacket payload && payload.getChannel().equals(AttackAnimations.PACKET)) {
				PacketByteBuf buf = new PacketByteBuf(payload.getData().copy());
				int entity = buf.readVarInt();
				if (entity != attacker) {
					return;
				}
				int ticks = buf.readVarInt();
				Identifier clip = ticks > 0 ? buf.readIdentifier() : null;
				clips.add(new Sent(entity, clip, ticks, ticks > 0 && buf.readBoolean()));
			}
		}
	}
}
