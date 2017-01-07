package eu.lubsen.StoredValueAccounts;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import eu.lubsen.StoredValueAccounts.entities.Account;
import eu.lubsen.StoredValueAccounts.entities.Transfer;
import eu.lubsen.StoredValueAccounts.entities.TransferStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ControllerTest {

	@Test
	public void testAddAndGetAccount(TestContext context) {
		Controller controller = new Controller();
		String id = UUID.randomUUID().toString();

		Account expected = new Account(new JsonObject().put("id", id).put("balance", 0).put("overdraft", 50));

		controller.addAccount(expected);

		Account actual = controller.getAccount(id);

		context.assertEquals(expected.toJson(), actual.toJson());
	}

	@Test
	public void testProcessTransferValid(TestContext context) {
		int amount = 20;
		Account from = new Account(UUID.randomUUID().toString(), amount);
		Account to = new Account(UUID.randomUUID().toString(), 0);
		Transfer validTransfer = new Transfer(UUID.randomUUID().toString(), from.getId(), to.getId(), 20);

		Controller controller = new Controller();
		controller.addAccount(from);
		controller.addAccount(to);
		controller.addTransfer(validTransfer);

		context.assertEquals(validTransfer.getStatus(), TransferStatus.PENDING);
		context.assertEquals(from.getBalance(), 0);
		context.assertEquals(to.getBalance(), 0);

		controller.processTransfer(validTransfer.getId());

		context.assertEquals(validTransfer.getStatus(), TransferStatus.CONFIRMED);
		context.assertEquals(from.getBalance(), -amount);
		context.assertEquals(to.getBalance(), amount);
	}

	@Test
	public void testProcessTransferAccountNotFound(TestContext context) {
		int amount = 40;
		Account from = new Account(UUID.randomUUID().toString(), amount);
		Account to = new Account(UUID.randomUUID().toString(), 0);
		Transfer invalidTransferFrom = new Transfer(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
				to.getId(), 20);
		Transfer invalidTransferTo = new Transfer(UUID.randomUUID().toString(), from.getId(),
				UUID.randomUUID().toString(), 20);
		Transfer invalidTransferBoth = new Transfer(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
				UUID.randomUUID().toString(), 20);

		Controller controller = new Controller();
		controller.addAccount(from);
		controller.addAccount(to);
		controller.addTransfer(invalidTransferFrom);
		controller.addTransfer(invalidTransferTo);
		controller.addTransfer(invalidTransferBoth);

		context.assertEquals(invalidTransferFrom.getStatus(), TransferStatus.PENDING);
		context.assertEquals(invalidTransferTo.getStatus(), TransferStatus.PENDING);
		context.assertEquals(invalidTransferBoth.getStatus(), TransferStatus.PENDING);

		controller.processTransfer(invalidTransferFrom.getId());

		context.assertEquals(invalidTransferFrom.getStatus(), TransferStatus.ACCOUNT_NOT_FOUND);
		context.assertEquals(from.getBalance(), 0);
		context.assertEquals(to.getBalance(), 0);
		
		controller.processTransfer(invalidTransferTo.getId());

		context.assertEquals(invalidTransferTo.getStatus(), TransferStatus.ACCOUNT_NOT_FOUND);
		context.assertEquals(from.getBalance(), 0);
		context.assertEquals(to.getBalance(), 0);
		
		controller.processTransfer(invalidTransferBoth.getId());

		context.assertEquals(invalidTransferBoth.getStatus(), TransferStatus.ACCOUNT_NOT_FOUND);
		context.assertEquals(from.getBalance(), 0);
		context.assertEquals(to.getBalance(), 0);
	}
	
	@Test
	public void testProcessTransferInsuffiencientFunds(TestContext context) {
		int amount = 40;
		int overdraft = 20;
		Account from = new Account(UUID.randomUUID().toString(), overdraft);
		Account to = new Account(UUID.randomUUID().toString(), 0);
		Transfer insufficientTransfer = new Transfer(UUID.randomUUID().toString(), from.getId(), to.getId(), amount);

		Controller controller = new Controller();
		controller.addAccount(from);
		controller.addAccount(to);
		controller.addTransfer(insufficientTransfer);

		context.assertEquals(insufficientTransfer.getStatus(), TransferStatus.PENDING);

		controller.processTransfer(insufficientTransfer.getId());

		context.assertEquals(insufficientTransfer.getStatus(), TransferStatus.INSUFFICIENT_FUNDS);
		context.assertEquals(from.getBalance(), 0);
		context.assertEquals(to.getBalance(), 0);
	}

}
