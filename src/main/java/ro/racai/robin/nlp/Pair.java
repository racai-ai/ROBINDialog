package ro.racai.robin.nlp;

/**
 * A generic Pair class, taking two types T and S.
 */
public class Pair<T, S> {
    private T memberT;
    private S memberS;

    public Pair(T t, S s) {
        memberT = t;
        memberS = s;
    }

    public T getFirstMember() {
        return memberT;
    }

    public S getSecondMember() {
        return memberS;
    }
}
