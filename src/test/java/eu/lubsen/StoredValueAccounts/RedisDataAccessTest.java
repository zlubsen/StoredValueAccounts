package eu.lubsen.StoredValueAccounts;

import java.io.IOException;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.redis.RedisOptions;

@RunWith(VertxUnitRunner.class)
public class RedisDataAccessTest {

	@Before
	public void setUp(TestContext context) throws IOException {
	}

	@After
	public void tearDown(TestContext context) {
	}

	private Controller getController() {
		Vertx vertx = Vertx.vertx();
		RedisOptions options = new RedisOptions();
		return new Controller(vertx, options);
	}

	@Test
	public void testAddAccount(TestContext context) {
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		Account account = new Account(new JsonObject().put("id", id).put("balance", 0).put("overdraft", 50));

		Future<Void> futAdd = controller.addAccount(account);
		futAdd.setHandler(context.asyncAssertSuccess());
	}

	@Test
	public void testGetAccount(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		Account expected = new Account(new JsonObject().put("id", id).put("balance", 0).put("overdraft", 50));

		Future<Void> futAdd = controller.addAccount(expected);
		futAdd.setHandler(resAdd -> {
			if (resAdd.succeeded()) {
				Future<Account> futGet = controller.getAccount(id);
				futGet.setHandler(resGet -> {
					if (resGet.succeeded()) {
						context.assertEquals(expected.toJson(), resGet.result().toJson());
					} else
						context.fail();
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testGetNonexistingAccount(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		Future<Account> futGet = controller.getAccount(id);
		futGet.setHandler(resGet -> {
			if (resGet.succeeded())
				context.assertNull(resGet.result());
			else
				context.fail();

			async.complete();
		});
	}

	@Test
	public void testAccountExists(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		Account expected = new Account(new JsonObject().put("id", id).put("balance", 0).put("overdraft", 50));

		Future<Void> futAdd = controller.addAccount(expected);
		futAdd.setHandler(resAdd -> {
			if (resAdd.succeeded()) {
				Future<Boolean> futExists = controller.accountExists(id);
				futExists.setHandler(resExists -> {
					if (resExists.succeeded()) {
						context.assertEquals(true, resExists.result());
					} else
						context.fail();
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testNonexistingAccountExists(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		Future<Boolean> futExists = controller.accountExists(id);
		futExists.setHandler(resExists -> {
			if (resExists.succeeded())
				context.assertEquals(false, resExists.result());
			else
				context.fail();

			async.complete();
		});
	}

	@Test
	public void testAddTransfer(TestContext context) {
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		int amount = 20;
		Account from = new Account(UUID.randomUUID().toString(), amount);
		Account to = new Account(UUID.randomUUID().toString(), 0);

		Transfer transfer = new Transfer(id, from.getId(), to.getId(), 20);

		Future<Void> futAdd = controller.addTransfer(transfer);
		futAdd.setHandler(context.asyncAssertSuccess());
	}

	@Test
	public void testGetTransfer(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		int amount = 20;
		Account from = new Account(UUID.randomUUID().toString(), amount);
		Account to = new Account(UUID.randomUUID().toString(), 0);

		Transfer expected = new Transfer(id, from.getId(), to.getId(), amount);

		Future<Void> futAdd = controller.addTransfer(expected);
		futAdd.setHandler(resAdd -> {
			if (resAdd.succeeded()) {
				Future<Transfer> futGet = controller.getTransfer(id);
				futGet.setHandler(resGet -> {
					if (resGet.succeeded()) {
						context.assertEquals(expected.toJson(), resGet.result().toJson());
					} else
						context.fail();
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testTransferExists(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		int amount = 20;
		Account from = new Account(UUID.randomUUID().toString(), amount);
		Account to = new Account(UUID.randomUUID().toString(), 0);

		Transfer expected = new Transfer(id, from.getId(), to.getId(), amount);

		Future<Void> futAdd = controller.addTransfer(expected);
		futAdd.setHandler(resAdd -> {
			if (resAdd.succeeded()) {
				Future<Boolean> futExists = controller.transferExists(id);
				futExists.setHandler(resExists -> {
					if (resExists.succeeded()) {
						context.assertEquals(true, resExists.result());
					} else
						context.fail();
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testUpdateTransferStatus(TestContext context) {
		Async async = context.async();
		Controller controller = getController();
		String id = UUID.randomUUID().toString();

		int amount = 20;
		Account from = new Account(UUID.randomUUID().toString(), amount);
		Account to = new Account(UUID.randomUUID().toString(), 0);

		Transfer expected = new Transfer(id, from.getId(), to.getId(), amount);

		context.assertEquals(expected.getStatus(), TransferStatus.PENDING);

		// add a transaction
		Future<Void> futAdd = controller.addTransfer(expected);
		futAdd.setHandler(resAdd -> {
			if (resAdd.succeeded()) {
				// update its status
				Future<Void> futStatus = controller.storage.updateTransferStatus(expected, TransferStatus.CONFIRMED);
				futStatus.setHandler(resStatus -> {
					if (resStatus.succeeded()) {
						// check the updated status by retrieving the transfer
						Future<Transfer> futGet = controller.getTransfer(id);
						futGet.setHandler(resGet -> {
							if (resGet.succeeded()) {
								context.assertEquals(expected.getStatus(), resGet.result().getStatus());
							} else
								context.fail();
						});
					} else {
						context.fail();
					}
				});

			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testProcessTransferValid(TestContext context) {
		Async async = context.async();
		Controller controller = getController();

		int overdraft = 20;
		int amount = 10;
		int expectedFromBalance = -amount;
		int expectedToBalance = amount;
		Account from = new Account(UUID.randomUUID().toString(), overdraft);
		Account to = new Account(UUID.randomUUID().toString(), 0);
		String transferId = UUID.randomUUID().toString();

		Transfer validTransfer = new Transfer(transferId, from.getId(), to.getId(), amount);

		// add a transaction and accounts
		Future<Void> futAdd = controller.addTransfer(validTransfer);
		Future<Void> futFrom = controller.addAccount(from);
		Future<Void> futTo = controller.addAccount(to);

		CompositeFuture.all(futAdd, futFrom, futTo).setHandler(ar -> {
			if (ar.succeeded()) {
				// process the transfer
				Future<Void> futProcess = controller.processTransfer(transferId);
				futProcess.setHandler(resProcess -> {
					if (resProcess.succeeded()) {
						// check the updated status by retrieving the transfer
						// and accounts
						Future<Transfer> futGetTx = controller.getTransfer(transferId);
						Future<Account> futGetFrom = controller.getAccount(from.getId());
						Future<Account> futGetTo = controller.getAccount(to.getId());

						CompositeFuture.all(futGetTx, futGetFrom, futGetTo).setHandler(res -> {
							if (res.succeeded()) {
								context.assertEquals(futGetTx.result().getStatus(), TransferStatus.CONFIRMED);
								context.assertEquals(futGetFrom.result().getBalance(), expectedFromBalance);
								context.assertEquals(futGetTo.result().getBalance(), expectedToBalance);
							} else
								context.fail();
						});
					} else {
						context.fail();
					}
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testProcessTransferDoesNotExist(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();

		String transferId = UUID.randomUUID().toString();

		Future<Void> futProcess = controller.processTransfer(transferId);
		futProcess.setHandler(ar -> {
			context.assertFalse(ar.succeeded());
			async.complete();
		});
	}

	@Test
	public void testProcessTransferAccountDoesNotExist(TestContext context) {
		final Async async = context.async();
		Controller controller = getController();

		int overdraft = 20;
		int amount = 10;
		int expectedFromBalance = 0;
		Account from = new Account(UUID.randomUUID().toString(), overdraft);
		String nonExistingAccountId = UUID.randomUUID().toString();
		String transferId = UUID.randomUUID().toString();

		Transfer validTransfer = new Transfer(transferId, from.getId(), nonExistingAccountId, amount);

		// add a transaction and accounts
		Future<Void> futAdd = controller.addTransfer(validTransfer);
		Future<Void> futFrom = controller.addAccount(from);

		CompositeFuture.all(futAdd, futFrom).setHandler(ar -> {
			if (ar.succeeded()) {
				// process the transfer
				Future<Void> futProcess = controller.processTransfer(transferId);
				futProcess.setHandler(resProcess -> {
					if (resProcess.failed()) {
						// check the updated status by retrieving the transfer
						Future<Transfer> futGetTx = controller.getTransfer(transferId);
						Future<Account> futGetFrom = controller.getAccount(from.getId());

						CompositeFuture.all(futGetTx, futGetFrom).setHandler(res -> {
							if (res.succeeded()) {
								context.assertEquals(futGetTx.result().getStatus(), TransferStatus.ACCOUNT_NOT_FOUND);
								context.assertEquals(futGetFrom.result().getBalance(), expectedFromBalance);
							} else
								context.fail();
						});
					} else {
						context.fail();
					}
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testProcessTransferInsufficientBalance(TestContext context) {
		Async async = context.async();
		Controller controller = getController();

		int overdraft = 0;
		int amount = 10;
		int expectedFromBalance = 0;
		int expectedToBalance = 0;
		Account from = new Account(UUID.randomUUID().toString(), overdraft);
		Account to = new Account(UUID.randomUUID().toString(), 0);
		String transferId = UUID.randomUUID().toString();

		Transfer insufficientTransfer = new Transfer(transferId, from.getId(), to.getId(), amount);

		// add a transaction and accounts
		Future<Void> futAdd = controller.addTransfer(insufficientTransfer);
		Future<Void> futFrom = controller.addAccount(from);
		Future<Void> futTo = controller.addAccount(to);

		CompositeFuture.all(futAdd, futFrom, futTo).setHandler(ar -> {
			if (ar.succeeded()) {
				// process the transfer
				Future<Void> futProcess = controller.processTransfer(transferId);
				futProcess.setHandler(resProcess -> {
					if (resProcess.succeeded()) {
						// check the updated status by retrieving the transfer
						Future<Transfer> futGetTx = controller.getTransfer(transferId);
						Future<Account> futGetFrom = controller.getAccount(from.getId());
						Future<Account> futGetTo = controller.getAccount(to.getId());

						CompositeFuture.all(futGetTx, futGetFrom, futGetTo).setHandler(res -> {
							if (res.succeeded()) {
								context.assertEquals(futGetTx.result().getStatus(), TransferStatus.INSUFFICIENT_FUNDS);
								context.assertEquals(futGetFrom.result().getBalance(), expectedFromBalance);
								context.assertEquals(futGetTo.result().getBalance(), expectedToBalance);
							} else
								context.fail();
						});						
					} else {
						context.fail();
					}
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}

	@Test
	public void testProcessTransferInsufficientOverdraft(TestContext context) {
		Async async = context.async();
		Controller controller = getController();

		int overdraft = 10;
		int amount = 20;
		int expectedFromBalance = 0;
		int expectedToBalance = 0;
		Account from = new Account(UUID.randomUUID().toString(), overdraft);
		Account to = new Account(UUID.randomUUID().toString(), 0);
		String transferId = UUID.randomUUID().toString();

		Transfer insufficientTransfer = new Transfer(transferId, from.getId(), to.getId(), amount);

		// add a transaction and accounts
		Future<Void> futAdd = controller.addTransfer(insufficientTransfer);
		Future<Void> futFrom = controller.addAccount(from);
		Future<Void> futTo = controller.addAccount(to);

		CompositeFuture.all(futAdd, futFrom, futTo).setHandler(ar -> {
			if (ar.succeeded()) {
				// process the transfer
				Future<Void> futProcess = controller.processTransfer(transferId);
				futProcess.setHandler(resProcess -> {
					if (resProcess.succeeded()) {
						// check the updated status by retrieving the transfer
						Future<Transfer> futGetTx = controller.getTransfer(transferId);
						Future<Account> futGetFrom = controller.getAccount(from.getId());
						Future<Account> futGetTo = controller.getAccount(to.getId());

						CompositeFuture.all(futGetTx, futGetFrom, futGetTo).setHandler(res -> {
							if (res.succeeded()) {
								context.assertEquals(futGetTx.result().getStatus(), TransferStatus.INSUFFICIENT_FUNDS);
								context.assertEquals(futGetFrom.result().getBalance(), expectedFromBalance);
								context.assertEquals(futGetTo.result().getBalance(), expectedToBalance);
							} else
								context.fail();
						});
					} else {
						context.fail();
					}
				});
			} else {
				context.fail();
			}
			async.complete();
		});
	}
}