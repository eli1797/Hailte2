package hlt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Player {

    private final Map<Integer, Ship> ships;
    private int dockedShips = -1;
    private final int id;

    public Player(final int id, Map<Integer, Ship> ships) {
        this.id = id;
        this.ships = Collections.unmodifiableMap(ships);
    }

    public Player(final int id, Map<Integer, Ship> ships, int dockedShips) {
        this(id, ships);
        this.dockedShips = dockedShips;
    }

    public Map<Integer, Ship> getShips() {
        return ships;
    }

    public Ship getShip(final int entityId) {
        return ships.get(entityId);
    }

    public int getId() {
        return id;
    }

    public int getDockedShips() {
        return dockedShips;
    }


}
