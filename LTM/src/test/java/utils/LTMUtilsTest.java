package utils;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.matsim.core.utils.collections.Tuple;

public class LTMUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCalculateProportionAndGradient() {
		double[] x = new double[] {1,2};
		double cap = 3;
		List<double[]> weightGradient = List.of(new double[] {.1,.5},new double[] {-.3,-.4});
		double[] capGrad = new double[] {.05,-.08};
		
		Tuple<double[], List<double[]>> out = LTMUtils.calculateProportionAndGradient(x, weightGradient, cap, capGrad);
		
		assertTrue(Arrays.equals(out.getFirst(), x));
		assertEquals(out.getSecond().get(0)[0], .55/3,.000001);
		assertEquals(out.getSecond().get(0)[1], .44,.000001);
		assertEquals(out.getSecond().get(1)[0], -.4/3,.000001);
		assertEquals(out.getSecond().get(1)[1], -.52,.000001);
	}

	@Test
	public void testCalculateRatioAndGradientDoubleArrayListOfdouble() {
		double[] x = new double[] {1,2};
		List<double[]> dx = List.of(new double[] {.1,.5},new double[] {-.3,-.4});
		Tuple<double[], List<double[]>> out = LTMUtils.calculateRatioAndGradient(x, dx);
		assertTrue(Arrays.equals(out.getFirst(), new double[] {1/3.,2/3.}));
		assertEquals(out.getSecond().get(0)[0], .5/9,.000001);
		assertEquals(out.getSecond().get(0)[1], 1.4/9,.000001);
		assertEquals(out.getSecond().get(1)[0], -.5/9,.000001);
		assertEquals(out.getSecond().get(1)[1], -1.4/9,.000001);		
	}

	@Test
	public void testCalculateRatioAndGradientMapOfTDoubleMapOfTdouble() {
		Map<Integer,Double> x = Map.of(1, 1., 2, 2.);
		Map<Integer,double[]> dx = Map.of(1,new double[] {.1,.5},2,new double[] {-.3,-.4});
		
		Map<Integer, Tuple<Double, double[]>> out = LTMUtils.calculateRatioAndGradient(x, dx);
		assertEquals(out.get(1).getFirst(), 1./3,.00001);
		assertEquals(out.get(2).getFirst(), 2./3,.00001);
		assertEquals(out.get(1).getSecond()[0], .5/9,.000001);
		assertEquals(out.get(1).getSecond()[1], 1.4/9,.000001);
		assertEquals(out.get(2).getSecond()[0], -.5/9,.000001);
		assertEquals(out.get(2).getSecond()[1], -1.4/9,.000001);
	}

	@Test
	public void testCalcLinearInterpolationAndGradient() {
		Tuple<Double,Double>xRange = new Tuple<>(1.,5.);
		Tuple<Double,Double>yRange = new Tuple<>(-.75,.5);
		Tuple<double[],double[]> xRangeGrad = new Tuple<>(new double[] {.1,.5},new double[] {-.3,-.4});
		Tuple<double[],double[]> yRangeGrad = new Tuple<>(new double[] {.035,-.17},new double[] {-.13,.041});
		
		Tuple<Double, double[]> out = LTMUtils.calcLinearInterpolationAndGradient(xRange, yRange, xRangeGrad, yRangeGrad, 2.5, new double[] {.2,-.3});
		
		double y = yRange.getFirst()+(yRange.getSecond()-yRange.getFirst())/(xRange.getSecond()-xRange.getFirst())*(2.5-xRange.getFirst());
		double x2Minusx1 = xRange.getSecond()-xRange.getFirst();
		double[] x2Minusx1Grad = LTMUtils.subtract(xRangeGrad.getSecond(),xRangeGrad.getFirst());
		double xMinusx1 = 2.5-xRange.getFirst();
		double[] xMinusx1Grad = LTMUtils.subtract(new double[] {.2,-.3},xRangeGrad.getFirst());
		double y2MinusY1 = yRange.getSecond()-yRange.getFirst();
		double[] y2Minusy1Grad = LTMUtils.subtract(yRangeGrad.getSecond(), yRangeGrad.getFirst());
		double dy1 = yRangeGrad.getFirst()[0]+//dy1+
				(x2Minusx1*(y2MinusY1*xMinusx1Grad[0]+xMinusx1*y2Minusy1Grad[0])//(x2-x1)((y2-y1)(dx-dx1)+(x-x1)(dy2-dy1))   i.e., vdu
						-y2MinusY1*xMinusx1*x2Minusx1Grad[0])/(x2Minusx1*x2Minusx1);//(y2-y1)(x-x1)(dx2-dx1) i.e., udv / (x2-x1)^2
		double dy2 = yRangeGrad.getFirst()[1]+//dy1+
				(x2Minusx1*(y2MinusY1*xMinusx1Grad[1]+xMinusx1*y2Minusy1Grad[1])//(x2-x1)((y2-y1)(dx-dx1)+(x-x1)(dy2-dy1))   i.e., vdu
						-y2MinusY1*xMinusx1*x2Minusx1Grad[1])/(x2Minusx1*x2Minusx1);//(y2-y1)(x-x1)(dx2-dx1) i.e., udv / (x2-x1)^2
		
		assertEquals(out.getFirst(),y,.00001);
		assertEquals(out.getSecond()[0], dy1,.00001);
		assertEquals(out.getSecond()[1], dy2,.000001);
	}


	@Test
	public void testCalcAbyBGrad() {
		double a = 2;
		double b = 3;
		double adt = .1;
		double bdt = .2;
		double[] aGrad = new double[] {.1,.5};
		double[] bGrad = new double[] {-.3,-.4};
		
		TuplesOfThree<Double, Double, double[]> out = LTMUtils.calcAbyBGrad(new TuplesOfThree<>(a,adt,aGrad), new TuplesOfThree<>(b,bdt,bGrad));
		assertEquals(out.getFirst(),2/3.,.00001);
		assertEquals(out.getSecond(),(3*.1-2*.2)/9,.00001);
		assertEquals(out.getThird()[0],(3*.1-2*-.3)/(3*3),.00001);
		assertEquals(out.getThird()[1],(3*.5-2*-.4)/(3*3),.00001);
	}

	@Test
	public void testCalcAtimesBGrad() {
		double a = 2;
		double b = 3;
		double adt = .1;
		double bdt = .2;
		double[] aGrad = new double[] {.1,.5};
		double[] bGrad = new double[] {-.3,-.4};
		
		TuplesOfThree<Double, Double, double[]> out = LTMUtils.calcAtimesBGrad(new TuplesOfThree<>(a,adt,aGrad), new TuplesOfThree<>(b,bdt,bGrad));
		assertEquals(out.getFirst(),6.,.00001);
		assertEquals(out.getSecond(),2*.2+.1*3,.00001);
		assertEquals(out.getThird()[0],2*-.3+.1*3,.00001);
		assertEquals(out.getThird()[1],2*-.4+.5*3,.00001);
		
	}

	@Test
	public void testSum() {
		double[] a = new double[] {1,2,3};
		double[] b = new double[] {1,2,3};
		assertTrue(Arrays.equals(LTMUtils.sum(a, b), new double[] {2,4,6}));
	}

	@Test
	public void testSubtract() {
		double[] a = new double[] {1,2,3};
		double[] b = new double[] {1,2,3};
		assertTrue(Arrays.equals(LTMUtils.subtract(a, b), new double[] {0,0,0}));
	}

}
