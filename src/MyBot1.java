import hlt.*;

import javax.print.attribute.standard.MediaSize;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyBot1 {

    private static final double TWO_DOMINANCE_RATIO = 1.5;

    private static final double POWER_RATIO = 1.8;

    private static final int TROLL_FACTOR = 21;

    private static final int DOCK_FACTOR = 60;

    private static final int CHASE_FACTOR = 30;

    private static final int SAFETY_FACTOR = 40;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Classic");

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
        boolean hasUndocked = false;
        Ship runAway = null;

        /* Game Analysis */
        Player me = gameMap.getMyPlayer();
        int myID = gameMap.getMyPlayerId();
        int myShips = 3;
        int numMyPlanets = 0;
        Position starting = null;
        int peakDocked = 0;

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
            numMyPlanets = myPlanets.size();

            int numMyDocked = me.getDockedShips();
            boolean firstUndock = false;

            if (numMyDocked > peakDocked) {
                peakDocked = numMyDocked;
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

            Log.log("Logging the ratio");
            double meToBorgShips = ((double) myShips) / borgShips;
            Log.log(meToBorgShips + " THE RATIO");

            /*
            Ship by Ship Commands
            */
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (i == 1) {
                    //first turn!!!!
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

                if (ship.isDocked()) {
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    double enemyDistance = ship.getDistanceTo(nearestEnemy);
                    boolean powerfulBorg = meToBorgShips < .7;

                    //@TODO: Need a way to determine how close a planet is to producing a ship
                    if (peakDocked >= 3) {
                        continue;
                    }

                    if (myShips == 1 && enemyDistance < 60 && peakDocked < 3) {
                        moveList.add(new UndockMove(ship));
                        hasUndocked = true;
                        firstUndock = true;
                        runAway = ship;
                        continue;
                    } else if (myShips == 2 && enemyDistance <= 42 && powerfulBorg && peakDocked < 3) {
                        moveList.add(new UndockMove(ship));
                        hasUndocked = true;
                        runAway = ship;
                        firstUndock = true;
                        continue;
                    } else {
                        Ship nearestFriend = ship.findNearestPlayersShip(gameMap, me, false);
                        if (nearestFriend == null) {
                            nearestFriend = ship;
                        }
                        double friendDistance = ship.getDistanceTo(nearestFriend);

                        boolean enemyCloser = enemyDistance < friendDistance;
                        boolean haveTime = enemyDistance > 50 || (myShips < 3 && nearestFriend.isDocked()); //how
                        // far does a bot move in a turn? account for where the 50 is above

                        if (haveTime && enemyCloser) {
                            moveList.add(new UndockMove(ship));
                            hasUndocked = true;
                            runAway = ship;
                            firstUndock = true;
                            continue;
                        }
                    }
                    continue;
                }

                //call off trolling
                if (toTroll == null || toTroll.getShips().size() == 0 || hasUndocked) {
                    trollFlag = false;
                }

                if (trollFlag) {
                    Log.log("Trolling");
                    moveList.add(troll(ship, gameMap, me, toTroll));
                    continue;
                }

                if (hasUndocked) {
                /* Panic mode */
                    Log.log(" Inside has undocked ");
                    Move running = runAway(ship, gameMap, me);
                    if (running == null) {
                        Log.log("Running move was null");
                        moveList.add(Navigation.emptyThrustMove(ship));
                    } else {
                        moveList.add(running);
                    }
                    boolean safe = running != null;
                    if (safe && running.getType() == Move.MoveType.Dock) {
                        hasUndocked = false;
                    } else if (!safe) {
                        hasUndocked = false;
                    }
                } else if (numPlayers == 2) {
                    /* Two Player Mode */

                    if (meToBorgShips >= TWO_DOMINANCE_RATIO) { //ratio of ships favors me. cease expanding. attack
                        //look for the nearest enemy ship
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);

                        //attack slowly
                        ThrustMove newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);

                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        } else {
                            moveList.add(Navigation.emptyThrustMove(ship));
                        }

                    } else {    //else proceed normally
                        //look for the nearest unoccupied planet
                        boolean goForShip;
                        boolean goForDocked = true;

                        Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        if (nearestDockedEnemy == null) {
                            nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, false);
                        }

                        if (!nearestEnemy.equals(nearestDockedEnemy)) {
                            //if a mobile enemy if closer than a docked enemy attack the mobile enemy
                            goForDocked = ship.getDistanceTo(nearestDockedEnemy) + 50 < ship.getDistanceTo
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
                                newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
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
                } else {
                /* Four Player Mode */

                    Ship target;
                    Log.log("1");
                    Ship nearestDocked = ship.findNearestEnemy(gameMap, me, true);
                    Log.log("2");
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    Log.log("3");
                    double nDistance = ship.getDistanceTo(nearestEnemy);
                    boolean dockedCloser = nearestDocked != null && ship.getDistanceTo(nearestDocked) + 60 <
                            nDistance && numMyDocked > 6;

                    if (dockedCloser) {
                        target = nearestDocked;
                    } else {
                        target = nearestEnemy;
                    }
                    double tDistance = ship.getDistanceTo(target);

                    Log.log("4");
                    Planet nearestPlanet = GenNav.nearestPlanetMining(ship, me, gameMap, false);

                    boolean targetCloser;
                    if (nearestPlanet != null) {
                        double pDistance = ship.getDistanceTo(nearestPlanet);
                        targetCloser = tDistance < pDistance;
                    } else {
                        targetCloser = true;
                    }

                    boolean notNull = nearestPlanet != null;
                    boolean openForMining = notNull && !nearestPlanet.isOwned() || nearestPlanet.belongsTo(me);
                    boolean expansion = numMyDocked < 8;
                    boolean goForShip = !notNull || (openForMining && targetCloser && (tDistance < 14 || !expansion));

                    ThrustMove newThrustMove = null;
                    if (goForShip) {
                        if (dockedCloser && target != null) {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.attack(gameMap, ship, target, Constants.MAX_SPEED);
                        } else {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, target);
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

    private static Move runAway(Ship ship, GameMap gameMap, Player me) {
        Log.log("Running away");
        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
        //Planet farthest = GenNav.farthestPlanet(nearestEnemy, me, gameMap, true);


        if (nearestEnemy != null) {
            return (Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy));
        } else {
            return Navigation.emptyThrustMove(ship);
        }

//        else {
//            if (ship.canDock(farthest) && !farthest.isFull()) {
//                return (new DockMove(ship, farthest));
//            } else {
//                return (Navigation.navigateShipToDock(gameMap, ship, farthest, Constants.MAX_SPEED));
//            }
//        }
    }

    private static Move troll(Ship ship, GameMap gameMap, Player me, Player toTroll) {
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
            return (goGoGo);
        } else {
            return (Navigation.emptyThrustMove(ship));
        }
    }
}
