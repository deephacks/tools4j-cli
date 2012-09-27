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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used on methods to define it as a command, such as
 * ls, cat, mv, cp, fsck and so on. The command will have the same name as 
 * the method. Any arguments the method define will be treated as command
 * arguments and they will be given to the method from left to right when
 * the command is executed. 
 * 
 * A class can have multiple methods annotated with this annotation.
 * 
 * Method arguments can be annotated with JSR303 1.1 Bean Validation annotations,
 * which will be enforced as long as api and implementation is available on 
 * classpath.
 * 
 * The method javadoc will be used whenever the user request --help
 * from the command and the arguments javadoc for the command arguments.    
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Inherited
public @interface CliCmd {

}
