package eu.lubsen.StoredValueAccounts.entities;

import java.io.Serializable;

import io.vertx.core.json.JsonObject;

public class Transfer implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String transactionId;
	private String fromAccount;
	private String toAccount;
	private int amount;
	private TransferStatus status;

	public Transfer(String transactionId, String from, String to, int amount) {
		this.transactionId = transactionId;
		this.fromAccount = from;
		this.toAccount = to;
		this.amount = amount;
		this.status = TransferStatus.PENDING;
	}

	public Transfer(JsonObject jsonTransfer) {
		this.transactionId = jsonTransfer.getString("transactionId");
		this.fromAccount = jsonTransfer.getString("from");
		this.toAccount = jsonTransfer.getString("to");
		this.amount = jsonTransfer.getInteger("amount").intValue();
		this.status = TransferStatus.valueOf(jsonTransfer.getString("status"));
	}

	public JsonObject toJson() {
		return new JsonObject().put("transactionId", this.transactionId).put("from", this.fromAccount).put("to", this.toAccount)
				.put("amount", this.amount).put("status", this.status);
	}

	public String getId() {
		return this.transactionId;
	}

	public String getFromAccount() {
		return this.fromAccount;
	}

	public String getToAccount() {
		return this.toAccount;
	}

	public int getAmount() {
		return this.amount;
	}

	public TransferStatus getStatus() {
		return this.status;
	}

	public void setStatus(TransferStatus status) {
		this.status = status;
	}
}
