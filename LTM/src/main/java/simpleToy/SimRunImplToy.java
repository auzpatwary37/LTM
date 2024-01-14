package simpleToy;

import java.util.LinkedHashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup.TransitRoutingAlgorithmType;

import smallScale.DynamicRoutingModuleV2;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.AnaModelCalibrationModule;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;

public class SimRunImplToy implements SimRun{

	private final int lastIteration;
	private Measurements outputMeasurements;
	private Scenario scn;
	
	public SimRunImplToy(int lastIter) {
		this.lastIteration=lastIter;
	}
	
	@Override
	public Measurements run(AnalyticalModel sue, Config config, LinkedHashMap<String, Double> params, boolean generateOd,
			String threadNo, MeasurementsStorage storage) {
		
		config.controler().setLastIteration(this.lastIteration);
		config.controler().setOutputDirectory("simpleToy/output"+threadNo);
		config.transit().setUseTransit(true);
		//config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		config.qsim().setUsePersonIdForMissingVehicleId(true);
		config.global().setCoordinateSystem("arbitrary");
		config.parallelEventHandling().setNumberOfThreads(3);
		config.controler().setWritePlansInterval(50);
		config.qsim().setRemoveStuckVehicles(false);
		config.global().setNumberOfThreads(3);
		config.controler().setLastIteration(150);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.controler().setWriteEventsInterval(50);
		config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.DijkstraBased);
		config.removeModule("swissRailRaptor");
		config.planCalcScore().getOrCreateModeParams("standing").setMarginalUtilityOfTraveling(-0.);
		config.planCalcScore().getOrCreateModeParams("metro").setMarginalUtilityOfTraveling(-0.);
		config.qsim().setStartTime(7.30*3600);
		config.qsim().setEndTime(10.30*3600);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DynamicRoutingModuleV2(NetworkAndPopulation.getBusFareCalculator(scenario.getTransitSchedule()),"simpleToy/data/Mtr_fare.csv",
				null, "simpleToy/data/lightRail.csv", "simpleToy/data/busFareGTFS.json", "simpleToy/data/ferryFareGTFS.json"));
		
		
		AnaModelCalibrationModule anaModule=new AnaModelCalibrationModule(storage, sue,"simpleToy/Calibration/",params,true,scenario.getTransitSchedule());
		this.outputMeasurements=anaModule.getOutputMeasurements();
		controler.addOverridingModule(anaModule);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		return this.outputMeasurements;
	}

	@Override
	public Measurements getOutputMeasurements() {
		
		return this.outputMeasurements;
	}
}
