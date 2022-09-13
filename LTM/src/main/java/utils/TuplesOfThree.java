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

}
