/*
 * 
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * The contents of this file are subject to the Sun Community Source
 * License v3.0/Jini Technology Specific Attachment v 1.0 (the
 * "License"). You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://wwws.sun.com/software/jini/licensing/SCSL3_JiniTSA1.html.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Reference Code is Jini Technology Core Platform code, v2.0. The
 * Developer of the Reference Code is Sun Microsystems, Inc.
 * 
 * Contributor(s): Sun Microsystems, Inc.
 * 
 * The contents of this file comply with the Jini Technology Core
 * Platform Compatibility Kit, v2.0A.
 * 
 * Tester(s): Sun Microsystems, Inc.
 * 
 * Test Platform(s):
 * Java 2 SDK, Standard Edition, Version 1.4.2_03 for Solaris(TM)SPARC/x86
 * Java 2 SDK, Standard Edition, Version 1.4.2_03 for Linux (Intel x86)
 * Java 2 SDK, Standard Edition, Version 1.4.2_03 for Windows (Intel Platform)
 * 
 */
package clifton.nodes;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.logging.Logger;



/** 
 * This class provides useful utilities for creating and
 * manipulating class loaders.
 *
 * @author Sun Microsystems, Inc.
 *
 */
public class ClassLoaderUtil {
    /** Configure logger */
    static final Logger logger = 
        Logger.getLogger("com.sun.jini.start.ClassLoaderUtil");
    
    /**
     * Utility method that converts the components of a <code>String</code>
     * representing a classpath into file <code>URL</code>(s).
     *
     * @param classpath <code>String</code> containing components separated
     *                  by path separators that represent the components
     *                  making up a classpath
     *
     * @return a <code>URL[]</code> where 
     *         each element of the array corresponds to one of the components
     *         in the <code>classpath</code> parameter
     *                       
     * @throws java.net.MalformedURLException 
     */
    public static URL[] getClasspathURLs(String classpath)
                                                 throws MalformedURLException
    {
        StringTokenizer st = new StringTokenizer(classpath,File.pathSeparator);
        URL[] urls = new URL[st.countTokens()];
        for (int i=0; st.hasMoreTokens(); i++) {
            urls[i] = new File(st.nextToken()).toURL();
        }
        return urls;
    }//end getClasspathURLs


    /**
     * Utility method that converts the components of a <code>String</code>
     * representing a codebase into standard <code>URL</code>(s).
     *
     * @param codebase  <code>String</code> containing components separated
     *                  by spaces in which each component is in 
     *                  <code>URL</code> format.
     *
     * @return a <code>URL[]</code> where
     *         each element of the array corresponds to one of the components
     *         in the <code>codebase</code> parameter
     *                       
     * @throws java.net.MalformedURLException 
     */
    public static URL[] getCodebaseURLs(String codebase)
                                                 throws MalformedURLException
    {
        StringTokenizer st = new StringTokenizer(codebase);
        URL[] urls = new URL[st.countTokens()];
        for (int i=0; st.hasMoreTokens(); i++) {
            urls[i] = new URL(st.nextToken());
        }
        return urls;
    }//end getCodebaseURLs

    /**
     * Utility method that retrieves the components making up the class loader
     * delegation tree for the current context class loader and returns each
     * in an <code>ArrayList</code>.
     *
     * @return an <code>ArrayList</code> instance in which each element of the
     *         list is one of the components making up the current delegation
     *         tree.
     */
    private static ArrayList getContextClassLoaderTree() {
        Thread curThread = Thread.currentThread();
        ClassLoader curClassLoader = curThread.getContextClassLoader();
        return getClassLoaderTree(curClassLoader);
    }//end getCurClassLoaderTree

    /**
     * Utility method that retrieves the components making up the class loader
     * delegation tree for the given <code>classloader</code> parameter and
     * returns them via an <code>ArrayList</code>.
     *
     * @param classloader <code>ClassLoader</code> instance whose delegation
     *                    tree is to be retrieved and returned
     *
     * @return an <code>ArrayList</code> instance in which each element of the
     *         list is one of the components making up the delegation tree
     *         of the given class loader.
     */
    private static ArrayList getClassLoaderTree(ClassLoader classloader) {
        ArrayList loaderList = new ArrayList();
        while(classloader != null) {
            loaderList.add(classloader);
            classloader = classloader.getParent();
        }//end loop
        loaderList.add(null); //Append boot classloader
        Collections.reverse(loaderList);
        return loaderList;
    }//end getClassLoaderTree

    /**
     * Utility method that displays the class loader delegation tree for
     * the current context class loader. For each class loader in the tree,
     * this method displays the locations from which that class loader
     * will retrieve and load requested classes.
     * <p>
     * This method can be useful when debugging problems related to the
     * receipt of exceptions such as <code>ClassNotFoundException</code>.
     */
    public static void displayContextClassLoaderTree() {
        Thread curThread = Thread.currentThread();
        ClassLoader curClassLoader = curThread.getContextClassLoader();
        displayClassLoaderTree(curClassLoader);
    }//end displayCurClassLoaderTree

    /**
     * Utility method that displays the class loader delegation tree for
     * the given class loader. For each class loader in the tree, this
     * method displays the locations from which that class loader will
     * retrieve and load requested classes.
     * <p>
     * This method can be useful when debugging problems related to the
     * receipt of exceptions such as <code>ClassNotFoundException</code>.
     *
     * @param classloader <code>ClassLoader</code> instance whose delegation
     *                    tree is to be displayed
     */
    public static void displayClassLoaderTree(ClassLoader classloader) {
        ArrayList loaderList = getClassLoaderTree(classloader);
        System.out.println("");
        System.out.println("ClassLoader Tree has " 
	    + loaderList.size() + " levels");
        System.out.println("  cl0 -- Boot ClassLoader ");
        ClassLoader curClassLoader = null;
        for(int i=1; i < loaderList.size(); i++) {
            System.out.println("   |");
            curClassLoader = (ClassLoader)loaderList.get(i);
            System.out.print("  cl"+i+" -- ClassLoader "
                                   +curClassLoader+": ");
            if(curClassLoader instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader)(curClassLoader)).getURLs();
                if(urls != null) {
                    System.out.print(urls[0]);
                    for(int j=1;j<urls.length;j++){
                        System.out.print(", "+urls[j]);
                    }
                } else {//urls == null
                    System.out.print("null search path");
                }//endif
            } else {
                if(curClassLoader instanceof SecureClassLoader) {
                    System.out.print("is instance of SecureClassLoader");
                } else {
                    System.out.print("is unknown ClassLoader type");
                }
            }//endif
            System.out.println("");
        }//end loop
            System.out.println("");
    }//end displayClassLoaderTree
     
}//end class ClassLoaderUtil
