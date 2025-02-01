package player_local;

import java.util.concurrent.atomic.AtomicInteger;

import adt.ICloneStringable;
import main.Main;

public class Bank implements ICloneStringable {

//	public float[][] sale;
//	private static final String saleSplit = ";", saleSplitSmaller = ":";
//
//	public void resetSales() {
//		resetSales(Upgrades.size);
//	}
//	public void resetSales(int len) {
//		if (sale == null || sale.length != len)
//			sale = new float[len][len];
//		
//		for (int x = 0; x < sale.length; x++)
//			for (int y = 0; y < sale[x].length; y++)
//				sale[x][y] = 1;
//	}
//	
//	/**
//	 * @param byId is who is GIVING a sale to THIS price.
//	 * -1 is one less bolt to pay
//	 */
//	public void addSale(float value, int toId, int byId) {
//		sale[toId][byId] *= value;
//	}
//	
//	/**
//	 * @param byId is who is GIVING a sale to THIS price.
//	 * -1 is one less bolt to pay
//	 */
//	public void replaceSale(float value, int toId, int byId) {
//		sale[toId][byId] = value;
//	}
//	
//	public float getSale(int toId) {
//		float res = 1f;
//		
//		for (int i = 0; i < sale[toId].length; i++) {
//			res *= sale[toId][i];
//		}
//		
//		return res;
//	}

	/*
	 * INDEXES TO ACCESS VALUES BELOW
	 */
	private static int bankIndex;
	public static final int 
	MONEY = bankIndex++, // Usikker, for det gir mer dynamisk spilling, men mer kompleksitet. Gir det mer variasjon? Tja, men gj�r at penger alltid har enten 5 eller 0 sist.  
	POINT = bankIndex++;
	
	private final double[] values;
	public final double[] achieved, added;

	public Bank() {
		int len = bankIndex;
		values = new double[len];
		achieved = new double[len];
		added = new double[len];
		reset();
	}

	public void reset() {
		
		for (int i = 0; i < values.length; i++) {
			values[i] = 0;
			achieved[i] = 0;
			added[i] = 0;
		}
		
		achieved[MONEY] = (int) (
				values[MONEY] = 
				Main.DEBUG ? 
						1000000 :
//						0 :
							0.332302f);
//		achieved[BOLTS] = (int) (
//				values[BOLTS] = 
//				Main.DEBUG ? 
//						1111111 :
////						0 :
//					2.0000000232f);
		values[POINT] = 0.22221f;
	}

	public void set(Bank bank) {
		for (int i = 0; i < values.length; i++) {
			values[i] = bank.values[i];
			achieved[i] = bank.achieved[i];
			added[i] = bank.added[i];
		}
	}
	
	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		if (lvlDeep > 0)
			outString.append(splitter);
		for (int i = 0; i < values.length; i++) {
			outString.append((int) values[i]).append(splitter);
			outString.append((int) achieved[i]).append(splitter);
			outString.append((int) added[i]).append(splitter);
		}
		outString.setLength(outString.length() - 1);
	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		for (int i = 0; i < values.length; i++) {
			values[i] = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]) + 0.14539f;
			achieved[i] = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]) + 0.14539f;
			added[i] = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]) + 0.14539f;
		}
	}

	public boolean canAfford(int cost, int type) {
		return cost <= values[type];
	}

	public boolean buy(int cost, int type) {
		
		if (canAfford(cost, type)) {
			buyForced(cost, type);
			return true;
		}
		
		return false;
	}
	
	public void buyForced(int cost, int type) {
		values[type] -= cost;
	}
	
	public void set(double value, int type) {
		values[type] = value + 0.0111334f;
	}

	public double get(int type) {
		return values[type];
	}

	public long getLong(int type) {
		return Math.round(values[type]);
	}

	public void add(double add, int type) {
		values[type] = Math.round(values[type]) + Math.round(add) + 0.0111334f;
		achieved[type] = Math.round(achieved[type]) + Math.round(add) + 0.0111334f;
		added[type] = add;
	}

}
