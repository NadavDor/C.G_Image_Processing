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
			try {
				initEdgesMatrix();
			} catch (Exception e) {
				System.out.println("exeption here!");
			}
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
				Pixel pixel = new Pixel(x, y, greyScale[y][x]);
				pixel.magnitude = calcMagnitude(pixel);
				edges[y].add(pixel);
			}
		}
		this.logger.log("Finished initializind Edges Matrix");
	}

	private void initSeamsVars() {
		this.seamsMatrix = new boolean[inHeight][inWidth];
		this.lastSeam = new int[inHeight];
	}

	private int calcMagnitude(Pixel pixel) {
		int magnitude;
		if(pixel.y == inHeight -1 && pixel.x == inWidth -1 ) {
			magnitude = (int)(Math.abs(this.greyScale[pixel.y][pixel.x-1]- this.greyScale[pixel.y][pixel.x]) + (Math.abs(this.greyScale[pixel.y-1][pixel.x] - this.greyScale[pixel.y][pixel.x]));
		}
		else {
			if (pixel.x == inWidth - 1) {
				magnitude = (int)(Math.abs((this.greyScale[pixel.y][pixel.x - 1] - this.greyScale[pixel.y][pixel.x]) + (Math.abs(this.greyScale[pixel.y + 1][pixel.x] - this.greyScale[pixel.y][pixel.x]))));
			} else {
				if (pixel.y == inHeight - 1) {
					magnitude = (int)(Math.abs(this.greyScale[pixel.y][pixel.x + 1] - this.greyScale[pixel.y][pixel.x]) + (Math.abs(this.greyScale[pixel.y - 1][pixel.x] - this.greyScale[pixel.y][pixel.x])));
				} else {
					magnitude = (int)(Math.abs(this.greyScale[pixel.y][pixel.x + 1] - this.greyScale[pixel.y][pixel.x]) + (Math.abs(this.greyScale[pixel.y + 1][pixel.x] - this.greyScale[pixel.y][pixel.x])));
				}
			}
		}
		return magnitude;
	}


		private int updateMagnitude (Pixel pixel, int eX,int eY){
			int magnitude, xMinus1, yMinus1, xPlus1, yPlus1;
			if (eY == this.edges.length - 1 && eX ==this.edges[0].size() - 1) {
				xMinus1 = edges[eY].get(eX - 1).x;
				yMinus1 = edges[eY - 1].get(eX).y;
				magnitude = (int) Math.sqrt(Math.pow(this.greyScale[pixel.y][xMinus1] - this.greyScale[pixel.y][pixel.x], 2) + (Math.pow(this.greyScale[yMinus1][pixel.x] - this.greyScale[pixel.y][pixel.x], 2)));
			} else {
				if (eX == this.edges[0].size() - 1) {
					xMinus1 = edges[eY].get(eX - 1).x;
					yPlus1 = edges[eY + 1].get(eX).y;
					magnitude = (int) Math.sqrt(Math.pow(this.greyScale[pixel.y][xMinus1] - this.greyScale[pixel.y][pixel.x], 2) + (Math.pow(this.greyScale[yPlus1][xMinus1] - this.greyScale[pixel.y][pixel.x], 2)));
				} else {
					if (eY == this.edges.length - 1) {
						xPlus1 = edges[eY].get(eX + 1).x;
						yMinus1 = edges[eY - 1].get(eX).y;
						magnitude = (int) Math.sqrt((this.greyScale[pixel.y][xPlus1] - this.greyScale[pixel.y][pixel.x]) + (Math.pow(this.greyScale[yMinus1][pixel.x] - this.greyScale[pixel.y][pixel.x], 2)));
					} else {
						xPlus1 = edges[eY].get(eX + 1).x;
						yPlus1 = edges[eY + 1].get(eX).y;
						magnitude = (int) Math.sqrt(Math.pow(this.greyScale[pixel.y][xPlus1] - this.greyScale[pixel.y][pixel.x], 2) + (Math.pow(this.greyScale[yPlus1][pixel.x] - this.greyScale[pixel.y][pixel.x], 2)));
					}
				}
			}
			return magnitude;
		}

		private void findKSeams () {
			this.logger.log("Finding" + this.numOfSeams + "seams");
			for (int i = 0; i < numOfSeams; i++) {
				findMinimalSeam(i);
				try {
                    updateEdgeMatrix();
                }catch(Exception e){
                    System.out.println("exception here2");
                }
			}
			this.logger.log("Founded" + this.numOfSeams + "seams!");
		}


		private void updateEdgeMatrix () {
			this.logger.log("updating Edges Matrix");
			//remove seam from edges matrix
			for (int i = 0; i < lastSeam.length; i++) {
				this.edges[i].remove(lastSeam[i]);
				this.edges[i].subList(this.edges[i].size(), this.edges[i].size()).clear();
			}
			//TODO: perform the calculation ONLY to the pixels located besides the removed seam.
			//calc new magnitude for the new edges matrix
			for (int y = 0; y < lastSeam.length ; y++) {
				int x = lastSeam[y];

				Pixel pixToUpdate;
				// the pixel on the left side of the seam.
				if(x > 0){
					pixToUpdate = edges[y].get(x-1);
					pixToUpdate.magnitude = updateMagnitude2(pixToUpdate, x-1);
				}
				// the pixel on the right side of the seam.
				if (x < edges[0].size()-1){
					pixToUpdate = edges[y].get(x+1);
					pixToUpdate.magnitude = updateMagnitude2(pixToUpdate, x+1);
				}
			}
			this.logger.log("Finished updating Edges Matrix");
		}

		private int updateMagnitude2(Pixel pixel, int eX) {
			int magnitude;
			Pixel lPixel, rPixel, tPixel, bPixel;

			// bottom right pixel
			if (eX == edges[0].size()-1 && pixel.y == edges.length-1){
				lPixel = edges[pixel.y].get(eX - 1);
				tPixel = edges[pixel.y-1].get(eX);
				magnitude = (int)(Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[pixel.y][pixel.x])
										 + Math.abs(greyScale[tPixel.y][tPixel.x] - greyScale[pixel.y][pixel.x]));
			}
			// right side pixel
			else if (eX == edges[0].size()-1){
				lPixel = edges[pixel.y].get(eX - 1);
				bPixel = edges[pixel.y+1].get(eX);
				magnitude = (int)(Math.abs(greyScale[lPixel.y][lPixel.x] - greyScale[pixel.y][pixel.x])
										 + Math.abs(greyScale[bPixel.y][bPixel.x] - greyScale[pixel.y][pixel.x]));
			}
			// bottom pixel
			else if (pixel.y == edges.length-1){
				rPixel = edges[pixel.y].get(eX + 1);
				tPixel = edges[pixel.y-1].get(eX);
				magnitude = (int)(Math.abs(greyScale[rPixel.y][rPixel.x] - greyScale[pixel.y][pixel.x]) + Math.abs(greyScale[tPixel.y][tPixel.x] - greyScale[pixel.y][pixel.x]));
			}
			// any other pixel
			else {
				rPixel = edges[pixel.y].get(eX + 1);
				bPixel = edges[pixel.y+1].get(eX);
				magnitude = (int)(Math.abs(greyScale[rPixel.y][rPixel.x] - greyScale[pixel.y][pixel.x])
										 + (Math.abs(greyScale[bPixel.y][bPixel.x] - greyScale[pixel.y][pixel.x])));
			}
			return magnitude;
		}




	/*find a minimal seam using the edges matrix
	 and store it in the DS.
	 input: the current seam number.
	 */
		private void findMinimalSeam ( int seamNum){

			long[][] costMat = new long[inHeight][inWidth - seamNum];

			//fill the matrix
			for (int y = 0; y < costMat.length; y++) {
				for (int x = 0; x < costMat[0].length; x++) {
				//	this.logger.log("working on" +y +"," +x);
					costMat[y][x] = edges[y].get(x).getPixelEnergy();

					// fill the first row without considering cl, cv of cr.
					if (y == 0) continue;

					long cl, cv, cr;
					Pixel lPixel, tPixel ,rPixel;

					//left most pixel in the row
					if (x == 0) {

						rPixel = edges[y].get(x+1);
						tPixel = edges[y-1].get(x);

						cr = Math.abs(greyScale[rPixel.y][rPixel.x] - greyScale[tPixel.y][tPixel.x]);

						costMat[y][x] += Math.min(costMat[y - 1][x], costMat[y - 1][x + 1] + cr);
					}
					// right most pixel in the row
					else if (x == costMat[0].length - 1) {
						lPixel = edges[y].get(x-1);
						tPixel = edges[y-1].get(x);

						cl = (int) Math.sqrt(Math.pow(greyScale[lPixel.y][lPixel.x] - greyScale[tPixel.y][tPixel.x], 2));

						costMat[y][x] += Math.min(costMat[y - 1][x], costMat[y - 1][x - 1] + cl);
					} else {
						lPixel = edges[y].get(x-1);
						tPixel = edges[y-1].get(x);
						rPixel = edges[y].get(x+1);


						cl = (int) (Math.sqrt(Math.pow(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x], 2)) +
							    	Math.sqrt(Math.pow(greyScale[lPixel.y][lPixel.x] - greyScale[tPixel.y][tPixel.x], 2)));

						cv = (int)  Math.sqrt(Math.pow(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x], 2));

						cr = (int) (Math.sqrt(Math.pow(greyScale[lPixel.y][lPixel.x] - greyScale[rPixel.y][rPixel.x], 2)) +
									Math.sqrt(Math.pow(greyScale[rPixel.y][rPixel.x] - greyScale[tPixel.y][tPixel.x], 2)));

						costMat[y][x] += Math.min(costMat[y - 1][x - 1] + cl,
										 Math.min(costMat[y - 1][x] + cv,
												  costMat[y - 1][x + 1] + cr));
					}
				}
			}

        //trace back in the cost matrix to find the minimal seam,
		//and update the seams variables.
		int xIndex = 0;
        long minValue = Long.MAX_VALUE;
		for (int x = 0; x < costMat[0].length ; x++) {
			if (costMat[costMat.length-1][x] < minValue){
			    xIndex = x;
                minValue = costMat[costMat.length-1][x];
            }
		}

		this.lastSeam[lastSeam.length-1] = xIndex;
		this.seamsMatrix[lastSeam.length-1][xIndex] = true;

			int nextXIndex;
			for (int y = costMat.length - 1; y > 0; y--) {
				//left most pixel in the row
				if (xIndex == 0) {
					nextXIndex = costMat[y - 1][xIndex] < costMat[y - 1][xIndex + 1] ? xIndex : xIndex + 1;
				}
				// right most pixel in the row
				else if (xIndex == costMat[0].length - 1) {
					nextXIndex = costMat[y - 1][xIndex] < costMat[y - 1][xIndex - 1] ? xIndex : xIndex - 1;
				} else {
					nextXIndex = costMat[y - 1][xIndex] < costMat[y - 1][xIndex + 1] ? xIndex : xIndex + 1;
					nextXIndex = costMat[y - 1][nextXIndex] < costMat[y - 1][xIndex - 1] ? nextXIndex : xIndex - 1;
				}

			this.lastSeam[y-1] = nextXIndex;
			this.seamsMatrix[y-1][nextXIndex] = true;
			xIndex = nextXIndex;
		}
	}

		public BufferedImage resize () {
			return resizeOp.resize();
		}

		// delete all the seams found in the DS from the original image.
		private BufferedImage reduceImageWidth () {
			// TODO: Implement this method, remove the exception.
			logger.log("Preparing for reducingImageWidth");
			BufferedImage ans = newEmptyInputSizedImage();

			forEach((y, x) -> {

				if (seamsMatrix[y][x]){
					Color c = new Color(255, 0, 0);
					ans.setRGB(x, y, c.getRGB());
				} else {
					Color c = new Color(workingImage.getRGB(x, y));
					ans.setRGB(x, y, c.getRGB());
				}
			});

			logger.log("reducingImageWidth done!");
			return ans;
			//throw new UnimplementedMethodException("reduceImageWidth");
		}

		// duplicate each of the seams found in the DS from the original image.
		private BufferedImage increaseImageWidth () {
			// TODO: Implement this method, remove the exception.
			throw new UnimplementedMethodException("increaseImageWidth");
		}

		public BufferedImage showSeams ( int seamColorRGB){
			// TODO: Implement this method (bonus), remove the exception.
			throw new UnimplementedMethodException("showSeams");
		}

		public boolean[][] getMaskAfterSeamCarving () {
			// TODO: Implement this method, remove the exception.
			// This method should return the mask of the resize image after seam carving.
			// Meaning, after applying Seam Carving on the input image,
			// getMaskAfterSeamCarving() will return a mask, with the same dimensions as the
			// resized image, where the mask values match the original mask values for the
			// corresponding pixels.
			// HINT: Once you remove (replicate) the chosen seams from the input image, you
			// need to also remove (replicate) the matching entries from the mask as well.

			forEach((y, x) -> {
				if (seamsMatrix[y][x]) imageMask[y][x] = false;
			});

			return imageMask;
			//throw new UnimplementedMethodException("getMaskAfterSeamCarving");
		}

		class Pixel {
			int x;
			int y;
			int greyColor;
			int magnitude;

			public Pixel(int x, int y, int greyColor) {
				this.x = x;
				this.y = y;
				this.greyColor = greyColor;
			}

			/**
			 * get the pixels energy considering his magnitude and mask value.
			 * NOTE: we assume that imageMask his a global variable in the scope.
			 */
			public long getPixelEnergy() {
				return imageMask[this.y][this.x] ? (long) this.magnitude + (long) Integer.MIN_VALUE : this.magnitude;
			}
		}
	}


