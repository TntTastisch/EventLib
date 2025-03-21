package de.tnttastisch.eventlib.callback;

/**
 * Callback interface for asynchronous operations.
 *
 * @param <V> Type of the result returned on success.
 */
public interface AsyncCallback<V> {
    void onSuccess(V result);

    void onFailure(V result, Throwable error);
}
