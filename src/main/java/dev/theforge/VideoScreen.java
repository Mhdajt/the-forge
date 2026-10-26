package dev.theforge;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

public class VideoScreen extends Screen {
	public VideoScreen() {
		super(Component.literal("The Forge"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

	@Override
	public void removed() {
		VideoPlayer.stop();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		VideoPlayer.pullFrame();
		g.fill(0, 0, this.width, this.height, 0xFF000000);

		if (VideoPlayer.hasTexture()) {
			int tw = VideoPlayer.texWidth();
			int th = VideoPlayer.texHeight();
			String mode = VideoPlayer.mode;

			if ("fit".equals(mode)) {
				double s = Math.min((double) this.width / tw, (double) this.height / th);
				int dw = (int) (tw * s);
				int dh = (int) (th * s);
				g.blit(RenderPipelines.GUI_TEXTURED, VideoPlayer.TEXTURE,
					(this.width - dw) / 2, (this.height - dh) / 2, 0, 0, dw, dh, tw, th, tw, th);
			} else if ("cover".equals(mode)) {
				double s = Math.max((double) this.width / tw, (double) this.height / th);
				int rw = Math.min(tw, (int) Math.ceil(this.width / s));
				int rh = Math.min(th, (int) Math.ceil(this.height / s));
				g.blit(RenderPipelines.GUI_TEXTURED, VideoPlayer.TEXTURE,
					0, 0, (tw - rw) / 2, (th - rh) / 2, this.width, this.height, rw, rh, tw, th);
			} else {
				// stretch: fill the whole screen at any GUI scale
				g.blit(RenderPipelines.GUI_TEXTURED, VideoPlayer.TEXTURE,
					0, 0, 0, 0, this.width, this.height, tw, th, tw, th);
			}
		}

		if (VideoPlayer.isFinished()) {
			this.onClose();
		}
	}
}
