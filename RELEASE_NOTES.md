# Phileas Release Notes

Issues whose identifiers start with `PHL-` were previously tracked in Jira before the project's issues were managed in GitHub.

## Version 2.6.0 (not yet released)

* PHL-313 - Remove OWNER project fork for managing application properties
* PHL-312 - Add death date detection similar to the birthdate detection
* PHL-311 - Return the probabilities of sentiment and offensiveness in addition to the predicted categories

## Version 2.5.0 - November 19, 2023

* PHL-309 - Load OpenNLP models from the classpath by default
* PHL-308 - Add sentiment as a condition

## Version 2.4.0 - October 17, 2023

* PHL-306 - Rename "filter profile" to "policy"
* PHL-305 - Allow dates to be shifted by a random value
* PHL-304 - Skip onnx runtime tests on OSX

## Version 2.3.0 - September 17, 2023

* PHL-277 - Combine PDFs image into a new PDF
* PHL-264 - Bring back Flair as PersonsV1 filter

## Version 2.2.0 - August 14, 2023

* PHL-277 - Combine PDFs image into a new PDF - **05/Sep/23 12:27** - Today 1:37
* PHL-264 - Bring back Flair as PersonsV1 filter

## Version 2.1.0 - May 16, 2023

* PHL-261 - Incorporate a sentence detector into the NER
* PHL-260 - Switch to Apache OpenNLP 2.0 instead of using ONNX Runtime directly
* PHL-259 - Allow for reading encryption values from environment variables
* PHL-258 - Implement format-preserving encryption

## Version 2.0.0 - February 5, 2022

* PHL-254 - Remove splitting capability
* PHL-251 - Replace phileas-ner with Java ONNX service

## Version 1.12.2 - December 20, 2021

* PHL-249 - Upgrade log4j to 2.16.0

## Version 1.12.1 - December 14, 2021

* PHL-249 - Upgrade log4j to 2.16.0

## Version 1.12.0 - December 14, 2021

* PHL-248 - Upgrade log4j to 2.15.0
* PHL-246 - Remove store from FilterService
* PHL-245 - Add S3 key to S3FilterProfileService
* PHL-244 - Allow user to set bounding boxes for PDF redaction
* PHL-243 - Allow for combining filter profiles per request
* PHL-242 - A date filter test is failing due to mid-month
* PHL-239 - Support dates like Aug. 31, 2020
* PHL-238 - Support ages like: 61 y/o
* PHL-233 - Filter profiles reference environment variable values
* PHL-227 - Support currency such as $.50
* PHL-173 - 9 digit zip codes without a delimiter are not found - Improvement

## Version 1.11.0 - June 7, 2021

* PHL-226 - Add currency filter
* PHL-222 - Add bank routing number filter
* PHL-218 - Add document analysis prior to filtering
* PHL-211 - Encapsulate the constructor arguments to the filters

## Version 1.10.0 - March 23, 2021

* PHL-217 - Change redisson delete to delete by index
* PHL-216 - Update project dependencies
* PHL-208 - Redact multiple occurrences of same span in line in PDF - Bug
* PHL-207 - Improve date year validation
* PHL-204 - Date "July 3, 2012" is not being identified - Bug
* PHL-201 - Add domain property to filter profile
* PHL-200 - Add properties to the filter profile to enable/disable post filters
* PHL-198 - Add property to NER filter to set a confidence threshold
* PHL-197 - Add function to return Span as a CSV
* PHL-196 - Create spans from LAPPS JSON
* PHL-195 - Include JPEG processing libraries for PDF extraction
* PHL-187 - PostFilter classes with empty constructors should be singletons
* PHL-183 - Adjust phone number confidence based on pattern
* PHL-162 - Fix RELATIVE date filter strategy for "October 2009" date

## Version 1.9.0 - January 19, 2021

* PHL-188 - Add street address filter
* PHL-184 - Remove new line characters from spans
* PHL-179 - Lowercase names in first name index
* PHL-178 - Surname filter tests aren't finding anything
* PHL-177 - Identify physician names
* PHL-176 - IDENTIFIER filter is finding just capitalized words
* PHL-175 - Add support for age format "64-year-old"
* PHL-174 - Ages should not identify "10 years"
* PHL-171 - Add redaction color for PDF
* PHL-169 - Add PDF document processing
* PHL-166 - Add redaction method to leave the last four digits
* PHL-165 - Redact dates to just the 4 digit year
* PHL-163 - Handle dates like 09-2021
* PHL-161 - Fix logger class names in date filter and identifier filter
* PHL-160 - Add optional period in ages
* PHL-159 - Increase ner.timeout.sec to a large value
* PHL-158 - Set -1 to split threshold to set no limit
* PHL-157 - Resolve NPEs with dictionary filter
* PHL-4 - Shift dates by some time period

## Version 1.8.0 - November 1, 2020

* PHL-155 - Maintain metrics on individual filter performance times
* PHL-148 - Read list of filter ignored terms from a file

## Version 1.7.0 - September 21, 2020

* PHL-154 - Add Prometheus monitoring endpoint
* PHL-152 - Add detect() method to filters
* PHL-151 - Make filter ignore lists be not case-sensitive
* PHL-150 - BloomDictionaryFilter needs to support phrases and not just words
* PHL-149 - Fix IBAN code validation - Bug
* PHL-147 - Allow for ignoring based on patterns
* PHL-146 - Allow timeout between Philter and Philter NER to be customizable
* PHL-145 - Split large text into multiple pieces
* PHL-141 - Read list of globally ignored words from a file
* PHL-140 - Add classification condition to IdentifierFilterStrategy
* PHL-139 - Allow spaces in IBAN codes

