/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 15.01.13
 * Time: 13:05
 */
package ru.game.aurora.world.equip;


import ru.game.aurora.application.JsonConfigManager;

import java.io.Serializable;

public class StarshipWeaponDesc implements Serializable, JsonConfigManager.EntityWithId
{
    private static final long serialVersionUID = 1L;

    public final String id;

    public final int damage;

    public final String name;

    public final String desc;

    public final String shotSprite;

    public final int range;

    public final int reloadTurns;

    public final String image;

    public StarshipWeaponDesc(int damage, String id, String name, String desc, String shotSprite, int range, int reloadTurns, String image) {
        this.id = id;
        this.damage = damage;
        this.name = name;
        this.desc = desc;
        this.shotSprite = shotSprite;
        this.range = range;
        this.reloadTurns = reloadTurns;
        this.image = image;
    }

    @Override
    public String getId() {
        return id;
    }
}
