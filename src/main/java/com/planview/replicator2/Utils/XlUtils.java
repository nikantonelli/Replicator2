package com.planview.replicator2.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

import com.planview.replicator2.Leankit.Board;
import com.planview.replicator2.Leankit.BoardBulkAccessId;
import com.planview.replicator2.Leankit.BoardUser;
import com.planview.replicator2.Leankit.Card;
import com.planview.replicator2.Leankit.CardType;
import com.planview.replicator2.Leankit.CustomField;
import com.planview.replicator2.Leankit.CustomIcon;
import com.planview.replicator2.Leankit.Lane;
import com.planview.replicator2.Leankit.User;
import com.planview.replicator2.System.AccessConfig;
import com.planview.replicator2.System.ChangesColumns;
import com.planview.replicator2.System.ColNames;
import com.planview.replicator2.System.Configuration;
import com.planview.replicator2.System.InternalConfig;

public class XlUtils {
	public static Debug d = new Debug(); // Use setLevel in your top level code

	/**
	 * First put all the spreadsheet related routines here:
	 */

	public static Boolean notIgnoreType(InternalConfig cfg, String type) {
		// Demo reset use
		Boolean runAction = true;
		if (cfg.ignTypes != null) {
			for (int i = 0; i < cfg.ignTypes.length; i++) {
				if (type != null) {
					if (type.equals(cfg.ignTypes[i])) {
						runAction = false;
					}
				}
			}
		}
		return runAction;
	}

	public static InternalConfig setConfig(InternalConfig config, Row row, HashMap<String, Integer> fieldMap) {

		d.setLevel(config.debugLevel);

		config.validConfig = true; // Reset prior to the tests below

		if (config.nameExtension) {
			NumberFormat nf = DecimalFormat.getInstance();
			nf.setMaximumFractionDigits(0);
			Cell cell = row.getCell(fieldMap.get(InternalConfig.DESTINATION_TID_COLUMN));
			if (cell == null) {
				config.validConfig = false;
				return config;
			}
			switch (cell.getCellType()) {
				case NUMERIC: {
					config.oldExtension = nf.format(
							cell.getNumericCellValue());
					break;
				}
				default: {
					config.oldExtension = cell
							.getStringCellValue();
				}
			}

			cell.setCellValue(config.extension);
			XSSFFormulaEvaluator.evaluateAllFormulaCells(config.wb);
		}

		Cell uc, bc, ac;
		uc = row.getCell(fieldMap.get(InternalConfig.SOURCE_URL_COLUMN));
		bc = row.getCell(fieldMap.get(InternalConfig.SOURCE_BOARDNAME_COLUMN));
		ac = row.getCell(fieldMap.get(InternalConfig.SOURCE_APIKEY_COLUMN));

		if ((uc == null) || (bc == null) || (ac == null)) {
			config.validConfig = false;
			return config;
		}
		String u, b, a;
		u = uc.getStringCellValue();
		b = bc.getStringCellValue();
		a = ac.getStringCellValue();
		if ((u.length() == 0) || (b.length() == 0) || (a.length() == 0)) { // Excel cell is there but empty
			config.validConfig = false;
			return config;
		}
		config.source = new AccessConfig(u, b, a);
		uc = row.getCell(fieldMap.get(InternalConfig.DESTINATION_URL_COLUMN));
		bc = row.getCell(fieldMap.get(InternalConfig.DESTINATION_BOARDNAME_COLUMN));
		ac = row.getCell(fieldMap.get(InternalConfig.DESTINATION_APIKEY_COLUMN));

		if ((uc == null) || (bc == null) || (ac == null)) {
			config.validConfig = false;
			return config;
		}
		u = uc.getStringCellValue();
		b = bc.getStringCellValue();
		a = ac.getStringCellValue();
		if ((u.length() == 0) || (b.length() == 0) || (a.length() == 0)) { // Excel cell is there but empty
			config.validConfig = false;
			return config;
		}
		config.destination = new AccessConfig(u, b, a);

		if (config.tasktop) {
			config.jira = new AccessConfig(
					"", // URL is fully encoded into externalLink
					null, // BoardName not relevant
					row.getCell(fieldMap.get(InternalConfig.DESTINATION_JIRA_KEY)) != null
							? row.getCell(fieldMap.get(InternalConfig.DESTINATION_JIRA_KEY)).getStringCellValue()
							: null);
			config.jira.setUser(row.getCell(fieldMap.get(InternalConfig.DESTINATION_JIRA_USER)) != null
					? row.getCell(fieldMap.get(InternalConfig.DESTINATION_JIRA_USER)).getStringCellValue()
					: null);
			config.ado = new AccessConfig(
					"", // URL is fully encoded into externalLink
					null, // BoardName not relevant
					row.getCell(fieldMap.get(InternalConfig.DESTINATION_ADO_TOKEN)) != null
							? row.getCell(fieldMap.get(InternalConfig.DESTINATION_ADO_TOKEN)).getStringCellValue()
							: null);
			config.ado.setUser(row.getCell(fieldMap.get(InternalConfig.DESTINATION_ADO_USER)) != null
					? row.getCell(fieldMap.get(InternalConfig.DESTINATION_ADO_USER)).getStringCellValue()
					: null);
		}

		if (config.ignoreCards) {
			// Find if column "Import Ignore" exists
			Integer ignCol = XlUtils.findColumnFromSheet(config.wb.getSheet("Config"), InternalConfig.IGNORE_LIST);
			if (ignCol != null) {
				Cell cl = row.getCell(ignCol);
				if (cl != null) {
					String typesString = row.getCell(ignCol).getStringCellValue();
					// Does the cell have anything in it?
					if (typesString != null) {
						config.ignTypes = typesString.split(",");
						// Trim all whitespace that the user might have left in
						for (int i = 0; i < config.ignTypes.length; i++) {
							config.ignTypes[i] = config.ignTypes[i].trim();
						}
					}
				}
			}
		}
		return config;
	}

