import hlt.Constants;
import hlt.DockMove;
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

public class FourVersion2 {

    private final static int SAFETY_FACTOR = 14;

    public static void main(final String[] args) {
        final Networking networking;
        networking = new Networking();
        final GameMap gameMap = networking.initialize("V2");

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

    /* Game Analysis */
    /* Game Analysis */
        Player me = gameMap.getMyPlayer();
        int myID = gameMap.getMyPlayerId();
        int myShips = 3;

        List<Player> players = gameMap.getAllPlayers();
        int numPlayers = players.size();

    /*
    Game Play
     */

        final ArrayList<Move> moveList = new ArrayList<>();
        for (int i = 1;; i++) {
            moveList.clear();
            networking.updateMap(gameMap);

        /* Update player information */
            players = gameMap.getPlayers();
            me = players.get(myID);
            myShips = me.getShips().size();
            int numMyDocked = me.getDockedShips();
            List<Planet> myPlanets = gameMap.getPlayerPlanets(me);
            int numMyPlanets = myPlanets.size();


            Log.log("Turn is  " + i + "   ");

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                    double enemyDistance = ship.getDistanceTo(nearestEnemy);
                    if (myShips == 1 && enemyDistance < 35.0) {
                        moveList.add(new UndockMove(ship));
                    }
                    continue;
                }

                if (numPlayers == 2) {
                /* Two Player Mode */

                    //get the nearest docked enemy
                    Ship nearestDockedEnemy = ship.findNearestEnemy(gameMap, me, true);
                    //if docked enemy exists navigate to attack him
                    if (nearestDockedEnemy != null) {
                        final ThrustMove newThrustMove = Navigation.speedSensitiveattack(gameMap, ship,
                                nearestDockedEnemy);
                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        }
                    } else {
                        //else get the nearest enemy ship
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        ThrustMove newThrustMove;
                        if (nearestEnemy != null) {
                            //if nearest enemy ship exists navigate to attack
                            newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
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

                    if (allPlanetsBeenCaptured || numMyPlanets >= 5) {

                        boolean goForShip;
                        boolean goForDocked = true;

                        Ship boat;
                        Ship enemyShip = ship.findNearestDockingEnemy(gameMap, me);
                        if (enemyShip == null) {
                            enemyShip = ship.findNearestEnemy(gameMap, me, true);
                        }

                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        if (enemyShip == null) {
                            enemyShip = nearestEnemy;
                        }

                        boolean priorityShip = false;
                        double nDist = ship.getDistanceTo(nearestEnemy);
                        if (nDist < 20) {
                            goForDocked = false;
                            boat = nearestEnemy;
                            priorityShip = true;
                        } else if (!nearestEnemy.equals(enemyShip) && (ship.getDistanceTo(enemyShip) < nDist)) {
                            //if a mobile enemy if closer than a docked enemy attack the mobile enemy
                            boat = enemyShip;
                            goForDocked = true;
                        } else {
                            boat = nearestEnemy;
                            goForDocked = false;
                        }



                        Planet nearestPlanet = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                        if (nearestPlanet == null || (nearestPlanet.isOwned() && nearestPlanet.getOwner() != me.getId
                                ()) || nearestPlanet.isFull()) {
                            goForShip = true;
                        } else if (boat != null && ship.getDistanceTo(boat) < ship.getDistanceTo(nearestPlanet)) {
                            goForShip = true;
                        } else {
                            goForShip = false;
                        }

                        ThrustMove newThrustMove = null;
                        if (goForShip || priorityShip) {
                            if (goForDocked) {
                                //if nearest enemy ship exists navigate to attack
                                newThrustMove = Navigation.attack(gameMap, ship, enemyShip, 7);
                            } else {
                                //if nearest enemy ship exists navigate to attack
                                newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, nearestEnemy);
                            }

                        } else {
                            //@TODO: check if docking is safe
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
                        Planet nearestUnowned;
                        if (numMyPlanets <= 2) {
                            //mine all mine the first planet
                            if (numMyPlanets == 0) {
                                nearestUnowned = GenNav.nearestPlanetMining(ship, me, gameMap, true);
                            } else {
                                nearestUnowned = GenNav.nearestPlanetMining(myPlanets.get(0), me, gameMap, false);
                            }
                            if (nearestUnowned == null) {
                                nearestUnowned = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                            }
                        } else {
                            nearestUnowned = GenNav.nearestPlanet(ship, me, gameMap, false, false);
                        }

                        if (GenNav.nearestPlanet(ship, me, gameMap, true, false) == null) {
                            allPlanetsBeenCaptured = true;
                            Log.log("ALL PLANETS BEEN CAPTURED!");
                        }

                        Ship priorityShip;
                        Ship enemyShip = ship.findNearestDockingEnemy(gameMap, me);
                        if (enemyShip == null) {
                            enemyShip = ship.findNearestEnemy(gameMap, me, true);
                        }
                        Ship mobileEnemy = ship.findNearestEnemy(gameMap, me, false);
                        if (enemyShip == null) {
                            enemyShip = mobileEnemy;
                        }

                        double mDist = ship.getDistanceTo(mobileEnemy);
                        if (mDist < SAFETY_FACTOR) {
                            priorityShip = mobileEnemy;
                        } else if(!enemyShip.equals(mobileEnemy) && mDist < ship.getDistanceTo(enemyShip)) {
                            priorityShip = mobileEnemy;
                        } else {
                            priorityShip = enemyShip;
                        }

                        boolean planetSafe = nearestUnowned != null && ship.getDistanceTo(nearestUnowned) < 100;
                        boolean shipSafe = priorityShip != null;
                        boolean attackAlert = ship.getDistanceTo(priorityShip) < SAFETY_FACTOR;
                        boolean dockable = (nearestUnowned != null && (!nearestUnowned.isOwned() || nearestUnowned
                                .getOwner() == me.getId()) && !nearestUnowned.isFull());


                        boolean shipCloser = planetSafe && shipSafe && ship.getDistanceTo(priorityShip) < ship
                                .getDistanceTo(nearestUnowned);


                        ThrustMove newThrustMove = null;
                        if (priorityShip != null && (shipCloser || attackAlert || !planetSafe || !dockable)) {
                            //if nearest enemy ship exists navigate to attack
                            if (priorityShip.isDocked()) {
                                newThrustMove = Navigation.attack(gameMap, ship, priorityShip, 7);
                            } else {
                                newThrustMove = Navigation.speedSensitiveattack(gameMap, ship, priorityShip);
                            }
                        } else if (planetSafe && dockable) {
                            if (ship.canDock(nearestUnowned)) {
                                moveList.add(new DockMove(ship, nearestUnowned));
                                continue;
                            }
                            newThrustMove = Navigation.navigateShipToDock(gameMap, ship, nearestUnowned, 7);
                        } else if (shipSafe) {
                            newThrustMove = Navigation.attack(gameMap, ship, enemyShip, 7);
                        }

                        if (newThrustMove != null) {
                            moveList.add(newThrustMove);
                        } else {
                            moveList.add(Navigation.emptyThrustMove(ship));
                        }

                    }
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}