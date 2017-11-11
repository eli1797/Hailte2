import hlt.*;

import javax.print.attribute.standard.MediaSize;
import java.net.HttpRetryException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MyBot3 {

    private static final int DOCK_FACTOR = 60;

    private static final int CHASE_FACTOR = 30;

    private static final int SAFETY_FACTOR = 70;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("3");

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
        Position avgHome = null;
        Map<Double, Entity> planetsToHome = null;

        /* Game Analysis */
        int numPlayers = gameMap.getAllPlayers().size();
        Player me = gameMap.getMyPlayer();

        /*
        Game Play
         */

        final ArrayList<Move> moveList = new ArrayList<>();
        for (int i = 1;; i++) {
            moveList.clear();
            networking.updateMap(gameMap);


            Log.log("Turn is  " + i + "   ");

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    if (numPlayers == 2) {
                        Ship nearestEnemy = ship.findNearestEnemy(gameMap, me, false);
                        if (ship.getDistanceTo(nearestEnemy) < SAFETY_FACTOR) {
                            moveList.add(new UndockMove(ship));
                        }
                    }
                    continue;
                }

                if (numPlayers == 2) {
                    /* Two Player Mode */


                } else {
                    /* Four Player Mode */
                }
            }
            Networking.sendMoves(moveList);
        }
    }
}
