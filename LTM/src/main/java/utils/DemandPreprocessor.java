package utils;

import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.*;
import java.util.Map;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.*;
/**
 * This class will pre-process the route demand 
 * Namely convert the more loose od demand timeBean to required LTM time Beans
 * @author ashraf
 *
 */
public class DemandPreprocessor {
	
	private Network network;// the network to perform the LTM 
	private TransitSchedule ts;
	private Map<String,Tuple<Double,Double>> routeTimeBean;// The large time bean of a route set
	private MapToArray<String> ltmTimeBeanM2a;// Time Bean for LTM and its map 2 array
	private double ltmTimeStepSize;// the time step size; has to be smaller than the free flow travel time of the smallest link in the network 
	private Map<String,Map<Id<AnalyticalModelRoute>,Double>> routeDemand;//the route demand 
	private Map<String,Map<Id<AnalyticalModelRoute>,Double>> trvRouteDemand;// the route demand from transitSchedule
	private LTMLoadableDemand ltmDemand;// the generated LTM demand on ltm time bean
	private Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> routes;// the routes 
	private Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> trvRoutes;// the trv routes
	
	
	
}
