package utils;

import java.io.Serializable;


public final class TuplesOfThree<A, B, C> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static <A, B, C> TuplesOfThree<A, B, C> of(final A first, final B second, final C third) {
		return new TuplesOfThree<>(first, second, third);
	}

	/**
	 * First entry of the tuple
	 */
	private final A first;
	/**
	 * Second entry of the tuple
	 */
	private final B second;
	
	/**
	 * Third entry of the tuple
	 */
	private final C third;
	/**
	 * Creates a new tuple with the two entries.
	 * @param first
	 * @param second
	 */
	public TuplesOfThree(final A first, final B second, final C third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public A getFirst() {
		return this.first;
	}

	public B getSecond() {
		return this.second;
	}
	
	public C getThird() {
		return this.third;
	}
	
	/**
	 * This method only works if this TuplesOfThree has instances A->Double,B->Double and C->double[]
	 * @param a the tuples of three to sum
	 * @return
	 */
	public TuplesOfThree<Double,Double,double[]> sum(TuplesOfThree<Double,Double,double[]>a){
		if(!(first instanceof Double))throw new IllegalArgumentException("the first item is not a Double. Method will exit!!!");
		else if(!(second instanceof Double))throw new IllegalArgumentException("the second item is not a Double. Method will exit!!!");
		else if(!(third instanceof double[]) || a.getThird().length!=((double[])this.third).length)throw new IllegalArgumentException("the third item is not a double[] or the dimension is not same. Method will exit!!!");
		double[] o = new double[a.third.length];
		for(int i=0;i<o.length;i++) {
			o[i]=((double[])third)[i]+a.getThird()[i];
		}
		return new TuplesOfThree<Double,Double,double[]>((double)first+a.getFirst(),(double)second+a.getSecond(),o);
	}
	
	/**
	 * This method only works if this TuplesOfThree has instances A->Double,B->Double and C->double[]
	 * @param a the tuples of three to subtract
	 * @return
	 */
	public TuplesOfThree<Double,Double,double[]> subtract(TuplesOfThree<Double,Double,double[]>a){
		if(!(first instanceof Double))throw new IllegalArgumentException("the first item is not a Double. Method will exit!!!");
		else if(!(second instanceof Double))throw new IllegalArgumentException("the second item is not a Double. Method will exit!!!");
		else if(!(third instanceof double[]) || a.getThird().length!=((double[])this.third).length)throw new IllegalArgumentException("the third item is not a double[] or the dimension is not same. Method will exit!!!");
		double[] o = new double[a.third.length];
		for(int i=0;i<o.length;i++) {
			o[i]=((double[])third)[i]-a.getThird()[i];
		}
		return new TuplesOfThree<Double,Double,double[]>((double)first-a.getFirst(),(double)second-a.getSecond(),o);
	}
}
