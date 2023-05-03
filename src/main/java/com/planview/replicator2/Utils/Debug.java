package com.planview.replicator2.Utils;

import java.util.Date;

public class Debug {
    public final static Integer ALWAYS = -1;
    public final static Integer ERROR = 0;
    public final static Integer INFO = 2;
    public final static Integer WARN = 1;
    public final static Integer DEBUG = 3;
    public final static Integer VERBOSE = 4;
    
    private Integer debugPrint = 0;
    
    public Debug() {};
    
    public  Debug(Integer lvl) {
        debugPrint = lvl;
    }

    public void p(Integer level, String fmt, String str) {
        p(level, fmt, (Object) str);
    }

    public void setLevel(Integer lvl){
        debugPrint = lvl;
    }

    public void p(Integer level, String fmt, Object... parms) {
		String defaultString = "NOTE: ";
        String lp = null;
        switch (level) {
            case 2: {
                lp = "INFO: ";
                break;
            }
            case 0: {
                lp = "ERROR: ";
                break;
            }
            case 1: {
                lp = "WARN: ";
                break;
            }
            case 3: {
                lp = "DEBUG: ";
                break;
            }
            case 4: {
                lp = "VERBOSE: ";
                break;
            }
            default: {
                lp = defaultString;
            }
        }
		
		//Switch on timestamps if we are debugging or verbose
		switch (debugPrint) {
			case 0:
			case 1:
			case 2: {
				break;
			}
			case 3:
			case 4: {
				//Only print dates on non "NOTE" ones
				if (!lp.equals(defaultString)) {
					lp += new Date().toString() + " : ";
				}
			}
		}
        if (level <= debugPrint) {
            System.out.printf(lp+fmt, parms);
        }
    }
}
