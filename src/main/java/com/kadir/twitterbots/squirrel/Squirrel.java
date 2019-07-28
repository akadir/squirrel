package com.kadir.twitterbots.squirrel;

import com.kadir.twitterbots.authentication.BotAuthenticator;
import com.kadir.twitterbots.ratelimithandler.handler.RateLimitHandler;
import com.kadir.twitterbots.ratelimithandler.process.ApiProcessType;
import com.kadir.twitterbots.squirrel.util.SquirrelConstans;
import twitter4j.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static com.kadir.twitterbots.squirrel.util.SquirrelConstans.*;

/**
 * @author akadir
 * Date: 2019-07-28
 * Time: 23:53
 */
public class Squirrel {
    private final Logger logger = Logger.getLogger(this.getClass());

    private Twitter twitter;

    public static void main(String[] args) {
        Squirrel squirrel = new Squirrel();
        squirrel.run();
    }

    public Squirrel() {
        this.authenticate();

    }

    public void run() {
        try {
            this.tweetAvailableUsernames();
        } catch (TwitterException e) {
            logger.error("Error occurred", e);
        }
    }

    private void authenticate() {
        twitter = BotAuthenticator.authenticate(SquirrelConstans.AUTH_PROPERTIES_FILE_NAME, "");
    }

    private void tweetAvailableUsernames() throws TwitterException {
        String name = getLastUsedUsername();
        logger.info("Found last used username as: " + name);
        int counter = 0;


        while (true) {
            counter++;
            name = findNextName(name);
            logger.info("Check: " + name);
            try {
                User u = twitter.showUser(name);
                RateLimitHandler.handle(twitter.getId(), u.getRateLimitStatus(), ApiProcessType.SHOW_USER);
            } catch (TwitterException e) {
                if (e.getErrorCode() == 50) {
                    logger.info("Found: " + name);
                    Status updatedStatus = twitter.updateStatus(name);
                    logger.info("Status updated to: " + updatedStatus.getText());
                    RateLimitHandler.handle(twitter.getId(), updatedStatus.getRateLimitStatus(), ApiProcessType.UPDATE_STATUS);

                } else if (e.getErrorCode() != 63) {
                    logger.error("TwitterError", e);
                    break;
                }

                if ((counter % 100) == 0) {
                    saveUsername(name);
                }
            }
        }
    }

    private void saveUsername(String name) {
        try {
            Path file = Paths.get(LAST_USERNAME_BACKUP_FILE_PATH);
            Files.createDirectories(file.getParent());
            Files.write(file, name.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Username saved into file: " + name);
        } catch (IOException e) {
            logger.error("Error occurred while saving username into file", e);
        }
    }


    private String findNextName(String name) {
        if (name.equals("")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < MIN_CHAR_COUNT; i++) {
                sb.append(CHARACTER_LIST.charAt(0));
            }

            return sb.toString();
        } else if (name.length() > MAX_CHAR_COUNT || name.length() < MIN_CHAR_COUNT) {
            throw new IndexOutOfBoundsException("Twitter username character count must be between 5-15.");
        } else {
            return getNextName(name, name.length() - 1);
        }
    }

    private String getNextName(String name, int pos) {
        if (pos < 0) {
            return name + CHARACTER_LIST.charAt(0);
        } else {
            int indexOfChar = CHARACTER_LIST.lastIndexOf(name.charAt(pos));
            StringBuilder sb = new StringBuilder(name);
            if (indexOfChar + 1 == CHARACTER_LIST.length()) {
                sb.setCharAt(pos, CHARACTER_LIST.charAt(0));
                return getNextName(sb.toString(), pos - 1);
            } else {
                sb.setCharAt(pos, CHARACTER_LIST.charAt(indexOfChar + 1));
                return sb.toString();
            }
        }
    }


    private String getLastUsedUsername() {
        String tweetedOne = findLastTweetedUsername();
        String savedOne = findLastSavedUserName();

        if (tweetedOne == null && savedOne == null) {
            return "";
        } else if (tweetedOne == null) {
            return savedOne;
        } else if (savedOne == null) {
            return tweetedOne;
        } else {
            if (tweetedOne.length() == savedOne.length()) {
                for (int i = 0; i < tweetedOne.length(); i++) {
                    if (tweetedOne.charAt(i) != savedOne.charAt(i)) {
                        if (CHARACTER_LIST.indexOf(tweetedOne.charAt(i)) > CHARACTER_LIST.indexOf(savedOne.charAt(i))) {
                            return tweetedOne;
                        } else {
                            return savedOne;
                        }
                    }
                }
                return tweetedOne;
            } else {
                if (tweetedOne.length() > savedOne.length()) {
                    return tweetedOne;
                } else {
                    return savedOne;
                }
            }
        }
    }

    private String findLastTweetedUsername() {
        try {
            Paging paging = new Paging(1, 1);
            ResponseList<Status> statuses;
            statuses = twitter.getUserTimeline(twitter.getId(), paging);
            RateLimitHandler.handle(twitter.getId(), statuses.getRateLimitStatus(), ApiProcessType.GET_USER_TIMELINE);

            if (!statuses.isEmpty()) {
                return statuses.get(0).getText();
            }

        } catch (TwitterException e) {
            logger.error("", e);
        }

        return null;
    }

    private String findLastSavedUserName() {
        String username = null;
        Path path = Paths.get(LAST_USERNAME_BACKUP_FILE_PATH);

        if (path.toFile().exists()) {
            try {
                username = new String(Files.readAllBytes(path));
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return username;
    }

}
