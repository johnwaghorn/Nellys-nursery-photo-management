package Core;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import Data.Picture;
import GUI.MainFrame;
import GUI.PictureLabel;
import GUI.PicturesFrame;

import java.text.SimpleDateFormat;

public class Library implements Serializable {

	public static BufferedImage DELETE_BUTTON;
    private static ThumbnailProcessor PROCESSOR = new ThumbnailProcessor();

    private static ArrayList<Thread> RUNNING_THREADS = new ArrayList<Thread>();
    private static ArrayList<Picture> PICTURE_LIBRARY = new ArrayList<Picture>();
    private static Map<File, ArrayList<Picture>> directoryPictureMap = new HashMap<File, ArrayList<Picture>>();
    private static ArrayList<Picture> LAST_VISITED_VIRTUAL_DIR = new ArrayList<Picture>();
    private static ArrayList<Taggable> taggableComponents = new ArrayList<Taggable>();
    private static ArrayList<Taggable> areaList = new ArrayList<Taggable>();
    private static final Object[] nurserySites = {"Dulwich", "Lancaster", "Rosendale", "Turney"};

	public static synchronized ArrayList<Taggable> getTaggableComponentsList() {
		return taggableComponents;
	}

	public static Vector<String> getTaggableComponentNamesVector(
			boolean forSearchField) {
		Vector<String> taggableComponentNames = new Vector<String>();
		if (forSearchField) {
			taggableComponentNames.add("View All");
		}
		for (Taggable t : taggableComponents) {
			taggableComponentNames.add(t.getName());
		}
		return taggableComponentNames;
	}

	public static ArrayList<Taggable> getAreaList() {
		return areaList;
	}

	public static Object[] getNurserySites() {
		return nurserySites;
	}

	public static synchronized ArrayList<Picture> getPictureLibrary() {
		return PICTURE_LIBRARY;
	}

	public static Map<File, ArrayList<Picture>> getDirectoryPictureMap() {
		return directoryPictureMap;
	}

	public static ThumbnailProcessor getThumbnailProcessor() {
		return PROCESSOR;
	}

	public static void setDirectoryPictureMap(Map<File, ArrayList<Picture>> newMap) {
		directoryPictureMap = newMap;
	}

	public static ArrayList<Thread> getRunningThreads() {
		return RUNNING_THREADS;
	}

	public static void addRunningThread(Thread t) {
		if (!RUNNING_THREADS.contains(t)) {
			RUNNING_THREADS.add(t);
		}
	}

	public static void removeRunningThread(Thread t) {
		RUNNING_THREADS.remove(t);
	}

    public static ArrayList<Picture> getLastVisitedVirtualDir() {
        return LAST_VISITED_VIRTUAL_DIR;
    }

    public static void setLastVisitedVirtualDir(ArrayList<Picture> newLastVirtualDir) {
        LAST_VISITED_VIRTUAL_DIR = newLastVirtualDir;
    }

    /**
     *
     *
     * @param importedPictures
     *            ArrayList of all pictures being imported in this batch
     */
    public static void importPicture(final ArrayList<Picture> importedPictures) {

        Settings.IMPORT_IN_PROGRESS = true;

        final PicturesFrame picturesPanel = MainFrame.getMainFrames().get(0)
                .getPicturesPanel();
        picturesPanel.revalidate();

        Thread thumbnailImport = new Thread() {


            public void run() {
                Library.addRunningThread(this);
                ArrayList<PictureLabel> thumbnailsForImport = new ArrayList<PictureLabel>();
                try {
                    if (importedPictures.size() > 0) {
                        for (Picture p : importedPictures) {
                            if (isInterrupted()) {
                                break;
                            }
                            thumbnailsForImport.add(p.getPictureLabel());
                        }
                        new ThumbnailImportThread(thumbnailsForImport)
                                .start();

                    }
                } finally {
                    System.gc();
                    Library.removeRunningThread(this);

                    picturesPanel.createThumbnailArray();
                    Settings.IMPORT_IN_PROGRESS = false;
                }
            }
        };
        thumbnailImport.start();
    }

