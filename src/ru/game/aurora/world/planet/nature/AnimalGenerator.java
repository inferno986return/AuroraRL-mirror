package ru.game.aurora.world.planet.nature;

import org.newdawn.slick.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.game.aurora.application.*;
import ru.game.aurora.frankenstein.Slick2DColor;
import ru.game.aurora.frankenstein.Slick2DFrankensteinImage;
import ru.game.aurora.frankenstein.Slick2DImageFactory;
import ru.game.aurora.util.EngineUtils;
import ru.game.aurora.util.ProbabilitySet;
import ru.game.aurora.world.equip.WeaponDesc;
import ru.game.aurora.world.planet.Planet;
import ru.game.frankenstein.*;
import ru.game.frankenstein.impl.MonsterPartsLoader;
import ru.game.frankenstein.util.CollectionUtils;
import ru.game.frankenstein.util.Size;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * Generates random alien animals
 */
public class AnimalGenerator {
    public static final Color[] supportedColors = {new Color(0x00697436), new Color(0x00a12e00), new Color(0x00ad5400), new Color(0x005f4d96), new Color(0x00966e00)};
    private static final Logger logger = LoggerFactory.getLogger(AnimalGenerator.class);
    private static AnimalGenerator instance;
    private final ProbabilitySet<AnimalModifier> modifierProbabilitySet = new ProbabilitySet<>();
    private MonsterGenerator monsterGenerator;
    private MonsterGenerator plantGenerator;
    private MonsterGenerationParams monsterGenerationParams;
    private MonsterGenerationParams plantGenerationParams;
    private int baseHp;
    private int baseDmg;
    private double rangeAttackChance;

    public AnimalGenerator() throws FileNotFoundException {
        try {
            ImageFactory imageFactory = new Slick2DImageFactory(new File("resources/animal_parts"));
            MonsterPartsSet monsterPartsSet = MonsterPartsLoader.loadFromJSON(imageFactory, new File("resources/animal_parts/parts_library.json"));
            monsterGenerator = new MonsterGenerator(imageFactory, monsterPartsSet);

            imageFactory = new Slick2DImageFactory(new File("resources/plant_parts"));
            MonsterPartsSet plantsPartSet = MonsterPartsLoader.loadFromJSON(imageFactory, new File("resources/plant_parts/parts_library.json"));
            plantGenerator = new MonsterGenerator(imageFactory, plantsPartSet);

            monsterGenerationParams = new MonsterGenerationParams(true, false);
            monsterGenerationParams.shadowType = MonsterGenerationParams.ShadowType.SHADOW_SKEW;
            plantGenerationParams = new MonsterGenerationParams(false, false);
            plantGenerationParams.tags = new HashSet<>();
            plantGenerationParams.shadowType = MonsterGenerationParams.ShadowType.SHADOW_SKEW;

            baseHp = Configuration.getIntProperty("monster.baseHp");
            baseDmg = Configuration.getIntProperty("monster.baseDmg");
            rangeAttackChance = Configuration.getDoubleProperty("monster.shootChance");

            modifierProbabilitySet.put(null, 2.0);
            for (AnimalModifier m : AnimalModifier.values()) {
                modifierProbabilitySet.put(m, m.weight);
            }
        } catch (FrankensteinException ex) {
            logger.error("Failed to initialize monster generator");
            throw new RuntimeException(ex);
        }
    }

    public static void init() throws FileNotFoundException {
        instance = new AnimalGenerator();
    }

    public static AnimalGenerator getInstance() {
        return instance;
    }

