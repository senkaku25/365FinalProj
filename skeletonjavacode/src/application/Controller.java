package application;

import javax.sound.sampled.LineUnavailableException;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.utils.Converters;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import utilities.Utilities;

import javax.swing.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.*;
import java.io.*;

public class Controller extends JPanel{
	
	@FXML
	private ImageView imageView; // the image display window in the GUI (left)
	
	@FXML
	private ImageView hisView; // the image display window in the GUI (right)
	
	@FXML
	private Slider slider;
	

	@FXML
	private Text imageTitle;
	
	
	private Mat image;
	private double[][][] stiRows; //colsxframesxrgb sti
	private double[][][] stiCols; //rowsxframesxrgb sti
	private double [][][][] chromaFrames; //framesxrowsxcolsxrgb of a video
	
	private int bins;
	private int[][] stiRowsChromHistogram;
	private int[][] stiColsChromHistogram;
	ArrayList<ArrayList<Double>> rowI;
	ArrayList<ArrayList<Double>> colI;
	
	private XYChart.Series<String, Number> series;
	
	private int numberOfFrames=0;
	private int cropWidth = 32;
	private int cropHeight = 32;
	private int totalFrames=0;
	private int sampleRate; // sampling frequency
	private int sampleSizeInBits;
	private int numberOfChannels;
	private double[] freq; // frequencies for each particular row
	private int numberOfQuantizionLevels;
	private int numberOfSamplesPerColumn;
	private boolean isVideo = false;
	String filename;
	int click_counter = 0;
	double frameSubTime = 30.0;
	double totalFrameCount = 0;
	
	private VideoCapture capture;
	private ScheduledExecutorService timer;
	
	   JButton go;
	   String sourceFolder="";
	   String theFile="";
	   JFileChooser chooser;
	   String choosertitle;
	
	@FXML
	private void initialize() {
		// Optional: You should modify the logic so that the user can change these values
		// You may also do some experiments with different values

		sampleRate = 8000;
		sampleSizeInBits = 8;
		numberOfChannels = 1;
		numberOfFrames = 0;
		
		numberOfQuantizionLevels = 16;
		
		numberOfSamplesPerColumn = 500;
		
		series = new XYChart.Series<>();

	}
	

	//grabs every frame and creates STI
	protected void createStiFromVideo() throws InterruptedException {
		 if (capture != null && capture.isOpened()) { // the video must be open
		 double framePerSecond = capture.get(Videoio.CAP_PROP_FPS);
		 slider.setMinorTickCount(1);
		 slider.setSnapToTicks(true);
		 slider.setMajorTickUnit(frameSubTime);
		 slider.setShowTickMarks(true);
		 slider.setShowTickLabels(true);
	
		 totalFrames = (int)capture.get(Videoio.CAP_PROP_FRAME_COUNT);
		 stiCols = new double[32][totalFrames][3];
		 stiRows = new double[32][totalFrames][3];
		 chromaFrames = new double[totalFrames][32][32][2];
		 rowI = new ArrayList<ArrayList<Double>>();
		 colI = new ArrayList<ArrayList<Double>>();
//		 bins = (int)Math.floor(1+log2(cropWidth));
//		 stiRowsChromHistogram = new int[bins][bins];//same size as stirows
		 // create a runnable to fetch new frames periodically
		Runnable frameGrabber = new Runnable() {
		 @Override
		 public void run() { 
			 Mat frame = new Mat();
			 if (capture.read(frame)) { // decode successfully				 

				 totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
				 image = frame;
				 double currentFrameNumber = capture.get(Videoio.CAP_PROP_POS_FRAMES);
				 slider.setValue(currentFrameNumber / totalFrameCount * (slider.getMax() - slider.getMin()));
				 updateSti(); //update sti images
				 createChromaticityFrame(); //create a chroma data version of the frame
				
				numberOfFrames++;
				 
			 } else { // reach the end of the video
				 capture.release();
				 capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
				 capture = null;
				 
				 //display the sti im
				 javafx.scene.image.Image im = Utilities.doubleArray2Image(stiRows);
				 Utilities.onFXThread(imageView.imageProperty(), im);
			
				 //display the sti histogram
				 chromaHistogramIntersection();
				 
				 timer.shutdown();
			 }
			 }
		 };
		 // terminate the timer if it is running
		 if (timer != null && !timer.isShutdown()) {
			 timer.shutdown();
			 timer.awaitTermination(Math.round(1000/framePerSecond), TimeUnit.MILLISECONDS);
		 }
		 // run the frame grabber
		 timer = Executors.newSingleThreadScheduledExecutor();
		 timer.scheduleAtFixedRate(frameGrabber, 0, Math.round(1000/framePerSecond), TimeUnit.MILLISECONDS);
		 }
	}
	

