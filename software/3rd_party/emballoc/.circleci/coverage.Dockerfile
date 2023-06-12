FROM rust:1.62.1

RUN rustup component add llvm-tools-preview
ENV PATH=/usr/local/rustup/toolchains/1.62.1-x86_64-unknown-linux-gnu/lib/rustlib/x86_64-unknown-linux-gnu/bin/:$PATH
RUN apt-get update && \
    apt-get install -y jq && \
    rm -rf /var/lib/apt/lists/*
