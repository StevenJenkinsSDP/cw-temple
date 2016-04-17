package student;

import game.EscapeState;
import game.ExplorationState;
import game.Node;
import game.NodeStatus;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;
import java.util.Stack;
import game.Tile;
import java.util.Set;
import java.lang.Long;

import java.util.Collection;

public class Explorer {

    private Stack<Node> escapeStack = new Stack();
   // ArrayList<Long> mazeMap = new ArrayList<Long>();
    ArrayList<Node> nodesVisited = new ArrayList<Node>();
    ArrayList<Long> nodeStatusVisited = new ArrayList<Long>();
    Stack<Node> exitStack = new Stack();
    Stack<Node> tempStack = new Stack();
    Stack<Node> wanderStack = new Stack();
    Stack<Node> bestEscapePathStack = new Stack();
    Stack<Node> moveAroundStack = new Stack();
    int totalEscapeTimeAllowed = 0;
    boolean outOfTime = false;


    /**
     * Explore the cavern, trying to find the orb in as few steps as possible.
     * Once you find the orb, you must return from the function in order to pick
     * it up. If you continue to move after finding the orb rather
     * than returning, it will not count.
     * If you return from this function while not standing on top of the orb,
     * it will count as a failure.
     * <p>
     * There is no limit to how many steps you can take, but you will receive
     * a score bonus multiplier for finding the orb in fewer steps.
     * <p>
     * At every step, you only know your current tile's ID and the ID of all
     * open neighbor tiles, as well as the distance to the orb at each of these tiles
     * (ignoring walls and obstacles).
     * <p>
     * To get information about the current state, use functions
     * getCurrentLocation(),
     * getNeighbours(), and
     * getDistanceToTarget()
     * in ExplorationState.
     * You know you are standing on the orb when getDistanceToTarget() is 0.
     * <p>
     * Use function moveTo(long id) in ExplorationState to move to a neighboring
     * tile by its ID. Doing this will change state to reflect your new position.
     * <p>
     * A suggested first implementation that will always find the orb, but likely won't
     * receive a large bonus multiplier, is a depth-first search.
     *
     * @param state the information available at the current state
     */
    public void explore(ExplorationState state) {
        Stack<Long> explorePath = new Stack<Long>();
        while (state.getDistanceToTarget() > 0) {
            Long currentNodeStatus = state.getCurrentLocation();
            nodeStatusVisited.add(currentNodeStatus);
            visitNextNodeStatus(getNeighboursNotBeenTo(state),state,explorePath,currentNodeStatus);
        }
    }

    public ArrayList<NodeStatus> getNeighboursNotBeenTo(ExplorationState state) {
        Collection<NodeStatus> neighbours = state.getNeighbours();
        ArrayList<NodeStatus> options = new ArrayList<NodeStatus>();
        for (NodeStatus n: neighbours) {
            if (!nodeStatusVisited.contains(n.getId())) {
                options.add(n);
            }
        }
        return options;
    }

    public void visitNextNodeStatus(ArrayList<NodeStatus> options, ExplorationState state,Stack<Long> explorePath,Long current) {
        NodeStatus visitNext = null;
        if (!options.isEmpty()) {
            for (NodeStatus n: options) {
                if (visitNext == null ) {
                    visitNext = n;
                } else if (n.getDistanceToTarget() < visitNext.getDistanceToTarget()) {
                    visitNext = n;
                }
            }
            state.moveTo(visitNext.getId());
            explorePath.push(current);
        } else {
            state.moveTo(explorePath.pop());
        }
    }


    public int getWightedTimeRemaining(Stack<Node> n) {
        int result = 0;
        for(int i = 0; i< n.size()-1; i++) {
            result =  result + n.get(i).getEdge(n.get(i+1)).length;
        }
        return result;
    }

    /**
     * Escape from the cavern before the ceiling collapses, trying to collect as much
     * gold as possible along the way. Your solution must ALWAYS escape before time runs
     * out, and this should be prioritized above collecting gold.
     * <p>
     * You now have access to the entire underlying graph, which can be accessed through EscapeState.
     * getCurrentNode() and getExit() will return you Node objects of interest, and getVertices()
     * will return a collection of all nodes on the graph.
     * <p>
     * Note that time is measured entirely in the number of steps taken, and for each step
     * the time remaining is decremented by the weight of the edge taken. You can use
     * getTimeRemaining() to get the time still remaining, pickUpGold() to pick up any gold
     * on your current tile (this will fail if no such gold exists), and moveTo() to move
     * to a destination node adjacent to your current node.
     * <p>
     * You must return from this function while standing at the exit. Failing to do so before time
     * runs out or returning from the wrong location will be considered a failed run.
     * <p>
     * You will always have enough time to escape using the shortest path from the starting
     * position to the exit, although this will not collect much gold.
     *
     * @param state the information available at the current state
     */
    public void escape(EscapeState state) {
        //find the path with the most gold
        findBestEscapePath(state);
        //move through this path
        followExitPath(bestEscapePathStack,state);
    }

