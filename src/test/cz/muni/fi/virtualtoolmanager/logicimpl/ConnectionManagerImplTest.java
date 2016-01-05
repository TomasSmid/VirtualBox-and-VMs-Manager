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

import cz.muni.fi.virtualtoolmanager.pubapi.io.OutputHandler;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PhysicalMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.IncompatibleVirtToolAPIVersionException;
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ClosingActionType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.doCallRealMethod;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * This test class ensure unit testing of class ConnectionManagerImpl and
 * is intended to be a pointer that class ConnectionManagerImpl works as expected.
 * 
 * @author Tomáš Šmíd
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ConnectionManagerImpl.class, NativeVBoxAPIConnection.class, NativeVBoxAPIManager.class, ConnectedPhysicalMachines.class})
public class ConnectionManagerImplTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private ConnectionManagerImpl sut;
    private NativeVBoxAPIConnection natAPIConMock;
    private VirtualizationToolManagerImpl vtmMock;
    private ConnectedPhysicalMachines conPhysMachMock;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ConnectedPhysicalMachines.class);
        conPhysMachMock = mock(ConnectedPhysicalMachines.class);
        natAPIConMock = mock(NativeVBoxAPIConnection.class);
        vtmMock = mock(VirtualizationToolManagerImpl.class);
        when(ConnectedPhysicalMachines.getInstance()).thenReturn(conPhysMachMock);
        whenNew(NativeVBoxAPIConnection.class).withNoArguments().thenReturn(natAPIConMock);
        whenNew(VirtualizationToolManagerImpl.class).withAnyArguments().thenReturn(vtmMock);
        sut = new ConnectionManagerImpl();
        OutputHandler.setErrorOutputStream(new PrintStream(errContent));
        OutputHandler.setOutputStream(new PrintStream(outContent));
    }
    
    @After
    public void cleanUp(){
        OutputHandler.setErrorOutputStream(null);
        OutputHandler.setOutputStream(null);
    }
    
    /**
     * This test tests that there does not appear any error nor exception with any
     * track on standard error output when valid not connected physical machine is
     * being connected.
     */
    @Test
    public void connectToValidNotConnectedPhysicalMachine() {
        //represents a not connected physical machine which should be connected
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that the neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        
        //no exception nor error should appear when the method ConnectionManagerImpl::connectTo() is called,
        //there is just object of type VirtualizationToolManager returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNotNull("There should has been returned non-null object of type VirtualizationToolManager", vtm);
        assertTrue("There should not be any error message written on standard error output",
                   errContent.toString().isEmpty());
        assertFalse("There should be written informing message on standard output that physical machine "
                   + pm + " was successfully connected", outContent.toString().isEmpty());
        verify(conPhysMachMock).add(pm);
    }
    
    /**
     * This test tests that there does not appear any error or exception with any
     * track on standard error output when valid already connected physical machine
     * is being connected.
     */
    @Test
    public void connectToValidConnectedPhysicalMachine(){
        //represents already connected physical machine which should be connected again
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that steps neccassery for physical machine connection
        //are to be skipped (physical connection and storing physical machine in the list of connected machines)
        when(conPhysMachMock.isConnected(pm)).thenReturn(true); 
        
        //no exception or error should appear when the method ConnectionManagerImpl::connectTo() is called,
        //there is just object of type VirtualizationToolManager returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNotNull("There should has been returned non-null object of type VirtualizationToolManager", vtm);
        assertTrue("There should not be any error message written on standard error output",
                     errContent.toString().isEmpty());
        assertFalse("There should be written informing message on standard output that physical machine "
                   + pm + " has already been connected", outContent.toString().isEmpty());
        
        //checks the physical machine has not been places in the list of connected physical machines again (a second time)
        verify(conPhysMachMock, never()).add(pm);
    }
    
    /**
     * This test tests that there is invoked an IllegalArgumentException when
     * there is made an attempt to connect to a null physical machine.
     */
    @Test
    public void connectToNullPhysicalMachine(){
        exception.expect(IllegalArgumentException.class);
        VirtualizationToolManager vtm = sut.connectTo(null);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to connect to a nonexistent physical machine or
     * existent physical machine which is being connected in a moment when the
     * network connection is not available.
     */
    @Test
    public void connectToNotExistingPhysicalMachineOrInavailableNetworkConnection(){
        //represents a physical machine with which there should be established a connection
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected
        doThrow(conFailExMock).when(natAPIConMock).connectTo(pm);
        
        exception.expect(ConnectionFailureException.class);
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        verify(conPhysMachMock, never()).add(pm);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when 
     * there is made an attempt to connect to a physical machine with an
     * incorrect web server port number.
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectWebServerPort(){
        //represents a physical machine with correct web server port number
        //with which there should be established a connection
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with incorrect web server port number
        //with which there should be established a connection
        PhysicalMachine incorPM = new PMBuilder().webserverPort("18082").build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(corPM)).thenReturn(false);
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that physical machine cannot be connected because of the incorrect web server
        //port number        
        doThrow(conFailExMock).when(natAPIConMock).connectTo(incorPM);        
        
        VirtualizationToolManager vtm1 = sut.connectTo(corPM);
        assertNotNull("Returned object should not be null",vtm1);
        
        exception.expect(ConnectionFailureException.class);
        VirtualizationToolManager vtm2 = sut.connectTo(incorPM);
        
        verify(conPhysMachMock).add(corPM);
        verify(conPhysMachMock, never()).add(incorPM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when 
     * there is made an attempt to connect to a physical machine with an
     * incorrect username.
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectUsername(){
        //represents a physical machine with correct username with which there
        //should be established a connection
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with incorrect username with which there
        //should be established a connection
        PhysicalMachine incorPM = new PMBuilder().username("User123456").build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(corPM)).thenReturn(false);
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that physical machine cannot be connected because of
        //the incorrect username
        doThrow(conFailExMock).when(natAPIConMock).connectTo(incorPM);        
        
        VirtualizationToolManager vtm1 = sut.connectTo(corPM);
        assertNotNull("Returned object should not be null",vtm1);
        
        exception.expect(ConnectionFailureException.class);
        VirtualizationToolManager vtm2 = sut.connectTo(incorPM);
        
        verify(conPhysMachMock).add(corPM);
        verify(conPhysMachMock, never()).add(incorPM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when 
     * there is made an attempt to connect to a physical machine with an
     * incorrect user password.
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectUserPassword(){
        //represents a physical machine with correct user password with which
        //there should be established a connection
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with incorrect user password with which
        //there should be established a connection
        PhysicalMachine incorPM = new PMBuilder().userPassword("pswd125487").build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(corPM)).thenReturn(false);
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that physical machine cannot be connected because of
        //user password
        doThrow(conFailExMock).when(natAPIConMock).connectTo(incorPM);        
        
        VirtualizationToolManager vtm1 = sut.connectTo(corPM);
        assertNotNull("Returned object should not be null",vtm1);
        
        exception.expect(ConnectionFailureException.class);
        VirtualizationToolManager vtm2 = sut.connectTo(incorPM);
        
        verify(conPhysMachMock).add(corPM);
        verify(conPhysMachMock, never()).add(incorPM);
    }
    
    /**
     * This test tests that there are made more attempts (3 in total) to establish
     * the connection with a physical machine when the attempt failures and in
     * this case the connection is established at the third/last attempt.
     */
    @Test
    public void connectToExistingPhysicalMachineAtThirdAttempt(){
        //represents a physical machine with which there should be the connection established
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        //if the method NativeVBoxAPIConnection::connectTo() is called with physical machine pm, then
        //during the first and the second connection attempt there will be thrown ConnectionFailureException
        //as a result of unsuccessful connection and at the third/last attempt there will not happen anything as a result of
        //successful connection
        doThrow(conFailExMock).doThrow(conFailExMock).doNothing().when(natAPIConMock).connectTo(pm);
                
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNotNull("There should has been returned a non-null object of type VirtualizationToolManager", vtm);
        assertTrue("There should not be written any message on a standard error output", errContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard output that the connection establishment "
                + "operation was successful", outContent.toString().isEmpty());
        
        //checks the method NativeVBoxAPIConnection::connectTo() was called 3 times as expected
        verify(natAPIConMock, times(3)).connectTo(pm);
        verify(conPhysMachMock).add(pm);
    }
    
    /**
     * This test tests that if the method ConnectionManagerImpl::connectTo() is called
     * with negative input value (-1) which should represent maximum wait time between
     * each attempt to establish connection with a required physical machine then
     * there is made just one attempt to establish connection.
     */
    @Test
    public void connectToWithNegativeMaxWaitTime1(){
        //represents a physical machine with which there should be the connection established
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
                        
        VirtualizationToolManager vtm = sut.connectTo(pm, -1);
        
        assertNotNull("There should has been returned a non-null object of type VirtualizationToolManager", vtm);
        assertTrue("There should not be written any message on a standard error output", errContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard output that the connection establishment "
                + "operation was successful", outContent.toString().isEmpty());
        
        //checks the method NativeVBoxAPIConnection::connectTo() was called once as expected
        verify(natAPIConMock).connectTo(pm);
        verify(conPhysMachMock).add(pm);
    }
    
    /**
     * This test tests that if the method ConnectionManagerImpl::connectTo() is called
     * with negative input value (Long.MIN_VALUE) which should represent maximum
     * wait time between each attempt to establish connection with a required
     * physical machine then there is made just one attempt to establish connection.
     */
    @Test
    public void connectToWithNegativeMaxWaitTime2(){
        //represents a physical machine with which there should be the connection established
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        
        VirtualizationToolManager vtm = sut.connectTo(pm, Long.MIN_VALUE);
        
        assertNotNull("There should has been returned a non-null object of type VirtualizationToolManager", vtm);
        assertTrue("There should not be written any message on a standard error output", errContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard output that the connection establishment "
                + "operation was successful", outContent.toString().isEmpty());
        
        //checks the method NativeVBoxAPIConnection::connectTo() was called once as expected
        verify(natAPIConMock).connectTo(pm);
        verify(conPhysMachMock).add(pm);
    }
    
    /**
     * This test tests that there is invoked IncompatibleVirtToolAPIVersionException
     * when there is made an attempt to connect to a physical machine on which
     * there is installed VirtualBox of an incorrect API version.
     */
    @Test
    public void connectToWithIncompatibleVBoxAPIVersion(){
        //represents a physical machine with which there should be the connection established
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type IncompatibleVirtToolAPIVersionException for better test control
        IncompatibleVirtToolAPIVersionException incVTAPIVerExMock =
                mock(IncompatibleVirtToolAPIVersionException.class);
        
        //there should be thrown IncompatibleVirtToolAPIVersionException when the method
        //NativeVBoxAPIIConnection::connectTo() is called with a required physical machine
        //and means there is incorrect VirtualBox API version on physical machine pm and thus
        //there cannot be remotely control virtual machines
        doThrow(incVTAPIVerExMock).when(natAPIConMock).connectTo(pm);
        
        exception.expect(IncompatibleVirtToolAPIVersionException.class);
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        //checks the method NativeVBoxAPIConnection::connectTo() was called just once
        verify(natAPIConMock, times(1)).connectTo(pm);
    }
    
    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to disconnect from a physical machine, but closing
     * action type is null.
     */
    @Test
    public void disconnectFromWithNullClosingAction(){
        //represents a connected physical machine which should be disconnected
        PhysicalMachine pm = new PMBuilder().build();
        
        exception.expect(IllegalArgumentException.class);
        sut.disconnectFrom(pm, null);        
    }
    
    /**
     * This test tests that if the method ConnectionManagerImpl::disconnectFrom()
     * is called with closing action NONE then there is just the physical machine
     * removed from the list of connected physical machines, but no other actions
     * are performed.
     */
    @Test
    public void disconnectFromWithClosingActionNone(){
        //represents a connected physical machine which should be disconnected
        PhysicalMachine pm = new PMBuilder().build();
        
        when(conPhysMachMock.isConnected(pm)).thenReturn(true);
        when(conPhysMachMock.remove(pm)).thenReturn(true);
        
        sut.disconnectFrom(pm, ClosingActionType.NONE);
        
        assertTrue("There should not be written error message", errContent.toString().isEmpty());
        assertFalse("There should be written any text on standard output", outContent.toString().isEmpty());
        
        verify(vtmMock, never()).close();
    }
    
    /**
     * This test tests that if the method ConnectionManagerImpl::disconnectFrom()
     * is called with closing action SHUT_DOWN_RUNNING_VM then there are all
     * running virtual machines on the physical machine shut down before the
     * physical machine itself is removed from the list of connected physical 
     * machines.
     */
    @Test
    public void disconnectFromWithClosingActionShutDownRunningVM(){
        //represents a connected physical machine which should be disconnected
        PhysicalMachine pm = new PMBuilder().build();
        
        when(conPhysMachMock.isConnected(pm)).thenReturn(true);
        when(conPhysMachMock.remove(pm)).thenReturn(true);
                
        sut.disconnectFrom(pm, ClosingActionType.SHUT_DOWN_RUNNING_VM);
        
        assertTrue("There should not be written error message", errContent.toString().isEmpty());
        assertFalse("There should be written any text on standard output", outContent.toString().isEmpty());
        
        verify(vtmMock).close();
        verify(conPhysMachMock).remove(pm);
    }
    
    /**
     * This test tests that there is invoked IncompatibleVirtToolAPIVersionException
     * when there is made an attempt to disconnect from a physical machine on
     * which there has been changed VirtualBox API version to incorrect one.
     */
    @Test
    public void disconnectFromWithClosingActionShutDownRunningVMWrongAPIVersion(){
        //represents a connected physical machine which should be disconnected
        PhysicalMachine pm = new PMBuilder().build();
        IncompatibleVirtToolAPIVersionException incVTAPIVersionExMock =
                mock(IncompatibleVirtToolAPIVersionException.class);
        
        when(conPhysMachMock.isConnected(pm)).thenReturn(true);
        when(conPhysMachMock.remove(pm)).thenReturn(true);
        doThrow(incVTAPIVersionExMock).when(natAPIConMock).connectTo(pm);
        when(conPhysMachMock.remove(pm)).thenReturn(true);
        
        exception.expect(IncompatibleVirtToolAPIVersionException.class);
        sut.disconnectFrom(pm, ClosingActionType.SHUT_DOWN_RUNNING_VM);
        
        verify(vtmMock, never()).close();
        verify(conPhysMachMock).remove(pm);
    }
    
    /**
     * This test tests that there should not be invoked any exception or error when
     * the method disconnectFrom() is called with valid connected physical machine,
     * with closing action SHUT_DOWN_RUNNING_VM and with available network connection
     * and running virtualization tool web server.
     */
    @Test
    public void disconnectFromConnectedPhysicalMachineWithAvailableConnection(){
        //represents a physical machine which should be disconnected
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that there will follow all neccessary steps for correct end up of work
        //with physical machine and its disconnection
        when(conPhysMachMock.isConnected(pm)).thenReturn(true).thenReturn(false);        
        //there should be returned a positive answer when the method ConnectedPhysicalMachines::remove()
        //is called with a required physical machines and means the PM was successfully removed from the list
        //of connected PMs
        when(conPhysMachMock.remove(pm)).thenReturn(true);
        //no exception nor error should appear when the method ConnectionManagerImpl::disconnectFrom() 
        //is called (the optimal scenario)
        sut.disconnectFrom(pm, ClosingActionType.SHUT_DOWN_RUNNING_VM);
        
        assertFalse("Physical machine " + pm + " should not already be connected", conPhysMachMock.isConnected(pm));
        assertTrue("There should not be written any error message on standard error output",
                   errContent.toString().isEmpty());
        assertFalse("There should be written informing text on standard output the physical machine "
                  + pm + " was disconnected successfully", outContent.toString().isEmpty());
        
        verify(vtmMock).close();
        verify(conPhysMachMock).remove(pm);
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with closing action SHUT_DOWN_RUNNING_VM
     * when the network connection is not available or the virtualization tool
     * web server is not running.
     */
    @Test
    public void disconnectFromConnectedPhysicalMachineWithInavailableConnection(){
        //represents a physical machine which should be disconnected
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);        
        
        //this step ensures that there should follow all neccessary steps for correct end up of work
        //with physical machine and its disconnection
        when(conPhysMachMock.isConnected(pm)).thenReturn(true).thenReturn(false);
        //this step ensures that the physical machine incorPM cannot be correctly disconnected
        doThrow(conFailExMock).when(natAPIConMock).connectTo(pm);
        //there should be returned a positive answer when the method ConnectedPhysicalMachines::remove()
        //is called with a required physical machines and means the PM was successfully removed from the list
        //of connected PMs
        when(conPhysMachMock.remove(pm)).thenReturn(true);
                
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(pm, ClosingActionType.SHUT_DOWN_RUNNING_VM);
        
        assertFalse("Physical machine " + pm + " should not already be connected", conPhysMachMock.isConnected(pm));
        verify(vtmMock, never()).close();
        verify(conPhysMachMock).remove(pm);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to disconnect from a not connected physical
     * machine and is used closing action NONE.
     */
    @Test
    public void disconnectFromNotConnectedPhysicalMachineWithClosingActionNone(){
        //represents a physical machine which should be disconnected, but is not connected
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that the method is ended without any further steps being performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(pm);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to disconnect from a not connected physical
     * machine and is used closing action SHUT_DOWN_RUNNING_VM.
     */
    @Test
    public void disconnectFromNotConnectedPhysicalMachineWithClosingActionShutDownRunningVM(){
        //represents a physical machine which should be disconnected, but is not connected
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that the method is ended without any further steps being performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(pm, ClosingActionType.SHUT_DOWN_RUNNING_VM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when 
     * there is made an attempt to disconnect from a physical machine with an
     * incorrect web server port number and is used closing action NONE.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectWebServerPortAndClosingActionNone() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect web server port against the connected physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().webserverPort("1111").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect web server port is not found in the list of connected PMs
        //because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(incorPM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when 
     * there is made an attempt to disconnect from a physical machine with an
     * incorrect web server port number and is used closing action SHUT_DOWN_RUNNING_VM.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectWebServerPortAndClosingActionShutDownRunningVM() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect web server port against the connected physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().webserverPort("1111").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect web server port is not found in the list of connected PMs
        //because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(incorPM, ClosingActionType.SHUT_DOWN_RUNNING_VM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to disconnect from a physical machine with an
     * incorrect username and is used closing action NONE.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUsernameAndClosingActionNone() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect username against the connected physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().username("Henry").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect username is not found in the list of connected PMs because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(incorPM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to disconnect from a physical machine with an
     * incorrect username and is used closing action SHUT_DOWN_RUNNING_VM.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUsernameAndClosingActionShutDownRunningVM() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect username against the connected physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().username("Henry").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect username is not found in the list of connected PMs because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(incorPM, ClosingActionType.SHUT_DOWN_RUNNING_VM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to disconnect from a physical machine with an
     * incorrect user password and is used closing action NONE.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUserPasswordAndClosingActionNone() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect user password against the connecte physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().userPassword("14gg44").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect user password is not found in the list of connected PMs because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(incorPM);
    }
    
    /**
     * This test tests that there is invoked ConnectionFailureException when
     * there is made an attempt to disconnect from a physical machine with an
     * incorrect user password and is used closing action SHUT_DOWN_RUNNING_VM.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUserPasswordAndClosingActionShutDownRunningVM() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect user password against the connecte physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().userPassword("14gg44").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect user password is not found in the list of connected PMs because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        exception.expect(ConnectionFailureException.class);
        sut.disconnectFrom(incorPM, ClosingActionType.SHUT_DOWN_RUNNING_VM);
    }
    
    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to disconnect from a null physical machine and is used
     * closing action NONE.
     */
    @Test
    public void disconnectFromNullPhysicalMachineWithClosingActionNone(){
        exception.expect(IllegalArgumentException.class);
        sut.disconnectFrom(null);
        
        //checks that the very first (espacially more further steps) step after null object check
        //has not been done as expected
        verify(conPhysMachMock, never()).isConnected(any(PhysicalMachine.class));
    }
    
    /**
     * This test tests that there is invoked IllegalArgumentException when there
     * is made an attempt to disconnect from a null physical machine and is used
     * closing action SHUT_DOWN_RUNNING_VM.
     */
    @Test
    public void disconnectFromNullPhysicalMachineWithClosingActionShutDownRunningVM(){
        exception.expect(IllegalArgumentException.class);
        sut.disconnectFrom(null, ClosingActionType.SHUT_DOWN_RUNNING_VM);
        
        //checks that the very first (espacially more further steps) step after null object check
        //has not been done as expected
        verify(conPhysMachMock, never()).isConnected(any(PhysicalMachine.class));
    }
    
    /**
     * This test tests that there is returned the TRUE value when a physical machine
     * is non-null and connected.
     */
    @Test
    public void isConnectedWithConnectedPhysicalMachine(){
        //represents a physical machine which is queried for its accessibility
        PhysicalMachine pm = new PMBuilder().build();
        
        //there should be returned a positive answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is connected
        when(conPhysMachMock.isConnected(pm)).thenReturn(true);
                
        assertTrue("Physical machine should be connected", sut.isConnected(pm));
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine is not recorded as a connected physical machine.
     */
    @Test
    public void isConnectedWithNotConnectedPhysicalMachine(){
        //represents a physical machine which is queried for its accessibility
        PhysicalMachine pm = new PMBuilder().build();
        
        //there should be returned a negative answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is not connected
        PowerMockito.when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        
        assertFalse("Physical machine should not be connected", sut.isConnected(pm));
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine with incorrect web server port is not absolutely equal
     * to some physical machine from the list of connected physical machines.
     */
    @Test
    public void isConnectedWithIncorrectWebServerPort(){
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with incorrect web server port against the connected physical
        //machine corPM and thus cannot be macthed as a connected physical machine
        PhysicalMachine incorPM = new PMBuilder().webserverPort("18080").build();
        
        //there should be returned a negative answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is not connected
        PowerMockito.when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //there should be returned a positive answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is connected
        PowerMockito.when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", sut.isConnected(incorPM));
        assertTrue("Physica machine " + corPM + " should be connected", sut.isConnected(corPM));
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine with incorrect username is not absolutely equal
     * to some physical machine from the list of connected physical machines.
     */
    @Test
    public void isConnectedWithIncorrectUsername(){
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with incorrect username against the connected physical
        //machine corPM and thus cannot be macthed as a connected physical machine
        PhysicalMachine incorPM = new PMBuilder().username("Jimbo").build();
        
        //there should be returned a negative answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is not connected
        PowerMockito.when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //there should be returned a positive answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is connected
        PowerMockito.when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", sut.isConnected(incorPM));
        assertTrue("Physica machine " + corPM + " should be connected", sut.isConnected(corPM));
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine with incorrect user password is not absolutely equal
     * to some physical machine from the list of connected physical machines.
     */
    @Test
    public void isConnectedWithIncorrectUserPassword(){
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with incorrect user password against the connected physical
        //machine corPM and thus cannot be macthed as a connected physical machine
        PhysicalMachine incorPM = new PMBuilder().userPassword("55448sad8").build();
        
        //there should be returned a negative answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is not connected
        PowerMockito.when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //there should be returned a positive answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is connected
        PowerMockito.when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", sut.isConnected(incorPM));
        assertTrue("Physica machine " + corPM + " should be connected", sut.isConnected(corPM));
    }
    
    /**
     * This test tests that there is returned a negative answer when the method
     * ConnectionManagerImpl::isConnected() is called with illegal argument
     * (null physical machine).
     */
    @Test
    public void isConnectedWithNullPhysicalMachine(){
        exception.expect(IllegalArgumentException.class);
        sut.isConnected(null);
        
        //checks the physical machine was not actually queried for its accessibility
        verify(conPhysMachMock, never()).isConnected(any(PhysicalMachine.class));
    }
    
    /**
     * This test tests that there are all connected physical machines disconnected
     * and work with them is ended up when the method close() is called.
     */
    @Test
    public void closeWithAnyConnectedPhysicalMachine(){
        //represents first of two connected physical machines which should be disconnected
        PhysicalMachine pm1 = new PMBuilder().build();
        //represents the second of two connected physical machines which should be disconnected
        PhysicalMachine pm2 = new PMBuilder().addressIP("10.12.11.9").username("Elphon")
                                             .userPassword("22mn54fg").build();
        //a nonempty list of connected physical machines
        List<PhysicalMachine> pmsList1 = Arrays.asList(pm1,pm2);
        List<PhysicalMachine> pmsList2 = Arrays.asList(pm2);
        //sutl represents the same as sut, but here is it for mocking intention (easier testing)
        ConnectionManagerImpl sutl = mock(ConnectionManagerImpl.class);
        //this step ensures the real method is called
        doCallRealMethod().when(sutl).close();
        //for the first and the second call, there should be returned a non-empty list with 2 PMs,
        //for the third and the fourth call, there should be returned a non-empty list with 1 PM
        //for the fifth(last) call, there should be returned an empty list of PMs when the method
        //ConnectionManagerImpl::getConnectedPhysicalMachines() is called
        doReturn(pmsList1).doReturn(pmsList1).
        doReturn(pmsList2).doReturn(pmsList2).
        doReturn(new ArrayList<>()).when(sutl).getConnectedPhysicalMachines();
        
        sutl.close();
        
        //checks the method ConnectionManager::disconnectFrom() was called two times, because in the list of
        //connected physical machines there were 2 physical machines stored
        verify(sutl, times(2)).disconnectFrom(any(PhysicalMachine.class), eq(ClosingActionType.SHUT_DOWN_RUNNING_VM));
    }
    
    /**
     * This test tests that if there is not any connected physical machine, then
     * there is not reason to perform disconnection.
     */
    @Test
    public void closeWithNotAnyConnectedPhysicalMachine(){
        //an empty list of connected physical machines
        List<PhysicalMachine> pmsList = new ArrayList<>();
        //sutl represents the same as sut, but here is it for mocking intention (easier testing)
        ConnectionManagerImpl sutl = mock(ConnectionManagerImpl.class);
        //this step ensures the real method is called
        doCallRealMethod().when(sutl).close();
        //this step that there will be returned the empty list of connected physical machines
        when(conPhysMachMock.getConnectedPhysicalMachines()).thenReturn(pmsList);
        
        sutl.close();
        
        //checks that there was not any call of method disconnectFrom(), because there was not
        //any physical machine to be disconnected
        verify(sutl, never()).disconnectFrom(any(PhysicalMachine.class));
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
