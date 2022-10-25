package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import linkModels.LinkModel;
import ltmAlgorithm.DNL;

/**
 * 
 * @author ashrafzaman
 * 
 * This class contains the utility functions for the LTM gradient 
 * 
 */
public class LTMUtils{

	/**
	 * 
	 * @param weights 
	 * @param weightGradients
	 * @param capacity
	 * @param capacityGradient
	 * 
	 * calculates the gradient of the following function
	 * 
	 * Y = X./Sum(X)*cap(matlab syntax) cap is the amount to be distributed mentioned as capacity
	 * X is the vector containing the weights of the proportions to be distributed. 
	 * Y is the vector of distributed capacity. 
	 * 
	 * The variable names are already self explanatory
	 * 
	 *  should work when the weights gradients is null or empty AND capacity gradient is null, the second element in the return tuple will be null in that case.
	 * 
	 * @return
	 */
	public static Tuple<double[],List<double[]>> calculateProportionAndGradient(double[] weights, List<double[]> weightGradients, double capacity, double[] capacityGradient){
		double[] p = new double[weights.length];
		double[] pCap = new double[weights.length];
		List<RealVector> pGrads =null;
		List<double[]>pCapGrad = null;
		
		double sum = 0;
		RealVector sumGradient = null;
		if(weightGradients !=null &&  !weightGradients.isEmpty()) {
			sumGradient = MatrixUtils.createRealVector(new double[weightGradients.get(0).length]);
			pGrads = new ArrayList<>();
			pCapGrad = new ArrayList<>();
		}
		
		for(int i=0;i<weights.length;i++) {
			sum+=weights[i];
			if(weightGradients != null && !weightGradients.isEmpty() && capacityGradient != null) {
				sumGradient = sumGradient.add(MatrixUtils.createRealVector(weightGradients.get(i)));
			}
		}
		if(sum==0)throw new IllegalArgumentException("The sum of the weights cannot be zero!!!");
		for(int i=0;i<weights.length;i++) {
			p[i]=weights[i]/sum;
			if(weightGradients != null && !weightGradients.isEmpty() && capacityGradient != null) {
				RealVector pGrad = MatrixUtils.createRealVector(weightGradients.get(i)).mapMultiply(sum).subtract(sumGradient.mapMultiply(weights[i])).mapDivide(sum*sum);
				pGrads.add(pGrad);
			}
		}
		
		for(int i=0;i<weights.length;i++) {
			pCap[i] = capacity*p[i];
			if(weightGradients != null && !weightGradients.isEmpty() && capacityGradient != null) {
				pCapGrad.add(MatrixUtils.createRealVector(capacityGradient).mapMultiply(p[i]).add(pGrads.get(i).mapMultiply(capacity)).getData());
			}
		}
		
		return new Tuple<>(pCap,pCapGrad);
	}
	
	
	/**
	 * 
	 * @param weights 
	 * @param weightGradients
	 * 
	 * calculates the gradient of the following function
	 * 
	 * Y = X./Sum(X)(matlab syntax) 
	 * X is the vector containing the weights of the proportions to be distributed. 
	 * Y is the vector of distributed proportion. 
	 * 
	 * The variable names are already self explanatory
	 * 
	 *  should work when the weights gradients is null or empty the second element in the return tuple will be null in that case.
	 * 
	 * @return
	 */
	public static Tuple<double[],List<double[]>> calculateRatioAndGradient(double[] weights, List<double[]> weightGradients){
		double[] p = new double[weights.length];
		List<RealVector> pGrads =null;
		
		double sum = 0;
		RealVector sumGradient = null;
		if(weightGradients !=null &&  !weightGradients.isEmpty()) {
			sumGradient = MatrixUtils.createRealVector(new double[weightGradients.get(0).length]);
			pGrads = new ArrayList<>();
		}
		
		for(int i=0;i<weights.length;i++) {
			sum+=weights[i];
			if(weightGradients != null && !weightGradients.isEmpty()) {
				sumGradient = sumGradient.add(MatrixUtils.createRealVector(weightGradients.get(i)));
			}
		}
		if(sum==0)throw new IllegalArgumentException("The sum of the weights cannot be zero!!!");
		for(int i=0;i<weights.length;i++) {
			p[i]=weights[i]/sum;
			if(weightGradients != null && !weightGradients.isEmpty()) {
				RealVector pGrad = MatrixUtils.createRealVector(weightGradients.get(i)).mapMultiply(sum).subtract(sumGradient.mapMultiply(weights[i])).mapDivide(sum*sum);
				pGrads.add(pGrad);
			}
		}
		
		return new Tuple<>(p,pGrads.stream().map(r->r.getData()).collect(Collectors.toList()));
	}
	
