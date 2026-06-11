package fun.bm.lophine.perf;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Lophine - Bootstrap hook for the perf monitoring stack.
 *
 * <p>Wires the {@link LophineRegionLoadMonitor}, {@link LophineTickProfiler}
 * and {@link LophineRegionSafetyGuard} into Folia's tick path. The
 * actual integration is done through:
 * <ul>
 *   <li>A patch on Folia's {@code RegionizedServer.tickServer} that
 *       calls {@link #onGlobalTick()} at the start of every global
 *       region tick.</li>
 *   <li>A patch on {@code ServerLevel.tick} that calls
 *       {@link #onRegionTick()} when each region begins its tick.</li>
 * </ul>
 *
 * <p>This class is a thin facade - the heavy lifting lives in the
 * patched code. The methods here are guarded so that the patches
 * always call into safe, no-throw code paths.
 */
public final class LophineTickSchedulerHook {
    private static final Logger LOGGER = LogUtils.getLogger();

    private LophineTickSchedulerHook() {
    }

    /**
     * Called once per global region tick (default 50ms). Samples the
     * region load, drains the profiler summary queue, and applies the
     * resolved CPU affinity to the global region thread on its first
     * invocation.
     */
    public static void onGlobalTick() {
        try {
            LophineAffinityAutoTuner.applyToCurrentThread();
            LophineRegionLoadMonitor.onSampleTick();
            LophineRegionLoadMonitor.maybeLogSummary();
            LophineTickProfiler.maybeLogSummary();
        } catch (Throwable t) {
            // never crash a global tick
            LOGGER.debug("Lophine perf hook (global): {}", t.getMessage());
        }
    }

    /**
     * Called at the start of each region tick. Returns a token that
     * MUST be passed to {@link #onRegionTickEnd(long, String)} so the
     * safety guard can measure how long the tick took.
     */
    public static long onRegionTickStart(String caller) {
        return org.leavesmc.leaves.util.LophineRegionSafetyGuard.beginRegionTick(caller);
    }

    /**
     * Called at the end of each region tick. See
     * {@link org.leavesmc.leaves.util.LophineRegionSafetyGuard#endRegionTick(long, String)}.
     */
    public static void onRegionTickEnd(long token, String caller) {
        try {
            org.leavesmc.leaves.util.LophineRegionSafetyGuard.endRegionTick(token, caller);
        } catch (Throwable t) {
            LOGGER.debug("Lophine perf hook (region end): {}", t.getMessage());
        }
    }
}
