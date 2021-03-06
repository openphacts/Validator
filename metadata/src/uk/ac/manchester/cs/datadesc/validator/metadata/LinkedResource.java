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
package uk.ac.manchester.cs.datadesc.validator.metadata;

import java.util.List;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import uk.ac.manchester.cs.datadesc.validator.RdfValidator;
import uk.ac.manchester.cs.datadesc.validator.constants.RdfConstants;
import uk.ac.manchester.cs.datadesc.validator.rdftools.RdfInterface;
import uk.ac.manchester.cs.datadesc.validator.rdftools.VoidValidatorException;

/**
 *
 * @author Christian
 */
@XmlRootElement(name="Linked Resource")
public class LinkedResource extends CardinalityMetaData {
    
    private final Set<URI> linkedTypes;
    private final MetaDataSpecification metaDataSpecification;
  
    public static final String ERROR_SEE_REPORT = " has errors! See report for that Resource! ";
    public static final String NO_KNOWN_TYPE = " has not been typed, and does not meet the requirements of any known type. ";
   
    public LinkedResource(URI predicate, int cardinality, RequirementLevel requirementLevel, 
            Set<URI> linkedTypes, MetaDataSpecification metaDataSpecification) {
       super(predicate, cardinality, requirementLevel);
       this.linkedTypes = linkedTypes;
       this.metaDataSpecification = metaDataSpecification;
    }

     @Override
     protected boolean appendIncorrectReport(StringBuilder builder, RdfInterface rdf, List<Statement> statements, 
            Resource context, int tabLevel, RdfValidator validator) throws VoidValidatorException {
        boolean appended = false;
        for (Statement statement:statements){
            if (statement.getObject() instanceof Resource){
                Resource linkedResource = (Resource)statement.getObject();
                boolean unknownType = true;
                List<Statement> typeStatements = rdf.getStatementList(linkedResource, RdfConstants.TYPE_URI, null, statement.getContext());
                for (Statement typeStatement: typeStatements){
                    if (typeStatement.getObject() instanceof URI){
                        URI linkedType = (URI)typeStatement.getObject();
                        if (getLinkedTypes().contains(linkedType)){
                            if (!this.isValid(rdf, linkedResource, context, linkedType)){
                                appendInvalidLinked(builder, statement, context, tabLevel);
                                validator.addResourceToValidate(linkedResource);
                                appended = true;
                            }
                            unknownType = false;
                        }
                    } else {
                        appendIncorretTypeStatement(builder, typeStatement, context, tabLevel);
                    }
                }
                if (unknownType){
                    for (URI possibleType: getLinkedTypes()){
                        if (unknownType && isValid(rdf, linkedResource, context, possibleType)){
                            unknownType = false;
                        }
                    }
                    if (unknownType){
                        appendNoKnownType(builder, statement, context, tabLevel);
                        appended = true;
                    }
                }
             } else {
                appendNotAResource(builder, statement, context, tabLevel);
                appended = true;
             }
        }
        return appended;
    }

    @Override
    protected String getType() {
        return getLinkedTypes().toString();
    }

    @Override
    boolean isValid(RdfInterface rdf, Resource resource, Resource context) throws VoidValidatorException {
        List<Statement> statements = rdf.getStatementList(resource, RdfConstants.TYPE_URI, null, context);
        if (!correctCardinality(statements)){
            return false;
        }
        for (Statement statement: statements){
            URI linkedType = (URI)statement.getSubject();
            if (getLinkedTypes().contains(statement.getSubject())){
                return isValid(rdf, resource, context, linkedType);
            }
        }
        for (URI linkedType: getLinkedTypes()){
            return isValid(rdf, resource, context, linkedType);
        }
        return false;
    }

    private boolean isValid(RdfInterface rdf, Resource linkedResource, Resource context, Resource linkedType) throws VoidValidatorException {
        ResourceMetaData resourceMetaData = metaDataSpecification.getResourceMetaData(linkedType);     
        if (resourceMetaData == null){
            throw new VoidValidatorException ("Unable to ResourceMetaData for " + linkedType);
        }
        return resourceMetaData.isValid(rdf, linkedResource, context);
    }

   private void appendErrorStart(StringBuilder builder, Statement statement, Resource context, int tabLevel) {
        tab(builder, tabLevel);
        builder.append("ERROR: Found: ");
        this.addStatement(builder, statement, context);
        builder.append("\n");            
        tab(builder, tabLevel+1);
    }
   
    private void appendNotAResource(StringBuilder builder, Statement statement, Resource context, int tabLevel) {
        appendErrorStart(builder, statement, context, tabLevel);
        builder.append("Object must be a URI of type: ");            
        builder.append(getType());
        builder.append("\n");
    }

    private void appendInvalidLinked(StringBuilder builder, Statement statement, Resource context, int tabLevel) {
        appendErrorStart(builder, statement, context, tabLevel);
        builder.append(statement.getObject());
        builder.append(ERROR_SEE_REPORT);            
        builder.append("\n");
    }

    private void appendNoKnownType(StringBuilder builder, Statement statement, Resource context, int tabLevel) {
        appendErrorStart(builder, statement, context, tabLevel);
        builder.append(statement.getObject());
        builder.append(NO_KNOWN_TYPE);            
        builder.append("\n");    
    }

    private void appendIncorretTypeStatement(StringBuilder builder, Statement typeStatement, Resource context, int tabLevel) {
        appendErrorStart(builder, typeStatement, context, tabLevel);
        builder.append("Object of this tye statement is not a URI!. ");            
        builder.append("\n");    
    }

    @Override
    void describe(StringBuilder builder, int tabLevel) {
        describeCardinality(builder, tabLevel);
        for (URI linkedType:getLinkedTypes()){
            builder.append("\n");
            tab(builder, tabLevel+1);
            builder.append(linkedType);
        }
    }

    /**
     * @return the linkedTypes
     */
    public Set<URI> getLinkedTypes() {
        return linkedTypes;
    }

 }
