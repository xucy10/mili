package fun.bm.mili.config.modules.misc;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.enums.EnumConfigCategory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * mili - Performance tuning hints for Folia region-based threading.
 *
 * <p>This does NOT modify Folia's scheduler. It only provides recommendations
 * logged at startup based on the detected CPU topology. The actual thread
 * count is controlled by Folia's {@code paper-global.yml}.
 */
@ConfigClassInfo(category = EnumConfigCategory.MISC, name = "performance-tuning")
public class PerformanceConfig implements IConfigModule {
    private static final Logger LOGGER = LoggerFactory.getLogger("mili|PerfTuning");

    @ConfigInfo(name = "suggested-thread-count", comments = """
            Suggested Folia region thread count. 0 = auto-calculate based on CPU cores.
            This value is NOT enforced by Lophine; it is only logged as a recommendation
            at server startup. To apply it, set the same value in paper-global.yml under
            'region-scheduler.thread-count'. For technical servers with clustered players
            and large machines, fewer threads often perform better than more.""")
    public static int suggestedThreadCount = 0;

    @ConfigInfo(name = "log-thread-recommendation", comments = """
            If true, logs the recommended Folia thread count at server startup.
            Useful for first-time setup; disable once you have configured paper-global.yml.""")
    public static boolean logThreadRecommendation = true;

    @ConfigInfo(name = "enable-equipment-tracking", comments = """
            Enables equipment tracking optimization to reduce unnecessary enchantment ticking.
            Based on Lithium's equipment tracking optimization.
            Only affects non-player entities.""")
    public static boolean enableEquipmentTracking = false;

    @ConfigInfo(name = "enable-suffocation-optimization", comments = """
            Enables suffocation optimization to reduce unnecessary damage checks.
            Based on Pufferfish's suffocation optimization.
            Checks if entity could possibly be hurt before checking isInWall().""")
    public static boolean enableSuffocationOptimization = false;

    @ConfigInfo(name = "enable-reduce-chunk-loading-lookups", comments = """
            Enables reduced chunk loading & lookups optimization.
            Based on Pufferfish's reduce chunk loading & lookups optimization.
            Uses getChunkIfLoaded instead of getBlockState to prevent unnecessary chunk loading.""")
    public static boolean enableReduceChunkLoadingLookups = false;

    @ConfigInfo(name = "enable-sheep-optimization", comments = """
            Enables sheep color mixing optimization.
            Based on Carpet-Fixes sheep optimization.
            Uses pre-computed color lookup table instead of recipe matching.""")
    public static boolean enableSheepOptimization = false;

    @ConfigInfo(name = "enable-command-block-parse-results-caching", comments = """
            Enables command block parse results caching.
            Caches parse results to avoid re-parsing the same command.""")
    public static boolean enableCommandBlockParseResultsCaching = false;

    @ConfigInfo(name = "enable-profile-result-caching", comments = """
            Enables player profile result caching.
            Caches Mojang session service results to reduce API calls.""")
    public static boolean enableProfileResultCaching = false;

    @ConfigInfo(name = "profile-result-caching-timeout", comments = """
            Timeout in minutes for player profile result caching.""")
    public static int profileResultCachingTimeout = 30;

    @Override
    public void onLoaded(CommentedFileConfig configInstance, @Nullable Set<Exception> exs) {
        if (!logThreadRecommendation) return;

        int processors = Runtime.getRuntime().availableProcessors();
        int recommended = calculateOptimalThreads(processors);

        if (suggestedThreadCount > 0) {
            recommended = suggestedThreadCount;
            LOGGER.info("User-configured thread count: {} (CPU cores: {})", recommended, processors);
        } else {
            LOGGER.info("Recommended Folia thread count: {} (CPU cores: {})", recommended, processors);
        }

        LOGGER.info("To apply: set 'region-scheduler.thread-count' to {} in paper-global.yml", recommended);

        if (processors >= 8) {
            LOGGER.info("Technical server tip: With {} cores, consider running fewer threads for better per-thread performance", processors);
        }
    }

    /**
     * Calculate optimal thread count for Folia's region scheduler on technical servers.
     *
     * <p>Technical servers tend to have players clustered around large machines,
     * so fewer threads with better per-thread performance is often preferable
     * to many threads with higher synchronization overhead.
     */
    private static int calculateOptimalThreads(int processors) {
        if (processors <= 4) {
            // Small server: use all but one core
            return Math.max(2, processors - 1);
        } else if (processors <= 8) {
            // Medium server: leave one core for main thread + GC
            return processors - 1;
        } else if (processors <= 16) {
            // Large server: leave two cores for overhead
            return processors - 2;
        } else {
            // Very large server: cap at 32 to avoid diminishing returns
            return Math.min(processors - 4, 32);
        }
    }
}
