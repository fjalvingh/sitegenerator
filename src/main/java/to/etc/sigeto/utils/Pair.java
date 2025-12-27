package to.etc.sigeto.utils;

public class Pair<A, B> {
	private final A m_a;

	private final B m_b;

	public Pair(A a, B b) {
		m_a = a;
		m_b = b;
	}

	public A getA() {
		return m_a;
	}

	public B getB() {
		return m_b;
	}

	public A getFirst() {
		return m_a;
	}

	public B getSecond() {
		return m_b;
	}
}
