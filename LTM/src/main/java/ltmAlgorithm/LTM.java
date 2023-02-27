package ltmAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;

import linkModels.LinkModel;
import nodeModels.DestinationNodeModel;
import nodeModels.GenericNodeModel;
import nodeModels.NodeModel;
import nodeModels.OriginNodeModel;
import transit.LinkTransitPassengerModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SignalFlowReductionGenerator;
import utils.EventBasedLTMLoadableDemand;
import utils.LTMLoadableDemandV2;
import utils.LTMUtils;
import utils.MapToArray;
import utils.TuplesOfThree;
import utils.VariableDetails;

public class LTM implements DNL{
	private LTMLoadableDemandV2 demand ;
	private EventBasedLTMLoadableDemand eDemand;
	private MapToArray<VariableDetails> variables = null;
	private final Network network;
	private Map<Id<Link>,LinkModel> linkModels = new HashMap<>();
	private Map<Id<Node>,NodeModel> nodeModels = new HashMap<>();
	private Map<Id<Link>,LinkTransitPassengerModel> linkPassengerModel = new HashMap<>();
	private Map<NetworkRoute, Tuple<OriginNodeModel,DestinationNodeModel>> routeODModels = new HashMap<>();
	private Map<NetworkRoute, Tuple<OriginNodeModel,DestinationNodeModel>> trvRouteODModels = new HashMap<>();
	private final int timeStepSize;
	public final Double minimumTimeStepSize = 5.;
	public double[] timePoints;
	public Map<NetworkRoute,double[]> transitPassengerCapacity;
	
