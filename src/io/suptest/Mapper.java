package io.suptest;

public class Mapper {

	byte[] ram;
	byte[] rom;
	byte[] cgram;
	byte[] sram;
	MappingMode mode;
	
	int cgramAddr;
	
	public Mapper(byte[] rom, byte[] ram, byte[] cgram, MappingMode mode) {
		this.rom = rom;
		this.ram = ram;
		this.cgram = cgram;
		this.mode = mode;
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
				int romAddr = (bank-0x80)*0x8000 + low-0x8000;
				System.err.println("Cannot set ROM! 0x" + Integer.toHexString(addr));
				return;
			} else if (bank >= 0xF0 && low < 0x8000) {
				int sramAddr = (bank-0xF0)*0x8000 + low;
				if (sramAddr < sram.length) {
					sram[sramAddr] = value;
					return;
				}
			} else if (bank >= 0xC0 && low < 0x8000) {
				int romAddr = (bank-0xC0)*0x8000 + low;
				System.err.println("Cannot set ROM! 0x" + Integer.toHexString(addr));
				return;
			}
		}
		
		if (mode == MappingMode.HIROM) {
			// TODO
		}
		
		if (addr == 0x2122) { // CGRAM write
			cgram[cgramAddr] = value;
		}
		
		if (addr == 0x2121) { // CGRAM address
			cgramAddr = Byte.toUnsignedInt(value);
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
		
		if (mode == MappingMode.HIROM) {
			// TODO
		}
		
		// OPEN BUS !!!
		System.err.println("Open Bus! 0x" + Integer.toHexString(addr));
		return rom[addr];
	}
	
}
