# Sandboxed Execution Worker for Untrusted Java Code
FROM eclipse-temurin:17-jdk-alpine

# Set non-root sandbox user
RUN addgroup -g 1001 sandbox && \
    adduser -D -u 1001 -G sandbox sandbox

# Create sandbox runtime directory
WORKDIR /sandbox

# Copy test harness dependencies / JUnit runner jars if needed
# COPY lib /sandbox/lib

# Ensure unprivileged permissions
RUN chown -R sandbox:sandbox /sandbox

USER sandbox

# Security defaults: ephemerality and non-interactive execution
ENTRYPOINT ["/bin/sh"]
