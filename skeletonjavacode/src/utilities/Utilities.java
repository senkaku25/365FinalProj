package utilities;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Provide general purpose methods for handling OpenCV-JavaFX data conversion.
 * Moreover, expose some "low level" methods for matching few JavaFX behavior.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a>
 * @version 1.0 (2016-09-17)
 * @since 1.0
 * 
 */
public final class Utilities
{
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @return the {@link Image} to show
	 */
	public static Image mat2Image(Mat frame)
	{
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat obejct: " + e);
			return null;
		}
	}
	
	public static Image doubleArray2Image(double[][][] frame) {
		try {
			return SwingFXUtils.toFXImage(DoubleListToBufferedImage(frame),null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the double array: " + e);
			return null;			
		}
	}
	
	public static Mat histogram2DArray2Mat(ArrayList<ArrayList<Double>> histogram) {
		try {
			BufferedImage bi = Histogram2DToBufferedImage(histogram);
			byte[] pixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
			Mat m = new Mat(bi.getHeight(),bi.getWidth(), CvType.CV_8UC3);
			m.put(0, 0, pixels);
			return m;
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the histogram array to mat: " + e);
			return null;			
		}	
	}
	
	public static Mat doubleArray2Mat(double[][][] frame) {
		try {
			BufferedImage bi = DoubleListToBufferedImage(frame);
			byte[] pixels = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
			Mat m = new Mat(bi.getHeight(),bi.getWidth(), CvType.CV_8UC3);
			m.put(0, 0, pixels);
			return m;
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the double array to mat: " + e);
			return null;			
		}	
	}
	
	public static Image histogram2DArray2Image(ArrayList<ArrayList<Double>> histogram) {
		try {
			return SwingFXUtils.toFXImage(Histogram2DToBufferedImage(histogram),null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the double array: " + e);
			return null;			
		}
	}
	
	/**
	 * Generic method for putting element running on a non-JavaFX thread on the
	 * JavaFX thread, to properly update the UI
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 */
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}
	
	/**
	 * Support for the {@link mat2image()} method
	 * 
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @return the corresponding {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original)
	{
		// init
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
	
	private static BufferedImage DoubleListToBufferedImage(double[][][] original) {
		BufferedImage image = new BufferedImage(original[0].length, original.length, BufferedImage.TYPE_3BYTE_BGR);
		for(int x = 0; x< original.length;x++) {
			for(int y = 0; y<original[0].length;y++) {
				int r = (int)original[x][y][0]; 
				int g = (int)original[x][y][1]; 
				int b = (int)original[x][y][2];
				int rgb = (1 & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
				image.setRGB(y, x,rgb);
			}
		}
		return image;
	}
	
	private static BufferedImage Histogram2DToBufferedImage(ArrayList<ArrayList<Double>> histogram) {
		BufferedImage image = new BufferedImage(histogram.get(0).size(), histogram.size(), BufferedImage.TYPE_3BYTE_BGR);
		for(int row = 0; row< histogram.size();row++) {
			for(int col = 0; col<histogram.get(0).size();col++) {
				int grey = (int)Math.floor(histogram.get(row).get(col)*255);
				int r = grey;
				int g = grey;
				int b = grey;
				int rgb = (1 & 0xff) << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
				image.setRGB(col, row,rgb);
			}
		}
		return image;
	}
}