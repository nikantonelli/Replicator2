package com.planview.replicator2.Leankit;


public class CustomFieldResult {
    public Integer limit;
	
    public CustomField[] customFields;
	public Integer getLimit() {
		return limit;
	}
	public void setLimit(Integer limit) {
		this.limit = limit;
	}
	public CustomField[] getCustomFields() {
		return customFields;
	}
	public void setCustomFields(CustomField[] customFields) {
		this.customFields = customFields;
	}
}
