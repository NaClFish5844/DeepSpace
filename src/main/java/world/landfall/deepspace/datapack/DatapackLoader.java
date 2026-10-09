package world.landfall.deepspace.datapack;

import com.google.gson.*;
import world.landfall.deepspace.Deepspace;
import world.landfall.deepspace.datapack.property.config.DefaultConfig;
import world.landfall.deepspace.datapack.property.config.WorldConfig;
import world.landfall.deepspace.datapack.property.sublevel.*;
import world.landfall.deepspace.util.FileIOUtil;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.*;

import static world.landfall.deepspace.util.AccessUtil.*;


public final class DatapackLoader {
    @Getter private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    public static ObjectSet<DatapackSource> loadDatapackSources() {
        ObjectSet<DatapackSource> datapacks = new ObjectArraySet<>();

        for ( Path root : DatapackScanner.scanDatapackRoots() ) {
            if ( root.toString().endsWith(".zip") ) {
                for ( String validRoot : DatapackScanner.scanRelativeValidRoot(root) ) {
                    PackMeta packMeta = DatapackScanner.readPackMeta(root, validRoot);
                    if ( packMeta == null ) { continue; }

                    datapacks.add( DatapackSource.ofZip(root, validRoot.isEmpty() ? null : validRoot, packMeta) );
                }
                continue;
            }

            PackMeta packMeta = DatapackScanner.readPackMeta(root, null);
            if ( packMeta == null ) { continue; }

            datapacks.add( DatapackSource.ofDir(root, packMeta) );
        }
        return datapacks;
    }
    public static DefaultConfig loadDefaultConfig() {
        DefaultConfig defaultConfig = DefaultConfig.ofDefault();

        Path path = DatapackScanner.getDefaultConfig();
        if ( path == null ) { return defaultConfig; }

        JsonObject object = FileIOUtil.readFile(path, FileIOUtil::readAsJson);
        if ( object == null ) { return defaultConfig; }

        if ( DatapackChecker.checkDefaultWorldConfigFormat(object) ) {
            defaultConfig.setWorldLevel( DatapackChecker.getNoDuplicatedSortedArrList(object.get("levels")) );
            defaultConfig.setEnemyPrefix( object.get("enemy_prefix").getAsString() );
            defaultConfig.setAllyPrefix( object.get("ally_prefix").getAsString() );
            defaultConfig.setNeutralPrefix( object.get("neutral_prefix").getAsString() );
        }

        return defaultConfig;
    }
    public static Object2ObjectOpenHashMap<PropertyKey, AbstractSchematicProperty> loadProperties(DatapackSource datapack) {
        Set<String> propertyPath = DatapackScanner.scanPropertiesOfPack(datapack);
        Object2ObjectOpenHashMap<PropertyKey, AbstractSchematicProperty> properties = new Object2ObjectOpenHashMap<>();
        if ( propertyPath.isEmpty() ) { return properties; }
        String packName = datapack.getPackMeta().packName();

        for ( String path : propertyPath ) {
            JsonObject object = FileIOUtil.readDatapackFile(datapack, path, FileIOUtil::readAsJson);
            if ( object == null ) {
                getLogger().warn("加载蓝图属性文件失败：{} -> {} | Failed to load property file: {} -> {}", packName, path, packName, path);
                continue;
            }

            DatapackChecker.PropertyCheckResult result = DatapackChecker.checkPropertyFormat(object);

            if ( !result.isTopLevelKeysValid() || !result.isTypeKeysValid() ) { continue; }

            if ( result.isEnemyPropertyValid() ) {
                EnemyProperty property = parseAsEnemyProperty(datapack, object);
                PropertyKey key = PropertyKey.of( getFileNameWithoutSuffix(path) ,packName, property.getSublevelType() );
                properties.put(key, property);
            }
            if ( result.isAllyPropertyValid() ) {
                AllyProperty property = parseAsAllyProperty(datapack, object);
                PropertyKey key = PropertyKey.of( getFileNameWithoutSuffix(path) ,packName, property.getSublevelType() );
                properties.put(key, property);
            }
            if ( result.isPrefabPropertyValid() ) {
                PrefabProperty property = parseAsPrefabProperty(datapack, object);
                PropertyKey key = PropertyKey.of( getFileNameWithoutSuffix(path) ,packName, property.getSublevelType() );
                properties.put(key, property);
            }

        }

        return properties;
    }
    public static Object2ObjectOpenHashMap<String, DefaultConfig> loadWorldConfigs(DatapackSource datapack, DefaultConfig defaultConfig) {
        Set<String> configPath = DatapackScanner.scanWorldConfigsOfPack(datapack);
        Object2ObjectOpenHashMap<String, DefaultConfig> configs = new Object2ObjectOpenHashMap<>();
        if ( configPath.isEmpty() ) { return configs; }

        for ( String path : configPath ) {
            WorldConfig worldConfig = WorldConfig.ofBasic(defaultConfig);

            JsonObject object = FileIOUtil.readDatapackFile(datapack, path, FileIOUtil::readAsJson);
            if ( object == null ) {
                getLogger().warn("加载世界配置文件失败：{} -> {} | Failed to load worldconfig file: {} -> {}", datapack.getPackMeta().packName(), path, datapack.getPackMeta().packName(), path);
                continue;
            }

            DatapackChecker.WorldConfigCheckResult result = DatapackChecker.checkWorldConfigFormat(object);

            if ( result.isDimensionValid() ) {
                String dimension = object.get("dimension").getAsString();
                worldConfig.setDimension(dimension);
            } else {
                continue;
            }
            if ( result.isSpawnPatternValid() ) { worldConfig.setSpawnPattern( WorldConfig.Pattern.valueOf( object.get("spawn_pattern").getAsString() ) ); }

            if ( result.isWorldLevelValid() ) { worldConfig.setWorldLevel( DatapackChecker.getNoDuplicatedSortedArrList(object.get("world_level")) ); }
            if ( result.isEnemyPrefixValid() ) { worldConfig.setEnemyPrefix( object.get("enemy_prefix").getAsString() ); }
            if ( result.isAllyPrefixValid() ) { worldConfig.setAllyPrefix( object.get("ally_prefix").getAsString() ); }
            if ( result.isNeutralPrefixValid() ) { worldConfig.setNeutralPrefix( object.get("neutral_prefix").getAsString() ); }

            configs.put(worldConfig.getDimension(), worldConfig);
        }

        return configs;
    }

