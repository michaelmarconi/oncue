package controllers;

import org.codehaus.jackson.JsonNode;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import views.html.index;
import actors.EventMachine;

public class Application extends Controller {

	public static Result index() {
		return ok(index.render());
	}

	public static WebSocket<JsonNode> socketHandler() {
		return new WebSocket<JsonNode>() {

			public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
				EventMachine.addSocket(in, out);
			}
		};
	}

}