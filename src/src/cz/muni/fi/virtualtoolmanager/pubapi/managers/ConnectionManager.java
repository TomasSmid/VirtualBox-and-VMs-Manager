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
 *
 * @author Tomáš Šmíd
 */
public interface ConnectionManager {
    
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine);
    
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine, long millis);
    
    public void disconnectFrom(PhysicalMachine physicalMachine);
    
    public void disconnectFrom(PhysicalMachine physicalMachine, ClosingActionType closingAction);
    
    public boolean isConnected(PhysicalMachine physicalMachine);
    
    public List<PhysicalMachine> getConnectedPhysicalMachines();
    
    public void close();
}
