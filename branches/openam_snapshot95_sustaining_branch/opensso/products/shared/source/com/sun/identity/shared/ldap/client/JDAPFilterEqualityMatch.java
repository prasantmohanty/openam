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

/**
 * This class implements the equality match filter.
 * <pre>
 * equalityMatch [3] AttributeValueAssertion
 * </pre>
 *
 * @version 1.0
 * @see <a href="http://www.faqs.org/rfcs/rfc1777.html">RFC1777</a>
 */
public class JDAPFilterEqualityMatch extends JDAPFilterAVA {
    /**
     * Constructs less or equal filter.
     * @param ava attribute value assertion
     */
    public JDAPFilterEqualityMatch(JDAPAVA ava) {
        super(BERTag.CONSTRUCTED|BERTag.CONTEXT|3, ava);
    }

    /**
     * Retrieves the string representation of the filter.
     * @return string representation
     */
    public String toString() {
        return "JDAPFilterEqualityMatch {" +
          super.getAVA().toString() + "}";
    }
}
