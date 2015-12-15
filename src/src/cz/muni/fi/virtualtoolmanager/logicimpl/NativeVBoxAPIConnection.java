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
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;



/**
 * This class is used as an interface for the VirtualBox native methods for
 * connection establishment. In fact, this class and its one method connectTo()
 * is primarily used for testing whether: 1. A physical machine with which is
 * required to establish a connection is accessible and can be connected to (it
 * has a valid key values - IP address, number of web server port of VirtualBox,
 * username and user password - and the web server port is running and the network
 * connection is working properly) / 2. The API version of virtualization tool
 * VirtualBox (the required API version is 4.3., with another API versions this
 * created API for remote virtual machine control has not to work properly or at
 * all).
 * 
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIConnection {
    /**
     * This method ensures the connection establishment with a required physical
     * machine.
     * 
     * @param physicalMachine represents a physical machine with which is required
     * to establish a connection in order to remotely control virtual machines located
     * on this physical machine
     * @throws ConnectionFailureException is invoked if the physical machine with which
     * is required to establish a connection has any key value (IP address, number of
     * web server port, username or user password) wrong or if there is not possible
     * to connect to the physical machine because of the network connection which is
     * not working properly or because of not running web server     
     * @throws IncompatibleVirtToolAPIVersionException is invoked if there is a wrong
     * API version of virtualization tool VirtualBox on the physical machine
     */ 
    public void connectTo(PhysicalMachine physicalMachine) throws ConnectionFailureException,
                                                                  IncompatibleVirtToolAPIVersionException{
            //url of the physical machine used for connecting to it
            String url = getURL(physicalMachine);
            //object from the native VirtualBox API which manages the connection establishment
            VirtualBoxManager virtualBoxManager = VirtualBoxManager.createInstance(null);
            
            try{
                //connect to the VirtualBox web server which should be running on the physical machine physicalMachine
                virtualBoxManager.connect(url, physicalMachine.getUsername(),
                                          physicalMachine.getUserPassword());
            }catch (VBoxException ex){//there occured any problem while connecting to the physical machine, more info at head of the method connectTo()
                //ends the connection (if successful) with the physical machine and cleans up after itself
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new ConnectionFailureException("Connection operation failure: "
                        + "Unable to establish a connection with a physical machine "
                        + physicalMachine + ". Most probably there occured one of "
                        + "these problems: 1. Network connection is not working "
                        + "properly or at all / 2. Physical machine has some key "
                        + "value (IP address, number of VirtualBox web server port, "
                        + "username or user password) wrong / 3. There is not running "
                        + "a VirtualBox web server on physical machine " 
                        + physicalMachine + ".");
            }
            
            IVirtualBox vbox = virtualBoxManager.getVBox();
            
            if(!vbox.getAPIVersion().equals("4_3")){
                //ends the connection with the physical machine and cleans up after itself
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new IncompatibleVirtToolAPIVersionException("Incompatible version "
                        + "of VirtualBox API: The required VirtualBox API version is 4_3, "
                        + "but the actual VirtualBox API version is " + vbox.getAPIVersion()
                        + ". There is no guarantee this API would work with incorrect "
                        + "VirtualBox API version correctly, that's why this physical machine "
                        + "has not been connected and thus there cannot be done any work with "
                        + "virtual machines.");
            }
            
            //ends the connection with the physical machine and cleans up after itself - now
            //the physical machine becomes one of the successfully connected physical machines
            //and there is possible to work with virtual machines located on this physical
            //machine
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
    }
    
    private String getURL(PhysicalMachine physicalMachine){
        if(physicalMachine.getAddressIP().contains(".")){
            return "http://" + physicalMachine.getAddressIP() + ":"
                    + physicalMachine.getPortOfVTWebServer();
        }
        
        return "http://[" + physicalMachine.getAddressIP() + "]:"
                + physicalMachine.getPortOfVTWebServer();
    }
}
