package ltmAlgorithm;

import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.*;
import utils.EventBasedLTMLoadableDemand;
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
	 * This will perform the actual LTM procedure
	 */
	public void reperformLTM(LTMLoadableDemandV2 demand, MapToArray<VariableDetails> v);
	
	/**
	 * 
	 * @return Get LTM link flow in LTM time Bean
	 */
	public Map<String,Map<Id<Link>,Tuple<Double,double[]>>> getLTMLinkFlow(Map<String,Tuple<Double,Double>> timeBean);
	
	/**
	 * 
	 * @return the time points of the LTM
	 */
	public double[] getTimePoints();
	/**
	 * Giving option to run both event based demand and timeBin specific demand
	 * this one is event based loadable demand 
	 * @param demand
	 * @param variables
	 */
	public void performLTM(EventBasedLTMLoadableDemand demand, MapToArray<VariableDetails> variables);


	
	
	public Map<NetworkRoute, Map<String, Tuple<Double,double[]>>> getTimeBeanRouteTravelTime(int numberOfPointToAverage);


	public Map<NetworkRoute, Map<Integer, Tuple<Double,double[]>>> getTimeStampedRouteTravelTime();
	
	public Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>>>>> getTimeBeanTransitTravelAndWaitingTime(int numberOfTimePointsToAverage);


	public void simulateTransit();


	public Map<NetworkRoute, Map<Integer, Map<Tuple<Id<Link>, Id<Link>>, Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>>>>> getTimeStampedTransitTravelAndWaitingTime();

	public void reSimulateTransit();

	public Map<String, Map<Id<Link>, Tuple<Double, double[]>>> getLTMLinkFlow(Map<String, Set<Id<Link>>> links,
			Map<String, Tuple<Double, Double>> timeBean);
}
