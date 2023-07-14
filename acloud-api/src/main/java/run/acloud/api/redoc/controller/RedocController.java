package run.acloud.api.redoc.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/api/redoc")
public class RedocController {

	@GetMapping("")
	public ModelAndView redoc() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("redoc/redoc");
		return modelAndView;
	}
	
}
