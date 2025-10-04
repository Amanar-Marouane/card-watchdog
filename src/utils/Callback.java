package utils;

@FunctionalInterface
public interface Callback<T> {
    T run() throws Exception;
}
