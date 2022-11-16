package utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

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
	private Map<NetworkRoute,Map<String,Map<Tuple<Id<Link>,Id<Link>>,Double>>> transitTravelTimeQuery;
	
	private Map<Id<Link>,Set<NetworkRoute>> linkToRouteIncidence = new HashMap<>();
	private Map<Id<Link>,Set<NetworkRoute>> linkToTrvRouteIncidence = new HashMap<>();
	
	public LTMLoadableDemandV2(Map<NetworkRoute, Map<String,Tuple<Double,double[]>>> demand, Map<NetworkRoute, Map<String,Tuple<Double,double[]>>> trvDemand, Map<String, Tuple<Double,Double>> timeBean, Map<NetworkRoute,Map<String,Map<Tuple<Id<Link>,Id<Link>>,Double>>> transitSubRouteQuery) {
		this.demand = demand;
		this.trvDemand = trvDemand;
		this.demandTimeBean = timeBean;
		this.transitTravelTimeQuery = transitSubRouteQuery;
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
	}
	
	public Map<NetworkRoute, Map<String, Map<Tuple<Id<Link>, Id<Link>>, Double>>> getTransitTravelTimeQuery() {
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
	
}
