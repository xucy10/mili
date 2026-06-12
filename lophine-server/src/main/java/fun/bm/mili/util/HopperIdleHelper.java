package fun.bm.mili.util;

import fun.bm.mili.config.modules.misc.ItemEntityPerfConfig;
import fun.bm.mili.utils.concurrent.ConcurrentWeakHashMap;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

public final class HopperIdleHelper {
    private static final ConcurrentWeakHashMap<HopperBlockEntity, Integer> idleTickMap = new ConcurrentWeakHashMap<>();

    private HopperIdleHelper() {}

    public static boolean shouldSkipTick(HopperBlockEntity blockEntity, Level level) {
        int skipTicks = ItemEntityPerfConfig.hopperIdleSkipTicks;
        int threshold = ItemEntityPerfConfig.hopperIdleThreshold;

        if (skipTicks <= 1 || threshold <= 0) {
            return false;
        }

        boolean isEmpty = isHopperEmpty(blockEntity);
        int idleTicks = idleTickMap.getOrDefault(blockEntity, 0);

        if (!isEmpty) {
            if (idleTicks > 0) {
                idleTickMap.put(blockEntity, 0);
            }
            return false;
        }

        idleTicks++;
        idleTickMap.put(blockEntity, idleTicks);

        if (idleTicks <= threshold) {
            return false;
        }

        return level.getGameTime() % skipTicks != 0;
    }

    private static boolean isHopperEmpty(HopperBlockEntity hopper) {
        if (!(hopper instanceof Container container)) {
            return true;
        }
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            if (!container.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
