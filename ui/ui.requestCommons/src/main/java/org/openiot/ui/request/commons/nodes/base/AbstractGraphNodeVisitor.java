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
package org.openiot.ui.request.commons.nodes.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.openiot.ui.request.commons.nodes.interfaces.GraphNode;

/**
 *
 * @author archie
 */
public abstract class AbstractGraphNodeVisitor {

    public abstract void defaultVisit(GraphNode node);

    public void visitViaReflection(GraphNode node) {
        try {
            Method downPolymorphic = this.getClass().getMethod("visit", new Class[]{ node.getClass()});

            if (downPolymorphic == null) {
                defaultVisit(node);
            } else {
                downPolymorphic.invoke(this, new Object[]{node});
            }
        } catch (NoSuchMethodException e) {
            defaultVisit(node);
        } catch (InvocationTargetException e) {
        	e.printStackTrace();        	
            defaultVisit(node);
        } catch (IllegalAccessException e) {
            defaultVisit(node);
        }
    }
}
