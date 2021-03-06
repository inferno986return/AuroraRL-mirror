package ru.game.aurora.world.dungeon;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.game.aurora.application.Camera;
import ru.game.aurora.application.GameLogger;
import ru.game.aurora.application.Localization;
import ru.game.aurora.application.ResourceManager;
import ru.game.aurora.world.*;
import ru.game.aurora.world.equip.WeaponInstance;
import ru.game.aurora.world.planet.MonsterBehaviour;
import ru.game.aurora.world.planet.MonsterDesc;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 13.09.13
 * Time: 12:40
 */
public class DungeonMonster extends DungeonObject implements IMonster {
    protected static final Logger logger = LoggerFactory.getLogger(DungeonMonster.class);
    private static final long serialVersionUID = 3L;
    private final ITileMap owner;
    private final MonsterDesc desc;
    protected int hp;
    protected MonsterController controller;
    private Set<String> tags = null;
    private MonsterBehaviour behaviour;
    private List<WeaponInstance> weapons = new ArrayList<>();

    public DungeonMonster(AuroraTiledMap map, int groupId, int objectId) {
        super(map, groupId, objectId);
        owner = map;
        // overrider behaviour set in desc,
        desc = ResourceManager.getInstance().getMonsterDescs().getEntity(map.getMap().getObjectProperty(groupId, objectId, "id", null));
        hp = desc.hp;
        drawable = desc.getDrawable();
        behaviour = MonsterBehaviour.valueOf(map.getMap().getObjectProperty(groupId, objectId, "behaviour", desc.behaviour.name()));
        final String tagsString = map.getMap().getObjectProperty(groupId, objectId, "tags", null);
        if (tagsString != null) {
            tags = new HashSet<>();
            Collections.addAll(tags, tagsString.split(","));
        }
        if (desc.weaponId != null) {
            weapons.add(new WeaponInstance(ResourceManager.getInstance().getWeapons().getEntity(desc.weaponId)));
        }

        controller = new MonsterController(map, this);
    }

    public DungeonMonster(String name, int x, int y, Set<String> tags, MonsterController controller, ITileMap owner, MonsterDesc desc, List<WeaponInstance> weapons) {
        super(name, x, y);
        this.tags = tags;
        this.hp = desc.hp;
        this.controller = controller;
        this.owner = owner;
        this.desc = desc;
        this.behaviour = desc.behaviour;
        this.weapons = weapons;

        drawable = desc.getDrawable();
    }

    @Override
    public boolean isAlive() {
        return hp > 0;
    }

    @Override
    public void update(GameContainer container, World world) {
        super.update(container, world);
        if (behaviour == MonsterBehaviour.AGGRESSIVE || behaviour == MonsterBehaviour.FRIENDLY) {
            controller.update(container, world);
        }
    }

    @Override
    public boolean canBeAttacked() {
        return behaviour != MonsterBehaviour.FRIENDLY && hp > 0;
    }

    @Override
    public void onAttack(World world, GameObject attacker, int damage) {
        hp -= damage;
        super.onAttack(world, attacker, damage);
        if (hp <= 0) {
            // clean obstacle flag
            if (!nowMoving()) {
                owner.setTilePassable(x, y, true);
            } else {
                owner.setTilePassable(getTargetX(), getTargetY(), true);
            }
            GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "surface.killed_message"), getName()));
            world.getCurrentRoom().getMap().getObjects().remove(this);
        }
    }

    public Set<String> getTags() {
        return tags;
    }

    @Override
    public int getHp() {
        return hp;
    }

    @Override
    public int getSpeed() {
        return desc.turnsBetweenMoves;
    }

    @Override
    public List<WeaponInstance> getWeapons() {
        return weapons;
    }

    public MonsterBehaviour getBehaviour() {
        return behaviour;
    }

    public void setBehaviour(MonsterBehaviour behaviour) {
        this.behaviour = behaviour;
    }

    @Override
    public void draw(GameContainer container, Graphics graphics, Camera camera, World world) {
        super.draw(container, graphics, camera, world);
        String hpText;
        if (hp < 100) {
            hpText = Integer.toString(Math.max(0, hp));
        } else {
            hpText = "N/A";
        }
        if (hp < desc.hp / 4) {
            graphics.setColor(Color.red);
        } else {
            graphics.setColor(Color.white);
        }
        graphics.drawString(hpText, camera.getXCoord(x) + getOffsetX(), camera.getYCoord(y) + getOffsetY());
    }

    @Override
    public String getName() {
        String rz = Localization.getText("monsters", desc.getId() + ".name");
        return rz != null ? rz : "";
    }
}
