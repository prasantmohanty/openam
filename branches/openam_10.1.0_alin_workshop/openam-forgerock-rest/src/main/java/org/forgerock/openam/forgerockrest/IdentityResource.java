/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2012 ForgeRock Inc.
 */
package org.forgerock.openam.forgerockrest;

import java.lang.Exception;
import java.lang.Object;
import java.lang.String;
import java.util.*;


import com.iplanet.am.util.SystemProperties;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idsvcs.*;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.*;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import static org.forgerock.openam.forgerockrest.RestUtils.getCookieFromServerContext;

import org.forgerock.json.resource.servlet.HttpContext;


/**
 * A simple {@code Map} based collection resource provider.
 */
public final class IdentityResource implements CollectionResourceProvider {
    // TODO: filters, sorting, paged results.


    private final List<Attribute> idSvcsAttrList = new ArrayList();
    private String realm = null;
    private String userType = null;


    /**
     * Creates a backend
     */
    public IdentityResource(String userType, String realm) {
        String[] userval = {userType};
        String[] realmval = {realm};
        this.realm = realm;
        this.userType = userType;
        idSvcsAttrList.add(new Attribute("objecttype", userval));
        idSvcsAttrList.add(new Attribute("realm", realmval));
    }

    private void idFromSession(final ServerContext context, final ActionRequest request,
                               final ResultHandler<JsonValue> handler) {

        //This allows a lookup of a user stored to a particular with only session given
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));
        SSOToken ssotok = null;
        AMIdentity amIdentity = null;

        try {
            SSOTokenManager mgr = SSOTokenManager.getInstance();
            ssotok = mgr.createSSOToken(getCookieFromServerContext(context));
            amIdentity = new AMIdentity(ssotok);

            //build resource
            result.put("id", amIdentity.getName());
            result.put("realm", com.sun.identity.sm.DNMapper.orgNameToRealmName(amIdentity.getRealm()));
            result.put("dn", amIdentity.getUniversalId());
            handler.handleResult(result);

        } catch (SSOException e) {
            RestDispatcher.debug.error("IdentityResource.idFromSession() :: Cannot retrieve SSO Token: " + e);
            handler.handleError(new ForbiddenException("SSO Token cannot be retrieved.", e));
        } catch (IdRepoException ex) {
            RestDispatcher.debug.error("IdentityResource.idFromSession() :: Cannot retrieve user from IdRepo" + ex);
            handler.handleError(new ForbiddenException("Cannot retrieve id from session.", ex));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(final ServerContext context, final ActionRequest request,
                                 final ResultHandler<JsonValue> handler) {

        final String action = request.getActionId();

        if (action.equalsIgnoreCase("idFromSession")) {
            idFromSession(context, request, handler);
        } else { //for now this is the only case coming in, so fail if otherwise
            final ResourceException e =
                    new NotSupportedException("Actions are not supported for resource instances");
            handler.handleError(e);
        }


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(final ServerContext context, final String resourceId, final ActionRequest request,
                               final ResultHandler<JsonValue> handler) {
        final ResourceException e =
                new NotSupportedException("Actions are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createInstance(final ServerContext context, final CreateRequest request,
                               final ResultHandler<Resource> handler) {
        //anyone can create an account add
        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        final JsonValue jVal = request.getContent();
        IdentityDetails dtls = null, identity = null;
        Resource resource = null;
        IdentityServicesImpl idsvc = null;
        String resourceId = null;

        try {
            idsvc = new IdentityServicesImpl();
            identity = jsonValueToIdentityDetails(jVal);
            resourceId = identity.getName();

            //Create the resource
            CreateResponse success = idsvc.create(identity, admin);
            //Read created resource
            dtls = idsvc.read(identity.getName(), idSvcsAttrList, admin);

            resource = new Resource(identity.getName(), "0", identityDetailsToJsonValue(dtls));
            handler.handleResult(resource);
        } catch (final ObjectNotFound notFound) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot READ " +
                    resourceId + ": Resource cannot be found." + notFound);
            handler.handleError(new NotFoundException("Resource not found.", notFound));
        } catch (final DuplicateObject duplicateObject) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ": Resource already exists!" + duplicateObject);
            handler.handleError(new NotFoundException("Resource already exists", duplicateObject));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new ForbiddenException("Token is expired", tokenExpired));
        } catch (final NeedMoreCredentials needMoreCredentials) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE " +
                    needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.createInstance() :: Cannot CREATE! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(final ServerContext context, final String resourceId, final DeleteRequest request,
                               final ResultHandler<Resource> handler) {
        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        JsonValue result = null;
        Resource resource = null;
        IdentityDetails dtls = null;
        IdentityServicesImpl idsvc = null;

        try {
            result = new JsonValue(new LinkedHashMap<String, Object>(1));
            idsvc = new IdentityServicesImpl();

            //read to see if resource is available to user
            dtls = idsvc.read(resourceId, idSvcsAttrList, admin);

            //delete the resource
            DeleteResponse success = idsvc.delete(dtls, admin);

            result.put("success", "true");
            resource = new Resource(resourceId, "0", result);
            handler.handleResult(resource);

        } catch (final NeedMoreCredentials ex) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ": User does not have enough privileges.");
            handler.handleError(new ForbiddenException(resourceId, ex));
        } catch (final ObjectNotFound notFound) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + notFound);
            handler.handleError(new NotFoundException("Resource cannot be found.", notFound));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new ForbiddenException("Token is expired", tokenExpired));
        } catch (final GeneralFailure generalFailure) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE " +
                    generalFailure.getMessage());
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.deleteInstance() :: Cannot DELETE! " +
                    exception.getMessage());
            result.put("success", "false");
            resource = new Resource(resourceId, "0", result);
            handler.handleResult(resource);
        }
    }

    /**
     * Returns a JsonValue containing appropriate identity details
     *
     * @param details The IdentityDetails of a Resource
     * @return The JsonValue Object
     */
    private JsonValue identityDetailsToJsonValue(IdentityDetails details) {
        JsonValue result = new JsonValue(new LinkedHashMap<String, Object>(1));
        try {
            result.put("name", details.getName());
            result.put("realm", details.getRealm());
            Attribute[] attrs = details.getAttributes();

            for (Attribute aix : attrs) {
                result.put(aix.getName(), aix.getValues());
            }
            return result;
        } catch (final Exception e) {
            throw new JsonValueException(result);
        }
    }

    /**
     * Returns an IdenityDetails from a JsonValue
     *
     * @param jVal The JsonValue Object to be converted
     * @return The IdentityDetails object
     */
    private IdentityDetails jsonValueToIdentityDetails(JsonValue jVal) {

        IdentityDetails identity = new IdentityDetails();
        List<Attribute> identityAttrList = new ArrayList();

        try {
            identity.setType(userType); //set type ex. user
            identity.setRealm(realm); //set realm
            identity.setName(jVal.get("name").asString());//set name from JsonValue object

            try {
                for (String s : jVal.keys()) {
                    JsonValue childValue = jVal.get(s);
                    if (childValue.isString()) {
                        String[] tArray = {childValue.asString()};
                        identityAttrList.add(new Attribute(s, tArray));
                    } else if (childValue.isList()) {
                        ArrayList<String> tList = (ArrayList<String>) childValue.getObject();
                        String[] tArray = tList.toArray(new String[tList.size()]);
                        identityAttrList.add(new Attribute(s, tArray));
                    }
                }
            } catch (Exception e) {
                RestDispatcher.debug.error("IdentityResource.jsonValueToIdentityDetails() :: " +
                        "Cannot Traverse JsonValue" + e);
            }
            Attribute[] attr = identityAttrList.toArray(new Attribute[identityAttrList.size()]);
            identity.setAttributes(attr);

        } catch (final Exception e) {
            RestDispatcher.debug.error("IdentityResource.jsonValueToIdentityDetails() ::" +
                    " Cannot convert JsonValue to IdentityDetials." + e);
            //deal with better exceptions
        }
        return identity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void patchInstance(final ServerContext context, final String resourceId, final PatchRequest request,
                              final ResultHandler<Resource> handler) {
        final ResourceException e = new NotSupportedException("Patch operations are not supported");
        handler.handleError(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(final ServerContext context, final QueryRequest request,
                                final QueryResultHandler handler) {

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));


        String queryFilter = null;

        try {
            //This will only return 1 user..
            //getQueryFilter() is not implemented yet..returns dummy false value
            queryFilter = request.getQueryId();
            if (queryFilter == null || queryFilter.isEmpty()) {
                queryFilter = "*";
            }
            IdentityServicesImpl id = new IdentityServicesImpl();
            List<String> users = id.search(queryFilter, idSvcsAttrList, admin);

            for (final String user : users) {
                JsonValue val = new JsonValue(user);
                Resource resource = new Resource(user, "0", val);
                handler.handleResource(resource);
            }
        } catch (Exception ex) {

        }

        handler.handleResult(new QueryResult());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(final ServerContext context, final String resourceId, final ReadRequest request,
                             final ResultHandler<Resource> handler) {

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        IdentityServicesImpl idsvc = null;
        IdentityDetails dtls = null;
        Resource resource = null;

        try {
            idsvc = new IdentityServicesImpl();
            dtls = idsvc.read(resourceId, idSvcsAttrList, admin);
            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(dtls));
            handler.handleResult(resource);
        } catch (final NeedMoreCredentials needMoreCredentials) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + needMoreCredentials);
            handler.handleError(new ForbiddenException("User does not have enough privileges.", needMoreCredentials));
        } catch (final ObjectNotFound objectNotFound) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + objectNotFound);
            handler.handleError(new NotFoundException("Resource cannot be found.", objectNotFound));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new ForbiddenException("Token is expired", tokenExpired));
        } catch (final GeneralFailure generalFailure) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ " +
                    generalFailure);
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        } catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.readInstance() :: Cannot READ! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(final ServerContext context, final String resourceId, final UpdateRequest request,
                               final ResultHandler<Resource> handler) {

        Token admin = new Token();
        admin.setId(getCookieFromServerContext(context));

        final JsonValue jVal = request.getNewContent();
        final String rev = request.getRevision();

        IdentityDetails dtls = null, newDtls = null;
        IdentityServicesImpl idsvc = null;
        Resource resource = null;

        try {
            idsvc = new IdentityServicesImpl();
            dtls = idsvc.read(resourceId, idSvcsAttrList, admin);//Retrieve details about user to be updated
            //Continue modifying the identity if read success

            newDtls = jsonValueToIdentityDetails(jVal);
            newDtls.setName(resourceId);
            //update resource with new details
            UpdateResponse message = idsvc.update(newDtls, admin);
            //read updated identity back to client
            IdentityDetails checkIdent = idsvc.read(dtls.getName(), idSvcsAttrList, admin);
            //handle updated resource
            resource = new Resource(resourceId, "0", identityDetailsToJsonValue(checkIdent));
            handler.handleResult(resource);
        } catch (final ObjectNotFound o) {
            //Create Resource
            try {
                dtls = jsonValueToIdentityDetails(jVal);
                dtls.setName(resourceId);

                //create resource because it does not exist
                CreateResponse success = idsvc.create(dtls, admin);
                //check created identity
                IdentityDetails checkIdent = idsvc.read(dtls.getName(), idSvcsAttrList, admin);
                //Send client back resource created response
                resource = new Resource(resourceId, "0", identityDetailsToJsonValue(checkIdent));
                handler.handleResult(resource);
            } catch (final TokenExpired tokenExpired) {
                RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot CREATE " +
                        resourceId + ":" + tokenExpired);
                handler.handleError(new ForbiddenException("Token is expired", tokenExpired));
            } catch (final Exception e) {
                RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " + e);
            }
        } catch (final NeedMoreCredentials needMoreCredentials) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + needMoreCredentials);
            handler.handleError(new ForbiddenException("Token is not authorized", needMoreCredentials));
        } catch (final TokenExpired tokenExpired) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    resourceId + ":" + tokenExpired);
            handler.handleError(new ForbiddenException("Token is expired", tokenExpired));
        } catch (final GeneralFailure generalFailure) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE " +
                    generalFailure);
            handler.handleError(new BadRequestException(generalFailure.getMessage(), generalFailure));
        }  catch (final Exception exception) {
            RestDispatcher.debug.error("IdentityResource.updateInstance() :: Cannot UPDATE! " +
                    exception);
            handler.handleError(new NotFoundException(exception.getMessage(), exception));
        }
    }

}