package world.landfall.deepspace.util;

import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import world.landfall.deepspace.Deepspace;

import javax.annotation.Nullable;
import java.nio.file.Path;

public final class AccessUtil {
    private AccessUtil() {}

    public static Logger getLogger() { return Deepspace.LOGGER; }

    public static Path getGameDir() { return FMLPaths.GAMEDIR.get(); }

    /** 空间数据的根分类，对应 data/&lt;namespace&gt;/&lt;root&gt;/... */
    public static String getBodyRoot() { return "bodies"; }
    public static String getGalaxyRoot() { return "galaxies"; }
    public static String getRingWorldRoot() { return "ringworlds"; }

    public static void setResourceManager(@Nullable ResourceManager manager) {
        Deepspace.RESOURCE_MANAGER = manager;
    }
    public static ResourceManager getResourceManager() {
        if ( Deepspace.RESOURCE_MANAGER == null ) {
            throw new IllegalStateException("资源管理器未就绪：在数据包加载之前访问了资源");
        }
        return Deepspace.RESOURCE_MANAGER;
    }
}
