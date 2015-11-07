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
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownPortRuleException;
import cz.muni.fi.virtualtoolmanager.pubapi.exceptions.UnknownVirtualMachineException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.doThrow;
import org.powermock.api.mockito.PowerMockito;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.virtualbox_4_3.IConsole;
import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.INATEngine;
import org.virtualbox_4_3.INetworkAdapter;
import org.virtualbox_4_3.IProgress;
import org.virtualbox_4_3.ISession;
import org.virtualbox_4_3.IVirtualBox;
import org.virtualbox_4_3.IVirtualBoxErrorInfo;
import org.virtualbox_4_3.MachineState;
import org.virtualbox_4_3.NetworkAttachmentType;
import org.virtualbox_4_3.SessionState;
import org.virtualbox_4_3.VBoxException;
import org.virtualbox_4_3.VirtualBoxManager;

/**
 * This test class ensures unit testing of class NativeVBoxAPIMachine and
 * is intended to be a pointer that class NativeVBoxAPIMachine works as expected.
 * 
 * @author Tomáš Šmíd
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeVBoxAPIMachine.class, VirtualBoxManager.class})
public class NativeVBoxAPIMachineTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private NativeVBoxAPIMachine sut;
    private VirtualBoxManager vbmMock;
    private IVirtualBox vboxMock;
    private IMachine vboxMachineMock;
    
    @Before
    public void setUp() {
        PowerMockito.mockStatic(VirtualBoxManager.class);
        vbmMock = mock(VirtualBoxManager.class);
        vboxMock = mock(IVirtualBox.class);
        vboxMachineMock = mock(IMachine.class);
        when(VirtualBoxManager.createInstance(null)).thenReturn(vbmMock);
        sut = new NativeVBoxAPIMachine();        
    }

    /**
     * This test tests that the virtual machine start up process is performed
     * without any exception or error occurance when all neccessary conditions
     * are met.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startVMIdealCase() throws Exception {
       //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       //represents mock object of type ISession for easier and better test control
       ISession sessionMock = mock(ISession.class);
       //represents mock object of type IProgress for easier and better test control
       IProgress progressMock = mock(IProgress.class);
       
       //there should be returned a mock object of type IVirtualBox when the method
       //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
       when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned a mock object of type IMachine when the method
       //IVirtualBox::findMachine() is called with an ID of a required virtual machine
       //in order to control returned values of its methods
       when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
       //there should be returned a positive answer to query for virtual machine accessibility which
       //means that there can be performed some operations with the virtual machine
       when(vboxMachineMock.getAccessible()).thenReturn(true);
       //there should be returned a state "PoweredOff" when the method IMachine::getState() is called
       //for first time (when the machine is checked if it is in a valid state for its start up) and
       //when is called for a second time then is returned state "Running" which signalizes that the virtual
       //machine was successfully started
       when(vboxMachineMock.getState()).thenReturn(MachineState.PoweredOff, MachineState.Running);
       //there should be returned a mock object of type ISession when the method
       //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
       when(vbmMock.getSessionObject()).thenReturn(sessionMock);
       //there should be returned a mock object of type IProgress when the method
       //IMachine::launchVMProcess() is called in order to control returned values of its methods
       when(vboxMachineMock.launchVMProcess(sessionMock, "gui", "")).thenReturn(progressMock);
       //returned negative answers say the virtual machine is still being started and the positive answer
       //says the virtual machine start up was finished successfully and virtual machine is running now
       when(progressMock.getCompleted()).thenReturn(false, false, true);
       //value = 0 says the starting operation has been finished successfully
       when(progressMock.getResultCode()).thenReturn(0);
       
       //there should not appear any exception nor error
       sut.startVM(vm);       
    }
    
    /**
     * This test tests that if the VM starting operation is not finished successfully
     * then the virtual machine is not started and UnexpectedVMStateException
     * exception is invoked.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startVMUnsuccessfulOperation() throws Exception {
       //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       //represents mock object of type ISession for easier and better test control
       ISession sessionMock = mock(ISession.class);
       //represents mock object of type IProgress for easier and better test control
       IProgress progressMock = mock(IProgress.class);
       
       //there should be returned a mock object of type IVirtualBox when the method
       //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
       when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned a mock object of type IMachine when the method
       //IVirtualBox::findMachine() is called with an ID of a required virtual machine
       //in order to control returned values of its methods
       when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
       //there should be returned a positive answer to query for virtual machine accessibility which
       //means that there can be performed some operations with the virtual machine
       when(vboxMachineMock.getAccessible()).thenReturn(true);
       //there should be returned a state "PoweredOff" when the method IMachine::getState() is called
       //for first time (when the machine is checked if it is in a valid state for its start up) and
       //when is called for a second time then is returned state "Running" which signalizes that the virtual
       //machine was successfully started
       when(vboxMachineMock.getState()).thenReturn(MachineState.PoweredOff, MachineState.Running);
       //there should be returned a mock object of type ISession when the method
       //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
       when(vbmMock.getSessionObject()).thenReturn(sessionMock);
       //there should be returned a mock object of type IProgress when the method
       //IMachine::launchVMProcess() is called in order to control returned values of its methods
       when(vboxMachineMock.launchVMProcess(sessionMock, "gui", "")).thenReturn(progressMock);
       //returned negative answers say the virtual machine is still being started and the positive answer
       //says the virtual machine start up was finished successfully and virtual machine is running now
       when(progressMock.getCompleted()).thenReturn(false, false, true);
       //value = -421548 says the starting operation has not been finished successfully
       when(progressMock.getResultCode()).thenReturn(-421548);
       
       exception.expect(UnexpectedVMStateException.class);
       sut.startVM(vm);
    }
    
    /**
     * This test tests that there is thrown the UnknownVirtualMachineException
     * exception when the method NativeVBoxAPIMachine::startVM() is called for
     * a virtual machine which does not exist.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startNonexistentVM() throws Exception {
        //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       
       //there should be returned a mock object of type IVirtualBox when the method
       //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
       when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be thrown VBoxException exception when the method
       //IVirtualBox::findMachine() is called with an ID of a required virtual machine
       //and means the virtual machine does not exist and cannot be started
       doThrow(VBoxException.class).when(vboxMock).findMachine(vm.getId().toString());
       
       exception.expect(UnknownVirtualMachineException.class);
       sut.startVM(vm);
    }
    
    /**
     * This test tests that there is thrown the UnexpectedVMStateException
     * exception when the method NativeVBoxAPIMachine::startVM() is called for
     * a virtual machine which is not accessible.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startInaccessibleVM() throws Exception {
       //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       //mock object of type IVirtualBoxErrorInfo for better test control
       IVirtualBoxErrorInfo vboxErrInfoMock = mock(IVirtualBoxErrorInfo.class);
       
       //there should be returned a mock object of type IVirtualBox when the method
       //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
       when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned a mock object of type IMachine when the method
       //IVirtualBox::findMachine() is called with an ID of a required virtual machine
       //in order to control returned values of its methods
       when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
       //there should be returned a negative answer to query for virtual machine accessibility
       //which means that the virtual machine cannot be started (invalid state)
       when(vboxMachineMock.getAccessible()).thenReturn(false);
       //there should be returned a mock object of type IVirtualBoxErrorInfo in order to
       //control returned values of its methods
       when(vboxMachineMock.getAccessError()).thenReturn(vboxErrInfoMock);
       
       exception.expect(UnexpectedVMStateException.class);
       sut.startVM(vm);
    }
    
    /**
     * This test tests that there is thrown the UnexpectedVMStateException
     * exception when the method NativeVBoxAPIMachine::startVM() is called for
     * a virtual machine which is not in a valid state for the start up
     * operation (invalid state "Running").
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startVMWithInvalidStateRunning() throws Exception {
       //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       
       //there should be returned a mock object of type IVirtualBox when the method
       //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
       when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned a mock object of type IMachine when the method
       //IVirtualBox::findMachine() is called with an ID of a required virtual machine
       //in order to control returned values of its methods
       when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
       //there should be returned a positive answer to query for virtual machine accessibility which
       //means that there can be performed some operations with the virtual machine
       when(vboxMachineMock.getAccessible()).thenReturn(true);
       //there should be returned a state "Running" when the method IMachine::getState() is called
       //(when the machine is checked if it is in a valid state for its start up) which means that
       //the virtual machine is not in a valid state (invalid states for start up operation
       //are "Running" and "Paused")
       when(vboxMachineMock.getState()).thenReturn(MachineState.Running);
       
       exception.expect(UnexpectedVMStateException.class);
       sut.startVM(vm);
    }
    
    /**
     * This test tests that there is thrown the UnexpectedVMStateException
     * exception when the method NativeVBoxAPIMachine::startVM() is called for
     * a virtual machine which is not in a valid state for the start up
     * operation (invalid state "Paused").
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startVMWithInvalidStatePaused() throws Exception {
        //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       
       //there should be returned a mock object of type IVirtualBox when the method
       //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
       when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned a mock object of type IMachine when the method
       //IVirtualBox::findMachine() is called with an ID of a required virtual machine
       //in order to control returned values of its methods
       when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
       //there should be returned a positive answer to query for virtual machine accessibility which
       //means that there can be performed some operations with the virtual machine
       when(vboxMachineMock.getAccessible()).thenReturn(true);
       //there should be returned a state "Paused" when the method IMachine::getState() is called
       //(when the machine is checked if it is in a valid state for its start up) which means that
       //the virtual machine is not in a valid state (invalid states for start up operation
       //are "Running" and "Paused")
       when(vboxMachineMock.getState()).thenReturn(MachineState.Paused);
       
       exception.expect(UnexpectedVMStateException.class);
       sut.startVM(vm);
    }
    
    /**
     * This test tests that there is thrown the UnexpectedVMStateException
     * exception when the method NativeVBoxAPIMachine::startVM() is called for
     * a virtual machine which is already locked by another process.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startAlreadyLockedVM() throws Exception {
       //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       //represents mock object of type ISession for easier and better test control
       ISession sessionMocked = mock(ISession.class);
              
       //there should be returned a mock object of type IVirtualBox when the method
       //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
       when(vbmMock.getVBox()).thenReturn(vboxMock);
       //there should be returned a mock object of type IMachine when the method
       //IVirtualBox::findMachine() is called with an ID of a required virtual machine
       //in order to control returned values of its methods
       when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
       //there should be returned a positive answer to query for virtual machine accessibility which
       //means that there can be performed some operations with the virtual machine
       when(vboxMachineMock.getAccessible()).thenReturn(true);
       //there should be returned a state "PoweredOff" when the method IMachine::getState() is called
       //(when the machine is checked if it is in a valid state for its start up) which means the
       //virtual machine is in a valid state and can be started
       when(vboxMachineMock.getState()).thenReturn(MachineState.PoweredOff);
       //there should be returned a mock object of type ISession when the method
       //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
       when(vbmMock.getSessionObject()).thenReturn(sessionMocked);
       //there should be thrown a VBoxException exception when the method
       //IMachine::launchVMProcess() is called and means that the virtual machine is already
       //locked by another process which is working with the virtual machine now
       doThrow(VBoxException.class).when(vboxMachineMock).launchVMProcess(sessionMocked, "gui", "");
       
       exception.expect(UnexpectedVMStateException.class);
       sut.startVM(vm);
    }
    
    /**
     * This test tests that there is thrown the ConnectionFailureException
     * exception when there appears any connection problem while the start up
     * operation is beginning.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void startVMWithSuddenNetworkConnectionLoss() throws Exception {
       //represents a virtual machine which should be started
       VirtualMachine vm = new VMBuilder().build();
       String url = "http://" + vm.getHostMachine().getAddressIP() + ":"
                  + vm.getHostMachine().getPortOfVTWebServer();
       String username = vm.getHostMachine().getUsername();
       String userPassword = vm.getHostMachine().getUserPassword();
       
       //there should be thrown a VBoxException exception when the method VirtualBoxManager::connect()
       //is called with a required host machine and means that there occured any connection problem
       doThrow(VBoxException.class).when(vbmMock).connect(url, username, userPassword);
       
       exception.expect(ConnectionFailureException.class);
       sut.startVM(vm);
    }
    
    /**
     * This test tests that the virtual machine shut down process is performed
     * without any exception or error occurance when all neccessary conditions
     * are met.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownVMIdealCase() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a mock object of type ISession for easier and better test control
        ISession sessionMock = mock(ISession.class);
        //represents a mock object of type IProgress for easier and better test control
        IProgress progressMock = mock(IProgress.class);
        //represents a mock object of type IConsole for easier and better test control
        IConsole consoleMock = mock(IConsole.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a positive answer to query for virtual machine accessibility
        //and means that there can be performed some operations with a virtual machine
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned the state "Running" when the method IMachine::getState()
        //is called for the first time and the state "PoweredOff" when it is called for the
        //second time and it signalizes switching virtual machine state (successful shut down)
        when(vboxMachineMock.getState()).thenReturn(MachineState.Running, MachineState.PoweredOff);
        //there should be returned a mock object of type ISession when the method
        //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
        when(vbmMock.getSessionObject()).thenReturn(sessionMock);
        //there should be returned a mock object of type IConsole when the method
        //ISession::getConsole() is called in order to control returned values of its methods
        when(sessionMock.getConsole()).thenReturn(consoleMock);
        //there should be returned a mock object of type IProgress when the method
        //IConsole::powerDown() is called in order to control returned values of its methods
        when(consoleMock.powerDown()).thenReturn(progressMock);
        //there should be returned a positive answer to query for powering down operation completion
        //and means the virtual machine was successfully powered off
        when(progressMock.getCompleted()).thenReturn(true);
        //returned value = 0 says the VM shutdown operation has been successful
        when(progressMock.getResultCode()).thenReturn(0);
        //there should be returned a state "Unlocked" which means successful finish of work with
        //the virtual machine
        when(sessionMock.getState()).thenReturn(SessionState.Unlocked);
        
        //there should not appear any exception nor error
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that the virtual machine shut down process is performed
     * without any exception or error occurance even the shut down process itself
     * last for a longer time when all neccessary conditions are met.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownVMIdealCaseWithLongShuttingDownProcess() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a mock object of type ISession for easier and better test control
        ISession sessionMocked = mock(ISession.class);
        //represents a mock object of type IProgress for easier and better test control
        IProgress progressMock = mock(IProgress.class);
        //represents a mock object of type IConsole for easier and better test control
        IConsole consoleMocked = mock(IConsole.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a positive answer to query for virtual machine accessibility
        //and means that there can be performed some operations with a virtual machine
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned the state "Running" (also can be "Paused" or "Stuck")
        //when the method IMachine::getState() is called for the first time and the state
        //"PoweredOff" when it is called for the second time and it signalizes switching
        //virtual machine state (successful shut down)
        when(vboxMachineMock.getState()).thenReturn(MachineState.Running, MachineState.PoweredOff);
        //there should be returned a mock object of type ISession when the method
        //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
        when(vbmMock.getSessionObject()).thenReturn(sessionMocked);
        //there should be returned a mock object of type IConsole when the method
        //ISession::getConsole() is called in order to control returned values of its methods
        when(sessionMocked.getConsole()).thenReturn(consoleMocked);
        //there should be returned a mock object of type IProgress when the method
        //IConsole::powerDown() is called in order to control returned values of its methods
        when(consoleMocked.powerDown()).thenReturn(progressMock);
        //for first five calls there should be returned a negative answer to query for shut down
        //operation completion and means the virtual machine shut down operation is still
        //being processed, after last 6th call there should be returned a positive answer
        //which signalizes a successful finish of the operation
        when(progressMock.getCompleted()).thenReturn(false, false, false, false, false, true);
        //returned value = 0 says the VM shutdown operation has been successful
        when(progressMock.getResultCode()).thenReturn(0);
        //there should be returned a state "Unlocked" which means successful finish of work with
        //the virtual machine
        when(sessionMocked.getState()).thenReturn(SessionState.Unlocked);
        
        //there should not appear any exception nor error
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that the virtual machine shut down process is performed
     * without any exception or error occurance even the shut down process itself
     * last for a longer time (switching virtual machine to the state "PoweredOff")
     * when all neccessary conditions are met.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownVMIdealCaseWithLongVMSwitchingToStatePoweredOff() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a mock object of type ISession for easier and better test control
        ISession sessionMocked = mock(ISession.class);
        //represents a mock object of type IProgress for easier and better test control
        IProgress progressMock = mock(IProgress.class);
        //represents a mock object of type IConsole for easier and better test control
        IConsole consoleMocked = mock(IConsole.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a positive answer to query for virtual machine accessibility
        //and means that there can be performed some operations with a virtual machine
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned the state "Running" when the method IMachine::getState()
        //is called for the first time, then five times the state "Stopping" and finally the state
        //"PoweredOff" which signalizes successful shut down
        when(vboxMachineMock.getState()).thenReturn(MachineState.Running, MachineState.Stopping,
                                                  MachineState.Stopping,MachineState.Stopping,
                                                  MachineState.Stopping,MachineState.Stopping,
                                                  MachineState.PoweredOff);
        //there should be returned a mock object of type ISession when the method
        //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
        when(vbmMock.getSessionObject()).thenReturn(sessionMocked);
        //there should be returned a mock object of type IConsole when the method
        //ISession::getConsole() is called in order to control returned values of its methods
        when(sessionMocked.getConsole()).thenReturn(consoleMocked);
        //there should be returned a mock object of type IProgress when the method
        //IConsole::powerDown() is called in order to control returned values of its methods
        when(consoleMocked.powerDown()).thenReturn(progressMock);
        //there should be returned a positive answer to query for powering down operation completion
        //and means the virtual machine was successfully powered off
        when(progressMock.getCompleted()).thenReturn(true);
        //returned value = 0 says the VM shutdown operation has been successful
        when(progressMock.getResultCode()).thenReturn(0);
        //there should be returned a state "Unlocked" which means successful finish of work with
        //the virtual machine
        when(sessionMocked.getState()).thenReturn(SessionState.Unlocked);
        
        //there should not appear any exception nor error
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that the virtual machine shut down process is performed
     * without any exception or error occurance even the shut down process itself
     * last for a longer time (unlocking virtual machine by the shut down process)
     * when all neccessary conditions are met.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownVMIdealCaseWithLongUnlockingVM() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a mock object of type ISession for easier and better test control
        ISession sessionMocked = mock(ISession.class);
        //represents a mock object of type IProgress for easier and better test control
        IProgress progressMock = mock(IProgress.class);
        //represents a mock object of type IConsole for easier and better test control
        IConsole consoleMocked = mock(IConsole.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a positive answer to query for virtual machine accessibility
        //and means that there can be performed some operations with a virtual machine
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned the state "Running" when the method IMachine::getState()
        //is called for the first time and the state "PoweredOff" when it is called for the
        //second time and it signalizes switching virtual machine state (successful shut down)
        when(vboxMachineMock.getState()).thenReturn(MachineState.Running, MachineState.PoweredOff);
        //there should be returned a mock object of type ISession when the method
        //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
        when(vbmMock.getSessionObject()).thenReturn(sessionMocked);
        //there should be returned a mock object of type IConsole when the method
        //ISession::getConsole() is called in order to control returned values of its methods
        when(sessionMocked.getConsole()).thenReturn(consoleMocked);
        //there should be returned a mock object of type IProgress when the method
        //IConsole::powerDown() is called in order to control returned values of its methods
        when(consoleMocked.powerDown()).thenReturn(progressMock);
        //there should be returned a positive answer to query for powering down operation completion
        //and means the virtual machine was successfully powered off
        when(progressMock.getCompleted()).thenReturn(true);
        //returned value = 0 says the VM shutdown operation has been successful
        when(progressMock.getResultCode()).thenReturn(0);
        //there should be returned a state "Unlocking" for first five calls and eventually
        //there is returned the state "Unlocked" which means successful finish of work with
        //the virtual machine
        when(sessionMocked.getState()).thenReturn(SessionState.Unlocking,SessionState.Unlocking,
                                                  SessionState.Unlocking,SessionState.Unlocking,
                                                  SessionState.Unlocking,SessionState.Unlocked);
        
        //there should not appear any exception nor error
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that if the VM shutdown operation is not finished successfully
     * then the VM is not shut down and UnexpectedVMStateException exception is
     * invoked.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownVMUnsuccessfulOperation() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a mock object of type ISession for easier and better test control
        ISession sessionMocked = mock(ISession.class);
        //represents a mock object of type IProgress for easier and better test control
        IProgress progressMock = mock(IProgress.class);
        //represents a mock object of type IConsole for easier and better test control
        IConsole consoleMocked = mock(IConsole.class);
        //represents a mock object of type IVirtualBoxErrorInfo for easier and better test control
        IVirtualBoxErrorInfo vboxErrInfoMock = mock(IVirtualBoxErrorInfo.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a positive answer to query for virtual machine accessibility
        //and means that there can be performed some operations with a virtual machine
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned the state "Running" when the method IMachine::getState()
        //is called for the first time and the state "PoweredOff" when it is called for the
        //second time and it signalizes switching virtual machine state (successful shut down)
        when(vboxMachineMock.getState()).thenReturn(MachineState.Running, MachineState.PoweredOff);
        //there should be returned a mock object of type ISession when the method
        //VirtualBoxManager::getSessionObject() is called in order to control returned values of its methods
        when(vbmMock.getSessionObject()).thenReturn(sessionMocked);
        //there should be returned a mock object of type IConsole when the method
        //ISession::getConsole() is called in order to control returned values of its methods
        when(sessionMocked.getConsole()).thenReturn(consoleMocked);
        //there should be returned a mock object of type IProgress when the method
        //IConsole::powerDown() is called in order to control returned values of its methods
        when(consoleMocked.powerDown()).thenReturn(progressMock);
        //there should be returned a positive answer to query for powering down operation completion
        //and means the virtual machine was successfully powered off
        when(progressMock.getCompleted()).thenReturn(true);
        //returned value = -4811 says the VM shutdown operation has not been successful
        when(progressMock.getResultCode()).thenReturn(-4811);
        //there should be returned a mock object of type IVirtualBoxErrorInfo
        when(progressMock.getErrorInfo()).thenReturn(vboxErrInfoMock);
        //non-empty string should be returned
        when(vboxErrInfoMock.getText()).thenReturn("error message");
        //there should be returned a state "Unlocked" which means successful finish of work with
        //the virtual machine
        when(sessionMocked.getState()).thenReturn(SessionState.Unlocked);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::shutDownVM() is
     * called with a virtual machine which does not exist then there is thrown
     * the UnknownVirtualMachineException exception.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownNonexistentVM() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be thrown VBoxException exception when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine and
        //means the virtual machine does not exist
        doThrow(VBoxException.class).when(vboxMock).findMachine(vm.getId().toString());
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::shutDownVM() is
     * called with a virtual machine which is not accessible then there is thrown
     * the UnexpectedVMStateException exception.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownInaccessibleVM() throws Exception{
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //mock object of type IVirtualBoxErrorInfo for better test control
        IVirtualBoxErrorInfo vboxErrInfoMock = mock(IVirtualBoxErrorInfo.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a negative answer to query for virtual machine accessibility
        //and means that the virtual machine cannot be powered off
        when(vboxMachineMock.getAccessible()).thenReturn(false);
        //there should be returned a mock object of type IVirtualBoxErrorInfo in order to
        //control the returned values of its methods
        when(vboxMachineMock.getAccessError()).thenReturn(vboxErrInfoMock);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::shutDownVM() is
     * called with a virtual machine which is not in a required (valid) state for
     * shut down operation then there is thrown the UnexpectedVMStateException exception.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownVMWithInvalidState() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a positive answer to query for virtual machine accessibility
        //and means that there can be performed some operations with a virtual machine
        when(vboxMachineMock.getAccessible()).thenReturn(true);
        //there should be returned the state "PoweredOff" when the method IMachine::getState()
        //is called for the first time (the virtual machine state check if it is valid), but the only
        //valid states for virtual machine shut down are "Running", "Paused", "Stuck", so the virtual
        //machine cannot be powered off
        when(vboxMachineMock.getState()).thenReturn(MachineState.PoweredOff);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that there is thrown the ConnectionFailureException
     * exception when there appears any connection problem while the shut down
     * operation is beginning.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void shutDownVMWithSuddenNetworkConnectionLoss() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        String url = "http://" + vm.getHostMachine().getAddressIP() + ":"
                  + vm.getHostMachine().getPortOfVTWebServer();
        String username = vm.getHostMachine().getUsername();
        String userPassword = vm.getHostMachine().getUserPassword();

        //there should be thrown a VBoxException exception when the method VirtualBoxManager::connect()
        //is called with a required host machine and means that there occured any connection problem
        doThrow(VBoxException.class).when(vbmMock).connect(url, username, userPassword);

        exception.expect(ConnectionFailureException.class);
        sut.shutDownVM(vm);
    }
    
    /**
     * This test tests that if there are all conditions neccassery for port rule
     * addition met then there does not appear any exception nor error and new
     * port rule is successfully created at the particular virtual machine.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void addPortRuleIdealCase() throws Exception {
        //represents virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be created at the virtual machine
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, 1540).build();
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        //represents a mock object of type INATEngine for easier and better test control
        INATEngine natEngineMocked = mock(INATEngine.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "NAT" which is the only valid value which can be used
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.NAT);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(adapterMocked.getNATEngine()).thenReturn(natEngineMocked);
        
        //there should not appear any exception nor error
        sut.addPortRule(vm, portRule);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::addPortRule() is
     * called with a virtual machine which does not exist then there is returned
     * the UnknownVirtualMachineException exception and the port rule is not added
     * to that virtual machine.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void addPortRuleWithNonexistentVM() throws Exception {
        //represents virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be created at the virtual machine
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, 1540).build();
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be thrown a VBoxException exception when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        doThrow(VBoxException.class).when(vboxMock).findMachine(vm.getId().toString());
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.addPortRule(vm, portRule);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::addPortRule() is
     * called with virtual machine which has not the attachment type NAT then
     * there is thrown UnexpectedVMStateException exception and the port rule is
     * not added.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void addPortRuleWithInvalidAttachmentType() throws Exception {
        //represents virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be created at the virtual machine
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, 1540).build();
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "Bridged" which is  not the valid state for port rule addition
        //(the same result would be with states: "Generic", "HostOnly", "Internal", "NATNetwork", "Null")
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.Bridged);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.addPortRule(vm, portRule);
    }
    
    /**
     * This test tests that there is thrown the ConnectionFailureException
     * exception when there appears any connection problem while the port rule
     * addition operation is beginning.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void addPortRuleWithSuddenNetworkConnectionLoss() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a new port rule which should be created at the virtual machine
        PortRule portRule = new PortRule.Builder("PortRule_01", 22, 1540).build();
        String url = "http://" + vm.getHostMachine().getAddressIP() + ":"
                  + vm.getHostMachine().getPortOfVTWebServer();
        String username = vm.getHostMachine().getUsername();
        String userPassword = vm.getHostMachine().getUserPassword();

        //there should be thrown a VBoxException exception when the method VirtualBoxManager::connect()
        //is called with a required host machine and means that there occured any connection problem
        doThrow(VBoxException.class).when(vbmMock).connect(url, username, userPassword);

        exception.expect(ConnectionFailureException.class);
        sut.addPortRule(vm, portRule);
    }
    
    /**
     * This test tests that if there are all neccessary conditions for port rule
     * deletion operation met then the port rule is successfully deleted and no
     * exception nor error appears.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void deletePortRuleIdealCase() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a name of the port rule which should be deleted
        String portRuleName = "PortRule_01";
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        //represents a mock object of type INATEngine for easier and better test control
        INATEngine natEngineMocked = mock(INATEngine.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "NAT" which is the valid state for port rule deletion
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.NAT);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(adapterMocked.getNATEngine()).thenReturn(natEngineMocked);
        
        //there should not appear any exception nor error
        sut.deletePortRule(vm, portRuleName);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::deletePortRule()
     * is called with a virtual machine which does not exist then the port rule
     * is not deleted and there is thrown UnknownVirtualMachineException exception.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void deletePortRuleWithNonexistentVM() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a name of the port rule which should be deleted
        String portRuleName = "PortRule_01";
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned thrown a VBoxException exception when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        doThrow(VBoxException.class).when(vboxMock).findMachine(vm.getId().toString());
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.deletePortRule(vm, portRuleName);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::deletePortRule()
     * is called with a port rule name which does not exist then the port rule
     * is not deleted and there is thrown UnknownPortRuleException exception.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void deleteNonexistentPortRule() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a name of the port rule which should be deleted
        String portRuleName = "PortRule_01";
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        //represents a mock object of type INATEngine for easier and better test control
        INATEngine natEngineMocked = mock(INATEngine.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "NAT" which is the valid state for port rule deletion
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.NAT);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(adapterMocked.getNATEngine()).thenReturn(natEngineMocked);
        //there should be thrown a VBoxException exception when the method INATEngine::removeRedirect()
        //is called and means that that port rule does not exist and cannot be deleted
        doThrow(VBoxException.class).when(natEngineMocked).removeRedirect(portRuleName);
        
        exception.expect(UnknownPortRuleException.class);
        sut.deletePortRule(vm, portRuleName);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::deletePortRule() is
     * called with virtual machine which has not the attachment type NAT then
     * there is thrown UnexpectedVMStateException exception and the port rule is
     * not deleted.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void deletePortRuleWithInvalidAttachmentType() throws Exception {
        //represents virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a name of the port rule which should be deleted
        String portRuleName = "PortRule_01";
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "Bridged" which is  not the valid state for port rule addition
        //(the same result would be with states: "Generic", "HostOnly", "Internal", "NATNetwork", "Null")
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.Bridged);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.deletePortRule(vm, portRuleName);
    }
    
    /**
     * This test tests that there is thrown the ConnectionFailureException
     * exception when there appears any connection problem while the port rule
     * deletion operation is beginning.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void deletePortRuleWithSuddenNetworkConnectionLoss() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a name of the port rule which should be deleted
        String portRuleName = "PortRule_01";
        String url = "http://" + vm.getHostMachine().getAddressIP() + ":"
                  + vm.getHostMachine().getPortOfVTWebServer();
        String username = vm.getHostMachine().getUsername();
        String userPassword = vm.getHostMachine().getUserPassword();

        //there should be thrown a VBoxException exception when the method VirtualBoxManager::connect()
        //is called with a required host machine and means that there occured any connection problem
        doThrow(VBoxException.class).when(vbmMock).connect(url, username, userPassword);

        exception.expect(ConnectionFailureException.class);
        sut.deletePortRule(vm, portRuleName);
    }
    
    /**
     * This test tests that there is returned a non-empty list of port rules
     * when there exist some port rules on the virtual machine which is valid.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getPortRulesWithReturnedNonemptyList() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents the first of two port rules from the virtual machine vm
        String portRule1 = "PortRule_01,TCP,,22,,1540";
        //represents the second of two port rules from the virtual machine vm
        String portRule2 = "PortRule_02,UDP,12.10.11.145,80,154.148.148.1,8080";
        //represents the list of port rules from the virtual machine vm which is expected as a result
        List<String> expList = Arrays.asList(portRule1, portRule2);
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        //represents a mock object of type INATEngine for easier and better test control
        INATEngine natEngineMocked = mock(INATEngine.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "NAT" which is the valid state for port rule deletion
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.NAT);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(adapterMocked.getNATEngine()).thenReturn(natEngineMocked);
        //there should be returned the list of port rules when the method INATEngine::getRedirects() is called
        when(natEngineMocked.getRedirects()).thenReturn(expList);
        
        //there should not be any exception nor error
        List<String> actList = sut.getPortRules(vm);
        
        assertFalse("The returned list of port rules should not be empty", actList.isEmpty());
        assertEquals("Port rules on index 0 should be same", actList.get(0), expList.get(0));
        assertEquals("Port rules on index 1 should be same", actList.get(1), expList.get(1));
    }
    
    /**
     * This test tests that there is returned an empty list of port rules
     * when there does not exist any port rule on the virtual machine.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getPortRulesWithReturnedEmptyList() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        //represents a mock object of type INATEngine for easier and better test control
        INATEngine natEngineMocked = mock(INATEngine.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "NAT" which is the valid state for port rule deletion
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.NAT);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(adapterMocked.getNATEngine()).thenReturn(natEngineMocked);
        //there should be returned the list of port rules when the method INATEngine::getRedirects() is called
        when(natEngineMocked.getRedirects()).thenReturn(new ArrayList<>());
        
        //there should not be any exception nor error
        List<String> actList = sut.getPortRules(vm);
        
        assertTrue("The returned list of port rules should be empty", actList.isEmpty());
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::getPortRules()
     * is called with a virtual machine which does not exist then there should
     * be thrown the UnknownVirtualMachineException exception.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getPortRulesWithNonexistentVM() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be thrown a VBoxException exception when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        doThrow(VBoxException.class).when(vboxMock).findMachine(vm.getId().toString());
        
        exception.expect(UnknownVirtualMachineException.class);
        sut.getPortRules(vm);
    }
    
    /**
     * This test tests that if the method NativeVBoxAPIMachine::getPortRules() is
     * called with virtual machine which has not the attachment type NAT then
     * there is thrown UnexpectedVMStateException exception and the port rules are
     * not retrieved.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getPortRulesWithInvalidAttachmentType() throws Exception {
        //represents virtual machine to which a new port rule should be added
        VirtualMachine vm = new VMBuilder().build();
        //represents a mock object of type INetworkAdapter for easier and better test control
        INetworkAdapter adapterMocked = mock(INetworkAdapter.class);
        
        //there should be returned a mock object of type IVirtualBox when the method
        //VirtualBoxManager::getVBox() is called in order to control returned values of its methods
        when(vbmMock.getVBox()).thenReturn(vboxMock);
        //there should be returned a mock object of type IMachine when the method
        //IVirtualBox::findMachine() is called with an ID of a required virtual machine
        //in order to control returned values of its methods
        when(vboxMock.findMachine(vm.getId().toString())).thenReturn(vboxMachineMock);
        //there should be returned a mock object of type INetworkAdapter when the method
        //IMachine::getNetworkAdapter() is called in order to control returned values of its methods
        when(vboxMachineMock.getNetworkAdapter(0L)).thenReturn(adapterMocked);
        //there is returned the attachment type "Bridged" which is  not the valid state for port rule addition
        //(the same result would be with states: "Generic", "HostOnly", "Internal", "NATNetwork", "Null")
        when(adapterMocked.getAttachmentType()).thenReturn(NetworkAttachmentType.Bridged);
        
        exception.expect(UnexpectedVMStateException.class);
        sut.getPortRules(vm);
    }
    
    /**
     * This test tests that there is thrown the ConnectionFailureException
     * exception when there appears any connection problem while retrive
     * operation is beginning.
     * 
     * @throws java.lang.Exception
     */
    @Test
    public void getPortRulesWithSuddenNetowrkConnectionLoss() throws Exception {
        //represents a virtual machine which should be shut down
        VirtualMachine vm = new VMBuilder().build();
        //represents a name of the port rule which should be deleted
        String portRuleName = "PortRule_01";
        String url = "http://" + vm.getHostMachine().getAddressIP() + ":"
                  + vm.getHostMachine().getPortOfVTWebServer();
        String username = vm.getHostMachine().getUsername();
        String userPassword = vm.getHostMachine().getUserPassword();

        //there should be thrown a VBoxException exception when the method VirtualBoxManager::connect()
        //is called with a required host machine and means that there occured any connection problem
        doThrow(VBoxException.class).when(vbmMock).connect(url, username, userPassword);

        exception.expect(ConnectionFailureException.class);
        sut.getPortRules(vm);
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
    
}
