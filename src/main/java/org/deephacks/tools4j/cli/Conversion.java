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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.deephacks.tools4j.cli.Conversion.Converter.ObjectToStringConverter;
import org.deephacks.tools4j.cli.Conversion.Converter.StringToBooleanConverter;
import org.deephacks.tools4j.cli.Conversion.Converter.StringToEnumConverter;
import org.deephacks.tools4j.cli.Conversion.Converter.StringToNumberConverter;
import org.deephacks.tools4j.cli.Conversion.Converter.StringToObjectConverter;

/**
 * Conversion is responsible for converting values using registered converters.
 * 
 * Inspiration from http://www.springsource.org
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class Conversion {
    /** Keeper for converters available. */
    private final HashMap<Class<?>, SourceTargetPair> converters = new HashMap<Class<?>, SourceTargetPair>();
    /** Lookup cache for finding converters. */
    private final ConcurrentHashMap<SourceTargetPairKey, Converter> cache = new ConcurrentHashMap<SourceTargetPairKey, Converter>();

    private static Conversion INSTANCE;

    private Conversion() {
        registerDefault();
    }

    public static synchronized Conversion get() {
        if (INSTANCE == null) {
            INSTANCE = new Conversion();

        }
        return INSTANCE;
    }

    /**
     * Convert a value to a specific class. 
     * 
     * The algorithm for finding a suitable converter is as follows: 
     * 
     * Find converters that is able to convert both source and target; a exact or 
     * superclass match. Pick the converter that have the best target match, if both
     * are equal, pick the one with best source match.  
     * 
     * That is, the converter that is most specialized in converting a value to 
     * a specific target class will be prioritized, as long as it recognizes the source 
     * value. 
     * 
     * @param source value to convert.
     * @param targetclass class to convert to.
     * @return converted value
     */
    public <T> T convert(final Object source, final Class<T> targetclass) {
        if (source == null) {
            return null;
        }

        final Class<?> sourceclass = source.getClass();
        final SourceTargetPairKey key = new SourceTargetPairKey(sourceclass, targetclass);
        Converter converter = cache.get(key);

        if (converter != null) {
            return (T) converter.convert(source, targetclass);
        }
        final LinkedList<SourceTargetPairMatch> matches = new LinkedList<SourceTargetPairMatch>();
        for (SourceTargetPair pair : converters.values()) {
            SourceTargetPairMatch match = pair.match(sourceclass, targetclass);
            if (match.matchesSource() && match.matchesTarget()) {
                matches.add(match);
            }
        }
        if (matches.size() == 0) {
            throw new ConversionException("No suitable converter found for target class ["
                    + targetclass.getName() + "] and source value [" + sourceclass.getName()
                    + "]. The following converters are available [" + converters.keySet() + "]");
        }

        Collections.sort(matches, SourceTargetPairMatch.bestTargetMatch());
        converter = matches.get(0).pair.converter;
        cache.put(key, converter);
        return (T) converter.convert(source, targetclass);

    }

    public <T, V> Collection<T> convert(Collection<V> values, final Class<T> clazz) {
        final ArrayList<T> objects = new ArrayList<T>();
        if (values == null) {
            return new ArrayList<T>();
        }
        for (V object : values) {
            objects.add(convert(object, clazz));
        }
        return objects;
    }

    public <T, V> Map<V, T> convert(Map<V, Object> values, final Class<T> clazz) {
        if (values == null) {
            return null;
        }
        throw new UnsupportedOperationException();
    }

    public <T, V> void register(Converter converter) {
        if (converters.get(converter.getClass()) != null) {
            return;
        }
        converters.put(converter.getClass(), new SourceTargetPair(converter));
        cache.clear();

    }

    private void registerDefault() {
        register(new StringToEnumConverter());
        register(new StringToObjectConverter());
        register(new ObjectToStringConverter());
        register(new StringToNumberConverter());
        register(new StringToBooleanConverter());
    }

    private static class SourceTargetPair {
        private final Class<?> source;
        private final Class<?> target;
        private final Converter converter;

        public SourceTargetPair(Converter converter) {
            List<Class<?>> types = getParameterizedType(converter.getClass(), Converter.class);
            if (types.size() < 2) {
                throw new IllegalArgumentException(
                        "Unable to the determine generic source and target type "
                                + "for converter. Please declare these generic types.");
            }
            this.source = types.get(0);
            this.target = types.get(1);
            this.converter = converter;
        }

        public SourceTargetPairMatch match(Class<?> sourceValueClass, Class<?> targetClass) {
            return new SourceTargetPairMatch(this, getSourceMatchDistance(sourceValueClass),
                    getTargetMatchDistance(targetClass));
        }

        /**
         * Returns a list of classes that matches the candidate in terms 
         * of converter source. The list is sorted with the most specific match first. 
         */
        private int getSourceMatchDistance(Class<?> candidate) {
            return distance(candidate, source);
        }

        /**
         * Returns a list of classes that matches the candidate in terms 
         * of converter target. The list is sorted with the most specific match first. 
         */
        private int getTargetMatchDistance(Class<?> candidate) {
            return distance(candidate, target);
        }

        /**
         * Climb the class hierarchy of the candidate class and calculate the distance 
         * between to the capability class. 
         * 
         * @return The distance in the class hierarchy between the candidate and capability.
         */
        private int distance(Class<?> candidate, Class<?> capability) {
            int distance = 0;
            if (candidate == capability) {
                return distance;
            }
            final LinkedList<Class<?>> superclasses = new LinkedList<Class<?>>();
            superclasses.add(candidate.getSuperclass());
            while (!superclasses.isEmpty()) {
                Class<?> candidateSuperclazz = superclasses.removeLast();
                if (candidateSuperclazz == null) {
                    // Object converters are absolute last resort
                    return Integer.MAX_VALUE;
                }
                if (candidateSuperclazz.equals(capability)) {
                    if (capability == Object.class) {
                        // Object converters are absolute last resort
                        return Integer.MAX_VALUE;
                    }
                    return ++distance;
                }
                addInterfaces(candidateSuperclazz, superclasses);
                if (candidateSuperclazz.getSuperclass() != null) {
                    superclasses.add(candidateSuperclazz.getSuperclass());
                }
            }
            // no match
            return -1;
        }

        private void addInterfaces(Class<?> clazz, LinkedList<Class<?>> superclasses) {
            for (Class<?> inheritedIfc : clazz.getInterfaces()) {
                addInterfaces(inheritedIfc, superclasses);
            }
        }
    }

    private static class SourceTargetPairMatch {
        private int bestTargetMatch = -1;
        private int bestSourceMatch = -1;
        private final SourceTargetPair pair;

        public SourceTargetPairMatch(SourceTargetPair pair, int bestSourceMatch, int bestTargetMatch) {
            this.pair = pair;
            this.bestSourceMatch = bestSourceMatch;
            this.bestTargetMatch = bestTargetMatch;
        }

        public boolean matchesTarget() {
            return (bestTargetMatch > -1 ? true : false);
        }

        public boolean matchesSource() {
            return (bestSourceMatch > -1 ? true : false);
        }

        public static Comparator<SourceTargetPairMatch> bestTargetMatch() {
            return new Comparator<Conversion.SourceTargetPairMatch>() {

                @Override
                public int compare(SourceTargetPairMatch o1, SourceTargetPairMatch o2) {
                    if (o1.bestTargetMatch < o2.bestTargetMatch) {
                        return -1;
                    } else if (o1.bestTargetMatch > o2.bestTargetMatch) {
                        return 1;
                    }
                    // equal target, pick best source.
                    if (o1.bestSourceMatch < o2.bestSourceMatch) {
                        return -1;
                    } else if (o1.bestSourceMatch > o2.bestSourceMatch) {
                        return 1;
                    }
                    return 0;
                }
            };
        }
    }

    private static class SourceTargetPairKey {
        final Class<?> source;
        final Class<?> target;

        public SourceTargetPairKey(Class<?> source, Class<?> target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((source == null) ? 0 : source.hashCode());
            result = prime * result + ((target == null) ? 0 : target.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SourceTargetPairKey other = (SourceTargetPairKey) obj;
            if (source == null) {
                if (other.source != null)
                    return false;
            } else if (!source.equals(other.source))
                return false;
            if (target == null) {
                if (other.target != null)
                    return false;
            } else if (!target.equals(other.target))
                return false;
            return true;
        }

    }

    /**
     * Returns the parameterized type of a class, if exists. Wild cards, type
     * variables and raw types will be returned as an empty list.
     * <p>
     * If a field is of type Set<String> then java.lang.String is returned.
     * </p>
     * <p>
     * If a field is of type Map<String, Integer> then [java.lang.String,
     * java.lang.Integer] is returned.
     * </p>
     * 
     * @param ownerClass the implementing target class to check against
     * @param the generic interface to resolve the type argument from
     * @return A list of classes of the parameterized type.
     */
    public static List<Class<?>> getParameterizedType(final Class<?> ownerClass,
            Class<?> genericSuperClass) {
        Type[] types = null;
        if (genericSuperClass.isInterface()) {
            types = ownerClass.getGenericInterfaces();
        } else {
            types = new Type[] { ownerClass.getGenericSuperclass() };
        }

        final List<Class<?>> classes = new ArrayList<Class<?>>();
        for (Type type : types) {

            if (!ParameterizedType.class.isAssignableFrom(type.getClass())) {
                // the field is it a raw type and does not have generic type
                // argument. Return empty list.
                return new ArrayList<Class<?>>();
            }

            final ParameterizedType ptype = (ParameterizedType) type;
            final Type[] targs = ptype.getActualTypeArguments();

            for (Type aType : targs) {

                classes.add(extractClass(ownerClass, aType));
            }
        }
        return classes;
    }

    private static Class<?> extractClass(Class<?> ownerClass, Type arg) {
        if (arg instanceof ParameterizedType) {
            return extractClass(ownerClass, ((ParameterizedType) arg).getRawType());
        } else if (arg instanceof GenericArrayType) {
            throw new UnsupportedOperationException("GenericArray types are not supported.");
        } else if (arg instanceof TypeVariable) {
            throw new UnsupportedOperationException("GenericArray types are not supported.");
        }
        return (arg instanceof Class ? (Class<?>) arg : Object.class);
    }

    static class ConversionException extends RuntimeException {
        private static final long serialVersionUID = 3116958531528669531L;

        public ConversionException(String msg) {
            super(msg);
        }

        public ConversionException(Throwable e) {
            super(e);
        }

        public ConversionException(String msg, Exception e) {
            super(msg, e);
        }
    }

    /**
     * Converts a object of type V to a object of type T.
     * 
     * Both V and T can be a super class or interface that handles a range of 
     * subclasses.
     * 
     * The algorithm for finding a suitable converter begins by first finding 
     * converters that are able to convert both source and target; a exact or 
     * superclass match. The final decision falls on the converter that have 
     * the best target match.  
     * 
     * That is, the converter that is most specialized in converting a value T to 
     * a specific target class will be prioritized, as long as it recognizes 
     * the source value V. 
     * 
     * Converter providers are regsitered using the standard java service provider 
     * mechanism.
     */
    interface Converter<V, T> {
        /**
         * @param source The source value to convert.
         * @param the most specific type that the value should be converted to.  
         * @return A converted object.
         */
        public T convert(V source, Class<? extends T> specificType);

        /**
         * This is the fallback string converter that simply does a toString on the
         * provided object.
         * 
         * Works fine for Number, Boolean, Enums and all other values that have a 
         * toString that represent their real values in a serialized form.
         */
        static final class ObjectToStringConverter implements Converter<Object, String> {

            @Override
            public String convert(Object source, Class<? extends String> specificType) {
                return (source != null ? source.toString() : null);
            }

        }

        static final class StringToBooleanConverter implements Converter<String, Boolean> {
            private static final Set<String> trueValues = new HashSet<String>();
            private static final Set<String> falseValues = new HashSet<String>();

            static {
                trueValues.addAll(Arrays.asList("true", "on", "yes", "y", "1"));
                falseValues.addAll(Arrays.asList("false", "off", "no", "n", "0"));
            }

            @Override
            public Boolean convert(String source, Class<? extends Boolean> specificType) {
                final String value = source.trim();
                if (trueValues.contains(value)) {
                    return Boolean.TRUE;
                } else if (falseValues.contains(value)) {
                    return Boolean.FALSE;
                } else {
                    throw new ConversionException("Invalid boolean value '" + source + "'");
                }
            }

        }

        /**
         * This class can convert any enum to a string.
         */
        static final class StringToEnumConverter implements Converter<String, Enum> {

            @Override
            public Enum convert(String source, Class<? extends Enum> specificType) {
                try {
                    return Enum.valueOf(specificType, source);
                } catch (IllegalArgumentException e) {
                    throw new ConversionException("Could not convert value [" + source
                            + "] to any of the possible values:  "
                            + getPossibleValueString(specificType) + ".");
                }
            }

            public String getPossibleValueString(Class<?> clazz) {
                StringBuffer sb = new StringBuffer();
                Field[] fields = clazz.getDeclaredFields();
                List<String> values = new ArrayList<String>();
                for (int i = 0; i < fields.length; i++) {
                    if (fields[i].isEnumConstant()) {
                        try {
                            Object aEnum = fields[i].get(null);
                            values.add(aEnum.toString());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                for (int i = 0; i < values.size(); i++) {
                    sb.append(values.get(i));
                    if ((i + 1) != values.size()) {
                        sb.append(", ");
                    }
                }
                return sb.toString();
            }

        }

        /**
         * This class can convert all number types such as BigDecimal, BigInteger, Byte, Double, 
         * Float, Integer, Long, and Short.
         */
        static final class StringToNumberConverter implements Converter<String, Number> {

            @Override
            public Number convert(String source, Class<? extends Number> specificType) {
                final String value = source.trim();
                try {
                    if (specificType.equals(Byte.class)) {
                        return Byte.valueOf(value);
                    } else if (specificType.equals(Short.class)) {
                        return Short.valueOf(value);
                    } else if (specificType.equals(Integer.class)) {
                        return Integer.valueOf(value);
                    } else if (specificType.equals(Long.class)) {
                        return Long.valueOf(value);
                    } else if (specificType.equals(BigInteger.class)) {
                        return new BigInteger(value);
                    } else if (specificType.equals(Float.class)) {
                        return Float.valueOf(value);
                    } else if (specificType.equals(Double.class)) {
                        return Double.valueOf(value);
                    } else if (specificType.equals(BigDecimal.class)
                            || specificType.equals(Number.class)) {
                        return new BigDecimal(value);
                    }
                    throw new ConversionException("Cannot convert [" + source + "] to ["
                            + specificType.getName() + "]");

                } catch (NumberFormatException e) {
                    throw new ConversionException("Cannot convert [" + source + "] to ["
                            + specificType.getName() + "]", e);
                }

            }

        }

        /**
         * General purpose converter that is able to convert a String to an object if the 
         * object have a suitable static valueof method or a single argument String constructor.
         * 
         * This should work fine for File, URL, DateTime, DurationTime
         */
        static final class StringToObjectConverter implements Converter<String, Object> {

            @Override
            public Object convert(String source, Class<? extends Object> specificType) {
                final Method valueof = getStaticMethod(specificType, "valueof", String.class);

                try {
                    if (valueof != null) {
                        valueof.setAccessible(true);
                        return valueof.invoke(null, source);
                    }
                    final Constructor<?> cons = getConstructor(specificType, String.class);
                    if (cons != null) {
                        cons.setAccessible(true);
                        return cons.newInstance(source);
                    }
                } catch (InvocationTargetException e) {
                    throw new ConversionException(e.getTargetException());
                } catch (Throwable e) {
                    throw new ConversionException(e);
                }
                throw new UnsupportedOperationException(
                        "No static valueOf(String.class) method or Constructor(String.class) exists on "
                                + specificType.getName());
            }

            public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... paramTypes) {
                try {
                    return clazz.getConstructor(paramTypes);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }

            public static Method getStaticMethod(Class<?> clazz, String methodName,
                    Class<?>... args) {
                try {
                    final Method method = clazz.getMethod(methodName, args);
                    return Modifier.isStatic(method.getModifiers()) ? method : null;
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }

        }

    }
}
