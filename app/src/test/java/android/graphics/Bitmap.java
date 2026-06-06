package android.graphics;

public class Bitmap {
    public enum Config {
        ARGB_8888
    }

    private final int width;
    private final int height;
    private final int[][] pixels;

    public Bitmap(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width][height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Pixel coordinate out of bounds: (" + x + ", " + y + ") for size " + width + "x" + height);
        }
        return pixels[x][y];
    }

    public void setPixel(int x, int y, int color) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IllegalArgumentException("Pixel coordinate out of bounds: (" + x + ", " + y + ") for size " + width + "x" + height);
        }
        pixels[x][y] = color;
    }

    public static Bitmap createBitmap(int width, int height, Config config) {
        return new Bitmap(width, height);
    }
}
