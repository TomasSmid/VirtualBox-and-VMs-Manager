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
    private static List<PhysicalMachine> accessedPhysicalMachines = new ArrayList<>();
    
    public static ConnectedPhysicalMachines getInstance(){
        return INSTANCE;
    }
    
    /*private static void addAPM(PhysicalMachine physicalMachine){
        accessedPhysicalMachines.add(physicalMachine);
    }
    
    private static boolean removeAPM(PhysicalMachine physicalMachine){       
        accessedPhysicalMachines.remove(physicalMachine);
        return true;
    }
    
    private static boolean isAccessedPM(PhysicalMachine physicalMachine){
        return accessedPhysicalMachines.contains(physicalMachine);
    }
    
    private static List<PhysicalMachine> getAccessedPMs(){
        return accessedPhysicalMachines;
    }*/
    
    private ConnectedPhysicalMachines(){ }
    
    public void add(PhysicalMachine physicalMachine){
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public boolean remove(PhysicalMachine physicalMachine){
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public boolean isConnected(PhysicalMachine physicalMachine){
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public List<PhysicalMachine> getConnectedPhysicalMachines(){
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
