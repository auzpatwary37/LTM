package nodeModels;

import java.util.List;
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

public class OriginNodeModel{
	private Node dummyNode;
	private Node actualNode;
	private NetworkRoute r;
	private Id<NetworkRoute> rId;
	private int T;
	private LinkModel outLinkModel;
	private double[] LTMTimePoints;
	
	private double[] Nr;
	private double[] Nrdt;
	private double[][] dNr;
	

	private MapToArray<VariableDetails> variables;
	private Map<String,Tuple<Double,Double>> demandTimeBean = null;
	private Map<String,Tuple<Double,double[]>>demand = null;
	private Map<Integer,Tuple<Double,double[]>>eDemand = null;
	
	
	public OriginNodeModel(Id<NetworkRoute> rId, NetworkRoute r,NodeModel originalNodeModel,Map<String,Tuple<Double,Double>> demandTimeBean, 
			Map<String,Tuple<Double,double[]>>demand, double[] LTMTimePoints, MapToArray<VariableDetails> variables) {
		this.demand =demand;
		this.demandTimeBean = demandTimeBean;
		this.actualNode = originalNodeModel.getNode();
		this.variables =variables;
		this.r = r;
		this.rId = rId;
		this.dummyNode = LTMUtils.createDummyNode(this.actualNode, true, rId);
		this.outLinkModel = new GenericLinkModel(LTMUtils.createDummyLink(dummyNode, actualNode, rId, true));
		this.outLinkModel.setLTMTimeBeanAndRouteSet(LTMTimePoints, Map.of(rId, r));
		if(variables!=null)this.outLinkModel.setOptimizationVariables(variables);
		originalNodeModel.addOriginNode(this);
		this.T = LTMTimePoints.length;
		TuplesOfThree<double[],double[],double[][]> Nrr = LTMUtils.setUpDemand(demand, demandTimeBean, variables, LTMTimePoints, 1800, true);
		this.Nr = Nrr.getFirst();
		this.Nrdt = Nrr.getSecond();
		this.dNr = Nrr.getThird();
	}
	/**
	 * For Event based demand 
	 * @param r
	 * @param originalNodeModel
	 * @param demand
	 * @param LTMTimePoints
	 * @param variables
	 */
	public OriginNodeModel(Id<NetworkRoute> rId, NetworkRoute r,NodeModel originalNodeModel,
			Map<Integer,Tuple<Double,double[]>>demand, double[] LTMTimePoints, MapToArray<VariableDetails> variables) {
		this.eDemand =demand;
		this.actualNode = originalNodeModel.getNode();
		this.variables =variables;
		this.r = r;
		this.rId = rId;
		this.dummyNode = LTMUtils.createDummyNode(this.actualNode, true, rId);
		this.outLinkModel = new GenericLinkModel(LTMUtils.createDummyLink(dummyNode, actualNode, rId, true));
		this.outLinkModel.setLTMTimeBeanAndRouteSet(LTMTimePoints, Map.of(rId,r));
		if(variables!=null)this.outLinkModel.setOptimizationVariables(variables);
		originalNodeModel.addOriginNode(this);
		this.T = LTMTimePoints.length;
		TuplesOfThree<double[],double[],double[][]> Nrr = LTMUtils.setUpDemand(eDemand,  variables, LTMTimePoints);
		this.Nr = Nrr.getFirst();
		this.Nrdt = Nrr.getSecond();
		this.dNr = Nrr.getThird();
	}
	
	
	public TuplesOfThree<Double,Double,double[]> generateIntendedTurnRatio(int timeStep) {
		TuplesOfThree<Double, Double, double[]> R = this.outLinkModel.getRecivingFlow(timeStep);
		return new TuplesOfThree<>(R.getFirst(),R.getSecond(),R.getThird());
		
	}
	
	
	public TuplesOfThree<Double, Double, double[]> applyNodeModel(int timeStep, TuplesOfThree<Double,Double,double[]>R) {
		double gj = 0;
		double gjdt = 0;
		double[] dgj = null;
		if(Nr[timeStep+1]-outLinkModel.getNx0()[timeStep]<R.getFirst()) {
			gj = Nr[timeStep+1]-outLinkModel.getNx0()[timeStep];
			gjdt = Nrdt[timeStep+1]-outLinkModel.getNx0dt()[timeStep];
			dgj = MatrixUtils.createRealVector(dNr[timeStep+1]).subtract(outLinkModel.getdNx0()[timeStep]).getData();
		}else {
			gj = R.getFirst();
			gjdt =R.getSecond();
			dgj = R.getThird();
		}
		return new TuplesOfThree<>(gj,gjdt,dgj);
		
	}

	public void updateFlow(int timeStep, TuplesOfThree<Double,Double,double[]>Gj) {
		this.outLinkModel.getNx0()[timeStep+1] = this.outLinkModel.getNx0()[timeStep]+Gj.getFirst();
		this.outLinkModel.getNrx0()[0][timeStep+1] = this.outLinkModel.getNrx0()[0][timeStep]+Gj.getFirst();
		this.outLinkModel.getNx0dt()[timeStep+1] = this.outLinkModel.getNx0dt()[timeStep]+Gj.getSecond();
		this.outLinkModel.getNrx0dt()[0][timeStep+1] = this.outLinkModel.getNrx0dt()[0][timeStep]+Gj.getSecond();
		this.outLinkModel.getdNx0()[timeStep+1] = MatrixUtils.createRealVector(this.outLinkModel.getdNx0()[timeStep]).add(Gj.getThird()).getData();
		this.outLinkModel.getdNrx0()[0][timeStep+1] = MatrixUtils.createRealVector(this.outLinkModel.getdNrx0()[0][timeStep]).add(Gj.getThird()).getData();
	}

	public void performLTMStep(int timeStep) {
		TuplesOfThree<Double,Double,double[]> R = this.generateIntendedTurnRatio(timeStep);
		TuplesOfThree<Double,Double,double[]> G = this.applyNodeModel(timeStep,R);
		this.updateFlow(timeStep,G);
	}


	public Node getActualNode() {
		return actualNode;
	}


	public LinkModel getOutLinkModel() {
		return outLinkModel;
	}


	public Tuple<Id<NetworkRoute>,NetworkRoute> getRoute() {
		return new Tuple<>(rId,r);
	}
	public Map<String, Tuple<Double, double[]>> getDemand() {
		return demand;
	}
	public void setDemand(Map<String, Tuple<Double, double[]>> demand) {
		this.demand = demand;
	}
	
	public void reset() {
		this.Nr = new double[this.LTMTimePoints.length];
		this.Nrdt = new double[this.LTMTimePoints.length];
		this.dNr = new double[this.LTMTimePoints.length][this.variables.getKeySet().size()];

			
	}
	
}
