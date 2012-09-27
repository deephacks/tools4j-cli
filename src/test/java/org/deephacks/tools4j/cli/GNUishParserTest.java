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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Iterator;

import org.junit.Test;

public class GNUishParserTest {

    @Test
    public void testShortBooleanOpts() {

        String[] args = new String[] { "cmd", "-a", "-b" };
        GNUishParser p = GNUishParser.parse(args);

        assertThat(p.getShortOpt("a"), notNullValue());
        assertThat(p.getShortOpt("b"), notNullValue());
        assertThat(p.getArgs().size(), is(0));
        assertThat(p.getLongOpts().size(), is(0));

        args = new String[] { "cmd", "-ab" };
        p = GNUishParser.parse(args);
        assertThat(p.getShortOpt("a"), notNullValue());
        assertThat(p.getShortOpt("b"), notNullValue());
        assertThat(p.getArgs().size(), is(0));
        assertThat(p.getLongOpts().size(), is(0));
    }

    @Test
    public void testShortArgOpts() {
        String[] args = new String[] { "cmd", "-a", "a-arg", "-b", "b-arg" };
        GNUishParser p = GNUishParser.parse(args);

        assertThat(p.getShortOpt("a"), is("a-arg"));
        assertThat(p.getShortOpt("b"), is("b-arg"));
        assertThat(p.getArgs().size(), is(0));
        assertThat(p.getLongOpts().size(), is(0));
    }

    @Test
    public void testLongArgOpts() {
        String[] args = new String[] { "cmd", "--foo", "foo-arg", "--bar", "bar-arg" };
        GNUishParser p = GNUishParser.parse(args);

        assertThat(p.getLongOpt("foo"), is("foo-arg"));
        assertThat(p.getLongOpt("bar"), is("bar-arg"));
        assertThat(p.getArgs().size(), is(0));
        assertThat(p.getShortOpts().size(), is(0));
    }

    @Test
    public void testLongBooleanOpts() {
        String[] args = new String[] { "cmd", "--foo", "--bar" };
        GNUishParser p = GNUishParser.parse(args);

        assertThat(p.getLongOpt("foo"), notNullValue());
        assertThat(p.getLongOpt("bar"), notNullValue());
        assertThat(p.getArgs().size(), is(0));
        assertThat(p.getShortOpts().size(), is(0));
    }

    @Test
    public void testOnlyArgs() {
        String[] args = new String[] { "cmd", "arg1", "arg2" };
        GNUishParser p = GNUishParser.parse(args);

        Iterator<String> argsIt = p.getArgs().iterator();
        assertThat(argsIt.next(), is("arg1"));
        assertThat(argsIt.next(), is("arg2"));
        assertThat(p.getLongOpts().size(), is(0));
        assertThat(p.getShortOpts().size(), is(0));

    }

    @Test
    public void testMixedBooleanOpts() {
        String[] args = new String[] { "cmd", "-a", "--bar" };
        GNUishParser p = GNUishParser.parse(args);

        assertThat(p.getShortOpt("a"), notNullValue());
        assertThat(p.getLongOpt("bar"), notNullValue());

        args = new String[] { "cmd", "--bar", "-a" };
        p = GNUishParser.parse(args);

        assertThat(p.getShortOpt("a"), notNullValue());
        assertThat(p.getLongOpt("bar"), notNullValue());
    }

    @Test
    public void testMixedArgOpts() {
        String[] args = new String[] { "cmd", "-a", "a-arg", "arg1", "-cd", "--bar", "bar-arg",
                "arg2" };
        GNUishParser p = GNUishParser.parse(args);

        assertThat(p.getShortOpt("a"), is("a-arg"));
        assertThat(p.getLongOpt("bar"), is("bar-arg"));
        assertThat(p.getShortOpt("c"), notNullValue());
        assertThat(p.getShortOpt("d"), notNullValue());
        Iterator<String> argsIt = p.getArgs().iterator();
        assertThat(argsIt.next(), is("arg1"));
        assertThat(argsIt.next(), is("arg2"));

    }

}
