import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;

public class Main {
	final public static String MAIN_URL = "http://steamcommunity.com/";
	final public static String PORTAL2_STATS = "/stats/Portal2?tab=leaderboards";

	final public static String DEFAULT_FILE = "Portal2_leaderboards.html";
	final public static String START_FILE = "start.txt";
	final public static String END_FILE = "end.txt";

	final public static String TZ = new SimpleDateFormat("z").format(new Date());
	final public static String URL_ENCODING = "UTF-8";
	final public static String N_A = "\u2013";
	final public static String TIE_PREFIX = "T-";

	public static void main(String[] args) throws IOException {
		final Scanner snInput = new Scanner(System.in);
		System.out.print("Enter your Steam Community ID: ");
		final String input = snInput.nextLine();
		snInput.close();
		final String subpage = (input.matches("7656119\\d{10}") ? "profiles" : "id") + "/" + input;
		final Scanner snParser = new Scanner(new URL(MAIN_URL + subpage + PORTAL2_STATS).openStream(), URL_ENCODING);
		final LinkedHashMap<String, String> lbs = getLeaderboards(snParser);
		activateSelector(lbs, subpage);
	}

	protected static LinkedHashMap<String, String> getLeaderboards(Scanner sn) {
		final LinkedHashMap<String, String> lbs = new LinkedHashMap<String, String>();
		final Pattern p = Pattern.compile("<option value=\"(\\d+)\"(?: selected=\"[^\"]+\")?>([^<]+)</option>",
				Pattern.CASE_INSENSITIVE);

		String line;
		while (!(line = sn.nextLine()).contains("<select name=\"lb\""))
			;
		while (!(line = sn.nextLine()).contains("</select>")) {
			final Matcher m = p.matcher(line);
			m.find();
			lbs.put(m.group(1), m.group(2));
		}
		return lbs;
	}

	/**
	 * Return the input but with invalid characters removed according to the
	 * HTML ID attribute specification.
	 *
	 * @param s
	 *            Input ID
	 * @return Input but with invalid characters removed according to the HTML
	 *         ID attribute specification.
	 */
	public static String idify(String s) {
		return s.replaceAll("[^0-9A-Za-z_]", "");
	}

