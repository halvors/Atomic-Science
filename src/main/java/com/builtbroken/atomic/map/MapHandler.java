package com.builtbroken.atomic.map;

import com.builtbroken.atomic.AtomicScience;
import com.builtbroken.atomic.api.AtomicScienceAPI;
import com.builtbroken.atomic.config.logic.ConfigRadiation;
import com.builtbroken.atomic.map.exposure.RadiationMap;
import com.builtbroken.atomic.map.exposure.ThreadRadExposure;
import com.builtbroken.atomic.map.thermal.ThermalMap;
import com.builtbroken.atomic.map.thermal.ThreadThermalAction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Core handler for registering maps and triggering events
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 5/6/2018.
 */
public final class MapHandler
{
    public static final String RAD_EXPOSURE_MAP_ID = AtomicScience.PREFIX + "radiation_exposure";
    public static final String RAD_MATERIAL_MAP_ID = AtomicScience.PREFIX + "radiation_material";

    public static final String NBT_RAD_CHUNK = AtomicScience.PREFIX + "radiation_data";

    /** Handles radiation exposure data storage */
    public static final RadiationMap RADIATION_MAP = new RadiationMap(); //TODO expose to API
    /** Handles radioactive material data storage */
    public static final ThermalMap THERMAL_MAP = new ThermalMap(); //TODO expose to API

    public static final MapSystem GLOBAL_DATA_MAP = new MapSystem();

    /** Thread used to calculate exposure values per location */
    public static ThreadRadExposure THREAD_RAD_EXPOSURE;
    /** Thread used to move heat around the map */
    public static ThreadThermalAction THREAD_THERMAL_ACTION;

    public static final MapHandler INSTANCE = new MapHandler();


    public static void register()
    {
        AtomicScienceAPI.radiationExposureSystem = RADIATION_MAP;
        AtomicScienceAPI.thermalSystem = THERMAL_MAP;

        MinecraftForge.EVENT_BUS.register(INSTANCE);

        if(ConfigRadiation.ENABLE_MAP)
        {
            MinecraftForge.EVENT_BUS.register(RADIATION_MAP);
        }

        MinecraftForge.EVENT_BUS.register(THERMAL_MAP);
    }

    ///----------------------------------------------------------------
    ///--------World events
    ///----------------------------------------------------------------

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event)
    {
        RADIATION_MAP.onWorldUnload(event.getWorld());
        THERMAL_MAP.onWorldUnload(event.getWorld());
    }
}
