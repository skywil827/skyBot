package myBot;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

import java.util.*;

import static rts.PhysicalGameState.TERRAIN_WALL;

public class myBot extends AIWithComputationBudget {
    UnitTypeTable m_utt;
    List<Integer> _locationsTaken;
    long _startCycleMilli;
    long _latestTsMilli;
    List<Unit> bases;
    List<Unit> barracks;
    List<Unit> workers;
    List<Unit> heavy;
    List<Unit> light;
    List<Unit> ranged;
    List<Unit> resource;
    List<Unit> enemy_workers;

    List<Unit> enemy_barracks;
    List<Unit> enemy_bases;

    List<Unit> enemy_heavy;
    List<Unit> enemy_light;
    List<Unit> enemy_ranged;

    List<Unit> enemyUnit;
    List<Unit> fighterUnits;

    Random r;
    PlayerAction pa;
    UnitAction ua;
    GameState gameState;
    PhysicalGameState pgs;

    int resourcesUsed = 0;

    public class Position {
        int x;
        int y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    };

    public myBot(UnitTypeTable utt) {
        super(-1, -1);
        m_utt = utt;
    }

    @Override
    public void reset() {

    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        pa = new PlayerAction();
        r = new Random();
        _latestTsMilli = 0;
        gameState = gs;
        _locationsTaken = new ArrayList<>();
        pgs = gs.getPhysicalGameState();

        bases = new ArrayList<>();
        barracks = new ArrayList<>();
        workers = new ArrayList<>();
        enemy_workers = new ArrayList<>();
        enemy_bases = new ArrayList<>();
        enemy_barracks = new ArrayList<>();
        heavy = new ArrayList<>();
        light = new ArrayList<>();
        ;
        ranged = new ArrayList<>();
        ;
        resource = new ArrayList<>();
        ;
        enemy_heavy = new ArrayList<>();
        ;
        enemy_light = new ArrayList<>();
        ;
        enemy_ranged = new ArrayList<>();
        ;
        enemyUnit = new ArrayList<>();
        fighterUnits = new ArrayList<>();
        resourcesUsed = 0;

        saveAllUnitsToList();

        workeraction();

        createBigUnits();

        heavyaction();

        rangedaction();

        pa.fillWithNones(gameState, player, 1);
        return pa;
    }

    private void rangedaction() {

        Position enemyPos = null;

        if (ranged.size() > 0) {
            for (Unit a : ranged) {

                enemyPos = getnearstEnemyPosition(a);

                if (!busy(a) && inAttackRange(a, pgs.getUnitAt(enemyPos.x, enemyPos.y))) {
                    attackEnemy(a, enemyPos);
                }

                if (!busy(a)) {
                    moveTowards(a, enemyPos);
                }

            }

        }
    }

    private void heavyaction() {
        Position enemyPos = null;

        if (heavy.size() > 0) {
            for (Unit a : heavy) {

                enemyPos = getnearstEnemyPosition(a);

                if (!busy(a) && inAttackRange(a, pgs.getUnitAt(enemyPos.x, enemyPos.y))) {
                    attackEnemy(a, enemyPos);
                }
                if (!busy(a) && enemy_ranged.size() > 0) {
                    moveTowards(a, enemyPos);
                } else if (!busy(a) && enemy_bases.size() > 0) {
                    moveTowards(a, new Position(enemy_bases.get(0).getX(), enemy_bases.get(0).getY()));
                } else if (!busy(a)) {
                    moveTowards(a, enemyPos);
                }

            }

        }
    }

