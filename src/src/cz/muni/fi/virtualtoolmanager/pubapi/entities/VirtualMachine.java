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
 *
 * @author Tomáš Šmíd
 */
public final class VirtualMachine implements Comparable<VirtualMachine>{
    private final UUID id;
    private final String name;
    private final PhysicalMachine hostMachine;
    private final Long countOfCPU;
    private final Long countOfMonitors;
    private final Long cpuExecutionCap;
    private final Long hardDiskFreeSpaceSize;
    private final Long hardDiskTotalSize;
    private final Long sizeOfRAM;
    private final Long sizeOfVRAM;
    private final String typeOfOS;
    private final String identifierOfOS;
    
    //builder for more transparent set up VirtualMachine attributes
    public static class Builder{
        private final UUID id;
        private final String name;
        private final PhysicalMachine hostMachine;
        private Long countOfCPU = 0L;
        private Long countOfMonitors = 0L;
        private Long cpuExecutionCap = 0L;
        private Long hardDiskFreeSpaceSize = 0L;
        private Long hardDiskTotalSize = 0L;
        private Long sizeOfRAM = 0L;
        private Long sizeOfVRAM = 0L;
        private String typeOfOS = "Unknown";
        private String identifierOfOS = "Unknown";
        
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
        
        public Builder countOfCPU(Long value){
            countOfCPU = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        public Builder countOfMonitors(Long value){
            countOfMonitors = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        public Builder cpuExecutionCap(Long value){
            cpuExecutionCap = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        public Builder hardDiskFreeSpaceSize(Long value){
            hardDiskFreeSpaceSize = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        public Builder hardDiskTotalSize(Long value){
            hardDiskTotalSize = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        public Builder sizeOfRAM(Long value){
            sizeOfRAM = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        public Builder sizeOfVRAM(Long value){
            sizeOfVRAM = (value != null && value >= 0L ? value : 0L);
            return this;
        }
        
        public Builder typeOfOS(String value){
            typeOfOS = (value != null && !value.equals("") ? value : "Unknown");
            return this;
        }
        
        public Builder identifierOfOS(String value){
            identifierOfOS = (value != null && !value.equals("") ? value : "Unknown");
            return this;
        }
        
        public VirtualMachine build(){
            return new VirtualMachine(this);
        }
    }
    
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
    
    @Override
    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof VirtualMachine)) return false;
        VirtualMachine vm = (VirtualMachine)obj;
        return ((this.id == vm.id) || 
                (this.id != null && this.id.equals(vm.id))) &&
               ((this.name == vm.name) ||
                (this.name != null && this.name.equals(vm.name)));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.name);
        return hash;
    }
    
    @Override
    public String toString(){
        return "[" + "Virtual machine: id=" + this.id + ", name=" + this.name +
               ", host machine=" + this.hostMachine + "]";
    }
    
    @Override
    public int compareTo(VirtualMachine vm) {
        int result = this.id.compareTo(vm.id);
        return (result == 0 ? this.name.compareTo(vm.name) : result);
    }
}