	public static String validateSheetName(String longSheetName) {
		String shortName = longSheetName.replaceAll("[^a-zA-Z0-9_]", "_");
		if (longSheetName.length() >= 32) {
			shortName = longSheetName.substring(0, 15) + ".." + longSheetName.substring(longSheetName.length() - 14);
		}
		return shortName;
	}

	public static ArrayList<Row> getRowsByStringValue(InternalConfig cfg, XSSFSheet sht, String name, String value) {
		ArrayList<Row> list = new ArrayList<>();

		// Check for daft stuff.
		if (sht == null) {
			d.p(Debug.ERROR, "getRowsByStringValue() passed null sheet\n");
			return new ArrayList<>();
		}
		Integer cellIdx = findColumnFromSheet(sht, name);
		if (cellIdx < 0) {
			d.p(Debug.ERROR, "getRowsByStringValue() passed incorrect field name\n");
			return new ArrayList<>();
		}

		Iterator<Row> iRow = sht.iterator();
		while (iRow.hasNext()) {
			Row row = iRow.next();
			Cell rCell = row.getCell(cellIdx);
			FormulaEvaluator evaluator = cfg.wb.getCreationHelper().createFormulaEvaluator();
			CellValue cValue = evaluator.evaluate(rCell);
			if (cValue != null) {
				if (cValue.getCellType().equals(CellType.STRING)) {
					if (cValue.getStringValue().equals(value)) {
						list.add(row);
					}
				} else if (cValue.getCellType().equals(CellType.NUMERIC)) {
					if (Double.toString(cValue.getNumberValue()).equals(value)) {
						list.add(row);
					}
				}
			}
		}
		return list;
	}

