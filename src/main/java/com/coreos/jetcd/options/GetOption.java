package com.coreos.jetcd.options;

public class GetOption {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private long limit = 0L;

        private Builder() {}

        public Builder withLimit(long limit) {
            this.limit = limit;
            return this;
        }

        // TODO: add sort options
        //       add keysOnly
        //       add countOnly
        //       add serializable
        //       add revision
        //       add end key

        public GetOption build() {
            return new GetOption(limit);
        }

    }

    private final long limit;

    private GetOption(long limit) {
        this.limit = limit;
    }

}
