/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package constraint;

import static org.apache.jena.shacl.lib.ShLib.displayStr;

import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.engine.ValidationContext;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.sparql.path.Path;

/** sh:equals */
public class EqualsConstraint extends ConstraintPairwise {

    public EqualsConstraint(Node property) {
        super(property);
    }

    @Override
    public void validate(ValidationContext vCxt, Shape shape, Node focusNode, Path path,
                         Set<Node> pathNodes, Set<Node> compareNodes) {
        for ( Node vn : pathNodes ) {
            if ( ! compareNodes.contains(vn) ) {
                String msg = toString()+": not disjoint: value node "+displayStr(vn)+" is not in "+compareNodes;
                vCxt.reportEntry(msg, shape, focusNode, path, vn, this);
            }
        }
        for ( Node v : compareNodes ) {
            if ( ! pathNodes.contains(v) ) {
                String msg = toString()+": not disjoint: value "+displayStr(v)+" is not in "+pathNodes;
                vCxt.reportEntry(msg, shape, focusNode, path, v, this);
            }
        }
    }

    @Override
    public Node getComponent() {
        return SHACL.EqualsConstraintComponent;
    }

    @Override
    public String toString() {
        return "Equals["+displayStr(property)+"]";
    }
    
    public Node getProperty() {
    	return property;
    }

}
