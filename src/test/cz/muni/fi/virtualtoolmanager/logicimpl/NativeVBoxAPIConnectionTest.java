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
import static org.mockito.Mockito.*;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;

/**
 * This test class ensure unit testing of class NativeVBoxAPIConnection and
 * is intended to be a pointer that class NativeVBoxAPIConnection works in
 * a required way.
 * 
 * @author Tomáš Šmíd
 */
public class NativeVBoxAPIConnectionTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private NativeVBoxAPIConnection sut;//The object of this class is going to be under the tests
    private VirtualBoxManager vbmMock;//The key object for class NativeVBoxAPIConnection a its method connectTo(),
                                      //which is needed to be mocked for isolated and correct testing
    
    @Before
    public void setUp(){        
        vbmMock = mock(VirtualBoxManager.class);
        sut = new NativeVBoxAPIConnection(vbmMock);
    }
    
    /**
     * Tests that there is not unexpectedly invoked any exception when the method connect() from class
     * VirtualBoxManager is called with a valid and existing input parameter of type PhysicalMachine.
     */
    @Test
    public void connectToValidExistingPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        IVirtualBox vboxMock = mock(IVirtualBox.class);//fact that vboxMock is mocked object means the real
                                                       //methods of class IVirtualBox are not called and in this
                                                       //case nothing happens because no further setting is done
        
        //nothing should happen after the method connectTo() is called () (no exception invoked)
        sut.connectTo(pm);
    }
    
    /**
     * Tests that there is ConnectionFailureException exception invoked when there is made an attempt to
     * connect to physical machine with correct (existing) IP address, web server port of VirtualBox and
     * user password, but with incorrect username.
     */
    @Test
    public void connectToExistingPhysicalMachineWithIncorrectUsername(){
        PhysicalMachine incoPM = new PMBuilder().build();//represents physical machine having incorrect username for its IP address
        PhysicalMachine coPM = new PMBuilder().username("Mark").build();//represents physical machine having correct username for its IP address        
        
        //if the method connect() is called with physical machine incoPM, then VBoxException will be thrown
        doThrow(VBoxException.class).when(vbmMock).connect(anyString(), eq("Jack"), anyString());
        
        //exception with error message that username is incorrect is expected to be invoked
        exception.expect(ConnectionFailureException.class);
        sut.connectTo(incoPM);
        
        //nothing should happen (no exception invoked), in fact everything should be done in a right way
        exception = ExpectedException.none();
        sut.connectTo(coPM);
    }
    
    /**
     * Tests that there is ConnectionFailureException exception invoked when there is made an attempt to
     * connect to physical machine with correct (existing) IP address, web server port of VirtualBox and
     * username, but with incorrect user password.
     */
    @Test
    public void connectToExistingPhysicalMachineWithIncorrectUserPassword(){
        PhysicalMachine incoPM = new PMBuilder().build();//represents physical machine having incorrect user password for its IP address
        PhysicalMachine coPM = new PMBuilder().username("Mark").build();//represents physical machine having correct user password for its IP address        
        
        //if the method connect() is called with physical machine incoPM, then VBoxException will be thrown
        doThrow(VBoxException.class).when(vbmMock).connect(anyString(), eq("Jack"), anyString());
        
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
     */
    @Test
    public void connectToNotExistingPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        //if the method connect() of class VirtualBoxManager is called with physical machine pm, then it will
        //throw VBoxException as a result
        doThrow(VBoxException.class).when(vbmMock).connect("http://180.148.14.10:18083", "Jack", "tr1h15jk7");
        
        //ConnectionFailureException exception with error message that IP address or web server port number
        //was used is expected to be thrown as a result
        exception.expect(ConnectionFailureException.class);
        sut.connectTo(pm);
    }
    
    /**
     * Tests that there are made more attempts (3 in total) to establish the connection with 
     * a physical machine when the attempt failures and in this case is established
     * in third/last attempt.
     */
    @Test
    public void connectToExistingPhysicalMachineAtThirdAttempt(){
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        IVirtualBox vboxMock = mock(IVirtualBox.class);
        
        //if the method connect() of class VirtualBoxManager is called with physical machine pm, then
        //there will be during first and second connection attempt thrown VBoxException as a result of
        //unsuccessful connection and in third/last attempt there will not happen anything as a result of
        //successful connection
        doThrow(VBoxException.class).
        doThrow(VBoxException.class).
        doNothing().when(vbmMock).connect(url, pm.getUsername(), pm.getUserPassword());
        
        //if the method getVBox() is called then returns mocked object of type IVirtualBox in order to ensure
        //that real methods of class IVirtualBox will not be called
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        
        //no exception should be invoked
        sut.connectTo(pm);
        
        //check the method connect() was called 3 times with the required parameters of physical machine pm
        verify(vbmMock, times(3)).connect(url, pm.getUsername(), pm.getUserPassword());
    }
    
    /**
     * Tests that there is ConnectionFailureException exception invoked when the connection with physical
     * machine is not successfully established after 3 attempts.
     */
    @Test
    public void connectToInaccessiblePhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        
        //each time when the method connect() of class VirtualBoxManager is called with physical machine pm,
        //then there is thrown VBoxException as a unsuccessful result
        doThrow(VBoxException.class).when(vbmMock).connect(url, pm.getUsername(), pm.getUserPassword());
        
        
        
        //there is ConnectionFailureException exception expected to be invoked
        exception.expect(ConnectionFailureException.class);
        sut.connectTo(pm);
        
        //check the method connect() was called 3 times with the required parameters of physical machine pm
        verify(vbmMock, times(3)).connect(url, pm.getUsername(), pm.getUserPassword());
    }
    
    /**
     * Tests that null object of type PhysicalMachine cannot be connected and the IllegalArgumentException
     * exception is thrown as a result.
     */
    @Test
    public void connectToNullPhysicalMachine(){
        exception.expect(IllegalArgumentException.class);
        sut.connectTo(null);
    }
    
    /**
     * Tests that there is not IncompatibleVirtToolAPIVersionException exception invoked when API version
     * of VirtualBox matches the required version.
     */
    @Test
    public void connectToWithCompatibleVBoxAPIVersion(){
        PhysicalMachine pm = new PMBuilder().build();
        IVirtualBox vboxMock = mock(IVirtualBox.class);
        
        //if the method getVBox() is called, then it will return mocked object (vboxMock) of type IVirtualBox as a result
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        
        //if the method getAPIVersion() is called, then string with the value "4_3" will be returned as a result
        when(vboxMock.getAPIVersion()).thenReturn("4_3");
        
        //nothing should happen when the method connectTo() is called (no exception invoked)
        sut.connectTo(pm);
    }
    
    /**
    * Tests that there is IncompatibleVirtToolAPIVersionException exception invoked when API version
    * of VirtualBox does not match the required version.
    */
    @Test
    public void connectToWithIncompatibleVBoxAPIVersion(){
        PhysicalMachine pm = new PMBuilder().build();
        IVirtualBox vboxMock = mock(IVirtualBox.class);
        
        //if method getVBox() is called, then it will return mocked object (vboxMock) of type IVirtualBox as a result
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

