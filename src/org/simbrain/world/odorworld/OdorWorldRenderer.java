package org.simbrain.world.odorworld;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Iterator;

import org.simbrain.util.SimbrainMath;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * The OdorWorldRenderer class draws a TileMap on the screen. It draws all
 * tiles, sprites, and an optional background image centered around the position
 * of the player.
 *
 * <p>
 * If the width of background image is smaller the width of the tile map, the
 * background image will appear to move slowly, creating a parallax background
 * effect.
 *
 * <p>
 * Also, three static methods are provided to convert pixels to tile positions,
 * and vice-versa.
 *
 * <p>
 * This TileMapRender uses a tile size of 64.
 *
 * See: http://www.cs.miami.edu/~visser/home_page/CSC_329_files/2DPlatform.pdf
 *
 * Adapted from Developing Games in Java, by David Brackeen.
 *
 */
public class OdorWorldRenderer {

    private static final int TILE_SIZE = 32;
    // the size in bits of the tile
    // Math.pow(2, TILE_SIZE_BITS) == TILE_SIZE
    private static final int TILE_SIZE_BITS = 6;

    /** Background image.*/
    private Image background;

    /** Sensor color. */
    private static float sensorColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    /** Sensor diameter. */
    private final static int SENSOR_DIAMATER = 6;

    /**
     * Converts a pixel position to a tile position.
     */
    public static int pixelsToTiles(float pixels) {
        return pixelsToTiles(Math.round(pixels));
    }


    /**
     * Converts a pixel position to a tile position.
     */
    public static int pixelsToTiles(int pixels) {
        // use shifting to get correct values for negative pixels
        //return pixels >> TILE_SIZE_BITS;

        // or, for tile sizes that aren't a power of two,
        // use the floor function:
        return (int)Math.floor((float)pixels / TILE_SIZE);
    }


    /**
     * Converts a tile position to a pixel position.
     */
    public static int tilesToPixels(int numTiles) {
        // no real reason to use shifting here.
        // it's slighty faster, but doesn't add up to much
        // on modern processors.
       // return numTiles << TILE_SIZE_BITS;

        // use this if the tile size isn't a power of 2:
        return numTiles * TILE_SIZE;
    }


    /**
     * Sets the background to draw.
     */
    public void setBackground(Image background) {
        this.background = background;
    }


    /**
     * Draws the specified TileMap.
     *
     * TODO: to be refactored
     */
    public void draw(Graphics2D g, OdorWorld world, int screenWidth, int screenHeight)
    {

        // Draw white background, if needed
        if (background == null || screenHeight > background.getHeight(null)) {
            g.setColor(Color.white);
            g.fillRect(0, 0, screenWidth, screenHeight);
        }

        // Draw background image, if any
        if (background != null) {
            g.drawImage(background, 0, 0, null);
        }

        // Draw the outline of the world (For debugging)
        //int mapHeight = tilesToPixels(map.getHeight());
        //int mapWidth = tilesToPixels(map.getWidth());
        //g.setColor(Color.black);
        //g.drawRect(0, 0, mapWidth, mapHeight);

        // Draw all tiles   
//        for (int y = 0; y < map.getHeight(); y++) {
//            for (int x = 0; x <= map.getWidth(); x++) {
//                Image image = map.getTile(x, y);
//                if (image != null) {
//                    g.drawImage(image,
//                        tilesToPixels(x),
//                        tilesToPixels(y),
//                        null);
//                }
//            }
//        }

        // Draw entities
        Iterator<OdorWorldEntity> i = world.getObjectList().iterator();
        while (i.hasNext()) {
            OdorWorldEntity sprite = i.next();
            int x = Math.round(sprite.getX());
            int y = Math.round(sprite.getY());
            //  Below: Was thinking about some sort of indication that entities collided.
            //if (sprite.hasCollided()) {
            //    g.drawRect((int) sprite.getX(), (int) sprite.getY(),sprite.getWidth(), sprite.getHeight());
            //}
            g.drawImage(sprite.getImage(), x, y, null);
            if (sprite.isShowSensors()) {
                // g.drawRect((int) sprite.getX(), (int) sprite.getY(),
                // sprite.getWidth(), sprite.getHeight());
                for (Sensor sensor : sprite.getSensors()) {
                    if (sensor instanceof SmellSensor) {
                        double val = SimbrainMath
                                .getVectorNorm(((SmellSensor) sensor)
                                        .getCurrentValue());
                        float saturation = checkValid((float) Math.abs(val
                                / (1 * world.getTotalSmellVectorLength())));
                        g.setPaint(Color.getHSBColor(sensorColor, saturation,
                                (float) 1));
                        // System.out.println(val + "--" + world.getTotalSmellVectorLength());
                        double[] location = ((SmellSensor) sensor)
                                .getLocation();
                        g.fillOval((int) location[0] - SENSOR_DIAMATER / 2,
                                (int) location[1] - SENSOR_DIAMATER / 2,
                                SENSOR_DIAMATER, SENSOR_DIAMATER);
                        g.setColor(Color.black);
                        g.drawOval((int) location[0] - SENSOR_DIAMATER / 2,
                                (int) location[1] - SENSOR_DIAMATER / 2,
                                SENSOR_DIAMATER, SENSOR_DIAMATER);
                    }
                }
            }
        }
    }

    /**
     * Check whether the specified saturation is valid or not.
     *
     * @param val the saturation value to check.
     * @return whether it is valid or not.
     */
    private float checkValid(final float val) {
        float tempval = val;

        if (val > 1) {
            tempval = 1;
        }

        if (val < 0) {
            tempval = 0;
        }

        return tempval;
    }

}
