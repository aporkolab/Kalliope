package hu.porkolab.kalliope.api;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Hibaválaszok RFC 9457 (ProblemDetail) szerint. A motor
 * {@link IllegalArgumentException}-t dob ismeretlen beállításra vagy
 * mértékazonosítóra — az a hívó hibája, tehát 400, nem 500.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Hibás kérés");
        problem.setType(URI.create("https://kalliope.porkolab.hu/problems/invalid-request"));
        return problem;
    }
}
