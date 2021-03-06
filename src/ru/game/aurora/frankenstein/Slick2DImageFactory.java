package ru.game.aurora.frankenstein;

import org.newdawn.slick.Color;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import ru.game.frankenstein.FrankensteinColor;
import ru.game.frankenstein.FrankensteinException;
import ru.game.frankenstein.FrankensteinImage;
import ru.game.frankenstein.ImageFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 09.08.13
 * Time: 18:08
 */
public class Slick2DImageFactory implements ImageFactory
{
    private final File root;

    public Slick2DImageFactory(File root) {
        this.root = root;
    }

    @Override
    public FrankensteinImage loadImage(String s) throws FrankensteinException {
        try {
            return new Slick2DFrankensteinImage(new Image(new FileInputStream(new File(root, s)), s, false));
        } catch (Exception e) {
            throw new FrankensteinException("Failed to load image", e);
        }
    }

    @Override
    public FrankensteinImage loadImage(File file) throws FrankensteinException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FrankensteinImage loadImage(InputStream inputStream) throws FrankensteinException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FrankensteinImage createImage(int i, int i1) {
        try {
            return new Slick2DFrankensteinImage(new Image(i, i1));
        } catch (SlickException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public FrankensteinColor decodeColor(String value) {
        return new Slick2DColor(Color.decode(value));
    }
}
