package protocol.interval;

public class OpenOpenInterval extends Interval{
    
    public OpenOpenInterval(int start, int end){
        super(start, end);
    }

    @Override
    public boolean contains(int x, int a, int b, int ringLength){
        x = ((x % ringLength) + ringLength) % ringLength;
        a = ((a % ringLength) + ringLength) % ringLength;
        b = ((b % ringLength) + ringLength) % ringLength;
        if (a == b) return false; // empty for strict open interval when identical
        if (a < b) return (x > a && x < b);
        // wrap-around
        return (x > a) || (x < b);
    }
    
}
