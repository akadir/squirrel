package com.kadir.twitterbots.squirrel;

import com.kadir.twitterbots.authentication.BotAuthenticator;
import com.kadir.twitterbots.ratelimithandler.handler.RateLimitHandler;
import com.kadir.twitterbots.ratelimithandler.process.ApiProcessType;
import com.kadir.twitterbots.squirrel.finder.UsernameMiner;
import com.kadir.twitterbots.squirrel.util.SquirrelConstants;
import twitter4j.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static com.kadir.twitterbots.squirrel.util.SquirrelConstants.CHARACTER_LIST;
import static com.kadir.twitterbots.squirrel.util.SquirrelConstants.LAST_USERNAME_BACKUP_FILE_PATH;

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
        this.tweetAvailableUsernames();
    }

    private void authenticate() {
        twitter = BotAuthenticator.authenticate(SquirrelConstants.AUTH_PROPERTIES_FILE_NAME, "");
    }

    private void tweetAvailableUsernames() {
        String name = getLastUsedUsername();
        logger.info("Found last used username as: " + name);
        int counter = 0;
        boolean go = true;

        while (go) {
            counter++;
            name = UsernameMiner.findNextName(name);
            logger.info("Check: " + name);
            try {
                User u = twitter.showUser(name);
                RateLimitHandler.handle(twitter.getId(), u.getRateLimitStatus(), ApiProcessType.SHOW_USER);
            } catch (TwitterException e) {
                try {
                    if (e.getErrorCode() == 50) {
                        logger.info("Found: " + name);
                        Status updatedStatus = twitter.updateStatus(name);
                        logger.info("Status updated to: " + updatedStatus.getText());
                        RateLimitHandler.handle(twitter.getId(), updatedStatus.getRateLimitStatus(), ApiProcessType.UPDATE_STATUS);
                    } else {
                        logger.error("Error occurred for username: " + name, e);
                        saveUsername(name);
                    }
                } catch (TwitterException ex) {
                    if (ex.getErrorCode() == 185) {
                        logger.error("Daily limit has been reached: ", ex);
                        break;
                    } else {
                        logger.error("Error occurred: ", ex);
                    }
                }
            }
            go = counter < Integer.MAX_VALUE;
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
            return compareStrings(tweetedOne, savedOne);
        }
    }

    private String compareStrings(String tweetedOne, String savedOne) {
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
            logger.error("Error occurred while getting last tweeted username: ", e);
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
                logger.error("Error occurred while getting last saved username from file", e);
            }
        }
        return username;
    }

}
