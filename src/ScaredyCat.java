import hlt.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScaredyCat {


    private static final int CHASE_FACTOR = 50;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Old Scaredy Cat");

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

        /* Game Analysis */;
        List<Player> players = gameMap.getAllPlayers();
        int numPlayers = players.size();
        Player me = gameMap.getMyPlayer();
        int myID = gameMap.getMyPlayerId();
        int myShips = 3;

        //Enemies
        Player borg = null;
        int borgShips = 3;
        Player romulan = null;
        int romulanShips = 3;
        Player klingon = null;
        int klingonShips = 3;

        ArrayList<Integer> enemyIDs = new ArrayList<>(3);
        for (Player player : players) {
            if (player.getId() == myID) {
                continue;
            }
            enemyIDs.add(player.getId());
        }
        Player toTaunt = null;

        /* Navigational Information */
        TreeMap<Double, Planet> starterMap = null;
        Planet safest = null;
        /*
        Game Play
         */
        final ArrayList<Move> moveList = new ArrayList<>();
        for (int i = 1; ; i++) {
            moveList.clear();
            networking.updateMap(gameMap);

            /* Progress Markers */
            boolean tenDockedFlag = false;
            boolean sixDockedFlag = false;

            /* Update player information */
            players = gameMap.getPlayers();
            me = players.get(myID);
            myShips = me.getShips().size();
            int numMyDocked = me.getDockedShips();
            if (numMyDocked >= 10) {
                tenDockedFlag = true;
            } else if (numMyDocked >= 6) {
                sixDockedFlag = true;
            }

            //Enemy
            borg = players.get(enemyIDs.get(0));
            borgShips = borg.getShips().size();

            if (numPlayers == 4) {
                romulan = players.get(enemyIDs.get(1));
                romulanShips = romulan.getShips().size();
                klingon = players.get(enemyIDs.get(2));
                klingonShips = klingon.getShips().size();
            }



            Position triangulatedEnemies = new Position(0, 0);
            Log.log("Turn is  " + i + "   ");
            int count = 1;
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }

                if (count == 1 && numPlayers == 4) {
                    Ship borgShip = GenNav.getNearestOfPlayer(borg, ship, gameMap, false);
                    Ship klingonShip = GenNav.getNearestOfPlayer(klingon, ship, gameMap, false);
                    Ship romulanShip = GenNav.getNearestOfPlayer(romulan, ship, gameMap, false);

                    triangulatedEnemies = GenNav.triangulateEnemies(borgShip, klingonShip, romulanShip);
                    count++;
                    Planet temp = GenNav.farthestPlanet(triangulatedEnemies, gameMap, true);
                    if (temp != null) {
                        safest = temp;
                    } else {
                        safest = GenNav.farthestPlanet(triangulatedEnemies, gameMap, false);
                    }

                    if (numMyDocked > 1) {
                        safest = GenNav.nearestPlanetMining(center(gameMap.getPlayerPlanets(me)), me, gameMap, false);
                    }
                }

                if (avgHome == null) {
                    avgHome = new Position(ship.getXPos(), ship.getYPos());
                    planetsToHome = gameMap.nearbyPlanetsByDistance(ship);
                }

                if (numPlayers == 2) {
                    /* Two Player Mode */
                    Log.log("Two player mode");

                    //@TODO: look at 2 player mode from divsurana

                    //get the nearest docked enemy
                    Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                    Log.log("1");
                    //if docked enemy exists navigate to attack him
                    ThrustMove newThrustMove = null;
                    if (nearestDockedEnemy != null) {
                        Log.log("2");
                        newThrustMove = Navigation.attack(gameMap, ship, nearestDockedEnemy, Constants.MAX_SPEED);
                    } else {
                        Log.log("3");
                        //else get the nearest enemy ship
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        if (nearestEnemy != null) {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
                        }
                    }
                    if (newThrustMove != null) {
                        Log.log("4");
                        moveList.add(newThrustMove);
                    } else {
                        Log.log("5");
                        moveList.add(Navigation.emptyThrustMove(ship));
                    }

                } else {
                    /* Four Player Mode */
                    Log.log("Four player mode");

                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);


                    boolean priorityShip = nearestEnemy != null && ship.getDistanceTo(nearestEnemy) < 30;
                    Log.log("2");

                    ThrustMove newThrustMove = null;
                    if (priorityShip) {
                        Player owner = players.get(nearestEnemy.getOwner());
                        int ownerShips = owner.getShips().size();
                        double nDist = ship.getDistanceTo(nearestEnemy);

                        boolean taunt = ((double) ownerShips) / myShips < 1; //&& numMyDocked < 20;
                        if (taunt) {
                            if (toTaunt == null) {
                                toTaunt = players.get(nearestEnemy.getOwner());
                            } else {
                                Ship tauntable = GenNav.getNearestOfPlayer(toTaunt, ship, gameMap, false);
                                Player closest = players.get(nearestEnemy.getOwner());
                                boolean safe = tauntable != null;
                                boolean tCloser = safe && ship.getDistanceTo(tauntable) < nDist;
                                if (safe && (ship.getDistanceTo(tauntable) > 30 || !tCloser)) {
                                    toTaunt = null;
                                } else if (!closest.equals(toTaunt)) {
                                    toTaunt = closest;
                                }
                            }

                            Log.log("attempt to define nearest enemy passed");
                            if (nearestEnemy != null && ship.getDistanceTo(nearestEnemy) < 30 ) {
                                Log.log("Nearest enemy is close!");
                                toTaunt = players.get(nearestEnemy.getOwner());
                                if (!toTaunt.equals(borg)) {
                                    //lead to the farthest borg
                                    Entity borgShip = gameMap.getPlayerShipsByDistance(ship, borg).lastEntry().getValue();
                                    newThrustMove = Navigation.taunt(gameMap, ship, borgShip, nearestEnemy);
                                } else {
                                    //lead to farthest romulan
                                    Entity romulanShip = gameMap.getPlayerShipsByDistance(ship, romulan).lastEntry().getValue();
                                    newThrustMove = Navigation.taunt(gameMap, ship, romulanShip, nearestEnemy);
                                }
                            }
                        } else {
                            newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
                        }
                    } else if (safest != null && !safest.isFull()) {
                        if (ship.canDock(safest)) {
                            moveList.add(new DockMove(ship, safest));
                            continue;
                        }
                        newThrustMove = Navigation.navigateShipToDock(gameMap, ship, safest, Constants.MAX_SPEED);
                    } else {
                        Planet nearestPlanet = GenNav.nearestPlanetMining(ship, me, gameMap, true);
                        if (nearestPlanet != null && !nearestPlanet.isFull()) {
                            safest = nearestPlanet;
                            newThrustMove = Navigation.navigateShipToDock(gameMap, ship, safest, Constants.MAX_SPEED);
                        } else {
                            Ship nearestShip = ship.findNearestEnemy(gameMap, me, false);
                            if (nearestShip != null) {
                                newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestShip);
                            }
                        }

                    }

                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                    } else {
                        Log.log("No valid thrustmove");
                        Position spot = new Position(3.0, 3.0);
                        moveList.add(Navigation.toPosition(gameMap, ship, spot));
                    }

                }
            }
            Networking.sendMoves(moveList);
        }
    }

    private static Position center(Entity centerAround) {
        return new Position(centerAround.getXPos(), centerAround.getYPos());
    }

    private static Position center(List<Planet> centerAround) {
        int xTot = 0;
        int yTot = 0;
        int rounds = 0;
        for (Entity ent : centerAround) {
            xTot += ent.getXPos();
            yTot += ent.getYPos();
            rounds++;
        }
        return new Position(xTot/ rounds, yTot / rounds);
    }
}
