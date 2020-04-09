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
	int[][] greyScale;
	boolean[][] seams;

	// TODO: Add some additional fields

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

		if(numOfSeams > 0) {
			initGreyscaleMatrix();
        /* init a matrix (might need to make it as a dynamic matrix) to represent the gradient magnitude
		 aka the "edges" of the image.
		 remember: work on the grayscale image and use forward differencing
		*/
			initEdgesMatrix();

			// init some data structure to store all the k seams.
			initSeamsMatrix();

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
		edges = new ArrayList[inHeight];

		// create the initial Edges matrix
		for (int y = 0; y < inHeight; y++) {
			edges[y] = new ArrayList<>();

			for (int x = 0; x < inWidth; x++) {
				Pixel pixel = new Pixel(x, y, greyScale[y][x]);
				pixel.magnitude = calcMagnitude(pixel);
				edges[y].add(pixel);
			}
		}
	}

	private void initSeamsMatrix() {
		this.seams = new boolean[inHeight][inWidth];
	}

	private double calcMagnitude(Pixel pixel) {
		double magnitude = Math.sqrt((this.greyScale[pixel.x + 1][pixel.y] - pixel.greyColor) + (this.greyScale[pixel.x][pixel.y +1] - pixel.greyColor));

		if(pixel.x == inWidth -1) {
			magnitude = Math.sqrt((this.greyScale[pixel.x-1][pixel.y] - pixel.greyColor) + (this.greyScale[pixel.x][pixel.y+1] - pixel.greyColor));
		}
		if(pixel.y == inHeight -1) {
			magnitude = Math.sqrt((this.greyScale[pixel.x + 1][pixel.y] - pixel.greyColor) + (this.greyScale[pixel.x][pixel.y -1] - pixel.greyColor));
		}
		if(pixel.y == inHeight -1 && pixel.x == inWidth -1 ) {
			magnitude = Math.sqrt((this.greyScale[pixel.x - 1][pixel.y] - pixel.greyColor) + (this.greyScale[pixel.x][pixel.y -1] - pixel.greyColor));
		}
		return magnitude;
	}

	private void findKSeams() {
		for (int i = 0; i < numOfSeams ; i++) {
			findMinimalSeam(i);
		}
	}

	/*find a minimal seam using the edges matrix
	 and store it in the DS.
	 input: the current seam number.
	 */
	private void findMinimalSeam(int seamNum) {

		double [][] costMat = new double[inHeight][inWidth - seamNum];

		// fill the first row
		for (int x = 0; x < costMat[0].length; x++) {
			costMat[0][x] = edges[0].get(x).magnitude;
		}

	}

	public BufferedImage resize() {
		return resizeOp.resize();
	}

	// delete all the seams found in the DS from the original image.
	private BufferedImage reduceImageWidth() {
		// TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("reduceImageWidth");
	}

	// duplicate each of the seams found in the DS from the original image.
	private BufferedImage increaseImageWidth() {
		// TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("increaseImageWidth");
	}

	public BufferedImage showSeams(int seamColorRGB) {
		// TODO: Implement this method (bonus), remove the exception.
		throw new UnimplementedMethodException("showSeams");
	}

	public boolean[][] getMaskAfterSeamCarving() {
		// TODO: Implement this method, remove the exception.
		// This method should return the mask of the resize image after seam carving.
		// Meaning, after applying Seam Carving on the input image,
		// getMaskAfterSeamCarving() will return a mask, with the same dimensions as the
		// resized image, where the mask values match the original mask values for the
		// corresponding pixels.
		// HINT: Once you remove (replicate) the chosen seams from the input image, you
		// need to also remove (replicate) the matching entries from the mask as well.
		throw new UnimplementedMethodException("getMaskAfterSeamCarving");
	}

    class Pixel {
        int x;
        int y;
        int greyColor;
        double magnitude;

        public Pixel (int x, int y, int greyColor){
            this.x = x;
            this.y = y;
            this.greyColor = greyColor;
        }

		/**
		 * get the pixels energy considering his magnitude and mask value.
		 */
		public long getPixelEnergy(){
			return 0;
		}
    }
}
