/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AMDynamicGroup.java,v 1.4 2008/06/25 05:41:19 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import com.iplanet.sso.SSOException;

/**
 * This interface provides methods to manage dynamic group.
 * <code>AMDynamicGroup</code> objects can be obtained by using
 * <code>AMStoreConnection</code>. A handle to this object can be obtained by
 * using the DN of the object.
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); 
 * if (amsc.doesEntryExist(dgDN)) { 
 *     AMDynamicGroup dg = amsc.getDynamicGroup(dgDN); 
 * }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMDynamicGroup extends AMGroup {
    /**
     * Returns the search filter for the dynamic group.
     * 
     * @return The filter in LDIF format for the dynamic group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public String getFilter() throws AMException, SSOException;

    /**
     * Sets the search filter for the dynamic group.
     * 
     * @param filter
     *            the filter in LDIF format.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void setFilter(String filter) throws AMException, SSOException;
}
