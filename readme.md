# tools4j-cli

Provides a small, simple, non-intrusive api for creating Java based command line interfaces.

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

    $ cmd [COMMAND] [OPTIONS...] [ARGUMENTS...]

The following options are reserved.

    --verbose
    --debug
    --help
