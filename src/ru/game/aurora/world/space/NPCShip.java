/**
 * User: jedi-philosopher
 * Date: 09.12.12
 * Time: 18:34
 */
package ru.game.aurora.world.space;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import ru.game.aurora.application.*;
import ru.game.aurora.dialog.Dialog;
import ru.game.aurora.effects.Effect;
import ru.game.aurora.effects.ExplosionEffect;
import ru.game.aurora.music.MusicDialogListener;
import ru.game.aurora.music.Playlist;
import ru.game.aurora.npc.AlienRace;
import ru.game.aurora.npc.Faction;
import ru.game.aurora.npc.NPC;
import ru.game.aurora.npc.shipai.CombatAI;
import ru.game.aurora.npc.shipai.LeaveSystemAI;
import ru.game.aurora.npc.shipai.NPCShipAI;
import ru.game.aurora.util.GameTimer;
import ru.game.aurora.util.ProbabilitySet;
import ru.game.aurora.world.*;
import ru.game.aurora.world.equip.TorpedoLauncher;
import ru.game.aurora.world.equip.WeaponDesc;
import ru.game.aurora.world.equip.WeaponInstance;
import ru.game.aurora.world.planet.MonsterBehaviour;
import ru.game.aurora.world.space.ships.ShipDesc;
import ru.game.aurora.world.space.ships.ShipItem;

import java.util.*;

public class NPCShip extends BaseGameObject implements IMonster, ShipItem {

    private static final long serialVersionUID = 2L;

    private String shipId;

    protected int maxHP;
    protected String name;

    protected NPC captain;
    protected int hp;
    protected int speed = 3;

    protected int curSpeed = 3;

    protected NPCShipAI ai;

    protected boolean isHostile;

    protected boolean canBeHailed = true;

    protected List<WeaponInstance> weapons;

    // this ship can not move
    private boolean isStationary;

    // if set to true, this ship will 100% drop some loot
    private boolean forceLootDrop;

    private GameTimer repairTimer = null;

    // map of loot that can be dropped by this ship, with its chances
    private ProbabilitySet<GameObject> loot;

    private transient Map<GameObject, Integer> threatMap = new WeakHashMap<>();

    public NPCShip(String shipId){
        super();
        this.shipId = shipId;

        ShipDesc shipDesc = getDesc();
        if(this.shipId == null){
            throw new NullPointerException("Ship Description can not be null");
        }
        setSprite(shipDesc.getDrawable());

        this.name = shipDesc.getDefaultName();
        this.maxHP = hp = shipDesc.getMaxHp();
        this.faction = shipDesc.getRace();
        this.isStationary = shipDesc.isStationary();
    }

    public NPCShip(String shipId, int x, int y){
        this(shipId);
        setPos(x, y);
    }

    public NPCShip(String shipId, int x, int y, String name){
        this(shipId, x, y);
        this.name = name;
    }

    public NPCShip(String shipId, int x, int y, String name, int maxHP){
        this(shipId, x, y, name);
        this.maxHP = this.hp = maxHP;
    }

    public NPCShip(String shipId, int x, int y, String name, int maxHP, Faction race, NPC captain) {
        this(shipId, x, y, name, maxHP);
        this.faction = race;
        this.captain = captain;
    }

    @Override
    public ShipDesc getDesc() {
        if(shipId == null){ // This can happen when the game is loaded from a version less than 0.6.0
            // hack: use sprite id for old ships names
            // todo: fix the hack after the abolition of support for older saved games
            shipId = drawable.getId();
        }

        return ResourceManager.getInstance().getShipDescs().getEntity(shipId);
    }

    @Override
    public boolean isDodged() {
        if(CommonRandom.getRandom().nextFloat() * 100.0f < getDesc().getDodgeChance()){
            return true;
        }
        else{
            return false;
        }
    }

    public void setForceLootDrop(boolean forceLootDrop) {
        this.forceLootDrop = forceLootDrop;
    }