    private static EnemyProperty parseAsEnemyProperty(DatapackSource datapack, JsonObject object) {
        return (EnemyProperty) parseBlock(datapack.getPackMeta().packName(), object, "enemy_property", EnemyProperty.class );
    }
    private static AllyProperty parseAsAllyProperty(DatapackSource datapack, JsonObject object) {
        return (AllyProperty) parseBlock(datapack.getPackMeta().packName(), object, "ally_property", AllyProperty.class );
    }
    private static PrefabProperty parseAsPrefabProperty(DatapackSource datapack, JsonObject object) {
        return (PrefabProperty) parseBlock(datapack.getPackMeta().packName(), object, "prefab_property", PrefabProperty.class );
    }

    private static AbstractSchematicProperty parseBlock(String packName, JsonObject object, String blockKey, Class<? extends AbstractSchematicProperty> propertyClass) {
        AbstractSchematicProperty prop;
        try {
            prop = GSON.fromJson( object.get(blockKey), propertyClass );
        } catch ( RuntimeException e ) {
            getLogger().warn("[{}] 反序列化失败，跳过该类型：{} | [{}] deserialization failed, skipping this type: {}", blockKey, e, blockKey, e.toString());
            return null;
        }
        parseBaseProperty(prop, packName, object);

        return prop;
    }
    private static void parseBaseProperty(AbstractSchematicProperty dest, String packName, JsonObject object) {
        dest.setPackName(packName);

        dest.setSchematicSource(DatapackManager.BlueprintSourceFileLocation.valueOf(object.get("schematic_source").getAsString()));
        dest.setSourceModId(DatapackManager.BlueprintSourceModId.valueOf(object.get("source_mod_id").getAsString()));

        String name = object.get("schematic_name").getAsString();
        dest.setSchematicName(name);

        if ( object.has("sublevel_function") ) {
            dest.setSublevelFunction(AbstractSchematicProperty.SublevelFunction.valueOf(object.get("sublevel_function").getAsString()));
        }

    }

    private static String getFileNameWithoutSuffix(String path) {
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;

        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

}
