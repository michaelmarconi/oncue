package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

public class Jobs extends Controller {

	public static Result index() {
		return ok(index.render());
	}

	public static Result show(Long id) {
		return ok(index.render());
	}

}