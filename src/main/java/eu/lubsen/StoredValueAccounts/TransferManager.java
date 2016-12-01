package eu.lubsen.StoredValueAccounts;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransferManager {
	private Map<String, JsonObject> transfers;	
	public TransferManager() {
		this.transfers = new HashMap<>();
	}
	
	public String createUUID() {
		 return UUID.randomUUID().toString();
	}
	
	public void createTransfer(String transferId, String from, String to, int amount) {
		this.transfers.put(transferId,
				new JsonObject().put("id", transferId).put("from", from).put("to", to).put("amount", amount).put("status", "PENDING"));
		// TODO process transfer, to set definitive status
	}
	
	public JsonObject getTransfer(String transactionId) {
		return transfers.get(transactionId);
	}
	
	public String listTransfersJSON() {
		JsonArray arr = new JsonArray();
		transfers.forEach((k, v) -> arr.add(v));
		return arr.encodePrettily();
	}
}
