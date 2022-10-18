package nodeModels;

import java.util.List;
import java.util.Map;

import org.apache.commons.math.linear.MatrixUtils;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import linkModels.GenericLinkModel;
import linkModels.LinkModel;
import utils.LTMUtils;
import utils.MapToArray;
import utils.TuplesOfThree;
import utils.VariableDetails;

public class DestinationNodeModel{

	private Node dummyNode;
	private Node actualNode;
	private NetworkRoute r;
	private int T;
	private LinkModel inLinkModel;
	private double[] LTMTimePoints;	
	
	
	private double[] Gi;
	private double[][] dGi;
	private double[] Gidt;
	
	private double[] Si;
	private double[][] dSi;
	private double[] Sidt;
	
	private MapToArray<VariableDetails> variables;

	
	public DestinationNodeModel(NetworkRoute r,NodeModel originalNodeModel, int T, double[] LTMTimePoints, MapToArray<VariableDetails> variables) {
		this.actualNode = originalNodeModel.getNode();
		this.variables =variables;
		this.r = r;
		this.dummyNode = LTMUtils.createDummyNode(this.actualNode, false, r);
		this.inLinkModel = new GenericLinkModel(LTMUtils.createDummyLink(dummyNode, actualNode, r, false));
		this.inLinkModel.setLTMTimeBeanAndRouteSet(LTMTimePoints, new MapToArray<NetworkRoute>("OriginForRoute",List.of(r)));
		if(variables!=null)this.inLinkModel.setOptimizationVariables(variables);
		originalNodeModel.addDestinationNode(this);
		this.T = T;
		
	}
	
	public void generateIntendedTurnRatio(int timeStep) {
		TuplesOfThree<double[], double[], double[][]> siOut= this.inLinkModel.getSendingFlow(timeStep);
		this.Si[timeStep] = siOut.getFirst()[timeStep];
		this.Sidt[timeStep] = siOut.getSecond()[timeStep];
		this.dSi[timeStep] = siOut.getThird()[timeStep]; 
	}




	public void applyNodeModel(int timeStep) {
		this.Gi[timeStep] = this.Si[timeStep];
		this.Gidt[timeStep] = this.Sidt[timeStep];
		this.dGi[timeStep] = this.dSi[timeStep];
	}

	
	public void updateFlow(int timeStep) {
		this.inLinkModel.getNxl()[timeStep+1] = this.inLinkModel.getNxl()[timeStep]+this.Gi[timeStep];
		this.inLinkModel.getNxldt()[timeStep+1] = this.inLinkModel.getNxldt()[timeStep]+this.Gidt[timeStep];
		this.inLinkModel.getdNxl()[timeStep+1] = MatrixUtils.createRealVector(this.inLinkModel.getdNxl()[timeStep]).add(this.dGi[timeStep]).getData();
		
		this.inLinkModel.getNrxl()[0][timeStep+1] = this.inLinkModel.getNrxl()[0][timeStep]+this.Gi[timeStep];
		this.inLinkModel.getNrxldt()[0][timeStep+1] = this.inLinkModel.getNrxldt()[0][timeStep]+this.Gidt[timeStep];
		this.inLinkModel.getdNrxl()[0][timeStep+1] = MatrixUtils.createRealVector(this.inLinkModel.getdNrxl()[0][timeStep]).add(this.dGi[timeStep]).getData();
		
	}

	
	public void performLTMStep(int timeStep) {
		this.generateIntendedTurnRatio(timeStep);
		this.applyNodeModel(timeStep);
		this.updateFlow(timeStep);
	}

	
	public void setTimeStepAndRoutes() {
		// TODO Auto-generated method stub
		
	}

	
	public NetworkRoute getRoute() {
		
		return r;
	}

	public LinkModel getInLinkModel() {
		return inLinkModel;
	}

	
	

}
