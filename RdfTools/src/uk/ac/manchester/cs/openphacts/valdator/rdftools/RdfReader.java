// OpenPHACTS RDF Validator,
// A tool for validating and storing RDF.
//
// Copyright 2012-2013  Christian Y. A. Brenninkmeijer
// Copyright 2012-2013  University of Manchester
// Copyright 2012-2013  OpenPhacts
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package uk.ac.manchester.cs.openphacts.valdator.rdftools;

import info.aduna.lang.FileFormat;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import uk.ac.manchester.cs.openphacts.valdator.utils.UrlReader;

/**
 *
 * @author Christian
 */
public class RdfReader implements RdfInterface{
    
    public static String DEFAULT_BASE_URI = "http://no/BaseURI/Set/";
    private static final boolean EXCLUDE_INFERRED =false;
    
    private final Repository repository;
    private RepositoryConnection connection = null;
    private final Set<URI> parentPredicates; //currently 1 hard coded value.
    private final Set<Resource> loadedContexts;
    private final Set<RdfInterface> others; 
    
    public static RdfReader factory(Repository repository) throws VoidValidatorException{
        RdfReader instance = new RdfReader(repository);
        try {
            repository.initialize();
            return instance;
        } catch (Exception ex) {
            throw new VoidValidatorException ("Error parsing RDf file ", ex);
        }       
    } 
   
    private RdfReader(Repository repository) throws VoidValidatorException{
        this.repository = repository; 
        parentPredicates = new HashSet<URI>();
        parentPredicates.add(new URIImpl ("http://rdfs.org/ns/void#subset"));
        loadedContexts = new HashSet<Resource>();
        others = new HashSet<RdfInterface>();
    }
   
    public Resource loadFile(File inputFile) throws VoidValidatorException{
        return loadFile(inputFile, null);
    }
    
    public Resource loadFile(File inputFile, RDFFormat format) throws VoidValidatorException{
        try {
            InputStream stream = new FileInputStream(inputFile);
            return loadInputStream(stream, inputFile.toURI().toString(), format);
        } catch (FileNotFoundException ex) {
            throw new VoidValidatorException("Unable to find file. " + inputFile.getAbsolutePath(), ex);
        }
    }
   
    public Resource loadURI(String address) throws VoidValidatorException {
        return loadURI(address, null);
    }
    
    public Resource loadURI(String address, RDFFormat format) throws VoidValidatorException {
        if (address.startsWith("file")){
            File file = new File(address);
            return loadFile(file);
        }
        UrlReader urlReader = new UrlReader(address);
        InputStream stream = urlReader.getInputStream();
        return loadInputStream(stream, address, format);
    }
   
    public void addOtherSource(RdfInterface other){
        others.add(other);
    }
    
    private Resource loadInputStream(InputStream stream, String address, RDFFormat format) throws VoidValidatorException{
        try {
            Resource context = new URIImpl(address);
            loadedContexts.add(context);
            RepositoryConnection repositoryConnection = getConnection();
            connection.setAutoCommit(false);
            if (format == null){
                format = getFormat(address);
            }
            repositoryConnection.add(stream, address, format, context);
            connection.commit();
            return context;
        } catch (Exception ex) {
            closeOnError();
            throw new VoidValidatorException ("Error parsing RDf file ", ex);
        }
    }

    public Resource loadString(String text, RDFFormat rdfFormat) throws VoidValidatorException {
        InputStream is = new ByteArrayInputStream(text.getBytes());
        if (rdfFormat == null){
            throw new VoidValidatorException("You must supply an rdfFormat");
        }
        return loadInputStream(is, DEFAULT_BASE_URI, rdfFormat);
    }
    
    @Override
    public List<Statement> getStatementList(Resource subjectResource, URI predicate, Value object, 
            Resource... contexts) throws VoidValidatorException {
        List<Statement> results = getLocalStatementList(subjectResource, predicate, object, contexts);
        if (!results.isEmpty()){
            return results;
        }
        if (subjectResource == null){
            return new ArrayList<Statement>();
        }
        results = loadExternalAndGetTheStatementList(subjectResource, predicate, object);
        if (!results.isEmpty()){
            return results;
        }
         return findByParentResouce(subjectResource, predicate, object);
    }

    private List<Statement> getLocalStatementList(Resource subjectResource, URI predicate, Value object, 
            Resource... contexts) throws VoidValidatorException {
        List<Statement> results = this.getDirectOnlyStatementList(subjectResource, predicate, object, contexts);
        if (!results.isEmpty()){
            return results;
        }
        results = this.getDirectOnlyStatementList(subjectResource, predicate, object);
        if (!results.isEmpty()){
            return results;
        }
        return getIndirectStatementList(subjectResource, predicate, object);
    }


