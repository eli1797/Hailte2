package hlt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Collection;

public class GameMap {
    private final int width, height;
    private final int playerId;
    private final List<Player> players;
    private final List<Player> playersUnmodifiable;
    private final Map<Integer, Planet> planets;
    private final List<Ship> allShips;
    private final List<Ship> allShipsUnmodifiable;

    // used only during parsing to reduce memory allocations
    private final List<Ship> currentShips = new ArrayList<>();

    public GameMap(final int width, final int height, final int playerId) {
        this.width = width;
        this.height = height;
        this.playerId = playerId;
        players = new ArrayList<>(Constants.MAX_PLAYERS);
        playersUnmodifiable = Collections.unmodifiableList(players);
        planets = new TreeMap<>();
        allShips = new ArrayList<>();
        allShipsUnmodifiable = Collections.unmodifiableList(allShips);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getMyPlayerId() {
        return playerId;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Player> getAllPlayers() {
        return playersUnmodifiable;
    }

    public Player getMyPlayer() {
        return getAllPlayers().get(getMyPlayerId());
    }

    public Ship getShip(final int playerId, final int entityId) throws IndexOutOfBoundsException {
        return players.get(playerId).getShip(entityId);
    }

    public Planet getPlanet(final int entityId) {
        return planets.get(entityId);
    }

    public Map<Integer, Planet> getAllPlanets() {
        return planets;
    }

    public List<Ship> getAllShips() {
        return allShipsUnmodifiable;
    }

    public ArrayList<Entity> objectsBetween(Position start, Position target) {
        final ArrayList<Entity> entitiesFound = new ArrayList<>();

        addEntitiesBetween(entitiesFound, start, target, planets.values());
        addEntitiesBetween(entitiesFound, start, target, allShips);

        return entitiesFound;
    }

    private static void addEntitiesBetween(final List<Entity> entitiesFound,
                                           final Position start, final Position target,
                                           final Collection<? extends Entity> entitiesToCheck) {

        for (final Entity entity : entitiesToCheck) {
            if (entity.equals(start) || entity.equals(target)) {
                continue;
            }
            if (Collision.segmentCircleIntersect(start, target, entity, Constants.FORECAST_FUDGE_FACTOR)) {
                entitiesFound.add(entity);
            }
        }
    }

    public Map<Double, Entity> nearbyEntitiesByDistance(final Entity entity) {
        final Map<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(planet), planet);
        }

        for (final Ship ship : allShips) {
            if (ship.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(ship), ship);
        }

        return entityByDistance;
    }

    public TreeMap<Double, Entity> nearbyShipsByDistance(final Entity entity) {
        final TreeMap<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Ship ship : allShips) {
            if (ship.equals(entity)) {
                //since I called the method with the entity I'll pass over it
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(ship), ship);
        }

        return entityByDistance;
    }

    public TreeMap<Double, Entity> nearbyShipsByDistance(final Position pos) {
        final TreeMap<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Ship ship : allShips) {
            entityByDistance.put(pos.getDistanceTo(ship), ship);
        }

        return entityByDistance;
    }

    public TreeMap<Double, Entity> nearbyPlanetsByDistance(final Entity entity) {
        final TreeMap<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.equals(entity)) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(planet), planet);
        }

        return entityByDistance;
    }

    public TreeMap<Double, Planet> nearbyPlanetsByDistance(final Position position) {
        final TreeMap<Double, Planet> entityByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            entityByDistance.put(position.getDistanceTo(planet), planet);
        }

        return entityByDistance;
    }

    public TreeMap<Double, Entity> nearbyPlanetsByDistanceMining(final Entity entity) {
        final TreeMap<Double, Entity> entityByDistance = new TreeMap<>();

        for (final Planet planet : planets.values()) {
            if (planet.isFull()) {
                continue;
            }
            entityByDistance.put(entity.getDistanceTo(planet), planet);
        }

        return entityByDistance;
    }

    public List<Planet> getPlayerPlanets(Player player) {
        List<Planet> myPlanets = new ArrayList<>();

        for (final Planet planet : planets.values()) {
            if (planet.getOwner() != player.getId()) {
                continue;
            }
            myPlanets.add(planet);
        }
        return myPlanets;
    }

    public TreeMap<Double, Entity> getPlayerShipsByDistance(final Entity entity, Player player) {
        TreeMap<Double, Entity> playerShips = new TreeMap<>();

        for (final Ship ship : allShips) {
            if (ship.getOwner() != player.getId()) {
                continue;
            }
            playerShips.put(entity.getDistanceTo(ship), ship);
        }
        return playerShips;
    }

    public GameMap updateMap(final Metadata mapMetadata) {
        final int numberOfPlayers = MetadataParser.parsePlayerNum(mapMetadata);

        players.clear();
        planets.clear();
        allShips.clear();

        // update players info
        for (int i = 0; i < numberOfPlayers; ++i) {
            currentShips.clear();
            final Map<Integer, Ship> currentPlayerShips = new TreeMap<>();
            final int playerId = MetadataParser.parsePlayerId(mapMetadata);

//            final Player currentPlayer = new Player(playerId, currentPlayerShips);
            MetadataParser.populateShipList(currentShips, playerId, mapMetadata);
            allShips.addAll(currentShips);

            int dockedCounter = 0;
            for (final Ship ship : currentShips) {
                if (ship.isDocked()) {
                    dockedCounter++;
                }
                currentPlayerShips.put(ship.getId(), ship);
            }

            final Player currentPlayer = new Player(playerId, currentPlayerShips, dockedCounter);
            players.add(currentPlayer);
        }

        final int numberOfPlanets = Integer.parseInt(mapMetadata.pop());

        for (int i = 0; i < numberOfPlanets; ++i) {
            final List<Integer> dockedShips = new ArrayList<>();
            final Planet planet = MetadataParser.newPlanetFromMetadata(dockedShips, mapMetadata);
            planets.put(planet.getId(), planet);
        }

        if (!mapMetadata.isEmpty()) {
            throw new IllegalStateException("Failed to parse data from Halite game engine. Please contact maintainers.");
        }

        return this;
    }
}
