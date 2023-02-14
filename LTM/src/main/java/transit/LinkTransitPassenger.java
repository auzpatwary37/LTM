package transit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import linkModels.LinkModel;
import utils.LTMUtils;
import utils.MapToArray;
import utils.TuplesOfThree;
import utils.VariableDetails;

public class LinkTransitPassenger {
	
	private int T;
	private double[] timeSteps;
	private MapToArray<VariableDetails> variables;
	private Map<Id<Link>,LinkTransitPassenger> linkTransitPassengerMap = new HashMap<>();// this is the map containing all the link transit passenger objects for all the 
	private LinkModel link;// the corresponding link model. 
	private MapToArray<NetworkRoute> routes;// routes going through this link
	private Map<NetworkRoute, double[]> capacity;//for each route and each time step
	private Map<NetworkRoute, double[][]> dcapacity;//for each route and each time step
	private Map<NetworkRoute, double[]> capacitydt;//for each route and each time step
	
	private Map<NetworkRoute,Double> vehicleCapacity = new HashMap<>();
	
	private BiMap<String,NetworkRoute> transitLine_routeToNetworkRouteMap = HashBiMap.create();
	
	private Map<NetworkRoute,Map<Id<Link>,Map<Id<Link>,double[]>>> onBoardPassengerX0 = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,Map<Id<Link>,double[][]>>> dOnBoardPassengerX0 = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,Map<Id<Link>,double[]>>> onBoardPassengerX0dt = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,Map<Id<Link>,double[]>>> onBoardPassengerXL = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,Map<Id<Link>,double[][]>>> dOnBoardPassengerXL = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,Map<Id<Link>,double[]>>> onBoardPassengerXLdt = new HashMap<>();
	
	private Map<NetworkRoute,Map<Id<Link>,double[]>> NrPassengerAlight = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,double[][]>> dNrPassengerAlight = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,double[]>> NrPassengerAlightdt = new HashMap<>();
	
	private Map<NetworkRoute,Map<Id<Link>,double[]>> NrPassengerBoard = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,double[][]>> dNrPassengerBoard = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,double[]>> NrPassengerBoarddt = new HashMap<>();
	
	private Map<NetworkRoute,Map<Id<Link>,double[]>> demandPassenger = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,double[][]>> dDemandPassenger = new HashMap<>();
	
	private Map<NetworkRoute,Map<Id<Link>,double[]>> queuedPassenger = new HashMap<>();
	private Map<NetworkRoute,Map<Id<Link>,double[][]>> dQueuedPassenger = new HashMap<>();
	
	
	public LinkTransitPassenger(LinkModel link, Map<Id<Link>,LinkTransitPassenger> ltpMap, TransitSchedule ts, Vehicles tsv) {
		this.link = link;
		this.timeSteps = link.getTimePoints();
		this.T = timeSteps.length;
		this.variables = link.getVariables();
	}
	
	
	public void addRoute(NetworkRoute r, Id<TransitLine> tlId, Id<TransitRoute> trId, double vehicleCapacity) {
		this.transitLine_routeToNetworkRouteMap.put(tlId.toString()+"___"+trId.toString(),r);
		this.vehicleCapacity.put(r, vehicleCapacity);
	}
	
