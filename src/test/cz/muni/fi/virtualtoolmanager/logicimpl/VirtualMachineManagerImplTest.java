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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.PortRule;
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.ProtocolType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This test class ensures unit testing of class VirtualMachineManagerImpl and
 * is intended to be a pointer that class VirtualMachineManagerImpl works as
 * expected.
 *
 * @author Tomáš Šmíd
 */
public class VirtualMachineManagerImplTest {

    private VirtualMachineManagerImpl sut;
    private NativeVBoxAPIMachine natAPIMachMocked;
    private ConnectionManagerImpl conManMocked;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setUp() {
        natAPIMachMocked = mock(NativeVBoxAPIMachine.class);
        conManMocked = mock(ConnectionManagerImpl.class);
        sut = new VirtualMachineManagerImpl(natAPIMachMocked, conManMocked);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanUp() {
        System.setOut(null);
        System.setErr(null);
    }

    /**
     * This test tests that if there are all neccessary conditions for virtual
     * machine start-up met then the virtual machine is successfully started and
     * on a standard output appears an informing message.
     */
    @Test
    public void startVMIdealCase() {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);

        //there should not appear any exception nor error
        sut.startVM(vm);

        assertFalse("There should be written a message on a standard output that the virtual "
                + "machine was successfully started", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a null virtual machine argument then the virtual machine
     * start-up operation itself is not even started and on a standard error
     * output appears an error informing message.
     */
    @Test
    public void startNullVM() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {

        sut.startVM(null);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a null virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which has a null host machine attribute
     * value then the virtual machine start-up operation itself is not even
     * started and on a standard error output appears an error informing
     * message.
     */
    /*@Test
    public void startVMWithNullPhysicalMachine() {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().hostMachine(null).build();

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine with a null host machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which has a null id then the virtual
     * machine start-up operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void startVMWithNullId() {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().id(null).build();

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine with a null id", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which has an empty id then the virtual
     * machine start-up operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void startVMWithEmptyId() {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().id(UUID.fromString("")).build();

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine with an empty id", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which has a null name then the virtual
     * machine start-up operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void startVMWithNullName() {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().name(null).build();

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine with a null name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which has an empty name then the virtual
     * machine start-up operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void startVMWithEmptyName() {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().name("").build();

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine with an empty name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which does not exist then the start-up
     * operation is ended and on a standard error output appears an error
     * informing message.
     */
    @Test
    public void startNonexistentVM() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that the virtual machine does not exist and cannot be started
        doThrow(UnknownVirtualMachineException.class).when(natAPIMachMocked).startVM(vm);

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine which does not exist", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which is not accessible (probably its
     * source files are missing or corrupted) then the start-up operation is
     * ended and on a standard error output appears an error informing message.
     */
    @Test
    public void startInaccessibleVM() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that the virtual machine's source files are missing or corrupted
        //and cannot be started
        doThrow(UnexpectedVMStateException.class).when(natAPIMachMocked).startVM(vm);

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine which is not accessible", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which is not in a required state (is in
     * invalid state "Running" or "Paused") then the start-up operation is ended
     * and on a standard error output appears an error informing message.
     */
    @Test
    public void startVMWithInvalidState() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that the virtual machine has already been started, because the only
        //invalid states are "Running" and "Paused" and therefore cannot be started again
        doThrow(UnexpectedVMStateException.class).when(natAPIMachMocked).startVM(vm);

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine which is not in a required state for start-up", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which is already locked (there exists
     * another process which is working with the virtual machine now) then the
     * start-up operation is ended and on a standard error output appears an
     * error informing message.
     */
    @Test
    public void startAlreadyLockedVM() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that the virtual machine is being used by another process now
        //which has the lock on the virtual machine
        doThrow(UnexpectedVMStateException.class).when(natAPIMachMocked).startVM(vm);

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine which is already being used by another process",
                errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which should be started on a physical
     * machine which is not connected then the virtual machine start-up operation
     * itself is not even started and on a standard error output appears an error
     * informing message.
     */
    @Test
    public void startVMOnDisconnectedPhysicalMachine() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.startVM(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to start a virtual machine on a physical machine which is not connected",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which should be started on a physical
     * machine which should be connected, but there did not manage to connect to it
     * again and means the physical machine is not connected now, then the virtual
     * machine start-up operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    @Test
    public void startVMWithSuddenNetworkConnectionLossOuterCheck() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //should be connected, but there did not manage to connect to it again and
        //therefore the physical machine is not connected now and there is not 
        //possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.startVM(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::startVM() has never been called as expected
        verify(natAPIMachMocked, never()).startVM(any(VirtualMachine.class));
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::startVM()
     * is called with a virtual machine which should be started on a physical
     * machine which should be connected, but there occured any connection
     * problem at the start-up operation beginning (when the VirtualBoxManager
     * instance is connecting to VirtualBox) then the start-up operation is ended
     * and on a standard error output appears an error informing message.
     */
    @Test
    public void startVMWithSuddenNetworkConnectionLossInnerCheck() throws ConnectionFailureException, UnknownVirtualMachineException, UnexpectedVMStateException {
        //represents a virtual machine which should be started
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::startVM() is called with a required virtual machine
        //and means that there occured any connection problem at the beginning
        //of the start-up operation
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).startVM(vm);

        sut.startVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for virtual
     * machine shutdown operation met then the operation is performed successfully
     * and on a standard output appears an informing message about that.
     */
    @Test
    public void shutDownVMIdealCase() {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);

        //there should not appear any exception nor error
        sut.shutDownVM(vm);

        assertFalse("There should be written a message on a standard output that the virtual "
                + "machine was successfully shut down", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a null virtual machine argument then the virtual machine
     * shutdown operation itself is not even started and on a standard error
     * output appears an error informing message.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownNullVM() throws Exception {

        sut.shutDownVM(null);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a null virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which has a null host machine attribute
     * value then the virtual machine shutdown operation itself is not even
     * started and on a standard error output appears an error informing
     * message.
     */
    /*@Test
    public void shutDownVMWithNullPhysicalMachine() {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().hostMachine(null).build();

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine with a null host machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which has a null id then the virtual
     * machine shutdown operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void shutDownVMWithNullId() {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().id(null).build();

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine with a null id", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which has an empty id then the virtual
     * machine shutdown operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void shutDownVMWithEmptyId() {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().id(UUID.fromString("")).build();

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine with an empty id", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which has a null name then the virtual
     * machine shutdown operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void shutDownVMWithNullName() {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().name(null).build();

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine with a null name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which has an empty name then the virtual
     * machine shutdown operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    /*@Test
    public void shutDownVMWithEmptyName() {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().name("").build();

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine with an empty name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which does not exist then the shutdown
     * operation is ended and on a standard error output appears an error
     * informing message.
     */
    @Test
    public void shutDownNonexistentVM() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that the virtual machine does not exist and cannot be shut down
        doThrow(UnknownVirtualMachineException.class).when(natAPIMachMocked).shutDownVM(vm);

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine which does not exist", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which is not accessible (probably its
     * source files are missing or corrupted) then the shutdown operation is
     * ended and on a standard error output appears an error informing message.
     */
    @Test
    public void shutDownInaccessibleVM() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that the virtual machine's source files are missing or corrupted
        //and cannot be shut down
        doThrow(UnexpectedVMStateException.class).when(natAPIMachMocked).shutDownVM(vm);

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine which is not accessible", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which is not in a required state (is not
     * in one of valid states "Running", "Paused" or "Stuck") then the shutdown
     * operation is ended and on a standard error output appears an error informing message.
     */
    @Test
    public void shutDownVMWithInvalidState() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the UnexpectedVMStateException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that the virtual machine is not ready to be shut down or it is already powered off,
        //because the only valid states are "Running", "Paused" or "Stuck" and all another states are
        //invalid
        doThrow(UnexpectedVMStateException.class).when(natAPIMachMocked).shutDownVM(vm);

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine which is not in a required state for shutdown", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which should be started on a physical
     * machine which is not connected then the virtual machine shutdown operation
     * itself is not even started and on a standard error output appears an error
     * informing message.
     */
    @Test
    public void shutDownVMOnDisconnectedPhysicalMachine() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.shutDownVM(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to shut down a virtual machine on a physical machine which is not connected",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which should be shut down on a physical
     * machine which should be connected, but there did not manage to connect to it
     * again and means the physical machine is not connected now, then the virtual
     * machine shutdown operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    @Test
    public void shutDownVMWithSuddenNetworkConnectionLossOuterCheck() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //should be connected, but there did not manage to connect to it again and
        //therefore the physical machine is not connected now and there is not 
        //possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.shutDownVM(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::shutDownVM() has never been called as expected
        verify(natAPIMachMocked, never()).shutDownVM(any(VirtualMachine.class));
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());        
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::shutDownVM()
     * is called with a virtual machine which should be shut down on a physical
     * machine which should be connected, but there occured any connection
     * problem at the shutdown operation beginning (when the VirtualBoxManager
     * instance is connecting to VirtualBox) then the shutdown operation is ended
     * and on a standard error output appears an error informing message.
     */
    @Test
    public void shutDownVMWithSuddenNetworkConnectionLossInnerCheck() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::shutDownVM() is called with a required virtual machine
        //and means that there occured any connection problem at the beginning
        //of the shutdown operation
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).shutDownVM(vm);

