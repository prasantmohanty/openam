/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMHaDB.java,v 1.6 2009/04/16 15:37:49 subashvarma Exp $
 *
 */
 
package com.sun.identity.ha.jmqdb.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;

import java.io.PrintWriter;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import com.sun.identity.ha.jmqdb.ConnectionFactoryProvider;
import com.sun.identity.ha.jmqdb.ConnectionFactoryProviderFactory;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;

public class FAMHaDB implements Runnable {
    
    static FAMHaDB dbs;
    /* Operations */
    static boolean debug = true;
    static boolean shutdownStatus = false;
    
    static public final String READ = "READ";
    static public final String WRITE = "WRITE";
    static public final String DELETE = "DELETE";
    static public final String DELETEBYDATE = "DELETEBYDATE";
    static public final String SHUTDOWN = "SHUTDOWN";
    static public final String GET_RECORD_COUNT =
        "GET_RECORD_COUNT"; 
    static public final String READ_WITH_SEC_KEY = "READ_WITH_SEC_KEY";

    static public final String NOT_FOUND = "notfound";
    static public final String OP_STATUS = "opstatus";
    
    /* JMQ Queue/Topic names */
    static public final String DBREQUEST = "AM_DBREQUEST";
    static public final String DBRESPONSE = "AM_DRESPONSE";
    static public final String DBNODESTATUS = "AM_DBNODE_STATUS";

    /* JMQ Message attrs */
    static public final String SESSIONID = "SessionId";
    static public final String EXPIRYDATE = "ExpiryDate";
    static public final String DATA = "Data";
    static public final String RANDOM = "RANDOM";

    /* JMQ Properties */
    static public final String ID = "ID";

    // Private data members
    private String _id;

    TopicConnectionFactory tFactory = null;
    TopicConnection tConn = null;
    TopicSession tSession = null;
    TopicSession tNodeSubSession = null;
    TopicSession tNodePubSession = null;
    Topic reqTopic = null;
    Topic resTopic = null;
    TopicSubscriber reqSub = null;
    TopicPublisher resPub = null;

    // Used for the determination of master BDB node
    Topic dbNodeStatusSubTopic = null;
    Topic dbNodeStatusPubTopic = null;
    TopicSubscriber dbNodeStatusSub = null;
    TopicPublisher dbNodeStatusPub = null;    
    private static long localStartTime;
    private static boolean isMasterNode = false;
    private static long localNodeID;
    private static Map serverStatusMap = new HashMap();
    private Thread nodeStatusSender;
    private Thread nodeStatusReceiver;
    private static long nodeUpdateInterval 
        = 5000; // 5 seconds in milli-seconds
    private static long nodeUpdateGraceperiod 
        = 1000; // 1 second in milli-seconds

    private DataAccessor da;

    // Encapsulates the database environment.
    private static HaDBEnv haDbEnv = new HaDBEnv();
    private static File sessDbEnvPath = null;

    /* Config data - TODO : move to properties/CLI options */
    private int MAX_RESPONSE_QUEUES = 1;

    private String databaseFileName = "amsessions.db";

    private int flags = 0; /* Db.DB_AUTO_COMMIT */
    
    private String userName = "guest";
    private String userPassword = "guest";
    private int cacheSize = 32;  /* 32 mb has the best overall performance */
    private String clusterAddress = null;
    private String dbDirectory = "sessiondb";
    private int numCleanSessions = 1000;
    private boolean verbose = false;
    private long statsInterval = 60 * 1000; //60 seconds
    private boolean statsEnabled = false;
    private boolean deleteDatabase = true;
    
    private static Map arguments = new HashMap();
    private static final int INVALID = 0;
    private static final int USER_NAME = 1;
    private static final int PASSWORD = 2;
    private static final int PASSWORD_FILE = 3;
    private static final int CACHE_SIZE = 4;
    private static final int DIRECTORY = 5;
    private static final int CLUSTER_ADDRESS = 6;
    private static final int NUM_CLEAN_SESSIONS = 7;
    private static final int DELETE_DATABASE = 8;
    private static final int VERBOSE = 9;
    private static final int STATS_INTERVAL = 10;
    private static final int HELP = 11;
    private static final int VERSION = 12;
    private static final int NODE_STATUS_UPDATE_INTERVAL = 13;
    private static final int PROPERTIES_FILE =14; 
    private static ResourceBundle bundle = null;
    private static final String RESOURCE_BUNDLE = "amSessionDB";
    
    private static boolean isServerUp = false;
    private int sleepTime = 60 * 1000; // 1 min in miillisec
    
    private static int readCount = 0;
    private static int writeCount = 0;
    private static int deleteCount = 0;
    private static int totalTrans = 0;
    
