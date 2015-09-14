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
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tomáš Šmíd
 */
public class ConnectionManagerImpl implements ConnectionManager{

    private final ConnectedPhysicalMachines connectedPhysicalMachines;
    private final NativeVBoxAPIConnection nativeVBoxAPIConnection;
    private final NativeVBoxAPIManager nativeVBoxAPIManager;
            
    public ConnectionManagerImpl(){
        this(ConnectedPhysicalMachines.getInstance(), new NativeVBoxAPIConnection(),
             new NativeVBoxAPIManager());
    }
    
    ConnectionManagerImpl(ConnectedPhysicalMachines cpm, NativeVBoxAPIConnection natapiCon,
                          NativeVBoxAPIManager natapiMan){
        
        this.connectedPhysicalMachines = cpm;
        this.nativeVBoxAPIConnection = natapiCon;
        this.nativeVBoxAPIManager = natapiMan;
        
    }
    
    @Override
    public VirtualizationToolManager connectTo(PhysicalMachine physicalMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void disconnectFrom(PhysicalMachine physicalMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isConnected(PhysicalMachine physicalMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<PhysicalMachine> getConnectedPhysicalMachines() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
