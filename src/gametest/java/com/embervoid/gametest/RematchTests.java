package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.BossFight;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.Realms;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import com.embervoid.reach.boss.AshOfRematch;
import com.embervoid.reach.boss.ReachBosses;
import com.embervoid.reach.gear.GearItems;
import com.embervoid.sanctum.SanctumItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

/**
 * Ash of Rematch (PLAN 12.7) at the real Necropolis arena in the realm: its structure is made, attached to its chunks
 * and placed the way worldgen would (the test world does not generate structures), so the shrine is found as in play.
 */
public final class RematchTests implements FabricGameTest {
	/**
	 * A player who beat the Crypt Keeper walks through its arena; Ash of Rematch on the shrine (refused to a player who
	 * has not beaten it) primes it, and stepping in summons it again as a rematch: loot drops, no soul (even at a repeat
	 * chance of 1) and no Flask Shard.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "rematch", tickLimit = 2400)
	public void ashOfRematchSummonsABeatenBoss(TestContext context) {
		BossArena arena = new BossArena(context, "{\"boss\": {\"repeatSoulChance\": 1}}");
		ServerWorld realm = context.getWorld().getServer().getWorld(Realms.ASHEN_REACH.world());
		RealmFeaturePiece[] piece = new RealmFeaturePiece[1];
		ServerPlayerEntity[] players = new ServerPlayerEntity[2];
		arena.step(() -> {
			piece[0] = placeArena(arena, realm, "ashen_reach_arena_c");
			players[0] = arena.join(2, 2);
			players[1] = arena.join(4, 2);
			for (ServerPlayerEntity player : players) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 2000, 4));
				moveTo(player, realm, piece[0].shrinePos().add(1, 0, 1));
			}
			ProgressManager.markBossBeaten(players[0], ReachBosses.NECROPOLIS);
			// A player who has beaten it walks in: nothing happens.
			moveTo(players[0], realm, piece[0].center().up(3).add(0, 0, 6));
		});
		arena.after(20, () -> {
			context.assertTrue(ArenaManager.fightAt(realm, piece[0].center()) == null, "the boss rose for a player who beat it");
			moveTo(players[0], realm, piece[0].shrinePos().add(1, 0, 1));
			ItemStack ash = new ItemStack(GearItems.ASH_OF_REMATCH, 2);
			context.assertTrue(AshOfRematch.use(players[1], realm, piece[0].shrinePos(), ash.copy()) == ActionResult.FAIL,
					"the ash answered a player who has not beaten the boss");
			context.assertTrue(AshOfRematch.use(players[0], realm, piece[0].shrinePos().down(), ash) == ActionResult.PASS,
					"the ash worked on a block that is not the shrine");
			context.assertTrue(AshOfRematch.use(players[0], realm, piece[0].shrinePos(), ash) == ActionResult.SUCCESS, "the ash failed");
			context.assertTrue(ash.getCount() == 1, "the ash was not used up: " + ash.getCount());
			context.assertTrue(ArenaManager.isRematchPrimed(realm, piece[0].center()), "the arena is not primed");
			moveTo(players[0], realm, piece[0].center().up(3).add(0, 0, 6));
		});
		arena.await(() -> ArenaManager.fightAt(realm, piece[0].center()) != null, 60, "the rematch to start", () -> {
			BossFight fight = ArenaManager.fightAt(realm, piece[0].center());
			arena.track(fight);
			context.assertTrue(fight.isRematch(), "not a rematch");
			MobEntity boss = fight.boss();
			if (boss == null) {
				throw new GameTestException("no boss in the rematch");
			}
			boss.kill();
			context.assertTrue(fight.isEnded(), "the kill did not end the rematch");
			context.assertTrue(players[0].getInventory().count(GearItems.CRYPT_KEEPER_SOUL) == 0, "a soul from a rematch");
			context.assertTrue(players[0].getInventory().count(SanctumItems.FLASK_SHARD) == 0, "a Flask Shard from a rematch");
			boolean loot = !realm.getEntitiesByClass(ItemEntity.class, new Box(boss.getBlockPos()).expand(6),
					item -> item.getStack().isOf(GearItems.RAW_GRAVE_IRON)).isEmpty();
			context.assertTrue(loot, "the rematch dropped no loot");
			context.assertTrue(!ArenaManager.isRematchPrimed(realm, piece[0].center()), "still primed after the rematch started");
			arena.finish();
		});
	}

	/** Makes the arena's structure start at its real spot, attaches it to its chunks (forced) and places it. */
	private static RealmFeaturePiece placeArena(BossArena arena, ServerWorld realm, String name) {
		Registry<Structure> structures = realm.getRegistryManager().get(RegistryKeys.STRUCTURE);
		Structure structure = structures.get(Embervoid.id(name));
		ChunkGenerator generator = realm.getChunkManager().getChunkGenerator();
		RealmArenas.Location location = RealmArenas.all(realm).stream()
				.filter(a -> a.spec().bossId().equals(ReachBosses.NECROPOLIS)).findFirst().orElseThrow();
		ChunkPos chunk = new ChunkPos(location.pos());
		StructureStart start = structure.createStructureStart(realm.getRegistryManager(), generator, generator.getBiomeSource(),
				realm.getChunkManager().getNoiseConfig(), realm.getStructureTemplateManager(), realm.getSeed(), chunk, 0, realm, biome -> true);
		if (!start.hasChildren()) {
			throw new GameTestException("no arena pieces at " + chunk);
		}
		RealmFeaturePiece piece = (RealmFeaturePiece) start.getChildren().get(0);
		BlockBox box = start.getBoundingBox();
		arena.forceChunks(realm, piece.center(), Math.max(box.getBlockCountX(), box.getBlockCountZ()) / 2 + 1);
		realm.getChunk(chunk.x, chunk.z).setStructureStart(structure, start);
		ChunkPos.stream(new ChunkPos(box.getMinX() >> 4, box.getMinZ() >> 4), new ChunkPos(box.getMaxX() >> 4, box.getMaxZ() >> 4))
				.forEach(c -> {
					WorldChunk part = realm.getChunk(c.x, c.z);
					part.addStructureReference(structure, chunk.toLong());
					start.place(realm, realm.getStructureAccessor(), generator, realm.getRandom(),
							new BlockBox(c.getStartX(), realm.getBottomY(), c.getStartZ(), c.getEndX(), realm.getTopY(), c.getEndZ()), c);
				});
		return piece;
	}

	/** Moves a player (into the realm if needed), as a real client confirming the teleport. */
	private static void moveTo(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
		player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		player.onTeleportationDone();
	}
}
