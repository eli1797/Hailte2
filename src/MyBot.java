import hlt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MyBot {
    private static final double TWO_DOMINANCE_RATIO = 1.5;

    private static final double POWER_RATIO = 1.8;

    private static final int TROLL_FACTOR = 14;

    private static final int DOCK_FACTOR = 60;

    private static final int CHASE_FACTOR = 30;

    private static final int SAFETY_FACTOR = 40;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Position");

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
        int numMyPlanets = 0;
        Position starting = null;

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
            List<Planet> myPlanets = gameMap.getPlayerPlanets(me);
            int updatedPlanets = myPlanets.size();
            boolean planetChange = false;
            if (updatedPlanets < numMyPlanets || updatedPlanets > numMyPlanets) {
                planetChange = true;
            }
            numMyPlanets = updatedPlanets;

            int numMyDocked = me.getDockedShips();


            //Enemy
            borg = players.get(enemyIDs.get(0));
            borgShips = borg.getShips().size();

            if (numPlayers == 4) {
                romulan = players.get(enemyIDs.get(1));
                romulanShips = romulan.getShips().size();
                klingon = players.get(enemyIDs.get(2));
                klingonShips = klingon.getShips().size();
            }

            if (planetChange && myPlanets.size() > 0) {
                starting = center(myPlanets);
            }

            /*
            Ship by Ship Commands
            */
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (i == 1) {
                    //first turn!!!!
                    starting = center(ship);
                    //determine whether or not to troll a player
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    Player shipOwner = players.get(nearestEnemy.getOwner());
                    double sDistance = ship.getDistanceTo(nearestEnemy);

                    Planet nearestPlanet = GenNav.nearestPlanet(ship, me, gameMap, true, false);
                    double pDistance = ship.getDistanceTo(nearestPlanet);
                    boolean largePlanet = nearestPlanet.getDockingSpots() > 3;

                    boolean troll = (sDistance < pDistance + TROLL_FACTOR) && !largePlanet;

                    if (troll) {
                        trollFlag = true;
                        toTroll = shipOwner;
                    }
                }

                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    double enemyDistance = ship.getDistanceTo(nearestEnemy);
                    double meToBorgShips = ((double) myShips) / borgShips;
                    boolean powerfulBorg = meToBorgShips < .7;

                    if (myShips == 1 && enemyDistance < 36) {
                        moveList.add(new UndockMove(ship));
                    } else {
                        Ship nearestFriend = ship.findNearestPlayersShip(gameMap, me, false);
                        if (nearestFriend == null) {
                            nearestFriend = ship;
                        }
                        double friendDistance = ship.getDistanceTo(nearestFriend);

                        boolean enemyCloser = enemyDistance < friendDistance;
                        boolean haveTime = enemyDistance > 50 || (myShips == 3 && nearestFriend.isDocked()); //how
                        // far does a bot move in a turn? account for where the 50 is above

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

                    final ThrustMove newThrustMove;

                    Planet planet;
                    Planet nearestUnowned = GenNav.nearestPlanetMining(starting, me, gameMap, true);
                    Planet nearestPlanet = GenNav.nearestPlanetToPosition(starting, me, gameMap, false, false);
                    Planet nearestToShip = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                    double pDistance = ship.getDistanceTo(nearestPlanet);
                    boolean notNull = nearestToShip != null;
                    boolean openForMining = !nearestToShip.isOwned() || nearestToShip.getOwner() == me.getId();
                    boolean dockablecloser = notNull && openForMining && !nearestToShip.isFull()
                            && ship.getDistanceTo(nearestToShip) < 20;
                    boolean unownedCloser = nearestUnowned != null && ship.getDistanceTo(nearestUnowned) < pDistance;

                    if (dockablecloser) {
                      planet = nearestToShip;
                    } else if (unownedCloser) {
                        planet = nearestUnowned;
                    } else {
                        planet = nearestPlanet;
                    }
                    double planetDistance = ship.getDistanceTo(planet);


                    Ship target;
                    Ship nearestDocked = ship.findNearestEnemy(gameMap, me, true);
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    double nDistance = ship.getDistanceTo(nearestEnemy);
                    boolean dockedCloser = nearestDocked != null && ship.getDistanceTo(nearestDocked) + 60 <
                            nDistance && numMyDocked > 6;

                    if (dockedCloser) {
                        target = nearestDocked;
                    } else {
                        target = nearestEnemy;
                    }
                    double tDistance = ship.getDistanceTo(target);

                    boolean powerRatio = players.get(target.getOwner()).getShips().size() < myShips;
                    boolean expansion = numMyDocked < 6;

                    boolean planetCloser = planetDistance < tDistance && (tDistance > 10 || powerRatio || expansion);
                    boolean closerDock = planetDistance < tDistance && tDistance > 30;

                    boolean available = !planet.isOwned() || planet.getOwner() == me.getId();
                    Log.log("Begining navigation");
                    if (available && planetCloser || closerDock) {
                        if (dockablecloser) {
                            if (ship.canDock(nearestToShip)) {
                                moveList.add(new DockMove(ship, nearestToShip));
                                continue;
                            }
                            newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestToShip, Constants
                                    .MAX_SPEED);
                        } else if (unownedCloser) {
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