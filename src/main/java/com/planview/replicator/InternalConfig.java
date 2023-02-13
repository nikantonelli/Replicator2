package com.planview.replicator;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class InternalConfig {

    public final static String CHANGES_SHEET_NAME = "C_";
    public final static Integer MAX_CARDS_PER_BOARD = 10000;    //Maybe add an override on the command line?

	public final static String SOURCE_URL_COLUMN = "srcUrl";
	public final static String SOURCE_BOARDNAME_COLUMN = "srcBoardName";
	public final static String SOURCE_APIKEY_COLUMN = "srcApiKey";
	public final static String DESTINATION_URL_COLUMN = "dstUrl";
	public final static String DESTINATION_BOARDNAME_COLUMN = "dstBoardName";
	public final static String DESTINATION_APIKEY_COLUMN = "dstApiKey";
	public final static String DESTINATION_TID_COLUMN = "Target Id";
	public final static String DESTINATION_ADO_USER = "ADO User";
	public final static String DESTINATION_ADO_TOKEN = "ADO Token";
	public final static String DESTINATION_JIRA_USER = "JIRA User";
	public final static String DESTINATION_JIRA_KEY = "JIRA Key";
	public final static String IGNORE_LIST = "Import Ignore";

    public String xlsxfn = "";


	public static String LANE_DIVIDER_CHAR = "^";
	public static String WIP_DIVIDER_CHAR = "`";
	public static String SPLIT_LANE_REGEX_CHAR = "\\" + LANE_DIVIDER_CHAR;
	public static String SPLIT_WIP_REGEX_CHAR = "\\" + WIP_DIVIDER_CHAR;

    /**
     * Contains the local stream of info for the program. The importer will need
     * multiple sheets, but the exporter will write to this file if selected on the
     * command line.
     */
    public XSSFWorkbook wb = null;
    
	public AccessConfig source = new AccessConfig();
	public AccessConfig destination = new AccessConfig();

	public AccessConfig jira = new AccessConfig();
	public AccessConfig ado = new AccessConfig();

    public Integer   debugLevel = -1;
    public boolean   exportArchived = false;
    public boolean   exportTasks = false;
    public boolean   exportAttachments = false;
    public boolean   exportComments = false;
    public boolean   addComment = false;
    public Boolean   dualFlow = false;
    public XSSFSheet changesSheet = null;
    public XSSFSheet itemSheet = null;
    public String    archive = null;
    public XSSFSheet replaySheet;

	public Integer  group = 0;
	public boolean  exporter = false;
	public boolean  importer = false;
	public boolean  obliterate = false;
	public boolean  remakeBoard = false;
	public boolean  updateLayout = false;
	public boolean  deleteCards = false;
	public boolean  eraseBoard = false;
	public boolean  ignoreCards;
	public String[] ignTypes;
	public boolean  nameExtension;
	public String   extension;
	public String oldExtension;
	public boolean tasktop;
	public boolean updateLevels;
}

