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
package uk.ac.manchester.cs.datadesc.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import uk.ac.manchester.cs.datadesc.validator.constants.RdfConstants;
import uk.ac.manchester.cs.datadesc.validator.constants.VoidConstants;
import uk.ac.manchester.cs.datadesc.validator.metadata.MetaDataSpecification;
import uk.ac.manchester.cs.datadesc.validator.metadata.ResourceMetaData;
import uk.ac.manchester.cs.datadesc.validator.rdftools.RdfInterface;
import uk.ac.manchester.cs.datadesc.validator.rdftools.VoidValidatorException;

/**
 *
 * @author Christian
 */
public class RdfValidator {
    
    private final RdfInterface reader;
    private final Resource context;
    private final MetaDataSpecification specifications;
    private final StringBuilder builder;
    private final Set<Resource> resourcesToCheck;
    private final Set<Resource> resourcesChecked;
    private final Set<Resource> extraResourcesToCheck;
 
    public static String FAILED = "Validation Failed!";
    public static String SUCCESS = "Validation Successfull!";
    
    static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RdfValidator.class);
    
    public static String validate(RdfInterface reader, Resource context, MetaDataSpecification specifications, 
            Boolean includeWarning) throws VoidValidatorException{
        RdfValidator validator = new RdfValidator(reader, context, specifications);
        validator.validate(includeWarning);
        String result = validator.builder.toString();
        reader.close();
        return result;
    }
    
    public void addResourceToValidate(Resource resource){
        if (resourcesToCheck.contains(resource)){
            return;
        }
        if (resourcesChecked.contains(resource)){
            return;
        }
        extraResourcesToCheck.add(resource);
    }
    
    private RdfValidator(RdfInterface reader, Resource context, MetaDataSpecification specifications) throws VoidValidatorException{
        this.reader = reader;
        this.context = context;
        this.specifications = specifications;
        builder = new StringBuilder();
        resourcesToCheck = getResourcesToCheck();
        resourcesChecked = new HashSet<Resource>();
        extraResourcesToCheck = new HashSet<Resource>();
    }
    
    private Set<Resource> getResourcesToCheck() throws VoidValidatorException {
        HashSet<Resource> resourcesToCheck = new HashSet<Resource>();
        List<Statement> typeStatements = reader.getDirectOnlyStatementList(null, RdfConstants.TYPE_URI, null, context);
        for (Statement typeStatement:typeStatements){
            resourcesToCheck.add(typeStatement.getSubject());
        }
        return resourcesToCheck;
   }
    
    private void validate(Boolean includeWarning) throws VoidValidatorException{
        if (includeWarning == null){
            includeWarning = true;
        }
        boolean error = false;
        List<Statement> inDataSetStatements = reader.getDirectOnlyStatementList(null, VoidConstants.IN_DATASET, null, context);
        for (Statement inDataSetStatement:inDataSetStatements){
            Value object = inDataSetStatement.getObject();
            if (object instanceof Resource){
                resourcesToCheck.add((Resource) object);
            } else {
                builder.append("ERROR none resource obkect for predicate ");
                builder.append(VoidConstants.IN_DATASET);
                builder.append("\n\tIN: ");
                builder.append(inDataSetStatement);
            }
        }
        while (!resourcesToCheck.isEmpty()){
            for (Resource resource:resourcesToCheck){
                 if (appendValidate(resource, includeWarning)){
                    error = true;
                }
            }
            resourcesToCheck.addAll(extraResourcesToCheck);
            extraResourcesToCheck.clear();
            resourcesToCheck.removeAll(resourcesChecked);
        }
        if (error){
            builder.append(FAILED);
        } else {
            builder.append(SUCCESS);            
        }
    }
    

    private boolean appendValidate(Resource resource, boolean includeWarning) throws VoidValidatorException{
        this.resourcesChecked.add(resource);
        List<Statement> typeStatements = reader.getStatementList(resource, RdfConstants.TYPE_URI, null, context);
        boolean error = false;
        boolean unknownType = true;
        for (Statement typeStatement:typeStatements){
            ResourceMetaData specs = specifications.getResourceMetaData((URI) typeStatement.getObject());
            if (specs != null){
                if (specs.appendValidate(builder, reader, typeStatement.getSubject(), context, includeWarning, 0, this)){
                    error = true;
                }
                unknownType = false;
            } 
        }
        if (unknownType){
            builder.append("Unable to validate ");
            builder.append(resource);
            builder.append(" as has no known type. \n");
            error = false;
        }
        return error;
    }

 }
