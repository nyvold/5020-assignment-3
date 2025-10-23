package protocol;


import crypto.ConsistentHashing;
import p2p.NetworkInterface;
import p2p.NodeInterface;

import java.util.*;

/**
 * This class implements the chord protocol. The protocol is tested using the custom built simulator.
 */
public class ChordProtocol implements Protocol{

    // length of the identifier that is used for consistent hashing
    public int m;

    // network object
    public NetworkInterface network;

    // consisent hasing object
    public ConsistentHashing ch;

    // key indexes. tuples of (<key name>, <key index>)
    public HashMap<String, Integer> keyIndexes;

    public TreeMap<Integer, NodeInterface> ring; // overlay network


    public ChordProtocol(int m){
        this.m = m;
        setHashFunction();
        this.keyIndexes = new HashMap<String, Integer>();
    }



    /**
     * sets the hash function
     */
    public void setHashFunction(){
        this.ch = new ConsistentHashing(this.m);
    }

  

    /**
     * sets the network
     * @param network the network object
     */
    public void setNetwork(NetworkInterface network){
        this.network = network;
    }


    /**
     * sets the key indexes. Those key indexes can be used to  test the lookup operation.
     * @param keyIndexes - indexes of keys
     */
    public void setKeys(HashMap<String, Integer> keyIndexes){
        this.keyIndexes = keyIndexes;
    }



    /**
     *
     * @return the network object
     */
    public NetworkInterface getNetwork(){
        return this.network;
    }






    /**
     * This method builds the overlay network.  It assumes the network object has already been set. It generates indexes
     *     for all the nodes in the network. Based on the indexes it constructs the ring and places nodes on the ring.
     *         algorithm:
     *           1) for each node:
     *           2)     find neighbor based on consistent hash (neighbor should be next to the current node in the ring)
     *           3)     add neighbor to the peer (uses Peer.addNeighbor() method)
     */
    public void buildOverlayNetwork(){
        LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();
        this.ring = new TreeMap<>(); 
        // a treemap sorts itself based on keys (indexs here), documentation: "The map is sorted according to the natural ordering of its keys"
        for(Map.Entry<String, NodeInterface> nodeEntry : topology.entrySet()){
            NodeInterface node = nodeEntry.getValue();
            int index = this.ch.hash(nodeEntry.getKey());
            node.setId(index);
            ring.put(index, node);
        }
        // ^^first make ring, put nodes on correct indexes in sorted way^^
        Integer[] nodeIndexes = ring.keySet().toArray(new Integer[0]);
        for(int i = 0; i < nodeIndexes.length; i++){
            int hash = nodeIndexes[i];
            NodeInterface node = ring.get(hash);
            int nextIndex = (i + 1) % nodeIndexes.length; // wraparound edgecase
            int nextHash = nodeIndexes[nextIndex];
            NodeInterface successor = ring.get(nextHash);
            node.addNeighbor("successor", successor);
        }   
        // ^^connect neighbors, handeling wraparound edgecase^^
    }






    /**
     * This method builds the finger table. The finger table is the routing table used in the chord protocol to perform
     * lookup operations. The finger table stores m-entries. Each ith entry points to the ith finger of the node.
     * Each ith entry stores the information of it's neighbor that is responsible for indexes ((n+2^i-1) mod 2^m).
     * i = 1,...,m.
     *
     *Each finger table entry should consists of
     *     1) start value - (n+2^i-1) mod 2^m. i = 1,...,m
     *     2) interval - [finger[i].start, finger[i+1].start)
     *     3) node - first node in the ring that is responsible for indexes in the interval
     */
    public void buildFingerTable() {
        int ringLength = (int)Math.pow(2, this.m);

        for(Map.Entry<Integer, NodeInterface> entry : this.ring.entrySet()){
            int hash = entry.getKey();
            NodeInterface currentNode = entry.getValue();
            NodeInterface[] ftable = new NodeInterface[this.m]; //finger table with m entries
            System.out.println(currentNode.getName() + " (at position " + hash + ") finger table:");
            // calculate each finger i=1 to m
            for(int i = 1; i <= this.m; i++){
                int power = (int) Math.pow(2, i-1);
                int start = (hash + power) % ringLength;
                
                Map.Entry<Integer, NodeInterface> successor = this.ring.ceilingEntry(start); // .ceilingEntry(start) finds next node given 'start' index, null otherwise
                if(successor == null){ // wraparound case, return first key found
                    successor = this.ring.firstEntry();
                }
                NodeInterface sNode =  successor.getValue();
                ftable[i-1] = sNode;
                
                System.out.println("  Finger " + i + " -> " + ftable[i-1].getName() + " (at position " + ftable[i-1].getId() + ")");
            }
            System.out.println(currentNode.getName() + " finger table completed.");
            currentNode.setRoutingTable(ftable);
        }
    }



