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
	
	public static int globalTimeStep = 0;
	
	
	private MapToArray<VariableDetails> variables;

	
	public DestinationNodeModel(Id<NetworkRoute>rId,NetworkRoute r,NodeModel originalNodeModel, double[] LTMTimePoints, MapToArray<VariableDetails> variables) {
		this.actualNode = originalNodeModel.getNode();
		this.variables =variables;
		this.r = r;
		this.rId = rId;
		this.dummyNode = LTMUtils.createDummyNode(this.actualNode, false, rId);
		this.inLinkModel = new GenericLinkModel(LTMUtils.createDummyLink(actualNode, dummyNode, rId, false));
		this.inLinkModel.setLTMTimeBeanAndRouteSet(LTMTimePoints, Map.of(rId,r));
		if(variables!=null)this.inLinkModel.setOptimizationVariables(variables);
		originalNodeModel.addDestinationNode(this);
		this.T = LTMTimePoints.length;
		this.LTMTimePoints = LTMTimePoints;
		
	}
	
	public Tuple<Double, double[]> generateIntendedTurnRatio(int timeStep) {
		Tuple<Double, double[]> siOut= this.inLinkModel.getSendingFlow(timeStep);
		return siOut;
	}




	public Tuple<Double, double[]> applyNodeModel(int timeStep,Tuple<Double, double[]> S ) {
		return S;
	}

	
	public void updateFlow(int timeStep,Tuple<Double, double[]> Gi) {
		this.inLinkModel.getNxl()[timeStep+1] = this.inLinkModel.getNxl()[timeStep]+Gi.getFirst();
		this.inLinkModel.getNxldt()[timeStep+1] = Gi.getFirst()/(this.LTMTimePoints[timeStep+1]-this.LTMTimePoints[timeStep]);
		this.inLinkModel.getdNxl()[timeStep+1] = MatrixUtils.createRealVector(this.inLinkModel.getdNxl()[timeStep]).add(Gi.getSecond()).getData();
		
		this.inLinkModel.getNrxl()[0][timeStep+1] = this.inLinkModel.getNrxl()[0][timeStep]+Gi.getFirst();
		this.inLinkModel.getNrxldt()[0][timeStep+1] = Gi.getFirst()/(this.LTMTimePoints[timeStep+1]-this.LTMTimePoints[timeStep]);
		this.inLinkModel.getdNrxl()[0][timeStep+1] = MatrixUtils.createRealVector(this.inLinkModel.getdNrxl()[0][timeStep]).add(Gi.getSecond()).getData();
		
	}

	
	public void performLTMStep(int timeStep) {
		Tuple<Double, double[]> S = this.generateIntendedTurnRatio(timeStep);
		Tuple<Double, double[]> Gi = this.applyNodeModel(timeStep,S);
		this.updateFlow(timeStep,Gi);
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

	}
	

}
