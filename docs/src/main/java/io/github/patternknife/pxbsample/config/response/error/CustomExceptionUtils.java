package io.github.patternknife.pxbsample.config.response.error;

import io.github.patternknife.pxbsample.config.logger.common.CommonLoggingRequest;
import io.github.patternknife.pxbsample.config.logger.dto.ErrorDetails;
import io.github.patternknife.pxbsample.config.logger.module.ResponseSuccessLogConfig;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.HashMap;
import java.util.Map;

public class CustomExceptionUtils {

    private static final Logger logger = LoggerFactory.getLogger(ResponseSuccessLogConfig.class);

    public static void createNonStoppableErrorMessage(String message) {

        logger.error("[NON-STOPPABLE ERROR] : ");

        try {
            CommonLoggingRequest commonLoggingRequest = new CommonLoggingRequest();
            logger.error(commonLoggingRequest.getText());
        } catch (Exception ex2) {
            logger.error(ex2.getMessage());
        } finally {
            ErrorDetails errorDetails = new ErrorDetails(message, "Without error param " + " / Thread ID = " + Thread.currentThread().getId() + " / StackTrace",
                    message, "", "");

            logger.error(" / " + errorDetails.toString());
        }

    }

    public static void createNonStoppableErrorMessage(String message, Throwable ex) {

        logger.error("[NON-STOPPABLE ERROR] : ");

        try {
            CommonLoggingRequest commonLoggingRequest = new CommonLoggingRequest();
            logger.error(commonLoggingRequest.getText());
        } catch (Exception ex2) {
            logger.error(ex2.getMessage());
        } finally {
            ErrorDetails errorDetails = new ErrorDetails(message, "Without error param " + " / Thread ID = " + Thread.currentThread().getId() + " / StackTrace",
                    message, CustomExceptionUtils.getAllStackTraces(ex), CustomExceptionUtils.getAllCauses(ex));

            logger.error(" / " + errorDetails.toString());
        }

    }

    public static String getAllCausesWithStartMessage(Throwable e, String causes) {
        if (e.getCause() == null) return causes;
        causes += e.getCause() + " / ";
        return getAllCausesWithStartMessage(e.getCause(), causes);
    }

    public static String getAllCauses(Throwable e) {
        String causes = "";
        return getAllCausesWithStartMessage(e, causes);
    }

    public static String getAllStackTraces(Throwable e) {
        return ExceptionUtils.getStackTrace(e);
    }


    public static Map<String, String> extractMethodArgumentNotValidErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return errors;
    }
}
