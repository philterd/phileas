package com.mtnfog.phileas.store;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mtnfog.phileas.model.objects.Span;
import com.mtnfog.phileas.model.services.Store;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Implementation of {@link Store} that uses MongoDB.
 */
public class MongoDBStore implements Store, Closeable {

    private static final Logger LOGGER = LogManager.getLogger(MongoDBStore.class);

    private static final String COLLECTION_NAME = "philter";

    private MongoClient mongoClient;
    private MongoCollection<Span> collection;

    /**
     * Generate a unique ID.
     * @return A generated unique ID.
     */
    public static String generateId() {

        final ObjectId id = new ObjectId();

        return id.toString();

    }

    /**
     * Creates a new connection to the MongoDB database.
     * @param uri The MongoDB connection URI.
     */
    public MongoDBStore(String uri) {

        final CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        final MongoClientURI mongoClientURI = new MongoClientURI(uri);
        mongoClient = new MongoClient(mongoClientURI);

        LOGGER.info("Connecting to MongoDB at {}", uri);

        final MongoDatabase database = mongoClient.getDatabase(mongoClientURI.getDatabase()).withCodecRegistry(pojoCodecRegistry);
        this.collection = database.getCollection(COLLECTION_NAME, Span.class);

    }

    public MongoDBStore(int port) {

        LOGGER.info("Connecting to MongoDB on localhost:", port);

        final CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        mongoClient = new MongoClient("localhost", port);

        final MongoDatabase database = mongoClient.getDatabase("philter").withCodecRegistry(pojoCodecRegistry);
        this.collection = database.getCollection(COLLECTION_NAME, Span.class);

    }

    @Override
    public void insert(Span span) {

        collection.insertOne(span);

        LOGGER.debug("1 span sent to MongoDB.");

    }

    @Override
    public void insert(List<Span> spans) {

        collection.insertMany(spans);

        LOGGER.debug("{} spans sent to MongoDB.", spans.size());

    }

    @Override
    public List<Span> getByDocumentId(String documentId) {

        LOGGER.debug("Finding spans for document ID {}", documentId);

        return collection.find(eq("documentId", documentId)).into(new LinkedList<>());

    }

    @Override
    public boolean isDocumentIdUnique(String documentId) {

        final Document query = new Document("_id", new Document("$eq", documentId));

        final long count = collection.countDocuments(query);

        if(count == 0) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void close() throws IOException {
        mongoClient.close();
    }

}
