package controllers.api;

import org.codehaus.jackson.node.ObjectNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

public class Status extends Controller {

	/**
	 * Returns a JSON object with all relevant service status information
	 */
	public static Result index() {

		// TODO implement!
		ObjectNode result = Json.newObject();
		result.put("version", "0.9.3-SNAPSHOT");
		result.put("failed_jobs_count", 0);
		return ok(result);
	}

}
