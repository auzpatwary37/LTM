package simpleToy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.pt.utils.TransitScheduleValidator.ValidationResult;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import dynamicTransitRouter.fareCalculators.FareCalculator;
import dynamicTransitRouter.fareCalculators.ZonalFareCalculator;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementType;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class NetworkAndPopulation {
public static void main(String[] args) {
	Config config = ConfigUtils.createConfig();
	Scenario scn = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Network net = scn.getNetwork();
	NetworkFactory netFac = net.getFactory();
	Node O1 = netFac.createNode(Id.createNodeId("O1"), new Coord(0,2500));
	Node n1 = netFac.createNode(Id.createNodeId("1"), new Coord(150,2500));
	Node N = netFac.createNode(Id.createNodeId("N"), new Coord(5000,5000));
	Node P = netFac.createNode(Id.createNodeId("P"), new Coord(10000,2500));
	Node D = netFac.createNode(Id.createNodeId("D"), new Coord(20000,2500));
	Node n3 = netFac.createNode(Id.createNodeId("3"), new Coord(19700,2500));
	
	net.addNode(O1);
	net.addNode(D);
	net.addNode(n1);
	net.addNode(n3);
	net.addNode(N);
	net.addNode(P);
	
	Link O1_1 = netFac.createLink(Id.createLinkId("O1_1"), O1, n1);
	Link n3_D = netFac.createLink(Id.createLinkId("3_D"), n3, D);
	Link n1_P = netFac.createLink(Id.createLinkId("1_P"), n1, P);
	Link n1_N = netFac.createLink(Id.createLinkId("1_N"), n1, N);
	Link N_P = netFac.createLink(Id.createLinkId("N_P"), N, P);
	Link P_n3 = netFac.createLink(Id.createLinkId("P_3"), P, n3);
	addLinkDetails(O1_1);
	addLinkDetails(n3_D);
	addLinkDetails(n1_N);
	addLinkDetails(n1_P);
	addLinkDetails(P_n3);
	addLinkDetails(N_P);
	O1_1.setCapacity(5400);
	n3_D.setCapacity(5400);
	P_n3.setCapacity(4000);
	n1_N.setCapacity(2000);
	N_P.setCapacity(2000);
	n1_P.setCapacity(1800);
	n1_P.setFreespeed(50*1000/3600);
	
	net.addLink(O1_1);
	net.addLink(n3_D);
	net.addLink(n1_N);
	net.addLink(n1_P);
	net.addLink(P_n3);
	net.addLink(N_P);
	
	Map<String,Tuple<Double,Double>>timeBean = new HashMap<>();
	
	timeBean.put("7_7.30", new Tuple<>(7*3600.,7.5*3600.));
	timeBean.put("7.30_8", new Tuple<>(7.5*3600.,8*3600.));
	timeBean.put("8_8.30", new Tuple<>(8*3600.,8.5*3600.));
	timeBean.put("8.30_9", new Tuple<>(8.5*3600.,9*3600.));
	
	Measurements measurements = Measurements.createMeasurements(timeBean);
	
	Measurement mO1_1 = measurements.createAnadAddMeasurement(O1_1.getId().toString(), MeasurementType.linkVolume);
	List<Id<Link>> a = new ArrayList<>();
	a.add(O1_1.getId());
	mO1_1.setAttribute(Measurement.linkListAttributeName, a);
	Measurement mn3_D = measurements.createAnadAddMeasurement(n3_D.getId().toString(), MeasurementType.linkVolume);
	a = new ArrayList<>();
	a.add(n3_D.getId());
	mn3_D.setAttribute(Measurement.linkListAttributeName, a);
	Measurement mn1_N = measurements.createAnadAddMeasurement(n1_N.getId().toString(), MeasurementType.linkVolume);
	a = new ArrayList<>();
	a.add(n1_N.getId());
	mn1_N.setAttribute(Measurement.linkListAttributeName, a);
	Measurement mn1_P = measurements.createAnadAddMeasurement(n1_P.getId().toString(), MeasurementType.linkVolume);
	a = new ArrayList<>();
	a.add(n1_P.getId());
	mn1_P.setAttribute(Measurement.linkListAttributeName,a);
	Measurement mP_n3 = measurements.createAnadAddMeasurement(P_n3.getId().toString(), MeasurementType.linkVolume);
	a = new ArrayList<>();
	a.add(P_n3.getId());
	mP_n3.setAttribute(Measurement.linkListAttributeName, a);
	Measurement mN_P = measurements.createAnadAddMeasurement(N_P.getId().toString(), MeasurementType.linkVolume);
	a = new ArrayList<>();
	a.add(N_P.getId());
	mN_P.setAttribute(Measurement.linkListAttributeName, a);
	
	measurements.getMeasurements().values().forEach(m->{
		measurements.getTimeBean().keySet().forEach(t->{
				m.putVolume(t, 0);
				m.putSD(t, 0.);
			});
		});
	
	TransitSchedule ts = scn.getTransitSchedule();
	Vehicles tv = scn.getTransitVehicles();
	TransitScheduleFactory tsf = ts.getFactory();
	VehiclesFactory tvf = tv.getFactory();
	VehicleType vt = tvf.createVehicleType(Id.create("bus", VehicleType.class));
	vt.setPcuEquivalents(3);
	//vt.setNetworkMode("bus");
	vt.getCapacity().setSeats(40);
	vt.getCapacity().setStandingRoom(10);
	vt.setLength(3);
	tv.addVehicleType(vt);
	
	Vehicles vehicles = scn.getVehicles();
	VehiclesFactory vf = vehicles.getFactory();
	VehicleType vt_car = tvf.createVehicleType(Id.create("car", VehicleType.class));
	vt_car.setPcuEquivalents(1);
	//vt_car.setNetworkMode("car");
	vt_car.getCapacity().setSeats(4);
	vt_car.getCapacity().setStandingRoom(0);
	vehicles.addVehicleType(vt_car);
	//public transit 
	
	TransitStopFacility bs1 = tsf.createTransitStopFacility(Id.create("O1_1", TransitStopFacility.class), O1_1.getFromNode().getCoord(), false);
	bs1.setLinkId(O1_1.getId());
	TransitStopFacility bs2 = tsf.createTransitStopFacility(Id.create("3_D", TransitStopFacility.class), n3_D.getToNode().getCoord(), false);
	bs2.setLinkId(n3_D.getId());
	
	ts.addStopFacility(bs1);
	ts.addStopFacility(bs2);
	
	TransitLine bus1 = tsf.createTransitLine(Id.create("bus1", TransitLine.class));
	TransitLine bus2 = tsf.createTransitLine(Id.create("bus2", TransitLine.class));

	NetworkRoute r1 = RouteUtils.createLinkNetworkRouteImpl(O1_1.getId(),List.of(n1_P.getId(),P_n3.getId()), n3_D.getId()); 
	TransitRouteStop s1 = tsf.createTransitRouteStop(bs1, 0, 10);
	TransitRouteStop s2 = tsf.createTransitRouteStop(bs2, 610,620);
	
	TransitRoute bus1_r = tsf.createTransitRoute(Id.create("bus1_r", TransitRoute.class), r1, List.of(s1,s2), "bus");
	for(int i = 0;i<3600;i=i+300) {
		Departure d = tsf.createDeparture(Id.create("bus1_r"+i,Departure.class), 8*3600.+i);
		Vehicle v = tvf.createVehicle(Id.createVehicleId(d.getId().toString()), vt);
		tv.addVehicle(v);
		d.setVehicleId(v.getId());
		bus1_r.addDeparture(d);
	}
	bus1.addRoute(bus1_r);
	bus1_r.setTransportMode("bus");
	ts.addTransitLine(bus1);
	
	NetworkRoute r2 = RouteUtils.createLinkNetworkRouteImpl(O1_1.getId(),List.of(n1_N.getId(),N_P.getId(),P_n3.getId()), n3_D.getId()); 
	s1 = tsf.createTransitRouteStop(bs1, 0, 10);
	s2 = tsf.createTransitRouteStop(bs2, 400,420);
	
	TransitRoute bus2_r = tsf.createTransitRoute(Id.create("bus2_r", TransitRoute.class), r2, List.of(s1,s2), "bus");
	bus2_r.setTransportMode("bus");
	for(int i = 0;i<3600;i=i+300) {
		Departure d = tsf.createDeparture(Id.create("bus2_r"+i,Departure.class), 8*3600.+i);
		Vehicle v = tvf.createVehicle(Id.createVehicleId(d.getId().toString()), vt);
		tv.addVehicle(v);
		d.setVehicleId(v.getId());
		bus2_r.addDeparture(d);
	}
	bus2.addRoute(bus2_r);
	ts.addTransitLine(bus2);
	
	Population pop = scn.getPopulation();
	PopulationFactory popFac = pop.getFactory();
	
	for(int i = 0;i<5000;i++) {
		Person p = popFac.createPerson(Id.createPersonId("p_"+i));
		Plan plan = popFac.createPlan();
		Activity a1 = popFac.createActivityFromCoord("h1", O1.getCoord());
		a1.setEndTime(8*3600+Math.random()*3600);
		Activity a2 = popFac.createActivityFromCoord("h2", D.getCoord());
		Leg l;
		if(Math.random()<0.5)l= popFac.createLeg("car");
		else l= popFac.createLeg("pt");
		plan.addActivity(a1);
		plan.addLeg(l);
		plan.addActivity(a2);
		p.addPlan(plan);
		pop.addPerson(p);
		Vehicle v = vf.createVehicle(Id.createVehicleId(p.getId().toString()), vt_car);
		Map<String,Id<Vehicle>> modeToV = new HashMap<>();
		modeToV.put("car", v.getId());
		VehicleUtils.insertVehicleIdsIntoAttributes(p, modeToV);
		vehicles.addVehicle(v);
	}
	ActivityParams param1 = new ActivityParams();
	param1.setActivityType("h1");
	//param1.setMinimalDuration(300);
	param1.setTypicalDuration(3600);
	param1.setScoringThisActivityAtAll(true);
	config.planCalcScore().addActivityParams(param1);
	
	ActivityParams param2 = new ActivityParams();
	param2.setActivityType("h2");
	//param2.setMinimalDuration(300);
	param2.setTypicalDuration(3600);
	param2.setScoringThisActivityAtAll(true);
	config.planCalcScore().addActivityParams(param2);
	
	config.transit().setUseTransit(true);
	//config.plansCalcRoute().setInsertingAccessEgressWalk(false);

	config.qsim().setUsePersonIdForMissingVehicleId(true);
	config.global().setCoordinateSystem("arbitrary");
	config.parallelEventHandling().setNumberOfThreads(4);
	config.controler().setWritePlansInterval(50);
	config.global().setNumberOfThreads(4);
	config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
	config.controler().setWriteEventsInterval(50);
	config.qsim().setStartTime(7*3600);
	config.qsim().setEndTime(15*3600);
	config.subtourModeChoice().setConsiderCarAvailability(false);
	config.subtourModeChoice().setProbaForRandomSingleTripMode(1);
	config.subtourModeChoice().setChainBasedModes(new String[] {"car"});
//	addStrategy(config, "SubtourModeChoice", null, 0.05D, 0 * 50);
//    addStrategy(config, "ReRoute", null, 0.1D, 0 * 50);
//    addStrategy(config, "ChangeExpBeta", null, 0.85D, 50);
//    config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.SpeedyALT);
//    config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.SwissRailRaptor);
	
	config.strategy().addParam("ModuleProbability_1", "0.8");
	config.strategy().addParam("Module_1", "ChangeExpBeta");
	config.strategy().addParam("ModuleProbability_2", "0.05");
	config.strategy().addParam("Module_2", "ReRoute");
	config.strategy().addParam("ModuleProbability_3", "0.1");
	config.strategy().addParam("Module_3", "ChangeTripMode");
    
    config.plans().setInputFile("population.xml");
    config.network().setInputFile("network.xml");
    config.transit().setTransitScheduleFile("transitSchedule.xml");
    config.transit().setVehiclesFile("transitVehicles.xml");
    config.vehicles().setVehiclesFile("vehicles.xml");
    config.qsim().setRemoveStuckVehicles(false);
    
    
    ValidationResult r = TransitScheduleValidator.validateAll(ts, net);
    System.out.println("transit is valid? "+ r.isValid());
	
	new ConfigWriter(config).write("simpleToy/data/config.xml");
	new PopulationWriter(pop).write("simpleToy/data/population.xml");
	new TransitScheduleWriter(ts).writeFile("simpleToy/data/transitSchedule.xml");
	new MatsimVehicleWriter(tv).writeFile("simpleToy/data/transitVehicles.xml");
	new NetworkWriter(net).write("simpleToy/data/network.xml");
	new MatsimVehicleWriter(vehicles).writeFile("simpleToy/data/vehicles.xml");
	new MeasurementsWriter(measurements).write("simpleToy/data/measurements.xml");
}

public static void addStrategy(Config config, String strategy, String subpopulationName, double weight, int disableAfter) {
    if (weight <= 0.0D || disableAfter < 0)
      throw new IllegalArgumentException("The parameters can't be less than or equal to 0!"); 
    StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
    strategySettings.setStrategyName(strategy);
    strategySettings.setSubpopulation(subpopulationName);
    strategySettings.setWeight(weight);
    if (disableAfter > 0)
      strategySettings.setDisableAfter(disableAfter); 
    config.strategy().addStrategySettings(strategySettings);
  }

static void addLinkDetails(Link l) {
	Set<String> modes = new HashSet<>();
	modes.add("car");
	modes.add("bus");
	modes.add("pt");
	l.setAllowedModes(modes);
	l.setCapacity(3600);
	l.setFreespeed(60*1000/3600);
	l.setNumberOfLanes(2);
	l.setLength(NetworkUtils.getEuclideanDistance(l.getFromNode().getCoord(), l.getToNode().getCoord()));
}

public static FareCalculator getBusFareCalculator(TransitSchedule ts) {
	ZonalFareCalculator fc = new ZonalFareCalculator(ts);
	fc.addLine(Id.create("bus1", TransitLine.class), false, 1);
	fc.addLine(Id.create("bus2", TransitLine.class), false, 2);
	fc.addRoute(Id.create("bus1", TransitLine.class), Id.create("bus1_r", TransitRoute.class), 1);
	fc.addRoute(Id.create("bus2", TransitLine.class), Id.create("bus2_r", TransitRoute.class), 2);
	fc.addSectionFare(Id.create("bus1", TransitLine.class), Id.create("bus1_r", TransitRoute.class), 
			Id.create("O1_1", TransitStopFacility.class), 1, Id.create("3_D", TransitStopFacility.class), 1, 1);
	fc.addSectionFare(Id.create("bus2", TransitLine.class), Id.create("bus2_r", TransitRoute.class), 
			Id.create("O1_1", TransitStopFacility.class), 1, Id.create("3_D", TransitStopFacility.class), 1, 2);
	return fc;
}

}