	public static int log2(int x)
	{
	    return (int) (Math.log(x) / Math.log(2));
	}
    
	// This method should return the filename of the image to be played
	// You should insert your code here to allow user to select the file
	private String getImageFileDirectory() {
	     chooser = new JFileChooser(); 
	     chooser.setCurrentDirectory(new java.io.File("."));
	     chooser.setDialogTitle(choosertitle);
	     FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        "JPG, PNG, GIF, and MP4", "jpg", "gif", "png", "mp4");
	     chooser.setFileFilter(filter);
	     chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	    
	    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) { 
	      
	         String dirr = "" + chooser.getCurrentDirectory();
	         File file = chooser.getSelectedFile();
	       
	      if(dirr.substring(dirr.length()-1,dirr.length()).equals(".")){
	           dirr = dirr.substring(0,dirr.length()-1);
	           sourceFolder=""+dirr + "" + file.getName();
	        }else{
	            
	            sourceFolder=""+dirr + "/" + file.getName();
	        }
	          System.out.println("Folder path: " + dirr + " | File Name: " + file.getName());
	          System.out.println(sourceFolder);
	 			//ExamineImage.lum(sourceFolder);
	          return sourceFolder;
	    
	      } else {
	  		return "resources/test.png";
	      }

	}
	
	
	@FXML
	protected void openImage(ActionEvent event) throws InterruptedException {
		// This method opens an image and display it using the GUI
		// You should modify the logic so that it opens and displays a video
		
		filename = getImageFileDirectory();
		File f = new File(filename);
		imageTitle.setText("Title: " + f.getName());

		slider.setMinorTickCount(1);
		slider.setSnapToTicks(true);
		slider.setMajorTickUnit(frameSubTime);
		slider.setShowTickMarks(true);
		slider.setShowTickLabels(true);
		
		if(filename.contains(".mp4")) {
			isVideo=true;
			image=null;
			if(capture != null) {
				capture.release();
			}
			capture = new VideoCapture(filename); // open video file
			 if (capture.isOpened()) { // open successfully
				 Mat frame = new Mat();
				 if (capture.read(frame)) { // decode successfully
					 javafx.scene.image.Image im = Utilities.mat2Image(frame);
					 Utilities.onFXThread(imageView.imageProperty(), im);
					 totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
					 slider.setMax(totalFrameCount);
				 }
			 }		
		} else { 
			isVideo=false;
			image=null;
	
		}
	}
	
	//creates a rowsxframes sti image
	//creates a columnsxframes sti image
	protected void updateSti(){
		 //resize frame
		 Mat resizedImage = new Mat();
		 Imgproc.resize(image, resizedImage, new Size(cropWidth, cropHeight));
		 
			javafx.scene.image.Image im = Utilities.mat2Image(resizedImage);
			Utilities.onFXThread(imageView.imageProperty(), im);
	
		//write middle column as sti's column
		for(int i = 0; i < resizedImage.rows();i++) {
			stiCols[i][numberOfFrames] = resizedImage.get(i,(int) Math.floor(cropWidth/2));
		}
		
		//write middle row as sti's column, update chromaticity histogram
		for(int i = 0; i < resizedImage.cols();i++) {
			stiRows[i][numberOfFrames] = resizedImage.get((int) Math.floor(cropHeight/2),i);
			
		}
	}
	
	//takes the current frame and stores the chroma values for it
	//stores it in ChromaFrames
	protected void createChromaticityFrame() {
		 Mat resizedImage = new Mat();
		 Imgproc.resize(image, resizedImage, new Size(cropWidth, cropHeight)); //resize frame to 32 by 32
		for(int i = 0 ; i < 32; i++) {
			for(int j = 0 ; j < 32; j ++) {
				double red = resizedImage.get(i, j)[0];
				double green =  resizedImage.get(i, j)[1];
				double blue =  resizedImage.get(i, j)[2];
				int rgb = (int) (red + blue + green);
				
				//{r, g} = {R, G}/(R + G + B)
				chromaFrames[numberOfFrames][i][j][0] = (rgb == 0)? 0 : (red/rgb);
				chromaFrames[numberOfFrames][i][j][1] = (rgb == 0)? 0 : (green/rgb);
			}
		}
	}
	

	@FXML
	protected void playImage(ActionEvent event) throws LineUnavailableException {
		System.out.println("play button pressed");
		series.getData().clear(); // clear graph data if any
		if (isVideo) {
			try {
				createStiFromVideo();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			// do nothing.

		}
	}
	
	
	//creates histograms from video frame columns and compares them
	//then uses the histograms to determine a histogram intersection for rows and cols
	protected void chromaHistogramIntersection() {
		for(int r = 0; r < 32; r++) {//create rowI
			//array of the image row r's frame histograms
			ArrayList<double[][]> rowHistograms = new ArrayList<double[][]>();
			for(int j = 0; j<totalFrames;j++) {//make a histogram for every frame make a histogram for this row
				double[][] histogram = createHistogramFromFrameRow(chromaFrames[j], r);//take
				rowHistograms.add(histogram);
			}
			//calculate row intersection
			//I = (sum i) (sum j) min [Ht(i, j), Ht-1(i, j)]
			ArrayList<Double> rI = new ArrayList<Double>();
			double sum = 0.0;
			for(int i = 1 ; i < totalFrames;i++) {
				double[][] previous_frame = rowHistograms.get(i-1);
				double[][] current_frame = rowHistograms.get(i);
				sum+=histogramIntersection(previous_frame,current_frame);
				rI.add(sum);
			}
			rowI.add(rI);
		}
		
		for(int c = 0; c < 32; c++) {//create colI
			//array of the image row r's frame histograms
			ArrayList<double[][]> colHistograms = new ArrayList<double[][]>();
			for(int j = 0; j<totalFrames;j++) {//make a histogram for every frame make a histogram for this row
				double[][] histogram = createHistogramFromFrameCol(chromaFrames[j], c);//take
				colHistograms.add(histogram);
			}
			//calculate col intersection
			//I = (sum i) (sum j) min [Ht(i, j), Ht-1(i, j)]
			ArrayList<Double> cI = new ArrayList<Double>();
			double sum = 0.0;
			for(int i = 1; i < totalFrames;i++) {
				double[][] previous_frame = colHistograms.get(i-1);
				double[][] current_frame = colHistograms.get(i);
				sum+=histogramIntersection(previous_frame,current_frame);
				cI.add(sum);
			}
			colI.add(cI);
		}
	}
	
	//creates a r x g 2d chroma histogram from frame row.
	protected double[][] createHistogramFromFrameRow(double[][][] frame, int row) {
		bins = (int)Math.floor(1+ log2(frame.length));//quantization
		double sum = 0.0;
		double[][] histogram = new double[bins][bins];
		for(int i = 0 ; i < frame[0].length ; i++) {//traverse frame columns in the row
				double buckets = (1.0/(bins-1));
				int red_bin = (int) Math.round(frame[row][i][0]/buckets);
				int green_bin = (int) Math.round(frame[row][i][1]/buckets);
				histogram[red_bin][green_bin]++;
				sum++;
		}
		//"normalize histogram"
		for(int i=0 ; i<bins ; i++)
			for(int j=0 ; j<bins; j++)
				histogram[i][j]= histogram[i][j]/sum;
		return histogram;
	}
	
	//creates a r x g 2d chroma histogram from frame column.
	protected double[][] createHistogramFromFrameCol(double[][][] frame, int col) {
		bins = (int)Math.floor(1+ log2(frame.length));//quantization
		double sum = 0.0;
		double[][] histogram = new double[bins][bins];
		for(int i = 0 ; i < frame.length ; i++) {//traverse frame rows in the column
				double buckets = (1.0/(bins-1));
				int red_bin = (int) Math.round(frame[i][col][0]/buckets);
				int green_bin = (int) Math.round(frame[i][col][1]/buckets);
				histogram[red_bin][green_bin]++;
				sum++;
		}
		//"normalize histogram"
		for(int i=0 ; i<bins ; i++)
			for(int j=0 ; j<bins; j++)
				histogram[i][j]= histogram[i][j]/sum;
		return histogram;
	}
	
	//returns min [Ht(i, j), Ht-1(i, j)]
	protected double histogramIntersection(double[][] previous_frame,double[][] current_frame) {
			double sum = 0.0;
			for(int i = 0 ; i < bins ; i++) {
				for(int j = 0 ; j < bins ; j++) {
					sum+= Math.min(previous_frame[i][j], current_frame[i][j]);
				}
			}
			return sum;
	}
}
