package io.suptest;

public class Mapper {

	private byte[] ram;
	private byte[] rom;
	private byte[] cgram;
	private byte[] oam;
	private byte[] vram;
	private byte[] sram;
	
	private MappingMode mode;
	private PPU ppu;
	
	private int cgramAddr;
	private int vramAddr;
	private int oamAddr;
	
	public Mapper(byte[] rom, MappingMode mode) {
		this.rom = rom;
		this.mode = mode;
		ram = new byte[0x2FFFF];
		cgram = new byte[0xFF];
		oam = new byte[544];
		vram = new byte[0xFFFF];
	}
	
	public void setPPU(PPU ppu) {
		this.ppu = ppu;
	}
	
	public short getShort(int addr) {
		return (short) getUnsignedShort(addr);
	}
	
	public int getUnsignedShort(int addr) {
		return (Byte.toUnsignedInt(get(addr+1)) << 8)
				| Byte.toUnsignedInt(get(addr));
	}
	
	public int getUnsignedByte(int addr) {
		return Byte.toUnsignedInt(get(addr));
	}
	
	public void setShort(int addr, short s) {
		int high = (s & 0xFF00) >> 8;
		int low = s & 0xFF;
		set(addr, (byte) low);
		set(addr+1, (byte) high);
	}
	
	public void set(int addr, byte value) {
		int bank = (addr & 0xFF0000) >> 16;
		int low = addr & 0xFFFF;
		if (bank >= 0x7E && bank <= 0x7F) { // WRAM
			ram[addr - 0x7E0000] = value;
			return;
		}
		if ((bank >= 0x80 && bank <= 0xC0) || (bank <= 0x40)) { // Quadrant I and III
			if (low <= 0x1FFF) {
				ram[addr & 0x1FFF] = value;
				return;
			}
		}
		
		if (mode == MappingMode.LOROM) {
			bank |= 0x80;
			if (bank >= 0x80 && low >= 0x8000) {
				System.err.println("Cannot set ROM! 0x" + Integer.toHexString(addr));
				return;
			} else if (bank >= 0xF0 && low < 0x8000) {
				int sramAddr = (bank-0xF0)*0x8000 + low;
				if (sramAddr < sram.length) {
					sram[sramAddr] = value;
					return;
				}
			} else if (bank >= 0xC0 && low < 0x8000) {
				System.err.println("Cannot set ROM! 0x" + Integer.toHexString(addr));
				return;
			}
		}
		
		if (addr == 0x2100) { // Screen Display register
			ppu.forceBlank = (value & 0x80) == 0x80;
			ppu.screenBrightness = value & 0xF;
			return;
		}
		
		// CGRAM
		if (addr == 0x2122) { // CGRAM write
			cgram[cgramAddr] = value;
			cgramAddr++;
		}
		if (addr == 0x2121) { // CGRAM address
			cgramAddr = Byte.toUnsignedInt(value);
		}
		
		// OAM
		if (addr == 0x2102) { // OAM address low
			oamAddr = (oamAddr & 0xFF00) | Byte.toUnsignedInt(value);
		} else if (addr == 0x2103) { // OAM address high
			oamAddr = (oamAddr & 0xFF) | (Byte.toUnsignedInt(value) << 8);
		}
		if (addr == 0x2104) { // OAM write
			oam[oamAddr] = value;
			oamAddr++;
		}
		
		if (addr >= 0x2140 && addr < 0x2144) { // APU I/O registers
			System.out.println("APU I/O #" + (addr-0x2140) + " = " + value);
			return;
		}
		
		if (addr == 0x4200) { // interrupt enable register
			System.out.println("interrupt enable = " + value);
			return;
		}
		
		if (addr == 0x420B) { // DMA enable
			System.out.println("DMA enable = " + value);
			return;
		}
		
		if (addr == 0x420C) { // HDMA enable
			System.out.println("HDMA enable = " + value);
			return;
		}
		
		// TODO: CPU registers
		// DMA: 4300-437B
		
		// OPEN BUS !!!
		System.err.println("Open Bus! 0x" + Integer.toHexString(addr));
		
	}
	
	public byte get(int addr) {
		int bank = (addr & 0xFF0000) >> 16;
		int low = addr & 0xFFFF;
		if (bank >= 0x7E && bank <= 0x7F) { // WRAM
			return ram[addr - 0x7E0000];
		}
		if ((bank >= 0x80 && bank <= 0xC0) || (bank <= 0x40)) { // Quadrant I and III
			if (low <= 0x1FFF) {
				return ram[addr & 0x1FFF];
			}
		}
		
		if (addr == 0x213B) { // CGRAM read
			return cgram[cgramAddr];
		}
		
		if (mode == MappingMode.LOROM) {
			bank |= 0x80;
			if (bank >= 0x80 && low >= 0x8000) {
				int romAddr = (bank-0x80)*0x8000 + low-0x8000;
				return rom[romAddr];
			} else if (bank >= 0xF0 && low < 0x8000) {
				int sramAddr = (bank-0xF0)*0x8000 + low;
				if (sramAddr < sram.length) {
					return sram[sramAddr];
				}
			} else if (bank >= 0xC0 && low < 0x8000) {
				int romAddr = (bank-0xC0)*0x8000 + low;
				return rom[romAddr];
			}
		}
		
		// OPEN BUS !!!
		System.err.println("Open Bus! 0x" + Integer.toHexString(addr));
		return rom[addr];
	}
	
}
