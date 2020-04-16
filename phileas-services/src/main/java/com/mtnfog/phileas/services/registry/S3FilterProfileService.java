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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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

    private Properties applicationProperties;

    private AmazonS3 s3Client;
    private String bucket;
    private String prefix;

    public S3FilterProfileService(Properties applicationProperties, boolean testing) {

        // Initialize the S3 client.
        this.bucket = applicationProperties.getProperty("filter.profiles.s3.bucket");
        this.prefix = applicationProperties.getProperty("filter.profiles.s3.prefix");
        final String region = applicationProperties.getProperty("filter.profiles.s3.region", "us-east-1");

        LOGGER.info("Looking for filter profiles in s3://{}/", bucket, prefix);

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

        final List<String> names = new LinkedList<>();

        try {

            ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix);
            ListObjectsV2Result result;

            do {

                result = s3Client.listObjectsV2(req);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {

                    final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, objectSummary.getKey()));
                    final String json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());

                    fullObject.close();

                    final JSONObject object = new JSONObject(json);
                    final String name = object.getString("name");

                    names.add(name);
                    LOGGER.debug("Added filter profile named {}", name);

                }

                // If there are more than maxKeys keys in the bucket, get a continuation token and list the next objects.
                final String token = result.getNextContinuationToken();

                req.setContinuationToken(token);

            } while (result.isTruncated());

        } catch (Exception ex) {

            LOGGER.error("Unable to get filter profile names.", ex);

            throw new InternalServerErrorException("Unable to get filter profile names.");

        }

        return names;

    }

    @Override
    public String get(String filterProfileName) {

        try {

            final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, buildKey(filterProfileName)));
            final String json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());

            fullObject.close();

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

            ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix);
            ListObjectsV2Result result;

            do {

                result = s3Client.listObjectsV2(req);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {

                    final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, objectSummary.getKey()));
                    final String json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());

                    fullObject.close();

                    final JSONObject object = new JSONObject(json);
                    final String name = object.getString("name");

                    filterProfiles.put(name, json);
                    LOGGER.debug("Added filter profile named {}", name);

                }

                // If there are more than maxKeys keys in the bucket, get a continuation token and list the next objects.
                final String token = result.getNextContinuationToken();

                req.setContinuationToken(token);

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

            final String key = buildKey(name);
            LOGGER.info("Uploading object to s3://{}/{}", bucket, key);
            s3Client.putObject(bucket, key, filterProfileJson);

        } catch (JSONException ex) {

            LOGGER.error("The provided filter profile is not valid.", ex);
            throw new BadRequestException("The provided filter profile is not valid.");

        } catch (Exception ex) {

            LOGGER.error("Unable to save filter profile.", ex);

            throw new InternalServerErrorException("Unable to save filter profile.");

        }

    }

    @Override
    public void delete(String name) {

        try {

            s3Client.deleteObject(bucket, buildKey(name));

        } catch (Exception ex) {

            LOGGER.error("Unable to delete filter profile.", ex);

            throw new InternalServerErrorException("Unable to delete filter profile.");

        }

    }

    private String buildKey(final String name) {

        LOGGER.debug("Building key from: {} and {} and {}", bucket, prefix, name);

        if(StringUtils.equals(prefix, "/")) {
            return name + ".json";
        } else if(prefix.endsWith("/")) {
            return prefix + name + ".json";
        } else {
            return prefix + "/" + name + ".json";
        }

    }

}
