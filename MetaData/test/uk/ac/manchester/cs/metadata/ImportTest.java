/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.cs.metadata;

import java.io.File;
import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import uk.ac.manchester.cs.rdftools.RdfFactory;
import uk.ac.manchester.cs.rdftools.RdfReader;
import uk.ac.manchester.cs.rdftools.Reporter;
import uk.ac.manchester.cs.rdftools.VoidValidatorException;
import uk.ac.manchester.cs.validator.Validator;

/**
 *
 * @author Christian
 */
public class ImportTest {
    
    static MetaDataSpecification specifications;
   
    private static final Resource ALL_SUBJECTS = null;
    private static final URI ALL_PREDICATES = null;
    private static final Value ALL_OBJECTS = null;
    
    @BeforeClass
    public static void setUpClass() throws VoidValidatorException {
        File file = new File ("test-data/testSimple.ttl");
        
        specifications = new MetaDataSpecification("test-data/simpleOntology.owl");
    }

    @Test
    @Ignore
    public void TwoPartValidate() throws VoidValidatorException {
        Reporter.println("TwoPartValidate");
        RdfReader reader = RdfFactory.getMemory();
        File part1 = new File ("test-data/testPart1.ttl");
        File part2 = new File ("test-data/testPart2.ttl");
        reader.loadFile(part1);
        List<Statement> list1 = reader.getStatementList(ALL_SUBJECTS, ALL_PREDICATES, ALL_OBJECTS);
        Resource context = reader.loadFile(part2);
        List<Statement> list2 = reader.getStatementList(ALL_SUBJECTS, ALL_PREDICATES, ALL_OBJECTS);
        assertThat(list2.size(), greaterThan(list1.size()));
        Validator validator = new Validator(reader, context, specifications);
        String result = validator.validate(context);
        assertThat(result,  endsWith(Validator.SUCCESS));
    }

    @Test
    public void TwoPartMissingValidate() throws VoidValidatorException {
        Reporter.println("TwoPartMissingValidate");
        RdfReader reader = RdfFactory.getMemory();
        File part1 = new File ("test-data/testPart1Missing.ttl");
        File part2 = new File ("test-data/testPart2.ttl");
        reader.loadFile(part1);
        List<Statement> list1 = reader.getStatementList(ALL_SUBJECTS, ALL_PREDICATES, ALL_OBJECTS);
        Resource context = reader.loadFile(part2);
        List<Statement> list2 = reader.getStatementList(ALL_SUBJECTS, ALL_PREDICATES, ALL_OBJECTS);
        assertThat(list2.size(), greaterThan(list1.size()));
        Validator validator = new Validator(reader, context, specifications);
        String result = validator.validate(context);
        System.out.println(result);
        assertThat(result, containsString(LinkedResource.ERROR_SEE_REPORT));
        assertThat(result,  endsWith(Validator.FAILED));
    }
}