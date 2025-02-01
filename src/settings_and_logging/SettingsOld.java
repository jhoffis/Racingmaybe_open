package settings_and_logging;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public abstract class SettingsOld {

	protected File file;
	protected List<String> lines;

	/**
	 * @param filename
	 *            without the type
	 * @return true if file did not exist from before
	 */
	protected boolean init(String filename, int expectedLinesAmount) {
		boolean doWriteLines = false;
		file = new File(filename);

		try {
			if (!file.isFile()) {
				if (file.createNewFile()) {
					PrintWriter pw = new PrintWriter(file);
					pw.flush();
					pw.close();
				}
			}
			readSettingsLines();
			doWriteLines = lines.size() != expectedLinesAmount;
		} catch (IOException e) {
			System.err.println("Error with settings: " + e.getMessage());
			doWriteLines = true;
		}

		return doWriteLines;
	}

	private void readSettingsLines() throws IOException {
		lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
	}

	/**
	 * overrides the previous line, but be vary of wrong linenr order
	 */
	protected void writeToLine(String line, int linenr) {

		while (linenr >= lines.size()) {
			lines.add("null");
		}

		lines.set(linenr, line);

		try {
			Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
			readSettingsLines();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected long getSettingLong(int linenr) {
		return Long.parseLong(getSetting(linenr));
	}

	protected double getSettingDouble(int linenr) {
		return Double.parseDouble(getSetting(linenr));
	}

	protected int getSettingInteger(int linenr) {
		return Integer.parseInt(getSetting(linenr));
	}

	protected boolean getSettingBoolean(int linenr) {
		return getSettingInteger(linenr) != 0;
	}

	protected String getSetting(int linenr) {
		String res = null;

		try {
			if (linenr < lines.size()) {
				String[] splitLine = lines.get(linenr).split("=");
				if (splitLine.length > 1)
					res = splitLine[1];

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;
	}
}
