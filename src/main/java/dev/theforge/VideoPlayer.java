package dev.theforge;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Plays a .fgv file: a simple container of JPEG frames.
 *   header: "FGV1", int fps, int frameCount
 *   then per frame: int length, <length> bytes of JPEG
 * Frames are decoded on a background thread and uploaded on the render thread.
 */
public final class VideoPlayer {
	public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("the_forge", "video_frame");

	/** stretch (fill screen), cover (fill + crop), fit (keep aspect, black bars) */
	public static volatile String mode = "stretch";

	private record Frame(int index, NativeImage image) {}

	private static volatile boolean running;
	private static volatile boolean decoderDone;
	private static volatile long startNanos;
	private static int fps = 30;
	private static int totalFrames;
	private static ArrayBlockingQueue<Frame> queue;
	private static Thread thread;
	private static SimpleSoundInstance sound;
	private static boolean hasTexture;
	private static int texW;
	private static int texH;

	private VideoPlayer() {}

	public static Path videoPath(String name) {
		return FabricLoader.getInstance().getGameDir().resolve("the_forge").resolve(name + ".fgv");
	}

	/** Returns an error message, or null if playback started. */
	public static synchronized String start(String name) {
		stop();
		if (!name.matches("[A-Za-z0-9_-]{1,64}")) {
			return "Bad video name: " + name;
		}
		Path path = videoPath(name);
		if (!Files.exists(path)) {
			try (var is = VideoPlayer.class.getResourceAsStream("/assets/the_forge/" + name + ".fgv")) {
				if (is != null) {
					Files.createDirectories(path.getParent());
					Files.copy(is, path);
				}
			} catch (Exception ignored) {}
		}
		if (!Files.exists(path)) {
			return "Video not found. Put it here: " + path;
		}
		DataInputStream in;
		try {
			in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path), 1 << 20));
			byte[] magic = in.readNBytes(4);
			if (magic.length != 4 || magic[0] != 'F' || magic[1] != 'G' || magic[2] != 'V' || magic[3] != '1') {
				in.close();
				return "Not a valid .fgv video file.";
			}
			fps = Math.max(1, in.readInt());
			totalFrames = in.readInt();
		} catch (Exception e) {
			return "Could not open video: " + e;
		}

		queue = new ArrayBlockingQueue<>(4);
		decoderDone = false;
		hasTexture = false;
		running = true;
		startNanos = System.nanoTime();

		final DataInputStream stream = in;
		thread = new Thread(() -> decodeLoop(stream), "the-forge-decoder");
		thread.setDaemon(true);
		thread.start();

		Minecraft mc = Minecraft.getInstance();
		// Sound event the_forge:<name> (defined in assets/the_forge/sounds.json). No event = silent video.
		Identifier soundId = Identifier.fromNamespaceAndPath("the_forge", name);
		sound = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(soundId), 1.0F);
		mc.getSoundManager().play(sound);
		return null;
	}

	private static long due() {
		return (long) ((System.nanoTime() - startNanos) * (double) fps / 1.0e9);
	}

	private static void decodeLoop(DataInputStream in) {
		try (in) {
			for (int i = 0; i < totalFrames && running; i++) {
				int len = in.readInt();
				byte[] data = in.readNBytes(len);
				while (running && i > due() + 3) {
					Thread.sleep(2);
				}
				if (!running) {
					break;
				}
				if (i < due() - 1) {
					continue; // we are behind: skip this frame
				}
				NativeImage image = NativeImage.read(NativeImage.Format.RGBA, new ByteArrayInputStream(data));
				Frame frame = new Frame(i, image);
				boolean queued = false;
				while (running && !queued) {
					queued = queue.offer(frame, 20, TimeUnit.MILLISECONDS);
				}
				if (!queued) {
					image.close();
				}
			}
		} catch (Exception e) {
			System.err.println("[The Forge] decoder stopped: " + e);
		}
		decoderDone = true;
	}

	/** Render thread: show the newest frame that is due. */
	public static void pullFrame() {
		ArrayBlockingQueue<Frame> q = queue;
		if (q == null || !running) {
			return;
		}
		long due = due();
		Frame latest = null;
		Frame f;
		while ((f = q.peek()) != null && f.index() <= due) {
			q.poll();
			if (latest != null) {
				latest.image().close();
			}
			latest = f;
		}
		if (latest != null) {
			final NativeImage image = latest.image();
			texW = image.getWidth();
			texH = image.getHeight();
			Minecraft.getInstance().getTextureManager()
				.register(TEXTURE, new DynamicTexture(() -> "the_forge video", image));
			hasTexture = true;
		}
	}

	public static boolean hasTexture() {
		return hasTexture;
	}

	public static int texWidth() {
		return texW;
	}

	public static int texHeight() {
		return texH;
	}

	public static boolean isFinished() {
		ArrayBlockingQueue<Frame> q = queue;
		return running && decoderDone && q != null && q.isEmpty() && due() >= totalFrames;
	}

	public static synchronized void stop() {
		running = false;
		Thread t = thread;
		if (t != null) {
			t.interrupt();
			thread = null;
		}
		ArrayBlockingQueue<Frame> q = queue;
		if (q != null) {
			Frame f;
			while ((f = q.poll()) != null) {
				f.image().close();
			}
			queue = null;
		}
		if (sound != null) {
			Minecraft.getInstance().getSoundManager().stop(sound);
			sound = null;
		}
		if (hasTexture) {
			Minecraft.getInstance().getTextureManager().release(TEXTURE);
			hasTexture = false;
		}
	}
}
