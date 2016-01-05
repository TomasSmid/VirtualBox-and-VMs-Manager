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
package cz.muni.fi.virtualtoolmanager.pubapi.entities;

import java.util.Objects;
import java.util.UUID;

/**
 * <div>
 * Class that is used to represent a virtual machine (guest) from a remote
 * connected physical machine.
 * </div> 
 * <div>
 * <p>
 * Virtual machine is used for determining which
 * real virtual machine on remote host machine should be manipulated.
 * <p>
 * For object initialization of type this class is used so called Builder pattern.
 * It is used because of the big amount of class attributes to preserve the
 * easy usability and readability of this class in client code.
 * <div>
 * Sample of usage this class without Builder pattern and with it:
 * <ol>
 * <li>Without Builder pattern:
 * <code>VirtualMachine vm = new VirtualMachine(UUID.fromString("uuidsample"),
 *                                              "MyTestVM",
 *                                              new PhysicalMachine("ip","port",
 *                                              "user","pswd"),
 *                                              1L,1L,100L,1545148487L,2011001470L,
 *                                              2048L,12L,"ostype","osid");</code>
 * <li>With Builder pattern:
 * <code>VirtualMachine vm = new VirtualMachine.Builder(UUID.fromString("uuidsample"),
 *                                                      "MyTestVM",
 *                                                      new PhysicalMachine("ip",
 *                                                      "port","user","pswd"))
 *                                             .countOfCPU(1L)
 *                                             .countOfMonitors(1L)
 *                                             .cpuExecutionCap(100L)
 *                                             .hardDiskFreeSpaceSize(1545148487L)
 *                                             .hardDiskTotalSize(2011001470L)
 *                                             .sizeOfRAM(2048L)
 *                                             .sizeOfVRAM(12L)
 *                                             .typeOfOS("ostype")
 *                                             .identifierOfOS("osid")
 *                                             .build();</code>
 * </ol>
 * </div>
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualMachineManager
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.VirtualizationToolManager
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.SearchManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualMachineManagerImpl
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.VirtualizationToolManagerImpl
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.SearchManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public final class VirtualMachine implements Comparable<VirtualMachine>{
    /** Represents an uuid of a virtual machine */
    private final UUID id;
    /** Represents a name of a virtual machine */
    private final String name;
    /** Represents a host machine of a virtual machine */
    private final PhysicalMachine hostMachine;
    /** Represents a number of virtual CPUs which a virtual machine uses */
    private final Long countOfCPU;
    /** Represents a number of monitors which a virtual machne uses */
    private final Long countOfMonitors;
    /** Represents a virtual CPU execution limitation in percent, 100 % means 
     * no limitations */
    private final Long cpuExecutionCap;
    /** Represents a free space on the virtual hard disk, expressed in bytes */
    private final Long hardDiskFreeSpaceSize;
    /** Represents a total space size of the virtual hard disk, expressed in bytes */
    private final Long hardDiskTotalSize;
    /** Represents a size of used memory by virtual machine, expressed in megabytes */
    private final Long sizeOfRAM;
    /** Represents a size of used video memory by virtual machine, expressed in 
     * megabytes */
    private final Long sizeOfVRAM;
    /** Represents the family (type) of used OS in virtual machine, e.g. Linux */
    private final String typeOfOS;
    /** Represents the particular title (id) of used OS in virtual machine,
     * e.g. Fedora_64*/
    private final String identifierOfOS;
    
    /**
     * Class that represents a key part of so called Builder pattern. Static class
     * <code>Builder</code> is used only for more transparent, easier and faster set
     * up of {@link cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine
     * VirtualMachine} attributes.
     */
    public static class Builder{
        /** uuid of virtual machine, mandatory - must be specified in Builder 
         * constructor */
        private final UUID id;
        /** name of virtual machine, mandatory - must be specified in Builder 
         * constructor */
        private final String name;
        /** host machine of virtual machine, mandatory - must be specified in
         * Builder constructor */
        private final PhysicalMachine hostMachine;
        /** number of used virtual CPUs by virtual machine, optional - has not
         * to be specified, default value is null */
        private Long countOfCPU = null;
        /** number of used monitors by virtual machine, optional - has not to 
         * be specified, default value is null */
        private Long countOfMonitors = null;
        /** virtual CPU execution limitation, optional - has not to be specified, 
         * default value is null */
        private Long cpuExecutionCap = null;
        /** free space size of virtual hard disk used by virtual machine,
         * optional - has not to be specified, default value is null */
        private Long hardDiskFreeSpaceSize = null;
        /** total space size of the virtual hard disk used by virtual machine,
         * optional - has not to be specified, default value is null */
        private Long hardDiskTotalSize = null;
        /** size of momery used by virtual machine, optional - has not to be
         * specified, default value is null */
        private Long sizeOfRAM = null;
        /** size of video memory used by virtual machine, optional - has not to
         * be specified, default value is null */
        private Long sizeOfVRAM = null;
        /** type of virtual machine OS, optional - has not to be specified,
         * default value is null */
        private String typeOfOS = null;
        /** id of virtual machine OS, optional - has not to be specified,
         * default value is null */
        private String identifierOfOS = null;
        
        /**
         * The first and the only construcotr of the static class Builder used
         * for setting up the key (mandatory) values of the virtual machine.
         * There can be thrown the following exceptions:
         * <ul>
         * <li><strong>IllegalArgumentException - </strong>thrown when there
         * occurs one of the following problems:
         * <ol>
         * <li>given uuid of virtual machine is null or empty
         * <li>given name of virtual machine is null or empty
         * <li>given host machine of virtual machine is null
         * </ol>
         * </ul>
         * @param id uuid which will be used for virtual machine which is going
         * to be initialized
         * @param vmName name which will be used for virtual machine which is
         * going to be initialized
         * @param hostMachine physical machine on which virtual machine which is
         * going to be initialized is present
         */
        public Builder(UUID id, String vmName, PhysicalMachine hostMachine){
            if(id == null || id.toString().trim().isEmpty()){
                throw new IllegalArgumentException("Virtual machine initialization failure: "
                        + " ID must not be null nor empty.");
            }else{
                this.id = id;
            }
            
            if(vmName == null || vmName.trim().isEmpty()){
                throw new IllegalArgumentException("Virtual machine initialization failure: "
                        + " Name of a virtual machine must not be null nor empty.");
            }else{
                this.name = vmName;
            }
            
            if(hostMachine == null){
                throw new IllegalArgumentException("Virtual machine initialization failure: "
                        + " Host machine of a virtual machine must be specified.");
            }else{
                this.hostMachine = hostMachine;
            }
        }
        
        /**
         * Sets the number of virtual CPUs which will be used for virtual machine
         * initialization. If the the given value is negative then the value is
         * set up to zero.
         * @param value number of virtual CPUs which virtual machine uses
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder countOfCPU(Long value){
            countOfCPU = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        /**
         * Sets the number of monitors which will be used for virtual machine
         * initialization. If the the given value is negative then the value is
         * set up to zero.
         * @param value number of monitors which virtual machine uses
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder countOfMonitors(Long value){
            countOfMonitors = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        /**
         * Sets the virtual CPU execution limitation which will by used for the
         * virtual machine initialization. If the the given value is negative 
         * then the value is set up to zero.
         * @param value CPU execution limitation expressed in percents
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder cpuExecutionCap(Long value){
            cpuExecutionCap = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        /**
         * Sets the free space size of the virtual hard disk which will be used
         * for the virtual machine initialization. If the the given value is
         * negative then the value is set up to zero.
         * @param value hard disk free space size in bytes
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder hardDiskFreeSpaceSize(Long value){
            hardDiskFreeSpaceSize = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        /**
         * Sets the total space size of the virtual hard disk which will be used
         * for the virtual machine initialization. If the the given value is
         * negative then the value is set up to zero.
         * @param value hard disk total size in bytes
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder hardDiskTotalSize(Long value){
            hardDiskTotalSize = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        /**
         * Sets the amount of memory allocated for virtual machine which will
         * be used for the virtual machine initialization. If the the given
         * value is negative then the value is set up to zero.
         * @param value size of allocated memory in megabytes
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder sizeOfRAM(Long value){
            sizeOfRAM = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        /**
         * Sets the size of video memory which will be used for the virtual
         * machine initialization. If the the given value is negative then
         * the value is set up to zero.
         * @param value size of video memory in megabytes, used
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder sizeOfVRAM(Long value){
            sizeOfVRAM = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        /**
         * Sets the type of virtual machine OS which will be used for virtual
         * machine initialization. If the given value is null or empty then the
         * string value is set up to "Unknown".
         * @param value type of virtual machine OS
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder typeOfOS(String value){
            typeOfOS = (value != null && !value.equals("") ? value : "Unknown");
            return this;
        }
        
        /**
         * Sets the identifier of virtual machine OS which will be used for virtual
         * machine initialization. If the given value is null or empty then the
         * string value is set up to "Unknown".
         * @param value identifier of virtual machine OS
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder identifierOfOS(String value){
            identifierOfOS = (value != null && !value.equals("") ? value : "Unknown");
            return this;
        }
        
        /**
         * Creates a new virtual machine with the set up attributes.
         * @return newly created virtual machine
         */
        public VirtualMachine build(){
            return new VirtualMachine(this);
        }
    }
    
    /**
     * The first and the only constructor which is private becase of the Builder
     * pattern usage.
     * 
     * @param builder builder containing set up attributes, which are going to be
     * used for virtual machine initialization
     */
    private VirtualMachine(Builder builder){
        this.id = builder.id;
        this.name = builder.name;
        this.hostMachine = builder.hostMachine;
        this.countOfCPU = builder.countOfCPU;
        this.countOfMonitors = builder.countOfMonitors;
        this.cpuExecutionCap = builder.cpuExecutionCap;
        this.hardDiskFreeSpaceSize = builder.hardDiskFreeSpaceSize;
        this.hardDiskTotalSize = builder.hardDiskTotalSize;
        this.sizeOfRAM = builder.sizeOfRAM;
        this.sizeOfVRAM = builder.sizeOfVRAM;
        this.typeOfOS = builder.typeOfOS;
        this.identifierOfOS = builder.identifierOfOS;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PhysicalMachine getHostMachine() {
        return hostMachine;
    }

    public Long getCountOfCPU() {
        return countOfCPU;
    }

    public Long getCountOfMonitors() {
        return countOfMonitors;
    }

    public Long getCPUExecutionCap() {
        return cpuExecutionCap;
    }

    public Long getHardDiskFreeSpaceSize() {
        return hardDiskFreeSpaceSize;
    }

    public Long getHardDiskTotalSize() {
        return hardDiskTotalSize;
    }

    public Long getSizeOfRAM() {
        return sizeOfRAM;
    }

    public Long getSizeOfVRAM() {
        return sizeOfVRAM;
    }

    public String getTypeOfOS() {
        return typeOfOS;
    }

    public String getIdentifierOfOS() {
        return identifierOfOS;
    }    
    
    /**
     * This method is used for check if this physical machine is same as
     * another physical machine. Comparison is performed according to all
     * key virtual machine properties - uuid, name and host machine.
     * 
     * @param obj represents virtual machine instance which is going to be
     * compared with this virtual machine
     * @return <code>true</code> if the objects are same,
     * <code>false</code> otherwise
     */
    @Override
    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof VirtualMachine)) return false;
        VirtualMachine vm = (VirtualMachine)obj;
        return ((this.id == vm.id) || 
                (this.id != null && this.id.equals(vm.id))) &&
               ((this.name == vm.name) ||
                (this.name != null && this.name.equals(vm.name))) &&
               ((this.hostMachine == vm.hostMachine) ||
                (this.hostMachine != null && this.hostMachine.equals(vm.hostMachine)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.hostMachine);
        return hash;
    }
    
    /**
     * Method is used to print information about the actual virtual machine to
     * string.
     * @return string in form "[Virtual machine: id=&lt;actualuuid&gt;,
     * name=&lt;actualname&gt;, host machine=&lt;actualhostmachine&gt;]"
     */
    @Override
    public String toString(){
        return "[" + "Virtual machine: id=" + this.id + ", name=" + this.name +
               ", host machine=" + this.hostMachine + "]";
    }
    
    /**
     * Method used to compare two virtual machines - this with another - in
     * order to sort (order) virtual machines. For comparison are used properties
     * uuid, name and host machine.
     * @param vm represents virtual machine with which is this one compared
     * @return <code>-1</code> if this virtual machine is less than another one,
     * <code>0</code>if this virtual machine is equal to another one,
     * <code>1</code>if this virtual machine is greater than another one
     */
    @Override
    public int compareTo(VirtualMachine vm) {
        int result = this.id.compareTo(vm.id);
        
        if(result == 0){
            result = this.name.compareTo(vm.name);
        }else{
            return result;
        }
        return (result == 0 ? this.hostMachine.compareTo(vm.hostMachine) : result);
    }
}
