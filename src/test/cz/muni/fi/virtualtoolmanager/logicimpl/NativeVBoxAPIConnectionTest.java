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
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * This test class ensure unit testing of class NativeVBoxAPIConnection and
 * is intended to be a pointer that class NativeVBoxAPIConnection works as expected.
 * 
 * @author Tomáš Šmíd
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(VirtualBoxManager.class)
public class NativeVBoxAPIConnectionTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    //The object of this class is going to be under the tests (System Under Test)
    private NativeVBoxAPIConnection sut;
    //The key object for class NativeVBoxAPIConnection a its method connectTo(),
    //which is needed to be mocked for isolated and correct testing
    private VirtualBoxManager vbmMock;
                                      
    
    @Before
    public void setUp(){
        //to get a required instance of type VirtualBoxManager it is needed to mock static methods
        //of class VirtualBoxManager because of the static build factory VirtualBoxManager::createInstance()
        PowerMockito.mockStatic(VirtualBoxManager.class);
        //mock object of type VirtualBoxManager for easier and faster testing (for the void methods there is
        //the default setting equal to _doNothing()_)
        vbmMock = mock(VirtualBoxManager.class);
        //there is always returned a mock object of type VirtualBoxManager in order to have easier and faster
        //testing when the static method VirtualBoxManager::createInstance() is called
        when(VirtualBoxManager.createInstance(null)).thenReturn(vbmMock);
        sut = new NativeVBoxAPIConnection();
    }
    
    /**
     * This test tests that there is not unexpectedly invoked any exception when
     * the method NativeVBoxAPIConnection::connect() is called with a valid and
     * existing input parameter of type PhysicalMachine.
     * 
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     */
    @Test
    public void connectToValidExistingPhysicalMachine() throws ConnectionFailureException,
                                                               IncompatibleVirtToolAPIVersionException{
        //represents a physical machine to which should manage to connect to
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();         
        //fact that vboxMock is mocked object means the real methods of class IVirtualBox are not 
        //called and in this case nothing happens because no further setting is done
        IVirtualBox vboxMock = mock(IVirtualBox.class);        
                
        //there should be returned a mock object of type IVirtualBox for easier and faster testing
        //when the method VirtualBoxManager::getVBox() is called
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a string with value "4_3" when there is query for the API version
        //of VirtualBox and represents a valid API version
        when(vboxMock.getAPIVersion()).thenReturn("4_3");
        
        
        //nothing should happen after the method connectTo() is called () (no exception invoked)
        sut.connectTo(pm);
    }
    
    /**
     * Tests that there is ConnectionFailureException exception invoked when there
     * is made an attempt to connect to physical machine with correct (existing)
     * IP address, web server port of VirtualBox and user password, but with
     * incorrect username (with the condition that for the web server port was used
     * this setting: "vboxmanage setproperty websrvauthlibrary default").
     * 
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     */
    @Test
    public void connectToExistingPhysicalMachineWithIncorrectUsername() throws ConnectionFailureException,
                                                                               IncompatibleVirtToolAPIVersionException{
        //represents physical machine having incorrect username for its IP address
        PhysicalMachine incoPM = new PMBuilder().build();
        //represents physical machine having correct username for its IP address
        PhysicalMachine coPM = new PMBuilder().username("Mark").build();
        
        //if the method VirtualBoxManager::connect() is called with physical machine incoPM,
        //then VBoxException will be thrown
        doThrow(VBoxException.class).when(vbmMock).connect(anyString(), eq("Jack"), anyString());
        
        //exception with error message that username is incorrect is expected to be invoked
        exception.expect(ConnectionFailureException.class);
        sut.connectTo(incoPM);
        
        //nothing should happen (no exception invoked),
        exception = ExpectedException.none();
        sut.connectTo(coPM);
    }
    
    /**
     * Tests that there is ConnectionFailureException exception invoked when there is made an attempt to
     * connect to physical machine with correct (existing) IP address, web server port of VirtualBox and
     * username, but with incorrect user password (with the condition that for the web server port was used
     * this setting: "vboxmanage setproperty websrvauthlibrary default").
     * 
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     */
    @Test
    public void connectToExistingPhysicalMachineWithIncorrectUserPassword() throws ConnectionFailureException,
                                                                                   IncompatibleVirtToolAPIVersionException{
        //represents physical machine having incorrect user password for its IP address
        PhysicalMachine incoPM = new PMBuilder().build();
        //represents physical machine having correct user password for its IP address
        PhysicalMachine coPM = new PMBuilder().userPassword("1100215asd").build();
        
        //if the method VirtualBoxManager::connect() is called with physical machine incoPM,
        //then VBoxException will be thrown
        doThrow(VBoxException.class).when(vbmMock).connect(anyString(), anyString(), eq("tr1h15jk7"));
        
        //exception with error message that user password is incorrect is expected to be invoked
        exception.expect(ConnectionFailureException.class);
        sut.connectTo(incoPM);
        
        //nothing should happen (no exception invoked), in fact everything should be done in a right way
        exception = ExpectedException.none();
        sut.connectTo(coPM);
    }
    
    /**
     * Tests that there is ConnectionFailureException exception invoked when there is made an attempt to
     * connect to a physical machine with not existing IP address in network or with incorrect number of
     * web server port of VirtualBox.
     * 
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     */
    @Test
    public void connectToNotExistingPhysicalMachine() throws ConnectionFailureException,
                                                             IncompatibleVirtToolAPIVersionException{
        //represents a physical machine with which should be the connection established
        PhysicalMachine pm = new PMBuilder().build();
        //if the method VirtualBoxManager::connect() is called with physical machine pm, then it will
        //throw VBoxException as a result
        doThrow(VBoxException.class).when(vbmMock).connect("http://180.148.14.10:18083", "Jack", "tr1h15jk7");
        
        //ConnectionFailureException exception with error message that IP address or web server port number
        //was used is expected to be thrown as a result
        exception.expect(ConnectionFailureException.class);
        sut.connectTo(pm);
    }
    
    /**
     * Tests that there is ConnectionFailureException exception invoked when the connection with physical
     * machine is not successfully established after 3 attempts.
     * 
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
     * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
     */
    @Test
    public void connectToInaccessiblePhysicalMachine() throws ConnectionFailureException,
                                                              IncompatibleVirtToolAPIVersionException{
        //represents a physical machine with which should be established the connection
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        
        //each time when the method VirtualBoxManager::connect() is called with physical machine pm,
        //then there is thrown VBoxException as a result and means unsuccess
        doThrow(VBoxException.class).when(vbmMock).connect(url, pm.getUsername(), pm.getUserPassword());
        
        //there is ConnectionFailureException exception expected to be invoked
        exception.expect(ConnectionFailureException.class);
        sut.connectTo(pm);
    }
    
    /**
    * Tests that there is IncompatibleVirtToolAPIVersionException exception invoked when API version
    * of VirtualBox does not match the required version.
    * 
    * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException
    * @throws cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException
    */
    @Test
    public void connectToWithIncompatibleVBoxAPIVersion() throws ConnectionFailureException,
                                                                 IncompatibleVirtToolAPIVersionException{
        //represents a physical machine with which should be established a connection
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type IVirtualBox for easier and faster testing
        IVirtualBox vboxMock = mock(IVirtualBox.class);
        
        //there should be returned a mock object of type IVirtualBox when the method VirtualBoxManager::getVBox()
        //is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        
        //if the method getAPIVersion() is called, then string with the value "4_2" will be returned as a result
        when(vboxMock.getAPIVersion()).thenReturn("4_2");
                                                         
        //exception of type IncompatibleVirtToolAPIVersionException is expected to be invoked when the method
        //connectTo() is called because of incompatible VBox API version 4.2, but required is 4.3
        exception.expect(IncompatibleVirtToolAPIVersionException.class);
        sut.connectTo(pm);
    }
    
    /**
     * Class Builder for easier and faster creating and setting up new object
     * of type PhysicalMachine.
     */
    class PMBuilder{
        private String addressIP = "180.148.14.10";
        private String portOfVBoxWebServer = "18083";
        private String username = "Jack";
        private String userPassword = "tr1h15jk7";

        public PMBuilder(){

        }

        public PMBuilder addressIP(String value){
            this.addressIP = value;
            return this;
        }

        public PMBuilder webserverPort(String value){
            this.portOfVBoxWebServer = value;
            return this;
        }

        public PMBuilder username(String value){
            this.username = value;
            return this;
        }

        public PMBuilder userPassword(String value){
            this.userPassword = value;
            return this;
        }

        public PhysicalMachine build(){
            return new PhysicalMachine(this.addressIP,this.portOfVBoxWebServer,
                                       this.username,this.userPassword);
        }
    }
    
}

