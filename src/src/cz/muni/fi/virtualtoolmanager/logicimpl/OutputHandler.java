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

import java.io.PrintStream;

/**
 *
 * @author Tomáš Šmíd
 */
class OutputHandler {
    private static PrintStream stdOutput = System.out;
    private static PrintStream stdErrOutput = System.err;
    
    public static void setStandardOutput(PrintStream printStream){
        stdOutput = printStream;
    }
    
    public static void setStandardErrorOutput(PrintStream printStream){
        stdErrOutput = printStream;
    }
    
    public void printMessage(String message){
        print(stdOutput,message);
    }
    
    public void printErrorMessage(String message){
        print(stdErrOutput,message);
    }
    
    private void print(PrintStream printStream, String message){
        if(printStream != null && message != null && !message.trim().isEmpty()){
            printStream.println(message);
        }
    }
}