	/**
	 * Output the selected leaderboards to a file.
	 *
	 * @param lbs
	 *            all leaderboards' ID-name pairs
	 * @param subpage
	 *            the relative path of the Steam profile
	 * @param subset
	 *            the selected leaderboards' IDs
	 */
	public static void outputLBs(LinkedHashMap<String, String> lbs, String subpage, List<String> subset) {
		try {
			final File file = new File(DEFAULT_FILE);
			final FileOutputStream fso = new FileOutputStream(file);
			final OutputStreamWriter osw = new OutputStreamWriter(fso, URL_ENCODING);
			final BufferedWriter bw = new BufferedWriter(osw);
			bw.write(copypaste(START_FILE));
			final Pattern divPattern = Pattern.compile("<(/?)div[>\\s]", Pattern.CASE_INSENSITIVE);

			// Date
			bw
					.write(String
							.format(
									"<p>Loaded on %s<script type=\"text/javascript\">document.write(' (' + dateDiff(%d) + ')');</script>.</p>",
									new Date(), System.currentTimeMillis()
											+ TimeZone.getDefault().getOffset(System.currentTimeMillis())));

			final String[][] anchorData = new String[subset.size()][2];
			int count = 0;
			for (final String key : subset) {
				anchorData[count][0] = N_A;
				anchorData[count][1] = N_A;

				final String title = lbs.get(key);
				final String url = MAIN_URL + subpage + PORTAL2_STATS + "&lb=" + key;
				final Scanner sn = new Scanner(new URL(url).openStream(), URL_ENCODING);

				// Find beginning of LB entries
				String line;
				while (!(line = sn.nextLine()).contains(" class=\"lbentry\"") && sn.hasNextLine())
					;
				if (sn.hasNextLine()) {
					// Leaderboard is not empty
					bw.write(String.format("<h2 id=\"%s\"><a href=\"%s\" target=\"_blank\">%s</a></h2>%n",
							idify(title), url.replaceAll("&", "&amp;"), title));
					bw.write("<table class=\"lb\">");

					String myScore = "", lastScore = "", lastScoreRank = null;
					while (!line.contains("<!-- footer -->")) {
						if ((line.contains(" class=\"lbentry\""))) {
							int divCount = 1;
							String entryData = "";
							while (divCount > 0) {
								line = sn.nextLine();
								final Matcher m = divPattern.matcher(line);
								entryData += line;
								while (m.find()) {
									divCount += m.group(1).equals("") ? 1 : -1;
								}
							}

							final Map<String, String> entryMap = new HashMap<String, String>();
							for (final String divHtml : entryData.trim().split("<div\\s[^>]*?class=\"")) {
								final int iValueEnd = divHtml.indexOf("</div>");
								if (iValueEnd == -1) {
									continue;
								}

								final int iQuote = divHtml.indexOf(divHtml.indexOf('"'));
								final int iValueStart = divHtml.indexOf('>', iQuote + 1) + 1;
								final String className = divHtml.substring(0, divHtml.indexOf('"'));
								final String value = divHtml.substring(iValueStart, iValueEnd);
								entryMap.put(className, value);
							}

							if (entryMap.containsKey("rRh")) {
								// Found myself
								myScore = entryMap.get("scoreh");
								anchorData[count][0] = myScore.equals(lastScore) ? TIE_PREFIX + lastScoreRank
										: entryMap.get("rRh").replaceAll("\\D", "").trim();
								String globalRank = entryMap.get("globalRankh").replaceFirst("Global rank: #", "")
										.trim();
								anchorData[count][1] = addTargetBlank(globalRank);

								writeRow(bw, entryMap, "h", new String[] { "rRh", "avatarIcon", "playerLink", "scoreh",
										"globalRankh" });
							} else {
								// Somebody else
								if (!lastScore.equals(entryMap.get("score"))) {
									lastScore = entryMap.get("score");
									lastScoreRank = entryMap.get("rR").replaceAll("\\D", "");
								}
								if (lastScore.equals(myScore) && !anchorData[count][0].startsWith(TIE_PREFIX)) {
									anchorData[count][0] = TIE_PREFIX + anchorData[count][0];
								}

								writeRow(bw, entryMap, null, new String[] { "rR", "avatarIcon", "playerLink", "score",
										"globalRank" });
							}
						}
						line = sn.nextLine();
					}

					sn.close();
					bw.write("</table>");
					System.out.printf("Read %d/%d: %s%n", ++count, subset.size(), title);
				} else {
					System.out.printf("No entries found for %d/%d: %s%n", ++count, subset.size(), title);
				}
			}

			addAnchorList(bw, lbs, subset, anchorData);
			bw.write(copypaste(END_FILE));
			bw.close();
			java.awt.Desktop.getDesktop().browse(file.toURI());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		System.exit(0);
	}

	protected static void addAnchorList(BufferedWriter bw, LinkedHashMap<String, String> lbs, List<String> subset,
			String[][] anchorData) throws IOException {
		bw.write("<div class=\"anchorList\"><table>");
		bw.write("<tr><th>Leaderboard</th>");
		bw.write("<th><abbr title=\"Relative rank (amongst friends)\">RR</abbr></th>");
		bw.write("<th><abbr title=\"Global rank\">GR</abbr></th></tr>");

		for (int i = 0; i < subset.size(); i++) {
			final String title = lbs.get(subset.get(i));
			bw.write(String.format("<tr><td><a href=\"#%s\">%s</a></td><td>%s</td><td>%s</td></tr>%n", idify(title),
					title, anchorData[i][0], anchorData[i][1]));
		}
		bw.write("</table></div>");
	}

	/**
	 * Returns all of the text in a file.
	 *
	 * @param path
	 *            the path of the file
	 * @return all of the text in a file
	 */
	protected static String copypaste(String path) {
		final StringBuffer sb = new StringBuffer();

		Scanner sn;
		try {
			sn = new Scanner(ClassLoader.getSystemClassLoader().getResourceAsStream(path));
		} catch (NullPointerException e) {
			try {
				sn = new Scanner(new File(path));
			} catch (FileNotFoundException e2) {
				return "";
			}
		}

		while (sn.hasNextLine()) {
			sb.append(String.format("%s%n", sn.nextLine()));
		}
		sn.close();
		return sb.toString();
	}

	public static String addTargetBlank(String src) {
		final int aLeft = src.indexOf("<a ");
		if (aLeft == -1) {
			return src;
		}

		final int aRight = src.indexOf(">", aLeft + 2);
		final String aHtml = src.substring(aLeft, aRight);
		if (aHtml.contains("target")) {
			return src;
		}

		return src.substring(0, aRight) + " target=\"_blank\"" + src.substring(aRight);
	}

	/**
	 * Writes TR HTML to the BufferedWriter.
	 *
	 * @param bw
	 *            the BufferedWriter
	 * @param map
	 *            the class-value pairs
	 * @param trClass
	 *            class for the TR element; null if no class
	 * @param keys
	 *            the names of the classes whose data should be printed in the
	 *            TD elements.
	 * @throws IOException
	 */
	public static void writeRow(BufferedWriter bw, Map<String, String> map, String trClass, String[] keys)
			throws IOException {
		bw.write("<tr" + (trClass == null ? "" : " class=\"" + trClass + "\"") + ">");
		for (final String key : keys) {
			String s = map.get(key);
			// W3 standards
			s = s.replaceAll("<img src=\"", "<img alt=\"\" src=\"");

			String output = "";
			int iLeft, iRight = 0;
			while ((iLeft = s.indexOf("<a class=\"playerName\"", iRight)) > 0) {
				output += s.substring(iRight, iLeft);
				iRight = s.indexOf("</a>", iLeft);
				final int iName = s.lastIndexOf('>', iRight) + 1;
				output += s.substring(iLeft, iName)
						+ s.substring(iName, iRight).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">",
								"&gt;").replaceAll("\"", "&quot;");
			}

			bw.write("<td>" + output + s.substring(iRight) + "</td>");
		}
		bw.write("</tr>");
	}

	/**
	 * Opens the GUI for leaderboard selection.
	 *
	 * @param lbs
	 *            all the leaderboards' ID-name pairs
	 * @param subpage
	 *            the relative path of the Steam profile page
	 */
	public static void activateSelector(LinkedHashMap<String, String> lbs, String subpage) {
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception evt) {
		}

		final SelectionGUI frame = new SelectionGUI(lbs, subpage);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setSize(240, GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height);
		frame.setVisible(true);
	}
}
