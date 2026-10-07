# Keyple Distributed Demo - server dashboard

React application monitoring the Keyple Reload Demo server, built with [Vite](https://vite.dev/).

## Prerequisites

Node.js 20.19+ or 22.12+ (required by Vite), then install the dependencies:

```bash
npm install
```

## Available Scripts

In the project directory, you can run:

### `npm start`

Runs the app in development mode at [http://localhost:3000](http://localhost:3000), with hot reload.

Be aware that the application relies on the Keyple Reload Demo server API: the API calls (`/activity`, `/card`) are
forwarded to the server at http://localhost:8080 (see `vite.config.js`), which must be running.

### `npm run build`

Builds the app for production into the `build` folder.

### `npm run lint`

Checks the source code with [ESLint](https://eslint.org/) (configuration in `eslint.config.js`). This check is also run
by the server build (`check` task), and therefore by the CI.

### `npm run preview`

Serves the production build locally, to check it before packaging.

## Package the application

The dashboard is built and embedded into the server by the server Gradle build:

```bash
cd ..
./gradlew build
```

See the [server documentation](../README.md).
