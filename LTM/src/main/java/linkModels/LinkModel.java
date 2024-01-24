package linkModels;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import utils.MapToArray;
import utils.TuplesOfThree;
import utils.VariableDetails;



public interface LinkModel {
	
	
	
	
	/**
	 * Converts continueous time into time index
	 * @param t
	 * @return
	 */
	public int getTimeIndex(double t);

	/**
	 * Set up the time step and route set of the LTM.
	 * @param timeLimits
	 * @param routeIndex
	 */
	public void setLTMTimeBeanAndRouteSet(double[] timePoints, Map<Id<NetworkRoute>,NetworkRoute> routes);

	/**
	 * Get the inherent link 
	 * @return
	 */
	public Link getLink();

	/**
	 * get the time steps' upper and lower limits
	 * This can potentially trigger event based algorithm? jan 2021 
	 * @return
	 */
	public double[] getTimePoints();

	/**
	 * Get the sending flow for the time index (See the thesis of Yperman. Yperman, I. (2007). The link transmission model for dynamic network loading.) 
	 * @param timeIdx
	 * @return tuplesOfThree containing S Sdt and dS
	 */
	public Tuple<Double, double[]> getSendingFlow(int timeIndx);
	
	/**
	 * Get the receiving flow for the time index (See the thesis of Yperman. Yperman, I. (2007). The link transmission model for dynamic network loading.) 
	 * @param timeIdx
	 * @return
	 * tuplesOfThree containing R, Rdt and dR
	 */
	public Tuple<Double, double[]> getRecivingFlow(int timeIdx);
	
	/**
	 * update the boundary cumulative flow for timeIndex timeIndx at x0
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNx0(double flow,int timeIndx,double[]dNx0);
	/**
	 * update the boundary cumulative flow for timeIndex timeIndx at xl 
	 * this should be done in house, i.e., inside the link model. (Updated from the upstream border)
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNxl(double flow, int timeInd,double[]dNxl); 
	/**
	 * update the path specific boundary cumulative flow for timeIndex timeIndx at x0
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNrx0(double flow,Id<NetworkRoute> route,int timeIndx,double[]dNrx0);
	/**
	 * update the path specific boundary cumulative flow for timeIndex timeIndx at xl
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNrxl(double flow,Id<NetworkRoute> route, int timeInd,double[]dNrxl); 
	
	/**
	 * Get the path specific output cumulative vehicle boundary conditions all through the simulation at x0
	 * @return
	 */
	public double[][] getNrx0();
	/**
	 * Get the path specific output cumulative vehicle boundary conditions all through the simulation at xl
	 * @return
	 */
	public double[][] getNrxl();
	

	/**
	 * Get the output cumulative vehicle boundary conditions all through the simulation at x0
	 * @return
	 */
	public double[] getNx0();
	/**
	 * Get the output cumulative vehicle boundary conditions all through the simulation at xl
	 * @return
	 */
	public double[] getNxl();
	
	
	public double[] getK();

	public MapToArray<Id<NetworkRoute>> getRouteIds();
	
	public Map<Id<NetworkRoute>,NetworkRoute> getRoutes();

	public int getTimeindexNo();
	/**
	 * This function should set up the internal state of the link model for gradient estimations 
	 * @param variables
	 */
	public void setOptimizationVariables(MapToArray<VariableDetails> variables);

	public double[][][] getdNrx0();

	public double[][] getNrxldt();

	public double[][][] getdNrxl();

	public double[] getNx0dt();
	public double[][] getdNx0();
	public double[][] getdNxl();
	
	public double[] getNxldt();

	public double[][] getNrx0dt();
	
	public MapToArray<VariableDetails> getVariables();
	
	
	public void reset();

	public boolean checkNx0SumChange();
}
