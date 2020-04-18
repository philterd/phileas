package com.mtnfog.phileas.services.registry;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.mtnfog.phileas.model.exceptions.api.BadRequestException;
import com.mtnfog.phileas.model.exceptions.api.InternalServerErrorException;
import com.mtnfog.phileas.model.services.FilterProfileService;
import com.mtnfog.phileas.services.cache.profiles.RedisFilterProfileCacheService;
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
    private RedisFilterProfileCacheService redisFilterProfileCacheService;

    public S3FilterProfileService(Properties applicationProperties, boolean testing) {

        // Initialize the S3 client.
        this.bucket = applicationProperties.getProperty("filter.profiles.s3.bucket");
        final String region = applicationProperties.getProperty("filter.profiles.s3.region", "us-east-1");

        // Create a filter profile cache.
        this.redisFilterProfileCacheService = new RedisFilterProfileCacheService(applicationProperties);

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

        List<String> names = new LinkedList<>();

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

                    names.add(name);
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

        return names;

    }

    @Override
    public String get(String filterProfileName, boolean ignoreCache) {

        try {

            String json;

            if(!ignoreCache) {

                // Get from cache.
                LOGGER.info("Getting profile names from the cache.");
                json = redisFilterProfileCacheService.get(filterProfileName);

                if(json == null) {
                    // The filter profile was not in the cache. Look in S3.
                    LOGGER.info("Filter profile was not cached. Looking for filter profile {} in s3 bucket {}", filterProfileName, bucket);
                    final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, filterProfileName + ".json"));
                    json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());
                    fullObject.close();
                }

            } else {

                LOGGER.info("Looking for filter profile {} in s3 bucket {}", filterProfileName, bucket);
                final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, filterProfileName + ".json"));
                json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());
                fullObject.close();

            }

            // Put it into the cache.
            redisFilterProfileCacheService.insert(filterProfileName, json);

            return json;

        } catch (Exception ex) {

            LOGGER.error("Unable to get filter profile.", ex);

            throw new InternalServerErrorException("Unable to get filter profile.");

        }

    }

    @Override
    public Map<String, String> getAll(boolean ignoreCache) {

        Map<String, String> filterProfiles = new HashMap<>();

        try {

            if(!ignoreCache) {

                // Get from cache.
                LOGGER.info("Getting profile names from the cache.");
                filterProfiles = redisFilterProfileCacheService.getAll();

            } else {

                LOGGER.info("Looking for all filter profiles in s3 bucket {}", bucket);
                ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(bucket);

                // Clear the cache and put the profiles into the cache.
                redisFilterProfileCacheService.clear();

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

                            LOGGER.debug("Adding filter profile named {}", name);
                            filterProfiles.put(name, json);
                            redisFilterProfileCacheService.insert(name, json);

                        }

                    }

                    // If there are more than maxKeys keys in the bucket, get a continuation token and list the next objects.
                    final String token = result.getNextContinuationToken();

                    listObjectsV2Request.setContinuationToken(token);

                } while (result.isTruncated());

            }

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
            redisFilterProfileCacheService.insert(name, filterProfileJson);

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
            redisFilterProfileCacheService.remove(filterProfileName);

        } catch (Exception ex) {

            LOGGER.error("Unable to delete filter profile.", ex);

            throw new InternalServerErrorException("Unable to delete filter profile.");

        }

    }

}
