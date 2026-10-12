package nro.models.data;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import nro.models.network.MySession;
import nro.models.player_system.Template.BgItem;

/** Source-served foregrounds and grounded decorations; native geometry is unchanged. */
public final class ServerMapScenery {
    private static final Path ROOT = Path.of("data/map");
    private static volatile Catalog catalog;
    private static final Set<MySession> SENT = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean warned;
    private ServerMapScenery() { }

    private static final class Route {
        final int bank;
        final String alias;
        Route(int bank, String alias) { this.bank = bank; this.alias = alias; }
    }
    private static final class Catalog {
        final List<BgItem> templates;
        final Map<Integer, Route> routes;
        Catalog(List<BgItem> templates, Map<Integer, Route> routes) {
            this.templates = Collections.unmodifiableList(templates);
            this.routes = Collections.unmodifiableMap(routes);
        }
    }
    private static List<String> rows(String file) throws Exception {
        List<String> result = new ArrayList<>();
        for (String raw : Files.readAllLines(ROOT.resolve(file))) {
            String line = raw.trim();
            if (!line.isEmpty() && !line.startsWith("#")) result.add(line);
        }
        return result;
    }
    private static int bounded(String value, int min, int max) {
        int number = Integer.parseInt(value);
        if (number < min || number > max) throw new IllegalArgumentException("Scenery integer out of range");
        return number;
    }
    private static Catalog load(List<BgItem> originals) throws Exception {
        Set<Integer> keep = new HashSet<>();
        for (String row : rows("server-scenery-keep.csv")) keep.add(bounded(row, 0, 32767));
        Map<Integer, BgItem> templates = new HashMap<>();
        for (BgItem item : originals) {
            if (item.idImage >= 0 || keep.contains(item.id)) {
                if (templates.put(item.id, item) != null) throw new IllegalArgumentException("Duplicate original BG ID");
            }
        }
        if (!templates.keySet().containsAll(keep)) throw new IllegalArgumentException("Missing protected BG template");
        Set<Integer> newImages = new HashSet<>();
        for (String row : rows("server-scenery-templates.csv")) {
            String[] fields = row.split(",", -1);
            if (fields.length != 5) throw new IllegalArgumentException("Bad scenery template row");
            BgItem item = new BgItem();
            item.id = bounded(fields[0], 29000, 32767);
            item.idImage = (short) bounded(fields[1], -19999, -16000);
            item.layer = (byte) bounded(fields[2], 0, 4);
            item.dx = (short) bounded(fields[3], -32768, 32767);
            item.dy = (short) bounded(fields[4], -32768, 32767);
            if (templates.putIfAbsent(item.id, item) != null) throw new IllegalArgumentException("Scenery BG ID collision");
            newImages.add((int) item.idImage);
        }
        if (templates.size() * 10L + 2 > 65535) throw new IllegalArgumentException("BG catalog exceeds protocol packet");
        // Check every fresh image before enabling any route, so a partial copy fails closed.
        for (int image : newImages) for (int zoom = 1; zoom <= 4; zoom++) {
            Path path = Path.of("data/item_bg_temp/x" + zoom + "/" + image + ".png");
            long size = Files.size(path);
            if (size < 24 || size >= 0xffffff - 32) throw new IllegalArgumentException("Bad scenery image size");
            try (DataInputStream input = new DataInputStream(Files.newInputStream(path))) {
                if (input.readLong() != 0x89504e470d0a1a0aL || input.readInt() != 13 || input.readInt() != 0x49484452)
                    throw new IllegalArgumentException("Bad scenery PNG");
                int width = input.readInt(), height = input.readInt();
                if (width <= 0 || height <= 0 || width > 4096 || height > 4096)
                    throw new IllegalArgumentException("Scenery image dimensions out of bounds");
            }
        }
        Map<Integer, Route> routes = new HashMap<>();
        for (String row : rows("server-scenery-maps.csv")) {
            String[] fields = row.split(",", -1);
            if (fields.length != 3) throw new IllegalArgumentException("Bad scenery route row");
            int map = bounded(fields[0], 0, 32767), bank = bounded(fields[1], 0, 255);
            // Vach Aru (42) is explicitly included by the latest scenery request.
            if (map == 0 || map == 3 || map >= 244) throw new IllegalArgumentException("Protected map must keep its existing route");
            if (!fields[2].equals(map + "_server_scenery_20261010")) throw new IllegalArgumentException("Unsafe scenery alias");
            if (routes.put(map, new Route(bank, fields[2])) != null) throw new IllegalArgumentException("Duplicate scenery map");
            byte[] bytes = Files.readAllBytes(ROOT.resolve("item_bg_map_data/" + fields[2]));
            try (DataInputStream input = new DataInputStream(new java.io.ByteArrayInputStream(bytes))) {
                int count = input.readShort();
                if (count < 0 || bytes.length != count * 6 + 2) throw new IllegalArgumentException("Bad scenery placements");
                for (int i = 0; i < count; i++) {
                    if (!templates.containsKey((int) input.readShort())) throw new IllegalArgumentException("Unknown scenery template");
                    // Existing scenery may extend past a map edge; coordinates are signed shorts.
                    input.readShort(); input.readShort();
                }
            }
        }
        if (routes.isEmpty()) throw new IllegalArgumentException("Empty scenery routes");
        List<BgItem> sorted = new ArrayList<>(templates.values());
        sorted.sort((left, right) -> Integer.compare(left.id, right.id));
        System.out.println("[ServerMapScenery] Ready: " + routes.size() + " maps, " + sorted.size() + " BG templates");
        return new Catalog(sorted, routes);
    }
    private static Catalog ready(List<BgItem> originals) {
        Catalog value = catalog;
        if (value != null) return value;
        synchronized (ServerMapScenery.class) {
            if (catalog == null) {
                try { catalog = load(originals); }
                catch (Exception error) {
                    if (!warned) {
                        System.err.println("[ServerMapScenery] Keeping existing routes: " + error.getClass().getSimpleName() + ": " + error.getMessage());
                        warned = true;
                    }
                }
            }
            return catalog;
        }
    }
    public static List<BgItem> templatesFor(MySession session, List<BgItem> originals) {
        if (!DataGame.supportsNative95(session)) return originals;
        Catalog value = ready(originals);
        return value == null ? originals : value.templates;
    }
    public static void templatesSent(MySession session) {
        if (DataGame.supportsNative95(session) && catalog != null) synchronized (SENT) { SENT.add(session); }
    }
    public static String bgFile(MySession session, int map, int bank) {
        if (!DataGame.supportsNative95(session)) return null;
        Catalog value = ready(nro.models.server.Manager.BG_ITEMS);
        if (value == null) return null;
        Route route = value.routes.get(map);
        if (route == null || route.bank != bank) return null;
        synchronized (SENT) {
            if (!SENT.contains(session)) DataGame.sendDataItemBG(session);
            if (!SENT.contains(session)) return null;
        }
        return route.alias;
    }
}
