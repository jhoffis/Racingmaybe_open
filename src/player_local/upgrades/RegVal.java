package player_local.upgrades;

import java.util.concurrent.atomic.AtomicInteger;

import adt.ICloneStringable;

public class RegVal implements ICloneStringable {


	public enum RegValType {
		NormalPercent, AdditionPercent, Decimal, Unlock 
	}
	
	public double value;
	public boolean only, unsigned, removeAtPlacement, replaceNeg, ignoreDifferentNewOne;
	public RegValType type;
	
	
	public RegVal(double value, RegValType type) {
		this.value = value;
		this.type = type;
	}
	
	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		outString.append(value);
		outString.append(splitter);
		byte a = (byte) (
				(only ?					 0b00000001 : 0) |
				(unsigned ?				 0b00000010 : 0) |
				(replaceNeg ? 			 0b00000100 : 0) |
				(removeAtPlacement ?     0b00001000 : 0) |
				(ignoreDifferentNewOne ? 0b00010000 : 0)
				);
		a++;
		outString.append((char) a);
		outString.append(splitter);
		outString.append(type.ordinal());
	}
	
	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		value = Double.parseDouble(cloneString[fromIndex.getAndIncrement()]);
		var b = (byte) cloneString[fromIndex.getAndIncrement()].charAt(0);
		b--;
		only = (b & 0b00000001) != 0; 
		unsigned = (b & 0b00000010) != 0; 
		replaceNeg = (b & 0b00000100) != 0;
		removeAtPlacement = (b & 0b00001000) != 0;
		ignoreDifferentNewOne = (b & 0b00010000) != 0;
		type = RegValType.values()[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])];
	}
	
	public boolean isPercent() {
		return type == RegValType.NormalPercent || type == RegValType.AdditionPercent;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RegVal other = (RegVal) obj;
		return only == other.only && type == other.type && removeAtPlacement == other.removeAtPlacement && unsigned == other.unsigned
				&& Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
	}
	
	
}