        sut.shutDownVM(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for new
     * port rule addition operation met then the operation is performed successfully
     * and on a standard output appears an informing message about that.
     */
    @Test
    public void addPortRuleIdealCase() {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
       
       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of used port rules in order to
        //fasten the port rule name and host port duplicity check
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(new ArrayList<>());
        
        //there should not appear any exception nor error
        sut.addPortRule(vm,portRule);

        assertFalse("There should be written a message on a standard output that the particular "
                + "port rule was successfully added to the particular virtual machine", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a null virtual machine then the new port rule addition operation
     * itself is not even started and on a standard error output appears an
     * informing error message.
     */
    @Test
    public void addPortRuleWithNullVirtualMachine() throws Exception {
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        sut.addPortRule(null, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a new port rule to a null virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /*@Test
    public void addPortRuleWithNullVMPhysicalMachine() {

    }

    @Test
    public void addPortRuleWithNullVMId() {

    }

    @Test
    public void addPortRuleWithEmptyVMId() {

    }

    @Test
    public void addPortRuleWithNullVMName() {

    }

    @Test
    public void addPortRuleWithEmptyVMName() {

    }*/
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::getPortRules() is
     * called as part of the method VirtualMachineManagerImpl::addPortRule() with
     * a virtual machine which does not exist then the new port rule addition 
     * operation is ended and on a standard error output appears an informing error message.
     */
    @Test
    public void addPortRuleWithNonexistentVMGetter() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required
        //virtual machine and means that the virtual machine does not exist and
        //port rules cannot got and used for new port rule name and host port duplicity
        doThrow(UnknownVirtualMachineException.class).when(natAPIMachMocked).getPortRules(vm);
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a new port rule to a virtual machine which does not exist",
                    errContent.toString().isEmpty());
        //check the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a virtual machine which does not exist then the new port rule
     * addition operation is ended and on a standard error output appears an
     * informing error message.
     */
    @Test
    public void addPortRuleWithNonexistentVMAddition() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::addPortRule() is called with a required
        //virtual machine and port rule and means that the virtual machine does not
        //exist and the port rule cannot be added to that virtual machine
        doThrow(UnknownVirtualMachineException.class).when(natAPIMachMocked).addPortRule(vm, portRule);
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a new port rule to a virtual machine which does not exist",
                    errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a null port rule then the new port rule addition operation
     * itself is not even started and on a standard error output appears an
     * informing error message.
     */
    @Test
    public void addNullPortRule() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        
        sut.addPortRule(vm, null);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a null port rule to a virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which has a null name then the new port rule addition
     * operation itself is not even started and on a standard error output appears an
     * informing error message.
     */
    @Test
    public void addPortRuleWithNullName() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder(null, 22, 1540).build();
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a port rule with a null name to a virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which has an empty name then the new port rule addition
     * operation itself is not even started and on a standard error output appears an
     * informing error message.
     */
    @Test
    public void addPortRuleWithEmptyName() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("", 22, 1540).build();
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a port rule with an empty name to a virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which has the host port number smaller than 0
     * then the new port rule addition operation itself is not even started and
     * on a standard error output appears an informing error message.
     */
    @Test
    public void addPortRuleWithInvalidHostPortNumberNegative() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", -1, 1540).build();
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a port rule with a negative host port number to a virtual machine",
                    errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which has the host port number bigger than 65535
     * then the new port rule addition operation itself is not even started and
     * on a standard error output appears an informing error message.
     */
    @Test
    public void addPortRuleWithInvalidHostPortNumberTooBig() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 65536, 1540).build();
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a port rule with a too big host port number to a virtual machine",
                    errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which has the guest port number smaller than 0
     * then the new port rule addition operation itself is not even started and
     * on a standard error output appears an informing error message.
     */
    @Test
    public void addPortRuleWithInvalidGuestPortNumberNegative() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, -1).build();
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a port rule with a negative guest port number to a virtual machine",
                    errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which has the guest port number bigger than 65535
     * then the new port rule addition operation itself is not even started and
     * on a standard error output appears an informing error message.
     */
    @Test
    public void addPortRuleWithInvalidGuestPortNumberTooBig() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, 65536).build();
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a port rule with a too big guest port number to a virtual machine",
                    errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which name is used by another active port rule
     * then the new port rule addition operation itself is not even started and
     * on a standard error output appears an informing error message.
     */
    @Test
    public void addPortRuleWithDuplicitName() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //represents a non=empty list of used port rules on virtual machine vm
        List<String> portRules = Arrays.asList("PortRule_01,TCP,,44,,19540");
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of used port rules with one
        //port rule which has the same name as a port rule which should be newly added
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(portRules);
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a new port rule with a duplicit name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a port rule which host port number is used by another active port rule
     * then the new port rule addition operation itself is not even started and
     * on a standard error output appears an informing error message.
     */
    @Test
    public void addPortRuleWithDuplicitHostPortNumber() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        //represents a non=empty list of used port rules on virtual machine vm
        List<String> portRules = Arrays.asList("PortRule_02,UDP,,22,,19540");
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of used port rules with one
        //port rule which has the same host port number as a port rule which should be newly added
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(portRules);
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a new port rule with a duplicit host port number", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule() is
     * called with a virtual machine which has not set up a required network
     * attachment type (NAT network attachment type) then the new port rule
     * addition operation is ended and on a standard error output appears an
     * informing error message.
     */
    @Test
    public void addPortRuleWithInvalidAttachmentType() throws Exception {
        //represents a virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of used port rules in order to
        //fasten the port rule name and host port duplicity check
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(new ArrayList<>());
        //there should returned the UnexpectedVMStateException exception when the method
        //NativeVBoxAPIMachine::addPortRule() is called with a required virtual machine
        //which has not set up a valid network attachment type
        doThrow(UnexpectedVMStateException.class).when(natAPIMachMocked).addPortRule(vm, portRule);
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a new port rule to a virtual machine with an invalid network attachment type",
                    errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule()
     * is called with a virtual machine to which should be added a new port rule
     * and which is located on a physical machine which is not connected then the
     * new port rule addition operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    @Test
    public void addPortRuleOnDisconnectedPhysicalMachine() throws Exception {
        //represents a virtual machine to which should be added a new port rule        
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to add a new port rule to a virtual machine on a physical machine which is not connected",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule()
     * is called with a virtual machine to which should be added a new port rule
     * and which is located on a physical machine which should be connected, but
     * there did not manage to connect to it again and means the physical machine
     * is not connected now, then the new port rule addition operation itself
     * is not even started and on a standard error output appears an error informing message.
     */
    @Test
    public void addPortRuleWithSuddenNetworkConnectionLossOuterCheck() throws Exception {
        //represents a virtual machine to which should be added a new port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a negative answer which means the host machine
        //should be connected, but there did not manage to connect to it again and
        //therefore the physical machine is not connected now and there is not 
        //possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.addPortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule()
     * is called with a virtual machine to which should be added a new port rule
     * and which is located on a physical machine which should be connected, but
     * there occured any connection problem while there are being got an active
     * existent port rules from the required virtual machine (when the VirtualBoxManager
     * instance is connecting to VirtualBox) then the new port rule addition operation
     * iself is not even started and on a standard error output appears an error informing message.
     */
    @Test
    public void addPortRuleWithSuddenNetworkConnectionLossInnerCheckGetter() throws Exception {        
        //represents a virtual machine to which should be added a new port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        //and means that there occured any connection problem and port rules cannot be retrived
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).getPortRules(vm);

        sut.addPortRule(vm, portRule);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::addPortRule() has never been called as expected
        verify(natAPIMachMocked, never()).addPortRule(any(VirtualMachine.class), any(PortRule.class));
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::addPortRule()
     * is called with a virtual machine to which should be added a new port rule
     * and which is located on a physical machine which should be connected, but
     * there occured any connection problem at the addition operation itself
     * beginning (when the VirtualBoxManager instance is connecting to VirtualBox)
     * then the new port rule addition operation is ended and on a standard error
     * output appears an error informing message.
     */
    @Test
    public void addPortRuleWithSuddenNetworkConnectionLossInnerCheckAddition() throws Exception {
        //represents a virtual machine to which should be added a new port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be added to the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::addPortRule() is called with a required virtual machine
        //and means that there occured any connection problem and a new port rule cannot be added
        //to the virtual machine
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).addPortRule(vm, portRule);

        sut.addPortRule(vm, portRule);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }
    
    /**
     * This test tests that if there are all neccessary conditions for port rule
     * deletion operation met then the port rule deletion operation is performed
     * successfully and on a standard output appears an informing message about that.
     */
    @Test
    public void deletePortRuleIdealCase() {
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
       
       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        
        //there should not appear any exception nor error
        sut.deletePortRule(vm, portRule);

        assertFalse("There should be written a message on a standard output that the particular "
                + "port rule was successfully deleted  from the particular virtual machine", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a null virtual machine then the port rule deletion operation
     * itself is not even started and on a standard error output appears an
     * informing error message.
     */
    @Test
    public void deletePortRuleWithNullVirtualMachine() throws Exception {
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        sut.deletePortRule(null, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete a port rule from a null virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /*@Test
    public void deletePortRuleWithNullVMPhysicalMachine() {

    }

    @Test
    public void deletePortRuleWithNullVMId() {

    }

    @Test
    public void deletePortRuleWithEmptyVMId() {

    }

    @Test
    public void deletePortRuleWithNullVMName() {

    }

    @Test
    public void deletePortRuleWithEmptyVMName() {

    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a virtual machine which does not exist then the port rule
     * deletion operation itself is not even started and on a standard error output
     * appears an informing error message.
     */
    @Test
    public void deletePortRuleWithNonexistentVM() throws Exception {
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();
        
        sut.deletePortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete a port rule from a virtual machine which does not exist", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a null port rule then the port rule deletion operation itself
     * is not even started and on a standard error output appears an informing error message.
     */
    @Test
    public void deleteNullPortRule() throws Exception {
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        
        sut.deletePortRule(vm, null);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete a null port rule", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a port rule which has a null name then the port rule
     * deletion operation itself is not even started and on a standard error output
     * appears an informing error message.
     */
    @Test
    public void deletePortRuleWithNullName() throws Exception {
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder(null,22,1540).build();
        
        sut.deletePortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete a port rule with a null name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a port rule which has an empty name then the port rule
     * deletion operation itself is not even started and on a standard error output
     * appears an informing error message.
     */
    @Test
    public void deletePortRuleWithEmptyName() throws Exception {
        //represents a virtual machine from which a port rule should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("",22,1540).build();
        
        sut.deletePortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete a port rule with an empty name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a virtual machine from which should be a port rule deleted
     * and which is located on a physical machine which is not connected then the
     * port rule deletion operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    @Test
    public void deletePortRuleFromDisconnectedPhysicalMachine() throws Exception {
        //represents a virtual machine from which should be deleted a port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.deletePortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete a port rule from a virtual machine on a physical machine which is not connected",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a virtual machine from which should be deleted a port rule
     * and which is located on a physical machine which should be connected, but
     * there did not manage to connect to it again and means the physical machine
     * is not connected now, then the port rule deletion operation itself is not
     * even started and on a standard error output appears an error informing message.
     */
    @Test
    public void deletePortRuleWithSuddenNetworkConnectionLossOuterCheck() throws Exception {
        //represents a virtual machine from which should be deleted a port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a negative answer which means the host machine
        //should be connected, but there did not manage to connect to it again and
        //therefore the physical machine is not connected now and there is not 
        //possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.deletePortRule(vm, portRule);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::deletePortRule()
     * is called with a virtual machine from which should be deleted a port rule
     * and which is located on a physical machine which should be connected, but
     * there occured any connection problem at the deletion operation itself
     * beginning (when the VirtualBoxManager instance is connecting to VirtualBox)
     * then the port rule deletion operation is ended and on a standard error
     * output appears an error informing message.
     */
    @Test
    public void deletePortRuleWithSuddenNetworkConnectionLossInnerCheck() throws Exception {
        //represents a virtual machine from which should be deleted a port rule
        VirtualMachine vm = new VMBuilder().build();
        //represents a port rule which should be deleted from the virtual machine vm
        PortRule portRule = new PortRule.Builder("PortRule_01",22,1540).build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::deletePortRule() is called with a required virtual machine
        //and means that there occured any connection problem and a port rule cannot be deleted
        //from the virtual machine
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).deletePortRule(vm, portRule.getName());

        sut.deletePortRule(vm, portRule);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules deletion met and some port rules exist on a particular virtual machine
     * then all these port rules are successfully deleted and on a standard output
     * appears an informing message about that.
     */
    @Test
    public void deleteAllPortRulesWithSomeRules() {
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents the first of two port rules which exist on the virtual machine vm
        String portRule1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm
        String portRule2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules on virtual machine vm
        List<String> portRules = Arrays.asList(portRule1,portRule2);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(portRules);
        
        //there should not appear any exception nor error
        sut.deleteAllPortRules(vm);
        
        assertFalse("There should be written a message on a standard output that all port rules were "
                + "successfully deleted", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());        
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules deletion met and no port rule exists on a particular virtual machine
     * then the port rule deletion operation is not needed to be performed and
     * on a standard output appears an informing message about that.
     */
    @Test
    public void deleteAllPortRulesWithNoRules() throws Exception {
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(new ArrayList<>());
        
        //there should not appear any exception nor error
        sut.deleteAllPortRules(vm);
        
        assertFalse("There should be written a message on a standard output that there are no port rules "
                + "to be deleted", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deleteAllPortRules()
     * is called with a null virtual machine then the port rules getting operation
     * nor the port rule deletion operation are not even performed and on a standard
     * error output appears an informing error message.
     */
    @Test
    public void deleteAllPortRulesWithNullVirtualMachine() throws Exception {
        
        sut.deleteAllPortRules(null);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete all port rules from a null virtual machine", errContent.toString().isEmpty());
        //checks that the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMocked, never()).getPortRules(any(VirtualMachine.class));
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /*@Test
    public void deleteAllPortRulesWithNullVMPhysicalMachine() {

    }

    @Test
    public void deleteAllPortRulesWithNullVMId() {

    }

    @Test
    public void deleteAllPortRulesWithEmptyVMId() {

    }

    @Test
    public void deleteAllPortRulesWithNullVMName() {

    }

    @Test
    public void deleteAllPortRulesWithEmptyVMName() {

    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deleteAllPortRules()
     * is called with a virtual machine which does not exist then the attempt to
     * delete all port rules is ended after the NativeVBoxAPIMachine::getPortRules()
     * method is called (there is founded out the virtual machine does not exist)
     * and on a standard error output appears an error informing message.
     */
    @Test
    public void deleteAllPortRulesWithNonexistentVMGetter() throws Exception {
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
                
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine and means
        //that there is no virtual machine like required on a particular physical machine
        doThrow(UnknownVirtualMachineException.class).when(natAPIMachMocked).getPortRules(vm);
                
        sut.deleteAllPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete all port rules from a virtual machine which does not exist", errContent.toString().isEmpty());
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::deleteAllPortRules()
     * is called with a virtual machine which exists, but after the port rules getting
     * operation is finished the virtual machine is suddenly lost, then the attempt to
     * delete all port rules is ended and on a standard error output appears an error
     * informing message.
     */
    @Test
    public void deleteAllPortRulesWithNonexistentVMDeletion() throws Exception {
        //represents a virtual machine from which all port rules should be deleted
        VirtualMachine vm = new VMBuilder().build();
        //represents the first of two port rules which exist on the virtual machine vm
        String portRule1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm
        String portRule2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules on virtual machine vm
        List<String> portRules = Arrays.asList(portRule1,portRule2);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(portRules);
        //there should be thrown the UnknownVirtualMachineException exception when
        //the method NativeVBoxAPIMachine::deletePortRule() is called with the first
        //of the rules and simulates a case when a virtual machine from which should be
        //deleted all port rules is suddenly lost (removed from a list of registered virtual machines)
        doThrow(UnknownVirtualMachineException.class).when(natAPIMachMocked).deletePortRule(vm, "PortRule_01");
                
        sut.deleteAllPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete all port rules from a virtual machine which does not exist", errContent.toString().isEmpty());        
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::deleteAllPortRules()
     * is called with a virtual machine from which should be deleted all port rules
     * and which is located on a physical machine which is not connected then the
     * port rule deletion operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    @Test
    public void deleteAllPortRulesFromDisconnectedPhysicalMachine() throws Exception {
        //represents a virtual machine from which should be all port rules deleted        
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.deleteAllPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to delete all port rules from a virtual machine which is on a physical machine which is not connected",
                errContent.toString().isEmpty());
        //checks that the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMocked, never()).getPortRules(any(VirtualMachine.class));
        //checks that the method NativeVBoxAPIMachine::deletePortRule() has never been called as expeted
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deleteAllPortRules()
     * is called with a virtual machine from which should be deleted all port rules
     * and which is located on a physical machine which should be connected, but
     * there did not manage to connect to it again and means the physical machine
     * is not connected now, then the port rule deletion operation itself
     * is not even started and on a standard error output appears an error informing message.
     */
    @Test
    public void deleteAllPortRulesWithSuddenNetworkConnectionLossOuterCheck() throws Exception {
        //represents a virtual machine from which should be all port rules deleted
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //should be connected, but there did not manage to connect to it again and
        //therefore the physical machine is not connected now and there is not 
        //possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.deleteAllPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMocked, never()).getPortRules(any(VirtualMachine.class));
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deleteAllPortRules()
     * is called with a virtual machine from which should be deleted all port rules
     * and which is located on a physical machine which should be connected, but
     * there occured any connection problem while there are being got an active
     * existent port rules from the required virtual machine (when the VirtualBoxManager
     * instance is connecting to VirtualBox) then the port rule deletion operation
     * iself is not even started and on a standard error output appears an error informing message.
     */
    @Test
    public void deleteAllPortRulesWithSuddenNetworkConnectionLossInnerCheckGetter() throws Exception {
        //represents a virtual machine from which should be all port rules deleted
        VirtualMachine vm = new VMBuilder().build();        

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        //and means that there occured any connection problem and port rules cannot be retrieved
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).getPortRules(vm);

        sut.deleteAllPortRules(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::deletePortRule() has never been called as expected
        verify(natAPIMachMocked, never()).deletePortRule(any(VirtualMachine.class), anyString());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::deleteAllPortRules()
     * is called with a virtual machine from which should be deleted all port rules
     * and which is located on a physical machine which should be connected, but
     * there occured any connection problem at the deletion operation itself
     * beginning (when the VirtualBoxManager instance is connecting to VirtualBox)
     * then the port rule deletion operation is ended and on a standard error
     * output appears an error informing message.
     */
    @Test
    public void deleteAllPortRulesWithSuddenNetworkConnectionLossInnerCheckDeletion() throws Exception {
        //represents a virtual machine from which should be all port rules deleted
        VirtualMachine vm = new VMBuilder().build();        
        //represents the first of two port rules which exist on the virtual machine vm
        String portRule1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm
        String portRule2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules on virtual machine vm
        List<String> portRules = Arrays.asList(portRule1,portRule2);

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(portRules);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::deletePortRule() is called with a required virtual machine
        //and first of port rules and means that there occured any connection problem and a port rule
        //cannot be deleted from the virtual machine 
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).deletePortRule(vm, "PortRule_01");

        sut.deleteAllPortRules(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules retrieve operation met and on a particular virtual machine are present
     * any port rules then these port rules are returned as a result of
     * VirtualMachineManagerImpl::getPortRules() method call and no exception nor
     * error should appear.
     */
    @Test
    public void getPortRulesWithReturnedNonemptyList() {
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();        
        //represents the first of two port rules which exist on the virtual machine vm in a string form
        String portRuleStr1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules which exist on the virtual machine vm in a string form
        String portRuleStr2 = "PortRule_02,UDP,10.10.10.10,5875,15.15.15.15,3650";
        //represents a list of existent port rules in a string form on virtual machine vm
        List<String> portRulesStr = Arrays.asList(portRuleStr1,portRuleStr2);
        PortRule portRule1 = new PortRule.Builder("PortRule_01", 22, 1540).build();
        PortRule portRule2 = new PortRule.Builder("PortRule_02", 5875, 3650).hostIP("10.10.10.10")
                                         .guestIP("15.15.15.15").protocol(ProtocolType.UDP).build();
        List<PortRule> expPortRules = Arrays.asList(portRule1,portRule2);
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned a non-empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(portRulesStr);
        
        List<PortRule> actPortRules = sut.getPortRules(vm);
        
        //lists are sorted in order to have surer comparison results
        Collections.sort(expPortRules, prComparator);
        Collections.sort(actPortRules, prComparator);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        assertDeepPRsEquals(expPortRules, actPortRules);
    }

    /**
     * This test tests that if there are all neccessary conditions for all port
     * rules retrieve operation met and on a particular virtual machine is not present
     * any port rule then there is returned an empty list of port rules as a result of
     * VirtualMachineManagerImpl::getPortRules() method call and no exception nor
     * error should appear.
     */
    @Test
    public void getPortRulesWithReturnedEmptyList() {
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be returned an empty list of existent port rules when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        when(natAPIMachMocked.getPortRules(vm)).thenReturn(new ArrayList<>());
        
        List<PortRule> actPortRules = sut.getPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        assertTrue("The list of port rules should be empty", actPortRules.isEmpty());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::getPortRules()
     * is called with a null virtual machine then the port rule retrieve operation
     * itself is not even started, also there is returned an empty list of port rules
     * as a result and on a standard error output appears an error informing message.
     */
    @Test
    public void getPortRulesWithNullVirtualMachine() {
        
        List<PortRule> actPortRules = sut.getPortRules(null);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to retrieve all port rules from a null virtual machine", errContent.toString().isEmpty());
        assertTrue("The list of port rules should be empty", actPortRules.isEmpty());
    }

    /*@Test
    public void getPortRulesWithNullVMPhysicalMachine() {

    }

    @Test
    public void getPortRulesWithNullVMId() {

    }

    @Test
    public void getPortRulesWithEmptyVMId() {

    }

    @Test
    public void getPortRulesWithNullVMName() {

    }

    @Test
    public void getPortRulesWithEmptyVMName() {

    }*/

    /**
     * This test tests that if the method VirtualMachineManagerImpl::getPortRules()
     * is called with a virtual machine which does not exist then the port rules
     * retrieve operation is ended and on a standard error output appears an error
     * informing message.
     */
    @Test
    public void getPortRulesWithNonexistentVM() {
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
        //there should be thrown the UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine and
        //means that the virtual machine does not exist and therefore port rules cannot be retrieved
        doThrow(UnknownVirtualMachineException.class).when(natAPIMachMocked).getPortRules(vm);
        
        List<PortRule> actPortRules = sut.getPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
        assertTrue("The list of port rules should be empty", actPortRules.isEmpty());
    }
    
    /**
     * This test tests that if the method VirtualMachineManagerImpl::getPortRules()
     * is called with a virtual machine from which should be all port rules retrieved
     * and which is located on a physical machine which is not connected then the
     * port rules retrieve operation itself is not even started and on a standard
     * error output appears an error informing message.
     */
    @Test
    public void getPortRulesFromDisconnectedPhysicalMachine() {
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();        

       //there should be returned a negative answer which means the host machine
        //is not connected and therefore there is not possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.getPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt "
                + "to retrieve port rules from a virtual machine which is on a physical machine which is not connected",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMocked, never()).getPortRules(any(VirtualMachine.class));
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::getPortRules()
     * is called with a virtual machine from which should be retrieved all port rules
     * and which is located on a physical machine which should be connected, but
     * there did not manage to connect to it again and means the physical machine
     * is not connected now, then the port rules retrieve operation itself is not
     * even started and on a standard error output appears an error informing message.
     */
    @Test
    public void getPortRulesWithSuddenNetworkConnectionLossOuterCheck() {
        //represents a virtual machine from which should be all port rules retrieved
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a negative answer which means the host machine
        //should be connected, but there did not manage to connect to it again and
        //therefore the physical machine is not connected now and there is not 
        //possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(false);
        
        sut.getPortRules(vm);
        
        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIMachine::getPortRules() has never been called as expected
        verify(natAPIMachMocked, never()).getPortRules(any(VirtualMachine.class));
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * This test tests that if the method VirtualMachineManagerImpl::getPortRules()
     * is called with a virtual machine from which should be all port rules retrieved
     * and which is located on a physical machine which should be connected, but
     * there occured any connection problem at the retrieve operation itself
     * beginning (when the VirtualBoxManager instance is connecting to VirtualBox)
     * then the port rule retrieve operation is ended and on a standard error
     * output appears an error informing message.
     */
    @Test
    public void getPortRulesWithSuddenNetworkConnectionLossInnerCheck() {
        //represents a virtual machine from which should be deleted a port rule
        VirtualMachine vm = new VMBuilder().build();

       //there should be returned a positive answer which means the host machine
        //is connected and therefore there is possible to work with virtual machines
        when(conManMocked.isConnected(vm.getHostMachine())).thenReturn(true);
       //there should be thrown the ConnectionFailureException exception when
        //the method NativeVBoxAPIMachine::getPortRules() is called with a required virtual machine
        //and means that there occured any connection problem and port rules cannot be retrieved
        //from the virtual machine
        doThrow(ConnectionFailureException.class).when(natAPIMachMocked).getPortRules(vm);

        sut.getPortRules(vm);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there occured "
                + "any connection problem", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(vm.getHostMachine());
    }

    /**
     * Class Builder for easier and faster creating and setting up new object of
     * type PhysicalMachine.
     */
    class PMBuilder {

        private String addressIP = "180.148.14.10";
        private String portOfVBoxWebServer = "18083";
        private String username = "Jack";
        private String userPassword = "tr1h15jk7";

        public PMBuilder() {

        }

        public PMBuilder addressIP(String value) {
            this.addressIP = value;
            return this;
        }

        public PMBuilder webserverPort(String value) {
            this.portOfVBoxWebServer = value;
            return this;
        }

        public PMBuilder username(String value) {
            this.username = value;
            return this;
        }

        public PMBuilder userPassword(String value) {
            this.userPassword = value;
            return this;
        }

        public PhysicalMachine build() {
            return new PhysicalMachine(this.addressIP, this.portOfVBoxWebServer,
                    this.username, this.userPassword);
        }
    }

    /**
     * Class Builder for easier and faster creating and setting up new object of
     * type VirtualMachine.
     */
    class VMBuilder {

        private UUID id = UUID.fromString("793d084a-0189-4a55-a9b7-531c455570a1");
        private String name = "VirtualMachine_01";
        private PhysicalMachine hostMachine = new PMBuilder().build();
        private Long countOfCPU = 1L;
        private Long countOfMonitors = 1L;
        private Long cpuExecutionCap = 100L;
        private Long hardDiskFreeSpaceSize = 14286848000L;
        private Long hardDiskTotalSize = 21474836480L;
        private Long sizeOfRAM = 4096L;
        private Long sizeOfVRAM = 12L;
        private String typeOfOS = "Linux";
        private String identifierOfOS = "Fedora_64";

        public VMBuilder() {

        }

        public VMBuilder id(UUID uuid) {
            this.id = uuid;
            return this;
        }

        public VMBuilder name(String name) {
            this.name = name;
            return this;
        }

        public VMBuilder hostMachine(PhysicalMachine hostMachine) {
            this.hostMachine = hostMachine;
            return this;
        }

        public VMBuilder countOfCPU(Long count) {
            this.countOfCPU = count;
            return this;
        }

        public VMBuilder countOfMonitors(Long count) {
            this.countOfMonitors = count;
            return this;
        }

        public VMBuilder cpuExecutionCap(Long cpuExecutionCap) {
            this.cpuExecutionCap = cpuExecutionCap;
            return this;
        }

        public VMBuilder hardDiskFreeSpaceSize(Long size) {
            this.hardDiskFreeSpaceSize = size;
            return this;
        }

        public VMBuilder hardDiskTotalSize(Long size) {
            this.hardDiskTotalSize = size;
            return this;
        }

        public VMBuilder sizeOfRAM(Long size) {
            this.sizeOfRAM = size;
            return this;
        }

        public VMBuilder sizeOfVRAM(Long size) {
            this.sizeOfVRAM = size;
            return this;
        }

        public VMBuilder typeOfOS(String typeOfOS) {
            this.typeOfOS = typeOfOS;
            return this;
        }

        public VMBuilder identifierOfOS(String identifierOfOS) {
            this.identifierOfOS = identifierOfOS;
            return this;
        }

        public VirtualMachine build() {
            return new VirtualMachine.Builder(id, name, hostMachine)
                    .countOfCPU(countOfCPU).countOfMonitors(countOfMonitors)
                    .cpuExecutionCap(cpuExecutionCap)
                    .hardDiskFreeSpaceSize(hardDiskFreeSpaceSize)
                    .hardDiskTotalSize(hardDiskTotalSize)
                    .sizeOfRAM(sizeOfRAM)
                    .sizeOfVRAM(sizeOfVRAM)
                    .typeOfOS(typeOfOS)
                    .identifierOfOS(identifierOfOS)
                    .build();
        }
    }
    
    private void assertDeepPRsEquals(List<PortRule> expPRs, List<PortRule> actPRs) {
        for (int i = 0; i < expPRs.size(); ++i) {
            PortRule expPR = expPRs.get(i);
            PortRule actPR = actPRs.get(i);
            assertDeepPRsEquals(expPR, actPR);
        }
    }

    private void assertDeepPRsEquals(PortRule expPR, PortRule actPR) {        
        assertEquals("Port rules should have same name", expPR.getName(), actPR.getName());
        assertEquals("Port rules should have same protocol", expPR.getProtocol(), actPR.getProtocol());
        assertEquals("Port rules should have same host IP address", expPR.getHostIP(), actPR.getHostIP());
        assertEquals("Port rules should have same host port number", expPR.getHostPort(), actPR.getHostPort());
        assertEquals("Port rules should have same guest IP address", expPR.getGuestIP(), actPR.getGuestIP());
        assertEquals("Port rules should have same guest port number", expPR.getGuestPort(), actPR.getGuestPort());        
    }
    
    private static Comparator<PortRule> prComparator = new Comparator<PortRule>() {

        @Override
        public int compare(PortRule o1, PortRule o2) {
            
            return o1.getName().compareTo(o2.getName());
            
        }
    };
}
