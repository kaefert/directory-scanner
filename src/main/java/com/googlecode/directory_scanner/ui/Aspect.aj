/**
 * 
 */
package com.googlecode.directory_scanner.ui;

import org.apache.log4j.Logger;

/**
 * @author kaefert
 * 
 */
public aspect Aspect {

    Logger logger = Logger.getLogger("directory-scanner-gui-logger_" + this.hashCode());
    
    pointcut publicOperation() : execution(* com.googlecode.directory_scanner.*.*(..))
    || execution(* com.googlecode.directory_scanner.workers.WorkManagerImpl.*(..))
//    || execution(* com.googlecode.directory_scanner.workers.DatabaseWorkerImpl.*(..))
//    || execution(* com.googlecode.directory_scanner.workers.DatabaseConnectionHandler.*(..))
    || execution(* com.googlecode.directory_scanner.ui.GUI.doReport(..))
//    || execution(* com.googlecode.directory_scanner.workers.*.*(..))
    ;

    Object around() : publicOperation() {

	String method = thisJoinPointStaticPart.getSignature().toShortString();
	logger.debug(method + " started now");
	long start = System.nanoTime();
	Object ret = proceed();
	long end = System.nanoTime();
	logger.debug(method + " took " + getNiceTime(end - start)); // + " nanoseconds");
	return ret;
    }

    private static final String[] timeUnits= {" ns ", " µs ", " ms ", " s ", " m ", " h "};
    private static final int[]    unitsRel = {1000, 1000, 1000, 60, 60, 24};
    
    private String getNiceTime(long time) {
	
	String niceTime = "(" + time + "ns)";
	
	for(int count = 0 ; count < timeUnits.length ; count++) {
	    long val = time % unitsRel[count];
	    niceTime = ((val > 0) ? val + timeUnits[count] : "") + niceTime;
	    time /= unitsRel[count];
	    if(time <= 0)
		break;
	}
	return niceTime;
    }
    

//	long microSecs = (time/1000) %1000;
//	
//	long milliSecs = (time/1000000) %1000;
//	long secs      = (time/1000000000);
//	;
//	return (secs > 0 ? (secs + " s ") : "") + 
//		(milliSecs > 0 ? (milliSecs + " ms ") : "") + 
//		(microSecs > 0 ? (microSecs + " µs ") : "") + 
//		(time > 0 ? (time + " ns ") : "");
}
