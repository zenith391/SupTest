package io.suptest;

public class CPU {

	int a; // accumulator (16 bits)
	int dbr; // data bank register (8 bits)
	int d; // direct register (16 bits)
	int k; // program bank (8 bits)
	int pc; // program counter (16 bits)
	int p; // processor status
	int s; // stack pointer (16 bits)
	int x; // 16 bits
	int y; // 16 bits
	
	boolean emulation = true; // emulation flag
	
	Mapper mapper;
	
	public CPU(Mapper mapper) {
		this.mapper = mapper;
	}
	
	// read next address in direct addressing mode
	int readDirectAddress() {
		int low = mapper.get(pc);
		int addr = d+low;
		if (emulation && (d & 0xFF) == 0) {
			addr = (d & 0xFF00) | low;
		}
		pc++;
		return addr;
	}
	
	// read value at next address in direct addressing mode
	int readDirect(boolean wide) {
		int addr = readDirectAddress();
		
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return mapper.getUnsignedByte(addr);
		}
	}
	
	// read immediate value
	int readImmediate(boolean wide) {
		if (wide) {
			int operand = mapper.getUnsignedShort(pc);
			pc++;
			pc++;
			return operand;
		} else {
			int operand = mapper.get(pc);
			pc++;
			return operand;
		}
	}
	
	int readLong(boolean wide) {
		// read 3-byte address
		int addr = mapper.get(pc);
		pc++;
		addr = addr | (mapper.get(pc) << 8);
		pc++;
		addr = addr | (mapper.get(pc) << 16);
		pc++;
		
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return Byte.toUnsignedInt(mapper.get(pc));
		}
	}
	
	int readLongX(boolean wide) {
		// read 3-byte address
		int addr = mapper.get(pc) + x;
		pc++;
		addr = addr | (mapper.get(pc) << 8);
		pc++;
		addr = addr | (mapper.get(pc) << 16);
		pc++;
		
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return Byte.toUnsignedInt(mapper.get(pc));
		}
	}
	
	int readStack(boolean wide) {
		int addr = (s + Byte.toUnsignedInt(mapper.get(pc))) & 0xFFFF;
		pc++;
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return Byte.toUnsignedInt(mapper.get(pc));
		}
	}
	
	int readAbsolute(boolean wide) {
		int addr = (dbr << 16) | readImmediate(wide);
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return Byte.toUnsignedInt(mapper.get(pc));
		}
	}
	
	int readAbsoluteX(boolean wide) {
		int addr = ((dbr << 16) | readImmediate(wide)) + x;
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return Byte.toUnsignedInt(mapper.get(pc));
		}
	}
	
	int readAbsoluteY(boolean wide) {
		int addr = ((dbr << 16) | readImmediate(wide)) + y;
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return Byte.toUnsignedInt(mapper.get(pc));
		}
	}
	
	void adc(int operand, boolean m) {
		a += operand + (p & 1); // a += operand + carry
		if (a > 0xFFFF || (m && a > 0xFF)) {
			p |= 1; // set carry
		} else {
			p &= 0xFE; // clear carry
		}
		a &= 0xFFFF;
		if (m) a &= 0xFF;
	}
	
	public void m(boolean val) {
		p &= 0b11011111;
		p |= (val ? 1 : 0) << 5;
	}
	
	public void push(byte operand) {
		mapper.set(s, operand);
		s = (s - 1) & 0xFFFF;
	}
	
	public byte pop() {
		s = (s + 1) & 0xFFFF;
		return mapper.get(s);
	}
	
	public int execute() {
		int opcode = Byte.toUnsignedInt(mapper.get(pc));
		
		if (emulation) {
			m(true);
		}
		
		boolean m = (p & 0b100000) == 0b100000; // memory width flag, false = 16-bit, true = 8-bit
		int mInt = m ? 1 : 0;
		int wInt = ((d & 0xFF) == 0) ? 0 : 1;
		
		System.out.println(Integer.toHexString(pc) + ": " + Integer.toHexString(opcode));
		pc++;
		System.out.println("a = " + a);
		switch (opcode) {
		case 0x00: // BRK
			if (emulation) {
				int irq = mapper.getUnsignedShort(0xFFFE);
				pc = irq;
				return 8;
			}
		case 0x69: // ADC (immediate)
			int operand = readImmediate(!m);
			System.out.println("ADC #$" + Integer.toHexString(operand));
			adc(operand, m);
			return 3-mInt;
		case 0x6D: // ADC (absolute)
			adc(readAbsolute(!m), m);
			return 4-mInt;
		case 0x6F: // ADC (long)
			adc(readLong(!m), m);
			return 6-mInt;
		case 0x82: // BRL
			pc++;
			short displacement = mapper.getShort(pc);
			pc++;
			System.out.println("displace by " + displacement);
			return 4;
		case 0x85: // STA (direct)
			int addr = readDirectAddress();
			mapper.setShort(addr, (short) a);
			System.out.println(Integer.toHexString(pc));
			System.out.println("set " + addr + " to " + a);
			return 4-mInt-wInt;
		case 0xA5: // LDA (direct)
			a = readDirect(!m);
			return 4-mInt+wInt;
		case 0xA9: // LDA (immediate)
			a = readImmediate(!m);
			return 3-mInt;
		case 0xAD: // LDA (absolute)
			a = readAbsolute(!m);
			return 5-mInt;
		case 0xAF: // LDA (long)
			a = readLong(!m);
			return 6-mInt;
		case 0xFB: // XCE
			int carry = p & 1;
			int emu = emulation ? 1 : 0;
			p &= 0xFE; // clear carry
			p |= emu; // set carry
			emulation = (carry == 1); // set emulation flag
			return 2;
		default:
			System.err.println("0x" + Integer.toHexString(pc) + ": unknown opcode (0x" + Integer.toHexString(opcode) + ")");
		}
		return 0;
	}
	
}