## Version 1.6.0 - June 8, 2020

* PHL-143 - Fix spans not going to the store - Bug
* PHL-142 - Add PhileasSystemLoader to OWNER
* PHL-137 - Create filter for mailing tracking numbers
* PHL-136 - Replace DropWizard metrics with micrometer
* PHL-135 - Change date parsing to use year instead of year of era - Improvement
* PHL-134 - Fix filter type for Mac address - Bug
* PHL-133 - Fix case sensitivity check for ignored terms - Bug
* PHL-132 - Fix credit card validation - Bug
* PHL-131 - Upgrade to JUnit 5
* PHL-129 - Change "label" to "classification" - Improvement
* PHL-128 - Fix ignore check for LuceneDictionaryFilter - Bug
* PHL-127 - Add fuzzy property to custom dictionary filter to trigger a bloom filter
* PHL-126 - Allow custom dictionaries to read terms from a file
* PHL-125 - Add tests for all FilterStrategy classes
* PHL-124 - Add condition for checking classification of a span
* PHL-123 - Expand US passport regex to only include valid first two digits
* PHL-122 - Fix mac address filter strategy assignment
* PHL-121 - Add option to disable metrics printed to the console
* PHL-120 - Set contextual words for each regex filter
* PHL-118 - Wrap regular expression patterns into an object

## Version 1.5.0 - April 30, 2020

* PHL-98 - Make cache generic and use cache for filter profiles
* PHL-97 - Incorporate the S3FilterProfileService into Phileas
* PHL-69 - Create "Section" filter that removes between start and end tags

## Version 1.4.0 - April 9, 2020

* PHL-96 - Upgrade libphonenumber to 8.12.1
* PHL-95 - Add token condition to NerFilterStrategy
* PHL-93 - Presence of a confidence condition on a phone number filter strategy causes the text to not be redacted
* PHL-92 - The token condition specifies quotes twice
* PHL-89 - Add detection framework
* PHL-87 - Drop ignored spans before overlapping spans
* PHL-86 - Use "input" and "token" consistently
* PHL-85 - StateAbbreviationFilter is making a span with the input and not the state
* PHL-84 - Capture a span's window when creating a span

## Version 1.3.1 - February 23, 2020

* PHL-82 - Allow client to set document ID
* PHL-78 - Switch to Java 11
* PHL-76 - Support credit card numbers with dashes
* PHL-75 - Add support for TIN to SsnFilter
* PHL-74 - Add filter for MAC addresses
* PHL-73 - Add a redaction option for a crypto value
* PHL-71 - Add replacement template value for the token
* PHL-70 - Add filter condition based on context

## Version 1.3.0 - January 27, 2020

* PHL-68 - When there are no strategies just redact
* PHL-67 - Don't identify "may" as a date
* PHL-66 - Check filter profile delete status
* PHL-65 - Span text should not end with a period or a space
* PHL-64 - Add regex for URLs containing IP addresses instead of domain name
* PHL-62 - Add option to URL filter to require http(s) protocol or www
* PHL-59 - Improve performance by tuning PyTorch filter client
* PHL-58 - Generate document ID based on input hash
* PHL-56 - Don't throw NPE when Philter-NER throws an error

## Version 1.2.0 - January 16, 2020

* PHL-55 - Fix issue where NER conditions are not being applied to NER spans
* PHL-54 - Add filter profile option to remove punctuation
* PHL-53 - Release 1.2.0
* PHL-52 - Move Status class from Phileas to Philter
* PHL-50 - Offer encrypted connections to Redis
* PHL-47 - Add enabled property to filter types in filter profile
* PHL-46 - Add option to require dates to be valid dates
* PHL-42 - Add option to find invalid credit cards
* PHL-35 - Add ignore lists specific to individual filters
* PHL-33 - Test multiple identifiers in a filter profile

## Version 1.1.0 - December 14, 2019

* PHL-41 - Fix replacement checks in strategies
* PHL-39 - Allow for the filters to be reloaded at runtime
* PHL-38 - Don't use spring boot for dependency management
* PHL-36 - Integrate Philter Profile Registry into Phileas
* PHL-34 - Make store implementation for Elasticsearch
* PHL-32 - In IdentifierFilter fix the name/label assignment
* PHL-31 - Automatically set Lucene distance based on the string length
* PHL-30 - Add ignore lists to filter profile schema
* PHL-29 - Include item text in Span
* PHL-24 - Allow for custom dictionary lookup in filters
* PHL-23 - Get philter-ner endpoint from Philter
* PHL-21 - Add filter profile schema
* PHL-19 - Provide an enhanced response to describe how the spans were found/removed
* PHL-18 - Set default values for filter profile fields
* PHL-16 - Report metrics per individual filter type
* PHL-15 - Add metrics prefix property
* PHL-13 - Do not require filter profile name for StaticFilterProfileService
* PHL-3 - Apply sensitivity level to NER entities

## Version 1.0.1

* PHL-11 - Fix issue where spans aren't getting applied - Bug
* PHL-10 - Remove unneeded guava dependency
* PHL-9 - Add license information to poms

## Version 1.0.0 - October 06, 2019

Initial release.

* PHL-7 - RedisAnonymizationCacheService needs containsValue implemented
* PHL-5 - Allow Identifier filters to specify regex