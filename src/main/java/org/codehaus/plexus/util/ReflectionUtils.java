package org.codehaus.plexus.util;

/*
 * Copyright The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.AccessibleObject;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Operations on a class' fields and their setters.
 * 
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @author <a href="mailto:jesse@codehaus.org">Jesse McConnell</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public final class ReflectionUtils
{
    // ----------------------------------------------------------------------
    // Field utils
    // ----------------------------------------------------------------------

    public static Field getFieldByNameIncludingSuperclasses( String fieldName, Class<?> clazz  )
    {
        Field retValue = null;

        try
        {
            retValue = clazz.getDeclaredField( fieldName );
        }
        catch ( NoSuchFieldException e )
        {
            Class<?> superclass = clazz.getSuperclass();

            if ( superclass != null )
            {
                retValue = getFieldByNameIncludingSuperclasses( fieldName, superclass );
            }
        }

        return retValue;
    }

    public static List<Field> getFieldsIncludingSuperclasses( Class<?> clazz )
    {
        List<Field> fields = new ArrayList<Field>( Arrays.asList( clazz.getDeclaredFields() ) );

        Class<?> superclass = clazz.getSuperclass();

        if ( superclass != null )
        {
            fields.addAll( getFieldsIncludingSuperclasses( superclass ) );
        }

        return fields;
    }

    // ----------------------------------------------------------------------
    // Setter utils
    // ----------------------------------------------------------------------

    /**
     * Finds a setter in the given class for the given field. It searches
     * interfaces and superclasses too.
     *
     * @param fieldName the name of the field (i.e. 'fooBar'); it will search for a method named 'setFooBar'.
     * @param clazz The class to find the method in.
     * @return null or the method found.
     */
    public static Method getSetter( String fieldName, Class<?> clazz )
    {
        Method[] methods = clazz.getMethods();

        fieldName = "set" + StringUtils.capitalizeFirstLetter( fieldName );

        for ( Method method : methods )
        {
            if ( method.getName().equals( fieldName ) && isSetter( method ) )
            {
                return method;
            }
        }

        return null;
    }

    /**
     * Finds all setters in the given class and super classes.
     */
    public static List<Method> getSetters( Class<?> clazz )
    {
        Method[] methods = clazz.getMethods();

        List<Method> list = new ArrayList<Method>();

        for ( Method method : methods )
        {
            if ( isSetter( method ) )
            {
                list.add( method );
            }
        }

        return list;
    }

    /**
     * Returns the class of the argument to the setter.
     *
     * Will throw an RuntimeException if the method isn't a setter.
     */
    public static Class<?> getSetterType( Method method )
    {
        if ( !isSetter( method ) )
        {
            throw new RuntimeException( "The method " + method.getDeclaringClass().getName() + "." + method.getName() + " is not a setter." );
        }

        return method.getParameterTypes()[0];
    }

    // ----------------------------------------------------------------------
    // Value accesstors
    // ----------------------------------------------------------------------

    /**
     * attempts to set the value to the variable in the object passed in
     *
     * @param object
     * @param variable
     * @param value
     * @throws IllegalAccessException
     */
    public static void setVariableValueInObject( Object object, String variable, Object value )
        throws IllegalAccessException
    {
        Field field = getFieldByNameIncludingSuperclasses( variable, object.getClass() );

        field.setAccessible( true );

        field.set(object, value );
    }

    /**
     * Generates a map of the fields and values on a given object,
     * also pulls from superclasses
     *
     * @param object the object to generate the list of fields from
     * @return map containing the fields and their values
     */
    public static Object getValueIncludingSuperclasses( String variable, Object object )
        throws IllegalAccessException
    {

        Field field = getFieldByNameIncludingSuperclasses( variable, object.getClass() );

        field.setAccessible( true );

        return field.get( object );
    }

    /**
     * Generates a map of the fields and values on a given object,
     * also pulls from superclasses
     *
     * @param object the object to generate the list of fields from
     * @return map containing the fields and their values
     */
    public static Map getVariablesAndValuesIncludingSuperclasses( Object object )
        throws IllegalAccessException
    {
        HashMap map = new HashMap();

        gatherVariablesAndValuesIncludingSuperclasses( object, map );

        return map;
    }

    // ----------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------

    public static boolean isSetter( Method method )
    {
        return method.getReturnType().equals( Void.TYPE ) && // FIXME: needed /required?
               !Modifier.isStatic( method.getModifiers() ) &&
               method.getParameterTypes().length == 1;
    }

    /**
     * populates a map of the fields and values on a given object,
     * also pulls from superclasses
     *
     * @param object the object to generate the list of fields from
     * @param map to populate
     */
    private static void gatherVariablesAndValuesIncludingSuperclasses( Object object, Map map )
        throws IllegalAccessException
    {

        Class<?> clazz = object.getClass();

        Field[] fields = clazz.getDeclaredFields();

        AccessibleObject.setAccessible( fields, true);

        for (int i = 0; i < fields.length; ++i)
        {
            Field field = fields[i];

            map.put( field.getName(), field.get( object ) );

        }

        Class<?> superclass = clazz.getSuperclass();

        if ( !Object.class.equals(  superclass ) )
        {
            gatherVariablesAndValuesIncludingSuperclasses( superclass, map );
        }
    }
}
