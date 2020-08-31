package io.suptest;

public class Mapper {

	byte[] ram;
	byte[] rom;
	byte[] sram;
	MappingMode mode;
	
	public Mapper(byte[] rom, byte[] ram, MappingMode mode) {
		this.rom = rom;
		this.ram = ram;
		this.mode = mode;
	}
	
	public short getShort(int addr) {
		return (short) getUnsignedShort(addr);
	}
	
	public int getUnsignedShort(int addr) {
		return (Byte.toUnsignedInt(get(addr)) << 8)
				| Byte.toUnsignedInt(get(addr+1));
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
				rom[romAddr] = value;
				return;
			} else if (bank >= 0xF0 && low < 0x8000) {
				int sramAddr = (bank-0xF0)*0x8000 + low;
				if (sramAddr < sram.length) {
					sram[sramAddr] = value;
					return;
				}
			} else if (bank >= 0xC0 && low < 0x8000) {
				int romAddr = (bank-0xC0)*0x8000 + low;
				rom[romAddr] = value;
				return;
			}
		}
		
		if (mode == MappingMode.HIROM) {
			// TODO
		}
		
		// OPEN BUS !!!
		System.err.println("Open Bus! 0x" + Integer.toHexString(addr));
		rom[addr] = value;
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
