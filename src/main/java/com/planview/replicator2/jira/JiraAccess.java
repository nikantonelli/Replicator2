package com.planview.replicator2.Jira;

import com.planview.replicator2.Network.NetworkAccess;
import com.planview.replicator2.System.AccessConfig;
import com.planview.replicator2.Utils.Debug;

public class JiraAccess  extends NetworkAccess {
	Debug d = new Debug();

	public JiraAccess(AccessConfig configp, Integer debugLevel) {
		config = configp;
		d.setLevel(debugLevel);
	}

	public Object deleteTicket(String url) {
		if (url != null) {
			//Split the URL into bits
			url = url.substring(8);
			String[] urlBits = url.split("/");
			reqUrl = "https://"+  urlBits[0] + "/rest/api/3/issue/" + urlBits[urlBits.length-1];
			reqParams.clear();
			reqHdrs.clear();
			reqType = "DELETE";
			return processRequest();
		}
		return null;
	}
}
