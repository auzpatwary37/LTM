package utils;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;

/**
 * Holds necessary information regarding an LTM or any other DNL model
 *
 * @author Ashraf
 *
 */
public class LTMLoadableDemand implements DNLDemand{
	private final Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> routes;
	private final Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> trvRoutes;
	private final Map<String,Tuple<Double,Double>> routeTimeBean;
	private final Map<String,Map<Id<AnalyticalModelRoute>,Double>>demand;
	
	/**
	 * if there is no transit vehicles just input null
	 * 
	 * @param routes the auto route set
	 * @param trvRoutes the transit vehicular route set
	 * @param timeBean the time bean information for the route demand 
	 * @param demand the actual route demand. It is okay to combine the auto and transit vehicular routes in this map. 
	 */
	public LTMLoadableDemand(Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> routes, Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> trvRoutes,
			Map<String,Tuple<Double,Double>>timeBean, Map<String,Map<Id<AnalyticalModelRoute>,Double>>demand) {
		this.routes = routes;
		this.trvRoutes = trvRoutes;
		this.routeTimeBean = timeBean;
		this.demand = demand;
		
	}
	

	/**
	 * if there is no transit vehicles just input null
	 * 
	 * @param routes the auto route set
	 * @param trvRoutes the transit vehicular route set
	 * @param timeBean the time bean information for the route demand 
	 * @param demand the actual route demand. It is okay to combine the auto and transit vehicular routes in this map. 
	 */
	public LTMLoadableDemand(Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> routes, Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> trvRoutes,
			Map<String,Tuple<Double,Double>>timeBean, Map<String,Map<Id<AnalyticalModelRoute>,Double>>autoDemand, Map<String,Map<Id<AnalyticalModelRoute>,Double>>trvDemand) {
		this.routes = routes;
		this.trvRoutes = trvRoutes;
		this.routeTimeBean = timeBean;
		this.demand = new HashMap<>(autoDemand) ;
		this.demand.putAll(trvDemand);
		
	}
	
	
	@Override
	public Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> getRoutes() {
		return routes;
	}
	

	@Override
	public Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> getTrvRoutes() {
		return trvRoutes;
	}
	

	@Override
	public Map<String, Tuple<Double, Double>> getRouteTimeBean() {
		return routeTimeBean;
	}

	@Override
	public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getDemand() {
		return demand;
	}
}
