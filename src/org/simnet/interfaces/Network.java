/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.event.EventListenerList;

import org.simnet.NetworkThread;
import org.simnet.coupling.InteractionMode;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.WorldListener;


/**
 * <b>Network</b> provides core neural network functionality and is the the main API for external calls. Network
 * objects are sets of neurons and  weights connecting them. Much of the  actual update and  learning logic occurs
 * (currently) in the individual nodes.
 */
public abstract class Network implements WorldListener {

    /** Id of this network; used in persistence. */
    protected String id;

    /** Reference to Workspace, which maintains a list of all worlds and gauges. */
    private Workspace workspace;

    /** Default interaction mode. */
    private static final InteractionMode DEFAULT_INTERACTION_MODE = InteractionMode.BOTH_WAYS;

    /** Whether network has been updated yet; used by thread. */
    private boolean updateCompleted;

    /** Current interaction mode. */
    private InteractionMode interactionMode = DEFAULT_INTERACTION_MODE;

    /** List of components which listen for changes to this network. */
    private EventListenerList listenerList = new EventListenerList();

    /** The thread that runs the network. */
    private NetworkThread networkThread;

    /** Whether this is a discrete or continuous time network. */
    private int timeType = DISCRETE;

    /** If this is a discrete-time network. */
    public static final int DISCRETE = 0;

    /** If this is a continuous-time network. */
    public static final int CONTINUOUS = 1;

    /** Array list of neurons. */
    protected ArrayList neuronList = new ArrayList();

    /** Array list of weights. */
    protected ArrayList weightList = new ArrayList();

    /** Keeps track of time. */
    protected double time = 0;

    /** Time step. */
    private double timeStep = .01;

    /** Whether to round off neuron values. */
    private boolean roundOffActivationValues = false;

    /** Degree to which to round off values. */
    private int precision = 0;
    
    /** Only used for sub-nets of complex networks which have parents. */
    private Network parentNet = null;
    
    /** Used to temporarily turn off all learning. */
    private boolean clampWeights = false;

    /**
     * Used to create an instance of network (Default constructor).
     */
    public Network() {
    }

    /**
     * Update the network.
     */
    public abstract void update();
    
    /**
     * Externally called update function which coordiantes input and output neurons and
     * connections with worlds and gauges.
     */
    public void updateTopLevel() {

        // Get stimulus vector from world and update input nodes
        updateInputs();
        
        update();
        
        // Update couplined worlds
        updateWorlds();
        
        // Notify network listeners
        this.fireNetworkChanged();

        // Clear input nodes
        clearInputs();
        
        // For thread
        updateCompleted = true;
    }
    
    /**
     * Respond to worldChanged event.
     */
    public void worldChanged() {
        updateInputs();
        update();
        fireNetworkChanged();
        clearInputs();
    }


    /**
     * Returns all Output Neurons.
     *
     * @return list of output neurons;
     */
    public Collection getOutputNeurons() {
        ArrayList outputs = new ArrayList();
        for (Iterator i = this.getNeuronList().iterator(); i.hasNext();) {
            Neuron neuron = (Neuron) i.next();
            if (neuron.isOutput()) {
                outputs.add(neuron);
            }
        }
        return outputs;
    }

    /**
     * Returns all Input Neurons.
     *
     * @return list of input neurons;
     */
    public Collection getInputNeurons() {
        ArrayList inputs = new ArrayList();
        for (Iterator i = this.getNeuronList().iterator(); i.hasNext();) {
            Neuron neuron = (Neuron) i.next();
            if (neuron.isInput()) {
                inputs.add(neuron);
            }
        }
        return inputs;
    }

    /**
     * Clears out input values of network nodes, which otherwise linger and
     * cause problems.
     */
    public void clearInputs() {
        if ((interactionMode.isWorldToNetwork() || interactionMode.isBothWays())) {
            return;
        }

        Iterator it = getInputNeurons().iterator();

        while (it.hasNext()) {
            Neuron n = (Neuron) it.next();
            n.setInputValue(0);
        }
    }

    
    /**
     * Go through each output node and send the associated output value to the
     * world component.
     */
    public void updateWorlds() {
        
        if (!(interactionMode.isNetworkToWorld() || interactionMode.isBothWays())) {
            return;
        }

        Iterator it = getOutputNeurons().iterator();
        while (it.hasNext()) {
            Neuron n = (Neuron) it.next();

            if (n.getMotorCoupling().getAgent() != null) {
                n.getMotorCoupling().getAgent().setMotorCommand(
                        n.getMotorCoupling().getCommandArray(),
                        n.getActivation());
            }
        }
    }

