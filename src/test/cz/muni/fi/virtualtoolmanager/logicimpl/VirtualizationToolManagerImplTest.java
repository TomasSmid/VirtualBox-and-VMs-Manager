/*
 * Copyright 2015 Tom8š Šmíd.
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
import cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.ConnectionFailureException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnexpectedVMStateException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import cz.muni.fi.virtualtoolmanager.pubapi.types.CloneType;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
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
 * This test class ensure unit testing of class VirtualizationToolManagerImpl
 * and is intended to be a pointer that class VirtualizationToolManagerImpl
 * works as expected.
 *
 * @author Tomáš Šmíd
 */
public class VirtualizationToolManagerImplTest {

    private VirtualizationToolManagerImpl sut;
    private NativeVBoxAPIManager natAPIManMocked;
    private PhysicalMachine hostMachine;
    private ConnectionManagerImpl conManMocked;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setUp() {
        hostMachine = new PMBuilder().build();
        natAPIManMocked = mock(NativeVBoxAPIManager.class);
        conManMocked = mock(ConnectionManagerImpl.class);
        sut = new VirtualizationToolManagerImpl(hostMachine, natAPIManMocked, conManMocked);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanUp() {
        System.setOut(null);
        System.setErr(null);
    }

    /**
     * This test tests that if the registration of a virtual machine is
     * successful then there should be written an information about that on
     * standard output, but standard error output should stay empty, because no
     * exception nor error should appear.
     */
    @Test
    public void registerVirtualMachineIdealCase() {
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there should be returned a positive answer when the method NativeVBoxAPIManager::registerVirtualMachine()
        //is called which means the operation was performed successfully
        when(natAPIManMocked.registerVirtualMachine(hostMachine, vmName)).thenReturn(true);
        //there should be returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with required physical machine which means that the physical machine is connected and can be worked with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);

        sut.registerVirtualMachine(vmName);

        assertFalse("There should be written a message on a standard output informing about"
                + "a successful operation", outContent.toString().isEmpty());
        assertTrue("There should not be written any message on a standard error output",
                errContent.toString().isEmpty());

    }

    /**
     * This test tests that there cannot be registered a virtual machine with a
     * null name.
     */
    @Test
    public void registerVirtualMachineWithNullName() {
        //there should be returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with required physical machine which means that the physical machine is connected and can be worked with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);

        //there is made an attempt to register a virtual machine with a null name
        sut.registerVirtualMachine(null);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt"
                + "to register a virtual machine with a null name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::registerVirtualMachine() was not called as expected
        verify(natAPIManMocked, never()).registerVirtualMachine(hostMachine, anyString());
    }

    /**
     * This test tests that there cannot be registered a virtual machine with an
     * empty name.
     */
    @Test
    public void registerVirtualMachineWithEmptyName() {
        //there should be returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with required physical machine which means that the physical machine is connected and can be worked with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);

        //there is made an attempt to register a virtual machine with an empty name
        sut.registerVirtualMachine("");

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt"
                + "to register a virtual machine with an empty name", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::registerVirtualMachine() was not called as expected
        verify(natAPIManMocked, never()).registerVirtualMachine(hostMachine, anyString());
    }

    /**
     * This test tests that there cannot be registered any virtual machine on a
     * physical machine which is not connected.
     */
    @Test
    public void registerVirtualMachineOnDisconnectedPhysicalMachine() {
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there is returned a negative answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is not connected and that's why the virtual
        //machine cannot be registered (in fact, the negative answer would firsly return
        //ConnectedPhysicalMachine::isConnected())
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        sut.registerVirtualMachine(vmName);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt"
                + "to register a virtual machine on physical machine which is not available at the moment",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::registerVirtualMachine() was not called as expected
        verify(natAPIManMocked, never()).registerVirtualMachine(hostMachine, anyString());
    }

    /**
     * This test tests that there cannot be registered a virtual machine if
     * there appears any connection problem when there is being performed a
     * physical machine connection check (outer check).
     */
    @Test
    public void registerVirtualMachineWithSuddenNetworkConnectionLossOuterCheck() {
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there is returned a negative answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is not connected and that's why the virtual
        //machine cannot be registered (in fact, there should be thrown ConnectionFailureException when
        //ConnectionManagerImpl::connect() is called)
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        sut.registerVirtualMachine(vmName);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt"
                + "to register a virtual machine on physical machine which is not available at the moment",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::registerVirtualMachine() was not called as expected
        verify(natAPIManMocked, never()).registerVirtualMachine(hostMachine, anyString());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that there cannot be registered a virtual machine if
     * there appears any connection problem while the registration is being
     * processed (inner check).
     */
    @Test
    public void registerVirtualMachineWithSuddenNetworkConnectionLossInnerCheck() {
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there is returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is connected and therefore there can be done
        //any work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::registerVirtualMachine() is called with a required arguments
        doThrow(ConnectionFailureException.class).when(natAPIManMocked).registerVirtualMachine(hostMachine, vmName);

        sut.registerVirtualMachine(vmName);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there appeared any"
                + "connection problem while registration was being processed", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that there is not possible to register one virtual
     * machine more than once (if virtual machine is still in the list of
     * registered virtual machines) on a particular physical machine.
     */
    @Test
    public void registerAlreadyRegisteredVirtualMachine() {
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there is returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is connected and therefore there can be done
        //any work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //this step simulates the second call of the method VirtualizationToolManager::registerVirtualMachine()
        //with the same virtual machine which is still registered after first call
        when(natAPIManMocked.registerVirtualMachine(hostMachine, vmName)).thenReturn(false);

        sut.registerVirtualMachine(vmName);

        assertFalse("There should be written a message on a standard output that the virtual machine"
                + "cannot be registered, because it is already registered", outContent.toString().isEmpty());
        assertTrue("There should not be written a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there cannot be registered a virtual machine which
     * is not present in a default folder for virtual machines of VirtualBox.
     */
    @Test
    public void registerNonexistentVirtualMachine() {
        //represents name of a virtual machine which should be newly registered
        String vmName = "VirtualMachine_01";

        //there is returned a positive answer when the method ConnectionManagerImpl::isConnected() is called
        //with a required host machine which means the host machine is connected and therefore there can be done
        //any work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::registerVirtualMachine() is called with a required arguments
        doThrow(UnknownVirtualMachineException.class).when(natAPIManMocked).registerVirtualMachine(hostMachine, vmName);

        sut.registerVirtualMachine(vmName);

        assertTrue("There should not be written a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be written a message on a standard error output that there was made an attempt"
                + "to register a virtual machine which is not located in a default folder for virtual machines"
                + "of VirtualBox", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if there are all neccessary conditions met and
     * required virtual machine exists on the particular physical machine, then
     * there is returned the required VirtualMachine object and no further steps
     * are performed.
     */
    @Test
    public void findVirtualMachineByIdWithSomeMatch() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be returned expVM object when the method NativeVBoxAPIManager::getVirtualMachine() is called
        //with an ID of a reguired virtual machine (all conditions (checks) are met at this point and virtual machine
        //can be returned)
        when(natAPIManMocked.getVirtualMachine(hostMachine, expVM.getId().toString())).thenReturn(expVM);

        VirtualMachine actVM = sut.findVirtualMachineById(expVM.getId());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        //checks the returned virtual machine is realy the same as the original
        assertDeepVMsEquals(expVM, actVM);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine when there does not exist any virtual machine with a
     * required ID on a particular physical machine and that this result is
     * supported with an informing message on a standard output.
     */
    @Test
    public void findVirtualMachineByIdWithNoMatch() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be returned a null object when the method NativeVBoxAPIManager::getVirtualMachine()
        //is called with an ID of a reguired virtual machine
        when(natAPIManMocked.getVirtualMachine(hostMachine, expVM.getId().toString())).thenReturn(null);

        VirtualMachine actVM = sut.findVirtualMachineById(expVM.getId());

        assertFalse("There should be a message on a standard output that there does not exist any virtual"
                + "machine with a required ID", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
    }

    /**
     * This test tests that if the passed ID argument is null then there is
     * returned a null VirtualMachine object and on a standard error output
     * should appear an informing error message.
     */
    @Test
    public void findVirtualMachineByIdWithNullUUID() {

        VirtualMachine actVM = sut.findVirtualMachineById(null);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there was made an attempt"
                + "to get a virtual machine with a null ID", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
        //checks the method ConnectionManagerImpl::isConnected() has never been called as expected
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that if the passed ID argument is empty then there is
     * returned a null VirtualMachine object and on a standard error output
     * should appear an informing error message.
     */
    @Test
    public void findVirtualMachineByIdWithEmptyUUID() {

        VirtualMachine actVM = sut.findVirtualMachineById(UUID.fromString(""));

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there was made an attempt"
                + "to get a virtual machine with an empty ID", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
        //checks the method ConnectionManagerImpl::isConnected() has never been called as expected
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine and on the standard error output should appear an
     * informing error message when there is made an attempt to get a virtual
     * machine from a disconnected physical machine.
     */
    @Test
    public void findVirtualMachineByIdOnDisconnectedPhysicalMachine() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the host machine is not connected and so there cannot be done any work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        VirtualMachine actVM = sut.findVirtualMachineById(expVM.getId());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the physical machine"
                + "is not connected", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine and on the standard error output should appear an
     * informing error message when there is made an attempt to get a virtual
     * machine if there appears any connection problem during connection test
     * (before NativeVBoxManager::getVirtualMachine() method call - outer
     * check).
     */
    @Test
    public void findVirtualMachineByIdWithSuddenNetworkConnectionLossOuterCheck() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the host machine was connected, but it is not now, because there did not 
        //manage to connect to it again (probably any network connection problem or server problem)
        // = there cannot be done any work with virtual machines on this physical machine at this moment
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        VirtualMachine actVM = sut.findVirtualMachineById(expVM.getId());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine and on the standard error output should appear an
     * informing error message when there is made an attempt to get a virtual
     * machine if there appears any connection problem when the get operation
     * itself is being processed (after connection test - after
     * NativeVBoxManager::getVirtualMachine() method has been called - inner
     * check).
     */
    @Test
    public void findVirtualMachineByIdWithSuddenNetworkConnectionLossInnerCheck() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::getVirtualMachine() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(ConnectionFailureException.class).when(natAPIManMocked).getVirtualMachine(hostMachine, expVM.getId().toString());

        VirtualMachine actVM = sut.findVirtualMachineById(expVM.getId());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that if there are all neccessary conditions met and
     * required virtual machine exists on the particular physical machine then
     * there is returned the required VirtualMachine object and no further steps
     * are performed.
     */
    @Test
    public void findVirtualMachineByNameWithSomeMatch() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be returned expVM object when the method NativeVBoxAPIManager::getVirtualMachine() is called
        //with a name of a reguired virtual machine (all conditions (checks) are met at this point and virtual machine
        //can be returned)
        when(natAPIManMocked.getVirtualMachine(hostMachine, expVM.getName())).thenReturn(expVM);

        VirtualMachine actVM = sut.findVirtualMachineByName(expVM.getName());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        //checks the returned virtual machine is realy the same as the original
        assertDeepVMsEquals(expVM, actVM);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine when there does not exist any virtual machine with a
     * required name on a particular physical machine and that this result is
     * supported with an informing message on a standard output.
     */
    @Test
    public void findVirtualMachineByNameWithNoMatch() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be returned a null object when the method NativeVBoxAPIManager::getVirtualMachine()
        //is called with a name of a reguired virtual machine
        when(natAPIManMocked.getVirtualMachine(hostMachine, expVM.getName())).thenReturn(null);

        VirtualMachine actVM = sut.findVirtualMachineByName(expVM.getName());

        assertFalse("There should be a message on a standard output that there does not exist any virtual"
                + "machine with a required name", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
    }

    /**
     * This test tests that if the passed string argument (name) is null then
     * there is returned a null VirtualMachine object and on a standard error
     * output should appear an informing error message.
     */
    @Test
    public void findVirtualMachineByNameWithNullName() {

        VirtualMachine actVM = sut.findVirtualMachineByName(null);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there was made an attempt"
                + "to get a virtual machine with a null name", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
        //checks the method ConnectionManagerImpl::isConnected() has never been called as expected
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that if the passed string argument (name) is empty then
     * there is returned a null VirtualMachine object and on a standard error
     * output should appear an informing error message.
     */
    @Test
    public void findVirtualMachineByNameWithEmptyName() {

        VirtualMachine actVM = sut.findVirtualMachineByName("");

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there was made an attempt"
                + "to get a virtual machine with an empty name", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
        //checks the method ConnectionManagerImpl::isConnected() has never been called as expected
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine and on the standard error output should appear an
     * informing error message when there is made an attempt to get a virtual
     * machine from a disconnected physical machine.
     */
    @Test
    public void findVirtualMachineByNameOnDisconnectedPhysicalMachine() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the host machine is not connected and so there cannot be done any work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        VirtualMachine actVM = sut.findVirtualMachineByName(expVM.getName());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the physical machine"
                + "is not connected", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine and on the standard error output should appear an
     * informing error message when there is made an attempt to get a virtual
     * machine if there appears any connection problem during connection test
     * (before NativeVBoxManager::getVirtualMachine() method call - outer
     * check).
     */
    @Test
    public void findVirtualMachineByNameWithSuddenNetworkConnectionLossOuterCheck() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

       //means that the host machine was connected, but it is not now, because there did not 
        //manage to connect to it again (probably any network connection problem or server problem)
        // = there cannot be done any work with virtual machines on this physical machine at this moment
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        VirtualMachine actVM = sut.findVirtualMachineByName(expVM.getName());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method NativeVBoxAPIManager::getVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).getVirtualMachine(hostMachine, anyString());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that there is returned a null object of type
     * VirtualMachine and on the standard error output should appear an
     * informing error message when there is made an attempt to get a virtual
     * machine if there appears any connection problem when the get operation
     * itself is being processed (after connection test - after
     * NativeVBoxManager::getVirtualMachine() method has been called - inner
     * check).
     */
    @Test
    public void findVirtualMachineByNameWithSuddenNetworkConnectionLossInnerCheck() {
        //represents a virtual machine which is required and should be returned as a result of get operation
        VirtualMachine expVM = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::getVirtualMachine() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(ConnectionFailureException.class).when(natAPIManMocked).getVirtualMachine(hostMachine, expVM.getName());

        VirtualMachine actVM = sut.findVirtualMachineByName(expVM.getName());

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertNull("The returned virtual machine object should be null", actVM);
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that if there exist some virtual machines on a particular
     * physical machine, then the list of this virtual machines is returned as a
     * result of get operation and no message should be written to any output.
     */
    @Test
    public void getVirtualMachinesWithReturnedNonemptyVMsList() {
        //represents first of two registered virtual machines from a particular physical machine
        VirtualMachine vm1 = new VMBuilder().build();
        //represents a second of two registered virtual machines from a particular physical machine
        VirtualMachine vm2 = new VMBuilder().name("VirtualMachine_02")
                .id(UUID.fromString("793d084a-cb01-4a55-a9b7-531c4555aaaa"))
                .build();
        List<VirtualMachine> expVMs = Arrays.asList(vm1, vm2);

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be returned the list of registered virtual machines when the method
        //NativeVBoxAPIManager::getAllVirtualMachines() is called
        when(natAPIManMocked.getAllVirtualMachines(hostMachine)).thenReturn(expVMs);

        List<VirtualMachine> actVMs = sut.getVirtualMachines();

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertDeepVMsEquals(expVMs, actVMs);
    }

    /**
     * This test tests that there is returned an empty list of virtual machines
     * without any informing message on any output when the method
     * VirtualizationToolManagerImpl::getVirtualMachines() is called.
     */
    @Test
    public void getVirtualMachinesWithReturnedEmptyVMsList() {
        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be returned an empty list of registered virtual machines when the method
        //NativeVBoxAPIManager::getAllVirtualMachines() is called
        when(natAPIManMocked.getAllVirtualMachines(hostMachine)).thenReturn(new ArrayList<>());

        List<VirtualMachine> actVMs = sut.getVirtualMachines();

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertTrue("The list of virtual machines should be empty", actVMs.isEmpty());
    }

    /**
     * This test tests that there is returned an empty list of virtual machines
     * when the method is called in a moment when the physical machine is not
     * connected and an informing error message appears on standard error
     * output.
     */
    @Test
    public void getVirtualMachinesFromDisconnectedPhysicalMachine() {
        //means that the host machine is not connected and so there cannot be done any work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        List<VirtualMachine> actVMs = sut.getVirtualMachines();

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there was made an attempt"
                + "to get all virtual machines from a disconnected physical machine", errContent.toString().isEmpty());
        assertTrue("The list of virtual machines should be empty", actVMs.isEmpty());
        //checks the method NativeVBoxAPIManager::getAllVirtualMachines() has never been called as expected
        verify(natAPIManMocked, never()).getAllVirtualMachines(hostMachine);
    }

    /**
     * This test tests that there is returned an empty list of virtual machines
     * and on the standard error output should appear an informing error message
     * when there is made an attempt to get all virtual machines if there
     * appears any connection problem during connection test (before
     * NativeVBoxAPIManager::getAllVirtualMachines() method call - outer check).
     */
    @Test
    public void getVirtualMachinesWithSuddenNetworkConnectionLossOuterCheck() {
       //means that the host machine was connected, but it is not now, because there did not 
        //manage to connect to it again (probably any network connection problem or server problem)
        // = there cannot be done any work with virtual machines on this physical machine at this moment
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        List<VirtualMachine> actVMs = sut.getVirtualMachines();

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertTrue("The list of virtual machines should be empty", actVMs.isEmpty());
        //checks the method NativeVBoxAPIManager::getAllVirtualMachines() has never been called as expected
        verify(natAPIManMocked, never()).getAllVirtualMachines(hostMachine);
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that there is returned an empty list of virtual machines
     * and on the standard error output should appear an informing error message
     * when there is made an attempt to get all virtual machines if there
     * appears any connection problem when the get operation itself is being
     * processed (after connection test - after
     * NativeVBoxAPIManager::getAllVirtualMachines() method has been called - inner
     * check).
     */
    @Test
    public void getVirtualMachinesWithSuddenNetworkConnectionLossInnerCheck() {
        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::getAllVirtualMachines() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(ConnectionFailureException.class).when(natAPIManMocked).getAllVirtualMachines(hostMachine);

        List<VirtualMachine> actVMs = sut.getVirtualMachines();

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertTrue("The list of virtual machines should be empty", actVMs.isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that the required virtual machine is successfully removed
     * when all important values are valid and that there appears an informing
     * message on a standard output about successful operation.
     */
    @Test
    public void removeValidVirtualMachine() {
        //represents a virtual machine which intended to be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);

        sut.removeVirtualMachine(vm);

        assertFalse("There should be a message on a standard output that the virtual machine was "
                + "successfully removed", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
    }

    /**
     * This test tests that there is written an error message on a standard
     * error output when the method
     * VirtualizationToolManagerImpl::removeVirtualMachine() is called with a
     * null object of type VirtualMachine and the operation is aborted.
     */
    @Test
    public void removeNullVirtualMachine() {

        sut.removeVirtualMachine(null);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there was made an attempt "
                + "to remove a null virtual machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMocked, never()).removeVirtualMachine(any(VirtualMachine.class));
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that the virtual machine deletion operation is aborted
     * when the virtual machine which should be removed has a null object of
     * type PhysicalMachine, because there is no information on which physical
     * machine should be the virtual machine located.
     */
    @Test
    public void removeVirtualMachineWithNullPhysicalMachine() {
        //represents a virtual machine which is intended to be removed
        VirtualMachine vm = new VMBuilder().hostMachine(null).build();

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine "
                + "has (belong to) a null physical machine", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMocked, never()).removeVirtualMachine(any(VirtualMachine.class));
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that the virtual machine deletion operation is aborted if
     * the physical machine of virtual machine is not equal to physical machine
     * of VirtualizationToolManager object from which is the method called and
     * the error message informing about this state is written on a standard
     * error output.
     */
    @Test
    public void removeVirtualMachineWithIncorrectPhysicalMachine() {
        //represents a virtual machine which is intended to be removed
        VirtualMachine vm = new VMBuilder().hostMachine(new PMBuilder().addressIP("11.11.11.0").build()).build();

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine "
                + "has a different physical machine from physical machine of virtualizationToolManager",
                errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMocked, never()).removeVirtualMachine(any(VirtualMachine.class));
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that the virtual machine deletion operation is aborted
     * when the virtual machine which should be removed has a null ID and the
     * error message informing about this problem is written on a standard error
     * output.
     */
    @Test
    public void removeVirtualMachineWithNullId() {
        //represents a virtual machine which is intended to be removed
        VirtualMachine vm = new VMBuilder().id(null).build();

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine "
                + "has a null ID", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMocked, never()).removeVirtualMachine(any(VirtualMachine.class));
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that the virtual machine deletion operation is aborted
     * when the virtual machine which should be removed has an empty ID and the
     * error message informing about this problem is written on a standard error
     * output.
     */
    @Test
    public void removeVirtualMachineWithEmptyId() {
        //represents a virtual machine which is intended to be removed
        VirtualMachine vm = new VMBuilder().id(UUID.fromString("")).build();

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine "
                + "has an empty ID", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMocked, never()).removeVirtualMachine(any(VirtualMachine.class));
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(hostMachine);
    }

    /**
     * This test tests that the virtual machine deletion is not finished
     * successfully if the virtual machine which should be removed does not
     * exist on a particular physical machine.
     */
    @Test
    public void removeNonexistentVirtualMachine() {
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIManager::removeVirtualMachine() is called with the required virtual machine
        //and means the virtual machine cannot be removed, because it does not exist on a particular
        //physical machine
        doThrow(UnknownVirtualMachineException.class).when(natAPIManMocked).removeVirtualMachine(vm);

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine "
                + "cannot be removed, because does not exist", errContent.toString().isEmpty());
    }

    /**
     * This test tests that the virtual machine deletion is not finished
     * successfully if the virtual machine which should be removed is not in a
     * required state for its removing.
     */
    @Test
    public void removeVirtualMachineWithInvalidState() {
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
       //there should be thrown UnexpectedVMStateException exception when the method
        //NativeVBoxAPIManager::removeVirtualMachine() is called with the required virtual machine
        //and means the virtual machine cannot be removed, because it is not in a required state for removing
        doThrow(UnexpectedVMStateException.class).when(natAPIManMocked).removeVirtualMachine(vm);

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine "
                + "cannot be removed, because is not in a required state", errContent.toString().isEmpty());
    }

    /**
     * This test tests that if the physical machine from which should be a
     * virtual machine removed is not connected then the virtual machine cannot
     * be removed.
     */
    @Test
    public void removeVirtualMachineOnDisconnectedPhysicalMachine() {
        //represents a virtual machine which is intended to be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the hostMachine is not connected and so there cannot be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine "
                + "has an empty ID", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called
        verify(natAPIManMocked, never()).removeVirtualMachine(any(VirtualMachine.class));
    }

    /**
     * This test tests that there cannot be removed a virtual machine if there
     * appears any connection problem during connection test (before
     * NativeVBoxAPIManager::remveVirtualMachine() method call - outer check).
     */
    @Test
    public void removeVirtualMachineWithSuddenNetworkConnectionLossOuterCheck() {
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the host machine was connected, but it is not now, because there did not 
        //manage to connect to it again (probably any network connection problem or server problem)
        // = there cannot be done any work with virtual machines on this physical machine at this moment
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::removeVirtualMachine() has never been called as expected
        verify(natAPIManMocked, never()).removeVirtualMachine(vm);
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that there cannot be removed a virtual machine if there
     * appears any connection problem when the removing operation itself is
     * being processed (after connection test - after
     * NativeVBoxAPIManager::removeVirtualMachine() method has been called - inner
     * check).
     */
    @Test
    public void removeVirtualMachineWithSuddenNetworkConnectionLossInnerCheck() {
        //represents a virtual machine which should be removed
        VirtualMachine vm = new VMBuilder().build();

        //means that the hostMachine is connected and so there can be done a work with it
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown a ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::removeVirtualMachine() is called and means there occured any connection problem
        //when there was being worked with VirtualBox on the host machine
        doThrow(ConnectionFailureException.class).when(natAPIManMocked).removeVirtualMachine(vm);

        sut.removeVirtualMachine(vm);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that if there are ale conditions for cloning met then
     * there is returned a virtual machine clone as a result and on a standard
     * output appears informing message that the operation was successful.
     */
    @Test
    public void cloneValidVirtualMachine() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();
        //represents a virtual machine clone which should be returned as a result of cloning operation
        VirtualMachine expVMClone = new VMBuilder().name(origVM + "_FullClone1")
                .id(UUID.fromString("793d084a-0189-4a55-a9c0-531c455aaab"))
                .build();

        //means that the host machine of virtual machine is connected and the cloning operation can start
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be returned expVMClone object when the method NativeVBoxAPIManager::createVirtualMachineClone()
        //is called with a required virtual machine
        when(natAPIManMocked.createVMClone(origVM, CloneType.FULL_FROM_ALL_STATES)).thenReturn(expVMClone);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(expVMClone, CloneType.FULL_FROM_ALL_STATES);

        assertFalse("There should be a message on a standard output that the virtual machine clone"
                + "was created successfully", outContent.toString().isEmpty());
        assertTrue("There should not be a message on a standard error output", errContent.toString().isEmpty());
        assertDeepVMsEquals(expVMClone, actVMClone);
    }

    /**
     * This test tests that if the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called with a
     * null virtual machine argument then the cloning operation itself is not
     * even started and there is returned a null object of type VirtualMachine
     * as a result and on a standard error output appears an error informing
     * message.
     */
    @Test
    public void cloneNullVirtualMachine() {

        VirtualMachine actVMClone = sut.cloneVirtualMachine(null, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should be a message on a standard error output that there was made an attempt "
                + "to clone a null virtual machine", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(any(PhysicalMachine.class));
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called
        verify(natAPIManMocked, never()).createVMClone(any(VirtualMachine.class), any(CloneType.class));
    }

    /**
     * This test tests that the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called with a
     * virtual machine argument which has a null host machine attribute then the
     * cloning operation itself is not even started and there is returned a null
     * object of type VirtualMachine and on a standard error output appears an
     * error informing message.
     */
    @Test
    public void cloneVirtualMachineWithNullPhysicalMachine() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().hostMachine(null).build();

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should be a message on a standard error output that there was made an attempt "
                + "to clone a virtual machine with a null host machine", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(any(PhysicalMachine.class));
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called
        verify(natAPIManMocked, never()).createVMClone(any(VirtualMachine.class), any(CloneType.class));
    }

    /**
     * This test tests that the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called with a
     * virtual machine argument which has an incorrect host machine attribute
     * (which is not equal to host machine attribute of
     * virtualizationToolManager) then the cloning operation itself is not even
     * started and there is returned a null object of type VirtualMachine and on
     * a standard error output appears an error informing message.
     */
    @Test
    public void cloneVirtualMachineWithIncorrectPhysicalMachine() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().hostMachine(new PMBuilder().addressIP("11.11.11.0").build()).build();

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should be a message on a standard error output that there was made an attempt "
                + "to clone a virtual machine with an incorrect host machine", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(any(PhysicalMachine.class));
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called
        verify(natAPIManMocked, never()).createVMClone(any(VirtualMachine.class), any(CloneType.class));
    }

    /**
     * This test tests that the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called with a
     * null clone type argument then the cloning operation itself is not even
     * started and there is returned a null object of type VirtualMachine and on
     * a standard error output appears an error informing message.
     */
    @Test
    public void cloneVirtualMachineWithNullCloneMode() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, null);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should be a message on a standard error output that there was made an attempt "
                + "to clone a virtual machine with a null clone type", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
        //checks the method ConnectionManager::isConnected() has never been called
        verify(conManMocked, never()).isConnected(any(PhysicalMachine.class));
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called
        verify(natAPIManMocked, never()).createVMClone(any(VirtualMachine.class), any(CloneType.class));

    }

    /*@Test
     public void cloneVirtualMachineWithInvalidCloneMode(){
    
     Here has should been written a test code for a situation when the method
     is called with an invalid (different from 4 correct literals) enum value
     which should abort a cloning operation.
    
     }*/
    /**
     * This test tests that if the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called when the
     * host machine is not connected then the cloning operation itself is not
     * even started and there is returned a null object of type VirtualMachine
     * and on a standard error output appears an error informing message.
     */
    @Test
    public void cloneVirtualMachineOnDisconnectedPhysicalMachine() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        //means that the hostMachine is not connected and therefore the virtual machine cannot be cloned
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that the virtual machine"
                + "cannot be cloned, because the host machine is disconnect", errContent.toString().isEmpty());
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone has never been called
        verify(natAPIManMocked, never()).createVMClone(any(VirtualMachine.class), any(CloneType.class));
    }

    /**
     * This test tests that if the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called with
     * virtual machine which does not exist then the cloning operation is
     * aborted and there is returned a null object of type VirtualMachine and on
     * a standard error output appears an error informing message.
     */
    @Test
    public void cloneNonexistentVirtualMachine() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        //means that the host machine is connected and therefore the virtual machine can be cloned
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown UnknownVirtualMachineException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called with a required virtual machine
        //and means the virtual machine does not exist and cannot be cloned
        doThrow(UnknownVirtualMachineException.class).when(natAPIManMocked).createVMClone(origVM, CloneType.FULL_FROM_ALL_STATES);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should be a message on a standard error output that there was made an attempt "
                + "to clone a virtual machine which does not exist", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
    }

    /**
     * This test tests that if the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called with
     * virtual machine which is registered, but for some reason is not
     * accessible then the cloning operation is aborted and there is returned a
     * null object of type VirtualMachine and on a standard error output appears
     * an error informing message.
     */
    @Test
    public void cloneInaccessibleVirtualMachine() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        //means that the host machine is connected and therefore the virtual machine can be cloned
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown UnexpectedVMStateException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called with a required virtual machine
        //and means the virtual machine is not accessible and cannot be cloned
        doThrow(UnexpectedVMStateException.class).when(natAPIManMocked).createVMClone(origVM, CloneType.FULL_FROM_ALL_STATES);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should be a message on a standard error output that there was made an attempt "
                + "to clone a virtual machine which is not accessible", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
    }

    /**
     * This test tests that if the method
     * VirtualizationToolManagerImpl::cloneVirtualMachine() is called with
     * virtual machine which should be cloned, but is not in a required state
     * for cloning then the cloning operation is aborted and there is returned a
     * null object of type VirtualMachine and on a standard error output appears
     * an error informing message.
     */
    @Test
    public void cloneVirtualMachineWithIncorrectState() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        //means that the host machine is connected and therefore the virtual machine can be cloned
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown UnexpectedVMStateException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called with a required virtual machine
        //and means the virtual machine is not in a required state for cloning and cannot be cloned
        doThrow(UnexpectedVMStateException.class).when(natAPIManMocked).createVMClone(origVM, CloneType.FULL_FROM_ALL_STATES);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertTrue("There should be a message on a standard error output that there was made an attempt "
                + "to clone a virtual machine which is not in a required state for cloning",
                errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
    }

    /**
     * This test tests that a virtual machine cannot be cloned if there appears
     * any connection problem during connection test (before
     * NativeVBoxAPIManager::createVirtualMachineClone() method call - outer
     * check) and there is returned a null object of type VirtualMachine and on
     * a standard error output appears an error informing message.
     */
    @Test
    public void cloneVirtualMachineWithSuddenNetworkConnectionLossOuterCheck() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        //means that the host machine was connected, but it is not now, because there did not 
        //manage to connect to it again (probably any network connection problem or server problem)
        // = there cannot be cloned the virtual machine at the moment
        when(conManMocked.isConnected(hostMachine)).thenReturn(false);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
        //checks the method NativeVBoxAPIManager::createVirtualMachineClone() has never been called as expected
        verify(natAPIManMocked, never()).createVMClone(any(VirtualMachine.class), any(CloneType.class));
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
    }

    /**
     * This test tests that a virtual machine cannot be cloned if there appears
     * any connection problem when the cloning operation itself is being
     * processed (after connection test - after
     * NativeVBoxAPIManager::createVirtualMachineClone() method has been called
     * - inner check) and there is returned a null object of type VirtualMachine
     * and on a standard error output appears an error informing message.
     */
    @Test
    public void cloneVirtualMachineWithSuddenNetworkConnectionLossInnerCheck() {
        //represents a virtual machine which should be cloned
        VirtualMachine origVM = new VMBuilder().build();

        //means that the hostMachine is connected and therefore the virtual machine can be cloned
        when(conManMocked.isConnected(hostMachine)).thenReturn(true);
        //there should be thrown ConnectionFailureException exception when the method
        //NativeVBoxAPIManager::createVirtualMachineClone() is called and means there
        //occured any connection problem when there was being worked with VirtualBox on the host machine
        doThrow(ConnectionFailureException.class).when(natAPIManMocked).removeVirtualMachine(origVM);

        VirtualMachine actVMClone = sut.cloneVirtualMachine(origVM, CloneType.FULL_FROM_ALL_STATES);

        assertTrue("There should not be a message on a standard output", outContent.toString().isEmpty());
        assertFalse("There should be a message on a standard error output that there occured any"
                + "connection problem", errContent.toString().isEmpty());
        assertNull("Returned virtual machine clone should be null", actVMClone);
        //checks the method ConnectionManager::disconnectFrom() has been called as expected
        verify(conManMocked).disconnectFrom(hostMachine);
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

    private void assertDeepVMsEquals(List<VirtualMachine> expVMs, List<VirtualMachine> actVMs) {
        for (int i = 0; i < expVMs.size(); ++i) {
            VirtualMachine expVM = expVMs.get(i);
            VirtualMachine actVM = actVMs.get(i);
            assertDeepVMsEquals(expVM, actVM);
        }
    }

    private void assertDeepVMsEquals(VirtualMachine expVM, VirtualMachine actVM) {
        assertEquals("VMs should have same id", expVM.getId(), actVM.getId());
        assertEquals("VMs should have same name", expVM.getName(), actVM.getName());
        assertEquals("VMs should have same host machine", expVM.getHostMachine(), actVM.getHostMachine());
        assertEquals("VMs should have same count of CPUs", expVM.getCountOfCPU(), actVM.getCountOfCPU());
        assertEquals("VMs should have same count of monitors", expVM.getCountOfMonitors(), actVM.getCountOfMonitors());
        assertEquals("VMs should have same CPUExecutionCap", expVM.getCPUExecutionCap(), actVM.getCPUExecutionCap());
        assertEquals("VMs should have same HDD free space size", expVM.getHardDiskFreeSpaceSize(), actVM.getHardDiskFreeSpaceSize());
        assertEquals("VMs should have same HDD total size", expVM.getHardDiskTotalSize(), actVM.getHardDiskTotalSize());
        assertEquals("VMs should have same RAM size", expVM.getSizeOfRAM(), actVM.getSizeOfRAM());
        assertEquals("VMs should have same video RAM size", expVM.getSizeOfVRAM(), actVM.getSizeOfVRAM());
        assertEquals("VMs should have same type of OS", expVM.getTypeOfOS(), actVM.getTypeOfOS());
        assertEquals("VMs should have same version of OS", expVM.getIdentifierOfOS(), actVM.getIdentifierOfOS());
    }

    private static Comparator<VirtualMachine> vmComparator = new Comparator<VirtualMachine>() {

        @Override
        public int compare(VirtualMachine o1, VirtualMachine o2) {
            int res = o1.getId().compareTo(o2.getId());

            if (res == 0) {
                return (o1.getName().compareTo(o2.getName()));
            }

            return res;
        }
    };
}
