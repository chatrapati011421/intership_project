package com.voting.app.service;

import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CompareFacesRequest;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.Image;

@Service
public class FaceService {

    // These values will be pulled from your application.properties
    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretAccessKey}")
    private String secretKey;

    public boolean compareFaces(String liveBase64, String storedBase64) {
        try {
            // 1. Setup AWS Credentials and Client
            // Replace Region.US_EAST_1 with your actual AWS region if different
            RekognitionClient client = RekognitionClient.builder()
                    .region(Region.US_EAST_1) 
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();

            // 2. Convert Base64 strings to SdkBytes for AWS
            SdkBytes liveBytes = SdkBytes.fromByteArray(Base64.getDecoder().decode(liveBase64));
            SdkBytes storedBytes = SdkBytes.fromByteArray(Base64.getDecoder().decode(storedBase64));

            // 3. Create the Request
            CompareFacesRequest request = CompareFacesRequest.builder()
                    .sourceImage(Image.builder().bytes(liveBytes).build())
                    .targetImage(Image.builder().bytes(storedBytes).build())
                    .similarityThreshold(95F) // Set to 95% for better reliability in testing
                    .build();

            // 4. Call AWS Rekognition
            CompareFacesResponse response = client.compareFaces(request);
            
            // Return true if AWS found a match
            return !response.faceMatches().isEmpty();

        } catch (Exception e) {
            System.err.println("AWS Face Comparison Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}