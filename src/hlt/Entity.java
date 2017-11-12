package hlt;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Entity extends Position {

    private final int owner;
    private final int id;
    private final int health;
    private final double radius;

    public Entity(final int owner, final int id, final double xPos, final double yPos, final int health, final double radius) {
        super(xPos, yPos);
        this.owner = owner;
        this.id = id;
        this.health = health;
        this.radius = radius;
    }

    public int getOwner() {
        return owner;
    }

    public int getId() {
        return id;
    }

    public int getHealth() {
        return health;
    }

    public double getRadius() {
        return radius;
    }

    public Ship findNearestEnemy(GameMap gameMap, Player me, boolean wantDocked) {
        Ship toReturn = null;
        TreeMap<Double, Entity> nearbyShips = gameMap.nearbyShipsByDistance(this);
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
     * Gets the nearest ship to the desired player
     * @param wantDocked True if I only want docked ships
     */
    public Ship findNearestPlayersShip(GameMap gameMap, Player wanted, boolean wantDocked) {
        Ship toReturn = null;
        TreeMap<Double, Entity> nearbyShips = gameMap.nearbyShipsByDistance(this);
        if (nearbyShips != null && !nearbyShips.isEmpty()) {
            for (Map.Entry<Double, Entity> entry : nearbyShips.entrySet()) {
                Ship ship = (Ship) entry.getValue();
                if (ship.equals(this)) {
                    //don't want myself
                    continue;
                }

                if (ship.getOwner() != wanted.getId()) {
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

    @Override
    public String toString() {
        return "Entity[" +
                super.toString() +
                ", owner=" + owner +
                ", id=" + id +
                ", health=" + health +
                ", radius=" + radius +
                "]";
    }
}
