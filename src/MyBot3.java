import hlt.*;

import javax.print.attribute.standard.MediaSize;
import java.net.HttpRetryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MyBot3 {

    private static final int DOCK_FACTOR = 60;

    private static final int CHASE_FACTOR = 30;

    private static final int SAFETY_FACTOR = 70;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("3");

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
        boolean ratioFlag = false;
        double meToThemShips = 1.0;

        /* Game Analysis */
        int numPlayers = gameMap.getAllPlayers().size();
        Player me = gameMap.getMyPlayer();

        /*
        Game Play
         */

        final ArrayList<Move> moveList = new ArrayList<>();
        for (int i = 1;; i++) {
            moveList.clear();
            Log.log("Trying update map");
            networking.updateMap(gameMap);


            Log.log("Turn is  " + i + " Let's go  ");

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }

                if (numPlayers == 2) {
                    /* Two Player Mode */

                    //@TODO: determine te ratio of my ships to theirs
                    //if the ratio favors me cease expanding and just attack
                    //else proceed normally

                    //Log.log("Ratio of me to them in ships: " + meToThemShips);


                    //look for the nearest unoccupied planet
                    Planet nearestPlanet =  GenNav.nearestPlanetMining(ship, me, gameMap, false);
                    if (nearestPlanet == null || (nearestPlanet.getOwner() != me.getId() && nearestPlanet.isOwned())) {
                        nearestPlanet = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                    }

                    //look for the nearest enemy ship
                    Ship nDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);

                    //determine which enemy is closer
                    boolean goForDocked = nDockedEnemy != null;
                    if (nDockedEnemy != null && nearestEnemy != null && !nDockedEnemy.equals(nearestEnemy)) {
                        goForDocked = ship.getDistanceTo(nDockedEnemy) + 20 < ship.getDistanceTo(nearestEnemy);
                    }

                    //if the nearest enemy is closer then the nearest unoccupied planet
                    boolean goForShip;
                    if (goForDocked) {
                        goForShip = ship.getDistanceTo(nDockedEnemy) + 20 < ship.getDistanceTo(nearestPlanet);
                    } else if (nearestPlanet.isFull()) {
                        goForShip = true;
                    } else {
                        goForShip = ship.getDistanceTo(nearestEnemy) < ship.getDistanceTo(nearestPlanet) + 30;
                    }



                    ThrustMove newThrustMove = null;
                    if (goForShip) {
                        if (goForDocked) {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                    .getClosestPoint(nDockedEnemy), Constants.MAX_SPEED, true, Constants
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
                        } else if (nearestPlanet.isFull()) {
                            newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                    .getClosestPoint(nearestEnemy), Constants.MAX_SPEED, true, Constants
                                    .MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);
                        }

                        newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestPlanet, Constants.MAX_SPEED);
                    }

                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                    } else if (nearestEnemy != null) {
                        newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                .getClosestPoint(nearestEnemy), Constants.MAX_SPEED, true, Constants
                                .MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);
                        moveList.add(newThrustMove);
                    } else {
                        moveList.add(Navigation.emptyThrustMove(ship));
                    }


                } else {
                    /* Four Player Mode */
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}
