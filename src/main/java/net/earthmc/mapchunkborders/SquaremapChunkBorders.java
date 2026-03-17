package net.earthmc.mapchunkborders;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.ChunkyBorderProvider;
import org.popcraft.chunkyborder.event.border.BorderChangeEvent;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.Color;

public class SquaremapChunkBorders extends JavaPlugin {
    private final Key layerKey = Key.of("chunk-borders");
    private ChunkyBorder chunkyBorder;
    private Squaremap squaremap;

    @Override
    public void onEnable() {
        chunkyBorder = ChunkyBorderProvider.get();
        squaremap = SquaremapProvider.get();

        chunkyBorder.getChunky().getEventBus().subscribe(BorderChangeEvent.class, event -> {
            final BorderData borderData = chunkyBorder.getBorder(event.world().getName()).orElse(null);

            createBorderLayer(event.world().getKey(), event.shape() == null || borderData == null
                ? null
                : new Bounds(borderData.getCenterX(), borderData.getCenterZ(), borderData.getRadiusX(), borderData.getRadiusZ())
            );
        });

        squaremap.mapWorlds().forEach(mapWorld -> {
            final World world = BukkitAdapter.bukkitWorld(mapWorld);

            final BorderData borderData = chunkyBorder.getBorder(world.getName()).orElse(null);
            if (borderData == null) {
                return;
            }

            createBorderLayer(mapWorld.identifier().asString(), new Bounds(borderData.getCenterX(), borderData.getCenterZ(), borderData.getRadiusX(), borderData.getRadiusZ()));
        });
    }

    private void createBorderLayer(final String worldKey, final @Nullable Bounds bounds) {
        final MapWorld mapWorld = squaremap.getWorldIfEnabled(WorldIdentifier.parse(worldKey)).orElse(null);
        if (mapWorld == null) {
            return;
        }

        if (bounds == null) {
            // border is removed
            if (mapWorld.layerRegistry().hasEntry(layerKey)) {
                mapWorld.layerRegistry().unregister(layerKey);
            }

            return;
        }

        final SimpleLayerProvider provider = getOrCreateChunkyOverlay(mapWorld);

        final int leftCoord = normalizePoint(bounds.centerX - bounds.radiusX);
        final int topCoord = normalizePoint(bounds.centerZ - bounds.radiusZ);
        final int rightCoord = normalizePoint(bounds.centerX + bounds.radiusX);
        final int bottomCoord = normalizePoint(bounds.centerZ + bounds.radiusZ);

        final MarkerOptions options = MarkerOptions.builder()
            .strokeColor(Color.BLACK)
            .fill(false)
            .strokeWeight(1)
            .build();

        // add vertical lines along the top
        for (int x = leftCoord; x <= rightCoord; x += 16) {
            provider.addMarker(Key.key("chunk-border-x-" + x), Marker.polyline(Point.point(x, topCoord), Point.point(x, bottomCoord)).markerOptions(options));
        }

        // and add horizontal lines along the side
        for (int z = topCoord; z <= bottomCoord; z += 16) {
            provider.addMarker(Key.key("chunk-border-z-" + z), Marker.polyline(Point.point(leftCoord, z), Point.point(rightCoord, z)).markerOptions(options));
        }
    }

    private int normalizePoint(final double point) {
        return ((int) Math.floor(point) >> 4) * 16;
    }

    private SimpleLayerProvider getOrCreateChunkyOverlay(final MapWorld world) {
        if (world.layerRegistry().hasEntry(layerKey)) {
            return (SimpleLayerProvider) world.layerRegistry().get(layerKey);
        } else {
            SimpleLayerProvider provider = SimpleLayerProvider.builder("Chunk Borders")
                .defaultHidden(true)
                .layerPriority(100)
                .zIndex(100)
                .build();

            world.layerRegistry().register(layerKey, provider);

            return provider;
        }
    }

    private record Bounds(double centerX, double centerZ, double radiusX, double radiusZ) {}
}
