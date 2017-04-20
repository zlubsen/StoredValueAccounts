package eu.lubsen.StoredValueAccounts.storage;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

public class RedisDataAccess implements DataAccess {
	private RedisClient redis;

	private final String ACCOUNT_LABEL = "account:";
	private final String TRANSFER_LABEL = "transfer:";

	public RedisDataAccess(Vertx vertx, RedisOptions options) {
		this.redis = RedisClient.create(vertx, options);
	}

	@Override
	public Future<Boolean> accountExists(String id) {
		Future<Boolean> result = Future.future();

		redis.exists(ACCOUNT_LABEL + id, res -> {
			if (res.succeeded() && res.result() > 0) {
				result.complete(true);
			} else if (res.succeeded())
				result.complete(false);
			} else {
				result.fail(res.cause());
			}
		});

		return result;
	}

	@Override
	public Future<Void> addAccount(Account account) {
		Future<Void> result = Future.future();

		redis.set(ACCOUNT_LABEL + account.getId(), account.toJson().encodePrettily(), res -> {
			if (res.succeeded())
				result.complete();
			else
				result.fail(res.cause());
		});

		return result;
	}

	@Override
	public Future<Account> getAccount(String id) {
		Future<Account> result = Future.future();

		redis.get(ACCOUNT_LABEL + id, res -> {
			if (res.succeeded())
				if(res.result()!=null)
					result.complete(new Account(new JsonObject(res.result())));
				else 
					result.complete(null);
			else
				result.fail(res.cause());
		});

		return result;
	}

	@Override
	public Future<Boolean> transferExists(String id) {
		Future<Boolean> result = Future.future();

		redis.exists(TRANSFER_LABEL + id, res -> {
			if (res.succeeded()) {
				if (res.result() > 0)
					result.complete(true);
				else
					result.complete(false);
			} else {
				result.fail(res.cause());
			}
		});

		return result;
	}

	@Override
	public Future<Void> addTransfer(Transfer transfer) {
		Future<Void> result = Future.future();

		redis.set(TRANSFER_LABEL + transfer.getId(), transfer.toJson().encodePrettily(), res -> {
			if (res.succeeded())
				result.complete();
			else
				result.fail(res.cause());
		});

		return result;
	}

	@Override
	public Future<Transfer> getTransfer(String id) {
		Future<Transfer> result = Future.future();

		redis.get(TRANSFER_LABEL + id, res -> {
			if (res.succeeded())
				if(res.result()!=null)
					result.complete(new Transfer(new JsonObject(res.result())));
				else
					result.complete(null);
			else
				result.fail(res.cause());
		});

		return result;
	}

	@Override
	public Future<Void> updateTransferStatus(Transfer transfer, TransferStatus status) {
		transfer.setStatus(status);
		Future<Void> result = addTransfer(transfer);
		return result;
	}
	
	@Override
	public Future<String> listAccountsJson() {
		return Future.failedFuture("Not implemented.");

		/*
		 * JsonArray arr = new JsonArray(); this.accounts.forEach((k, v) ->
		 * arr.add(v.toJson())); return
		 * Future.succeededFuture(arr.encodePrettily());
		 */
	}

	@Override
	public Future<String> listTransfersJson() {
		return Future.failedFuture("Not implemented.");

		/*
		 * JsonArray arr = new JsonArray(); this.transfers.forEach((k, v) ->
		 * arr.add(v.toJson())); return
		 * Future.succeededFuture(arr.encodePrettily());
		 */
	}

	@Override
	public Future<String> listTransactionsJson() {
		return Future.failedFuture("Not implemented.");

		/*
		 * JsonArray arr = new JsonArray(); this.transfers.forEach((k, v) -> {
		 * if (v.getStatus() == TransferStatus.CONFIRMED) arr.add(v.toJson());
		 * }); return Future.succeededFuture(arr.encodePrettily());
		 */
	}
}
