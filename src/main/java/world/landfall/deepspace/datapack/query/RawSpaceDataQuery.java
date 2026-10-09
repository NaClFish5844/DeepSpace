package world.landfall.deepspace.datapack.query;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import world.landfall.deepspace.datapack.DatapackProvider;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class RawSpaceDataQuery {
    // 由于工具类不走缓存 这东西将会成为模版
    private final Map<ResourceLocation, DatapackProvider.ProvidedJson> source;
    private BiPredicate<ResourceLocation, DatapackProvider.ProvidedJson> predicate = (id, data) -> true;

    public RawSpaceDataQuery(Map<ResourceLocation, DatapackProvider.ProvidedJson> source) {
        this.source = source;
    }

    public RawSpaceDataQuery ofGalaxy()           { return ofRoot("galaxies"); }
    public RawSpaceDataQuery ofPlanet()           { return ofType("planet"); }
    public RawSpaceDataQuery ofStar()             { return ofType("star"); }
    public RawSpaceDataQuery ofTeleporter()       { return ofType("teleporter"); }
    public RawSpaceDataQuery ofRingWorldBody()    { return ofRoot("ringworlds"); }
    public RawSpaceDataQuery ofRingWorldSection() { return ofType("ringWorldSection"); }

    public RawSpaceDataQuery where(BiPredicate<ResourceLocation, DatapackProvider.ProvidedJson> condition) {
        this.predicate = this.predicate.and(condition);
        return this;
    }
    private RawSpaceDataQuery ofRoot(String root) {
        return where((id, data) -> id.getPath().startsWith(root + "/"));
    }
    private RawSpaceDataQuery ofType(String type) {
        return where((id, data) -> type.equals(typeOf(data)));
    }

    private static String typeOf(DatapackProvider.ProvidedJson data) {
        JsonElement json = data.json();
        if (json == null || !json.isJsonObject()) { return ""; }

        JsonElement type = json.getAsJsonObject().get("type");
        return type == null || !type.isJsonPrimitive() ? "" : type.getAsString();
    }

    @Nullable public Map<ResourceLocation, DatapackProvider.ProvidedJson> collect() {
        return this.source.entrySet().stream()
                .filter(entry -> this.predicate.test(entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
