package eu.lubsen.StoredValueAccounts;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import org.apache.commons.lang3.StringUtils;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;

public class NodeVerticle extends AbstractVerticle {

	private HazelcastInstance hz;
	private Controller controller;
	private String nodeName;
	private boolean statusRunning = false;

	@Override
	public void start(Future<Void> fut) {
		this.nodeName = config().getString("node.name", "defaultNode");

		this.hz = Hazelcast.getHazelcastInstanceByName(config().getString("cluster.name","sva-cluster"));
		// setupCluster();
		setupManagers();

		Router router = Router.router(vertx);
		setupRoutes(router);
		vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("http.port", 8080),
				result -> {
					if (result.succeeded()) {
						fut.complete();
					} else {
						fut.fail(result.cause());
					}
				});

		statusRunning = true;
	}

	private void setupManagers() {
		this.controller = new Controller(hz.getReplicatedMap("accounts"), hz.getReplicatedMap("transfers"));
	}

	private void setupRoutes(Router router) {
		router.route().handler(BodyHandler.create());

		router.post("/account").handler(this::handleAddAccount);
		router.get("/account/:accountId").handler(this::handleGetAccount);

		router.post("/transfer").handler(this::handleMakeTransfer);
		router.get("/transfer/:transactionId").handler(this::handleGetTransfer);
		router.get("/transaction/:transactionId").handler(this::handleGetTransaction);

		router.get("/accounts").handler(this::handleListAccounts);
		router.get("/transfers").handler(this::handleListTransfers);
		router.get("/transactions").handler(this::handleListTransactions);

		router.get("/health").handler(this::handleHealth);
	}

	// The create account method must return immediately (i.e. within 25ms) ,
	// yielding a unique accountId. The HTTP-status must be 202 indicating that
	// the message is being processed, but may not yet be finished.
	// However, if a response is given, we do expect that in due time (i.e. 60
	// seconds) an account will be created. In the meantime the GET /account
	// command below shall return a HTTP 404 status.
	// The response should not return a body, but a location header:
	// /account/:accountId, pointing to the URL where the new account can be
	// retrieved once it is fully processed.
	private void handleAddAccount(RoutingContext routingContext) {
		// create an account with the overdraft value specified in the request
		// body 'overdraft' (>0, default is 0)

		String accountId = controller.createUUID();

		int overdraft = 0;
		JsonObject body = routingContext.getBodyAsJson();

		if (body.containsKey("overdraft")) {
			overdraft = validateOverdraftValue(body.getInteger("overdraft"));
		}

		controller.addAccount(new Account(accountId, overdraft));

		routingContext.response().setStatusCode(202)
				.putHeader("Location", routingContext.request().host() + "/account/" + accountId).end();
	}

	private int validateOverdraftValue(int overdraftInput) {
		if (overdraftInput > 0) {
			return overdraftInput;
		}
		return 0;
	}

	// The method yields the description of an account and HTTP-status 200. As
	// long as the account is not created yet, this function returns HTTP-status
	// 404.
	private void handleGetAccount(RoutingContext routingContext) {
		String accountId = routingContext.request().getParam("accountId");
		HttpServerResponse response = routingContext.response();
		if (accountId == null) {
			response.setStatusCode(400).end();
		} else {
			if (controller.accountExists(accountId)) {
				JsonObject account = controller.getAccount(accountId).toJson();
				if (account == null) {
					response.setStatusCode(404).end();
				} else {
					response.putHeader("content-type", "application/json").setStatusCode(200)
							.end(account.encodePrettily());
				}
			} else
				response.setStatusCode(404).end();
		}
	}

	// Like with account-creation, this method should return within a 25ms
	// threshold with HTTP-status 202. It returns a location header to
	// /transfer/:transactionId. If the method returns a 202, we expect that
	// within 60s a call to /transfer/:transactionId yields a status 200-result.
	// Before that time it may return a 404.
	// All transfers, successful or failed, can be inspected using: GET
	// /transfer/:transactionId
	// Input
	// from String The accountId of the source account.
	// to String The accountId of the destination account
	// amount Integer > 0 The positive amount, in cents, to be transferred.
	private void handleMakeTransfer(RoutingContext routingContext) {
		JsonObject requestBody = routingContext.getBodyAsJson();
		String from = requestBody.getString("from");
		String to = requestBody.getString("to");
		int amount = requestBody.getInteger("amount").intValue();

		if (amount > 0) {
			String transferId = controller.createUUID();
			controller.addTransfer(new Transfer(transferId, from, to, amount));

			routingContext.response().setStatusCode(202)
					.putHeader("Location", routingContext.request().host() + "/transfer/" + transferId).end();
			controller.processTransfer(transferId);
		} else
			routingContext.response().setStatusCode(400)
					.end("{\"error\":\"Amount to be transfered must be a positve integer (>0).\"}");
	}

	// All transfers, successful or failed, can be inspected using: GET
	// /transfer/:transactionId
	// Output
	// transactionId String The transaction identifier
	// from String The accountId of the source account
	// to String The accountId of the destination account
	// amount Integer > 0 The positive amount, in cents, transferred.
	// status String [PENDING | CONFIRMED | INSUFFICIENT_FUNDS |
	// ACCOUNT_NOT_FOUND ]
	private void handleGetTransfer(RoutingContext routingContext) {
		String transactionId = routingContext.request().getParam("transactionId");

		if (StringUtils.isBlank(transactionId)) {
			routingContext.response().setStatusCode(400).end();
		} else {
			if (controller.transferExists(transactionId)) {
				JsonObject transfer = controller.getTransfer(transactionId).toJson();
				routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
						.end(transfer.encodePrettily());
			} else
				routingContext.response().setStatusCode(404).end();
		}
	}

	// Only CONFIRMED transfers are saved as a transaction. Transactions can be
	// retrieved using GET /transaction/:transactionId. Should PENDING,
	// INSUFFICIENT_FUNDS or ACCOUNT_NOT_FOUND be requested through this
	// endpoint an HTTP status 404 is returned.
	// Output
	// transactionId String The transaction identifier, which is the same as the
	// corresponding CONFIRMED transfer.
	// from String The accountId of the source account
	// to String The accountId of the destination account
	// amount Integer > 0 The positive amount, in cents, transferred.
	// status String [PENDING | CONFIRMED | INSUFFICIENT_FUNDS |
	// ACCOUNT_NOT_FOUND ]
	private void handleGetTransaction(RoutingContext routingContext) {
		String transactionId = routingContext.request().getParam("transactionId");

		if (StringUtils.isBlank(transactionId)) {
			routingContext.response().setStatusCode(400).end();
		} else {
			if (controller.transferExists(transactionId)) {
				Transfer transfer = controller.getTransfer(transactionId);
				if (transfer.getStatus() != TransferStatus.CONFIRMED) {
					routingContext.response().setStatusCode(404).end();
				} else {
					routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
							.end(transfer.toJson().encodePrettily());
				}
			} else
				routingContext.response().setStatusCode(404).end();
		}
	}

	private void handleListAccounts(RoutingContext routingContext) {
		routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
				.end(controller.listAccountsJSON());
	}

	private void handleListTransfers(RoutingContext routingContext) {
		routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
				.end(controller.listTransfersJson());
	}

	private void handleListTransactions(RoutingContext routingContext) {
		routingContext.response().putHeader("content-type", "application/json").setStatusCode(200)
				.end(controller.listTransactionsJson());
	}

	private void handleHealth(RoutingContext routingContext) {
		JsonObject status = new JsonObject();
		status.put("nodeName", this.nodeName);
		status.put("clusterInstance", this.hz.toString());
		HttpServerResponse response = routingContext.response();
		if (statusRunning)
			response.setStatusCode(200).end(status.encodePrettily());
		else
			response.setStatusCode(503).end("Node not available, cluster not running (yet).");
	}
}
