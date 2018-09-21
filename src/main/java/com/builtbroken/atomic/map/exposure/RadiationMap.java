package com.builtbroken.atomic.map.exposure;

import com.builtbroken.atomic.AtomicScience;
import com.builtbroken.atomic.api.radiation.IRadiationExposureSystem;
import com.builtbroken.atomic.api.radiation.IRadiationSource;
import com.builtbroken.atomic.api.radiation.IRadioactiveItem;
import com.builtbroken.atomic.map.MapHandler;
import com.builtbroken.atomic.map.data.DataChange;
import com.builtbroken.atomic.map.data.node.DataMapType;
import com.builtbroken.atomic.map.data.storage.DataMap;
import com.builtbroken.atomic.map.exposure.wrapper.RadSourceEntityItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles radiation exposure map. Is not saved and only cached to improve runtime.
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/24/2018.
 */
public class RadiationMap implements IRadiationExposureSystem
{
    /** List of radiation sources in the world that are not part of the {@link com.builtbroken.atomic.api.radiation.IRadioactiveMaterialSystem} */
    private HashMap<IRadiationSource, RadSourceWrapper> radiationSourceMap = new HashMap();
    /** Map of entity to its wrapper */
    private HashMap<Entity, IRadiationSource> radiationEntityMap = new HashMap();
    private HashMap<Class<? extends Entity>, Function<Entity, IRadiationSource>> wrapperFactories = new HashMap();

    private int reloadTimer = 0;

    public RadiationMap()
    {
        wrapperFactories.put(EntityItem.class, e -> RadSourceEntityItem.build((EntityItem) e));
    }

    ///----------------------------------------------------------------
    ///-------- Radiation Sources
    ///----------------------------------------------------------------

    /**
     * Called to add source to the map
     * <p>
     * Only call this from the main thread. As the list of sources
     * is iterated at the end of each tick to check for changes.
     *
     * @param source - valid source currently in the world
     */
    public void addSource(IRadiationSource source)
    {
        if (source != null && source.isRadioactive() && !radiationSourceMap.containsKey(source))
        {
            radiationSourceMap.put(source, new RadSourceWrapper(source));
            onSourceAdded(source);
        }
    }

    public void addSource(Entity entity)
    {
        if (entity != null && entity.isEntityAlive() && !radiationEntityMap.containsKey(entity))
        {
            IRadiationSource source = getSource(entity);
            if (source != null)
            {
                addSource(source);
                radiationEntityMap.put(entity, source);
            }
        }
    }

    /**
     * Called to remove a source from the map
     * <p>
     * Only call this from the main thread. As the list of sources
     * is iterated at the end of each tick to check for changes.
     * <p>
     * Only remove if external logic requires it. As the source
     * should return false for {@link IRadiationSource#isRadioactive()}
     * to be automatically removed.
     *
     * @param source - valid source currently in the world
     */
    public void removeSource(IRadiationSource source)
    {
        if (radiationSourceMap.containsKey(source))
        {
            onSourceRemoved(source);
            radiationSourceMap.remove(source);
        }
    }

    public void removeSource(Entity entity)
    {
        if (radiationEntityMap.containsKey(entity))
        {
            removeSource(radiationEntityMap.get(entity));
            radiationEntityMap.remove(entity);
        }
    }

    /**
     * Called when a source is added
     *
     * @param source
     */
    protected void onSourceAdded(IRadiationSource source)
    {
        if (AtomicScience.runningAsDev)
        {
            AtomicScience.logger.info("RadiationMap: adding source " + source);
        }
        fireSourceChange(source, source.getRadioactiveMaterial());
    }

    /**
     * Called when a source is removed
     *
     * @param source
     */
    protected void onSourceRemoved(IRadiationSource source)
    {
        if (AtomicScience.runningAsDev)
        {
            AtomicScience.logger.info("RadiationMap: remove source " + source);
        }
        fireSourceChange(source, 0);
    }

