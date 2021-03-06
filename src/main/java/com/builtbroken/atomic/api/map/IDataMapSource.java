package com.builtbroken.atomic.api.map;

import com.builtbroken.atomic.lib.transform.IPosWorld;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 *
 * Created by Dark(DarkGuardsman, Robert) on 9/21/2018.
 */
public interface IDataMapSource extends IPosWorld
{
    /**
     * Type of the source
     * <p>
     * Used for referencing when pulling from global data map
     *
     * @return source type
     */
    @Nonnull
    DataMapType getType();

    /**
     * Called to clear any data attached to the source
     * that belongs to the map. Normally called on
     * world unload or source death.
     */
    void clearMapData();

    /**
     * Called to remove stored map data from the
     * {@link com.builtbroken.atomic.map.MapHandler#GLOBAL_DATA_MAP} without
     * de-validating the data. As the data may be recycled to reduce
     * memory churn.
     */
    void disconnectMapData();

    /**
     * Called to see if the source has map data
     * stored
     *
     * @return true if map data is stored
     */
    boolean hasMapData();

    /**
     * Called to see if the source has map data
     * stored and currently placed in the map
     *
     * @return true if map data is stored
     */
    boolean hasActiveMapData();

    /**
     * Called to connect all stored data into
     * {@link com.builtbroken.atomic.map.MapHandler#GLOBAL_DATA_MAP}
     */
    void connectMapData();

    /**
     * Called after new data has been added to the source
     * and old data has been cleared. Allows the source
     * to do some quick checks and trigger additional logic.
     */
    default void initMapData()
    {

    }

    /**
     * Checks if the source is still
     * valid. This is used during clean up
     * checks of the map to remove sources
     * that can not remove themselves.
     * <p>
     * Example: Map sources that are not
     * stored in the map data. Which are
     * used to act as fake source for map
     * radiation.
     *
     * @return true if still valid
     */
    default boolean isStillValid()
    {
        return doesSourceExist();
    }

    /**
     * Checks if the source still exists in the world
     * and should continue to be used in the data map.
     *
     * @return true if all good, false to remove
     */
    default boolean doesSourceExist()
    {
        return true;
    }

    /**
     * Called when the data source is removed from the map.
     * Use this call to clear all data stored in the tile.
     */
    default void onRemovedFromMap()
    {
        disconnectMapData();
        clearMapData();
    }

    /**
     * Called when the thread is done generating
     * data for this source. Data will already
     * be set into the source from the thread.
     */
    default void onThreadComplete()
    {

    }

    /**
     * Checks if the source has changed and needs to be
     * queued for an update on the map thread.
     *
     * @param tagCompound - state save data
     * @return true if should queue to thread
     */
    boolean shouldQueueForUpdate(@Nullable NBTTagCompound tagCompound);

    /**
     * Gets the save state for the source
     * Only used for {@link #shouldQueueForUpdate(NBTTagCompound)}
     *
     * @return save state
     */
    @Nullable
    default NBTTagCompound getSaveState()
    {
        return null;
    }

    /**
     * Called each tick in the {@link net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent}
     * Only use as needed
     */
    default void update()
    {

    }
}
