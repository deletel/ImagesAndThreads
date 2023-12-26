package ru.netology.graphics;

import ru.netology.graphics.image.BadImageSizeException;
import ru.netology.graphics.image.TextColorSchema;
import ru.netology.graphics.image.TextGraphicsConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Converter implements TextGraphicsConverter {

    //example: https://dtst.su/wp-content/uploads/2021/03/1c_3.png

    private int maxWidth = 10;
    private int maxHeight = 10;
    private double maxRatio;
    private static final int numOfThreads = 16;
    private static int newHeight;

    @Override
    public String convert(String url) throws IOException, BadImageSizeException {
        BufferedImage img = ImageIO.read(new URL(url));

        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        double imgRatio = imgWidth / imgHeight;
        if (maxRatio != 0 && maxRatio <= imgRatio) {
            throw new BadImageSizeException(maxRatio, imgRatio);
        }

        double needfulPercent;
        int newWidth = imgWidth;
        newHeight = imgHeight;
        if (maxWidth < imgWidth || maxHeight < imgHeight) {
            needfulPercent = (imgWidth / maxWidth > imgHeight / maxHeight) ? imgWidth / maxWidth : imgHeight / maxHeight;
            newWidth = (int) Math.round(imgWidth / needfulPercent);
            newHeight = (int) Math.round(imgHeight / needfulPercent);
        }

        Image scaledImage = img.getScaledInstance(newWidth, newHeight, BufferedImage.SCALE_SMOOTH);
        BufferedImage bwImg = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = bwImg.createGraphics();
        graphics.drawImage(scaledImage, 0, 0, null);  //scaledImage взять бы через if img

        ImageIO.write(bwImg, "png", new File("out.png"));

        WritableRaster bwRaster = bwImg.getRaster(); // проходимся по пикселям

        ForkJoinPool pool = new ForkJoinPool(numOfThreads);
        String bigString = pool.invoke(new MyFork(0, newWidth, newHeight, bwRaster));

        return bigString;
    }


    @Override
    public void setMaxWidth(int width) {
        maxWidth = width;
    }

    @Override
    public void setMaxHeight(int height) {
        maxHeight = height;
    }

    @Override
    public void setMaxRatio(double maxRatio) {
        this.maxRatio = maxRatio;
    }

    @Override
    public void setTextColorSchema(TextColorSchema schema) {

    }

    static class MyFork extends RecursiveTask<String> implements TextColorSchema {
        WritableRaster bwRaster;
        int from;
        int newWidth;
        int to;

        char[][] rotatedColors;
        public MyFork(int from, int newWidth, int newHeight, WritableRaster bwRaster) {
            this.bwRaster = bwRaster;
            this.from = from;
            this.to = newHeight;
            this.newWidth = newWidth;
        }

        @Override
        protected String compute() {
            if(to - from <= newWidth/numOfThreads) {

                char[][] rotatedColors = new char[newWidth][to];
                for (int w = 0; w < newWidth; w++) {
                    for (int h = from; h < to; h++) {
                        int colour = bwRaster.getPixel(w, h, new int[3])[0];
                        rotatedColors[w][h] = convert(colour);
                    }
                }

                StringBuilder bigString = new StringBuilder();
                for (int h = from; h < to; h++) {
                    for (int w = 0; w < newWidth; w++) {
                        //а это как в одну строчку?
                        bigString.append((rotatedColors[w][h]));
                        bigString.append((rotatedColors[w][h]));
                    }
                    bigString.append("\n");
                }
                return bigString.toString();

            } else {
                int middle = (to + from)/2;
                MyFork firstHalf = new MyFork(from, newWidth, middle, bwRaster);
                MyFork secondHalf = new MyFork(middle, newWidth, to, bwRaster);

                firstHalf.fork();
                secondHalf.fork();

                return firstHalf.join() + secondHalf.join();
            }
        }

        @Override
        public char convert(int color) {
            //Как в одну строчку?
            if (color <= 30) {
                return '@';
            } else if (color <= 60) {
                return '$';
            } else if (color <= 90) {
                return '%';
            } else if (color <= 120) {
                return '#';
            } else if (color <= 190) {
                return '+';
            } else if (color <= 220) {
                return '*';
            } else if (color <= 240) {
                return '-';
            } else {
                return '.';
            }
        }
    }
}