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

		// TODO: You may initialize your additional fields and apply some preliminary
		// calculations.

        // get the grayscale image
        BufferedImage greyImg = this.greyscale();

        // create the initial Edges matrix
        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int grayVal = c.getRed();



        });

		/* init a matrix (might need to make it as a dynamic matrix) to represent the gradient magnitude
		 aka the "edges" of the image.
		 remember: work on the grayscale image and use forward differencing
		*/



        ArrayList<ArrayList<Pixel>> edges = new ArrayList<>();

		// init some data structure to store all the k seams.

		// find the 𝑘 most minimal seams
		for (int i = 0; i < numOfSeams ; i++) {
			findMinimalSeam();
		}


		this.logger.log("preliminary calculations were ended.");
	}

	public BufferedImage resize() {
		return resizeOp.resize();
	}

	/*find a minimal seam using the edges matrix
	 and store it in the DS */
	private void findMinimalSeam() {
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
        int magnitude;

        public Pixel (int x, int y){
            this.x = x;
            this.y = y;
        }
    }
}
