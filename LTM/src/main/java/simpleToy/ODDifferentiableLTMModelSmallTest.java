package simpleToy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import dta.ODDifferentiableLTMModel;
import dta.ODUtils;
import dynamicTransitRouter.fareCalculators.FareCalculator;
import dynamicTransitRouter.fareCalculators.MTRFareCalculator;
import optimizer.Adam;
import optimizer.Optimizer;
import smallScale.ConfigGenerator;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelODpair;
import ust.hk.praisehk.metamodelcalibration.calibrator.ObjectiveCalculator;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;
import utils.MapToArray;
import utils.VariableDetails;

public class ODDifferentiableLTMModelSmallTest {

	//public static void main(String[] args) {
	public static void main(String[] args) {
		Measurements originalMeasurements = new MeasurementsReader().readMeasurements("simpleToy/data/measurements1.xml");
		Network net = NetworkUtils.readNetwork("simpleToy/data/network.xml");
		
		Set<Id<Measurement>> mKeys = new HashSet<>(originalMeasurements.getMeasurements().keySet());
		mKeys.forEach(mKey->{
			Id<Link> link = ((List<Id<Link>>) originalMeasurements.getMeasurements().get(mKey).getAttribute(Measurement.linkListAttributeName)).get(0);
			if(!net.getLinks().get(link).getAllowedModes().contains("car")) {
				originalMeasurements.removeMeasurement(mKey);
			}
		});
		
		Config config = ConfigGenerator.generateToyConfig();
		//ConfigUtils.loadConfig("toyScenarioData/output_config.xml");
		config.transit().setTransitScheduleFile("simpleToy/data/transitSchedule.xml");
		config.transit().setVehiclesFile("simpleToy/data/transitVehicles.xml");
		
		config.plans().setInputFile("simpleToy/base/output_plans.xml.gz");
		config.network().setInputFile("simpleToy/data/network.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		//Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		
		
		
		Map<String,FareCalculator>fareCalculators = new HashMap<>();
//		try {
			fareCalculators.put("bus", NetworkAndPopulation.getBusFareCalculator(scenario.getTransitSchedule()));
			//fareCalculators.put("train", new MTRFareCalculator("toyScenarioData/Mtr_fare.csv",scenario.getTransitSchedule()));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		Network odNetwork=NetworkUtils.readNetwork("siouxfalls-2014/odNet.xml");
		
		odNetwork = null;
		
		ODDifferentiableLTMModel model = new ODDifferentiableLTMModel(originalMeasurements.getTimeBean(), config);
		model.generateRoutesAndOD(scenario.getPopulation(), scenario.getNetwork(), odNetwork, scenario.getTransitSchedule(), scenario, fareCalculators);
		Set<String> uniqueVars = new HashSet<>();
		int  kk = 0;
		int kmax = 7000;
		for(String timeId:originalMeasurements.getTimeBean().keySet()) {
			for(Entry<Id<AnalyticalModelODpair>, AnalyticalModelODpair> k:model.getOdPairs().getODpairset().entrySet()){
				uniqueVars.add(ODUtils.createODMultiplierVariableName(k.getKey(),k.getValue().getSubPopulation(), ODUtils.OriginMultiplierVariableName, timeId));
				uniqueVars.add(ODUtils.createODMultiplierVariableName(k.getKey(),k.getValue().getSubPopulation(), ODUtils.DestinationMultiplierVariableName, timeId));
				
			}
			
		}
		System.out.println(uniqueVars.size());
		Map<String,VariableDetails> Param = new HashMap<>();
		for(String k:uniqueVars)Param.put(k, new VariableDetails(k, new Tuple<Double,Double>(0.1,8.), Math.sqrt(1.0)));
		Optimizer adam = new Adam("odOptim",Param,0.008,0.9,.999,10e-6);
		//Optimizer gd = new GD("odOptim",Param,0.00005,1000);
		for(int counter = 0;counter<1;counter++) {
			//System.out.println("GB: " + (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024*1024));
			long t = System.currentTimeMillis();
			Measurements modelMeasurements = model.perFormSUE(Param, originalMeasurements);
			Map<String,Double> grad = ODUtils.calcODObjectiveGradient(originalMeasurements, modelMeasurements, model);
			writeMeasurementsComparison(originalMeasurements,modelMeasurements,counter,"seperateODMultiplier","all");
			Param = adam.takeStep(grad);
			//Param = gd.takeStep(grad);
			double objective = ObjectiveCalculator.calcObjective(originalMeasurements, modelMeasurements, ObjectiveCalculator.TypeMeasurementAndTimeSpecific);
			//model = null;
			//if(counter%10==0)performFireTest(config, scenario, odNetwork, fareCalculators, Param, singleTimeBean, grad, 1, objective, timeSplitMeasurements.get(timeBean.getKey()), "seperateODMultiplier", counter, 0.005);
			System.out.println("Finished iteration "+counter);
			System.out.println("Objective = "+objective);
			final Map<String,VariableDetails> p = new LinkedHashMap<String,VariableDetails>(Param);
			Map<String,Double> paramValues = Param.keySet().stream().collect(Collectors.toMap(k->k, k->p.get(k).getCurrentValue()));
			MapToArray<String> m2a = new MapToArray<String>("writerM2A",grad.keySet());
			double gradNorm = m2a.getRealVector(grad).getNorm();
			dumpData("seperateODMultiplier",counter,"all",objective,paramValues,grad,gradNorm);
			System.out.println("Used Memory in GB: " + (double) (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024*1024));
			System.out.println("Time Required for iteratio "+counter+" = "+(System.currentTimeMillis()-t)/1000+" seconds.");
			if(gradNorm<10)break;
			
		}
		
	}

	public static Map<String,Double> readParams(String fileLoc){
		Map<String,Double> param = new HashMap<>();
		try {
			BufferedReader bf  = new BufferedReader(new FileReader(new File(fileLoc)));
			bf.readLine();
			String line = null;
			while((line = bf.readLine())!=null) {
				String[] part = line.split(",");
				param.put(part[0], Double.parseDouble(part[1]));
			}
			bf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return param;
	}
	
	private static Map<String,Measurements> timeSplitMeasurements(Measurements m){
		Map<String,Measurements> mOuts=new HashMap<>();
		for(String timeId:m.getTimeBean().keySet()) {
			Map<String,Tuple<Double,Double>> singleBeanTimeBean=new HashMap<>();
			singleBeanTimeBean.put(timeId, m.getTimeBean().get(timeId));
			Measurements mt=Measurements.createMeasurements(singleBeanTimeBean);
			mOuts.put(timeId, mt);
			for(Entry<Id<Measurement>, Measurement> d:m.getMeasurements().entrySet()) {
				if(d.getValue().getVolumes().containsKey(timeId)) {
					mt.createAnadAddMeasurement(d.getKey().toString(), d.getValue().getMeasurementType());
					for(Entry<String, Object> attribue:d.getValue().getAttributes().entrySet()) {
						mt.getMeasurements().get(d.getKey()).setAttribute(attribue.getKey(), attribue.getValue());
					}
					mt.getMeasurements().get(d.getKey()).putVolume(timeId, d.getValue().getVolumes().get(timeId));
				}
			}
		}
		
		return mOuts;
	}
	
	public static void dumpData(String folderLoc, int counter, String timeBean, double objective, Map<String,Double> param,Map<String,Double> grad,double gradNorm) {
		File file = new File(folderLoc);
		if(!file.exists())file.mkdir();
		String paramAndGradFileName = folderLoc+"/gradAndParam_"+timeBean+"_"+counter+".csv";
		MapToArray<String> m2a = new MapToArray<String>("writerM2A",param.keySet());
		Map<String,double[]> mapToWrite = new HashMap<>();
		mapToWrite.put("Variables",m2a.getMatrix(param));
		mapToWrite.put("Gradient",m2a.getMatrix(grad));
		
		m2a.writeCSV(mapToWrite, paramAndGradFileName);
		String iterLogerFileName = folderLoc+"/iterLogger"+timeBean+".csv";
		try {
			FileWriter fw = new FileWriter(new File(iterLogerFileName),true);
			if(counter == 0)fw.append("Iterantion,timeId,Objective,GradNorm\n");
			fw.append(counter+","+timeBean+","+objective+","+gradNorm+"\n");
			fw.flush();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	 
	public static void writeMeasurementsComparison(Measurements realMeasurements, Measurements modelMeasurements, int counter, String fileLoc, String timeId) {
		fileLoc = fileLoc+"/measurementsComparison"+timeId+"_"+counter+".csv";
		try {
			FileWriter fw = new FileWriter(new File(fileLoc));
			fw.append("MeasurementId,tiemBeanId,realCount,modelCount,apa,geh\n");
			fw.flush();
			for(Entry<Id<Measurement>, Measurement> m:realMeasurements.getMeasurements().entrySet()) {
				for(Entry<String, Double> v:m.getValue().getVolumes().entrySet()) {
					Measurement mModel = modelMeasurements.getMeasurements().get(m.getKey());
					if(mModel!=null && mModel.getVolumes().containsKey(v.getKey())) {
						double apa = Math.abs(v.getValue()-mModel.getVolumes().get(v.getKey()))/v.getValue()*100;
						double geh = Math.sqrt(2*Math.pow(v.getValue()-mModel.getVolumes().get(v.getKey()),2)/(v.getValue()+mModel.getVolumes().get(v.getKey())));
						fw.append(m.getKey().toString()+","+v.getKey()+","+v.getValue()+","+mModel.getVolumes().get(v.getKey())+","+apa+","+geh+"\n");
						fw.flush();
					}
				}
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void performFireTest(Config config,Scenario scenario, Network odNetwork,Map<String,FareCalculator> fareCalculators, Map<String,VariableDetails> variables, Map<String,Tuple<Double,Double>> timeBean,
			Map<String,Double> currentGradient, int numberOfVariableToTest, double currentObj, Measurements originalMeasurements, String reportFileLoc, int counter,double c) {
		
		try {
		
			FileWriter fw = new FileWriter(new File(reportFileLoc+"/gradientFireTest"+counter+".csv"));
			fw.append("counter,gradKey,currentGrad,newGrad\n");//header
			//Arbitrarily pick up #numberOfVariableToTest number of variables for testing
			Random rnd = new Random();
			List<String> keyList = new ArrayList<>(currentGradient.keySet());
			Set<String> currentKeyList = new HashSet<>();
			for(int i=0;i<numberOfVariableToTest;i++) {
				boolean stop = false; 
				String key = null;
				while (stop) {
					key = keyList.get(rnd.nextInt(keyList.size()));
					if(!currentKeyList.contains(key) && currentGradient.get(key)!=0) {
						currentKeyList.add(key);
						stop = true;
					}
				}
				//start the test. We do forward difference.
				LinkedHashMap<String,Double> vars = new LinkedHashMap<>();
				ODDifferentiableLTMModel model = new ODDifferentiableLTMModel(timeBean, config);
				model.generateRoutesAndOD(scenario.getPopulation(), scenario.getNetwork(), odNetwork, scenario.getTransitSchedule(), scenario, fareCalculators);
				Measurements modelMeasurements = model.perFormSUE(variables, originalMeasurements);
				double newObj = ObjectiveCalculator.calcObjective(originalMeasurements, modelMeasurements, ObjectiveCalculator.TypeMeasurementAndTimeSpecific);
				double gradNew = 0.5*(newObj-currentObj)/c;
				fw.append(counter+","+key+","+currentGradient.get(key)+","+gradNew+"\n");
				fw.flush();
			}
			fw.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}