package com.planview.replicator2.Utils;
import com.planview.replicator2.Debug;
import com.planview.replicator2.InternalConfig;
import com.planview.replicator2.jira.JiraAccess;

public class JiraDeleter {
	InternalConfig config;
	JiraAccess jAcc = null;
	Debug d = new Debug();

	public int go(InternalConfig cfg, String[] adoDeletes) {
		config = cfg;
		d.setLevel(config.debugLevel);

		//Check that we have both user and token as ADO is non-standard.
		if (config.ado.getApiKey() != null){
			jAcc = new JiraAccess(config.jira, cfg.debugLevel);
		}
		else {
			return -1;
		}
		for (int i = 0; i < adoDeletes.length; i++) {
			String url = adoDeletes[i];
			jAcc.deleteTicket(url);
			d.p(Debug.INFO, "Delete attempted %s", url);	
		}
		return 0;
	}
}
