package ru.game.aurora.world.equip;

import org.newdawn.slick.GameContainer;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.CommonRandom;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.common.Drawable;
import ru.game.aurora.effects.Effect;
import ru.game.aurora.effects.ExplosionEffect;
import ru.game.aurora.npc.Faction;
import ru.game.aurora.npc.shipai.LandAI;
import ru.game.aurora.world.BasePositionable;
import ru.game.aurora.world.GameObject;
import ru.game.aurora.world.World;
import ru.game.aurora.world.space.NPCShip;

/**
 * Fires a huge torpedo. Torpedo is like a ship, has a movement speed and can be shot. Deals a lot of damage.
 */
public class TorpedoLauncher extends WeaponDesc {

    public TorpedoLauncher(String id, Drawable drawable, int damage, int range, int price, String shotImage, String shotSound, int reloadTurns, String explosionAnimation, String particlesAnimation, int size) {
        super(id, drawable, damage, 0.25f, range, price, shotImage, shotSound, reloadTurns, explosionAnimation, particlesAnimation, size);
    }

    @Override
    public Effect createShotEffect(World world, GameObject shooter, GameObject target, Camera camera, int moveSpeed) {
        Torpedo t = new Torpedo(shooter.getTargetX(), shooter.getTargetY(), target, shooter.getFaction(), getDeviationDamage());
        world.getCurrentRoom().getMap().getObjects().add(t);
        // dummy attack for 0 damage to make targeted ship hostile and to enable its Combat AI so that it could shot down torpedoes
        target.onAttack(world, shooter, 0);
        return null;
    }

    public static class Torpedo extends NPCShip {

        private static final long serialVersionUID = -5197035695990846257L;

        private GameObject target;

        private int ttl;

        private int damage;

        public Torpedo(int x, int y, GameObject target, Faction faction, int damage) {
            super("torpedo", x, y);
            setFaction(faction);
            setSpeed(1);
            this.target = target;
            final LandAI ai = new LandAI(target);
            ai.setIsOverridable(false);
            setAi(ai);
            setMovementSpeed(2);
            ttl = 20;
            this.damage = damage;
        }

        @Override
        protected void movementCompleted() {
            if (BasePositionable.getDistance(this, target) == 0) {
                target.onAttack(World.getWorld(), this, damage);
                World.getWorld().getCurrentRoom().getMap().getEffects().add(new ExplosionEffect(x, y, "ship_explosion", false, true));
                isAlive = false;
            }
        }

        @Override
        public void onAttack(World world, GameObject attacker, int dmg) {
            if (CommonRandom.getRandom().nextDouble() > 0.5) {
                super.onAttack(world, attacker, dmg);
            } else {
                GameLogger.getInstance().logMessage(Localization.getText("gui", "space.torpedo_missed"));
            }
        }

        @Override
        public void update(GameContainer container, World world) {
            if (!isAlive()) {
                return;
            }
            if (BasePositionable.getDistance(this, target) < 2) {
                setMovementSpeed(1);
            }
            super.update(container, world);
            if (world.isUpdatedThisFrame()) {
                if (BasePositionable.getDistance(this, target) == 0) {
                    target.onAttack(world, this, damage);
                    world.getCurrentRoom().getMap().getEffects().add(new ExplosionEffect(x, y, "ship_explosion", false, true));
                    isAlive = false;
                } else if (ttl-- <= 0 || !target.isAlive()) {
                    isAlive = false;
                    GameLogger.getInstance().logMessage(Localization.getText("gui", "torpedo.selfdestruct"));
                    world.getCurrentRoom().getMap().getEffects().add(new ExplosionEffect(x, y, "ship_explosion", false, true));
                }

            }
        }
    }
}
