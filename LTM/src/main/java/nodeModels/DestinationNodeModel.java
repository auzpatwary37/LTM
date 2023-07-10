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
	
	public TuplesOfThree<Double, Double, double[]> generateIntendedTurnRatio(int timeStep) {
		TuplesOfThree<Double, Double, double[]> siOut= this.inLinkModel.getSendingFlow(timeStep);
		return siOut;
	}




	public TuplesOfThree<Double, Double, double[]> applyNodeModel(int timeStep,TuplesOfThree<Double, Double, double[]> S ) {
		return S;
	}

	
	public void updateFlow(int timeStep,TuplesOfThree<Double, Double, double[]> Gi) {
		this.inLinkModel.getNxl()[timeStep+1] = this.inLinkModel.getNxl()[timeStep]+Gi.getFirst();
		this.inLinkModel.getNxldt()[timeStep+1] = this.inLinkModel.getNxldt()[timeStep]+Gi.getSecond();
		this.inLinkModel.getdNxl()[timeStep+1] = MatrixUtils.createRealVector(this.inLinkModel.getdNxl()[timeStep]).add(Gi.getThird()).getData();
		
		this.inLinkModel.getNrxl()[0][timeStep+1] = this.inLinkModel.getNrxl()[0][timeStep]+Gi.getFirst();
		this.inLinkModel.getNrxldt()[0][timeStep+1] = this.inLinkModel.getNrxldt()[0][timeStep]+Gi.getSecond();
		this.inLinkModel.getdNrxl()[0][timeStep+1] = MatrixUtils.createRealVector(this.inLinkModel.getdNrxl()[0][timeStep]).add(Gi.getThird()).getData();
		
	}

	
	public void performLTMStep(int timeStep) {
		TuplesOfThree<Double, Double, double[]> S = this.generateIntendedTurnRatio(timeStep);
		TuplesOfThree<Double, Double, double[]> Gi = this.applyNodeModel(timeStep,S);
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
