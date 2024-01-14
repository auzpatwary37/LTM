package smallScale;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

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
		config.controler().setOutputDirectory("toyScenario/output"+threadNo);
		config.transit().setUseTransit(true);
		//config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		config.qsim().setUsePersonIdForMissingVehicleId(true);
		config.global().setCoordinateSystem("arbitrary");
		config.parallelEventHandling().setNumberOfThreads(3);
		config.controler().setWritePlansInterval(50);
		config.qsim().setStartTime(0.);
		config.qsim().setEndTime(24*3600.0);
		config.global().setNumberOfThreads(3);
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.controler().setWriteEventsInterval(50);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		try {
			controler.addOverridingModule(new DynamicRoutingModuleV2(transitGenerator.createBusFareCalculator(scenario.getTransitSchedule()),"toyScenarioData/Mtr_fare.csv",
					null, "toyScenarioData/fare/light_rail_fares.csv", "toyScenarioData/fare/busFareGTFS.json", "toyScenarioData/fare/ferryFareGTFS.json"));
		} catch (IOException e) {
		
			e.printStackTrace();
		}
		
		
		AnaModelCalibrationModule anaModule=new AnaModelCalibrationModule(storage, sue,"src/main/resources/toyScenarioData/Calibration/",params,true,scenario.getTransitSchedule());
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
