package com.embervoid.sanctum;

import com.embervoid.progress.ProgressManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/** Holds the realm an obelisk leads to; sent to clients so they can draw its glow. */
public class VoidObeliskBlockEntity extends BlockEntity {
	private static final String REALM = "Realm";

	private Identifier realm = ProgressManager.REALM_1;

	public VoidObeliskBlockEntity(BlockPos pos, BlockState state) {
		super(SanctumBlocks.VOID_OBELISK_ENTITY, pos, state);
	}

	public Identifier getRealm() {
		return realm;
	}

	public void setRealm(Identifier realm) {
		this.realm = realm;
		markDirty();
		if (world != null) {
			world.updateListeners(pos, getCachedState(), getCachedState(), 3);
		}
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		Identifier id = Identifier.tryParse(nbt.getString(REALM));
		realm = id != null && nbt.contains(REALM) ? id : ProgressManager.REALM_1;
	}

	@Override
	protected void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.putString(REALM, realm.toString());
	}

	@Override
	public Packet<ClientPlayPacketListener> toUpdatePacket() {
		return BlockEntityUpdateS2CPacket.create(this);
	}

	@Override
	public NbtCompound toInitialChunkDataNbt() {
		return createNbt();
	}
}
