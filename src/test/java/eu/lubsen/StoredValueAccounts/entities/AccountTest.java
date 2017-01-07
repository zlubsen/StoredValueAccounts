package eu.lubsen.StoredValueAccounts.entities;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import eu.lubsen.StoredValueAccounts.entities.Account;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AccountTest {
	
	@Test
	public void testApproveDeduction(TestContext context) {
		Account account = new Account(UUID.randomUUID().toString(),50);
		
		context.assertTrue(account.approveDeduction(50));
		context.assertTrue(account.approveDeduction(49));
		context.assertFalse(account.approveDeduction(51));
	}
	
	@Test
	public void testDeductAmount(TestContext context) {
		Account account = new Account(UUID.randomUUID().toString(),50);
		
		account.deductAmount(50);
		context.assertEquals(account.getBalance(), -50);
	}
	
	@Test
	public void testAddAmount(TestContext context) {
		Account account = new Account(UUID.randomUUID().toString(),50);
		
		account.addAmount(50);
		context.assertEquals(account.getBalance(), 50);
	}

}
