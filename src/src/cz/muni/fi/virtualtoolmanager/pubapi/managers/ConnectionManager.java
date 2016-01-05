/*
 * Copyright 2015 Tomáš Šmíd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.muni.fi.virtualtoolmanager.pubapi.managers;

import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ClosingActionType;
import java.util.List;

/**
 * <div>
 * Interface that declare what method the implementation of the connection manager
 * should provide.
 * </div>
 * <div>
 * By implementing the declared methods, it should be possible to remotely
 * access a particular physical machine with the virtualization tool VirtualBox.
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.ConnectionManagerImpl
 * @see cz.muni.fi.virtualtoolmanager.pubapi.types.ClosingActionType
 * 
 * @author Tomáš Šmíd
 */
public interface ConnectionManager {
    
    /**
     * <div>
     * Method that ensures the connection establishment with the required physical
     * machine on which is present the VirtualBox installation with the web
     * server that must be running at the moment when there is made an attempt to
     * establish the connection with the physical machine.
     * </div>
     * <div>
     * <p>
     * The physical machine with which should be the connection established
     * is defined by input parameter of type
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine PhysicalMachine}.
     * <p>
     * If the physical machine is connected successfully, then as the result of
     * the connection operation is returned an object of type
     * {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
     * VirtualizationToolManager} and the operations with the newly accessible
     * virtual machines can be performed.
     * <p>
     * The connected physical machine is connected until any disconnection
     * operation is called for that physical machine.
     * </div>
     * @param physicalMachine represents host machine which is going to get connected
     * @return manager of type {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
     * VirtualizationToolManager} for managing virtual machines on the newly
     * connected physical machine
     */
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine);
    
    /**
     * <div>
     * Method that is same as {@link #connectTo(PhysicalMachine)}, but with this
     * method is possible to define the time in milliseconds which determine the
     * waiting time between each unsuccessful connection establishment attempt.
     * </div>
     * @param physicalMachine represents host machine which is going to get connected
     * @param millis time in milliseconds determining the waiting time between
     * each unsuccessful connection establishment attempt
     * @return manager of type {@link cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
     * VirtualizationToolManager} for managing virtual machines on the newly
     * connected physical machine
     */
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine, long millis);
    
    /**
     * <div>
     * Method that ensures the disconnection of a particular physical machine.
     * </div>
     * <div>
     * <p>
     * After the method call there cannot be performed any operation with
     * virtual machines from the disconnected physical machine.
     * </div>
     * @param physicalMachine represents the host machine which is going to be
     * disconnected
     */
    public void disconnectFrom(PhysicalMachine physicalMachine);
    
    /**
     * <div>
     * Method that is same as {@link #disconnectFrom(PhysicalMachine)}, but furthermore
     * there can be specified if and what final actions shall be performed during
     * the physical machine disconnection operation.
     * </div>
     * @param physicalMachine represents the host machine which is going to be
     * disconnected
     * @param closingAction represents the final action which should be performed
     * during the disconnection operation
     */
    public void disconnectFrom(PhysicalMachine physicalMachine, ClosingActionType closingAction);
    
    /**
     * <div>
     * Method that checks the queried physical machine is connected or not.
     * </div>
     * @param physicalMachine represents the queried physical machine
     * @return <code>true</code> if the queried physical machine is connected,
     * <code>false</code> otherwise
     */
    public boolean isConnected(PhysicalMachine physicalMachine);
    
    /**
     * <div>
     * Method that gets all the connected physical machines at the moment of the
     * method call.
     * </div>
     * @return list of all the connected physical machines
     */
    public List<PhysicalMachine> getConnectedPhysicalMachines();
    
    /**
     * <div>
     * Method that ensures the termination of the code, respectively after this method
     * call are all connected physical machines disconnected and no virtual
     * machine operation can be performed.
     * </div>
     */
    public void close();
}
