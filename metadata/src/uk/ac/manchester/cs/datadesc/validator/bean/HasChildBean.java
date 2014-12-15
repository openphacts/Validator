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
package uk.ac.manchester.cs.datadesc.validator.bean;

import java.util.HashSet;
import java.util.Set;
import org.codehaus.jackson.annotate.JsonTypeName;
import uk.ac.manchester.cs.datadesc.validator.metadata.HasChildrenMetaData;
import uk.ac.manchester.cs.datadesc.validator.metadata.MetaDataBase;
import uk.ac.manchester.cs.datadesc.validator.rdftools.VoidValidatorException;

/**
 *
 * @author Christian
 */
@JsonTypeName(Names.GROUPS_NAME)
class HasChildBean extends ValidatorBean{
    
    private Set<ValidatorBean> requirements = new HashSet<ValidatorBean>();

    HasChildBean(){
    }
        
    HasChildBean(HasChildrenMetaData metaData) throws VoidValidatorException {
        for (MetaDataBase child: metaData.getChildren()){
            ValidatorBean childBean = ValidatorBean.convertToBean(child);
            requirements.add(childBean);
        }
    }

    /**
     * @return the requirements
     */
    public Set<ValidatorBean> getRequirements() {
        return requirements;
    }

    /**
     * @param requirements the requirements to set
     */
    public void setRequirements(Set<ValidatorBean> requirements) {
        this.requirements = requirements;
    }
}