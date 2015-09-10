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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 *
 * @author Tomáš Šmíd
 */
public class ConnectedPhysicalMachinesTest {
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private ConnectedPhysicalMachines sut;
    private List<PhysicalMachine> physicalMachines;
    
    public ConnectedPhysicalMachinesTest(){
        physicalMachines = new ArrayList<>();
    }
    
    @Before
    public void setUp(){
        sut = ConnectedPhysicalMachines.getInstance();
    }
    
    @After
    public void cleanup(){
        if(physicalMachines != null){
            for(PhysicalMachine pm : physicalMachines){
                sut.remove(pm);
            }
            physicalMachines.clear();
        }
    }
    
    @Test
    public void addPhysicalMachineWithValidArgument(){
        PhysicalMachine pm = new PMBuilder().build();
        physicalMachines.add(pm); //for after test cleanup
        
        assertNotNull("Physical machine " + pm.toString() + " should be correctly instantiated, not null",pm);
        assertFalse("Physical machine " + pm.toString() + " should not be accessed", sut.isConnected(pm));
        
        sut.add(pm);
        assertTrue("Physical machine " + pm.toString() + " should be accessed",sut.isConnected(pm));
    }
    
    @Test
    public void addNullPhysicalMachine(){
        exception.expect(IllegalArgumentException.class);
        sut.add(null);
    }
    
    @Test
    public void removeValidConnectedPhysicalMachine(){
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("140.150.10.10").username("").userPassword("").build();
        physicalMachines.add(pm1);//for after test cleanup
        physicalMachines.add(pm2);//for after test cleanup
        
        assertFalse("Physical machine " + pm1.toString() + " should not be accessed (it has not been added to the list of accessed physical machines yet)", sut.isConnected(pm1));
        assertFalse("Physical machine " + pm2.toString() + " should not be accessed (it has not been added to the list of accessed physical machines yet)", sut.isConnected(pm2));
        
        sut.add(pm1);
        sut.add(pm2);
        
        assertTrue("Physical machine " + pm1.toString() + " should be accessed (it has already been added to the list of accessed physical machines)",sut.isConnected(pm1));
        assertTrue("Physical machine " + pm2.toString() + " should be accessed (it has already been added to the list of accessed physical machines)",sut.isConnected(pm2));
        
        assertTrue("Physical machine " + pm1.toString() + " should be successfully removed, but it is not",sut.remove(pm1));
        
        assertFalse("Physical machine " + pm1.toString() + " should not be accessed, because it has been removed from the list of accessed physical machines", sut.isConnected(pm1));
        assertTrue("Physical machine " + pm2.toString() + " should be accessed",sut.isConnected(pm2));
    }
    
    @Test
    public void removeValidNotConnectedPhysicalMachine(){
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("140.150.10.10").username("").userPassword("").build();
        physicalMachines.add(pm2);//for after test cleanup        
        
        sut.add(pm2);
        
        assertFalse("Physical machine " + pm1.toString() + " should not be possible to remove, "
                  + "because it should not be accessed",sut.remove(pm1));
        
        assertFalse("Physical machine " + pm1.toString() + " should not be accessed", sut.isConnected(pm1));
        assertTrue("Physical machine " + pm2.toString() + " should be accessed",sut.isConnected(pm2));
    }
    
    @Test
    public void removeNullPhysicalMachine(){
        PhysicalMachine pm = new PhysicalMachine("140.150.12.0","10000","Hornd","140nb48");
        physicalMachines.add(pm);//for after test cleanup
        
        sut.add(pm);
        
        exception.expect(IllegalArgumentException.class);
        sut.remove(null);
        
        exception = ExpectedException.none();
        assertTrue("Physical machine " + pm.toString() + " should be accessed",sut.isConnected(pm));
    }
    
