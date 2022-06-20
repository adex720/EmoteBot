package com.adex.emotebot;

import org.imgscalr.Scalr;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class Editor {

    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    public static BufferedImage rotate(BufferedImage image, int degrees, boolean transparent) {
        int drawLocationX = 0;
        int drawLocationY = 0;

        double rotationRequired = Math.toRadians(degrees);
        double locationX = image.getWidth() / 2d;
        double locationY = image.getHeight() / 2d;
        AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);

        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = newImage.getGraphics();

        if (!transparent) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(TRANSPARENT);
        }
        g.drawRect(0, 0, image.getWidth(), image.getHeight());


        g.drawImage(op.filter(image, null), drawLocationX, drawLocationY, null);
        return newImage;
    }

    public static BufferedImage makeSmaller(BufferedImage image, int size) {
        if (size == 100) {
            return image;
        }

        return Scalr.resize(image, Scalr.Method.BALANCED, image.getWidth() * size / 100, image.getHeight() * size / 100);
    }
}