    public void setLoot(ProbabilitySet<GameObject> loot) {
        this.loot = loot;
    }

    public void setAi(NPCShipAI ai) {
        this.ai = ai;
    }

    @Override
    public void update(GameContainer container, World world) {
        if (!isAlive()) {
            return;
        }
        Ship player = world.getPlayer().getShip();

        super.update(container, world);

        if (!world.isUpdatedThisFrame()) {
            return;
        }

        if (weapons != null) {
            for (WeaponInstance w : weapons) {
                w.reload();
            }
        }
        curSpeed--;
        if (repairTimer != null && hp < maxHP && repairTimer.update()) {
            ++hp;
        }
        if (curSpeed > 0) {
            return;
        }
        curSpeed = speed;
        StarSystem ss = world.getCurrentStarSystem();
        if (ss == null) {
            return;
        }

        if (threatMap == null) {
            threatMap = new HashMap<>();
        }

        for (GameObject so : ss.getShips()) {
            if (so.canBeAttacked() && !threatMap.containsKey(so) && isHostile(world, so)) {
                threatMap.put(so, 0);
            }
        }
        if (!threatMap.containsKey(player) && isHostile(world, player)) {
            threatMap.put(player, 0);
        }

        updateThreatMap(world);

        if (ai == null || ai.isOverridable()) {
            while (!threatMap.isEmpty()) {
                GameObject mostThreatTarget = getMostThreatTarget();
                if (mostThreatTarget != null) {
                    if (!mostThreatTarget.isAlive()) {
                        threatMap.remove(mostThreatTarget);
                        continue;
                    }
                    ai = new CombatAI(mostThreatTarget);
                    break;
                }
            }

            if (threatMap.isEmpty() && ai instanceof CombatAI && !isStationary) {
                ai = new LeaveSystemAI();
            }
        }

        if (ai != null) {
            ai.update(this, world, (StarSystem) world.getCurrentRoom());
        }
    }

    public void enableRepairs(int turnsPerHp) {
        repairTimer = new GameTimer(turnsPerHp);
    }

    @Override
    public void draw(GameContainer container, Graphics g, Camera camera, World world) {
        if(drawable == null){
            // todo: need make drawable object transient
            setSprite(getDesc().getDrawable());
        }

        super.draw(container, g, camera, world);
        if (hp < 3) {
            g.setColor(Color.red);
        } else {
            g.setColor(Color.white);
        }

        String hpText;
        if (hp < 100) {
            hpText = Integer.toString(Math.max(0, hp));
        } else {
            hpText = "N/A";
        }
        g.drawString(hpText, camera.getXCoord(x) + getOffsetX(), camera.getYCoord(y) + getOffsetY());
    }

    /**
     * Returns true if this ship is hostile to player
     * Hostile ships can not be hailed and will attack player when they see it
     */
    public boolean isHostile(World world, GameObject object) {
        return (object instanceof Ship && isHostile)
                || (faction != null && faction.isHostileTo(world, object));
    }

