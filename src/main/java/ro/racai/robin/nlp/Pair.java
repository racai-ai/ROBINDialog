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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Pair<?, ?>) {
            Pair<?, ?> pbj = (Pair<?, ?>) obj;

            return pbj.getFirstMember().equals(getFirstMember())
                    && pbj.getSecondMember().equals(getSecondMember());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getFirstMember().hashCode() ^ getSecondMember().hashCode();
    }
}
