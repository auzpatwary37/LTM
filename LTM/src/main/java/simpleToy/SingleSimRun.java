package simpleToy;

import java.util.LinkedHashMap;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.MeasurementsStorage;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SimRun;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsReader;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class SingleSimRun {
public static void main(String[] args) {
	Measurements calibrationMeasurements=new MeasurementsReader().readMeasurements("simpleToy/data/measurements.xml");
	Config config=ConfigUtils.loadConfig("simpleToy/data/config.xml");
	config.qsim().setRemoveStuckVehicles(false);
	
	MeasurementsStorage storage=new MeasurementsStorage(calibrationMeasurements);
	SimRun simRun=new SimRunImplToy(50);
	AnalyticalModel sue=new CNLSUEModel(calibrationMeasurements.getTimeBean());
	
	sue.setFileLoc("simpleToy/Calibration");
	Measurements m = simRun.run(sue, config, new LinkedHashMap<String,Double>(), true, "0", storage);
	new MeasurementsWriter(m).write("simpleToy/data/measurements1.xml");
}
}
