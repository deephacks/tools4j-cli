/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.tools4j.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * GNUishParser is responsible for parsing the command line arguments more or less 
 * according to the GNU Argument Syntax: 
 * 
 * http://www.gnu.org/s/hello/manual/libc/Argument-Syntax.html.
 * 
 * Inspiration from http://commons.apache.org/cli
 */
final class GNUishParser {
    private final Map<String, String> shortOpts = new HashMap<String, String>();
    private final Map<String, String> longOpts = new HashMap<String, String>();
    private final List<String> arguments = new ArrayList<String>();
    private String command;
    private static String VERBOSE_LONG_OPT = "verbose";
    private static String DEBUG_LONG_OPT = "debug";
    private static String HELP_LONG_OPT = "help";

    private GNUishParser() {
    }

    static GNUishParser parse(String[] terminalArgs) {
        final GNUishParser p = new GNUishParser();
        if (terminalArgs == null || terminalArgs.length == 0) {
            return p;
        }
        p.command = terminalArgs[0].trim();
        terminalArgs = Arrays.copyOfRange(terminalArgs, 1, terminalArgs.length);

        // we strip opts after parsing
        terminalArgs = p.parseOpts(terminalArgs);

        // arguments is what's left
        p.arguments.addAll(Arrays.asList(terminalArgs));
        return p;
    }

    /**
     * Parse the options for the command.
     * 
     * @param args includes the options and arguments, command word have been stripped.
     * @return the remaining terminal args, if any
     */
    private String[] parseOpts(String[] args) {

        if (args == null || args.length == 0) {
            return new String[0];
        }
        final List<String> remainingArgs = new ArrayList<String>();
        final List<String> argsList = Arrays.asList(args);
        final ListIterator<String> argsIt = argsList.listIterator();

        while (argsIt.hasNext()) {
            String word = argsIt.next();
            if (word.startsWith("--")) {
                // long option --foo
                final String option = stripLeadingHyphens(word);
                if (VERBOSE_LONG_OPT.equals(option)) {
                    longOpts.put(option, "true");
                } else if (DEBUG_LONG_OPT.equals(option)) {
                    longOpts.put(option, "true");
                } else if (HELP_LONG_OPT.equals(option)) {
                    longOpts.put(option, "true");
                } else {
                    final String arg = parseOptionArg(option, argsIt);
                    longOpts.put(option, arg);
                }

            } else if (word.startsWith("-")) {
                String options = stripLeadingHyphens(word);

                // single short option -f
                if (options.length() == 1) {
                    // only slurp argument if option is argumented
                    final String arg = parseOptionArg(options, argsIt);
                    shortOpts.put(options, arg);
                    continue;
                }
                // multiple short options -fxy, 
                // treat as non-argumented java.lang.Boolean variables, no slurp 
                for (int i = 0; i < options.length(); i++) {
                    final String option = Character.toString(options.charAt(i));
                    shortOpts.put(option, "true");
                }
            } else {
                remainingArgs.add(word);
            }
        }
        return remainingArgs.toArray(new String[0]);
    }

    private String parseOptionArg(String option, ListIterator<String> argsIt) {
        if (!argsIt.hasNext()) {
            // no argument. assume boolean opt
            return "true";
        }

        String arg = argsIt.next();
        // the token following the option is a new option (not an argument)
        // digits following the hyphen are treated as arguments
        if (arg.startsWith("-") && arg.length() > 1 && !Character.isDigit(arg.charAt(1))) {
            // arg was the next opt, take one step back 
            argsIt.previous();
            // assume argument for opt is a boolean 
            arg = "true";
        }
        return arg;
    }

    private static String stripLeadingHyphens(String str) {
        if (str == null) {
            return null;
        }
        if (str.startsWith("--")) {
            return str.substring(2, str.length());
        } else if (str.startsWith("-")) {
            return str.substring(1, str.length());
        }
        return str;
    }

    String getCommand() {
        return command;
    }

    List<String> getArgs() {
        return arguments;
    }

    Map<String, String> getShortOpts() {
        return shortOpts;
    }

    String getShortOpt(String id) {
        return shortOpts.get(id);
    }

    Map<String, String> getLongOpts() {
        return longOpts;
    }

    String getLongOpt(String id) {
        return longOpts.get(id);
    }

    public static List<String> getReservedNonArgumentOptions() {
        return Arrays.asList(VERBOSE_LONG_OPT, DEBUG_LONG_OPT, HELP_LONG_OPT);
    }

    public boolean verbose() {
        if (getLongOpt(VERBOSE_LONG_OPT) != null) {
            return true;
        }
        return false;
    }

    public boolean debug() {
        if (getLongOpt(DEBUG_LONG_OPT) != null) {
            return true;
        }
        return false;
    }

    public boolean help() {
        if (getLongOpt(HELP_LONG_OPT) != null) {
            return true;
        }
        return false;
    }

}
