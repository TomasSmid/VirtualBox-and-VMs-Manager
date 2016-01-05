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

import java.util.UUID;

/**
 * <div>
 * Class that is used as criteria for virtual machine search operation 
 * determining what parameters must searched virtual machines met to be matched 
 * as a required result.
 * </div>
 * <div>
 * Class <code>SearchCriteria</code> uses similar way as
 * {@link cz.muni.fi.virtualtoolmanager.pubapi.entities.VirtualMachine VirtualMachine}
 * class to initialize new object of type of this class. It uses static building class
 * for easier creation of objects of type of this class.
 * </div>
 * 
 * @see cz.muni.fi.virtualtoolmanager.pubapi.managers.SearchManager
 * @see cz.muni.fi.virtualtoolmanager.logicimpl.SearchManagerImpl
 * 
 * @author Tomáš Šmíd
 */
public final class SearchCriteria {
    /** Represents uuid of the searched virtual machine */
    private UUID vmId;
    /** Represents name of the searched virtual machine */
    private String vmName;
    /** Represents number of used virtual CPUs by searched virtual machine */
    private Long countOfCPU;
    /** Represents number of used monitors by searched virtual machine */
    private Long countOfMonitors;
    /** Represents CPU execution limitation used by searched virtual machine */
    private Long cpuExecutionCap;
    /** Represents free space size of the virtual hard disk used by searched 
     virtual machine, expressed in bytes */
    private Long hardDiskFreeSpaceSize;
    /** Represents total space size of the virtual hard disk used by searched 
     virtual machine, expressed in bytes */
    private Long hardDiskTotalSize;
    /** Represents size of allocated memory for the searched virtual machine */
    private Long sizeOfRAM;
    /** Represents size of the video memory used by searched virtual machine */
    private Long sizeOfVRAM;
    /** Represents type of OS used in searched virtual machine */
    private String typeOfOS;
    /** Represents identifier of OS used in searched virtual machine */
    private String identifierOfOS;
    
    /**
     * Class that represents a key part of so called Builder pattern. Static class
     * <code>Builder</code> is used only for more transparent, easier and faster set
     * up of {@link cz.muni.fi.virtualtoolmanager.pubapi.entities.SearchCriteria
     * SearchCriteria} attributes.
     */
    public static class Builder {
        /** uuid of searched virtual machine, optional - default value is null */
        private UUID vmId = null;
        /** name of searched virtual machine, optional - default value is null */
        private String vmName = null;
        /** number of used virtual CPUs by searched virtual machine,
         * optional - default value is null */
        private Long countOfCPU = null;
        /** number of monitors used by searched virtual machine,
         * optional - default value is null */
        private Long countOfMonitors = null;
        /** CPU execution limitation used by searched virtual machine,
         * optional - default value is null */
        private Long cpuExecutionCap = null;
        /** free space size of virtual hard disk used by searched virtual machine,
         * optional - default value is null */
        private Long hardDiskFreeSpaceSize = null;
        /** total space size of virtual hard disk used by searched virtual machine,
         * optional - default value is null */
        private Long hardDiskTotalSize = null;
        /** size of memory allocated for searched virtual machine,
         * optional - default value is null */
        private Long sizeOfRAM = null;
        /** size of video memory used by searched virtual machine,
         * optional - default value is null */
        private Long sizeOfVRAM = null;
        /** type of OS used in searched virtual machine,
         * optional - default value is null */
        private String typeOfOS = null;
        /** identifier of OS used in searched virtual machine,
         * optional - default value is null */
        private String identifierOfOS = null;
        
