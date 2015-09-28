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

import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Tomáš Šmíd
 */
public interface VirtualizationToolManager {
    
    public void registerVirtualMachine(String name);
    
    public VirtualMachine findVirtualMachineById(UUID id);
    
    public VirtualMachine findVirtualMachineByName(String name);
    
    public List<VirtualMachine> getVirtualMachines();
    
    public void removeVirtualMachine(VirtualMachine virtualMachine);
    
    public VirtualMachine cloneVirtualMachine(VirtualMachine virtualMachine, CloneType type);
    
    public VirtualMachineManager getVirtualMachineManager();
    
    public void close();
}
