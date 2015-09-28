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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * This test class ensure unit testing of class OutputHandler and is intended to
 * be a pointer that class OutputHandler works as expected.
 * 
 * @author Tomáš Šmíd
 */
public class OutputHandlerTest {
    
    private OutputHandler sut;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    
    @Before
    public void setUp() {        
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        sut = new OutputHandler();
    }
    
    @After
    public void cleanUp(){
        System.setOut(null);
        System.setErr(null);
        OutputHandler.setStandardErrorOutput(null);
        OutputHandler.setStandardOutput(null);
    }
    
    /**
     * This test tests that if the method OutputHandler::printMessage() is called
     * with a valid print stream and valid message, then the message is correctly
     * printed to the required print stream with function PrintStream::println().
     */
    @Test
    public void printValidMessage(){
        //this step ensures the print stream is set up as standard output stream
        OutputHandler.setStandardOutput(System.out);
        String message = "Message for check";
        
        sut.printMessage(message);
        
        assertEquals("Contents should be same", message + System.lineSeparator(), outContent.toString());
    }
    
    /**
     * This test tests that if the method OutputHandler::printMessage() is called
     * with a valid print stream and null message, then the message is not printed
     * to the required print stream.
     */
    @Test
    public void printNullMessage(){
        //this step ensures the print stream is set up as standard output stream
        OutputHandler.setStandardOutput(System.out);
        
        sut.printMessage(null);
        
        assertTrue("There should not be any text on the standard output stream", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the method OutputHandler::printMessage() is called
     * with a valid print stream and empty string message, then the message is
     * not printed to the required print stream.
     */
    @Test
    public void printEmptyMessage(){
        //this step ensures the print stream is set up as standard output stream
        OutputHandler.setStandardOutput(System.out);
        
        sut.printMessage("");        
        sut.printMessage("       ");//message with white spaces
        
        assertTrue("There should not be any text on the standard output stream", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the method OutputHandler::printMessage() is called
     * with a null print stream and valid message, then the message is not printed
     * to the required print stream.
     */
    @Test
    public void printMessageWithNullPrintStream(){
        //this step ensures the print stream is set up as null print stream
        OutputHandler.setStandardOutput(null);
        String message = "Message for check";
        
        sut.printMessage(message);
        
        assertTrue("There should not be any text on the standard output stream", outContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the method OutputHandler::printErrorMessage() is
     * called with a valid print stream and valid message, then the message is
     * printed to the required print stream with function PrintStream::println().
     */
    @Test
    public void printValidErrorMessage(){
        //this step ensures the print stream is set up as standard error output stream
        OutputHandler.setStandardErrorOutput(System.err);
        String message = "Message for check";
        
        sut.printErrorMessage(message);
        
        assertEquals("Contents should be same", message + System.lineSeparator(), errContent.toString());
    }

    /**
     * This test tests that if the method OutputHandler::printErrorMessage() is
     * called with a valid print stream and null message, then the message is not
     * printed to the required print stream.
     */
    @Test
    public void printNullErrorMessage(){
        //this step ensures the print stream is set up as a standard error output stream
        OutputHandler.setStandardErrorOutput(System.err);
        
        sut.printErrorMessage(null);
        
        assertTrue("There should not be any text on the standard error output stream", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the method OutputHandler::printErrorMessage() is
     * called with a valid print stream and empty string message, then the message
     * is not printed to the required print stream.
     */
    @Test
    public void printEmptyErrorMessage(){
        //this step ensures the print stream is set up as standard error output stream
        OutputHandler.setStandardErrorOutput(System.err);
        
        sut.printErrorMessage("");        
        sut.printErrorMessage("       ");//message with white spaces
        
        assertTrue("There should not be any text on the standard error output stream", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the method OutputHandler::printErrorMessage() is
     * called with a null print stream and valid message, then the message is not
     * printed to the required print stream.
     */
    @Test
    public void printErrorMessageWithNullPrintStream(){
        //this step ensures the print stream is set up as null print stream
        OutputHandler.setStandardErrorOutput(null);
        String message = "Message for check";
        
        sut.printErrorMessage(message);
        
        assertTrue("There should not be any text on the standard error output stream", errContent.toString().isEmpty());
    }
    
    /**
     * This test tests that if the methods OutputHandler::printMessage() and
     * OutputHandler::printErrorMessage() are called with two different
     * messages and the error output stream and output stream are set up to same
     * print stream, then both messages appears on the same stream.
     */
    @Test
    public void printMessageAndErrorMessageWithSamePrintStream(){
        //both printed streams are set up as the standard output stream
        OutputHandler.setStandardErrorOutput(System.out);
        OutputHandler.setStandardOutput(System.out);
        String message1 = "Message1 for check";
        String message2 = "Message2 for check";
        
        sut.printMessage(message1);
        sut.printErrorMessage(message2);
        
        assertTrue("There should not be any text on the standard error output stream", errContent.toString().isEmpty());
        assertFalse("There should be both messages on the standard output stream", outContent.toString().isEmpty());
        assertEquals("Contents should be same", message1 + System.lineSeparator() + message2 + System.lineSeparator(),
                                                outContent.toString());
    }
    
    /**
     * This test tests that if the methods OutputHandler::printMessage() and
     * OutputHandler::printErrorMessage() are called with the same message, then
     * the message appears on both print streams.
     */
    @Test
    public void printMessageWithOutAndErrPrintStream(){
        //print stream is set up as the standard error output stream
        OutputHandler.setStandardErrorOutput(System.err);
        //print stream is set up as the standard output stream
        OutputHandler.setStandardOutput(System.out);
        String message = "Message for check";
        
        sut.printMessage(message);
        sut.printErrorMessage(message);
        
        assertEquals("Contents should be same", message + System.lineSeparator(), errContent.toString());        
        assertEquals("Contents should be same", message + System.lineSeparator(), outContent.toString());
    }
}
