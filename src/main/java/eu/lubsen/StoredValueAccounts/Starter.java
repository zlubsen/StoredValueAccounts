package eu.lubsen.StoredValueAccounts;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.hazelcast.config.Config;

public class Starter {

	public static void main(String[] args) throws UnknownHostException {
		String dockerIp = InetAddress.getLocalHost().getHostAddress();
		VertxOptions options = new VertxOptions();

		options.setClusterManager(new HazelcastClusterManager(new Config("sva-cluster")));
		options.setClusterHost(dockerIp);
		Vertx.clusteredVertx(options, result -> {
			Vertx vertx = result.result();
			vertx.deployVerticle(NodeVerticle.class.getName());
		});
	}
}