	public static XSSFSheet newChgSheet(InternalConfig cfg, String cShtName) {
		// Make a new one
		XSSFSheet changesSheet = cfg.wb.createSheet(cShtName);

		/**
		 * Create the Changes Sheet layout
		 */

		int chgCellIdx = 0;
		Row chgHdrRow = changesSheet.createRow(0);

		// These next lines are the fixed format of the Changes sheet
		chgHdrRow.createCell(chgCellIdx++, CellType.STRING).setCellValue(ColNames.GROUP);

		Integer col = chgCellIdx;
		chgHdrRow.createCell(chgCellIdx++, CellType.STRING).setCellValue(ColNames.ITEM_ROW);
		changesSheet.setColumnWidth(col, 18 * 256); // Set the width so that the ID string is fully visible

		chgHdrRow.createCell(chgCellIdx++, CellType.STRING).setCellValue(ColNames.ACTION);
		chgHdrRow.createCell(chgCellIdx++, CellType.STRING).setCellValue(ColNames.FIELD);

		col = chgCellIdx;
		chgHdrRow.createCell(chgCellIdx++, CellType.STRING).setCellValue(ColNames.VALUE);
		changesSheet.setColumnWidth(col, 30 * 256);

		return changesSheet;
	}

	public static ChangesColumns checkChangeSheetColumns(XSSFSheet changesSht) {
		if (changesSht == null)
			return null;
		ChangesColumns cc = new ChangesColumns();
		cc.group = findColumnFromSheet(changesSht, ColNames.GROUP);
		cc.row = findColumnFromSheet(changesSht, ColNames.ITEM_ROW);
		cc.action = findColumnFromSheet(changesSht, ColNames.ACTION);
		cc.field = findColumnFromSheet(changesSht, ColNames.FIELD);
		cc.value = findColumnFromSheet(changesSht, ColNames.VALUE);

		if ((cc.group == null) || (cc.row == null) || (cc.action == null) || (cc.field == null)
				|| (cc.value == null)) {
			d.p(Debug.ERROR,
					"Could not find all required columns in %s sheet: \"%s\", \"%s\", \"%s\", \"%s\", \"%s\"\n",
					changesSht.getSheetName(),
					ColNames.GROUP,
					ColNames.ITEM_ROW,
					ColNames.ACTION,
					ColNames.FIELD,
					ColNames.VALUE);
			return null;
		}
		return cc;
	}

	public static Integer findRowIdxByStringValue(XSSFSheet itemSht, String fieldName, String value) {
		for (int rowIndex = 1; rowIndex <= itemSht.getLastRowNum(); rowIndex++) {
			Row row = itemSht.getRow(rowIndex);
			if (row != null
					&& row.getCell(findColumnFromSheet(itemSht, fieldName)).getStringCellValue().equals(value)) {
				return rowIndex;
			}
		}
		return null;
	}

	public static Row findRowByStringValue(XSSFSheet itemSht, String fieldName, String value) {
		for (int rowIndex = 1; rowIndex <= itemSht.getLastRowNum(); rowIndex++) {
			Row row = itemSht.getRow(rowIndex);
			if ((row != null)
					&& (row.getCell(findColumnFromSheet(itemSht, fieldName)) != null)
					&& row.getCell(findColumnFromSheet(itemSht, fieldName)).getStringCellValue().equals(value)) {
				return row;
			}
		}
		return null;
	}

	public static Integer findColumnFromName(Row firstRow, String name) {
		Iterator<Cell> frtc = firstRow.iterator();
		// First, find the column that the "Day Delta" info is in
		int dayCol = -1;
		int td = 0;

		while (frtc.hasNext()) {
			Cell tc = frtc.next();
			if (!tc.getStringCellValue().equals(name)) {
				td++;
			} else {
				dayCol = td;
				break;
			}
		}
		return dayCol;
	}

	public static Integer findColumnFromSheet(XSSFSheet sht, String name) {
		Iterator<Row> row = sht.iterator();
		if (!row.hasNext()) {
			return null;
		}
		Row firstRow = row.next(); // Get the header row
		Integer col = findColumnFromName(firstRow, name);
		if (col < 0) {
			return null;
		}
		return col;
	}

