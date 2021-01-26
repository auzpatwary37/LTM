package linkModels;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

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
	public void setLTMTimeBeanAndRouteSet(double[] timePoints, int[] routeIndex);

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
	 * @return
	 */
	public double getSendingFlow(int timeIdx);
	
	/**
	 * Get the recieving flow for the time index (See the thesis of Yperman. Yperman, I. (2007). The link transmission model for dynamic network loading.) 
	 * @param timeIdx
	 * @return
	 */
	public double getRecivingFlow(int timeIdx);
	
	/**
	 * update the boundary cumulative flow for timeIndex timeIndx at x0
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNx0(double flow,int timeIndx);
	/**
	 * update the boundary cumulative flow for timeIndex timeIndx at xl
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNxl(double flow, int timeInd); 
	/**
	 * update the path specific boundary cumulative flow for timeIndex timeIndx at x0
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNrx0(double flow,int routeIndx,int timeIndx);
	/**
	 * update the path specific boundary cumulative flow for timeIndex timeIndx at xl
	 * @param flow
	 * @param timeIndx
	 */
	public void updateNrxl(double flow,int routeIndx, int timeInd); 
	
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

	public int[] getrIndex();

	public int getTimeindexNo();

}
