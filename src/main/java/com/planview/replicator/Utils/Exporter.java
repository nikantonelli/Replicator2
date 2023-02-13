package com.planview.replicator.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import com.planview.replicator.Changes;
import com.planview.replicator.ColNames;
import com.planview.replicator.Debug;
import com.planview.replicator.InternalConfig;
import com.planview.replicator.SupportedXlsxFields;
import com.planview.replicator.leankit.Attachment;
import com.planview.replicator.leankit.BlockedStatus;
import com.planview.replicator.leankit.Board;
import com.planview.replicator.leankit.Card;
import com.planview.replicator.leankit.CardType;
import com.planview.replicator.leankit.Comment;
import com.planview.replicator.leankit.CustomField;
import com.planview.replicator.leankit.CustomIcon;
import com.planview.replicator.leankit.CustomId;
import com.planview.replicator.leankit.ExternalLink;
import com.planview.replicator.leankit.ItemType;
import com.planview.replicator.leankit.Lane;
import com.planview.replicator.leankit.ParentCard;
import com.planview.replicator.leankit.ParentChild;
import com.planview.replicator.leankit.PlanningIncrement;
import com.planview.replicator.leankit.Task;
import com.planview.replicator.leankit.User;

/**
 * We need to get a unique board ID from the user or allow for a selection
 * mechanism Then retrieve all the 'cards' on the board. We don't need to know
 * what type they are during selection, but need the info for the export.
 * 
 * The importer program gets data from an excel spreadsheet. We cannot create
 * all the info needed for the config sheet. What we could do though, is to
 * create a sheet for each card type (e.g. Epic, Stories, Tasks).
 * 
 * We can ask each card whether it has subtasks and create entrires to add those
 * (when importer has that functionality added).
 * 
 * Parents are easy - we can do it in a single pass: 1- when parents is found
 * with children cards check whether the children exist and make the connection
 * 2- when cards with parents are found check whether the parent(s) exists and
 * add the child
 * 
 */
public class Exporter {
	public static InternalConfig cfg = null;

	int itmRowIdx = 0;
	int chgRowIdx = 0;

	Integer chgShtIdx = -1; // Set to invalid as a precaution for misuse.

	ArrayList<ParentChild> parentChild = new ArrayList<>();
	Debug d = new Debug();

	public Exporter(InternalConfig config) {
		cfg = config;
		d.setLevel(cfg.debugLevel);
		XlUtils.d.setLevel(cfg.debugLevel);
	}

	public void go() {
		d.p(Debug.ALWAYS, "Starting Export of \"%s\" at: %s\n", cfg.source.getBoardName(), new Date());
		doExport(setUpNewSheets(cleanSheets()));
	}

	public String getSheetName() {
		return XlUtils.validateSheetName(InternalConfig.CHANGES_SHEET_NAME + cfg.source.getBoardName());
	}

	public String cleanSheets() {
		Integer shtIdx = null;
		String cShtName = getSheetName();

		shtIdx = cfg.wb.getSheetIndex(cShtName);
		if (shtIdx >= 0) {
			cfg.wb.removeSheetAt(shtIdx);
		}

		// Now make sure we don't have any left over item information
		shtIdx = cfg.wb.getSheetIndex(XlUtils.validateSheetName(cfg.source.getBoardName()));
		if (shtIdx >= 0) {
			cfg.wb.removeSheetAt(shtIdx);
		}
		return cShtName;

	}

	public String[] setUpNewSheets(String cShtName) {
		cfg.changesSheet = XlUtils.newChgSheet(cfg, cShtName);
		chgRowIdx = 1; // Start after header row
		return newItmSheet();
	}

