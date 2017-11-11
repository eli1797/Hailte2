import hlt.*;

import java.util.ArrayList;
import java.util.Map;

public class MyBot {

    private static final int CHASE_FACTOR = 50;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("0");

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

                    if (allPlanetsBeenCaptured) {

                        boolean goForShip;
                        boolean goForDocked = true;

                        Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        if (nearestDockedEnemy == null) {
                            nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, false);
                        }

                        if (!nearestEnemy.equals(nearestDockedEnemy)) {
                            //if a mobile enemy if closer than a docked enemy attack the mobile enemy
                            goForDocked = ship.getDistanceTo(nearestDockedEnemy) + CHASE_FACTOR < ship.getDistanceTo
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

                    } else {
                        //find the nearest un-owned planet
                        Planet nearestUnowned = null;
                        for (Map.Entry<Double, Entity> ent : planetsToHome.entrySet()) {
                            Planet planet = (Planet) ent.getValue();
                            if (planet.isFull()) {
                                continue;
                            }
                            if (planet.getOwner() != me.getId()) {
                                continue;
                            }
                            if (!ship.canDock(planet)) {
                                continue;
                            }
                            nearestUnowned = planet;
                            break;
                        }
//                        if (gameMap.getPlayerPlanets(me).size() <= 2) {
//                            //mine all mine the first planet
//                            nearestUnowned = GenNav.nearestPlanet(ship, me, gameMap, false, false);
//                            if (nearestUnowned.isFull()) {
//                                nearestUnowned = GenNav.nearestPlanet(ship, me, gameMap, true, false);
//                            }
//                        } else {
//                            nearestUnowned = GenNav.nearestPlanet(ship, me, gameMap, true, false);
//                        }

                        if (nearestUnowned == null) {
                            nearestUnowned = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                            allPlanetsBeenCaptured = true;
                            Log.log("ALL PLANETS BEEN CAPTURED!");
                        }

                        Log.log("  Nearest Planet is:  " + nearestUnowned + " ");

                        if (nearestUnowned != null && ship.getDistanceTo(nearestUnowned) > 100) {
                            /*  nearestUnowned = GenNav.nearestPlanet(ship, me, gameMap, false, true);
                            @TODO:
                            Later it might be optimal if finding the nearest docked enemy ship first looks for
                            enemy planets then gets their docked ships because there will generally be fewer planets than
                            ships
                             */
                            Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                            if (nearestDockedEnemy == null) {
                                nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, false);
                            }
                            ThrustMove newThrustMove;
                            if (nearestDockedEnemy != null) {
                                //if nearest enemy ship exists navigate to attack
                                newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                        .getClosestPoint(nearestDockedEnemy), Constants.MAX_SPEED, true, Constants
                                        .MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);
                            } else {
                                //empty thrust move
                                newThrustMove = Navigation.emptyThrustMove(ship);
                            }

                            if (newThrustMove != null) {
                                moveList.add(newThrustMove);
                            }

                        } else {
                            Log.log("Inside else");
                            //if the distance is far or I own x number of planets
                            //find and attack the nearest enemy planet or ship
                            //else
                            //go to and dock at the nearest planet

                            Log.log("Looking for dock");
                            if (ship.canDock(nearestUnowned)) {
                                moveList.add(new DockMove(ship, nearestUnowned));
                                continue;
                            }

                            Log.log("Dock failed. Moving closer");

                            final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestUnowned,
                                    Constants.MAX_SPEED);
                            if (newThrustMove != null) {
                                moveList.add(newThrustMove);
                            } else {
                                Log.log("Empty thrust move");
                                moveList.add(Navigation.emptyThrustMove(ship));
                            }
                        }
                    }
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}