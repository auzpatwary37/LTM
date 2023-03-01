package smallScale;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import createPTGTFS.FareCalculatorPTGTFS;
import dynamicTransitRouter.TransitRouterFareDynamicImpl;
import dynamicTransitRouter.costs.PTRecordHandler;
import dynamicTransitRouter.costs.StopStopTime;
import dynamicTransitRouter.costs.TransferWalkingTime;
import dynamicTransitRouter.costs.VehicleOccupancy;
import dynamicTransitRouter.costs.WaitingTime;
import dynamicTransitRouter.fareCalculators.FareCalculator;
import dynamicTransitRouter.fareCalculators.LRFareCalculator;
import dynamicTransitRouter.fareCalculators.MTRFareCalculator;
import dynamicTransitRouter.fareCalculators.UniformFareCalculator;
import dynamicTransitRouter.fareCalculators.ZonalFareXMLParserV2;
import dynamicTransitRouter.transfer.TransferDiscountCalculator;

public class DynamicRoutingModuleV2 extends AbstractModule {

	private FareCalculator busCalculator;
	private final String MTRFareFilePath;
	private final String LRFareFilePath;
	private final String transferDiscountJson;
	private final String busFareJsonPath;
	private final String ferryFareJsonPath;
	public static final double tramFare = 2.6;
	
	private boolean fixedValueTransferEnabled = false;
	private double busBusDiscount = 0.0;
	private double busTrainDiscount = 0.0;
	
	
	private boolean fareRateEnabled = false;
	private double fareRate = 1.;
	
	public static final String transferDiscountJsonName = "TransferDiscountJson";
	public static final String fixedDiscountName = "FixedDiscount";
	public static final String busBusDiscountName = "BBI";
	public static final String busTrainDiscountName = "TBI";
	
	public static final String fareRateEnabledName = "FareRateEnabled";
	public static final String fareRateName = "FareRate";
	
	public static final String selectedFareRateName = "SelectedFareRate";
	public static final String selectedStopsName = "SelectedStops";
	public static final String selectedFareRateName2 = "SelectedFareRate2";
	public static final String selectedStopsName2 = "SelectedStops2";
	
	private List<Id<TransitStopFacility>> selectedStops = null;
	private List<Id<TransitStopFacility>> selectedStops2 = null;
	private Double selectedFareRate = null;
	private Double selectedFareRate2 = null;
	
	/**
	 * An alternate constructor that do the bus fare loading itself.
	 * 
	 * @param busFareCalculator
	 * @param MTRFareFilePath The MTR fare, downloaded from data.gov.hk
	 * @param GMBDiscountFilePath The list of GMB route that have interchange discount with KMB
	 * @param LRFareFilePath The light rail fare, downloaded from data.gov.hk
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public DynamicRoutingModuleV2(TransitSchedule ts, String minibusFareFilePath, String MTRFareFilePath,
			String transferDiscountJson, String LRFareFilePath, String busFareJsonPath, String ferryFareJsonPath) 
					throws SAXException, IOException, ParserConfigurationException {
		
		ZonalFareXMLParserV2 busFareGetter = new ZonalFareXMLParserV2(ts);
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		saxParser.parse(minibusFareFilePath, busFareGetter);
		
		this.busCalculator = busFareGetter.get();
		this.MTRFareFilePath = MTRFareFilePath;
		this.transferDiscountJson = transferDiscountJson;
		this.LRFareFilePath = LRFareFilePath;
		this.busFareJsonPath = busFareJsonPath;
		this.ferryFareJsonPath = ferryFareJsonPath;
	}
	
	
	
	
	/**
	 * 
	 * @param minibusFareCalculator
	 * @param MTRFareFilePath The MTR fare, downloaded from data.gov.hk
	 * @param GMBDiscountFilePath The list of GMB route that have interchange discount with KMB
	 * @param LRFareFilePath The light rail fare, downloaded from data.gov.hk
	 */
	public DynamicRoutingModuleV2(FareCalculator minibusFareCalculator, String MTRFareFilePath,
			String transferDiscountJson, String LRFareFilePath, String busFareJsonPath, String ferryFareJsonPath) {
		this.busCalculator = minibusFareCalculator;
		this.MTRFareFilePath = MTRFareFilePath;
		this.transferDiscountJson = transferDiscountJson;
		this.LRFareFilePath = LRFareFilePath;
		this.busFareJsonPath = busFareJsonPath;
		this.ferryFareJsonPath = ferryFareJsonPath;
	}
	
	
	/**
	 * Call this function to enable the fix valued transfer discount.
	 * @param busBusInterchangeDiscount
	 * @param trainBusInterchangeDiscount
	 */
	public void enableFixValuedTransfer(double busBusInterchangeDiscount, double trainBusInterchangeDiscount) {
		this.busBusDiscount = busBusInterchangeDiscount;
		this.busTrainDiscount = trainBusInterchangeDiscount;
		this.fixedValueTransferEnabled = true;
	}
	
