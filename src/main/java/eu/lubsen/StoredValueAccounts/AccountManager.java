package eu.lubsen.StoredValueAccounts;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AccountManager {
	
	private Map<String, JsonObject> accounts;

	public AccountManager() {
		this.accounts = new HashMap<>();
	}
	
	public String createUUID() {
		 return UUID.randomUUID().toString();
	}
	
	public void createAccount(String accountId, String overdraftAmount) {
		this.accounts.put(accountId,
				new JsonObject().put("id", accountId).put("balance", "0").put("overdraft", overdraftAmount));
	}

	public boolean isValidOverdraft(String value) {
		int overdraft = Integer.valueOf(value);

		if (overdraft > 0)
			return true;

		return false;
	}
	
	// checks whether a transfer can become a valid transaction.
	// returns the resulting status of the transfer.
	public String validateTransfer(JsonObject transfer) {
		String fromId = transfer.getString("from");
		Integer amount = transfer.getInteger("amount");
		
		JsonObject fromAccount = accounts.get(fromId);
		
		if(amount <= fromAccount.getInteger("balance") + fromAccount.getInteger("overdraft")) {
			return "CONFIRMED";
		}
		return "";
	}
	
	public void processTransfer(JsonObject transfer) {
		JsonObject fromAccount = accounts.get(transfer.getString("from"));
		JsonObject toAccount = accounts.get(transfer.getString("to"));
		
		fromAccount.put("balance", fromAccount.getInteger("balance") - transfer.getInteger("amount"));
		toAccount.put("balance", toAccount.getInteger("balance") + transfer.getInteger("amount"));
		transfer.put("status", "CONFIRMED");
	}
	
	public JsonObject getAccount(String accountId) {
		return accounts.get(accountId);
	}
	
	public boolean isValidAccount(String accountId) {
		return accounts.containsKey(accountId);
	}
	
	public String listAccountsJSON() {
		JsonArray arr = new JsonArray();
		accounts.forEach((k, v) -> arr.add(v));
		return arr.encodePrettily();
	}
}