	public static <T> Map<T,Tuple<Double,double[]>> calculateRatioAndGradient(Map<T,Double> weights, Map<T,double[]> weightGradients){
		Map<T,Double> p = new HashMap<>();
		Map<T,RealVector> pGrads =new HashMap<>();
		
		double sum = 0;
		RealVector sumGradient = null;
//		if(weightGradients !=null &&  !weightGradients.isEmpty()) {
//			sumGradient = MatrixUtils.createRealVector(new double[weightGradients.get(0).length]);
//		}
		for(Entry<T, Double> w:weights.entrySet()) {
			sum+=w.getValue();
			if(weightGradients != null && !weightGradients.isEmpty()) {
				if(sumGradient == null)sumGradient= MatrixUtils.createRealVector(weightGradients.get(w.getKey()));
				else sumGradient = sumGradient.add(weightGradients.get(w.getKey()));
			}
		}
		
		if(sum==0)throw new IllegalArgumentException("The sum of the weights cannot be zero!!!");
		
		for(Entry<T, Double> d:weights.entrySet()) {
			p.put(d.getKey(),d.getValue()/sum);
			if(weightGradients != null && !weightGradients.isEmpty()) {
				RealVector pGrad = MatrixUtils.createRealVector(weightGradients.get(d.getKey())).mapMultiply(sum).subtract(sumGradient.mapMultiply(weights.get(d.getKey()))).mapDivide(sum*sum);
				pGrads.put(d.getKey(),pGrad);
			}
		}
		Map<T,Tuple<Double,double[]>>out = new HashMap<>();
		for(T k:p.keySet()) {
			out.put(k, new Tuple<>(p.get(k),pGrads.get(k).getData()));
		}
		return out;
	}
	/**
	 * 
	 * @param X tuple of X1 and X2, assumed sorted with X1<X2, though might not be necessary
	 * @param Y tuple of Y1 and Y2, no need to be sorted. 
	 * @param dX tuple of vectors of dX1 and dX2
	 * @param dY tuple of vectors of dY1 and dY2
	 * @param x the point to interpolate at 
	 * @param dx gradient at point x
	 * @return tuple containing interpolated value y and the gradient vector dy
	 * Should work with dX And dY null And dx null, in that case only the interpolated value will be returned, i.e., the second term, i.e., the gradient of y in the tuple will return null
	 */
	public static Tuple<Double,double[]> calcLinearInterpolationAndGradient(Tuple<Double,Double> X, Tuple<Double,Double> Y,Tuple<double[],double[]> dX, 
			Tuple<double[],double[]>dY,double x, double[] dx){
		
		RealVector X2minusX1Grad = null;
		RealVector Y2minusY1Grad = null;
		RealVector xminusX1Grad = null;
		double[] dy = null;
		
		double X2minusX1 = X.getSecond()-X.getFirst();
		if(X2minusX1==0)throw new IllegalArgumentException("Both points cannot have same x values!!!");
		if(dX!=null && dY!=null && dx!=null){
			X2minusX1Grad = MatrixUtils.createRealVector(dX.getSecond()).subtract(MatrixUtils.createRealVector(dX.getFirst()));
		}
		double Y2minusY1 = Y.getSecond()-Y.getFirst();
		if(dX!=null && dY!=null && dx!=null){
			Y2minusY1Grad = MatrixUtils.createRealVector(dY.getSecond()).subtract(MatrixUtils.createRealVector(dY.getFirst()));
		}
		
		double xminusX1 = x-X.getFirst();
		if(dX!=null && dY!=null && dx!=null){
			xminusX1Grad = MatrixUtils.createRealVector(dx).subtract(MatrixUtils.createRealVector(dX.getFirst()));
		}
		double y = Y.getFirst()+Y2minusY1/X2minusX1*xminusX1;
		
		if(dX!=null && dY!=null && dx!=null){
			RealVector numeratorGrad = Y2minusY1Grad.mapMultiply(xminusX1).add(xminusX1Grad.mapMultiply(Y2minusY1));
			dy = numeratorGrad.mapMultiply(X2minusX1).subtract(X2minusX1Grad.mapMultiply(Y2minusY1*xminusX1)).mapDivide(X2minusX1*X2minusX1).getData();
		}
		return new Tuple<>(y,dy);
		
	}
	
