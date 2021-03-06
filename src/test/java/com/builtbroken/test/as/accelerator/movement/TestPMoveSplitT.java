package com.builtbroken.test.as.accelerator.movement;

import com.builtbroken.atomic.content.machines.accelerator.data.TubeConnectionType;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;

/**
 * Created by Dark(DarkGuardsman, Robert) on 2019-04-16.
 */
public class TestPMoveSplitT extends PMoveCommon
{
    public static int LEFT = 0;
    public static int RIGHT = 1;

    @Test
    public void northEnterBackStepOnce()
    {
        checkEnterStep(EnumFacing.NORTH, TubeConnectionType.T_SPLIT, EnumFacing.NORTH);
    }

    @Test
    public void eastEnterBackStepOnce()
    {
        checkEnterStep(EnumFacing.EAST, TubeConnectionType.T_SPLIT, EnumFacing.EAST);
    }

    @Test
    public void southEnterBackStepOnce()
    {
        checkEnterStep(EnumFacing.SOUTH, TubeConnectionType.T_SPLIT, EnumFacing.SOUTH);
    }

    @Test
    public void westEnterBackStepOnce()
    {
        checkEnterStep(EnumFacing.WEST, TubeConnectionType.T_SPLIT, EnumFacing.WEST);
    }

    @Test
    public void northExitLeft()
    {
        checkExit(EnumFacing.NORTH, TubeConnectionType.T_SPLIT, EnumFacing.WEST);
    }

    @Test
    public void northExitRight()
    {
        checkExit(EnumFacing.NORTH, TubeConnectionType.T_SPLIT, EnumFacing.EAST);
    }

    @Test
    public void eastExitLeft()
    {
        checkExit(EnumFacing.EAST, TubeConnectionType.T_SPLIT, EnumFacing.NORTH);
    }

    @Test
    public void eastExitRight()
    {
        checkExit(EnumFacing.EAST, TubeConnectionType.T_SPLIT, EnumFacing.SOUTH);
    }

    @Test
    public void southExitLeft()
    {
        checkExit(EnumFacing.SOUTH, TubeConnectionType.T_SPLIT, EnumFacing.EAST);
    }

    @Test
    public void southExitRight()
    {
        checkExit(EnumFacing.SOUTH, TubeConnectionType.T_SPLIT, EnumFacing.WEST);
    }

    @Test
    public void westExitLeft()
    {
        checkExit(EnumFacing.WEST, TubeConnectionType.T_SPLIT, EnumFacing.SOUTH);
    }

    @Test
    public void westExitRight()
    {
        checkExit(EnumFacing.WEST, TubeConnectionType.T_SPLIT, EnumFacing.NORTH);
    }

    @Test
    public void northFullSplitLeft()
    {
        checkTurn(EnumFacing.NORTH, TubeConnectionType.T_SPLIT, EnumFacing.SOUTH, LEFT, EnumFacing.WEST);
    }

    @Test
    public void northFullSplitRight()
    {
        checkTurn(EnumFacing.NORTH, TubeConnectionType.T_SPLIT, EnumFacing.SOUTH, RIGHT, EnumFacing.EAST);
    }

    @Test
    public void eastFullSplitLeft()
    {
        checkTurn(EnumFacing.EAST, TubeConnectionType.T_SPLIT, EnumFacing.WEST, LEFT, EnumFacing.NORTH);
    }

    @Test
    public void eastFullSplitRight()
    {
        checkTurn(EnumFacing.EAST, TubeConnectionType.T_SPLIT, EnumFacing.WEST, RIGHT, EnumFacing.SOUTH);
    }

    @Test
    public void southFullSplitLeft()
    {
        checkTurn(EnumFacing.SOUTH, TubeConnectionType.T_SPLIT, EnumFacing.NORTH, LEFT, EnumFacing.EAST);
    }

    @Test
    public void southFullSplitRight()
    {
        checkTurn(EnumFacing.SOUTH, TubeConnectionType.T_SPLIT, EnumFacing.NORTH, RIGHT, EnumFacing.WEST);
    }

    @Test
    public void westFullSplitLeft()
    {
        checkTurn(EnumFacing.WEST, TubeConnectionType.T_SPLIT, EnumFacing.EAST, LEFT, EnumFacing.SOUTH);
    }

    @Test
    public void westFullSplitRight()
    {
        checkTurn(EnumFacing.WEST, TubeConnectionType.T_SPLIT, EnumFacing.EAST, RIGHT, EnumFacing.NORTH);
    }
}

