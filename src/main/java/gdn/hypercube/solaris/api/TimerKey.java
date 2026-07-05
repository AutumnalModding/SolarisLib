package gdn.hypercube.solaris.api;


import org.jspecify.annotations.NonNull;

public interface TimerKey<T> extends Comparable<T> {
    T raw();

    class UUID implements TimerKey<java.util.UUID> {
        private final java.util.UUID value;

        public UUID(java.util.UUID value) {
            this.value = value;
        }

        @Override
        public java.util.UUID raw() {
            return this.value;
        }

        @Override
        public int compareTo(java.util.@NonNull UUID uuid) {
            return this.value.compareTo(uuid);
        }
    }
}
