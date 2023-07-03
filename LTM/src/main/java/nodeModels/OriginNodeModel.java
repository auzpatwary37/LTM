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
	
	
	private double[] Gj;
	private double[][] dGj;
	private double[] Gjdt;
	
	private double[] Rj;
	private double[][] dRj;
	private double[] Rjdt;
	
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
	
	
	public void generateIntendedTurnRatio(int timeStep) {
		TuplesOfThree<double[], double[], double[][]> R = this.outLinkModel.getRecivingFlow(timeStep);
		this.Rj[timeStep] = R.getFirst()[timeStep];
		this.Rjdt[timeStep] = R.getSecond()[timeStep];
		this.dRj[timeStep] = R.getThird()[timeStep];
		
	}
	
	
	public void applyNodeModel(int timeStep) {
		if(Nr[timeStep+1]-outLinkModel.getNx0()[timeStep]<Rj[timeStep]) {
			this.Gj[timeStep] = Nr[timeStep+1]-outLinkModel.getNx0()[timeStep];
			this.Gjdt[timeStep] = Nrdt[timeStep+1]-outLinkModel.getNx0dt()[timeStep];
			this.dGj[timeStep] = MatrixUtils.createRealVector(dNr[timeStep+1]).subtract(outLinkModel.getdNx0()[timeStep]).getData();
		}else {
			this.Gj[timeStep] = Rj[timeStep];
			this.Gjdt[timeStep] =Rjdt[timeStep];
			this.dGj[timeStep] = dRj[timeStep];
		}
		
		
	}

	public void updateFlow(int timeStep) {
		this.outLinkModel.getNx0()[timeStep+1] = this.outLinkModel.getNx0()[timeStep]+this.Gj[timeStep];
		this.outLinkModel.getNrx0()[0][timeStep+1] = this.outLinkModel.getNrx0()[0][timeStep]+this.Gj[timeStep];
		this.outLinkModel.getNx0dt()[timeStep+1] = this.outLinkModel.getNx0dt()[timeStep]+this.Gjdt[timeStep];
		this.outLinkModel.getNrx0dt()[0][timeStep+1] = this.outLinkModel.getNrx0dt()[0][timeStep]+this.Gjdt[timeStep];
		this.outLinkModel.getdNx0()[timeStep+1] = MatrixUtils.createRealVector(this.outLinkModel.getdNx0()[timeStep]).add(this.dGj[timeStep]).getData();
		this.outLinkModel.getdNrx0()[0][timeStep+1] = MatrixUtils.createRealVector(this.outLinkModel.getdNrx0()[0][timeStep]).add(this.dGj[timeStep]).getData();
	}

	public void performLTMStep(int timeStep) {
		this.generateIntendedTurnRatio(timeStep);
		this.applyNodeModel(timeStep);
		this.updateFlow(timeStep);
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
		this.Gj = new double[this.LTMTimePoints.length];
		this.Gjdt = new double[this.LTMTimePoints.length];
		this.dGj = new double[this.LTMTimePoints.length][this.variables.getKeySet().size()];
			
	}
	
}