    /**
     * Update input nodes of the network based on the state of the world.
     */
    public void updateInputs() {
        if (!(interactionMode.isWorldToNetwork() || interactionMode.isBothWays())) {
            return;
        }

        Iterator it = getInputNeurons().iterator();
        while (it.hasNext()) {
            Neuron n = (Neuron) it.next();
            if (n.getSensoryCoupling().getAgent() != null) {
                double val = n.getSensoryCoupling().getAgent().getStimulus(
                        n.getSensoryCoupling().getSensorArray());
                n.setInputValue(val);
            } else {
                n.setInputValue(0);
            }
        }
    }

    /**
     * Update all ids. Used in for persistences before writing xml file.
     */
    public void updateIds() {

        setId("root_net");
        
        // Update neuron ids
        int nIndex = 1;
        for (Iterator neurons = getNeuronList().iterator(); neurons.hasNext(); nIndex++) {
            Neuron neuron = (Neuron) neurons.next();
            neuron.setId("n_" + nIndex);
        }

        // Update synapse ids
        int sIndex = 1;
        for (Iterator synapses = getWeightList().iterator(); synapses.hasNext(); sIndex++) {
            Synapse synapse = (Synapse) synapses.next();
            synapse.setId("s_" + sIndex);
        }

    }

    /**
     * Initialize the network.
     */
    public void init() {
        initWeights();
        initParents();
    }

    /**
     * Updates weights with fan-in.  Used when weights have been added.
     */
    public void initWeights() {
        //initialize fan-in and fan-out on each neuron
        for (int i = 0; i < weightList.size(); i++) {
            Synapse w = getWeight(i);
            w.init();
        }
    }

