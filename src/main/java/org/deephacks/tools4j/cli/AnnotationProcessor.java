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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.deephacks.tools4j.cli.Command.Argument;
import org.deephacks.tools4j.cli.Command.Option;
import org.deephacks.tools4j.cli.Command.XmlCommands;

/**
 * AnnotationProcessor is responsible for producing a command.xml to remove this
 * burden from the developer. It will look at the javadoc for command methods and
 * their arguments, also the class parameters (options). 
 * 
 * This information will be used to display a command help screen whenever the user 
 * requests it, including a description of all arguments and options.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("org.deephacks.tools4j.cli.CliCmd")
public final class AnnotationProcessor extends AbstractProcessor {
    private Map<String, Command> commands = new HashMap<String, Command>();

    public AnnotationProcessor() {
        super();
    }

    public @Override
    final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.errorRaised()) {
            debug("roundEnv.errorRaised(true)");
            return false;
        }
        if (roundEnv.processingOver()) {
            writeFiles();
            debug("writeFiles()");
            return true;
        } else {
            debug("handleProcess()");
            return handleProcess(annotations, roundEnv);
        }

    }

    protected boolean handleProcess(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        debug("ServiceProviderAnnotationProcessor");

        for (TypeElement type : ElementFilter.typesIn(roundEnv.getRootElements())) {

            for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
                CliCmd cc = method.getAnnotation(CliCmd.class);
                if (cc == null) {
                    continue;
                }
                final String javadoc = processingEnv.getElementUtils().getDocComment(method);
                final String methodjavadoc = Utils.parseJavadoc(javadoc);
                final Command cmd = new Command(method.getSimpleName().toString(), type
                        .getQualifiedName().toString(), methodjavadoc);
                final HashMap<String, String> paramjavadoc = Utils.parseParamsJavadoc(javadoc);
                int pos = 0;
                for (VariableElement e : method.getParameters()) {
                    final String var = e.getSimpleName().toString();

                    cmd.addArgument(new Argument(var, e.asType().toString(), pos++, paramjavadoc
                            .get(var)));
                }
                commands.put(cmd.getCommand(), cmd);
                for (VariableElement var : ElementFilter.fieldsIn(type.getEnclosedElements())) {
                    final String varjavadoc = Utils.parseJavadoc(processingEnv.getElementUtils()
                            .getDocComment(var));
                    CliOption anno = var.getAnnotation(CliOption.class);
                    if (anno == null) {
                        continue;
                    }
                    final String shortName = anno.shortName();
                    final Option opt = new Option(shortName, var.getSimpleName().toString(),
                            varjavadoc);
                    cmd.addOptions(opt);
                }
            }

        }
        return false;
    }

    private void writeFiles() {
        try {
            FileObject file = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", XmlCommands.FILEPATH, (Element[]) null);
            PrintWriter pw = new PrintWriter(file.openWriter());
            XmlCommands.toXml(commands, pw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a debug statement to a log file. 
     * 
     * @param msg Log file.
     */
    public static void debug(String msg) {
        //        PrintWriter w;
        //        try {
        //            w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(
        //                    "annotation_debug_log.txt"), true), "UTF-8"));
        //            w.write(msg);
        //            w.write("\r\n");
        //            w.flush();
        //            w.close();
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }
    }
}
