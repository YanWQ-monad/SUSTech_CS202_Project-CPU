FROM rust:1.62

# the specific nightly version to use
ARG nightly_version

RUN rustup toolchain install nightly-$nightly_version --component miri && \
    rustup override set nightly-$nightly_version
RUN cargo miri setup --target x86_64-unknown-linux-gnu
RUN cargo miri setup --target mips64-unknown-linux-gnuabi64