	public boolean routeEquals(NetworkRoute r1, NetworkRoute r2) {
		return r1.toString().equals(r2.toString());
	}
	
	public static Id<Link> findNextLink(NetworkRoute r,Id<Link> fromLink) {
		int k = 1;
		if(r.getStartLinkId().equals(fromLink))return r.getLinkIds().get(0);
		else if(r.getEndLinkId().equals(fromLink))return null;
		else if(r.getLinkIds().get(r.getLinkIds().size()-1).equals(fromLink))return r.getEndLinkId();
		else{
			for(Id<Link> lId:r.getLinkIds()) {
				if(lId.equals(fromLink))return r.getLinkIds().get(k);
				k++;
			}
		}
		return null;
	}
	
	public static TuplesOfThree<Double,Double,double[]> calcAbyBGrad(TuplesOfThree<Double,Double,double[]> A, TuplesOfThree<Double,Double,double[]> B){
		Double abyb = A.getFirst()/B.getFirst();
		double bsquare = Math.pow(B.getFirst(), 2);
		double[] grad = null;
		if(A.getThird() != null && B.getThird() != null)MatrixUtils.createRealVector(A.getThird()).mapMultiply(B.getFirst()).subtract(MatrixUtils.createRealVector(B.getThird()).mapMultiply(A.getFirst())).mapDivide(bsquare).getData();//(bda-adb)/b^2
		Double dt = null;
		if(A.getSecond() != null && B.getSecond() != null) dt = (B.getFirst()*A.getSecond()-A.getFirst()*B.getSecond())/bsquare;
		return new TuplesOfThree<>(abyb,dt,grad);
	}

	public static TuplesOfThree<Double,Double,double[]> calcAtimesBGrad(TuplesOfThree<Double,Double,double[]> A, TuplesOfThree<Double,Double,double[]> B){
		Double abyb = A.getFirst()*B.getFirst();
		
		double[] grad = null;
		if(A.getThird() != null && B.getThird() != null)grad = MatrixUtils.createRealVector(A.getThird()).mapMultiply(B.getFirst()).add(MatrixUtils.createRealVector(B.getThird()).mapMultiply(A.getFirst())).getData();//(bda+adb)
		Double dt = null;
		if(A.getSecond() != null && B.getSecond() != null)dt = (B.getFirst()*A.getSecond()+A.getFirst()*B.getSecond());
		return new TuplesOfThree<>(abyb,dt,grad);
	}
	
