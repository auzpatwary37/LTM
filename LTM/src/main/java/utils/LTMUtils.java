package utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.matsim.core.utils.collections.Tuple;

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
	public Tuple<double[],List<double[]>> calculateProportionAndGradient(double[] weights, List<double[]> weightGradients, double capacity, double[] capacityGradient){
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
	 * @param X tuple of X1 and X2, assumed sorted with X1<X2, though might not be necessary
	 * @param Y tuple of Y1 and Y2, no need to be sorted. 
	 * @param dX tuple of vectors of dX1 and dX2
	 * @param dY tuple of vectors of dY1 and dY2
	 * @param x the point to interpolate at 
	 * @param dx gradient at point x
	 * @return tuple containing interpolated value y and the gradient vector dy
	 * Should work with dX And dY null And dx null, in that case only the interpolated value will be returned, i.e., the second term, i.e., the gradient of y in the tuple will return null
	 */
	public Tuple<Double,double[]> calcLinearInterpolationAndGradient(Tuple<Double,Double> X, Tuple<Double,Double> Y,Tuple<double[],double[]> dX, 
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
	
	
}
