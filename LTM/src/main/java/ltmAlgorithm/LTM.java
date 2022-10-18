package ltmAlgorithm;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import linkModels.GenericLinkModel;
import linkModels.LinkModel;
import nodeModels.DestinationNodeModel;
import nodeModels.GenericNodeModel;
import nodeModels.NodeModel;
import nodeModels.OriginNodeModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;
import utils.LTMLoadableDemand;

public class LTM implements DNL{
	
	private final Network network;
	private Map<Id<Link>,LinkModel> linkModels = new HashMap<>();
	private Map<Id<Node>,NodeModel> nodeModels = new HashMap<>();
	private Map<NetworkRoute, Tuple<OriginNodeModel,DestinationNodeModel>> ODModels = new HashMap<>();
	private final int timeStepSize;
	public final Double minimumTimeStepSize = 5.;
	
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
		network.getNodes().entrySet().forEach(n->{
			nodeModels.put(n.getKey(), new GenericNodeModel(n.getValue(), linkModels));
		});
	}
	
	
	
	@Override
	public void performLTM(LTMLoadableDemand demand) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getRouteTravelTime() {
		// TODO Auto-generated method stub
		return null;
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
