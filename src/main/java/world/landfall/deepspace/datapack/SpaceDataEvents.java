package world.landfall.deepspace.datapack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import world.landfall.deepspace.Deepspace;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static world.landfall.deepspace.util.AccessUtil.*;

/**
 * 数据域事件入口：注册数据包监听器。
 * <p>
 * 目前只读取与统计，不落地注册表 —— 旧的 PlanetDataReloadListener 仍在独立运行，两条链路互不干扰。
 * 数据模型（spacedata）与校验（Checker）就绪后，再在这里接上"转对象 → 替换注册表"。
 */
@EventBusSubscriber(modid = Deepspace.MODID)
public final class SpaceDataEvents {
    private SpaceDataEvents() {}

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Listener());
    }

    private static final class Listener implements PreparableReloadListener {
        @Override public CompletableFuture<Void> reload(
                PreparationBarrier stage,
                ResourceManager resourceManager,
                ProfilerFiller preparationsProfiler,
                ProfilerFiller reloadProfiler,
                Executor backgroundExecutor,
                Executor gameExecutor
        ) {
            return CompletableFuture
                    .runAsync(() -> resolve(resourceManager), backgroundExecutor)
                    .thenCompose(stage::wait);
        }

        /** 后台线程：注入资源管理器，取两个源并统计 —— 主线程不碰 IO */
        private static void resolve(ResourceManager resourceManager) {
            setResourceManager(resourceManager);

            Map<ResourceLocation, List<DatapackProvider.ProvidedJson>> all = DatapackProvider.getAllSpaceData();
            Map<ResourceLocation, DatapackProvider.ProvidedJson> effective = DatapackProvider.getEffectiveSpaceData();

            long overwritten = all.values().stream().filter(sources -> sources.size() > 1).count();
            long unreadable = all.values().stream()
                    .flatMap(List::stream)
                    .filter(provided -> provided.json() == null)
                    .count();

            getLogger().info(
                    "deepspace 数据：{} 个 ID（生效 {}，被覆盖 {}，读取失败 {}） | deepspace data: {} ids ({} effective, {} overwritten, {} unreadable)",
                    all.size(), effective.size(), overwritten, unreadable,
                    all.size(), effective.size(), overwritten, unreadable
            );
        }
    }
}
