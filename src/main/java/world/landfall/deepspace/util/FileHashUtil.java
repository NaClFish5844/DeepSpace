package world.landfall.deepspace.util;

import world.landfall.deepspace.Deepspace;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static world.landfall.deepspace.util.AccessUtil.*;

public final class FileHashUtil {
    private FileHashUtil() {}

    public static String getFileMD5(Path path) {
        try ( InputStream stream = Files.newInputStream(path) ) {
            return hashStream(stream);
        } catch ( IOException e ) {
            getLogger().error("计算文件 MD5 失败：{} | Failed to compute file MD5: {}", path, path, e);
            return null;
        }
    }
    public static String getFileMD5(InputStream stream) {
        if ( stream == null ) { return null; }
        return hashStream(stream);
    }

    private static String hashStream(InputStream stream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ( (read = stream.read(buffer)) != -1 ) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex( digest.digest() );
        } catch ( IOException | NoSuchAlgorithmException e ) {
            getLogger().error("计算 MD5 失败 | Failed to compute MD5", e);
            return null;
        }
    }

}
