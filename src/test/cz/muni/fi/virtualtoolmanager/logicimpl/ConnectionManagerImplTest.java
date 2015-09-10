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
import org.junit.Rule;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This test class ensure unit testing of class ConnectionManagerImpl and
 * is intended to be a pointer that class ConnectionManagerImpl works as expected.
 * 
 * @author Tomáš Šmíd
 */
public class ConnectionManagerImplTest {
    
    private ConnectionManagerImpl sut;
    private NativeVBoxAPIConnection conMocked;
    private VirtualizationToolManagerImpl vtmMocked;
    private ConnectedPhysicalMachines cpmMocked;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    
    @Before
    public void setUp() {        
        cpmMocked = mock(ConnectedPhysicalMachines.class);
        conMocked = mock(NativeVBoxAPIConnection.class);
        vtmMocked = mock(VirtualizationToolManagerImpl.class);        
        sut = new ConnectionManagerImpl(cpmMocked,conMocked, vtmMocked);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }
    
    @After
    public void cleanUp(){
        System.setOut(null);
        System.setErr(null);
    }
    
    /**
     * This test tests that there does not appear any error or exception with any
     * track on standard error output when valid not connected physical machine is
     * being connected.
     */
    @Test
    public void connectToValidNotConnectedPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that the neccessary steps for physical machine connection are to be done
        when(cpmMocked.isConnected(pm)).thenReturn(false);
        
        //no exception or error should appear when the method connectTo() is called, there is just
        //object of type VirtualizationToolManager returned as a result
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
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that steps neccassery for physical machine connection
        //are to be skipped (physical connection and storing physical machine in the list of connected machines)
        when(cpmMocked.isConnected(pm)).thenReturn(true); 
        
        //no exception or error should appear when the method connectTo() is called, there is just
        //object of type VirtualizationToolManager returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNotNull("There should has been returned non-null object of type VirtualizationToolManager", vtm);
        assertTrue("There should not be any error message written on standard error output",
                     errContent.toString().isEmpty());
        assertFalse("There should be written informing message on standard output that physical machine "
                   + pm + " has already been connected", outContent.toString().isEmpty());
        
