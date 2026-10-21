package com.embervoid.sanctum;

import com.embervoid.player.XpUtil;
import com.embervoid.sanctum.leveling.LevelUpNetworking;
import com.embervoid.sanctum.leveling.StatSheet;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Ashen Maiden's level-up screen: every stat with its value and a "+" button, a preview of what the next point
 * in the hovered stat gives, the cost of the next level and the player's XP. One point per click; the server checks
 * and answers with a fresh {@link StatSheet}.
 */
public class LevelUpScreen extends Screen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 214;
	private static final int ROW_HEIGHT = 18;
	private static final int ROWS_TOP = 54;
	private static final int BACKGROUND = 0xE0141012;
	private static final int BORDER = 0xFF8A4A22;
	private static final int GOLD = 0xFFC870;
	private static final int TEXT = 0xE0D8D0;
	private static final int DIM = 0x8C8480;
	private static final int GREEN = 0x7FD47F;
	private static final int RED = 0xE06060;

	private final int maidenId;
	private StatSheet sheet;
	/** Stats hash a refresh was last requested for, so a mismatch asks the server only once. */
	private int requestedFor;
	private final List<ButtonWidget> plusButtons = new ArrayList<>();
	/** Row whose preview is shown: the hovered one, or the last one hovered. */
	private int selected;
	private int left;
	private int top;

	public LevelUpScreen(int maidenId, StatSheet sheet) {
		super(Text.translatable("entity.embervoid.ashen_maiden"));
		this.maidenId = maidenId;
		this.sheet = sheet;
	}

	/** A refreshed sheet from the server after a level-up request. */
	public void update(StatSheet sheet) {
		this.sheet = sheet;
		clearAndInit();
	}

	public int maidenId() {
		return maidenId;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		plusButtons.clear();
		for (int i = 0; i < sheet.stats().size(); i++) {
			StatSheet.Stat stat = sheet.stats().get(i);
			ButtonWidget button = ButtonWidget.builder(Text.literal("+"), b -> request(stat.id()))
					.dimensions(left + 124, top + ROWS_TOP + i * ROW_HEIGHT - 3, 14, 14)
					.build();
			button.active = sheet.canAfford() && stat.value() < stat.max();
			plusButtons.add(addDrawableChild(button));
		}
		addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
				.dimensions(left + WIDTH - 64, top + HEIGHT - 24, 56, 18).build());
	}

	private void request(String statId) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(maidenId);
		buf.writeString(statId);
		ClientPlayNetworking.send(LevelUpNetworking.LEVEL_UP, buf);
	}

	@Override
	public void tick() {
		// Walking away (or the Maiden vanishing) closes the screen; the server would refuse the requests anyway.
		MinecraftClient client = MinecraftClient.getInstance();
		Entity maiden = client.world != null ? client.world.getEntityById(maidenId) : null;
		if (client.player == null || maiden == null
				|| client.player.squaredDistanceTo(maiden) > AshenMaidenEntity.REACH * AshenMaidenEntity.REACH) {
			close();
			return;
		}
		// XP gained or spent elsewhere (orbs, /xp, enchanting) changes what can be bought right away.
		int xp = XpUtil.getPoints(client.player);
		if (xp != sheet.xpPoints()) {
			update(new StatSheet(sheet.level(), sheet.cost(), xp, sheet.stats()));
		}
		// Stats changed elsewhere (commands, another screen): ask the server for a fresh sheet, once per change.
		if (requestedFor != statsVersion(client.player) && !matchesStats(client.player)) {
			requestedFor = statsVersion(client.player);
			request(LevelUpNetworking.REFRESH);
		}
	}

	/** Whether the sheet shows the stat values the client currently has for the player. */
	private boolean matchesStats(PlayerEntity player) {
		for (StatSheet.Stat stat : sheet.stats()) {
			Stat s = Stat.byId(stat.id());
			if (s != null && PlayerStats.get(player, s) != stat.value()) {
				return false;
			}
		}
		return true;
	}

	private static int statsVersion(PlayerEntity player) {
		return Arrays.hashCode(PlayerStats.getAll(player));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		renderBackground(context);
		context.fill(left - 1, top - 1, left + WIDTH + 1, top + HEIGHT + 1, BORDER);
		context.fill(left, top, left + WIDTH, top + HEIGHT, BACKGROUND);

		context.drawCenteredTextWithShadow(textRenderer, title, left + WIDTH / 2, top + 8, GOLD);
		context.drawCenteredTextWithShadow(textRenderer,
				Text.translatable("embervoid.maiden.greeting").formatted(Formatting.ITALIC), left + WIDTH / 2, top + 20, DIM);
		context.drawTextWithShadow(textRenderer, Text.translatable("embervoid.levelup.level", sheet.level()),
				left + 10, top + 36, TEXT);

		int hovered = (mouseY - (top + ROWS_TOP - 4)) / ROW_HEIGHT;
		if (mouseX >= left && mouseX < left + 140 && mouseY >= top + ROWS_TOP - 4
				&& hovered >= 0 && hovered < sheet.stats().size()) {
			selected = hovered;
		}
		for (int i = 0; i < sheet.stats().size(); i++) {
			StatSheet.Stat stat = sheet.stats().get(i);
			int y = top + ROWS_TOP + i * ROW_HEIGHT;
			if (i == selected) {
				context.fill(left + 6, y - 4, left + 142, y + 12, 0x30FFB060);
			}
			context.drawTextWithShadow(textRenderer, Text.translatable("embervoid.stat." + stat.id()), left + 10, y, TEXT);
			String value = Integer.toString(stat.value());
			context.drawTextWithShadow(textRenderer, value, left + 116 - textRenderer.getWidth(value), y,
					stat.value() >= stat.max() ? GOLD : TEXT);
		}
		context.fill(left + 148, top + ROWS_TOP - 4, left + 149, top + ROWS_TOP + 7 * ROW_HEIGHT - 6, 0x40FFFFFF);
		renderPreview(context, left + 156, top + ROWS_TOP);

		int bottom = top + HEIGHT - 38;
		context.drawTextWithShadow(textRenderer, Text.translatable("embervoid.levelup.cost", sheet.cost()),
				left + 10, bottom, TEXT);
		context.drawTextWithShadow(textRenderer, Text.translatable("embervoid.levelup.xp", sheet.xpPoints()),
				left + 10, bottom + 12, sheet.canAfford() ? GREEN : RED);

		super.render(context, mouseX, mouseY, delta);
	}

	/** What one more point in the selected stat gives. */
	private void renderPreview(DrawContext context, int x, int y) {
		if (sheet.stats().isEmpty()) {
			return;
		}
		StatSheet.Stat stat = sheet.stats().get(Math.min(selected, sheet.stats().size() - 1));
		context.drawTextWithShadow(textRenderer, Text.translatable("embervoid.stat." + stat.id()).formatted(Formatting.BOLD),
				x, y, GOLD);
		y += 14;
		if (stat.value() >= stat.max()) {
			context.drawTextWithShadow(textRenderer, Text.translatable("embervoid.levelup.capped"), x, y, DIM);
			return;
		}
		line(context, x, y, Text.translatable("embervoid.levelup.points"), Integer.toString(stat.value()),
				Integer.toString(stat.value() + 1));
		y += 12;
		for (StatSheet.Derived derived : stat.derived()) {
			line(context, x, y, Text.translatable(derived.key()), derived.current(), derived.next());
			y += 12;
		}
		if (stat.derived().isEmpty()) {
			context.drawTextWrapped(textRenderer, Text.translatable("embervoid.stat." + stat.id() + ".desc"),
					x, y + 4, WIDTH - (x - left) - 8, DIM);
		}
	}

	private void line(DrawContext context, int x, int y, Text name, String current, String next) {
		context.drawTextWithShadow(textRenderer, name, x, y, TEXT);
		String values = current + " → ";
		int valuesX = left + WIDTH - 10 - textRenderer.getWidth(values + next);
		context.drawTextWithShadow(textRenderer, values, valuesX, y, DIM);
		context.drawTextWithShadow(textRenderer, next, valuesX + textRenderer.getWidth(values), y,
				current.equals(next) ? DIM : GREEN);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
