/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package gui;

import shared.helpers.Extractor;
import java.util.*;
import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.lcdui.*;
import javax.microedition.lcdui.CommandListener;

/**
 * The <code>FileBrowser</code> custom component lets the user list files and
 * directories. It's uses FileConnection Optional Package (JSR 75). The FileConnection
 * Optional Package APIs give J2ME devices access to file systems residing on mobile devices,
 * primarily access to removable storage media such as external memory cards.
 * @author breh
 */

public class FileBrowser extends javax.microedition.lcdui.List implements javax.microedition.lcdui.CommandListener {

    /**
     * Command fired on file selection.
     */
    public static final Command SELECT_FILE_COMMAND = new Command("Select", Command.OK, 1);
    public static final Command EXIT_COMMAND = new Command("Exit", Command.BACK, 1);

    private String currDirName;
    private String currFile;
    private Image dirIcon;
    private Image fileIcon;
    private Image[] iconList;
    private javax.microedition.lcdui.CommandListener commandListener;

    /* special string denotes upper directory */
    private static final String UP_DIRECTORY = "..";

    /* special string that denotes upper directory accessible by this browser.
     * this virtual directory contains all roots.
     */
    private static final String MEGA_ROOT = "/";

    /* separator string as defined by FC specification */
    private static final String SEP_STR = "/";

    /* separator character as defined by FC specification */
    private static final char SEP = '/';

    public Display display;

    private String selectedURL;

    private String URL;

    private String filter = null;

    private String title;

    /**
     * Creates a new instance of FileBrowser for given <code>Display</code> object.
     * @param display non null display object.
     */
    public FileBrowser(Display display) {

        super("", IMPLICIT);
        currDirName = MEGA_ROOT;
        this.display = display;
        super.setCommandListener(this);
        setSelectCommand(SELECT_FILE_COMMAND);
        try {
            dirIcon = Image.createImage("/org/netbeans/microedition/resources/dir.png");
        } catch (IOException e) {
            dirIcon = null;
        }
        try {
            fileIcon = Image.createImage("/org/netbeans/microedition/resources/file.png");
        } catch (IOException e) {
            fileIcon = null;
        }
        iconList = new Image[]{fileIcon, dirIcon};

        showDir();
    }

    private void showDir() {
        new Thread(new Runnable() {

            public void run() {
                try {
                    showCurrDir();
                } catch (SecurityException e) {
                    Alert alert = new Alert("Error", "You are not authorized to access the restricted API", null, AlertType.ERROR);
                    alert.setTimeout(2000);
                    display.setCurrent(alert, FileBrowser.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Indicates that a command event has occurred on Displayable d.
     * @param c a <code>Command</code> object identifying the command. This is either
     * one of the applications have been added to <code>Displayable</code> with <code>addCommand(Command)</code>
     * or is the implicit <code>SELECT_COMMAND</code> of List.
     * @param d the <code>Displayable</code> on which this event has occurred
     */
    public void commandAction(Command c, Displayable d) {
        if (c.equals(SELECT_FILE_COMMAND)) {
            System.out.println("Select in FileBrowser");
            List curr = (List) d;
            currFile = curr.getString(curr.getSelectedIndex());
            new Thread(new Runnable() {

                public void run() {
                    if (currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY)) {
                        openDir(currFile);
                    } else {
                        //switch To Next
                        doDismiss();
                    }
                }
            }).start();
        } else {
            commandListener.commandAction(c, d);
        }
    }

    /**
     * Sets component's title.
     *  @param title component's title.
     */
    public void setTitle(String title) {
        this.title = title;
        super.setTitle(title);
    }

    /**
     * Show file list in the current directory .
     */
    private void showCurrDir() {
        if (title == null) {
            super.setTitle(currDirName);
        }
        Enumeration e = null;
        FileConnection currDir = null;

        deleteAll();
        if (MEGA_ROOT.equals(currDirName)) {
            append(UP_DIRECTORY, dirIcon);
            e = FileSystemRegistry.listRoots();

        } else {
            try {
                currDir = (FileConnection) Connector.open("file:///" + currDirName);
                e = currDir.list();

            } catch (IOException ioe) {
            }
            append(UP_DIRECTORY, dirIcon);
        }

        if (e == null) {
            try {
                currDir.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return;
        }

        while (e.hasMoreElements()) {
            String fileName = (String) e.nextElement();
            if (fileName.charAt(fileName.length() - 1) == SEP) {
                // This is directory
                append(fileName, dirIcon);
            } else {
                // this is regular file
                if (filter == null || fileName.indexOf(filter) > -1) {
                    append(fileName, fileIcon);
                }
            }
        }

        if (currDir != null) {
            try {
                currDir.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private void openDir(String fileName) {
        /* In case of directory just change the current directory
         * and show it
         */
        if (currDirName.equals(MEGA_ROOT)) {
            if (fileName.equals(UP_DIRECTORY)) {
                // can not go up from MEGA_ROOT
                return;
            }
            currDirName = fileName;
        } else if (fileName.equals(UP_DIRECTORY)) {
            // Go up one directory
            // TODO use setFileConnection when implemented
            int i = currDirName.lastIndexOf(SEP, currDirName.length() - 2);
            if (i != -1) {
                currDirName = currDirName.substring(0, i + 1);
            } else {
                currDirName = MEGA_ROOT;
            }
        } else {
            currDirName = currDirName + fileName;
        }
        showDir();
    }

    /**
     * Returns selected file as a <code>FileConnection</code> object.
     * @return non null <code>FileConection</code> object
     */
    public FileConnection getSelectedFile() throws IOException {
        FileConnection fileConnection = (FileConnection) Connector.open(selectedURL);
        return fileConnection;
    }

    /**
     * Returns selected <code>FileURL</code> object.
     * @return non null <code>FileURL</code> object
     */
    public String getSelectedFileURL() {
        return selectedURL;
    }

    /**
     * Sets the file filter.
     * @param filter file filter String object
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Returns command listener.
     * @return non null <code>CommandListener</code> object
     */
    protected CommandListener getCommandListener() {
        return commandListener;
    }

    /**
     * Sets command listener to this component.
     * @param commandListener <code>CommandListener</code> to be used
     */
    public void setCommandListener(CommandListener commandListener) {
        this.commandListener = commandListener;
    }

    public String getFileName () {
    	return currFile;
    }

    private void doDismiss() {
        //selectedURL = "file:///" + currDirName + SEP_STR + currFile;
    	selectedURL = "file:///" + currDirName + currFile;
        System.out.println("selURL: "+ currDirName + "    "+ currFile);
        CommandListener commandListener = getCommandListener();
        if (commandListener != null) {
            commandListener.commandAction(SELECT_FILE_COMMAND, this);
        }
    }


    public String getToBeSharedFileName () {
        List d = (List) ((Displayable) display.getCurrent());
        currFile = d.getString(d.getSelectedIndex());
        return currFile;
    }

    public String getToBeSharedFileLocation () {
    	List d = (List) ((Displayable) display.getCurrent());
        currFile = d.getString(d.getSelectedIndex());
    	return currDirName + currFile;
    }
}
