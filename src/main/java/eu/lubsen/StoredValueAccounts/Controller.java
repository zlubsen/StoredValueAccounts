package eu.lubsen.StoredValueAccounts;

import java.util.Map;
import java.util.UUID;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;
import eu.lubsen.StoredValueAccounts.storage.DataAccess;
import eu.lubsen.StoredValueAccounts.storage.MapDataAccess;
import eu.lubsen.StoredValueAccounts.storage.RedisDataAccess;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.redis.RedisOptions;

public class Controller {
	DataAccess storage;

	public Controller(Map<String, Account> replicatedAccountsMap, Map<String, Transfer> replicatedTransfersMap) {
		this.storage = new MapDataAccess(replicatedAccountsMap, replicatedTransfersMap);
	}

	public Controller(Vertx vertx, RedisOptions options) {
		this.storage = new RedisDataAccess(vertx, options);
	}

	public Controller(RedisDataAccess da) {
		this.storage = da;
	}

	public String createUUID() {
		return UUID.randomUUID().toString();
	}

	public Future<String> listAccountsJson() {
		return storage.listAccountsJson();
	}

	public Future<String> listTransfersJson() {
		return storage.listTransfersJson();
	}

	public Future<String> listTransactionsJson() {
		return storage.listTransactionsJson();
	}

	public Future<Boolean> accountExists(String id) {
		return storage.accountExists(id);
	}

	public Future<Void> addAccount(Account account) {
		return storage.addAccount(account);
	}

	public Future<Account> getAccount(String id) {
		return storage.getAccount(id);
	}

	public Future<Boolean> transferExists(String id) {
		return storage.transferExists(id);
	}

	public Future<Void> addTransfer(Transfer transfer) {
		return storage.addTransfer(transfer);
	}

	public Future<Transfer> getTransfer(String id) {
		return storage.getTransfer(id);
	}

	public Future<Void> processTransfer(String transferId) {
		// check whether the two accounts exist
		// get both accounts
		// when both exist: execute transfer, else set status to
		// ACCOUNT_NOT_FOUND

		Future<Void> processFuture = Future.future();
		// if transfer exists
		Future<Boolean> futTxExists = this.transferExists(transferId);

		futTxExists.compose(exists -> {
			Future<Transfer> futGetTx;
			if (exists) {
				// get the transfer object
				futGetTx = this.getTransfer(transferId);
				return futGetTx;
			} else {
				futGetTx = Future.failedFuture("Transfer with id " + transferId + " not found.");
			}
			return futGetTx;
		}).compose(tx -> {
			// both accounts exists?
			Future<Transfer> futProceed = Future.future();
			Future<Boolean> futFromExists = this.accountExists(tx.getFromAccount());
			Future<Boolean> futToExists = this.accountExists(tx.getToAccount());

			CompositeFuture.all(futFromExists, futToExists).setHandler(arCompFut -> {
				if (arCompFut.succeeded()) {
					// continue composition
					if (futFromExists.result() && futToExists.result()) {
						futProceed.complete(tx);
					} else {
						(storage.updateTransferStatus(tx, TransferStatus.ACCOUNT_NOT_FOUND)).setHandler(arUpdate -> {
							futProceed.fail("Error processing transfer: at least one of the accounts was not found.");
						});
					}
				} else {
					(storage.updateTransferStatus(tx, TransferStatus.ACCOUNT_NOT_FOUND)).setHandler(arUpdate -> {
						futProceed.fail("Error processing transfer: could not determine if the accounts exist.");
					});
				}
			});
			return futProceed;
		}).compose(tx -> {
			// we can execute the transfer
			Future<Void> futExecute = executeTransfer(tx);
			futExecute.setHandler(ar -> processFuture.complete());
		}, processFuture);

		return processFuture;
	}

	private Future<Void> executeTransfer(Transfer transfer) {
		Future<Void> fut = Future.future();

		Future<Account> futFrom = storage.getAccount(transfer.getFromAccount());
		Future<Account> futTo = storage.getAccount(transfer.getToAccount());

		CompositeFuture.all(futFrom, futTo).setHandler(ar -> {
			Account from = futFrom.result();
			Account to = futTo.result();

			Future<Void> futUpdateFrom, futUpdateTo, futUpdateStatus;

			if (from.approveDeduction(transfer.getAmount())) {
				from.deductAmount(transfer.getAmount());
				to.addAmount(transfer.getAmount());
				transfer.setStatus(TransferStatus.CONFIRMED);
				futUpdateFrom = storage.addAccount(from);
				futUpdateTo = storage.addAccount(to);
			} else {
				transfer.setStatus(TransferStatus.INSUFFICIENT_FUNDS);
				futUpdateFrom = Future.succeededFuture();
				futUpdateTo = Future.succeededFuture();
			}

			futUpdateStatus = storage.updateTransferStatus(transfer, transfer.getStatus());

			CompositeFuture.all(futUpdateFrom, futUpdateTo, futUpdateStatus).setHandler(res -> {
				if (res.succeeded())
					fut.complete();
				else
					fut.fail("Error executing transfer: failed to update the status of the transfer.");
			});
		});

		return fut;
	}
}
