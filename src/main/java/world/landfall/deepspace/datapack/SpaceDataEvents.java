package world.landfall.deepspace.datapack;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import world.landfall.deepspace.Deepspace;
import world.landfall.deepspace.datapack.load.DatapackProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static world.landfall.deepspace.util.AccessUtil.*;

/**
 * 数据域事件入口：注册数据包监听器。
 * <p>
 * /reload 时在后台线程走完「取源 → 一轮加载 → 二轮过滤 → 落地到 DatapackManager」，主线程不碰 IO。
 * 旧的 PlanetDataReloadListener 仍在独立运行，两条链路互不干扰。
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

        /** 后台线程：注入资源管理器 → 覆盖审计 → 触发一轮加载与二轮过滤，落地到 Manager */
        private static void resolve(ResourceManager resourceManager) {
            setResourceManager(resourceManager);

            // 覆盖审计：同一 ID 被多个包提供时，说明谁覆盖了谁
            Map<ResourceLocation, List<DatapackProvider.ProvidedJson>> all = DatapackProvider.getAllSpaceData();

            long overwritten = all.values().stream().filter(sources -> sources.size() > 1).count();
            long unreadable = all.values().stream()
                    .flatMap(List::stream)
                    .filter(provided -> provided.json() == null)
                    .count();

            if (overwritten > 0 || unreadable > 0) {
                getLogger().info(
                        "空间数据源：{} 个 ID（被覆盖 {}，读取失败 {}） | space data sources: {} ids ({} overwritten, {} unreadable)",
                        all.size(), overwritten, unreadable,
                        all.size(), overwritten, unreadable
                );
            }

            getDatapackManager().reloadDatapack();
        }
    }
}
