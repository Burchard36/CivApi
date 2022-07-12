package org.example.utils.database;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ServerHeartbeatFailedEvent;
import com.mongodb.event.ServerHeartbeatStartedEvent;
import com.mongodb.event.ServerHeartbeatSucceededEvent;
import com.mongodb.event.ServerMonitorListener;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.internal.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class MongoDB implements ServerMonitorListener {
    protected final String databaseName;
    protected final Encrypt sha256;
    protected MongoClient client;
    protected MongoDatabase database;
    protected HashMap<String, MongoCollection<Document>> cachedCollections;

    public MongoDB(String databaseName) {
        this.databaseName = databaseName;
        this.sha256 = new Encrypt();
        this.cachedCollections = new HashMap<>();

    }

    public void connect() {
        try {
            MongoClientOptions.Builder clientOptions = new MongoClientOptions.Builder()
                    .addServerMonitorListener(this);
            this.client = new MongoClient(new MongoClientURI("mongodb+srv://admin:eVbDKlqQi6bGpQdr@storage.knoye.mongodb.net/", clientOptions));
            if (!this.loadDatabase(databaseName))
                throw new IllegalArgumentException("Database does not exist! Database name: " + databaseName);


            this.onConnect();
        } catch (Exception ex) { // failed connect
            System.out.println("Error while connecting to MongoDB");
            ex.printStackTrace();
        }
    }

    protected final boolean loadDatabase(String name) {
        try {
            this.database = this.client.getDatabase(name);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public void onConnect() {

    }

    protected CompletableFuture<Boolean> updateData(String collectionName,
                                                    Bson filter,
                                                    Bson projections,
                                                    Document data,
                                                    boolean usingAsync) {
        final MongoCollection<Document> collection = this.getCollection(collectionName);
        boolean exists = true;
        try {
            exists = this.dataExists(collectionName, filter, projections, false).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return this.returnFuture(usingAsync, () -> false);
        }

        if (!exists) return this.returnFuture(usingAsync, () -> false);

        collection.updateOne(filter, data);
        return this.returnFuture(usingAsync, () -> true);

    }

    /**
     * Inserts data into the database
     *
     * Recommended to always call this async, it gets data from another future, and calls a blocking get() method
     *
     * @param collectionName Name of collection to insert data to
     * @param searchFilter Key to use to insert against, used for checking if exists
     * @param usingAsync Recommended true for this call
     * @return false if data exists or error occurs within future completable
     */
    protected CompletableFuture<Boolean> insertData(String collectionName,
                                                    Bson searchFilter,
                                                    Bson projections,
                                                    BasicDBObject data,
                                                    boolean usingAsync) {
        final MongoCollection<Document> collection = this.getCollection(collectionName);
        boolean alreadyExists;
        try {
            alreadyExists = this.dataExists(collectionName, searchFilter, projections, false).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return this.returnFuture(usingAsync, () -> false);
        }

        if (alreadyExists) return this.returnFuture(usingAsync, () -> false);

        collection.insertOne(new Document(data.toMap()));
        return this.returnFuture(usingAsync, () -> true);
    }

    /**
     * Gets data from the MongoDatabase
     * @param collectionName Collection name to get data from
     * @param searchFilter Bson filter to search against
     * @param usingAsync true if this query should execute async, server side
     * @return FutureCompletable of Document
     */
    protected CompletableFuture<Document> getData(String collectionName,
                                                  Bson searchFilter,
                                                  boolean usingAsync) {
        final MongoCollection<Document> collection = this.getCollection(collectionName);
        final FindIterable<Document> searchResults = collection.find(searchFilter);
        final Document firstResult = searchResults.first();

        if (firstResult == null) return this.returnFuture(usingAsync, () -> null);
        return this.returnFuture(usingAsync, () -> firstResult);
    }

    /**
     * Checks if data exists inside of a database
     * @param collectionName Name of collection
     * @param searchFilter Bson filter to search against
     * @param usingAsync true if the query should execute async
     * @return CompletableFuture with boolean result value
     */
    protected CompletableFuture<Boolean> dataExists(String collectionName,
                                                    Bson searchFilter,
                                                    Bson projections,
                                                    boolean usingAsync) {
        System.out.println("Performing dataExists call for collection: " + collectionName);
        final MongoCollection<Document> collection = this.getCollection(collectionName);
        System.out.println("Pulled collection! " + searchFilter.toString());
        final FindIterable<Document> searchResults = collection.find(searchFilter).projection(projections);
        System.out.println("Executed search filter!");
        if (searchResults == null) {
            System.out.println("results were null, returning");
            return this.returnFuture(usingAsync, () -> false);
        }

        Document firstResult;
        try {
            firstResult = searchResults.first();
        } catch (Exception e) {
            e.printStackTrace();
            return this.returnFuture(usingAsync, () -> false);
        }

        System.out.println("returning dataExists results");
        if (firstResult == null) return this.returnFuture(usingAsync, () -> false);
        return this.returnFuture(usingAsync, () -> true);
    }

    /**
     * Returns a future, async or sync depends on input value
     * @param runAsync true if this CompletableFuture should run async
     * @param value Supplier callback value
     * @return CompletableFuture
     * @param <T> Generic Document/Json/Bson instance
     */
    protected <T> CompletableFuture<T> returnFuture(boolean runAsync, Supplier<T> value) {
        if (runAsync) return CompletableFuture.supplyAsync(value);
        return CompletableFuture.completedFuture(value.get());
    }

    /**
     * Gets a MongoCollection, it will create it upon creating it
     * @param collectionName Name of collection to get
     * @return the MongoCollection instance, should never be null
     */
    protected MongoCollection<Document> getCollection(String collectionName) {
        return this.database.getCollection(collectionName);
    }


    @Override
    public void serverHearbeatStarted(ServerHeartbeatStartedEvent event) {

    }

    @Override
    public void serverHeartbeatSucceeded(ServerHeartbeatSucceededEvent event) {
        // Maybe do something?
    }

    @Override
    public void serverHeartbeatFailed(ServerHeartbeatFailedEvent event) {
        System.out.println("Error with Database class: " + this.getClass().getName());
        event.getThrowable().printStackTrace();
    }

    public static class Encrypt {

        protected final String privateKey = "QkwzaG92YlNpaXNZa1hWUmVUeW0=";
        protected byte[] key;
        protected SecretKeySpec secretKey;
        public Encrypt() {
            try {

                this.key = this.privateKey.getBytes(StandardCharsets.UTF_8);
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                this.key = sha1.digest(this.key);
                this.key = Arrays.copyOf(this.key, 16);
                this.secretKey = new SecretKeySpec(this.key, "AES");

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        /**
         * Encrypts a String and encodes it in Base64
         * @param aString Any string you want encrypted
         * @return Base64 encoded result, null if an error occurred
         */
        public String encrypt(String aString) {
            try {
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
                return Base64.encode(cipher.doFinal(aString.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                     IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Decrypts a string, input must be base64 encoded
         * @param aString Base64 ended encrypted string
         * @return Decrypted string, sensitive data do not send to backend, do not send decrypted strings to a database
         */
        public String decrypt(String aString) {
            try {
                String base64Decoded = new String(Base64.decode(aString), StandardCharsets.UTF_8);
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, this.secretKey);
                return new String(cipher.doFinal(base64Decoded.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException |
                     BadPaddingException | InvalidKeyException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}

