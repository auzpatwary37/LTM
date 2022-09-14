package nodeModels;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;


public interface NodeModel {

	/**
	 * This one calculates the realised turn ratio Np(x_l,t)
	 * @param timeStep
	 */
	@Deprecated
	public void generateTurnRatio(int timeStep);
	
	/**
	 * Instead of using the t_x0(N(x_l,t+delt) time step's route specific flow at the upstream boundary, this one uses the sending flow i.e., Np(x_0,t_x_0(N(x_l,t)+s(t))))-Np(x_l,t)
	 * The Np(x_l,t) is the realised turn ratio that is calculated using the actual flow. 
	 * @param timeStep
	 */
	public void generateIntendedTurnRatio(int timeStep);
	
	
	/**
	 * This might not be necessary
	 * @param fromLink
	 * @param toLink
	 * @return
	 */
	
	public String generatelinkToLinkKey(Id<Link> fromLink, Id<Link>toLink);

	/**
	 * This method should utilize the S_ij, S_ir, Rj signal (g/c) and turn capacity information to calculate G_ij 
	 * The required information should be already available inside the class
	 * The method updates G_ij for timeStep 
	 */
	public void applyNodeModel(int timeStep);
	
	/**
	 * This model should update Nx0, Nxl, Nxr0, Nxrl for the time step timeStep+1 from the available G_ij
	 * @param timeStep
	 */
	public void updateFlow(int timeStep);

	/**
	 * This should run all necessary step of calculation inside a node model to advance the LTM for timeStep
	 * @param timeStep
	 */
	public void performLTMStep(int timeStep);

	/**
	 * This function should set up the MapToArray for routes and T and variables inside the node and link models
	 */
	public void setTimeStepAndRoutes();
}
