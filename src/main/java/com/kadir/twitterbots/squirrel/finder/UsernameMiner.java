package com.kadir.twitterbots.squirrel.finder;

import static com.kadir.twitterbots.squirrel.util.SquirrelConstants.*;

/**
 * @author akadir
 * Date: 2019-07-29
 * Time: 22:06
 */
public class UsernameMiner {

    private UsernameMiner() {
    }

    public static String findNextName(String name) {
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

    private static String getNextName(String name, int pos) {
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
}
