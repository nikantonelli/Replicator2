package com.planview.replicator.Utils;
import com.planview.replicator.Debug;
import com.planview.replicator.InternalConfig;
import com.planview.replicator.jira.JiraAccess;

public class JiraDeleter {
	InternalConfig config;
	JiraAccess jAcc = null;
	Debug d = new Debug();

	public int go(InternalConfig cfg, String[] adoDeletes) {
		config = cfg;
		d.setLevel(config.debugLevel);

		//Check that we have both user and token as ADO is non-standard.
		if ((config.jira.getUser() != null) && (config.ado.getApiKey() != null)){
			jAcc = new JiraAccess(config.jira, cfg.debugLevel);
		}
		else {
			return -1;
		}
		for (int i = 0; i < adoDeletes.length; i++) {
			String url = adoDeletes[i];
			if ( null != jAcc.deleteTicket(url)) {
				d.p(Debug.INFO, "Deleted %s", url);
			} else {
				d.p(Debug.INFO, "Failed to delete %s", url);
			}
		}
		return 0;
	}
}
