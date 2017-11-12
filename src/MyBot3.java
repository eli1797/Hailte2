import hlt.*;

import javax.print.attribute.standard.MediaSize;
import java.net.HttpRetryException;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class MyBot3 {

    private static final double TWO_DOMINANCE_RATIO = 1.5;

    private static final double POWER_RATIO = 1.8;

    private static final int TROLL_FACTOR = 21;

    private static final int DOCK_FACTOR = 60;

    private static final int CHASE_FACTOR = 30;

    private static final int SAFETY_FACTOR = 40;

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
        boolean trollFlag = false;
        Player toTroll = null;

        /* Game Analysis */
        Player me = gameMap.getMyPlayer();
        int myID = gameMap.getMyPlayerId();
        int myShips = 3;

        List<Player> players = gameMap.getAllPlayers();
        int numPlayers = players.size();
        Log.log(numPlayers + "");

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

        /* Navigational Information */
        TreeMap<Double, Planet> starterMap = null;

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

            /*
            Ship by Ship Commands
            */
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (i == 1) {
                    //first turn!!!!
                    Position starting = center(ship);
                    starterMap = gameMap.nearbyPlanetsByDistance(starting);
                    //determine whether or not to troll a player
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    Player shipOwner = players.get(nearestEnemy.getOwner());
                    double sDistance = ship.getDistanceTo(nearestEnemy);

                    Planet nearestPlanet = GenNav.nearestPlanet(ship, me, gameMap, true, false);
                    double pDistance = ship.getDistanceTo(nearestPlanet);
                    boolean largePlanet = nearestPlanet.getDockingSpots() > 3;

                    boolean troll = (sDistance + TROLL_FACTOR < pDistance) && !largePlanet;

                    if (troll) {
                        trollFlag = true;
                        toTroll = shipOwner;
                    }
                }

                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    double enemyDistance = ship.getDistanceTo(nearestEnemy);
                    if (myShips == 1 && enemyDistance < 30) {
                        moveList.add(new UndockMove(ship));
                    } else {
                        Ship nearestFriend = ship.findNearestPlayersShip(gameMap, me, false);
                        if (nearestFriend == null) {
                            nearestFriend = ship;
                        }
                        double friendDistance = ship.getDistanceTo(nearestFriend);

                        boolean enemyCloser = enemyDistance < friendDistance;
                        boolean haveTime = enemyDistance > 50; //how far does a bot move in a turn?

                        if (haveTime && enemyCloser) {
                            moveList.add(new UndockMove(ship));
                        }
                    }
                    continue;
                }

                //call off trolling
                if (toTroll == null || toTroll.getShips().size() == 0) {
                    trollFlag = false;
                }

                if (trollFlag) {
                    Log.log("Trolling");
                    Ship trollee = ship.findNearestPlayersShip(gameMap, toTroll, false);
                    boolean isDocked = trollee.getDockingStatus() != Ship.DockingStatus.Undocked;
                    ThrustMove goGoGo;
                    if (isDocked) {
                        goGoGo = Navigation.attack(gameMap, ship, trollee, Constants.MAX_SPEED);
                    } else {
                        goGoGo = Navigation.speedSensitiveattack(gameMap, ship, trollee);
                    }

                    if (goGoGo != null) {
                        moveList.add(goGoGo);
                    } else {
                        moveList.add(Navigation.emptyThrustMove(ship));
                    }
                }

                if (numPlayers == 2) {
                /* Two Player Mode */

                    double meToBorgShips = ((double) myShips) / borgShips;
                    Log.log(meToBorgShips + " THE RATIO");

                    if (meToBorgShips >= TWO_DOMINANCE_RATIO) { //ratio of ships favors me. cease expanding. attack
                        //look for the nearest enemy ship
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);

                        //attack slowly
                        ThrustMove newThrustMove = Navigation.navigateShipTowardsTarget(gameMap, ship, ship
                                .getClosestPoint(nearestEnemy), 5, true, Constants
                                .MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);

                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        } else {
                            moveList.add(Navigation.emptyThrustMove(ship));
                        }

                    } else {    //else proceed normally
                        //look for the nearest unoccupied planet
                        Planet nearestPlanet = GenNav.nearestPlanetMining(ship, me, gameMap, false);
                        if (nearestPlanet == null || (nearestPlanet.getOwner() != me.getId() && nearestPlanet.isOwned())) {
                            nearestPlanet = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                        }

                        Log.log(nearestPlanet.toString());

                        //look for the nearest enemy ship
                        Ship nDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);

                        //determine which enemy is closer
                        boolean goForDocked = nDockedEnemy != null;
                        if (goForDocked && nearestEnemy != null && !nDockedEnemy.equals(nearestEnemy)) {
                            goForDocked = ship.getDistanceTo(nDockedEnemy) + 20 < ship.getDistanceTo(nearestEnemy);
                        }

                        boolean goForShip = nearestEnemy != null;

                        //if the nearest enemy is closer then the nearest unoccupied planet
                        if (goForDocked) {
                            goForShip = ship.getDistanceTo(nDockedEnemy) + 30 < ship.getDistanceTo(nearestPlanet);
                        } else {
                            goForShip = nearestPlanet.isFull() || ship.getDistanceTo(nearestEnemy) < ship.getDistanceTo(nearestPlanet) + 30;
                        }

                        ThrustMove newThrustMove = null;
                        if (goForShip) {
                            Log.log("Going for ship");
                            ;
                            if (goForDocked) {
                                //if docked enemy ship exists navigate to attack
                                newThrustMove = Navigation.attack(gameMap, ship, nDockedEnemy, Constants.MAX_SPEED);
                            } else {
                                //if nearest enemy ship exists navigate to attack
                                newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
                            }

                        } else {
                            Log.log("Going for nearest planet");
                            if (ship.canDock(nearestPlanet) && !nearestPlanet.isFull()) {
                                moveList.add(new DockMove(ship, nearestPlanet));
                                continue;
                            } else if (nearestPlanet.isFull()) {
                                newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
                            } else {
                                newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestPlanet, Constants.MAX_SPEED);
                            }
                        }

                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        } else {
                            moveList.add(Navigation.emptyThrustMove(ship));
                        }
                    }


                } else {
                    /* Four Player Mode */
                    Log.log("Four player mode");

                    final ThrustMove newThrustMove;

                    Planet planet;
                    Planet nearestUnowned = GenNav.nearestPlanetMining(ship, me, gameMap, true);
                    Planet nearestPlanet = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                    double pDistance = ship.getDistanceTo(nearestPlanet);
                    boolean unownedCloser = nearestUnowned != null && ship.getDistanceTo(nearestUnowned) < pDistance;

                    if (unownedCloser) {
                        planet = nearestUnowned;
                    } else {
                        planet = nearestPlanet;
                    }
                    double planetDistance = ship.getDistanceTo(planet);


                    Ship target;
                    Ship nearestDocked = ship.findNearestEnemy(gameMap, me, true);
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    double nDistance = ship.getDistanceTo(nearestEnemy);
                    boolean dockedCloser = nearestDocked != null && ship.getDistanceTo(nearestDocked) + 60 < nDistance;

                    if (dockedCloser) {
                        target = nearestDocked;
                    } else {
                        target = nearestEnemy;
                    }
                    double tDistance = ship.getDistanceTo(target);

                    boolean powerRatio = players.get(target.getOwner()).getShips().size() < myShips;

                    boolean planetCloser = planetDistance < tDistance && (tDistance > 10 || powerRatio);

                    boolean available = !nearestPlanet.isOwned() || nearestPlanet.getOwner() == me.getId();
                    Log.log("Begining navigation");
                    if (planetCloser && available) {
                        if (unownedCloser) {
                            if (ship.canDock(nearestUnowned)) {
                                moveList.add(new DockMove(ship, nearestUnowned));
                                continue;
                            }
                            newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestUnowned, Constants
                                    .MAX_SPEED);
                        } else {
                            if (!nearestPlanet.isFull()) {
                                if (ship.canDock(nearestPlanet) && !nearestPlanet.isFull()) {
                                    moveList.add(new DockMove(ship, nearestPlanet));
                                    continue;
                                }
                                newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestPlanet, Constants.MAX_SPEED);
                            } else {
                                newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
                            }
                        }
                    } else if (nearestEnemy.isDocked()) {
                        newThrustMove = Navigation.attack(gameMap, ship, nearestEnemy, Constants.MAX_SPEED);
                    } else {
                        newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
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

    private static Position center(Entity centerAround) {
        return new Position(centerAround.getXPos(), centerAround.getYPos());
    }

}
