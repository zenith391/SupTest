package io.suptest;

import java.awt.image.BufferedImage;

public class PPU {

	private Mapper mapper;
	private BufferedImage backBuffer;
	
	private byte[] oam, cgram, vram;
	
	public int screenBrightness;
	public boolean forceBlank;
	
	public PPU(Mapper mapper) {
		this.mapper = mapper;
		oam = mapper.oam;
		cgram = mapper.cgram;
		vram = mapper.vram;
		backBuffer = new BufferedImage(256, 224, BufferedImage.TYPE_INT_RGB);
	}
	
	public BufferedImage getBackBuffer() {
		return backBuffer;
	}
	
}