    public void findBestEscapePath(EscapeState state) {
        totalEscapeTimeAllowed = state.getTimeRemaining();
        System.out.println(totalEscapeTimeAllowed);
        Node startNode = state.getCurrentNode();
        //find 100 paths that exit in time and take the path with the most gold
        for (int i = 0; i < 100; i++) {
            findEscapePath(startNode,state);
            checkAndReplaceBestEscapeStack();
            clearAll();
        }
    }

    public void checkAndReplaceBestEscapeStack() {
        Stack<Node> newEscapePathStack = createNewEscapePathStack();
        //choose the path with the most gold
        if (bestEscapePathStack.isEmpty()) {
            bestEscapePathStack = newEscapePathStack;
        } else {
            int bestEscapePathGold = getGoldFromStack(bestEscapePathStack);
            int newEscapePathGold = getGoldFromStack(newEscapePathStack);

            if (newEscapePathGold > bestEscapePathGold) {
                bestEscapePathStack = newEscapePathStack;
            }
        }
    }

    public Stack<Node> createNewEscapePathStack() {
        Stack<Node> result = new Stack<Node>();
        for (int j = 0; j < wanderStack.size(); j++) {
            result.push(wanderStack.get(j));
        }
        for (int j = 1; j < exitStack.size(); j++) {
            result.push(exitStack.get(j));
        }
        return result;
    }

    public void clearAll() {
        wanderStack.removeAllElements();
        exitStack.removeAllElements();
        moveAroundStack.removeAllElements();
        nodesVisited.clear();
        outOfTime = false;
    }

    public void findEscapePath(Node n, EscapeState state) {
        Node currentNode = n;
        ArrayList<Node> moveOptions;
        wanderStack.push(n);
        moveAroundStack.push(n);
        Stack<Node> tempExitStack = new Stack<Node>();
        while(!outOfTime) {
            Node previousNode;
            if (moveAroundStack.size()>=2) {
                previousNode = moveAroundStack.get(moveAroundStack.size()-2);
            } else {
                previousNode = currentNode;
            }
            moveOptions = getNextMoveOptions(currentNode, previousNode);
            exitStack = tempExitStack;
            if (moveOptions.isEmpty()) {
                moveAroundStack.pop();
                wanderStack.push(moveAroundStack.peek());
                currentNode = moveAroundStack.peek();

            } else {
                Node nextMove = chooseNextMove(moveOptions);
                moveAroundStack.push(nextMove);
                wanderStack.push(nextMove);
                nodesVisited.add(currentNode);
                currentNode = nextMove;
            }
            tempExitStack = getShortestExitPath(currentNode,state.getExit(),state);;
            outOfTime = checkIfOutOfTime(wanderStack, tempExitStack);
        }
        //last move led to out of time so step back one
        wanderStack.pop();
    }

    public Node chooseNextMove(ArrayList<Node> options) {
        Random r = new Random();
        int rand = r.nextInt(options.size());
        Node result = options.get(rand);
        return result;
    }

    public Stack<Node> setExitStack(Node n, EscapeState state) {
        totalEscapeTimeAllowed = state.getTimeRemaining();
        boolean exitPathWithinTime = false;
        Stack<Node> result = new Stack<Node>();
        //always return an exit path that is within the time limit
        while(!exitPathWithinTime) {
            result = getShortestExitPath(n,state.getExit(),state);
            int newExitTime = getWeightedTimeFromStack(result);
            System.out.println("total time allowed" + totalEscapeTimeAllowed);
            System.out.println("new exit time" + newExitTime);
            if (totalEscapeTimeAllowed > newExitTime) {
                exitPathWithinTime = true;
            }
        }
        return result;
    }

    public ArrayList<Node> getNextMoveOptions(Node curr, Node prev) {
        ArrayList<Node> result = new ArrayList();
        Set<Node> neighbours = curr.getNeighbours();
        Long longPrev = prev.getId();
        for(Node n:neighbours) {
            Long longCurr = n.getId();
            int sameNode = longCurr.compareTo(longPrev);
            if( sameNode != 0 && !nodesVisited.contains(n.getId())) {
                result.add(n);
            }
        }
        return result;
    }

    public Node getPreviousNode(Node n) {
        if (tempStack.isEmpty()) {
            return n;
        } else {
            return tempStack.get(tempStack.size()-1);
        }
    }

