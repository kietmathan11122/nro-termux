package nro.models.data;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Original map backgrounds, shared by every client zoom level. */
public final class MapBackgroundStore {
    public static final String PREFIX = "mapbg_v1_";
    public static final int CHUNK_BYTES = 64 * 1024;
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final Pattern REQUEST = Pattern.compile("mapbg_v1_([0-9]{3})_([0-9]{1,8})");

    private MapBackgroundStore() { }

    public static int[] parseRequest(String name) {
        if (name == null) return null;
        Matcher match = REQUEST.matcher(name);
        if (!match.matches()) return null;
        int map = Integer.parseInt(match.group(1));
        int offset = Integer.parseInt(match.group(2));
        if (fileName(map) == null || offset >= MAX_BYTES || offset % CHUNK_BYTES != 0) return null;
        return new int[] { map, offset };
    }

    public static String fileName(int map) {
        switch (map) {
            case 1: return "bg_doi_hoa_cuc.png";
            case 2: return "bg_thung_lung_tre.png";
            case 3: return "bg_rung_nam.png";
            case 21: return "bg_nha_gohan.png";
            case 125: case 130: case 150: case 180: case 181: case 182:
            case 186: case 187: case 188: case 189: case 190: case 192:
            case 193: case 194: case 195: case 196: case 197: case 212:
                return null;
            default: return map >= 4 && map <= 216 ? String.format(java.util.Locale.ROOT, "map_%03d.png", map) : null;
        }
    }

    /** Payload: big-endian total bytes, modification time, then one PNG chunk. */
    public static byte[] readChunk(Path directory, int map, int offset) throws IOException {
        String name = fileName(map);
        if (name == null || offset < 0 || offset >= MAX_BYTES || offset % CHUNK_BYTES != 0) return null;
        Path path = directory.resolve(name);
        if (!Files.isRegularFile(path)) return null;
        long revision = Files.getLastModifiedTime(path).toMillis();
        try (FileChannel file = FileChannel.open(path, StandardOpenOption.READ)) {
            long size = file.size();
            if (size < 24 || size > MAX_BYTES || offset >= size) return null;
            int count = (int)Math.min(CHUNK_BYTES, size - offset);
            ByteBuffer chunk = ByteBuffer.allocate(count);
            file.position(offset);
            while (chunk.hasRemaining()) {
                if (file.read(chunk) < 0) return null;
            }
            if (file.size() != size || Files.getLastModifiedTime(path).toMillis() != revision) return null;
            ByteArrayOutputStream result = new ByteArrayOutputStream(count + 12);
            try (DataOutputStream out = new DataOutputStream(result)) {
                out.writeInt((int)size);
                out.writeLong(revision);
                out.write(chunk.array());
            }
            return result.toByteArray();
        }
    }
}
