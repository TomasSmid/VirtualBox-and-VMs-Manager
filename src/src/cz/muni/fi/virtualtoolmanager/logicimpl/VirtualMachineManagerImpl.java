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

import cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.ConnectionManager;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager;
import java.util.List;

/**
 *
 * @author Tomáš Šmíd
 */
public class VirtualMachineManagerImpl implements VirtualMachineManager{

    private NativeVBoxAPIMachine nativeVBoxAPIMachine;
    private ConnectionManager connectionManager;
    
    public VirtualMachineManagerImpl(){
        this(new NativeVBoxAPIMachine(), new ConnectionManagerImpl());
    }
    
    VirtualMachineManagerImpl(NativeVBoxAPIMachine natAPIMachine, ConnectionManager conMan){
        this.nativeVBoxAPIMachine = natAPIMachine;
        this.connectionManager = conMan;
    }
    
    @Override
    public void startVM(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void shutDownVM(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addPortRule(VirtualMachine virtualMachine, PortRule rule) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deletePortRule(VirtualMachine virtualMachine, PortRule rule) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deleteAllPortRules(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<PortRule> getPortRules(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getVMState(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