        //checks the physical machine has not been places in the list of connected physical machines again (a second time)
        verify(cpmMocked, never()).add(pm);
    }
    
    /**
     * 
     */
    @Test
    public void connectToNullPhysicalMachine(){
        //as null object of type PhysicalMachine is an illegal input argument for method connectTo()
        //an error message with this information is written on standard error output and null object of type
        //VirtualizationToolManager as a result
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
     */
    @Test
    public void connectToNotExistingPhysicalMachineOrInavailableNetworkConnection(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that neccessary steps for physical machine connection are to be done
        when(cpmMocked.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected
        doThrow(ConnectionFailureException.class).when(conMocked).connectTo(pm);
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - inavailable network connection or nonexistent physical machine"
                  + "(incorrect IP address)", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that a physical machine with an incorrect web server port
     * of virtualization tool cannot be and is not successfully connected.
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectWebServerPort(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that neccessary steps for physical machine connection are to be done
        when(cpmMocked.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected
        doThrow(ConnectionFailureException.class).when(conMocked).connectTo(pm);
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - incorrect web server port of virtualization tool", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that a physical machine with an incorrect username
     * cannot be and is not successfully connected.
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectUsername(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that neccessary steps for physical machine connection are to be done
        when(cpmMocked.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected
        doThrow(ConnectionFailureException.class).when(conMocked).connectTo(pm);
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - incorrect username", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that a physical machine with an incorrect user password
     * cannot be and is not successfully connected.
     */
    @Test
    public void connectToPhysicalMachineWithIncorrectUserPassword(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that neccessary steps for physical machine connection are to be done
        when(cpmMocked.isConnected(pm)).thenReturn(false);
        //this step ensures that physical machine cannot be connected
        doThrow(ConnectionFailureException.class).when(conMocked).connectTo(pm);
        
        //as physical machine cannot be connected, an error message is written to the standard error
        //output and null object of type VirtualizationToolManager is returned as a result
        VirtualizationToolManager vtm = sut.connectTo(pm);
        
        assertNull("There should has been returned null object of type VirtualizationToolManager", vtm);
        assertFalse("There should be written error message that the physical machine cannot be connected "
                  + "at the moment - incorrect user password", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should not be invoked any exception or error when
     * the method disconnectFrom() is called with valid connected physical machine
     * and with available network connection and running virtualization tool web server.
     */
    @Test
    public void disconnectFromConnectedPhysicalMachineWithAvailableConnection(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that there will follow all neccessary steps for correct end up of work
        //with physical machine and its disconnection
        when(cpmMocked.isConnected(pm)).thenReturn(true).thenReturn(false);
        
        //no exception or error should appear when the method disconnect() is called (the optimal scenario)
        sut.disconnectFrom(pm);
        
        assertFalse("Physical machine " + pm + " should not already be connected", cpmMocked.isConnected(pm));
        assertTrue("There should not be written any error message on standard error output",
                   errContent.toString().isEmpty());
        assertFalse("There should be written informing text on standard output the physical machine "
                  + pm + " was disconnected sucessfully", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called when the network connection is not available or
     * the virtualization tool web server is not running.
     */
    @Test
    public void disconnectFromConnectedPhysicalMachineWithInavailableConnection(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that there should follow all neccessary steps for correct end up of work
        //with physical machine and its disconnection
        when(cpmMocked.isConnected(pm)).thenReturn(true).thenReturn(false);
        //this step ensures that the physical machine incorPM cannot be correctly disconnected
        doThrow(ConnectionFailureException.class).when(conMocked).connectTo(pm);
        
        //there should be written an error message on standard error output with information
        //about incorrect disconnection of physical machine
        sut.disconnectFrom(pm);
        
        assertFalse("Physical machine " + pm + " should not already be connected", cpmMocked.isConnected(pm));
        assertFalse("There should be written error message that physical machine " + pm + " could not "
                  + "be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
        
        //checks this step (and also following steps after this one) has not been done as expected
        verify(vtmMocked,never()).getVirtualMachines();
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which is not connected
     * at the moment of method execution.
     */
    @Test
    public void disconnectFromNotConnectedPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that the method is ended without any further steps being done
        when(cpmMocked.isConnected(pm)).thenReturn(false);
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(pm);
        
        assertFalse("Physical machine " + pm + " should not be connected", cpmMocked.isConnected(pm));
        assertFalse("There should be written error message that physical machine " + pm + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
        
        //checks this step (and also following steps after this one) has not been done as expected
        verify(conMocked, never()).connectTo(pm);
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which has incorrect
     * virtualization tool web server port (against the original physical machine which is
     * recorded as connected physical machine) and that that physical machine cannot and is not
     * disconnected.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectWebServerPort(){
        PhysicalMachine corPM = new PMBuilder().build();
        PhysicalMachine incorPM = new PMBuilder().webserverPort("1111").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect web server port is not found in the list of connected PMs
        //because of the absolute equality)
        when(cpmMocked.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(cpmMocked.isConnected(corPM)).thenReturn(true);
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(incorPM);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", cpmMocked.isConnected(incorPM));
        assertFalse("There should be written error message that physical machine " + incorPM + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
        
        //checks this step (and also following steps after this one) has not been done as expected
        verify(conMocked, never()).connectTo(incorPM);
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which has incorrect
     * username (against the original physical machine which is recorded as connected physical machine)
     * and that that physical machine cannot and is not disconnected.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUsername(){
        PhysicalMachine corPM = new PMBuilder().build();
        PhysicalMachine incorPM = new PMBuilder().username("Henry").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect username is not found in the list of connected PMs because of the absolute equality)
        when(cpmMocked.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(cpmMocked.isConnected(corPM)).thenReturn(true);
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(incorPM);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", cpmMocked.isConnected(incorPM));
        assertFalse("There should be written error message that physical machine " + incorPM + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
        
        //checks this step (and also following steps after this one) has not been done as expected
        verify(conMocked, never()).connectTo(incorPM);
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with physical machine which has incorrect
     * user password (against the original physical machine which is recorded as connected physical machine)
     * and that that physical machine cannot and is not disconnected.
     */
    @Test
    public void disconnectFromPhysicalMachineWithIncorrectUserPassword(){
        PhysicalMachine corPM = new PMBuilder().build();
        PhysicalMachine incorPM = new PMBuilder().userPassword("14gg44").build();
        
        //this step ensures that the method is ended without any further steps being done
        //(PM with incorrect user password is not found in the list of connected PMs because of the absolute equality)
        when(cpmMocked.isConnected(incorPM)).thenReturn(false);
        //this step ensures that quering PM corPM if it is connected returns a positive answer
        when(cpmMocked.isConnected(corPM)).thenReturn(true);
        
        //there should be written an error message informing that physical machine was not connected
        sut.disconnectFrom(incorPM);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", cpmMocked.isConnected(incorPM));
        assertFalse("There should be written error message that physical machine " + incorPM + " was not connected "
                  + " and therefore could not be correctly disconnected", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
        
        //checks this step (and also following steps after this one) has not been done as expected
        verify(conMocked, never()).connectTo(incorPM);
    }
    
    /**
     * This test tests that there should be written an error message on standard error output
     * when the method disconnectFrom() is called with null physical machine, because null physical
     * machine cannot be disconnected.
     */
    @Test
    public void disconnectFromNullPhysicalMachine(){
        //there should be written an error message that there was used an illegal argument
        //for the method disconnectFrom() when this method is called
        sut.disconnectFrom(null);
        
        assertFalse("There should be written an error message that there was made an attempt to "
                  + "disconnect from null physical machine", errContent.toString().isEmpty());
        assertTrue("There should not be written any text on standard output", outContent.toString().isEmpty());
        
        //checks that the very first (espacially more further steps) step after null object check
        //has not been done as expected
        verify(cpmMocked, never()).isConnected(any(PhysicalMachine.class));
    }
    
    /**
     * This test tests that there is returned the TRUE value when a physical machine
     * is connected and a network connection is working.
     */
    @Test
    public void isConnectedWithConnectedPhysicalMachineAndAvailableConnection(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that there will follow a physical test of connection
        when(cpmMocked.isConnected(pm)).thenReturn(true);
        
        //conMocked has not any other options set up which means that everything should work as expected
                
        assertTrue("Physical machine " + pm + " should be connected", sut.isConnected(pm));
    }
    
    /**
     * This test tests that physical machine should not be connected in the future
     * when is recorded as connected, but there is a connection problem
     * during connection recognition.
     */
    @Test
    public void isConnectedWithConnectedPhysicalMachineAndInavailableConnection(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that there will follow a physical test of connection
        when(cpmMocked.isConnected(pm)).thenReturn(true);
        //this step ensures that there appear any connection problem -> physical machine is not connected        
        doThrow(ConnectionFailureException.class).when(conMocked).connectTo(pm);
        
        assertFalse("There should be a network connection problem or virtualization "
                  + "tool web server is not running -> physical machine should be disconnected",
                    sut.isConnected(pm));
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine is not recorded as a connected physical machine.
     */
    @Test
    public void isConnectedWithNotConnectedPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        
        //this step ensures that there will not follow a physical test of connection
        //because the physical machine is not stored in list of connected physical machines
        when(cpmMocked.isConnected(pm)).thenReturn(false);
        
        assertFalse("Physical machine " + pm + " should not be connected", sut.isConnected(pm));
        //checks that the physical test of connection was not done
        verify(conMocked, never()).connectTo(pm);
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine with incorrect web server port is not absolutely equal
     * to some physical machine from the list of connected physical machines.
     */
    @Test
    public void isConnectedWithIncorrectWebServerPort(){
        PhysicalMachine corPM = new PMBuilder().build();
        PhysicalMachine incorPM = new PMBuilder().webserverPort("18080").build();
        
        //this step ensures that there will not follow a physical test of connection
        //because the physical machine is not stored in list of connected physical machines
        when(cpmMocked.isConnected(incorPM)).thenReturn(false);
        //this step ensures that there will follow a physical test of connection for physical machine corPM
        when(cpmMocked.isConnected(corPM)).thenReturn(true);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", sut.isConnected(incorPM));
        assertTrue("Physica machine " + corPM + " should be connected", sut.isConnected(corPM));
        //checks that the physical test of connection was not done for physical machine incorPM
        verify(conMocked, never()).connectTo(incorPM);
        //checks that the physical test of connection was done for physical machine corPM
        verify(conMocked).connectTo(corPM);
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine with incorrect username is not absolutely equal
     * to some physical machine from the list of connected physical machines.
     */
    @Test
    public void isConnectedWithIncorrectUsername(){
        PhysicalMachine corPM = new PMBuilder().build();
        PhysicalMachine incorPM = new PMBuilder().username("Jimbo").build();
        
        //this step ensures that there will not follow a physical test of connection
        //because the physical machine is not stored in list of connected physical machines
        when(cpmMocked.isConnected(incorPM)).thenReturn(false);
        //this step ensures that there will follow a physical test of connection for physical machine corPM
        when(cpmMocked.isConnected(corPM)).thenReturn(true);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", sut.isConnected(incorPM));
        assertTrue("Physica machine " + corPM + " should be connected", sut.isConnected(corPM));
        //checks that the physical test of connection was not done for physical machine incorPM
        verify(conMocked, never()).connectTo(incorPM);
        //checks that the physical test of connection was done for physical machine corPM
        verify(conMocked).connectTo(corPM);
    }
    
    /**
     * This test tests that there is returned a negative answer when a queried
     * physical machine with incorrect user password is not absolutely equal
     * to some physical machine from the list of connected physical machines.
     */
    @Test
    public void isConnectedWithIncorrectUserPassword(){
        PhysicalMachine corPM = new PMBuilder().build();
        PhysicalMachine incorPM = new PMBuilder().userPassword("55448sad8").build();
        
        //this step ensures that there will not follow a physical test of connection
        //because the physical machine is not stored in list of connected physical machines
        when(cpmMocked.isConnected(incorPM)).thenReturn(false);
        //this step ensures that there will follow a physical test of connection for physical machine corPM
        when(cpmMocked.isConnected(corPM)).thenReturn(true);
        
        assertFalse("Physical machine " + incorPM + " should not be connected", sut.isConnected(incorPM));
        assertTrue("Physica machine " + corPM + " should be connected", sut.isConnected(corPM));
        //checks that the physical test of connection was not done for physical machine incorPM
        verify(conMocked, never()).connectTo(incorPM);
        //checks that the physical test of connection was done for physical machine corPM
        verify(conMocked).connectTo(corPM);
    }
    
    /**
     * This test tests that there is returned a negative answer when the method
     * isConnected() is called with illegal argument (null physical machine).
     */
    @Test
    public void isConnectedWithNullPhysicalMachine(){
        assertFalse("Null physical machine cannot be connected", sut.isConnected(null));
        
        //checks that there was not done the first connection test because of illegal argument
        verify(cpmMocked, never()).isConnected(any(PhysicalMachine.class));
    }
    
    /**
     * This test tests that there are all connected physical machines disconnected
     * and work with them is ended up when the method close() is called.
     */
    @Test
    public void closeWithAnyConnectedPhysicalMachine(){         
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("10.12.11.9").username("Elphon")
                                             .userPassword("22mn54fg").build();
        //a nonempty list of connected physical machines
        List<PhysicalMachine> pmsList = Arrays.asList(pm1,pm2);
        //sutl represents the same as sut, but here is it for mocking intention (easier testing)
        ConnectionManagerImpl sutl = mock(ConnectionManagerImpl.class);
        //this step ensures the real method is called
        doCallRealMethod().when(sutl).close();
        //this step that there will be returned the nonempty list of connected physical machines
        when(cpmMocked.getConnectedPhysicalMachines()).thenReturn(pmsList);
        
        sutl.close();
        
        //checks the method disconnectFrom() was called two times, because in the list of
        //connected physical machines there was 2 physical machines stored
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
        when(cpmMocked.getConnectedPhysicalMachines()).thenReturn(pmsList);
        
        sutl.close();
        
        //checks that there was not any call of method disconnectFrom(), because there was not
        //any physical machine to be disconnected
        verify(sutl, never()).disconnectFrom(any(PhysicalMachine.class));
    }    
    
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
