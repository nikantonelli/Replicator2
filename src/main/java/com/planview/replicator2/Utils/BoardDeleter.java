package com.planview.replicator2.Utils;

import org.json.JSONObject;

import com.planview.replicator2.Leankit.Board;
import com.planview.replicator2.System.InternalConfig;

public class BoardDeleter {
	Debug d = new Debug();
	InternalConfig cfg = null;

	public BoardDeleter(InternalConfig config) {
		cfg = config;
		d.setLevel(config.debugLevel);
	}

	public void go() {
		Board brd = LkUtils.getBoardByTitle(cfg, cfg.destination);
		if (brd != null) {

			// Tell PRM integration that we are leaving the system
			JSONObject details = new JSONObject();
			details.put("allowPlanviewIntegration", false);
			d.p(Debug.INFO, "Removing board \"%s\" (ID: %s) from Planview Integration", brd.id, brd.title);
			LkUtils.updateBoard(cfg, cfg.destination, brd.id, details);

			if (LkUtils.deleteBoard(cfg, cfg.destination)) {
				d.p(Debug.INFO, "Deleted board \"%s\" from \"%s\"\n", brd.title,
						cfg.destination.getUrl());
			} else {
				d.p(Debug.WARN, "Delete of board \"%s\" from \"%s\" unsuccessful\n", brd.title,
						cfg.destination.getUrl());
			}
		} else {
			d.p(Debug.INFO, "Board \"%s\" not present on \"%s\" (for deletion)\n", cfg.destination.getBoardName(),
					cfg.destination.getUrl());
		}
	}
}