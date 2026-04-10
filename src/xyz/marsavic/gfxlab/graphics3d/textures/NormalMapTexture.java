package xyz.marsavic.gfxlab.graphics3d.textures;

import xyz.marsavic.functions.F1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.Vec3;
import xyz.marsavic.utils.Numeric;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public record NormalMapTexture(BufferedImage image) implements F1<Vec3, Vector> {

    public static NormalMapTexture fromFile(String path) throws IOException {
        return new NormalMapTexture(ImageIO.read(new File(path)));
    }

    public static NormalMapTexture empty(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int rgb = (128 << 16) | (128 << 8) | 255;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return new NormalMapTexture(img);
    }

    @Override
    public Vec3 at(Vector uv) {
        int w = image.getWidth();
        int h = image.getHeight();

        int x = (int) Numeric.mod((int) (uv.x() * w), w);
        int y = (int) Numeric.mod((int) (uv.y() * h), h);

        int rgb = image.getRGB(x, y);
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        return new Vec3(
                r * 2.0 - 1.0,
                g * 2.0 - 1.0,
                b * 2.0 - 1.0
        ).normalized_();
    }
}