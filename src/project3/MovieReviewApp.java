package project3;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
    @author Stanley
    @author Dike
 */
    
public class MovieReviewApp {
    
    // create ReviewHandler object
    private static final ReviewHandler rh = new ReviewHandler();

    // Static log used in package
    static protected final Logger log = Logger.getLogger("SentimentAnalysis");

    // Components for GUI layout
    static private final JPanel topPanel = new JPanel();
    static private final JPanel bottomPanel = new JPanel();
    static private final JLabel commandLabel = new JLabel("Please choose the command.", JLabel.RIGHT);
    static private final JComboBox<String> comboBox = new JComboBox<>();
    static private final JButton databaseButton = new JButton("Show Database");
    static private final JButton saveButton = new JButton("Save Database");

    // Output area
    static protected JTextArea outputArea = new JTextArea();
    static private JScrollPane outputScrollPane = new JScrollPane(outputArea);

    // monitor width and height
    private static int width = Toolkit.getDefaultToolkit().getScreenSize().width;
    private static int height = Toolkit.getDefaultToolkit().getScreenSize().height;

    // window width and height
    private static int windowsWidth = 800;
    private static int windowsHeight = 800;

    public static void main(String [] args) {
        // set up the logger
        FileHandler fh;
        try {
            fh = new FileHandler("SentimentAnalysis.%u.%g.log");
            log.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        // Check if the correct number of arguments was provided
        // TODO edit the below?
        if (args.length < 2) {
            System.err.println("Please provide command liner arguments: <posFilePath> and <negFilePath>");
            return;
        }
        String pathToPosWords = args[0];
        String pathToNegWords = args[1];
        try {
            // Load the positive and negative words
            rh.loadPosNegWords(pathToPosWords, pathToNegWords);
        } catch (IOException ex) {
            System.err.println("Could not load positive and negative words. "
                    + "Please check that the file paths are correct and try again.");
            return;
        }
        // run the GUI
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
            File databaseFile = new File(ReviewHandler.DATA_FILE_NAME);
            if(databaseFile.exists()){
                try {
                    rh.loadDB();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    } // END main

    /**
     * Create a top panel to put in GUI pane, contains a combobox for main menu options,
     * a database button, and a save button with threading and logging where applicable
     */

    private static void createTopPanel() {
        comboBox.addItem("Please select...");
        comboBox.addItem(" 1. Load new movie review collection (given a folder or a file path).");
        comboBox.addItem(" 2. Delete movie review from database (given its id).");
        comboBox.addItem(" 3. Search movie reviews in database by id.");
        comboBox.addItem(" 4. Search movie reviews in database by substring.");
        comboBox.addItem(" 0. Exit program.");
        comboBox.setSelectedIndex(0);

        comboBox.addItemListener(e -> {
            log.info("Command chosen, Item - " + e.getItem());
            log.info("StateChange = " + e.getStateChange());
            if (e.getStateChange() == 1) {
                if (e.getItem().equals("Please select...")) {
                    outputArea.setText("");
                    outputArea.append(rh.getDatabase().size() + "records in database.\n");
                    outputArea.append("Please select a command to continue.\n");
                    topPanel.removeAll();
                    topPanel.add(commandLabel);
                    topPanel.add(comboBox);

                    topPanel.add(new JLabel());
                    topPanel.add(new JLabel());
                    topPanel.add(new JLabel());
                    topPanel.add(new JLabel());
                    topPanel.add(new JLabel());
                    topPanel.add(new JLabel());

                    topPanel.add(new JLabel());
                    topPanel.add(databaseButton);
                    topPanel.add(saveButton);
                    topPanel.updateUI();

                } else if (e.getItem().equals(" 1. Load new movie review collection (given a folder or a file path).")) {
                    loadReviews();
                } else if (e.getItem().equals(" 2. Delete movie review from database (given its id).")) {
                    deleteReviews();
                } else if (e.getItem().equals(" 3. Search movie reviews in database by id.")) {
                    searchReviewsId();
                } else if (e.getItem().equals(" 4. Search movie reviews in database by substring.")) {
                    searchReviewsSubstring();
                } else if (e.getItem().equals(" 0. Exit program.")) {
                    exit();
                }
            }
        });

        // Threaded databaseButton Listener
        databaseButton.addActionListener(e -> {
            log.info("database button clicked.");
            Runnable myRunnable = () -> printJTable(rh.searchBySubstring(""));
            Thread thread = new Thread(myRunnable);
            thread.start();
        });

        // Threaded saveButton listener
        saveButton.addActionListener(e -> {
            log.info("Save button clicked.");
            Runnable myRunnable = () -> {
                try {
                    rh.saveDB();
                } catch (IOException ex) {
                    log.setLevel(Level.WARNING);
                    log.warning("Save DB failed");
                    ex.printStackTrace();
                }
                outputArea.append("Database saved.\n");
            };
            Thread thread = new Thread(myRunnable);
            thread.start();
        });

        // Layout for Top panel and adding all buttons
        GridLayout topPanelGrid = new GridLayout(0, 2, 10, 10);
        topPanel.setLayout(topPanelGrid);
        topPanel.add(commandLabel);
        topPanel.add(comboBox);
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(databaseButton);
        topPanel.add(saveButton);
        topPanel.updateUI();

    } // END createTopPanel

    /**
     * CreateBottomPanel creates a scroll pane, and is where all previous command line output
     * has been directed
     */
    private static void createBottomPanel() {
        //set a font for text output in bottom panel
        final Font fontCourier = new Font("Courier", Font.PLAIN, 16);
        DefaultCaret caret = (DefaultCaret)outputArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        outputArea.setFont(fontCourier);

        outputArea.setText("Welcome to Sentiment analysis App.\n");
        outputArea.setEditable(false);
        // Make a border and scroll function
        final Border border = BorderFactory.createLineBorder(Color.BLACK);
        outputArea.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10,10,10,10)));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(10,10,10,10)));
        outputScrollPane.createVerticalScrollBar();
        outputScrollPane.createHorizontalScrollBar();
        bottomPanel.setLayout(new GridLayout(1, 0 ));
        bottomPanel.add(outputScrollPane);

    } // END createBottomPanel

    /**
     * This method creates and displays a new JTable popup that shows movie reivew's
     * in a semi adjustable window.  It may not be convenient to show full text, but
     * this is a good starting point for JTables
     * @param targetList the list of Movie Reviews to be put in the pop up table
     */
    public static void printJTable(List<MovieReview> targetList) {
        // Create column names
        String columnNames[] = {"ID", "Text", "Predicted", "Real"};
        // Data
        String dataValues[][] = new String[targetList.size()][4];
        for (int i = 0; i < targetList.size(); i++) {
            dataValues[i][0] = String.valueOf((targetList.get(i).getId()));
            dataValues[i][1] = targetList.get(i).getText();
            dataValues[i][2] = String.valueOf(targetList.get(i).getPredictedScore());
            dataValues[i][3] = String.valueOf(targetList.get(i).getRealScore());
        }
        JTable table = new JTable(dataValues,columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.createVerticalScrollBar();
        scrollPane.createHorizontalScrollBar();
        scrollPane.createVerticalScrollBar();
        scrollPane.createHorizontalScrollBar();
        JFrame.setDefaultLookAndFeelDecorated(true);

        JFrame resultFrame = new JFrame("Database Entries");
        resultFrame.setBounds((width - windowsWidth)/5, (height - windowsHeight)/5, windowsWidth, windowsHeight/2);
        resultFrame.setContentPane(scrollPane);
        resultFrame.setVisible(true);
    } //END printJTable

    // used for selecting class in a combo box
    static int realClass = 0;
    /**
     * A new loadreviews that calls rh.loadReviews so that it properly updates in the GUI
     */
    public static void loadReviews() {
        outputArea.setText("");
        outputArea.append(rh.getDatabase().size()+ " records in database.\n");
        outputArea.append("Command 1\n");
        outputArea.append("Please input the path of file or folder.\n");
        outputArea.append("Example ./data/Movie-reviews/neg\n");

        topPanel.removeAll();
        topPanel.add(commandLabel);
        topPanel.add(comboBox);

        //Textbox and Label for file path input
        final JLabel pathLabel = new JLabel("File path", JLabel.RIGHT);
        final JTextField pathInput = new JTextField("");

        //combobox and label for classification of files
        final JLabel polarityLabel = new JLabel("Classification:", JLabel.RIGHT);
        final JComboBox<String> classLabel = new JComboBox<String>();

        // ComboBox for Classification
        classLabel.addItem("Please select:");
        classLabel.addItem("Negative");
        classLabel.addItem("Positive");
        classLabel.addItem("Unknown");
        classLabel.setSelectedIndex(0);

        //Listener for classification label combobox statechange
        classLabel.addItemListener(e -> {
            log.info("Command chosen, Item = " + e.getItem());
            log.info("StateChange = " + e.getStateChange());
            if (e.getStateChange() == 1) {
                if (e.getItem().equals("Negative")) {
                    realClass = 0;
                }
                else if (e.getItem().equals("Positive")) {
                    realClass = 1;
                }
                else if (e.getItem().equals("Unknown")) {
                    realClass = 2;
                }
            }
        });
        final JButton confirmButton = new JButton("Confirm");
        // listener for confirm button
        confirmButton.addActionListener(e -> {
            log.info("Confirm button clicked. (Command 1)");
            Runnable myRunnable = () -> {
                String path = pathInput.getText();
                rh.loadReviews(path, realClass);
            };
            Thread thread = new Thread(myRunnable);
            thread.start();
        });
        topPanel.add(pathLabel);
        topPanel.add(pathInput);
        topPanel.add(polarityLabel);
        topPanel.add(classLabel);

        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());

        topPanel.add(confirmButton);
        topPanel.add(databaseButton);
        topPanel.add(saveButton);
        topPanel.updateUI();
    } // END loadReviews

    /**
     * Delete Review UI implementation
     */
    public static void deleteReviews() {
        outputArea.setText("");
        outputArea.append(rh.getDatabase().size()+ " records in database.\n");
        outputArea.append("Command 2\n");
        outputArea.append("Please input the ID of the review to delete.\n");

        topPanel.removeAll();
        topPanel.add(commandLabel);
        topPanel.add(comboBox);

        final JLabel idLabel = new JLabel("Review ID:", JLabel.RIGHT);
        final JTextField idInput = new JTextField("");

        final JButton deleteButton = new JButton("Confirm");

        deleteButton.addActionListener(e -> {
            log.info("Confirm button clicked. (Command 2)");
            Runnable myRunnable = () -> {
                int inputID = Integer.parseInt(idInput.getText());
                rh.deleteReview(inputID);
            };
            Thread thread = new Thread(myRunnable);
            thread.start();
        });
        topPanel.add(idLabel);
        topPanel.add(idInput);
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());

        topPanel.add(deleteButton);
        topPanel.add(databaseButton);
        topPanel.add(saveButton);
        topPanel.updateUI();
    } // END deleteReview UI

    /**
     * SearchID UI implementation
     */
    static ArrayList<MovieReview> results = new ArrayList<>();
    public static void searchReviewsId() {
        outputArea.setText("");
        outputArea.append(rh.getDatabase().size() + " records in database.\n");
        outputArea.append("Command 3\n");
        outputArea.append("Please type the numerical ID of the review in the field.\n");

        topPanel.removeAll();
        topPanel.add(commandLabel);
        topPanel.add(comboBox);

        final JLabel idSearch = new JLabel("Review ID:", JLabel.RIGHT);
        final JTextField iDInput = new JTextField("");

        final JButton searchButton = new JButton("Search");

        searchButton.addActionListener(e -> {
            log.info("Search button clicked. (Command 3)");
            Runnable myRunnable = () -> {
                try {
                    int reviewId = Integer.parseInt(iDInput.getText());
                    results = new ArrayList<>();
                    MovieReview result = rh.searchById(reviewId);
                    results.add(result);
                    printJTable(results);
                } catch (NumberFormatException e1) {
                    log.setLevel(Level.WARNING);
                    log.warning("Integer exception (SearchReviewsId)");
                    outputArea.append("Please input an integer. EX: 1-2000");
                }
            };
            Thread thread = new Thread(myRunnable);
            thread.start();
        });
        topPanel.add(idSearch);
        topPanel.add(iDInput);
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());

        topPanel.add(searchButton);
        topPanel.add(databaseButton);
        topPanel.add(saveButton);
        topPanel.updateUI();
    } // END searchReviewsID UI


    /**
     * searchSubstring UI
     */
    public static void searchReviewsSubstring() {
        outputArea.setText("");
        outputArea.append(rh.getDatabase().size()+ " records in database.\n");
        outputArea.append("Command 4\n");
        outputArea.append("Please type the string to search for.\n");

        topPanel.removeAll();
        topPanel.add(commandLabel);
        topPanel.add(comboBox);
        //String input field and label
        final JLabel stringLabel = new JLabel("String:", JLabel.RIGHT);
        final JTextField stringField = new JTextField("");
        //Button to make actions happen
        final JButton searchButton = new JButton("Search");
        //Listener that invokes search for searchbutton
        searchButton.addActionListener(e -> {
            log.info("Search button clicked. (Command 4)");
            Runnable myRunnable = () -> {
                String searcher = stringField.getText();
                printJTable(rh.searchBySubstring(searcher));
            };
            Thread thread = new Thread(myRunnable);
            thread.start();
        });
        topPanel.add(stringLabel);
        topPanel.add(stringField);

        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());

        topPanel.add(searchButton);
        topPanel.add(databaseButton);
        topPanel.add(saveButton);
        topPanel.updateUI();
    } // END SubstringSearch UI

    /**
     * New Exit function to implement GUI functionality
     */
    public static void exit() {
        outputArea.setText("");
        outputArea.append(rh.getDatabase().size()+ " records in database.\n");
        outputArea.append("Command 0\n");
        outputArea.append("Please click Confirm to save and exit the system.\n");

        topPanel.removeAll();
        topPanel.add(commandLabel);
        topPanel.add(comboBox);

        // Confirm button and listener to verify exit
        final JButton confirmButton = new JButton("Confirm");
        confirmButton.addActionListener(e -> {
            log.info("confirm button clicked. (Command 0)");
            Runnable myRunnable = () -> {
                try {
                    rh.saveDB();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                outputArea.append("Database saved. System will be closed in 4 seconds.\n");
                outputArea.append("Thank you for using Sentiment Analyzer!\n");

                log.info("Exit the database. (Command 0)");
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                log.info("System shutdown.");
                System.exit(0);
            };
            Thread thread = new Thread(myRunnable);
            thread.start();
        });

        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());
        topPanel.add(new JLabel());

        topPanel.add(confirmButton);
        topPanel.add(databaseButton);
        topPanel.add(saveButton);
        topPanel.updateUI();
        topPanel.updateUI();
    } // END EXIT UI

    /**
     * Main GUI that calls instances of Top and Bottom panel, and has a listener
     * for closing the window.
     */
    private static void createAndShowGUI() {

        //instantiate and add containers
        createTopPanel();
        createBottomPanel();

        topPanel.getIgnoreRepaint();
        JPanel panelContainer = new JPanel();
        panelContainer.setLayout(new GridLayout(2,0));
        panelContainer.add(topPanel);
        panelContainer.add(bottomPanel);

        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("SentimentAnalysis");

        frame.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                log.info("Closing window.");
                outputArea.append("Closing window. Database will be saved.\n");
                super.windowClosing(e);
                log.info("Saving database.");
                try {
                    rh.saveDB();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                log.info("System shutdown.");
                System.exit(0);
            }
        });
        panelContainer.setOpaque(true);
        frame.setBounds((width - windowsWidth) / 2,(height - windowsHeight) / 2, windowsWidth, windowsHeight);
        frame.setContentPane(panelContainer);
        frame.setVisible(true);
    } // END createAndShowGui

}