    // Session Constraints
    private static int scReadCount = 0;
    private static final int SESSION_VALID = 1;
    
    private static PrintWriter statsWriter = null;
    
    private Thread processThread;
    private String propertiesfile = null; 
    
    static {
        arguments.put("--username", new Integer(USER_NAME));
        arguments.put("-u", new Integer(USER_NAME));
        arguments.put("--password", new Integer(PASSWORD));
        arguments.put("-w", new Integer(PASSWORD));
        arguments.put("--passwordfile", new Integer(PASSWORD_FILE));
        arguments.put("-f", new Integer(PASSWORD_FILE));
        arguments.put("--cachesize", new Integer(CACHE_SIZE));
        arguments.put("-c", new Integer(CACHE_SIZE));
        arguments.put("--dbdirectory", new Integer(DIRECTORY));
        arguments.put("-b", new Integer(DIRECTORY));
        arguments.put("--clusteraddress", new Integer(CLUSTER_ADDRESS));
        arguments.put("-a", new Integer(CLUSTER_ADDRESS));
        arguments.put("--numcleansessions", new Integer(NUM_CLEAN_SESSIONS));
        arguments.put("-s", new Integer(NUM_CLEAN_SESSIONS));
        arguments.put("--deletedatabase", new Integer(DELETE_DATABASE));
        arguments.put("-r", new Integer(DELETE_DATABASE));
        arguments.put("--verbose", new Integer(VERBOSE));
        arguments.put("-v", new Integer(VERBOSE));
        arguments.put("--statsInterval", new Integer(STATS_INTERVAL));
        arguments.put("-i", new Integer(STATS_INTERVAL));
        arguments.put("--help", new Integer(HELP));
        arguments.put("-h", new Integer(HELP));
        arguments.put("--version", new Integer(VERSION));
        arguments.put("-n", new Integer(VERSION));
        arguments.put("--nodestatusupdateinterval", 
                      new Integer(NODE_STATUS_UPDATE_INTERVAL));
        arguments.put("-p", 
                      new Integer(NODE_STATUS_UPDATE_INTERVAL));
        arguments.put("-m", new Integer(PROPERTIES_FILE)); 
        arguments.put("--propertiesfile", 
                      new Integer(PROPERTIES_FILE));
        try {
            bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE, 
                     Locale.getDefault());
        } catch (MissingResourceException mre) {
            System.err.println("Cannot get the resource bundle.");
            System.exit(1);
        }
        