	public String[] newItmSheet() {
		cfg.itemSheet = cfg.wb.createSheet(XlUtils.validateSheetName(cfg.source.getBoardName()));

		/**
		 * Now create the Item Sheet layout
		 */

		Row itmHdrRow = cfg.itemSheet.createRow(itmRowIdx++);

		int itmCellIdx = 0;
		itmHdrRow.createCell(itmCellIdx++, CellType.STRING).setCellValue(ColNames.ID);

		/**
		 * Now write out the fields
		 * There are user accessible fields - some are r/w, some are r/o (outFields)
		 * There are also fields we might want to check as part of the program, but are
		 * not exported directly (checkFields). They actually cause other things to
		 * happen,
		 * e.g. a Modify line is added to the changes sheet.
		 * 
		 * The outFields and checkFields are kept in line by this software. If you
		 * change something,
		 * make sure they are aligned
		 * 
		 */

		SupportedXlsxFields allFields = new SupportedXlsxFields();
		Field[] rwFields = (allFields.new Modifiable()).getClass().getFields(); // Public fields that will be written
																				// as columns

		Field[] pseudoFields = (allFields.new Pseudo()).getClass().getFields(); // Inlcudes pseudo fields that
																				// will
																				// cause alternative actions
		CustomField[] customFields = LkUtils.getCustomFields(cfg, cfg.source);

		Integer checkFieldsLength = ((rwFields != null) ? rwFields.length : 0) +
				((customFields != null) ? customFields.length : 0) +
				((pseudoFields != null) ? pseudoFields.length : 0);

		Integer outFieldsLength = ((rwFields != null) ? rwFields.length : 0) +
				((customFields != null) ? customFields.length : 0);

		String[] checkFields = new String[checkFieldsLength];
		Integer cfi = 0;
		String[] outFields = new String[outFieldsLength];
		Integer ofi = 0;

		if (rwFields != null) {
			for (int i = 0; i < rwFields.length; i++) {
				outFields[ofi++] = checkFields[cfi++] = rwFields[i].getName();
			}
		}
		if (customFields != null) {
			for (int i = 0; i < customFields.length; i++) {
				outFields[ofi++] = checkFields[cfi++] = customFields[i].label;
			}
		}
		if (pseudoFields != null) {
			for (int i = 0; i < pseudoFields.length; i++) {
				checkFields[cfi++] = pseudoFields[i].getName();
			}
		}

		// Put column headers out
		for (int i = 0; i < outFieldsLength; i++) {
			itmHdrRow.createCell(itmCellIdx++, CellType.STRING).setCellValue(outFields[i]);
		}

		Integer col = XlUtils.findColumnFromSheet(cfg.itemSheet, ColNames.ID);
		cfg.itemSheet.setColumnWidth(col, 18 * 256); // First two columns are usually ID and srcID
		col = XlUtils.findColumnFromSheet(cfg.itemSheet, ColNames.SOURCE_ID);
		cfg.itemSheet.setColumnWidth(col, 18 * 256);

		return checkFields;
	}

