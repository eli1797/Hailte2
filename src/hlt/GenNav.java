package hlt;

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
     * @param entity The entity that's looking for a nearby planet
     * @param gameMap The shared 2D playing board
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
     * @param entity The ship/planet that is looking for nearby planets
     * @param me The player
     * @param gameMap The 2D playing field
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
}
