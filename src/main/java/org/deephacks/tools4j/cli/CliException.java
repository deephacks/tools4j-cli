package org.deephacks.tools4j.cli;

/**
 * CliException is thrown when user input is wrong, in one way or 
 * another. It will contain a message that can be displayed directly
 * to the user.
 */
public class CliException extends RuntimeException {
    private static final long serialVersionUID = -9018213066631940115L;
    static final String COMMAND_NOT_FOUND_MSG = "command not found";
    static final String WRONG_OPT_TYPE_MSG = "Option has wrong type. ";
    static final String WRONG_ARG_TYPE_MSG = "Argument has wrong type. ";

    public CliException(String message) {
        super(message);
    }

    public CliException() {
        super();
    }

    public CliException(String message, Throwable cause) {
        super(message, cause);
    }

    public CliException(Throwable cause) {
        super(cause);
    }

    static CliException COMMAND_NOT_FOUND(String cmd) {
        return new CliException(cmd + ": " + COMMAND_NOT_FOUND_MSG);
    }

    static CliException WRONG_OPT_TYPE(String name, String type, String value) {
        String msg = WRONG_OPT_TYPE_MSG + name + " with input value " + value + " should be "
                + type + ".";
        return new CliException(msg);
    }

    static CliException WRONG_ARG_TYPE(String name, String type, String value) {
        String msg = WRONG_ARG_TYPE_MSG + name + " with input value " + value + " should be "
                + type + ".";
        return new CliException(msg);
    }

}