package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitDirectLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitLink;

/**
 * This will be the liason class between the demand input and corresponding route travel time output
 * 
 * @author ashrafzaman
 *
 */
public class LTMLoadableDemandV2 {
	private Map<String,NetworkRoute> routes = new HashMap<>();
	private Map<String,NetworkRoute> trvRoutes = new HashMap<>();
	private Map<NetworkRoute, Map<String,Tuple<Double,double[]>>> demand;
	private Map<NetworkRoute, Map<String,Tuple<Double,double[]>>> trvDemand;
	private Map<String,Tuple<Double,Double>> demandTimeBean;
	private Map<NetworkRoute,Map<String,Map<Tuple<Id<Link>,Id<Link>>,Tuple<Double,double[]>>>> transitTravelTimeQuery;
	
	private Map<Id<Link>,Set<NetworkRoute>> linkToRouteIncidence = new HashMap<>();
	private Map<Id<Link>,Set<NetworkRoute>> linkToTrvRouteIncidence = new HashMap<>();
	private Map<Id<TransitLine>,Map<Id<TransitRoute>,NetworkRoute>> lineRouteMapping = new HashMap<>();
	private Map<String, Map<NetworkRoute,Double>> capacity = new HashMap<>();
	
	//Info saved if created from metamodel 
	
	private Map<Id<AnalyticalModelRoute>,Map<String,Tuple<Double,double[]>>> demandFromMetamodel;
	private Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> routesFromMetamodel;
	private TransitSchedule ts;
	private Vehicles trv;
	private Map<String,Map<Id<TransitLink>,Tuple<Double,double[]>>> transitDemandFromMetamodel;
	private Map<String,Map<Id<TransitLink>, TransitDirectLink>> trDirectLinksFromMetamodel = new HashMap<>();
	private MapToArray<VariableDetails> variables;
	private double scalingFactor;
	
	public LTMLoadableDemandV2(Map<NetworkRoute, Map<String,Tuple<Double,double[]>>> demand, Map<NetworkRoute, Map<String,Tuple<Double,double[]>>> trvDemand, Map<String, Tuple<Double,Double>> timeBean,
			Map<NetworkRoute,Map<String,Map<Tuple<Id<Link>,Id<Link>>,Tuple<Double,double[]>>>> transitSubRouteQuery,Map<Id<TransitLine>,Map<Id<TransitRoute>,NetworkRoute>>lineRouteMapping, 
			Map<String,Map<NetworkRoute,Double>>capacity, MapToArray<VariableDetails> variables) {
		this.demand = demand;
		this.trvDemand = trvDemand;
		this.demandTimeBean = timeBean;
		this.transitTravelTimeQuery = transitSubRouteQuery;
		this.variables = variables;
		demand.keySet().stream().forEach(a->routes.put(a.getRouteDescription(), a));
		trvDemand.keySet().stream().forEach(a->trvRoutes.put(a.getRouteDescription(),a));	
		routes.values().forEach(r->{
			if(!this.linkToRouteIncidence.containsKey(r.getStartLinkId()))this.linkToRouteIncidence.put(r.getStartLinkId(), new HashSet<>());
			if(!this.linkToRouteIncidence.containsKey(r.getEndLinkId()))this.linkToRouteIncidence.put(r.getEndLinkId(), new HashSet<>());
			this.linkToRouteIncidence.get(r.getStartLinkId()).add(r);
			this.linkToRouteIncidence.get(r.getEndLinkId()).add(r);
			r.getLinkIds().stream().forEach(l->{
				if(!this.linkToRouteIncidence.containsKey(l))this.linkToRouteIncidence.put(l, new HashSet<>());
				this.linkToRouteIncidence.get(l).add(r);
			});
		});
		
		trvRoutes.values().forEach(r->{
			if(!this.linkToTrvRouteIncidence.containsKey(r.getStartLinkId()))this.linkToTrvRouteIncidence.put(r.getStartLinkId(), new HashSet<>());
			if(!this.linkToTrvRouteIncidence.containsKey(r.getEndLinkId()))this.linkToTrvRouteIncidence.put(r.getEndLinkId(), new HashSet<>());
			this.linkToTrvRouteIncidence.get(r.getStartLinkId()).add(r);
			this.linkToTrvRouteIncidence.get(r.getEndLinkId()).add(r);
			r.getLinkIds().stream().forEach(l->{
				if(!this.linkToTrvRouteIncidence.containsKey(l))this.linkToTrvRouteIncidence.put(l, new HashSet<>());
				this.linkToTrvRouteIncidence.get(l).add(r);
			});
		});
		this.lineRouteMapping = lineRouteMapping;
		this.capacity = capacity;
	}
	
