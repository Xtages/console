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

# Run console pointing to dev
This is a workaround until we develop the assume role for dev in the Console code.

Run assume-role to get the credentials
```yaml
aws sts assume-role --role-arn "arn:aws:iam::605769209612:role/task-role-console" \
--role-session-name console-dev --duration-seconds 42300 --profile terraform-dev
```

Once you have the credentials and token add it in your `~/.aws/credentials` as your **default profile**
```
[default]
aws_access_key_id = SOMETHING
aws_secret_access_key = SOMETHING_ELSE
aws_session_token = TOKEN
```
Note: The token will last for 12 hours, after that the keys and token need to be updated