	public LTM(Network network, double minTime, double maxTime, SignalFlowReductionGenerator sg) {
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
			nodeModels.put(n.getKey(), new GenericNodeModel(n.getValue(), linkModels,sg)); 
		});
	}
	
	
	
	@Override
	public void performLTM(LTMLoadableDemandV2 demand, MapToArray<VariableDetails> variables) {
		this.demand = demand;
		this.variables = variables;
		Map<Id<Link>,List<NetworkRoute>> totalInc = new HashMap<>();
		demand.getLinkToRouteIncidence().entrySet().forEach(e->{
			totalInc.put(e.getKey(), new ArrayList<>(e.getValue()));
		});
		demand.getLinkToTrvRouteIncidence().entrySet().forEach(e->totalInc.compute(e.getKey(), 
				(k,v)->{
					if(v==null) {
						List<NetworkRoute> rs = new ArrayList<>();
						rs.addAll(e.getValue());
						return rs;
					}
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
		this.runLTMSimulation();
		this.simulateTransit();
	}
	
	@Override
	public void reperformLTM(LTMLoadableDemandV2 demand, MapToArray<VariableDetails> variables) {
		this.demand = demand;
		this.variables = variables;
//		Map<Id<Link>,List<NetworkRoute>> totalInc = new HashMap<>();
//		demand.getLinkToRouteIncidence().entrySet().forEach(e->{
//			totalInc.put(e.getKey(), new ArrayList<>(e.getValue()));
//		});
//		demand.getLinkToTrvRouteIncidence().entrySet().forEach(e->totalInc.compute(e.getKey(), 
//				(k,v)->{
//					if(v==null) {
//						List<NetworkRoute> rs = new ArrayList<>();
//						rs.addAll(e.getValue());
//						return rs;
//					}
//					else {
//						v.addAll(e.getValue());
//						return v;
//					}
//				}));
//		totalInc.entrySet().forEach(e->{
//			List<NetworkRoute> routes = new ArrayList<>();
//			routes.addAll(e.getValue());
//			this.linkModels.get(e.getKey()).setLTMTimeBeanAndRouteSet(timePoints, new MapToArray<NetworkRoute>("routes for link "+e.getKey(),e.getValue()));
//			if(variables!=null)this.linkModels.get(e.getKey()).setOptimizationVariables(variables);
//		});
//		
		demand.getDemand().entrySet().forEach(e->{
			if(!this.routeODModels.containsKey(e.getKey())) {
				this.routeODModels.put(e.getKey(), new Tuple<OriginNodeModel,DestinationNodeModel>(new OriginNodeModel(e.getKey(),
						this.nodeModels.get(network.getLinks().get(e.getKey().getStartLinkId()).getFromNode().getId()),demand.getDemandTimeBean(),e.getValue(),this.timePoints,variables)
						, new DestinationNodeModel(e.getKey(), this.nodeModels.get(network.getLinks().get(e.getKey().getEndLinkId()).getToNode().getId()), timePoints, variables)));
			}else {
				this.routeODModels.get(e.getKey()).getFirst().setDemand(e.getValue());
			}
			
		});
		
		
		this.runLTMSimulation();
		this.simulateTransit();
	}
	
	
	@Override
	public void performLTM(EventBasedLTMLoadableDemand demand, MapToArray<VariableDetails> variables) {
		this.eDemand = demand;
		this.variables = variables;
		Map<Id<Link>,List<NetworkRoute>> totalInc = new HashMap<>();
		demand.getLinkToRouteIncidence().entrySet().forEach(e->{
			totalInc.put(e.getKey(), new ArrayList<>(e.getValue()));
		});
		eDemand.getLinkToTrvRouteIncidence().entrySet().forEach(e->totalInc.compute(e.getKey(), 
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
		
		eDemand.getDemand().entrySet().forEach(e->{
			this.routeODModels.put(e.getKey(), new Tuple<OriginNodeModel,DestinationNodeModel>(new OriginNodeModel(e.getKey(),
					this.nodeModels.get(network.getLinks().get(e.getKey().getStartLinkId()).getFromNode().getId()),e.getValue(),this.timePoints,variables)
					, new DestinationNodeModel(e.getKey(), this.nodeModels.get(network.getLinks().get(e.getKey().getEndLinkId()).getToNode().getId()), timePoints, variables)));
		
			
		});
		
		eDemand.getTrvDemand().entrySet().forEach(e->{
			this.trvRouteODModels.put(e.getKey(), new Tuple<OriginNodeModel,DestinationNodeModel>(new OriginNodeModel(e.getKey(),
					this.nodeModels.get(network.getLinks().get(e.getKey().getStartLinkId()).getFromNode().getId()),e.getValue(),this.timePoints,variables)
					, new DestinationNodeModel(e.getKey(), this.nodeModels.get(network.getLinks().get(e.getKey().getEndLinkId()).getToNode().getId()), timePoints, variables)));
			
		});
		this.runLTMSimulation();
		
	}
	private void runLTMSimulation() {
		for(int i = 0; i<this.timePoints.length-1;i++) {
			int timeStep = i;
			this.nodeModels.entrySet().parallelStream().forEach(e->{
				e.getValue().performLTMStep(timeStep);
			});
		}
	}

	@Override
	public Map<NetworkRoute, Map<String, Tuple<Double, double[]>>> getTimeBeanRouteTravelTime(int numberOfPointsToAverage) {
		if(this.demand==null)throw new IllegalArgumentException("TimeBean specific demand is null!!!");
		if(numberOfPointsToAverage<1 ||numberOfPointsToAverage>3) {
			System.out.println("Number of points to average can not be less than 1 or greater than 3");
			numberOfPointsToAverage = 3;
		}
		Map<NetworkRoute,Map<String,Tuple<Double,double[]>>> tt = new HashMap<>();
		int n = numberOfPointsToAverage;
		this.demand.getDemand().entrySet().parallelStream().forEach(d->{
			tt.put(d.getKey(), new HashMap<>());
			for(Entry<String, Tuple<Double, double[]>> timeD:d.getValue().entrySet()) {
				
				double[] departureTime = new double[n];
				double travelTime = 0;
				RealVector travelTimeGrad = MatrixUtils.createRealVector(new double[this.variables.getKeySet().size()]);
				for(int i = 0;i<n;i++) {
					departureTime[i] = this.demand.getDemandTimeBean().get(timeD.getKey()).getFirst()+
							(this.demand.getDemandTimeBean().get(timeD.getKey()).getSecond()-
									this.demand.getDemandTimeBean().get(timeD.getKey()).getFirst())/n*i;
					TuplesOfThree<Double,Double,double[]> out = LTMUtils.getRouteTravelTime(d.getKey(), timePoints, departureTime[i],
							this.routeODModels.get(d.getKey()).getFirst().getOutLinkModel(), this.routeODModels.get(d.getKey()).getSecond().getInLinkModel());
					travelTime += 1./n*out.getFirst();
					travelTimeGrad = travelTimeGrad.add(out.getThird()).mapMultiply(1./n);
				}
				tt.get(d.getKey()).put(timeD.getKey(), new Tuple<>(travelTime,travelTimeGrad.getData()));
			}
		});
		return tt;
	}
	@Override
	public Map<NetworkRoute, Map<Integer, Tuple<Double, double[]>>> getTimeStampedRouteTravelTime() {
		if(eDemand==null)throw new IllegalArgumentException("Event based demand is null!!!");
		Map<NetworkRoute, Map<Integer, Tuple<Double, double[]>>> tt = new HashMap<>();
		this.eDemand.getDemand().entrySet().parallelStream().forEach(d->{
			tt.put(d.getKey(), new HashMap<>());
			for(Entry<Integer, Tuple<Double, double[]>> timeD:d.getValue().entrySet()) {
				TuplesOfThree<Double,Double,double[]> result = LTMUtils.getRouteTravelTime(d.getKey(), timePoints, timeD.getKey(), 
						this.routeODModels.get(d.getKey()).getFirst().getOutLinkModel(), this.routeODModels.get(d.getKey()).getSecond().getInLinkModel());
				tt.get(d.getKey()).put(timeD.getKey(), new Tuple<>(result.getFirst(),result.getThird()));
			}
		});
		return tt;
	}
	
	public double[] getTimePoints() {
		return timePoints;
	}


	@Override
	public Map<String, Map<Id<Link>, Tuple<Double,double[]>>> getLTMLinkFlow(Map<String,Set<Id<Link>>>links,Map<String,Tuple<Double,Double>> timeBean) {
		Map<String,Map<Id<Link>,Tuple<Double,double[]>>> linkFlow = new HashMap<>();
		links.entrySet().forEach(t->{
			linkFlow.put(t.getKey(), new HashMap<>());
			t.getValue().parallelStream().forEach(lId->{
				LinkModel l = this.linkModels.get(lId);
				TuplesOfThree<Double,Double,double[]> out = LTMUtils.getLinkVolume(l, timeBean.get(t.getKey()).getFirst(), timeBean.get(t.getKey()).getSecond(), timePoints);
				linkFlow.get(t.getKey()).put(lId, new Tuple<>(out.getFirst(),out.getThird()));
			});
		});
		return linkFlow;
	}
	

	@Override
	public Map<String, Map<Id<Link>, Tuple<Double,double[]>>> getLTMLinkFlow(Map<String,Tuple<Double,Double>> timeBean) {
		Map<String,Map<Id<Link>,Tuple<Double,double[]>>> linkFlow = new HashMap<>();
		timeBean.entrySet().forEach(t->{
			linkFlow.put(t.getKey(), new HashMap<>());
			this.linkModels.entrySet().parallelStream().forEach(l->{
				TuplesOfThree<Double,Double,double[]> out = LTMUtils.getLinkVolume(l.getValue(), t.getValue().getFirst(), t.getValue().getSecond(), timePoints);
				linkFlow.get(t.getKey()).put(l.getKey(), new Tuple<>(out.getFirst(),out.getThird()));
			});
		});
		return linkFlow;
	}

	



	@Override
	public Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>,Tuple<Double, double[]>>>>> getTimeBeanTransitTravelAndWaitingTime(int numberOfPointsToAverage) {
		if(this.demand==null)throw new IllegalArgumentException("TimeBean specific demand is null!!!");
		if(numberOfPointsToAverage<1 ||numberOfPointsToAverage>3) {
			System.out.println("Number of points to average can not be less than 1 or greater than 3");
			numberOfPointsToAverage = 3;
		}
		int n = numberOfPointsToAverage;
		Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>,Tuple<Double, double[]>>>>> result = new HashMap<>();
		this.demand.getTransitTravelTimeQuery().entrySet().parallelStream().forEach(d->{
			result.put(d.getKey(), new HashMap<>());
			for(Entry<String, Map<Tuple<Id<Link>, Id<Link>>, Tuple<Double, double[]>>> timeD:d.getValue().entrySet()) {
				result.get(d.getKey()).put(timeD.getKey(), new HashMap<>());
				for(Tuple<Id<Link>,Id<Link>> lPair:timeD.getValue().keySet()) {
					double[] departureTime = new double[n];
					double travelTime = 0;
					double waitTime = 0;
					RealVector travelTimeGrad = MatrixUtils.createRealVector(new double[this.variables.getKeySet().size()]);
					RealVector waitTimeGrad = MatrixUtils.createRealVector(new double[this.variables.getKeySet().size()]);
					for(int i = 0;i<n;i++) {
						departureTime[i] = this.demand.getDemandTimeBean().get(timeD.getKey()).getFirst()+
								(this.demand.getDemandTimeBean().get(timeD.getKey()).getSecond()-
										this.demand.getDemandTimeBean().get(timeD.getKey()).getFirst())/n*i;
						Tuple<TuplesOfThree<Double, Double, double[]>, TuplesOfThree<Double, Double, double[]>> out = LTMUtils.getTransitRouteWaitAndTravelTime(d.getKey(), this.timePoints, departureTime[i], this.linkPassengerModel.get(lPair.getFirst()), this.linkPassengerModel.get(lPair.getSecond()));
						travelTime+=1./n*out.getFirst().getFirst();
						travelTimeGrad = travelTimeGrad.add(out.getFirst().getThird()).mapMultiply(1./n);
						waitTime+=1./n*out.getSecond().getFirst();
						waitTimeGrad = travelTimeGrad.add(out.getSecond().getThird()).mapMultiply(1./n);
						
					}
					result.get(d.getKey()).get(timeD.getKey()).put(lPair.getFirst().toString()+"___"+lPair.getSecond().toString(), new Tuple<>(new Tuple<>(travelTime,travelTimeGrad.getData()),new Tuple<>(waitTime,waitTimeGrad.getData())));
				}
			}
		});
		return result;
	}



	@Override
	public Map<NetworkRoute, Map<Integer, Map<Tuple<Id<Link>, Id<Link>>, Tuple<Tuple<Double,double[]>,Tuple<Double, double[]>>>>> getTimeStampedTransitTravelAndWaitingTime() {
		if(eDemand==null)throw new IllegalArgumentException("Event based demand is null!!!");
		Map<NetworkRoute, Map<Integer, Map<Tuple<Id<Link>, Id<Link>>, Tuple<Tuple<Double,double[]>,Tuple<Double, double[]>>>>> result = new HashMap<>();
		this.eDemand.getTransitTravelTimeQuery().entrySet().parallelStream().forEach(d->{
			result.put(d.getKey(), new HashMap<>());
			for(Entry<Integer, Map<Tuple<Id<Link>, Id<Link>>, Tuple<Double, double[]>>> timeD:d.getValue().entrySet()) {
				result.get(d.getKey()).put(timeD.getKey(), new HashMap<>());
				for(Tuple<Id<Link>, Id<Link>> linkPair:timeD.getValue().keySet()) {
					Tuple<TuplesOfThree<Double, Double, double[]>, TuplesOfThree<Double, Double, double[]>> out = LTMUtils.getTransitRouteWaitAndTravelTime(d.getKey(), timePoints, (double)timeD.getKey(), this.linkPassengerModel.get(linkPair.getFirst()), this.linkPassengerModel.get(linkPair.getSecond()));
					result.get(d.getKey()).get(timeD.getKey()).put(linkPair, new Tuple<>(new Tuple<>(out.getFirst().getFirst(),out.getFirst().getThird()),new Tuple<>(out.getSecond().getFirst(),out.getSecond().getThird())));
				}
			}
		});
		return result;
	}
	
	@Override
	public void simulateTransit() {
		Map<NetworkRoute,double[]>capacity = LTMUtils.processTransitPassengerCapacity(demand.getCapacity(), demand.getDemandTimeBean(), timePoints);
		for(Entry<Id<Link>, Set<NetworkRoute>> link:demand.getLinkToTrvRouteIncidence().entrySet()) {
			LinkTransitPassengerModel lpModel = null;
			if(!this.linkPassengerModel.containsKey(link.getKey())) {
				lpModel = new LinkTransitPassengerModel(this.linkModels.get(link.getKey()), linkPassengerModel);
			}else {
				lpModel = this.linkPassengerModel.get(link.getKey());
			}
			for(NetworkRoute r:link.getValue()) {
				
				TuplesOfThree<Map<Id<Link>, double[]>, Map<Id<Link>, double[]>, Map<Id<Link>, double[][]>> d = LTMUtils.generatePassengerDemand(demand, r, link.getKey(),this.timePoints);
				lpModel.addRoute(r,capacity.get(r),d.getFirst(),d.getThird(),d.getSecond());
			}
		}	
		
		for(int t = 0; t<timePoints.length-1;t++) {//loop through each time step
			int tt = t;
			this.linkPassengerModel.entrySet().parallelStream().forEach(l->{
				l.getValue().updateFlow(tt);
			});
		}
		
	}
	
	
	
	public static ActivityFacility drawRandomFacility(Map<Id<ActivityFacility>,ActivityFacility> facilities) {

        Random generator = new Random();

        List<Map.Entry<Id<ActivityFacility>,ActivityFacility>> entries = new ArrayList<>(facilities.entrySet());

        ActivityFacility randomValue = entries.get(generator.nextInt(entries.size())).getValue();

        return randomValue;

    }



	@Override
	public void reSimulateTransit() {
		this.linkModels.values().parallelStream().forEach(l->l.reset());
		this.nodeModels.values().parallelStream().forEach(n->n.reset());
		this.routeODModels.entrySet().parallelStream().forEach(n->{
			n.getValue().getFirst().reset();
			n.getValue().getSecond().reset();
		});
	}

}