    public Node getPreviousNode2(Node n) {
        if (tempStack.isEmpty()) {
            return n;
        } else {
            return tempStack.get(tempStack.size()-2);
        }
    }

    public boolean checkIfOutOfTime( Stack<Node> wander, Stack<Node> exit ) {
        boolean result = false;
        int wanderStackTime = getWeightedTimeFromStack(wander);
        int exitStackTime = getWeightedTimeFromStack(exit);
        int wanderToExitTime = getWeightedTimeFromNodes(wander.peek(), exit.get(0));
      //  int nextMoveTime = getWeightedTimeFromNodes(src, dest);
        if(totalEscapeTimeAllowed < (wanderStackTime + exitStackTime + wanderToExitTime)) {
            result = true;
        }
        return result;
    }

    public int getWeightedTimeFromStack(Stack<Node> n) {
        //System.out.println("size of stack" + tempStack.size());
        int result = 0;
        if (n.size() > 1) {
            for(int i = 0; i< n.size()-1; i++) {
                result =  result + n.get(i).getEdge(n.get(i+1)).length();
            }
        }
        return result;
    }

    public int getWeightedTimeFromNodes(Node src, Node dest) {
        int result = 0;
        Long srcLong = src.getId();
        Long destLong = dest.getId();
        if(srcLong.compareTo(destLong) == 0) {
            return result;
        } else {
            result =  src.getEdge(dest).length;
            return result;
        }

    }

    public int getGoldFromStack(Stack<Node> s) {
        int result = 0;
        ArrayList<Long> visited = new ArrayList<Long>();
        for(Node n: s) {
            if (!visited.contains(n.getId())) {
                result = result + n.getTile().getGold();
                visited.add(n.getId());
            }
        }
        return result;
    }

    public void followExitPath(Stack<Node> s, EscapeState state) {
        for (int i = 1; i < s.size(); i++) {
            state.moveTo(s.get(i));
            if (state.getCurrentNode().getTile().getGold() > 0) {
                state.pickUpGold();
            }
        }
    }

    public Stack<Node> getShortestExitPath(Node c, Node e, EscapeState state) {

        Collection<Node> allNodes = state.getVertices();
        Node exitNode = e;
        Tile exitTile = exitNode.getTile();
        int exitRow = exitTile.getRow();
        int exitColumn = exitTile.getColumn();


        long exitPathDistance = calculateDistance(c,exitNode);
        Node exitPathNode = c;
        Stack<Node> exitStack = new Stack<Node>();
        ArrayList<Long> mazeMap2 = new ArrayList<Long>();

        //This calculates the number of steps to exit
        while (calculateDistance(exitPathNode,exitNode) != 0) {

            //get current location
            Node currentNode = exitPathNode;
            mazeMap2.add(currentNode.getId());
            //calculate distance to exit using tile coordinates
            long distanceToExit = calculateDistance(currentNode, exitNode);
            //get neighbours
            ArrayList<Node> neighbours = new ArrayList();
            for (Node n : allNodes) {
                long neighbourDistance = calculateDistance(currentNode, n);
                if (neighbourDistance == 1) {
                    neighbours.add(n);
                }
            }
            //nodes visited

            //nodes not visited
            ArrayList<Node> nodesNotVisited = new ArrayList();
            //get neighbours not visited
            for (Node n : neighbours) {
                if (!mazeMap2.contains(n.getId())) {
                    nodesNotVisited.add(n);
                }
            }
            //if blank square, choose shortest distance, if no blank square go back
            Node visitNext = null;
            if (!nodesNotVisited.isEmpty()) {
                for (Node n : nodesNotVisited) {
                    if (visitNext == null) {
                        visitNext = n;
                    } else if (calculateDistance(n, exitNode) < calculateDistance(visitNext, exitNode)) {
                        visitNext = n;
                    }
                }
                exitStack.push(currentNode);
                exitPathNode = visitNext;
            } else {
                exitPathNode = exitStack.pop();
            }
        }
    exitStack.push(e);
        return exitStack;
    }

    public long calculateDistance(Node n1, Node n2) {
        Tile n1Tile = n1.getTile();
        Tile n2Tile = n2.getTile();
        int n1Row = n1Tile.getRow();
        int n1Column = n1Tile.getColumn();
        int n2Row = n2Tile.getRow();
        int n2Column = n2Tile.getColumn();
        long distanceRow = n1Row-n2Row;
        long distanceColumn = n1Column-n2Column;
        long distance = Math.abs(distanceRow) + Math.abs(distanceColumn);
        return distance;
    }

}
