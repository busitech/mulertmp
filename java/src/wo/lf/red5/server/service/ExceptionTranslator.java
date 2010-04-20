package wo.lf.red5.server.service;

import flex.messaging.MessageException;

/**
 * Duplicated spring translator.
 */
public interface ExceptionTranslator {

    /**
     * Checks if the translator can handle the specified exception class
     *
     * @param clazz the class of the exception
     * @return true if the exception type can be handled
     */
    boolean handles(Class<?> clazz);

    /**
     * Translate the specified exception into an appropriate {@link flex.messaging.MessageException}
     *
     * @param t the original exception
     * @return the translated exception
     */
    MessageException translate(Throwable t);

}
