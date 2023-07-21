package kr.chuyong.springspigot.external;

public interface ExternalDependencyProvider {
    <T> T get(Class<T> clazz);

    <T> T getNamed(Class<T> clazz, String qualifier);
}
