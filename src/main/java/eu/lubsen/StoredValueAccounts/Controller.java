package eu.lubsen.StoredValueAccounts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;
import io.vertx.core.json.JsonArray;

public class Controller {
	private Map<String, Account> accounts;
	private Map<String, Transfer> transfers;

	public Controller() {
		this.accounts = new HashMap<>();
		this.transfers = new HashMap<>();
	}
	
	public Controller(Map<String,Account> replicatedAccountsMap, Map<String,Transfer> replicatedTransfersMap) {
		this.accounts = replicatedAccountsMap;
		this.transfers = replicatedTransfersMap;
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

		if (accounts.containsKey(transfer.getFromAccount()) && accounts.containsKey(transfer.getToAccount())) {
			executeTransfer(transfer);
		} else {
			transfer.setStatus(TransferStatus.ACCOUNT_NOT_FOUND);
		}
		transfers.replace(transfer.getId(), transfer); // trigger replication
	}
	
	private void executeTransfer(Transfer transfer) {
		Account from = accounts.get(transfer.getFromAccount());
		Account to = accounts.get(transfer.getToAccount());
		if (from.approveDeduction(transfer.getAmount())) {
			from.deductAmount(transfer.getAmount());
			to.addAmount(transfer.getAmount());
			transfer.setStatus(TransferStatus.CONFIRMED);
			
			accounts.replace(from.getId(), from); // trigger replication
			accounts.replace(to.getId(), to); // trigger replication
		} else
			transfer.setStatus(TransferStatus.INSUFFICIENT_FUNDS);
	}
}
