package utils;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;

public interface DNLDemand {

	/**
	 * the auto route set 
	 * @return
	 */
	public Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> getRoutes();
	
	
	/**
	 * the transit vehicle route set
	 * @return
	 */
	public Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> getTrvRoutes();
	
	/**
	 * Get the route demand time Bean
	 * @return
	 */
	public Map<String, Tuple<Double, Double>> getRouteTimeBean();

	/**
	 * Get the combined route demand for auto and transit routes
	 * @return
	 */
	public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getDemand();

}
