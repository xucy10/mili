package fun.bm.mili.utils;

import fun.bm.mili.config.modules.experiment.EntityTrackingPerfConfig;
import net.minecraft.world.entity.Entity;

import java.util.concurrent.atomic.AtomicInteger;

public final class EntityTrackingThrottle {
    private EntityTrackingThrottle() {
    }

    private static final AtomicInteger currentTick = new AtomicInteger(0);

    public static void onTickStart() {
        currentTick.incrementAndGet();
    }

    public static boolean shouldBroadcast(Entity entity) {
        if (!EntityTrackingPerfConfig.enabled) return true;

        int entityId = entity.getId();
        int absId = entityId & 0x7FFFFFFF;
        int tick = currentTick.get();

        int forcedInterval = EntityTrackingPerfConfig.forcedUpdateInterval;
        if (forcedInterval > 0 && tick % forcedInterval == absId % forcedInterval) {
            return true;
        }

        double tpsThreshold = EntityTrackingPerfConfig.tpsThrottleThreshold;
        if (tpsThreshold > 0) {
            double currentTps = MiliTpsThrottle.currentTps();
            if (currentTps < tpsThreshold) {
                double factor = MiliTpsThrottle.throttleFactor(tpsThreshold, tpsThreshold * 0.5);
                if ((entityId + tick) % 4 >= (int) (factor * 4)) {
                    return false;
                }
            }
        }

        int maxUpdates = EntityTrackingPerfConfig.maxUpdatesPerTick;
        if (maxUpdates > 0) {
            int slot = absId % maxUpdates;
            if (slot != tick % maxUpdates) {
                return false;
            }
        }

        return true;
    }
}
