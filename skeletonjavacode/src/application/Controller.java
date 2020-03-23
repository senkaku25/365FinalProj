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
	private ImageView imageView; // the image display window in the GUI
	
	@FXML
	private Slider slider;
	

	@FXML
	private Text imageTitle;
	
	
	private Mat image;
	private double[][][] stiRows; //colsxframesxrgb sti
	private double[][][] stiCols; //rowsxframesxrgb sti
	
	private int bins;
	private int[][] stiRowsChromHistogram;
	
	private XYChart.Series<String, Number> series;
	
	private int numberOfFrames=0;
	private int width;
	private int height;
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
		width = 32;
		height = 32;
		sampleRate = 8000;
		sampleSizeInBits = 8;
		numberOfChannels = 1;
		numberOfFrames = 0;
		
		numberOfQuantizionLevels = 16;
		
		numberOfSamplesPerColumn = 500;
		
		series = new XYChart.Series<>();
		
		// assign frequencies for each particular row
		freq = new double[height]; // Be sure you understand why it is height rather than width
		freq[height/2-1] = 440.0; // 440KHz - Sound of A (La)
		for (int m = height/2; m < height; m++) {
			freq[m] = freq[m-1] * Math.pow(2, 1.0/12.0); 
		}
		for (int m = height/2-2; m >=0; m--) {
			freq[m] = freq[m+1] * Math.pow(2, -1.0/12.0); 
		}

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
	
		 stiCols = new double[width][(int)capture.get(Videoio.CAP_PROP_FRAME_COUNT)][3];
		 stiRows = new double[height][(int)capture.get(Videoio.CAP_PROP_FRAME_COUNT)][3];
		 
		 bins = (int)Math.floor(1+log2(height));
		 stiRowsChromHistogram = new int[bins][bins];//same size as stirows
		 // create a runnable to fetch new frames periodically
		Runnable frameGrabber = new Runnable() {
		 @Override
		 public void run() { 
			 Mat frame = new Mat();
			 if (capture.read(frame)) { // decode successfully				 
				javafx.scene.image.Image im = Utilities.mat2Image(frame);
				Utilities.onFXThread(imageView.imageProperty(), im);

				 totalFrameCount = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
				 image = frame;
				 double currentFrameNumber = capture.get(Videoio.CAP_PROP_POS_FRAMES);
				 slider.setValue(currentFrameNumber / totalFrameCount * (slider.getMax() - slider.getMin()));
				 updateSti();
				numberOfFrames++;
				 
			 } else { // reach the end of the video
				 capture.release();
				 capture.set(Videoio.CAP_PROP_POS_FRAMES, 0);
				 capture = null;
				 System.out.println(stiRowsChromHistogram);
				 javafx.scene.image.Image im = Utilities.doubleArray2Image(stiRows);
				 Utilities.onFXThread(imageView.imageProperty(), im);
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
		 Imgproc.resize(image, resizedImage, new Size(width, height));
	
		//write middle column as sti's column
		for(int i = 0; i < resizedImage.rows();i++) {
			double[] pixel = resizedImage.get(i,(int) Math.floor(width/2));
			stiCols[i][numberOfFrames] = pixel;
			
		}
		
		//write middle row as sti's column, update chromaticity histogram
		for(int i = 0; i < resizedImage.cols();i++) {
			double[] pixel = resizedImage.get((int) Math.floor(height/2),i);
			stiRows[i][numberOfFrames] = pixel;
			int [] chroma = getChromaticity(pixel);
			stiRowsChromHistogram[chroma[0]][chroma[1]]+=1;
			
		}
	}
	
	//returns quantisized r and g chroma values
	protected int[] getChromaticity(double[] pixel){
		if(pixel[0]==0.0 && pixel[1]==0.0 && pixel[2]==0.0) { //black pixel
			int [] chromaticity = {0,0};
			return chromaticity;
		} else {
			double rChromaticity = pixel[0]/(pixel[0]+pixel[1]+pixel[2]);
			double gChromaticity = pixel[1]/(pixel[0]+pixel[1]+pixel[2]);
			
			double lvlSize = (1.0/bins);
			int rQuantisized = (int) Math.round(rChromaticity/lvlSize);
			int gQuantisized = (int) Math.round(gChromaticity/lvlSize);
			
			int [] chromaticity = {rQuantisized,gQuantisized};
			return chromaticity;
		}
	}

	//put in the histogram code here for the frame
	protected void playSound() throws LineUnavailableException{
		Mat grayImage = new Mat();
		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
		
		// resize the image
		Mat resizedImage = new Mat();
		Imgproc.resize(grayImage, resizedImage, new Size(width, height));
		
        ObservableList<XYChart.Data<String, Number>> data = FXCollections.<XYChart.Data<String, Number>>observableArrayList();

    	series.getData().addAll(data);
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
}