        statsWriter = new PrintWriter(System.out);

    }    
        

    public FAMHaDB(String id) throws Exception {
        
        _id = id;
    }
    
    private void initDB() throws Exception {
        try {
            haDbEnv.setup(sessDbEnvPath, // path to the environment home
                               false);        // is this environment read-only?
            // Open the data accessor. This is used to retrieve
            // persistent objects.
            da = new DataAccessor(haDbEnv.getEntityStore(),
                propertiesfile);
        } catch (DatabaseException dbe) {
            // Exception handling goes here
            System.err.println("Error in creating session data accessor");
            System.err.println(dbe.getMessage());     
            if(verbose) {
                dbe.printStackTrace();
            }
        } 
    }
       
    private void initJMQ () throws Exception {

        ConnectionFactoryProvider provider = ConnectionFactoryProviderFactory
                .getProvider();
        tFactory = provider.newTopicConnectionFactory(clusterAddress,
                true, true, userName, userPassword);

        tConn = tFactory.createTopicConnection();
        tSession = tConn.createTopicSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        reqTopic = tSession.createTopic(DBREQUEST);
        resTopic = tSession.createTopic(DBRESPONSE);

        reqSub = tSession.createSubscriber(reqTopic);
        resPub = tSession.createPublisher(resTopic);
        
        tNodeSubSession = tConn.createTopicSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        tNodePubSession = tConn.createTopicSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        
        dbNodeStatusSubTopic = tNodeSubSession.createTopic(DBNODESTATUS);
        dbNodeStatusSub =
            tNodeSubSession.createSubscriber(dbNodeStatusSubTopic);
        dbNodeStatusPubTopic = tNodePubSession.createTopic(DBNODESTATUS);
        dbNodeStatusPub =
            tNodePubSession.createPublisher(dbNodeStatusPubTopic);

        tConn.start();
        /*
         * reseting the StartTime for MasterDBNodeChecker
         * in case of Broker restart/connection failure
         */
        isMasterNode = false;
        localStartTime = System.currentTimeMillis();
        isServerUp = true;
    }
    
    private void initialize(String args[]) throws Exception {
        parseCommandLine(args);
        System.out.println(bundle.getString("initializing"));
        initDB();
        initJMQ();
        initMasterDBNodeChecker();
        processThread = new Thread(this);        
        processThread.setName(_id);
        processThread.start();
        System.out.println(bundle.getString("startsuccess"));
    }
    
    private void initMasterDBNodeChecker() {
        localNodeID = (new SecureRandom()).nextLong();
        localStartTime = System.currentTimeMillis();
        nodeStatusSender = new Thread(new NodeStatusSender());        
        nodeStatusSender.setDaemon(true); 
        nodeStatusSender.start();
        nodeStatusReceiver = new Thread(new NodeStatusReceiver());
        nodeStatusReceiver.setDaemon(true); 
        nodeStatusReceiver.start();
        if(verbose) {
            System.out.println(bundle.getString("waitfornodechecking"));
        }      
        try {
            // Wait until the NodeInfo of all the peer BDB nodes to 
            // be received by the local server. 
            Thread.sleep(nodeUpdateInterval+nodeUpdateGraceperiod);
        } catch (Exception e) {
            System.err.println(e.getMessage());     
            if(verbose) {
                e.printStackTrace();
            }         
        }     
        determineMasterDBNode();
    }
    
    private void Shutdown() {
        try {
            shutdownStatus = true;
            Thread.sleep(3000); 
            haDbEnv.close();
        } catch (Exception e) {
            System.out.println("e.getMessage");
        }
    }

    public int process() throws Exception 
    {
        BytesMessage message = (BytesMessage) reqSub.receive();
                
        String id = message.getStringProperty(ID);
        String op = message.getStringProperty("op");
        String svc = message.getStringProperty("service");
        if(verbose) {
            System.out.println("OP=" + op);
            System.out.println("service=" + svc);
        }
        //showAllSessionRecords();
        PrimaryIndex recordByPrimaryKey = da.getPrimaryIndex(svc);
        if (op.equals(READ)) {
            if(verbose) {
                System.out.println(bundle.getString("readmsgrecv"));
            }
	        if(statsEnabled) {
                readCount++;
            }
            String pKey = getLenString(message);
            long random = message.readLong();
            
            if(verbose) {
                System.out.println(">>>>>>>>>>>>>> Read by Primary Key : " + pKey);
            }

            BaseRecord baseRecord = null;
            try {
                baseRecord = (BaseRecord)recordByPrimaryKey.get(pKey);
            } catch (DatabaseException ex) {
                ex.printStackTrace();
                System.out.println("READ exc: " + ex);
            }
            
            if(verbose) {
                if (baseRecord != null) { 
                    System.out.println(">>>>>>>>>>>>>> Found record !");
                } else {
                    System.out.println(">>>>>>>>>>>>>> Not found record !");
                }
            }
            
            if (baseRecord != null) { 
                BytesMessage resmsg = 
                    (BytesMessage) tSession.createBytesMessage();
                resmsg.setStringProperty(ID, id);
	            resmsg.writeLong(random);

                byte blob[] = baseRecord.getBlob();
                resmsg.writeLong(blob.length);
                resmsg.writeBytes(blob);
                resPub.publish(resmsg);
            } else if(isMasterNode) {
                BytesMessage resmsg = 
                    (BytesMessage) tSession.createBytesMessage();
                resmsg.setStringProperty(ID, id);
                resmsg.setStringProperty(OP_STATUS, NOT_FOUND);
                resmsg.writeLong(random);
                resPub.publish(resmsg);
            }
        } else if (op.equals(WRITE)) {
            
            if(verbose) {
               System.out.println(bundle.getString("writemsgrecv"));
            }    
	    if(statsEnabled) {
                writeCount++;
            }
            String pKey = getLenString(message);
            long expdate = message.readLong();
            byte[] secondKey = getLenBytes(message);
            byte[] auxdata = getLenBytes(message);
            int state = message.readInt();
            byte[] stuff = getLenBytes(message);
            
            if(verbose) {
                System.out.println(">>>>>>>>>>>>>> Write by Primary Key : " + pKey);
            }

            BaseRecord record = (BaseRecord) da.classes.get(svc).newInstance();   
            record.setPrimaryKey(pKey);
            record.setExpDate(expdate);
            if (secondKey != null) {
                record.setSecondaryKey(new String(secondKey, "utf8"));
            }
            if (auxdata != null) {
                record.setAuxData(new String(auxdata, "utf8"));
            }   
            record.setState(state);
            record.setBlob(stuff);

            recordByPrimaryKey.put(record);
            
        } else if (op.equals(DELETEBYDATE)) {
            if(verbose) {
                System.out.println(bundle.getString("datemsgrecv"));
            }    
            long expDate = message.readLong();
            if(verbose) {
                System.out.println(">>>>>>>>>>>>>> Delete by Date : " + expDate);
            }
            deleteByDate(expDate, numCleanSessions, svc); 
        } else if (op.equals(DELETE)) {
            if(verbose) {
                System.out.println(bundle.getString("deletemsgrecv"));
            }
	        if(statsEnabled) {
                deleteCount++;
            }
            String pKey = getLenString(message);
            if(verbose) {
                System.out.println(">>>>>>>>>>>>>> Delete by Primary Key : " + pKey);
            }

            Transaction txn = null;
            try {
                txn = haDbEnv.getEnv().beginTransaction(null, null);
                recordByPrimaryKey.delete(txn, pKey);
                txn.commit();
            } catch (Exception e) {
                txn.abort();
                System.out.println("Aborted txn: " + e.toString());
                e.printStackTrace();
            }             
        } else if (op.equals(SHUTDOWN)) {
            Shutdown();
            return(1);
        } else if (op.equals(GET_RECORD_COUNT)) {
  
            if(verbose) {
                System.out.println(bundle.getString("getsessioncount"));
            }    
	    
            if(statsEnabled) {
                scReadCount++;
            }
            getRecordsBySecondaryKey(message, id, svc);
        } else if (op.equals(READ_WITH_SEC_KEY)) {
            if(verbose) {
                System.out.println(bundle.getString("readwithseckey"));
            }    
	    
            if(statsEnabled) {
                scReadCount++;
            }
            readWithSecondaryKey(message, id, svc);
        } 
        return 0;
    }

    public void getRecordsBySecondaryKey(BytesMessage message,
                                 String id, String service) 
        throws Exception {
        
        if (!isMasterNode) {
            if (verbose) {
                System.out.println(bundle.getString("notmasterdbnode"));
            }
            return;
        }

        String secondKey = getLenString(message);
        if(verbose) {
            System.out.println("SEC"+ secondKey);
        }
        long random = message.readLong();
        int nrows = 0;
        Vector rows = new Vector();
        EntityCursor<? extends BaseRecord> records = null;
        
        try {
            // Use the BaseRecord secondary key to retrieve
            // these objects.
            SecondaryIndex recordBySecondaryIndx =
                da.getSecondaryIndex2(service);  
            records = recordBySecondaryIndx.subIndex(secondKey).entities();
            for (BaseRecord record : records) {
                // only the "valid" non-expired sessions with the 
                // right secondaryKey will be counted
                long currentTime = 
                    System.currentTimeMillis()/1000;
                Long expdate = record.getExpDate();
                if ((record.getState() == SESSION_VALID) &&
                    (currentTime < expdate.longValue())) {
                    nrows++;
                    RecordExpTimeInfo info = 
                        new RecordExpTimeInfo();
                    info.auxDataLen = record.getAuxData().length();
                    info.auxyData = record.getAuxData().getBytes("utf8");
                    info.expTime = expdate;
                    rows.add(info);
                }
            }
        } catch (Exception e) {
            if (verbose) {
                e.printStackTrace();
            }
        } finally {
            records.close();
        }
        // construct a response message which contains the
        // session count
        BytesMessage resmsg 
            = (BytesMessage) tSession.createBytesMessage();
        resmsg.setStringProperty(ID, id);
        resmsg.writeLong(random);
        resmsg.writeInt(nrows);
        for (int i=0;i<rows.size();i++) {
            RecordExpTimeInfo info =
                (RecordExpTimeInfo)rows.get(i);            
            resmsg.writeInt(info.auxDataLen);
            resmsg.writeBytes(info.auxyData);
            resmsg.writeLong(info.expTime);
        }
        resPub.publish(resmsg);                    
    }
     
    public void readWithSecondaryKey(BytesMessage message, String id,
        String service) throws Exception {
        
        if (!isMasterNode) {
            if (verbose) {
                System.out.println(bundle.getString("notmasterdbnode"));
            }
            return;
        }

        String secondKey = getLenString(message);
        if(verbose) {
            System.out.println("SEC"+ secondKey);
        }
        long random = message.readLong();
        Vector rows = new Vector();
        EntityCursor<? extends BaseRecord> records = null;
        
        try {
            // Use the BaseRecord secondary key to retrieve
            // these objects.
            SecondaryIndex recordBySecondaryIndx =
                da.getSecondaryIndex2(service);  
            records = recordBySecondaryIndx.subIndex(secondKey).entities();
            for (BaseRecord record : records) {
                rows.add(record.getBlob());
            }
        } catch (Exception e) {
            if (verbose) {
                e.printStackTrace();
            }
        } finally {
            records.close();
        }

        BytesMessage resmsg = (BytesMessage) tSession.createBytesMessage();
        resmsg.setStringProperty(ID, id);
        resmsg.writeLong(random);
        int nrows = rows.size();
        resmsg.writeInt(nrows);
        for (int i=0; i<nrows; i++) {
            byte[] blob = (byte[])rows.get(i);
            resmsg.writeInt(blob.length);
            resmsg.writeBytes(blob);
        }
        resPub.publish(resmsg);                    
    }

    private class RecordExpTimeInfo {
        int auxDataLen;
        byte[] auxyData;
        long expTime;
    }
    
    public void deleteByDate(long expTime, int cleanCount, String svc) 
    throws Exception {       
        Transaction txn = haDbEnv.getEnv().beginTransaction(null, null);
        EntityCursor<? extends BaseRecord> records = 
            da.getSecondaryIndex1(svc).entities(txn, null);
        int count = 0;
        
        if (verbose) {
            System.out.println("expTime : " + expTime);
            System.out.println("cleanCount : " + cleanCount);
            System.out.println("svc : " + svc);
        }
        
        try {
            for (BaseRecord record : records) {
                Long expdate = record.getExpDate();
                if (expdate.longValue() <= expTime) {
                    records.delete();
                    if (verbose) {
                        System.out.println(">>>>> Delete the " + count 
                            + "th record has " + expdate);
                    }
                    
                    if (count++ >= cleanCount) {
                        break;
                    }
                } else {
                    break;
                }
            }
            records.close();
            records = null;
            txn.commit();
            txn = null;
        } catch (Exception e) {
            System.out.println("Exception for delete record by cursor : "
                + e.toString());
            e.printStackTrace();
            if (records != null) {
                records.close();
                records = null;
            }
            if (txn != null) {
                txn.abort();
                txn = null;
            }
        }             
    }
    
    public void deleteByDate(long expTime, int cleanCount) 
    throws Exception {
        //TODO: loop through all services 
        deleteByDate(expTime, cleanCount, "session");
    }

    // Displays all the session records in the store
    private void showAllSessionRecords(String service) 
        throws DatabaseException {

        // Get a cursor that will walk every
        // inventory object in the store.
        EntityCursor<? extends BaseRecord> records =
            da.getSecondaryIndex1(service).entities();

        try { 
            for (BaseRecord record : records) {
                displaySessionRecord(record);
            }
        } catch(DatabaseException de){
            System.err.println(de.getMessage());     
            if(verbose) {
                de.printStackTrace();
            }        	
        } finally {
            records.close();
        }
    }
    
    private void displaySessionRecord(BaseRecord theSession)
        throws DatabaseException {

        assert theSession != null;
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println("Primary Key :\t " + theSession.getPrimaryKey());
        System.out.println("Expiration Date :\t " + theSession.getExpDate());
        System.out.println("Aux Data :\t " + theSession.getAuxData());
        System.out.println("State :\t " + theSession.getState());
        System.out.println("Secondary Key :\t " + theSession.getSecondaryKey());
        
    }
    
    String getLenString(Message msg) throws Exception
    {
            BytesMessage message = (BytesMessage) msg;
            long pKeylen = message.readLong();
            byte[] pKeybytes  = new byte[(int) pKeylen];
            message.readBytes(pKeybytes);
            return(new String(pKeybytes, "utf8"));
    }
    
    byte[] getLenBytes(Message msg) throws Exception
    {
            BytesMessage message = (BytesMessage) msg;
            long keylen = message.readLong();
            byte[] keybytes=null; 
            if (keylen > 0) {
                keybytes  = new byte[(int) keylen];
                message.readBytes(keybytes);
            }
            return(keybytes);
    }

    public void run() {
        long startTime = System.currentTimeMillis();
        long epoch = startTime;
        totalTrans = 0;
        while (!shutdownStatus) {
            try {
                if (isServerUp) {
                    int ret = process();
                    totalTrans++;

                    if (statsEnabled && 
                       ((System.currentTimeMillis() - startTime) >= statsInterval)) {

                        printStats(); 
                        startTime = System.currentTimeMillis();
                        totalTrans = 0;
                        readCount = 0;
                        writeCount = 0;
                        deleteCount = 0;
                        scReadCount = 0;
                    }
                } else {
                    /*
                     * When server is down this thread runs with a sleep
                     * interval of 1 minute and cleans sessions 5 times the
                     * numCleanSessions value from the Database.
                     */
                    Thread.sleep(sleepTime);
                    long curTime = System.currentTimeMillis()/1000;
                    int cleanCount = numCleanSessions * 5;
                    deleteByDate(curTime, cleanCount);

                    if(verbose) {
                        System.out.println(bundle.getString("reconnecttobroker"));
                    }
                    initJMQ();
                    if(verbose) {
                        System.out.println(bundle.getString("reconnectsuccessfull"));
                    }
                }
            } catch (Exception ex) {
                isServerUp = false;
                System.err.println(bundle.getString("brokerdown"));
                if(verbose) {
                    ex.printStackTrace();
                }    
            } catch (Throwable t) {
                if(verbose) {
                     t.printStackTrace();
                }     
            }
        }

    }
    // TODO move out to seperate class?
    private void sunSpecificConfig(TopicConnectionFactory tFactory) 
                 throws Exception
    {
        ConnectionFactory cf = (com.sun.messaging.ConnectionFactory) tFactory;
        
        cf.setProperty(ConnectionConfiguration.imqAddressList,
                clusterAddress);
        cf.setProperty(ConnectionConfiguration.imqAddressListBehavior,
                "RANDOM");
        cf.setProperty(ConnectionConfiguration.imqReconnectEnabled, "true");
        cf.setProperty(ConnectionConfiguration.imqConnectionFlowLimitEnabled,
                "true");
        cf.setProperty(ConnectionConfiguration.imqDefaultUsername,
                userName);
        cf.setProperty(ConnectionConfiguration.imqDefaultPassword,
                userPassword);
    }

    static public void main(String args[]) 
    {
        
        try {
             dbs = new FAMHaDB("FAMHaDB");
             dbs.initialize(args);
             
             AMDBShutdown shutDownHook = new AMDBShutdown();
             Runtime.getRuntime().addShutdownHook(shutDownHook);
            
        } catch (Exception ex) {
            System.out.println("Exception main()");
            ex.printStackTrace();
            System.exit(1);
            
        }
        
    }
    
    static class AMDBShutdown extends Thread {
        public void run() {
            dbs.Shutdown();
        }
    }
    
   private void printCommandError(String errorMessage, String command) {
        System.err.println(bundle.getString(errorMessage) + " " + command);
        System.err.println(bundle.getString("usage"));
        System.exit(1);
   }
   
   private void printUsage() {
       System.err.println(bundle.getString("usage"));
       System.exit(1);
   }
   
   private void printStats() {
       
       
       statsWriter.println(bundle.getString("printingstats"));
       statsWriter.println(bundle.getString("totalreq") + " " +
            totalTrans);
       statsWriter.println(bundle.getString("totalread") + " " +
               readCount);
       statsWriter.println(bundle.getString("totalwrite") + " " +
               writeCount);
       statsWriter.println(bundle.getString("totaldelete") + " " +
               deleteCount);
       statsWriter.println(bundle.getString("totalreadsessioncount") 
                           + " " + scReadCount);
       statsWriter.flush();
       
   }
    
    private void parseCommandLine(String[] argv) throws Exception {
        if (!validateArguments(argv, bundle)) {
            printUsage();
        }

        for (int i = 0; i < argv.length; i++) {
            int opt = getToken(argv[i]);
            switch (opt) {

            case USER_NAME:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }
                userName = argv[i];
                if (getToken(userName.toLowerCase()) != INVALID) {
                    printCommandError("nousername", argv[i-1]);                    
                }
                break;
            case PASSWORD:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }
                userPassword = argv[i];
                if (getToken(userPassword.toLowerCase()) != INVALID) {
                    printCommandError("nopassword", argv[i-1]);                    
                    printCommandError("nopassword", argv[i-1]);                    
                    printCommandError("nopassword", argv[i-1]);                    
                }
                break;    
            case PASSWORD_FILE:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }
                String passwordfile = argv[i];
                if ((getToken(passwordfile.toLowerCase()) != INVALID)) {
                        printCommandError("nopasswordfile",  argv[i-1]);                    
                }
                
                String pwd = CryptUtil.decrypt(CryptUtil.DEFAULT_PBE_PWD,
                        FAMSFOPassword.readEncPasswordFromFile(passwordfile));

                if (pwd == null) {
                    printCommandError("nopwdinfile",  argv[i]); 
                }

                userPassword = pwd.trim();
                break;
            case CACHE_SIZE:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }

                String cache = argv[i];
                if (getToken(cache) != INVALID) {
                    printCommandError("nocachesize",  argv[i-1]);                    
                }
                try {
                    cacheSize = Integer.parseInt(cache);
                } catch (NumberFormatException e) {
                    printCommandError("invalidvalue", argv[i-1]);
                    
                }
                break;
            case DIRECTORY:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }
                dbDirectory = argv[i];
                if (getToken(dbDirectory.toLowerCase()) != INVALID) {
                    printCommandError("nodbdirectory",  argv[i-1]);
                }
                
                sessDbEnvPath = new File(dbDirectory);
                break;
            case CLUSTER_ADDRESS:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }
                clusterAddress = argv[i];
                if (getToken(clusterAddress.toLowerCase()) != INVALID) {
                    printCommandError("noclusteraddress",  argv[i-1]);
                }
                break;
            case NUM_CLEAN_SESSIONS:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }

                String nCleanSessions = argv[i];
                if (getToken(nCleanSessions) != INVALID) {
                    printCommandError("nonumcleansessions",  argv[i-1]);
                }
                try {
                    numCleanSessions = Integer.parseInt(nCleanSessions);
                } catch (NumberFormatException e) {
                    printCommandError("invalidvalue", argv[i-1]);

                }
                break;
            case VERBOSE:
                verbose = true;
                break;
            case STATS_INTERVAL:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }

                String sInterval = argv[i];
                if (getToken(sInterval) != INVALID) {
                    printCommandError("nostatsinterval",  argv[i-1]);
                }
                try {
                    statsInterval = Long.parseLong(sInterval);
                } catch (NumberFormatException e) {
                    printCommandError("invalidvalue", argv[i-1]);

                }
                if (statsInterval <= 0) {
                    statsInterval = 60;
                }
		statsInterval = statsInterval * 1000; //converting to millisec
                statsEnabled = true;
                break;
            case HELP:
                System.err.println(bundle.getString("usage"));
                System.exit(0);
                break;
            case VERSION:
                System.out.println("\n" + bundle.getString("version"));
                System.exit(0);
                break;
            case NODE_STATUS_UPDATE_INTERVAL:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }
                String interval = argv[i];
                if (getToken(interval) != INVALID) {
                    printCommandError("nonodestatusupdateinterval",  
                                      argv[i-1]);
                }
                try {
                    nodeUpdateInterval = Long.parseLong(interval);
                } catch (NumberFormatException e) {
                    printCommandError("invalidvalue", argv[i-1]);

                }
                if (nodeUpdateInterval <= 0) {
                    nodeUpdateInterval = 5000;
                }
                break;
             case PROPERTIES_FILE:
                i++;
                if (i >= argv.length) {
                    printUsage();
                }
                propertiesfile = argv[i];
                break;
            default:
                System.err.println(bundle.getString("usage"));
                System.err.println(bundle.getString("invalid-option") + argv[i]);
                System.exit(1);
            }

        }
    }
    
    
    /**
     * Return true if arguments are valid.
     * 
     * @param argv
     *            Array of arguments.
     * @param bundle
     *            Resource Bundle.
     * @return true if arguments are valid.
     */
    static boolean validateArguments(String[] argv, ResourceBundle bundle) {
	int len = argv.length;
	boolean hasClusterAddress = false;
	boolean retValue = true;
        boolean hasPassword = false;
        boolean hasPwdFile = false;

        if (len == 0) {
	    retValue = false;
	} else if (len == 1) {
	    String arg = argv[0].toLowerCase();
	    if (!(arg.equals("--help") || 
	            arg.equals("-h") ||
	            arg.equals("--version") ||
	            arg.equals("-n")) )  {
		System.err.println(bundle.getString("invalid-option") + arg);
		retValue = false;
	    }
        } else {
            for (int i = 0; (i < (len -1)); i++) {
    	        String arg = argv[i].toLowerCase();
    	    	if (arg.equals("--clusteraddress") || arg.equals("-a")) {
    		    hasClusterAddress = true;
    		}
                if(arg.equals("--password") || arg.equals("-w")) {
                    hasPassword = true;
                }
                if(arg.equals("--passwordfile") || arg.equals("-f")) {
                    hasPwdFile = true;
                }
    	    }
            
            if(hasPassword && hasPwdFile) {
                retValue = false;
            }
                        
            if(!hasClusterAddress) {
                retValue = false;
            }
	}

	return retValue;
    }
    
    
    int getToken(String arg) {
        try {
            return(((Integer)arguments.get(arg)).intValue());
        } catch(Exception e) {
            return 0;
        }
    }
    
   
      
    private void deleteDirectory(String fileName) {
        File dir = new File(fileName);
        
        if(dir.exists()) {
            File files[] = dir.listFiles();
            for (int i=0; i < files.length; i++) {
                files[i].delete();
            }
        } else {
            dir.mkdir();
        }
        
    }

    // Check if the local daemon process is the master (longest-lived) 
    // BDB node
    static private void determineMasterDBNode() {
        synchronized (serverStatusMap) {                        
            Set s = serverStatusMap.keySet();
            Iterator iter = s.iterator();
            
            boolean masterDB = true;
            while (iter.hasNext()) {
                String key = (String)iter.next();
                NodeInfo info = (NodeInfo)serverStatusMap.get(key);
                if (info.startTime < localStartTime) {
                    masterDB = false;
                    break;
                }
            }
            isMasterNode = masterDB;        
        }
    }    

   static void debugMessage(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    static void pendRunning() {
        // System.in std input stream already opened by default.
        // Wrap in a new reader stream to obtain 16 bit capability.
        InputStreamReader reader = new InputStreamReader (System.in);

        // Wrap the reader with a buffered reader.
        BufferedReader buf_in = new BufferedReader (reader);

        String str = "q";
        try {
            System.out.print("Please press the return key to continue.");
            // Read a whole line a time. Check the string for
            // the "quit" input to jump from the loop.
            do {
                // Read text from keyboard
                str = buf_in.readLine ();
                if (str == null) 
                    debugMessage("------------>str is null !");
                else
                    debugMessage("------------>str is not null !");

            } while (!str.equals("q"));
        } catch (Exception e) {
                debugMessage("Exception in pendRunning : "+e);
                e.printStackTrace();
        }
    }

    // NodeInfo data structure
    private class NodeInfo {
        long nodeID;
        long startTime;
        long lastUpdateTime;
    }

    // This NodeStatusSender thread keeps performing the following
    // actions (interval defined 5 secs):
    // (1) Send out the local nodeID/startTime to its peer BDB nodes.
    // (2) Remove the outdated NodeInfo entries from the 
    //     serverStatusMap map if the information is obsolete.
    // (3) Determine whether the local BDB node can become the master
    //     BDB node.

    class NodeStatusSender implements Runnable {

        NodeStatusSender() {       
        }
        
        public void run() {
            while (!shutdownStatus) {
                
                try {
                    if (isServerUp) {
                        long nextRun = System.currentTimeMillis()
                                + nodeUpdateInterval;
                        BytesMessage msg = (BytesMessage) tSession
                                .createBytesMessage();
                        msg.writeLong(localNodeID);
                        msg.writeLong(localStartTime);
                        dbNodeStatusPub.publish(msg);

                        long sleeptime = nextRun - System.currentTimeMillis();
                        if (sleeptime > 0) {
                            Thread.sleep(sleeptime);
                        }
                        RemoveOutdatedNodeInfo();
                        determineMasterDBNode();
                    } else {
                        Thread.sleep(nodeUpdateInterval);
                    }
                } catch (Exception e) {
                    isServerUp = false;
                    System.err.println(bundle.getString("brokerdown"));
                    if (verbose) {
                        e.printStackTrace();
                    }
                } catch (Throwable t) {
                    if (verbose) {
                        t.printStackTrace();
                    }     
               }
            }
        }

        // Remove the outdated NodeInfo from the serverStatusMap map 
        // if the information is obsolete.
        void RemoveOutdatedNodeInfo() {            
            synchronized (serverStatusMap) {
                Set s = serverStatusMap.keySet();
                Iterator iter = s.iterator();
                while (iter.hasNext()) {
                    String key = (String)iter.next();
                    NodeInfo info = (NodeInfo)serverStatusMap.get(key);
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > 
                        (info.lastUpdateTime +                         
                         nodeUpdateInterval + 
                         nodeUpdateGraceperiod)) {
                        iter.remove();
                    }
                }        
            }
        }
        
    }
    
    // This NodeStatusReceiver thread keeps waiting for the message
    // sent by its peer BDB nodes and update the local serverStatusMap
    // accordingly.
    class NodeStatusReceiver implements Runnable {
        
        NodeStatusReceiver() {            
        }
        
        public void run() {
            while (!shutdownStatus) {
                try {
                    if (isServerUp) {
                        BytesMessage msg = (BytesMessage) dbNodeStatusSub
                                .receive();
                        long nodeID = msg.readLong();    
                        long startTime = msg.readLong();
                        if (nodeID == localNodeID) {
                            // ignore the message sent by the local server
                            continue;
                        }
                        NodeInfo info = new NodeInfo();
                        info.nodeID = nodeID;
                        info.startTime = startTime;
                        info.lastUpdateTime = System.currentTimeMillis();
                        synchronized (serverStatusMap) {
                            serverStatusMap.put(String.valueOf(nodeID), info);
                        }
                    } else {
                        Thread.sleep(nodeUpdateInterval);
                    }
                } catch (Exception e) {
                    isServerUp = false;
                    System.err.println(bundle.getString("brokerdown"));
                    if (verbose) {
                        e.printStackTrace();
                    }
                } catch (Throwable t) {
                    if (verbose) {
                        t.printStackTrace();
                    }     
               }
            }
        }
    }

}
