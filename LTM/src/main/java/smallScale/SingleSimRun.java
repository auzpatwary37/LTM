package smallScale;

import java.util.LinkedHashMap;

import org.matsim.core.config.Config;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class SingleSimRun {
public static void main(String[] args) {
	Measurements calibrationMeasurements=new MeasurementsReader().readMeasurements("toyScenarioData/toyMeasurements.xml");
	Config config=ConfigGenerator.generateToyConfig();
	MeasurementsStorage storage=new MeasurementsStorage(calibrationMeasurements);
	SimRun simRun=new SimRunImplToy(150);
	AnalyticalModel sue=new CNLSUEModel(calibrationMeasurements.getTimeBean());
	
	sue.setFileLoc("toyScenario/");
	Measurements m = simRun.run(sue, config, new LinkedHashMap<String,Double>(), true, "0", storage);
	new MeasurementsWriter(m).write("toyScenario/measurements1.xml");
}
}