	public void performInitialSetup() {
		this.routes = new MapToArray<NetworkRoute>("transit routes on link",this.transitLine_routeToNetworkRouteMap.values());
		for(NetworkRoute r:this.routes.getKeySet()) {
			this.capacity.put(r, new double[T]);
			this.dcapacity.put(r, new double[T][this.variables.getKeySet().size()]);
			this.capacitydt.put(r, new double[T]);
			
			this.demandPassenger.put(r,new HashMap<>());
			this.dDemandPassenger.put(r,new HashMap<>());
			
			this.queuedPassenger.put(r, new HashMap<>());
			this.dQueuedPassenger.put(r, new HashMap<>());
			
			this.NrPassengerBoard.put(r, new HashMap<>());
			this.dNrPassengerBoard.put(r, new HashMap<>());
			this.NrPassengerBoarddt.put(r, new HashMap<>());
			
			this.NrPassengerAlight.put(r, new HashMap<>());
			this.dNrPassengerAlight.put(r, new HashMap<>());
			this.NrPassengerAlightdt.put(r, new HashMap<>());
			
			this.onBoardPassengerX0.put(r, new HashMap<>());
			this.dOnBoardPassengerX0.put(r, new HashMap<>());
			this.onBoardPassengerX0dt.put(r, new HashMap<>());
			
			this.onBoardPassengerXL.put(r, new HashMap<>());
			this.onBoardPassengerXLdt.put(r, new HashMap<>());
			this.dOnBoardPassengerXL.put(r, new HashMap<>());
			
			int rInd = this.link.getRoutes().getIndex(r);
			
			for(int i = 0;i<this.timeSteps.length;i++) {
				this.capacity.get(r)[i] = (this.link.getNrxl()[rInd][i]-this.link.getNrx0()[rInd][i])*vehicleCapacity.get(r);
				this.capacitydt.get(r)[i] = (this.link.getNrxldt()[rInd][i]-this.link.getNrx0dt()[rInd][i])*vehicleCapacity.get(r);
				this.dcapacity.get(r)[i] = MatrixUtils.createRealVector(this.link.getdNrxl()[rInd][i]).subtract(this.link.getdNrx0()[rInd][i]).mapMultiply(vehicleCapacity.get(r)).toArray();
			}
		}
	}
	
	
	/**
	 * This should update the flow of passengers 
	 * @param timeId
	 */
	public void updateFlow(int timeId) {
		
		
		for(NetworkRoute r:this.routes.getKeySet()) {
			
			LinkTransitPassenger previousLink = this.linkTransitPassengerMap.get(findThePreviousLinkId(link.getLink().getId(),r));
			
			// First calculate the supply gradient
			double onBoard = 0;
			RealVector onBoardGrad = MatrixUtils.createRealVector(new double[variables.getKeySet().size()]);
			double onBoarddt = 0;
			for(Entry<Id<Link>, Map<Id<Link>, double[]>> fromTo:previousLink.getOnBoardPassengerXL().get(r).entrySet()) {
				for(Entry<Id<Link>, double[]> b:fromTo.getValue().entrySet()){
					if(!b.getKey().equals(this.link.getLink().getId())) {
						onBoard+=b.getValue()[timeId];
						onBoardGrad.add(previousLink.getdOnBoardPassengerXL().get(r).get(fromTo.getKey()).get(b.getKey())[timeId]);
						onBoarddt+=previousLink.getOnBoardPassengerXLdt().get(r).get(fromTo.getKey()).get(b.getKey())[timeId];
						
						if(!this.onBoardPassengerX0.get(r).containsKey(fromTo.getKey())) {
							this.onBoardPassengerX0.get(r).put(fromTo.getKey(), new HashMap<>());
							this.dOnBoardPassengerX0.get(r).put(fromTo.getKey(), new HashMap<>());
							this.onBoardPassengerX0dt.get(r).put(fromTo.getKey(), new HashMap<>());
						}
						
						if(!this.onBoardPassengerX0.get(r).get(fromTo.getKey()).containsKey(b.getKey())) {
							this.onBoardPassengerX0.get(r).get(fromTo.getKey()).put(b.getKey(), new double[this.timeSteps.length]);
							this.dOnBoardPassengerX0.get(r).get(fromTo.getKey()).put(b.getKey(), new double[this.timeSteps.length][this.variables.getKeySet().size()]);
							this.onBoardPassengerX0dt.get(r).get(fromTo.getKey()).put(b.getKey(), new double[this.timeSteps.length]);
						}
						
						this.onBoardPassengerX0.get(r).get(fromTo.getKey()).get(b.getKey())[timeId+1] =previousLink.getOnBoardPassengerXL().get(r).get(fromTo.getKey()).get(b.getKey())[timeId]; 
					
					}else {//alight
						if(!this.NrPassengerAlight.get(r).containsKey(fromTo.getKey())) {
							this.NrPassengerAlight.get(r).put(fromTo.getKey(), new double[this.timeSteps.length]);
							this.NrPassengerAlightdt.get(r).put(fromTo.getKey(), new double[this.timeSteps.length]);
							this.dNrPassengerAlight.get(r).put(fromTo.getKey(), new double[this.timeSteps.length][this.variables.getKeySet().size()]);
						}
						if(timeId==0) {
							this.NrPassengerAlight.get(r).get(fromTo.getKey())[timeId] = b.getValue()[timeId];
							this.dNrPassengerAlight.get(r).get(fromTo.getKey())[timeId] = previousLink.getdOnBoardPassengerXL().get(r).get(fromTo.getKey()).get(this.link.getLink().getId())[timeId];
							this.NrPassengerAlightdt.get(r).get(fromTo.getKey())[timeId] = previousLink.getOnBoardPassengerXLdt().get(r).get(fromTo.getKey()).get(this.link.getLink().getId())[timeId];
							
						}else {
							this.NrPassengerAlight.get(r).get(fromTo.getKey())[timeId] = b.getValue()[timeId]+this.NrPassengerAlight.get(r).get(fromTo.getKey())[timeId-1];
							this.dNrPassengerAlight.get(r).get(fromTo.getKey())[timeId] = LTMUtils.sum(previousLink.getdOnBoardPassengerXL().get(r).get(fromTo.getKey()).get(this.link.getLink().getId())[timeId],
									this.dNrPassengerAlight.get(r).get(fromTo.getKey())[timeId-1]);
							this.NrPassengerAlightdt.get(r).get(fromTo.getKey())[timeId] = previousLink.getOnBoardPassengerXLdt().get(r).get(fromTo.getKey()).get(this.link.getLink().getId())[timeId]+
									this.NrPassengerAlightdt.get(r).get(fromTo.getKey())[timeId-1];
							
						}
					}
				}
			}
			double emptySeat = capacity.get(r)[timeId]-onBoard;
			double emptySeatdt = capacitydt.get(r)[timeId]-onBoarddt;
			RealVector demptySeat = MatrixUtils.createRealVector(dcapacity.get(r)[timeId]).subtract(onBoardGrad);
			
			//Calculate the demand gradient
			
			
			Map<Id<Link>,TuplesOfThree<Double,Double,double[]>> demand = new HashMap<>();
		
			Set<Id<Link>> links = new HashSet<>();
			links.addAll(this.demandPassenger.get(r).keySet());
			links.addAll(this.queuedPassenger.get(r).keySet());
			
			for(Id<Link>link:links) {
				double dd = 0;
				RealVector ddd = MatrixUtils.createRealVector(new double[demptySeat.getData().length]);
				if(this.demandPassenger.get(r).get(link)!=null) {
					dd+=this.demandPassenger.get(r).get(link)[timeId];
					ddd.add(this.dDemandPassenger.get(r).get(link)[timeId]);
				}
				if(this.queuedPassenger.get(r).get(link)!=null) {
					dd+=this.queuedPassenger.get(r).get(link)[timeId];
					ddd.add(this.dQueuedPassenger.get(r).get(link)[timeId]);
				}
				demand.put(link, new TuplesOfThree<Double,Double,double[]>(dd,50.,ddd.getData()));// Here assuming the passenger flow rate is 50 per second
				
			}
			
			Map<Id<Link>, TuplesOfThree<Double, Double, double[]>> boarding = LTMUtils.proportionalDistribution(new TuplesOfThree<Double,Double,double[]>(emptySeat,emptySeatdt,demptySeat.getData()), demand);
			
			for(Id<Link>link:links) {
				TuplesOfThree<Double, Double, double[]> remainingDemand = demand.get(link).subtract(boarding.get(link));
				
				if(this.onBoardPassengerX0.get(r).containsKey(this.link.getLink().getId())) {
					this.onBoardPassengerX0.get(r).put(this.link.getLink().getId(), new HashMap<>());
					this.onBoardPassengerX0dt.get(r).put(this.link.getLink().getId(), new HashMap<>());
					this.dOnBoardPassengerX0.get(r).put(this.link.getLink().getId(), new HashMap<>());
				}
				
				
				if(!this.onBoardPassengerX0.get(r).get(this.link.getLink().getId()).containsKey(link)) {
					this.onBoardPassengerX0.get(r).get(this.link.getLink().getId()).put(link, new double[this.timeSteps.length]);
					this.dOnBoardPassengerX0.get(r).get(this.link.getLink().getId()).put(link, new double[this.timeSteps.length][this.variables.getKeySet().size()]);
					this.onBoardPassengerX0dt.get(r).get(this.link.getLink().getId()).put(link, new double[this.timeSteps.length]);
				}
				this.onBoardPassengerX0.get(r).get(this.link.getLink().getId()).get(link)[timeId+1] = boarding.get(link).getFirst();
				this.onBoardPassengerX0dt.get(r).get(this.link.getLink().getId()).get(link)[timeId+1] = boarding.get(link).getSecond();
				this.dOnBoardPassengerX0.get(r).get(this.link.getLink().getId()).get(link)[timeId+1] = boarding.get(link).getThird();
				
				if(!this.NrPassengerBoard.get(r).containsKey(link)) {
					this.NrPassengerBoard.get(r).put(link, new double[this.timeSteps.length]);
					this.NrPassengerBoarddt.get(r).put(link, new double[this.timeSteps.length]);
					this.dNrPassengerBoard.get(r).put(link, new double[this.timeSteps.length][this.variables.getKeySet().size()]);
				}
				if(timeId==0) {
					this.NrPassengerBoard.get(r).get(link)[timeId] = demand.get(link).getFirst();
					this.dNrPassengerBoard.get(r).get(link)[timeId] = demand.get(link).getThird();
					this.NrPassengerBoarddt.get(r).get(link)[timeId] = demand.get(link).getSecond();
					
				}else {
					this.NrPassengerBoard.get(r).get(link)[timeId] = demand.get(link).getFirst()+this.NrPassengerBoard.get(r).get(link)[timeId-1];
					this.dNrPassengerBoard.get(r).get(link)[timeId] = LTMUtils.sum(demand.get(link).getThird(),
							this.dNrPassengerBoard.get(r).get(link)[timeId-1]);
					this.NrPassengerBoarddt.get(r).get(link)[timeId] = demand.get(link).getSecond()+
							this.NrPassengerBoarddt.get(r).get(link)[timeId-1];
					
				}
				
				if(!this.queuedPassenger.get(r).containsKey(link)) {
					this.queuedPassenger.get(r).put(link, new double[this.timeSteps.length]);
					this.dQueuedPassenger.get(r).put(link, new double[this.timeSteps.length][this.variables.getKeySet().size()]);
				}
				this.queuedPassenger.get(r).get(link)[timeId+1] = remainingDemand.getFirst();
				this.dQueuedPassenger.get(r).get(link)[timeId+1] = remainingDemand.getThird();
				
				
			}
			
			
			// here we update the flow from the front of the link to the end of the link
			int rInd = this.link.getRoutes().getIndex(r);
			double flow = this.link.getNrxl()[rInd][timeId+1];
			int tBefore = timeId;
			int tAfter = timeId;
			for(int j = timeId;j>=0;j--) {
				if(flow<=this.link.getNrx0()[rInd][j])tAfter = j;
				if(flow>this.link.getNrx0()[rInd][j]) {
					tBefore = j;
					break;
				}
			}
			if(tBefore>tAfter)tBefore=tAfter;
			RealVector tBeforeGrad = null;
					
			
			double tBeforedt = 0;
			if(this.link.getNrx0dt()[rInd][tBefore] == 0) {
				System.out.println("Flow rate is zero!!!");
			}else {
				tBeforedt = this.link.getNrxldt()[rInd][timeId+1]*1/this.link.getNrx0dt()[rInd][tBefore];
				if(this.variables!=null)tBeforeGrad = MatrixUtils.createRealVector(this.link.getdNrxl()[rInd][timeId+1]).mapMultiply(1/this.link.getNrx0dt()[rInd][tBefore]);
			}
			RealVector tAfterGrad = null;
			
			double tAfterdt= 0;
			if(this.link.getNrx0dt()[rInd][tBefore] == 0) {
				System.out.println("Flow rate is zero!!!");
			}else {
			 tAfterdt = this.link.getNrxldt()[rInd][timeId+1]*1/this.link.getNrx0dt()[rInd][tAfter];
			 if(this.variables!=null)tAfterGrad = MatrixUtils.createRealVector(this.link.getdNrxl()[rInd][timeId+1]).mapMultiply(1/this.link.getNrx0dt()[rInd][tAfter]);
			}
			
			for(Id<Link>fromLink:this.onBoardPassengerX0.get(r).keySet()) {
				if(!this.onBoardPassengerXL.get(r).containsKey(fromLink)) {
					this.onBoardPassengerXL.get(r).put(fromLink, new HashMap<>());
					this.dOnBoardPassengerXL.get(r).put(fromLink, new HashMap<>());
					this.onBoardPassengerXLdt.get(r).put(fromLink, new HashMap<>());
				}
				for(Id<Link>toLink:this.onBoardPassengerX0.get(r).get(fromLink).keySet()) {
					if(!this.onBoardPassengerXL.get(r).get(fromLink).containsKey(toLink)) {
						this.onBoardPassengerXL.get(r).get(fromLink).put(toLink, new double[this.T]);
						this.dOnBoardPassengerXL.get(r).get(fromLink).put(toLink, new double[this.T][this.variables.getKeySet().size()]);
						this.onBoardPassengerXLdt.get(r).get(fromLink).put(toLink, new double[this.T]);
					}
					
					double PrxltBefore = this.onBoardPassengerX0.get(r).get(fromLink).get(toLink)[tBefore];
					RealVector dPrxltBefore = tBeforeGrad.ebeMultiply(this.dOnBoardPassengerX0.get(r).get(fromLink).get(toLink)[tBefore]);
					double PrxltBeforedt = tBeforedt*this.onBoardPassengerX0dt.get(r).get(fromLink).get(toLink)[tBefore];
					
					double PrxltAfter = this.onBoardPassengerX0.get(r).get(fromLink).get(toLink)[tAfter];
					RealVector dPrxltAfter = tAfterGrad.ebeMultiply(this.dOnBoardPassengerX0.get(r).get(fromLink).get(toLink)[tAfter]);
					double PrxltAfterdt = tAfterdt*this.onBoardPassengerX0dt.get(r).get(fromLink).get(toLink)[tAfter];
					
					double Prxl = 0;
					double Prxldt = 0;
					double[] dPrxl = null;
					
					if(tBefore==tAfter) {
						Prxl = PrxltBefore;
						if(this.variables!=null)dPrxl = dPrxltBefore.getData();
						Prxldt = PrxltBeforedt;
					}else {
						Tuple<Double,double[]> PrxlTuple = null;
						if(this.variables!=null) PrxlTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<Double,Double>(this.link.getNrx0()[rInd][tBefore],this.link.getNrx0()[rInd][tAfter]),
								new Tuple<Double,Double>(PrxltBefore,PrxltAfter),
								new Tuple<double[],double[]>(this.link.getdNrx0()[rInd][tBefore],this.link.getdNrx0()[rInd][tAfter]),
								new Tuple<double[],double[]>(dPrxltBefore.getData(),dPrxltAfter.getData()),
								flow,
								this.link.getdNrxl()[rInd][timeId+1]);
						Tuple<Double,double[]> PrxldtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<Double,Double>(this.link.getNrx0()[rInd][tBefore],this.link.getNrx0()[rInd][tAfter]),
								new Tuple<Double,Double>(PrxltBefore,PrxltAfter),
								new Tuple<>(new double[] {this.link.getNrx0dt()[rInd][tBefore]},new double[] {this.link.getNrx0dt()[rInd][tAfter]}),
								new Tuple<>(new double[] {PrxltBeforedt},new double[] {PrxltAfterdt}),
								flow,
								new double[] {this.link.getNrxldt()[rInd][timeId+1]});

						Prxl = PrxldtTuple.getFirst();
						if(this.variables!=null)dPrxl = PrxlTuple.getSecond();
						Prxldt = PrxldtTuple.getSecond()[0];
						
					}
					this.onBoardPassengerXL.get(r).get(fromLink).get(toLink)[timeId+1] = Prxl;
					this.dOnBoardPassengerXL.get(r).get(fromLink).get(toLink)[timeId+1] = dPrxl;
					this.onBoardPassengerXLdt.get(r).get(fromLink).get(toLink)[timeId+1] = Prxldt;
					
				}
				
			}
		}
		
		
		
	}
	
	private static Id<Link> findThePreviousLinkId(Id<Link> currentLinkId, NetworkRoute r) {
		if(currentLinkId.equals(r.getStartLinkId()))return null;
		else if(currentLinkId.equals(r.getEndLinkId()))return r.getLinkIds().get(r.getLinkIds().size()-1);
		else {
			for(int i = 0;i<r.getLinkIds().size();i++) {
				if(currentLinkId.equals(r.getLinkIds().get(i))) {
					if(i==0)return r.getStartLinkId();
					else return r.getLinkIds().get(i-1);
				}
			}
		}
		throw new IllegalArgumentException("The link id "+currentLinkId+" is not present in the route. Please check!!!");
	}

	public Map<Id<Link>, LinkTransitPassenger> getLinkTransitPassengerMap() {
		return linkTransitPassengerMap;
	}

	public LinkModel getLink() {
		return link;
	}

	public MapToArray<NetworkRoute> getRoutes() {
		return routes;
	}

	public Map<NetworkRoute, double[]> getCapacity() {
		return capacity;
	}

	public Map<NetworkRoute, double[][]> getDcapacity() {
		return dcapacity;
	}

	public Map<NetworkRoute, double[]> getCapacitydt() {
		return capacitydt;
	}

	public Map<NetworkRoute, Map<Id<Link>, Map<Id<Link>, double[]>>> getOnBoardPassengerX0() {
		return onBoardPassengerX0;
	}

	public Map<NetworkRoute, Map<Id<Link>, Map<Id<Link>, double[][]>>> getdOnBoardPassengerX0() {
		return dOnBoardPassengerX0;
	}

	public Map<NetworkRoute, Map<Id<Link>, Map<Id<Link>, double[]>>> getOnBoardPassengerX0dt() {
		return onBoardPassengerX0dt;
	}

	public Map<NetworkRoute, Map<Id<Link>, Map<Id<Link>, double[]>>> getOnBoardPassengerXL() {
		return onBoardPassengerXL;
	}

	public Map<NetworkRoute, Map<Id<Link>, Map<Id<Link>, double[][]>>> getdOnBoardPassengerXL() {
		return dOnBoardPassengerXL;
	}

	public Map<NetworkRoute, Map<Id<Link>, Map<Id<Link>, double[]>>> getOnBoardPassengerXLdt() {
		return onBoardPassengerXLdt;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[]>> getNrPassengerAlight() {
		return NrPassengerAlight;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[][]>> getdNrPassengerAlight() {
		return dNrPassengerAlight;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[]>> getNrPassengerAlightdt() {
		return NrPassengerAlightdt;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[]>> getNrPassengerBoard() {
		return NrPassengerBoard;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[][]>> getdNrPassengerBoard() {
		return dNrPassengerBoard;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[]>> getNrPassengerBoarddt() {
		return NrPassengerBoarddt;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[]>> getDemandPassenger() {
		return demandPassenger;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[][]>> getdDemandPassenger() {
		return dDemandPassenger;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[]>> getQueuedPassenger() {
		return queuedPassenger;
	}

	public Map<NetworkRoute, Map<Id<Link>, double[][]>> getdQueuedPassenger() {
		return dQueuedPassenger;
	}

}
