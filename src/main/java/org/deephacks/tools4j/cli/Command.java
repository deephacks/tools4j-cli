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

import static org.deephacks.tools4j.cli.Utils.validateArgs;
import static org.deephacks.tools4j.cli.Utils.validateOpts;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.deephacks.tools4j.cli.Conversion.ConversionException;

/**
 * Internal representation of commands.
 */
@XmlAccessorType(XmlAccessType.FIELD)
final class Command {
    /** handle conversion of command input strings to objects */
    private static final Conversion c = Conversion.get();
    /** command alias */
    @XmlAttribute(name = "cmd")
    private String cmd;
    /** maintain the mapping between command alias and class */
    @XmlAttribute(name = "class")
    private String className;
    /** documentation of this command */
    @XmlElement(name = "doc")
    private String doc;
    /** options of this command */
    @XmlElement(name = "opt")
    private List<Option> options = new ArrayList<Option>();
    /** arguments of this command */
    @XmlElement(name = "arg")
    private List<Argument> arguments = new ArrayList<Argument>();
    /** the object instance that will execute the command */
    private Object instance;

    public Command() {

    }

    public static List<Command> create(Object command) {
        final List<Command> commands = new ArrayList<Command>();
        final Class<?> cmdClazz = command.getClass();
        for (Method m : cmdClazz.getDeclaredMethods()) {
            m.setAccessible(true);
            final CliCmd anno = m.getAnnotation(CliCmd.class);
            if (anno == null) {
                continue;
            }
            final String cmdname = m.getName();
            final Command cmd = new Command(cmdname, cmdClazz.getName(), "n/a");
            int i = 0;
            for (Class<?> cls : m.getParameterTypes()) {
                cmd.addArgument(new Argument("n/a", cls.getName(), i++, "n/a"));
            }
            commands.add(cmd);
        }
        return commands;
    }

    public Command(String cmd, String className, String doc) {
        this.cmd = cmd;
        this.className = className;
        if (doc == null) {
            this.doc = "";
        } else {
            this.doc = doc;
        }
    }

    /**
     * @return the alias that activates the command.
     */
    public String getCommand() {
        return cmd;
    }

    /**
     * @return raw the documentation of the command, as read from commands.xml.
     */
    public String getDoc() {
        return doc;
    }

    /**
     * Will be treated in the same order as they are added.
     * 
     * @param arg argument of this command.
     */
    public void addArgument(Argument arg) {
        arguments.add(arg);
    }

    /**
     * @return all arguments of this command.
     */
    public List<Argument> getArguments() {
        return arguments;
    }

    /**
     * @param opt add an option to this command.
     */
    public void addOptions(Option opt) {
        options.add(opt);
    }

    /**
     * @return all options of this command.
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * @param o instance that will handle this command.
     */
    void setInstance(Object o) {
        this.instance = o;
    }

