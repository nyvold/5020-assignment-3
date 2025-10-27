package protocol.interval;

public class OpenClosedInterval extends Interval{

    public OpenClosedInterval(int start, int end){
        super(start, end);
    }
    
    @Override
    public boolean contains(int x, int a, int b, int ringLength){
        x = ((x % ringLength) + ringLength) % ringLength;
        a = ((a % ringLength) + ringLength) % ringLength;
        b = ((b % ringLength) + ringLength) % ringLength;
        if (a == b) return true; // full ring
        if (a < b) return (x > a && x <= b);
        // wrap-around
        return (x > a) || (x <= b);
    }

}
