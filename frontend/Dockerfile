FROM node:26.10.0-alpine3.23@sha256:c3c6e314fd42e41962360b2482fc18d150beb47976c3aa7b8b9689d7ef42a5c2
WORKDIR /app

COPY package*.json ./
RUN npm install --global npm@12.0.2
RUN npm ci

COPY . .

EXPOSE 4200

CMD ["npm", "start", "--", "--host", "0.0.0.0"]