    private List<Statement> getIndirectStatementList(Resource subjectResource, URI predicate, Value object) throws VoidValidatorException {
        List<Statement> fromOthers = new ArrayList<Statement>();
        if (!others.isEmpty()){
            for (RdfInterface other:others){
                List<Statement> extra = 
                        other.getDirectOnlyStatementList(subjectResource, predicate, object);
                fromOthers = mergeStatementListsIgnoringContext(fromOthers, extra);
            }  
        }
        return fromOthers;
   }
     
    @Override
    public void runSparqlQuery(String queryString, TupleQueryResultHandler handler) throws VoidValidatorException {
        runSparqlQuery(queryString, handler, true);
    }
    
    private void runSparqlQuery(String queryString, TupleQueryResultHandler handler, boolean mainRdf) throws VoidValidatorException {
        TupleQueryResult result = null;
        try {
            RepositoryConnection repositoryConnection = getConnection();
            TupleQuery tupleQuery;
            tupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
            result = tupleQuery.evaluate();
            if (mainRdf){
                handler.startQueryResult(result.getBindingNames());
            }
            while (result.hasNext()){
                handler.handleSolution(result.next());
            }
            for (RdfInterface other:others){
                if (other instanceof RdfReader){
                 ((RdfReader)other).runSparqlQuery(queryString, handler, false);
                }
            }
            if (mainRdf){
                handler.endQueryResult();
            }
        } catch (RepositoryException ex) {
            closeOnError();
            throw new VoidValidatorException("Unable to connect to repository ", ex);
        } catch (MalformedQueryException ex) {
            closeOnError();
            throw new VoidValidatorException("Query " + queryString + " is malformed.", ex);
        } catch (QueryEvaluationException ex) {
            closeOnError();
            throw new VoidValidatorException("Error evaluating query " + queryString, ex);
        } catch (TupleQueryResultHandlerException ex) {
            closeOnError();
            throw new VoidValidatorException("Error handling result of  " + queryString, ex);
        } finally {
            if (result != null){
                try {
                    result.close();
                } catch (QueryEvaluationException ex) {
                    closeOnError();
                    //ignore it as there is probab;y already an error.
                }
            }
        }
    }
   
    private RepositoryResult<Statement> getTheStatementList(Resource subjectResource, URI predicate, Value object, 
            Resource... contexts) throws VoidValidatorException {
        try {
            RepositoryConnection repositoryConnection = getConnection();
            RepositoryResult<Statement> results = 
                        repositoryConnection.getStatements(subjectResource, predicate, object, EXCLUDE_INFERRED, contexts);
            if (results.hasNext()){
                //ystem.out.println("Found something so done");
                return results;
            }
            if (subjectResource == null){
                //stem.out.println("No subject so don't look elsehwere");
                return results;
            }
            results = repositoryConnection.getStatements(subjectResource, predicate, object, EXCLUDE_INFERRED);
            if (results.hasNext()){
                //ystem.out.println("Found something in another context so done.");
                return results;
            }
            return null;
         } catch (RepositoryException ex) {
            closeOnError();
            throw new VoidValidatorException("Error getting the Statements ", ex);
        }
    }

    private List<Statement> loadExternalAndGetTheStatementList(Resource subjectResource, URI predicate, Value object) 
            throws VoidValidatorException {
        try {
            //ystem.out.println("looking for " + subjectResource);
            RepositoryConnection repositoryConnection = getConnection();
            if (!(subjectResource instanceof URI)){
                //ystem.out.println("Not URI");
                return new ArrayList<Statement>();
            }
            URI subjectUri = (URI)subjectResource;
            String contextString = subjectUri.getNamespace();
            contextString = contextString.substring(0, contextString.length()-1);
            Resource subjectContext = new URIImpl(contextString);
            if (loadedContexts.contains(subjectContext)){
                //ystem.out.println("Already loaded " + subjectContext);
                return new ArrayList<Statement>();
            }
            try {
                if (repositoryConnection.hasStatement(null, null, null, EXCLUDE_INFERRED, subjectContext)){
                    //ystem.out.println("Already loaded " + subjectContext + " so not trying again ");
                    loadedContexts.add(subjectContext);
                    return new ArrayList<Statement>();
                }
            } catch (RepositoryException ex) {
                throw new VoidValidatorException("Unable to check source " + subjectContext + " is loaded. ", ex);
            }
            Resource newSubjectContext;
            try{
                newSubjectContext = loadURI(subjectContext.toString());
                //ystem.out.println("Loaded " + subjectContext);                
            } catch (Exception ex){
                //ystem.out.println("External load of " + subjectContext + " failed. quess it wasn't a URL (locator) after all");
                return new ArrayList<Statement>();
            }
            if (!newSubjectContext.equals(subjectContext)){
                throw new VoidValidatorException(newSubjectContext + " != " + subjectContext);
            }
            RepositoryResult<Statement> results = 
                    repositoryConnection.getStatements(subjectResource, predicate, object, EXCLUDE_INFERRED, subjectContext);
            return results.asList();
        } catch (RepositoryException ex) {
            closeOnError();
            throw new VoidValidatorException("Error loading external the Statements ", ex);
        }
    }

