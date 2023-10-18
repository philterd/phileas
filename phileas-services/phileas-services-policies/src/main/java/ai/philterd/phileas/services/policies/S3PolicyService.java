/*
 *     Copyright 2023 Philterd, LLC @ https://www.philterd.ai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.philterd.phileas.services.policies;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import ai.philterd.phileas.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.exceptions.api.BadRequestException;
import ai.philterd.phileas.model.exceptions.api.InternalServerErrorException;
import ai.philterd.phileas.model.services.AbstractPolicyService;
import ai.philterd.phileas.model.services.PolicyCacheService;
import ai.philterd.phileas.model.services.PolicyService;
import ai.philterd.phileas.services.policies.cache.PolicyCacheServiceFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Implementation of {@link PolicyService} that is backed by S3.
 */
public class S3PolicyService extends AbstractPolicyService implements PolicyService {

    private static final Logger LOGGER = LogManager.getLogger(S3PolicyService.class);

    private static final String JSON_EXTENSION = ".json";

    private AmazonS3 s3Client;
    private String bucket;
    private String prefix;
    private PolicyCacheService policyCacheService;

    public S3PolicyService(PhileasConfiguration phileasConfiguration, boolean testing) throws IOException {

        this.bucket = phileasConfiguration.policiesS3Bucket();
        final String region = phileasConfiguration.policiesS3Region();

        // If the prefix is not empty it must end with a forward slash.
        if(!StringUtils.isEmpty(phileasConfiguration.policiesS3Prefix())) {
            if(!phileasConfiguration.policiesS3Prefix().endsWith("/")) {
                this.prefix = phileasConfiguration.policiesS3Prefix() + "/";
            }
        }

        // Create a policy cache.
        this.policyCacheService = PolicyCacheServiceFactory.getInstance(phileasConfiguration);

        LOGGER.info("Configuring S3 backend for policies in s3 bucket {}", bucket);

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

        final List<String> policyNames = new LinkedList<>();

        try {

            LOGGER.info("Looking for policies in s3 bucket {}", bucket);
            final ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(prefix);

            ListObjectsV2Result result;

            do {

                result = s3Client.listObjectsV2(listObjectsV2Request);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {

                    final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, objectSummary.getKey()));
                    final String json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());

                    fullObject.close();

                    final JSONObject object = new JSONObject(json);
                    final String name = object.getString("name");

                    policyNames.add(name);
                    LOGGER.debug("Found policy named {}", name);

                }

                // If there are more than maxKeys keys in the bucket, get a continuation token and list the next objects.
                final String token = result.getNextContinuationToken();

                listObjectsV2Request.setContinuationToken(token);

            } while (result.isTruncated());

        } catch (Exception ex) {

            LOGGER.error("Unable to get policy names.", ex);

            throw new InternalServerErrorException("Unable to get policy names.");

        }

        return policyNames;

    }

    @Override
    public String get(String policyName) {

        try {

            // Get from cache.
            LOGGER.info("Getting policy name [{}] from the cache.", policyName);
            String json = policyCacheService.get(policyName);

            if(json == null) {

                // The policy was not in the cache. Look in S3.
                LOGGER.info("Policy was not cached. Looking for policy {} in s3 bucket {}", policyName, bucket);
                final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, prefix + policyName + JSON_EXTENSION));
                json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());
                fullObject.close();

                // Put it in the cache.
                LOGGER.info("Caching policy [{}]", policyName);
                policyCacheService.insert(policyName, json);

            }

            return json;

        } catch (Exception ex) {

            LOGGER.error("Unable to get policy.", ex);

            throw new InternalServerErrorException("Unable to get policy.");

        }

    }

    @Override
    public Map<String, String> getAll() {

        final Map<String, String> policies = new HashMap<>();

        try {

            LOGGER.info("Looking for all policies in s3 bucket {}", bucket);
            final ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(prefix);

            ListObjectsV2Result result;

            do {

                result = s3Client.listObjectsV2(listObjectsV2Request);

                LOGGER.info("Found {} policies.", result.getObjectSummaries().size());

                for (final S3ObjectSummary objectSummary : result.getObjectSummaries()) {

                    // Ignore any non .json files.
                    if (objectSummary.getKey().endsWith(JSON_EXTENSION)) {

                        final S3Object fullObject = s3Client.getObject(new GetObjectRequest(bucket, objectSummary.getKey()));
                        final String json = IOUtils.toString(fullObject.getObjectContent(), StandardCharsets.UTF_8.name());

                        fullObject.close();

                        final JSONObject object = new JSONObject(json);
                        final String name = object.getString("name");

                        LOGGER.info("Adding policy named [{}]", name);
                        policies.put(name, json);

                        LOGGER.info("Caching policy [{}]", name);
                        policyCacheService.insert(name, json);

                    }

                }

                // If there are more than maxKeys keys in the bucket, get a continuation token and list the next objects.
                final String token = result.getNextContinuationToken();

                listObjectsV2Request.setContinuationToken(token);

            } while (result.isTruncated());

        } catch (Exception ex) {

            LOGGER.error("Unable to get all policy names.", ex);

            throw new InternalServerErrorException("Unable to get all policies.");

        }

        return policies;

    }

    @Override
    public void save(String policyJson) {

        try {

            final JSONObject object = new JSONObject(policyJson);
            final String name = object.getString("name");

            LOGGER.info("Uploading object to s3://{}/{}", bucket, name + JSON_EXTENSION);
            s3Client.putObject(bucket, prefix + name + JSON_EXTENSION, policyJson);

            // Insert it into the cache.
            policyCacheService.insert(name, policyJson);

        } catch (JSONException ex) {

            LOGGER.error("The provided policy is not valid.", ex);
            throw new BadRequestException("The provided policy is not valid.");

        } catch (Exception ex) {

            LOGGER.error("Unable to save policy.", ex);

            throw new InternalServerErrorException("Unable to save policy.");

        }

    }

    @Override
    public void delete(String policyName) {

        try {

            LOGGER.info("Deleting object from s3://{}/{}", bucket, policyName + JSON_EXTENSION);
            s3Client.deleteObject(bucket, prefix + policyName + JSON_EXTENSION);

            // Remove it from the cache.
            policyCacheService.remove(policyName);

        } catch (Exception ex) {

            LOGGER.error("Unable to delete policy.", ex);

            throw new InternalServerErrorException("Unable to delete policy.");

        }

    }

}
