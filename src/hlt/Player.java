package hlt;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Player {

    private final Map<Integer, Ship> ships;
    private List<Ship> shipList;
    private final int id;

    public Player(final int id, Map<Integer, Ship> ships) {
        this.id = id;
        this.ships = Collections.unmodifiableMap(ships);
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

    public List<Ship> getShipList() {
        return shipList;
    }

    public void updateStats(Metadata playerMetadata) {
        final Map<Integer, Ship> currentPlayerShips = new TreeMap<>();

        final Player currentPlayer = this;
        MetadataParser.populateShipList(shipList, id, playerMetadata);

        for (final Ship ship : shipList) {
            ships.put(ship.getId(), ship);
        }
    }

}