	public static Boolean parseRow(Row drRow, Configuration cfg, Field[] p, HashMap<String, Object> fieldMap,
			ArrayList<String> cols) {
		String cv = drRow.getCell((int) (fieldMap.get(cols.get(0)))).getStringCellValue();
		if (cv != null) {

			for (int i = 0; i < cols.size(); i++) {
				String idx = cols.get(i);
				Object obj = fieldMap.get(idx);
				String val = obj.toString();
				try {
					Cell cell = drRow.getCell(Integer.parseInt(val));

					if (cell != null) {
						switch (cell.getCellType()) {
							case STRING:
								// When you copy'n'paste on WIndows, it sometimes picks up the whitespace too -
								// so remove it.
								p[i].set(cfg,
										(cell != null ? drRow.getCell(Integer.parseInt(val)).getStringCellValue().trim()
												: ""));
								break;
							case NUMERIC:
								p[i].set(cfg, (cell != null ? drRow.getCell(Integer.parseInt(val)).getNumericCellValue()
										: ""));
								break;
							default:
								break;
						}
					} else {
						p[i].set(cfg, (p[i].getType().equals(String.class)) ? "" : 0.0);
					}

				} catch (IllegalArgumentException | IllegalAccessException e) {
					d.p(Debug.ERROR, "Conversion error on \"%s\": Verify cell type in Excel\n %s\n", idx,
							e.getMessage());
					System.exit(12);
				}

			}
			return true;
		}
		return false;
	}

	public static String findColumnLetterFromSheet(XSSFSheet sht, String name) {
		Iterator<Row> row = sht.iterator();
		if (!row.hasNext()) {
			return null;
		}
		Row firstRow = row.next(); // Get the header row
		Integer col = findColumnFromName(firstRow, name);
		if (col < 0) {
			return null;
		}
		return CellReference.convertNumToColString(col);
	}

	public static Object getCell(Row change, Integer col) {

		if (change.getCell(col) != null) {
			// Need to get the correct type of field
			if (change.getCell(col).getCellType() == CellType.FORMULA) {
				if (change.getCell(col).getCachedFormulaResultType() == CellType.STRING) {
					return change.getCell(col).getStringCellValue();
				} else if (change.getCell(col).getCachedFormulaResultType() == CellType.NUMERIC) {
					if (DateUtil.isCellDateFormatted(change.getCell(col))) {
						SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd");
						Date date = change.getCell(col).getDateCellValue();
						return dtf.format(date).toString();
					} else {
						return (int) change.getCell(col).getNumericCellValue();
					}
				}
			} else if (change.getCell(col).getCellType() == CellType.STRING) {
				return change.getCell(col).getStringCellValue();
			} else if (change.getCell(col).getCellType() == CellType.BLANK) {
				return null;
			} else {
				if (DateUtil.isCellDateFormatted(change.getCell(col))) {
					SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd");
					Date date = change.getCell(col).getDateCellValue();
					return dtf.format(date).toString();
				} else {
					return change.getCell(col).getNumericCellValue();
				}
			}
		}
		return null;
	}

	public static void copyRow(Row row, Row ldr) {
		Iterator<Cell> lsrci = row.iterator();
		int lcellIdx = 0;
		while (lsrci.hasNext()) {
			Cell srcCell = lsrci.next();
			Cell dstCell = ldr.createCell(lcellIdx++, srcCell.getCellType());

			switch (srcCell.getCellType()) {
				case STRING: {
					dstCell.setCellValue(srcCell.getStringCellValue());
					break;
				}
				case NUMERIC: {
					dstCell.setCellValue(srcCell.getNumericCellValue());
					break;
				}
				case FORMULA: {
					dstCell.setCellFormula(srcCell.getCellFormula());
					break;
				}
				default: {
					break;
				}
			}
		}
	}

