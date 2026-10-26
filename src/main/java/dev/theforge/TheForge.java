package dev.theforge;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * Usage:  /video <name> [player]     e.g.  /video the_forge @s     /video the_forge @a
 *         /video stop [player]
 * <name> is the file  <game folder>/the_forge/<name>.fgv  on the player's own computer.
 */
public class TheForge implements ModInitializer {
	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(PlayVideoPayload.TYPE, PlayVideoPayload.CODEC);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("video")
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(ctx -> {
						ServerPlayer self = ctx.getSource().getPlayerOrException();
						return send(List.of(self), StringArgumentType.getString(ctx, "name"));
					})
					.then(Commands.argument("player", EntityArgument.players())
						.executes(ctx -> send(
							EntityArgument.getPlayers(ctx, "player"),
							StringArgumentType.getString(ctx, "name"))))));
		});
	}

	private static int send(Collection<ServerPlayer> players, String name) {
		for (ServerPlayer player : players) {
			ServerPlayNetworking.send(player, new PlayVideoPayload(name));
		}
		return players.size();
	}
}
