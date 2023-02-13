package com.planview.replicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.planview.replicator.Utils.BoardArchiver;
import com.planview.replicator.Utils.BoardCreator;
import com.planview.replicator.Utils.BoardDeleter;
import com.planview.replicator.Utils.CardDeleter;
import com.planview.replicator.Utils.Exporter;
import com.planview.replicator.Utils.Importer;
import com.planview.replicator.Utils.XlUtils;
import com.planview.replicator.leankit.AccessCache;

public class Main {
	static Debug d = null;

	/**
	 * This defaults to true so we behave as an exporter if it is not otherwise
	 * specified. If the command line contains a -i flag, this is set to false.
	 */
	static Boolean setToExport = true;
	static Boolean setToImport = false;
	/**
	 * One line sheet that contains the credentials to access the Leankit Server.
	 * Must contain columns "url", "username", "password" and "apiKey", but not
	 * necessarily have data in all of them - see getConfigFromFile()
	 */
	static XSSFSheet configSht = null;

	/**
	 * The expectation is that there is a common config for the while execution.
	 * Therefore this is extracted once and passed to all sub tasks
	 */
	static InternalConfig config = new InternalConfig();

	public static void main(String[] args) {
		d = new Debug();
		getCommandLine(args);

		checkXlsx();
		HashMap<String, Integer> fieldMap = chkConfigFromFile();

		if (configSht != null) {
			/**
			 * Fieldmap contains col index info for each set of pairs, so for each line.
			 * 1. populate src and dst configs
			 * 2. check for src board
			 * 3. do a export for it
			 * 4. check for dst board
			 * 5. check/create dst for layout against src
			 * 6. Check/create card types
			 * 7. Check/create custom fields
			 * 8. Check/create custom icons
			 * 9. Check Users - warn if not all available
			 * 10. import to dst
			 * 11. Hook up parents
			 */

			Iterator<Row> rowItr = null;
			Row row = null;
			rowItr = configSht.iterator();
			row = rowItr.next(); // Move past headers
			while (rowItr.hasNext()) {
				Boolean ok = true;
				row = rowItr.next();
				// 1
				config = XlUtils.setConfig(config, row, fieldMap);

				config.source.setCache(new AccessCache());
				config.destination.setCache(new AccessCache());
				config.group = 0;
				if (config.exporter || config.obliterate) {
					// 2 & 3 (Exporter does check for board)
					Exporter exp = new Exporter(config);
					exp.go();
				}

				// Now we need to check/reset the destination board if needed

				if ((config.deleteCards || config.obliterate) && !config.remakeBoard) {
					CardDeleter cd = new CardDeleter(config);
					cd.go();
				}

				if ((config.remakeBoard && !config.eraseBoard) || config.obliterate) {
					BoardArchiver ba = new BoardArchiver(config);
					ba.go();
				}

				if (config.eraseBoard) {
					BoardDeleter bd = new BoardDeleter(config);
					bd.go();
				}

				if ((config.remakeBoard) || (config.updateLayout)) {
					BoardCreator bd = new BoardCreator(config);
					ok = bd.go();
				}

				if (ok && (config.importer || config.obliterate)) {
					Importer imp = new Importer(config);
					imp.go();
				}
			}
		}

		try

		{
			config.wb.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		d.p(Debug.ALWAYS, "Finished at: %s\n", new Date());
		System.exit(0);
	}

	public static void getCommandLine(String[] args) {

		CommandLineParser p = new DefaultParser();
		HelpFormatter hf = new HelpFormatter();
		CommandLine cmdLn = null;

		Options cmdOpts = new Options();

		cmdOpts.addRequiredOption("f", "filename", true, "Working XLSX Spreadsheet");

		Option expOpt = new Option("e", "export", false, "Do exports only (not with import)");
		expOpt.setRequired(false);
		cmdOpts.addOption(expOpt);

		Option impOpt = new Option("i", "import", false, "Do imports only (not with export)");
		impOpt.setRequired(false);
		cmdOpts.addOption(impOpt);

		Option dbp = new Option("x", "debug", true,
				"Print out loads of helpful stuff: 0 - Errors, 1 - And Warnings, 2 - And Info, 3 - And Debugging, 4 - And Network");
		dbp.setRequired(false);
		cmdOpts.addOption(dbp);

		Option remakeOpt = new Option("r", "remake", false, "Remake target boards by archiving old and adding new");
		remakeOpt.setRequired(false);
		cmdOpts.addOption(remakeOpt);

		Option removeOpt = new Option("R", "remove", false, "Remove target boards");
		removeOpt.setRequired(false);
		cmdOpts.addOption(removeOpt);

		Option eraseOpt = new Option("d", "delete", false, "Delete cards on target boards");
		eraseOpt.setRequired(false);
		cmdOpts.addOption(eraseOpt);

		Option askOpt = new Option("F", "fresh", false, "Fresh Start of all steps and changes needed");
		askOpt.setRequired(false);
		cmdOpts.addOption(askOpt);

		Option tasktopOpt = new Option("t", "tasktop", false, "Follow External Links to delete remote artifacts");
		tasktopOpt.setRequired(false);
		cmdOpts.addOption(tasktopOpt);

		Option layoutOpt = new Option("l", "layout", false, "Update layoput of target boards");
		layoutOpt.setRequired(false);
		cmdOpts.addOption(layoutOpt);

		Option lvlOpt = new Option("v", "level", true, "Update levels on target system");
		lvlOpt.setRequired(false);
		cmdOpts.addOption(lvlOpt);

		Option renOpt = new Option("n", "name", true, "Board Name extension to apply on create");
		renOpt.setRequired(false);
		cmdOpts.addOption(renOpt);

		Option archiveOpt = new Option("O", "archived", false, "Include older Archived cards in export (if present)");
		archiveOpt.setRequired(false);
		cmdOpts.addOption(archiveOpt);

		Option tasksOpt = new Option("T", "tasks", false, "Include Task cards in export (if present)");
		tasksOpt.setRequired(false);
		cmdOpts.addOption(tasksOpt);

		Option attsOpt = new Option("A", "attachments", false,
				"Export card attachments in local filesystem (if present)");
		attsOpt.setRequired(false);
		cmdOpts.addOption(attsOpt);

		Option comsOpt = new Option("C", "comments", false, "Export card comments in local filesystem (if present)");
		comsOpt.setRequired(false);
		cmdOpts.addOption(comsOpt);

		Option originOpt = new Option("S", "origin", false, "Add comment for source artifact recording");
		originOpt.setRequired(false);
		cmdOpts.addOption(originOpt);

		Option epicOpt = new Option("c", "epics", false, "Do not import certain types");
		epicOpt.setRequired(false);
		cmdOpts.addOption(epicOpt);

		try {
			cmdLn = p.parse(cmdOpts, args, true);

		} catch (ParseException e) {
			// Not expecting to ever come here, but compiler needs something....
			d.p(Debug.ERROR, "(13): %s\n", e.getMessage());
			hf.printHelp(" ", cmdOpts);
			System.exit(5);
		}

		if (cmdLn.hasOption("debug")) {
			String optVal = cmdLn.getOptionValue("debug");
			if (optVal != null) {
				config.debugLevel = Integer.parseInt(optVal);
				d.setLevel(config.debugLevel);
			} else {
				config.debugLevel = 99;
			}
		}

		config.xlsxfn = cmdLn.getOptionValue("filename");

		// Do the exports
		if (cmdLn.hasOption("export")) {
			config.exporter = true;
		}

		// Do the imports
		if (cmdLn.hasOption("import")) {
			config.importer = true;
		}

		// What to export/import
		if (cmdLn.hasOption("archived")) {
			config.exportArchived = true;
		}
		if (cmdLn.hasOption("tasks")) {
			config.exportTasks = true;
		}
		if (cmdLn.hasOption("comments")) {
			config.exportComments = true;
		}
		if (cmdLn.hasOption("tasktop")) {
			config.tasktop = true;
		}
		if (cmdLn.hasOption("attachments")) {
			config.exportAttachments = true;
		}
		if (cmdLn.hasOption("origin")) {
			config.addComment = true;
		}

		// Option to push through all work regardless
		if (cmdLn.hasOption("fresh")) {
			config.obliterate = true;
		}

		// Archive the destination board
		if (cmdLn.hasOption("remake")) {
			config.remakeBoard = true;
		}

		// Delete the destination board
		if (cmdLn.hasOption("remove")) {
			config.eraseBoard = true;
		}

		// Update the layout of the destination board to match the src
		if (cmdLn.hasOption("layout")) {
			config.updateLayout = true;
		}
		// Update the levels of the destination system to match the src
		if (cmdLn.hasOption("level")) {
			config.updateLevels = true;
		}

		// Delete all the cards on the destination board
		if (cmdLn.hasOption("delete")) {
			config.deleteCards = true;
		}
		// Specific demoarea use-case
		if (cmdLn.hasOption("epics")) {
			config.ignoreCards = true;
		}
		// Specific demoarea use-case
		if (cmdLn.hasOption("name")) {
			config.nameExtension = true;
			String optVal = cmdLn.getOptionValue("name");
			if (optVal != null) {
				config.extension = optVal;

			} else {
				config.extension = "";
			}
		}

	}

	/**
	 * Check if the XLSX file provided has the correct sheets and we can parse the
	 * details we need
	 */
	public static void checkXlsx() {
		// Check we can open the file
		FileInputStream xlsxfis = null;
		try {
			xlsxfis = new FileInputStream(new File(config.xlsxfn));
		} catch (FileNotFoundException e) {
			d.p(Debug.ERROR, "(4) %s", e.getMessage());

			System.exit(6);

		}
		try {
			config.wb = new XSSFWorkbook(xlsxfis);
			xlsxfis.close();
		} catch (IOException e) {
			d.p(Debug.ERROR, "(5) %s", e.getMessage());
			System.exit(7);
		}

		configSht = config.wb.getSheet("Config");
		if (configSht == null) {
			d.p(Debug.ERROR, "%s", "Did not detect required sheet in the spreadsheet: \"Config\"");
			System.exit(8);
		}
		if (configSht.getLastRowNum() < 1) {
			d.p(Debug.ERROR, "%s", "Did not detect enough rows (header+1) in sheet in the spreadsheet: \"Config\"");
			System.exit(15);
		}
	}

	private static HashMap<String, Integer> chkConfigFromFile() {
		// Make the contents of the file lower case before comparing with these.

		Field[] p = (new Access()).getClass().getDeclaredFields();

		// Use fields as the ones we need to have in the spreadsheet for both src and
		// dst
		ArrayList<String> cols = new ArrayList<String>();
		for (int i = 0; i < p.length; i++) {
			p[i].setAccessible(true); // Set this up for later
			cols.add("src" + p[i].getName());
			cols.add("dst" + p[i].getName());
		}
		cols.add(InternalConfig.DESTINATION_TID_COLUMN);

		if (config.tasktop) {
			cols.add(InternalConfig.DESTINATION_ADO_USER);
			cols.add(InternalConfig.DESTINATION_ADO_TOKEN);
			cols.add(InternalConfig.DESTINATION_JIRA_USER);
			cols.add(InternalConfig.DESTINATION_JIRA_KEY);
		}

		HashMap<String, Integer> fieldMap = new HashMap<>();

		// Assume that the titles are the first row
		Iterator<Row> ri = configSht.iterator();
		if (!ri.hasNext()) {
			d.p(Debug.ERROR, "%s", "Did not detect any header info on Config sheet (first row!)");
			System.exit(9);
		}
		Row hdr = ri.next();
		Iterator<Cell> cptr = hdr.cellIterator();

		while (cptr.hasNext()) {
			Cell cell = cptr.next();
			Integer idx = cell.getColumnIndex();
			String cellName = cell.getStringCellValue().trim();
			if (cols.contains(cellName)) {
				fieldMap.put(cellName, idx); // Store the column index of the field
			}
		}

		if (fieldMap.size() != cols.size()) {
			d.p(Debug.ERROR, "%s", "Did not detect correct columns on Config sheet: " + cols.toString());
			System.exit(10);
		}

		if (!ri.hasNext()) {
			d.p(Debug.ERROR, "%s",
					"Did not detect any potential transfer info on Config sheet (first cell must be non-blank, e.g. url to a real host)");
			System.exit(11);
		}

		return fieldMap;
	}
}