    /**
     * @return the name of the class of this network
     */
    public String getType() {
        return this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    /**
     * @return how many subnetworks down this is
     */
    public int getDepth() {
        Network net = this;
        int n = 0;

        while (net != null) {
            net = net.getNetworkParent();
            n++;
        }

        return n;
    }

    /**
     * @return a string of tabs for use in indenting debug info accroding to the depth of a subnet
     */
    public String getIndents() {
        String ret = new String("");

        for (int i = 0; i < (this.getDepth() - 1); i++) {
            ret = ret.concat("\t");
        }

        return ret;
    }

    /**
     * @return List of neurons in network.
     */
    public Collection getNeuronList() {
        return this.neuronList;
    }

    /**
     * @return List of weights in network.
     */
    public Collection getWeightList() {
        return this.weightList;
    }

    /**
     * @return Number of neurons in network.
     */
    public int getNeuronCount() {
        return neuronList.size();
    }

    /**
     * @param index Number of neuron in array list.
     * @return Neuron at the point of the index
     */
    public Neuron getNeuron(final int index) {
        return (Neuron) neuronList.get(index);
    }

    /**
     * Adds a new neuron.
     * @param neuron Type of neuron to add
     * @param notify whether to notify listeners that this neuron has been added
     */
    protected void addNeuron(final Neuron neuron, final boolean notify) {
        neuron.setParentNetwork(this);
        neuronList.add(neuron);
        if (notify) {
            fireNeuronAdded(neuron);
        }
    }
    
    /**
     * Adds a new neuron.
     * @param neuron Type of neuron to add
     */    
    public void addNeuron(final Neuron neuron) {
        addNeuron(neuron, true);
    }

    /**
     * @return Number of weights in network
     */
    public int getWeightCount() {
        return weightList.size();
    }

    /**
     * @param index Number of weight in array list.
     * @return Weight at the point of the indesx
     */
    public Synapse getWeight(final int index) {
        return (Synapse) weightList.get(index);
    }

    public double getTime() {
        return time;
    }

    public void setTime(final double i) {
        time = i;
    }

    /**
     * Adds a weight to the neuron network, where that weight already has designated source and target neurons.
     *
     * @param weight the weight object to add
     * @param notify whether to notify listeners that a weight has been added.
     */
    protected void addWeight(final Synapse weight, final boolean notify) {
        
        Neuron source = (Neuron) weight.getSource();
        source.addTarget(weight);

        Neuron target = (Neuron) weight.getTarget();
        target.addSource(weight);
        weight.initSpikeResponder();
        weightList.add(weight);

        if (notify) {
            fireSynapseAdded(weight);
        }
    }

    /**
     * Adds a weight to the neuron network, where that weight already has designated source and target neurons.
     *
     * @param weight the weight object to add
     * @param notify whether to notify listeners that a weight has been added.
     */
    public void addWeight(final Synapse weight) {
        addWeight(weight, true);
    }

    /**
     * Calls {@link Neuron#update} for each neuron.
     */
    public void updateAllNeurons() {

        // First update the activation buffers
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.update(); // update neuron buffers
        }

        // Then update the activations themselves
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.setActivation(n.getBuffer());
        }
    }

    /**
     * Calls {@link Weight#update} for each weight.
     */
    public void updateAllWeights() {

        if (clampWeights) {
            return;
        }

        // No Buffering necessary because the values of weights don't depend on one another
        for (int i = 0; i < weightList.size(); i++) {
            Synapse w = (Synapse) weightList.get(i);
            w.update();
        }
    }

    /**
     * Calls {@link Neuron#checkBounds} for each neuron, which makes sure the neuron has not exceeded its upper bound
     * or gone below its lower bound.   TODO: Add or replace with normalization within bounds?
     */
    public void checkAllBounds() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = (Neuron) neuronList.get(i);
            n.checkBounds();
        }

        for (int i = 0; i < weightList.size(); i++) {
            Synapse w = (Synapse) weightList.get(i);
            w.checkBounds();
        }
    }

    /**
     * Round activations of to intergers; for testing.
     */
    public void roundAll() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron temp = (Neuron) neuronList.get(i);
            temp.round(precision);
        }
    }

    /**
     * Deletes a neuron from the network.
     *
     * @param toDelete neuron to delete
     * @param notify notify listeners that this neuron has been deleted
     */
    protected void deleteNeuron(final Neuron toDelete, final boolean notify) {

        if (neuronList.contains(toDelete)) {

            // Remove outgoing synapses
            while (toDelete.getFanOut().size() > 0) {
                Synapse s = (Synapse) toDelete.getFanOut().get(toDelete.getFanOut().size() - 1);
                deleteWeight(s, notify);
            }

            // Remove incoming synapses
            while (toDelete.getFanIn().size() > 0) {
              Synapse s = (Synapse) toDelete.getFanIn().get(toDelete.getFanIn().size() - 1);
              deleteWeight(s, notify);
            }

            neuronList.remove(toDelete);

            // Notify listeners (views) that this neuron has been deleted
            if (notify) {
                this.fireNeuronDeleted(toDelete);
            }
        }
    }

    /**
     * Deletes a neuron from the network.
     *
     * @param toDelete neuron to delete
     */
    public void deleteNeuron(final Neuron toDelete) {
        deleteNeuron(toDelete, true);
    }

    /**
     * Delete a specified weight.
     *
     * @param toDelete the weight to delete
     * @param notify whether to fire a synapse deleted event
     */
    protected void deleteWeight(final Synapse toDelete, final boolean notify) {

        toDelete.getSource().getFanOut().remove(toDelete);
        toDelete.getTarget().getFanIn().remove(toDelete);

        weightList.remove(toDelete);

        if (this.getNetworkParent() != null) {
            getNetworkParent().deleteWeight(toDelete, notify);
        }

        if (notify) {
            fireSynapseDeleted(toDelete);
        }
    }

    /**
     * Delete a specified weight.
     *
     * @param toDelete the weight to delete
     */
    public void deleteWeight(final Synapse toDelete) {
        deleteWeight(toDelete);
    }

    /**
     * Set the activation level of all neurons to zero.
     */
    public void setNeuronsToZero() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron temp = (Neuron) neuronList.get(i);
            temp.setActivation(0);
        }
    }

    /**
     * Returns the "state" of the network--the activation level of its neurons.  Used by the gauge component
     *
     * @return an array representing the activation levels of all the neurons in this network
     */
    public double[] getState() {
        double[] ret = new double[this.getNeuronCount()];

        for (int i = 0; i < this.getNeuronCount(); i++) {
            Neuron n = getNeuron(i);
            ret[i] = (int) n.getActivation();
        }

        return ret;
    }

    /**
     * Sets all weight values to zero, effectively eliminating them.
     */
    public void setWeightsToZero() {
        for (int i = 0; i < weightList.size(); i++) {
            Synapse temp = (Synapse) weightList.get(i);
            temp.setStrength(0);
        }
    }

    /**
     * Randomizes all neurons.
     */
    public void randomizeNeurons() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron temp = (Neuron) neuronList.get(i);
            temp.randomize();
        }
    }

    /**
     * Randomizes all weights.
     */
    public void randomizeWeights() {
        for (int i = 0; i < weightList.size(); i++) {
            Synapse temp = (Synapse) weightList.get(i);
            temp.randomize();
        }

        //Must make this symmetrical
    }

    /**
     * Round a value off to indicated number of decimal places.
     *
     * @param value value to round off
     * @param decimalPlace degree of precision
     *
     * @return rounded number
     */
    public static double round(final double value, final int decimalPlace) {
        double powerOfTen = 1;
        int place = decimalPlace;

        while (place-- > 0) {
            powerOfTen *= 10.0;
        }

        return Math.round(value * powerOfTen) / powerOfTen;
    }

    /**
     * Sends relevant information about the network to standard output.
     *
     * @return string representation of this network
     */
    public String toString() {
        String ret = new String();
        if (neuronList.size() > 0) {
            for (int i = 0; i < neuronList.size(); i++) {
                Neuron tempRef = (Neuron) neuronList.get(i);
                ret += (getIndents() + tempRef + "\n");
            }
        }

        if (weightList.size() > 0) {
            for (int i = 0; i < weightList.size(); i++) {
                Synapse tempRef = (Synapse) weightList.get(i);
                ret += (getIndents() + "Weight [" + i + "]: " + tempRef);
                ret += ("  Connects neuron " + tempRef.getSource() + " to neuron "
                                   + tempRef.getTarget() + "\n");
            }
        }
        return ret;
    }

    /**
     * @return Degree to which to round off values.
     */
    public int getPrecision() {
        return precision;
    }

    /**
     * @return Whether to round off neuron values.
     */
    public boolean getRoundingOff() {
        return roundOffActivationValues;
    }

    /**
     * Sets the degree to which to round off values.
     * @param i Degeree to round off values
     */
    public void setPrecision(final int i) {
        precision = i;
    }

    /**
     * Whether to round off neuron values.
     * @param b Round off
     */
    public void setRoundingOff(final boolean b) {
        roundOffActivationValues = b;
    }

    /**
     * @return Returns the roundOffActivationValues.
     */
    public boolean isRoundOffActivationValues() {
        return roundOffActivationValues;
    }

    /**
     * @param roundOffActivationValues The roundOffActivationValues to set.
     */
    public void setRoundOffActivationValues(final boolean roundOffActivationValues) {
        this.roundOffActivationValues = roundOffActivationValues;
    }

    /**
     * @param neuronList The neuronList to set.
     */
    public void setNeuronList(final ArrayList neuronList) {
        System.out.println("-->" + neuronList.size());
        this.neuronList = neuronList;
    }

    /**
     * @param weightList The weightList to set.
     */
    public void setWeightList(final ArrayList weightList) {
        this.weightList = weightList;
    }

    /**
     * Add an array of neurons and set their parents to this.
     *
     * @param neurons list of neurons to add
     */
    public void addNeuronList(final ArrayList neurons) {
        for (int i = 0; i < neurons.size(); i++) {
            Neuron n = (Neuron) neurons.get(i);
            n.setParentNetwork(this);
            neuronList.add(n);
        }
    }

    /**
     * Sets the upper bounds.
     * @param u Upper bound
     */
    public void setUpperBounds(final double u) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(u);
        }
    }

    /**
     * Sets the lower bounds.
     * @param l Lower bound
     */
    public void setLowerBounds(final double l) {
        for (int i = 0; i < getNeuronCount(); i++) {
            getNeuron(i).setUpperBound(l);
        }
    }

    /**
     * Returns a reference to the synapse connecting two neurons, or null if there is none.
     *
     * @param src source neuron
     * @param tar target neuron
     *
     * @return synapse from source to target
     */
    public static Synapse getWeight(final Neuron src, final Neuron tar) {
        for (int i = 0; i < src.fanOut.size(); i++) {
            Synapse s = (Synapse) src.fanOut.get(i);

            if (s.getTarget() == tar) {
                return s;
            }
        }

        return null;
    }

    /**
     * Replace one neuron with another.
     *
     * @param oldNeuron out with the old
     * @param newNeuron in with the new...
     */
    public void changeNeuron(final Neuron oldNeuron, final Neuron newNeuron) {
        newNeuron.setId(oldNeuron.getId());
        newNeuron.setFanIn(oldNeuron.getFanIn());
        newNeuron.setFanOut(oldNeuron.getFanOut());
        newNeuron.setParentNetwork(this);

       fireNeuronChanged(oldNeuron, newNeuron);

        for (int i = 0; i < oldNeuron.getFanIn().size(); i++) {
            ((Synapse) oldNeuron.getFanIn().get(i)).setTarget(newNeuron);
        }

        for (int i = 0; i < oldNeuron.getFanOut().size(); i++) {
            ((Synapse) oldNeuron.getFanOut().get(i)).setSource(newNeuron);
        }

        getNeuronList().remove(oldNeuron);
        getNeuronList().add(newNeuron);
        initParents();

        // If the neuron is a spiker, add spikeResponders to target weights, else remove them
        for (int i = 0; i < newNeuron.getFanOut().size(); i++) {
            ((Synapse) newNeuron.getFanOut().get(i)).initSpikeResponder();
        }

        updateTimeType();
    }

    /**
     * Change synapse type / replace one synapse with another.
     *
     * @param oldSynapse out with the old
     * @param newSynapse in with the new...
     */
    public void changeSynapse(final Synapse oldSynapse, final Synapse newSynapse) {
        newSynapse.setTarget(oldSynapse.getTarget());
        newSynapse.setSource(oldSynapse.getSource());
        deleteWeight(oldSynapse, false);
        addWeight(newSynapse, false);
        fireSynapseChanged(oldSynapse, newSynapse);
    }

    /**
     * Initializes parent networks.
     */
    public void initParents() {
        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = getNeuron(i);
            n.setParentNetwork(this);
        }
    }

    /**
     * If there is a single continuous neuron in the network, consider this a continuous network.
     */
    public void updateTimeType() {
        timeType = DISCRETE;

        for (int i = 0; i < neuronList.size(); i++) {
            Neuron n = getNeuron(i);

            if (n.getTimeType() == CONTINUOUS) {
                timeType = CONTINUOUS;
            }
        }
        
        time = 0;
    }

    /**
     * Increment the time counter, using a different method depending on whether this is a continuous or discrete.
     * network
     */
    public void updateTime() {
        if (timeType == CONTINUOUS) {
            time += this.getTimeStep();
        } else {
            time += 1;
        }
    }

    /**
     * Gets the weight at particular point.
     * @param i Neuorn number
     * @param j Weight to get
     * @return Weight at the points defined
     */
    //TODO: Either fix this or make its assumptions explicit
    public Synapse getWeight(final int i, final int j) {
        return (Synapse) getNeuron(i).getFanOut().get(j);
    }

    /**
     * @return Returns the parentNet.
     */
    public Network getNetworkParent() {
        return parentNet;
    }

    /**
     * @param parentNet The parentNet to set.
     */
    public void setNetworkParent(final Network parentNet) {
        this.parentNet = parentNet;
    }

    /**
     * @return Returns the timeStep.
     */
    public double getTimeStep() {
        return timeStep;
    }

    /**
     * @param timeStep The timeStep to set.
     */
    public void setTimeStep(final double timeStep) {
        this.timeStep = timeStep;
    }

    /**
     * @return Units by which to count.
     */
    public static String[] getUnits() {
        String[] units = {"Seconds", "Iterations" };

        return units;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @return String lable with time units.
     */
    public String getTimeLabel() {
        if (timeType == DISCRETE) {
            return getUnits()[1];
        } else {
            return getUnits()[0];
        }
    }

    /**
     * @return integer representation of time type.
     */
    public int getTimeType() {
        return timeType;
    }

    /**
     * @return Clamped weights.
     */
    public boolean getClampWeights() {
        return clampWeights;
    }

    /**
     * Sets weights to clamped values.
     * @param clampWeights Weights to set
     */
    public void setClampWeights(final boolean clampWeights) {
        this.clampWeights = clampWeights;
    }

    /**
     * Add the specified network listener.
     *
     * @param l listener to add
     */
    public void addNetworkListener(final NetworkListener l) {
        listenerList.add(NetworkListener.class, l);
    }

    /**
     * Remove the specified network listener.
     *
     * @param l listener to remove
     */
    public void removeNetworkListener(final NetworkListener l) {
        listenerList.remove(NetworkListener.class, l);
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     *
     * @param deleted neuron which has been deleted
     */
    public void fireNeuronDeleted(final Neuron deleted) {
        NetworkEvent networkEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                // Lazily create the event:
                if (networkEvent == null) {
                    networkEvent = new NetworkEvent(this, deleted);
                }
                ((NetworkListener) listeners[i + 1]).neuronRemoved(networkEvent);
            }
        }
    }

    /**
     * Fire a network changed event to all registered model listeners.
     */
    public void fireNetworkChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NetworkListener.class) {
                ((NetworkListener) listeners[i + 1]).networkChanged();
            }
        }
    }

    /**
     * Fire a neuron added event to all registered model listeners.
     */
    public void fireNeuronAdded(final Neuron added)
    {
        NetworkEvent networkEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==NetworkListener.class) {
                // Lazily create the event:
                if (networkEvent == null)
                    networkEvent = new NetworkEvent(this, added);
                ((NetworkListener)listeners[i+1]).neuronAdded(networkEvent);
            }
        }
    }
    
    /**
     * Fire a neuron added event to all registered model listeners.
     */
    public void fireNeuronChanged(final Neuron old, final Neuron changed) {
        NetworkEvent networkEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i-= 2) {
            if (listeners[i]==NetworkListener.class) {
                // Lazily create the event:
                if (networkEvent == null) {
                    networkEvent = new NetworkEvent(this, old, changed);                    
                }
                ((NetworkListener)listeners[i+1]).neuronChanged(networkEvent);
            }
        }
    }
    
    /**
     * Fire a neuron added event to all registered model listeners.
     */
    public void fireSynapseAdded(final Synapse added)
    {
        NetworkEvent networkEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==NetworkListener.class) {
                // Lazily create the event:
                if (networkEvent == null)
                    networkEvent = new NetworkEvent(this, added);
                ((NetworkListener)listeners[i+1]).synapseAdded(networkEvent);
            }
        }
    }

    /**
     * Fire a neuron deleted event to all registered model listeners.
     */
    public void fireSynapseDeleted(final Synapse deleted) {
        NetworkEvent networkEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i>=0 ; i-=2) {
            if (listeners[i]==NetworkListener.class) {
                // Lazily create the event:
                if (networkEvent == null)
                    networkEvent = new NetworkEvent(this, deleted);
                ((NetworkListener)listeners[i+1]).synapseRemoved(networkEvent);
            }
        }
    }
    
    /**
     * Fire a neuron deleted event to all registered model listeners.
     */
    public void fireSynapseChanged(final Synapse old, final Synapse changed) {
        NetworkEvent networkEvent = null;
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i>=0 ; i-=2) {
            if (listeners[i]==NetworkListener.class) {
                // Lazily create the event:
                if (networkEvent == null)
                    networkEvent = new NetworkEvent(this, old, changed);
                ((NetworkListener)listeners[i+1]).synapseChanged(networkEvent);
            }
        }
    }
    
    /**
     * Set the current interaction mode for this network panel to <code>interactionMode</code>.
     *
     * <p>This is a bound property.</p>
     *
     * @param interactionMode interaction mode for this network panel, must not be null
     */
    public void setInteractionMode(final InteractionMode interactionMode) {

        if (interactionMode == null) {
            throw new IllegalArgumentException("interactionMode must not be null");
        }

        InteractionMode oldInteractionMode = this.interactionMode;
        this.interactionMode = interactionMode;
    }
    
    /**
     * Return the current interaction mode for this network panel.
     *
     * @return the current interaction mode for this network panel
     */
    public InteractionMode getInteractionMode() {
        return interactionMode;
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @return whether the network has been updated or not
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Used by Network thread to ensure that an update cycle is complete before
     * updating again.
     *
     * @param b whether the network has been updated or not.
     */
    public void setUpdateCompleted(final boolean b) {
        updateCompleted = b;
    }
    
    /**
     * @return Returns the networkThread.
     */
    public NetworkThread getNetworkThread() {
        return networkThread;
    }

    /**
     * @param networkThread The networkThread to set.
     */
    public void setNetworkThread(final NetworkThread networkThread) {
        this.networkThread = networkThread;
    }

    /**
     * @return Returns the workspace.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * @param workspace The workspace to set.
     */
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }
}
