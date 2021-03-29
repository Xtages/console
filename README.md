# Getting Started

To get started run [`setup.sh`](setup.sh) (currently only works on macOS).

`setup.sh` should install the necessary utilities for the environment to run. It will also create a new DB called `xtages_console` belonging to the `xtages_console` user in `postgresql`.

## Connecting to the DB

Run `psql xtages_console -U xtages_console`.

# Running the dev servers

For development we need to run two servers:

* frontend (`cd frontend & npm start`)
* backend (`cd backend & gradle bootRun`)

# Other resources

The frontend server is a React app that was generated using `create-react-app`, see [frontend/HELP.md](frontend/HELP.md) for more details.

The backend server is a Sprint Boot server that was generated using [Spring Initializr](https://start.spring.io/), see [backend/HELP.md](backend/HELP.md) for more details.