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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

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
	private Map<Id<NetworkRoute>,NetworkRoute> routes = new HashMap<>();
	private Map<Id<AnalyticalModelRoute>,Id<NetworkRoute>> routeIdMap = new HashMap<>();
	private Map<Id<TransitLine>,Map<Id<TransitRoute>,Tuple<Id<NetworkRoute>,NetworkRoute>>> trvRouteIdMap = new HashMap<>();
	private Map<Id<NetworkRoute>,NetworkRoute> trvRoutes = new HashMap<>();
	private Map<Id<NetworkRoute>, Map<String,Tuple<Double,double[]>>> demand;
	private Map<Id<NetworkRoute>, Map<String,Tuple<Double,double[]>>> trvDemand =new HashMap<>();
	private Map<String,Tuple<Double,Double>> demandTimeBean;
	private Map<Id<NetworkRoute>,Map<String,Map<Tuple<Id<Link>,Id<Link>>,Tuple<Double,double[]>>>> transitTravelTimeQuery = new HashMap<>();
	
	private Map<Id<Link>,Set<Id<NetworkRoute>>> linkToRouteIncidence = new HashMap<>();
	private Map<Id<Link>,Set<Id<NetworkRoute>>> linkToTrvRouteIncidence = new HashMap<>();
	private Map<Id<TransitLine>,Map<Id<TransitRoute>,Id<NetworkRoute>>> lineRouteMapping = new HashMap<>();
	private Map<String, Map<Id<NetworkRoute>,Double>> capacity = new HashMap<>();
	
	//Info saved if created from metamodel 
	
	private Map<String, Map<Id<AnalyticalModelRoute>, Tuple<Double, double[]>>> demandFromMetamodel;
	private Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> routesFromMetamodel;
	private TransitSchedule ts;
	private Vehicles trv;
	private Map<String,Map<Id<TransitLink>,Tuple<Double,double[]>>> transitDemandFromMetamodel;
	private Map<String,Map<Id<TransitLink>, TransitDirectLink>> trDirectLinksFromMetamodel = new HashMap<>();
	private MapToArray<VariableDetails> variables;
	private double scalingFactor;
	
	@Deprecated
	public LTMLoadableDemandV2(Map<Id<NetworkRoute>, Map<String,Tuple<Double,double[]>>> demand, Map<Id<NetworkRoute>, Map<String,Tuple<Double,double[]>>> trvDemand, Map<String, Tuple<Double,Double>> timeBean,
			Map<Id<NetworkRoute>,Map<String,Map<Tuple<Id<Link>,Id<Link>>,Tuple<Double,double[]>>>> transitSubRouteQuery,Map<Id<TransitLine>,Map<Id<TransitRoute>,Id<NetworkRoute>>>lineRouteMapping, 
			Map<String,Map<Id<NetworkRoute>,Double>>capacity, MapToArray<VariableDetails> variables, Map<Id<NetworkRoute>,NetworkRoute>routes, Map<Id<NetworkRoute>, NetworkRoute>trvRoutes) {
		this.demand = demand;
		this.trvDemand = trvDemand;
		this.demandTimeBean = timeBean;
		this.transitTravelTimeQuery = transitSubRouteQuery;
		this.variables = variables;
		this.routes = routes;
		this.trvRoutes = trvRoutes;
		routes.entrySet().forEach(rEntry->{
			Id<NetworkRoute> rId = rEntry.getKey();
			NetworkRoute r = rEntry.getValue();
			if(!this.linkToRouteIncidence.containsKey(r.getStartLinkId()))this.linkToRouteIncidence.put(r.getStartLinkId(), new HashSet<>());
			if(!this.linkToRouteIncidence.containsKey(r.getEndLinkId()))this.linkToRouteIncidence.put(r.getEndLinkId(), new HashSet<>());
			this.linkToRouteIncidence.get(r.getStartLinkId()).add(rId);
			this.linkToRouteIncidence.get(r.getEndLinkId()).add(rId);
			r.getLinkIds().stream().forEach(l->{
				if(!this.linkToRouteIncidence.containsKey(l))this.linkToRouteIncidence.put(l, new HashSet<>());
				this.linkToRouteIncidence.get(l).add(rId);
			});
		});
		
		trvRoutes.entrySet().forEach(rEntry->{
			Id<NetworkRoute> rId = rEntry.getKey();
			NetworkRoute r = rEntry.getValue();
			if(!this.linkToTrvRouteIncidence.containsKey(r.getStartLinkId()))this.linkToTrvRouteIncidence.put(r.getStartLinkId(), new HashSet<>());
			if(!this.linkToTrvRouteIncidence.containsKey(r.getEndLinkId()))this.linkToTrvRouteIncidence.put(r.getEndLinkId(), new HashSet<>());
			this.linkToTrvRouteIncidence.get(r.getStartLinkId()).add(rId);
			this.linkToTrvRouteIncidence.get(r.getEndLinkId()).add(rId);
			r.getLinkIds().stream().forEach(l->{
				if(!this.linkToTrvRouteIncidence.containsKey(l))this.linkToTrvRouteIncidence.put(l, new HashSet<>());
				this.linkToTrvRouteIncidence.get(l).add(rId);
			});
		});
		this.lineRouteMapping = lineRouteMapping;
		this.capacity = capacity;
	}
	
	public static Id<NetworkRoute> getNetRouteId(NetworkRoute r){
		String s = r.getStartLinkId().toString()+"__";
		for(Id<Link> l:r.getLinkIds())s = s+l.toString()+"__";
		s=s+r.getEndLinkId().toString();
		return Id.create(s,NetworkRoute.class);
	}
	
	public static Id<NetworkRoute> getTrvNetworkRoute(Id<TransitLine>lineId, Id<TransitRoute> routeId, NetworkRoute r) {
		String s = lineId.toString()+"__";
		s = s+routeId.toString()+"__";
		s = s+r.getStartLinkId().toString()+"__";
		for(Id<Link>l:r.getLinkIds())s =s+l.toString()+"__";
		s = s+r.getEndLinkId().toString();
		return Id.create(s,NetworkRoute.class);
	}
	
	
	
	public Map<Id<AnalyticalModelRoute>, Id<NetworkRoute>> getRouteIdMap() {
		return routeIdMap;
	}

	public Map<Id<TransitLine>, Map<Id<TransitRoute>, Tuple<Id<NetworkRoute>, NetworkRoute>>> getTrvRouteIdMap() {
		return trvRouteIdMap;
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
	public LTMLoadableDemandV2(Map<String,Map<Id<AnalyticalModelRoute>,Tuple<Double,double[]>>> demand,Map<Id<AnalyticalModelRoute>,AnalyticalModelRoute> routes, TransitSchedule ts, Vehicles trv,
			Map<String,Map<Id<TransitLink>,Tuple<Double,double[]>>> transitDemand, Map<String,Map<Id<TransitLink>, TransitDirectLink>> trLinks, Map<String,Tuple<Double,Double>>timeBean, 
			MapToArray<VariableDetails>variables, double transitScalingFactor) {
		
		this.demandFromMetamodel = demand;
		this.routesFromMetamodel = routes;
		this.ts = ts;
		this.trv = trv;
		this.transitDemandFromMetamodel = transitDemand;
		
		trLinks.entrySet().forEach(t->{
			this.trDirectLinksFromMetamodel.put(t.getKey(), new HashMap<>());
			t.getValue().entrySet().forEach(trl->{
				if(trl.getValue() instanceof TransitDirectLink) {
					this.trDirectLinksFromMetamodel.get(t.getKey()).put(trl.getKey(), (TransitDirectLink)trl.getValue());
				}
			});
		});
		
		this.demandTimeBean = timeBean;
		this.variables = variables;
		this.scalingFactor = transitScalingFactor;
		
		this.demand = new HashMap<>();
		
		for(Entry<String, Map<Id<AnalyticalModelRoute>, Tuple<Double, double[]>>> eTime:this.demandFromMetamodel.entrySet()) {
			for(Entry<Id<AnalyticalModelRoute>, Tuple<Double, double[]>> e:eTime.getValue().entrySet()) {
				NetworkRoute r = (NetworkRoute)this.routesFromMetamodel.get(e.getKey()).getRoute();
				Id<NetworkRoute> rId = getNetRouteId(r);
				if(!this.demand.containsKey(rId))this.demand.put(rId,new HashMap<>());
				this.demand.get(rId).put(eTime.getKey(),e.getValue());
			
				this.routes.put(rId, r);
				this.routeIdMap.put(e.getKey(), rId);
				Id<Link> startLink = r.getStartLinkId();
				Id<Link> endLink = r.getEndLinkId();
				if(!this.linkToRouteIncidence.containsKey(startLink))this.linkToRouteIncidence.put(startLink, new HashSet<>());
				this.linkToRouteIncidence.get(startLink).add(rId);
				if(!this.linkToRouteIncidence.containsKey(endLink))this.linkToRouteIncidence.put(endLink, new HashSet<>());
				this.linkToRouteIncidence.get(endLink).add(rId);
				for(Id<Link>l:r.getLinkIds()) {
					if(!this.linkToRouteIncidence.containsKey(l))this.linkToRouteIncidence.put(l, new HashSet<>());
					this.linkToRouteIncidence.get(l).add(rId);
			}
			}
		}
		
		for(Entry<Id<TransitLine>, TransitLine> tl:this.ts.getTransitLines().entrySet()) {
			this.lineRouteMapping.put(tl.getKey(), new HashMap<>());
			for(Entry<Id<TransitRoute>, TransitRoute> tr:tl.getValue().getRoutes().entrySet()) {
				NetworkRoute r = tr.getValue().getRoute();
				Id<NetworkRoute> rId = getTrvNetworkRoute(tl.getKey(), tr.getKey(), r);
				if(!this.trvRouteIdMap.containsKey(tl.getKey()))this.trvRouteIdMap.put(tl.getKey(), new HashMap<>());
				this.trvRouteIdMap.get(tl.getKey()).put(tr.getKey(), new Tuple<>(rId,r));
				this.lineRouteMapping.get(tl.getKey()).put(tr.getKey(), rId);
				this.trvRoutes.put(rId, tr.getValue().getRoute());
				Map<String,Tuple<Double,double[]>> demandFromRoute = new HashMap<>();
				Map<String,Double> space = new HashMap<>();
				Map<String,Integer> cnt = new HashMap<>();
				for(Entry<Id<Departure>, Departure> dp:tr.getValue().getDepartures().entrySet()) {
					Vehicle v = this.trv.getVehicles().get(dp.getValue().getVehicleId());
					String timeBeanId = getTimeBean(demandTimeBean, dp.getValue().getDepartureTime());
					if(timeBeanId!=null) {
						space.compute(timeBeanId, (k,vv)->vv==null?v.getType().getCapacity().getSeats()+v.getType().getCapacity().getStandingRoom():vv+v.getType().getCapacity().getSeats()+v.getType().getCapacity().getStandingRoom());
						cnt.compute(timeBeanId, (k,vv)->vv==null?1:vv+1);
						
						if(!demandFromRoute.containsKey(timeBeanId))demandFromRoute.put(timeBeanId, new Tuple<Double,double[]>(0.,new double[this.variables.getKeySet().size()]));
						demandFromRoute.put(timeBeanId, new Tuple<Double,double[]>(demandFromRoute.get(timeBeanId).getFirst()+v.getType().getPcuEquivalents()*this.scalingFactor,
								LTMUtils.sum(demandFromRoute.get(timeBeanId).getSecond(), new double[this.variables.getKeySet().size()])));
					}
				}
				
				
				for(Entry<String, Double> s:space.entrySet()) {
					if(!this.capacity.containsKey(s.getKey()))this.capacity.put(s.getKey(), new HashMap<>());
					if(cnt.get(s.getKey())!=0)this.capacity.get(s.getKey()).put(rId, s.getValue()/cnt.get(s.getKey()));
				}
				if(!this.linkToTrvRouteIncidence.containsKey(r.getStartLinkId()))this.linkToTrvRouteIncidence.put(r.getStartLinkId(), new HashSet<>());
				if(!this.linkToTrvRouteIncidence.containsKey(r.getEndLinkId()))this.linkToTrvRouteIncidence.put(r.getEndLinkId(), new HashSet<>());
				this.linkToTrvRouteIncidence.get(r.getStartLinkId()).add(rId);
				this.linkToTrvRouteIncidence.get(r.getEndLinkId()).add(rId);
				r.getLinkIds().stream().forEach(l->{
					if(!this.linkToTrvRouteIncidence.containsKey(l))this.linkToTrvRouteIncidence.put(l, new HashSet<>());
					this.linkToTrvRouteIncidence.get(l).add(rId);
				});
				for(String t:timeBean.keySet()) {
					if(!demandFromRoute.containsKey(t)) {
						double d = 0;
						double[] dD = new double[this.variables.getKeySet().size()];
						demandFromRoute.put(t, new Tuple<>(d,dD));
					}
				}
				this.trvDemand.put(rId, demandFromRoute);
				
			}
		}
		
		for(Entry<String, Map<Id<TransitLink>, Tuple<Double, double[]>>> timeentry:this.transitDemandFromMetamodel.entrySet()) {
			for(Entry<Id<TransitLink>, Tuple<Double, double[]>> tdlEntry:timeentry.getValue().entrySet()) {
				TransitDirectLink tdl = (TransitDirectLink) this.trDirectLinksFromMetamodel.get(timeentry.getKey()).get(tdlEntry.getKey());
				Id<Link> startLink = tdl.getStartingLinkId();
				Id<Link> endLink = tdl.getEndingLinkId();
				NetworkRoute r = this.ts.getTransitLines().get(Id.create(tdl.getLineId(),TransitLine.class)).getRoutes().get(Id.create(tdl.getRouteId(),TransitRoute.class)).getRoute();
				Id<NetworkRoute> rId = getTrvNetworkRoute(Id.create(tdl.getLineId(),TransitLine.class), Id.create(tdl.getRouteId(),TransitRoute.class), r);
				if(!this.transitTravelTimeQuery.containsKey(rId))this.transitTravelTimeQuery.put(rId, new HashMap<>());
				if(!this.transitTravelTimeQuery.get(rId).containsKey(timeentry.getKey()))this.transitTravelTimeQuery.get(rId).put(timeentry.getKey(), new HashMap<>());
				this.transitTravelTimeQuery.get(rId).get(timeentry.getKey()).put(new Tuple<>(startLink,endLink),tdlEntry.getValue());
			}
		}
		
		
		
		
	}
	
	public Map<Id<NetworkRoute>, Map<String, Map<Tuple<Id<Link>, Id<Link>>, Tuple<Double, double[]>>>> getTransitTravelTimeQuery() {
		return transitTravelTimeQuery;
	}
	
	public Map<Id<NetworkRoute>, NetworkRoute> getRoutes() {
		return routes;
	}

	public Map<Id<NetworkRoute>, NetworkRoute> getTrvRoutes() {
		return trvRoutes;
	}

	public Map<Id<NetworkRoute>, Map<String, Tuple<Double, double[]>>> getDemand() {
		return demand;
	}

	public Map<Id<NetworkRoute>, Map<String, Tuple<Double, double[]>>> getTrvDemand() {
		return trvDemand;
	}

	public Map<String, Tuple<Double, Double>> getDemandTimeBean() {
		return demandTimeBean;
	}

	public Map<Id<Link>, Set<Id<NetworkRoute>>> getLinkToRouteIncidence() {
		return linkToRouteIncidence;
	}

	public Map<Id<Link>, Set<Id<NetworkRoute>>> getLinkToTrvRouteIncidence() {
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
	
	public Map<String, Map<Id<NetworkRoute>, Double>> getCapacity(){
		return this.capacity;
	}
}
