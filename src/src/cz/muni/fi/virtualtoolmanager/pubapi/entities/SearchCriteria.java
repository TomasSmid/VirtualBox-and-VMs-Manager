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
 *
 * @author Tomáš Šmíd
 */
public final class SearchCriteria {
    private UUID vmId;
    private String vmName;
    private Long countOfCPU;
    private Long countOfMonitors;
    private Long cpuExecutionCap;
    private Long hardDiskFreeSpaceSize;
    private Long hardDiskTotalSize;
    private Long sizeOfRAM;
    private Long sizeOfVRAM;
    private String typeOfOS;
    private String identifierOfOS;
    
    public static class Builder {
        private UUID vmId = null;
        private String vmName = null;
        private Long countOfCPU = null;
        private Long countOfMonitors = null;
        private Long cpuExecutionCap = null;
        private Long hardDiskFreeSpaceSize = null;
        private Long hardDiskTotalSize = null;
        private Long sizeOfRAM = null;
        private Long sizeOfVRAM = null;
        private String typeOfOS = null;
        private String identifierOfOS = null;
        
        public Builder id(UUID value){
            vmId = value;
            return this;
        }
        
        public Builder name(String value){
            vmName = value;
            return this;
        }
        
        public Builder countOfCPU(Long value){
            countOfCPU = value;
            return this;
        }
        
        public Builder countOfMonitors(Long value){
            countOfMonitors = value;
            return this;
        }
        
        public Builder cpuExecutionCap(Long value){
            this.cpuExecutionCap = value;
            return this;
        }
        
        public Builder hardDiskFreeSpaceSize(Long value){
            hardDiskFreeSpaceSize = value;
            return this;
        }
        
        public Builder hardDiskTotalSize(Long value){
            hardDiskTotalSize = value;
            return this;
        }
        
        public Builder sizeOfRAM(Long value){
            sizeOfRAM = value;
            return this;
        }
        
        public Builder sizeOfVRAM(Long value){
            sizeOfVRAM = value;
            return this;
        }
        
        public Builder typeOfOS(String value){
            typeOfOS = value;
            return this;
        }
        
        public Builder identifierOfOS(String value){
            identifierOfOS = value;
            return this;
        }
        
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
