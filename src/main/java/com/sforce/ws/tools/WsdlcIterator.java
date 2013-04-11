/*
 * Copyright 2007, SALESFORCE.com All Rights Reserved Company Confidential
 */

package com.sforce.ws.tools;

import java.io.File;
import java.io.FilenameFilter;

import org.stringtemplate.v4.STGroupDir;

/**
 * Simple tool for running wsdlc on a group of wsdl files. This was written for use by ApexWebServiceTestExposer.
 * 
 * @author swanaselja
 * @since 148
 */

public class WsdlcIterator {

    private static final String WsdlSuffix = ".wsdl";
    private static final String UniqueJarSuffix = ".apextest.jar";

    private static final FilenameFilter WsdlFilter = new FilenameFilter() {
        @Override
        public boolean accept(File unused, String name) {
            if (name == null) return false;
            return (name.endsWith(WsdlSuffix));
        }
    };

    // main() entry point for use by ant build-wsdl.xml target jartestwsdl
    // to compile the wsdls that are generated by WsdlUpdater having called
    // other methods of this class.

    public static void main(String[] args) {

        String packagePrefix = System.getProperty(wsdlc.PACKAGE_PREFIX);
        boolean standAlone = Boolean.parseBoolean(System.getProperty(wsdlc.STANDALONE_JAR, "false"));
        consoleMessage("Beginning run of multiple calls to wsdlc");

        // Parse input parameters
        if (args.length != 4) {
            showMainUsage();
            System.exit(2);
        }
        if (!"wsdldir".equalsIgnoreCase(args[0]) || !"jardir".equalsIgnoreCase(args[2])) {
            showMainUsage();
            System.exit(2);
        }
        String wsdlDir = args[1];
        String jarDir = args[3];

        try {
            // Validate input parameters
            File wDir = new File(wsdlDir).getCanonicalFile();
            File jDir = new File(jarDir).getCanonicalFile();
            if (!wDir.exists() || !wDir.isDirectory()) {
                consoleMessage("###  Input wsdldir '" + wsdlDir + "' does not exist or is not a directory");
                showMainUsage();
                System.exit(1);
            }
            if (!jDir.exists() || !jDir.isDirectory()) {
                consoleMessage("###  Input jardir '" + jarDir + "' does not exist or is not a directory");
                showMainUsage();
                System.exit(1);
            }
            wsdlDir = wDir.getCanonicalPath();
            jarDir = jDir.getCanonicalPath();

            // Make the list of wsdl files and validate it.
            // The list will not contain any path info, just filenames:
            String[] wsdlFiles = wDir.list(WsdlFilter);
            if (wsdlFiles == null || wsdlFiles.length == 0) {
                consoleMessage("###  Input wsdldir '" + wsdlDir + "' does not contain any " + WsdlSuffix + " files.");
                showMainUsage();
                System.exit(1);
            }
            for (String aWsdl : wsdlFiles) {
                if (!aWsdl.endsWith(WsdlSuffix)) {
                    consoleMessage("###  Software Error: Input wsdldir '" + wsdlDir
                            + "' produced listing of non-wsdl file '" + aWsdl + "'");
                    showMainUsage();
                    System.exit(1);
                }
            }

            // Generate new jar names from wsdl names:
            // The jar files go to a different directory than the input wsdl files.
            // The jars have a special extension to make them easy to delete during build cleaning.
            // Also generate wsdl full paths, for wsdlc:
            String[] jarFiles = new String[wsdlFiles.length];
            String[] wsdlPaths = new String[wsdlFiles.length];
            for (int ix = 0; ix < wsdlFiles.length; ix++) {
                // The validation loop above guarantees this actually does a replace for each name:
                String jarName = wsdlFiles[ix].replace(WsdlSuffix, UniqueJarSuffix);
                jarFiles[ix] = new File(jarDir, jarName).getCanonicalPath();
                wsdlPaths[ix] = new File(wDir, wsdlFiles[ix]).getCanonicalPath();
            }

            // Delete any existing jar files. wsdlc won't delete anything:
            for (int ix = 0; ix < wsdlFiles.length; ix++) {
                File aJar = new File(jarFiles[ix]);
                if (aJar.exists()) {
                    consoleMessage("Deleting existing " + aJar.getAbsolutePath());
                    aJar.delete();
                }
            }

            STGroupDir templates = new STGroupDir(wsdlc.TEMPLATE_DIR, '$', '$');
            // Run wsdlc on each wsdl in the wsdl directory:
            for (int ix = 0; ix < wsdlFiles.length; ix++) {
                consoleMessage("Running wsdlc on " + wsdlPaths[ix] + "\n       to create " + jarFiles[ix]);
                wsdlc.run(wsdlPaths[ix], jarFiles[ix], packagePrefix, standAlone, templates, null, true);
            }

        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(1);
        }
    }

    private static final void consoleMessage(String message) {
        System.out.println("[WsdlcIterator]    " + message);
    }

    private static void showMainUsage() {
        System.out.println("Usage:  WsdlcIterator wsdldir <dir> jardir <dir>");
        System.out
                .println("        wsdldir = The directory into which ApexWebServiceTestExposer (or other source) has placed all the wsdl files it creates. This tool will pass all wsdl files therein to com.sforce.ws.tools.wsdlc,");
        System.out.println("        jardir  = The directory to which the resulting jars will be written.");
    }

}