        /**
         * Sets the virtual machine uuid as a search criterion which will be used
         * during virtual machine search operation.
         * @param value uuid of the search virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder id(UUID value){
            vmId = value;
            return this;
        }
        
        /**
         * Sets the virtual machine name as a search criterion which will be used
         * during virtual machine search operation.
         * @param value name of the searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder name(String value){
            vmName = value;
            return this;
        }
        
        /**
         * Sets the number of used virtual CPUs as a search criterion which will be used
         * during virtual machine search operation.
         * @param value number of used virtual CPUs by the searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder countOfCPU(Long value){
            countOfCPU = value;
            return this;
        }
        
        /**
         * Sets the number of used monitors as a search criterion which will be used
         * during virtual machine search operation.
         * @param value number of used monitors by the searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder countOfMonitors(Long value){
            countOfMonitors = value;
            return this;
        }
        
        /**
         * Sets the CPU execution limitation as a search criterion which will be used
         * during virtual machine search operation.
         * @param value CPU execution limitation used by the searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder cpuExecutionCap(Long value){
            this.cpuExecutionCap = value;
            return this;
        }
        
        /**
         * Sets the virtual hard disk free space size as a search criterion
         * which will be used during virtual machine search operation.
         * @param value free space size of the virtual hard disk used by the
         * searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder hardDiskFreeSpaceSize(Long value){
            hardDiskFreeSpaceSize = value;
            return this;
        }
        
        /**
         * Sets the virtual hard disk total space size as a search criterion
         * which will be used during virtual machine search operation.
         * @param value total space size of the virtual hard disk used by the
         * searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder hardDiskTotalSize(Long value){
            hardDiskTotalSize = value;
            return this;
        }
        
        /**
         * Sets the size of memory as a search criterion
         * which will be used during virtual machine search operation.
         * @param value size of the allocated memory used by the searched
         * virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder sizeOfRAM(Long value){
            sizeOfRAM = value;
            return this;
        }
        
        /**
         * Sets the size of video memory as a search criterion
         * which will be used during virtual machine search operation.
         * @param value size of the video memory used by the searched
         * virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder sizeOfVRAM(Long value){
            sizeOfVRAM = value;
            return this;
        }
        
        /**
         * Sets the type of OS as a search criterion which will be used during
         * virtual machine search operation.
         * @param value type of OS used in the searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder typeOfOS(String value){
            typeOfOS = value;
            return this;
        }
        
        /**
         * Sets the identifier of OS as a search criterion which will be used during
         * virtual machine search operation.
         * @param value identifier of OS used in the searched virtual machine
         * @return instance of actual Builder class for more compact and dynamic
         * building class
         */
        public Builder identifierOfOS(String value){
            identifierOfOS = value;
            return this;
        }
        
        /**
         * Creates a new search criteria object with the all specified attribute
         * values in builder class.
         * @return new search criteria for virtual machine search operation
         */
        public SearchCriteria build(){
            return new SearchCriteria(this);
        }
    }
    
    private SearchCriteria(Builder builder){
        this.vmId = builder.vmId;
        this.vmName = builder.vmName;
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

    public UUID getVmId() {
        return vmId;
    }

    public void setVmId(UUID vmId) {
        this.vmId = vmId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public Long getCountOfCPU() {
        return countOfCPU;
    }

    public void setCountOfCPU(Long countOfCPU) {
        this.countOfCPU = countOfCPU;
    }

    public Long getCountOfMonitors() {
        return countOfMonitors;
    }

    public void setCountOfMonitors(Long countOfMonitors) {
        this.countOfMonitors = countOfMonitors;
    }

    public Long getCpuExecutionCap() {
        return cpuExecutionCap;
    }

    public void setCpuExecutionCap(Long cpuExecutionCap) {
        this.cpuExecutionCap = cpuExecutionCap;
    }

    public Long getHardDiskFreeSpaceSize() {
        return hardDiskFreeSpaceSize;
    }

    public void setHardDiskFreeSpaceSize(Long hardDiskFreeSpaceSize) {
        this.hardDiskFreeSpaceSize = hardDiskFreeSpaceSize;
    }

    public Long getHardDiskTotalSize() {
        return hardDiskTotalSize;
    }

    public void setHardDiskTotalSize(Long hardDiskTotalSize) {
        this.hardDiskTotalSize = hardDiskTotalSize;
    }

    public Long getSizeOfRAM() {
        return sizeOfRAM;
    }

    public void setSizeOfRAM(Long sizeOfRAM) {
        this.sizeOfRAM = sizeOfRAM;
    }

    public Long getSizeOfVRAM() {
        return sizeOfVRAM;
    }

    public void setSizeOfVRAM(Long sizeOfVRAM) {
        this.sizeOfVRAM = sizeOfVRAM;
    }

    public String getTypeOfOS() {
        return typeOfOS;
    }

    public void setTypeOfOS(String typeOfOS) {
        this.typeOfOS = typeOfOS;
    }

    public String getIdentifierOfOS() {
        return identifierOfOS;
    }

    public void setIdentifierOfOS(String versionOfOS) {
        this.identifierOfOS = versionOfOS;
    }
    
}