	public static Link createDummyLink(Node fromNode,Node toNode,NetworkRoute r,boolean ifOriginElseDestination) {
		String originDestinationIdentifier = "O";
		if(!ifOriginElseDestination)originDestinationIdentifier = "D";
		Link l = NetworkUtils.createLink(Id.createLinkId(r.getRouteDescription()+originDestinationIdentifier), 
				fromNode, toNode, null, 10, 10000, 36000, 1);
		return l;
	}
	public static Node createDummyNode(Node originalNode, boolean ifOriginElseDestination,NetworkRoute r) {
		String originDestinationIdentifier = "O";
		if(!ifOriginElseDestination)originDestinationIdentifier = "D";
		Node n = NetworkUtils.createNode(Id.create(r.getRouteDescription()+originDestinationIdentifier,
				Node.class), new Coord(originalNode.getCoord().getX()+Math.random()*100,
						originalNode.getCoord().getY()+Math.random()*100));
		return n;
	}
	/**
	 * 
	 * @param demand map of timebinKey - tuple<deamnd, demandGradient>
	 * @param demandTimeBean - timeBeanKey -Tuple<start, end>
	 * @param variables - variables with respect to which calculate gradient.
	 * @param T - number of timeSteps in the LTM
	 * @param LTMTimePoints 
	 * @param maxflowRate 
	 * @param ifUniformElseConstFlowRate
	 * @return Nr Nrdt dNr
	 */
	public static TuplesOfThree<double[],double[],double[][]> setUpDemand(Map<String,Tuple<Double,double[]>>demand,Map<String,Tuple<Double,Double>> demandTimeBean,
			MapToArray<VariableDetails>variables,double[] LTMTimePoints, double maxflowRate, boolean ifUniformElseConstFlowRate) {
		double[] Nr = new double[LTMTimePoints.length];
		double[] Nrdt = new double[LTMTimePoints.length];
		double[][] dNr = null;
		
		if(variables!=null)dNr = new double[LTMTimePoints.length][variables.getKeySet().size()];
		
		
		for(Entry<String, Tuple<Double, Double>> timeBean:demandTimeBean.entrySet()) {
			Set<Integer> timeSteps = new HashSet<>();
			for(int t=0;t<LTMTimePoints.length;t++) {
				if(LTMTimePoints[t]<=timeBean.getValue().getSecond() && LTMTimePoints[t]>timeBean.getValue().getFirst()) {
					timeSteps.add(t);
				}
				
			}
			if(ifUniformElseConstFlowRate) {
				double demandTotal = demand.get(timeBean.getKey()).getFirst();
				double[] demandTotalGrad = demand.get(timeBean.getKey()).getSecond();
				double rate = demandTotal/(timeBean.getValue().getSecond()-timeBean.getValue().getFirst());
				RealVector rateGrad = null;
				if(demandTotalGrad!=null)rateGrad = MatrixUtils.createRealVector(demandTotalGrad).mapDivide(timeBean.getValue().getSecond()-timeBean.getValue().getFirst());
				for(int t:timeSteps) {
					Nrdt[t] = rate; 
					if(t==0) {
						Nr[t] = rate*(LTMTimePoints[t]- LTMTimePoints[t-1]);
						if(dNr!=null && rateGrad!=null)dNr[t] = rateGrad.mapMultiply(LTMTimePoints[t]- LTMTimePoints[t-1]).getData();							;
					}else {
						Nr[t] = Nr[t-1]+rate*(LTMTimePoints[t]- LTMTimePoints[t-1]);
						if(dNr!=null && rateGrad!=null)dNr[t] = rateGrad.mapMultiply(LTMTimePoints[t]- LTMTimePoints[t-1]).add(dNr[t-1]).getData();							;
					
					}
				}
			}else {
				throw new IllegalArgumentException("Constant demand loading is not yet implemented!!");
			}
		}
		
		return new TuplesOfThree<double[], double[], double[][]>(Nr, Nrdt, dNr);
	}
	
	
	/**
	 * 
	 * @param demand map of timeStamp- tuple<deamnd, demandGradient>
	 * @param demandTimeBean - timeBeanKey -Tuple<start, end>
	 * @param variables - variables with respect to which calculate gradient.
	 * @param T - number of timeSteps in the LTM
	 * @param LTMTimePoints 
	 * @param maxflowRate 
	 * @param ifUniformElseConstFlowRate
	 * @return Nr Nrdt dNr
	 */
	public static TuplesOfThree<double[],double[],double[][]> setUpDemand(Map<Integer,Tuple<Double,double[]>>demand,
			MapToArray<VariableDetails>variables,double[] LTMTimePoints) {
		double[] Nr = new double[LTMTimePoints.length];
		double[] Nrdt = new double[LTMTimePoints.length];
		double[][] dNr = null;
		
		if(variables!=null)dNr = new double[LTMTimePoints.length][variables.getKeySet().size()];
		
		for(int t=1;t<LTMTimePoints.length;t++) {
			Set<Integer> timeStamps = new HashSet<>();
			
			for(Entry<Integer, Tuple<Double, double[]>> tdemand:demand.entrySet()) {
			
				int time = tdemand.getKey();
				if(time==0)time=1;
				if(time<=LTMTimePoints[t] && time>LTMTimePoints[t-1]) {
					timeStamps.add(time);
				}
				
			}
			double d = 0;
			double ddt = 0;
			RealVector dGrad = MatrixUtils.createRealVector(new double[variables.getKeySet().size()]);
			for(int time:timeStamps) {
				d += demand.get(time).getFirst();
				dGrad = dGrad.add(demand.get(time).getSecond());
			}
			ddt = d/LTMTimePoints[t]-LTMTimePoints[t-1];
			Nr[t] = d;
			Nrdt[t] = ddt;
			dNr[t] = dGrad.getData();
		}
		
		return new TuplesOfThree<double[], double[], double[][]>(Nr, Nrdt, dNr);
	}
	