    // this function calculates the distance between the enemy unit and our unit
    double squareDist(Position p, Position u) {
        int dx = p.getX() - u.getX();
        int dy = p.getY() - u.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // this function checks if an enemyunit is in attack-range of attacker
    boolean inAttackRange(Unit attacker, Unit runner) {
        return squareDist(new Position(attacker.getX(), attacker.getY()),
                new Position(runner.getX(), runner.getY())) <= attacker.getAttackRange();
    }

    // this function calculates the Enemy-Unit which is nearest to Unit.
    private Position getnearstEnemyPosition(Unit u) {
        Position shortestDistance = null;// position to save the shortestdistance
        int shortestDistanceValue = Integer.MAX_VALUE; // to calculate the shortestdistance

        for (Unit unit : enemyUnit) {// loop through the enemyworker list
            int distance = Math.abs(u.getX() - unit.getX()) + Math.abs(u.getY() - unit.getY());
            // and calculate the distance

            if (distance < shortestDistanceValue) { // if the current distance is less than the shortestdistance
                shortestDistance = new Position(unit.getX(), unit.getY()); // save this position
                shortestDistanceValue = distance; // and change the distance to the shortestdistance
            }

        }
        return shortestDistance;
    }

    // This function checks if Unit u is currently busy executing actions.
    boolean busy(Unit u) {
        if (pa.getAction(u) != null)
            return true;
        UnitActionAssignment aa = gameState.getActionAssignment(u);
        return aa != null;
    }

    private void workeraction() {

        List<Position> resources = getResourcePositions(); // List with positions of the resources
        Position base = null;
        int direction = r.nextInt(1, 3);
        int resourceInBase = gameState.getPlayer(0).getResources();
        Unit close;

        // produce new units, for example workers in the base
        if (bases.size() > 0 && !busy(bases.get(0)) && workers.size() < 2) {
            produceNewUnits(bases.get(0), m_utt.getUnitType("Worker"), 3);

        }

        if (resource.isEmpty() && !busy(bases.get(0)) || enemy_bases.isEmpty() && !busy(bases.get(0))) {
            produceNewUnits(bases.get(0), m_utt.getUnitType("Worker"), direction);

        }

        if (pgs.getWidth() == 8 && bases.size() > 0 && !busy(bases.get(0))) {
            produceNewUnits(bases.get(0), m_utt.getUnitType("Worker"), direction);

        }
        if ((enemy_workers.size() > 2 || enemy_light.size() > 0) && bases.size() > 0 && !busy(bases.get(0))) {
            produceNewUnits(bases.get(0), m_utt.getUnitType("Worker"), direction);

        }

        if (!bases.isEmpty()) {
            base = new Position(bases.get(0).getX(), bases.get(0).getY()); // set position of base
        }

        for (Unit a : workers) {

            if (!busy(a) && pgs.getWidth() == 8 && workers.get(0) != a) {// if the map is selected 8x8, then push with
                                                                         // workers
                Position enemy = getnearstEnemyPosition(a);
                if (!busy(a) && inAttackRange(a, pgs.getUnitAt(enemy.x, enemy.y))) {
                    attackEnemy(a, enemy);
                } else if (!busy(a) && enemy_bases.size() > 0) {
                    moveTowards(a, new Position(enemy_bases.get(0).getX(), enemy_bases.get(0).getY()));
                } else if (!busy(a) && enemyUnit.size() > 0) {
                    moveTowards(a, enemy);

                }
            }

            // first and second worker just to farm resources

            if (workers.get(0) == a || workers.get(1) == a) {
                // only worker 0 and 1 do harvest and return resources to base

                if (!busy(a) && a.getResources() == 1 && !bases.isEmpty()) {
                    havestAction(a, base); // move unit to base
                }
                if (!busy(a) && a.getResources() == 1 && !bases.isEmpty()) {
                    returnResource(a, base); // put resource in base
                }
                if (!busy(a) && !resources.isEmpty()) {

                    havestAction(a, resources.get(resources.size() - 1));

                    // move unit to the position of the resource
                }
                if (!busy(a) && !resources.isEmpty()) {
                    harvest(a, resources.get(resources.size() - 1)); // tell unit to harvest the resource
                }

                if (resourceInBase > 4 && barracks.size() < 1 && pgs.getWidth() != 8) {
                    createBarracks(a);
                }

            }

            // when the enemy is increasing the workers fast then create workers to defend
            // the base

            if (!busy(a) && resource.isEmpty() || !busy(a) && enemy_bases.isEmpty()
                    || !busy(a) && enemy_workers.size() > 0) {
                Position enemy = getnearstEnemyPosition(a);
                if (!busy(a) && inAttackRange(a, pgs.getUnitAt(enemy.x, enemy.y))) {
                    attackEnemy(a, enemy);
                }
                if (!busy(a) && enemy_bases.size() > 0) {
                    moveTowards(a, new Position(enemy_bases.get(0).getX(), enemy_bases.get(0).getY()));
                } else if (!busy(a)) {
                    moveTowards(a, enemy);
                }
            }
        }
    }

    private boolean createBigUnits() {
        int random;
        random = r.nextInt(1, 3); // just the random number
        // to create the heavy units up, down, left or right of the barracks
        Position pos;
        UnitAction ua = null;
        UnitType ut;

        if (gameState.getPlayer(0).getResources() - resourcesUsed < m_utt.getUnitType("Heavy").cost) {
            return false;
        }
        if (barracks.size() < 1 || bases.size() < 1) {
            return false;

        }

        pos = futurePosition(barracks.get(0), random);
        if (pos == null) {
            return false;
        }

        if (!checkPositionIsFree(pos)) {
            return false;
        }

        if (enemy_workers.size() > 2 || enemy_light.size() > 1) {
            ua = new UnitAction(UnitAction.TYPE_PRODUCE, random, m_utt.getUnitType("Ranged"));
        } else {

            ua = new UnitAction(UnitAction.TYPE_PRODUCE, random, m_utt.getUnitType("Heavy"));
        }

        if (ua == null) {
            return false;
        }
        ut = ua.getUnitType();

        if (!gameState.isUnitActionAllowed(barracks.get(0), ua)) {
            return false;
        }

        if (!busy(barracks.get(0))) {
            pa.addUnitAction(barracks.get(0), ua);
            resourcesUsed += ut.cost;
            return true;
        }

        return false;
    }

    private void createBarracks(Unit a) {

        // create the barracks to the position of base by adding two to x and y
        int dir = 2;
        // when the worker is at this position then create the barracks
        if (checkPositionIsFree(new Position(a.getX(), a.getY() + 1))) {
            if (bases.size() > 0 && a.getY() == bases.get(0).getY()) {
                produceNewUnits(a, m_utt.getUnitType("Barracks"), dir);
            }
        }

    }

    // returns a list with all positions of the resources
    List<Position> getResourcePositions() {
        List<Position> resourcePositions = new ArrayList<>();
        for (Unit u : resource) {
            Position pos = new Position(u.getX(), u.getY());
            resourcePositions.add(pos);
        }
        return resourcePositions;
    }

    // method for harvesting resources
    private boolean harvest(Unit a, Position p) {
        Position workerPos = new Position(a.getX(), a.getY()); // get the position of the worker
        int dir = toDir(workerPos, p); // get the direction in which the unit has to face to harvest
        UnitAction ua = new UnitAction(UnitAction.TYPE_HARVEST, dir);

        if (!gameState.isUnitActionAllowed(a, ua)) {
            return false;
        }
        pa.addUnitAction(a, ua);
        return true;
    }

    // return the collected resource to the base
    private boolean returnResource(Unit a, Position base) {
        Position workerPos = new Position(a.getX(), a.getY()); // get position of the worker
        int dir = toDir(workerPos, base); // get the direction of the base
        UnitAction ua = new UnitAction(UnitAction.TYPE_RETURN, dir);

        if (!gameState.isUnitActionAllowed(a, ua)) {
            return false;
        }
        pa.addUnitAction(a, ua);
        return true;
    }

    // check in which direction a destination is
    int toDir(Position src, Position dst) {
        int dx = dst.getX() - src.getX(); // get difference of the x values
        int dy = dst.getY() - src.getY(); // get difference of the y values
        int dirX;
        int dirY;
        // check which direction is needed
        if (dx > 0) {
            dirX = UnitAction.DIRECTION_RIGHT;
        } else {
            dirX = UnitAction.DIRECTION_LEFT;
        }
        if (dy > 0) {
            dirY = UnitAction.DIRECTION_DOWN;
        } else {
            dirY = UnitAction.DIRECTION_UP;
        }
        // if unit is in the adjacent square dx or dy is 1/0
        // return the direction with the value 1
        if (Math.abs(dx) > Math.abs(dy)) {
            return dirX;
        }
        return dirY;
    }

    // this function is to move the workers to the resource position.
    private boolean havestAction(Unit a, Position p) {

        UnitAction ua;

        if ((p.getX() > a.getX())) {
            ua = new UnitAction(1, UnitAction.DIRECTION_RIGHT);
            if (gameState.isUnitActionAllowed(a, ua)) {
                pa.addUnitAction(a, ua);
                return true;
            }

        }

        if (p.getX() < a.getX()) {
            ua = new UnitAction(1, UnitAction.DIRECTION_LEFT);
            if (gameState.isUnitActionAllowed(a, ua)) {
                pa.addUnitAction(a, ua);
                return true;
            }

        }

        if (p.getY() < a.getY()) {
            ua = new UnitAction(1, UnitAction.DIRECTION_UP);
            if (gameState.isUnitActionAllowed(a, ua)) {
                pa.addUnitAction(a, ua);
                return true;
            }

        }
        if (p.getY() > a.getY()) {
            ua = new UnitAction(1, UnitAction.DIRECTION_DOWN);
            if (gameState.isUnitActionAllowed(a, ua)) {
                pa.addUnitAction(a, ua);
                return true;
            }

        }

        return false;

    }

    // this function takes the Unit a and a Position p, and attacks the Enemy which
    // is at Position p
    private boolean attackEnemy(Unit a, Position p) {

        ua = new UnitAction(5, p.getX(), p.getY());// make an attack if found unit on position x,y

        if (gameState.isUnitActionAllowed(a, ua)) {
            pa.addUnitAction(a, ua);
            return true;
        }

        return false;

    }

    boolean checkPositionIsFree(Position pos) {

        int rasterPos = pos.getX() + pos.getY() * pgs.getWidth();
        if (_locationsTaken.contains(rasterPos))
            return false;

        if (positionIsOutofBound(pos))
            return true;

        if (pgs.getUnitAt(pos.getX(), pos.getY()) != null) {
            return false;
        }
        if (pgs.getHeight() < pos.getY() && pgs.getWidth() < pos.getX()) {
            return false;
        }
        if (pgs.getTerrain(pos.getX(), pos.getY()) == 1) {
            return false;
        }
        return true;
    }

    boolean produceNewUnits(Unit u, UnitType uType, int whereToSet) {

        Position pos = futurePosition(u, whereToSet);

        // if the resources of player 0 is less than the cost of the new unit return
        // false
        if (gameState.getPlayer(0).getResources() - resourcesUsed < uType.cost) {
            return false;
        } else {
            if (!checkPositionIsFree(pos)) {
                return false;
            }
            UnitAction ua = new UnitAction(UnitAction.TYPE_PRODUCE, whereToSet, uType);
            if (!gameState.isUnitActionAllowed(u, ua))
                return false;

            pa.addUnitAction(u, ua);
            resourcesUsed += uType.cost;
            return true;
        }
    }

    private void saveAllUnitsToList() {
        for (Unit u : pgs.getUnits()) {
            if (u.getType() == m_utt.getUnitType("Worker") && u.getPlayer() == 0) {
                workers.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Worker") && u.getPlayer() == 1) {
                enemy_workers.add(u);
                enemyUnit.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Barracks") && u.getPlayer() == 0) {
                barracks.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Barracks") && u.getPlayer() == 1) {
                enemy_barracks.add(u);
                enemyUnit.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Base") && u.getPlayer() == 0) {
                bases.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Base") && u.getPlayer() == 1) {
                enemy_bases.add(u);
                enemyUnit.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Heavy") && u.getPlayer() == 0) {
                heavy.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Heavy") && u.getPlayer() == 1) {
                enemy_heavy.add(u);
                enemyUnit.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Light") && u.getPlayer() == 0) {
                light.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Light") && u.getPlayer() == 1) {
                enemy_light.add(u);
                enemyUnit.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Ranged") && u.getPlayer() == 0) {
                ranged.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Ranged") && u.getPlayer() == 1) {
                enemy_ranged.add(u);
                enemyUnit.add(u);
            }
            if (u.getType() == m_utt.getUnitType("Resource") && u.getX() == 0) {
                resource.add(u);
            }
        }
    }

    // this function saves the future position of a Unit.
    Position futurePosition(Unit a, int direction) {
        Position newPos;
        switch (direction) {
            case 0:
                newPos = new Position(a.getX(), a.getY() - 1);
                break;
            case 1:
                newPos = new Position(a.getX() + 1, a.getY());
                break;
            case 2:
                newPos = new Position(a.getX(), a.getY() + 1);
                break;
            case 3:
                newPos = new Position(a.getX() - 1, a.getY());
                break;
            default:
                return newPos = null;
        }
        return newPos;
    }

    @Override
    public AI clone() {
        return new myBot(m_utt);
    }

    // function checks if an Unit is an enemey Unit
    boolean isEnemyUnit(Unit u) {
        return u.getPlayer() >= 0; // can be neither ally ot foe
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        return new ArrayList<>();
    }

    boolean positionIsOutofBound(Position p) {
        if (p.getX() < 0 || p.getY() < 0 || p.getX() >= pgs.getWidth()
                || p.getY() >= pgs.getHeight())
            return true;
        return false;
    }

    List<Position> allPosDist(Position src, int dist) {
        List<Position> poss = new ArrayList<>();
        int sx = src.getX();
        int sy = src.getY();

        for (int x = -dist; x <= dist; x++) {
            int y = dist - Math.abs(x);
            poss.add(new Position(sx + x, sy + y));
            if (y != 0)
                poss.add(new Position(sx + x, sy - y));
        }
        return poss;
    }

    boolean isBlocked(Unit u, Position p) {
        if (positionIsOutofBound(p) || pgs.getTerrain(p.getX(), p.getY()) != PhysicalGameState.TERRAIN_NONE)
            return true;
        if (!checkPositionIsFree(new Position(p.getX(), p.getY())))
            return true;
        Unit pu = pgs.getUnitAt(p.getX(), p.getY());
        if (pu == null)
            return false;
        if (pu.getType().isResource)
            return true;
        if (!isEnemyUnit(pu))
            return true;
        if (u.getType() == m_utt.getUnitType("Worker")
                && pu.getType() != m_utt.getUnitType("Worker"))
            return true;
        return false;
    }

    UnitAction findPathAstar(Unit u, Position dst, int maxDist) {
        int proximity[][] = new int[pgs.getWidth()][pgs.getHeight()]; // Creates a 2D array for proximity distances
        for (int[] row : proximity)
            Arrays.fill(row, Integer.MAX_VALUE); // Sets all values in the array to the maximum integer value
        proximity[dst.getX()][dst.getY()] = 0; // Sets the distance to the destination position to 0
        int dist = 1; // Initializes the distance counter to 1
        List<Position> markNext = allPosDist(dst, 1); // Creates a list of positions near the destination position
        while (!markNext.isEmpty() && dist <= maxDist) { // While there are positions to mark and the maximum distance
                                                         // is not exceeded
            List<Position> queue = new ArrayList<>(); // Creates a queue for the next positions to mark
            for (Position p : markNext) { // Iterates over the positions to mark
                if (isBlocked(u, p) || proximity[p.getX()][p.getY()] != Integer.MAX_VALUE)
                    continue; // Skips blocked positions or positions already marked
                proximity[p.getX()][p.getY()] = dist; // Sets the distance of the position to the current value
                List<Position> nn = allPosDist(p, 1); // Gets the neighboring positions of the current position
                for (Position n : nn) { // Iterates over the neighboring positions
                    if (isBlocked(u, n) || proximity[n.getX()][n.getY()] != Integer.MAX_VALUE || queue.contains(n))
                        continue; // Skips blocked positions, already marked positions, or positions already in
                                  // the queue
                    queue.add(n); // Adds the position to the queue
                }
            }
            if (proximity[u.getX()][u.getY()] != Integer.MAX_VALUE)
                break; // If the unit's position is marked, break the loop
            dist += 1; // Increases the distance counter by 1
            markNext.clear(); // Clears the list of positions to mark
            markNext.addAll(queue); // Updates the list of positions to mark with the queue
        }
        List<Position> moves = allPosDist(new Position(u.getX(), u.getY()), 1); // Gets the reachable positions of the
                                                                                // unit
        Integer bestFit = Integer.MIN_VALUE; // Initializes the best fitness score with the smallest integer value
        Position bestPos = null; // Initializes the position with the best fitness score as null
        for (Position p : moves) { // Iterates over the reachable positions
            if (positionIsOutofBound(p) || pgs.getTerrain(p.getX(), p.getY()) == TERRAIN_WALL)
                continue; // Skips positions outside the game field or positions with a wall
            if (proximity[p.getX()][p.getY()] == Integer.MAX_VALUE)
                continue; // Skips positions that are not marked
            Unit pu = pgs.getUnitAt(p.getX(), p.getY()); // Gets the unit at the position
            if (pu != null)
                continue; // Skips positions where there is already a unit
            int fit = -1000 * proximity[p.getX()][p.getY()] - (int) squareDist(p, dst);
            if (fit > bestFit) { // If the fit score is better than the current best score
                bestFit = fit; // Updates the best score
                bestPos = p; // Updates the position with the best score
            }
        }
        if (bestPos == null)
            return null; // If no position with a fitness score is found, return null
        int dir = toDir(new Position(u.getX(), u.getY()), bestPos); // Gets the direction to the best position
        return new UnitAction(UnitAction.TYPE_MOVE, dir); // Creates a move action to the best position and returns it
    }

    boolean moveTowards(Unit a, Position e) {
        int pos = e.getX() + e.getY() * pgs.getWidth();
        int x = pos % pgs.getWidth();
        int y = pos / pgs.getWidth();
        int radius = pgs.getUnits().size() > 32 ? 42 : 64;
        Position dstP = new Position(x, y);

        UnitAction move = findPathAstar(a, dstP, radius);
        if (move == null)
            return false;
        if (!gameState.isUnitActionAllowed(a, move))
            return false;
        Position futureposition = futurePosition(a, move.getDirection());
        int fPos = futureposition.getX() + futureposition.getY() * pgs.getWidth();
        if (_locationsTaken.contains(fPos))
            return false;
        pa.addUnitAction(a, move);
        _locationsTaken.add(fPos);
        return true;
    }
}
