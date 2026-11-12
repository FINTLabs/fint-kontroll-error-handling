# fint-kontroll-error-handling

The library creates problem responses with a correlation ID and can send actionable HTTP failures and `ERROR` log events to Slack. HTTP 5xx responses are alerted by default. Other HTTP statuses can be selected. All 4xx exceptions are logged at `WARN` with status, exception type, detail, and correlation ID; their stack traces are available at `DEBUG`. HTTP 5xx exceptions are logged at `ERROR` with a stack trace. Slack notification selection does not change log severity.

New to this code? Start with [How error handling and Slack alerts work](docs/how-it-works.md).

## Slack error notifications

Add the library as a dependency and configure the following properties in the consuming application:

```yaml
fint:
  application-id: my-app
  kontroll:
    slack:
      enabled: true
      bot-token: ${SLACK_BOT_TOKEN}
      channel-id: ${SLACK_CHANNEL_ID}
      grafana-url: ${GRAFANA_URL:}
      grafana-namespace: my-namespace
      additional-http-statuses: [429, 422, 409]
```

The bot token needs Slack's `chat:write` scope, and the bot must be a member of the selected channel. Put the token and channel ID in the application's 1Password secret, and expose them as `SLACK_BOT_TOKEN` and `SLACK_CHANNEL_ID` in the consuming application's environment. Keep their values out of source control. `enabled` defaults to `false`; when enabled, `bot-token` and `channel-id` must be supplied. The Grafana URL is optional; set `GRAFANA_URL` to include investigation links in alerts. Set `grafana-namespace` from the deployment namespace; if omitted, the library reads `POD_NAMESPACE`.

When a Grafana URL is configured, HTTP notifications link to ten minutes around the error and search the correlation ID. Ordinary `ERROR` log notifications link to thirty seconds around the log event, without a correlation search. Both links filter by application ID and namespace. Without a Grafana URL, alerts omit the investigation link. Additional status codes default to `429`, `422`, and `409`; `404` is excluded unless explicitly listed.

Slack messages are sent by a single background worker so an HTTP response or log call does not wait for the Slack API. Up to 256 pending messages are buffered. When the queue is full, new notifications are dropped and the dropped count is logged at `WARN` (first drop and every hundredth thereafter). Shutdown allows up to five seconds for pending messages to finish.

Publish a new library release before updating a consuming application's dependency version.
