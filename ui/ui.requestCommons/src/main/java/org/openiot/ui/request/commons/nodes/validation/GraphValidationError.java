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
package org.openiot.ui.request.commons.nodes.validation;

/**
 *
 * @author Achilleas Anagnostopoulos (aanag) email: aanag@sensap.eu
 */
public class GraphValidationError {
    public enum ErrorType{
        /**
         * Connections form a closed loop
         */
        ConnectionsFormClosedLoop,
        
        /**
         * A required property is missing
         */
        RequiredPropertyMissing,
        
        /**
         * An endpoint with required connection is not connected
         */
        RequiredEndpointNotConnected
    }
    
    private ErrorType type;
    private String nodeClassName;
    private String details;
    private String elementId;

    public GraphValidationError(ErrorType type, String nodeClassName, String details, String elementId) {
        this.type = type;
        this.nodeClassName = nodeClassName;
        this.details = details;
        this.elementId = elementId;
    }

    public String getNodeClassName() {
        return nodeClassName;
    }

    public String getDetails() {
        return details;
    }
    
    public ErrorType getType() {
        return type;
    }

    public String getElementId() {
        return elementId;
    }
}
