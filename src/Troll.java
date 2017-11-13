import hlt.Constants;
import hlt.DockMove;
import hlt.Entity;
import hlt.GameMap;
import hlt.GenNav;
import hlt.Log;
import hlt.Move;
import hlt.Navigation;
import hlt.Networking;
import hlt.Planet;
import hlt.Player;
import hlt.Position;
import hlt.Ship;
import hlt.ThrustMove;
import hlt.UndockMove;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class Troll {
    private static final double TWO_DOMINANCE_RATIO = 1.5;

    private static final double POWER_RATIO = 1.8;

    private static final int TROLL_FACTOR = 21;

    private static final int DOCK_FACTOR = 60;

    private static final int CHASE_FACTOR = 30;

    private static final int SAFETY_FACTOR = 40;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Taunting Troll");

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

        Player toTaunt = null;

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

                if (numPlayers == 2) {
                /* Two Player Mode */

                    //get the nearest docked enemy
                    Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                    //if docked enemy exists navigate to attack him
                    ThrustMove newThrustMove = null;
                    if (nearestDockedEnemy != null) {
                        newThrustMove = Navigation.attack(gameMap, ship, nearestDockedEnemy, Constants.MAX_SPEED);
                    } else {
                        //else get the nearest enemy ship
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        if (nearestEnemy != null) {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
                        }
                    }
                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                    } else {
                        moveList.add(Navigation.emptyThrustMove(ship));
                    }

                } else {
                    /* Four Player Mode */
                    Log.log("Four player mode");

                    ThrustMove newThrustMove = null;

                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);

                    if (toTaunt == null) {
                        toTaunt = players.get(nearestEnemy.getOwner());
                    } else {
                        Ship tauntable = GenNav.getNearestOfPlayer(toTaunt, ship, gameMap, false);
                        Player closest = players.get(nearestEnemy.getOwner());
                        if (ship.getDistanceTo(tauntable) > 30) {
                            toTaunt = null;
                        } else if (!closest.equals(toTaunt)) {
                            toTaunt = closest;
                        }
                    }
//
//                    Ship nearestEnemy;
//                    if (toTaunt == null) {
//                        nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
//                    } else {
//                        nearestEnemy = GenNav.getNearestOfPlayer(toTaunt, ship, gameMap, false);
//                    }

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

                    if (newThrustMove != null) {
                        moveList.add(newThrustMove);
                        Log.log("New Thrust move was not null");
                    } else {
                        Log.log("New Thrust move was null");
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
