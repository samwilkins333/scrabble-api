package com.swilkins.scrabbleapi.controller.proceed;

import com.swilkins.scrabbleapi.JDIDebuggerServer;
import com.swilkins.scrabbleapi.controller.proceed.model.ProceedRequest;
import com.swilkins.scrabbleapi.controller.proceed.model.ProceedResponse;
import com.swilkins.scrabbleapi.debug.Debugger;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProceedController {

  @PostMapping("/proceed")
  public ProceedResponse proceed(@RequestBody ProceedRequest request) {
    Debugger debugger = JDIDebuggerServer.getDebugger();
    debugger.setRequestedStepRequestDepth(request.depth);
    debugger.signalAndAwaitCoroutine();

    ProceedResponse response = new ProceedResponse();
    response.dereferencedVariables = debugger.getDereferencedVariables();
    response.location = debugger.getSuspendedLocation();

    return response;
  }

}
