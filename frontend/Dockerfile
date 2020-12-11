# Load a Node v.14 Alpine Base image
FROM node:12-alpine
WORKDIR app
# Copy over source files built by Maven
COPY package.json ./
COPY node_modules ./node_modules
COPY .next ./.next
COPY public ./public
# Start the server
CMD npm run start