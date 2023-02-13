package com.planview.replicator2.jira;

import com.planview.replicator2.AccessConfig;
import com.planview.replicator2.Debug;
import com.planview.replicator2.Network.NetworkAccess;

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
