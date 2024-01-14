package dta;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import dynamicTransitRouter.fareCalculators.FareCalculator;
import ltmAlgorithm.LTM;
import transitFareAndHandler.FareLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelODpair;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelTransitRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.SUEModelOutput;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitDirectLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLODpairs;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SignalFlowReductionGenerator;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementType;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import utils.LTMLoadableDemandV2;
import utils.LTMUtils;
import utils.MapToArray;
import utils.VariableDetails;

public class ODDifferentiableLTMModel {
private static final Logger logger = Logger.getLogger(ODDifferentiableLTMModel.class);

private double consecutiveSUEErrorIncrease=0;
private LinkedHashMap<String,Double> AnalyticalModelInternalParams=new LinkedHashMap<>();
private LinkedHashMap<String,Double> Params=new LinkedHashMap<>();
private LinkedHashMap<String,Tuple<Double,Double>> AnalyticalModelParamsLimit=new LinkedHashMap<>();
private boolean intiializeGradient = true;


private LTM ltm = null;


private double alphaMSA=1.9;//parameter for decreasing MSA step size
private double gammaMSA=.1;//parameter for decreasing MSA step size

//other Parameters for the Calibration Process
private double tolerance= 1;

//user input

private int maxIter = 100;


private Map<String, Tuple<Double,Double>> timeBeans;

private MapToArray<VariableDetails> variables = null;

//MATSim Input
private Network network;
private TransitSchedule ts;
private Config config;
private Scenario scenario;


protected Map<String,FareCalculator> fareCalculator=new HashMap<>();

//Used Containers
private List<Double> beta = new ArrayList<>(); //This is related to weighted MSA of the SUE
private List<Double> error = new ArrayList<>();
private List<Double> error1 = new ArrayList<>();//This is related to weighted MSA of the SUE

//TimebeanId vs demands map
private Map<String,Map<Id<AnalyticalModelODpair>,Double>> Demand=new HashMap<>();//Holds ODpair based demand
private Map<String,HashMap<Id<AnalyticalModelODpair>,Double>> carDemand=new ConcurrentHashMap<>(); 
private CNLLTMODpairs odPairs;

private Map<String,Map<Id<TransitLink>,TransitLink>> transitLinks=new ConcurrentHashMap<>();
private Population lastPopulation;

private SUEModelOutput flow;



private Map<String,Map<String,Double>> suPopSpecificParam = new ConcurrentHashMap<>();
//Internal database for the utility, mode choice and utility


private Map<String,Map<Id<AnalyticalModelRoute>,Double>> routeProb = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelTransitRoute>,Double>> trRouteProb = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelRoute>,double[]>> routeProbGrad = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelTransitRoute>,double[]>> trRouteProbGrad = new HashMap<>();

private Map<String,Map<Id<AnalyticalModelODpair>,Double>> carProb = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelODpair>,double[]>> carProbGrad = new HashMap<>();

private Map<String,Map<Id<AnalyticalModelODpair>,Double>> trProb = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelODpair>,double[]>> trProbGrad = new HashMap<>();

private Map<String,Map<Id<AnalyticalModelRoute>,Double>> routeFlow = new HashMap<>();

private Map<String,Map<Id<AnalyticalModelTransitRoute>,Double>> trRouteFlow = new HashMap<>();

private LTMLoadableDemandV2 ltmDemand = null;


private double linkGradL1NormThreshold = 1000;
//The gradient containers
//time->id->varKey->grad
private Map<Id<AnalyticalModelODpair>,List<Id<AnalyticalModelRoute>>> routeODIncidence = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelODpair>,List<Id<AnalyticalModelTransitRoute>>>> trRouteODIncidence = new HashMap<>();
private Map<Id<Link>, List<Id<AnalyticalModelRoute>>> linkIncidenceMatrix = new HashMap<>();
private Map<String,Map<Id<TransitLink>, List<Id<AnalyticalModelTransitRoute>>>> trLinkIncidenceMatrix = new HashMap<>();
private Map<String,Map<String,List<Id<AnalyticalModelTransitRoute>>>> fareLinkincidenceMatrix = new HashMap<>();
private MapToArray<String> gradientKeys;


private Map<String,Map<Id<AnalyticalModelRoute>,double[]>> routeFlowGradient = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelTransitRoute>,double[]>> trRouteFlowGradient = new HashMap<>();

private Map<String,Map<String,double[]>> fareLinkGradient = new HashMap<>();


private Map<String,Map<Id<AnalyticalModelODpair>,double[]>>odParameterIncidence = new HashMap<>();
private boolean ifODParameterIncidence = true;
//This are needed for output generation 

private double[] gradMultiplier;
private boolean ifGradMultiply = false;
private double maxAbsGrad = .2;
private double minAbsGrad = 0.01;
private double maxAbsL1Norm = 1e150;
private double minAbsL1Norm = 1e-150;

private Map<Id<AnalyticalModelRoute>, AnalyticalModelRoute> routes = new HashMap<>();
private Map<String,Map<Id<AnalyticalModelTransitRoute>,CNLTransitRouteLTM>> trRoutes = new HashMap<>();

private Map<String,Map<Id<TransitLink>,TransitDirectLink>> trDirectLinks = new HashMap<>();


//All the parameters name
//They are kept public to make it easily accessible as they are final they can not be modified

private boolean emptyMeasurements;

private Measurements measurementsToUpdate;

private boolean calculateGradient = true;


public static final String BPRalphaName="BPRalpha";
public static final String BPRbetaName="BPRbeta";
public static final String LinkMiuName="LinkMiu";
public static final String ModeMiuName="ModeMiu";
public static final String TransferalphaName="Transferalpha";
public static final String TransferbetaName="Transferbeta";

private Map<String,Map<Id<AnalyticalModelODpair>,Double>>originalDemand = new HashMap<>();

public boolean isCalculateGradient() {
	return calculateGradient;
}

public void setCalculateGradient(boolean calculateGradient) {
	this.calculateGradient = calculateGradient;
}

private void defaultParameterInitiation(Config config){
	this.AnalyticalModelInternalParams.put(CNLSUEModel.LinkMiuName,0.1);
	this.AnalyticalModelInternalParams.put(CNLSUEModel.ModeMiuName, 0.1);
	this.AnalyticalModelInternalParams.put(CNLSUEModel.BPRalphaName, 0.15);
	this.AnalyticalModelInternalParams.put(CNLSUEModel.BPRbetaName, 4.);
	this.AnalyticalModelInternalParams.put(CNLSUEModel.TransferalphaName, 0.5);
	this.AnalyticalModelInternalParams.put(CNLSUEModel.TransferbetaName, 2.);
	this.loadAnalyticalModelInternalPamamsLimit();
	this.Params.put(CNLSUEModel.CapacityMultiplierName, 1.0);
}

protected void loadAnalyticalModelInternalPamamsLimit() {
	this.AnalyticalModelParamsLimit.put(CNLSUEModel.LinkMiuName, new Tuple<Double,Double>(0.0075,0.25));
	this.AnalyticalModelParamsLimit.put(CNLSUEModel.ModeMiuName, new Tuple<Double,Double>(0.01,0.5));
	this.AnalyticalModelParamsLimit.put(CNLSUEModel.BPRalphaName, new Tuple<Double,Double>(0.10,4.));
	this.AnalyticalModelParamsLimit.put(CNLSUEModel.BPRbetaName, new Tuple<Double,Double>(1.,15.));
	this.AnalyticalModelParamsLimit.put(CNLSUEModel.TransferalphaName, new Tuple<Double,Double>(0.25,5.));
	this.AnalyticalModelParamsLimit.put(CNLSUEModel.TransferbetaName, new Tuple<Double,Double>(0.75,4.));
	
}
double minTime = 24*3600;
double maxTime = 0;
public ODDifferentiableLTMModel(Map<String, Tuple<Double, Double>> timeBean,Config config) {
	this.timeBeans=timeBean;
	//this.defaultParameterInitiation(null);
	for(String timeId:this.timeBeans.keySet()) {
		if(this.minTime>this.timeBeans.get(timeId).getFirst())this.minTime = this.timeBeans.get(timeId).getFirst();
		if(this.maxTime<this.timeBeans.get(timeId).getSecond())this.maxTime = this.timeBeans.get(timeId).getSecond();
		this.transitLinks.put(timeId, new HashMap<Id<TransitLink>, TransitLink>());
		this.Demand.put(timeId, this.originalDemand.get(timeId));
		
		this.carDemand.put(timeId, new HashMap<Id<AnalyticalModelODpair>, Double>());
		
		this.carProb.put(timeId, new ConcurrentHashMap<>());
		this.carProbGrad.put(timeId, new ConcurrentHashMap<>());
		
		this.trProb.put(timeId, new ConcurrentHashMap<>());
		this.trProbGrad.put(timeId, new ConcurrentHashMap<>());
		
		this.routeFlow.put(timeId, new ConcurrentHashMap<>());
		this.routeFlowGradient.put(timeId, new ConcurrentHashMap<>());
		
		this.trRouteFlow.put(timeId, new ConcurrentHashMap<>());
		this.trRouteFlowGradient.put(timeId, new ConcurrentHashMap<>());
		
		this.routeProb.put(timeId, new ConcurrentHashMap<>());
		this.routeProbGrad.put(timeId, new ConcurrentHashMap<>());
		
		this.trRouteProb.put(timeId, new ConcurrentHashMap<>());
		this.trRouteProbGrad.put(timeId, new ConcurrentHashMap<>());
		
		this.transitLinks.put(timeId, new ConcurrentHashMap<>());
		this.trDirectLinks.put(timeId, new ConcurrentHashMap<>());
		
		this.trRoutes.put(timeId, new ConcurrentHashMap<>());
		this.odParameterIncidence.put(timeId, new ConcurrentHashMap<>());
		
		this.error = new ArrayList<>();
		this.beta = new ArrayList<>();
		this.error1 = new ArrayList<>();
		
	}
	if(config==null) config = ConfigUtils.createConfig();
	this.defaultParameterInitiation(config);
	this.config = config;
	logger.info("Model created.");
}



/**
 * This function identifies and counts the special cases of the od pair
 * @param timeBeanId
 * @param ODWithNoModeChoice
 * @param ODWithNoCarRouteChoice
 * @param ODWithNoPTRouteChoice
 * @param ODWithNoChoice
 * @param odpair
 */
private static void identifySpecialCases(String timeBeanId, Map<String,Integer> ODWithNoModeChoice, 
		Map<String,Integer> ODWithNoCarRouteChoice, Map<String,Integer> ODWithNoPTRouteChoice, 
		Map<String,Integer> ODWithNoChoice, AnalyticalModelODpair odpair) {
	//OD pairs with one
	if(odpair.getRoutes()!=null && odpair.getRoutes().size()<2) {
		ODWithNoCarRouteChoice.compute(timeBeanId,(k,v)->v==null?1:v+1);
	}
	//OD pairs with only one TR route (but can have some auto routes)
	if(odpair.getTrRoutes(timeBeanId)!=null && odpair.getTrRoutes(timeBeanId).size()<2) {
		ODWithNoPTRouteChoice.compute(timeBeanId,(k,v)->v==null?1:v+1);
	}
	//OD pairs with no mode choice
	if((odpair.getSubPopulation()!=null && odpair.getSubPopulation().contains("trip"))||
			(odpair.getRoutes()==null && odpair.getTrRoutes(timeBeanId)!=null && odpair.getTrRoutes(timeBeanId).size()!=0 )||
			(odpair.getRoutes()!=null && odpair.getRoutes().size()!=0 && odpair.getTrRoutes(timeBeanId)==null)) {
		ODWithNoModeChoice.compute(timeBeanId, (k,v)->v==null?1:v+1);
	}
	//OD pairs with only one tr route and no auto route; or only one auto route and no tr route
	if((odpair.getRoutes()==null && odpair.getTrRoutes(timeBeanId)!=null && odpair.getTrRoutes(timeBeanId).size()<2)||
			odpair.getTrRoutes(timeBeanId)==null && odpair.getRoutes()!=null &&odpair.getRoutes().size()<2) {
		ODWithNoChoice.compute(timeBeanId, (k,v)->v==null?1:v+1);
	}
}

public void generateRoutesAndOD(Population population,Network network,Network odNetwork, TransitSchedule transitSchedule,
		Scenario scenario,Map<String,FareCalculator> fareCalculator) {
	this.scenario = scenario;
	this.config = scenario.getConfig();
	this.network = network;
	
	//System.out.println("");
	this.odPairs = new CNLLTMODpairs(network,population,transitSchedule,scenario,this.timeBeans);
//	Config odConfig=ConfigUtils.createConfig();
//	odConfig.network().setInputFile("data/odNetwork.xml");

	//This is for creating ODpairs based on TPUSBs
	this.odPairs.generateODpairsetSubPop(odNetwork);//This network has priority over the constructor network. This allows to use a od pair specific network 
	this.odPairs.generateOdSpecificRouteKeys();
	this.odPairs.generateRouteandLinkIncidence(0.);
	
	Map<String,Integer> ODWithNoModeChoice = new HashMap<>();
	Map<String,Integer> ODWithNoCarRouteChoice = new HashMap<>();
	Map<String,Integer> ODWithNoPTRouteChoice = new HashMap<>();
	Map<String,Integer> ODWithNoChoice = new HashMap<>();
	
	SignalFlowReductionGenerator sg=new SignalFlowReductionGenerator(scenario);
	
	for(String s:this.timeBeans.keySet()) {
		this.transitLinks.put(s,this.odPairs.getTransitLinks(s));
	}
	
	this.fareCalculator = fareCalculator;
	this.ts = transitSchedule;
	//this.population.getPersons().values().forEach(p->p.getPlans().clear());
	for(String timeBeanId:this.timeBeans.keySet()) {
		this.consecutiveSUEErrorIncrease=0;
		this.Demand.put(timeBeanId, new HashMap<>(this.odPairs.getdemand(timeBeanId)));
		for(Id<AnalyticalModelODpair> odId:this.Demand.get(timeBeanId).keySet()) {
			double totalDemand=this.Demand.get(timeBeanId).get(odId);
			AnalyticalModelODpair odpair = this.odPairs.getODpairset().get(odId);
			this.carDemand.get(timeBeanId).put(odId, 0.5*totalDemand);
			this.carProb.get(timeBeanId).put(odId, 0.5);
			
			identifySpecialCases(timeBeanId, ODWithNoModeChoice, ODWithNoCarRouteChoice, 
					ODWithNoPTRouteChoice, ODWithNoChoice, odpair);

			if(odpair.getSubPopulation()!= null && odpair.getSubPopulation().contains("GV")) {
				this.carDemand.get(timeBeanId).put(odId, totalDemand); 
				this.carProb.get(timeBeanId).put(odId, 1.0);
				//ODWithNoModeChoice.compute(timeBeanId, (k,v)->v==null?1:v+1);
			}
			//System.out.println();
		}
		
	}
	
	this.ltm = new LTM(network,this.minTime, this.maxTime, sg);
	
	logger.info("Total OD Pairs = "+ this.odPairs.getODpairset().size());
	logger.info("OD with no choice = " + ODWithNoChoice.toString());
	logger.info("OD with no mode choice = " + ODWithNoModeChoice.toString());
	logger.info("OD with no auto route choice = " + ODWithNoCarRouteChoice.toString());
	logger.info("OD with no PT route choice = "+ ODWithNoPTRouteChoice.toString());
	
	int agentTrip=0;
	int matsimTrip=0;
	int agentDemand=0;
	for(AnalyticalModelODpair odPair:this.odPairs.getODpairset().values()) {
		agentTrip+=odPair.getAgentCounter();
		for(String s:odPair.getTimeBean().keySet()) {
			agentDemand+=odPair.getDemand().get(s);
		}
		
	}
	logger.info("Demand total = "+agentDemand);
	logger.info("Total Agent Trips = "+agentTrip);
	this.createLinkRouteIncidence();
}

private void createLinkRouteIncidence(){
	this.timeBeans.keySet().forEach(t->{
		this.trLinkIncidenceMatrix.put(t, new HashMap<>());
		this.fareLinkincidenceMatrix.put(t, new HashMap<>());
		this.odPairs.getTransitLinks(t).keySet().forEach(linkId->{
			this.trLinkIncidenceMatrix.get(t).put(linkId, new ArrayList<>());
		});
	});
	this.odPairs.getODpairset().entrySet().forEach(od->{
		od.getValue().getLinkIncidence().entrySet().forEach(linkInd->{
			List<Id<AnalyticalModelRoute>> routeIds = new ArrayList<>();
			linkInd.getValue().forEach(r->{
				routeIds.add(r.getRouteId());
				
			});
			if(this.linkIncidenceMatrix.containsKey(linkInd.getKey())) {
				this.linkIncidenceMatrix.get(linkInd.getKey()).addAll(routeIds);
			}else {
				this.linkIncidenceMatrix.put(linkInd.getKey(), routeIds);
			}
			List<Id<AnalyticalModelRoute>>routes = new ArrayList<>();
			od.getValue().getRoutes().forEach(r->routes.add(r.getRouteId()));
			this.routeODIncidence.put(od.getKey(), routes);
		});
		this.timeBeans.keySet().forEach(t->{
			Map<Id<TransitLink>,List<Id<AnalyticalModelTransitRoute>>> timeRoutes = this.trLinkIncidenceMatrix.get(t);
			Map<String, List<Id<AnalyticalModelTransitRoute>>> fareLinkTimeRoutes = this.fareLinkincidenceMatrix.get(t);
			od.getValue().getTrLinkIncidence().entrySet().forEach(linkInd->{				
				if(timeRoutes.containsKey(linkInd.getKey())) {
					List<Id<AnalyticalModelTransitRoute>> routeIds = new ArrayList<>();
					linkInd.getValue().forEach(r->{
						routeIds.add(r.getTrRouteId());
					});
					timeRoutes.get(linkInd.getKey()).addAll(routeIds);
				}
				
			});
			this.trRouteODIncidence.compute(t, (k,v)->v==null?v=new HashMap<>():v);
			this.trRouteODIncidence.get(t).put(od.getKey(), new ArrayList<>());
			if(od.getValue().getTrRoutes(t)!=null) {
			od.getValue().getTrRoutes(t).forEach(r->{
				r.getFareLinks().forEach(fl->{
					if(!fareLinkTimeRoutes.containsKey(fl.toString())){
						fareLinkTimeRoutes.put(fl.toString(), new ArrayList<>());	
					}
					fareLinkTimeRoutes.get(fl.toString()).add(r.getTrRouteId());
				});
				this.trRouteODIncidence.get(t).get(od.getKey()).add(r.getTrRouteId());
			});
			}
		
		});
	});
	
}
/**
 * This will not deal with any param containing sub population name or All
 * This hampers in time so currently is turned off
 * @param params
 * @param subPopulation
 * @param config
 * @return
 */
private LinkedHashMap<String,Double> handleBasicParams(LinkedHashMap<String,Double> oldparams, String subPopulation, Config config){
	LinkedHashMap<String,Double> params = new LinkedHashMap<>(oldparams);
	// Handle the original params first
//	for(String s:params.keySet()) {
//		if(subPopulation!=null && (s.contains(subPopulation)||s.contains("All"))) {
//			newParams.put(s.split(" ")[1],params.get(s));
//		}else if (subPopulation == null) {
//			newParams.put(s, params.get(s));
//		}else if(subPopulation!=null) {//this will allow the unknown param to enter
//			newParams.put(s, params.get(s));
//		}
//	}
	if(subPopulation == null) subPopulation = "";
	if(!this.suPopSpecificParam.containsKey(subPopulation)) {
		LinkedHashMap<String,Double> newParams = new LinkedHashMap<>();
		@SuppressWarnings("deprecation")
		ScoringParameters scParam = new ScoringParameters.Builder(config.planCalcScore(), config.planCalcScore().getScoringParameters(subPopulation), config.scenario()).build();
		
		newParams.compute(CNLSUEModel.MarginalUtilityofTravelCarName,(k,v)->v==null?scParam.modeParams.get("car").marginalUtilityOfTraveling_s*3600:v);
		newParams.compute(CNLSUEModel.MarginalUtilityofDistanceCarName, (k,v)->v==null?scParam.modeParams.get("car").marginalUtilityOfDistance_m:v);
		newParams.compute(CNLSUEModel.MarginalUtilityofMoneyName, (k,v)->v==null?scParam.marginalUtilityOfMoney:v);
		newParams.compute(CNLSUEModel.DistanceBasedMoneyCostCarName, (k,v)->v==null?scParam.modeParams.get("car").monetaryDistanceCostRate:v);
		newParams.compute(CNLSUEModel.MarginalUtilityofTravelptName, (k,v)->v==null?scParam.modeParams.get("pt").marginalUtilityOfTraveling_s*3600:v);
		newParams.compute(CNLSUEModel.MarginalUtilityOfDistancePtName, (k,v)->v==null?scParam.modeParams.get("pt").marginalUtilityOfDistance_m:v);
		newParams.compute(CNLSUEModel.MarginalUtilityofWaitingName, (k,v)->v==null?scParam.marginalUtilityOfWaitingPt_s*3600:v);
		newParams.compute(CNLSUEModel.UtilityOfLineSwitchName, (k,v)->v==null?scParam.utilityOfLineSwitch:v);
		newParams.compute(CNLSUEModel.MarginalUtilityOfWalkingName, (k,v)->v==null?scParam.modeParams.get("walk").marginalUtilityOfTraveling_s*3600:v);
		newParams.compute(CNLSUEModel.DistanceBasedMoneyCostWalkName, (k,v)->v==null?scParam.modeParams.get("walk").monetaryDistanceCostRate:v);
		newParams.compute(CNLSUEModel.ModeConstantCarName, (k,v)->v==null?scParam.modeParams.get("car").constant:v);
		newParams.compute(CNLSUEModel.ModeConstantPtname, (k,v)->v==null?scParam.modeParams.get("pt").constant:v);
		newParams.compute(CNLSUEModel.MarginalUtilityofPerformName, (k,v)->v==null?scParam.marginalUtilityOfPerforming_s*3600:v);
		
		newParams.compute(CNLSUEModel.CapacityMultiplierName, (k,v)->v==null?config.qsim().getFlowCapFactor():v);
		this.suPopSpecificParam.put(subPopulation, newParams);
		
	}
	this.suPopSpecificParam.get(subPopulation).entrySet().forEach(pp->{
		params.compute(pp.getKey(), (k,v)->v==null?pp.getValue():v);
	});
	return params;
}


public Measurements perFormSUE(Map<String,VariableDetails>variables,Measurements originalMeasurements) {
	this.variables = new MapToArray<VariableDetails>("variables",new ArrayList<>(variables.values()));
	
	LinkedHashMap<String,Double> params = new LinkedHashMap<>();
	this.variables.getKeySet().forEach(v->{
		params.put(v.getVariableName(),v.getCurrentValue());
	});
	
	this.gradientKeys = new MapToArray<String>("variableNames",params.keySet());
	
	this.resetCarDemand();
	this.Demand = ODUtils.applyODPairMultiplier(this.Demand, params,this.odPairs.getODpairset());
	this.consecutiveSUEErrorIncrease = 0;
	
	return this.performAssignment(params, this.AnalyticalModelInternalParams,originalMeasurements);
}

public Measurements perFormSUE(LinkedHashMap<String, VariableDetails> variables,Measurements originalMeasurements,boolean calcGrad) {
	this.variables = new MapToArray<VariableDetails>("variables",new ArrayList<>(variables.values()));
	LinkedHashMap<String,Double> params = new LinkedHashMap<>();
	this.variables.getKeySet().forEach(v->{
		params.put(v.toString(),v.getCurrentValue());
	});
	
	this.resetCarDemand();
	this.Demand = ODUtils.applyODPairMultiplier(this.Demand, params,this.odPairs.getODpairset());
	this.consecutiveSUEErrorIncrease = 0;
	for(String timeBeanId:this.timeBeans.keySet()) {
		
		//this.Demand.put(timeBeanId, new HashMap<>(this.odPairs.getdemand(timeBeanId)));
		for(Id<AnalyticalModelODpair> odId:this.Demand.get(timeBeanId).keySet()) {
			double totalDemand=this.Demand.get(timeBeanId).get(odId);
			AnalyticalModelODpair odpair = this.odPairs.getODpairset().get(odId);
			this.carDemand.get(timeBeanId).put(odId, 0.5*totalDemand);
			
			if(odpair.getSubPopulation().contains("GV")) {
				this.carDemand.get(timeBeanId).put(odId, totalDemand); 
			}
			//System.out.println();
		}
		
	}
	return this.performAssignment(params, this.AnalyticalModelInternalParams,originalMeasurements,calcGrad);
}

private Measurements performAssignment(LinkedHashMap<String,Double> params, LinkedHashMap<String,Double> anaParams, Measurements originalMeasurements) {
	Measurements measurementsToUpdate = null;
	
	SUEModelOutput flow = this.performAssignment(params, anaParams);
	this.flow = flow;
	if(originalMeasurements==null) {//for now we just add the fare link and link volume for a null measurements
		this.emptyMeasurements=true;
		measurementsToUpdate=Measurements.createMeasurements(this.timeBeans);
		//create and insert link volume measurement
		for(Entry<String, Map<Id<Link>, Double>> timeFlow:flow.getLinkVolume().entrySet()) {
			for(Entry<Id<Link>, Double> link:timeFlow.getValue().entrySet()) {
				Id<Measurement> mid = Id.create(link.getKey().toString(), Measurement.class);
				if(measurementsToUpdate.getMeasurements().containsKey(mid)) {
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}else {
					measurementsToUpdate.createAnadAddMeasurement(mid.toString(), MeasurementType.linkVolume);
					List<Id<Link>> links = new ArrayList<>();
					links.add(link.getKey());
					measurementsToUpdate.getMeasurements().get(mid).setAttribute(Measurement.linkListAttributeName, links);
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}
			}
		}
		
		for(Entry<String, Map<String, Double>> timeFlow:flow.getFareLinkVolume().entrySet()) {
			for(Entry<String, Double> link:timeFlow.getValue().entrySet()) {
				Id<Measurement> mid = Id.create(link.getKey().toString(), Measurement.class);
				if(measurementsToUpdate.getMeasurements().containsKey(mid)) {
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}else {
					measurementsToUpdate.createAnadAddMeasurement(mid.toString(), MeasurementType.fareLinkVolume);
					measurementsToUpdate.getMeasurements().get(mid).setAttribute(Measurement.FareLinkAttributeName, new FareLink(link.getKey()));
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}
			}
		}
		
	}else {
		
		measurementsToUpdate=originalMeasurements.clone();
		measurementsToUpdate.resetMeasurements();
		measurementsToUpdate.updateMeasurements(flow, null, null);
	}
	
	return measurementsToUpdate;
}

/**
 * For now this function, specially the calcGrad will not work. The gradient will be calculated irrespective of the input
 * @param params
 * @param anaParams
 * @param originalMeasurements
 * @param calcGrad (does not work)
 * @return
 */
private Measurements performAssignment(LinkedHashMap<String,Double> params, LinkedHashMap<String,Double> anaParams, Measurements originalMeasurements,boolean calcGrad) {
	Measurements measurementsToUpdate = null;
	
	SUEModelOutput flow = this.performAssignment(params, anaParams);
	this.flow = flow;
	if(originalMeasurements==null) {//for now we just add the fare link and link volume for a null measurements
		this.emptyMeasurements=true;
		measurementsToUpdate=Measurements.createMeasurements(this.timeBeans);
		//create and insert link volume measurement
		for(Entry<String, Map<Id<Link>, Double>> timeFlow:flow.getLinkVolume().entrySet()) {
			for(Entry<Id<Link>, Double> link:timeFlow.getValue().entrySet()) {
				Id<Measurement> mid = Id.create(link.getKey().toString(), Measurement.class);
				if(measurementsToUpdate.getMeasurements().containsKey(mid)) {
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}else {
					measurementsToUpdate.createAnadAddMeasurement(mid.toString(), MeasurementType.linkVolume);
					List<Id<Link>> links = new ArrayList<>();
					links.add(link.getKey());
					measurementsToUpdate.getMeasurements().get(mid).setAttribute(Measurement.linkListAttributeName, links);
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}
			}
		}
		
		for(Entry<String, Map<String, Double>> timeFlow:flow.getFareLinkVolume().entrySet()) {
			for(Entry<String, Double> link:timeFlow.getValue().entrySet()) {
				Id<Measurement> mid = Id.create(link.getKey().toString(), Measurement.class);
				if(measurementsToUpdate.getMeasurements().containsKey(mid)) {
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}else {
					measurementsToUpdate.createAnadAddMeasurement(mid.toString(), MeasurementType.fareLinkVolume);
					measurementsToUpdate.getMeasurements().get(mid).setAttribute(Measurement.FareLinkAttributeName, new FareLink(link.getKey()));
					measurementsToUpdate.getMeasurements().get(mid).putVolume(timeFlow.getKey(), link.getValue());
				}
			}
		}
	}else {
		measurementsToUpdate=originalMeasurements.clone();
		measurementsToUpdate.resetMeasurements();
		measurementsToUpdate.updateMeasurements(flow, null, null);
	}
	return measurementsToUpdate;
}



private SUEModelOutput performAssignment( LinkedHashMap<String,Double> params, LinkedHashMap<String,Double> anaParams) {
	SUEModelOutput flow = new SUEModelOutput(new HashMap<>(),new HashMap<>(),new HashMap<>(),new HashMap<>(),new HashMap<>());
	flow.setTrainCount(new HashMap<>());
	flow.setTrainTransfers(new HashMap<>());
	Map<String,Object> additionalDataContainer = new HashMap<>();
	
	//this.resetCarDemand();
	
	for(int counter = 0;counter<this.maxIter;counter++) {
		additionalDataContainer.put("variableKeys", this.gradientKeys);
		if(counter!=0) {
			this.ltm.performLTM(this.ltmDemand, variables);
			additionalDataContainer.put("car", this.ltm.getTimeBeanRouteTravelTime(3));
			additionalDataContainer.put("transit", ltm.getTimeBeanTransitTravelAndWaitingTime(3));
			
		}
	
		long time = System.currentTimeMillis();
		Tuple<Map<String, Map<Id<AnalyticalModelRoute>, Tuple<Double, double[]>>>, Map<String, Map<Id<AnalyticalModelTransitRoute>, Tuple<Double, double[]>>>> auxflow = 
				this.applyChoiceModel(params, anaParams, additionalDataContainer);
		System.out.println(System.currentTimeMillis()-time);
		
		
		Map<String,Map<Id<TransitLink>,Tuple<Double,double[]>>> trDirectLinkDemand = new HashMap<>();
		Map<String,Map<Id<TransitLink>,TransitDirectLink>> directLinks = new HashMap<>();
		//this.trDirectLinks = new HashMap<>();
		auxflow.getSecond().entrySet().forEach(e->{
			for(Entry<Id<AnalyticalModelTransitRoute>, Tuple<Double, double[]>> d:e.getValue().entrySet()) {
				Map<String, Set<TransitDirectLink>> tdlUsage = this.trRoutes.get(e.getKey()).get(d.getKey()).getDirectLinkUsage(this.network,this.timeBeans, e.getKey(), additionalDataContainer);
				for(Entry<String, Set<TransitDirectLink>> u: tdlUsage.entrySet()) {
					if(!trDirectLinkDemand.containsKey(u.getKey()))trDirectLinkDemand.put(u.getKey(), new HashMap<>());
					if(!directLinks.containsKey(u.getKey()))directLinks.put(u.getKey(), new HashMap<>());
					for(TransitDirectLink tdl:u.getValue()) {
						trDirectLinkDemand.get(u.getKey()).compute(tdl.getTrLinkId(), (k,v)->v==null?d.getValue():new Tuple<>(v.getFirst()+d.getValue().getFirst(),LTMUtils.sum(v.getSecond(), d.getValue().getSecond())));
						if(!directLinks.get(u.getKey()).containsKey(tdl.getTrLinkId()))directLinks.get(u.getKey()).put(tdl.getTrLinkId(), tdl);
					}
				}
			}
		});
		
		boolean ifStop = this.UpdateRouteFlow(auxflow,counter);
		
		if(ifStop)break;
		double scale = 1.;
		if(params.get(CNLSUEModel.CapacityMultiplierName)!=null)scale = params.get(CNLSUEModel.CapacityMultiplierName);
				
		this.ltmDemand = new LTMLoadableDemandV2(auxflow.getFirst(), this.routes, ts, this.scenario.getTransitVehicles(), trDirectLinkDemand, directLinks, timeBeans, variables, scale);
	
	}
	
	Map<String,Map<String,Double>> fareLinkVolume = new HashMap<>();
	Map<String,Map<String,double[]>> fareLinkVolumeGrad = new HashMap<>();
	this.trRouteFlow.entrySet().forEach(e->{
		e.getValue().entrySet().forEach(r->{
			
			Map<String, Set<FareLink>> fareLinkUsage = ((CNLTransitRouteLTM)this.trRoutes.get(e.getKey()).get(r.getKey())).getFareLinkUsage(this.network, timeBeans, e.getKey(), additionalDataContainer);
			for(Entry<String,Set<FareLink>> tfl:fareLinkUsage.entrySet()) {
				if(!fareLinkVolume.containsKey(tfl.getKey())) {
					fareLinkVolume.put(tfl.getKey(),new HashMap<>());
					fareLinkVolumeGrad.put(tfl.getKey(),new HashMap<>());
				}
				for(FareLink fl:tfl.getValue()) {
					fareLinkVolume.get(tfl.getKey()).compute(fl.toString(), (k,v)->v==null?r.getValue():v+r.getValue());
					fareLinkVolumeGrad.get(tfl.getKey()).compute(fl.toString(),(k,v)->v==null?this.trRouteFlowGradient.get(e.getKey()).get(r.getKey()):LTMUtils.sum(v,this.trRouteFlowGradient.get(e.getKey()).get(r.getKey())));
				}
			}
		});
	});
	
	
	flow.setLinkVolumeAndGradient(this.ltm.getLTMLinkFlow(timeBeans));
	flow.setFareLinkVolume(fareLinkVolume);
	flow.setFareLinkVolumeGrad(fareLinkVolumeGrad);
	
	return flow;
}




public Network getNetworks() {
	return this.network;
}

public Map<String, Map<Id<TransitLink>, TransitLink>> getTransitLinks() {
	return transitLinks;
}




public Map<String, Tuple<Double, Double>> getTimeBeans() {
	return timeBeans;
}

public Map<String, Map<Id<AnalyticalModelODpair>, Double>> getCarProb() {
	return carProb;
}

private double demandDistributed= 0;
private int rNum = 0;
private int trNum = 0;
/**
 * Applys the choice model to a specific od pair and generates route flows for car and transit
 * Mainly consists of three components, route choice, tr route choice and mode choice 
 * Finally will calculate the route flows and gradients and send it back.
 * @param ODpairId
 * @param timeBeanId
 * @param counter
 * @param oparams
 * @param anaParams
 * @param additionalDataContainer
 * @return
 */
protected Tuple<Map<String,Map<Id<AnalyticalModelRoute>,Tuple<Double,double[]>>>,Map<String,Map<Id<AnalyticalModelTransitRoute>,Tuple<Double,double[]>>>> applyChoiceModel(LinkedHashMap<String,Double> oparams, LinkedHashMap<String, Double> anaParams,Map<String,Object> additionalDataContainer){
	
	this.demandDistributed = 0;
	this.rNum = 0;
	this.trNum = 0;
	Map<String,Map<Id<AnalyticalModelRoute>,Tuple<Double,double[]>>> routeFlows=new HashMap<>();
	Map<String,Map<Id<AnalyticalModelTransitRoute>,Tuple<Double,double[]>>> trRouteFlows = new HashMap<>();
	for(String timeBeanId:this.timeBeans.keySet()) {
		routeFlows.put(timeBeanId, new ConcurrentHashMap<>());
		trRouteFlows.put(timeBeanId, new ConcurrentHashMap<>());
		this.odPairs.getODpairset().keySet().parallelStream().forEach(ODpairId->{
		double distributedDemand =0;
		
			//________car utility____________

			AnalyticalModelODpair odpair=this.odPairs.getODpairset().get(ODpairId);
			String subPopulation = odpair.getSubPopulation();


			List<AnalyticalModelRoute> routes=odpair.getRoutes();

			

			LinkedHashMap<String,Double> params = this.handleBasicParams(oparams, subPopulation, this.config);
			

			Map<Id<AnalyticalModelRoute>,Double> utility=new HashMap<>();
			Map<Id<AnalyticalModelRoute>,double[]> utilityGrad=new HashMap<>();
			
			double maxUtil = Double.NEGATIVE_INFINITY;
			double[] maxUtilGrad = null;
			double denominator = 0;
			double expectedMaxUtil = 0;
			double[] emuGrad = null;
			if(routes!=null && routes.size()!=0) {
			for(AnalyticalModelRoute rr:routes){//First loop to calculate utility
				if(!this.routes.containsKey(rr.getRouteId()))this.routes.put(rr.getRouteId(), rr);
				CNLLTMRoute r = (CNLLTMRoute)rr;
				double u=0;
				double[] du;
				Tuple<Double,double[]>util=r.calcRouteUtility(params, anaParams,this.network,this.timeBeans.get(timeBeanId),timeBeanId,additionalDataContainer);
				u = util.getFirst();
				du = util.getSecond();
				u=u+Math.log(odpair.getAutoPathSize().get(r.getRouteId()));//adding the path size term
				if(maxUtil<u) {
					maxUtil = u;
					maxUtilGrad = du;
				}
				utility.put(r.getRouteId(), u);
				utilityGrad.put(r.getRouteId(), du);
			}

			for(AnalyticalModelRoute r:routes){// second loop calculates expected maximum utility and the denominator of the logit model
				//(a seperate loop is required to subtract the maximum utility)
				denominator += Math.exp(utility.get(r.getRouteId())-maxUtil);
				expectedMaxUtil+=Math.exp((utility.get(r.getRouteId())-maxUtil));

			}
			expectedMaxUtil = maxUtil+1/anaParams.get(CNLSUEModel.LinkMiuName)*Math.log(expectedMaxUtil);
			

			double totalProb = 0;
			for(AnalyticalModelRoute r:routes){// now calculate the route choice probability 
				double u=utility.get(r.getRouteId());
				double prob = Math.exp((u-maxUtil))/denominator;
				if(emuGrad == null) {
					emuGrad = MatrixUtils.createRealVector(utilityGrad.get(r.getRouteId())).mapMultiply(prob).toArray();
				}else {
					emuGrad = MatrixUtils.createRealVector(utilityGrad.get(r.getRouteId())).mapMultiply(prob).add(emuGrad).toArray();
				}
				this.routeProb.get(timeBeanId).put(r.getRouteId(), prob);//we will save the route flow and its gradients for further analysis. 
				totalProb+=prob;
			}
			if(Math.abs(totalProb-1)>.1) {
				logger.debug("sum of probabilities is not 1. Debug!!!!");
			}

			for(AnalyticalModelRoute r:routes) {// now the gradient computations // requires another loop as we need the choice probabilities to calculate this.
				double[] du = utilityGrad.get(r.getRouteId());
				double[] probGrad = MatrixUtils.createRealVector(du).subtract(emuGrad).mapMultiply(this.routeProb.get(timeBeanId).get(r.getRouteId())).toArray();
				this.routeProbGrad.get(timeBeanId).put(r.getRouteId(), probGrad);
			}
			emuGrad = MatrixUtils.createRealVector(emuGrad).mapDivide(anaParams.get(CNLSUEModel.LinkMiuName)).getData();
			}
			//__________start tr utility______________

			List<AnalyticalModelTransitRoute> trRoutes=odpair.getTrRoutes(timeBeanId);

			Map<Id<AnalyticalModelTransitRoute>,Double> trUtility = new HashMap<>();
			Map<Id<AnalyticalModelTransitRoute>,double[]> trUtilityGrad = new HashMap<>();

			double maxTrUtil = Double.NEGATIVE_INFINITY;
			double[] maxTrUtilGrad = null;
			double denominatorTr = 0;
			double expectedMaxTrUtil = 0;
			double[] emuTrGrad = null;
			
			if(trRoutes!=null && trRoutes.size()!=0) {
	
				
				
				for(AnalyticalModelTransitRoute rr:trRoutes){//First loop to calculate utility
					
					CNLTransitRouteLTM r = (CNLTransitRouteLTM)rr;
					if(!this.trRoutes.get(timeBeanId).containsKey(rr.getTrRouteId())) {
						this.trRoutes.get(timeBeanId).put(rr.getTrRouteId(), r);
						this.transitLinks.get(timeBeanId).putAll(r.getTransitLinks());
						
					}
					
					double u=0;
					double[] du;
					Tuple<Double,double[]>util=r.calcRouteUtility(params, anaParams,this.network,this.transitLinks.get(timeBeanId),this.fareCalculator ,additionalDataContainer,this.timeBeans.get(timeBeanId),timeBeanId);
					u = util.getFirst();
					du = util.getSecond();
					u=u+Math.log(odpair.getTrPathSize().get(timeBeanId).get(r.getTrRouteId()));//adding the path size term
					if(maxTrUtil<u) {
						maxTrUtil = u;
						maxTrUtilGrad = du;
					}
					trUtility.put(r.getTrRouteId(), u);
					trUtilityGrad.put(r.getTrRouteId(), du);
				}
				for(AnalyticalModelTransitRoute r:trRoutes){// second loop calculates expected maximum utility and the denominator of the logit model
					//(a seperate loop is required to subtract the maximum utility)
					denominatorTr += Math.exp(trUtility.get(r.getTrRouteId())-maxTrUtil);
					expectedMaxTrUtil+=Math.exp((trUtility.get(r.getTrRouteId())-maxTrUtil));
	
				}
	
				expectedMaxTrUtil = maxUtil+1/anaParams.get(CNLSUEModel.LinkMiuName)*Math.log(expectedMaxTrUtil);
				double totaltrProb = 0;
				for(AnalyticalModelTransitRoute r:trRoutes){// now calculate the route choice probability 
					double u=trUtility.get(r.getTrRouteId());
	
					double prob = Math.exp((u-maxTrUtil))/denominatorTr;
					if(emuTrGrad == null) {
						emuTrGrad = MatrixUtils.createRealVector(trUtilityGrad.get(r.getTrRouteId())).mapMultiply(prob).toArray();
					}else {
						emuTrGrad = MatrixUtils.createRealVector(trUtilityGrad.get(r.getTrRouteId())).mapMultiply(prob).add(emuTrGrad).toArray();
					}
					this.trRouteProb.get(timeBeanId).put(r.getTrRouteId(), prob);//we will save the route flow and its gradients for further analysis. 
					totaltrProb+=prob;
				}
				if(Math.abs(totaltrProb-1)>.1) {
					logger.debug("sum of tr probabilities is not 1. Debug!!!!");
				}
				for(AnalyticalModelTransitRoute r:trRoutes) {// now the gradient computations // requires another loop as we need the choice probabilities to calculate this.
					double[] du = trUtilityGrad.get(r.getTrRouteId());
					double[] probGrad = MatrixUtils.createRealVector(du).subtract(emuTrGrad).mapMultiply(this.trRouteProb.get(timeBeanId).get(r.getTrRouteId())).toArray();
					this.trRouteProbGrad.get(timeBeanId).put(r.getTrRouteId(), probGrad);
				}
				emuTrGrad = MatrixUtils.createRealVector(emuTrGrad).mapDivide(anaParams.get(CNLSUEModel.LinkMiuName)).getData();
			}
			//____________________Mode Choice _______________________
			double modeMiu=anaParams.get(CNLSUEModel.ModeMiuName);
			double carProb = 1;
			double[] carProbGrad = new double[this.gradientKeys.getKeySet().size()];
			double trProb = 0;
			double[] trProbGrad = new double[this.gradientKeys.getKeySet().size()];
			
			double demand=this.Demand.get(timeBeanId).get(odpair.getODpairId());
			//if(demand!=0) { 

			if(expectedMaxUtil==0)expectedMaxUtil = Double.NEGATIVE_INFINITY;
			if(expectedMaxTrUtil==0)expectedMaxTrUtil = Double.NEGATIVE_INFINITY;
			if(expectedMaxUtil==Double.NEGATIVE_INFINITY||expectedMaxTrUtil==Double.POSITIVE_INFINITY||
					Math.exp(expectedMaxTrUtil*modeMiu)==Double.POSITIVE_INFINITY) {
				carProb = 0;
				trProb = 1;

			}else if(expectedMaxTrUtil==Double.NEGATIVE_INFINITY||expectedMaxUtil==Double.POSITIVE_INFINITY
					||Math.exp(expectedMaxUtil*modeMiu)==Double.POSITIVE_INFINITY) {
				carProb = 1;
				trProb = 0;
			}else {
				double maxModeUtil = Math.max(expectedMaxUtil, expectedMaxTrUtil);
				carProb = Math.exp((expectedMaxUtil-maxModeUtil)*modeMiu)/(Math.exp((expectedMaxUtil-maxModeUtil)*modeMiu)+Math.exp((expectedMaxTrUtil-maxModeUtil)*modeMiu));
				trProb = Math.exp((expectedMaxTrUtil-maxModeUtil)*modeMiu)/(Math.exp((expectedMaxUtil-maxModeUtil)*modeMiu)+Math.exp((expectedMaxTrUtil-maxModeUtil)*modeMiu));
				carProbGrad = MatrixUtils.createRealVector(emuGrad).subtract(MatrixUtils.createRealVector(emuGrad).mapMultiply(carProb).add(MatrixUtils.createRealVector(emuTrGrad).mapMultiply(trProb))).mapMultiply(carProb*modeMiu).toArray();
				trProbGrad = MatrixUtils.createRealVector(emuTrGrad).subtract(MatrixUtils.createRealVector(emuGrad).mapMultiply(carProb).add(MatrixUtils.createRealVector(emuTrGrad).mapMultiply(trProb))).mapMultiply(trProb*modeMiu).toArray();

			}
			if(Math.abs(carProb+trProb-1)>.1) {
			
				logger.debug("mode probability is not summing up to 1. Debug!!!!");
			}
			this.carProb.get(timeBeanId).put(ODpairId, carProb);
			this.carProbGrad.get(timeBeanId).put(ODpairId, carProbGrad);
			this.trProb.get(timeBeanId).put(ODpairId, trProb);
			this.trProbGrad.get(timeBeanId).put(ODpairId, trProbGrad);
			//}

			//_____________Demand gradient____________

			if(this.ifODParameterIncidence) {
				Map<String,Double> oi = new HashMap<>();
				double sum = 0;
				for(String var1:this.gradientKeys.getKeySet()) {
					double term2 = ODUtils.ifMatch_1_else_0(odpair.getODpairId(),this.odPairs.getODpairset().get(odpair.getODpairId()).getSubPopulation(), timeBeanId, var1);
					oi.put(var1, term2);
					sum+=term2;
				}
				if(sum == 0) {
					logger.debug("No variable found!!!");
				}
				this.odParameterIncidence.get(timeBeanId).put(odpair.getODpairId(), this.gradientKeys.getMatrix(oi));
			}
			RealVector odInc = MatrixUtils.createRealVector(this.odParameterIncidence.get(timeBeanId).get(odpair.getODpairId()));
			RealVector p = this.gradientKeys.getRealVector(oparams);

			RealVector demandGradient = odInc.ebeDivide(p).mapMultiply(demand);

			//____________flow__________
			
			if(routes!=null && routes.size()!=0) {
				for(AnalyticalModelRoute rr:routes){//First loop to calculate utility
					this.rNum++;
					double flow = demand*carProb*this.routeProb.get(timeBeanId).get(rr.getRouteId());
					distributedDemand+=flow;
					double[] flowGrad = demandGradient.mapMultiply(carProb*this.routeProb.get(timeBeanId).get(rr.getRouteId()))
							.add(MatrixUtils.createRealVector(carProbGrad).mapMultiply(demand*this.routeProb.get(timeBeanId).get(rr.getRouteId())))
							.add(MatrixUtils.createRealVector(this.routeProbGrad.get(timeBeanId).get(rr.getRouteId())).mapMultiply(demand*carProb)).toArray();
					Tuple<Double,double[]> d = routeFlows.get(timeBeanId).put(rr.getRouteId(), new Tuple<>(flow,flowGrad));
					if(d!=null) {
						logger.debug("route keys are not unique!!!");
					}
				}
			}
			
			//___________trFlow____________
			
			
			if(trRoutes!=null && trRoutes.size()!=0) {
			for(AnalyticalModelTransitRoute rr:trRoutes){//First loop to calculate utility
				this.trNum++;
				double flow = demand*trProb*this.trRouteProb.get(timeBeanId).get(rr.getTrRouteId());
				distributedDemand+=flow;
				double[] flowGrad = demandGradient.mapMultiply(trProb*this.trRouteProb.get(timeBeanId).get(rr.getTrRouteId()))
						.add(MatrixUtils.createRealVector(trProbGrad).mapMultiply(demand*this.trRouteProb.get(timeBeanId).get(rr.getTrRouteId())))
						.add(MatrixUtils.createRealVector(this.trRouteProbGrad.get(timeBeanId).get(rr.getTrRouteId())).mapMultiply(demand*trProb)).toArray();
				Tuple<Double,double[]> d = trRouteFlows.get(timeBeanId).put(rr.getTrRouteId(), new Tuple<>(flow,flowGrad));
				if(d!=null) {
					logger.debug("route keys are not unique!!!");
				}
			}
			}
			if(Math.abs(distributedDemand-demand)>1) {
				logger.debug("ditributed demand do not sum up to total demand of the od pair. Debug!!!");
			}
			this.demandDistributed+=distributedDemand;
		});
	}
	this.ifODParameterIncidence = false;
	double sum = 0;
	int rNum = 0;
	int trNum = 0;
	for(Map<Id<AnalyticalModelRoute>, Tuple<Double, double[]>> d:routeFlows.values()) {
		for(Tuple<Double,double[]>dd:d.values()) {
			sum+=dd.getFirst();
			rNum++;
		}
	}
	for(Map<Id<AnalyticalModelTransitRoute>, Tuple<Double, double[]>> d:trRouteFlows.values()) {
		for(Tuple<Double,double[]>dd:d.values()) {
			sum+=dd.getFirst();
			trNum++;
		}
	}
	Tuple<Map<String,Map<Id<AnalyticalModelRoute>,Tuple<Double,double[]>>>,Map<String,Map<Id<AnalyticalModelTransitRoute>,Tuple<Double,double[]>>>>  out = new Tuple<>(routeFlows,trRouteFlows);
	this.checkForNan(out);
	return out;
}

private boolean checkForNan(Tuple<Map<String,Map<Id<AnalyticalModelRoute>,Tuple<Double,double[]>>>,Map<String,Map<Id<AnalyticalModelTransitRoute>,Tuple<Double,double[]>>>> flows) {
	for(Entry<String, Map<Id<AnalyticalModelRoute>, Tuple<Double, double[]>>> e:flows.getFirst().entrySet()){
		for(Entry<Id<AnalyticalModelRoute>, Tuple<Double, double[]>> ee:e.getValue().entrySet()){
			boolean flowNan = !Double.isFinite(ee.getValue().getFirst());
			boolean gradNan = MatrixUtils.createRealVector(ee.getValue().getSecond()).isNaN() ||MatrixUtils.createRealVector(ee.getValue().getSecond()).isInfinite(); 
			if(flowNan==true) {
				return true;
			}
			if(gradNan == true) {
				return true;
			}
		}
	}
	
	for(Entry<String, Map<Id<AnalyticalModelTransitRoute>, Tuple<Double, double[]>>> e:flows.getSecond().entrySet()){
		for(Entry<Id<AnalyticalModelTransitRoute>, Tuple<Double, double[]>> ee:e.getValue().entrySet()){
			boolean flowNan = !Double.isFinite(ee.getValue().getFirst());
			boolean gradNan = MatrixUtils.createRealVector(ee.getValue().getSecond()).isNaN() ||MatrixUtils.createRealVector(ee.getValue().getSecond()).isInfinite(); 
			if(flowNan==true) {
				return true;
			}
			if(gradNan == true) {
				return true;
			}
		}
	}
	return false;
}

public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getRouteProb() {
	return routeProb;
}

public Map<String, Map<Id<AnalyticalModelTransitRoute>, Double>> getTrRouteProb() {
	return trRouteProb;
}




public Config getConfig() {
	return config;
}

public void setConfig(Config config) {
	this.config = config;
}

/**
 * 
 * @param auxflow tuple of car and transit route flows
 * @param counter
 * @return
 */

protected boolean UpdateRouteFlow(Tuple<Map<String, Map<Id<AnalyticalModelRoute>, Tuple<Double, double[]>>>, Map<String, Map<Id<AnalyticalModelTransitRoute>, Tuple<Double, double[]>>>> auxflow ,int counter){
	
	Map<String,Map<Id<AnalyticalModelRoute>,Double>> routeFlowUpdate = new HashMap<>();
	Map<String,Map<Id<AnalyticalModelRoute>,double[]>> routeFlowUpdateGrad = new HashMap<>();
	
	
	Map<String,Map<Id<AnalyticalModelTransitRoute>,Double>> trRouteFlowUpdate = new HashMap<>();
	Map<String,Map<Id<AnalyticalModelTransitRoute>,double[]>> trRouteFlowUpdateGrad = new HashMap<>();
	
	
	double linkAbove1=0;
	double squareSum=0;
	double sum=0;
	double error=0;
	
	double totalDemand = 0;
	double totalSqDemand = 0;
	for(String t:this.timeBeans.keySet()){
		routeFlowUpdate.put(t, new HashMap<>());
		routeFlowUpdateGrad.put(t, new HashMap<>());
		
		trRouteFlowUpdate.put(t, new HashMap<>());
		trRouteFlowUpdateGrad.put(t, new HashMap<>());
		
		

		for(Entry<Id<AnalyticalModelRoute>, Tuple<Double, double[]>> r:auxflow.getFirst().get(t).entrySet()){
				
				double currentVolume=0;
				double[] currentVolumeGrad = new double[this.variables.getKeySet().size()];
				
				if(counter!=0) {
					currentVolume = this.routeFlow.get(t).get(r.getKey());
					currentVolumeGrad = this.routeFlowGradient.get(t).get(r.getKey());
				}
				totalDemand+=r.getValue().getFirst();
				totalSqDemand+=Math.pow(r.getValue().getFirst(),2);
				double delta = r.getValue().getFirst()-currentVolume;
				routeFlowUpdate.get(t).put(r.getKey(), delta);
				routeFlowUpdateGrad.get(t).put(r.getKey(), LTMUtils.subtract(r.getValue().getSecond(), currentVolumeGrad));
				error=Math.pow(delta,2);
				
				if(error==Double.POSITIVE_INFINITY||error==Double.NEGATIVE_INFINITY) {
					throw new IllegalArgumentException("Error is infinity!!!");
				}
				if(r.getValue().getFirst() != 0 && error/(r.getValue().getFirst()+.00000001)*100>tolerance) {					
					sum+=1;
				}
				if(error>1) {
					linkAbove1++;
				}
			
			squareSum+=error;
			if(squareSum==Double.POSITIVE_INFINITY||squareSum==Double.NEGATIVE_INFINITY) {
				throw new IllegalArgumentException("error is infinity!!!");
			}
		}
		for(Entry<Id<AnalyticalModelTransitRoute>, Tuple<Double, double[]>> r:auxflow.getSecond().get(t).entrySet()){

			double currentVolume=0;
			double[] currentVolumeGrad = new double[this.variables.getKeySet().size()];
			
			if(counter!=0) {
				currentVolume = this.trRouteFlow.get(t).get(r.getKey());
				currentVolumeGrad = this.trRouteFlowGradient.get(t).get(r.getKey());
			}
			totalDemand+=r.getValue().getFirst();
			totalSqDemand+=Math.pow(r.getValue().getFirst(),2);
			
			double delta = r.getValue().getFirst()-currentVolume;
			trRouteFlowUpdate.get(t).put(r.getKey(), delta);
			trRouteFlowUpdateGrad.get(t).put(r.getKey(), LTMUtils.subtract(r.getValue().getSecond(), currentVolumeGrad));
			
			error=Math.pow(delta,2);
			
			
			if(r.getValue().getFirst() != 0 && error/(r.getValue().getFirst()+.00000001)*100>tolerance) {					
				sum+=1;
			}
			if(error>1) {
				linkAbove1++;
			}
			//}
			if(error==Double.NaN||error==Double.NEGATIVE_INFINITY) {
				throw new IllegalArgumentException("Stop!!! There is something wrong!!!");
			}
			squareSum+=error;
		}
	}
	System.out.println("total demand = "+totalDemand);
	System.out.println("total demand squared= "+totalSqDemand);
	
	if(squareSum==Double.NaN) {
		System.out.println("WAIT!!!!Problem!!!!!");
	}
	squareSum=Math.sqrt(squareSum);
	if(counter==0) {
		this.error = new ArrayList<>();
		this.error1 = new ArrayList<>();
	}
	this.error.add(squareSum);
	logger.info("ERROR amount at SUE iteration "+counter+" = "+squareSum);

	if(counter==0) {
		this.beta = new ArrayList<>();
		this.beta.add(1.);
	}else {
		if(this.error.get(counter)<this.error.get(counter-1)) {
			beta.add(beta.get(counter-1)+this.gammaMSA);
		}else {
			this.consecutiveSUEErrorIncrease+=1;
			beta.add(beta.get(counter-1)+this.alphaMSA);
		}
	}
	double alpha=1/beta.get(counter);
	
	for(Entry<String, Map<Id<AnalyticalModelRoute>, Double>> t:routeFlowUpdate.entrySet()) {
		for(Entry<Id<AnalyticalModelRoute>, Double> r:t.getValue().entrySet()) {
			this.routeFlow.get(t.getKey()).compute(r.getKey(), (k,v)->v==null?r.getValue()*1/alpha:v+r.getValue()*1/alpha);
			this.routeFlowGradient.get(t.getKey()).compute(r.getKey(), (k,v)->v==null?MatrixUtils.createRealVector(routeFlowUpdateGrad.get(t.getKey()).get(r.getKey())).mapMultiply(1/alpha).toArray():
				MatrixUtils.createRealVector(routeFlowUpdateGrad.get(t.getKey()).get(r.getKey())).mapMultiply(1/alpha).add(v).toArray());
		}
	}
	for(Entry<String, Map<Id<AnalyticalModelTransitRoute>, Double>> t:trRouteFlowUpdate.entrySet()) {
		for(Entry<Id<AnalyticalModelTransitRoute>, Double> r:t.getValue().entrySet()) {
			this.trRouteFlow.get(t.getKey()).compute(r.getKey(), (k,v)->v==null?r.getValue()*1/alpha:v+r.getValue()*1/alpha);
			this.trRouteFlowGradient.get(t.getKey()).compute(r.getKey(), (k,v)->v==null?MatrixUtils.createRealVector(trRouteFlowUpdateGrad.get(t.getKey()).get(r.getKey())).mapMultiply(1/alpha).toArray():
				MatrixUtils.createRealVector(trRouteFlowUpdateGrad.get(t.getKey()).get(r.getKey())).mapMultiply(1/alpha).add(v).toArray());
		}
	}
	
	if (squareSum<=1||sum==0||linkAbove1==0){
		return true;
	}else{
		return false;
	}
}





public Map<String, Map<Id<AnalyticalModelODpair>, Double>> getDemand() {
	return Demand;
}






public Scenario getScenario() {
	return scenario;
}

public void setScenario(Scenario scenario) {
	this.scenario = scenario;
}



private void resetCarDemand() {
	
	
	for(String timeId:this.timeBeans.keySet()) {
		this.Demand.put(timeId, this.odPairs.getdemand(timeId));
		this.carDemand.put(timeId, new HashMap<Id<AnalyticalModelODpair>, Double>());
		
		this.routeFlow.put(timeId, new ConcurrentHashMap<>());
		this.trRouteFlow.put(timeId, new ConcurrentHashMap<>());
		this.routeProb.put(timeId, new ConcurrentHashMap<>());
		this.trRouteProb.put(timeId, new ConcurrentHashMap<>());
		
		for(Entry<String, Map<Id<TransitLink>, TransitLink>> lSet:this.transitLinks.entrySet()) {
			lSet.getValue().values().forEach(l->{
				l.resetLink();
			});
		}
		this.transitLinks.entrySet().forEach(e->{
			e.getValue().entrySet().forEach(ee->{
				ee.getValue().resetLink();
			});
		});
		this.intiializeGradient = true;
		for(Id<AnalyticalModelODpair> o:this.Demand.get(timeId).keySet()) {
			double totalDemand=this.Demand.get(timeId).get(o);
			AnalyticalModelODpair odpair = this.odPairs.getODpairset().get(o);
			
			this.carDemand.get(timeId).put(o, 0.5*totalDemand);
			this.carProb.get(timeId).put(o, 0.5);
			
			if(odpair.getSubPopulation() != null && odpair.getSubPopulation().contains("GV")) {
				this.carDemand.get(timeId).put(o, totalDemand); 
				this.carProb.get(timeId).put(o, 1.0);
			}
			}
		
	//	System.out.println();

	}
}







//public Map<String, Map<String, Map<String, Double>>> getFareLinkGradient() {
public Map<String, Map<String, double[]>> getFareLinkGradient() {
	return fareLinkGradient;
}

public MapToArray<String> getGradientKeys() {
	return gradientKeys;
}

public CNLLTMODpairs getOdPairs() {
	return odPairs;
}

public void setOdPairs(CNLLTMODpairs odPairs) {
	this.odPairs = odPairs;
}

public MapToArray<VariableDetails> getVariables() {
	return this.variables;
}


//private void applyODBasedGradeintClipping(Map<Id<AnalyticalModelRoute>,double[]>routeGrad,Map<Id<AnalyticalModelTransitRoute>,double[]>trRouteGrad,Id<AnalyticalModelODpair> odId,String timeBeanId) {
//	RealVector delta = MatrixUtils.createRealVector(this.odParameterIncidence.get(timeBeanId).get(odId));
//	RealVector norm = MatrixUtils.createRealVector(new double[delta.toArray().length]);
//	for(double[] g:routeGrad.values())norm = norm.add(MatrixUtils.createRealVector(g).map(k->Math.abs(k)));
//	for(double[] g:trRouteGrad.values())norm = norm.add(MatrixUtils.createRealVector(g).map(k->Math.abs(k)));
//	RealVector demand  =  delta.mapAdd(2).mapMultiply(this.Demand.get(timeBeanId).get(odId));
//	RealVector multiplier = demand.ebeDivide(norm);
//	RealVector m = demand.subtract(norm).map(k->k>0?1:0).ebeMultiply(multiplier).map(k->k==0?1:k);
//	multiplier = multiplier.ebeDivide(m);
//	
//	for(Entry<Id<AnalyticalModelRoute>,double[]> g:routeGrad.entrySet())g.setValue(MatrixUtils.createRealVector(g.getValue()).ebeMultiply(multiplier).toArray());
//	for(Entry<Id<AnalyticalModelTransitRoute>,double[]> g:trRouteGrad.entrySet())g.setValue(MatrixUtils.createRealVector(g.getValue()).ebeMultiply(multiplier).toArray());
//}
//
//private void applyODBasedGradeintClipping(Set<Id<AnalyticalModelRoute>>routeIds,Set<Id<AnalyticalModelTransitRoute>>trRouteIds,Id<AnalyticalModelODpair> odId,String timeBeanId) {
//	RealVector delta = MatrixUtils.createRealVector(this.odParameterIncidence.get(timeBeanId).get(odId));
//	RealVector norm = MatrixUtils.createRealVector(new double[delta.toArray().length]);
//	for(Id<AnalyticalModelRoute> rId:routeIds)norm = norm.add(MatrixUtils.createRealVector(this.routeFlowGradient.get(timeBeanId).get(rId)).map(k->Math.abs(k)));
//	for(Id<AnalyticalModelTransitRoute> rId:trRouteIds)norm = norm.add(MatrixUtils.createRealVector(this.trRouteFlowGradient.get(timeBeanId).get(rId)).map(k->Math.abs(k)));
//	RealVector demand  =  delta.mapAdd(2).mapMultiply(this.Demand.get(timeBeanId).get(odId));
//	RealVector multiplier = demand.ebeDivide(norm);
//	RealVector m = demand.subtract(norm).map(k->k>0?1:0).ebeMultiply(multiplier).map(k->k==0?1:k);
//	multiplier = multiplier.ebeDivide(m);
//	for(Id<AnalyticalModelRoute> rId:routeIds)this.routeFlowGradient.get(timeBeanId).put(rId, MatrixUtils.createRealVector(this.routeFlowGradient.get(timeBeanId).get(rId)).ebeMultiply(multiplier).toArray());
//	for(Id<AnalyticalModelTransitRoute> rId:trRouteIds)this.trRouteFlowGradient.get(timeBeanId).put(rId, MatrixUtils.createRealVector(this.trRouteFlowGradient.get(timeBeanId).get(rId)).ebeMultiply(multiplier).toArray());
//}
//
//private void applyVerticalGradeintClipping(Set<Id<AnalyticalModelRoute>>routeIds,Set<Id<AnalyticalModelTransitRoute>>trRouteIds,Id<AnalyticalModelODpair> odId,String timeBeanId) {
//	RealVector delta = MatrixUtils.createRealVector(this.odParameterIncidence.get(timeBeanId).get(odId));
//	RealVector norm = MatrixUtils.createRealVector(new double[delta.toArray().length]);
//	for(Id<AnalyticalModelRoute> rId:routeIds)norm = norm.add(MatrixUtils.createRealVector(this.routeFlowGradient.get(timeBeanId).get(rId)).map(k->Math.abs(k)));
//	for(Id<AnalyticalModelTransitRoute> rId:trRouteIds)norm = norm.add(MatrixUtils.createRealVector(this.trRouteFlowGradient.get(timeBeanId).get(rId)).map(k->Math.abs(k)));
//	RealVector demand  =  delta.mapAdd(2).mapMultiply(this.Demand.get(timeBeanId).get(odId));
//	RealVector multiplier = demand.ebeDivide(norm);
//	RealVector m = demand.subtract(norm).map(k->k>0?1:0).ebeMultiply(multiplier).map(k->k==0?1:k);
//	multiplier = multiplier.ebeDivide(m);
//	for(Id<AnalyticalModelRoute> rId:routeIds)this.routeFlowGradient.get(timeBeanId).put(rId, MatrixUtils.createRealVector(this.routeFlowGradient.get(timeBeanId).get(rId)).ebeMultiply(multiplier).toArray());
//	for(Id<AnalyticalModelTransitRoute> rId:trRouteIds)this.trRouteFlowGradient.get(timeBeanId).put(rId, MatrixUtils.createRealVector(this.trRouteFlowGradient.get(timeBeanId).get(rId)).ebeMultiply(multiplier).toArray());
//}

public static double[] findAbsMax(double[]a,double[]b){
	double[] out = new double[a.length];
	 for(int i = 0;i<a.length;i++) {
		out[i] = Double.max(Math.abs(a[i]),Math.abs(b[i]));
	}
	return out; 
}

//private void scaleBackGradients() {
//	RealVector m = MatrixUtils.createRealVector(this.gradMultiplier);
//	this.routeFlowGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeMultiply(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.trRouteFlowGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeMultiply(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.linkGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeMultiply(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.trLinkGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeMultiply(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.linkTTGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeMultiply(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.trLinkTTGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeMultiply(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.fareLinkGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeMultiply(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//}
//
//private void scaleGradients() {
//	RealVector m = MatrixUtils.createRealVector(this.gradMultiplier);
//	this.routeFlowGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeDivide(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.trRouteFlowGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeDivide(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.linkGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeDivide(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.trLinkGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeDivide(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.linkTTGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeDivide(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.trLinkTTGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeDivide(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//	
//	this.fareLinkGradient.entrySet().forEach(t->{
//		t.getValue().entrySet().forEach(d->{
//			RealVector dd = MatrixUtils.createRealVector(d.getValue()).ebeDivide(m); 
//			if(dd.isInfinite()||dd.isNaN()) {
//				logger.debug("gradient infinite or nan!!!");
//			}
//			d.setValue(dd.toArray());
////			for(int i = 0;i<d.length;i++)
////				d[i] = d[i]*this.gradMultiplier[i];
//		});
//	});
//}
}
