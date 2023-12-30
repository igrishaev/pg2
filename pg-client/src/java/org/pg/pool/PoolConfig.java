package org.pg.pool;

public record PoolConfig (
        int minSize,
        int maxSize,
        int maxLifetime
) {

    public static Builder builder () {
        return new Builder();
    }

    public static PoolConfig standard() {
        return PoolConfig.builder().build();
    }

    public static class Builder {

        private int minSize = 2;
        private int maxSize = 8;
        private int maxLifetime = 1000 * 60 * 60;

        public Builder minSize(final int minSize) {
            this.minSize = minSize;
            return this;
        }

        public Builder maxSize(final int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder maxLifetime(final int maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        public PoolConfig build () {
            return new PoolConfig(
                    minSize,
                    maxSize,
                    maxLifetime
            );
        }
    }
}