    public static void main(String[] args) throws FileNotFoundException {
        instance.monsterGenerationParams.tags.clear(); //todo: thread-safe?
        instance.monsterGenerationParams.tags.add("style1");
        instance.monsterGenerationParams.colorMap = null;//instance.createDefault4TintMap(CollectionUtils.selectRandomElement(supportedColors));
        for (int i = 0; i < 5; ++i) {
            try {
                Monster monster = instance.monsterGenerator.generateMonster(instance.monsterGenerationParams);
                BufferedImage img = EngineUtils.convertToBufferedImage(((Slick2DFrankensteinImage) monster.monsterImage).getImpl());
                try {
                    ImageIO.write(img, "jpg", new File("out/monster" + i + ".jpg"));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (FrankensteinException e) {
                logger.error("Failed to generate monster image", e);
            }
        }
    }

    public AnimalSpeciesDesc generateMonster(Planet home) {
        Random random = CommonRandom.getRandom();
        int hp = 2 * baseDmg + random.nextInt(baseDmg);
        int dmg = (int) Math.ceil(((baseHp / 2.0 - baseHp / 3.0) * random.nextDouble() + baseHp / 3.0));

        WeaponDesc weapon;
        boolean ranged = random.nextDouble() < rangeAttackChance;
        int range = 3;

        Set<AnimalModifier> modifiers = new HashSet<>();
        AnimalModifier m = modifierProbabilitySet.getRandom();
        if (m != null) {
            modifiers.add(m);
            switch (m) {
                case SNIPER:
                    range *= 2;
                    break;
                case LARGE:
                    hp *= 2;
                    break;
                case SMALL:
                    hp /= 2;
                    break;
            }
        }

        if (ranged) {
            weapon = new WeaponDesc("claw", null, dmg, 0.33f, range, 0, "bullet_shot", "bullet_1", 0, null, null, 0);
        } else {
            weapon = new WeaponDesc("melee", null, dmg, 0.33f, 1, 0, "", "melee_1", 0, "melee_1", null, 0);
        }

        AnimalSpeciesDesc result = new AnimalSpeciesDesc(
                home
                , Localization.getText("research", "animal.default_name")
                , random.nextBoolean()
                , random.nextBoolean()
                , hp
                , weapon
                , 1 + random.nextInt(2),
                ru.game.aurora.util.CollectionUtils.selectRandomElement(AnimalSpeciesDesc.animalBehaviours)
                , modifiers
        );

        if (modifiers.contains(AnimalModifier.ARMOR)) {
            result.setArmor(random.nextInt(5) + 1);
        }

        return result;
    }

    private Map<Integer, Slick2DColor> createDefault4TintMap(Color color) {
        Map<Integer, Slick2DColor> result = new HashMap<>();
        result.put(1, new Slick2DColor(color.darker()));
        result.put(2, new Slick2DColor(color));
        final Color brighter = color.brighter();
        result.put(3, new Slick2DColor(brighter));
        result.put(4, new Slick2DColor(brighter.brighter()));

        return result;
    }

    public void getImageForAnimal(AnimalSpeciesDesc desc) {
        if("Marine".equals(desc.getName())) { //TODO: ?
            desc.setImages(ResourceManager.getInstance().getSpriteSheet("humanity_tileset").getSprite(1, 9), ResourceManager.getInstance().getImage("no_image"));
            return;
        }
        
        try {
            monsterGenerationParams.tags.clear(); //todo: thread-safe?
            monsterGenerationParams.tags.add(desc.getHomePlanet().getFloraAndFauna().getAnimalsStyleTag());
            monsterGenerationParams.colorMap = createDefault4TintMap(CollectionUtils.selectRandomElement(supportedColors));
            if (desc.getModifiers().contains(AnimalModifier.LARGE)) {
                monsterGenerationParams.targetSize = new Size(AuroraGame.tileSize * 2, AuroraGame.tileSize * 2);
            } else if (desc.getModifiers().contains(AnimalModifier.SMALL)) {
                monsterGenerationParams.targetSize = new Size(AuroraGame.tileSize / 2, AuroraGame.tileSize / 2);
            } else {
                monsterGenerationParams.targetSize = null;
            }
            Monster monster = monsterGenerator.generateMonster(monsterGenerationParams);
            desc.setImages(((Slick2DFrankensteinImage) monster.monsterImage).getImpl(), ((Slick2DFrankensteinImage) monster.deadImage).getImpl());
        } catch (FrankensteinException e) {
            logger.error("Failed to generate monster image", e);
        }
    }

    public void getImageForPlant(PlantSpeciesDesc desc) {
        try {
            plantGenerationParams.tags.clear(); //todo: thread-safe?
            plantGenerationParams.tags.add(desc.getMyFlora().getPlantsStyleTag());
            plantGenerationParams.colorMap = desc.getMyFlora().getColorMap();
            desc.setImage(((Slick2DFrankensteinImage) plantGenerator.generateMonster(plantGenerationParams).monsterImage).getImpl());
        } catch (FrankensteinException e) {
            logger.error("Failed to generate plant image", e);
        }
    }

}
