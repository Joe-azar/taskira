FROM node:26.8-alpine3.23@sha256:a3024faf41c40992531ecfb00604384665be870a44626afaf181c6d583f89296
WORKDIR /app

COPY package*.json ./
RUN npm install --global npm@12.0.2
RUN npm ci

COPY . .

EXPOSE 4200

CMD ["npm", "start", "--", "--host", "0.0.0.0"]
