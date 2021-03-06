/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.cs.datadesc.validator.rdftools;

import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;

/**
 *
 * @author Christian
 */
public class RdfInterfaceTest {
    
    /**
     * Test of add method, of class RdfInterface.
     */
    @Test
    public void testAdd() throws Exception {
        Reporter.println("add");
        RdfReader original = RdfFactory.getMemory();
        try {
            original.loadURI(ExampleConstants.EXAMPLE_CONTEXT, null);
        } catch (Exception ex){
            ex.printStackTrace();
            return;
        }
        List<Statement> statements = original.getStatementList(null, null, null, new URIImpl(ExampleConstants.EXAMPLE_CONTEXT));
        RdfReader instance = RdfFactory.getMemory();
        Resource newContext = new URIImpl("http://www.example.com/testAdd/");
        for (Statement statment:statements){
            instance.add(statment, newContext);
        }
        instance.commit();
        List<Statement> results = original.getStatementList(null, null, null, new URIImpl(ExampleConstants.EXAMPLE_CONTEXT));
        assertEquals(statements.size(), results.size());
     }
   
}
