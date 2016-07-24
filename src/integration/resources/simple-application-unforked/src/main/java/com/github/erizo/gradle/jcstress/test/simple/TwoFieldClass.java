package pl.erizo.gradle.jcstress.reordering;

public class TwoFieldClass {

    long x;
    long y;

    public TwoFieldClass(int x, long y) {
        this.x = x;
        this.y = y;
    }

    public long getX() {
        return x;
    }

    public void setX(long x) {
        this.x = x;
    }

    public long getY() {
        return y;
    }

    public void setY(long y) {
        this.y = y;
    }
}
