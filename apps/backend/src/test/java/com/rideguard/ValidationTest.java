package com.rideguard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.rideguard.shared.ApiExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;

class ValidationTest {
  // Test-only controller: exercises infrastructure without publishing a fake API.
  @RestController
  static class ValidationProbe {
    record Input(@NotBlank String value) {}

    @PostMapping("/validation-probe")
    void accept(@Valid @RequestBody Input input) {}
  }

  @Test
  void invalidAndMalformedBodiesReturnRedactedErrors() throws Exception {
    try (var validator = new LocalValidatorFactoryBean()) {
      validator.afterPropertiesSet();
      var mvc =
          MockMvcBuilders.standaloneSetup(new ValidationProbe())
              .setControllerAdvice(new ApiExceptionHandler())
              .setValidator(validator)
              .build();
      for (String body : new String[] {"{\"value\":\"\"}", "{"}) {
        mvc.perform(post("/validation-probe").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.message").value("Invalid request"))
            .andExpect(jsonPath("$.trace").doesNotExist());
      }
    }
  }
}
