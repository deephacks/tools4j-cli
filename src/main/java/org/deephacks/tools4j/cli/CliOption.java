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
 * CliOption is responsible for adding an option to specific command(s).
 * 
 * At command line, options begin with a hyphen delimiter (‘-’).
 * 
 * Multiple options may follow a hyphen delimiter in a single token if the options 
 * do not take arguments. Thus, ‘-abc’ is equivalent to ‘-a -b -c’.
 * 
 * Options can have arguments. Argument-less options must only be annotated 
 * on variables of type Boolean.  
 *
 * The long name is the name of the parameter it annotates. It is activated using 
 * double dash "--". So "provided" is activated by typing "--provided".  
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Inherited
public @interface CliOption {
    /**
     * The short name of the parameter is used if name is provided through a
     * single dash "-" So a shortName of "p" is provided as "-p"
     * 
     * The longName will default to name of the parameter.
     * 
     * @return The shorter name of the parameter.
     */
    String shortName();

}