	public static TuplesOfThree<Double,Double,double[]> getRouteTravelTime(NetworkRoute r, double[] timePoints, double departureTime, LinkModel boardingLinkModel, LinkModel alightingLinkModel){
		
		double travelTime = 0;
		double travelTimedt = 0;
		double[] dTravelTime = null;
		int timeStepBefore = 0;
		int timeStepAfter = 0;
		for(int t=0;t<timePoints.length;t++) {
			
			if(timePoints[t]<=departureTime) {
				timeStepBefore = t;
			}else {
				break;
			}
		}
		for(int t=timePoints.length-1;t<0;t--) {
				
			if(timePoints[t]>=departureTime) {
				timeStepAfter = t;
			}else {
				break;
			}
		}
		//Assuming there is no gradient of departure time
		int rInd = boardingLinkModel.getRoutes().getIndex(r);
		double nrx0Before = boardingLinkModel.getNrx0()[rInd][timeStepBefore];
		double nrx0Beforedt = boardingLinkModel.getNrx0dt()[rInd][timeStepBefore];
		double[] dnrx0Before = boardingLinkModel.getdNrx0()[rInd][timeStepBefore];
		
		
		double nrx0After = boardingLinkModel.getNrx0()[rInd][timeStepAfter];
		double nrx0Afterdt = boardingLinkModel.getNrx0dt()[rInd][timeStepAfter];
		double[] dnrx0After = boardingLinkModel.getdNrx0()[rInd][timeStepAfter];
		
		double nrx0 = 0;
		double nrx0dt = 0;
		RealVector dnrx0 = null;
		
		if(timeStepBefore!=timeStepAfter) {
		
			Tuple<Double,double[]> dnrx0Tuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>((double)timeStepBefore,(double)timeStepAfter),new Tuple<>(nrx0Before,nrx0After),new Tuple<>(new double[dnrx0Before.length],
					new double[dnrx0After.length]), new Tuple<>(dnrx0Before,dnrx0After), departureTime, new double[dnrx0After.length]);
			
			Tuple<Double,double[]> nrx0dtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>((double)timeStepBefore,(double)timeStepAfter),new Tuple<>(nrx0Before,nrx0After),new Tuple<>(new double[] {0},
					new double[] {0}) , new Tuple<>(new double[] {nrx0Beforedt},new double[] {nrx0Afterdt}), departureTime, new double[] {0});
			
