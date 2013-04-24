/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.cs.metadata;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import uk.ac.manchester.cs.rdftools.RdfInterface;
import uk.ac.manchester.cs.rdftools.VoidValidatorException;

/**
 *
 * @author Christian
 */
abstract class MetaDataBase {
    public static int NO_CARDINALITY = -1;
    String name;

    abstract boolean appendValidate(StringBuilder builder, RdfInterface rdf, Resource resource, Resource context, boolean includeWarnings, 
            int tabLevel)  throws VoidValidatorException;
    
    abstract boolean appendError(StringBuilder builder, RdfInterface rdf, Resource resource, Resource context, int tabLevel)
            throws VoidValidatorException;

    abstract void appendRequirement(StringBuilder builder, RdfInterface rdf, Resource resource, Resource context, int tabLevel) 
            throws VoidValidatorException;

    abstract boolean hasRequiredValues(RdfInterface rdf, Resource resource, Resource context) throws VoidValidatorException;

    abstract boolean isValid(RdfInterface rdf, Resource resource, Resource context) throws VoidValidatorException;
 
    final void addValue(StringBuilder builder, Value value, Resource context){
        if (value instanceof URI){
           URI uri = (URI)value;
           if (uri.getNamespace().startsWith(context.stringValue())){
               builder.append(uri.getLocalName());
           } else {
               builder.append("<");
               builder.append(uri.stringValue());
               builder.append(">");
           }
        } else{
            builder.append(value);        
        }
    }
    
    final void addStatement(StringBuilder builder, Statement statement, Resource context){
        addValue(builder, statement.getSubject(), context);
        builder.append(" ");
        addValue(builder, statement.getPredicate(), context);
        builder.append(" ");
        addValue(builder, statement.getObject(), context);
    }
    
    final void tab(StringBuilder builder, int tabLevel){
        for (int i = 0; i < tabLevel; i++){
            builder.append("\t");
        }
    }


}
