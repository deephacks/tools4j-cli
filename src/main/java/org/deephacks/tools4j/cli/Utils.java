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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.deephacks.tools4j.cli.Command.Argument;
import org.deephacks.tools4j.cli.Command.Option;

/**
 * Utility code are kept here to avoid distorting readability of other 
 * classes with code that is irrelevant to their domain.
 */
final class Utils {
    /** name of the validator class */
    public static final String VALIDATOR_CLASSNAME = "org.deephacks.tools4j.cli.Validator";
    /** API class for JSR 303 1.0 bean validation */
    public static final String JSR303_1_0_CLASSNAME = "javax.validation.Validation";
    /** API class for JSR 303 1.1 bean validation, class only exist in 1.1 */
    public static final String JSR303_1_1_CLASSNAME = "javax.validation.metadata.MethodDescriptor";

    /** the instance of the Validator object */
    private static Object validator;
    /** new line character */
    public static final String NEWLINE = System.getProperty("line.separator");
    static final String AVAILABLE_CMDS_MSG = "Available commands are:";

    static Object newInstance(String className) {
        try {
            Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(className);
            Class<?> enclosing = type.getEnclosingClass();
            if (enclosing == null) {
                Constructor<?> c = type.getDeclaredConstructor();
                c.setAccessible(true);
                return type.cast(c.newInstance());
            }
            Object o = enclosing.newInstance();
            Constructor<?> cc = type.getDeclaredConstructor(enclosing);
            cc.setAccessible(true);
            return type.cast(cc.newInstance(o));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    static String stripTrailingWhitespace(String str) {
        if (str == null || "".equals(str.trim())) {
            return "";
        }
        StringBuffer sb = new StringBuffer(str);
        int pos = str.length() - 1;
        while (true) {
            if (Character.isWhitespace(sb.charAt(pos))) {
                sb.deleteCharAt(pos--);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Validate that the method parameters if Bean Validation 1.1 is available 
     * on classpath.
     */
    static void validateArgs(List<Object> args, Object instance, Method m, Command cmd) {
        if (!onClasspath(JSR303_1_1_CLASSNAME)) {
            return;
        }
        try {
            Object validator = getValidator();
            Method validate = validator.getClass().getMethod("validateArgs", List.class,
                    Object.class, Method.class, Command.class);
            validate.invoke(validator, args, instance, m, cmd);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validate that the options if Bean Validation is available on classpath.
     */
    static void validateOpts(Object instance) {
        if (!onClasspath(JSR303_1_0_CLASSNAME)) {
            return;
        }
        try {
            Object validator = getValidator();
            Method validate = validator.getClass().getMethod("validateOpts", Object.class);
            validate.invoke(validator, instance);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            throw new RuntimeException(e.getTargetException());
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getValidator() throws InstantiationException, IllegalAccessException,
            ClassNotFoundException {
        if (validator == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            validator = cl.loadClass(VALIDATOR_CLASSNAME).newInstance();

        }
        return validator;
    }

    /**
     * Checks to see if JSR303 implementation is 
     * available on classpath.
     */
    static boolean onClasspath(String className) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    static String[] getDefaultArgValues(Method m) {
        String[] defaultValues = new String[m.getParameterTypes().length];
        Annotation[][] a = m.getParameterAnnotations();
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                if (a[i][j].annotationType() == Default.class) {
                    Default def = (Default) a[i][j];
                    defaultValues[i] = def.value();
                }
            }
        }
        return defaultValues;
    }

    public static HashMap<String, String> parseParamsJavadoc(String javadoc) {
        if (javadoc == null || "".equals(javadoc.trim())) {
            return new HashMap<String, String>();
        }
        final HashMap<String, String> params = new HashMap<String, String>();
        final StringBuffer sb = new StringBuffer(javadoc.trim());
        int pos;
        while ((pos = sb.indexOf("@param")) > -1) {
            sb.delete(0, pos + 1);
            int end = sb.indexOf(" ");
            sb.delete(0, end + 1);
            end = sb.indexOf(" ");
            String param = sb.substring(0, end);
            sb.delete(0, end + 1);
            int next = sb.indexOf("@");
            if (next == -1) {
                next = sb.length();
            }
            params.put(param, Utils.stripTrailingWhitespace(sb.substring(0, next)));
            sb.delete(0, next);
        }
        return params;
    }

    public static String parseJavadoc(String javadoc) {
        if (javadoc == null || "".equals(javadoc.trim())) {
            return "";
        }
        javadoc = javadoc.trim();
        int pos = javadoc.indexOf('@');
        if (pos < 0) {
            return javadoc;
        }
        javadoc = javadoc.substring(0, pos);
        return Utils.stripTrailingWhitespace(javadoc);
    }

    public static void printAvailableCommandsHelp(Map<String, Command> commands) {
        StringBuilder sb = new StringBuilder();
        sb.append(AVAILABLE_CMDS_MSG).append(NEWLINE).append(NEWLINE);
        int maxlength = getMaxCmdLength(commands);
        for (Command cmd : commands.values()) {
            StringBuilder sentence = new StringBuilder();
            for (char c : cmd.getDoc().toCharArray()) {
                sentence.append(c);
                if (c == '.') {
                    break;
                }
            }
            sb.append(String.format(" %-" + maxlength + "s : %s %n", cmd.getCommand(), sentence));
        }
        sb.append(NEWLINE).append(" Try `[command] --help' for more information.");
        System.out.println(sb.toString());
    }

    public static void printCommandHelp(Command cmd) {
        StringBuilder sb = new StringBuilder();

        sb.append("usage: ").append(cmd.getCommand());
        if (cmd.getOptions().size() > 0) {
            sb.append(" [OPTION]... ");
        }

        for (Argument arg : cmd.getArguments()) {
            sb.append(arg.getName()).append(" ");
        }
        int optlength = getMaxOptLength(cmd.getOptions());

        sb.append(NEWLINE).append(NEWLINE);
        sb.append(" ").append(cmd.getDoc()).append(NEWLINE).append(NEWLINE);
        sb.append("OPTIONS").append(NEWLINE).append(NEWLINE);
        for (Option opt : cmd.getOptions()) {
            StringBuilder optstr = new StringBuilder();
            optstr.append(" ");
            optstr.append("-").append(opt.getShortName()).append(",");
            optstr.append("--").append(opt.getLongName());
            sb.append(String.format(" %-" + (optlength + 3) + "s : ", optstr.toString()));

            List<String> lines = splitAndIndent(opt.getDoc(), optlength + 6);
            for (String line : lines) {
                sb.append(line).append(NEWLINE);
            }
            sb.append(NEWLINE);
        }

        if (cmd.getArguments().size() > 0) {
            sb.append("ARGUMENTS").append(NEWLINE);
        }
        int arglength = getMaxArgLength(cmd.getArguments());
        sb.append(NEWLINE);
        for (Argument arg : cmd.getArguments()) {
            sb.append(String.format(" %-" + arglength + "s : ", arg.getName()));
            List<String> lines = splitAndIndent(arg.getDoc(), arglength + 3);
            for (String line : lines) {
                sb.append(line).append(NEWLINE);
            }
            sb.append(NEWLINE);
        }

        System.out.println(sb.toString());
    }

    private static List<String> splitAndIndent(String str, int indentLength) {
        String[] split = str.split(System.getProperty("line.separator"));
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < split.length; i++) {
            if (i == 0) {
                // dont indent first line, it already is
                lines.add(split[i]);
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < indentLength; j++) {
                sb.append(' ');
            }
            sb.append(split[i]);
            lines.add(sb.toString());
        }
        return lines;
    }

    private static int getMaxCmdLength(Map<String, Command> cmds) {
        int length = 0;
        for (Command cmd : cmds.values()) {
            int l = cmd.getCommand().length();
            if (l > length) {
                length = l;
            }
        }
        return length;
    }

    private static int getMaxArgLength(List<Argument> args) {
        int length = 0;
        for (Argument arg : args) {
            int l = arg.getName().length();
            if (l > length) {
                length = l;
            }
        }
        return length;
    }

    private static int getMaxOptLength(List<Option> args) {
        int length = 0;
        for (Option opt : args) {
            int l = opt.getLongName().length() + opt.getShortName().length() + 3;
            if (l > length) {
                length = l;
            }
        }
        return length;
    }
}
