package ltmAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import linkModels.LinkModel;
import nodeModels.DestinationNodeModel;
import nodeModels.GenericNodeModel;
import nodeModels.NodeModel;
import nodeModels.OriginNodeModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;
import utils.LTMLoadableDemandV2;
import utils.MapToArray;
import utils.VariableDetails;

public class LTM implements DNL{
	private LTMLoadableDemandV2 demand;
	private MapToArray<VariableDetails> variables = null;
	private final Network network;
	private Map<Id<Link>,LinkModel> linkModels = new HashMap<>();
	private Map<Id<Node>,NodeModel> nodeModels = new HashMap<>();
	private Map<NetworkRoute, Tuple<OriginNodeModel,DestinationNodeModel>> routeODModels = new HashMap<>();
	private Map<NetworkRoute, Tuple<OriginNodeModel,DestinationNodeModel>> trvRouteODModels = new HashMap<>();
	private final int timeStepSize;
	public final Double minimumTimeStepSize = 5.;
	public double[] timePoints;
	
	public LTM(Network network, double minTime, double maxTime) {
		this.network = network;
		double minFFTravelTime = Double.POSITIVE_INFINITY;
		for(Link link:this.network.getLinks().values()) {
			double tt = link.getLength()/(double)link.getFreespeed();
			if(tt!=0 && tt<minFFTravelTime && tt>=this.minimumTimeStepSize) {
				minFFTravelTime = tt;
			}else {
				minFFTravelTime = this.minimumTimeStepSize;
				link.setLength(link.getFreespeed()*this.minimumTimeStepSize);
			}
		}
		this.timeStepSize = (int)minFFTravelTime;
		this.timePoints = new double[(int)((maxTime-minTime)/timeStepSize)+1];
		timePoints[0] = minTime;
		for(int i = 1;i<timePoints.length;i++)timePoints[i] = timePoints[i-1]+this.timeStepSize;
		network.getNodes().entrySet().forEach(n->{
			nodeModels.put(n.getKey(), new GenericNodeModel(n.getValue(), linkModels)); 
		});
	}
	
	
	
	@Override
	public void performLTM(LTMLoadableDemandV2 demand, MapToArray<VariableDetails> variables) {
		this.demand = demand;
		
		Map<Id<Link>,List<NetworkRoute>> totalInc = new HashMap<>();
		demand.getLinkToRouteIncidence().entrySet().forEach(e->{
			totalInc.put(e.getKey(), new ArrayList<>(e.getValue()));
		});
		demand.getLinkToTrvRouteIncidence().entrySet().forEach(e->totalInc.compute(e.getKey(), 
				(k,v)->{
					if(v==null)return v;
					else {
						v.addAll(e.getValue());
						return v;
					}
				}));
		totalInc.entrySet().forEach(e->{
			List<NetworkRoute> routes = new ArrayList<>();
			routes.addAll(e.getValue());
			this.linkModels.get(e.getKey()).setLTMTimeBeanAndRouteSet(timePoints, new MapToArray<NetworkRoute>("routes for link "+e.getKey(),e.getValue()));
			if(variables!=null)this.linkModels.get(e.getKey()).setOptimizationVariables(variables);
		});
		
		demand.getDemand().entrySet().forEach(e->{
			this.routeODModels.put(e.getKey(), new Tuple<OriginNodeModel,DestinationNodeModel>(new OriginNodeModel(e.getKey(),
					this.nodeModels.get(network.getLinks().get(e.getKey().getStartLinkId()).getFromNode().getId()),demand.getDemandTimeBean(),e.getValue(),this.timePoints,variables)
					, new DestinationNodeModel(e.getKey(), this.nodeModels.get(network.getLinks().get(e.getKey().getEndLinkId()).getToNode().getId()), timePoints, variables)));
		
			
		});
		
		demand.getTrvDemand().entrySet().forEach(e->{
			this.trvRouteODModels.put(e.getKey(), new Tuple<OriginNodeModel,DestinationNodeModel>(new OriginNodeModel(e.getKey(),
					this.nodeModels.get(network.getLinks().get(e.getKey().getStartLinkId()).getFromNode().getId()),demand.getDemandTimeBean(),e.getValue(),this.timePoints,variables)
					, new DestinationNodeModel(e.getKey(), this.nodeModels.get(network.getLinks().get(e.getKey().getEndLinkId()).getToNode().getId()), timePoints, variables)));
			
		});
		
	}

	@Override
	public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getRouteTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public double[] getTimePoints() {
		return timePoints;
	}



	@Override
	public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getTrvRouteTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMLinkFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMTrvLinkFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMCombinedLinkFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMLinkTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}

}
