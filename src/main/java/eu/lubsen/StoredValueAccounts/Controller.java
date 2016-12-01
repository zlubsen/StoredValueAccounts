package eu.lubsen.StoredValueAccounts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.lubsen.entities.Account;
import eu.lubsen.entities.Transfer;
import eu.lubsen.entities.TransferStatus;
import io.vertx.core.json.JsonArray;

public class Controller {
	private Map<String, Account> accounts;
	private Map<String, Transfer> transfers;

	public Controller() {
		this.accounts = new HashMap<>();
		this.transfers = new HashMap<>();
	}

	public String createUUID() {
		return UUID.randomUUID().toString();
	}

	public String listAccountsJsonJ() {
		JsonArray arr = new JsonArray();
		transfers.forEach((k, v) -> arr.add(v.toJson()));
		return arr.encodePrettily();
	}

	public String listTransfersJson() {
		JsonArray arr = new JsonArray();
		transfers.forEach((k, v) -> arr.add(v.toJson()));
		return arr.encodePrettily();
	}

	public String listTransactionsJson() {
		JsonArray arr = new JsonArray();
		transfers.forEach((k, v) -> {
			if (v.getStatus() == TransferStatus.CONFIRMED)
				arr.add(v.toJson());
		});
		return arr.encodePrettily();
	}

	public boolean accountExists(String id) {
		return accounts.containsKey(id);
	}

	public void addAccount(Account account) {
		this.accounts.put(account.getId(), account);
	}

	public Account getAccount(String id) {
		return this.accounts.get(id);
	}
	
	public boolean transferExists(String id) {
		return transfers.containsKey(id);
	}

	public void addTransfer(Transfer transfer) {
		this.transfers.put(transfer.getId(), transfer);
	}

	public Transfer getTransfer(String id) {
		return this.transfers.get(id);
	}

	public String listAccountsJSON() {
		JsonArray arr = new JsonArray();
		accounts.forEach((k, v) -> arr.add(v.toJson()));
		return arr.encodePrettily();
	}

	public void processTransfer(String transferId) {
		Transfer transfer = transfers.get(transferId);
		String from = transfer.getFromAccount();
		String to = transfer.getToAccount();

		if (accounts.containsKey(from) && accounts.containsKey(to)) {
			if (accounts.get(from).approveDeduction(transfer.getAmount())) {
				accounts.get(from).deductAmount(transfer.getAmount());
				accounts.get(to).addAmount(transfer.getAmount());
				transfer.setStatus(TransferStatus.CONFIRMED);
			} else
				transfer.setStatus(TransferStatus.INSUFFICIENT_FUNDS);
		} else {
			transfer.setStatus(TransferStatus.ACCOUNT_NOT_FOUND);
		}
	}
}
