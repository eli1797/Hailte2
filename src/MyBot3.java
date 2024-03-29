import hlt.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class MyBot3 {

    private static final double TWO_DOMINANCE_RATIO = 1.5;

    private static final double POWER_RATIO = 1.8;

    private static final int TROLL_FACTOR = 55;

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
            boolean overwridden = false;

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

                    Planet farPlanet = GenNav.farthestPlanet(nearestEnemy, me, gameMap, false);
                    double pDistance = ship.getDistanceTo(farPlanet);
                    boolean largePlanet = farPlanet.getDockingSpots() > 3;

                    boolean troll = (sDistance < pDistance + TROLL_FACTOR) && !largePlanet;

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


                if (numPlayers == 2 && trollFlag) {
                    /* Two Player Mode */
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

                    ThrustMove newThrustMove = null;
                    boolean planetCloser;
                    double planetDistance;

                    boolean planetsNull = false;
                    boolean available = false;
                    Planet planet = null;
                    Log.log("1");
                    Planet nearestUnowned = GenNav.nearestPlanetMining(ship, me, gameMap, true);
                    Planet nearestPlanet = GenNav.nearestPlanetMining(ship, me, gameMap, false);

                    if (nearestPlanet == null) {
                        planetCloser = false;
                        planetsNull = true;
                        planetDistance = 100000;
                    } else {
                        double pDistance = ship.getDistanceTo(nearestPlanet);
                        boolean unownedCloser = nearestUnowned != null && ship.getDistanceTo(nearestUnowned) < pDistance;

                        if (unownedCloser) {
                            planet = nearestUnowned;
                        } else {
                            planet = nearestPlanet;
                        }
                        available = (!planet.isOwned() || planet.belongsTo(me)) && !planet.isFull();
                        planetDistance = ship.getDistanceTo(planet);
                    }

                    Log.log("2");
                    Ship target = null;
                    Ship nearestDocked = ship.findNearestEnemy(gameMap, me, true);
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    if (nearestDocked == null && nearestEnemy == null) {
                        planetCloser = true;
                    } else {
                        double nDistance = ship.getDistanceTo(nearestEnemy);
                        boolean dockedCloser = nearestDocked != null && ship.getDistanceTo(nearestDocked) + 60 < nDistance;

                        if (dockedCloser) {
                            target = nearestDocked;
                        } else {
                            target = nearestEnemy;
                        }
                        double tDistance = ship.getDistanceTo(target);
                        Log.log("4");
                        boolean powerRatio = players.get(target.getOwner()).getShips().size() < myShips;

                        if (numMyDocked < 5) {
                            planetCloser = !planetsNull && planetDistance < tDistance && (tDistance > 34 || powerRatio);
                        } else {
                            planetCloser = !planetsNull && planetDistance < tDistance && (tDistance > 27 || powerRatio);
                        }
                    }

                    Log.log("4");
                    Log.log("Begining navigation");
                    if (planetCloser && available && !planetsNull) {
                        if (ship.canDock(planet)) {
                            moveList.add(new DockMove(ship, planet));
                            continue;
                        }
                        newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants
                                .MAX_SPEED);

                    } else if (target != null && target.isDocked()) {
                        newThrustMove = Navigation.attack(gameMap, ship, target, Constants.MAX_SPEED);
                    } else if (target != null) {
                        newThrustMove = Navigation.attack(gameMap, ship, target, 7);
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
