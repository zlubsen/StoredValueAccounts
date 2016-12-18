package eu.lubsen.StoredValueAccounts;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.hazelcast.config.Config;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Starter {

	public static void main(String[] args) throws UnknownHostException, IOException {
		String dockerIp = InetAddress.getLocalHost().getHostAddress();
		VertxOptions options = new VertxOptions();
		
		DeploymentOptions depoptions = new DeploymentOptions().setConfig(getConfigJson(args));
		
		options.setClusterManager(new HazelcastClusterManager(new Config("sva-cluster")));
		options.setClusterHost(dockerIp);
		Vertx.clusteredVertx(options, result -> {
			Vertx vertx = result.result();
			vertx.deployVerticle(NodeVerticle.class.getName(), depoptions);
		});
	}

	private static JsonObject getConfigJson(String[] args) throws IOException {
		File configFile = new File(getPathFromArgs(args));
		if(configFile.exists()) {
			return new JsonObject(getFileContents(configFile));
		}

		return new JsonObject();
	}
	
	private static String getFileContents(File file) throws IOException {
		List<String> lines = Files.readAllLines(file.toPath());
		return String.join("", lines);
	}
	
	private static String getPathFromArgs(String[] args) {
		String path = "";
		Options clioptions = new Options();
		String argName = "conf";
		clioptions.addOption(argName, true, "Vertx JSON configuration file");
		
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine line = parser.parse(clioptions, args);
			if(line.hasOption(argName)) {
				path = line.getOptionValue(argName).toString();
			}
		} catch (ParseException pe) {
			System.out.println("Invalid cli options provided");
		}
		return path;
	}
}
