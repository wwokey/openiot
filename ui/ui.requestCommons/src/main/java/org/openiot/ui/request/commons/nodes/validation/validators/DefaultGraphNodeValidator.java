/*******************************************************************************
 * Copyright (c) 2011-2014, OpenIoT
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it either under the terms of the GNU Lesser General Public
 *  License version 2.1 as published by the Free Software Foundation
 *  (the "LGPL"). If you do not alter this
 *  notice, a recipient may use your version of this file under the LGPL.
 *  
 *  You should have received a copy of the LGPL along with this library
 *  in the file COPYING-LGPL-2.1; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *  
 *  This software is distributed on an "AS IS" basis, WITHOUT WARRANTY
 *  OF ANY KIND, either express or implied. See the LGPL  for
 *  the specific language governing rights and limitations.
 *  
 *  Contact: OpenIoT mailto: info@openiot.eu
 ******************************************************************************/
package org.openiot.ui.request.commons.nodes.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import org.openiot.ui.request.commons.interfaces.GraphModel;
import org.openiot.ui.request.commons.nodes.base.AbstractGraphNodeVisitor;
import org.openiot.ui.request.commons.nodes.enums.EndpointType;
import org.openiot.ui.request.commons.nodes.interfaces.GraphNode;
import org.openiot.ui.request.commons.nodes.interfaces.GraphNodeConnection;
import org.openiot.ui.request.commons.nodes.interfaces.GraphNodeEndpoint;
import org.openiot.ui.request.commons.nodes.interfaces.GraphNodeProperty;
import org.openiot.ui.request.commons.nodes.validation.GraphValidationError;
import org.openiot.ui.request.commons.nodes.validation.GraphValidationWarning;

/**
 *
 * @author archie
 */
public class DefaultGraphNodeValidator extends AbstractGraphNodeVisitor {

    private GraphModel model;
    private List<GraphNode> visitedNodes;
    private Stack<GraphNodeConnection> visitedConnectionGraphStack;
    private List<GraphValidationWarning> validationWarnings;
    private List<GraphValidationError> validationErrors;

    public DefaultGraphNodeValidator(GraphModel model) {
        this.model = model;
        this.visitedConnectionGraphStack = new Stack<GraphNodeConnection>();
        this.visitedNodes = new ArrayList<GraphNode>();
        this.validationErrors = new ArrayList<GraphValidationError>();
        this.validationWarnings = new ArrayList<GraphValidationWarning>();
    }

    public List<GraphValidationWarning> getValidationWarnings() {
        return validationWarnings;
    }

    public List<GraphValidationError> getValidationErrors() {
        return validationErrors;
    }

    public boolean validate() {
        validationErrors.clear();
        validationWarnings.clear();
        visitedConnectionGraphStack.clear();
        visitedNodes.clear();

        // Visit all nodes once
        for (GraphNode node : model.getNodes()) {
            if (!visitedNodes.contains(node)) {
                visitViaReflection(node);
            }
        }

        return validationErrors.isEmpty();
    }

    //-------------------------------------------------------------------------
    // Visitors
    //-------------------------------------------------------------------------
    @Override
    public void defaultVisit(GraphNode node) {
        // Check if we have visited this node before.
        // If so we need to check for closed loops
        if (visitedNodes.contains(node)) {
            boolean detectedCloseLoop = false;

            // Unwrap connection stack to detect origin point
            String elementIdList = "";

            ListIterator<GraphNodeConnection> unwindIt = visitedConnectionGraphStack.listIterator(visitedConnectionGraphStack.size());
            while (unwindIt.hasPrevious()) {
                GraphNodeConnection loopConnection = unwindIt.previous();
                elementIdList += (elementIdList.isEmpty() ? "" : ", ") + loopConnection.getDestinationEndpoint().getUID();
                elementIdList += (elementIdList.isEmpty() ? "" : ", ") + loopConnection.getSourceEndpoint().getUID();

                // Reached this node
                if (loopConnection.getSourceNode().getUID().equals(node.getUID())) {
                    detectedCloseLoop = true;
                    break;
                }
            }

            if (detectedCloseLoop) {
                GraphValidationError error = new GraphValidationError(GraphValidationError.ErrorType.ConnectionsFormClosedLoop, node.getClass().getSimpleName(), "", elementIdList);
                validationErrors.add(error);
            }


            // We dont need to re-validate this node
            return;
        }

        // Add to visited set
        visitedNodes.add(node);

        // Validate node properties
        for (GraphNodeProperty property : node.getPropertyDefinitions()) {
            if (property.isRequired() && node.getPropertyValueMap().get(property.getName()) == null) {
                GraphValidationError error = new GraphValidationError(GraphValidationError.ErrorType.RequiredPropertyMissing, node.getClass().getSimpleName(), property.getName(), node.getUID());
                validationErrors.add(error);
            }
        }

        // Validate endpoint connections
        boolean noConnections = true;
        for (GraphNodeEndpoint endpoint : node.getEndpointDefinitions()) {
            if (endpoint.getType().equals(EndpointType.Input)) {
                List<GraphNodeConnection> incomingConnections = model.findGraphEndpointConnections(endpoint);

                if (!incomingConnections.isEmpty()) {
                    noConnections = false;
                }

                if (endpoint.isRequired() && incomingConnections.isEmpty()) {
                    GraphValidationError error = new GraphValidationError(GraphValidationError.ErrorType.RequiredEndpointNotConnected, node.getClass().getSimpleName(), "", endpoint.getUID());
                    validationErrors.add(error);
                }
            } else {
                List<GraphNodeConnection> outgoingConnections = model.findGraphEndpointConnections(endpoint);

                if (!outgoingConnections.isEmpty()) {
                    noConnections = false;
                }

                if (endpoint.isRequired() && outgoingConnections.isEmpty()) {
                    GraphValidationError error = new GraphValidationError(GraphValidationError.ErrorType.RequiredEndpointNotConnected, node.getClass().getSimpleName(), "", endpoint.getUID());
                    validationErrors.add(error);
                }

                // Visit outgoing connection targets
                for (GraphNodeConnection outgoingConnection : outgoingConnections) {
                    visitedConnectionGraphStack.push(outgoingConnection);
                    visitViaReflection(outgoingConnection.getDestinationNode());
                    visitedConnectionGraphStack.pop();
                }
            }
        }

        // Check for unconnected nodes
        if (noConnections) {
            GraphValidationWarning warning = new GraphValidationWarning(GraphValidationWarning.WarningType.NodeHasNoConnections, node.getClass().getSimpleName(), "", node.getUID());
            validationWarnings.add(warning);
        }
    }
}