	/**
	 * @param iCfg
	 * @param xlsxfn
	 * @param wb
	 */
	static public void writeFile(InternalConfig iCfg, String xlsxfn, XSSFWorkbook wb) {

		Boolean donePrint = true;
		Integer loopCnt = 12;
		while (loopCnt > 0) {
			FileOutputStream oStr = null;
			try {
				oStr = new FileOutputStream(xlsxfn);
				try {
					wb.write(oStr);
					try {
						oStr.close();
						oStr = null;
						loopCnt = 0;
					} catch (IOException e) {
						d.p(Debug.ERROR, "%s while closing file %s\n", e, xlsxfn);
					}
				} catch (IOException e) {
					d.p(Debug.ERROR, "%s while writing file %s\n", e, xlsxfn);
					oStr.close(); // If this fails, just give up!
				}
			} catch (IOException e) {
				d.p(Debug.ERROR, "%s while opening/closing file %s\n", e, xlsxfn);
			}
			if (loopCnt == 0) {
				break;
			}

			Calendar now = Calendar.getInstance();
			Calendar then = Calendar.getInstance();
			then.add(Calendar.SECOND, 5);
			Long timeDiff = then.getTimeInMillis() - now.getTimeInMillis();
			if (donePrint) {
				d.p(Debug.ALWAYS, "File \"%s\" in use. Please close to let this program continue\n", xlsxfn);
				donePrint = false;
			}
			try {
				Thread.sleep(timeDiff);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			--loopCnt;
		}
	}

	/**
	 * If cardId is null, we assume this is a card on a board If non-null, then this
	 * is a task on a card
	 * 
	 * @param cfg
	 * @param accessCfg
	 * @param fieldLst
	 * @param item
	 * @param cardId
	 * @return JSONObject ready for passing to a LeanKitAccess call.
	 * 
	 */
	public static JSONObject jsonCardFromRow(InternalConfig cfg, AccessConfig accessCfg, JSONObject fieldLst, Row item,
			String cardId) {
		JSONObject flds = new JSONObject();

		ArrayList<CustomField> customF = new ArrayList<>();
		Iterator<String> keyIt = fieldLst.keys();
		while (keyIt.hasNext()) {
			String key = keyIt.next();
			switch (key) {
				/**
				 * Don't include if not present
				 * 
				 */
				case ColNames.ASSIGNED_USERS: {
					/**
					 * We need to try and match the email address in the destination and fetch the
					 * userID
					 */
					Object fv = getCell(item, fieldLst.getInt(key));

					if (fv != null) {
						String usersList = (String) fv;
						ArrayList<BoardUser> boardUsers = LkUtils.getUsers(cfg, accessCfg); // Get the board users
						if (boardUsers != null) {

							if (usersList != null) {
								String[] users = usersList.split(",");
								ArrayList<String> usersToPut = new ArrayList<String>();
								String usernames = "";
								for (int i = 0; i < users.length; i++) {
									User realUser = LkUtils.getUserByName(cfg, accessCfg, users[i]);
									if (realUser != null) {
										// Check if they are a board user so we don't error.
										for (int j = 0; j < boardUsers.size(); j++) {
											if (realUser.id.equals(boardUsers.get(j).userId)) {
												usersToPut.add(realUser.id);
												usernames += (i != 0) ? "," : "" + users[i];
											}
										}
									} else {
										d.p(Debug.WARN, "Cannot locate assignedUser: %s on system \"%s\"\n", users[i],
												accessCfg.getUrl());
									}
								}
								if (usersToPut.size() > 0) {
									Board brd = LkUtils.getBoardByTitle(cfg, accessCfg);
									BoardBulkAccessId bba = new BoardBulkAccessId();
									String[] bids = {};
									bba.boardIds = (String[]) ArrayUtils.add(bids, brd.id);
									bba.userIds = usersToPut.toArray(new String[0]);
									bba.boardRole = "boardUser";

									d.p(Debug.INFO, "Adding users \"%s\" to board \"%s\"\n", usernames, brd.title);
									LkUtils.updateBoardUsers(cfg, accessCfg, bba);
									flds.put("assignedUserIds", usersToPut.toArray());
								}
							}
						}
					}
					break;
				}

				case ColNames.BLOCKED_REASON: {
					String reason = (String) getCell(item, fieldLst.getInt(key));
					if ((reason != null) && !reason.equals("")) {

						flds.put(key, reason);
					} else {
						continue;
					}
					break;
				}
				case ColNames.CUSTOM_ICON: {
					// Incoming customIcon value is a name. We need to translate to
					// an id
					String iconName = (String) getCell(item, fieldLst.getInt(key));
					CustomIcon ci = null;
					if (fieldLst.has("boardId")) {
						ci = LkUtils.getCustomIcon(cfg, cfg.destination, iconName,
								item.getCell(fieldLst.getInt("boardId")).getStringCellValue());
					} else {
						ci = LkUtils.getCustomIcon(cfg, cfg.destination, iconName);
					}

					if (ci != null) {
						flds.put("customIconId", ci.id);
					}
					break;
				}
				case ColNames.EXTERNAL_LINK: {
					String link = (String) getCell(item, fieldLst.getInt(key));
					if (link != null) {
						if (!link.isEmpty()) {
							String[] bits = link.split(",");
							JSONObject el = new JSONObject();

							switch (bits.length) {
								case 2: {
									el.put("label", bits[0]);
									el.put("url", bits[1]);
									break;
								}
								case 1: {
									el.put("label", "");
									el.put("url", bits[0]);
									break;
								}
								default:
									break;
							}
							if (el.has("url")) {
								flds.put(key, el);
							}
						}
					}
					break;
				}
				case ColNames.LANE: {
					Lane lane = null;
					String laneType = (String) getCell(item, fieldLst.getInt(key));
					if (cardId != null) {
						if (laneType.isEmpty()) {
							laneType = "ready";
						}
						lane = LkUtils.getLaneFromCard(cfg, accessCfg, cardId, laneType);
						if (lane != null) {
							flds.put("laneType", lane.laneType);
						} else {
							flds.put("laneType", laneType);
						}
					} else {
						String[] bits = laneType.split(InternalConfig.SPLIT_WIP_REGEX_CHAR);
						if (fieldLst.has("boardId")) {
							lane = LkUtils.getLaneFromBoardId(cfg, accessCfg,
									item.getCell(fieldLst.getInt("boardId")).getStringCellValue(), bits[0]);
						} else {
							lane = LkUtils.getLaneFromBoardTitle(cfg, accessCfg, accessCfg.getBoardName(), bits[0]);
						}
						if (lane != null) {
							flds.put("laneId", lane.id);
							if (bits.length > 1) {
								flds.put("wipOverrideComment", bits[1]);
							}
						}
					}
					break;
				}
				case ColNames.SIZE:
					// The index will be set by the exporter in extra 'Modify' rows. This is here
					// for manually created (import) spreadsheets
				case ColNames.INDEX: {
					Integer digits = ((Double) getCell(item, fieldLst.getInt(key))).intValue();
					if (digits != null) {
						flds.put(key, digits);
					}
					break;
				}

				/**
				 * Tags need to be as an array of strings
				 */
				case ColNames.TAGS: {
					String tagLine = (String) getCell(item, fieldLst.getInt(key));
					if ((tagLine != null) && !tagLine.equals("")) {
						String[] tags = tagLine.split(",");
						flds.put("tags", tags);
					}
					break;
				}
				case ColNames.TYPE: {
					String cardtype = (String) getCell(item, fieldLst.getInt(key));
					ArrayList<CardType> cts = null;
					if (fieldLst.has("boardId")) {
						cts = LkUtils.getCardTypesFromBoard(cfg, cfg.destination,
								item.getCell(fieldLst.getInt("boardId")).getStringCellValue());
					} else {
						cts = LkUtils.getCardTypesFromBoard(cfg, cfg.destination, cfg.destination.getBoardName());
					}
					CardType ct = LkUtils.getCardTypeFromList(cts, cardtype);
					if (ct != null) {
						flds.put("typeId", ct.getId());
					}
					break;
				}
				default: {
					// See if the field is part of the standard list of fields. If not, it's a
					// custom field

					Card c = new Card();
					Field[] validFields = c.getClass().getFields();
					Boolean found = false;
					for (int i = 0; i < validFields.length; i++) {
						if (validFields[i].getName().equals(key)) {
							found = true;
						}
					}
					if (found) {
						if (item.getCell(fieldLst.getInt(key)) != null) {
							Object obj = getCell(item, fieldLst.getInt(key));
							if (obj != null)
								flds.put(key, obj);
						}
					} else {
						CustomField[] customFields = null;

						if (fieldLst.has("boardId")) {
							customFields = LkUtils.getCustomFields(cfg, cfg.destination,
									item.getCell(fieldLst.getInt("boardId")).getStringCellValue());
						} else {
							customFields = LkUtils.getCustomFields(cfg, cfg.destination);
						}
						CustomField cf = new CustomField();
						for (int i = 0; i < customFields.length; i++) {
							if (customFields[i].label.equals(key)) {
								cf.fieldId = customFields[i].id;
								cf.value = getCell(item, fieldLst.getInt(key));
								if (cf.value != null) {
									customF.add(cf);
								}
							}
						}
					}

					break;
				}
			}

		}

		if (customF.size() > 0) {
			// Create a entry to push the custom fields in
			JSONArray jsa = new JSONArray();
			for (int i = 0; i < customF.size(); i++) {
				JSONObject jso = new JSONObject();
				jso.put("fieldId", customF.get(i).fieldId);
				jso.put("value", customF.get(i).value);
				jsa.put(jso);
			}
			flds.put("customFields", jsa);
		}
		return flds;
	}

	/**
	 * Scan through the list of source boards, find the itemSheet for each one and
	 * check
	 * for an entry that matches title. Then use that sheetname (aka boardName) to
	 * find
	 * the source to destination translation and then get the destination board Id
	 * to
	 * see if the card already exists. If so, get that card, else return null.
	 * 
	 * @param cfg
	 * @param parentId
	 * @return
	 */
	public static Card findCardByTitle(InternalConfig cfg, String parentId) {
		XSSFSheet cSht = cfg.wb.getSheet("Config");
		Integer sCol = findColumnFromSheet(cSht, InternalConfig.SOURCE_BOARDNAME_COLUMN);
		Integer dCol = findColumnFromSheet(cSht, InternalConfig.DESTINATION_BOARDNAME_COLUMN);

		Iterator<Row> rIter = cSht.iterator();
		rIter.next(); // Skip headers
		while (rIter.hasNext()) {
			Row row = rIter.next();
			// Get the sheet with the same name as the board
			XSSFSheet st = cfg.wb.getSheet(XlUtils.validateSheetName(row.getCell(sCol).getStringCellValue()));
			if (st != null) {
				Row targ = findRowByStringValue(st, ColNames.TITLE, parentId);
				if (targ != null) {
					String brdName = row.getCell(dCol).getStringCellValue();
					Card crd = LkUtils.getCardByTitle(cfg, cfg.destination, brdName, parentId);
					if (crd != null)
						return crd;
				}
			}
		}
		return null;
	}

	public static ArrayList<Card> getDstCards(InternalConfig cfg) {
		ArrayList<Card> cards = new ArrayList<>();
		XSSFSheet itemSht = cfg.wb.getSheet(XlUtils.validateSheetName(cfg.source.getBoardName()));
		if (itemSht != null) {
			Integer idCol = findColumnFromSheet(itemSht, ColNames.ID);
			Integer titleCol = findColumnFromSheet(itemSht, ColNames.TITLE);

			for (int rowIndex = 1; rowIndex <= itemSht.getLastRowNum(); rowIndex++) {
				Row row = itemSht.getRow(rowIndex);
				Cell idCell = row.getCell(idCol);
				Cell titleCell = row.getCell(titleCol);
				if ((idCell != null) && (titleCell != null)
						&& (idCell.getCellType().equals(CellType.STRING))
						&& (titleCell.getCellType().equals(CellType.STRING))) {
					Card cd = new Card();
					cd.setId(row.getCell(idCol).getStringCellValue());
					cd.setTitle(row.getCell(titleCol).getStringCellValue());
					cards.add(cd);
				}
			}
		}

		return cards;
	}
}