    @Override
    public boolean isAlive() {
        boolean rz = isAlive && hp > 0;
        if (ai != null) {
            rz &= ai.isAlive();
        }
        return rz;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    @Override
    public String getName() {
        //todo: localize
        return name;
    }

    public String getScanDescription(World world) {
        return String.format(Localization.getText("gui", "scan.ship.race"), faction != null ? faction.getName() : "Unknown") + '\n' + Localization.getText("gui", "scan.ship.relation_prefix") + " " + (isHostile(world, world.getPlayer().getShip()) ? Localization.getText("gui", "scan.ship.hostile") : Localization.getText("gui", "scan.ship.friendly"));
    }

    @Override
    public boolean interact(World world) {
        if (!isCanBeHailed() || isHostile(world, world.getPlayer().getShip())) {
            GameLogger.getInstance().logMessage(Localization.getText("gui", "space.hail_not_responded"));
            return false;
        }

        world.onPlayerContactedAlienShip(this);
        if (captain != null) {
            // play NPC custom music
            if(captain.getCustomMusicId() != null){
                Playlist customPlaylist = ResourceManager.getInstance().getPlaylist(captain.getCustomMusicId());
                if(!customPlaylist.isPlaying()){
                    customPlaylist.play();
                }
            }

            // fire this listener after NPC dialog end
            if(captain.getMusicListener() != null){
                captain.getCustomDialog().addListener(captain.getMusicListener());
            }


            if(captain.getCustomDialog() != null){
                world.addOverlayWindow(captain.getCustomDialog());
            }
            return true;
        }

        if (faction != null && (faction instanceof AlienRace)) {
            AlienRace race = (AlienRace) faction;
            Dialog d = race.getDefaultDialog();
            if (race.getMusic() != null && !race.getMusic().isPlaying()) {
                d.addListener(new MusicDialogListener(Playlist.getCurrentPlaylist().getId()));
                race.getMusic().play();
            }
            world.addOverlayWindow(d);
        }

        return true;
    }


    @Override
    public void onAttack(World world, GameObject attacker, int dmg) {
        hp -= dmg;
        final StarSystem currentStarSystem = world.getCurrentStarSystem();
        if (hp <= 0) {
            explode(currentStarSystem);
        }
        world.onGameObjectAttacked(attacker, this, dmg);

        if (attacker != null) {
            changeThreat(world, attacker, dmg * 2);   //todo: balance

            if (faction != null && !currentStarSystem.getReputation().isHostile(faction.getName(), attacker.getFaction().getName())) {
                currentStarSystem.getReputation().setHostile(faction.getName(), attacker.getFaction().getName());
            }
        }
    }

    public void explode(StarSystem currentStarSystem) {
        hp = 0;
        GameLogger.getInstance().logMessage(getName() + " " + Localization.getText("gui", "space.destroyed"));
        currentStarSystem.addEffect(new ExplosionEffect(x, y, "ship_explosion", false, true));

        if (loot != null) {
            if (forceLootDrop || CommonRandom.getRandom().nextDouble() < Configuration.getDoubleProperty("ship.drop_chance")) {
                currentStarSystem.getShips().add(new SpaceDebris(x, y, loot));
            }
        }
    }

    public List<WeaponInstance> getWeapons() {
        return weapons;
    }

    public void setWeapons(WeaponInstance... weapons) {
        this.weapons = new ArrayList<>(weapons.length);
        Collections.addAll(this.weapons, weapons);
    }

    public void setWeapons(WeaponDesc... weaponDescs) {
        this.weapons = new ArrayList<>(weaponDescs.length);
        for (WeaponDesc weaponDesc : weaponDescs) {
            this.weapons.add(new WeaponInstance(weaponDesc));
        }
    }

    @Override
    public MonsterBehaviour getBehaviour() {
        return null;
    }

    public void fire(World world, StarSystem ss, int weaponIdx, final GameObject target) {
        final WeaponInstance weaponInstance = weapons.get(weaponIdx);
        weaponInstance.fire();
        final WeaponDesc weaponDesc = weaponInstance.getWeaponDesc();
        final int damage = weaponDesc.getDeviationDamage();

        Effect e = weaponDesc.createShotEffect(world, this, target, world.getCamera(), 800);
        if (e != null) {
            e.setEndListener(new IStateChangeListener<World>() {
                private static final long serialVersionUID = -3379281638297845046L;

                @Override
                public void stateChanged(World world) {
                    boolean dodge = false;
                    if(target instanceof ShipItem){
                        dodge = ((ShipItem)target).isDodged();
                    }

                    if(dodge){
                        GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "space.attack_dodge")
                                ,getName()
                                , target.getName()
                                , weaponDesc.getName()
                        ));
                    }
                    else{
                        target.onAttack(world, NPCShip.this, damage);
                        GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "space.attack")
                                , getName()
                                , target.getName()
                                , weaponDesc.getName()
                                , damage
                        ));
                    }

