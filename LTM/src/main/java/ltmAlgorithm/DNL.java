package ltmAlgorithm;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.*;
import utils.LTMLoadableDemand;
import utils.LTMLoadableDemandV2;
import utils.MapToArray;
import utils.VariableDetails;

public interface DNL {

	/**
	 * This will perform the actual LTM procedure
	 */
	public void performLTM(LTMLoadableDemandV2 demand, MapToArray<VariableDetails> v);
	/**
	 * 
	 * @return the route travel time in LTM timeBean
	 */
	public Map<String,Map<Id<AnalyticalModelRoute>,Double>> getRouteTravelTime();
	/**
	 * 
	 * @return TRV routes' travel time in LTM timeBean
	 */
	public Map<String,Map<Id<AnalyticalModelRoute>,Double>> getTrvRouteTravelTime();
	
	/**
	 * 
	 * @return Get LTM link flow in LTM time Bean
	 */
	public Map<String,Map<Id<Link>,Double>> getLTMLinkFlow();
	/**
	 * 
	 * @return Get LTM TRV link flow in LTM time Bean
	 */
	public Map<String,Map<Id<Link>,Double>> getLTMTrvLinkFlow();
	
	/**
	 * 
	 * @return Get LTM TRV link TT in LTM time Bean
	 */
	public Map<String,Map<Id<Link>,Double>> getLTMCombinedLinkFlow();
	
	/**
	 * 
	 * @return get travel time of each link for each LTM time bean
	 */
	public Map<String,Map<Id<Link>,Double>> getLTMLinkTravelTime();
	
	/**
	 * 
	 * @return the time points of the LTM
	 */
	public double[] getTimePoints();
	
}
