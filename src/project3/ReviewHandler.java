package project3;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ReviewHandler extends AbstractReviewHandler {

    private static AtomicInteger ID = new AtomicInteger(1);

    /**
     * Loads reviews from a given path. If the given path is a .txt file, then
     * a single review is loaded. Otherwise, if the path is a folder, all reviews
     * in it are loaded.
     * @param filePath The path to the file (or folder) containing the review(sentimentModel).
     * @param realClass The real class of the review (0 = Negative, 1 = Positive
     * 2 = Unknown).
     */
    @Override
    public void loadReviews(String filePath, int realClass) {
        File fileOrFolder = new File(filePath);
        try {
            if (fileOrFolder.isFile()) {
                // File
                if (filePath.endsWith(".txt")) {
                    // Import review
                    MovieReview review = readReview(filePath, realClass);
                    // Classify review
                    ReviewScore rs = classifyReview(review);
                    review.setPredictedScore(rs);
                    // Add to getDatabase()
                    getDatabase().put(review.getId(), review);
                    //Output result: single file
                    MovieReviewApp.outputArea.append("Review imported.\n");
                    MovieReviewApp.outputArea.append("ID: " + review.getId() + "\n");
                    MovieReviewApp.outputArea.append("Text: " + review.getText() + "\n");
                    MovieReviewApp.outputArea.append("Real Class: " + review.getRealScore() + "\n");
                    MovieReviewApp.outputArea.append("Classification result: " + review.getPredictedScore() + "\n");
                    if (realClass == 2) {
                        MovieReviewApp.outputArea.append("Real class Unknown.\n");
                    } else if (realClass == 0 && review.getPredictedScore() == ReviewScore.NEGATIVE
                            || realClass == 1 && review.getPredictedScore() == ReviewScore.POSITIVE) {
                        MovieReviewApp.outputArea.append("Correctly classified.\n");
                    } else {
                        MovieReviewApp.outputArea.append("Misclassified.\n");
                    }
                    MovieReviewApp.outputArea.append("\n");

                } else {
                    // Cannot import non-txt files
                    MovieReviewApp.outputArea.append("Input file path is neither a txt file nor folder.\n");
                }
            } else {
                // Folder
                MovieReviewApp.outputArea.append("Loading reviews...");
                String[] files = fileOrFolder.list();
                String fileSeparatorChar = System.getProperty("file.separator");
                AtomicInteger counter = new AtomicInteger( 0);
                // Create threadpool for classifying
                BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1024);
                ExecutorService threadPoolExecutor = new ThreadPoolExecutor(10,40,60, TimeUnit.SECONDS, workQueue);
                for (String fileName : files) {
                    if (fileName.endsWith(".txt")) {
                        Runnable myRunnable = () -> {
                            MovieReview review = null;
                            try {
                                review = readReview(filePath + fileSeparatorChar + fileName, realClass);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (review == null) {
                                return;
                            }
                            getDatabase().put(review.getId(), review);
                            ReviewScore rs = classifyReview(review);
                            review.setPredictedScore(rs);
                            if (realClass != 2 && review.getRealScore() == review.getPredictedScore()) {
                                counter.getAndIncrement();
                            }
                        };
                        Thread thread = new Thread(myRunnable);
                        threadPoolExecutor.execute(thread);
                    } else {
                        // Do nothing block
                    }
                }
                threadPoolExecutor.shutdown();
                try {
                    threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    MovieReviewApp.log.warning("InterruptedException " + e + "(threadpool shutdown)");
                    e.printStackTrace();
                }

                // Output result: folder
                MovieReviewApp.outputArea.append("Folder imported.\n");
                MovieReviewApp.outputArea.append("Number of entries: " + files.length + "\n");

                // Only output accuracy if real class is known
                if (realClass != 2) {
                    MovieReviewApp.outputArea.append("Correctly classified: " + counter + "\n");
                    MovieReviewApp.outputArea.append("Misclassified: " + (files.length - counter.get() + "\n"));
                    MovieReviewApp.outputArea.append("Accuracy: " + ((double)counter.get() / (double)files.length * 100) + "%\n");
                }
            }
        } catch (IOException e) {
            MovieReviewApp.outputArea.append(e.toString() + "\n");
            MovieReviewApp.log.warning("IOException " + e + " (loadReviews)\n");
        }

    }

    /**
     * Reads a single review file and returns it as a MovieReview object.
     * @param reviewFilePath A path to a .txt file containing a review.
     * @param realClass The real class entered by the user.
     * @return a MovieReview object.
     * @throws IOException if specified file cannot be opened.
     */
    @Override
    public MovieReview readReview(String reviewFilePath, int realClass) throws IOException {
        // Read file for text
        Scanner inFile = new Scanner(new FileReader(reviewFilePath));
        String text = "";
        while (inFile.hasNextLine()) {
            text += inFile.nextLine();
        }
        inFile.close();

        // Remove the <br /> occurences in the text and replace them with a space
        text = text.replaceAll("<br />"," ");

        // Create review object, assigning reviewIdCounter and real class
        MovieReview review = new MovieReview(ID.getAndIncrement(), reviewFilePath,
                text, ReviewScore.fromInteger(realClass), ReviewScore.UNKNOWN);

        // Update reviewIdCounter
        // THIS IS NOW DONE IN LOAD WITH THREADS
        //setReviewIdCounter(getReviewIdCounter() + 1);

        return review;
    }


    /**
     * Classifies a review as negative, or positive by using the text of the review.
     * It updates the predictedPolarity value of the review object and it also
     * returns the predicted polarity.
     * Note: the classification is achieved by counting positive and negative words
     * in the review text.
     * @param review A review object.
     * @return 0 = negative, 1 = positive.
     */
    @Override
    public ReviewScore classifyReview(MovieReview review){
        int positive = 0;
        int negative = 0;
        // Remove the <br /> occurences in the text and replace them with a space
        String text = review.getText().replaceAll("<br />", " ");
        //Remove punctuation marks and replace them with spaces.
        text = text.replaceAll("\\p{Punct}", " ");
        //call function to format text and classify Review
        text = text.toLowerCase();

        // Split the text into tokens using white spaces as the separator character.
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            if (getPosWords().contains(token)) {
                positive++;
            }
            if (getNegWords().contains(token)) {
                negative++;
            }
        }

        if ( positive > negative ){

            return ReviewScore.POSITIVE;

        }else if ( positive < negative ){

            return ReviewScore.NEGATIVE;

        } else return ReviewScore.UNKNOWN;

    }

    /**
     * Deletes a review from the getDatabase(), given its id.
     * @param id The id value of the review.
     */
    @Override
    public void deleteReview(int id) {

        if (!getDatabase().containsKey(id)) {
            // Review with given reviewIdCounter does not exist
            MovieReviewApp.outputArea.append("ID " + id + " does not exist.\n");
        } else {
            getDatabase().remove(id);
            MovieReviewApp.outputArea.append("Review with ID " + id + " deleted.\n");
        }
    }


    /**
     * Saves the getDatabase() in the working directory as a text file.
     * @throws java.io.IOException
     */
    @Override
    public void saveDB() throws IOException {
        PrintWriter out = new PrintWriter(DATA_FILE_NAME);

        for (MovieReview mr : getDatabase().values()) {
            out.println(mr.getId() + " @ " +
                    mr.getFilePath() + " @ " +
                    mr.getRealScore() + " @ " +
                    mr.getPredictedScore());
        }

        close(out);
    }

    /**
     * Loads review getDatabase().
     */
    @Override
    public void loadDB() throws IOException {
        MovieReviewApp.outputArea.append("Reading getDatabase()...");

        File dataFile = new File(DATA_FILE_NAME);
        if (!dataFile.exists()) {
            MovieReviewApp.outputArea.append("No getDatabase().txt file found. A new empty "
                    + "getDatabase() will be created.");
            return;
        }

        Scanner inFile = new Scanner(new FileReader(DATA_FILE_NAME));

        String line = "";
        while (inFile.hasNextLine()) {
            line = inFile.nextLine();
            String[] lineParts = line.split(" @ ");
            int id = Integer.parseInt(lineParts[0]);
            String filePath = lineParts[1];
            String text = readReview(filePath, 0).getText();
            ReviewScore realScore = ReviewScore.fromString(lineParts[2]);
            ReviewScore predictedScore = ReviewScore.fromString(lineParts[3]);

            MovieReview mr = new MovieReview(id, filePath, text, realScore, predictedScore);
            getDatabase().put(id, mr);
        }

        // Set the reviewIdCounter to be one greater than the largest id in the database().
        if (getDatabase().keySet().size() > 0) {
            int currMaxId = Collections.max(getDatabase().keySet());
            setReviewIdCounter(currMaxId + 1);
            ID.set(currMaxId + 1);
        }
        close(inFile);
        MovieReviewApp.outputArea.append("Done.\n");
    }

    /**
     * Searches the review getDatabase() by id.
     * @param id The id to search for.
     * @return The review that matches the given id or null if the id does not
     * exist in the getDatabase().
     */
    @Override
    public MovieReview searchById(int id) {
        if (getDatabase().containsKey(id)) {
            return getDatabase().get(id);
        }
        MovieReviewApp.outputArea.append("No review found.\n");
        return null;
    }

    /**
     * Searches the review getDatabase() for reviews matching a given substring.
     * @param substring The substring to search for.
     * @return A list of review objects matching the search criterion.
     */
    @Override
    public List<MovieReview> searchBySubstring(String substring) {
        List<MovieReview> tempList = new ArrayList<>();

        for (Map.Entry<Integer, MovieReview> entry : getDatabase().entrySet()){
            if (entry.getValue().getText().contains(substring)) {
                tempList.add(entry.getValue());
            }
        }
        if (!tempList.isEmpty()) {
            return tempList;
        } else {
            // No review has given substring
            MovieReviewApp.outputArea.append("No review(s) found.\n");
            return null;
        }

    }
}