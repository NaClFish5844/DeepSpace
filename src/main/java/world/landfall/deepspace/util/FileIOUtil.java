package world.landfall.deepspace.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static world.landfall.deepspace.util.AccessUtil.*;

public final class FileIOUtil {

    public static <T> T readFile(Path path, Function<InputStream, T> reader) {
        try ( InputStream stream = Files.newInputStream(path) ) {
            return reader.apply(stream);
        } catch ( IOException e ) {
            getLogger().error("读取文件失败：{} | Failed to read file: {}", path, path, e);
            return null;
        }
    }
    public static <T> T readFile(String path, Function<InputStream, T> reader) {
        try ( InputStream stream = Files.newInputStream( Path.of(path) ) ) {
            return reader.apply(stream);
        } catch ( IOException e ) {
            getLogger().error("读取文件失败：{} | Failed to read file: {}", path, path, e);
            return null;
        }
    }
    public static <T> T readFile(Path root, String validRoot, String filePath, Function<InputStream, T> reader) {
        if ( root.toString().endsWith(".zip") ) {
            return readZipEntry(root, concatPath(validRoot, filePath), reader);
        }
        return readFile( folderPathOf(root, validRoot, filePath), reader );
    }
    public static <T> T readZipEntry(Path zip, String entry, Function<InputStream, T> reader) {
        try ( ZipFile zipFile = openZip(zip) ) {
            ZipEntry zipEntry = findEntry(zipFile, entry);
            if ( zipEntry == null ) {
                getLogger().warn("压缩包内未找到条目：{} -> {} | Entry not found in zip: {} -> {}", zip, entry, zip, entry);
                return null;
            }
            try ( InputStream stream = zipFile.getInputStream(zipEntry) ) {
                return reader.apply(stream);
            }
        } catch ( IOException e ) {
            getLogger().error("读取压缩包条目失败：{} -> {} | Failed to read zip entry: {} -> {}", zip, entry, zip, entry, e);
            return null;
        }
    }
    /** 从 MC 的数据来源读一个 JSON；读取或解析失败只打日志并返回 null，不打断整批 */
    @Nullable public static JsonElement readJson(ResourceLocation id, Resource resource) {
        String text;
        try ( Reader reader = resource.openAsReader() ) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ( (read = reader.read(buffer)) != -1 ) { builder.append(buffer, 0, read); }
            text = builder.toString();
        } catch ( IOException e ) {
            getLogger().error("读取数据文件失败：{}（来源 {}） | Failed to read data file: {} (from {})",
                    id, resource.sourcePackId(), id, resource.sourcePackId(), e);
            return null;
        }

        try {
            return JsonParser.parseString(text);
        } catch ( RuntimeException directError ) {
            return repairAndParse(text, directError);
        }
    }


    @Nullable private static JsonElement repairAndParse(String text, RuntimeException directError) {
        String normalized = normalizeJsonPunctuation(text);

        if ( !normalized.equals(text) ) {
            try {
                JsonElement fixed = JsonParser.parseString(normalized);
                getLogger().warn("JSON 含全角字符，已自动修正后解析成功 | Full-width characters auto-corrected | {}",
                        directError.getMessage());
                return fixed;
            } catch ( RuntimeException ignored ) { }
        }

        getLogger().error("读取 JSON 失败：{} | Failed to parse JSON: {}",
                directError.getMessage(), directError.getMessage());
        return null;
    }
    private static String normalizeJsonPunctuation(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean inString = false;
        boolean escaped = false;

        for ( int i = 0; i < text.length(); i++ ) {
            char c = text.charAt(i);

            if ( inString ) {
                if ( escaped ) { out.append(c); escaped = false; continue; }
                if ( c == '\\' ) { out.append(c); escaped = true; continue; }
                if ( c == '"' ) { out.append(c); inString = false; continue; }
                if ( c == '“' || c == '”' ) { out.append('"'); inString = false; continue; }
                out.append(c);
                continue;
            }

            if ( c == '"' ) { out.append(c); inString = true; continue; }
            if ( c == '“' || c == '”' ) { out.append('"'); inString = true; continue; }

            out.append(toHalfWidth(c));
        }
        return out.toString();
    }
    private static char toHalfWidth(char c) {
        if ( c == '　' ) { return ' '; }
        if ( c == '。' ) { return '.'; }
        if ( c == '‘' || c == '’' ) { return '\''; }
        if ( c >= '\uFF01' && c <= '\uFF5E' ) { return (char) (c - 0xFEE0); }
        return c;
    }
    private static boolean hasBrokenNames(ZipFile zip) {
        return zip.stream().anyMatch( e -> e.getName().indexOf('\uFFFD') >= 0 );
    }

    public static Set<String> treeDir(Path path) {
        Set<String> files = new HashSet<>();
        if ( !Files.isDirectory(path) ) { return files; }

        try ( Stream<Path> walk = Files.walk(path) ) {
            walk.filter(Files::isRegularFile)
                    .forEach( p -> files.add( pathToString(path.relativize(p)) ) );
        } catch ( IOException e ) {
            getLogger().error("扫描目录树失败：{} | Failed to walk directory tree: {}", path, path, e);
        }
        return files;
    }
    public static Set<String> listZipEntries(Path zip) {
        Set<String> entryNames = new HashSet<>();
        try ( ZipFile zipFile = openZip(zip) ) {
            zipFile.stream()
                    .map( e -> pathToString(e.getName()) )
                    .forEach(entryNames::add);
        } catch ( IOException e ) {
            getLogger().error("列出压缩包条目失败：{} | Failed to list zip entries: {}", zip, zip, e);
        }
        return entryNames;
    }
    public static Set<String> listZipEntriesOfDirEntry(Path zip, String dirEntry) {
        Set<String> files = new HashSet<>();

        String prefix = dirEntry.isEmpty()
                ? ""
                : ( dirEntry.endsWith("/") ? dirEntry : dirEntry + "/" );

        for ( String name : listZipEntries(zip) ) {
            if ( !name.startsWith(prefix) ) { continue; }
            if ( name.endsWith("/") ) { continue; }

            files.add(name);
        }
        return files;
    }

    public static String pathToString(String path) {
        if ( path == null || path.isEmpty() ) { return path; }

        String normalized = path.replace('\\', '/');
        while ( normalized.contains("//") ) { normalized = normalized.replace("//", "/"); }
        return normalized;
    }
    public static String pathToString(Path path) {
        if ( path == null ) { return null; }
        return pathToString( path.toString() );
    }

    public static ZipFile openZip(Path zipPath) throws IOException {
        ZipFile utf8 = new ZipFile(zipPath.toFile());
        if ( !hasBrokenNames(utf8) ) { return utf8; }
        utf8.close();

        ZipFile gb = new ZipFile(zipPath.toFile(), Charset.forName("GB18030"));
        if ( !hasBrokenNames(gb) ) { return gb; }
        gb.close();

        return new ZipFile(zipPath.toFile());
    }
    @Nullable public static ZipEntry findEntry(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        if ( entry != null ) { return entry; }

        String target = pathToString(name);
        for ( ZipEntry candidate : zip.stream().toList() ) {
            if ( pathToString(candidate.getName()).equals(target) ) { return candidate; }
        }
        return null;
    }

    private static String concatPath(String validRoot, String path) {
        if ( validRoot == null || validRoot.isEmpty() ) { return path; }
        return validRoot + "/" + path;
    }
    private static Path folderPathOf(Path root, String validRoot, String filePath) {
        if ( validRoot == null || validRoot.isEmpty() ) { return root.resolve(filePath); }
        return root.resolve(validRoot).resolve(filePath);
    }

}
