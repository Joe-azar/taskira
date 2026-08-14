FROM node:22-alpine
WORKDIR /app

COPY package*.json ./
RUN npm install --global npm@11.9.0
RUN npm ci

COPY . .

EXPOSE 4200

CMD ["npm", "start", "--", "--host", "0.0.0.0"]
