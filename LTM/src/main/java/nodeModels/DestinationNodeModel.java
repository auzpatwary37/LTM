package nodeModels;

import java.util.Map;

import org.apache.commons.math.linear.MatrixUtils;
import org.matsim.api.core.v01.Id;
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
	private Id<NetworkRoute>rId;
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

	
	public DestinationNodeModel(Id<NetworkRoute>rId,NetworkRoute r,NodeModel originalNodeModel, double[] LTMTimePoints, MapToArray<VariableDetails> variables) {
		this.actualNode = originalNodeModel.getNode();
		this.variables =variables;
		this.r = r;
		this.rId = rId;
		this.dummyNode = LTMUtils.createDummyNode(this.actualNode, false, rId);
		this.inLinkModel = new GenericLinkModel(LTMUtils.createDummyLink(dummyNode, actualNode, rId, false));
		this.inLinkModel.setLTMTimeBeanAndRouteSet(LTMTimePoints, Map.of(rId,r));
		if(variables!=null)this.inLinkModel.setOptimizationVariables(variables);
		originalNodeModel.addDestinationNode(this);
		this.T = LTMTimePoints.length;
		this.LTMTimePoints = LTMTimePoints;
		
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

	
	public Tuple<Id<NetworkRoute>,NetworkRoute> getRoute() {
		
		return new Tuple<Id<NetworkRoute>,NetworkRoute>(rId,r);
	}

	public LinkModel getInLinkModel() {
		return inLinkModel;
	}

	public void reset() {
		this.Gi = new double[this.LTMTimePoints.length];
		this.dGi = new double[this.LTMTimePoints.length][this.variables.getKeySet().size()];
		this.Gidt = new double[this.LTMTimePoints.length];
		
		this.Si = new double[this.LTMTimePoints.length];
		this.dSi = new double[this.LTMTimePoints.length][this.variables.getKeySet().size()];
		this.Sidt = new double[this.LTMTimePoints.length];
	}
	

}
