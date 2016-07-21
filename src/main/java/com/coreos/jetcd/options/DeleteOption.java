package com.coreos.jetcd.options;

public class DeleteOption {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Builder() {}

        // TODO: add prevKV
        //       add end key

        public DeleteOption build() {
            return new DeleteOption();
        }

    }

    private DeleteOption() {
    }

}
