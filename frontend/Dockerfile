FROM node:26.8.1-alpine3.23@sha256:871eb674ad6e692c91330a8959f1ce2f80ba3f445cdc54e306869d2ea265e42d
WORKDIR /app

COPY package*.json ./
RUN npm install --global npm@12.0.2
RUN npm ci

COPY . .

EXPOSE 4200

CMD ["npm", "start", "--", "--host", "0.0.0.0"]
