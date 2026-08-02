package com.pathmarker;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class PathMinimapMarkerOverlay extends Overlay
{
    private final Client client;
    private final PathMarkerPlugin plugin;

    @Inject
    private PathMarkerConfig config;

    @Inject
    private PathMinimapMarkerOverlay(Client client, PathMarkerConfig config, PathMarkerPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (client.getLocalPlayer() == null)
        {
            return null;
        }
        double angle = client.getCameraYawTarget() * 0.000383495196971D;
        Widget minimapDrawWidget;
        if (client.isResized())
        {
            if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1)
            {
                minimapDrawWidget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA);
            }
            else
            {
                minimapDrawWidget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA);
            }
        }
        else
        {
            minimapDrawWidget = client.getWidget(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
        }
        if (minimapDrawWidget == null || minimapDrawWidget.isHidden())
        {
            return null;
        }
        Point minimapWidgetLocation = minimapDrawWidget.getCanvasLocation();
        Point minimapPoint = new Point( minimapWidgetLocation.getX() + minimapDrawWidget.getWidth()/2, minimapWidgetLocation.getY() + minimapDrawWidget.getHeight()/2);
        graphics.rotate(angle, minimapPoint.getX(), minimapPoint.getY());
        if ((config.hoverPathDisplaySetting() != PathMarkerConfig.PathDisplaySetting.NEVER)
                && (config.activePathDisplaySetting() == PathMarkerConfig.PathDisplaySetting.NEVER || !plugin.isPathActive() || !config.drawOnlyIfNoActivePath())
                && (plugin.isKeyDisplayHoverPath() || config.hoverPathDisplaySetting() == PathMarkerConfig.PathDisplaySetting.ALWAYS))
        {
            for (WorldPoint worldPoint : plugin.getHoverPathTiles())
            {
                if (config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.MINIMAP)
                {
                    if (config.hoverPathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getHoverPathTiles().get(plugin.getHoverPathTiles().size() - 1))
                    {
                        renderMinimapTile(graphics, worldPoint, config.hoverPathFill1(), minimapPoint);
                    }
                }
            }
            for (WorldPoint worldPoint : plugin.getHoverMiddlePathTiles())
            {
                if (config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.hoverPathDrawLocations() == PathMarkerConfig.drawLocations.MINIMAP)
                {
                    if (config.hoverPathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getHoverPathTiles().get(plugin.getHoverPathTiles().size() - 1))
                    {
                        renderMinimapTile(graphics, worldPoint, config.hoverPathFill2(), minimapPoint);
                    }
                }
            }
        }
        if ((config.activePathDisplaySetting() != PathMarkerConfig.PathDisplaySetting.NEVER) && plugin.isPathActive()
                && (plugin.isKeyDisplayActivePath() || config.activePathDisplaySetting() == PathMarkerConfig.PathDisplaySetting.ALWAYS))
        {
            for (WorldPoint worldPoint : plugin.getActivePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.activePathDrawLocations() == PathMarkerConfig.drawLocations.MINIMAP)
                {
                    if (config.activePathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getActivePathTiles().get(plugin.getActivePathTiles().size() - 1))
                    {
                        renderMinimapTile(graphics, worldPoint, config.activePathFill1(), minimapPoint);
                    }
                }
            }
            for (WorldPoint worldPoint : plugin.getActiveMiddlePathTiles())
            {
                if (config.activePathDrawLocations() == PathMarkerConfig.drawLocations.BOTH || config.activePathDrawLocations() == PathMarkerConfig.drawLocations.MINIMAP)
                {
                    if (config.activePathDrawMode() == PathMarkerConfig.DrawMode.FULL_PATH || worldPoint == plugin.getActivePathTiles().get(plugin.getActivePathTiles().size() - 1))
                    {
                        renderMinimapTile(graphics, worldPoint, config.activePathFill2(), minimapPoint);
                    }
                }
            }
        }
        graphics.rotate(-angle, minimapPoint.getX(), minimapPoint.getY());
        return null;
    }

    private void renderMinimapTile(Graphics2D graphics, WorldPoint worldPoint, Color color, Point miniMapPoint)
    {
        double minimapTileSize = client.getMinimapZoom();
        LocalPoint lp = LocalPoint.fromWorld(client.getLocalPlayer().getWorldView(), worldPoint);
        if (lp == null)
        {
            return;
        }
        LocalPoint localLocation = client.getLocalPlayer().getLocalLocation();
        if (localLocation == null)
        {
            return;
        }
        // Every world tile is 128 units apart
        double locationSize = 128.0/minimapTileSize;
        int x = lp.getX() - localLocation.getX();
        int y = localLocation.getY() - lp.getY();
        int squareDistance = x*x + y*y;
        // Roughly measured
        double tileRadius = 74.0 / minimapTileSize;
        double maxDistance = tileRadius * 128.0;
        int maxSquareDistance = (int)(maxDistance * maxDistance);
        if (squareDistance > maxSquareDistance) {
            return;
        }
        int tileStartX = (int)Math.round((x - 64) / locationSize);
        int tileStartY = (int)Math.round((y - 64) / locationSize);
        int tileEndX = (int)Math.round((x + 64) / locationSize);
        int tileEndY = (int)Math.round((y + 64) / locationSize);
        int tileSizeX = tileEndX - tileStartX;
        int tileSizeY = tileEndY - tileStartY;
        graphics.setColor(color);
        graphics.fillRect(tileStartX + miniMapPoint.getX(), tileStartY + miniMapPoint.getY(), tileSizeX, tileSizeY);
    }
}