                    if (!target.isAlive()) {
                        GameLogger.getInstance().logMessage(target.getName() + " " + Localization.getText("gui", "space.destroyed"));
                    }
                }
            });
            e.setStartSound(weaponDesc.shotSound);
            ss.addEffect(e);
        }
    }

    @Deprecated
    public void move(int dx, int dy) {
        if (isStationary) {
            return;
        }
        x += dx;
        y += dy;
    }

    @Override
    public void moveDown() {
        if (isStationary) {
            return;
        }
        super.moveDown();
    }

    @Override
    public void moveLeft() {
        if (isStationary) {
            return;
        }
        super.moveLeft();
    }

    @Override
    public void moveRight() {
        if (isStationary) {
            return;
        }
        super.moveRight();
    }

    @Override
    public void moveUp() {
        if (isStationary) {
            return;
        }
        super.moveUp();
    }

    public void setHostile(boolean hostile) {
        isHostile = hostile;
    }

    public boolean isStationary() {
        return isStationary;
    }

    public void setStationary(boolean stationary) {
        isStationary = stationary;
    }

    public NPC getCaptain() {
        return captain;
    }

    public void setCaptain(NPC captain) {
        this.captain = captain;
    }

    public boolean isCanBeHailed() {
        return canBeHailed;
    }

    public void setCanBeHailed(boolean canBeHailed) {
        this.canBeHailed = canBeHailed;
    }

    //изменяем значение агро для цели. Если цели нет в списке - добавляем.
    public void changeThreat(World world, GameObject target, int amount) {
        if (threatMap == null) {
            threatMap = new WeakHashMap<>();
        }
        if (threatMap.containsKey(target)) {
            int newAmount;
            if (amount > 0) {
                newAmount = Math.min(Integer.MAX_VALUE, threatMap.get(target) + amount);    //воизбежание переполнения
            } else {
                newAmount = Math.max(0, threatMap.get(target) + amount);    //агро не может быть меньше 1
            }

            threatMap.put(target, newAmount);
        } else {
            GameLogger.getInstance().logMessage(String.format(Localization.getText("gui", "space.hostile"), getName(), target.getName()));
            if (target.equals(world.getPlayer().getShip())) {
                isHostile = true;
            }
            threatMap.put(target, Math.max(1, amount));    //агро не может быть меньше 1
        }
    }

    //цель для атаки
    public GameObject getMostThreatTarget() {
        if (!threatMap.isEmpty()) {
            GameObject mostThreat = threatMap.keySet().iterator().next();
            int maxValue = threatMap.get(mostThreat);
            for (Iterator<Map.Entry<GameObject, Integer>> iterator = threatMap.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<GameObject, Integer> entry = iterator.next();
                if (!entry.getKey().isAlive()) {
                    iterator.remove();
                    continue;
                }
                int nextValue = entry.getValue();
                if (nextValue > maxValue) {
                    mostThreat = entry.getKey();
                    maxValue = nextValue;
                }
            }
            return mostThreat;
        }
        return null;    //если в агролисте никого нет
    }

    //происходит каждый ход корабля (как часто - зависит от скорости)
    public void updateThreatMap(World world) {
        for (Object o : threatMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            GameObject ship = (GameObject) entry.getKey();
            if (ship.isAlive()) {
                //немного уменьшается агро каждой цели
                changeThreat(world, ship, -1);  //todo: balance

                //более близкие (в радиусе 2 ходов) цели становятся привлекательнее
                if (getDistance(ship) < (speed * 2)) {
                    changeThreat(world, ship, 2);   //todo: balance
                }

                if (ship instanceof TorpedoLauncher.Torpedo) {
                    // torpedoes have a highest priority
                    changeThreat(world, ship, 10);
                }
            }
        }
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    @Override
    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getMaxHP() {
        return maxHP;
    }

    public Map<GameObject, Integer> getThreatMap() {
        if (threatMap == null) {
            threatMap = new HashMap<>();
        }
        return threatMap;
    }

    @Override
    public boolean canBeAttacked() {
        return true;
    }
}