    /**
     * Execute this command according to the user input arguments
     * parsed by the parser. 
     */
    public void execute(GNUishParser p) {
        if (instance == null) {
            instance = Utils.newInstance(className);
        }
        final Class<?> clazz = instance.getClass();
        final Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            final CliCmd a = m.getAnnotation(CliCmd.class);
            m.setAccessible(true);
            if (a == null || !m.getName().equals(p.getCommand())) {
                continue;
            }
            final List<Object> args = adjustArgs(p.getArgs(), m);
            injectOpts(p, clazz);
            try {
                validateArgs(args, instance, m);
                m.invoke(instance, args.toArray());
                return;
            } catch (InvocationTargetException e) {
                final Throwable ex = e.getTargetException();
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new RuntimeException(e.getTargetException());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    /**
     * Convert options to appropriate type and inject them into 
     * the command instance.
     */
    private void injectOpts(GNUishParser p, Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            final CliOption anno = f.getAnnotation(CliOption.class);
            if (anno == null) {
                continue;
            }
            String value = p.getShortOpt(anno.shortName());
            if (value == null) {
                value = p.getLongOpt(f.getName());
                if (value == null) {
                    continue;
                }
            }
            try {
                f.set(instance, c.convert(value, f.getType()));
            } catch (ConversionException e) {
                throw CliException.WRONG_OPT_TYPE(f.getName(), f.getType().getName(), value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        validateOpts(instance);
    }

    /**
     * Add or remove parameters to fit the method declaration and also convert them
     * to appropriate data type.
     */
    private List<Object> adjustArgs(List<String> args, Method m) {
        String[] defaultValues = Utils.getDefaultArgValues(m);
        final int argnum = args.size() - m.getParameterTypes().length;
        if (argnum > 0) {
            // too many, remove tail
            args = args.subList(0, argnum);
        } else if (argnum < 0) {
            // too few, add null
            int idx = defaultValues.length + argnum;
            for (int i = argnum; i < 0; i++) {
                args.add(defaultValues[idx++]);
            }
        }
        final List<Object> result = new ArrayList<Object>();
        final Class<?>[] types = m.getParameterTypes();
        for (int i = 0; i < types.length; i++) {
            try {
                result.add(c.convert(args.get(i), types[i]));
            } catch (ConversionException e) {
                throw CliException.WRONG_ARG_TYPE(getArguments().get(i).getName(),
                        types[i].getName(), args.get(i));
            }
        }
        return result;
    }

    public String toString() {
        return cmd + " " + className + " " + doc + " " + options + " " + arguments;
    }

    /**
     * Internal representation of options.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    static final class Option {
        /** short name of this option, triggered using '-' */
        @XmlAttribute(name = "short")
        private String shortName;
        /** long name of this option, triggered using '--' */
        @XmlAttribute(name = "long")
        private String longName;
        /** documentation of this option */
        @XmlElement(name = "doc")
        private String doc;

        public Option() {

        }

        public Option(String shortName, String longName, String doc) {
            this.shortName = shortName;
            this.longName = longName;
            if (doc == null) {
                this.doc = "";
            } else {
                this.doc = doc;
            }

        }

        public String getShortName() {
            return shortName;
        }

        public String getLongName() {
            return longName;
        }

        public String getDoc() {
            return doc;
        }

        public String toString() {
            return shortName + " " + longName + " " + doc;
        }

    }

    /**
     * Internal representation of arguments.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    static final class Argument {
        @XmlAttribute
        private String name;
        @XmlAttribute
        private int position;
        @XmlAttribute
        private String type;
        @XmlElement(name = "doc")
        private String doc;

        public Argument() {

        }

        public Argument(String name, String type, int position, String doc) {
            this.name = name;
            this.position = position;
            this.type = type;
            if (doc == null) {
                this.doc = "";
            } else {
                this.doc = doc;
            }

        }

        public Class<?> getType() {
            try {
                return Class.forName(type);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public String getName() {
            return name;
        }

        public String getDoc() {
            return doc;
        }

        public String toString() {
            return name + " " + position + " " + doc;
        }

    }

    /**
     * XmlCommands is responsible for converting commands to xml and back.
     */
    @XmlRootElement(name = "command-xml")
    @XmlAccessorType(XmlAccessType.FIELD)
    static final class XmlCommands {
        /** classpath location of command xml file */
        public static final String FILEPATH = "META-INF/cli/commands.xml";
        /** available commands */
        @XmlElement(name = "command")
        public List<Command> commands = new ArrayList<Command>();

        public XmlCommands() {

        }

        public XmlCommands(Map<String, Command> cmds) {
            for (Command cmd : cmds.values()) {
                this.commands.add(cmd);
            }
        }

        public List<Command> getCommands() {
            return commands;
        }

        /**
         * Convert a set of commands to xml.
         * 
         * @param cmds commands to convert
         * @param pw output to write to
         */
        public static void toXml(Map<String, Command> cmds, PrintWriter pw) {
            try {
                final XmlCommands xml = new XmlCommands(cmds);
                final JAXBContext context = JAXBContext.newInstance(XmlCommands.class);
                final Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                marshaller.marshal(xml, pw);
            } catch (PropertyException e) {
                throw new RuntimeException(e);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            } finally {
                if (pw != null) {
                    pw.flush();
                    pw.close();
                }
            }

        }

        /**
         * Create a set of commands from commands.xml.
         * 
         * @param in input stream of commands.xml.
         * @return A set of commands. 
         */
        public static List<Command> fromXml(InputStream in) {
            try {
                final JAXBContext context = JAXBContext.newInstance(XmlCommands.class);
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                final XmlCommands beans = (XmlCommands) unmarshaller.unmarshal(in);
                return beans.getCommands();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {

                    }
                }
            }
        }
    }

}
