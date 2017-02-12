package eu.lubsen.StoredValueAccounts.storage;

import java.util.Map;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

public class MapDataAccess implements DataAccess {
	private Map<String, Account> accounts;
	private Map<String, Transfer> transfers;

	public MapDataAccess(Map<String, Account> accountsMap, Map<String, Transfer> transfersMap) {
		this.accounts = accountsMap;
		this.transfers = transfersMap;
	}

	@Override
	public Future<Boolean> accountExists(String id) {
		return Future.succeededFuture(this.accounts.containsKey(id));
	}

	@Override
	public Future<Void> addAccount(Account account) {
		this.accounts.put(account.getId(), account);
		return Future.succeededFuture();
	}

	@Override
	public Future<Account> getAccount(String id) {
		return Future.succeededFuture(this.accounts.get(id));
	}

	@Override
	public Future<Boolean> transferExists(String id) {
		return Future.succeededFuture(this.transfers.containsKey(id));
	}

	@Override
	public Future<Void> addTransfer(Transfer transfer) {
		this.transfers.put(transfer.getId(), transfer);
		return Future.succeededFuture();
	}

	@Override
	public Future<Transfer> getTransfer(String id) {
		return Future.succeededFuture(this.transfers.get(id));
	}
	
	@Override
	public Future<Void> updateTransferStatus(Transfer transfer, TransferStatus status) {
		transfer.setStatus(status);
		//this.transfers.get(id).setStatus(status);
		return Future.succeededFuture();
	}

	@Override
	public Future<String> listAccountsJson() {
		JsonArray arr = new JsonArray();
		this.accounts.forEach((k, v) -> arr.add(v.toJson()));
		return Future.succeededFuture(arr.encodePrettily());
	}

	@Override
	public Future<String> listTransfersJson() {
		JsonArray arr = new JsonArray();
		this.transfers.forEach((k, v) -> arr.add(v.toJson()));
		return Future.succeededFuture(arr.encodePrettily());
	}

	@Override
	public Future<String> listTransactionsJson() {
		JsonArray arr = new JsonArray();
		this.transfers.forEach((k, v) -> {
			if (v.getStatus() == TransferStatus.CONFIRMED)
				arr.add(v.toJson());
		});
		return Future.succeededFuture(arr.encodePrettily());
	}
}