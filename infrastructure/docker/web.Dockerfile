# SpexCrafters Web — multi-stage build (Next.js standalone output)
# Build context: repository root
#   docker build -f infrastructure/docker/web.Dockerfile -t spexcrafters/web .

FROM node:22-alpine AS deps
RUN corepack enable
WORKDIR /workspace
COPY pnpm-workspace.yaml package.json .npmrc ./
COPY apps/web/package.json apps/web/
COPY packages/design-tokens/package.json packages/design-tokens/
COPY packages/ui/package.json packages/ui/
COPY packages/api-client/package.json packages/api-client/
COPY packages/config/package.json packages/config/
COPY pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile

FROM deps AS build
COPY packages ./packages
COPY apps/web ./apps/web
RUN pnpm --filter @spexcrafters/design-tokens build \
 && pnpm --filter @spexcrafters/web build

FROM node:22-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build --chown=app:app /workspace/apps/web/.next/standalone ./
COPY --from=build --chown=app:app /workspace/apps/web/.next/static ./apps/web/.next/static
COPY --from=build --chown=app:app /workspace/apps/web/public ./apps/web/public
EXPOSE 3000
ENV PORT=3000 HOSTNAME=0.0.0.0
CMD ["node", "apps/web/server.js"]
