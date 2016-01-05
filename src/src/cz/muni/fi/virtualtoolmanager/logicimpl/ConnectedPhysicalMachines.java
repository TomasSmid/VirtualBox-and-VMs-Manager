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
package cz.muni.fi.virtualtoolmanager.logicimpl;

import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that is used to hold overview about the all connected physical machines.
 * It is inspired by the singleton pattern, but in this case it is not singleton -
 * - it uses static builder factory, but also it has one attribute except the
 * instance attribute. But in a time, there always exists just one instance
 * of this class.
 * 
 * @author Tomáš Šmíd
 */
class ConnectedPhysicalMachines {
    /** The only instance of this class*/
    private static final ConnectedPhysicalMachines INSTANCE = new ConnectedPhysicalMachines();
    /** list of all connected physical machines */
    private List<PhysicalMachine> connectedPhysicalMachines = new ArrayList<>();
    
    /**
     * This method represents so called static builder factory, which is used to
     * get the instance of this class.
     * @return object of type of this class
     */
    public static ConnectedPhysicalMachines getInstance(){
        return INSTANCE;
    }
    
    private ConnectedPhysicalMachines(){ }
    
    /**
     * This method adds a new physical machine to the list of connected physical
     * machines.
     * If there occurs any error it can throw the following exceptions:
     *  • IllegalArgumentException - when the given physical machine is null
     * @param physicalMachine represents the physical machines which is going to
     * be added to the list of connected physical machines
     */
    public void add(PhysicalMachine physicalMachine){
        if(physicalMachine == null){
            throw new IllegalArgumentException("There was made an attempt to add "
                            + "a null physical machine to the list of connected "
                            + "physical machines.");
        }
        
        connectedPhysicalMachines.add(physicalMachine);
    }
    
    /**
     * This method removes a particular physical machine from the list of
     * connected physical machines.
     * If there occurs any error it can throw the following exceptions:
     *  • IllegalArgumentException - when the given physical machine is null
     * @param physicalMachine represents the physical machine which is going to
     * be removed from the list of connected physical machines
     * @return true if the physical machine was present in the list and was 
     * removed successfully, false otherwise
     */
    public boolean remove(PhysicalMachine physicalMachine){
        if(physicalMachine == null){
            throw new IllegalArgumentException("There was made an attempt to remove "
                            + "a null physical machine from the list of connected "
                            + "physical machines.");
        }
        
        return connectedPhysicalMachines.remove(physicalMachine);
    }
    
    /**
     * This method checks the physical machine is present in the list of
     * connected physical machines.
     * @param physicalMachine represents a queried physical machine
     * @return true if the physical machine is one of the connected physical
     * machines, false otherwise
     */
    public boolean isConnected(PhysicalMachine physicalMachine){
        if(physicalMachine == null){
            throw new IllegalArgumentException("There was made an attempt to find "
                            + "out if a null physical machine is connected.");
        }
        
        return connectedPhysicalMachines.contains(physicalMachine);
    }
    
    /**
     * Gets the list of all connected physical machines.
     * @return list of all connected physical machines
     */
    public List<PhysicalMachine> getConnectedPhysicalMachines(){
        return connectedPhysicalMachines;
    }
}
