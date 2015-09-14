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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Tomáš Šmíd
 */
public class VirtualizationToolManagerImpl implements VirtualizationToolManager{
    
    private final NativeVBoxAPIManager nativeVBoxAPIManager;
    private final ConnectionManager connectionManager;
    private final PhysicalMachine hostMachine;
    
    VirtualizationToolManagerImpl(PhysicalMachine hostMachine){
        this(hostMachine, new NativeVBoxAPIManager(), new ConnectionManagerImpl());
    }
    
    VirtualizationToolManagerImpl(PhysicalMachine hostMachine, NativeVBoxAPIManager nativeVBoxAPIManager,
                                  ConnectionManager connectionManager){
        this.hostMachine = hostMachine;
        this.nativeVBoxAPIManager = nativeVBoxAPIManager;
        this.connectionManager = connectionManager;
    }
    
    @Override
    public void registerVirtualMachine(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VirtualMachine findVirtualMachineById(UUID id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VirtualMachine findVirtualMachineByName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<VirtualMachine> getVirtualMachines() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeVirtualMachine(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VirtualMachine cloneVirtualMachine(VirtualMachine virtualMachine, CloneType type) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public VirtualMachineManager getVirtualMachineManager() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