	@Override
	public void install() {
		if (getConfig().transit().isUseTransit()) {
			MapBinder<String, FareCalculator> mapbinder = MapBinder.newMapBinder(binder(), String.class,
					FareCalculator.class);

			// Bind the MTR fare calculator
			mapbinder.addBinding("train").to(MTRFareCalculator.class).in(Scopes.SINGLETON);
			bind(String.class).annotatedWith(Names.named("trainFareInput")).toInstance(MTRFareFilePath); // File path

			// The bus fare calculator
//			if (busCalculator == null) {
//				mapbinder.addBinding("bus").toInstance(new UniformFareCalculator(3.0));
//				mapbinder.addBinding("minibus").toInstance(new UniformFareCalculator(3.0));
//			} else {
//				mapbinder.addBinding("bus").toInstance(this.busCalculator);
//				mapbinder.addBinding("minibus").toInstance(this.busCalculator);
//			}
			try {
				mapbinder.addBinding("bus").toInstance(this.busCalculator);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//mapbinder.addBinding("minibus").toInstance(this.busCalculator);

			// The tram and ship fare calculator
			mapbinder.addBinding("tram").toInstance(new UniformFareCalculator(tramFare));
			try {
				mapbinder.addBinding("ferry").toInstance(FareCalculatorPTGTFS.loadFareCalculatorPTGTFS(ferryFareJsonPath));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//The LR fare calculator
			mapbinder.addBinding("LR").to(LRFareCalculator.class).in(Scopes.SINGLETON);
			bind(String.class).annotatedWith(Names.named("LRFareInput")).toInstance(LRFareFilePath);

			addEventHandlerBinding().to(transitFareAndHandler.TransitFareHandler.class).in(Singleton.class); // Add the transit fare handler
			addEventHandlerBinding().to(PTRecordHandler.class).in(Singleton.class); // Only one PTRecordHandler exists

			addControlerListenerBinding().to(transitFareAndHandler.TransitFareControlerListener.class);
			bind(transitFareAndHandler.TransitFareHandler.class).in(Singleton.class);
			bind(TransferWalkingTime.class).in(Singleton.class);;
			bind(String.class).annotatedWith(Names.named("WalkTimeInput")).toInstance("");
			// Bind the fare to new dynamic and fare calculator
			bind(TransitRouter.class).to(TransitRouterFareDynamicImpl.class);

			// This one class is useful for all three calculations
			bind(StopStopTime.class).to(PTRecordHandler.class).in(Singleton.class);
			bind(VehicleOccupancy.class).to(PTRecordHandler.class).in(Singleton.class);
			bind(WaitingTime.class).to(PTRecordHandler.class).in(Singleton.class);
			
			
			bind(TransferDiscountCalculator.class).toInstance(new Temp());
			//bind(String.class).annotatedWith(Names.named("TransferDiscountJson")).toInstance(transferDiscountJson); //Changed name
			bind(Boolean.class).annotatedWith(Names.named("FixedDiscount")).toInstance(fixedValueTransferEnabled);
			bind(Double.class).annotatedWith(Names.named("BBI")).toInstance(busBusDiscount);
			bind(Double.class).annotatedWith(Names.named("TBI")).toInstance(busTrainDiscount);
			
			
		
			//bind(String.class).annotatedWith(Names.named(transferDiscountJsonName)).toInstance(transferDiscountJson); //Changed name
		
			
			bind(Boolean.class).annotatedWith(Names.named(fareRateEnabledName)).toInstance(fareRateEnabled);
			bind(Double.class).annotatedWith(Names.named(fareRateName)).toInstance(fareRate);
			
			if(selectedFareRate==null) {
				selectedFareRate = -1.;
				selectedStops = Lists.newArrayList();
			}
			if(selectedFareRate2==null) {
				selectedFareRate2 = -1.;
				selectedStops2 = Lists.newArrayList();
			}
			bind(new TypeLiteral<List<Id<TransitStopFacility>>>(){}).annotatedWith(Names.named(selectedStopsName)).toInstance(selectedStops);
			bind(Double.class).annotatedWith(Names.named(selectedFareRateName)).toInstance(selectedFareRate);
			
			bind(new TypeLiteral<List<Id<TransitStopFacility>>>(){}).annotatedWith(Names.named(selectedStopsName2)).toInstance(selectedStops2);
			bind(Double.class).annotatedWith(Names.named(selectedFareRateName2)).toInstance(selectedFareRate2);
		}

	}
	
	class Temp implements TransferDiscountCalculator{
		
		
		
		@Override
		public double getInterchangeDiscount(Id<TransitLine> fromTransitLineId, Id<TransitLine> toTransitLineId,
				Id<TransitRoute> fromTransitRouteId, Id<TransitRoute> toTransitRouteId, String fromMode, String toMode,
				double lastStartTime, double lastEndTime, double thisAboardTime, double lastFare, double thisFare) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public BigDecimal getExactInterchangeDiscount(Id<TransitLine> fromTransitLineId,
				Id<TransitLine> toTransitLineId, Id<TransitRoute> fromTransitRouteId, Id<TransitRoute> toTransitRouteId,
				String fromMode, String toMode, double lastStartTime, double lastEndTime, double thisAboardTime,
				double lastFare, double thisFare) {
			// TODO Auto-generated method stub
			return BigDecimal.ZERO;
		}
		
		
		
	}

}

