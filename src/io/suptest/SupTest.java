package io.suptest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SupTest {

	public static void main(String[] args) throws Exception {
		Path path = Paths.get("Super Mario World (USA).sfc");
		byte[] rom = Files.readAllBytes(path);
		byte[] ram = new byte[0x2FFFF];
		byte[] cgram = new byte[0xFF];
		
		String str = "";
		int romOffset = 0x8000;
		for (int i = 0; i < 21; i++) {
			str += (char) (rom[i+0xFFC0-romOffset]);
		}
		int misc = Byte.toUnsignedInt(rom[0xFFD5-romOffset]);
		MappingMode mapMode = MappingMode.values()[misc & 0xF];
		System.out.println("mapping mode = " + mapMode);
		
		System.out.println("Launching " + str);
		
		int romSize = (int) Math.pow(2, rom[0xFFD7-romOffset]);
		int sramSize = (int) Math.pow(2, rom[0xFFD8-romOffset]);
		System.out.println(romSize + "K ROM, " + sramSize + "K SRAM.");
		
		Mapper mapper = new Mapper(rom, ram, cgram, mapMode);
		CPU cpu = new CPU(mapper);
		cpu.pc = mapper.getUnsignedShort(0xFFFC); // jump to RESET vector
		
		PPU ppu = new PPU(mapper);
		Display display = new Display(ppu);
		
		while (true) {
			cpu.execute();
			Thread.sleep(100);
		}
	}

}
