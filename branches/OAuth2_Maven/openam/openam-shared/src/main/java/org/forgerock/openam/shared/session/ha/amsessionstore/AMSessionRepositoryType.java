/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS Inc. All Rights Reserved
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
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Portions Copyrighted [2010-2012] [ForgeRock AS]
 *
 */
package org.forgerock.openam.shared.session.ha.amsessionstore;

import java.util.*;


/**
 * AM Session Repository Type, provides Naming, Description and Implementation ClassName for
 * the various Types of AMSessionRepository Types.
 *
 * @author jeff.schenk@forgerock.com
 */
public enum AMSessionRepositoryType {

    /**
     * Enumerator Values
     */
    NONE(0, 0, "none", "None", "Session Failover High Availability Disabled", "session.store.type.none", null),

    CONFIG(1, 1, "config", "Config", "OpenAM Configuration Directory", "session.store.type.config",
            "org.forgerock.openam.session.ha.amsessionstore.store.opendj.OpenDJPersistentStore");

    /**
     * Index
     */
    private final int index;
    /**
     * Text for Type
     */
    private final String type;
    /**
     * Text for Type
     */
    private final String displayType;
    /**
     * Display Order
     */
    private final int displayOrder;
    /**
     * Long Text For Type
     */
    private final String textDefinition;
    /**
     * Associated AMSessionRepository Implementation Class.
     */
    private final String amSessionRepositoryImplementationClassName;

    /**
     * Index Entry
     * @return int of Enum
     */
    public int index() {
        return this.index;
    }
    /**
     * Index Entry
     * @return int of Enum
     */
    public String amSessionRepositoryImplementationClassName() {
        return this.amSessionRepositoryImplementationClassName;
    }
    /**
     * Index Entry
     * @return int of Enum
     */
    public String type() {
        return this.type;
    }
    /**
     * Display Type
     * @return String of Display Type
     */
    public String displayType() {
        return this.displayType;
    }
    /**
     * Text Definition
     * @return String of Textual Definition
     */
    public String textDefinition() {
        return this.textDefinition;
    }

    /**
     * Private, internally used constructor.
     *
     * @param index
     * @param type
     * @param textDefinition
     */
    private AMSessionRepositoryType(int index, int displayOrder, String type, String displayType, String textDefinition,
                                    String i18nKeyName, String amSessionRepositoryImplementationClassName) {
        this.index = index;
        this.displayOrder = displayOrder;
        this.type = type;
        this.displayType = displayType;
        this.textDefinition = textDefinition;

        this.amSessionRepositoryImplementationClassName = amSessionRepositoryImplementationClassName;
    }

    /**
     * Obtain a Map of the Various Types.
     * @return Map<String, String>
     */
    public static Map<String, String> getAMSessionRepositoryTypes() {
        Map<String, String> typeMap = new TreeMap<String, String>();
        for (AMSessionRepositoryType type : AMSessionRepositoryType.values()) {
            typeMap.put(String.valueOf(type.type), type.textDefinition);
        }
        return typeMap;
    }

    /**
     * Obtain the Am Session Repository Type Textual Information.
     *
     * @param type
     * @return String
     */
    public static String getAMSessionRepositoryTypeText(String type) {
        for (AMSessionRepositoryType amSessionRepositoryType : AMSessionRepositoryType.values()) {
            if (amSessionRepositoryType.type().equalsIgnoreCase(type)) {
                return amSessionRepositoryType.textDefinition;
            } else if (amSessionRepositoryType.displayType.equalsIgnoreCase(type)) {
                return amSessionRepositoryType.textDefinition;
            }
        }
        return null;
    }

    /**
     * Obtain the AM Session Repository Implementation Class.
     *
     * @param type
     * @return String
     */
    public static String getAMSessionRepositoryTypeImplementationClass(String type) {
        for (AMSessionRepositoryType amSessionRepositoryType : AMSessionRepositoryType.values()) {
            if (amSessionRepositoryType.type().equalsIgnoreCase(type)) {
                return amSessionRepositoryType.amSessionRepositoryImplementationClassName;
            }  else if (amSessionRepositoryType.displayType.equalsIgnoreCase(type)) {
                return amSessionRepositoryType.amSessionRepositoryImplementationClassName;
            }
        }
        return null;
    }

}
