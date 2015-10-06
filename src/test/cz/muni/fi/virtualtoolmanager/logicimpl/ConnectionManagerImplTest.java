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
import cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.any;
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
     * This test tests that there is not possible to connect to a null physical
     * machine and thus there appears an error informing message on a standard
     * error output and null object of type VirtualizationToolManager is returned
     * as a result.
     */
    @Test
    public void connectToNullPhysicalMachine(){
        //as null object of type PhysicalMachine is an illegal input argument for method 
        //ConnectionManagerImpl::connectTo(), an error message with this information is
        //written on standard error output and null object of type VirtualizationToolManager
        //is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(null);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that there was used null object"
                  + "of type PhysicalMachine", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that a nonexistent or existent physical machine which is being connected
     * in a moment when the network connection is not available cannot be and is not successfully
     * connected.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void connectToNotExistingPhysicalMachineOrInavailableNetworkConnection() throws Exception {
        //represents a physical machine with which there should be established a connection
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected
        doThrow(conFailExMock).when(natAPIConMock).connectTo(pm);
        //there should be a non-empty string value when the method ConnectionFailureException::getMessage() is called        
        when(conFailExMock.getMessage()).thenReturn("Any error string message");
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - inavailable network connection or nonexistent physical machine"
                  + "(incorrect IP address)", errContent.toString().isEmpty());        
    }
    
    /**
     * This test tests that a physical machine with an incorrect web server port
     * of virtualization tool cannot be and is not successfully connected.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectWebServerPort() throws Exception {
        //represents a physical machine with which there should be established a connection
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected because of the incorrect web server
        //port number        
        doThrow(conFailExMock).when(natAPIConMock).connectTo(pm);
        //there should be a non-empty string value when the method ConnectionFailureException::getMessage() is called        
        when(conFailExMock.getMessage()).thenReturn("Any error string message");
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - incorrect web server port of virtualization tool", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that a physical machine with an incorrect username
     * cannot be and is not successfully connected.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectUsername() throws Exception {
        //represents a physical machine with which there should be established a connection
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected because of the incorrect username
        doThrow(conFailExMock).when(natAPIConMock).connectTo(pm);
        //there should be a non-empty string value when the method ConnectionFailureException::getMessage() is called        
        when(conFailExMock.getMessage()).thenReturn("Any error string message");
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - incorrect username", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that a physical machine with an incorrect user password
     * cannot be and is not successfully connected.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectUserPassword() throws Exception{
        //represents a physical machine with which there should be established a connection
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
        //this step ensures that neccessary steps for physical machine connection are to be performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected because of the incorrect user password
        doThrow(conFailExMock).when(natAPIConMock).connectTo(pm);
        //there should be a non-empty string value when the method ConnectionFailureException::getMessage() is called        
        when(conFailExMock.getMessage()).thenReturn("Any error string message");
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - incorrect user password", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there are made more attempts (3 in total) to establish
     * the connection with a physical machine when the attempt failures and in
     * this case the connection is established at the third/last attempt.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void connectToExistingPhysicalMachineAtThirdAttempt() throws Exception {
        //represents a physical machine with which there should be the connection established
        PhysicalMachine pm = new PMBuilder().build();
        String url = "http://" + pm.getAddressIP() + ":" + pm.getPortOfVTWebServer();
        //mock object of type ConnectionFailureException for better test control
        ConnectionFailureException conFailExMock = mock(ConnectionFailureException.class);
        
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
    }
    
    /**
     * This test tests that if the method ConnectionManagerImpl::connectTo() is
     * called with a physical machine on which there is not VirtualBox with a
     * required API version then there is made just one attempt to connect to
     * the physical machine during which there is found out the incorrect API
     * version and then there appears an error informing message on a standard
     * error output.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void connectToWithIncompatibleVBoxAPIVersion() throws Exception {
        //represents a physical machine with which there should be the connection established
        PhysicalMachine pm = new PMBuilder().build();
        //mock object of type IncompatibleVirtToolAPIVersionException for better test control
        IncompatibleVirtToolAPIVersionException incVTAPIVerExMock = mock(IncompatibleVirtToolAPIVersionException.class);
        
        //there should be thrown IncompatibleVirtToolAPIVersionException when the method
        //NativeVBoxAPIIConnection::connectTo() is called with a required physical machine
        //and means there is incorrect VirtualBox API version on physical machine pm and thus
        //there cannot be remotely control virtual machines
        doThrow(incVTAPIVerExMock).when(natAPIConMock).connectTo(pm);
        //there should be returned a non-empty string value when the method
        //IncompatibleVirtToolAPIVersionException::getMesssage() is called
        when(incVTAPIVerExMock.getMessage()).thenReturn("Any string error message");
        
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned a null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written a message on a standard error output that there is VirtualBox "
                + "of not required API version on the physical machine", errContent.toString().isEmpty());
        
        //checks the method NativeVBoxAPIConnection::connectTo() was called just once
        verify(natAPIConMock, times(1)).connectTo(pm);
    }
    
    /**
     * This test tests that there should not be invoked any exception or error when
     * the method disconnectFrom() is called with valid connected physical machine
     * and with available network connection and running virtualization tool web server.
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
        sut.disconnectFrom(pm);
        
        assertFalse("Physical machine " + pm + " should not already be connected", conPhysMachMock.isConnected(pm));
        assertTrue("There should not be written any error message on standard error output",
                   errContent.toString().isEmpty());
        assertFalse("There should be written informing text on standard output the physical machine "
                  + pm + " was disconnected successfully", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called when the network connection is not available or
     * the virtualization tool web server is not running.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void disconnectFromConnectedPhysicalMachineWithInavailableConnection() throws Exception{
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
        
        //there should be written an error message on standard error output with information
        //about incorrect disconnection of physical machine
        sut.disconnectFrom(pm);
        
        assertFalse("Physical machine " + pm + " should not already be connected", conPhysMachMock.isConnected(pm));
        assertFalse("There should be written error message that physical machine " + pm + " could not "
                  + "be correctly disconnected", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which is not connected
     * at the moment of method execution.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void disconnectFromNotConnectedPhysicalMachine() throws Exception{
        //represents a physical machine which should be disconnected, but is not connected
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that the method is ended without any further steps being performed
        when(conPhysMachMock.isConnected(pm)).thenReturn(false);
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(pm);
        
        assertFalse("Physical machine " + pm + " should not be connected", conPhysMachMock.isConnected(pm));
        assertFalse("There should be written error message that physical machine " + pm + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which has incorrect
     * virtualization tool web server port (against the original physical machine which is
     * recorded as connected physical machine) and that that physical machine cannot and is not
     * disconnected.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectWebServerPort() {
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
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(incorPM);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", conPhysMachMock.isConnected(incorPM));
        assertFalse("There should be written error message that physical machine " + incorPM + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which has incorrect
     * username (against the original physical machine which is recorded as connected physical machine)
     * and that that physical machine cannot and is not disconnected.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUsername() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect username against the connected physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().username("Henry").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect username is not found in the list of connected PMs because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(incorPM);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", conPhysMachMock.isConnected(incorPM));
        assertFalse("There should be written error message that physical machine " + incorPM + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which has incorrect
     * user password (against the original physical machine which is recorded as connected physical machine)
     * and that that physical machine cannot and is not disconnected.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUserPassword() {
        //represents a connected physical machine
        PhysicalMachine corPM = new PMBuilder().build();
        //represents a physical machine with an incorrect user password against the connecte physical machine corPM
        PhysicalMachine incorPM = new PMBuilder().userPassword("14gg44").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect user password is not found in the list of connected PMs because of the absolute equality)
        when(conPhysMachMock.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(conPhysMachMock.isConnected(corPM)).thenReturn(true);
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(incorPM);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", conPhysMachMock.isConnected(incorPM));
        assertFalse("There should be written error message that physical machine " + incorPM + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with null physical machine, because null physical
     * machine cannot be disconnected.
     */
    @Test
    public void disconnectFromNullPhysicalMachine(){
        //there should be written an error message that there was used an illegal argument
        //for the method ConnectionManagerImpl::disconnectFrom() when this method is called
        sut.disconnectFrom(null);
        
        assertFalse("There should be written an error message that there was made an attempt to "
                  + "disconnect from null physical machine", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
        
        //checks that the very first (espacially more further steps) step after null object check
        //has not been done as expected
        verify(conPhysMachMock, never()).isConnected(any(PhysicalMachine.class));
    }
    
    /**
     * This test tests that there is returned the TRUE value when a physical machine
     * is non-null and connected.
     */
    @Test
    public void isConnectedWithConnectedPhysicalMachineAndAvailableConnection(){
        //represents a physical machine which is queried for its accessibility
        PhysicalMachine pm = new PMBuilder().build();
        
        //there should be returned a positive answer when the method ConnectedPhysicalMachines::isConnected()
        //is called with a required physical machine and means the PM is connected
        PowerMockito.when(conPhysMachMock.isConnected(pm)).thenReturn(true);
                
        assertTrue("Physical machine " + pm + " should be connected", sut.isConnected(pm));
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
        
        assertFalse("Physical machine " + pm + " should not be connected", sut.isConnected(pm));
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
        assertFalse("Null physical machine cannot be connected", sut.isConnected(null));
        
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
        verify(sutl, times(2)).disconnectFrom(any(PhysicalMachine.class));
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
