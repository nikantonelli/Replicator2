package com.planview.replicator.jira;

import com.planview.replicator.AccessConfig;
import com.planview.replicator.Debug;
import com.planview.replicator.Network.NetworkAccess;

public class JiraAccess  extends NetworkAccess {
	Debug d = new Debug();

	public JiraAccess(AccessConfig configp, Integer debugLevel) {
		config = configp;
		d.setLevel(debugLevel);

		configCheck();
	}

	public Object deleteTicket(String url) {
		return null;
	}
}
