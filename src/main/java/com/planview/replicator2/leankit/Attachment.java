package com.planview.replicator2.leankit;

import java.util.Date;

public class Attachment {
    public String id, text, storageId, name, description;
    public User createdBy, changedBy;
    public Date createdOn, updatedOn;
    public Integer attachmentSize;
}
