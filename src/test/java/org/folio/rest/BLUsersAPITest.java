package org.folio.rest;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.folio.rest.tools.client.test.HttpClientMock2;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


@RunWith(VertxUnitRunner.class)
public class BLUsersAPITest {
  static Vertx vertx;
  static RequestSpecification okapi;
  static int okapiPort;
  /** port of BLUsersAPI */
  static int port;

  private static final String NOT_EXPIRED_PASSWORD_RESET_ACTION_ID = "5ac3b82d-a7d4-43a0-8285-104e84e01274";
  private static final String EXPIRED_PASSWORD_RESET_ACTION_ID = "16423d10-f403-4de5-a6e9-8e0add61bf5b";
  private static final String NONEXISTENT_PASSWORD_RESET_ACTION_ID = "41a9a229-6492-46ae-b9fc-017ba1e2705d";
  private static final String FAKE_USER_ID_PASSWORD_RESET_ACTION_ID = "2a604a02-666c-44b6-b238-e81f379f1eb4";
  private static final String USER_ID = "0bb4f26d-e073-4f93-afbc-dcc24fd88810";
  private static final String FAKE_USER_ID = "f2216cfc-4abb-4f54-85bb-4945c9fd91cb";

  @BeforeClass
  public static void before(TestContext context) {
    vertx = Vertx.vertx();
    vertx.exceptionHandler(context.exceptionHandler());

    okapiPort = NetworkUtils.nextFreePort();
    DeploymentOptions okapiOptions = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", okapiPort));
    vertx.deployVerticle(MockOkapi.class.getName(), okapiOptions, context.asyncAssertSuccess());

    insertData();

    port = NetworkUtils.nextFreePort();
    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", port).putNull(HttpClientMock2.MOCK_MODE));
    vertx.deployVerticle(RestVerticle.class.getName(), options, context.asyncAssertSuccess());

    RestAssured.port = port;

    RequestSpecBuilder builder = new RequestSpecBuilder();
    builder.addHeader("X-Okapi-URL", "http://localhost:" + okapiPort);
    builder.addHeader("X-Okapi-Tenant", "supertenant");
    builder.addHeader("X-Okapi-Token", token("supertenant", "maxi"));
    okapi = builder.build();
  }

  private static String token(String tenant, String user) {
    JsonObject payload = new JsonObject()
        .put("sub", user)
        .put("tenant", tenant);
    byte[] bytes = payload.encode().getBytes(StandardCharsets.UTF_8);
    return "dummyJwt." + Base64.getEncoder().encodeToString(bytes) + ".sig";
  }

