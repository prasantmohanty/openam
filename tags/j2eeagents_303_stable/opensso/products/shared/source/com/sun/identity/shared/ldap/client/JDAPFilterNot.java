/* -*- Mode: C++; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s): 
 */
package com.sun.identity.shared.ldap.client;

import java.util.*;
import com.sun.identity.shared.ldap.ber.stream.*;
import java.io.*;
import com.sun.identity.shared.ldap.LDAPRequestParser;

/**
 * This class implements the filter not.
 * <pre>
 *   not [2] Filter
 * </pre>
 *
 * @version 1.0
 * @see <a href="http://www.faqs.org/rfcs/rfc1777.html">RFC1777</a>
 */
public class JDAPFilterNot extends JDAPFilter {

    /**
     * Internal variables
     */
    private JDAPFilter m_filter = null;

    /**
     * Constructs the filter.
     */
    public JDAPFilterNot(JDAPFilter filter) {
        super();
        m_filter = filter;
    }

    /**
     * Gets ber representation of the filter.
     * @return ber representation
     */
    public BERElement getBERElement() {
        BERTag element = new BERTag(BERTag.CONSTRUCTED|BERTag.CONTEXT|2,
          m_filter.getBERElement(), false /* true */);
        return element;
    }

    public int addLDAPFilter(LinkedList bytesList) {
        int Length = m_filter.addLDAPFilter(bytesList);
        byte[] tempBytes = LDAPRequestParser.getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        byte[] tempTag = new byte[1];
        tempTag[0] = (byte) (BERTag.CONSTRUCTED | BERTag.CONTEXT | 2);
        bytesList.addFirst(tempTag);
        Length++;
        return Length;
    }

    /**
     * Gets string reprensetation of the filter.
     * @return string representation
     */
    public String toString() {
        return "JDAPFilterNot {" + m_filter.toString() + "}";
    }
}
