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
 * <div>
 * Class that is used to call native methods of VirtualBox API in order to ensure
 * connection to the VirtualBox web server (service), which is present on
 * a particular physical (host) machine, so that there could be possible to
 * manage virtual machines remotely.
 * </div>
 * 
 * @author Tomáš Šmíd
 */
class NativeVBoxAPIConnection {
    /**
     * <div>
     * <p>
     * This method ensures the connection establishment with a required physical
     * machine and its VirtualBox web server (service).
     * <p>
     * In fact, this method just tests that a physical machine, which is required
     * to be used to establish a connection with in order to manage its virtual
     * machines remotely, is accessible and can be connected.
     * <p>
     * If the connection operation is finished successfully, then the physical
     * machine is disconnected, but still acts like a connected one and can be
     * used for virtual machines remote control (before each virtual machine 
     * operation is made a new connection to the physical machine).
     * <p>
     * If the required physical machine and its virtualization tool VirtualBox
     * cannot be used for the virtual machine remote control, then it is caused
     * by some of the failures during the connection establishment with the
     * physical machine. In that case there can be thrown one of the following
     * exceptions:
     * <ul>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * ConnectionFailureException} - </strong>thrown when there cannot be
     * the connection with the physical machine established and one of the following
     * problems occur:
     * <ol>
     * <li>Physical machine given as the input parameter of this method has some
     * key value (IP address, port number of VirtualBox web server, username and
     * user password) incorrect.
     * <li>VirtualBox web server is not running at the moment of the connection
     * establishment.
     * <li>Network connection is not working properly or at all.
     * </ol>
     * <li><strong>{@link cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     * IncompatibleVirtToolAPIVersionException} - </strong>thrown when the API
     * version of VirtualBox installation on the physical machine, which
     * there is being established the connection with, is incorrect (different
     * from the correct API version 4.3).
     * </ul>
     * </div>
     * 
     * @param physicalMachine represents a physical machine with which is required
     * to establish a connection in order to control virtual machines located
     * on this physical machine remotely
     */ 
    void connectTo(PhysicalMachine physicalMachine){
            //url of the physical machine used for connecting to it
            String url = getURL(physicalMachine);
            //object from the native VirtualBox API which manages
            //the connection establishment
            VirtualBoxManager virtualBoxManager =
                    VirtualBoxManager.createInstance(null);
            
            try{
                //connect to the VirtualBox web server which should be running
                //on the physical machine physicalMachine
                virtualBoxManager.connect(url, physicalMachine.getUsername(),
                                          physicalMachine.getUserPassword());
            }catch (VBoxException ex){//there occured some problem while connecting to the physical machine
                //ends the connection (if successful) with the physical
                //machine and cleans up after itself
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
            
            //get instance of VirtualBox installation
            IVirtualBox vbox = virtualBoxManager.getVBox();
            
            //check the VirtualBox API version is correct
            if(!vbox.getAPIVersion().equals("4_3")){
                //ends the connection with the physical machine and cleans up after itself
                virtualBoxManager.disconnect();
                virtualBoxManager.cleanup();
                throw new IncompatibleVirtToolAPIVersionException("Incompatible "
                        + "version of VirtualBox API: The required VirtualBox "
                        + "API version is 4_3, but the actual VirtualBox API "
                        + "version is " + vbox.getAPIVersion() + ". There is no "
                        + "guarantee this API would work with incorrect "
                        + "VirtualBox API version correctly - physical machine "
                        + physicalMachine + " has not been connected.");
            }
            
            //ends the connection with the physical machine and cleans up after 
            //itself - now the physical machine becomes one of the successfully
            //connected physical machines and there is possible to work with 
            //virtual machines located on this physical machine
            virtualBoxManager.disconnect();
            virtualBoxManager.cleanup();
    }
    
    /**
     * This method creates the correct form of url from the given IP address
     * and the port number of the VirtualBox web server. Thanks this method
     * it is possible to connect to the physical machine and its VirtualBox
     * web server with IPv4 or IPv6.
     * 
     * @param physicalMachine represents the physical machine whose IP address
     * and port number of VirtualBox web server will be used for new url creation
     * @return newly created url defining the physical machine
     */
    private String getURL(PhysicalMachine physicalMachine){
        if(physicalMachine.getAddressIP().contains(".")){
            return "http://" + physicalMachine.getAddressIP() + ":"
                    + physicalMachine.getPortOfVTWebServer();
        }
        
        return "http://[" + physicalMachine.getAddressIP() + "]:"
                + physicalMachine.getPortOfVTWebServer();
    }
}
