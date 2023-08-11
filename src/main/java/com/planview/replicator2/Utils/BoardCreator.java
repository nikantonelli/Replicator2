package com.planview.replicator2.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planview.replicator2.Leankit.Board;
import com.planview.replicator2.Leankit.BoardLevel;
import com.planview.replicator2.Leankit.CardType;
import com.planview.replicator2.Leankit.CustomField;
import com.planview.replicator2.Leankit.CustomIcon;
import com.planview.replicator2.Leankit.Lane;
import com.planview.replicator2.Leankit.Layout;
import com.planview.replicator2.System.InternalConfig;

public class BoardCreator {
	Debug d = new Debug();
	InternalConfig cfg = null;

	public BoardCreator(InternalConfig config) {
		cfg = config;
		d.setLevel(config.debugLevel);
	}

	public Boolean go() {
		// Check to see if source and destination are on the same machine
		// If so, just create board from original (without cards)
		String src = cfg.source.getUrl();
		String dst = cfg.destination.getUrl();

		// Check whether user has mistyped url with / on end.
		if (src.endsWith("/"))
			src = src.substring(0, src.length() - 1);
		if (dst.endsWith("/"))
			dst = dst.substring(0, dst.length() - 1);

		JSONObject details = new JSONObject();
		Board dstBrd = LkUtils.getBoardByTitle(cfg, cfg.destination);
		Board srcBrd = LkUtils.getBoardByTitle(cfg, cfg.source);

		if (src.equals(dst)) {

			if ((srcBrd != null) && (dstBrd == null)) {
				dstBrd = LkUtils.duplicateBoard(cfg);
				if (dstBrd == null) {
					d.p(Debug.ERROR, "Cannot duplicate locally from \"%s\" to \"%s\" ... skipping\n",
							cfg.source.getBoardName(),
							cfg.destination.getBoardName());
					return false;
				}
			}
		} else {

			// Here, we have to copy across all the set up of the original board
			if (srcBrd != null) {
				// Create a blank board if needed
				if (dstBrd == null) {
					dstBrd = LkUtils.createBoard(cfg, cfg.destination);
				}
			}
		}

		if (srcBrd == null) {
			d.p(Debug.INFO, "Cannot locate board title: \"%s\"\n", cfg.source.getBoardName());
			return false;
		}

		if (dstBrd == null) {
			d.p(Debug.ERROR, "Could not create/locate destination board \"%s\" ... skipping\n",
					cfg.destination.getBoardName());
			return false;
		} else {
			d.p(Debug.INFO, "Board Available for update on \"%s\". id: %s, title: \"%s\"\n", cfg.destination.getUrl(),
					dstBrd.id,
					cfg.destination.getBoardName());
		}

		//For Portfolios, we need to toggle this flag
		details.put("allowPlanviewIntegration", false);
		LkUtils.updateBoard(cfg, cfg.destination, dstBrd.id, details);

		details.put("allowPlanviewIntegration", true);
		details.put("allowUsersToDeleteCards", srcBrd.allowUsersToDeleteCards);
		details.put("baseWipOnCardSize", srcBrd.baseWipOnCardSize);
		details.put("description", srcBrd.description);

		/**
		 * 
		 * Check for correct board levels
		 * 
		 * 
		 **/
		ArrayList<BoardLevel> srcLevels = LkUtils.getBoardLevels(cfg, cfg.source);
		ArrayList<BoardLevel> dstLevels = LkUtils.getBoardLevels(cfg, cfg.destination);

		int gotDstLevels = 0;
		for (int i = 0; (i < srcLevels.size()) && (i < dstLevels.size()); i++) {
			if (srcLevels.get(i).label.equals(dstLevels.get(i).label)) {
				gotDstLevels += 1;
			}
		}
		if (gotDstLevels != srcLevels.size()) {
			d.p(Debug.WARN, "Mismatch between source and destination board levels\n");
			if (cfg.updateLevels) {
				d.p(Debug.WARN, "    - resetting destination\n");
				BoardLevel[] bla = {};
				for (int i = 0; i < srcLevels.size(); i++) {
					BoardLevel current = srcLevels.get(i);
					bla = (BoardLevel[]) ArrayUtils.add(bla,
							new BoardLevel(current.depth, current.label, current.color));
				}
				LkUtils.setBoardLevels(cfg, cfg.destination, bla);
			}
		} else {
			d.p(Debug.INFO, "Board levels match between \"%s\" and \"%s\"\n", cfg.source.getUrl(),
					cfg.destination.getUrl());
		}
		/**
		 * 
		 * Check for correct customIcons
		 * 
		 * 
		 **/

		// Fetch the customIcons on the source, if there are some, then set enable on
		// destination - this shouldn't affect any boards that already have customIcons
		CustomIcon[] srcIcons = LkUtils.getCustomIcons(cfg, cfg.source);
		if (srcIcons != null) {
			// Enable these now or else they don't get added correctly.
			JSONObject enabler = new JSONObject();
			enabler.put("enableCustomIcon", srcBrd.classOfServiceEnabled);
			enabler.put("customIconFieldLabel", srcBrd.customIconFieldLabel);
			LkUtils.updateBoard(cfg, cfg.destination, dstBrd.id, enabler);

			LkUtils.enableCustomIcons(cfg, cfg.destination);

			// Get the custom Fields from the destination, if they already exist
			CustomIcon[] dstIcons = LkUtils.getCustomIcons(cfg, cfg.destination);

			Integer matchedIcons = 0;
			Integer[] unMatched = {};
			for (int i = 0; i < srcIcons.length; i++) {
				Boolean matched = false;
				// If we have them, check to see if it is the same name ('unique' identifier on
				// LK)
				if (dstIcons != null) {
					for (int j = 0; j < dstIcons.length; j++) {
						if (dstIcons[j].name.equals(srcIcons[i].name)) {
							matchedIcons++;
							matched = true;
							break;
						} else {
							unMatched = (Integer[]) ArrayUtils.add(unMatched, j);
						}
					}
				}
				if (!matched) {
					LkUtils.createCustomIcon(cfg, cfg.destination, srcIcons[i]);
					d.p(Debug.INFO, "Creating Icon %s on %s\n", srcIcons[i].name, cfg.destination.getBoardName());
				}
			}
		} else {
			d.p(Debug.INFO, "No customIcons to transfer from \"%s\"\n", cfg.source.getBoardName());
		}
		/**
		 * 
		 * Check for correct custom fields
		 * 
		 **/

		CustomField[] srcFields = LkUtils.getCustomFields(cfg, cfg.source);
		CustomField[] dstFields = LkUtils.getCustomFields(cfg, cfg.destination);
		if (srcFields.length != 0) {
			JSONArray opArr = new JSONArray();
			for (int i = 0; i < srcFields.length; i++) {
				CustomField scf = srcFields[i];
				String label = scf.label;
				CustomField dcf = Arrays.stream(dstFields).filter(cf -> cf.label.equals(label)).findFirst()
						.orElse(null);

				// Do we have one? If so, update rather than add
				JSONObject val = new JSONObject();
				val.put("label", label);
				val.put("helpText", scf.getHelpText());
				JSONObject op = new JSONObject();

				if (dcf != null) {
					if (dcf.getType().equals(scf.getType())) {
						op.put("op", "replace");
						op.put("path", "/" + dcf.getId());
						if (scf.choiceConfiguration != null) {
							val.put("choiceConfiguration", new JSONObject(scf.choiceConfiguration));
						}
						op.put("value", val);
					} else {
						d.p(Debug.INFO, "Custom Field type mismatch for \"%s\" ...skipping\n", dcf.getLabel());
						break;
					}
				} else {
					op.put("op", "add");
					op.put("path", "/");
					val.put("type", scf.getType());
					if (scf.choiceConfiguration != null) {
						val.put("choiceConfiguration", new JSONObject(scf.choiceConfiguration));
					}
					op.put("value", val);
				}
				opArr.put(op);
			}
			if (null == LkUtils.updateCustomField(cfg, cfg.destination, opArr)) {
				d.p(Debug.WARN, "Unable to update all Custom Fields for \"%s\"\n",
						cfg.destination.getBoardName());
			}

		} else {
			d.p(Debug.INFO, "No Custom Fields to transfer from \"%s\"\n", cfg.source.getBoardName());
		}

		/**
		 * 
		 * Check for correct card types
		 * 
		 */
		ArrayList<CardType> dstTypes = LkUtils.getCardTypesFromBoard(cfg, cfg.destination);
		// Remove all the card types from the destination
		CardType dstCardType = null;
		CardType dstTaskType = null;
		for (int i = 0; i < dstTypes.size(); i++) {
			CardType ct = dstTypes.get(i);
			if (ct.getIsDefault() == true) {
				dstCardType = ct;
				CardType card = new CardType("_defaultCardType_");	//Have to move them away in case someone is using the name
				card.setIsCardType(true);
				card.setIsTaskType(false);
				LkUtils.updateCardType(cfg, cfg.destination, dstCardType.getId(), card);
			} else if (ct.getIsDefaultTaskType() == true) {
				dstTaskType = ct;
				CardType card = new CardType("_defaultTaskType_");	//Have to move them away in case someone is using the name
				card.setIsCardType(false);
				card.setIsTaskType(true);
				LkUtils.updateCardType(cfg, cfg.destination, dstTaskType.getId(), card);
			} else {
				LkUtils.removeCardTypeFromBoard(cfg, cfg.destination, ct);
			}
		}

		ArrayList<CardType> srcTypes = LkUtils.getCardTypesFromBoard(cfg, cfg.source);
		for (int i = 0; i < srcTypes.size(); i++) {
			CardType ct = srcTypes.get(i);
			CardType card = new CardType(ct.getName());
			card.setColorHex(ct.getColorHex());

			if (ct.getIsDefault()) {
				LkUtils.updateCardType(cfg, cfg.destination, dstCardType.getId(), card);
			} else if (ct.getIsDefaultTaskType()) {
				card.setIsCardType(false);
				card.setIsTaskType(true);
				LkUtils.updateCardType(cfg, cfg.destination, dstTaskType.getId(), card);
			} else {
				LkUtils.addCardTypeToBoard(cfg, cfg.destination, card);
			}
		}
		/**
		 * 
		 * Push all the remaining updates
		 * 
		 */
		if (srcBrd.level != null)
			details.put("level", srcBrd.level.depth);
		LkUtils.updateBoard(cfg, cfg.destination, dstBrd.id, details);

		/**
		 * 
		 * Overwrite lane layout
		 * 
		 * AP doesn't allow for incremental layout updates
		 * 
		 */

		Lane[] srcLanes = LkUtils.getLanesFromBoardTitle(cfg, cfg.source, cfg.source.getBoardName());
		if (srcLanes != null) {
			Layout newLayout = LkUtils.createLaneTree(srcLanes);
			ObjectMapper om = new ObjectMapper();
			try {
				d.p(Debug.VERBOSE, "Layout: %s\n", om.writeValueAsString(newLayout));
			} catch (JsonProcessingException e) {
				d.p(Debug.ERROR, "Layout conversion failed : %s\n", cfg.source.getBoardName());
				return false;
			}
			newLayout = LkUtils.updateBoardLayout(cfg, cfg.destination, newLayout);
			LkUtils.setSortedLanes(cfg, cfg.destination, srcLanes, newLayout);
		} else {
			d.p(Debug.WARN, "Cannot retrieve board layout from \"%s\" ...skipping update to \"%s\"\n",
					cfg.source.getBoardName(), cfg.destination.getBoardName());
		}

		return true;
	}
}