	public void doExport(String[] checkFields) {
		/**
		 * Read all the normal cards on the board - up to a limit?
		 */
		ArrayList<Card> cards = LkUtils.getCardIdsFromBoard(cfg, cfg.source);
		if (cards == null) {
			d.p(Debug.ERROR, "Cannot identify cards on board: \"%s\"", cfg.source.getBoardName());
			System.exit(4);
		}
		Collections.sort(cards);
		ArrayList<Row> addModifyRows = new ArrayList<>();
		/**
		 * Write all the cards out to the cfg.itemSheet
		 */
		Iterator<Card> ic = cards.iterator();
		while (ic.hasNext()) {
			Card c = ic.next();

			/**
			 * We have to re-fetch the cards to get the relevant parent information.
			 */
			c = LkUtils.getCard(cfg, cfg.source, c.id);

			/* Write a 'Create' line to the changes sheet */
			// We can only write out cards here. Tasks are handled differently

			createChangeRow(chgRowIdx, itmRowIdx, "Create", "", "");
			Changes changeTotal = createItemRowFromCard(chgRowIdx, itmRowIdx, c, checkFields); // checkFields contains
																								// extra
			Row thisRow = cfg.itemSheet.getRow(itmRowIdx);
			String type = thisRow.getCell(XlUtils.findColumnFromName(cfg.itemSheet.getRow(0), ColNames.TYPE))
					.getStringCellValue();

			if (!XlUtils.notIgnoreType(cfg, type)) {
				addModifyRows.add(thisRow);
			}
			chgRowIdx = changeTotal.getChangeRow();
			itmRowIdx = changeTotal.getItemRow();

			// Do these after because we have changed the index in the subr calls
			chgRowIdx++;
			itmRowIdx++;

		}

		/**
		 * Scan the ignore type cards so we can move them to the right place and update
		 * all fields
		 */
		Iterator<Row> rowItr = addModifyRows.iterator();
		while (rowItr.hasNext()) {
			Row iFirst = cfg.itemSheet.getRow(0);
			Row row = rowItr.next();
			// Get last cell from the row rather than iFirst to minimise the number of
			// Modify lines.
			short numCells = row.getLastCellNum();

			String[] colsToIgnore = {
					// These are internal
					ColNames.ID,
					ColNames.SOURCE_ID,
					// E1 sets these
					ColNames.TITLE,
					ColNames.TYPE,
					ColNames.PLANNED_END,
					ColNames.PLANNED_START,
					ColNames.EXTERNAL_LINK,
					// Can't patch color back in. It comes from the cardtype
					ColNames.COLOUR
			};
			d.p(Debug.INFO, "Creating Modify rows for: \"%s\"\n",
					row.getCell(XlUtils.findColumnFromName(iFirst, ColNames.TITLE)));
			for (int i = 0; i < numCells; i++) {
				String fieldName = iFirst.getCell(i).getStringCellValue();
				if (ArrayUtils.contains(colsToIgnore, fieldName)) {
					continue;
				}
				String letter = CellReference.convertNumToColString(i);
				Cell targetCell = row.getCell(XlUtils.findColumnFromName(iFirst, fieldName));
				boolean writeItOut = true;
				if (targetCell == null) {
					writeItOut = false;
				} else {
					if ((targetCell.getCellType() == CellType.STRING) &&
							(targetCell.getStringCellValue().length() == 0)) {
						writeItOut = false;
					}
				}

				if (writeItOut) {
					d.p(Debug.DEBUG, "    : %s to \"%s\"\n",
							fieldName, row.getCell(XlUtils.findColumnFromName(iFirst, fieldName)));
					createChangeRow(chgRowIdx++, row.getRowNum(), "Modify", fieldName,
							"='" + cfg.itemSheet.getSheetName() + "'!" + letter + (row.getRowNum() + 1));
				}
			}
		}
		/**
		 * Now scan the parent/child register and add "Modify" lines
		 */

		Iterator<ParentChild> pci = parentChild.iterator();
		while (pci.hasNext()) {
			ParentChild pc = pci.next();
			Integer parentShtIdx = cfg.wb.getSheetIndex(XlUtils.validateSheetName(pc.boardName));
			if (parentShtIdx >= 0) {
				XSSFSheet pSht = cfg.wb.getSheetAt(parentShtIdx);

				Integer parentRow = XlUtils.findRowIdxByStringValue(pSht, ColNames.TITLE, pc.parentId);
				Integer childRow = XlUtils.findRowIdxByStringValue(cfg.itemSheet, ColNames.SOURCE_ID, pc.childId);

				if ((parentRow == null) || (childRow == null)) {
					d.p(Debug.WARN, "Ignoring parent/child relationship for: %s/%s. Is parent archived?\n",
							pc.parentId, pc.childId);
				} else {

					String type = cfg.itemSheet.getRow(childRow)
							.getCell(XlUtils.findColumnFromName(cfg.itemSheet.getRow(0), ColNames.TYPE))
							.getStringCellValue();
					boolean runAction = XlUtils.notIgnoreType(cfg, type);

					if (runAction) {
						Integer col = XlUtils.findColumnFromSheet(cfg.itemSheet, ColNames.TITLE);
						String letter = CellReference.convertNumToColString(col);
						d.p(Debug.INFO, "Creating parent/child relationship for: %s/%s\n",
								pc.parentId, pc.childId);
						createChangeRow(chgRowIdx++, childRow, "Modify", "Parent",
								"='" + XlUtils.validateSheetName(pc.boardName) + "'!" + letter + (parentRow + 1));
					} else {
						d.p(Debug.INFO, "Skipping parent/child relationship for: %s/%s\n",
								pc.parentId, pc.childId);
					}
				}
			}
		}

		/**
		 * Open the output stream and send the file back out.
		 */
		XlUtils.writeFile(cfg, cfg.xlsxfn, cfg.wb);
	}

