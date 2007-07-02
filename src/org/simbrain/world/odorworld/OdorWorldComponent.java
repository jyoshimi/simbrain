/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.world.odorworld;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.util.LocalConfiguration;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;


/**
 * <b>WorldPanel</b> is the container for the world component.   Handles toolbar buttons, and serializing of world
 * data.  The main environment codes is in {@link OdorWorld}.
 */
public class OdorWorldComponent extends WorkspaceComponent implements ActionListener {

    /** Current file. */
    private File currentFile = null;

    /** Allows the world to be scrolled if it is bigger than the display window. */
    private JScrollPane worldScroller = new JScrollPane();

    /** Odor world to be in frame. */
    private OdorWorld world;

    /** Odor world frame menu. */
    private OdorWorldFrameMenu menu;

    /** List of couplings for this world. */
    private ArrayList<Coupling> couplings = new ArrayList<Coupling>();

    /**
     * Default constructor.
     */
    public OdorWorldComponent() {
        super();
        init();
    }

    /**
     * Initializes frame.
     */
    public void init() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", worldScroller);
        world = new OdorWorld(this);
        world.resize();
        worldScroller.setViewportView(world);
        worldScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        worldScroller.setEnabled(false);
        menu = new OdorWorldFrameMenu(this);
        menu.setUpMenus();
    }

    /**
     * Return the current file.
     *
     * @return Current file
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * Return the odor world.
     *
     * @return Odor world
     */
    public OdorWorld getWorld() {
        return world;
    }

    /**
     * Read a world from a world-wld file.
     *
     * @param theFile the wld file containing world information
     */
    public void read(final File theFile) {
        currentFile = theFile;

        try {
            Reader reader = new FileReader(theFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "world_mapping.xml");

            Unmarshaller unmarshaller = new Unmarshaller(world);
            unmarshaller.setMapping(map);

            // unmarshaller.setDebug(true);
            //this.getWorkspace().removeAgentsFromCouplings(world);
            world.clear();
            world = (OdorWorld) unmarshaller.unmarshal(reader);
            world.init();
            world.setParentFrame(this);
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(
                                          null, "Could not find world file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                                          null, "There was a problem opening file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        }

        //getWorkspace().attachAgentsToCouplings();
        setName(theFile.getName());
        OdorWorldPreferences.setCurrentDirectory(getCurrentDirectory());

        //Set Path; used in workspace persistence
        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, theFile.getAbsolutePath()));
        world.repaint();
    }

    /**
     * Opens a file-save dialog and saves world information to the specified file  Called by "Save As".
     */
    public void saveWorld() {
        SFileChooser chooser = new SFileChooser(getCurrentDirectory(), getFileExtension());
        File worldFile = chooser.showSaveDialog();

        if (worldFile != null) {
            saveWorld(worldFile);
            currentFile = worldFile;
            setCurrentDirectory(chooser.getCurrentLocation());
        }
    }

    /**
     * Save a specified file  Called by "save".
     *
     * @param worldFile the file to save to
     */
    public void saveWorld(final File worldFile) {
        currentFile = worldFile;
        LocalConfiguration.getInstance().getProperties().setProperty("org.exolab.castor.indent", "true");

        try {
            FileWriter writer = new FileWriter(worldFile);
            Mapping map = new Mapping();
            map.loadMapping("." + FS + "lib" + FS + "world_mapping.xml");

            Marshaller marshaller = new Marshaller(writer);
            marshaller.setMapping(map);

            //marshaller.setDebug(true);
            marshaller.marshal(world);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String localDir = new String(System.getProperty("user.dir"));
        setPath(Utils.getRelativePath(localDir, worldFile.getAbsolutePath()));

        setName("" + worldFile.getName());
        setChangedSinceLastSave(false);
    }

    /**
     * Responds to actions performed.
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object e1 = e.getSource();

        if (e1 == menu.getOpenItem()) {
            //openWorld();
            this.setChangedSinceLastSave(false);
        } else if (e1 == menu.getSaveItem()) {
            if (currentFile == null) {
                saveWorld();
            } else {
                saveWorld(currentFile);
            }
        } else if (e1 == menu.getSaveAsItem()) {
            saveWorld();
        } else if (e1 == menu.getPrefsItem()) {
            world.showGeneralDialog();
            this.setChangedSinceLastSave(true);
        } else if (e1 == menu.getScriptItem()) {
            world.showScriptDialog();
        } else if (e1 == menu.getClose()) {
            if (isChangedSinceLastSave()) {
                hasChanged();
            } else {
                dispose();
            }
        } else if (e1 == menu.getHelpItem()) {
            Utils.showQuickRef("World.html");
        }
    }

    /**
     * Tasks to peform when frame is opened.
     * @param e Internal frame event
     */
    public void internalFrameOpened(final InternalFrameEvent e) {
    }

    /**
     * Tasks to perform when frame is closing.
     * @param e Internal frame event
     */
    public void internalFrameClosing(final InternalFrameEvent e) {
        if (isChangedSinceLastSave()) {
            hasChanged();
        } else {
            dispose();
        }
    }

    /**
     * Return the arraylist of agents.
     *
     * @return List of agents
     */
    public ArrayList getAgentList() {
        return world.getAgentList();
    }

    /**
     * @return Odor world frame menu.
     */
    public OdorWorldFrameMenu getMenu() {
        return menu;
    }

    /**
     * Sets odor world frame menu.
     * @param menu Menu
     */
    public void setMenu(final OdorWorldFrameMenu menu) {
        this.menu = menu;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public int getDefaultWidth() {
        return 450;
    }

    @Override
    public int getDefaultHeight() {
        return 450;
    }

    @Override
    public int getDefaultLocationX() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDefaultLocationY() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void save(File saveFile) {
        // TODO Auto-generated method stub
        
    }

    public List<Consumer> getConsumers() {
        return null;
    }

    public List<Coupling> getCouplings() {
        return couplings;
    }

    public List<Producer> getProducers() {
        return new ArrayList<Producer>(world.getEntityList());
    }

    @Override
    public void open(File openFile) {
        // TODO Auto-generated method stub
    }

    @Override
    public int getWindowIndex() {
        // TODO Auto-generated method stub
        return 0;
    }
}