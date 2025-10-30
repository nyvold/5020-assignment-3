package protocol;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * This class prints the the response of the lookup. This is class prints the names of the nodes whose finger table
 * has been checked, the destination node index, it's name and hop count.
 */
public class LookUpResponse {
    public LinkedHashSet<String> peers_looked_up;
    public int node_index;
    public String node_name;

    public LookUpResponse(LinkedHashSet<String> peers_looked_up, int node_index,String node_name){
        this.peers_looked_up = peers_looked_up;
        this.node_index = node_index;
        this.node_name = node_name;
    }

    public String toString(){
        String line = "----------";
        String result = "";
        result = result.concat(line);
        result = result.concat("LOOKUP RESPONSE");
        result = result.concat(line + "\n");
        result = result.concat("peers : ");
        for(String peer: peers_looked_up){
            result = result.concat(peer+ " ");
        }
        result=result.concat(" hop count : "+peers_looked_up.size());
        result = result.concat(" node index : "+node_index);
        result = result.concat(" node name : "+node_name);
        result = result.concat("\n" + line + "END LOOKUP RESPONSE" + "\n");
        return  result;
    }

    public List<String> getVisitedPeers() {
        return new ArrayList<>(peers_looked_up);
    }

    public int getHopCount() {
        return peers_looked_up.size();
    }

    public int getNodeIndex() {
        return node_index;
    }

    public String getNodeName() {
        return node_name;
    }
}
