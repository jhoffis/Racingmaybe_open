package settings_and_logging;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class Settings {

	private File file;
	public List<String> lines;

	/**
	 * @param filename
	 *            without the type
	 * @return true if file did not exist from before
	 */
	public void init(String filename) throws IOException{
		file = new File(filename);

		if (!file.isFile()) {
			if (file.createNewFile()) {
				PrintWriter pw = new PrintWriter(file);
				pw.flush();
				pw.close();
			}
		}
		readSettingsLines();
	}

	private void readSettingsLines() throws IOException {
		lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
	}

	/**
	 * overrides the previous line, but be vary of wrong linenr order
	 */
	public void writeToLine(String line, int linenr) {

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

	public long getLong(int linenr) {
		return Long.parseLong(get(linenr));
	}

	public double getDouble(int linenr) {
		return Double.parseDouble(get(linenr));
	}

	public int getInt(int linenr) {
		return Integer.parseInt(get(linenr));
	}

	public boolean getBool(int linenr) {
		return getInt(linenr) != 0;
	}

	public String get(int linenr) {
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
