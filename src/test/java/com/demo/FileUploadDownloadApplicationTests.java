package com.demo;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class FileUploadDownloadApplicationTests {

    public static final String BASE = "uploads/";

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() throws IOException {
        File file = new File(BASE);
        if (file.exists() && file.isDirectory()) {
            Stream.of(file.listFiles()).forEach(File::delete);
        } else {
            file.mkdir();
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File(BASE));
    }

    @Test
    public void testUpload() {
        String fileName = "file_" + getRandom(Integer.MAX_VALUE) + ".txt";
        File uploadedFile = Paths.get(BASE + fileName).toFile();
        byte[] file = new byte[getRandom(99) * 1024];

        ResponseEntity<Void> response = restTemplate.postForEntity("/uploader", prepareRequest(fileName, file), Void.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(true, uploadedFile.exists());
        assertEquals(true, uploadedFile.isFile());
        assertEquals(file.length, uploadedFile.length());
    }

    @Test
    public void testDuplicateUpload() {
        String fileName = "file_" + getRandom(Integer.MAX_VALUE) + ".txt";
        File uploadedFile = Paths.get(BASE + fileName).toFile();

        byte[] file = new byte[getRandom(99) * 1024];
        ResponseEntity<Void> response = restTemplate.postForEntity("/uploader", prepareRequest(fileName, file), Void.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        file = new byte[getRandom(99) * 1024];
        response = restTemplate.postForEntity("/uploader", prepareRequest(fileName, file), Void.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertEquals(file.length, uploadedFile.length());
    }

    @Test
    public void testFileSizeExceeds() {
        String fileName = "file_" + getRandom(Integer.MAX_VALUE) + ".txt";
        File uploadedFile = Paths.get(BASE + fileName).toFile();

        byte[] file = new byte[getRandom(1) * 1024 * 200];
        ResponseEntity<String> response = restTemplate.postForEntity("/uploader", prepareRequest(fileName, file), String.class);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(false, uploadedFile.exists());
        //assertTrue(org.hamcrest.Matchers.containsString("The field file exceeds its maximum permitted size").matches(response.getBody()));
    }

    @Test
    public void testDownload() {
        String fileName = "file_" + getRandom(Integer.MAX_VALUE) + ".txt";
        File uploadedFile = Paths.get("uploads/" + fileName).toFile();

        byte[] file = new byte[getRandom(9) * 1024];
        ResponseEntity<String> response = restTemplate.postForEntity("/uploader", prepareRequest(fileName, file), String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.ALL));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> fileResponse = restTemplate.exchange("/downloader?fileName=" + fileName, HttpMethod.GET, entity, byte[].class);

        assertEquals(HttpStatus.OK, fileResponse.getStatusCode());
        assertEquals(fileResponse.getBody().length, file.length);
        assertEquals(fileResponse.getBody().length, uploadedFile.length());
    }

    private HttpEntity<MultiValueMap<String, Object>> prepareRequest(String fileName, byte[] file) {
        MultiValueMap<String, String> fileInfo = new LinkedMultiValueMap<>();
        ContentDisposition fileDetails = ContentDisposition
                .builder("form-data")
                .name("file")
                .filename(fileName)
                .build();
        fileInfo.add(HttpHeaders.CONTENT_DISPOSITION, fileDetails.toString());
        HttpEntity<byte[]> fileEntity = new HttpEntity<>(file, fileInfo);

        MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        if (fileName != null) {
            parameters.add("fileName", fileName);
        }
        if (file != null) {
            parameters.add("file", fileEntity);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parameters, headers);

        return requestEntity;
    }

    private int getRandom(int upperBoundary) {
        Random random = new Random(1);
        return random.nextInt(upperBoundary) + 1;
    }
}