  private static void insertData() {
    JsonObject userPost = new JsonObject()
        .put("username", "maxi")
        .put("id", USER_ID)
        .put("patronGroup", "b4b5e97a-0a99-4db9-97df-4fdf406ec74d")
        .put("active", true)
        .put("personal", new JsonObject().put("email", "maxi@maxi.com"));
    given().body(userPost.encode()).
    when().post("http://localhost:" + okapiPort + "/users").
    then().statusCode(201);

    JsonObject groupPost = new JsonObject().put("group", "staff")
        .put("desc", "people running the library")
        .put("id", "b4b5e97a-0a99-4db9-97df-4fdf406ec74d");
    given().body(groupPost.encode()).
    when().post("http://localhost:" + okapiPort + "/groups").
    then().statusCode(201);

    JsonObject permission = new JsonObject().
        put("permissionName", "ui-checkin.all").
        put("displayName", "Check in: All permissions").
        put("id", "604a6236-1c9d-4681-ace1-a0dd1bba5058");
    JsonObject permsUsersPost = new JsonObject()
        .put("permissions", new JsonArray().add(permission))
        .put("userId", USER_ID);
    given().body(permsUsersPost.encode()).
    when().post("http://localhost:" + okapiPort + "/perms/users").
    then().statusCode(201);

    given().body(new JsonObject()
      .put("module", "USERSBL")
      .put("configName", "fogottenData")
      .put("code", "userName")
      .put("description", "userName")
      .put("default", "false")
      .put("enabled", "true")
      .put("value", "username").encode()).
      when().post("http://localhost:" + okapiPort + "/configurations/entries").
      then().statusCode(201);

    given().body(new JsonObject()
      .put("module", "USERSBL")
      .put("configName", "fogottenData")
      .put("code", "phoneNumber")
      .put("description", "personal.phone, personal.mobilePhone")
      .put("default", "false")
      .put("enabled", "true")
      .put("value", "personal.phone, personal.mobilePhone").encode()).
      when().post("http://localhost:" + okapiPort + "/configurations/entries").
      then().statusCode(201);

    given().body(new JsonObject()
      .put("module", "USERSBL")
      .put("configName", "fogottenData")
      .put("code", "email")
      .put("description", "personal.email")
      .put("default", "false")
      .put("enabled", "true")
      .put("value", "personal.email").encode()).
      when().post("http://localhost:" + okapiPort + "/configurations/entries").
      then().statusCode(201);

    given().body(new JsonObject()
      .put("id", NOT_EXPIRED_PASSWORD_RESET_ACTION_ID)
      .put("userId", USER_ID)
      .put("expirationTime", Instant.now().plus(1, ChronoUnit.DAYS))
      .encode())
      .when().post("http://localhost:" + okapiPort + "/authn/password-reset-action")
      .then().statusCode(201);

    given().body(new JsonObject()
      .put("id", EXPIRED_PASSWORD_RESET_ACTION_ID)
      .put("userId", USER_ID)
      .put("expirationTime", Instant.now().minus(1, ChronoUnit.DAYS))
      .encode())
      .when().post("http://localhost:" + okapiPort + "/authn/password-reset-action")
      .then().statusCode(201);

    given().body(new JsonObject()
      .put("id", FAKE_USER_ID_PASSWORD_RESET_ACTION_ID)
      .put("userId", FAKE_USER_ID)
      .put("expirationTime", Instant.now().minus(1, ChronoUnit.DAYS))
      .encode())
      .when().post("http://localhost:" + okapiPort + "/authn/password-reset-action")
      .then().statusCode(201);
  }

  @AfterClass
  public static void after(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void getBlUsers(TestContext context) {
    given().
            spec(okapi).port(port).
    when().
            get("/bl-users").
    then().
            statusCode(200).
            body("compositeUsers[0].users.username", equalTo("maxi"));
  }

  @Test
  public void postBlUsersForgottenPassword(TestContext context) {
    given().
      spec(okapi).port(port).
      body(new JsonObject().put("id", "maxi").encode()).
      accept("text/plain").
      contentType("application/json").
      when().
      post("/bl-users/forgotten/password").
      then().
      statusCode(204);
  }

  @Test
  public void postBlUsersForgottenUsername(TestContext context) {
    given().
      spec(okapi).port(port).
      body(new JsonObject().put("id", "maxi@maxi.com").encode()).
      accept("text/plain").
      contentType("application/json").
      when().
      post("/bl-users/forgotten/username").
      then().
      statusCode(204);
  }

  @Test
  public void postBlUsersUpdatePasswordFail(TestContext context) {
    given().
      spec(okapi).port(port).
      body(new JsonObject().put("username", "superuser")
        .put("password", "12345")
        .put("newPassword", "123456")
        .put("userId", "99999999-9999-9999-9999-999999999999")
        .encode()).
      accept("text/plain").
      contentType("application/json").
      when().
      post("/bl-users/settings/myprofile/password").
      then().
      statusCode(400);
  }

  @Test
  public void postBlUsersUpdatePasswordInvalidOldPassword(TestContext context) {
    given().
      spec(okapi).port(port).
      body(new JsonObject().put("username", "superuser")
        .put("password", "123456")
        .put("newPassword", "1q2w3E!190")
        .put("userId", "99999999-9999-9999-9999-999999999999")
        .encode()).
      accept("text/plain").
      contentType("application/json").
      when().
      post("/bl-users/settings/myprofile/password").
      then().
      statusCode(401);
  }

  @Test
  public void postBlUsersUpdatePasswordOk(TestContext context) {
    given().
      spec(okapi).port(port).
      body(new JsonObject().put("username", "superuser")
        .put("password", "12345")
        .put("newPassword", "1q2w3E!190")
        .put("userId", "99999999-9999-9999-9999-999999999999")
        .encode()).
      accept("text/plain").
      contentType("application/json").
      when().
      post("/bl-users/settings/myprofile/password").
      then().
      statusCode(204);
  }

  @Test
  public void postBlUsersUpdatePasswordNoUser(TestContext context) {
    given().
      spec(okapi).port(port).
      body(new JsonObject().put("username", "superuser")
        .put("password", "12345")
        .put("newPassword", "1q2w3E!190")
        .put("userId", "99999999-9999-9999-9999-999999999991")
        .encode()).
      accept("text/plain").
      contentType("application/json").
      when().
      post("/bl-users/settings/myprofile/password").
      then().
      statusCode(500);
  }

  @Test
  public void postBlUsersPasswordResetValidate() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-url", "http://localhost:" + okapiPort))
      .header(new Header("x-okapi-token", buildToken(NOT_EXPIRED_PASSWORD_RESET_ACTION_ID)))
      .header(new Header("x-okapi-tenant", "supertenant"))
      .when()
      .post("/bl-users/password-reset/validate")
      .then()
      .statusCode(HttpStatus.SC_OK);
  }