    /**
     * This method performs the lookup operation.
     *  Given the key index, it starts with one of the node in the network and follows through the finger table.
     *  The correct successors would be identified and the request would be checked in their finger tables successively.
     *   Finally the request will reach the node that contains the data item.
     *
     * @param keyIndex index of the key
     * @return names of nodes that have been searched and the final node that contains the key
     */
    public LookUpResponse lookUp(int keyIndex){
        LinkedHashSet<String> peersLookedUp = new LinkedHashSet<>();
        NodeInterface currentNode = this.ring.firstEntry().getValue();
        
        while(true) {
            // Add current node to visited list
            peersLookedUp.add(currentNode.getName());
            
            // Check if current node is responsible for the key
            // A node is responsible if keyIndex is between current node and its successor
            NodeInterface successor = currentNode.getSuccessor();
            int currentId = currentNode.getId();
            int successorId = successor.getId();
            
            boolean responsible;
            if(currentId < successorId) {
                // Normal case: currentId < keyIndex <= successorId
                responsible = (keyIndex > currentId && keyIndex <= successorId);
            } else {
                // Wraparound case: keyIndex > currentId OR keyIndex <= successorId
                responsible = (keyIndex > currentId || keyIndex <= successorId);
            }
            
            if(responsible) {
                // Found responsible range - now find actual node with data
                // Check all nodes in the network topology to find who actually has the key
                LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();
                for(Map.Entry<String, NodeInterface> entry : topology.entrySet()) {
                    NodeInterface node = entry.getValue();
                    Object data = node.getData();
                    if(data != null && ((LinkedHashSet<Object>) data).contains(keyIndex)) {
                        // Found the actual node with the data
                        return new LookUpResponse(peersLookedUp, node.getId(), node.getName());
                    }
                }
                
                // If no node has the data, return the successor as responsible
                return new LookUpResponse(peersLookedUp, successor.getId(), successor.getName());
            }
            
            // Key not at current node, use finger table to jump closer
            NodeInterface[] fingerTable = (NodeInterface[]) currentNode.getRoutingTable();
            NodeInterface nextNode = successor; // Default to successor
            
            // Find the closest preceding finger
            for(int i = m - 1; i >= 0; i--) {
                NodeInterface finger = fingerTable[i];
                int fingerId = finger.getId();
                
                // Check if this finger is between current and keyIndex
                boolean between;
                if(currentId < keyIndex) {
                    // Normal case: currentId < fingerId < keyIndex
                    between = (fingerId > currentId && fingerId < keyIndex);
                } else {
                    // Wraparound case: fingerId > currentId OR fingerId < keyIndex
                    between = (fingerId > currentId || fingerId < keyIndex);
                }
                
                if(between) {
                    nextNode = finger;
                    break;
                }
            }
            
            // Prevent infinite loops
            if(nextNode.equals(currentNode)) {
                // Search all nodes for the key as last resort
                LinkedHashMap<String, NodeInterface> topology = this.network.getTopology();
                for(Map.Entry<String, NodeInterface> entry : topology.entrySet()) {
                    NodeInterface node = entry.getValue();
                    Object data = node.getData();
                    if(data != null && ((LinkedHashSet<Object>) data).contains(keyIndex)) {
                        return new LookUpResponse(peersLookedUp, node.getId(), node.getName());
                    }
                }
                return new LookUpResponse(peersLookedUp, currentNode.getId(), currentNode.getName());
            }
            
            // Move to next node
            currentNode = nextNode;
        }
    }
}