    @Override
    public List<Statement> getDirectOnlyStatementList(Resource subjectResource, URI predicate, Value object, Resource... contexts) 
            throws VoidValidatorException {
        try {
            RepositoryConnection repositoryConnection = getConnection();
            RepositoryResult<Statement> repositoryResult = 
                    repositoryConnection.getStatements(subjectResource, predicate, object, EXCLUDE_INFERRED, contexts);
            return repositoryResult.asList();
        } catch (Exception ex) {
            closeOnError();
            throw new VoidValidatorException ("Error getting Type Statements ", ex);
        }        
    }

    private RepositoryConnection getConnection() throws VoidValidatorException{
        try {
            if (connection != null){
                if (connection.isOpen()){
                    return connection;
                }         
            }
            connection = repository.getConnection();
            return connection;
        } catch (Exception ex) {
            throw new VoidValidatorException ("Error getting connection ", ex);
        }        
    }

   private static RDFFormat getFormat(String fileName) throws VoidValidatorException{
        if (fileName.endsWith(".n3")){
            fileName = "try.ttl";
        }
        RDFParserRegistry reg = RDFParserRegistry.getInstance();
        FileFormat fileFormat = reg.getFileFormatForFileName(fileName);
        if (fileFormat == null || !(fileFormat instanceof RDFFormat)){
            //added bridgeDB/OPS specific extension here if required.  
            throw new VoidValidatorException("Unable to dettermine RDF formst based on file name " + fileName);
        } else {
            return (RDFFormat)fileFormat;
        }
    }

    public final void close() throws VoidValidatorException {
        try {
            if (connection != null){
                if (connection.isOpen()){
                    connection.close();
                    connection = null;
                }
            
            }
        } catch (Exception ex) {
            throw new VoidValidatorException ("Error shutting down connection", ex);
        }        
    }

    private void closeOnError() {
        try {
            close();
        } catch (VoidValidatorException ex) {
            //do nothing as there is already an error
        }
    }

    private List<Statement> findByParentResouce(Resource subjectResource, URI predicate, Value object) throws VoidValidatorException {
        //ystem.out.println ("check parent " + subjectResource);
        ArrayList<Statement> results = new ArrayList<Statement>();
        for (URI parentPredicate:parentPredicates){
            List<Statement> parentStatements = this.getLocalStatementList(null, parentPredicate, subjectResource);
            for (Statement parentStatement:parentStatements){
                List<Statement> found = 
                        this.getStatementList(parentStatement.getSubject(), predicate, object);
                for (Statement find:found){
                    if (!results.contains(find)){
                        results.add(find);
                    }
                }
            }
        }
        return results;
     }

    public void write(RDFFormat format, OutputStream out) throws RDFHandlerException, VoidValidatorException, RepositoryException{
        RDFWriter rdfWriter = Rio.createWriter(format, out);
        rdfWriter.startRDF();
        RepositoryConnection repositoryConnection = getConnection();
        RepositoryResult<Statement> statements = 
                repositoryConnection.getStatements(null, null, null, true);
        while (statements.hasNext()) {
            Statement statement = statements.next();
            rdfWriter.handleStatement(statement);
        }
        rdfWriter.endRDF();

    }
    
    public static void main(String[] args) throws VoidValidatorException, RDFHandlerException, RepositoryException{
        RdfReader reader =  RdfFactory.getMemory();
        reader.loadURI("https://github.com/openphacts/Validator/blob/Christian/MetaData/test-data/testSimple.ttl");
        for (RDFFormat format:RDFFormat.values() ){
            Reporter.println(format.toString());
            Reporter.println("  " + format.getDefaultMIMEType());
            //reader.write(format, System.out);
            //Reporter.println();
            //Reporter.println("**** " + format);            
       }
    }

    private List<Statement> mergeStatementListsIgnoringContext(List<Statement> fullList, List<Statement> extraStatements) {
        if (fullList.isEmpty()){
            return extraStatements;
        }
        for (Statement extraStatement:extraStatements){
            boolean found = false;
            for (Statement existing:fullList){
                if (existing.getSubject().equals(extraStatement.getSubject())){
                    if (existing.getPredicate().equals(extraStatement.getPredicate())){
                        if (existing.getObject().equals(extraStatement.getObject())){
                            found = true;
                        }
                    }
                }
            }
            if (!found){
               fullList.add(extraStatement);
            }
        }
        return fullList;
    }

    @Override
    public List<Statement> getStatementList(Resource resource) throws VoidValidatorException {
        List<Statement> results = this.getLocalStatementList(resource, null, null);
        results.addAll(this.getLocalStatementList(null, null, resource));
        return results;
    }

 
}
