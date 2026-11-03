package name.modid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class BorderAchievement implements ModInitializer {
	public static final String MOD_ID = "border-achievement";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final double START_SIZE = 1.0;
	private static final double NORMAL_BONUS = 1.0;      // Task + Goal
	private static final double CHALLENGE_BONUS = 50.0;  // lila Advancements
	private static final double UNSET_BORDER_THRESHOLD = 1.0E7; // Vanilla-Border ist ~6.0E7

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				update(server);
			}
		});
	}

	private void update(MinecraftServer server) {
		if (server.getPlayerList().getPlayers().isEmpty()) {
			return;
		}

		WorldBorder border = server.overworld().getWorldBorder();

		// Erster Start der Welt: Border auf 1x1 am Spawn des ersten Spielers setzen
		if (border.getSize() >= UNSET_BORDER_THRESHOLD) {
			ServerPlayer first = server.getPlayerList().getPlayers().get(0);
			border.setCenter(Mth.floor(first.getX()) + 0.5, Mth.floor(first.getZ()) + 0.5);
			border.setSize(START_SIZE);
			border.setDamagePerBlock(0.0);
			border.setDamageSafeZone(0.0);
			LOGGER.info("Worldborder auf 1x1 gesetzt.");
		}

		Set<Identifier> done = new HashSet<>();
		double bonus = 0;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
				DisplayInfo display = holder.value().display().orElse(null);

				// Rezepte und technische Advancements ignorieren
				if (display == null) continue;
				// Root-Advancements (Tab-Header) ignorieren
				if (holder.value().parent().isEmpty()) continue;

				if (!player.getAdvancements().getOrStartProgress(holder).isDone()) continue;

				// Jedes Advancement zählt nur einmal, auch bei mehreren Spielern
				if (!done.add(holder.id())) continue;

				bonus += (display.getType() == AdvancementType.CHALLENGE) ? CHALLENGE_BONUS : NORMAL_BONUS;
			}
		}

		double target = START_SIZE + bonus;

		// Border wird nie kleiner (z.B. wenn Spieler offline sind)
		if (target > border.getSize()) {
			border.setSize(target);
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
