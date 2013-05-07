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
package uk.ac.manchester.cs.openphacts.valdator.metadata;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyRange;
import org.semanticweb.owlapi.model.OWLQuantifiedRestriction;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import uk.ac.manchester.cs.openphacts.valdator.rdftools.VoidValidatorException;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLClassAssertionImpl;

/**
 *
 * @author Christian
 */
public class MetaDataSpecification {
    private OWLOntology ontology;
    
    private Map<Resource, ResourceMetaData> resourcesByType = new HashMap<Resource, ResourceMetaData>();
   // Map<Resource, ResourceMetaData> resourcesById = new HashMap<Resource, ResourceMetaData>();
    //   static String documentationRoot = "";
    private static String THING_ID = "http://www.w3.org/2002/07/owl#Thing";
//    private final Set<URI> linkingPredicates;    
    private String description;
    
    public MetaDataSpecification(String fileName) throws VoidValidatorException{
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        File file = new File(fileName);
        try {
            ontology = m.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException ex) {
            throw new VoidValidatorException("Unable to read owl from inputStream", ex);
        }
        init();
        description = "Read from " + fileName;
    }
    
    public MetaDataSpecification(InputStream stream, String source) throws VoidValidatorException{
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
        try {
            ontology = m.loadOntologyFromOntologyDocument(stream);
        } catch (OWLOntologyCreationException ex) {
            throw new VoidValidatorException("Unable to read owl from inputStream", ex);
        }
        init();
        description = "Read from " + source;
    }
     
    private void init() throws VoidValidatorException{
        Map<URI,Map<OWLClassExpression,RequirementLevel>> requirements = extractRequirements();
 //       linkingPredicates = new HashSet<URI>();
        loadSpecification(requirements);        
    }
    //private Set<URI>
    public ResourceMetaData getResourceMetaData(Resource type){
        return resourcesByType.get(type);
    }
            
    private Map<URI,Map<OWLClassExpression,RequirementLevel>> extractRequirements() throws VoidValidatorException{
        Map<URI,Map<OWLClassExpression,RequirementLevel>> requirements = 
                new HashMap<URI,Map<OWLClassExpression,RequirementLevel>>();
        Set<OWLAxiom> axioms = ontology.getAxioms();
        for (OWLAxiom axiom:axioms){
            if (axiom instanceof OWLSubClassOfAxiom){
                RequirementLevel requirementLevel = null;
                URI type;
                OWLSubClassOfAxiom holder = (OWLSubClassOfAxiom)axiom;
                OWLClassExpression expression = holder.getSuperClass();
                if (expression.isAnonymous()){
                    OWLClassExpression sub = holder.getSubClass();
                    if (sub.isAnonymous()){
                        throw new VoidValidatorException("subClass " + sub + " + is not an OWLClass in " + axiom);
                    } else {
                        OWLClass theClass = sub.asOWLClass();
                        type = new URIImpl(theClass.toStringID());
                    }
                    requirementLevel = extractRequirementLevel(axiom, type);
                    Map<OWLClassExpression,RequirementLevel> inner = requirements.get(type);
                    if (inner == null){
                        inner = new HashMap<OWLClassExpression,RequirementLevel>();
                    }
                    inner.put(expression, requirementLevel);
                    requirements.put(type, inner);
                } else {
                    //Class subclass class statement.
                }
            } else if (axiom instanceof OWLDeclarationAxiom){
                //ok do nothing
            } else if (axiom instanceof OWLAnnotationAssertionAxiom){
                //ok do nothing                
            } else if (axiom instanceof OWLSubObjectPropertyOfAxiom){
                //ok do nothing
            } else if (axiom instanceof OWLClassAssertionImpl){
                //ok do nothing
            } else if (axiom instanceof OWLSubAnnotationPropertyOfAxiom){
                //ok do nothing      
            } else {
                throw new VoidValidatorException ("Unexpected axiom type " + axiom.getClass() + " in " + axiom);
            }
        }
 
        return requirements;
    }
    
    private RequirementLevel extractRequirementLevel(OWLAxiom axiom, URI type) throws VoidValidatorException{
        RequirementLevel requirementLevel = null;
        Set<OWLAnnotation> annotations = axiom.getAnnotations();
        if (annotations.size() > 0){
            for (OWLAnnotation annotation:annotations){
                 OWLAnnotationValue value = annotation.getValue();
                if (requirementLevel == null){
                    requirementLevel = RequirementLevel.parseString(value.toString());
                } else {
                    RequirementLevel newRequirementLevel = RequirementLevel.parseString(value.toString());
                    if (newRequirementLevel != null){
                        throw new VoidValidatorException ("Two different values found in " + axiom);
                    }
                }
            }
            if (requirementLevel == null){
                throw new VoidValidatorException ("No requirement level annotaion found in " + axiom);
            }
        } else {
            throw new VoidValidatorException ("No annotaions found in " + axiom);
        } 
        return requirementLevel;
    }
    