    /**
     * Called to cleanup invalid sources from the map
     */
    public void clearDeadSources()
    {
        Iterator<Map.Entry<IRadiationSource, RadSourceWrapper>> it = radiationSourceMap.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<IRadiationSource, RadSourceWrapper> next = it.next();
            if (next == null || next.getKey() == null || next.getValue() == null || !next.getKey().isRadioactive())
            {
                if (next.getKey() != null)
                {
                    onSourceRemoved(next.getKey());
                }
                it.remove();
            }
        }
    }

    /**
     * Called to fire a source change event
     *
     * @param source
     * @param newValue
     */
    protected void fireSourceChange(IRadiationSource source, int newValue)
    {
        if (AtomicScience.runningAsDev)
        {
            AtomicScience.logger.info("RadiationMap: on changed " + source);
        }
        RadSourceWrapper wrapper = getRadSourceWrapper(source);
        if (wrapper != null && wrapper.radioactiveMaterialValue != newValue)
        {
            //Remove old, called separate in case position changed
            if (wrapper.radioactiveMaterialValue != 0)
            {
                MapHandler.THREAD_RAD_EXPOSURE.queuePosition(DataChange.get(wrapper.dim, wrapper.xi(), wrapper.yi(), wrapper.zi(), wrapper.radioactiveMaterialValue, 0));
            }
            //Log changes
            wrapper.logCurrentData();

            //Add new, called separate in case position changed
            if (newValue != 0 && source.isRadioactive())
            {
                MapHandler.THREAD_RAD_EXPOSURE.queuePosition(DataChange.get(wrapper.dim, wrapper.xi(), wrapper.yi(), wrapper.zi(), 0, newValue));
            }
        }
    }

    protected RadSourceWrapper getRadSourceWrapper(IRadiationSource source)
    {
        if (radiationSourceMap.containsKey(source))
        {
            RadSourceWrapper wrapper = radiationSourceMap.get(source);
            if (wrapper == null)
            {
                radiationSourceMap.put(source, wrapper = new RadSourceWrapper(source));
            }
            return wrapper;
        }
        return null;
    }

    public IRadiationSource getSource(Entity entity)
    {
        if (entity instanceof IRadiationSource)
        {
            return (IRadiationSource) entity;
        }

        Class<? extends Entity> clazz = entity.getClass();
        if (wrapperFactories.containsKey(clazz))
        {
            return wrapperFactories.get(clazz).apply(entity);
        }
        //TODO wrapper IInventory
        return null;
    }

    ///----------------------------------------------------------------
    ///-------- Level Data Accessors
    ///----------------------------------------------------------------

    /**
     * Gets the '(REM) roentgen equivalent man' at the given location
     *
     * @param world - location
     * @param pos   - location
     * @return rem level in mili-rads (1/1000ths of a rem)
     */
    public int getRadLevel(World world, BlockPos pos)
    {
        DataMap map = MapHandler.GLOBAL_DATA_MAP.getMap(world, false);
        if (map != null)
        {
            return map.getValue(pos, DataMapType.RADIATION);
        }
        return 0;
    }

    /**
     * Gets the '(REM) roentgen equivalent man' at the given location
     *
     * @param world - location
     * @param x     - location
     * @param y     - location
     * @param z     - location
     * @return rem level in mili-rads (1/1000ths of a rem)
     */
    public int getRadLevel(World world, int x, int y, int z)
    {
        DataMap map = MapHandler.GLOBAL_DATA_MAP.getMap(world, false);
        if (map != null)
        {
            return map.getValue(x, y, z, DataMapType.RADIATION);
        }
        return 0;
    }

    /**
     * Gets the REM exposure for the entity
     *
     * @param entity - entity, will use the entity size to get an average value
     * @return REM value
     */
    public float getRemExposure(Entity entity)
    {
        float value = 0;

        //Top point
        value += getRadLevel(entity.world, (int) Math.floor(entity.posX), (int) Math.floor(entity.posY + entity.height), (int) Math.floor(entity.posZ));

        //Mid point
        value += getRadLevel(entity.world, (int) Math.floor(entity.posX), (int) Math.floor(entity.posY + (entity.height / 2)), (int) Math.floor(entity.posZ));

        //Bottom point
        value += getRadLevel(entity.world, (int) Math.floor(entity.posX), (int) Math.floor(entity.posY), (int) Math.floor(entity.posZ));

        //Average TODO build alg to use body size (collision box)
        value /= 3f;

        //Convert from mili rem to rem
        value /= 1000f;

        return value;
    }

    ///----------------------------------------------------------------
    ///--------Edit events
    ///----------------------------------------------------------------

    public void onWorldUnload(World world)
    {
        radiationSourceMap.clear();
        radiationEntityMap.clear();
    }

    /*
    @SubscribeEvent()
    public void onChunkAdded(MapSystemEvent.AddChunk event)
    {
        if (event.world() != null && !!event.world().isRemote && event.map.mapSystem == MapHandler.MATERIAL_MAP)
        {
            MapHandler.THREAD_RAD_EXPOSURE.queueChunkForAddition(event.chunk);
        }
    }

    @SubscribeEvent()
    public void onChunkRemove(MapSystemEvent.RemoveChunk event)
    {
        if (event.world() != null && !!event.world().isRemote && event.map.mapSystem == MapHandler.MATERIAL_MAP)
        {
            MapHandler.THREAD_RAD_EXPOSURE.queueChunkForRemoval(event.chunk);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRadiationChange(MapSystemEvent.UpdateValue event)
    {
        if (event.world() != null && !event.world().isRemote && event.map.mapSystem == MapHandler.MATERIAL_MAP)
        {
            MapHandler.THREAD_RAD_EXPOSURE.queuePosition(DataChange.get(event.dim(), event.x, event.y, event.z, event.prev_value, event.node));
        }
    }
    */

    @SubscribeEvent()
    public void serverTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            //Cleanup
            clearDeadSources();

            //Loop sources looking for changes
            for (RadSourceWrapper wrapper : radiationSourceMap.values())
            {
                if (wrapper.hasSourceChanged())
                {
                    fireSourceChange(wrapper.source, wrapper.source.getRadioactiveMaterial());
                }
            }
        }
    }

    @SubscribeEvent()
    public void itemPickUpEvent(PlayerEvent.ItemPickupEvent event)
    {
        if (!event.player.world.isRemote)
        {
            EntityItem entityItem = event.pickedUp;
            if (entityItem != null)
            {
                ItemStack stack = entityItem.getItem();
                if (stack != null && stack.getItem() instanceof IRadioactiveItem)
                {
                    removeSource(entityItem);
                }
            }
        }
    }

    @SubscribeEvent()
    public void entityJoinWorld(EntityJoinWorldEvent event)
    {
        if (!event.getWorld().isRemote && event.getEntity().isEntityAlive())
        {
            //Add source handles checking if its an actual source
            addSource(event.getEntity());
        }
    }
}
