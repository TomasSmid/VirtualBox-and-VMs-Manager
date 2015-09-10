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
import java.util.List;



/**
 *
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIMachine {
    
    private static final NativeVBoxAPIMachine INSTANCE = new NativeVBoxAPIMachine();
    
    public static NativeVBoxAPIMachine getInstance(){
        return INSTANCE;
    }
    
    private NativeVBoxAPIMachine(){
        
    }
    
    public void startVM(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public void shutDownVM(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public void addPortRule(VirtualMachine virtualMachine, PortRule portRule) {
        throw new UnsupportedOperationException("Unsupported operation");        
    }
    
    public void deletePortRule(VirtualMachine virtualMachine, String ruleName) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public List<PortRule> getPortRules(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    public String getVMState(VirtualMachine virtualMachine) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
    
    /*private void checkVMIsNotNull(VirtualMachine vm, String errMsg){
        if(vm == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPMIsNotNull(PhysicalMachine pm, String errMsg){
        if(pm == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkVMIdIsNotNullNorEmpty(UUID id, String errMsg){
        if(id == null || id.toString().isEmpty()){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkVMNameIsNotNullNorEmpty(String name, String errMsg){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPMIsConnected(PhysicalMachine pm, String errMsg) throws UnexpectedVMStateException{
        NativeVBoxAPIConnection natapiCon = NativeVBoxAPIConnection.getInstance();
        
        if(!natapiCon.isConnected(pm)){
            throw new UnexpectedVMStateException(errMsg);
        }
    }
    
    private void checkVMStateIsValidForStart(MachineState state, String errMsg) throws UnexpectedVMStateException{
        switch(state){
            case Running:
            case Paused : throw new UnexpectedVMStateException(errMsg);
            default     : break;
        }
    }
    
    private void checkVMStateIsValidForShutdown(MachineState state, String errMsg) throws UnexpectedVMStateException{
        switch(state){
            case Running:
            case Paused :
            case Stuck  : break;
            default     : throw new UnexpectedVMStateException(errMsg);
        }
    }
    
    private void checkPortRuleValidity(VirtualMachine virtualMachine, PortRule portRule, IMachine vboxMachine) throws PortRuleDuplicityException{
        String errMsgForPRNullCheck = "Creating new port forwarding rule failure: There was made an attempt to create a null port forwarding rule for virtual machine " + virtualMachine + ".";
        String errMsgForPRNameCheck = "Creating new port forwarding rule failure: Name of port rule " + portRule + " is null or empty.";
        String errMsgForPRHostPortCheck = "Creating new port forwarding rule failure: Host port number of new port forwarding rule " + portRule + " is negative or too big. Host port number can be from the range 0-65535.";
        String errMsgForPRGuestPortCheck = "Creating new port forwarding rule failure: Guest port number of new port forwarding rule " + portRule + " is negative or too big. Guest port number can be from the range 0-65535.";
        String errMsgForPRNameDuplicityCheck = "Crating new port forwarding rule failure: There already exists port forwarding rule with name = " + portRule.getName() + " on virtual machine " + virtualMachine + ".";
        String errMsgForPRHPDuplicityCheck = "Creating new port forwarding rule failure: There already exists port forwarding rule using host port number = " + portRule.getHostPort() + " on virtual machine " + virtualMachine + ".";
        
        checkPortRuleIsNotNull(portRule,errMsgForPRNullCheck);
        checkPortRuleNameIsNotNullNorEmpty(portRule.getName(),errMsgForPRNameCheck);
        checkPortRuleHostOrGuestPortIsValid(portRule.getHostPort(),errMsgForPRHostPortCheck);
        checkPortRuleHostOrGuestPortIsValid(portRule.getGuestPort(),errMsgForPRGuestPortCheck);
        checkPortRuleNameDuplicity(vboxMachine, portRule.getName(),errMsgForPRNameDuplicityCheck);
        checkPortRuleHostPortDuplicity(vboxMachine, portRule.getHostPort(),errMsgForPRHPDuplicityCheck);
    }
    
    private void checkPortRuleIsNotNull(PortRule portRule, String errMsg){
        if(portRule == null){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPortRuleNameIsNotNullNorEmpty(String name, String errMsg){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPortRuleHostOrGuestPortIsValid(int port, String errMsg){
        if(port < 0 || port > 65535){
            throw new IllegalArgumentException(errMsg);
        }
    }
    
    private void checkPortRuleNameDuplicity(IMachine vboxMachine, String name, String errMsg) throws PortRuleDuplicityException{
        INetworkAdapter adapter = vboxMachine.getNetworkAdapter(0L);
        INATEngine natEngine = adapter.getNATEngine();
        List<String> redirects = natEngine.getRedirects();
        
        for(String redirect : redirects){
            String[] parts = redirect.split(",");
            if(parts[0].equals(name)){
                throw new PortRuleDuplicityException(errMsg);
            }
        }
    }
    
    private void checkPortRuleHostPortDuplicity(IMachine vboxMachine, int port, String errMsg) throws PortRuleDuplicityException{
        INetworkAdapter adapter = vboxMachine.getNetworkAdapter(0L);
        INATEngine natEngine = adapter.getNATEngine();
        List<String> redirects = natEngine.getRedirects();
        
        for(String redirect : redirects){
            String[] parts = redirect.split(",");
            int redHostPort = Integer.parseInt(parts[3]);
            if(redHostPort == port){
                throw new PortRuleDuplicityException(errMsg);
            }
        }
    }
    
    private PortRule redirectToPortRule(String redirect){
        String parts[] = redirect.split(",");
        String name = parts[0];
        ProtocolType protocol = ProtocolType.valueOf(parts[1]);
        String hostIP = parts[2];
        int hostPort = Integer.parseInt(parts[3]);
        String guestIP = parts[4];
        int guestPort = Integer.parseInt(parts[5]);
        
        return new PortRule.Builder(name, hostPort, guestPort).protocol(protocol)
                           .hostIP(hostIP).guestIP(guestIP).build();
    }*/
}
