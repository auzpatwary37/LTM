package utils;

import java.util.Arrays;

import org.apache.commons.math.linear.MatrixUtils;

public class GeneralTesting {
public static void main(String[] args) {
	double[] a = new double[] {0,0,0};
	System.out.println(Arrays.toString(MatrixUtils.createRealVector(a).add(new double[] {3,3,3}).getData()));
	Double b = a[2];
	b = b+1;
	System.out.println(Arrays.toString(a));
}
}
