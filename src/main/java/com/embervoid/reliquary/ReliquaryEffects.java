package com.embervoid.reliquary;

import com.embervoid.Embervoid;
import com.embervoid.player.DeathStashes;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.StatEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * What worn accessories do outside their own items: artifact stat points through the stat system (not counted in
 * level or level cost), and the Reliquary as a death stash, so accessories go into the Ash Remnant with the inventory
 * and follow its rules.
 */
final class ReliquaryEffects {
	static void register() {
		PlayerStats.registerBonusSource((player, bonus) -> {
			for (ItemStack stack : ReliquaryManager.equipped(player, SlotKinds.ARTIFACT)) {
				if (stack.getItem() instanceof ArtifactItem artifact) {
					artifact.addPoints(bonus);
				}
			}
		});
		ReliquaryManager.CHANGED.register(StatEffects::apply);
		DeathStashes.register(Embervoid.id("reliquary"), new Stash());
	}

	/** The Reliquary's slots, in {@link ReliquaryManager#slots()} order; leftovers after them. */
	private static final class Stash implements DeathStashes.Stash {
		@Override
		public List<ItemStack> takeAll(ServerPlayerEntity player) {
			return ReliquaryManager.takeAll(player);
		}

		@Override
		public ItemStack restore(ServerPlayerEntity player, int index, ItemStack stack) {
			List<ReliquaryManager.SlotRef> slots = ReliquaryManager.slots();
			if (index < slots.size()) {
				ReliquaryManager.SlotRef ref = slots.get(index);
				if (ReliquaryManager.get(player).get(ref.kind(), ref.slot()).isEmpty()
						&& ReliquaryManager.canEquip(player, ref.kind(), ref.slot(), stack)) {
					ReliquaryManager.setStack(player, ref.kind(), ref.slot(), stack);
					return ItemStack.EMPTY;
				}
			}
			return stack;
		}

		@Override
		public boolean isEmpty(ServerPlayerEntity player) {
			return ReliquaryManager.get(player).isEmpty();
		}
	}

	private ReliquaryEffects() {}
}
