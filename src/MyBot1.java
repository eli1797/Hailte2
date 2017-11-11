import hlt.*;

import java.util.ArrayList;
import java.util.Map;

public class MyBot1 {

    private static final int DOCK_FACTOR = 60;
    private static final int CHASE_FACTOR = 50;
    private static final int SAFETY_FACTOR = 30;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("1");

        /*
        One minute of map analysis
         */
        final String initialMapIntelligence =
                "width: " + gameMap.getWidth() +
                "; height: " + gameMap.getHeight() +
                "; players: " + gameMap.getAllPlayers().size() +
                "; planets: " + gameMap.getAllPlanets().size() + "    ";
        Log.log(initialMapIntelligence);

        /* Game Progression Flags and Variables */
        boolean allPlanetsBeenCaptured = false;
        boolean homePlanets = false;
        Position avgHome = null;
        Map<Double, Entity> planetsToHome = null;

        /* Game Analysis */
        int numPlayers = gameMap.getAllPlayers().size();
        Player me = gameMap.getMyPlayer();

        /*
        Game Play
         */

        final ArrayList<Move> moveList = new ArrayList<>();
        for (int i = 1;; i++) {
            moveList.clear();
            networking.updateMap(gameMap);


            Log.log("Turn is  " + i + "   ");

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }

                if (avgHome == null) {
                    avgHome = new Position(ship.getXPos(), ship.getYPos());
                    planetsToHome = gameMap.nearbyPlanetsByDistance(ship);
                }

                if (numPlayers == 0) {
                    /* Two Player Mode */

                    //@TODO: look at 2 player mode from divsurana

                    //get the nearest docked enemy
                    Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                    //if docked enemy exists navigate to attack him
                    if (nearestDockedEnemy != null) {
                        final ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                .getClosestPoint(nearestDockedEnemy), Constants.MAX_SPEED, true, Constants
                                .MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0);
                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        }
                    } else {
                        //else get the nearest enemy ship
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        ThrustMove newThrustMove;
                        if (nearestEnemy != null) {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                    .getClosestPoint(nearestEnemy), Constants.MAX_SPEED, true, Constants
                                    .MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);
                        } else {
                            //empty thrust move
                            newThrustMove = Navigation.emptyThrustMove(ship);
                        }

                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        }
                        
                    }

                } else {
                    /* Four Player Mode */

                    boolean goForShip;
                    boolean goForDocked = true;

                    Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    if (nearestDockedEnemy == null) {
                        nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, false);
                    }

                    if (!nearestEnemy.equals(nearestDockedEnemy)) {
                        //if a mobile enemy if closer than a docked enemy attack the mobile enemy
                        goForDocked = ship.getDistanceTo(nearestDockedEnemy) + DOCK_FACTOR < ship.getDistanceTo
                                (nearestEnemy);
                    }


                    Planet nearestPlanet = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                    if (nearestPlanet == null || nearestPlanet.isFull()) {
                        goForShip = true;
                    } else {
                        goForShip = ship.getDistanceTo(nearestDockedEnemy) < 30;
                    }

                    ThrustMove newThrustMove = null;
                    if (goForShip) {
                        if (goForDocked && nearestDockedEnemy != null) {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                    .getClosestPoint(nearestDockedEnemy), Constants.MAX_SPEED, true, Constants
                                    .MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);
                        } else {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                    .getClosestPoint(nearestEnemy), Constants.MAX_SPEED, true, Constants
                                    .MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);
                        }

                    } else {
                        if (ship.canDock(nearestPlanet) && !nearestPlanet.isFull()) {
                            moveList.add(new DockMove(ship, nearestPlanet));
                            continue;
                        }

                        newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestPlanet, Constants.MAX_SPEED);
                    }

                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                    } else {
                        moveList.add(Navigation.emptyThrustMove(ship));
                    }
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}