	/**
	 * All fields handled here must have mirror in createItemRowFromTask (which
	 * probably does nothing) This is because we use the one list of fields from
	 * SupportedXlsxField.java The importer will select the fields correctly for the
	 * type of item
	 * 
	 * Got a chick-and-egg situation with the indexes.
	 */
	public Changes createItemRowFromCard(Integer chgRow, Integer itmRow, Card c, String[] pbFields) {

		Integer item = itmRow;

		Row iRow = cfg.itemSheet.createRow(itmRow);
		d.p(Debug.INFO, "Creating row %d for id: %s (%s)\n", itmRow, c.id,
				((c.customId.value != null) && (c.customId.value.length() != 0)) ? c.customId.value : c.title);
		// We need to keep a separate counter for the fields we actually write out
		Integer fieldCounter = 1;
		for (int i = 0; i < pbFields.length; i++) {
			try {
				switch (pbFields[i]) {

					case "assignedUsers": {
						Object fv = c.getClass().getField(pbFields[i]).get(c);
						String outStr = "";
						if (fv != null) {
							User[] au = (User[]) fv;
							for (int j = 0; j < au.length; j++) {
								/**
								 * I have to fetch the realuser because the assignedUser != boardUser != user
								 */
								User realUser = LkUtils.getUser(cfg, cfg.source, au[j].id);
								if (realUser != null) {
									outStr += ((outStr.length() > 0) ? "," : "") + realUser.username;
								}
							}
							if (outStr.length() > 0) {
								iRow.createCell(fieldCounter, CellType.STRING).setCellValue(outStr);
							}
						}
						fieldCounter++;
						break;
					}
					case "attachments": {
						Object fv = c.getClass().getField(pbFields[i]).get(c);
						if ((fv != null) && cfg.exportAttachments) {
							Attachment[] atts = ((Attachment[]) fv);
							if (atts.length > 0) {
								/**
								 * If the attachments length is greater than zero, try to create a sub folder in
								 * the current directory called attachments. Then try to make a sub-subfolder
								 * based on the card id. Then add a file entitled based on the attachment of
								 */
								Files.createDirectories(Paths.get("attachments/" + c.id));

							}
							for (int j = 0; j < atts.length; j++) {
								File af = new File("attachments/" + c.id + "/" + atts[j].name);
								FileOutputStream fw = new FileOutputStream(af);
								byte[] data = (byte[]) LkUtils.getAttachment(cfg, cfg.source, c.id, atts[j].id);
								d.p(Debug.INFO, "Saving attachment %s\n", af.getPath());
								fw.write(data, 0, data.length);
								fw.flush();
								fw.close();
								chgRow++;

								createChangeRow(chgRow, item, "Modify", "attachments", af.getPath());
							}
						}
						break;
					}
					case "comments": {
						Object fv = c.getClass().getField(pbFields[i]).get(c);
						if ((fv != null) && cfg.exportComments) {
							Comment[] cmts = (Comment[]) fv;
							for (int j = 0; j < cmts.length; j++) {
								chgRow++;
								createChangeRow(chgRow, item, "Modify", "comments",
										String.format("%s : %s wrote: \n", cmts[j].createdOn,
												cmts[j].createdBy.fullName)
												+ cmts[j].text);
							}
						}
						break;
					}
					/**
					 * We need to extract the blockedStatus type and re-create a blockReason
					 */
					case "blockReason": {
						// Get blockedStatus from card
						Object fv = c.getClass().getField("blockedStatus").get(c);
						if (fv != null) {
							if (((BlockedStatus) fv).isBlocked) {
								iRow.createCell(fieldCounter, CellType.STRING)
										.setCellValue(((BlockedStatus) fv).reason);
							} else {
								iRow.createCell(fieldCounter, CellType.STRING).setCellValue("");
							}
						}
						fieldCounter++;
						break;
					}
					case "customId": {
						Object fv = c.getClass().getField(pbFields[i]).get(c);
						if (fv != null) {
							CustomId ci = (CustomId) fv;
							if ((ci.value != null) && (ci.value.length() != 0)) {
								iRow.createCell(fieldCounter, CellType.STRING).setCellValue(ci.value);
							}
						}
						fieldCounter++;
						break;
					}

					case ColNames.EXTERNAL_LINK: {
						Object fv = c.getClass().getField("externalLinks").get(c);
						if (fv != null) {
							ExternalLink[] extlnks = (ExternalLink[]) fv;
							if ((extlnks.length > 0) && (extlnks[0].url != null)) {
								if ((extlnks[0].label != null) && (extlnks[0].url != null) && (extlnks[0].url.length() != 0)) {
									iRow.createCell(fieldCounter, CellType.STRING)
											.setCellValue(extlnks[0].label.replace(",", " ") + "," + extlnks[0].url);
								} 
							}
						}
						fieldCounter++;
						break;
					}
					case ColNames.LANE: {
						Object fv = c.getClass().getField(pbFields[i]).get(c);
						if (fv != null) { // Might be a task
							CardType ct = LkUtils.getCardTypeFromBoard(cfg, cfg.source, c.type.title,
									cfg.source.getBoardName());
							if (ct.getIsTaskType()) {
								Lane taskLane = (Lane) fv;
								if (taskLane.laneType.equals("untyped")) {
									String lane = LkUtils.getLanePathFromId(cfg, cfg.source, ((Lane) fv).id);
									d.p(Debug.ERROR,
											"Invalid card type - check \"Task\" setting on \"%s\". Opting to use lane \"%s\"\n",
											c.type.title, lane);
									iRow.createCell(fieldCounter, CellType.STRING)
											.setCellValue(LkUtils.getLanePathFromId(cfg, cfg.source, ((Lane) fv).id));

								} else {
									iRow.createCell(fieldCounter, CellType.STRING).setCellValue(taskLane.laneType);
								}
							} else {
								iRow.createCell(fieldCounter, CellType.STRING)
										.setCellValue(LkUtils.getLanePathFromId(cfg, cfg.source, ((Lane) fv).id));
							}
						}
						fieldCounter++;
						break;
					}
					case "parentCards": {
						Object fv = c.getClass().getField(pbFields[i]).get(c);
						if (fv != null) {
							ParentCard[] pcs = (ParentCard[]) fv;
							for (int j = 0; j < pcs.length; j++) {
								Card crd = LkUtils.getCard(cfg, cfg.source, pcs[j].cardId);
								Board brd = LkUtils.getBoardById(cfg, cfg.source, pcs[j].boardId);
								parentChild.add(new ParentChild(brd.title, crd.title, c.id));
							}
						}
						break;
					}
					case ColNames.SOURCE_ID: {
						iRow.createCell(fieldCounter, CellType.STRING).setCellValue(c.id);
						if (cfg.addComment) {
							chgRow++;
							createChangeRow(chgRow, item, "Modify", "comments",
									LkUtils.getUrl(cfg, cfg.source) + "/card/" + c.id);
						}
						fieldCounter++;
						break;
					}
					case "tags": {
						Object fv = c.getClass().getField(pbFields[i]).get(c);
						if (fv != null) {
							iRow.createCell(fieldCounter, CellType.STRING)
									.setCellValue(String.join(",", ((String[]) fv)));
						}
						fieldCounter++;
						break;
					}

					// Pseudo-field that does something different
					case "taskBoardStats": {
						// If the task count is non zero, get the tasks for this card and
						// resolve the lanes for the tasks,
						// Add the tasks to the items and put some Modify statements in.
						if (cfg.exportTasks && (c.taskBoardStats != null)) {
							ArrayList<Task> tasks = LkUtils.getTaskIdsFromCard(cfg, cfg.source, c.id);
							for (int j = 0; j < tasks.size(); j++) {
								chgRow++;
								Card task = LkUtils.getCard(cfg, cfg.source, tasks.get(j).id);
								// Increment the row index ready for the item row create
								itmRow++;
								createChangeRow(chgRow, item, "Modify", "Task",
										"='" + XlUtils.validateSheetName(cfg.source.getBoardName()) + "'!A" + (itmRow + 1));

								// Now create the item row itself
								// Changes changesMade = new Changes(0,0); //Testing!
								Changes childChanges = createItemRowFromCard(chgRow, itmRow, task, pbFields);
								// Need to pick up the indexes again as we might have created task entries

								chgRow = childChanges.getChangeRow();
								itmRow = childChanges.getItemRow();
							}
							// itmRowIncr += tasks.size();
						}

						break;
					}
					default: {
						Object fv;

						try {
							fv = c.getClass().getField(pbFields[i]).get(c);
							if (fv != null) {
								switch (fv.getClass().getSimpleName()) {
									case "String": {
										iRow.createCell(fieldCounter, CellType.STRING).setCellValue(fv.toString());
										break;
									}
									case "Boolean": {
										iRow.createCell(fieldCounter, CellType.BOOLEAN).setCellValue(((Boolean) fv));
										break;
									}
									case "Integer": {
										iRow.createCell(fieldCounter, CellType.NUMERIC).setCellValue(((Integer) fv));
										break;
									}
									case "ItemType": {
										iRow.createCell(fieldCounter, CellType.STRING)
												.setCellValue(((ItemType) fv).title);
										break;
									}
									case "CustomIcon": {
										iRow.createCell(fieldCounter, CellType.STRING)
												.setCellValue(((CustomIcon) fv).title);
										break;
									}
									case "Date": {
										SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd");
										Cell cl = iRow.createCell(fieldCounter);
										cl.setCellValue(dtf.format(((Date) fv)).toString());

										break;
									}
									// Ignore these pseudo-fields
									case "ParentCard": {
										break;
									}
									case "User": {
										iRow.createCell(fieldCounter, CellType.STRING)
												.setCellValue(((User) fv).emailAddress);
										break;
									}
									case "PlanningIncrement[]": {
										PlanningIncrement[] pia = (PlanningIncrement[]) fv;
										ArrayList<String> ids = new ArrayList<>();
										for (int p = 0; p < pia.length; p++) {
											ids.add(pia[p].label);
										}
										iRow.createCell(fieldCounter, CellType.STRING)
												.setCellValue(ids.toString());
										break;
									}
									default: {
										System.out.printf("Unknown class: %s\n", fv.getClass().getSimpleName());
										break;
									}
								}

							}
						} catch (NoSuchFieldException e) {
							// This is probably a custom field so look for it in the customFields
							// array
							Object cfa = c.getClass().getField("customFields").get(c);
							CustomField[] cfs = (CustomField[]) cfa;
							CustomField foundField = null;
							for (int j = 0; j < cfs.length; j++) {
								if (cfs[j].label.equals(pbFields[i])) {
									foundField = cfs[j];
								}
							}
							// We now know that the field is part of the Custom Field set, so find the value
							// in the cards array
							// of
							if (foundField != null) {
								if (foundField.value != null) {
									switch (foundField.type) {
										case "number": {
											Double number = Double.parseDouble((String) foundField.value);
											iRow.createCell(fieldCounter, CellType.NUMERIC)
//													.setCellValue(Integer.parseInt((String) foundField.value));
													.setCellValue(number);
											break;
										}
										default: {
											iRow.createCell(fieldCounter, CellType.STRING)
													.setCellValue((String) foundField.value);
											break;
										}
									}
								}
							}
						}
						fieldCounter++;
						break;

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return new Changes(chgRow, itmRow);

	}

	private void createChangeRow(Integer CRIdx, Integer IRIdx, String action, String field, String value) {
		Integer localCellIdx = 0;
		String cellFormula = "'" + XlUtils.validateSheetName(cfg.source.getBoardName()) + "'!"
				+ XlUtils.findColumnLetterFromSheet(cfg.itemSheet, "title")
				+ (IRIdx + 1);
		Row chgRow = cfg.changesSheet.createRow(CRIdx);
		chgRow.createCell(localCellIdx++, CellType.STRING).setCellValue(cfg.group);
		chgRow.createCell(localCellIdx++, CellType.FORMULA)
				// .setCellFormula("'" + cfg.source.BoardName + "'!B" + (IRIdx + 1));
				.setCellFormula(cellFormula);
		chgRow.createCell(localCellIdx++, CellType.STRING).setCellValue(action);
		chgRow.createCell(localCellIdx++, CellType.STRING).setCellValue(field);

		if (value.startsWith("=")) {
			FormulaEvaluator evaluator = cfg.wb.getCreationHelper().createFormulaEvaluator();
			Cell cell = chgRow.createCell(localCellIdx++);
			cell.setCellFormula(value.substring(1));
			evaluator.evaluateFormulaCell(cell);
		} else {
			chgRow.createCell(localCellIdx++, CellType.STRING).setCellValue(value);
		}
	}
}
