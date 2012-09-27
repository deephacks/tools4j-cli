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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deephacks.tools4j.cli.Command.XmlCommands;

/**
 * This is the central class used to execute commands.  
 */
public final class CliMain {
    /** commands available */
    private final Map<String, Command> commands = new HashMap<String, Command>();
    /** cli command parser */
    private GNUishParser p;
    /** raw terminal arguments */
    private String[] terminalArgs;

    /**
     * Executed when the user runs: 
     * 
     * $ java -jar tools4j-cli.jar
     * 
     * @param args command arguments 
     */
    public static void main(String[] args) {
        CliMain main = new CliMain(args);
        main.run();
    }

    public CliMain(String terminalArgs[]) {
        this.terminalArgs = terminalArgs;
    }

    /**
     * @see run()
     * @param command Instance of a class that defines at least one command.
     */
    public void run(Object command) {
        List<Command> cmds = Command.create(command);
        for (Command cmd : cmds) {
            cmd.setInstance(command);
            commands.put(cmd.getCommand(), cmd);
        }
        run();
    }

    /**
     * Start evaluating the user input and eventually execute the command 
     * requested by the user.
     * 
     * @throws RuntimeException Any runtime exceptions thrown by either this
     * library or user commands will fall through and thrown from this method. 
     */
    public void run() throws RuntimeException {
        if (terminalArgs == null) {
            terminalArgs = new String[0];
        }
        p = GNUishParser.parse(terminalArgs);
        readCommands();
        if (p.getCommand() == null || "".equals(p.getCommand())) {
            Utils.printAvailableCommandsHelp(commands);
            return;
        }

        Command cmd = commands.get(p.getCommand());
        if (cmd == null) {
            throw CliException.COMMAND_NOT_FOUND(p.getCommand());
        }
        if (p.help()) {
            Utils.printCommandHelp(cmd);
            return;
        }
        try {
            cmd.execute(p);
        } catch (Exception e) {
            if (p.debug()) {
                e.printStackTrace();
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }
    }

    /**
     * Find all commands available on classpath.
     */
    private void readCommands() {
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader()
                    .getResources(XmlCommands.FILEPATH);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                InputStream in = url.openStream();
                for (Command command : XmlCommands.fromXml(in)) {
                    commands.put(command.getCommand(), command);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
