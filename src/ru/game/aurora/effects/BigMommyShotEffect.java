/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 22.03.13
 * Time: 15:17
 */

package ru.game.aurora.effects;


import org.newdawn.slick.Animation;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.world.Positionable;
import ru.game.aurora.world.World;

public class BigMommyShotEffect extends BlasterShotEffect {
    private static final long serialVersionUID = 5526660301767290658L;

    private final transient Animation animation;

    public BigMommyShotEffect(World world, Positionable source, int targetScreenX, int targetScreenY, Camera camera, int moveSpeed) {
        super(source, targetScreenX, targetScreenY, camera, moveSpeed, "blaster_shot", world.getCurrentStarSystem());
        animation = ResourceManager.getInstance().getAnimation("big_mommy");
        animation.setLooping(false);
    }

    @Override
    public void draw(GameContainer container, Graphics graphics, Camera camera, World world) {
        graphics.drawAnimation(animation, currentPos.x, currentPos.y);
    }

    @Override
    public void update(GameContainer container, World world) {
        super.update(container, world);
        if (isOver()) {
            world.getCurrentStarSystem().addEffect(
                    new ExplosionEffect(
                            (int) currentPos.x
                            , (int) currentPos.y
                            , "ship_explosion"
                            , true
                            , true
                    )
            );
        }
    }
}
