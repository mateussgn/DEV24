package com.company.service;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@RestController
public class MyObjectObjectAction {

  private static final String GET_EMPLOYEES_AGE_ENDPOINT_URL = "https://api.agify.io";

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE,
      value = "/myobject/action")
  public ResponseEntity<String> create(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody String json)
    throws JsonMappingException, JsonProcessingException {

    System.out.println("\n============================\n");

    System.out.println("JWT ID: " + jwt.getId());
    System.out.println("JWT SUBJECT: " + jwt.getSubject());
    System.out.println("JWT CLAIMS: " + jwt.getClaims());

    System.out.println("\n============================\n");

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(json);

    String msg = jsonNode.toString();
    System.out.println(msg);

    System.out.println("\n============================\n");

    Long objectEntryId =
            jsonNode.path("objectEntry")
                    .path("objectEntryId")
                    .asLong(0);

    System.out.println("ObjectEntryId: " + objectEntryId);

    String name =
            jsonNode.path("objectEntry")
                    .path("values")
                    .path("name")
                    .asText("No name");

    System.out.println("Name: " + name);

    System.out.println("\n============================\n");

    updateObjectEntry(objectEntryId, getAgeByName(name), jwt);

    System.out.println("\n============================\n");

    return new ResponseEntity<>(msg, HttpStatus.CREATED);
  }

  private Integer getAgeByName(String name) throws JsonProcessingException {

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

    HttpEntity<String> entity = new HttpEntity <String> ("parameters", headers);

    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity <JsonNode> result = restTemplate.exchange(
            GET_EMPLOYEES_AGE_ENDPOINT_URL + "?name=" + name, HttpMethod.GET, entity, JsonNode.class);

    System.out.println("\n============================\n");

    System.out.println("Body: " + result.getBody());

    JsonNode jsonNode = result.getBody();

    System.out.println("\n============================\n");

    Integer age = 0;
    if (jsonNode != null) {
      age = jsonNode.path("age").intValue();
    }

    System.out.println("Age: " + age);

    System.out.println("\n============================\n");

    return age;
  }

  private void updateObjectEntry(Long objectEntryId, Integer age, Jwt jwt) {

    WebClient _webClient = WebClient.builder().baseUrl(
            "https://".concat("dxp.lfr.dev")
    ).defaultHeader(
            HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE
    ).defaultHeader(
            HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE
    ).build();

    _webClient.put().uri(
            "/o/c/employees/"+objectEntryId
    ).header(
            HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue()
    ).bodyValue(
            new ObjectMapper().createObjectNode().put(
                    "age", age
            )
    ).exchangeToMono(
            r -> {
              if (r.statusCode().equals(HttpStatus.OK)) {
                return r.bodyToMono(String.class);
              }
              else if (r.statusCode().is4xxClientError()) {
                return Mono.just("Error response");
              }
              else {
                return r.createException()
                        .flatMap(Mono::error);
              }
            }
    ).doOnNext(System.out::println).subscribe();

  }

}
