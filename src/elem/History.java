package elem;

import java.util.ArrayList;
import java.util.Collections;

public class History {

	private final ArrayList<ArrayList<String>> history = new ArrayList<>();
	private ArrayList<String> base, construction;
	private int count, parseCount;
	private static final String valueSplit = "`", indexSplit = "=";
	
	private final void start() {
		count = 0;
		construction = new ArrayList<>();
	}
	
	public void startCreation(boolean all) {
		start();
		if (all)
			base = null;
	}

	public String endCreation() {
		String res = "";
		if (construction.size() != 0) {
			var sb = new StringBuilder();			

			for (int i = 0; i < construction.size(); i++) {
				sb.append(construction.get(i));
				construction.set(i, construction.get(i).substring(1));
			}
			
			res = sb.toString().substring(1);
		}
		end();
		history.add(base);
		
		construction = null;
		count = 0;
		
		return res;
	}

	private final String count(int i) {
		return valueSplit + i + indexSplit;
	}
	
	public void add(int i) {
		if (base == null || base.size() <= count || !(base.get(count).matches("[0-9]+") && Integer.parseInt(base.get(count)) == i)) {
			construction.add(count(count) + String.valueOf(i));
		}
		count++;
	}

	public void add(long l) {
		if (base == null || base.size() <= count || !(base.get(count).matches("[0-9]+") && Long.parseLong(base.get(count)) == l)) {
			construction.add(count(count) + String.valueOf(l));
		}
		count++;
	}

	public void add(String s) {
		if (base == null || base.size() <= count || !base.get(count).equals(s)) {
			construction.add(count(count) + s);
		}
		count++;
	}

	public void startRecieve(String res) {
		start();
		parseCount = 0;
		if (res.length() > 0)
			Collections.addAll(construction, res.split(valueSplit));
	}

	public void endRecieve() {
		end();
	}
	
	private void end() {
		if (base == null)
			base = new ArrayList<String>();
		for (int i = 0; i < construction.size(); i++) {
			var indexVal = construction.get(i).split(indexSplit);
			int realIndex = Integer.parseInt(indexVal[0]);
			while(base.size() <= realIndex)
				base.add("");
			base.set(realIndex, indexVal[1]);
		}
		construction = null;		
	}

	private final boolean countFromRecieved() {
		return parseCount >= construction.size() || Integer.parseInt(construction.get(parseCount).split(indexSplit)[0]) != count;
	}

	public byte parseByte() {
		byte res = 0;
		if (countFromRecieved())
			res = Byte.parseByte(base.get(count));
		else {
			res = Byte.parseByte(construction.get(parseCount).split(indexSplit)[1]);
			parseCount++;
		}
		count++;
		return res;
	}
	
	public int parseInt() {
		int res = 0;
		if (countFromRecieved())
			res = Integer.parseInt(base.get(count));
		else {
			res = Integer.parseInt(construction.get(parseCount).split(indexSplit)[1]);
			parseCount++;
		}
		count++;
		return res;
	}
	
	public long parseLong() {
		long res = 0;
		if (countFromRecieved())
			res = Long.parseLong(base.get(count));
		else {
			res = Long.parseLong(construction.get(parseCount).split(indexSplit)[1]);
			parseCount++;
		}
		count++;
		return res;
	}
	
	public String parseStr() {
		String res = null;
		if (countFromRecieved())
			res = base.get(count);
		else {
			res = construction.get(parseCount).split(indexSplit)[1];
			parseCount++;
		}
		count++;
		return res;
	}
	

	/*
	 * ==========================  History ==========================
	 */
	
//	public void addHistory(String cloneString) {
//		history.add(cloneString);
//		historyIndex = history.size() - 1;
//		canUndoHistory = true;
//		
//		System.out.println("HISTORY: " + cloneString);
//	}
//
//	public void addHistory(String[] input, int fromIndex) {
//		StringBuilder cloneString = new StringBuilder(input[fromIndex]);
//		for (int i = fromIndex + 1; i < input.length - 1; i++) {
//			cloneString.append(Translator.splitterStd + input[i]);
//		}
//		int replaceLast = Integer.parseInt(input[input.length - 1]); 
//		if (replaceLast != 0 && history.size() > 0) {
//			history.remove(history.size() - 1);
//		}
//		String str = cloneString.toString();
//		addHistory(str);
//		Translator.setCloneString(this, str);
//	}
//	
//	public boolean historyForward() {
//		if (isHistoryNow())
//			return false;
//		Translator.setCloneString(this, history.get(++historyIndex));
//		return true;
//	}
//
//	public boolean historyBack() {
//		if (historyIndex <= 0)
//			return false;
//		Translator.setCloneString(this, history.get(--historyIndex));
//		return true;
//	}
//
//	public boolean historyBackHome() {
//		if (historyIndex == 0)
//			return false;
//		historyIndex = 0;
//		Translator.setCloneString(this, history.get(historyIndex));
//		return true;
//	}
//	
//	public boolean setHistoryNow() {
//		if (isHistoryNow()) return false;
//		
//		historyIndex = history.size() - 1;
//		Translator.setCloneString(this, history.get(historyIndex));
//		return true;
//	}
//
//	public boolean isHistoryNow() {
//		return historyIndex >= history.size() - 1;
//	}
//	
//	public void resetHistory() {
//		historyIndex = 0;
//		history.clear();
//		canUndoHistory = false;
//	}
//
//	public void redoLastHistory() {
//		if (history.size() > 0)
//			history.remove(history.size() - 1);
//		addHistory(Translator.getCloneString(this, false, true));
//	}
//	
//	public void undoHistory() {
//		if (history.size() <= 0) return;
//		history.remove(history.size() - 1);
//		historyIndex = history.size() - 1;
//		Translator.setCloneString(this, history.get(historyIndex));
//		canUndoHistory = false;
//	}
//
//	public boolean canUndoHistory() {
//		return canUndoHistory;
//	}
//
//
//	public int getHistoryIndex() {
//		return historyIndex;
//	}
//
//
//	public String peekHistory() {
//		int size = history.size();
//		if (size > 0)
//			return history.get(size - 1);
//		return null;
//	}
//
//	public boolean historyHasBought() {
//		return history.size() > 1;
//	}
	
	
}
