/**
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.deephacks.tools4j.cli;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.junit.Test;

@SuppressWarnings("unused")
public class CliMainTest {

    String stringValue = "string";
    String byteString = "1";
    Byte byteValue = new Byte(byteString);
    String integerString = "5";
    Integer integerValue = new Integer(integerString);
    String floatString = "5.0";
    Float floatValue = new Float(floatString);
    String doubleString = "1000000000000.0";
    Double doubleValue = new Double(doubleString);
    String longString = "1000000000000";
    Long longValue = new Long(longString);
    String shortString = "120";
    Short shortValue = new Short(shortString);
    String booleanString = "true";
    Boolean booleanValue = new Boolean(booleanString);
    String timeunitEnumString = "SECONDS";
    TimeUnit timeunitEnumValue = TimeUnit.SECONDS;
    String urlString = "http://www.test.com";
    URL urlValue = newURL(urlString);
    String fileString = ".";
    File fileValue = new File(fileString);
    String dateString = "2010-10-10";
    File cliArg1 = new File(".");
    Integer cliArg2 = 1;

    @Test
    public void test_success() {
        TestCommand command = new TestCommand();
        // command
        String[] args = new String[] { "commandword" };
        // short opts
        args = fill(args, new String[] { "-a", stringValue });
        args = fill(args, new String[] { "-b", byteString });
        args = fill(args, new String[] { "-c", integerString });
        args = fill(args, new String[] { "-e", floatString });
        args = fill(args, new String[] { "-f", doubleString });
        args = fill(args, new String[] { "-g", longString });
        // -h and --help is reserved for help
        args = fill(args, new String[] { "-i", shortString });
        // non-argumented option
        args = fill(args, new String[] { "-j" });
        args = fill(args, new String[] { "-k", timeunitEnumString });
        args = fill(args, new String[] { "-l", urlString });
        args = fill(args, new String[] { "-m", fileString });
        // turn on debug
        args = fill(args, new String[] { "--debug" });
        // args
        args = fill(args, new String[] { "." });
        args = fill(args, new String[] { "1" });

        CliMain cli = new CliMain(args);

        cli.run(command);

        assertThat(command.stringValue, is(stringValue));
        assertThat(command.byteValue, is(byteValue));
        assertThat(command.integerValue, is(integerValue));
        assertThat(command.floatValue, is(floatValue));
        assertThat(command.doubleValue, is(doubleValue));
        assertThat(command.longValue, is(longValue));
        assertThat(command.shortValue, is(shortValue));
        assertThat(command.booleanValue, is(booleanValue));
        assertThat(command.timeunitEnumValue, is(timeunitEnumValue));
        assertThat(command.urlValue, is(urlValue));
        assertThat(command.fileValue, is(fileValue));
        assertThat(command.cliArg1.getAbsolutePath(), is(cliArg1.getAbsolutePath()));
        assertThat(command.cliArg2, is(cliArg2));

    }

    @Test
    public void test_command_help() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stdout = redirectOut(out);

        // short opt
        String[] args = new String[] { "commandword" };
        args = fill(args, new String[] { "--help" });
        CliMain cli = new CliMain(args);
        cli.run(new TestCommand());
        String helpscreen = new String(out.toByteArray());
        assertTrue(helpscreen.startsWith("usage"));
        // reset out
        System.setOut(stdout);
    }

    @Test
    public void test_list_commands_help() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stdout = redirectOut(out);

        String[] args = new String[] { "" };
        CliMain cli = new CliMain(args);
        cli.run(new TestCommand());
        String helpscreen = new String(out.toByteArray());
        assertTrue(helpscreen.startsWith(Utils.AVAILABLE_CMDS_MSG));
        assertTrue(helpscreen.contains("commandword"));
        // reset out
        System.setOut(stdout);
    }

    @Test
    public void test_default_arg_values() {
        final class DefaultArgCommand {
            String arg1;
            String arg2;

            @CliCmd
            public void exec(@Default("default1") String arg1, @Default("default2") String arg2) {
                this.arg1 = arg1;
                this.arg2 = arg2;
            }
        }
        String[] args = new String[] { "exec", "arg1" };
        CliMain cli = new CliMain(args);
        DefaultArgCommand d = new DefaultArgCommand();
        cli.run(d);
        assertEquals("arg1", d.arg1);
        assertEquals("default2", d.arg2);

        args = new String[] { "exec" };
        cli = new CliMain(args);
        d = new DefaultArgCommand();
        cli.run(d);
        assertEquals("default1", d.arg1);
        assertEquals("default2", d.arg2);

        final class DefaultArgCommand2 {
            String arg1;
            String arg2;

            @CliCmd
            public void exec(String arg1, @Default("default2") String arg2) {
                this.arg1 = arg1;
                this.arg2 = arg2;
            }
        }
        args = new String[] { "exec" };
        cli = new CliMain(args);
        DefaultArgCommand2 d2 = new DefaultArgCommand2();
        cli.run(d2);
        assertEquals(null, d2.arg1);
        assertEquals("default2", d2.arg2);
    }

    @Test
    public void test_unexpected_exception() {
        final IllegalStateException e = new IllegalStateException("Unexpected Exception");
        final class UnexpectedExceptionCommand {
            @CliCmd
            public void unexpected() {
                throw e;
            }
        }
        ;

        String[] args = new String[] { "unexpected" };
        CliMain cli = new CliMain(args);
        try {
            cli.run(new UnexpectedExceptionCommand());
            fail("exception expected");
        } catch (Exception ex) {
            assertEquals(ex, e);
        }

    }

    @Test
    public void test_invalid_option_input() {
        String[] args = new String[] { "commandword" };
        // try double value on integer
        args = fill(args, new String[] { "-c", doubleValue.toString() });
        CliMain cli = new CliMain(args);
        try {
            cli.run(new TestCommand());
            fail("Should have thrown exception");
        } catch (CliException e) {
            assertTrue("Wrong message: " + e.getMessage(),
                    e.getMessage().contains(CliException.WRONG_OPT_TYPE_MSG));
        }

    }

    @Test
    public void test_invalid_argument_input() {

        String[] args = new String[] { "commandword" };

        // try double value on integer
        args = fill(args, new String[] { "invalid_file" });
        args = fill(args, new String[] { "second_arg" });
        CliMain cli = new CliMain(args);
        try {
            cli.run(new TestCommand());
            fail("exception expected");
        } catch (CliException e) {
            assertTrue("Wrong message: " + e.getMessage(),
                    e.getMessage().contains(CliException.WRONG_ARG_TYPE_MSG));
        }

    }

    @Test
    public void test_missing_argument_input() {
        String[] args = new String[] { "missing" };
        final class MissingArgCommand {
            @CliCmd
            public void missing(@NotNull String msg) {

            }
        }
        ;
        CliMain cli = new CliMain(args);
        try {
            cli.run(new MissingArgCommand());
            fail("exception expected");
        } catch (ValidationException e) {
            assertTrue("Wrong message: " + e.getMessage(),
                    e.getMessage().contains(Validator.ARG_VIOLATION_MSG));
        }
    }

    @Test
    public void test_input_constraint_violation() {
        final class ValidatedCommand {
            @CliOption(shortName = "o")
            @Max(10)
            @Min(5)
            @NotNull
            private Integer opt;

            @CliCmd
            public void validate(@Max(10) @Min(5) @NotNull Integer arg) {

            }

        }
        ;

        // success
        String[] args = new String[] { "validate" };
        args = fill(args, new String[] { "-o", "6" });
        args = fill(args, new String[] { "6" });
        CliMain cli = new CliMain(args);
        try {
            cli.run(new ValidatedCommand());
        } catch (ValidationException e) {
            fail(e.getMessage());
        }

        // too small opt
        args = new String[] { "validate" };
        args = fill(args, new String[] { "-o", "4" });
        args = fill(args, new String[] { "6" });
        cli = new CliMain(args);

        try {
            cli.run(new ValidatedCommand());
            fail("Validation exception expected");
        } catch (ValidationException e) {
            assertTrue("Wrong message: " + e.getMessage(),
                    e.getMessage().contains(Validator.OPT_VIOLATION_MSG));
        }

        // too small arg
        args = new String[] { "validate" };
        args = fill(args, new String[] { "-o", "6" });
        args = fill(args, new String[] { "1" });
        cli = new CliMain(args);
        try {
            cli.run(new ValidatedCommand());
            fail("Validation exception expected");
        } catch (ValidationException e) {
            assertTrue("Wrong message: " + e.getMessage(),
                    e.getMessage().contains(Validator.ARG_VIOLATION_MSG));
        }

    }

    @Test
    public void test_command_does_not_exist() {
        String[] args = new String[] { "bogus" };
        CliMain cli = new CliMain(args);

        try {
            cli.run();
        } catch (CliException e) {
            assertTrue("Wrong message: " + e.getMessage(),
                    e.getMessage().contains(CliException.COMMAND_NOT_FOUND_MSG));
        }

    }

    private static URL newURL(String urlString) {
        try {
            return new URL(urlString);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    String[] fill(String[] first, String[] second) {
        List<String> both = new ArrayList<String>(first.length + second.length);
        Collections.addAll(both, first);
        Collections.addAll(both, second);
        return both.toArray(new String[] {});
    }

    public class TestCommand {
        @CliOption(shortName = "a")
        private String stringValue;
        @CliOption(shortName = "b")
        private Byte byteValue;
        @CliOption(shortName = "c")
        private Integer integerValue;
        @CliOption(shortName = "e")
        private Float floatValue;
        @CliOption(shortName = "f")
        private Double doubleValue;
        @CliOption(shortName = "g")
        private Long longValue;
        @CliOption(shortName = "i")
        private Short shortValue;
        @CliOption(shortName = "j")
        private Boolean booleanValue;
        @CliOption(shortName = "k")
        private TimeUnit timeunitEnumValue;
        @CliOption(shortName = "l")
        private URL urlValue;
        @CliOption(shortName = "m")
        private File fileValue;

        public File cliArg1;
        public Integer cliArg2;

        @CliCmd
        public void commandword(File cliarg1, Integer cliarg2) {
            this.cliArg1 = cliarg1;
            this.cliArg2 = cliarg2;
        }
    }

    public PrintStream redirectOut(ByteArrayOutputStream out) {
        PrintStream stdout = System.out;
        PrintStream ps = new PrintStream(out, true);
        System.setOut(ps);
        return stdout;
    }
}