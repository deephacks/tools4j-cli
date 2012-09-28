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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.ValidationException;

/**
 * Validator perform JSR303 bean validation on options and method arguments.
 */
final class Validator {
    static final String ARG_VIOLATION_MSG = "Argument validation failed";
    static final String OPT_VIOLATION_MSG = "Options validation failed";
    private static final javax.validation.Validator validator = Validation
            .buildDefaultValidatorFactory().getValidator();

    /**
     * This method will only be called if we know that JSR 303 1.0 Bean Validation API 
     * and compliant implementation are available on classpath. 
     */
    public void validateOpts(Object instance) {
        final Set<ConstraintViolation<Object>> set = validator.validate(instance);
        final StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<Object> violation : set) {
            final Path path = violation.getPropertyPath();
            final String msg = violation.getMessage();
            sb.append(path.toString()).append(" ").append(msg).append(" ");
        }
        if (sb.length() > 0) {
            // is ConstraintViolationException more appropriate,
            // letting user choose their error message?
            throw new ValidationException(OPT_VIOLATION_MSG + ", " + sb.toString());
        }
    }

    /**
     * This method will only be called if we know that JSR 303 1.1 Bean Validation API 
     * and compliant implementation are available on classpath. 
     * 
     * Method validation was first introduced in version 1.1
     */
    public void validateArgs(List<Object> args, Object instance, Method m) {
        final Set<ConstraintViolation<Object>> set = validator.validateParameters(instance, m,
                args.toArray());
        final StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<Object> violation : set) {
            final Path path = violation.getPropertyPath();
            final String msg = violation.getMessage();
            sb.append(path.toString()).append(" ").append(msg).append(" ");
        }
        if (sb.length() > 0) {
            // is ConstraintViolationException more appropriate,
            // letting user choose their error message?
            throw new ValidationException(ARG_VIOLATION_MSG + ", " + sb.toString());
        }
    }
}
