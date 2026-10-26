package dev.theforge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TheForgeClient implements ClientModInitializer {
	private static volatile String pendingName;

	@Override
	public void onInitializeClient() {
		// The server told us to play something.
		ClientPlayNetworking.registerGlobalReceiver(PlayVideoPayload.TYPE, (payload, context) -> {
			pendingName = payload.name();
		});

		// Optional: how the picture fills the screen.  /forgevideo stretch | cover | fit
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
			dispatcher.register(ClientCommands.literal("forgevideo")
				.then(ClientCommands.literal("stretch").executes(ctx -> {
					VideoPlayer.mode = "stretch";
					return 1;
				}))
				.then(ClientCommands.literal("cover").executes(ctx -> {
					VideoPlayer.mode = "cover";
					return 1;
				}))
				.then(ClientCommands.literal("fit").executes(ctx -> {
					VideoPlayer.mode = "fit";
					return 1;
				})));
		});

		// The command is typed in the chat screen, so we open the video one tick later.
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			String name = pendingName;
			if (name == null || mc.screen != null) {
				return;
			}
			pendingName = null;

			if ("stop".equals(name)) {
				VideoPlayer.stop();
				return;
			}
			String error = VideoPlayer.start(name);
			if (error != null) {
				if (mc.player != null) {
					mc.player.sendSystemMessage(Component.literal("[The Forge] " + error));
				}
			} else {
				mc.setScreen(new VideoScreen());
			}
		});
	}
}
