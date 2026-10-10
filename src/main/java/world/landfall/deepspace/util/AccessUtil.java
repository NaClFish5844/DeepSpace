package world.landfall.deepspace.util;

import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import world.landfall.deepspace.Deepspace;
import world.landfall.deepspace.datapack.DatapackManager;

import javax.annotation.Nullable;
import java.nio.file.Path;

public final class AccessUtil {
    private AccessUtil() {}

    public static Logger getLogger() { return Deepspace.LOGGER; }

    public static Path getGameDir() { return FMLPaths.GAMEDIR.get(); }

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
    public static DatapackManager getDatapackManager() {
        return Deepspace.DATAPACK_MANAGER;
    }
}
