package eu.lubsen.StoredValueAccounts.entities;

import java.io.Serializable;

import io.vertx.core.json.JsonObject;

public class Account implements Serializable {
	private static final long serialVersionUID = 1L;

	private String accountId;
	private int balance;
	private int overdraft = 0;

	public Account(String accountId, int overdraft) {
		this.accountId = accountId;
		this.balance = 0;
		this.overdraft = overdraft;
	}

	public Account(JsonObject jsonAccount) {
		this.accountId = jsonAccount.getString("accountId");
		this.balance = jsonAccount.getInteger("balance").intValue();
		this.overdraft = jsonAccount.getInteger("overdraft").intValue();
	}

	public boolean approveDeduction(int amount) {
		return (this.balance + this.overdraft) >= amount;
	}

	public void deductAmount(int amount) {
		this.balance = this.balance - amount;
	}

	public void addAmount(int amount) {
		this.balance = this.balance + amount;
	}

	public JsonObject toJson() {
		return new JsonObject().put("accountId", this.accountId).put("balance", this.balance).put("overdraft", this.overdraft);
	}

	public String getId() {
		return this.accountId;
	}

	public int getBalance() {
		return this.balance;
	}

	public int getOverdraft() {
		return this.balance;
	}
}
