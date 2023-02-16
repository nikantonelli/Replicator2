package com.planview.replicator2.Utils;

import java.util.Date;

import org.json.JSONObject;

import com.planview.replicator2.Leankit.Board;
import com.planview.replicator2.System.InternalConfig;

/**
 * Board deleter needs to move the destination board away (if it exists)
 * by renaming and archiving and then recreating a new one to mimic the
 * source
 */
public class BoardArchiver {
	Debug d = new Debug();
	InternalConfig cfg = null;

	public BoardArchiver(InternalConfig config) {
		cfg = config;
		d.setLevel(config.debugLevel);
	}

	public void go() {
		Board brd = LkUtils.getBoardByTitle(cfg, cfg.destination);
		if (brd != null) {
			Date timeNow = new Date();
			JSONObject updates = new JSONObject();
			updates.put("title", brd.title + " " + (timeNow.toString()));
			LkUtils.updateBoard(cfg, cfg.destination, brd.id, updates);
			LkUtils.archiveBoardById(cfg, cfg.destination, brd.id);
		}
	}
}