  @Test
  public void postBlUsersPasswordResetValidateExpiredAction() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-url", "http://localhost:" + okapiPort))
      .header(new Header("x-okapi-token", buildToken(EXPIRED_PASSWORD_RESET_ACTION_ID)))
      .header(new Header("x-okapi-tenant", "supertenant"))
      .when()
      .post("/bl-users/password-reset/validate")
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void postBlUsersPasswordResetValidateNonexistentAction() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-url", "http://localhost:" + okapiPort))
      .header(new Header("x-okapi-token", buildToken(NONEXISTENT_PASSWORD_RESET_ACTION_ID)))
      .header(new Header("x-okapi-tenant", "supertenant"))
      .when()
      .post("/bl-users/password-reset/validate")
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void postBlUsersPasswordResetValidateFakeUserId() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-url", "http://localhost:" + okapiPort))
      .header(new Header("x-okapi-token", buildToken(FAKE_USER_ID_PASSWORD_RESET_ACTION_ID)))
      .header(new Header("x-okapi-tenant", "supertenant"))
      .when()
      .post("/bl-users/password-reset/validate")
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void postPasswordReset() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-user-id", "99999999-9999-9999-9999-999999999999"))
      .port(port)
      .body(new JsonObject()
        .put("resetPasswordActionId", NOT_EXPIRED_PASSWORD_RESET_ACTION_ID)
        .put("newPassword", "1q2w3E!190").encode())
      .accept("text/plain")
      .contentType("application/json")
      .when()
      .post("/bl-users/password-reset/reset")
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  @Test
  public void postPasswordResetInvalidPassword() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-user-id", "99999999-9999-9999-9999-999999999999"))
      .port(port)
      .body(new JsonObject()
        .put("resetPasswordActionId", NOT_EXPIRED_PASSWORD_RESET_ACTION_ID)
        .put("newPassword", "123456").encode())
      .accept("text/plain")
      .contentType("application/json")
      .when()
      .post("/bl-users/password-reset/reset")
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void postPasswordResetNonexistentAction() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-user-id", "99999999-9999-9999-9999-999999999999"))
      .port(port)
      .body(new JsonObject()
        .put("resetPasswordActionId", NONEXISTENT_PASSWORD_RESET_ACTION_ID)
        .put("newPassword", "1q2w3E!190").encode())
      .accept("text/plain")
      .contentType("application/json")
      .when()
      .post("/bl-users/password-reset/reset")
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void postPasswordResetExpiredAction() {
    given()
      .spec(okapi)
      .header(new Header("x-okapi-user-id", "99999999-9999-9999-9999-999999999999"))
      .port(port)
      .body(new JsonObject()
        .put("resetPasswordActionId", EXPIRED_PASSWORD_RESET_ACTION_ID)
        .put("newPassword", "1q2w3E!190").encode())
      .accept("text/plain")
      .contentType("application/json")
      .when()
      .post("/bl-users/password-reset/reset")
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  private String buildToken(String passwordResetActionId) {
    JsonObject payload = new JsonObject()
      .put("passwordResetActionId", passwordResetActionId);
    byte[] bytes = payload.encode().getBytes(StandardCharsets.UTF_8);
    return "dummyJwt." + Base64.getEncoder().encodeToString(bytes) + ".sig";
  }
}
