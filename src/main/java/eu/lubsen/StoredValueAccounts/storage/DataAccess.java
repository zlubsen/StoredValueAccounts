package eu.lubsen.StoredValueAccounts.storage;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;
import io.vertx.core.Future;

public interface DataAccess {
	public Future<Boolean> accountExists(String id);

	public Future<Void> addAccount(Account account);

	public Future<Account> getAccount(String id);

	public Future<Boolean> transferExists(String id);

	public Future<Void> addTransfer(Transfer transfer);

	public Future<Transfer> getTransfer(String id);
	
	public Future<Void> updateTransferStatus(Transfer transfer, TransferStatus status);

	public Future<String> listAccountsJson();

	public Future<String> listTransfersJson();

	public Future<String> listTransactionsJson();
}