    private void loadSpecification(Map<URI,Map<OWLClassExpression,RequirementLevel>> requirements) throws VoidValidatorException{
        Set<URI> types = requirements.keySet();
        for (URI type:types){
            List<MetaDataBase> childMetaData = new ArrayList<MetaDataBase>();
            Map<OWLClassExpression,RequirementLevel> inner = requirements.get(type);
            for (OWLClassExpression expr: inner.keySet()){
                MetaDataBase child = parseExpression(expr, type.getLocalName(), inner.get(expr));
                childMetaData.add(child);
            }
            //ystem.out.println(theClass);
            ResourceMetaData resourceMetaData = new ResourceMetaData(type, childMetaData);
            resourcesByType.put(type, resourceMetaData);
        }
    }
    
    private URI toURI(OWLObject object) throws VoidValidatorException{
        OWLEntity entity;
        if (object instanceof OWLEntity){
            entity = (OWLEntity)object;
        } else {
            Set<OWLEntity> signature = object.getSignature();
            if (signature.size() != 1){
                throw new VoidValidatorException ("Object " + object + " has unexpected signature " + signature);
            }
            entity = signature.iterator().next();
        }
        String id = entity.toStringID();
        return new URIImpl(id);
    }
    
   private MetaDataBase parseExpression(OWLClassExpression expr, String type, RequirementLevel requirementLevel) 
            throws VoidValidatorException {
        if (expr instanceof OWLQuantifiedRestriction){
            return parseOWLQuantifiedRestriction ((OWLQuantifiedRestriction) expr, type, requirementLevel);
        }
        if (expr instanceof OWLNaryBooleanClassExpression){
            return parseOWLNaryBooleanClassExpression ((OWLNaryBooleanClassExpression) expr, type, requirementLevel);
        }
        throw new VoidValidatorException("Unexpected expression." + expr + " " + expr.getClass());
    }
        
    private MetaDataBase parseOWLNaryBooleanClassExpression(OWLNaryBooleanClassExpression expression, String type, 
            RequirementLevel requirementLevel) throws VoidValidatorException{
        ArrayList<MetaDataBase> children = new ArrayList<MetaDataBase>();
        Set<OWLClassExpression> operands = expression.getOperands();
        for (OWLClassExpression expr:operands){
            MetaDataBase child = parseExpression(expr, type, requirementLevel);
            children.add(child);
        }
        if (expression instanceof OWLObjectIntersectionOf){
            String name = children.get(0).name;
            for (int i = 1; i < children.size(); i++){
                name = name + " and " + children.get(i).name;
            }
            return new MetaDataGroup(name, type, requirementLevel, children);
        } 
        if (expression instanceof OWLObjectUnionOf){
            String name = children.get(0).name;
            for (int i = 1; i < children.size(); i++){
                name = name + " or " + children.get(i).name;
            }
            return new MetaDataAlternatives(name, type, requirementLevel, children);
        } 
        throw new VoidValidatorException("Unexpected expression." + expression);
    }
    
    private MetaDataBase parseOWLQuantifiedRestriction(OWLQuantifiedRestriction restriction, String type, 
            RequirementLevel requirementLevel) throws VoidValidatorException{
        URI predicate;
        OWLPropertyRange range = restriction.getFiller();
        OWLPropertyExpression owlPropertyExpression = restriction.getProperty();
        predicate = toURI(owlPropertyExpression);
        int cardinality = getCardinality(restriction);
        if (range instanceof OWLClass){
            OWLClass owlClass = (OWLClass)range;
            if (owlClass.isOWLThing()){
                return new PropertyMetaData(predicate, type, cardinality, requirementLevel, range.toString());
            }
            IRI iri = owlClass.getIRI();
 //           ontology.containsClassInSignature(iri);
            //linkingPredicates.add(predicate);
            Set<URI> linkedTypes = new HashSet<URI>();
            linkedTypes.add(new URIImpl(iri.toString()));
            return new LinkedResource(predicate, type, cardinality, requirementLevel, linkedTypes, this);
        } else if (range instanceof OWLObjectUnionOf){
            Set<URI> linkedTypes = new HashSet<URI>();
            OWLObjectUnionOf objectUnionOf = (OWLObjectUnionOf)range;
            for (OWLClassExpression expr:objectUnionOf.getOperands()){
                if (expr instanceof OWLClass){
                    OWLClass owlClass = (OWLClass)expr;
                    IRI iri = owlClass.getIRI();
                    linkedTypes.add(new URIImpl(iri.toString()));       
                } else {
                    linkedTypes.add(new URIImpl(expr.toString()));
                }
            }
            //.add(predicate);
            return new LinkedResource(predicate, type, cardinality, requirementLevel, linkedTypes, this);
        } else {
            return new PropertyMetaData(predicate, type, cardinality, requirementLevel, range.toString());
        }
    }

    private int getCardinality(OWLQuantifiedRestriction restriction){
        if (restriction instanceof OWLCardinalityRestriction){
            OWLCardinalityRestriction card = (OWLCardinalityRestriction)restriction;
            return card.getCardinality();
        }
        return MetaDataBase.NO_CARDINALITY;
    }
       
    //Set<URI> getLinkingPredicates() {
    //    return linkingPredicates;
    //}

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }


}