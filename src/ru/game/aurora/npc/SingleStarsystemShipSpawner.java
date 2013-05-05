/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 15.04.13
 * Time: 17:54
 */
package ru.game.aurora.npc;


import ru.game.aurora.application.CommonRandom;
import ru.game.aurora.world.GameEventListener;
import ru.game.aurora.world.World;
import ru.game.aurora.world.space.NPCShip;
import ru.game.aurora.world.space.StarSystem;

/**
 * Just spawns ships in given star system
 */
public class SingleStarsystemShipSpawner extends GameEventListener
{
    private static final long serialVersionUID = 3478980636928820446L;

    private NPCShipFactory factory;

    private double probability;

    private StarSystem targetSystem;

    public SingleStarsystemShipSpawner(NPCShipFactory factory, double probability, StarSystem targetSystem) {
        this.factory = factory;
        this.probability = probability;
        this.targetSystem = targetSystem;
    }

    @Override
    public void onPlayerEnterStarSystem(World world, StarSystem ss) {
        if (ss != targetSystem) {
            return;
        }

        if (CommonRandom.getRandom().nextDouble() > probability) {
            return;
        }

        NPCShip ship = factory.createShip();
        ship.setPos(CommonRandom.getRandom().nextInt(ss.getRadius()) - ss.getRadius() / 2, CommonRandom.getRandom().nextInt(ss.getRadius()) - ss.getRadius() / 2);
        ss.getShips().add(ship);
    }
}