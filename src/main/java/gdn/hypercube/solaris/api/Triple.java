package gdn.hypercube.solaris.api;

public class Triple<A, B, C> {
    private A left;
    private B middle;
    private C right;

    public Triple(final A left, final B middle, final C right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public A getLeft() {
        return this.left;
    }

    public void setLeft(final A left) {
        this.left = left;
    }

    public B getMiddle() {
        return this.middle;
    }

    public void setMiddle(final B middle) {
        this.middle = middle;
    }

    public C getRight() {
        return this.right;
    }

    public void setRight(final C right) {
        this.right = right;
    }
}