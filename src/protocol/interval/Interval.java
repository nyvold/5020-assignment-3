package protocol.interval;

public abstract class Interval {
    public abstract boolean contains(int x, int a, int b, int ringLength);
    private int start, end;
    public Interval(int start, int end){
        this.start = start;
        this.end = end;

    }
}
