# How to Evaluate Phileas' Performance

A common question we receive is how well does Phileas perform? Our answer to this question is probably less than satisfactory because it simply depends. What does it depend on? Phileas' performance is heavily dependent upon your individual data. Sharing to compare metrics of Phileas' performance between different customer datasets is like comparing apples and oranges.

If your data is not exactly like another customer's data then the metrics will not be applicable to your data. In terms of the classic information retrieval metrics precision and recall, comparing these values between customers can give false impressions about Phileas' performance, both good and bad.

> This guide walks you through how to evaluate Phileas' performance. If you are just getting started with Phileas please see the Quick Starts instead. Then you can come back here to learn how to evaluate Phileas' performance.

## Guide to Evaluating Performance

We have created this guide to help guide you in evaluating Phileas' performance on your data. The guide involves determining the types of sensitive information you want to redact, configuring those filters, optimizing the configuration, and then capturing the performance metrics.

> If you are using Philter we will gladly perform these steps for you and provide you a detailed Phileas performance report generated from your data. Please contact us to start the process.

#### What You Need

To evaluate Phileas' performance you need:

* An application using Phileas.
* A list of the types of sensitive information you want to redact.
* A data set representative of the text you will be redacting using Phileas. It's important the data set be representative so the evaluation results will transfer to the actual data redaction.
* The same data set but with annotated sensitive information. These annotations will be used to calculate the precision and recall metrics.

#### Configuring Phileas

Before we can begin our evaluation we need to create a policy. A [policy](policies_README.md) is a file that defines the types of sensitive information that will be redacted and how it will be redacted. The policies are stored on the Phileas instance under `/opt/Phileas/policies`. You can edit the policies directly there using a text editor or you can use Phileas' [API](policies-api.md) to upload a policy. In this case we recommend just using a text editor on the Phileas instance to create a policy.

When using a text editor to create and edit a policy, be sure to save the policy often. Frequent saving can make editing a policy easier.

We also recommend considering to place your policy directory under source control to have a history and change log of your policies.

#### Creating a Policy

Make a copy of the default policy, and we will modify the copy for our needs.

`cp /opt/Phileas/policies/default.json /opt/Phileas/policies/evaluation.json`

Now open `/opt/Phileas/policies/evaluation.json` in a text editor. (The content of `evaluation.json` will be similar to what's shown below but may have minor differences between different versions of Phileas.)

```
{
   "name": "default",
   "identifiers": {
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      },
      "phoneNumber": {
         "phoneNumberFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

The first thing we need to do is to set the name of the policy. Replace `default` with `evaluation` and save the file.

#### Identifying the Filters You Need

The rest of the file contains the filters that are enabled in the default policy. We need to make sure that each type of sensitive information that you want to redact is represented by a filter in this file. Look through the rest of the policy and determine which filters are listed that you do not need and also which filters you do need that are not listed.

#### Disabling Filters We Do Not Need

If a filter is listed in the policy and you do not need the filter you have two options. You can either delete those lines from the policy and save the file, or you can set the filter's `enabled` property to false. Using the `enabled` property allows you to keep the filter configuration in the policy in case it is needed later but both options have the same effect.

#### Enabling Filters Not in the Default Policy

Let's say you want to redact bitcoin addresses. The bitcoin address filter is not in the default policy. To add the bitcoin address filter we will refer to Phileas' documentation on the bitcoin address filter, get the configuration, and copy it into the policy.

From the [bitcoin address filter documentation](bitcoin-addresses.md) we see the configuration for the bitcoin address filter is:

```
      "bitcoinAddress": {
         "bitcoinAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
```

We can copy this configuration and paste it into our policy:

```
{
   "name": "evaluation",
   "identifiers": {
      "bitcoinAddress": {
         "bitcoinAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      },
      "emailAddress": {
         "emailAddressFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      },
      "phoneNumber": {
         "phoneNumberFilterStrategies": [
            {
               "strategy": "REDACT",
               "redactionFormat": "{{{REDACTED-%t}}}"
            }
         ]
      }
   }
}
```

The order of the filters in the policy does not matter and has no impact on performance. We typically place the filters in the policy alphabetically just to improve readability.

Repeat these steps until you have added a filter for each of the types of sensitive information you want to redact. Typically, the default redaction `strategy` and `redactionFormat` values for each filter should be fine for evaluation.

When finished modifying the policy, save the file and close the text editor. Now restart Phileas for the policy changes to be loaded:

```
sudo systemctl restart Phileas
```

#### Submitting Text for Redaction

With our policy in place we can now send text to Phileas for redaction using that policy:

```
PhileasConfiguration phileasConfiguration = ConfigFactory.create(PhileasConfiguration.class);

FilterService filterService = new PhileasFilterService(phileasConfiguration);

FilterResponse response = filterService.filter(policies, context, documentId, body, MimeType.TEXT_PLAIN);
```

The `explain` API [endpoint](filtering-api.md#explain) produces a detailed description of the redaction. The response will include a list of spans that contain the start and stop positions of redacted text and the type of sensitive information that was redacted. Using this information we can compare the redacted information to our annotated file to calculate precision and recall metrics.

#### Calculating Precision and Recall

Now we can calculate the precision and recall metrics.

* Precision is the number of true positives divided by the number true positives plus false positives.
* Recall is the number of true positives divided by the number of false negatives plus true positives.

![Calculating the precision and recall](Images/precision.png)

* The F-1 score is the harmonic mean of precision and recall.

![Calculating the F-1 score](Images/f1.png)
