package hlt;

import javax.xml.stream.events.EndElement;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * By Elijah Bailey
 * A class for methods involved in locating game entities and other general
 * navigation functions
 */
public class GenNav {

    /**
     * Returns the planet not owned by anyone
     *
     * @param entity      The entity that's looking for a nearby planet
     * @param gameMap     The shared 2D playing board
     * @param wantUnowned True if I want unowned planets
     * @return Planet The closest planet
     */
    public static Planet nearestPlanet(Entity entity, Player me, GameMap gameMap, boolean wantUnowned, boolean
            wantEnemy) {
        Planet toReturn = null;

        TreeMap<Double, Entity> planetMap = gameMap.nearbyPlanetsByDistance(entity);

        if (planetMap != null && !planetMap.isEmpty()) {
            for (Map.Entry<Double, Entity> ent : planetMap.entrySet()) {
                Planet planet = (Planet) ent.getValue();

                if (wantUnowned && planet.isOwned()) {
                    continue;
                }

                if (wantEnemy && planet.getOwner() == me.getId()) {
                    continue;
                }

                toReturn = planet;
                break;
            }
        }

        return toReturn;
    }

    public static Planet nearestPlanetToPosition(Position pos, Player me, GameMap gameMap, boolean wantUnowned, boolean
            wantEnemy) {
        Planet toReturn = null;

        TreeMap<Double, Planet> planetMap = gameMap.nearbyPlanetsByDistance(pos);

        if (planetMap != null && !planetMap.isEmpty()) {
            for (Map.Entry<Double, Planet> ent : planetMap.entrySet()) {
                Planet planet = ent.getValue();
                if (planet.isFull() && planet.getOwner() == me.getId()) {
                    continue;
                }

                if (wantUnowned && planet.isOwned()) {
                    continue;
                }

                if (wantEnemy && planet.getOwner() == me.getId()) {
                    continue;
                }

                toReturn = planet;
                break;
            }
        }

        return toReturn;
    }

    /**
     * Returns the nearest non full planet
     *
     * @param entity      The ship/planet that is looking for nearby planets
     * @param me          The player
     * @param gameMap     The 2D playing field
     * @param wantUnowned True if I want planets that are unoccupied
     * @return The nearest non-full planet
     */
    public static Planet nearestPlanetMining(Entity entity, Player me, GameMap gameMap, boolean wantUnowned) {
        Planet toReturn = null;

        TreeMap<Double, Entity> planetMap = gameMap.nearbyPlanetsByDistanceMining(entity);

        if (planetMap != null && !planetMap.isEmpty()) {
            for (Map.Entry<Double, Entity> ent : planetMap.entrySet()) {
                Planet planet = (Planet) ent.getValue();

                if (wantUnowned && planet.isOwned()) {
                    continue;
                }

                toReturn = planet;
                break;
            }
        }

        return toReturn;
    }

    public static Planet nearestPlanetMining(Position pos, Player me, GameMap gameMap, boolean wantUnowned) {
        Planet toReturn = null;

        TreeMap<Double, Planet> planetMap = gameMap.nearbyPlanetsByDistance(pos);

        if (planetMap != null && !planetMap.isEmpty()) {
            for (Map.Entry<Double, Planet> ent : planetMap.entrySet()) {
                Planet planet = ent.getValue();
                if (planet.isFull()) {
                    continue;
                }
                if (wantUnowned && planet.isOwned()) {
                    continue;
                }

                toReturn = planet;
                break;
            }
        }

        return toReturn;
    }


    public static Ship findNearestEnemy(Position pos, GameMap gameMap, Player me, boolean wantDocked) {
        Ship toReturn = null;
        TreeMap<Double, Entity> nearbyShips = gameMap.nearbyShipsByDistance(pos);
        if (nearbyShips != null && !nearbyShips.isEmpty()) {
            for (Map.Entry<Double, Entity> entry : nearbyShips.entrySet()) {
                Ship ship = (Ship) entry.getValue();
                if (ship.getOwner() == me.getId()) {
                    //don't want to see my ships
                    continue;
                }
                if (wantDocked && ship.getDockingStatus() == Ship.DockingStatus.Undocked) {
                    continue;
                }

                toReturn = ship;
                break;

            }
        }
        return toReturn;
    }


    /**
     * Emengencies only. For running away...
     */
    public static Planet farthestPlanet(Entity entity, Player me, GameMap gameMap, boolean wantUnowned) {
        Planet toReturn = null;

        TreeMap<Double, Entity> planetMap = gameMap.nearbyPlanetsByDistance(entity);

        if (planetMap != null && !planetMap.isEmpty()) {
            for (Map.Entry<Double, Entity> ent : planetMap.entrySet()) {
                Planet planet = (Planet) ent.getValue();

                if (wantUnowned && planet.isOwned()) {
                    continue;
                }

                toReturn = planet;
            }
        }

        return toReturn;
    }

    /**
     * Emengencies only. For running away...
     */
    public static Planet farthestPlanet(Position pos, GameMap gameMap, boolean wantUnowned) {
        Planet toReturn = null;

        TreeMap<Double, Planet> planetMap = gameMap.nearbyPlanetsByDistance(pos);
        if (planetMap != null && !planetMap.isEmpty()) {
            for (Map.Entry<Double, Planet> ent : planetMap.entrySet()) {
                Planet planet = ent.getValue();
                if (planet.isFull()) {
                    continue;
                }
                if (wantUnowned && planet.isOwned()) {
                    continue;
                }

                toReturn = planet;
            }
        }

            return toReturn;
    }

    public static Position triangulateEnemies(Entity one, Entity two, Entity three) {
        double xTot = 0;
        double yTot = 0;
        int count = 0;
        if (one != null) {
            xTot += one.getXPos();
            yTot += one.getYPos();
            count++;
        }
        if (two != null) {
            xTot += two.getXPos();
            yTot += two.getYPos();
            count++;
        }
        if (three != null) {
            xTot += three.getXPos();
            yTot += three.getYPos();
            count++;
        }
        return new Position(xTot / count, yTot / count);
    }


    public static Ship getNearestOfPlayer(Player player, Entity entity, GameMap gameMap, boolean wantDocked) {
        Ship toReturn = null;
        TreeMap<Double, Entity> nearbyShips = gameMap.getPlayerShipsByDistance(entity, player);

        if (nearbyShips != null && !nearbyShips.isEmpty()) {
            for (Map.Entry<Double, Entity> entry : nearbyShips.entrySet()) {
                Ship ship = (Ship) entry.getValue();
                if (!ship.belongsTo(player)) {
                    continue;
                }
                if (ship.equals(entity)) {
                    continue;
                }
                if (wantDocked && ship.getDockingStatus() == Ship.DockingStatus.Undocked) {
                    continue;
                }

                toReturn = ship;
                break;

            }
        }
        return toReturn;
    }

    public static Planet getBigPlanetNearby(Entity entity, Player me, GameMap gameMap, boolean wantUnowned, boolean
            wantEnemy) {
        Planet toReturn = null;

        TreeMap<Double, Entity> planetMap = gameMap.nearbyPlanetsByDistance(entity);

        if (planetMap != null && !planetMap.isEmpty()) {
            for (Map.Entry<Double, Entity> ent : planetMap.entrySet()) {
                Planet planet = (Planet) ent.getValue();
                if (planet.getDockingSpots() < 3) {
                    continue;
                }
                if (wantUnowned && planet.isOwned()) {
                    continue;
                }
                if (wantEnemy && planet.getOwner() == me.getId()) {
                    continue;
                }
                toReturn = planet;
                break;
            }
        }

        return toReturn;
    }
}
