package org.folio.rest;

import org.folio.rest.tools.client.test.HttpClientMock2;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;


/**
 * @author shale
 *
 */
@RunWith(VertxUnitRunner.class)
public class HTTPMockTest {

  private static Vertx      vertx;
  int                       port;

  @Before
  public void setUp(TestContext context) throws Exception {
    System.setProperty(HttpClientMock2.MOCK_MODE, "true");

    vertx = Vertx.vertx();

    port = NetworkUtils.nextFreePort();

    DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port",
      port));
    TestUtil.deploy(RestVerticle.class, options, vertx, context);
  }

  @After
  public void tearDown(TestContext context) throws Exception {
    vertx.close(context.asyncAssertSuccess());
    System.clearProperty(HttpClientMock2.MOCK_MODE);
  }

  @Test
  public void dummyTest(TestContext context) {
    context.async().complete();
  }

  /** Commented out until I can figure out the HttpClientMock2 thing
  @Test
  public void test(TestContext context) {
    HttpModuleClient2 httpClient = new HttpModuleClient2("localhost", port, "user_bl2");
    Async async = context.async();

    try {

      CompletableFuture<Response> response = httpClient.request("/bl-users?include=perms");
      response.whenComplete( (resp, ex) -> {
      try {
        assertEquals(200, resp.getCode());
        if(resp.getError() != null){
          System.out.println("------------------"+resp.getError().encode());
        }
        async.complete();
      } catch (Throwable e) {
        context.fail(e.getMessage());
      }
      });
    } catch (Exception e1) {
      context.fail(e1.getMessage());
    }


  }
  */



}