			nrx0 = dnrx0Tuple.getFirst();
			nrx0dt = nrx0dtTuple.getSecond()[0];
			dnrx0 = MatrixUtils.createRealVector(dnrx0Tuple.getSecond());
		}else {
			nrx0 = nrx0Before;
			nrx0dt = nrx0Beforedt;
			dnrx0 = MatrixUtils.createRealVector(dnrx0Before);
		}
		//Find out the tBefore and tAfter for the arrival
		int tBefore = timeStepBefore;
		int tAfter = timeStepBefore;
		rInd = alightingLinkModel.getRoutes().getIndex(r);
		for(int j = timeStepBefore;j<timePoints.length;j++) {
			if(nrx0>=alightingLinkModel.getNrxl()[rInd][j]) {
				tBefore = j;
				
			}
			if(nrx0<=alightingLinkModel.getNrxl()[rInd][j]) {
				tAfter = j;
				break;
			}
		}
		double tBeforedt = 1/alightingLinkModel.getNrxldt()[rInd][tBefore]*nrx0dt;
		RealVector dtBefore = dnrx0.mapMultiply(1/alightingLinkModel.getNrxldt()[rInd][tBefore]);
		
		double tAfterdt = 1/alightingLinkModel.getNrxldt()[rInd][tAfter]*nrx0dt;
		RealVector dtAfter = dnrx0.mapMultiply(1/alightingLinkModel.getNrxldt()[rInd][tAfter]);
		double t = 0;
		double tdt = 0;
		double[] dt = null;
		if(tBefore!=tAfter) {
		
			Tuple<Double,double[]>dtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(alightingLinkModel.getNrxl()[rInd][tBefore],alightingLinkModel.getNrxl()[rInd][tAfter]),
					new Tuple<Double,Double>((double)tBefore,(double)tAfter), 
					new Tuple<double[],double[]>(alightingLinkModel.getdNrxl()[rInd][tBefore],alightingLinkModel.getdNrxl()[rInd][tAfter]),
					new Tuple<double[],double[]>(dtBefore.getData(),dtAfter.getData()), nrx0, dnrx0.getData());
			
			Tuple<Double,double[]>tdtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(alightingLinkModel.getNrxl()[rInd][tBefore],alightingLinkModel.getNrxl()[rInd][tAfter]),
					new Tuple<Double,Double>((double)tBefore,(double)tAfter), 
					new Tuple<double[],double[]>(new double[] {alightingLinkModel.getNrxldt()[rInd][tBefore]},new double[] {alightingLinkModel.getNrxldt()[rInd][tAfter]}),
					new Tuple<double[],double[]>(new double[] {tBeforedt},new double[] {tAfterdt}), nrx0, new double[] {nrx0dt});
			
			t = dtTuple.getFirst();
			dt = dtTuple.getSecond();
			tdt = tdtTuple.getSecond()[0];
		}else {
			t = tBefore;
			dt = dtBefore.getData();
			tdt = tBeforedt;
		}
		travelTime = t-departureTime;
		travelTimedt = tdt;
		dTravelTime = dt;
		return new TuplesOfThree<>(travelTime,travelTimedt,dTravelTime);
		
	}
	/**
	 * Entry 
	 * @return
	 */
	public static TuplesOfThree<Double,Double,double[]> getLinkVolume(LinkModel link,double fromTime,double toTime, double[] timePoints){
		double volume = 0;
		double volumedt = 0;
		double[] dVolume = null;
		int fromTimeBefore = 0;
		int fromTimeAfter = 0;
		int toTimeBefore = 0;
		int toTimeAfter = 0;
		
		for(int i = 0;i<timePoints.length;i++) {
			if(timePoints[i]<=fromTime) fromTimeBefore = i;
			if(timePoints[i]>=fromTime) {
				fromTimeAfter = i;
				break;
			}
		}
		for(int i = 0;i<timePoints.length;i++) {
			if(timePoints[i]<=toTime) toTimeBefore = i;
			if(timePoints[i]>=toTime) {
				toTimeAfter = i;
				break;
			}
		}
		double fromNx0Before = link.getNx0()[fromTimeBefore];
		double fromNx0dtBefore = link.getNx0dt()[fromTimeBefore];
		double[] fromdNx0Before = link.getdNx0()[fromTimeBefore];
		
		double fromNx0After = link.getNx0()[fromTimeAfter];
		double fromNx0dtAfter = link.getNx0dt()[fromTimeAfter];
		double[] fromdNx0After = link.getdNx0()[fromTimeAfter];
		
		Tuple<Double,double[]> fromdNx0Tuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(timePoints[fromTimeBefore],timePoints[fromTimeAfter]),
				new Tuple<>(fromNx0Before,fromNx0After), new Tuple<>(new double[fromdNx0Before.length], new double[fromdNx0Before.length]),
				new Tuple<>(fromdNx0Before,fromdNx0After), fromTime, new double[fromdNx0Before.length]);
		
		Tuple<Double,double[]> fromNx0dtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(timePoints[fromTimeBefore],timePoints[fromTimeAfter]),
				new Tuple<>(fromNx0Before,fromNx0After), new Tuple<>(new double[] {0}, new double[] {0}),
				new Tuple<>(new double[] {fromNx0dtBefore},new double[] {fromNx0dtAfter}), fromTime, new double[] {0});
		
		double fromNx0 = fromdNx0Tuple.getFirst();
		double fromNx0dt = fromNx0dtTuple.getSecond()[0];
		double[] fromdNx0 = fromdNx0Tuple.getSecond();
		
		double toNx0Before = link.getNx0()[toTimeBefore];
		double toNx0dtBefore = link.getNx0dt()[toTimeBefore];
		double[] todNx0Before = link.getdNx0()[toTimeBefore];
		
		double toNx0After = link.getNx0()[toTimeAfter];
		double toNx0dtAfter = link.getNx0dt()[toTimeAfter];
		double[] todNx0After = link.getdNx0()[toTimeAfter];
		
		Tuple<Double,double[]> todNx0Tuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(timePoints[toTimeBefore],timePoints[toTimeAfter]),
				new Tuple<>(toNx0Before,toNx0After), new Tuple<>(new double[todNx0Before.length], new double[todNx0Before.length]),
				new Tuple<>(todNx0Before,todNx0After), toTime, new double[todNx0Before.length]);
		
		Tuple<Double,double[]> toNx0dtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(timePoints[toTimeBefore],timePoints[toTimeAfter]),
				new Tuple<>(toNx0Before,toNx0After), new Tuple<>(new double[] {0}, new double[] {0}),
				new Tuple<>(new double[] {toNx0dtBefore},new double[] {toNx0dtAfter}), toTime, new double[] {0});
		
		double toNx0 = todNx0Tuple.getFirst();
		double toNx0dt = toNx0dtTuple.getSecond()[0];
		double[] todNx0 = todNx0Tuple.getSecond();
		
		volume = toNx0-fromNx0;
		volumedt = toNx0dt-fromNx0dt;
		dVolume = MatrixUtils.createRealVector(todNx0).subtract(fromdNx0).getData();
		
		return new TuplesOfThree<>(volume,volumedt,dVolume);
	}
}
