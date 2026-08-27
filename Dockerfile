FROM node:20-alpine
WORKDIR /app
COPY package*.json tsconfig.json ./
RUN npm install -g ts-node typescript
RUN npm install
COPY . .
RUN npm run build
EXPOSE 5000
CMD ["npm", "start"]
