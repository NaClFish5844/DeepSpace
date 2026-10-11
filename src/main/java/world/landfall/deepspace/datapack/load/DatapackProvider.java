package world.landfall.deepspace.datapack.load;

import com.google.gson.JsonElement;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import world.landfall.deepspace.util.FileIOUtil;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static world.landfall.deepspace.util.AccessUtil.*;

/** 加载链路最前段：只负责从资源管理器解析，不持有、不缓存 */
public final class DatapackProvider {
    private DatapackProvider() {}

    public record ProvidedJson(String packId, @Nullable JsonElement json) {}

    /** 全量源（校验用）：三个根分类的全部来源，含被覆盖的 */
    public static Map<ResourceLocation, List<ProvidedJson>> getAllSpaceData() {
        Map<ResourceLocation, List<ProvidedJson>> all = new HashMap<>();

        for (String root : List.of(getBodyRoot(), getGalaxyRoot(), getRingWorldRoot())) {
            all.putAll(readRoot(root));
        }
        return all;
    }
    /** 生效源（加载用）：每个 ID 只保留生效的那份 */
    public static Map<ResourceLocation, ProvidedJson> getEffectiveSpaceData() {
        return getAllSpaceData().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getLast()));
    }  
    /** 超空间航道全量源（加载用）：此类数据不应当丢弃，而是全部合并 */
    public static Map<ResourceLocation, List<ProvidedJson>> getAllHyperlaneData() {
        return new HashMap<>(readRoot(getHyperlaneRoot()));
    }
    /** 读一个根分类下的全部来源 */
    private static Map<ResourceLocation, List<ProvidedJson>> readRoot(String root) {
        FileToIdConverter converter = FileToIdConverter.json(root);
        Map<ResourceLocation, List<Resource>> stacks = converter.listMatchingResourceStacks(getResourceManager());

        return stacks.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> converter.fileToId(entry.getKey()),
                        entry -> readDuplicatedSources(converter.fileToId(entry.getKey()), entry.getValue())
                ));
    }
    /** 读一个 ID 的全部来源，列表顺序即覆盖顺序，最后一项优先级最高 */
    private static List<ProvidedJson> readDuplicatedSources(ResourceLocation id, List<Resource> resources) {
        return resources.stream()
                .map(resource -> new ProvidedJson(resource.sourcePackId(), FileIOUtil.readJson(id, resource)))
                .toList();
    }

}