	/**
	 * 
	 * @param demand route demand from the metamodel
	 * @param routes analytical model routes from the metamodel
	 * @param ts
	 * @param trv
	 * @param transitDemand
	 * @param trLinks
	 * @param timeBean
	 * @param variables
	 * @param transitScalingFactor
	 */
	public LTMLoadableDemandV2(Map<Id<AnalyticalModelRoute>,Map<String,Tuple<Double,double[]>>> demand,Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> routes, TransitSchedule ts, Vehicles trv,
			Map<String,Map<Id<TransitLink>,Tuple<Double,double[]>>> transitDemand, Map<String,Map<Id<TransitLink>, TransitLink>> trLinks, Map<String,Tuple<Double,Double>>timeBean, 
			MapToArray<VariableDetails>variables, double transitScalingFactor) {
		
		this.demandFromMetamodel = demand;
		this.routesFromMetamodel = routes;
		this.ts = ts;
		this.trv = trv;
		this.transitDemandFromMetamodel = transitDemand;
		
		trLinks.entrySet().forEach(t->{
			this.trDirectLinksFromMetamodel.put(t.getKey(), new HashMap<>());
			t.getValue().entrySet().forEach(trl->{
				if(trl instanceof TransitDirectLink) {
					this.trDirectLinksFromMetamodel.get(t.getKey()).put(trl.getKey(), (TransitDirectLink)trl.getValue());
				}
			});
		});
		
		this.demandTimeBean = timeBean;
		this.variables = variables;
		this.scalingFactor = transitScalingFactor;
		
		this.demand = new HashMap<>();
		
		for(Entry<Id<AnalyticalModelRoute>, Map<String, Tuple<Double, double[]>>> e:this.demandFromMetamodel.entrySet()) {
			this.demand.put((NetworkRoute)this.routesFromMetamodel.get(e.getKey()).getRoute(), e.getValue());
			NetworkRoute r = (NetworkRoute)this.routesFromMetamodel.get(e.getKey()).getRoute();
			this.routes.put(e.getKey().toString(), r);
			Id<Link> startLink = r.getStartLinkId();
			Id<Link> endLink = r.getEndLinkId();
			if(!this.linkToRouteIncidence.containsKey(startLink))this.linkToRouteIncidence.put(startLink, new HashSet<>());
			this.linkToRouteIncidence.get(startLink).add(r);
			if(!this.linkToRouteIncidence.containsKey(endLink))this.linkToRouteIncidence.put(endLink, new HashSet<>());
			this.linkToRouteIncidence.get(endLink).add(r);
			for(Id<Link>l:r.getLinkIds()) {
				if(!this.linkToRouteIncidence.containsKey(l))this.linkToRouteIncidence.put(l, new HashSet<>());
				this.linkToRouteIncidence.get(l).add(r);
			}
		}
		
		for(Entry<Id<TransitLine>, TransitLine> tl:this.ts.getTransitLines().entrySet()) {
			this.lineRouteMapping.put(tl.getKey(), new HashMap<>());
			for(Entry<Id<TransitRoute>, TransitRoute> tr:tl.getValue().getRoutes().entrySet()) {
				this.lineRouteMapping.get(tl.getKey()).put(tr.getKey(), tr.getValue().getRoute());
				this.trvRoutes.put(tl.getKey().toString()+"___"+tr.getKey().toString(), tr.getValue().getRoute());
				Map<String,Tuple<Double,double[]>> demandFromRoute = new HashMap<>();
				Map<String,Double> space = new HashMap<>();
				Map<String,Integer> cnt = new HashMap<>();
				for(Entry<Id<Departure>, Departure> dp:tr.getValue().getDepartures().entrySet()) {
					Vehicle v = this.trv.getVehicles().get(dp.getValue().getVehicleId());
					String timeBeanId = getTimeBean(demandTimeBean, dp.getValue().getDepartureTime());
					space.compute(timeBeanId, (k,vv)->vv==null?v.getType().getCapacity().getSeats()+v.getType().getCapacity().getStandingRoom():vv+v.getType().getCapacity().getSeats()+v.getType().getCapacity().getStandingRoom());
					cnt.compute(timeBeanId, (k,vv)->vv==null?1:vv+1);
					
					if(!demandFromRoute.containsKey(timeBeanId))demandFromRoute.put(timeBeanId, new Tuple<Double,double[]>(0.,new double[this.variables.getKeySet().size()]));
					demandFromRoute.put(timeBeanId, new Tuple<Double,double[]>(demandFromRoute.get(timeBeanId).getFirst()+v.getType().getPcuEquivalents()*this.scalingFactor,
							LTMUtils.sum(demandFromRoute.get(timeBeanId).getSecond(), new double[this.variables.getKeySet().size()])));
				}
				
				NetworkRoute r = tr.getValue().getRoute();
				for(Entry<String, Double> s:space.entrySet()) {
					if(!this.capacity.containsKey(s.getKey()))this.capacity.put(s.getKey(), new HashMap<>());
					if(cnt.get(s.getKey())!=0)this.capacity.get(s.getKey()).put(r, s.getValue()/cnt.get(s.getKey()));
				}
				if(!this.linkToTrvRouteIncidence.containsKey(r.getStartLinkId()))this.linkToTrvRouteIncidence.put(r.getStartLinkId(), new HashSet<>());
				if(!this.linkToTrvRouteIncidence.containsKey(r.getEndLinkId()))this.linkToTrvRouteIncidence.put(r.getEndLinkId(), new HashSet<>());
				this.linkToTrvRouteIncidence.get(r.getStartLinkId()).add(r);
				this.linkToTrvRouteIncidence.get(r.getEndLinkId()).add(r);
				r.getLinkIds().stream().forEach(l->{
					if(!this.linkToTrvRouteIncidence.containsKey(l))this.linkToTrvRouteIncidence.put(l, new HashSet<>());
					this.linkToTrvRouteIncidence.get(l).add(r);
				});
				this.trvDemand.put(r, demandFromRoute);
				
			}
		}
		
		for(Entry<String, Map<Id<TransitLink>, Tuple<Double, double[]>>> timeentry:this.transitDemandFromMetamodel.entrySet()) {
			for(Entry<Id<TransitLink>, Tuple<Double, double[]>> tdlEntry:timeentry.getValue().entrySet()) {
				TransitDirectLink tdl = (TransitDirectLink) this.trDirectLinksFromMetamodel.get(timeentry.getKey()).get(tdlEntry.getKey());
				Id<Link> startLink = tdl.getStartingLinkId();
				Id<Link> endLink = tdl.getEndingLinkId();
				NetworkRoute r = this.ts.getTransitLines().get(Id.create(tdl.getLineId(),TransitLine.class)).getRoutes().get(Id.create(tdl.getRouteId(),TransitRoute.class)).getRoute();
				if(!this.transitTravelTimeQuery.containsKey(r))this.transitTravelTimeQuery.put(r, new HashMap<>());
				if(!this.transitTravelTimeQuery.get(r).containsKey(timeentry.getKey()))this.transitTravelTimeQuery.get(r).put(timeentry.getKey(), new HashMap<>());
				this.transitTravelTimeQuery.get(r).get(timeentry.getKey()).put(new Tuple<>(startLink,endLink),tdlEntry.getValue());
			}
		}
		
		
		
		
	}
	
