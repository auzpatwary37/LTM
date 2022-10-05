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

public class OriginNodeModel{
	private Node dummyNode;
	private Node actualNode;
	private NetworkRoute r;
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
	private Map<String,Tuple<Double,Double>> demandTimeBean;
	private Map<String,Tuple<Double,double[]>>demand;
	
	public OriginNodeModel(NetworkRoute r,NodeModel originalNodeModel,Map<String,Tuple<Double,Double>> demandTimeBean, 
			Map<String,Tuple<Double,double[]>>demand, int T, double[] LTMTimePoints, MapToArray<VariableDetails> variables) {
		this.demand =demand;
		this.demandTimeBean = demandTimeBean;
		this.actualNode = originalNodeModel.getNode();
		this.variables =variables;
		this.r = r;
		this.dummyNode = LTMUtils.createDummyNode(this.actualNode, true, r);
		this.outLinkModel = new GenericLinkModel(LTMUtils.createDummyLink(dummyNode, actualNode, r, true));
		this.outLinkModel.setLTMTimeBeanAndRouteSet(LTMTimePoints, new MapToArray<NetworkRoute>("OriginForRoute",List.of(r)));
		originalNodeModel.addOriginNode(this);
		this.T = T;
		TuplesOfThree<double[],double[],double[][]> Nrr = LTMUtils.setUpDemand(demand, demandTimeBean, variables, T, LTMTimePoints, 1800, true);
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


	public NetworkRoute getRoute() {
		return r;
	}
	
	
}
