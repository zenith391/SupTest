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
			pc += 2;
			return operand;
		} else {
			int operand = mapper.getUnsignedByte(pc);
			pc++;
			return operand;
		}
	}
	
	int readLongAddress() {
		// read 3-byte address
		int addr = mapper.getUnsignedByte(pc);
		pc++;
		addr |= (mapper.getUnsignedByte(pc) << 8);
		pc++;
		addr |= (mapper.getUnsignedByte(pc) << 16);
		pc++;
		return addr;
	}
	
	int readLong(boolean wide) {
		int addr = readLongAddress();
		if (wide) {
			return mapper.getUnsignedShort(addr);
		} else {
			return Byte.toUnsignedInt(mapper.get(pc));
		}
	}
	
	int readLongX(boolean wide) {
		int addr = readLongAddress();
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
	
	int readAbsoluteAddress() {
		return (dbr << 16) | readImmediate(true);
	}
	
	int readAbsolute(boolean wide) {
		int addr = readAbsoluteAddress();
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
	
	/**
	 * Sets the flags for register A after an ADC or SBC.<br/>
	 * Affected flags: carry, negative, zero
	 */
	void flagsA(boolean m) {
		p &= 0b11111110; // clear carry
		if (a > 0xFFFF || (m && a > 0xFF)) p |= 1; // set carry
		
		p &= 0x7F; // clear negative flag
		p |= (a & 0x80); // set negative flag if A is negative
		
		p &= 0b11111101; // clear zero flag
		if (a == 0) p |= 0b10; // set zero flag if A is zero
	}
	
	void adc(int operand, boolean m) {
		a += operand + (p & 1); // a += operand + carry
		flagsA(m);
		a &= 0xFFFF;
		if (m) a &= 0xFF;
	}
	
	void sbc(int operand, boolean m) {
		a = a - operand - 1 + (p & 1); // a = a - operand - 1 + carry
		flagsA(m);
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
	
	public void push(short operand) {
		push((byte) (operand & 0xFF00));
		push((byte) (operand & 0xFF));
	}
	
	public byte pop() {
		s = (s + 1) & 0xFFFF;
		return mapper.get(s);
	}
	
	void debug(String str) {
		if (true) System.out.println(str);
	}
	
	/**
	 * Execute one operation.
	 * @return cycle count
	 */
	public int execute() {
		int opcode = Byte.toUnsignedInt(mapper.get(pc));
		
		if (emulation) {
			m(true);
			s &= 0xFF; // clear SH
			s |= 0x01; // set SH to 0x01
		}
		
		boolean m = (p & 0b100000) == 0b100000; // memory width flag, false = 16-bit, true = 8-bit
		boolean xf = (p & 0b10000) == 0b10000; // index width flag, false = 16-bit, true = 8-bit
		int mInt = m ? 1 : 0;
		int xInt = xf ? 1 : 0;
		int wInt = ((d & 0xFF) == 0) ? 0 : 1;
		
		int addr = 0;
		int operand = 0;
		
		System.out.println(Integer.toHexString(pc) + ": " + Integer.toHexString(opcode));
		pc++;
		//System.out.println("a = " + a);
		switch (opcode) {
		case 0x00: // BRK
			debug("BRK");
			if (emulation) {
				int irq = mapper.getUnsignedShort(0xFFFE);
				pc = irq;
				return 8;
			}
		case 0x08: // PHP
			push((byte) p);
			return 3;
		case 0x10: // BPL
			int rel = mapper.getUnsignedByte(pc);
			debug("BPL $" + Integer.toHexString(pc+rel));
			if ((p & 0b10000000) == 0) {
				pc = pc + rel;
				return emulation ? 3 : 4;
			} else {
				pc++;
			}
			return 2;
		case 0x18: // CLC
			p = p & 0xFE;
			debug("CLC");
			return 2;
		case 0x1B: // TCS
			debug("TCS");
			s = a;
			return 2;
		case 0x20: // JSR (absolute)
			addr = readAbsoluteAddress();
			debug("JSR $" + Integer.toHexString(addr));
			pc--; // set PC to last byte of JSR instead of next instruction
			push((short) (pc & 0xFFFF));
			pc = (pc & 0xFF0000) | addr;
			return 6;
		case 0x28: // PLP
			p = pop();
			return 4;
		case 0x38: // SEC
			debug("SEC");
			p = p | 1;
			return 2;
		case 0x3B: // TSC
			a = s;
			return 2;
		case 0x58: // CLI
			p = p & 0b11111011;
			return 2;
		case 0x5B: //TCD
			debug("TCD");
			d = a;
			return 2;
		case 0x69: // ADC (immediate)
			operand = readImmediate(!m);
			debug("ADC #$" + Integer.toHexString(operand));
			adc(operand, m);
			return 3-mInt;
		case 0x6D: // ADC (absolute)
			adc(readAbsolute(!m), m);
			return 4-mInt;
		case 0x6F: // ADC (long)
			adc(readLong(!m), m);
			return 6-mInt;
		case 0x78: // SEI
			debug("SEI");
			p |= 0b100;
			return 2;
		case 0x7B: // TDC
			a = d;
			return 2;
		case 0x7D: // ADC (absolute,X)
			operand = readAbsoluteX(!m);
			debug("ADC (absolute,X)");
			adc(operand, m);
			return 3-mInt;
		case 0x82: // BRL
			debug("BRL");
			short displacement = mapper.getShort(pc);
			pc += displacement + 1;
			return 4;
		case 0x85: // STA (direct)
			addr = readDirectAddress();
			debug("STA $" + Integer.toHexString(addr));
			if (m) mapper.set(addr, (byte) a);
			else mapper.setShort(addr, (short) a);
			return 4-mInt-wInt;
		case 0x88: // DEY
			debug("DEY");
			y = (y - 1) & (xf ? 0xFF : 0xFFFF);
			return 2;
		case 0x8A: // TXA
			a = x;
			debug("TXA");
			return 2;
		case 0x8D: // STA (absolute)
			addr = readAbsoluteAddress();
			debug("STA $" + Integer.toHexString(addr));
			if (m) mapper.set(addr, (byte) a);
			else mapper.setShort(addr, (short) a);
			return 4-mInt;
		case 0x8F: // STA (long)
			addr = readLongAddress();
			debug("STA $" + Integer.toHexString(addr));
			if (m) mapper.set(addr, (byte) a);
			else mapper.setShort(addr, (short) a);
			return 6-mInt;
		case 0x98: // TYA
			debug("TYA");
			a = y;
			return 2;
		case 0x9A: // TXS
			s = x;
			return 2;
		case 0x9B: // TXY
			y = x;
			return 2;
		case 0x9C: // STZ (absolute)
			addr = readAbsoluteAddress();
			debug("STZ $" + Integer.toHexString(addr));
			if (m) {
				mapper.set(addr, (byte) 0);
			} else {
				mapper.setShort(addr, (short) 0);
			}
			return 5-mInt;
		case 0x9F: // STA (long,X)
			addr = readLongAddress();
			debug("STA $" + Integer.toHexString(addr) + ",X");
			if (m) mapper.set(addr+x, (byte) a);
			else mapper.setShort(addr+x, (short) a);
			return 6-mInt;
		case 0xA0: // LDY (immediate)
			y = readImmediate(!xf);
			debug("LDY #$" + Integer.toHexString(y));
			return 3-xInt;
		case 0xA2: // LDX (immediate)
			x = readImmediate(!xf);
			debug("LDX #$" + Integer.toHexString(x));
			return 3-xInt;
		case 0xA5: // LDA (direct)
			a = readDirect(!m);
			debug("LDA = " + a);
			return 4-mInt+wInt;
		case 0xA8: // TAY
			debug("TAY");
			y = a;
			return 2;
		case 0xA9: // LDA (immediate)
			a = readImmediate(!m);
			debug("LDA #$" + Integer.toHexString(a));
			return 3-mInt;
		case 0xAA: // TAX
			x = a;
			return 2;
		case 0xAD: // LDA (absolute)
			a = readAbsolute(!m);
			return 5-mInt;
		case 0xAF: // LDA (long)
			a = readLong(!m);
			return 6-mInt;
		case 0xB8: // CLV
			p = p & 0b10111111;
			return 2;
		case 0xBA: // TSX
			x = s;
			return 1;
		case 0xBB: // TYX
			x = y;
			return 2;
		case 0xC2: // REP
			int bits = readImmediate(false);
			debug("REP #" + Integer.toBinaryString(bits));
			p = p & (~bits);
			return 3;
		case 0xCA: // DEX
			debug("DEX");
			x = (x - 1) & (xf ? 0xFF : 0xFFFF);
			return 2;
		case 0xD8: // CLD
			p &= 0b11110111;
			return 2;
		case 0xE2: // SEP
			int setBits = readImmediate(false);
			debug("SEP #" + Integer.toBinaryString(setBits));
			p = p | setBits;
			return 3;
		case 0xE9: // SBC (immediate)
			operand = readImmediate(!m);
			debug("SBC #$" + Integer.toHexString(operand));
			sbc(operand, m);
			return 3-mInt;
		case 0xF8: // SED
			p |= 0b1000;
			return 2;
		case 0xFB: // XCE
			debug("XCE");
			int carry = p & 1;
			int emu = emulation ? 1 : 0;
			p &= 0xFE; // clear carry
			p |= emu; // set carry to emulation flag
			emulation = (carry == 1); // set emulation flag
			return 2;
		case 0xCD: // CMP(Absolute)
			// this implementation of cmp absolute is incorrect(sorry)
			int value = readAbsolute(!m);
			if(value == a) flagsA(true);
		default:
			System.err.println("0x" + Integer.toHexString(pc) + ": unknown opcode (0x" + Integer.toHexString(opcode) + ")");
		}
		return 0;
	}
	
}