	public Map<NetworkRoute, Map<String, Map<Tuple<Id<Link>, Id<Link>>, Tuple<Double,double[]>>>> getTransitTravelTimeQuery() {
		return transitTravelTimeQuery;
	}
	
	public Map<String, NetworkRoute> getRoutes() {
		return routes;
	}

	public Map<String, NetworkRoute> getTrvRoutes() {
		return trvRoutes;
	}

	public Map<NetworkRoute, Map<String, Tuple<Double, double[]>>> getDemand() {
		return demand;
	}

	public Map<NetworkRoute, Map<String, Tuple<Double, double[]>>> getTrvDemand() {
		return trvDemand;
	}

	public Map<String, Tuple<Double, Double>> getDemandTimeBean() {
		return demandTimeBean;
	}

	public Map<Id<Link>, Set<NetworkRoute>> getLinkToRouteIncidence() {
		return linkToRouteIncidence;
	}

	public Map<Id<Link>, Set<NetworkRoute>> getLinkToTrvRouteIncidence() {
		return linkToTrvRouteIncidence;
	}
	
	public static String getTimeBean(Map<String,Tuple<Double,Double>>timeBean,double time) {
		String timeBeanId = null;
		
		for(Entry<String, Tuple<Double, Double>> e:timeBean.entrySet()) {
			if(time == 0)time=time+1;
			if(time>e.getValue().getFirst() && time<=e.getValue().getSecond())return e.getKey();
		}
		
		return timeBeanId;
	}
	
	public Map<String, Map<NetworkRoute, Double>> getCapacity(){
		return this.capacity;
	}
}
