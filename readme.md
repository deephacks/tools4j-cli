# Tools4j CLI: Command Line Interface

Provide an easy way to create programs that interact with command line.

GNUish syntax is used for parsing arguments: http://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html.

## Usage

Options have a long form and a short form.

* The short form of an option has a single dash (-) followed by a single character.

* The long form of an option has two dashes (--) followed by an option word.

For example, the short form and the long form of the option for specifying terse output are as follows:

* Short form: -t

* Long form: --terse

Most options require argument values, except Boolean options, which toggle to enable or disable a feature.

Commands are invoked using the following syntax.

    $ t4j_cli [COMMAND] [OPTIONS...] [ARGUMENTS...]

The following options are reserved.

    -v --verbose
    -d --debug
    -h --help

### Examples:

Display all available commands.

    $ t4j_cli

Display the help menu for the 'server' command, showing a short description for all arguments and options.

    $ t4j_cli start-server -h
    $ t4j_cli start-server --help

Invoke 'start-client' command with option --host to 'localhost' and short option -p (port) to '7000'

    $ t4j_cli start-client --host localhost -p 7000

Invoke 'stop-server' command with boolean short option -f (force) and arguments '0' to 'localhost' and '1' to '7000'

    $ t4j_cli stop-server -f localhost 7000


