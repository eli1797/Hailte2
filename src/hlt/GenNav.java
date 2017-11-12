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
}
