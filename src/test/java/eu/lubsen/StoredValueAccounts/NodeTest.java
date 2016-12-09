package eu.lubsen.StoredValueAccounts;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class NodeTest {
	private Vertx vertx;

	int port;
	String host = "localhost";

	@Before
	public void setUp(TestContext context) throws IOException {
		vertx = Vertx.vertx();
		ServerSocket socket = new ServerSocket(0);
		port = socket.getLocalPort();
		socket.close();
		DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
		vertx.deployVerticle(Node.class.getName(), options, context.asyncAssertSuccess());
	}

	@After
	public void tearDown(TestContext context) {
		vertx.close(context.asyncAssertSuccess());
	}

	@Test
	public void testGetAccountNotExisting(TestContext context) {
		final Async async = context.async();
		String route = "/account";
		
		vertx.createHttpClient().getNow(port, host, route, response -> {
			context.assertEquals(response.statusCode(), 404);
			async.complete();
		});
	}

	@Test
	public void testAddAccount(TestContext context) {
		final Async async = context.async();
		String route = "/account";

		int overdraftValue = 50;
		JsonObject requestBody = new JsonObject().put("overdraft", overdraftValue);

		vertx.createHttpClient().post(port, host, route, response -> {
			String returnedLocation = response.headers().get("Location");
			String uriPart = (host + ":" + port + route + "/");
			int expectedResultLength = (uriPart + UUID.randomUUID().toString()).length();

			context.assertEquals(response.statusCode(), 202);
			context.assertEquals(returnedLocation.length(), expectedResultLength);

			context.put("createdId", returnedLocation.substring(uriPart.length()));
			context.put("overdraftValue", overdraftValue);
			async.complete();
		}).end(requestBody.encodePrettily());
	}
}
