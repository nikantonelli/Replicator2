package com.planview.replicator2.Leankit;

public class ParentChild {
    public String boardName, parentId, childId;

    public ParentChild(String board, String parent, String child){
        boardName = board;
        parentId = parent;
        childId = child;
    }
}
