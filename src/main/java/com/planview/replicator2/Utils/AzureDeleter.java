package com.planview.replicator2.Utils;

import com.planview.replicator2.Debug;
import com.planview.replicator2.InternalConfig;
import com.planview.replicator2.azure.AzureAccess;

public class AzureDeleter {
	InternalConfig config;
	AzureAccess aAcc = null;
	Debug d = new Debug();

	public int go(InternalConfig cfg, String[] adoDeletes) {
		config = cfg;
		d.setLevel(config.debugLevel);

		//Check that we have both user and token as ADO is non-standard.
		if ((config.ado.getUser() != null) && (config.ado.getApiKey() != null)){
			aAcc = new AzureAccess(config.ado, cfg.debugLevel);
		}
		else {
			return -1;
		}
		for (int i = 0; i < adoDeletes.length; i++) {
			String url = adoDeletes[i];
			if ( null != aAcc.deleteTicket(url)) {
				d.p(Debug.INFO, "Deleted %s", url);
			} else {
				d.p(Debug.INFO, "Failed to delete %s", url);
			}
		}
		return 0;
	}
}
