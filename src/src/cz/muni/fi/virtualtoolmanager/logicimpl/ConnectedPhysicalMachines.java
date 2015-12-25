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
 *
 * @author Tomáš Šmíd
 */
public class ConnectedPhysicalMachines {
    
    private static final ConnectedPhysicalMachines INSTANCE = new ConnectedPhysicalMachines();
    private List<PhysicalMachine> connectedPhysicalMachines = new ArrayList<>();
    
    public static ConnectedPhysicalMachines getInstance(){
        return INSTANCE;
    }
    
    private ConnectedPhysicalMachines(){ }
    
    public void add(PhysicalMachine physicalMachine){
        if(physicalMachine == null){
            throw new IllegalArgumentException("There was made an attempt to add "
                            + "a null physical machine to the list of connected "
                            + "physical machines.");
        }
        
        connectedPhysicalMachines.add(physicalMachine);
    }
    
    public boolean remove(PhysicalMachine physicalMachine){
        if(physicalMachine == null){
            throw new IllegalArgumentException("There was made an attempt to remove "
                            + "a null physical machine from the list of connected "
                            + "physical machines.");
        }
        
        return connectedPhysicalMachines.remove(physicalMachine);
    }
    
    public boolean isConnected(PhysicalMachine physicalMachine){
        if(physicalMachine == null){
            throw new IllegalArgumentException("There was made an attempt to find "
                            + "out if a null physical machine is connected.");
        }
        
        return connectedPhysicalMachines.contains(physicalMachine);
    }
    
    public List<PhysicalMachine> getConnectedPhysicalMachines(){
        return connectedPhysicalMachines;
    }
}
