package player_local.upgrades;

import java.util.concurrent.atomic.AtomicInteger;

import adt.ICloneStringable;
import communication.Translator;
import engine.graphics.ui.UIColors;
import main.Texts;

public class RegValList implements ICloneStringable {

	public double[] values;

	public RegValList(double[] values) {
		this.values = values;
	}

	public RegValList(int size) {
		this(new double[size]);
	}

	public RegValList() {
	}

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		if (lvlDeep > 0)
			outString.append(splitter);
//		lvlDeep++;
		outString.append(values.length);
		int skip = 0;
		for (var val : values) {
			if (val == 0.0) {
				skip++;
			} else {
				if (skip > 0)
					outString.append(splitter).append('s').append(skip);
				skip = 0;
				outString.append(splitter).append(val);
			}
		}

		if (skip > 0)
			outString.append(splitter).append('e');
	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		values = new double[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])];
		for (int i = 0; i < values.length; i++) {
			if (cloneString[fromIndex.get()].startsWith("s")) {
				i += Integer.parseInt(cloneString[fromIndex.getAndIncrement()].substring(1)) - 1;
			} else if (cloneString[fromIndex.get()].startsWith("e")) {
				fromIndex.getAndIncrement();
				return;
			} else {
				values[i] = Double.parseDouble(cloneString[fromIndex.getAndIncrement()]);
			}
		}
	}

	public String toPlainInfoString(int lineLength, UIColors color) {
		return toPlainInfoString(values, lineLength, color);
	}

	public static String toPlainInfoString(double[] values, int lineLength, UIColors color) {
		if (values.length == 0)
			return "";
		StringBuilder gainedStr = new StringBuilder();
		int lines = 1;
		for (int i = 0; i < values.length; i++) {
			if (values[i] == 0)
				continue;
			var tempStr = new StringBuilder();
			if (values[i] > 0)
				tempStr.append("+");
			else
				tempStr.append("-");
			tempStr.append(Texts.formatNumber(Math.abs(values[i]))).append(" ").append(Texts.tags[i]).append(", ");

			if (gainedStr.length() + tempStr.length() > lineLength * lines) {
				gainedStr.append(Translator.split).append(color);
				gainedStr.append("\n");
				lines++;
			}

			gainedStr.append(tempStr);
		}
		if (gainedStr.length() > 2) {
			gainedStr.setLength(gainedStr.length() - 2);
			gainedStr.append(Translator.split).append(color);
		}
		return gainedStr.toString();
	}

	public static String toPlainInfoString(String str, int lineLength, UIColors color) {
		StringBuilder gainedStr = new StringBuilder();
		int lines = 1;
		var values = str.split(",");
		for (var val : values) {

			val = val.trim();
			if (val.length() == 0)
				continue;

			val += ",";
			
			if (gainedStr.length() + val.length() > lineLength * lines) {
				gainedStr.append(Translator.split).append(color);
				gainedStr.append("\n");
				lines++;
			}
			gainedStr.append(val).append(" ");
		}
		if (gainedStr.length() > 2) {
			gainedStr.setLength(gainedStr.length() - 2);
		} else {
			gainedStr.append("Nothing to collect!");
		}
		gainedStr.append(Translator.split).append(color);
		return gainedStr.toString();
	}

}
