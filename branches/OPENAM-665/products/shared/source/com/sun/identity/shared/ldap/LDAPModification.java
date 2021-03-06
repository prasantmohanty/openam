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
package com.sun.identity.shared.ldap;

import com.sun.identity.shared.ldap.ber.stream.*;

import java.util.LinkedList;

/**
 * Specifies changes to be made to the values of an attribute.  The change is
 * specified in terms of the following aspects:
 * <P>
 *
 * <UL>
 * <LI>the type of modification (add, replace, or delete the value of an attribute)
 * <LI>the type of value being modified (string or binary)
 * <LI>the name of the attribute being modified
 * <LI>the actual value
 * </UL>
 * <P>
 *
 * After you specify a change to an attribute, you can execute the change
 * by calling the <CODE>LDAPConnection.modify</CODE> method and specifying
 * the DN of the entry that you want to modify.
 * <P>
 *
 * @version 1.0
 * @see com.sun.identity.shared.ldap.LDAPConnection#modify(java.lang.String, com.sun.identity.shared.ldap.LDAPModification)
 */
public class LDAPModification implements java.io.Serializable {

    static final long serialVersionUID = 4836472112866826595L;

    /**
     * Specifies that a value should be added to an attribute.
     */
    public static final int ADD     = 0;

    /**
     * Specifies that a value should be removed from an attribute.
     */
    public static final int DELETE  = 1;

    /**
     * Specifies that a value should replace the existing value in an attribute.
     */
    public static final int REPLACE = 2;

    /**
     * Internal variables
     */
    private int operation;
    private LDAPAttribute attribute;

    /**
     * Specifies a modification to be made to an attribute.
     * @param op the type of modification to make. This can be one of the following:
     *   <P>
     *   <UL>
     *   <LI><CODE>LDAPModification.ADD</CODE> (the value should be added to the attribute)
     *   <LI><CODE>LDAPModification.DELETE</CODE> (the value should be removed from the attribute)
     *   <LI><CODE>LDAPModification.REPLACE</CODE> (the value should replace the existing value of the attribute)
     *   </UL><P>
     * @param attr the attribute (possibly with values) to modify
     * @see com.sun.identity.shared.ldap.LDAPAttribute
     */
    public LDAPModification( int op, LDAPAttribute attr ) {
        operation = op;
        attribute = attr;
    }

    /**
     * Returns the type of modification specified by this object.
     * @return one of the following types of modifications:
     *   <P>
     *   <UL>
     *   <LI><CODE>LDAPModification.ADD</CODE> (the value should be added to the attribute)
     *   <LI><CODE>LDAPModification.DELETE</CODE> (the value should be removed from the attribute)
     *   <LI><CODE>LDAPModification.REPLACE</CODE> (the value should replace the existing value of the attribute)
     *   </UL><P>
     */
    public int getOp() {
        return operation;
    }

    /**
     * Returns the attribute (possibly with values) to be modified.
     * @return the attribute to be modified.
     * @see com.sun.identity.shared.ldap.LDAPAttribute
     */
    public LDAPAttribute getAttribute() {
        return attribute;
    }

    /**
     * Retrieves the BER (Basic Encoding Rules) representation
     * of the current modification.
     * @return BER representation of the modification.
     */
    public BERElement getBERElement() {
        BERSequence seq = new BERSequence();
        seq.addElement(new BEREnumerated(operation));
        seq.addElement(attribute.getBERElement());
        return seq;
    }

    protected int addLDAPModification(LinkedList bytesList) {
        byte[] tempBytes;
        int Length = 0;
        if (attribute != null) {
            Length += attribute.addLDAPAttribute(bytesList);
        }
        Length += LDAPRequestParser.addInt(bytesList, operation);
        bytesList.addFirst(BERElement.ENUMERATED_BYTES);
        Length++;
        tempBytes = LDAPRequestParser.getLengthBytes(Length);
        bytesList.addFirst(tempBytes);
        Length += tempBytes.length;
        bytesList.addFirst(BERElement.SEQUENCE_BYTES);
        Length++;
        tempBytes = null;
        return Length;
    }

    /**
     * Retrieves the string representation of the current
     * modification. For example:
     *
     * <PRE>
     * LDAPModification: REPLACE, LDAPAttribute {type='mail', values='babs@example.com'}
     * LDAPModification: ADD, LDAPAttribute {type='description', values='This entry was modified with the modattrs program'}
     * </PRE>
     *
     * @return string representation of the current modification.
     */
    public String toString() {
        String s = "LDAPModification: ";
        if ( operation == ADD )
            s += "ADD, ";
        else if ( operation == DELETE )
            s += "DELETE, ";
        else if ( operation == REPLACE )
            s += "REPLACE, ";
        else
            s += "INVALID OP, ";
        s += attribute;
        return s;
    }
}