	/**
	 * Recursive method looking at all files inside a folder. Any folders found
	 * call the method on themselves. All files found are added to a list. After
	 * all folders are opened the list of all files is sent to importPicture().
	 *
	 * @param importDirectory
	 *            root directory to be imported
	 */
	public static void importFolder(final File importDirectory) {
		// TODO for every picture in folder do importPicture()
		ArrayList<File> nestedPictures = new ArrayList<File>();
		File[] nestedItems = importDirectory.listFiles();

		for (File file : nestedItems) {
			if (file.isFile()
					&& (FilenameUtils.getExtension(file.getPath())
							.equalsIgnoreCase("jpg") || FilenameUtils
							.getExtension(file.getPath()).equalsIgnoreCase(
									"jpeg"))) {
				nestedPictures.add(file);
			} else if (file.isDirectory()) {
				importFolder(file);
			}
		}
		File[] toProcess = new File[0];
		if (nestedPictures.size() > 0) {
			new PictureImportThread(nestedPictures.toArray(toProcess)).start();
		}

	}

	public static void addTaggableComponent(Taggable t) {
		taggableComponents.add(t);
		if (t.getType() == Settings.AREA_TAG) {
			areaList.add(t);
		}
	}

	public static synchronized void addPictureToLibrary(Picture picture) {
        boolean exists = false;
        for (Picture p : PICTURE_LIBRARY) {
            if (p.getImagePath().equals(picture.getImagePath())) {
                exists = true;
                break;
            }
        }
        if (!exists)
            PICTURE_LIBRARY.add(picture);
    }

	public static void deletePictureLibrary() {
		PICTURE_LIBRARY = null;
	}

	public static void deleteTaggableComponentsList() {
		taggableComponents = null;
	}

	public static String getFormattedDate(Date date) {
        if (date != null) {
            String d = "" + date.getDate();
            if (d.length() == 1)
                d = "0" + d;
            String m = "" + (date.getMonth() + 1);
            if (m.length() == 1)
                m = "0" + m;
            String y = "" + (date.getYear() + 1900);
            return (new SimpleDateFormat("EE").format(date))+ " " + d + "/" + m + "/" + y;
        }
        return "";
	}

	public static void promptSelectSite(JFrame frame) {
		String selectedSite = null;
		while (selectedSite == null) {
			selectedSite = (String) JOptionPane.showInputDialog(frame,
					"In which nursery site is this computer?", "Select Site",
					JOptionPane.PLAIN_MESSAGE, null, Library.getNurserySites(),
					"Rosendale");

			if ((selectedSite != null) && (selectedSite.length() > 0)) {
				Settings.NURSERY_LOCATION = selectedSite;
				break;
			}
		}
	}

	public static void promptSelectCSV(JFrame frame) {
		final JFileChooser csvFileChooser = new JFileChooser();
		csvFileChooser.setDialogTitle("Select children list CSV file");
		csvFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
				"CSV File", "csv"));
        csvFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(
                "TXT File", "txt"));
		csvFileChooser.setAcceptAllFileFilterUsed(false);
		int wasFileSelected = csvFileChooser.showOpenDialog(frame);

		if (wasFileSelected == JFileChooser.APPROVE_OPTION) {
			Settings.CSV_PATH = csvFileChooser.getSelectedFile().getPath();
		} else {
			JOptionPane.showMessageDialog(frame,
					"Without importing a CSV file, it is not possible to tag pictures.\n"
							+ "You can import a CSV from the file menu.");
		}
	}

	public static void promptSelectHomeDir() {
		new JFXPanel();
		final CountDownLatch latch = new CountDownLatch(1);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				DirectoryChooser directoryChooser = new DirectoryChooser();
				directoryChooser
						.setTitle("Select root directory of all pictures");
				Settings.PICTURE_HOME_DIR = directoryChooser
						.showDialog(new Stage());
				latch.countDown();
			}
		});
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
