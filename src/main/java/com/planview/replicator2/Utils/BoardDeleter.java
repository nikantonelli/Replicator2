package com.planview.replicator2.Utils;

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
			if (LkUtils.deleteBoard(cfg, cfg.destination)) {
				d.p(Debug.INFO, "Deleted board \"%s\" from \"%s\"\n", cfg.destination.getBoardName(), cfg.destination.getUrl());
			} else {
				d.p(Debug.WARN, "Delete of board \"%s\" from \"%s\" unsuccessful\n", cfg.destination.getBoardName(),
						cfg.destination.getUrl());
			}
		} else {
			d.p(Debug.INFO, "Board \"%s\" not present on \"%s\" (for deletion)\n", cfg.destination.getBoardName(),
					cfg.destination.getUrl());
		}
	}
}