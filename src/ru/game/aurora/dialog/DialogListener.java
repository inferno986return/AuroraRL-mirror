/**
 * Created with IntelliJ IDEA.
 * User: Egor.Smirnov
 * Date: 14.03.13
 * Time: 12:13
 */

package ru.game.aurora.dialog;

import ru.game.aurora.world.World;

import java.io.Serializable;

public interface DialogListener extends Serializable
{
    public void onDialogEnded(World world, int returnCode);
}