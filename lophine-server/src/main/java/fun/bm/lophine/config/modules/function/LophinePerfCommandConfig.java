package fun.bm.lophine.config.modules.function;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import fun.bm.lophine.feature.LophinePerfCommand;
import me.earthme.luminol.config.IConfigModule;
import me.earthme.luminol.config.flags.ConfigClassInfo;
import me.earthme.luminol.config.flags.ConfigInfo;
import me.earthme.luminol.config.flags.DoNotLoad;
import me.earthme.luminol.enums.EnumConfigCategory;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Lophine - /lophine-perf command registration.
 */
@ConfigClassInfo(category = EnumConfigCategory.FUNCTION, name = "lophine-perf-command")
public class LophinePerfCommandConfig implements IConfigModule {
    @ConfigInfo(name = "enabled", comments = """
            Enable the /lophine-perf command. Provides status, affinity,
            region load, tick profiler, and deadlock stats. Permission:
            lophine.commands.perf""")
    public static boolean enabled = true;

    @DoNotLoad
    private static LophinePerfCommand command;

    @Override
    public void onLoaded(CommentedFileConfig configInstance, @Nullable Set<Exception> exs) {
        if (enabled) {
            if (command == null) {
                command = new LophinePerfCommand();
            }
            command.register();
        }
    }

    @Override
    public void onUnloaded(CommentedFileConfig configInstance) {
        if (command != null) {
            command.unregister();
            command = null;
        }
    }
}
