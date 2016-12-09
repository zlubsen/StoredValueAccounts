package eu.lubsen.entities;

import io.vertx.core.json.JsonObject;

public class Account {
	private String id;
	private int balance;
	private int overdraft = 0;

	public Account(String id, int overdraft) {
		this.id = id;
		this.balance = 0;
		this.overdraft = overdraft;
	}

	public Account(JsonObject jsonAccount) {
		this.id = jsonAccount.getString("id");
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
		return new JsonObject().put("id", this.id).put("balance", this.balance).put("overdraft", this.overdraft);
	}

	public String getId() {
		return this.id;
	}

	public int getBalance() {
		return this.balance;
	}

	public int getOverdraft() {
		return this.balance;
	}
}
