package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class SeamsCarver extends ImageProcessor {

    // MARK: An inner interface for functional programming.
    @FunctionalInterface
    interface ResizeOperation {
        BufferedImage resize();
    }

    // MARK: Fields
    private int numOfSeams;
    private ResizeOperation resizeOp;
    boolean[][] imageMask;
    ArrayList<Pixel>[] edges;
    long[][] costMat;
    int[][] greyScale;
    boolean[][] seamsMatrix;

    // the last seam that was found
    int[] lastSeam;

    public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
                       boolean[][] imageMask) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight());

        numOfSeams = Math.abs(outWidth - inWidth);
        this.imageMask = imageMask;
        if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException("Can not apply seam carving: workingImage is too small");

        if (numOfSeams > inWidth / 2)
            throw new RuntimeException("Can not apply seam carving: too many seams...");

        // Setting resizeOp by with the appropriate method reference
        if (outWidth > inWidth)
            resizeOp = this::increaseImageWidth;
        else if (outWidth < inWidth)
            resizeOp = this::reduceImageWidth;
        else
            resizeOp = this::duplicateWorkingImage;

        if (numOfSeams > 0) {
            initGreyscaleMatrix();
        /* init a matrix (might need to make it as a dynamic matrix) to represent the gradient magnitude
		 aka the "edges" of the image.
		 remember: work on the grayscale image and use forward differencing
		*/
            initEdgesMatrix();
            // init some data structure to store all the k seams.
            initSeamsVars();
            // find the 𝑘 most minimal seams
            findKSeams();
        }
        this.logger.log("preliminary calculations were ended.");
    }

    private void initGreyscaleMatrix() {
        // get the grayscale image
        BufferedImage greyImg = this.greyscale();
        this.greyScale = new int[inHeight][inWidth];

        forEach((y, x) -> {
            Color c = new Color(greyImg.getRGB(x, y));
            this.greyScale[y][x] = c.getRed();
        });
    }

    private void initEdgesMatrix() {
        this.logger.log("initializing Edges Matrix");
        edges = new ArrayList[inHeight];

        // create the initial Edges matrix
        for (int y = 0; y < inHeight; y++) {
            edges[y] = new ArrayList<>();
            for (int x = 0; x < inWidth; x++) {
                Pixel pixel = new Pixel(x, y);
                edges[y].add(pixel);
            }
        }

        // calc energy for all the pixels
        for (int y = 0; y < inHeight; y++) {
            for (int x = 0; x < inWidth; x++) {
                edges[y].get(x).calcPixelEnergy(x);
            }
        }
        this.logger.log("Finished initializind Edges Matrix");
    }

    private void initSeamsVars() {
        this.seamsMatrix = new boolean[inHeight][inWidth];
        this.lastSeam = new int[inHeight];
    }


    private void findKSeams() {
        this.logger.log("Finding" + this.numOfSeams + "seams");
        for (int i = 0; i < numOfSeams; i++) {
            findMinimalSeam(i);
            updateEdgeMatrix();
        }
        this.logger.log("Founded" + this.numOfSeams + "seams!");
    }


    private void updateEdgeMatrix() {
        this.logger.log("updating Edges Matrix");
        //remove seam from edges matrix
        for (int i = 0; i < lastSeam.length; i++) {
            this.edges[i].remove(lastSeam[i]);
            this.edges[i].subList(this.edges[i].size(), this.edges[i].size()).clear();
        }
        //calc new magnitude for the new edges matrix
        for (int y = 0; y < lastSeam.length; y++) {
            int x = lastSeam[y];

            // the pixel on the left side of the seam.
            if (x > 0) {
                edges[y].get(x - 1).calcPixelEnergy(x - 1);
            }
            // the pixel on the right side of the seam.
            //NOTE: as we deceased the width of edges by 1 the pixel
            // to the right of the seam in on  and not x+1
            if (x < edges[0].size() - 1) {
                edges[y].get(x).calcPixelEnergy(x);
            }
        }
        this.logger.log("Finished updating Edges Matrix");
    }

    /*find a minimal seam using the edges matrix
     and store it in the DS.
     input: the current seam number.
     */
    private void findMinimalSeam(int seamNum) {

        costMat = new long[inHeight][inWidth - seamNum];

        //fill the matrix
        for (int y = 0; y < costMat.length; y++) {
            for (int x = 0; x < costMat[0].length; x++) {
                //	this.logger.log("working on" +y +"," +x);
                costMat[y][x] = edges[y].get(x).getPixelEnergy();

                // fill the first row without considering cl, cv of cr.
                if (y == 0) continue;

                long cl, cv, cr;
                Pixel lPixel, tPixel, rPixel;

                //left most pixel in the row
                if (x == 0) {

                    rPixel = edges[y].get(x + 1);
                    tPixel = edges[y - 1].get(x);

                    cr = Math.abs(greyScale[rPixel.y][rPixel.x] - greyScale[tPixel.y][tPixel.x]);

                    costMat[y][x] += Math.min(costMat[y - 1][x], costMat[y - 1][x + 1] + cr);
                }
                // right most pixel in the row
                else if (x == costMat[0].length - 1) {
                    lPixel = edges[y].get(x - 1);
                    tPixel = edges[y - 1].get(x);

                    cl = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[tPixel.y][tPixel.x]);

                    costMat[y][x] += Math.min(costMat[y - 1][x], costMat[y - 1][x - 1] + cl);
                } else {
                    lPixel = edges[y].get(x - 1);
                    tPixel = edges[y - 1].get(x);
                    rPixel = edges[y].get(x + 1);


                    cl = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x]) +
                            Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[tPixel.y][tPixel.x]);

                    cv = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x]);

                    cr = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x]) +
                            Math.abs(greyScale[rPixel.y][rPixel.x] - greyScale[tPixel.y][tPixel.x]);

                    costMat[y][x] += Math.min(costMat[y - 1][x - 1] + cl,
                            Math.min(costMat[y - 1][x] + cv,
                                    costMat[y - 1][x + 1] + cr));
                }
            }
        }
        traceBack();
    }

    //trace back in the cost matrix to find the minimal seam,
    //and update the seams variables.
    private void traceBack() {

        int xIndex = 0;
        long minValue = Long.MAX_VALUE;
        for (int x = 0; x < costMat[0].length; x++) {
            if (costMat[costMat.length - 1][x] < minValue) {
                xIndex = x;
                minValue = costMat[costMat.length - 1][x];
            }
        }

        this.lastSeam[lastSeam.length - 1] = xIndex;
        this.seamsMatrix[lastSeam.length - 1][ edges[lastSeam.length - 1].get(xIndex).x ] = true;

        int nextXIndex = 0;
        Pixel curPixel, lPixel, tPixel, rPixel;
        for (int y = costMat.length - 1; y > 0; y--) {

            if (xIndex > 0 && xIndex < costMat[0].length - 1) {

                curPixel = edges[y].get(xIndex);
                lPixel = edges[y].get(xIndex - 1);
                tPixel = edges[y - 1].get(xIndex);
                rPixel = edges[y].get(xIndex + 1);

                long cl = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x]) +
                        Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[tPixel.y][tPixel.x]);

                long cv = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x]);

                long cr = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x]) +
                        Math.abs(greyScale[rPixel.y][rPixel.x] - greyScale[tPixel.y][tPixel.x]);


                //check the upper-left
                if (costMat[y][xIndex] == (curPixel.getPixelEnergy() + costMat[y - 1][xIndex - 1] + cl)) {
                    nextXIndex = xIndex - 1;
                }
                //check the top
                else if (costMat[y][xIndex] == (curPixel.getPixelEnergy() + costMat[y - 1][xIndex] + cv)) {
                    nextXIndex = xIndex;
                }
                // upper-right
                else if (costMat[y][xIndex] == (curPixel.getPixelEnergy() + costMat[y - 1][xIndex + 1] + cr)) {
                    nextXIndex = xIndex + 1;
                } else {
                    System.out.println("trace back could't find a way up");
                }
            } else if (xIndex == 0) {
                curPixel = edges[y].get(xIndex);
                tPixel = edges[y - 1].get(xIndex);
                rPixel = edges[y].get(xIndex + 1);

                long cr = Math.abs(greyScale[rPixel.y][rPixel.x] - greyScale[tPixel.y][tPixel.x]);

                // check the top
                if (costMat[y][xIndex] == (curPixel.getPixelEnergy() + costMat[y - 1][xIndex])) {
                    nextXIndex = xIndex;
                }
                // upper-right
                else if (costMat[y][xIndex] == (curPixel.getPixelEnergy() + costMat[y - 1][xIndex + 1] + cr)) {
                    nextXIndex = xIndex + 1;
                } else {
                    System.out.println("trace back could't find a way up");
                }
            } else {
                curPixel = edges[y].get(xIndex);
                lPixel = edges[y].get(xIndex - 1);
                tPixel = edges[y - 1].get(xIndex);

                long cl = Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[tPixel.y][tPixel.x]);

                // check the top
                if (costMat[y][xIndex] == (curPixel.getPixelEnergy() + costMat[y - 1][xIndex])) {
                    nextXIndex = xIndex;
                }
                // top-left
                else if (costMat[y][xIndex] == (curPixel.getPixelEnergy() + costMat[y - 1][xIndex - 1] + cl)) {
                    nextXIndex = xIndex - 1;
                } else {
                    System.out.println("trace back could't find a way up");
                }
            }

            this.lastSeam[y - 1] = nextXIndex;

            if(this.seamsMatrix[y - 1][ edges[y-1].get(nextXIndex).x ]){
                System.out.println("buggg");
            }else {
                this.seamsMatrix[y - 1][ edges[y-1].get(nextXIndex).x ] = true;
            }
            xIndex = nextXIndex;
        }
    }

    public BufferedImage resize() {
        return resizeOp.resize();
    }

    // delete all the seams found in the DS from the original image.
    private BufferedImage reduceImageWidth() {
        logger.log("Preparing for reducingImageWidth");
        BufferedImage ans = newEmptyOutputSizedImage();
        boolean[][] newImageMask = new boolean[outHeight][outWidth];

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int max = rgbWeights.maxWeight;

        int removedCount;
        for (int y = 0; y < inHeight ; y++) {
            removedCount = 0;
            for (int x = 0; x < inWidth ; x++) {
                if (!seamsMatrix[y][x]) {
                    Color c = new Color(workingImage.getRGB(x, y));
                    int red = r * c.getRed() / max;
                    int green = g * c.getGreen() / max;
                    int blue = b * c.getBlue() / max;
                    Color color = new Color(red, green, blue);

                    ans.setRGB(x - removedCount, y, color.getRGB());
                    newImageMask[y][x - removedCount] = imageMask[y][x];
                } else {
                    removedCount++;
                }
            }
        }

        this.imageMask = newImageMask;
        logger.log("reducingImageWidth done!");
        return ans;
    }

    // duplicate each of the seams found in the DS from the original image.
    private BufferedImage increaseImageWidth() {
        logger.log("Preparing for reducingImageWidth");
        BufferedImage ans = newEmptyOutputSizedImage();
        boolean[][] newImageMask = new boolean[outHeight][outWidth];

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int max = rgbWeights.maxWeight;

        int addedCount;
        for (int y = 0; y < inHeight ; y++) {
            addedCount = 0;
            for (int x = 0; x < inWidth ; x++) {
                if (!seamsMatrix[y][x]) {
                    Color c = new Color(workingImage.getRGB(x, y));
                    int red = r * c.getRed() / max;
                    int green = g * c.getGreen() / max;
                    int blue = b * c.getBlue() / max;
                    Color color = new Color(red, green, blue);


                    ans.setRGB(x + addedCount, y, color.getRGB());
                    newImageMask[y][x + addedCount] = imageMask[y][x];
                } else {
                    Color c = new Color(workingImage.getRGB(x, y));
                    int red = r * c.getRed() / max;
                    int green = g * c.getGreen() / max;
                    int blue = b * c.getBlue() / max;
                    Color color = new Color(red, green, blue);

                    ans.setRGB(x + addedCount, y, color.getRGB());
                    ans.setRGB(x + 1 + addedCount, y, color.getRGB());

                    newImageMask[y][x + addedCount] = imageMask[y][x];
                    newImageMask[y][x + 1 + addedCount] = imageMask[y][x];

                    addedCount++;

                    logger.log("x - removedCount = " + (x - addedCount) + "\n" +
                            "y = " + y );
                }
            }
        }

        this.imageMask = newImageMask;
        logger.log("reducingImageWidth done!");
        return ans;
    }

    public BufferedImage showSeams(int seamColorRGB) {
        logger.log("Preparing for showSeams");
        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            if (seamsMatrix[y][x]) {
                Color c = new Color(seamColorRGB);
                ans.setRGB(x, y, c.getRGB());
            } else {
                Color c = new Color(workingImage.getRGB(x, y));
                ans.setRGB(x, y, c.getRGB());
            }
        });

        logger.log("showSeams done!");
        return ans;
    }

    public boolean[][] getMaskAfterSeamCarving() {
        return this.imageMask;
    }

    class Pixel {
        int x;
        int y;
        int magnitude;

        public Pixel(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * get the pixels energy considering his magnitude and mask value.
         * NOTE: we assume that imageMask his a global variable in the scope.
         */
        public long getPixelEnergy() {
            return this.magnitude;
        }

        private void calcPixelEnergy(int eX) {
            this.magnitude = E1(eX) + E2(eX) + E3();
        }

        private int E1(int eX) {
            int ans;
            if (eX < edges[0].size() - 1) {
                Pixel rPixel = edges[this.y].get(eX + 1);
                ans = Math.abs(greyScale[this.y][this.x] - greyScale[rPixel.y][rPixel.x]);
            } else {
                Pixel lPixel = edges[this.y].get(eX - 1);
                ans = Math.abs(greyScale[this.y][this.x] - greyScale[lPixel.y][lPixel.x]);
            }
            return ans;
        }

        private int E2(int eX) {
            int ans;
            if (this.y < edges.length - 1) {
                Pixel bPixel = edges[this.y + 1].get(eX);
                ans = Math.abs(greyScale[this.y][this.x] - greyScale[bPixel.y][bPixel.x]);
            } else {
                Pixel tPixel = edges[this.y - 1].get(eX);
                ans = Math.abs(greyScale[this.y][this.x] - greyScale[tPixel.y][tPixel.x]);
            }
            return ans;
        }

        private int E3() {
            int ans;
            if (imageMask[this.y][this.x]) {
                ans = Integer.MIN_VALUE;
            } else {
                ans = 0;
            }
            return ans;
        }
    }
}