    @Test
    public void isMachineConnectedWithValidNotConnectedPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        
        assertNotNull("Physical machine " + pm.toString() + " should be correctly instantiated, not null",pm);
        assertFalse("Physical machine " + pm.toString() + " should not be accessed", sut.isConnected(pm));        
    }
    
    @Test
    public void isMachineConnectedWithValidConnectedPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        physicalMachines.add(pm);//for after test cleanup        
        
        sut.add(pm);
        assertTrue("Physical machine " + pm.toString() + " should be accessed",sut.isConnected(pm));
    }
    
    @Test
    public void isMachineConnectedWithNullPhysicalMachine(){
        PhysicalMachine pm = new PMBuilder().build();
        physicalMachines.add(pm);//for after test cleanup
        
        sut.add(pm);
        
        exception.expect(IllegalArgumentException.class);
        sut.isConnected(null);
        
        exception = ExpectedException.none();
        assertTrue("Physical machine " + pm.toString() + " should be accessed",sut.isConnected(pm));
    }
    
    @Test
    public void getConnectedPhysicalMachinesWithNonemptyPMsListBasic(){
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("10.0.0.10").webserverPort("1154")
                                             .username("").userPassword("").build();        
        
        physicalMachines.add(pm1);//for after test cleanup
        physicalMachines.add(pm2);//for after test cleanup
        
        sut.add(pm1);
        sut.add(pm2);
        
        List<PhysicalMachine> expectedList = Arrays.asList(pm1,pm2);        
        List<PhysicalMachine> actualList = sut.getConnectedPhysicalMachines();
        
        assertListsEquals(expectedList,actualList);
    }
    
    @Test
    public void getConnectedPhysicalMachinesWithNonemptyPMsListAfterPMDeletion(){
        PhysicalMachine pm1 = new PMBuilder().build();
        PhysicalMachine pm2 = new PMBuilder().addressIP("10.0.0.10").webserverPort("1154")
                                             .username("").userPassword("").build();
        
        physicalMachines.add(pm2);//for after test cleanup
        
        sut.add(pm1);
        sut.add(pm2);
        
        sut.remove(pm1);
        
        List<PhysicalMachine> expectedList = Arrays.asList(pm2);
        List<PhysicalMachine> actualList = sut.getConnectedPhysicalMachines();
        
        assertListsEquals(expectedList,actualList);        
    }
    
    @Test
    public void getConnectedPhysicalMahcinesWithEmptyPMsList(){
        assertTrue("List of accessed physical machines should be empty",
                   sut.getConnectedPhysicalMachines().isEmpty());
    }
    
    @Test
    public void getConnectedPhysicalMachinesWithEmptyPMsListAfterPMDeletion(){
        PhysicalMachine pm = new PMBuilder().build();
        
        sut.add(pm);        
        assertFalse("List of accessed physical machines should not be empty",
                    sut.getConnectedPhysicalMachines().isEmpty());
        
        sut.remove(pm);
        assertTrue("List of accessed physical machines should be empty after deletion of last machine in list",
                    sut.getConnectedPhysicalMachines().isEmpty());
    }
    
    @Test
    public void getConnectedPhysicalMachinesWithEmptyPMsListAfterNullPMAdditionToEmptyList(){
        exception.expect(IllegalArgumentException.class);
        sut.add(null);
        
        assertTrue("List of accessed physical machines should be still empty after null physical machine addition",
                    sut.getConnectedPhysicalMachines().isEmpty());
    }
    
    @Test
    public void getConnectedPhysicalMachinesWithEmptyPMsListAfterNullPMAdditionToNonemptyList(){
        PhysicalMachine pm = new PMBuilder().build();
        physicalMachines.add(pm);//for after test cleanup
        
        sut.add(pm);
        exception.expect(IllegalArgumentException.class);
        sut.add(null);
        
        List<PhysicalMachine> expectedList = Arrays.asList(pm);
        List<PhysicalMachine> actualList = sut.getConnectedPhysicalMachines();
        
        assertListsEquals(expectedList, actualList);
    }
    
    private void assertListsEquals(List<PhysicalMachine> expList, List<PhysicalMachine> actList){
        assertEquals("Size of both lists should be same",expList.size(),actList.size());
        for(int i = 0; i < expList.size(); ++i){
           PhysicalMachine expPM = expList.get(i);
           PhysicalMachine actPM = actList.get(i);
           assertEquals("IP Addresses of both physical machines should be same (PMs on index " + i + ")",
                        expPM.getAddressIP(),actPM.getAddressIP());
           assertEquals("Port of virtualization tool web server of both physical machines should be same (PMs on index " + i + ")",
                        expPM.getPortOfVTWebServer(), actPM.getPortOfVTWebServer());
           assertEquals("Username of both physical machines should be same (PMs on index " + i + ")",
                        expPM.getUsername(),actPM.getUsername());
           assertEquals("User password of both physical machines should be same (on index " + i + ")",
                        expPM.getUserPassword(), actPM.getUserPassword());
        }
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


