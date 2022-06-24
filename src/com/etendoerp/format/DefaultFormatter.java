/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.etendoerp.format;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.text.ParseException;

/**
 * <code>DefaultFormatter</code> formats arbitrary objects. Formatting is done
 * by invoking the <code>toString</code> method. In order to convert the
 * value back to a String, your class must provide a constructor that
 * takes a String argument. If no single argument constructor that takes a
 * String is found, the returned value will be the String passed into
 * <code>stringToValue</code>.
 * <p>
 * This class is taken from Java Swing and modified to remove dependencies to Swing classes.
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultFormatter implements Serializable {
    /** Indicates if the value being edited must match the mask. */
    private boolean allowsInvalid;

    /** Class used to create new instances. */
    private Class<?> valueClass;


    /**
     * Creates a DefaultFormatter.
     */
    public DefaultFormatter() {
        allowsInvalid = true;
    }

    /**
     * Sets whether or not the value being edited is allowed to be invalid
     * for a length of time (that is, <code>stringToValue</code> throws
     * a <code>ParseException</code>).
     * It is often convenient to allow the user to temporarily input an
     * invalid value.
     *
     * @param allowsInvalid Used to indicate if the edited value must always
     *        be valid
     */
    public void setAllowsInvalid(boolean allowsInvalid) {
        this.allowsInvalid = allowsInvalid;
    }

    /**
     * Returns whether or not the value being edited is allowed to be invalid
     * for a length of time.
     *
     * @return false if the edited value must always be valid
     */
    public boolean getAllowsInvalid() {
        return allowsInvalid;
    }

    /**
     * Sets that class that is used to create new Objects. If the
     * passed in class does not have a single argument constructor that
     * takes a String, String values will be used.
     *
     * @param valueClass Class used to construct return value from
     *        stringToValue
     */
    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    /**
     * Returns that class that is used to create new Objects.
     *
     * @return Class used to construct return value from stringToValue
     */
    public Class<?> getValueClass() {
        return valueClass;
    }

    /**
     * Converts the passed in String into an instance of
     * <code>getValueClass</code> by way of the constructor that
     * takes a String argument. If <code>getValueClass</code>
     * returns null, the Class of the current value in the
     * <code>JFormattedTextField</code> will be used. If this is null, a
     * String will be returned. If the constructor throws an exception, a
     * <code>ParseException</code> will be thrown. If there is no single
     * argument String constructor, <code>string</code> will be returned.
     *
     * @throws ParseException if there is an error in the conversion
     * @param string String to convert
     * @return Object representation of text
     */
    public Object stringToValue(String string) throws ParseException {
        Class<?> vc = getValueClass();
        if (vc != null) {
            Constructor<?> cons;

            try {
                cons = vc.getConstructor(String.class);

            } catch (NoSuchMethodException nsme) {
                cons = null;
            }

            if (cons != null) {
                try {
                    return cons.newInstance(string);
                } catch (Exception ex) {
                    throw new ParseException("Error creating instance", 0);
                }
            }
        }
        return string;
    }

    /**
     * Converts the passed in Object into a String by way of the
     * <code>toString</code> method.
     *
     * @throws ParseException if there is an error in the conversion
     * @param value Value to convert
     * @return String representation of value
     */
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
