package com.mtnfog.phileas.services.registry;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.mtnfog.phileas.configuration.PhileasConfiguration;
import com.mtnfog.phileas.model.exceptions.api.BadRequestException;
import com.mtnfog.phileas.model.exceptions.api.InternalServerErrorException;
import com.mtnfog.phileas.model.services.FilterProfileCacheService;
import com.mtnfog.phileas.model.services.FilterProfileService;
import com.mtnfog.phileas.services.cache.profiles.FilterProfileCacheServiceFactory;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Implementation of {@link FilterProfileService} that is backed by S3.
 */
public class S3FilterProfileService implements FilterProfileService {

    private static final Logger LOGGER = LogManager.getLogger(S3FilterProfileService.class);

    private AmazonS3 s3Client;
    private String bucket;
    private FilterProfileCacheService filterProfileCacheService;

    public S3FilterProfileService(PhileasConfiguration phileasConfiguration, boolean testing) {

        // Initialize the S3 client.
        this.bucket = phileasConfiguration.filterProfilesS3Bucket();
        final String region = phileasConfiguration.filterProfilesS3Region();

        // Create a filter profile cache.
        this.filterProfileCacheService = FilterProfileCacheServiceFactory.getInstance(phileasConfiguration);

        LOGGER.info("Configuring S3 backend for filter profiles in s3 bucket {}", bucket);

        if(testing) {

            final AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration("http://localhost:8001", "us-west-2");

            this.s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withPathStyleAccessEnabled(true)
                    .withEndpointConfiguration(endpoint)
                    .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
                    .build();

            this.s3Client.createBucket(bucket);

        } else {

            // Only permits credentials via the standard channels.
            this.s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withCredentials(new DefaultAWSCredentialsProviderChain())
                    .withRegion(region)
                    .build();

        }

    }

    @Override
    public List<String> get() {

        final List<String> filterProfileNames = new LinkedList<>();

        try {

            LOGGER.info("Looking for filter profiles in s3 bucket {}", bucket);
            final ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(bucket);

            ListObjectsV2Result result;

            do {

                result = s3Client.listObjectsV2(listObjectsV2Request);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {

                    final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, objectSummary.getKey()));
                    final String json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());

                    fullObject.close();

                    final JSONObject object = new JSONObject(json);
                    final String name = object.getString("name");

                    filterProfileNames.add(name);
                    LOGGER.debug("Found filter profile named {}", name);

                }

                // If there are more than maxKeys keys in the bucket, get a continuation token and list the next objects.
                final String token = result.getNextContinuationToken();

                listObjectsV2Request.setContinuationToken(token);

            } while (result.isTruncated());

        } catch (Exception ex) {

            LOGGER.error("Unable to get filter profile names.", ex);

            throw new InternalServerErrorException("Unable to get filter profile names.");

        }

        return filterProfileNames;

    }

    @Override
    public String get(String filterProfileName) {

        try {

            // Get from cache.
            LOGGER.info("Getting profile name [{}] from the cache.", filterProfileName);
            String json = filterProfileCacheService.get(filterProfileName);

            if(json == null) {

                // The filter profile was not in the cache. Look in S3.
                LOGGER.info("Filter profile was not cached. Looking for filter profile {} in s3 bucket {}", filterProfileName, bucket);
                final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, filterProfileName + ".json"));
                json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());
                fullObject.close();

                // Put it in the cache.
                LOGGER.info("Caching filter profile [{}]", filterProfileName);
                filterProfileCacheService.insert(filterProfileName, json);

            }

            return json;

        } catch (Exception ex) {

            LOGGER.error("Unable to get filter profile.", ex);

            throw new InternalServerErrorException("Unable to get filter profile.");

        }

    }

    @Override
    public Map<String, String> getAll() {

        final Map<String, String> filterProfiles = new HashMap<>();

        try {

            LOGGER.info("Looking for all filter profiles in s3 bucket {}", bucket);
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(bucket);

            ListObjectsV2Result result;

            do {

                result = s3Client.listObjectsV2(listObjectsV2Request);

                LOGGER.info("Found {} filter profiles.", result.getObjectSummaries().size());

                for (final S3ObjectSummary objectSummary : result.getObjectSummaries()) {

                    // Ignore any non .json files.
                    if (objectSummary.getKey().endsWith(".json")) {

                        final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, objectSummary.getKey()));
                        final String json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());

                        fullObject.close();

                        final JSONObject object = new JSONObject(json);
                        final String name = object.getString("name");

                        LOGGER.info("Adding filter profile named [{}]", name);
                        filterProfiles.put(name, json);

                        LOGGER.info("Caching filter profile [{}]", name);
                        filterProfileCacheService.insert(name, json);

                    }

                }

                // If there are more than maxKeys keys in the bucket, get a continuation token and list the next objects.
                final String token = result.getNextContinuationToken();

                listObjectsV2Request.setContinuationToken(token);

            } while (result.isTruncated());

        } catch (Exception ex) {

            LOGGER.error("Unable to get all filter profile names.", ex);

            throw new InternalServerErrorException("Unable to get all filter profiles.");

        }

        return filterProfiles;

    }

    @Override
    public void save(String filterProfileJson) {

        try {

            final JSONObject object = new JSONObject(filterProfileJson);
            final String name = object.getString("name");

            LOGGER.info("Uploading object to s3://{}/{}", bucket, name + ".json");
            s3Client.putObject(bucket, name + ".json", filterProfileJson);

            // Insert it into the cache.
            filterProfileCacheService.insert(name, filterProfileJson);

        } catch (JSONException ex) {

            LOGGER.error("The provided filter profile is not valid.", ex);
            throw new BadRequestException("The provided filter profile is not valid.");

        } catch (Exception ex) {

            LOGGER.error("Unable to save filter profile.", ex);

            throw new InternalServerErrorException("Unable to save filter profile.");

        }

    }

    @Override
    public void delete(String filterProfileName) {

        try {

            LOGGER.info("Deleting object from s3://{}/{}", bucket, filterProfileName + ".json");
            s3Client.deleteObject(bucket, filterProfileName + ".json");

            // Remove it from the cache.
            filterProfileCacheService.remove(filterProfileName);

        } catch (Exception ex) {

            LOGGER.error("Unable to delete filter profile.", ex);

            throw new InternalServerErrorException("Unable to delete filter profile.");

        }

    }

}
