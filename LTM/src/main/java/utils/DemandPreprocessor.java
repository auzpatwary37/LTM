package utils;

import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import java.util.Map;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.*;
/**
 * This class will pre-process the route demand 
 * Namely convert the more loose od demand timeBean to required LTM time Beans
 * @author ashraf
 *
 */
public class DemandPreprocessor {
	
	private Network network;
	private Map<String,Tuple<Double,Double>> routeTimeBean;
	private MapToArray<String> ltmTimeBeanM2a;
	private double ltmTimeStepSize;
	private Map<String,Map<Id<AnalyticalModelRoute>,Double>> routeDemand;
	private Map<String,Map<Id<AnalyticalModelRoute>,Double>> trvRouteDemand;
	private LTMLoadableDemand ltmDemand;
	private Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> routes;
	private Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> trvRoutes;
	

	
}
